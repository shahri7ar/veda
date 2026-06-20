package com.vibemusic.app.data.queue

import com.vibemusic.app.VibeMusicApp
import com.vibemusic.app.data.TrackEntity
import com.vibemusic.app.data.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Manages multiple concurrent playback queues (Musicolet-inspired).
 */
class QueueRepository {

    private val db get() = VibeMusicApp.instance.database

    /** Max queues allowed (Musicolet = 20). */
    private val maxQueues = 20

    fun observeAll(): Flow<List<PlayerQueue>> =
        db.queueDao().observeAll().map { list -> list.map { it.toModel() } }

    suspend fun saveQueue(
        name: String,
        sourceType: String,
        sourceId: String,
        trackIds: List<String>,
        currentIndex: Int = 0,
        positionMs: Long = 0L,
        isShuffled: Boolean = false,
    ): String {
        val count = db.queueDao().count()
        if (count >= maxQueues) {
            val oldest = db.queueDao().observeAll().first()
                .minByOrNull { it.lastAccessedAt }
            if (oldest != null) db.queueDao().delete(oldest.id)
        }

        val id = UUID.randomUUID().toString().take(8)
        db.queueDao().upsert(
            QueueEntity(
                id = id, name = name, sourceType = sourceType,
                sourceId = sourceId, trackIdsJson = trackIds.joinToString("|"),
                currentIndex = currentIndex, positionMs = positionMs,
                isShuffled = isShuffled,
            )
        )
        return id
    }

    suspend fun updatePosition(queueId: String, index: Int, positionMs: Long) {
        val entity = db.queueDao().byId(queueId) ?: return
        db.queueDao().upsert(
            entity.copy(
                currentIndex = index,
                positionMs = positionMs,
                lastAccessedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun deleteQueue(id: String) = db.queueDao().delete(id)

    suspend fun getQueueTracks(queueId: String): List<Track> {
        val entity = db.queueDao().byId(queueId) ?: return emptyList()
        val ids = entity.trackIdsJson.split("|").filter { it.isNotBlank() }
        return ids.mapNotNull { db.trackDao().byId(it)?.toModel() }
    }

    suspend fun touch(queueId: String) {
        val entity = db.queueDao().byId(queueId) ?: return
        db.queueDao().upsert(entity.copy(lastAccessedAt = System.currentTimeMillis()))
    }

    // ─── Mappers ───────────────────────────────────────────────────

    private fun QueueEntity.toModel() = PlayerQueue(
        id = id, name = name, sourceType = sourceType, sourceId = sourceId,
        trackIds = trackIdsJson.split("|").filter { it.isNotBlank() },
        currentIndex = currentIndex, positionMs = positionMs,
        isShuffled = isShuffled, createdAt = createdAt,
        lastAccessedAt = lastAccessedAt,
    )

    private fun TrackEntity.toModel() = Track(
        id = id, title = title, artist = artist, album = album,
        albumId = albumId, durationMs = durationMs, sourceUri = sourceUri,
        sourceType = com.vibemusic.app.data.model.SourceType.valueOf(sourceType),
        mountId = mountId, sizeBytes = sizeBytes, cached = cached,
        cachePath = cachePath, albumArtUri = albumArtUri,
        isFavorite = isFavorite, year = year,
    )
}
