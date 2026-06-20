package com.vibemusic.app.data.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.vibemusic.app.data.model.SourceType
import com.vibemusic.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans MediaStore for local audio files. Returns lightweight Track models
 * that MusicRepository persists to the Room database.
 */
class LocalScanner(private val context: Context) {

    suspend fun scanDevice(): List<Track> = withContext(Dispatchers.IO) {
        val out = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.IS_MUSIC,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} > 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { c ->
            val idC = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val yearC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            while (c.moveToNext()) {
                val mediaId = c.getLong(idC)
                val uri = ContentUris.withAppendedId(collection, mediaId).toString()
                val albumId = c.getLong(albumIdC)
                val artUri = ContentUris.withAppendedId(
                    android.net.Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                out += Track(
                    id = "local_$mediaId",
                    title = c.getString(titleC) ?: "Unknown",
                    artist = c.getString(artistC) ?: "Unknown Artist",
                    album = c.getString(albumC) ?: "Unknown Album",
                    albumId = "local_album_$albumId",
                    durationMs = c.getLong(durC),
                    sourceUri = uri,
                    sourceType = SourceType.LOCAL,
                    mountId = null,
                    sizeBytes = c.getLong(sizeC),
                    cached = true,
                    cachePath = null,
                    albumArtUri = artUri,
                    isFavorite = false,
                    year = c.getInt(yearC),
                )
            }
        }
        out
    }
}
