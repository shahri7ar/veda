package com.vibemusic.app.data.tags

import android.media.MediaMetadataRetriever
import com.vibemusic.app.data.model.Track
import java.io.File

/**
 * Musicolet-style Tag Editor.
 * Read/write embedded ID3 tags and album art.
 *
 * Note: Full ID3 writing requires JAudioTagger or similar.
 * This module provides the read side + a framework for writing
 * plain-text tags (title, artist, album, year) via MediaMetadataRetriever.
 *
 * For production write support, add:
 *   implementation("org:jaudiotagger:3.0.1")
 */
object TagEditor {

    data class TrackTags(
        val path: String,
        val title: String = "",
        val artist: String = "",
        val album: String = "",
        val year: String = "",
        val genre: String = "",
        val trackNumber: String = "",
        val hasEmbeddedLyrics: Boolean = false,
        val embeddedLyrics: String? = null,
        val hasArt: Boolean = false,
    )

    /** Read all tags from a file. */
    fun readTags(filePath: String): TrackTags {
        val f = File(filePath)
        if (!f.exists()) return TrackTags(filePath)

        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(filePath)

            // Check for embedded picture
            val art = mmr.embeddedPicture
            val hasArt = art != null && art.isNotEmpty()

            TrackTags(
                path = filePath,
                title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: f.nameWithoutExtension,
                artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown",
                album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown",
                year = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR) ?: "",
                genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: "",
                trackNumber = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER) ?: "",
                hasEmbeddedLyrics = false, // requires JAudioTagger
                embeddedLyrics = null,
                hasArt = hasArt,
            )
        } catch (e: Exception) {
            TrackTags(filePath)
        } finally {
            mmr.release()
        }
    }

    /** Read tags for multiple tracks at once (batch mode). */
    fun readTagsBatch(tracks: List<Track>): List<TrackTags> {
        return tracks.map { track ->
            val path = track.sourceUri.removePrefix("file://")
            readTags(path)
        }
    }

    /**
     * Write simple text tags.
     * Requires JAudioTagger for actual ID3 write.
     * Placeholder for now — shows the architecture.
     */
    fun writeTags(
        filePath: String,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        year: String? = null,
        genre: String? = null,
    ): Boolean {
        // TODO: implement with JAudioTagger
        // For now returns false as write is not yet implemented
        // val audioFile = AudioFileIO.read(File(filePath))
        // val tag = audioFile.tag
        // title?.let { tag.setField(FieldKey.TITLE, it) }
        // ...
        // AudioFileIO.write(audioFile)
        return false
    }

    /** Extracts just the embedded lyrics text if present. */
    fun extractEmbeddedLyrics(filePath: String): String? {
        val f = File(filePath)
        if (!f.exists()) return null
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(filePath)
            // MediaMetadataRetriever doesn't expose lyrics directly.
            // JAudioTagger is needed for this.
            null
        } catch (_: Exception) {
            null
        } finally {
            mmr.release()
        }
    }
}
