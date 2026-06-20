package com.vibemusic.app.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.vibemusic.app.VibeMusicApp
import com.vibemusic.app.data.model.MountConfig
import com.vibemusic.app.ui.MainActivity
import com.vibemusic.app.widget.MusicWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Foreground MediaSessionService — the single owner of the ExoPlayer instance. */
class MusicService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        val mountsProvider: () -> List<MountConfig> = {
            // synchronous snapshot — repository owns the cache
            runCatching { PlayerHub.currentMounts }.getOrNull() ?: emptyList()
        }

        val dsFactory = NetworkAudioDataSource.Factory(
            mounts = mountsProvider,
            cache = VibeMusicApp.instance.cacheManager
        )
        val mediaSourceFactory = DefaultMediaSourceFactory(this).setDataSourceFactory(dsFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .build()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                PlayerHub.update(player)
                MusicWidgetReceiver.requestUpdate(this@MusicService)
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                PlayerHub.update(player)
                MusicWidgetReceiver.requestUpdate(this@MusicService)
            }
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                PlayerHub.update(player)
            }
        })

        val sessionActivity = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        session = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()

        PlayerHub.attach(player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        scope.cancel()
        session?.run { player.release(); release() }
        session = null
        PlayerHub.detach()
        super.onDestroy()
    }
}

/** Tiny in-process hub so the UI layer can publish mount configs and read player state. */
object PlayerHub {
    @Volatile var currentMounts: List<MountConfig> = emptyList()
    private var player: ExoPlayer? = null
    val state = MutableStateFlow(PlaybackSnapshot())

    fun setMounts(list: List<MountConfig>) { currentMounts = list }
    fun attach(p: ExoPlayer) { player = p }
    fun detach() { player = null }
    fun player(): ExoPlayer? = player
    fun update(p: ExoPlayer) {
        state.value = PlaybackSnapshot(
            isPlaying = p.isPlaying,
            title = p.mediaMetadata.title?.toString().orEmpty(),
            artist = p.mediaMetadata.artist?.toString().orEmpty(),
            albumTitle = p.mediaMetadata.albumTitle?.toString().orEmpty(),
            artUri = p.mediaMetadata.artworkUri?.toString(),
            positionMs = p.currentPosition,
            durationMs = p.duration.coerceAtLeast(0L),
            shuffleEnabled = p.shuffleModeEnabled,
            repeatMode = p.repeatMode,
            mediaId = p.currentMediaItem?.mediaId,
        )
    }
}

data class PlaybackSnapshot(
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val albumTitle: String = "",
    val artUri: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val mediaId: String? = null,
)
