package com.vibemusic.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.vibemusic.app.data.bookmarks.BookmarkDao
import com.vibemusic.app.data.bookmarks.BookmarkEntity
import com.vibemusic.app.data.queue.QueueEntity
import com.vibemusic.app.data.queue.QueueDao
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String = "",
    val durationMs: Long,
    val sourceUri: String,
    val sourceType: String,
    val mountId: String?,
    val sizeBytes: Long,
    val cached: Boolean,
    val cachePath: String?,
    val albumArtUri: String?,
    val isFavorite: Boolean = false,
    val year: Int = 0,
    val lastPlayedAt: Long = 0L,
    val playCount: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "mounts")
data class MountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val host: String,
    val port: Int,
    val share: String,
    val path: String,
    val username: String,
    val passwordEnc: String,
    val domain: String,
    val enabled: Boolean,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val trackIdsJson: String,
    val createdAt: Long,
)

class Converters {
    @TypeConverter
    fun fromList(value: List<String>?): String = value?.joinToString("|") ?: ""

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("|")
}

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE sourceType = 'LOCAL' ORDER BY title COLLATE NOCASE")
    fun observeLocal(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE mountId = :mountId")
    fun observeByMount(mountId: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE cached = 1")
    fun observeCached(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE")
    fun observeFavorites(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY addedAt DESC LIMIT 50")
    fun observeRecent(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TrackEntity>)

    @Update
    suspend fun update(item: TrackEntity)

    @Query("DELETE FROM tracks WHERE mountId = :mountId")
    suspend fun deleteByMount(mountId: String)

    @Query("DELETE FROM tracks WHERE sourceType = 'LOCAL'")
    suspend fun clearLocal()

    @Query("UPDATE tracks SET lastPlayedAt = :ts, playCount = playCount + 1 WHERE id = :id")
    suspend fun markPlayed(id: String, ts: Long)
}

@Dao
interface MountDao {
    @Query("SELECT * FROM mounts ORDER BY name")
    fun observeAll(): Flow<List<MountEntity>>

    @Query("SELECT * FROM mounts WHERE enabled = 1")
    suspend fun enabled(): List<MountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MountEntity)

    @Query("DELETE FROM mounts WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(
    entities = [TrackEntity::class, MountEntity::class, PlaylistEntity::class, QueueEntity::class, BookmarkEntity::class],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun mountDao(): MountDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun queueDao(): QueueDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(ctx: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                ctx.applicationContext, AppDatabase::class.java, "vibemusic.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
