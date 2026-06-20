package com.vibemusic.app.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.vibemusic.app.VibeMusicApp
import com.vibemusic.app.data.cache.CacheManager
import com.vibemusic.app.data.model.MountConfig
import com.vibemusic.app.data.model.SourceType
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.source.FtpDataSource
import com.vibemusic.app.data.source.SmbDataSource
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.io.InputStream

/**
 * Custom [DataSource] that resolves smb:// and ftp:// URIs and routes them through
 * the cache. Local file:// URIs and cached files fall back to a plain FileInputStream.
 */
class NetworkAudioDataSource(
    private val mounts: () -> List<MountConfig>,
    private val cache: CacheManager
) : BaseDataSource(/* isNetwork = */ true) {

    private var stream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        uri = dataSpec.uri
        val scheme = dataSpec.uri.scheme.orEmpty()

        // Resolve the matching Track if we know about it (for cache lookup).
        val track = lookupTrack(dataSpec.uri.toString())

        // 1) Already cached fully? -> serve from disk.
        if (track != null && cache.isFullyCached(track)) {
            cache.touch(track)
            val f = cache.fileFor(track)
            val fis = FileInputStream(f)
            if (dataSpec.position > 0) fis.skip(dataSpec.position)
            stream = fis
            bytesRemaining = f.length() - dataSpec.position
            opened = true
            transferStarted(dataSpec)
            return bytesRemaining
        }

        // 2) Open from network.
        val net: InputStream = when (scheme) {
            "smb" -> openSmb(dataSpec.uri)
            "ftp" -> openFtp(dataSpec.uri)
            "file" -> FileInputStream(dataSpec.uri.path!!)
            else -> throw UnsupportedOperationException("Unsupported scheme: $scheme")
        }

        // 3) Tee through cache when we recognize the track (and not a seek).
        stream = if (track != null && dataSpec.position == 0L) cache.teeStream(track, net) else net

        if (dataSpec.position > 0) stream!!.skip(dataSpec.position)

        bytesRemaining =
            if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length
            else if (track != null && track.sizeBytes > 0) track.sizeBytes - dataSpec.position
            else C.LENGTH_UNSET.toLong()

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val toRead =
            if (bytesRemaining == C.LENGTH_UNSET.toLong()) length
            else minOf(length.toLong(), bytesRemaining).toInt()
        val read = stream!!.read(buffer, offset, toRead)
        if (read == -1) return C.RESULT_END_OF_INPUT
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= read.toLong()
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        if (opened) {
            runCatching { stream?.close() }
            opened = false
            transferEnded()
        }
        stream = null
    }

    private fun lookupTrack(uri: String): Track? {
        val db = VibeMusicApp.instance.database.trackDao()
        return runBlocking { db.byId(SmbDataSource.hash(uri)) }?.let {
            Track(
                id = it.id, title = it.title, artist = it.artist, album = it.album,
                durationMs = it.durationMs, sourceUri = it.sourceUri,
                sourceType = SourceType.valueOf(it.sourceType),
                mountId = it.mountId, sizeBytes = it.sizeBytes,
                cached = it.cached, cachePath = it.cachePath, albumArtUri = it.albumArtUri
            )
        }
    }

    private fun openSmb(uri: Uri): InputStream {
        // smb://host/share/path/to/file.mp3
        val host = uri.host ?: error("SMB host missing")
        val segs = uri.pathSegments
        require(segs.isNotEmpty()) { "SMB share missing" }
        val share = segs[0]
        val remotePath = segs.drop(1).joinToString("/")
        val cfg = mounts().firstOrNull { it.type == SourceType.SMB && it.host == host && it.share == share }
            ?: error("No SMB mount configured for $host/$share")
        return runBlocking { SmbDataSource().openStream(cfg, remotePath) }
    }

    private fun openFtp(uri: Uri): InputStream {
        val host = uri.host ?: error("FTP host missing")
        val port = if (uri.port > 0) uri.port else 21
        val cfg = mounts().firstOrNull { it.type == SourceType.FTP && it.host == host && (it.port == port || it.port == 0) }
            ?: error("No FTP mount configured for $host")
        return runBlocking { FtpDataSource().openStream(cfg, uri.path ?: "/") }
    }

    class Factory(
        private val mounts: () -> List<MountConfig>,
        private val cache: CacheManager
    ) : DataSource.Factory {
        private val listeners = mutableListOf<TransferListener>()
        fun addTransferListener(listener: TransferListener) { listeners += listener }
        override fun createDataSource(): DataSource {
            val ds = NetworkAudioDataSource(mounts, cache)
            listeners.forEach { ds.addTransferListener(it) }
            return ds
        }
    }
}
