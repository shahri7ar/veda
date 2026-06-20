package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vibemusic.app.VibeMusicApp
import com.vibemusic.app.playback.PlaybackSnapshot
import com.vibemusic.app.ui.MusicViewModel
import com.vibemusic.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    playback: PlaybackSnapshot,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onDismiss: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ctx = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = Ink,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextLow) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Ink, InkSurface)))
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Album art
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(InkRaised),
                contentAlignment = Alignment.Center,
            ) {
                if (!playback.artUri.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx).data(playback.artUri).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Rounded.Album, null,
                        tint = Primary, modifier = Modifier.size(96.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                playback.title.ifBlank { "Nothing playing" },
                color = TextHigh,
                fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 1,
            )
            Text(
                playback.artist.ifBlank { "—" },
                color = TextMid, fontSize = 14.sp, maxLines = 1,
            )

            Spacer(Modifier.height(16.dp))

            // Progress slider
            val progress = if (playback.durationMs > 0)
                playback.positionMs.toFloat() / playback.durationMs else 0f
            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = { v -> onSeek((v * playback.durationMs).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = Primary,
                    activeTrackColor = Primary,
                    inactiveTrackColor = InkRaised,
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(playback.positionMs), color = TextMid, fontSize = 11.sp)
                Text(formatDuration(playback.durationMs), color = TextMid, fontSize = 11.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Main playback controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Rounded.Shuffle, null,
                        modifier = Modifier.size(22.dp),
                        tint = if (shuffleEnabled) Primary else TextMid,
                    )
                }
                IconButton(onClick = onPrev) {
                    Icon(Icons.Rounded.SkipPrevious, null,
                        modifier = Modifier.size(40.dp), tint = TextHigh)
                }
                FilledIconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Primary,
                        contentColor = OnPrimary,
                    ),
                ) {
                    Icon(
                        if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null, modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, null,
                        modifier = Modifier.size(40.dp), tint = TextHigh)
                }
                IconButton(onClick = onToggleRepeat) {
                    val icon = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    }
                    val tint = if (repeatMode != Player.REPEAT_MODE_OFF) Primary else TextMid
                    Icon(icon, null, modifier = Modifier.size(22.dp), tint = tint)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Secondary actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenLyrics,
                    colors = ButtonDefaults.textButtonColors(contentColor = TextMid)) {
                    Icon(Icons.Rounded.Lyrics, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Lyrics", fontSize = 13.sp)
                }
                TextButton(onClick = onOpenQueue,
                    colors = ButtonDefaults.textButtonColors(contentColor = TextMid)) {
                    Icon(Icons.Rounded.QueueMusic, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Queue", fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun SettingsScreen(vm: MusicViewModel) {
    val app = VibeMusicApp.instance
    val usage by app.cacheManager.usageBytes.collectAsState()
    val maxCacheMb by app.settingsRepository.maxCacheMb.collectAsState(initial = 1024)
    val cacheOnPlay by app.settingsRepository.cacheOnPlay.collectAsState(initial = true)
    val wifiOnly by app.settingsRepository.wifiOnly.collectAsState(initial = false)
    val autoScan by app.settingsRepository.autoScanOnStart.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("Storage")
        SettingRow(
            title = "Cache usage",
            subtitle = "${usage / (1024 * 1024)} MB / $maxCacheMb MB",
        )
        SettingSlider(
            title = "Max cache size",
            valueMb = maxCacheMb,
            onChange = { mb ->
                scope.launch {
                    app.settingsRepository.setMaxCacheMb(mb)
                    app.cacheManager.setMaxBytes(mb.toLong() * 1024 * 1024)
                }
            },
        )
        SettingToggle("Cache while playing", cacheOnPlay) {
            scope.launch { app.settingsRepository.setCacheOnPlay(it) }
        }
        FilledTonalButton(onClick = {
            scope.launch { app.cacheManager.clearAll() }
        }) { Text("Clear cache now") }

        Spacer(Modifier.height(8.dp))
        SectionHeader("Network")
        SettingToggle("Stream on Wi-Fi only", wifiOnly) {
            scope.launch { app.settingsRepository.setWifiOnly(it) }
        }

        Spacer(Modifier.height(8.dp))
        SectionHeader("Library")
        SettingToggle("Rescan device on launch", autoScan) {
            scope.launch { app.settingsRepository.setAutoScan(it) }
        }
        FilledTonalButton(onClick = { vm.rescanLocal() }) {
            Icon(Icons.Rounded.Sync, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Rescan device now")
        }

        Spacer(Modifier.height(8.dp))
        SectionHeader("About")
        Text("VibeMusic v2.0.0", color = TextMid, fontSize = 13.sp)
        Text("Offline music player · ViMusic-inspired UI",
            color = TextLow, fontSize = 12.sp)
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        color = Primary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextHigh, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextMid, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingToggle(title: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextHigh, modifier = Modifier.weight(1f))
        Switch(
            checked = value, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnPrimary,
                checkedTrackColor = Primary,
                uncheckedThumbColor = TextMid,
                uncheckedTrackColor = InkRaised,
            ),
        )
    }
}

@Composable
private fun SettingSlider(title: String, valueMb: Int, onChange: (Int) -> Unit) {
    Column {
        Text("$title: $valueMb MB", color = TextHigh)
        Slider(
            value = valueMb.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 128f..8192f,
            steps = 31,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = InkRaised,
            ),
        )
    }
}
