# CodeWave v1.1.0 Release Notes

**Release Date:** September 16, 2026  
**Package:** `com.codewave.player`  
**Binary:** `release/v1.1.0/CodeWave-v1.1.0.apk`  
**Size:** 4.34 MB (R8 minified & shrinkResources enabled)  
**SHA-256:** `F73E9BA1A8794171651D36883605FD6FC1A0BE756FDB9D392B81152B8A0182D8`

---

## 🚀 Key Improvements & Glitch Fixes

### 1. ⏱️ Sleep Timer Engine & Live Countdown Overhaul
- **Fixed Premature 1m Glitch**: Replaced the faulty `(remainingMs / 60000).coerceAtLeast(1)` clamp that froze the timer at `1m` during the entire last two minutes.
- **Monotonic System Clock**: Transitioned sleep timer calculations to `SystemClock.elapsedRealtime()` with 250ms polling loops, preventing drift during device sleep.
- **Dynamic Dual-Mode Display**:
  - Displays `X min` when remaining time is >= 60s.
  - Transitions to second-by-second countdown `X sec` when under 60 seconds.
  - Active chip glows cyan with live countdown.
- **Prominent Turn Off Action**: Opening the Sleep Timer dialog while active reveals an active stop card showing exact remaining time (`Xm Ys remaining`) and a dedicated `Turn Off Timer` cancellation action.

### 2. 🎤 Real Lyrics Experience & Minimal No-Lyrics State
- **Eliminated Fake Lyrics**: Removed all randomly generated placeholder lines.
- **State Flow**: Explicitly tracks `Loading`, `Synchronized`, `Plain`, `Unavailable`, and `Error`.
- **Apple-Clean Empty State**: When lyrics are not available, presents a clean, centered soundboard tile with track title, artist, and subtle lyrics icon—no error warnings or simulated lines.
- **Real-Time Synchronized LRC**: Loads external `.lrc` and `.txt` files, highlighting the active lyric in bold neon cyan with smooth auto-scrolling and tap-to-seek timestamp navigation.

### 3. ⚡ Playback Speed Preservation
- **Queue & Track Preservation**: Speed selection (`0.5x` - `2.0x`) is now preserved across track skips, queue resets, and app restarts.
- **High-Contrast Single-Line Chip**: Displayed cleanly as `1.0x`, `1.1x`, `1.25x`, etc., with responsive selector dialog.

### 4. 🔘 Button Contrast & `CWPlayAllButton`
- **Dynamic Luminance Calculation**: Added `Color.contentColor()` to calculate WCAG luminance ($0.299R + 0.587G + 0.114B$), ensuring 7:1+ contrast across all 5 themes.
- **Fixed Grey-on-Pink Bug**: Corrected `CWTypography.titleSmall` style inheritance so button text renders in crisp white on Synthwave '84 neon pink buttons.
- **Dedicated Play All & Shuffle Buttons**: Standardized 44dp `CWPlayAllButton` across Library and Collection sheets.

### 5. 🎚️ Scrubber Progress Bar Gesture & Visual Polish
- **Unified Touch Input**: Migrated from dual competing gesture handlers to a single `awaitEachGesture` block (`awaitFirstDown` + `awaitPointerEvent`) for frictionless tap and drag scrubbing.
- **Removed Murky Yellow Halo**: Fixed Synthwave '84 color tokens (`AccentViolet` set to neon `#E056FD`), giving the scrubber thumb a crisp neon pink/violet aura.
- **10Hz Fluid Updates**: Increased polling frequency to 100ms for continuous thumb tracking.

### 6. 🎨 Apple-Style Adaptive App Launcher Icon
- **Vector Waveform Mark**: Created `< ||||| >` (acoustic soundmark enclosed in code brackets) in `ic_launcher_foreground.xml`.
- **Obsidian Soundstage**: Created `ic_launcher_background.xml` with deep dark gradient, central cyan acoustic glow, and concentric sub-bass pulse rings.
- **Multi-Density WebP Mipmaps**: Packaged across `mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, and `xxxhdpi` for pristine sharpness on all Android launcher grids.

### 7. ❤️ Core Glitch Fixes (Retained & Verified)
- Instant Room database & StateFlow favorite toggling.
- Full drill-down collection sheets for Albums, Artists, and Playlists.
- Bottom navigation tabs dismiss Now Playing immediately to prevent navigation traps.
- Automatic library scanning upon app launch and permission grant.
- DAW-grade vertical hardware equalizer faders with ±12 dB detents and master DSP telemetry.
- 5 comprehensive VS Code themes: **Obsidian**, **Synthwave '84**, **Tokyo Night**, **Dracula Pro**, and **Monokai Pro**.
