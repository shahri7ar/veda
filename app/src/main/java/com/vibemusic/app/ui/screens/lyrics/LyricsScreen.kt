package com.vibemusic.app.ui.screens.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibemusic.app.data.lyrics.LrclibResponse
import com.vibemusic.app.data.lyrics.LyricsState
import com.vibemusic.app.playback.PlayerHub
import com.vibemusic.app.ui.screens.formatDuration
import com.vibemusic.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Full-screen lyrics overlay with active-line highlighting for synced LRC.
 */
@Composable
fun LyricsScreen(
    viewModel: LyricsViewModel,
    artist: String,
    track: String,
    durationMs: Long,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Auto-fetch on first composition + whenever the track changes.
    LaunchedEffect(artist, track) {
        if (artist.isNotBlank() && track.isNotBlank()) {
            viewModel.fetchLyrics(artist, track, durationMs)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Ink, InkSurface)))
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Top bar ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    viewModel.reset()
                    onDismiss()
                }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, null,
                        tint = TextHigh, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.ifBlank { "Unknown" },
                        color = TextHigh, fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, maxLines = 1)
                    Text(artist.ifBlank { "Unknown Artist" },
                        color = TextMid, fontSize = 13.sp, maxLines = 1)
                }
                Text("Lyrics", color = TextLow, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            Box(Modifier.fillMaxSize()) {
                when (val s = state) {
                    is LyricsState.Idle,
                    is LyricsState.Loading -> LoadingPlaceholder()
                    is LyricsState.Success -> {
                        if (s.isSynced) SyncedLyricsView(s.lines)
                        else PlainLyricsView(s.lines)
                    }
                    is LyricsState.SearchResults -> SearchResultsList(
                        items = s.items,
                        onSelect = { viewModel.onTrackSelected(it.id!!) },
                    )
                    is LyricsState.Error -> ErrorContent(
                        message = s.message,
                        onRetry = { viewModel.fetchLyrics(artist, track, durationMs) },
                    )
                }
            }
        }
    }
}

/* ── Synced (LRC) view — highlights the active line and auto-scrolls ── */

private data class LrcLine(val timeMs: Long, val text: String)

private fun parseLrc(raw: String): List<LrcLine> {
    // Matches [mm:ss.xx] or [mm:ss.xxx]
    val regex = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\](.*)""")
    val out = mutableListOf<LrcLine>()
    raw.lines().forEach { line ->
        regex.findAll(line).forEach { m ->
            val mm = m.groupValues[1].toLong()
            val ss = m.groupValues[2].toLong()
            val frac = m.groupValues[3].let {
                when {
                    it.isEmpty() -> 0L
                    it.length == 2 -> it.toLong() * 10
                    else -> it.toLong()
                }
            }
            val text = m.groupValues[4].trim()
            // Some LRC files include multiple timestamps on one line
            out += LrcLine((mm * 60 + ss) * 1000L + frac, text)
        }
    }
    return out.sortedBy { it.timeMs }
}

@Composable
private fun SyncedLyricsView(raw: String) {
    val lines = remember(raw) { parseLrc(raw) }

    if (lines.isEmpty()) {
        PlainLyricsView(raw)
        return
    }

    // Track current playback position (poll the player every 200ms).
    var positionMs by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            positionMs = PlayerHub.player()?.currentPosition ?: PlayerHub.state.value.positionMs
            delay(200)
        }
    }

    val activeIndex = remember(positionMs, lines) {
        // last line whose timestamp <= positionMs
        var idx = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) idx = i else break
        }
        idx
    }

    val listState = rememberLazyListState()

    // Auto-scroll active line into view
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem(
                index = activeIndex.coerceAtLeast(0),
                scrollOffset = -200,
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 32.dp),
    ) {
        item {
            Text(
                "Synced lyrics", color = Primary,
                fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
        }
        itemsIndexed(lines, key = { i, _ -> i }) { i, line ->
            val isActive = i == activeIndex
            val isPast = i < activeIndex
            val color = when {
                isActive -> TextHigh
                isPast -> TextLow
                else -> TextMid
            }
            val weight = if (isActive) FontWeight.Bold else FontWeight.Medium
            val size = if (isActive) 22.sp else 17.sp
            Text(
                text = line.text.ifBlank { "♪" },
                color = color,
                fontWeight = weight,
                fontSize = size,
                lineHeight = (size.value * 1.4f).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
        }
        item { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
private fun PlainLyricsView(text: String) {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = text,
            color = TextHigh.copy(alpha = 0.9f),
            fontSize = 17.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
    }
}

@Composable
private fun SearchResultsList(
    items: List<LrclibResponse>,
    onSelect: (LrclibResponse) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Multiple matches found. Tap one to load lyrics:",
            color = TextMid, fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(items) { _, item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(InkRaised)
                        .clickable { onSelect(item) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Lyrics, null,
                        tint = Primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.trackName ?: "Unknown",
                            color = TextHigh, fontWeight = FontWeight.Medium,
                            fontSize = 14.sp, maxLines = 1)
                        Text(item.artistName ?: "Unknown",
                            color = TextMid, fontSize = 12.sp, maxLines = 1)
                    }
                    if (item.duration != null) {
                        Text(formatDuration(item.duration * 1000L),
                            color = TextLow, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.ChevronRight, null,
                        tint = TextLow, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.ErrorOutline, null,
            tint = TextLow, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, color = TextMid, fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}
