package com.vibemusic.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.vibemusic.app.playback.PlaybackSnapshot
import com.vibemusic.app.ui.MusicViewModel
import com.vibemusic.app.ui.screens.equalizer.EqualizerScreen
import com.vibemusic.app.ui.screens.folders.FoldersScreen
import com.vibemusic.app.ui.screens.lyrics.LyricsScreen
import com.vibemusic.app.ui.screens.lyrics.LyricsViewModel
import com.vibemusic.app.ui.screens.search.SearchScreen
import com.vibemusic.app.ui.screens.sleep.BookmarksScreen
import com.vibemusic.app.ui.screens.sleep.SleepTimerSheet
import com.vibemusic.app.data.backup.BackupRestoreScreen
import com.vibemusic.app.ui.theme.*

/**
 * Root screen — ViMusic-style unified navigation.
 *
 * Five primary screens reachable via left labels:
 *   0 Quick picks  1 Songs  2 Playlists  3 Artists  4 Albums
 *
 * Extras (Folders, Mounts, Sleep timer, Bookmarks, Equalizer, Backup, Settings)
 * are reachable via the top-left hamburger menu.
 */
@Composable
fun RootScreen(vm: MusicViewModel) {
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    val playback by vm.playback.collectAsState()
    val lyricsVm = remember { LyricsViewModel() }

    Box(Modifier.fillMaxSize().background(Ink)) {

        // Main content — switch between root screens
        when (currentTab) {
            0 -> QuickPicksScreen(vm,
                onSearch = { overlay = Overlay.SEARCH },
                onNavigate = { currentTab = it },
            )
            1 -> SongsScreen(vm,
                onSearch = { overlay = Overlay.SEARCH },
                onNavigate = { currentTab = it },
            )
            2 -> PlaylistsScreen(vm,
                onPlay = {},
                onSearch = { overlay = Overlay.SEARCH },
                onNavigate = { currentTab = it },
            )
            3 -> ArtistsScreen(vm,
                onPlay = {},
                onSearch = { overlay = Overlay.SEARCH },
                onNavigate = { currentTab = it },
            )
            4 -> AlbumsScreen(vm,
                onPlay = {},
                onSearch = { overlay = Overlay.SEARCH },
                onNavigate = { currentTab = it },
            )
        }

        // Mini player (only when something is playing)
        AnimatedVisibility(
            visible = playback.title.isNotBlank(),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ViMusicMiniPlayer(
                playback = playback,
                onTogglePlay = vm::togglePP,
                onNext = vm::next,
                onClick = { overlay = Overlay.NOW_PLAYING },
            )
        }

        // Floating "extras" menu button at top-left
        IconButton(
            onClick = { overlay = Overlay.EXTRAS },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 24.dp),
        ) {
            Icon(Icons.Rounded.Menu, null, tint = TextHigh, modifier = Modifier.size(24.dp))
        }
    }

    // Overlays
    when (overlay) {
        Overlay.NOW_PLAYING -> NowPlayingSheet(
            playback = playback,
            shuffleEnabled = playback.shuffleEnabled,
            repeatMode = playback.repeatMode,
            onDismiss = { overlay = null },
            onTogglePlay = vm::togglePP,
            onNext = vm::next,
            onPrev = vm::prev,
            onSeek = vm::seek,
            onToggleShuffle = vm::toggleShuffle,
            onToggleRepeat = vm::toggleRepeat,
            onOpenLyrics = { overlay = Overlay.LYRICS },
            onOpenQueue = { /* future: queue viewer */ },
        )
        Overlay.EXTRAS -> ExtrasMenu(
            onDismiss = { overlay = null },
            onMounts = { overlay = Overlay.MOUNTS },
            onFolders = { overlay = Overlay.FOLDERS },
            onSettings = { overlay = Overlay.SETTINGS },
            onSleep = { overlay = Overlay.SLEEP },
            onBookmarks = { overlay = Overlay.BOOKMARKS },
            onEqualizer = { overlay = Overlay.EQUALIZER },
            onBackup = { overlay = Overlay.BACKUP },
        )
        Overlay.MOUNTS -> MountsScreen(vm, onPlay = {}, onBack = { overlay = null })
        Overlay.FOLDERS -> FoldersScreen(onBack = { overlay = null })
        Overlay.SETTINGS -> SettingsOverlay(vm, onDismiss = { overlay = null })
        Overlay.SEARCH -> SearchScreen(
            vm = vm,
            onDismiss = { overlay = null },
        )
        Overlay.SLEEP -> SleepTimerSheet(
            timer = vm.sleepTimer,
            onDismiss = { overlay = null },
        )
        Overlay.BOOKMARKS -> BookmarksScreen(vm, onDismiss = { overlay = null })
        Overlay.EQUALIZER -> EqualizerScreen(vm, onDismiss = { overlay = null })
        Overlay.BACKUP -> BackupRestoreScreen(onDismiss = { overlay = null })
        Overlay.LYRICS -> LyricsScreen(
            viewModel = lyricsVm,
            artist = playback.artist,
            track = playback.title,
            durationMs = playback.durationMs,
            onDismiss = { overlay = Overlay.NOW_PLAYING },
        )
        null -> Unit
    }
}

