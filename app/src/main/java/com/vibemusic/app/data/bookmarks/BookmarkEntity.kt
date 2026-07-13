package com.vibemusic.app.data.bookmarks

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val trackId: String,
    val trackTitle: String,
    val trackArtist: String,
    val positionMs: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE trackId = :trackId LIMIT 1")
    suspend fun byTrackId(trackId: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE trackId = :trackId")
    suspend fun delete(trackId: String)

    @Query("DELETE FROM bookmarks")
    suspend fun clear()
}
