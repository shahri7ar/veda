package com.vibemusic.app.data.queue

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Musicolet-style Multiple Queue system.
 *
 * Each queue remembers:
 *  - The source context (folder, album, artist, playlist name/id)
 *  - The full track list (serialized as pipe-separated ids)
 *  - The currently-playing track id + position within it
 *  - Shuffle state
 *
 * Up to 20 concurrent queues.
 */

@Entity(tableName = "queues")
data class QueueEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourceType: String,      // "folder", "album", "artist", "playlist", "manual"
    val sourceId: String,         // folder path, album id, artist name, playlist id, etc.
    val trackIdsJson: String,     // pipe-separated track ids
    val currentIndex: Int = 0,    // which track in the list is active
    val positionMs: Long = 0L,    // position within the active track
    val isShuffled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
)

@Dao
interface QueueDao {
    @Query("SELECT * FROM queues ORDER BY lastAccessedAt DESC")
    fun observeAll(): Flow<List<QueueEntity>>

    @Query("SELECT * FROM queues WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): QueueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(queue: QueueEntity)

    @Query("DELETE FROM queues WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM queues")
    suspend fun count(): Int
}

/**
 * In-app model (clean).
 */
data class PlayerQueue(
    val id: String,
    val name: String,
    val sourceType: String,
    val sourceId: String,
    val trackIds: List<String>,
    val currentIndex: Int = 0,
    val positionMs: Long = 0L,
    val isShuffled: Boolean = false,
    val createdAt: Long = 0L,
    val lastAccessedAt: Long = 0L,
)