private enum class Overlay {
    NOW_PLAYING, EXTRAS, MOUNTS, FOLDERS, SETTINGS, SEARCH,
    SLEEP, BOOKMARKS, EQUALIZER, BACKUP, LYRICS,
}

/* ─────────────── Mini player bar ─────────────── */

@Composable
private fun ViMusicMiniPlayer(
    playback: PlaybackSnapshot,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        color = InkSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column {
            // Tiny progress bar at the very top of the mini-player
            if (playback.durationMs > 0) {
                val pct = (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { pct },
                    color = Primary,
                    trackColor = InkRaised,
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniArt(playback.artUri)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(playback.title, color = TextHigh,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, fontSize = 14.sp)
                    if (playback.artist.isNotBlank()) {
                        Text(playback.artist, color = TextMid,
                            maxLines = 1, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onTogglePlay, modifier = Modifier.size(44.dp)) {
                    Icon(
                        if (playback.isPlaying) Icons.Rounded.Pause
                        else Icons.Rounded.PlayArrow,
                        null, tint = TextHigh, modifier = Modifier.size(28.dp),
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.SkipNext, null,
                        tint = TextMid, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniArt(artUri: String?) {
    val ctx = LocalContext.current
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(InkRaised),
        contentAlignment = Alignment.Center,
    ) {
        if (!artUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(ctx).data(artUri).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Rounded.MusicNote, null,
                tint = Primary, modifier = Modifier.size(22.dp))
        }
    }
}

/* ─────────────── Extras menu (drawer-style) ─────────────── */

@Composable
private fun ExtrasMenu(
    onDismiss: () -> Unit,
    onMounts: () -> Unit,
    onFolders: () -> Unit,
    onSettings: () -> Unit,
    onSleep: () -> Unit,
    onBookmarks: () -> Unit,
    onEqualizer: () -> Unit,
    onBackup: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize()
            .background(Ink.copy(alpha = 0.92f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.78f)
                .align(Alignment.CenterStart)
                .padding(24.dp)
                .systemBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("More", color = TextHigh,
                fontWeight = FontWeight.Bold, fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 12.dp))

            ExtrasItem(Icons.Rounded.Folder, "Folders", onClick = onFolders)
            ExtrasItem(Icons.Rounded.Cloud, "Network mounts", onClick = onMounts)
            ExtrasItem(Icons.Rounded.Bedtime, "Sleep timer", onClick = onSleep)
            ExtrasItem(Icons.Rounded.BookmarkBorder, "Bookmarks", onClick = onBookmarks)
            ExtrasItem(Icons.Rounded.Equalizer, "Equalizer", onClick = onEqualizer)
            ExtrasItem(Icons.Rounded.Backup, "Backup & restore", onClick = onBackup)
            ExtrasItem(Icons.Rounded.Settings, "Settings", onClick = onSettings)
        }
    }
}

@Composable
private fun ExtrasItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = TextHigh,
            fontWeight = FontWeight.Medium, fontSize = 16.sp)
    }
}

/** Wrap SettingsScreen so it has its own close button when used as overlay. */
@Composable
private fun SettingsOverlay(vm: MusicViewModel, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Ink).systemBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, null, tint = TextHigh)
            }
            Spacer(Modifier.width(4.dp))
            Text("Settings", color = TextHigh,
                fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
        SettingsScreen(vm)
    }
}
