# CodeWave — System Context & Architectural Blueprint

> **Document Purpose**: Single Source of Truth (SSOT) describing the exact architecture, technology stack, directory layout, and safe-zone boundaries for the **CodeWave** codebase. Every AI agent and developer must reference this file before proposing or executing code modifications.

---

## 1. Project Overview & Philosophy

**CodeWave** is an industry-grade, offline-first Hi-Res audio workstation and music player for Android. It marries a developer-centric **Obsidian IDE aesthetic** (dark slate backgrounds, cyan/neon telemetry, physical console faders) with audiophile-grade audio fidelity and zero placebo transparency.

- **Offline-First & Privacy**: 100% offline. Zero accounts, zero analytics, zero advertising, zero cloud database dependencies. Internet permission is utilized strictly for optional, user-triggered GitHub Releases OTA update checks.
- **Audio Transparency**: Unfiltered technical audio insight via the **Track Inspector** (revealing decoded source format, bit depth, container specs vs. active Android hardware output routing and sample rate).
- **Studio DSP Engine**: 32-bit Float `DynamicsProcessing` architecture with 10-band studio graphic EQ, calibrated bipolar master preamp gain (`-12 dB` to `+12 dB`), and transparent limiter anti-clipping guard.

---

## 2. Exact Tech Stack & Dependency Matrix

All dependencies and plugins are managed through Gradle Version Catalog (`gradle/libs.versions.toml`) and Kotlin DSL (`build.gradle.kts`):

| Layer | Technology | Version | Notes / Reference |
|---|---|---|---|
| **Language** | Kotlin | `2.3.20` | JVM Toolchain 17 |
| **Build System** | Gradle / AGP | `9.0.1` | Android Application Plugin, Kotlin DSL |
| **Android Targets** | SDK | `minSdk = 26`, `targetSdk = 35`, `compileSdk = 36` | Android 8.0 Oreo through Android 15+ |
| **UI Framework** | Jetpack Compose | BOM `2026.03.01` | Material 3, Material Icons Extended |
| **Compiler Plugin** | Compose Compiler | Plugin `2.3.20` | Official Kotlin Compose plugin |
| **Audio Engine** | AndroidX Media3 | `1.5.1` | ExoPlayer, MediaSession, MediaSessionService |
| **DSP Framework** | Android AudioFX | Native API 28+ | `DynamicsProcessing` (Float32) + `Equalizer` fallback |
| **Database** | Room + SQLite FTS4 | `2.8.5` | KSP compiler `2.3.12`, Room KTX coroutines |
| **Key-Value Store** | DataStore Preferences | `1.1.2` | Persistent user preferences & themes |
| **Image Loading** | Coil Compose | `2.7.0` | Custom bounded memory (20%) & disk (128MB) cache |
| **HTTP / OTA** | OkHttp | `4.12.0` | Resilient chunked APK downloads from GitHub Releases |
| **Coroutines** | Kotlinx Coroutines | `1.10.2` | Android & Test modules |

---

## 3. Architecture & Design Patterns

### 3.1 Unidirectional Data Flow (UDF) & State Management
- **State Holders**: Android Architecture Components `ViewModel` holding immutable data models in Kotlin `StateFlow`.
- **State Consumption**: UI consumes state via `collectAsState()` / `collectAsStateWithLifecycle()` in Composable roots.
- **Atomic Mutations**: StateFlow updates utilize `_state.update { current -> current.copy(...) }` to eliminate race conditions.
- **Concurrency**: Operations bound to structured scopes (`viewModelScope`, `applicationScope`, `CoroutineScope(Dispatchers.IO)` for database/file scanning).

### 3.2 Dependency Injection: Pure Kotlin Container Pattern
- **No Dagger/Hilt or Koin**: Avoids annotation-processing overhead and keeps APK footprint minimal (~4.4 MB).
- **`AppContainer` (`core/data/AppContainer.kt`)**: Interface defining singletons for `database`, `audioScanner`, `libraryRepository`, `equalizerRepository`, `settingsRepository`, `playbackRepository`, and `otaUpdateManager`.
- **`DefaultAppContainer`**: Lazy-instantiated singletons hosted inside `CodeWaveApplication.instance.container`.
- **ViewModel Factories**: ViewModels define companion factories (e.g., `HomeViewModel.provideFactory(...)`) taking repositories directly from `AppContainer`.

