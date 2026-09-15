# CodeWave v1.2.0 Release Notes

**Release Date:** September 16, 2026  
**Package:** `com.codewave.player`  
**Binary:** `release/v1.2.0/CodeWave-v1.2.0.apk`  
**Size:** 4.34 MB (R8 minified & shrinkResources enabled)  
**SHA-256:** `C9590FDDDE0A9D581F2FD1D2480A03A2A828D7535F5D0E0C6932A9702D6DA76C`

---

## 🚀 What's New in v1.2.0

### 1. 🔄 Non-Intrusive OTA GitHub Update Checks Fixed
- **Fixed 404 Endpoint**: Corrected the release endpoint to `santjsx/CodeWave` with proper `User-Agent: CodeWave-App` header required by the GitHub API.
- **Semver-Aware Comparison**: Accurate version comparison preventing false alarms.
- **One-Tap Update Action**: Added direct "Get Update" action button in Settings using system URI handler.

### 2. 📜 Embedded & Multi-Format Lyrics Engine
- **ID3v2 Tag Extraction**: Binary scanning of embedded `USLT` (unsynchronized), `SYLT` (synchronized), and `COMM` (comment) frames from MP3 and WAV files.
- **FLAC & Vorbis Comments**: Deep extraction of `LYRICS`, `UNSYNCEDLYRICS`, and `SYNCEDLYRICS` blocks.
- **MP4 / M4A / AAC**: Native decoding of the `©lyr` atom.
- **Case-Insensitive External Search**: Automatic matching for `.lrc`, `.LRC`, `.txt`, `.TXT` in both the track folder and adjacent `lyrics/` subdirectories.

### 3. 💻 Terminal-Style Lyrics Window
- **Hacker Aesthetic**: Retro-futuristic terminal window bar with macOS/Unix dots (`● ● ●`) and `LYRICS_STREAM :: TTY0` header.
- **Monospaced Telemetry**: Dynamic prompt indicator (`❯`) and animated blinking block cursor (`▋`) tracking the current active line.
- **Touch Event Isolation**: Absorbs background touches to prevent taps on empty space from leaking down to underlying track lists.

### 4. 🛡️ Accidental Touch & Random Equalizer Opening Prevention
- **Dynamic Bottom Bar Hiding**: Bottom navigation bar (`Home`, `Library`, `Search`, `Playlists`, `DSP / EQ`) automatically hides when Now Playing expands fullscreen, eliminating palm brushes against the `DSP / EQ` tab.
- **Click Event Bubbling Barrier**: Non-propagating touch barriers prevent accidental song skips when interacting with player controls or lyrics.

### 5. 📍 "Continue Playing" Exact Millisecond Location Memory
- **Millisecond Precision**: Persists both `track_id` and exact playback position in DataStore every 1.5s and upon pause.
- **Interactive Badge**: Displays `RESUME AT mm:ss` on the Home screen Hero card.
- **Instant Seek**: Tapping "Continue Playing" immediately restores playback to the exact timestamp where you left off.

### 6. ✨ Emil Kowalski Tactile Micro-Interactions
- **Physical Spring Feedback**: High-agency `Modifier.tactilePress(scale = 0.96f)` with punchy 120ms `FastOutSlowInEasing`.
- **System-Wide Polish**: Tactile response integrated across buttons, auxiliary chips (`Speed`, `Timer`, `Queue`), player transport buttons, album cards, and dialogs.

### 7. 🎛️ Equalizer & Player Controls Breathing Room
- **Balanced Auxiliary Row**: Aligned `Speed`, `Sleep Timer`, and `Queue` chips into equal `weight(1f)` columns with uniform 44dp height.
- **Refined Vertical Rhythm**: Increased breathing space above and below the auxiliary row for relaxed one-handed control.

### 8. 🖼️ Album Artwork in List View
- **Dual Display Modes**: Grid mode cards complemented by 52dp rounded artwork thumbnails in List mode.
- **Rich Metadata & Badges**: Album art accompanied by artist names, track counts, and Hi-Res / Lossless indicators.

### 9. 💖 Favorites Consolidated Inside Favorites List
- **Clean Playlists Hub**: Removed raw track rows dumped on the outer Playlists screen.
- **Dedicated Drill-Down**: Favorites Hero Card opens the complete `CollectionDetailSheet` with Play All, Shuffle, and full track inspector.

### 10. 📊 Minimal & Clean Audio Specs Dialog
- **Streamlined 2x3 Telemetry Grid**: Replaced dense textbook paragraphs with a minimal technical sheet showing Container, Encoding, Sample Rate, Bit Depth, Bitrate, and 32-Bit Float DSP.
- **Zero-Clipping Headroom Badge**: Elegant status indicator showing clean signal processing.
