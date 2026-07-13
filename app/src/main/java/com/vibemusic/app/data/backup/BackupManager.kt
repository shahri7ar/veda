package com.vibemusic.app.data.backup

import android.content.Context
import com.vibemusic.app.VibeMusicApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Musicolet-style Backup & Restore.
 * Saves: settings, playlists, play counts, favorites.
 * Exports as JSON under app-private external storage.
 */
object BackupManager {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private const val BACKUP_DIR = "VibeMusic/backups"

    @Serializable
    data class BackupData(
        val version: Int = 1,
        val timestamp: Long = System.currentTimeMillis(),
        val settings: Map<String, String> = emptyMap(),
        val playlists: List<PlaylistBackup> = emptyList(),
        val playCounts: Map<String, Int> = emptyMap(),
        val favorites: List<String> = emptyList(),
        val queueStates: List<QueueBackup> = emptyList(),
    )

    @Serializable
    data class PlaylistBackup(
        val name: String,
        val trackIds: List<String>,
    )

    @Serializable
    data class QueueBackup(
        val name: String,
        val sourceType: String,
        val sourceId: String,
        val trackIds: List<String>,
        val currentIndex: Int,
        val positionMs: Long,
    )

    // ─── Backup ─────────────────────────────────

    suspend fun createBackup(context: Context): File = withContext(Dispatchers.IO) {
        val db = VibeMusicApp.instance.database
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

        val settingsMap = prefs.all.mapValues { it.value.toString() }

        val playlists = db.playlistDao().observeAll().first().map { pl ->
            PlaylistBackup(
                name = pl.name,
                trackIds = pl.trackIdsJson.split("|").filter { it.isNotBlank() },
            )
        }

        val allTracks = db.trackDao().observeAll().first()
        val favorites = allTracks.filter { it.isFavorite }.map { it.id }
        val playCounts = allTracks.filter { it.playCount > 0 }
            .associate { it.id to it.playCount }

        val data = BackupData(
            settings = settingsMap,
            playlists = playlists,
            playCounts = playCounts,
            favorites = favorites,
        )

        val parent = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(parent, BACKUP_DIR).apply { mkdirs() }

        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val file = File(dir, "vibemusic_backup_$date.json")
        file.writeText(json.encodeToString(data))
        file
    }

    // ─── Restore ────────────────────────────────

    suspend fun restoreFromFile(context: Context, file: File): Int =
        withContext(Dispatchers.IO) {
            val raw = file.readText()
            val data = json.decodeFromString<BackupData>(raw)
            val db = VibeMusicApp.instance.database

            var restored = 0

            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().apply {
                data.settings.forEach { (k, v) ->
                    when {
                        v == "true" || v == "false" -> putBoolean(k, v.toBoolean())
                        v.toIntOrNull() != null -> putInt(k, v.toInt())
                        v.toLongOrNull() != null -> putLong(k, v.toLong())
                        v.toFloatOrNull() != null -> putFloat(k, v.toFloat())
                        else -> putString(k, v)
                    }
                }
            }.apply()

            val existingPlaylists = db.playlistDao().observeAll().first()
            data.playlists.forEach { pl ->
                val existing = existingPlaylists.find { it.name == pl.name }
                if (existing == null) {
                    db.playlistDao().upsert(
                        com.vibemusic.app.data.PlaylistEntity(
                            name = pl.name,
                            trackIdsJson = pl.trackIds.joinToString("|"),
                            createdAt = System.currentTimeMillis(),
                        )
                    )
                    restored++
                }
            }

            data.playCounts.forEach { (tid, count) ->
                val track = db.trackDao().byId(tid)
                if (track != null && count > track.playCount) {
                    db.trackDao().update(track.copy(playCount = count))
                }
            }

            data.favorites.forEach { tid ->
                val t = db.trackDao().byId(tid)
                if (t != null && !t.isFavorite) {
                    db.trackDao().update(t.copy(isFavorite = true))
                    restored++
                }
            }

            restored
        }

    /** List all backup files. */
    fun listBackups(context: Context): List<File> {
        val parent = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(parent, BACKUP_DIR)
        return dir.listFiles()?.filter { it.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
