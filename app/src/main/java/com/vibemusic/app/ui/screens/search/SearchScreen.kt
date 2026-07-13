package com.vibemusic.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vibemusic.app.data.model.Album
import com.vibemusic.app.data.model.Artist
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.MusicViewModel
import com.vibemusic.app.ui.screens.formatDuration
import com.vibemusic.app.ui.theme.*

/**
 * Global search overlay — searches across tracks, albums, and artists.
 * ViMusic-style: dark, immersive, with grouped result sections.
 */
@Composable
fun SearchScreen(
    vm: MusicViewModel,
    onDismiss: () -> Unit,
) {
    val query by vm.searchQuery.collectAsState()
    val results by vm.searchResults.collectAsState()
    val all by vm.allTracks.collectAsState()
    val focus = remember { FocusRequester() }
    val ctx = LocalContext.current

    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        Modifier.fillMaxSize().background(Ink).systemBarsPadding()
    ) {
        // Top: close + input
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                vm.setSearchQuery("")
                onDismiss()
            }) {
                Icon(Icons.Rounded.ArrowBack, null, tint = TextHigh)
            }

            OutlinedTextField(
                value = query,
                onValueChange = { vm.setSearchQuery(it) },
                placeholder = { Text("Search songs, albums, artists…", color = TextLow) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TextMid) },
                trailingIcon = if (query.isNotEmpty()) {
                    @Composable {
                        IconButton(onClick = { vm.setSearchQuery("") }) {
                            Icon(Icons.Rounded.Close, null, tint = TextMid)
                        }
                    }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = OutlineColor,
                    focusedTextColor = TextHigh,
                    unfocusedTextColor = TextHigh,
                    cursorColor = Primary,
                ),
            )
        }

        when {
            query.isBlank() -> EmptyHint(all.size)
            results.isEmpty -> NoResults(query)
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (results.artists.isNotEmpty()) {
                        item { ResultHeader("Artists", results.artists.size) }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(results.artists) { a ->
                                    ArtistChip(a)
                                }
                            }
                        }
                    }

                    if (results.albums.isNotEmpty()) {
                        item { ResultHeader("Albums", results.albums.size) }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(results.albums) { a ->
                                    AlbumChip(a)
                                }
                            }
                        }
                    }

                    if (results.tracks.isNotEmpty()) {
                        item { ResultHeader("Songs", results.tracks.size) }
                        items(results.tracks, key = { it.id }) { t ->
                            TrackRow(t) {
                                vm.play(t, results.tracks)
                                onDismiss()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultHeader(label: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextHigh,
            fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text("$count", color = TextLow, fontSize = 13.sp)
    }
}

@Composable
private fun TrackRow(t: Track, onPlay: () -> Unit) {
    val ctx = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(InkRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (!t.albumArtUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(t.albumArtUri).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Rounded.MusicNote, null, tint = Primary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, color = TextHigh,
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
            Text("${t.artist} · ${t.album}",
                color = TextMid, fontSize = 12.sp, maxLines = 1)
        }
        Text(formatDuration(t.durationMs), color = TextLow, fontSize = 12.sp)
    }
}

@Composable
private fun AlbumChip(a: Album) {
    val ctx = LocalContext.current
    Column(Modifier.width(130.dp)) {
        Box(
            Modifier.size(130.dp).clip(RoundedCornerShape(12.dp)).background(InkRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (!a.artUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(a.artUri).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Rounded.Album, null, tint = Primary, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(a.title, color = TextHigh, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(a.artist, color = TextMid, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun ArtistChip(a: Artist) {
    Column(
        Modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(80.dp).clip(CircleShape).background(InkRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Person, null,
                tint = Secondary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(a.name, color = TextHigh, fontSize = 12.sp,
            fontWeight = FontWeight.Medium, maxLines = 1)
        Text("${a.trackCount} songs", color = TextMid, fontSize = 10.sp)
    }
}

@Composable
private fun EmptyHint(libraryCount: Int) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.Search, null,
            tint = TextLow, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text("Search your library", color = TextMid,
            fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            if (libraryCount > 0)
                "$libraryCount tracks indexed — type to filter"
            else
                "Your library is empty. Scan your device first.",
            color = TextLow, fontSize = 12.sp,
        )
    }
}

@Composable
private fun NoResults(query: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.SearchOff, null,
            tint = TextLow, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text("No matches for “$query”",
            color = TextMid, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}
