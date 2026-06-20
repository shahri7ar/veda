package com.vibemusic.app.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orchestrates the LRCLIB fallback flow described in the requirements.
 *
 *  1. Exact fetch  → Success or 404
 *  2. If 404       → Search
 *  3. If search returns 1 hit → use it; otherwise → return search list
 */
class LyricsRepository {

    /**
     * Returns one of:
     *  - [LyricsResult.Single]   when we have lyrics to display,
     *  - [LyricsResult.List]     when the user must pick from search results,
     *  - [LyricsResult.NotFound] when nothing matched at all,
     *  - [LyricsResult.Error]    when a network / parsing error occurred.
     */
    suspend fun fetchLyrics(
        artist: String,
        track: String,
        durationSeconds: Long,
    ): LyricsResult = withContext(Dispatchers.IO) {
        try {
            val exact = LrclibApi.getExact(artist, track, durationSeconds)
            exact.fold(
                onSuccess = { r ->
                    val lines = r.syncedLyrics ?: r.plainLyrics
                    if (!lines.isNullOrBlank()) {
                        LyricsResult.Single(lines, r.syncedLyrics != null)
                    } else {
                        fallbackSearch(artist, track)
                    }
                },
                onFailure = { cause ->
                    if (cause is NoSuchElementException) {
                        fallbackSearch(artist, track)
                    } else {
                        LyricsResult.Error(cause.message ?: "Unknown error")
                    }
                },
            )
        } catch (e: Exception) {
            LyricsResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Fetch lyrics for a specific LRCLIB track id (used in step 4).
     */
    suspend fun fetchById(id: Long): LyricsResult = withContext(Dispatchers.IO) {
        val res = LrclibApi.getById(id)
        res.fold(
            onSuccess = { resp ->
                val lines = resp.syncedLyrics ?: resp.plainLyrics
                if (!lines.isNullOrBlank()) {
                    LyricsResult.Single(lines, resp.syncedLyrics != null)
                } else {
                    LyricsResult.NotFound
                }
            },
            onFailure = { LyricsResult.Error(it.message ?: "Unknown error") },
        )
    }

    // ---------------------------------------------------------------
    //  Private
    // ---------------------------------------------------------------

    private suspend fun fallbackSearch(
        artist: String,
        track: String,
    ): LyricsResult {
        val search = LrclibApi.search(artist, track)
        return search.fold(
            onSuccess = { results ->
                when {
                    results.isEmpty() -> LyricsResult.NotFound
                    results.size == 1 -> {
                        val r = results.first()
                        val lines = r.syncedLyrics ?: r.plainLyrics
                        if (!lines.isNullOrBlank()) {
                            LyricsResult.Single(lines, r.syncedLyrics != null)
                        } else {
                            LyricsResult.List(results)
                        }
                    }
                    else -> LyricsResult.List(results)
                }
            },
            onFailure = { LyricsResult.Error(it.message ?: "Search failed") },
        )
    }
}

// ─────────────────────────────────────────────────────
// Result type used by the repository
// ─────────────────────────────────────────────────────

sealed interface LyricsResult {
    /** Lyrics found — ready to display. */
    data class Single(val lines: String, val isSynced: Boolean) : LyricsResult

    /** Multiple results — user must select from list. */
    data class List(val items: kotlin.collections.List<LrclibResponse>) : LyricsResult

    /** Nothing matched. */
    data object NotFound : LyricsResult

    /** Terminal error. */
    data class Error(val message: String) : LyricsResult
}
