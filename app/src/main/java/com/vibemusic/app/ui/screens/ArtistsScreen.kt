package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.vibemusic.app.data.model.Artist
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.MusicViewModel
import com.vibemusic.app.ui.theme.*

/**
 * Artists screen — unified ViMusic layout.
 * Big "Artists" title top-right, left labels, scrollable artist list.
 */
@Composable
fun ArtistsScreen(
    vm: MusicViewModel,
    onPlay: () -> Unit,
    onSearch: () -> Unit = {},
    onNavigate: (Int) -> Unit = {},
    currentTab: Int = 3, // 0=QuickPicks 1=Songs 2=Playlists 3=Artists 4=Albums
) {
    val artists by vm.artists.collectAsState()
    var selectedArtist by remember { mutableStateOf<String?>(null) }

    if (selectedArtist != null) {
        ArtistDetailScreen(
            vm = vm,
            name = selectedArtist!!,
            onPlay = onPlay,
            onBack = { selectedArtist = null },
        )
        return
    }

    ViMusicScaffold(
        title = "Artists",
        tabs = ROOT_TABS,
        currentTab = currentTab,
        onTabSelect = onNavigate,
        floatingAction = { ViMusicSearchFab(onSearch) },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(artists, key = { it.name }) { artist ->
                ArtistListRow(artist) { selectedArtist = artist.name }
            }
            if (artists.isEmpty()) {
                item {
                    Box(
                        Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No artists", color = TextLow)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistListRow(artist: Artist, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(InkRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Person, null, tint = Secondary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                artist.name, color = TextHigh,
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1,
            )
            Text(
                "${artist.trackCount} songs · ${artist.albumCount} albums",
                color = TextMid, fontSize = 12.sp,
            )
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = TextLow)
    }
}

/* ───────────────────────── ARTIST DETAIL ───────────────────────── */

@Composable
private fun ArtistDetailScreen(
    vm: MusicViewModel,
    name: String,
    onPlay: () -> Unit,
    onBack: () -> Unit,
) {
    val tracks by vm.artistTracks(name).collectAsState()
    var currentSubTab by remember { mutableIntStateOf(0) }

    // Sub-tabs unique to artist detail page (matches ViMusic exactly)
    val artistTabs = listOf(
        ViMusicTab("Overview", Icons.Rounded.AutoAwesome),
        ViMusicTab("Songs", Icons.Rounded.MusicNote),
        ViMusicTab("Albums", Icons.Rounded.Album),
        ViMusicTab("Singles", Icons.Rounded.Audiotrack),
        ViMusicTab("Library", Icons.Rounded.LibraryMusic),
    )

    val albums = remember(tracks) {
        tracks.groupBy { it.album }
            .map { (album, songs) -> album to songs }
            .filter { it.first != "Unknown Album" }
    }

    ViMusicScaffold(
        title = name,
        tabs = artistTabs,
        currentTab = currentSubTab,
        onTabSelect = { currentSubTab = it },
        onBack = onBack,
        actionChip = {
            ViMusicPillChip("Shuffle") {
                if (tracks.isNotEmpty()) {
                    vm.playShuffled(tracks); onPlay()
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
    ) {
        when (currentSubTab) {
            0 -> ArtistOverview(vm, tracks, albums, onPlay)
            1 -> ArtistSongs(vm, tracks, onPlay)
            2 -> ArtistAlbums(vm, albums, onPlay)
            3 -> ArtistSingles(vm, tracks, onPlay)
            4 -> ArtistLibrary(vm, tracks, onPlay)
        }
    }
}

@Composable
private fun ArtistOverview(
    vm: MusicViewModel,
    tracks: List<Track>,
    albums: List<Pair<String, List<Track>>>,
    onPlay: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            // Big circular artist photo placeholder
            Box(
                Modifier.fillMaxWidth().height(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(180.dp).clip(CircleShape).background(InkRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Person, null,
                        tint = Secondary, modifier = Modifier.size(80.dp)
                    )
                }
            }
        }

        // Songs section
        if (tracks.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                ) {
                    Text(
                        "Songs", color = TextHigh,
                        fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "View all", color = TextMid, fontSize = 13.sp,
                        modifier = Modifier.clickable { /* navigate to Songs sub-tab */ },
                    )
                }
            }
            items(tracks.take(4), key = { it.id }) { track ->
                ArtistTrackRow(track) { vm.play(track, tracks); onPlay() }
            }
        }

        // Albums section
        if (albums.isNotEmpty()) {
            item {
                Text(
                    "Albums", color = TextHigh,
                    fontWeight = FontWeight.Bold, fontSize = 22.sp,
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(albums.take(10)) { (albumName, songs) ->
                        ArtistAlbumCard(albumName, songs) { vm.play(songs.first(), songs); onPlay() }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistSongs(vm: MusicViewModel, tracks: List<Track>, onPlay: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
    ) {
        items(tracks, key = { it.id }) { track ->
            ArtistTrackRow(track) { vm.play(track, tracks); onPlay() }
        }
    }
}

@Composable
private fun ArtistAlbums(
    vm: MusicViewModel,
    albums: List<Pair<String, List<Track>>>,
    onPlay: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(albums) { (albumName, songs) ->
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { vm.play(songs.first(), songs); onPlay() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(InkRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Album, null, tint = Primary)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(albumName, color = TextHigh, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("${songs.size} tracks", color = TextMid, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ArtistSingles(vm: MusicViewModel, tracks: List<Track>, onPlay: () -> Unit) {
    val singles = tracks.filter { it.album == "Unknown Album" || it.album.isBlank() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
    ) {
        if (singles.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No singles", color = TextLow)
                }
            }
        } else {
            items(singles, key = { it.id }) { track ->
                ArtistTrackRow(track) { vm.play(track, singles); onPlay() }
            }
        }
    }
}

@Composable
private fun ArtistLibrary(vm: MusicViewModel, tracks: List<Track>, onPlay: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
    ) {
        items(tracks.filter { it.isFavorite }, key = { it.id }) { track ->
            ArtistTrackRow(track) { vm.play(track, tracks); onPlay() }
        }
    }
}

@Composable
private fun ArtistTrackRow(track: Track, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(InkRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MusicNote, null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = TextHigh, fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp, maxLines = 1)
            Text(track.artist, color = TextMid, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ArtistAlbumCard(name: String, tracks: List<Track>, onClick: () -> Unit) {
    Column(
        Modifier.width(150.dp).clickable(onClick = onClick),
    ) {
        Box(
            Modifier.size(150.dp).clip(RoundedCornerShape(14.dp)).background(InkRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Album, null, tint = Primary, modifier = Modifier.size(50.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(name, color = TextHigh, fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp, maxLines = 1)
        Text("${tracks.size} tracks", color = TextMid, fontSize = 11.sp, maxLines = 1)
    }
}

/** Shared tabs across root screens (matches ViMusic exactly). */
val ROOT_TABS = listOf(
    ViMusicTab("Quick picks", Icons.Rounded.AutoAwesome),
    ViMusicTab("Songs", Icons.Rounded.MusicNote),
    ViMusicTab("Playlists", Icons.Rounded.QueueMusic),
    ViMusicTab("Artists", Icons.Rounded.Person),
    ViMusicTab("Albums", Icons.Rounded.Album),
)
