package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.vibemusic.app.data.model.Album
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.MusicViewModel
import com.vibemusic.app.ui.theme.*

/**
 * Quick picks (home) — unified ViMusic layout.
 *
 * - Big "Quick picks" title top-right
 * - Left labels: Quick picks · Songs · Playlists · Artists · Albums
 * - Vertical scrolling sections: top tracks, related albums
 * - Floating search FAB bottom-right
 */
@Composable
fun QuickPicksScreen(
    vm: MusicViewModel,
    onSearch: () -> Unit = {},
    onNavigate: (Int) -> Unit = {},
) {
    val recent by vm.recent.collectAsState()
    val albums by vm.albums.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val all by vm.allTracks.collectAsState()

    ViMusicScaffold(
        title = "Quick picks",
        tabs = ROOT_TABS,
        currentTab = 0,
        onTabSelect = onNavigate,
        topActions = {
            IconButton(onClick = { /* equalizer */ }) {
                Icon(Icons.Rounded.Tune, null, tint = TextLow, modifier = Modifier.size(20.dp))
            }
        },
        floatingAction = { ViMusicSearchFab(onSearch) },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Featured tracks (recent or top)
            if (recent.isNotEmpty()) {
                items(recent.take(4), key = { it.id }) { track ->
                    QuickPickTrackRow(track) { vm.play(track, recent) }
                }
            }

            // Related albums
            if (albums.isNotEmpty()) {
                item {
                    Text(
                        "Related albums", color = TextHigh,
                        fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(albums.take(10)) { album ->
                            AlbumPreviewCard(album)
                        }
                    }
                }
            }

            // Favorites
            if (favorites.isNotEmpty()) {
                item {
                    Text(
                        "Favorites", color = TextHigh,
                        fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    )
                }
                items(favorites.take(6), key = { it.id }) { track ->
                    QuickPickTrackRow(track) { vm.play(track, favorites) }
                }
            }

            // Empty state
            if (all.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.LibraryMusic, null,
                                tint = TextLow, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No music yet", color = TextLow)
                            Text("Add a local folder or network mount",
                                color = TextLow, fontSize = 12.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { vm.rescanLocal() }) {
                                Text("Scan device")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPickTrackRow(track: Track, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)).background(InkRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (!track.albumArtUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(track.albumArtUri).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Rounded.MusicNote, null, tint = Primary, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = TextHigh,
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
            Text(track.artist, color = TextMid, fontSize = 13.sp, maxLines = 1)
        }
        if (track.isFavorite) {
            Icon(Icons.Rounded.Star, null,
                tint = Secondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        if (track.durationMs > 0) {
            Text(
                formatQuickPicksDuration(track.durationMs),
                color = TextLow, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun AlbumPreviewCard(album: Album) {
    val ctx = LocalContext.current
    Column(Modifier.width(140.dp)) {
        Box(
            Modifier.size(140.dp).clip(RoundedCornerShape(12.dp)).background(InkRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (!album.artUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(album.artUri).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Rounded.Album, null, tint = Primary, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(album.title, color = TextHigh,
            fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
        Text(album.artist, color = TextMid, fontSize = 11.sp, maxLines = 1)
    }
}

private fun formatQuickPicksDuration(ms: Long): String {
    val s = ms / 1000; val m = s / 60; val r = s % 60
    return "%d:%02d".format(m, r)
}
