# CodeWave v1.3.0 Release Notes

**Release Version**: 1.3.0  
**Build Number**: 4  
**Date**: September 16, 2026  
**Status**: Stable  

---

### Highlights & Key Improvements

#### 1. Enhanced LRC Normalization & Synchronization Overhaul
- **Enhanced Word-Level Tag Stripping**: Resolved display corruptions where karaoke timestamps (`<00:01.960>word <00:02.895>`) leaked into lyric lines (e.g., *O My Brotheru*). Cleanly normalizes lines into fluent, pristine lyrics.
- **Intro Instrumental Desync Fix**: Resolved premature line activation during introductory guitar/instrumental passages (e.g., 0-28s in *Thakadimithom*). The active index now stays inactive until the track actually reaches the first timestamp.
- **Global Offset Support**: Implemented `[offset: +/-ms]` header parsing in `LrcParser` to automatically calibrate synced lyrics timing.
- **Redesigned Minimal No-Lyrics State**: Replaced cluttered pseudo-command dumps with a sleek, centered graphic equalizer card and clean status message.

#### 2. Full Playlist Management (Rename & Delete)
- **Rename Playlists**: Easily rename custom playlists directly from the playlist item menu (3-dots) or from within the playlist detail sheet.
- **Delete Playlists**: Added safe deletion with an explicit confirmation dialog that frees playlist associations while safeguarding user audio files.
- **Room Database DAOs**: Implemented transactional DAO queries (`renamePlaylist`, `deletePlaylistById`, `clearPlaylistTracks`) in `PlaylistDao` and `LibraryRepository`.

#### 3. Instant Playback Controls on Cold Relaunch
- Fixed cold-start controller synchronization: preloads restored last-played track and seek position into the `MediaController` so tapping Play or Seek works immediately without requiring track re-selection.

#### 4. New Premium App Icon: Avatar with Wired Earphones
- Brand new adaptive and high-density launcher icons featuring a sleek, minimalist human avatar silhouette wearing in-ear wired earphones with elegant flowing cables and cyber-cyan/magenta acoustic wave aura.
- Fully optimized across all Android launcher densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).

#### 5. Restored Persistent Bottom Navbar in Now Playing
- The bottom navigation bar now remains accessible while viewing the Now Playing screen, enabling instant one-tap navigation to Library, Playlists, Search, and Settings.

---

### Package Details
- **File**: `CodeWave-v1.3.0.apk`
- **File Size**: `4,376,073 bytes (4.17 MB)`
- **SHA-256**: `8932dfd77848f96c087f2f6434083b8ea32683cacc58ee27d3ea801f573c0f98`
