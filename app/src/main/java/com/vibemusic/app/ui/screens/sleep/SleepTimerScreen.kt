package com.vibemusic.app.ui.screens.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibemusic.app.ui.theme.*

/**
 * Musicolet-style sleep timer UI.
 * Two modes: duration (hh:mm) and song count.
 */
@Composable
fun SleepTimerSheet(
    timer: SleepTimerManager,
    onDismiss: () -> Unit,
) {
    val remainingMs by timer.remainingMs.collectAsState()
    val remainingSongs by timer.remainingSongs.collectAsState()
    val isActive = remainingMs > 0 || remainingSongs > 0

    // Duration picker state
    var hours by remember { mutableIntStateOf(0) }
    var mins by remember { mutableIntStateOf(30) }
    // Song count state
    var songCount by remember { mutableIntStateOf(5) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(InkSurface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Handle
        Box(Modifier.width(40.dp).height(4.dp).background(TextLow, RoundedCornerShape(2.dp)))

        Text("Sleep Timer", color = TextHigh, fontWeight = FontWeight.Bold, fontSize = 20.sp)

        if (isActive) {
            // Show active timer
            Card(
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.Bedtime, null, tint = Primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(timer.formatRemaining(), color = Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Duration mode ──
        Text("Stop after time", color = TextMid, fontSize = 13.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberPicker("h", hours, 0..23) { hours = it }
            Text(":", color = TextHigh, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            NumberPicker("m", mins, 0..59) { mins = it }
        }

        OutlinedButton(onClick = {
            val totalMs = ((hours * 60L) + mins) * 60_000L
            if (totalMs > 0) timer.setDuration(totalMs)
        }) { Text("Start timer") }

        HorizontalDivider(color = OutlineColor)

        // ── Song count mode ──
        Text("Stop after N songs", color = TextMid, fontSize = 13.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberPicker("songs", songCount, 1..50) { songCount = it }
        }
        OutlinedButton(onClick = { timer.setSongCount(songCount) }) {
            Text("Start countdown")
        }

        // ── Cancel ──
        if (isActive) {
            TextButton(
                onClick = { timer.cancel() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Cancel timer") }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss) { Text("Close") }
    }
}

@Composable
private fun NumberPicker(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { if (value < range.last) onChange(value + 1) }) {
            Icon(Icons.Rounded.KeyboardArrowUp, null, tint = Primary)
        }
        Text(
            "%02d".format(value),
            color = TextHigh,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
        )
        IconButton(onClick = { if (value > range.first) onChange(value - 1) }) {
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Primary)
        }
        Text(label, color = TextLow, fontSize = 10.sp)
    }
}
