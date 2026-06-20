package com.vibemusic.app.data

import com.vibemusic.app.VibeMusicApp
import com.vibemusic.app.data.model.MountConfig
import com.vibemusic.app.data.model.Playlist
import com.vibemusic.app.data.model.SourceType
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.source.FtpDataSource
import com.vibemusic.app.data.source.SmbDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single place the UI layer talks to for tracks, mounts, and playlists.
 */
class MusicRepository {

    private val db get() = VibeMusicApp.instance.database
    private val secure get() = VibeMusicApp.instance.settingsRepository.secure()

    // ─── Tracks ────────────────────────────────────────────────────

    fun observeTracks(): Flow<List<Track>> =
        db.trackDao().observeAll().map { rows -> rows.map { it.toModel() } }

    fun observeCachedTracks(): Flow<List<Track>> =
        db.trackDao().observeCached().map { rows -> rows.map { it.toModel() } }

    fun observeFavorites(): Flow<List<Track>> =
        db.trackDao().observeAll().map { rows ->
            rows.filter { it.isFavorite }.map { it.toModel() }
        }

    suspend fun toggleFavorite(trackId: String) {
        val current = db.trackDao().byId(trackId) ?: return
        db.trackDao().update(current.copy(isFavorite = !current.isFavorite))
    }

    // ─── Mounts ────────────────────────────────────────────────────

    fun observeMounts(): Flow<List<MountConfig>> =
        db.mountDao().observeAll().map { rows -> rows.map { it.toModel() } }

    suspend fun upsertMount(cfg: MountConfig) {
        secure.edit().putString("pw_${cfg.id}", cfg.password).apply()
        db.mountDao().upsert(
            MountEntity(
                id = cfg.id, name = cfg.name, type = cfg.type.name,
                host = cfg.host, port = cfg.port, share = cfg.share, path = cfg.path,
                username = cfg.username, passwordEnc = "stored", domain = cfg.domain,
                enabled = cfg.enabled,
            )
        )
    }

    suspend fun deleteMount(id: String) {
        secure.edit().remove("pw_$id").apply()
        db.trackDao().deleteByMount(id)
        db.mountDao().delete(id)
    }

    suspend fun scanMount(cfg: MountConfig): Int {
        val tracks = when (cfg.type) {
            SourceType.SMB -> SmbDataSource().listTracks(cfg)
            SourceType.FTP -> FtpDataSource().listTracks(cfg)
            SourceType.LOCAL -> emptyList()
        }
        db.trackDao().deleteByMount(cfg.id)
        db.trackDao().upsertAll(tracks.map { it.toEntity() })
        return tracks.size
    }

    suspend fun testMount(cfg: MountConfig): Result<Unit> = when (cfg.type) {
        SourceType.SMB -> SmbDataSource().testConnection(cfg)
        SourceType.FTP -> FtpDataSource().testConnection(cfg)
        SourceType.LOCAL -> Result.success(Unit)
    }

    // ─── Local scanning ─────────────────────────────────────────────

    suspend fun scanLocal(): Int {
        val context = VibeMusicApp.instance
        val scanner = com.vibemusic.app.data.local.LocalScanner(context)
        val tracks = scanner.scanDevice()
        db.trackDao().clearLocal()
        db.trackDao().upsertAll(tracks.map { it.toEntity() })
        return tracks.size
    }

    // ─── Playlists ─────────────────────────────────────────────────

    fun observePlaylists(): Flow<List<Playlist>> =
        db.playlistDao().observeAll().map { rows ->
            rows.map { pl ->
                val ids = pl.trackIdsJson.ifBlank { "" }
                    .split("|").filter { it.isNotBlank() }
                Playlist(pl.id, pl.name, ids, pl.createdAt)
            }
        }

    suspend fun createPlaylist(name: String): Long = db.playlistDao().upsert(
        PlaylistEntity(name = name, trackIdsJson = "",
            createdAt = System.currentTimeMillis()),
    )

    suspend fun addToPlaylist(pid: Long, trackId: String) {
        val pl = db.playlistDao().byId(pid) ?: return
        val ids = (pl.trackIdsJson.ifBlank { "" }.split("|") + trackId)
            .filter { it.isNotBlank() }
            .distinct()
        db.playlistDao().upsert(pl.copy(trackIdsJson = ids.joinToString("|")))
    }

    suspend fun removeFromPlaylist(pid: Long, trackId: String) {
        val pl = db.playlistDao().byId(pid) ?: return
        val ids = pl.trackIdsJson.split("|")
            .filter { it.isNotBlank() && it != trackId }
        db.playlistDao().upsert(pl.copy(trackIdsJson = ids.joinToString("|")))
    }

    suspend fun deletePlaylist(id: Long) {
        db.playlistDao().delete(id)
    }

    suspend fun getPlaylistTracks(pid: Long): List<Track> {
        val pl = db.playlistDao().byId(pid) ?: return emptyList()
        return pl.trackIdsJson.split("|")
            .filter { it.isNotBlank() }
            .mapNotNull { db.trackDao().byId(it)?.toModel() }
    }

    // ─── Mappers ───────────────────────────────────────────────────

    private fun TrackEntity.toModel() = Track(
        id = id, title = title, artist = artist, album = album,
        albumId = albumId, durationMs = durationMs,
        sourceUri = sourceUri, sourceType = SourceType.valueOf(sourceType),
        mountId = mountId, sizeBytes = sizeBytes,
        cached = cached, cachePath = cachePath,
        albumArtUri = albumArtUri, isFavorite = isFavorite,
        year = year,
    )

    private fun Track.toEntity() = TrackEntity(
        id = id, title = title, artist = artist, album = album,
        albumId = albumId, durationMs = durationMs,
        sourceUri = sourceUri, sourceType = sourceType.name,
        mountId = mountId, sizeBytes = sizeBytes,
        cached = cached, cachePath = cachePath,
        albumArtUri = albumArtUri, isFavorite = isFavorite,
        year = year,
    )

    private fun MountEntity.toModel() = MountConfig(
        id = id, name = name, type = SourceType.valueOf(type),
        host = host, port = port, share = share, path = path,
        username = username,
        password = secure.getString("pw_$id", "") ?: "",
        domain = domain, enabled = enabled,
    )
}
