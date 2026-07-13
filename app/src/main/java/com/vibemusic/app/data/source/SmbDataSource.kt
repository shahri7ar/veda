package com.vibemusic.app.data.source

import com.vibemusic.app.data.model.MountConfig
import com.vibemusic.app.data.model.SourceType
import com.vibemusic.app.data.model.Track
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest
import java.util.EnumSet

/**
 * Streaming + indexing for SMB/CIFS shares. Each method opens its own short-lived
 * session so that simultaneous reads (scan + playback + cache fetch) don't fight
 * over a single connection.
 */
class SmbDataSource {

    private val client by lazy { SMBClient() }

    private fun open(cfg: MountConfig): Triple<Connection, Session, DiskShare> {
        val conn = client.connect(cfg.host, if (cfg.port > 0) cfg.port else 445)
        try {
            val auth = if (cfg.username.isBlank()) AuthenticationContext.anonymous()
            else AuthenticationContext(cfg.username, cfg.password.toCharArray(), cfg.domain)
            val session = conn.authenticate(auth)
            try {
                val share = session.connectShare(cfg.share) as DiskShare
                return Triple(conn, session, share)
            } catch (e: Exception) {
                runCatching { session.close() }
                throw e
            }
        } catch (e: Exception) {
            runCatching { conn.close() }
            throw e
        }
    }

    suspend fun testConnection(cfg: MountConfig): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (conn, _, share) = open(cfg)
            share.close(); conn.close()
        }
    }

    /** Recursively walks the share for audio files. */
    suspend fun listTracks(cfg: MountConfig): List<Track> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Track>()
        val (conn, _, share) = open(cfg)
        try {
            walk(share, cfg.path.trimEnd('/'), cfg, result)
        } finally {
            runCatching { share.close() }
            runCatching { conn.close() }
        }
        result
    }

    private fun walk(share: DiskShare, path: String, cfg: MountConfig, out: MutableList<Track>) {
        val safePath = if (path.isEmpty()) "" else path.trimStart('/')
        if (!share.folderExists(safePath)) return
        share.list(safePath).forEach { info ->
            val name = info.fileName
            if (name == "." || name == "..") return@forEach
            val child = if (safePath.isEmpty()) name else "$safePath/$name"
            val isDir = (info.fileAttributes and 0x10L) != 0L
            if (isDir) {
                walk(share, child, cfg, out)
            } else if (isAudio(name)) {
                val uri = "smb://${cfg.host}/${cfg.share}/$child"
                out += Track(
                    id = hash(uri),
                    title = name.substringBeforeLast('.'),
                    artist = "Unknown Artist",
                    album = path.substringAfterLast('/', "Unknown Album"),
                    sourceUri = uri,
                    sourceType = SourceType.SMB,
                    mountId = cfg.id,
                    sizeBytes = info.endOfFile
                )
            }
        }
    }

    /** Open a streaming InputStream for [remotePath] (relative to the share root). */
    suspend fun openStream(cfg: MountConfig, remotePath: String): InputStream = withContext(Dispatchers.IO) {
        val (conn, session, share) = open(cfg)
        val file: File = try {
            share.openFile(
                remotePath.trimStart('/'),
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
        } catch (e: Exception) {
            runCatching { share.close() }
            runCatching { session.close() }
            runCatching { conn.close() }
            throw e
        }
        // wrap so closing the stream closes everything
        object : InputStream() {
            private val inner = file.inputStream
            override fun read() = inner.read()
            override fun read(b: ByteArray, off: Int, len: Int) = inner.read(b, off, len)
            override fun available() = inner.available()
            override fun close() {
                runCatching { inner.close() }
                runCatching { file.close() }
                runCatching { share.close() }
                runCatching { session.close() }
                runCatching { conn.close() }
            }
        }
    }

    companion object {
        private val AUDIO_EXT = setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "wma", "alac")
        fun isAudio(name: String) = AUDIO_EXT.contains(name.substringAfterLast('.', "").lowercase())
        fun hash(s: String): String {
            val md = MessageDigest.getInstance("SHA-1").digest(s.toByteArray())
            return md.joinToString("") { "%02x".format(it) }.take(20)
        }
    }
}
