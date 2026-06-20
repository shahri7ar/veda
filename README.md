# Veda v2.0.0 — Offline Android Music Player (ViMusic-Inspired UI)

A **core offline music player** for Android with **SMB/FTP streaming as optional modules**, featuring a faithful **ViMusic-style dark UI** built with Jetpack Compose.

## ✨ What's new in v2.0.0

This release adds the long list of finished features previously stubbed out, and fixes every build-breaking issue in v1.0.0:

### New features
- 🔍 **Global search overlay** — single search across songs, albums, and artists, opened from the search FAB on every screen
- 😴 **Sleep timer wired up** in the Extras menu — duration mode (h:mm) and song-count mode, with active state visible
- 🔖 **Bookmarks** persisted to Room — save any track + position + note, tap to resume from the bookmarked spot
- 🎚 **Equalizer** — hardware EQ via audio session: 5+ adjustable band sliders, vendor presets, on/off switch
- 💾 **Backup & restore** — JSON export of playlists, favorites and play counts; one-tap restore
- 🎵 **Album art everywhere** — Coil-backed `AsyncImage` in QuickPicks, Songs, Albums, Search, MiniPlayer and NowPlaying (was a hardcoded fallback icon before)
- 🎤 **Synced lyrics with active-line highlight** — 22 sp bold on the current line, auto-scroll keeps it centred (uses LRCLIB)
- ⭐️ **Built-in playlists** — `Favorites` and `Cached` cards now open dedicated track lists with shuffle FAB
- 🎯 **Shuffle/repeat reflected in Now Playing** — toggles are bound to the player and tint when active
- 🆙 **Foreground service started correctly on Android 8+** (`startForegroundService` instead of `startService`)

### Build-breaking bugs fixed in v1.0.0
- ❌ Missing `LocalScanner` class → ✅ Implemented (MediaStore scanner)
- ❌ Broken `cancel()` in `MusicService` → ✅ Proper `kotlinx.coroutines.cancel` import
- ❌ `Dispatchers.IO.use {}` in `LrclibApi` → ✅ `withContext(Dispatchers.IO)`
- ❌ `Result.Success`/`Result.Failure` references → ✅ `kotlin.Result.fold(...)`
- ❌ Missing `androidx.security:security-crypto` → ✅ Added (EncryptedSharedPreferences)
- ❌ Missing `androidx.preference:preference-ktx` → ✅ Added (BackupManager)
- ❌ Typo `dependencyResolution` → ✅ `dependencyResolutionManagement`
- ❌ Private `formatDuration` referenced from another package → ✅ Promoted to public utility
- ❌ `FolderScanner.scanHierarchical()` return-type mismatch → ✅ `listOfNotNull(buildTree(...))`

## Architecture

```
Core (Offline Music Player)
├── data/local/LocalScanner.kt          MediaStore audio scanner
├── data/AppDatabase.kt                 Room — tracks, mounts, playlists, queues, bookmarks
├── data/bookmarks/                     Room-backed BookmarkDao + entity
├── data/cache/CacheManager.kt          LRU disk cache for streamed audio
├── data/lyrics/                        LRCLIB client + LRC parser
├── playback/MusicService.kt            Media3 / ExoPlayer foreground service
├── ui/MusicViewModel.kt                Single VM driving every screen
├── ui/screens/*                        Compose screens (ViMusic-faithful)
└── widget/MusicWidgetReceiver.kt       Home-screen widget

Optional Modules
├── data/source/SmbDataSource.kt        SMB/CIFS via smbj
├── data/source/FtpDataSource.kt        FTP via Apache Commons Net
└── playback/NetworkAudioDataSource.kt  Tees network bytes through the cache
```

## 🎨 ViMusic-Inspired UI

- **Vertical rotated tab labels** (left side): Quick picks · Songs · Playlists · Artists · Albums
- **Big 36 sp black title** in the top-right (matches ViMusic exactly)
- **Deep Ink dark theme** — `#0F0F14` background, lilac / amber / teal accents
- **Mini player bar** slides up from the bottom when something is playing
- **Full-screen Now Playing sheet** with album art, slider, shuffle/repeat, lyrics & queue links
- **Action pill chips** ("Enqueue", "Shuffle") under the back button
- **Floating search FAB** in the bottom-right of every root screen → opens global search

## Building the APK

### Option A — locally (Android Studio or command line)
1. Install **JDK 17** and the **Android SDK** (Studio installs both).
2. `export ANDROID_HOME=...` (`~/Library/Android/sdk` on macOS, `~/Android/Sdk` on Linux).
3. From the project root run:
   ```bash
   ./build-local.sh            # debug APK
   ./build-local.sh release    # signed release APK
   ```
4. The APK appears under `app/build/outputs/apk/{debug,release}/`.

### Option B — GitHub Actions (zero local setup)
1. Push this repo to GitHub.
2. The included workflow (`.github/workflows/android.yml`) builds both **debug** and **release** APKs on every push and uploads them as artifacts.
3. Tag a commit with `v2.0.0` (or any `v*` tag) to automatically create a GitHub Release with the APK attached.

### Option C — Android Studio
1. Open the `music-player/` folder in Android Studio Hedgehog (2023.1.1) or newer.
2. Build → Build Bundle(s) / APK(s) → Build APK(s).
3. APK ends up in `app/build/outputs/apk/debug/`.

## How to use

1. Install the APK on Android 8.0+
2. Grant `READ_MEDIA_AUDIO` (Android 13+) or `READ_EXTERNAL_STORAGE` permission
3. App auto-scans on first launch — browse Quick picks, Songs, Albums, Artists
4. Tap any track to play; mini-player slides up from the bottom
5. Tap the mini-player for the full Now Playing sheet
6. Hamburger menu (top-left) reveals: Folders · Network mounts · Sleep timer · Bookmarks · Equalizer · Backup · Settings
7. Add SMB/FTP servers under Network mounts to stream from your NAS — files cache automatically on first play

## Tech Stack

- **Kotlin 1.9.22** + **Jetpack Compose** (BOM 2024.02.00) + **Material 3**
- **Media3 / ExoPlayer 1.2.1** for playback (custom `DataSource` for SMB/FTP)
- **Room 2.6.1** (+ KSP) for the persistence layer
- **Coil 2.5.0** for album art (Compose `AsyncImage`)
- **smbj 0.13.0** + **Apache Commons Net 3.10.0** for network sources
- **EncryptedSharedPreferences** for SMB/FTP credentials
- **LRCLIB** for synced lyrics (via `HttpURLConnection` + `kotlinx.serialization`)

## License
MIT
