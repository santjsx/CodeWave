# ==============================================================================
# CODEWAVE — SYSTEM CONTEXT & ARCHITECTURE REFERENCE
# ==============================================================================

> **Project Name**: CodeWave  
> **Package**: `com.codewave.player`  
> **Active Baseline**: v1.4.1 (Pure Hi-Res Studio Audio Workstation)  
> **Target Platform**: Android (minSdk 26, targetSdk 35, compileSdk 36)  

---

## 🛠️ 1. EXACT TECH STACK & DEPENDENCIES

| Layer | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin | `2.3.20` | Core application logic, Coroutines & Flow |
| **Java Toolchain** | OpenJDK JVM | `17` | Compilation target |
| **Build Tooling** | AGP & KSP | `9.0.1` / `2.3.12` | Gradle build orchestration & Room code-gen |
| **UI Framework** | Jetpack Compose | BOM `2026.03.01` | Declarative UI, Hardware-accelerated Canvas |
| **Design System** | Material 3 & Custom Tokens | In-tree (`CWColors`, `CWTypography`, `CWShapes`) | Cyber-studio DAW tactile visual language |
| **Audio Engine** | Jetpack Media3 ExoPlayer | `1.5.1` | Low-latency audio pipeline, gapless, ReplayGain |
| **Media Session** | Media3 `MediaSessionService` | `1.5.1` | Background service, lockscreen, notification controls |
| **Hardware DSP** | Android `DynamicsProcessing` | Native Android O+ | 10-band parametric EQ, Preamp gain, Limiter |
| **Local Database** | Room Database | `2.8.5` (KSP) | Local music library cache, playlists, metadata |
| **Key-Value Store** | DataStore Preferences | `1.1.2` | EQ presets, theme choice, audio settings |
| **Image Loading** | Coil Compose | `2.7.0` | Async album artwork loading from MediaStore URIs |
| **Network Client** | Square OkHttp | `4.12.0` | In-app OTA GitHub release updater |
| **Testing** | JUnit 4 + Coroutines Test | `4.13.2` / `1.10.2` | Unit & repository state verification |

---

## 🏗️ 2. MAJOR ARCHITECTURAL PATTERNS

### Manual Dependency Injection (`AppContainer.kt`)
CodeWave uses a clean, zero-reflection, compile-time manual dependency injection container initialized in `CodeWaveApplication.kt`:
```
CodeWaveApplication
 └── AppContainerImpl
      ├── CodeWaveDatabase (Room: tracks, albums, artists, playlists)
      ├── AudioScanner & MetadataExtractor
      ├── LibraryRepository
      ├── PlaybackRepository (ExoPlayer + DynamicsProcessing)
      ├── EqualizerRepository (DataStore + DSP sync)
      ├── SettingsRepository (DataStore)
      └── OtaUpdateManager (OkHttp GitHub Releases API)
```

### State Management & Reactive Data Flow
- **Unidirectional Data Flow (UDF)**: ViewModels expose immutable `StateFlow<UiState>`, collected in Compose using `.collectAsState()`.
- **Coroutines & Dispatchers**:
  - `Dispatchers.Main`: UI events and Compose animations.
  - `Dispatchers.IO`: MediaStore scanning, database read/writes, LRC parsing, OTA downloads.
  - Audio rendering operates asynchronously through ExoPlayer's dedicated internal playback thread.

### Navigation Hierarchy (`Screen.kt`)
Navigation uses modern AndroidX Navigation (`Screen.kt` sealed hierarchy) embedded in `CodeWaveApp.kt`:
- `Screen.Home`: Track collection, recently added, quick favorites.
- `Screen.Playlists`: Custom user-created playlists and smart collections.
- `Screen.Search`: Real-time indexed local search by title, artist, album, and format.
- `Screen.Settings`: Themes, crossfade, gapless, and OTA update center.
- **Global Sheets & Overlays**:
  - `NowPlayingScreen`: Expanded player, scrub bar, seek, queue, hardware telemetry.
  - `EqualizerSheet`: 10-band hardware graphic EQ faders + Preamp gain slider.
  - `TrackInspectorDrawer`: Audio bit depth, sample rate, codec format, and file specs.
  - `OtaUpdateDialog`: Modal download & install flow with weighted non-squeezing cards.

