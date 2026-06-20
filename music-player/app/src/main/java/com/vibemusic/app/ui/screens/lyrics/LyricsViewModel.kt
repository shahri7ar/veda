package com.vibemusic.app.ui.screens.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibemusic.app.data.lyrics.LrclibResponse
import com.vibemusic.app.data.lyrics.LyricsRepository
import com.vibemusic.app.data.lyrics.LyricsResult
import com.vibemusic.app.data.lyrics.LyricsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LyricsViewModel(
    private val repository: LyricsRepository = LyricsRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val state: StateFlow<LyricsState> = _state.asStateFlow()

    // Cache the search results so the user can pick one later.
    private var cachedSearchResults: List<LrclibResponse> = emptyList()

    // ─────────────────────────────────────────────────────
    //  Main entry point — called when a song starts
    // ─────────────────────────────────────────────────────

    fun fetchLyrics(
        artist: String,
        track: String,
        durationMs: Long,
    ) {
        if (artist.isBlank() || track.isBlank()) return

        val durationSeconds = durationMs / 1000L
        _state.value = LyricsState.Loading

        viewModelScope.launch {
            when (val result = repository.fetchLyrics(artist, track, durationSeconds)) {
                is LyricsResult.Single -> {
                    _state.value = LyricsState.Success(
                        lines = result.lines,
                        isSynced = result.isSynced,
                    )
                }
                is LyricsResult.List -> {
                    cachedSearchResults = result.items
                    _state.value = LyricsState.SearchResults(result.items)
                }
                is LyricsResult.NotFound -> {
                    _state.value = LyricsState.Error("No lyrics found")
                }
                is LyricsResult.Error -> {
                    _state.value = LyricsState.Error(result.message)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────
    //  Step 4 — User selected a track from search results
    // ─────────────────────────────────────────────────────

    fun onTrackSelected(trackId: Long) {
        _state.value = LyricsState.Loading

        viewModelScope.launch {
            when (val result = repository.fetchById(trackId)) {
                is LyricsResult.Single -> {
                    _state.value = LyricsState.Success(
                        lines = result.lines,
                        isSynced = result.isSynced,
                    )
                }
                is LyricsResult.List,
                is LyricsResult.NotFound -> {
                    _state.value = LyricsState.Error("Could not fetch lyrics for this track")
                }
                is LyricsResult.Error -> {
                    _state.value = LyricsState.Error(result.message)
                }
            }
        }
    }

    /** Reset to idle (e.g. when user dismisses the lyrics screen). */
    fun reset() {
        _state.value = LyricsState.Idle
        cachedSearchResults = emptyList()
    }
}
