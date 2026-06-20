package com.vibemusic.app.ui

import android.app.Application
import android.media.audiofx.Equalizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.vibemusic.app.VibeMusicApp
import com.vibemusic.app.data.MusicRepository
import com.vibemusic.app.data.bookmarks.BookmarkEntity
import com.vibemusic.app.data.model.Album
import com.vibemusic.app.data.model.Artist
import com.vibemusic.app.data.model.MountConfig
import com.vibemusic.app.data.model.Playlist
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.playback.PlayerHub
import com.vibemusic.app.ui.screens.sleep.SleepTimerManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Unified ViewModel — drives all root screens (QuickPicks, Songs, Playlists,
 * Artists, Albums) and detail screens.
 */
class MusicViewModel(
    app: Application,
) : AndroidViewModel(app) {

    private val repo: MusicRepository = MusicRepository()

    // ─── Track flows ─────────────────────────────────────────────────

    /** All tracks (local + network). */
    val allTracks: StateFlow<List<Track>> =
        repo.observeTracks().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Recent / "Quick picks" — most recently added. */
    val recent: StateFlow<List<Track>> =
        repo.observeTracks().map { it.sortedByDescending { t -> t.id.hashCode() }.take(20) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** User-marked favorites. */
    val favorites: StateFlow<List<Track>> =
        repo.observeTracks().map { it.filter { t -> t.isFavorite } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Cached (offline-available) tracks. */
    val cached: StateFlow<List<Track>> =
        repo.observeTracks().map { it.filter { t -> t.cached } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Grouped by album. */
    val albums: StateFlow<List<Album>> =
        repo.observeTracks().map { tracks ->
            tracks.filter { it.album != "Unknown Album" }
                .groupBy { it.albumId }
                .map { (id, ts) ->
                    val first = ts.first()
                    Album(
                        id = id, title = first.album, artist = first.artist,
                        year = first.year, trackCount = ts.size,
                        artUri = first.albumArtUri, sourceType = first.sourceType,
                    )
                }
                .sortedBy { it.title }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Grouped by artist. */
    val artists: StateFlow<List<Artist>> =
        repo.observeTracks().map { tracks ->
            tracks.groupBy { it.artist }
                .map { (name, ts) ->
                    Artist(
                        name = name,
                        trackCount = ts.size,
                        albumCount = ts.map { it.album }.distinct().size,
                        artUri = ts.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                    )
                }
                .sortedBy { it.name }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mounts: StateFlow<List<MountConfig>> =
        repo.observeMounts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<Playlist>> =
        repo.observePlaylists().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playback: StateFlow<com.vibemusic.app.playback.PlaybackSnapshot> = PlayerHub.state

    /** Sleep timer manager — single shared instance per VM. */
    val sleepTimer: SleepTimerManager by lazy { SleepTimerManager(getApplication()) }

    /** Search query state — drives global filtered results. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    fun setSearchQuery(q: String) { _searchQuery.value = q }

    /** Global filtered search results. */
    val searchResults: StateFlow<SearchHits> = combine(
        allTracks, albums, artists, _searchQuery,
    ) { tracks, albs, arts, q ->
        if (q.isBlank()) SearchHits.empty()
        else {
            val lower = q.trim().lowercase()
            SearchHits(
                tracks = tracks.filter {
                    it.title.lowercase().contains(lower) ||
                        it.artist.lowercase().contains(lower) ||
                        it.album.lowercase().contains(lower)
                }.take(30),
                albums = albs.filter {
                    it.title.lowercase().contains(lower) ||
                        it.artist.lowercase().contains(lower)
                }.take(15),
                artists = arts.filter { it.name.lowercase().contains(lower) }.take(15),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, SearchHits.empty())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _snackbar = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val snackbar = _snackbar.asSharedFlow()

    /** Equalizer state — band gains (millibels) and preset name. */
    private val _eqEnabled = MutableStateFlow(false)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()
    private val _eqBands = MutableStateFlow<List<Short>>(emptyList())
    val eqBands: StateFlow<List<Short>> = _eqBands.asStateFlow()
    private val _eqBandFreqs = MutableStateFlow<List<Int>>(emptyList())
    val eqBandFreqs: StateFlow<List<Int>> = _eqBandFreqs.asStateFlow()
    private val _eqRange = MutableStateFlow<Pair<Short, Short>>(Pair(-1500, 1500))
    val eqRange: StateFlow<Pair<Short, Short>> = _eqRange.asStateFlow()
    private val _eqPresets = MutableStateFlow<List<String>>(emptyList())
    val eqPresets: StateFlow<List<String>> = _eqPresets.asStateFlow()
    private var equalizer: Equalizer? = null

    init {
        viewModelScope.launch { mounts.collect { PlayerHub.setMounts(it) } }
    }

    // ─── Per-album / per-artist tracks ────────────────────────────────

    fun albumTracks(albumId: String): StateFlow<List<Track>> =
        repo.observeTracks().map { it.filter { t -> t.albumId == albumId } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun artistTracks(name: String): StateFlow<List<Track>> =
        repo.observeTracks().map { it.filter { t -> t.artist == name } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    suspend fun getPlaylistTracks(pid: Long): List<Track> = repo.getPlaylistTracks(pid)

    // ─── Playback ────────────────────────────────────────────────────

    fun play(track: Track, queue: List<Track>) = playTrack(track, queue)
    fun playTrack(track: Track, queue: List<Track>) {
        val player = PlayerHub.player() ?: return
        val items = queue.map { it.toMediaItem() }
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        player.setMediaItems(items, index, 0L)
        player.prepare()
        player.play()
        ensureEqualizerAttached(player.audioSessionId)
    }

    fun playShuffled(queue: List<Track>) {
        if (queue.isEmpty()) return
        val shuffled = queue.shuffled()
        play(shuffled.first(), shuffled)
    }

    fun playFavorites() = playShuffled(favorites.value)
    fun playCached() = playShuffled(cached.value)

    fun resumeBookmark(bm: BookmarkEntity) {
        val all = allTracks.value
        val target = all.firstOrNull { it.id == bm.trackId } ?: return
        play(target, all)
        viewModelScope.launch {
            // small delay for prepare()
            kotlinx.coroutines.delay(300)
            PlayerHub.player()?.seekTo(bm.positionMs)
        }
    }

    fun togglePP() = togglePlayPause()
    fun togglePlayPause() {
        val p = PlayerHub.player() ?: return
        if (p.isPlaying) p.pause() else p.play()
    }
    fun next() { PlayerHub.player()?.seekToNext() }
    fun prev() { PlayerHub.player()?.seekToPrevious() }
    fun previous() { PlayerHub.player()?.seekToPrevious() }
    fun seek(ms: Long) { PlayerHub.player()?.seekTo(ms) }
    fun seekTo(ms: Long) { PlayerHub.player()?.seekTo(ms) }
    fun toggleShuffle() {
        PlayerHub.player()?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
            PlayerHub.update(it)
        }
    }
    fun toggleRepeat() {
        PlayerHub.player()?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            PlayerHub.update(it)
        }
    }

    // ─── Equalizer ───────────────────────────────────────────────────

    private fun ensureEqualizerAttached(audioSessionId: Int) {
        if (equalizer != null) return
        if (audioSessionId == 0) return
        runCatching {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            val n = eq.numberOfBands.toInt()
            val low = eq.bandLevelRange[0]
            val high = eq.bandLevelRange[1]
            _eqRange.value = Pair(low, high)
            _eqBands.value = (0 until n).map { eq.getBandLevel(it.toShort()) }
            _eqBandFreqs.value = (0 until n).map { eq.getCenterFreq(it.toShort()) }
            _eqPresets.value = (0 until eq.numberOfPresets.toInt())
                .map { eq.getPresetName(it.toShort()) }
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        _eqEnabled.value = enabled
        equalizer?.enabled = enabled
    }

    fun setEqBand(index: Int, levelMillibel: Short) {
        equalizer?.setBandLevel(index.toShort(), levelMillibel)
        _eqBands.value = _eqBands.value.toMutableList().also { it[index] = levelMillibel }
    }

    fun applyEqPreset(presetIndex: Int) {
        val eq = equalizer ?: return
        runCatching {
            eq.usePreset(presetIndex.toShort())
            val n = eq.numberOfBands.toInt()
            _eqBands.value = (0 until n).map { eq.getBandLevel(it.toShort()) }
        }
    }

    // ─── Favorites ────────────────────────────────────────────────────

    fun toggleFavorite(trackId: String) = viewModelScope.launch {
        repo.toggleFavorite(trackId)
    }

    // ─── Mounts ───────────────────────────────────────────────────────

    fun upsertMount(cfg: MountConfig) {
        val withId = if (cfg.id.isBlank()) cfg.copy(id = UUID.randomUUID().toString()) else cfg
        viewModelScope.launch { repo.upsertMount(withId) }
    }
    fun addOrUpdateMount(cfg: MountConfig) = upsertMount(cfg)
    fun deleteMount(id: String) = viewModelScope.launch { repo.deleteMount(id) }
    fun scanMount(cfg: MountConfig) = viewModelScope.launch {
        _busy.value = true
        try { repo.scanMount(cfg) } finally { _busy.value = false }
    }
    fun scan(cfg: MountConfig) = scanMount(cfg)
    suspend fun testMount(cfg: MountConfig) = repo.testMount(cfg)
    suspend fun test(cfg: MountConfig) = repo.testMount(cfg)

    // ─── Local scan ───────────────────────────────────────────────────

    fun rescanLocal() = viewModelScope.launch {
        _busy.value = true
        try {
            val n = repo.scanLocal()
            _snackbar.emit("Found $n local tracks")
        } catch (e: Exception) {
            _snackbar.emit("Error: ${e.message}")
        } finally {
            _busy.value = false
        }
    }

    // ─── Playlists ────────────────────────────────────────────────────

    fun createPlaylist(name: String) = viewModelScope.launch { repo.createPlaylist(name) }
    fun addToPlaylist(pid: Long, tid: String) = viewModelScope.launch {
        repo.addToPlaylist(pid, tid)
    }
    fun removeFromPlaylist(pid: Long, tid: String) = viewModelScope.launch {
        repo.removeFromPlaylist(pid, tid)
    }
    fun deletePlaylist(id: Long) = viewModelScope.launch { repo.deletePlaylist(id) }

    // ─── MediaItem mapper ────────────────────────────────────────────

    private fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri(sourceUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri?.let { android.net.Uri.parse(it) })
                .build()
        )
        .build()

    override fun onCleared() {
        super.onCleared()
        runCatching { equalizer?.release() }
        equalizer = null
        sleepTimer.cancel()
    }
}

data class SearchHits(
    val tracks: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>,
) {
    val isEmpty: Boolean get() = tracks.isEmpty() && albums.isEmpty() && artists.isEmpty()
    val totalCount: Int get() = tracks.size + albums.size + artists.size
    companion object {
        fun empty() = SearchHits(emptyList(), emptyList(), emptyList())
    }
}
