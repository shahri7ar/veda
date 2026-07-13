package com.vibemusic.app.data.cache

import android.content.Context
import com.vibemusic.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

/**
 * Disk-backed cache for streamed audio.
 *
 * Layout:   /cacheDir/music/<id>.dat
 * Strategy: write-through during playback (the player writes bytes it reads
 *           from the network into the cache file). When the file size matches
 *           the expected source size, the track is marked "fully cached" and
 *           future plays go straight to disk.
 *
 * LRU eviction is triggered when totalSize > maxBytes. The least-recently-
 * touched files are removed first.
 */
class CacheManager(context: Context) {

    private val root: File = File(context.cacheDir, "music").apply { mkdirs() }
    private var maxBytes: Long = 1024L * 1024L * 1024L   // default 1 GB

    private val _usage = MutableStateFlow(0L)
    val usageBytes: StateFlow<Long> = _usage

    init { recalcUsage() }

    fun setMaxBytes(value: Long) { maxBytes = value.coerceAtLeast(64L * 1024 * 1024) }

    fun fileFor(track: Track): File = File(root, "${track.id}.dat")

    fun isFullyCached(track: Track): Boolean {
        val f = fileFor(track)
        return f.exists() && (track.sizeBytes <= 0 || f.length() >= track.sizeBytes)
    }

    fun touch(track: Track) { fileFor(track).setLastModified(System.currentTimeMillis()) }

    /**
     * Pipe a network [InputStream] through to the player AND to the local cache file.
     * The returned InputStream is a "tee" — every byte handed to the player is also
     * persisted to disk. Closing it finalizes the cache file.
     */
    fun teeStream(track: Track, network: InputStream): InputStream {
        val target = fileFor(track)
        val tmp = File(target.parentFile, "${target.name}.part")
        val out: OutputStream = tmp.outputStream().buffered()
        var totalWritten = 0L
        return object : InputStream() {
            override fun read(): Int {
                val b = network.read()
                if (b >= 0) { out.write(b); totalWritten++ }
                return b
            }
            override fun read(buf: ByteArray, off: Int, len: Int): Int {
                val n = network.read(buf, off, len)
                if (n > 0) { out.write(buf, off, n); totalWritten += n }
                return n
            }
            override fun available() = network.available()
            override fun close() {
                runCatching { out.flush(); out.close() }
                runCatching { network.close() }
                if (totalWritten > 0) {
                    if (target.exists()) target.delete()
                    tmp.renameTo(target)
                    target.setLastModified(System.currentTimeMillis())
                    recalcUsage()
                    enforceQuota()
                } else {
                    tmp.delete()
                }
            }
        }
    }

    /** Wholesale evict — used from settings UI. */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        root.listFiles()?.forEach { it.delete() }
        recalcUsage()
    }

    suspend fun delete(track: Track) = withContext(Dispatchers.IO) {
        fileFor(track).delete()
        recalcUsage()
    }

    private fun recalcUsage() {
        val sum = root.listFiles()?.sumOf { it.length() } ?: 0L
        _usage.value = sum
    }

    private fun enforceQuota() {
        if (_usage.value <= maxBytes) return
        val files = root.listFiles()?.toMutableList() ?: return
        files.sortBy { it.lastModified() }              // oldest first
        var freed = 0L
        val needed = _usage.value - maxBytes
        for (f in files) {
            if (freed >= needed) break
            freed += f.length()
            f.delete()
        }
        recalcUsage()
    }

    companion object {
        fun keyFor(uri: String): String {
            val d = MessageDigest.getInstance("SHA-1").digest(uri.toByteArray())
            return d.joinToString("") { "%02x".format(it) }.take(20)
        }
    }
}
