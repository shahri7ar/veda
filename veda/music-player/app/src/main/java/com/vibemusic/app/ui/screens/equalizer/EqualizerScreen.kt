package com.vibemusic.app.ui.screens.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibemusic.app.ui.MusicViewModel
import com.vibemusic.app.ui.theme.*

/**
 * Equalizer screen — wraps Android's hardware Equalizer through the playing
 * audio session. Bands and presets are discovered at runtime when playback
 * starts.
 */
@Composable
fun EqualizerScreen(vm: MusicViewModel, onDismiss: () -> Unit) {
    val enabled by vm.eqEnabled.collectAsState()
    val bands by vm.eqBands.collectAsState()
    val freqs by vm.eqBandFreqs.collectAsState()
    val range by vm.eqRange.collectAsState()
    val presets by vm.eqPresets.collectAsState()
    val playback by vm.playback.collectAsState()

    Column(Modifier.fillMaxSize().background(Ink).systemBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, null, tint = TextHigh)
            }
            Spacer(Modifier.width(4.dp))
            Text("Equalizer", color = TextHigh,
                fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = enabled,
                onCheckedChange = { vm.setEqEnabled(it) },
                enabled = bands.isNotEmpty(),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnPrimary,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = TextMid,
                    uncheckedTrackColor = InkRaised,
                ),
            )
        }

        if (bands.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Equalizer, null,
                        tint = TextLow, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Equalizer is not ready",
                        color = TextMid, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (playback.title.isBlank())
                            "Start playing a track first, then open the equalizer."
                        else
                            "This device's audio framework didn't expose any EQ bands.",
                        color = TextLow, fontSize = 12.sp,
                    )
                }
            }
            return@Column
        }

        // Presets
        if (presets.isNotEmpty()) {
            Text("Presets", color = TextMid, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(6.dp))
            LazyHorizontalScroll {
                presets.forEachIndexed { i, name ->
                    AssistChip(
                        onClick = { vm.applyEqPreset(i) },
                        label = { Text(name, fontSize = 12.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = InkRaised,
                            labelColor = TextHigh,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Bands
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            bands.forEachIndexed { i, level ->
                EqBandSlider(
                    level = level.toInt(),
                    range = range.first.toInt()..range.second.toInt(),
                    frequencyHz = freqs.getOrNull(i) ?: 0,
                    onChange = { vm.setEqBand(i, it.toShort()) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LazyHorizontalScroll(content: @Composable () -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() } }
    }
}

@Composable
private fun EqBandSlider(
    level: Int,
    range: IntRange,
    frequencyHz: Int,
    onChange: (Int) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(min = 44.dp),
    ) {
        // Label (top — gain in dB)
        Text(
            "${(level / 100f).let { if (it >= 0) "+%.1f".format(it) else "%.1f".format(it) }} dB",
            color = TextMid, fontSize = 10.sp,
        )

        Spacer(Modifier.height(6.dp))

        // Vertical slider — implemented via drag on a column.
        val height = 220.dp
        val span = range.last - range.first
        val pct = (level - range.first).toFloat() / span.coerceAtLeast(1)

        Box(
            modifier = Modifier
                .height(height)
                .width(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(InkRaised)
                .pointerInput(range) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        // 1 dp of drag = (span / height_in_dp) millibels
                        val deltaPercent = -dragAmount / size.height
                        val newLevel = (level + (deltaPercent * span)).toInt()
                            .coerceIn(range.first, range.last)
                        if (newLevel != level) onChange(newLevel)
                    }
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Fill — height proportional to level
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(pct.coerceIn(0f, 1f))
                    .background(
                        Color(0xFFB8A9E8),
                        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    ),
            )
            // Thumb indicator at the top of the fill
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(TextHigh)
                    .offset(y = -(pct * (height.value - 2)).dp),
            )
        }

        Spacer(Modifier.height(6.dp))

        // Frequency label
        Text(
            formatFreq(frequencyHz),
            color = TextLow, fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatFreq(milliHz: Int): String {
    val hz = milliHz / 1000
    return when {
        hz >= 1000 -> "${hz / 1000}k"
        else -> "$hz"
    }
}
