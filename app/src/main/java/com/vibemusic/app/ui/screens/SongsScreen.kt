package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.MusicViewModel
import com.vibemusic.app.ui.theme.*

/**
 * Songs screen — unified ViMusic layout.
 *
 * - Big "Songs" title top-right
 * - Left labels: Quick picks · Songs · Playlists · Artists · Albums
 * - Sort icons in top-right action area
 * - Search FAB bottom-right
 */
@Composable
fun SongsScreen(
    vm: MusicViewModel,
    onSearch: () -> Unit = {},
    onNavigate: (Int) -> Unit = {},
) {
    val tracks by vm.allTracks.collectAsState()
    var sortMode by remember { mutableStateOf(SongSort.TITLE) }
    val sorted = remember(tracks, sortMode) {
        when (sortMode) {
            SongSort.TITLE -> tracks.sortedBy { it.title.lowercase() }
            SongSort.ARTIST -> tracks.sortedBy { it.artist.lowercase() }
            SongSort.ADDED -> tracks // already by addedAt DESC from DAO ordering
            SongSort.DURATION -> tracks.sortedBy { it.durationMs }
        }
    }

    ViMusicScaffold(
        title = "Songs",
        tabs = ROOT_TABS,
        currentTab = 1,
        onTabSelect = onNavigate,
        topActions = {
            IconButton(onClick = { sortMode = SongSort.TITLE }) {
                Icon(Icons.Rounded.SortByAlpha, null,
                    tint = if (sortMode == SongSort.TITLE) TextHigh else TextLow,
                    modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { sortMode = SongSort.ARTIST }) {
                Icon(Icons.Rounded.Person, null,
                    tint = if (sortMode == SongSort.ARTIST) TextHigh else TextLow,
                    modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { sortMode = SongSort.ADDED }) {
                Icon(Icons.Rounded.AccessTime, null,
                    tint = if (sortMode == SongSort.ADDED) TextHigh else TextLow,
                    modifier = Modifier.size(20.dp))
            }
        },
        floatingAction = { ViMusicSearchFab(onSearch) },
    ) {
        if (sorted.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No songs", color = TextLow)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
            ) {
                items(sorted, key = { it.id }) { track ->
                    SongRowSimple(
                        track = track,
                        onPlay = { vm.play(track, sorted) },
                        onToggleFav = { vm.toggleFavorite(track.id) },
                    )
                }
            }
        }
    }
}

private enum class SongSort { TITLE, ARTIST, ADDED, DURATION }

@Composable
private fun SongRowSimple(
    track: Track,
    onPlay: () -> Unit,
    onToggleFav: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onPlay)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(InkRaised),
            contentAlignment = Alignment.Center,
        ) {
            val ctx = LocalContext.current
            if (!track.albumArtUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(track.albumArtUri).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Rounded.MusicNote, null, tint = Primary, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = TextHigh,
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
            Text("${track.artist} · ${track.album}",
                color = TextMid, fontSize = 12.sp, maxLines = 1)
        }
        IconButton(onClick = onToggleFav, modifier = Modifier.size(34.dp)) {
            Icon(
                if (track.isFavorite) Icons.Rounded.Favorite
                else Icons.Rounded.FavoriteBorder,
                null,
                tint = if (track.isFavorite) Secondary else TextLow,
                modifier = Modifier.size(18.dp),
            )
        }
        if (track.durationMs > 0) {
            Text(formatSongsDuration(track.durationMs),
                color = TextLow, fontSize = 11.sp)
        }
    }
}

private fun formatSongsDuration(ms: Long): String {
    val s = ms / 1000; val m = s / 60; val r = s % 60
    return "%d:%02d".format(m, r)
}