### 3.3 Routing & Presentation Architecture
- **Host**: `MainActivity` initializes edge-to-edge styling, permissions, and hosts `CodeWaveApp`.
- **Top-Level Navigation**: State-driven navigation via `Screen` sealed class (`Home`, `Library`, `Search`, `Playlists`, `Equalizer`, `Settings`) in `ui/CodeWaveApp.kt`.
- **Overlays & Dialogs**: Bottom sheets and full-screen modes are handled as layered Composable sheets (`NowPlayingScreen`, `TrackInspectorSheet`, `TrackActionMenuSheet`, `AddToPlaylistSheet`, `CollectionDetailSheet`, `ThemeSelectorSheet`, `OtaUpdateDialog`) coordinated with `BackHandler`.

---

## 4. Directory Structure Map

```
app/src/main/
├── AndroidManifest.xml                        # Permissions, Foreground Service, FileProvider
├── java/com/codewave/player/
│   ├── CodeWaveApplication.kt                 # Application class, Coil cache sizing, Container holder
│   ├── MainActivity.kt                        # Edge-to-edge entry point, permissions, theme host
│   ├── core/
│   │   ├── audio/
│   │   │   └── DspEngine.kt                   # 10-band DynamicsProcessing & Limiter DSP engine
│   │   ├── data/
│   │   │   ├── AppContainer.kt                # Pure Kotlin DI interface & DefaultAppContainer
│   │   │   ├── EqualizerRepository.kt         # EQ presets, band gains, preamp configuration
│   │   │   ├── LibraryRepository.kt           # Library tracks, albums, artists, smart collections
│   │   │   ├── PlaybackRepository.kt          # MediaController connection, queue, playback state
│   │   │   └── SettingsRepository.kt          # DataStore settings, active theme, scan folders
│   │   ├── database/
│   │   │   ├── CodeWaveDatabase.kt            # Room database with SQLite FTS4 integration
│   │   │   ├── dao/Daos.kt                    # TrackDao, PlaylistDao, EqPresetDao
│   │   │   └── entity/Entities.kt             # TrackEntity, PlaylistEntity, EqPresetEntity
│   │   ├── designsystem/
│   │   │   ├── component/                     # CWMiniPlayer, CWVerticalFader, TrackInspectorSheet...
│   │   │   └── theme/                         # CWColors (Obsidian palette), CWTypography, CWTheme
│   │   ├── media/
│   │   │   ├── AudioBecomingNoisyReceiver.kt  # Auto-pause on headphone disconnect
│   │   │   ├── AudioFocusManager.kt           # Transient / permanent audio focus handling
│   │   │   ├── CodeWaveMediaSessionService.kt # Media3 Foreground Service for background audio
│   │   │   └── LrcParser.kt                   # Synchronized / unsynchronized lyrics parsing
│   │   ├── model/                             # Domain models: Track, Album, Artist, EqualizerConfig...
│   │   ├── ota/
│   │   │   └── OtaUpdateManager.kt            # GitHub Releases sideload updates & APK verification
│   │   └── scanner/
│   │       ├── AudioFingerprint.kt            # Unique audio identification surviving renames
│   │       ├── AudioScanner.kt                # MediaStore incremental scanner + ContentObserver
│   │       └── MetadataExtractor.kt           # High-precision ID3/FLAC/Vorbis metadata parser
│   └── ui/
│       ├── CodeWaveApp.kt                     # Root Scaffold, NavigationBar, Sheet orchestrator
│       ├── collection/                        # CollectionDetailSheet, TrackActionMenuSheet
│       ├── equalizer/                         # EqualizerScreen (DSP console, 10-band sliders)
│       ├── home/                              # HomeScreen, HomeViewModel (Smart collections)
│       ├── library/                           # LibraryScreen, LibraryViewModel (Tabs & filter chips)
│       ├── navigation/                        # Screen sealed class definitions
│       ├── ota/                               # OtaUpdateDialog (Progress, changelog, installation)
│       ├── player/                            # NowPlayingScreen, LyricsView, AudioQualityExplainer
│       ├── playlists/                         # PlaylistsScreen, AddToPlaylistSheet, Vitrine shelves
│       ├── search/                            # SearchScreen, SearchViewModel (SQLite FTS4)
│       └── settings/                          # SettingsScreen, SettingsViewModel, ThemeSelectorSheet
```

