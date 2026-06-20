package com.vibemusic.app.ui.screens

/** Format milliseconds as `m:ss` (or `h:mm:ss` over an hour). */
fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, mins, secs)
    else "%d:%02d".format(mins, secs)
}
