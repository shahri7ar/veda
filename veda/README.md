# VibeMusic — Offline Android Music Player (ViMusic-Inspired UI)

A **core offline music player** for Android with **SMB/FTP streaming as optional modules**, featuring a faithful **ViMusic-style dark UI** built with Jetpack Compose.

## Architecture

```
Core (Offline Music Player)
├── Local device music scanner (MediaStore)
├── Library management (songs, albums, artists, playlists)
├── Favorites, play history, play counts
├── Full playback (Media3/ExoPlayer)
├── Now Playing with immersive UI
└── Home-screen widget

Optional Modules
├── SMB/CIFS mounts (smbj)
├── FTP mounts (Apache Commons Net)
└── Transparent caching for streamed content
```

## 🎨 ViMusic-Inspired UI

- **Vertical Navigation Rail** (left side): Quick Picks, Songs, Albums, Artists, Playlists, Mounts, Settings
- **Deep Ink Dark Theme** (#0D0D14 background, lilac/amber/teal accents)
- **Mini Player Bar** at bottom when track is playing (slides up/down)
- **Full-Screen Now Playing** with large album art, progress slider, shuffle/repeat
- **Quick Picks** home with recently added, related albums, favorites
- **Album/Artist drill-down** with track listing
- **Search** in Songs tab
- **Playlist management** with create/add/remove

## Screens

| Tab | Description |
|-----|-------------|
| Quick Picks | Home — shuffle, recent tracks, related albums, favorites |
| Songs | All tracks with search/filter |
| Albums | Album grid → album detail with track list |
| Artists | Artist list → artist detail with tracks |
| Playlists | Create/manage playlists with track management |
| Mounts | Add/edit SMB/FTP network mounts (modular) |
| Settings | Cache management, rescan device |

## Download

[VibeMusic v2.0.0 APK](https://app.zaro.ai/api/files/download?fid=766ffece-9e2c-4a49-9ab6-807caad8b054&exp=1781983154&sig=OWG2Wfdg7tKW0M5yv6EuaQ)

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Media3/ExoPlayer for playback
- Room Database for metadata
- Coil for album art
- SMBJ + Apache Commons Net for network sources
- EncryptedSharedPreferences for credentials

## How to Use

1. **Install** the APK on Android 8.0+
2. App auto-scans your device for local music on first launch
3. Browse **Quick Picks**, **Songs**, **Albums**, or **Artists**
4. Tap any track to play — mini player appears at bottom
5. Tap the mini player for **full-screen Now Playing**
6. Go to **Mounts** → **+** to add SMB/FTP servers for streaming
7. Streamed music is auto-cached for offline playback
