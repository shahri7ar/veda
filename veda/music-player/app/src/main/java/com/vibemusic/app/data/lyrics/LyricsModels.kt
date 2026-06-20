package com.vibemusic.app.data.lyrics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Full response from LRCLIB exact fetch / search endpoints.
 */
@Serializable
data class LrclibResponse(
    val id: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Long? = null,
    val instrumental: Boolean? = null,

    @SerialName("plainLyrics")
    val plainLyrics: String? = null,

    @SerialName("syncedLyrics")
    val syncedLyrics: String? = null,
)

/**
 * Sealed UI state for the lyrics screen / overlay.
 */
sealed interface LyricsState {
    /** Nothing has been attempted yet. */
    data object Idle : LyricsState

    /** Fetching lyrics from the network. */
    data object Loading : LyricsState

    /**
     * Lyrics were successfully fetched.
     * @param lines  the lyrics text (plain or synced).
     * @param isSynced  `true` when [lines] contains LRC timestamps.
     */
    data class Success(
        val lines: String,
        val isSynced: Boolean = false,
    ) : LyricsState

    /**
     * Exact match returned 404 — we have a list of search results.
     * The UI should show this list so the user can pick the correct track.
     */
    data class SearchResults(
        val items: List<LrclibResponse>,
    ) : LyricsState

    /**
     * A terminal error happened (network, parsing, etc.).
     */
    data class Error(
        val message: String,
    ) : LyricsState
}
