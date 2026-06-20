package com.vibemusic.app.ui.screens.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.vibemusic.app.VibeMusicApp
import com.vibemusic.app.data.bookmarks.BookmarkEntity
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.playback.PlayerHub
import com.vibemusic.app.ui.MusicViewModel
import com.vibemusic.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Musicolet-style bookmarks: save position + note for any track.
 * Persisted to Room.
 */
object BookmarkStore {

    private val dao get() = VibeMusicApp.instance.database.bookmarkDao()

    fun observe() = dao.observeAll()

    suspend fun add(track: Track, positionMs: Long, note: String = "") =
        withContext(Dispatchers.IO) {
            dao.upsert(
                BookmarkEntity(
                    trackId = track.id,
                    trackTitle = track.title,
                    trackArtist = track.artist,
                    positionMs = positionMs,
                    note = note,
                )
            )
        }

    suspend fun addQuick(positionMs: Long, note: String = "") = withContext(Dispatchers.IO) {
        val snap = PlayerHub.state.value
        val id = snap.mediaId ?: return@withContext
        if (snap.title.isBlank()) return@withContext
        dao.upsert(
            BookmarkEntity(
                trackId = id,
                trackTitle = snap.title,
                trackArtist = snap.artist,
                positionMs = positionMs,
                note = note,
            )
        )
    }

    suspend fun remove(trackId: String) = withContext(Dispatchers.IO) {
        dao.delete(trackId)
    }

    suspend fun clear() = withContext(Dispatchers.IO) { dao.clear() }
}

@Composable
fun BookmarksScreen(vm: MusicViewModel, onDismiss: () -> Unit) {
    val bookmarks by BookmarkStore.observe().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val playback by vm.playback.collectAsState()
    var newNote by remember { mutableStateOf("") }
    var showQuick by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(Ink).systemBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, null, tint = TextHigh)
            }
            Spacer(Modifier.width(4.dp))
            Text("Bookmarks", color = TextHigh,
                fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            if (playback.title.isNotBlank()) {
                FilledTonalButton(onClick = { showQuick = true }) {
                    Icon(Icons.Rounded.BookmarkAdd, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bookmark current")
                }
            }
        }

        if (bookmarks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.BookmarkBorder, null,
                        tint = TextLow, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No bookmarks yet", color = TextLow)
                    if (playback.title.isNotBlank()) {
                        Text("Tap “Bookmark current” to save where you are now",
                            color = TextLow, fontSize = 12.sp)
                    } else {
                        Text("Play a track, then come back to bookmark it",
                            color = TextLow, fontSize = 12.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(bookmarks, key = { it.trackId }) { bm ->
                    BookmarkCard(bm,
                        onResume = { vm.resumeBookmark(bm) },
                        onDelete = { scope.launch { BookmarkStore.remove(bm.trackId) } })
                }
            }
        }
    }

    if (showQuick) {
        AlertDialog(
            onDismissRequest = { showQuick = false; newNote = "" },
            containerColor = InkSurface,
            title = { Text("Bookmark this track", color = TextHigh) },
            text = {
                Column {
                    Text(playback.title, color = TextHigh, fontWeight = FontWeight.SemiBold)
                    Text(playback.artist, color = TextMid, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newNote,
                        onValueChange = { newNote = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pos = playback.positionMs
                    val note = newNote.trim()
                    scope.launch { BookmarkStore.addQuick(pos, note) }
                    newNote = ""
                    showQuick = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showQuick = false; newNote = "" }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun BookmarkCard(
    bm: BookmarkEntity,
    onResume: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = InkRaised),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onResume),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bookmark, null,
                    tint = Secondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(bm.trackTitle, color = TextHigh,
                        fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
                    Text(bm.trackArtist, color = TextMid, fontSize = 12.sp, maxLines = 1)
                }
                Text(bm.positionMmSs(), color = TextLow, fontSize = 11.sp)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Close, null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp))
                }
            }
            if (bm.note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(bm.note, color = TextMid, fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(InkSurface, RoundedCornerShape(8.dp))
                        .padding(10.dp))
            }
        }
    }
}

private fun BookmarkEntity.positionMmSs(): String {
    val s = positionMs / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