---

## 🔒 3. "DO NOT TOUCH / WORKING FEATURES" DIRECTORY REFERENCE

The following components are battle-tested, verified, and strictly protected from refactoring:

### ⛔ `core/audio/` (Hardware DSP Engine)
- **Files**: `DspEngine.kt`
- **Function**: Manages Android's `DynamicsProcessing` audio session, 10-band EQ channel configurations (31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz), Preamp Master Gain, and bit-perfect fallback mode.
- **Rule**: **DO NOT MODIFY** band frequencies, session attach logic, or DSP allocation.

### ⛔ `core/media/` (ExoPlayer & Background Media Service)
- **Files**: `CodeWaveMediaSessionService.kt`, `AudioFocusManager.kt`, `AudioBecomingNoisyReceiver.kt`, `LrcParser.kt`
- **Function**: Audio playback engine, media notification session, lockscreen scrubber, headset unplug handlers, and local LRC lyrics line-by-line sync.
- **Rule**: **DO NOT BREAK** media notification lifecycle, audio focus ducking/pause protocols, or gapless audio sink configurations.

### ⛔ `core/database/` (Room Persistence & Downgrade Safety)
- **Files**: `CodeWaveDatabase.kt`, `dao/TrackDao.kt`, `dao/PlaylistDao.kt`, `entity/*`
- **Function**: SQLite database at `version = 1`.
- **Rule**: Must always retain `.fallbackToDestructiveMigrationOnDowngrade()`. **DO NOT INTRODUCE BREAKING SCHEMA CHANGES** without backwards compatibility.

### ⛔ `core/scanner/` (Async Local Media Scanner)
- **Files**: `AudioScanner.kt`, `MetadataExtractor.kt`
- **Function**: Scans device audio via `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`, extracts sample rate, bit depth, format mime type, and embedded album art URIs.
- **Rule**: Must remain non-blocking, non-leaking, and memory-efficient.

### ⛔ `core/designsystem/` (Studio UI Design Tokens)
- **Files**: `CWColors.kt`, `CWTypography.kt`, `CWShapes.kt`, `CWButton.kt`, `CWTechnicalBadge.kt`
- **Function**: Central design system tokens for Obsidian, Cyberpunk, and Studio dark themes.
- **Rule**: Do not hardcode arbitrary raw hex colors into feature screens. Always draw from `CWColors`.

### ⛔ `core/ota/` (In-App GitHub Release Updater)
- **Files**: `OtaUpdateManager.kt`, `ui/ota/OtaUpdateDialog.kt`
- **Function**: Queries GitHub `releases/latest`, displays release notes, downloads APK to cache, and launches `FileProvider` package installer.
- **Rule**: Always keep `Cache-Control: no-cache` headers and ensure flexible `weight(1f)` text layouts to prevent horizontal text squeezing.

---

## 🚫 4. FORBIDDEN ACTIONS & ANTI-PATTERNS

1. **NO NETWORK AUDIO SCRAPERS**: Never re-add unauthenticated third-party streaming scrapers or unofficial APIs. CodeWave is built as a pure, high-performance offline local player.
2. **NO FULL-FILE REWRITES**: Always perform localized, surgical replacements.
3. **NO GLOBAL STATE SHIFT**: Do not introduce external state libraries (MVI frameworks, Redux, Hilt). Keep manual DI via `AppContainer`.
4. **NO MAIN-THREAD IO**: Media scanning, database operations, and file I/O must always run on `Dispatchers.IO`.