---

## 5. DO NOT TOUCH / Working Core Features (Safe Zones)

The following components represent fully stabilized, battle-tested core systems. **DO NOT refactor, rename, or re-architect these modules without explicit, authorized instructions**:

### 🛡️ Safe Zone 1: `core/audio/DspEngine.kt`
- **Why**: Implements low-level Android `DynamicsProcessing` with precise frequency resolution (`VARIANT_FAVOR_FREQUENCY_RESOLUTION`), disabled multiband compressor to prevent distortion, channel gain staging, and 10-band calibrated EQ filters.
- **Risk of touching**: Audio distortion, digital clipping, crashes on OEM devices with non-standard audio HALs, or broken session attachments.

### 🛡️ Safe Zone 2: `core/media/` (`CodeWaveMediaSessionService.kt`, `AudioFocusManager.kt`, `AudioBecomingNoisyReceiver.kt`)
- **Why**: Handles Android 8 to 15 foreground service lifecycle, lockscreen notification media styling, audio focus interruptions (phone calls, navigation prompts), and headphone disconnect broadcasts.
- **Risk of touching**: Silent background service termination by Android OS, missing media notifications, playback audio overlap during phone calls.

### 🛡️ Safe Zone 3: `core/data/PlaybackRepository.kt` & `AppContainer.kt`
- **Why**: Orchestrates asynchronous connection to `MediaController` via `ListenableFuture`, maintains queue order, manages sleep timers, and acts as the central DI container for the entire app.
- **Risk of touching**: Broken controller connection, deadlocks between UI and Service, memory leaks, broken DI tree across all ViewModels.

### 🛡️ Safe Zone 4: `core/database/` (`CodeWaveDatabase.kt`, `dao/Daos.kt`, `entity/Entities.kt`)
- **Why**: Contains Room schema definitions, SQLite FTS4 virtual tables for sub-millisecond search across tens of thousands of tracks, and custom JSON type converters.
- **Risk of touching**: Database migration crashes (`IllegalStateException: Room cannot verify data integrity`), corrupted FTS4 indexes, data loss of custom user playlists and EQ presets.

### 🛡️ Safe Zone 5: `core/scanner/` (`AudioScanner.kt`, `MetadataExtractor.kt`, `AudioFingerprint.kt`)
- **Why**: Incremental MediaStore crawler with `ContentObserver` that efficiently reconciles local storage changes, parses embedded artwork URIs, extracts bit depth / sample rate, and calculates persistent fingerprints without OutOfMemory crashes.
- **Risk of touching**: Severe UI thread stutter during scans, duplicate track entries, missing file detection on modern scoped storage.

### 🛡️ Safe Zone 6: `core/designsystem/theme/CWColors.kt` & Component Math
- **Why**: Houses the Obsidian color palette, glassmorphism surface tokens, and custom UI components like `CWVerticalFader` (containing precision touch gesture math and bipolar zero-center snap logic).
- **Risk of touching**: Visual degradation of the developer aesthetic, erratic fader drag behaviors, broken contrast in dark mode.

---

## 6. Developer & AI Operational Guidelines

1. **Before any code change**: Read the specific file and its dependencies thoroughly.
2. **Never introduce unvetted libraries**: Check `gradle/libs.versions.toml` before referencing any class.
3. **No whole-file replaces**: Provide surgical edits targeting specific methods or blocks.
4. **Follow the Obsidian Design System**: Always use `CWColors`, `CWTypography`, and existing reusable components (`core/designsystem/component`).
5. **Verify with Gradle**: Confirm changes do not break Kotlin compilation:
   ```bash
   ./gradlew assembleDebug
   ```
