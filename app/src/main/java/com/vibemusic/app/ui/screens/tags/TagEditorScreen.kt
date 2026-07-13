package com.vibemusic.app.ui.screens.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.tags.TagEditor
import com.vibemusic.app.data.tags.TagEditor.TrackTags
import com.vibemusic.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Musicolet-style Tag Editor UI.
 * Shows tags for a track and allows editing title, artist, album, year.
 */
@Composable
fun TagEditorScreen(
    track: Track,
    onDismiss: () -> Unit,
) {
    val path = track.sourceUri.removePrefix("file://")
    val tags by produceState(initialValue = TrackTags(path), path) {
        value = withContext(Dispatchers.IO) { TagEditor.readTags(path) }
    }

    var title by remember(tags) { mutableStateOf(tags.title) }
    var artist by remember(tags) { mutableStateOf(tags.artist) }
    var album by remember(tags) { mutableStateOf(tags.album) }
    var year by remember(tags) { mutableStateOf(tags.year) }
    var genre by remember(tags) { mutableStateOf(tags.genre) }

    Column(
        Modifier.fillMaxSize().background(Ink).systemBarsPadding()
    ) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, null, tint = TextHigh)
            }
            Spacer(Modifier.width(8.dp))
            Text("Tag Editor", color = TextHigh,
                fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                TagEditor.writeTags(
                    track.sourceUri.removePrefix("file://"),
                    title = title, artist = artist,
                    album = album, year = year, genre = genre,
                )
            }) { Text("Save") }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // File path
                Text(track.sourceUri.removePrefix("file://"),
                    color = TextLow, fontSize = 11.sp, maxLines = 2)
                Spacer(Modifier.height(8.dp))
            }

            item { TagField("Title", title) { title = it } }
            item { TagField("Artist", artist) { artist = it } }
            item { TagField("Album", album) { album = it } }
            item { TagField("Year", year) { year = it } }
            item { TagField("Genre", genre) { genre = it } }

            item {
                Spacer(Modifier.height(8.dp))
                // Info row
                Card(colors = CardDefaults.cardColors(containerColor = InkRaised)) {
                    Column(Modifier.padding(14.dp)) {
                        InfoRow("Has album art", if (tags.hasArt) "Yes" else "No")
                        InfoRow("Track number", tags.trackNumber.ifBlank { "N/A" })
                        InfoRow("Embedded lyrics", if (tags.hasEmbeddedLyrics) "Yes" else "No")
                    }
                }
            }

            // Lyrics preview if present
            if (tags.embeddedLyrics != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = InkRaised)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Embedded Lyrics", color = Primary,
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                tags.embeddedLyrics.take(500) +
                                    if (tags.embeddedLyrics.length > 500) "..." else "",
                                color = TextMid, fontSize = 12.sp, lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun TagField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = OutlineColor,
            focusedTextColor = TextHigh,
            unfocusedTextColor = TextHigh,
            cursorColor = Primary,
            focusedLabelColor = Primary,
        ),
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = TextLow, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextHigh, fontSize = 12.sp)
    }
}

/**
 * Multi-select tag editor for batch operations.
 */
@Composable
fun BatchTagEditorScreen(
    tracks: List<Track>,
    onDismiss: () -> Unit,
) {
    var newArtist by remember { mutableStateOf("") }
    var newAlbum by remember { mutableStateOf("") }
    var newYear by remember { mutableStateOf("") }
    var newGenre by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().background(Ink).systemBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, null, tint = TextHigh)
            }
            Text("Batch Edit (${tracks.size} tracks)", color = TextHigh,
                fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Leave fields blank to skip. Only filled fields will be applied to all selected tracks.",
                    color = TextLow, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
            }
            item { TagField("Artist (all)", newArtist) { newArtist = it } }
            item { TagField("Album (all)", newAlbum) { newAlbum = it } }
            item { TagField("Year (all)", newYear) { newYear = it } }
            item { TagField("Genre (all)", newGenre) { newGenre = it } }

            item {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        tracks.forEach { track ->
                            val path = track.sourceUri.removePrefix("file://")
                            TagEditor.writeTags(path,
                                artist = newArtist.ifBlank { null },
                                album = newAlbum.ifBlank { null },
                                year = newYear.ifBlank { null },
                                genre = newGenre.ifBlank { null },
                            )
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Apply to ${tracks.size} tracks") }
            }

            item { Spacer(Modifier.height(8.dp)) }

            // Track list
            item {
                Text("Selected tracks:", color = TextMid,
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            itemsIndexed(tracks) { i, track ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("${i + 1}.", color = TextLow, fontSize = 12.sp,
                        modifier = Modifier.width(24.dp))
                    Column {
                        Text(track.title, color = TextHigh, fontSize = 13.sp, maxLines = 1)
                        Text("${track.artist} — ${track.album}",
                            color = TextMid, fontSize = 11.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
