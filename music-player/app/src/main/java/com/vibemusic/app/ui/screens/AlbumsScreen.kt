package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vibemusic.app.data.model.Album
import com.vibemusic.app.ui.MusicViewModel
import com.vibemusic.app.ui.theme.*

/**
 * Albums screen — unified ViMusic layout.
 *
 * - Big "Albums" title top-right
 * - Sort/filter icons in top action row
 * - Left labels: Quick picks · Songs · Playlists · Artists · Albums
 * - Search FAB bottom-right
 */
@Composable
fun AlbumsScreen(
    vm: MusicViewModel,
    onPlay: () -> Unit,
    onSearch: () -> Unit = {},
    onNavigate: (Int) -> Unit = {},
    currentTab: Int = 4, // Albums tab
) {
    val albums by vm.albums.collectAsState()
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var sortMode by remember { mutableStateOf(AlbumSort.TITLE) }

    if (selectedAlbum != null) {
        AlbumDetailScreen(
            vm = vm,
            album = selectedAlbum!!,
            onPlay = onPlay,
            onBack = { selectedAlbum = null },
        )
        return
    }

    val sortedAlbums = remember(albums, sortMode) {
        when (sortMode) {
            AlbumSort.TITLE -> albums.sortedBy { it.title.lowercase() }
            AlbumSort.ARTIST -> albums.sortedBy { it.artist.lowercase() }
            AlbumSort.YEAR -> albums.sortedByDescending { it.year }
            AlbumSort.RECENT -> albums  // already in order
        }
    }

    ViMusicScaffold(
        title = "Albums",
        tabs = ROOT_TABS,
        currentTab = currentTab,
        onTabSelect = onNavigate,
        topActions = {
            // Sort icons (matches ViMusic: calendar, alphabetical, clock, arrow-up)
            IconButton(onClick = { sortMode = AlbumSort.YEAR }) {
                Icon(Icons.Rounded.CalendarToday, null,
                    tint = if (sortMode == AlbumSort.YEAR) TextHigh else TextLow,
                    modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { sortMode = AlbumSort.TITLE }) {
                Icon(Icons.Rounded.SortByAlpha, null,
                    tint = if (sortMode == AlbumSort.TITLE) TextHigh else TextLow,
                    modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { sortMode = AlbumSort.RECENT }) {
                Icon(Icons.Rounded.AccessTime, null,
                    tint = if (sortMode == AlbumSort.RECENT) TextHigh else TextLow,
                    modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { /* toggle direction */ }) {
                Icon(Icons.Rounded.ArrowUpward, null,
                    tint = TextLow, modifier = Modifier.size(20.dp))
            }
        },
        floatingAction = { ViMusicSearchFab(onSearch) },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(sortedAlbums, key = { it.id }) { album ->
                AlbumListRow(album) { selectedAlbum = album }
            }
            if (sortedAlbums.isEmpty()) {
                item {
                    Box(
                        Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No albums", color = TextLow)
                    }
                }
            }
        }
    }
}

private enum class AlbumSort { TITLE, ARTIST, YEAR, RECENT }

@Composable
private fun AlbumListRow(album: Album, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Album art
        Box(
            Modifier.size(88.dp).clip(RoundedCornerShape(8.dp)).background(InkRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (!album.artUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(album.artUri).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Rounded.Album, null, tint = Primary, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                album.title, color = TextHigh,
                fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1,
            )
            Text(
                album.artist, color = TextMid,
                fontSize = 14.sp, maxLines = 1,
            )
            if (album.year > 0) {
                Text(
                    album.year.toString(), color = TextLow,
                    fontSize = 13.sp, maxLines = 1,
                )
            }
        }
    }
}

/* ───────────────────────── ALBUM DETAIL ───────────────────────── */

@Composable
private fun AlbumDetailScreen(
    vm: MusicViewModel,
    album: Album,
    onPlay: () -> Unit,
    onBack: () -> Unit,
) {
    val tracks by vm.albumTracks(album.id).collectAsState()
    var currentSubTab by remember { mutableIntStateOf(0) }

    // Album detail labels (matches ViMusic: Songs · Other versions)
    val albumTabs = listOf(
        ViMusicTab("Songs", Icons.Rounded.MusicNote),
        ViMusicTab("Other versions", Icons.Rounded.Album),
    )

    ViMusicScaffold(
        title = album.title,
        tabs = albumTabs,
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
            IconButton(onClick = { /* bookmark */ }) {
                Icon(Icons.Rounded.BookmarkBorder, null, tint = TextMid)
            }
            IconButton(onClick = { /* share */ }) {
                Icon(Icons.Rounded.Share, null, tint = TextMid)
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
        when (currentSubTab) {
            0 -> AlbumSongsList(vm, album, tracks, onPlay)
            1 -> AlbumOtherVersions(album)
        }
    }
}

@Composable
private fun AlbumSongsList(
    vm: MusicViewModel,
    album: Album,
    tracks: List<com.vibemusic.app.data.model.Track>,
    onPlay: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
    ) {
        // Big square album art at top
        item {
            val ctx = LocalContext.current
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).padding(end = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(InkRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!album.artUri.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(ctx).data(album.artUri).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(Icons.Rounded.Album, null, tint = Primary,
                            modifier = Modifier.size(120.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Numbered track list
        itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
            AlbumTrackRow(
                index = index + 1,
                track = track,
                onClick = { vm.play(track, tracks); onPlay() },
            )
        }
    }
}

@Composable
private fun AlbumOtherVersions(album: Album) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("No other versions", color = TextLow)
    }
}

@Composable
private fun AlbumTrackRow(
    index: Int,
    track: com.vibemusic.app.data.model.Track,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Track number
        Text(
            text = index.toString(),
            color = TextLow,
            fontSize = 16.sp,
            modifier = Modifier.width(40.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(track.title, color = TextHigh,
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
            Text(track.artist, color = TextMid, fontSize = 13.sp, maxLines = 1)
        }
        if (track.durationMs > 0) {
            Text(
                formatDuration(track.durationMs),
                color = TextLow, fontSize = 13.sp,
            )
        }
    }
}

// formatDuration is provided by FormatUtils.kt in the same package.
