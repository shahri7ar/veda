package com.vibemusic.app.data.source

import com.vibemusic.app.data.model.MountConfig
import com.vibemusic.app.data.model.SourceType
import com.vibemusic.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.InputStream

/**
 * FTP listing + streaming. Uses Apache Commons Net.
 * One FTPClient per call — playback and scan can run independently.
 */
class FtpDataSource {

    private fun connect(cfg: MountConfig): FTPClient {
        val client = FTPClient()
        client.connectTimeout = 15_000
        client.connect(cfg.host, if (cfg.port > 0) cfg.port else 21)
        if (!FTPReply.isPositiveCompletion(client.replyCode)) {
            client.disconnect()
            throw IllegalStateException("FTP connect refused (${client.replyCode})")
        }
        val ok = if (cfg.username.isBlank()) client.login("anonymous", "anonymous@vibemusic")
                 else client.login(cfg.username, cfg.password)
        if (!ok) {
            client.disconnect()
            throw IllegalStateException("FTP login failed")
        }
        client.enterLocalPassiveMode()
        client.setFileType(FTPClient.BINARY_FILE_TYPE)
        client.controlEncoding = "UTF-8"
        return client
    }

    suspend fun testConnection(cfg: MountConfig): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val c = connect(cfg)
            c.logout(); c.disconnect()
        }
    }

    suspend fun listTracks(cfg: MountConfig): List<Track> = withContext(Dispatchers.IO) {
        val out = mutableListOf<Track>()
        val client = connect(cfg)
        try {
            walk(client, cfg.path.ifBlank { "/" }, cfg, out)
        } finally {
            runCatching { client.logout() }
            runCatching { client.disconnect() }
        }
        out
    }

    private fun walk(c: FTPClient, path: String, cfg: MountConfig, out: MutableList<Track>) {
        val files = runCatching { c.listFiles(path) }.getOrNull() ?: return
        for (f in files) {
            val name = f.name ?: continue
            if (name == "." || name == "..") continue
            val full = if (path.endsWith("/")) "$path$name" else "$path/$name"
            if (f.isDirectory) {
                walk(c, full, cfg, out)
            } else if (SmbDataSource.isAudio(name)) {
                val uri = "ftp://${cfg.host}:${if (cfg.port > 0) cfg.port else 21}$full"
                out += Track(
                    id = SmbDataSource.hash(uri),
                    title = name.substringBeforeLast('.'),
                    artist = "Unknown Artist",
                    album = path.substringAfterLast('/', "Unknown Album"),
                    sourceUri = uri,
                    sourceType = SourceType.FTP,
                    mountId = cfg.id,
                    sizeBytes = f.size
                )
            }
        }
    }

    suspend fun openStream(cfg: MountConfig, remotePath: String): InputStream = withContext(Dispatchers.IO) {
        val client = connect(cfg)
        val inner = try {
            client.retrieveFileStream(remotePath)
                ?: throw IllegalStateException("FTP stream failed for $remotePath")
        } catch (e: Exception) {
            runCatching { client.disconnect() }
            throw e
        }
        object : InputStream() {
            override fun read() = inner.read()
            override fun read(b: ByteArray, off: Int, len: Int) = inner.read(b, off, len)
            override fun available() = inner.available()
            override fun close() {
                runCatching { inner.close() }
                runCatching { client.completePendingCommand() }
                runCatching { client.logout() }
                runCatching { client.disconnect() }
            }
        }
    }
}
