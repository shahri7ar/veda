package com.vibemusic.app.data.model

/** A single track from any source (SMB / FTP / local). */
data class Track(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val albumId: String = "",
    val durationMs: Long = 0L,
    val sourceUri: String,
    val sourceType: SourceType = SourceType.LOCAL,
    val mountId: String? = null,
    val sizeBytes: Long = 0L,
    val cached: Boolean = false,
    val cachePath: String? = null,
    val albumArtUri: String? = null,
    val isFavorite: Boolean = false,
    val year: Int = 0,
)

enum class SourceType { LOCAL, SMB, FTP }

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int = 0,
    val trackCount: Int = 0,
    val artUri: String? = null,
    val sourceType: SourceType = SourceType.LOCAL,
)

data class Artist(
    val name: String,
    val trackCount: Int = 0,
    val albumCount: Int = 0,
    val artUri: String? = null,
)

data class MountConfig(
    val id: String = "",
    val name: String = "New mount",
    val type: SourceType = SourceType.SMB,
    val host: String = "",
    val port: Int = 0,
    val share: String = "",
    val path: String = "/",
    val username: String = "",
    val password: String = "",
    val domain: String = "",
    val enabled: Boolean = true,
)

data class Playlist(
    val id: Long = 0,
    val name: String,
    val trackIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)
