package com.vibemusic.app.ui.screens.sleep

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vibemusic.app.playback.PlayerHub
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Musicolet-style Sleep Timer: two modes —
 *  1. Duration-based: stop after hh:mm
 *  2. Song-count-based: stop after N songs
 */
class SleepTimerManager(private val context: Context) {

    /** How many songs remain (for song-count mode). -1 = disabled. */
    private val _remainingSongs = MutableStateFlow(-1)
    val remainingSongs: StateFlow<Int> = _remainingSongs.asStateFlow()

    /** How many millis remain (for duration mode). -1 = disabled. */
    private val _remainingMs = MutableStateFlow(-1L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private var songCountJob: Job? = null
    private var durationJob: Job? = null

    val isActive: Boolean
        get() = _remainingSongs.value > 0 || _remainingMs.value > 0

    // ─── Song-count mode ──────────────────────────

    /** Stop after [count] more songs finish. */
    fun setSongCount(count: Int) {
        cancel()
        if (count <= 0) return
        _remainingSongs.value = count
        songCountJob = CoroutineScope(Dispatchers.Default).launch {
            // Listen for track transitions
            var lastTrack = ""
            while (isActive) {
                delay(1_000)
                val current = PlayerHub.state.value.title
                if (current.isNotBlank() && current != lastTrack) {
                    lastTrack = current
                    val rem = _remainingSongs.value - 1
                    if (rem <= 0) {
                        stopPlayback()
                        cancel()
                        return@launch
                    }
                    _remainingSongs.value = rem
                }
            }
        }
    }

    // ─── Duration mode ────────────────────────────

    /** Stop after [millis] milliseconds. */
    fun setDuration(millis: Long) {
        cancel()
        if (millis <= 0) return
        _remainingMs.value = millis
        val startAt = System.currentTimeMillis()
        durationJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startAt
                val rem = millis - elapsed
                if (rem <= 0) {
                    _remainingMs.value = 0
                    stopPlayback()
                    cancel()
                    return@launch
                }
                _remainingMs.value = rem
                delay(1_000)
            }
        }
    }

    // ─── Control ──────────────────────────────────

    fun cancel() {
        _remainingSongs.value = -1
        _remainingMs.value = -1L
        songCountJob?.cancel()
        durationJob?.cancel()
        songCountJob = null
        durationJob = null
    }

    private fun stopPlayback() {
        PlayerHub.player()?.pause()
    }

    fun formatRemaining(): String {
        val ms = _remainingMs.value
        val songs = _remainingSongs.value
        return when {
            songs > 0 -> "$songs song${if (songs > 1) "s" else ""} left"
            ms > 0 -> {
                val totalSec = ms / 1000
                val hours = totalSec / 3600
                val mins = (totalSec % 3600) / 60
                val secs = totalSec % 60
                if (hours > 0) "${hours}h ${mins}m ${secs}s"
                else "${mins}m ${secs}s"
            }
            else -> "Not set"
        }
    }
}
