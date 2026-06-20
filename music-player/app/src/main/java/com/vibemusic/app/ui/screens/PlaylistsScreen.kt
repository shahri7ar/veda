package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibemusic.app.data.model.Playlist
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.MusicViewModel
import com.vibemusic.app.ui.theme.*

/**
 * Playlists screen — unified ViMusic layout.
 */
@Composable
fun PlaylistsScreen(
    vm: MusicViewModel,
    onPlay: () -> Unit,
    onSearch: () -> Unit = {},
    onNavigate: (Int) -> Unit = {},
    currentTab: Int = 2, // Playlists tab
) {
    val playlists by vm.playlists.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val cached by vm.cached.collectAsState()
    var selected by remember { mutableStateOf<Playlist?>(null) }
    var virtualOpen by remember { mutableStateOf<VirtualPlaylist?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    virtualOpen?.let { vp ->
        val tracks = when (vp) {
            VirtualPlaylist.FAVORITES -> favorites
            VirtualPlaylist.CACHED -> cached
        }
        VirtualPlaylistDetailScreen(
            vm = vm,
            title = vp.title,
            tracks = tracks,
            onBack = { virtualOpen = null },
        )
        return
    }

    if (selected != null) {
        PlaylistDetailScreen(
            vm = vm,
            playlist = selected!!,
            onPlay = onPlay,
            onBack = { selected = null },
        )
        return
    }

    ViMusicScaffold(
        title = "Playlists",
        tabs = ROOT_TABS,
        currentTab = currentTab,
        onTabSelect = onNavigate,
        topActions = {
            IconButton(onClick = { showCreate = true }) {
                Icon(Icons.Rounded.Add, null, tint = TextHigh, modifier = Modifier.size(24.dp))
            }
        },
        floatingAction = { ViMusicSearchFab(onSearch) },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // "Built-in" playlists (favorites/cached) at top
            item {
                BuiltinPlaylistCard(
                    title = "Favorites",
                    icon = Icons.Rounded.Favorite,
                    accent = Secondary,
                    subtitle = "${favorites.size} loved tracks",
                ) { virtualOpen = VirtualPlaylist.FAVORITES }
            }
            item {
                BuiltinPlaylistCard(
                    title = "Cached",
                    icon = Icons.Rounded.OfflineBolt,
                    accent = Tertiary,
                    subtitle = "${cached.size} available offline",
                ) { virtualOpen = VirtualPlaylist.CACHED }
            }

            if (playlists.isNotEmpty()) {
                item {
                    Text(
                        "Your playlists", color = TextMid,
                        fontWeight = FontWeight.Medium, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
            }

            items(playlists, key = { it.id }) { pl ->
                PlaylistRow(
                    pl,
                    onClick = { selected = pl },
                    onDelete = { vm.deletePlaylist(pl.id) },
                )
            }

            if (playlists.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.QueueMusic, null,
                                tint = TextLow, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No playlists yet", color = TextLow)
                            Text("Tap + above to create one",
                                color = TextLow, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false; newName = "" },
            containerColor = InkSurface,
            title = { Text("New Playlist", color = TextHigh) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        vm.createPlaylist(newName.trim())
                        newName = ""
                        showCreate = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false; newName = "" }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun BuiltinPlaylistCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(60.dp).clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextHigh, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(subtitle, color = TextMid, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlaylistRow(
    pl: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(InkRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.QueueMusic, null, tint = Primary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(pl.name, color = TextHigh,
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
            Text("${pl.trackIds.size} tracks", color = TextMid, fontSize = 12.sp)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.MoreVert, null, tint = TextLow)
        }
    }
}

/* ───────────────────────── VIRTUAL PLAYLISTS (Favorites / Cached) ─────── */

private enum class VirtualPlaylist(val title: String) {
    FAVORITES("Favorites"),
    CACHED("Cached"),
}

@Composable
private fun VirtualPlaylistDetailScreen(
    vm: MusicViewModel,
    title: String,
    tracks: List<Track>,
    onBack: () -> Unit,
) {
    val tabs = listOf(ViMusicTab("Songs", Icons.Rounded.MusicNote))
    ViMusicScaffold(
        title = title,
        tabs = tabs,
        currentTab = 0,
        onTabSelect = {},
        onBack = onBack,
        actionChip = {
            ViMusicPillChip("Enqueue") {
                if (tracks.isNotEmpty()) vm.play(tracks.first(), tracks)
            }
        },
        floatingAction = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = InkRaised,
                shadowElevation = 4.dp,
                modifier = Modifier.size(60.dp).clickable {
                    if (tracks.isNotEmpty()) vm.playShuffled(tracks)
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Shuffle, null,
                        tint = TextHigh, modifier = Modifier.size(26.dp))
                }
            }
        },
    ) {
        if (tracks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Empty", color = TextLow)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
            ) {
                itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { vm.play(track, tracks) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            color = TextLow, fontSize = 14.sp,
                            modifier = Modifier.width(32.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(track.title, color = TextHigh,
                                fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                            Text(track.artist, color = TextMid, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/* ───────────────────────── PLAYLIST DETAIL ───────────────────────── */

@Composable
private fun PlaylistDetailScreen(
    vm: MusicViewModel,
    playlist: Playlist,
    onPlay: () -> Unit,
    onBack: () -> Unit,
) {
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var currentSubTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(playlist.id) {
        tracks = vm.getPlaylistTracks(playlist.id)
    }

    val playlistTabs = listOf(
        ViMusicTab("Songs", Icons.Rounded.MusicNote),
    )

    ViMusicScaffold(
        title = playlist.name,
        tabs = playlistTabs,
        currentTab = currentSubTab,
        onTabSelect = { currentSubTab = it },
        onBack = onBack,
        actionChip = {
            ViMusicPillChip("Enqueue") {
                if (tracks.isNotEmpty()) {
                    vm.play(tracks.first(), tracks); onPlay()
                }
            }
        },
        topActions = {
            IconButton(onClick = { vm.deletePlaylist(playlist.id); onBack() }) {
                Icon(Icons.Rounded.Delete, null,
                    tint = MaterialTheme.colorScheme.error)
            }
        },
        floatingAction = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = InkRaised,
                shadowElevation = 4.dp,
                modifier = Modifier.size(60.dp).clickable {
                    if (tracks.isNotEmpty()) {
                        vm.playShuffled(tracks); onPlay()
                    }
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Shuffle, null,
                        tint = TextHigh, modifier = Modifier.size(26.dp))
                }
            }
        },
    ) {
        if (tracks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Empty playlist", color = TextLow)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
            ) {
                itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { vm.play(track, tracks); onPlay() }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            color = TextLow,
                            fontSize = 16.sp,
                            modifier = Modifier.width(40.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(track.title, color = TextHigh,
                                fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
                            Text(track.artist, color = TextMid, fontSize = 13.sp, maxLines = 1)
                        }
                        IconButton(onClick = { vm.removeFromPlaylist(playlist.id, track.id) }) {
                            Icon(Icons.Rounded.RemoveCircleOutline, null,
                                tint = TextLow, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
