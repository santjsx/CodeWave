# CodeWave

### Industry-Grade Offline Hi-Res Music Player for Android

**CODEWAVE** is an offline-first local music player engineered for audiophiles and power users who own their music library. Built with a VS-Code-inspired developer workstation aesthetic ("Obsidian" dark theme, technical monospaced telemetry, and the signature Track Inspector), it pairs local library management with Jetpack Media3 ExoPlayer, real hardware DSP/dynamics processing, and uncompromising transparency regarding audio quality.

---

## Key Features

- **Offline-First Local Ownership**: No accounts, no cloud library, no streaming, no ads, and zero analytics. Your music remains strictly on your device.
- **Developer Workstation Aesthetic**: Built with the Obsidian color palette (`#0B0D10`), crisp borders, and technical monospaced telemetry font for sample rates, bit depths, and codecs.
- **Audio Transparency (Never Lie About Quality)**: Factual separation between file **SOURCE** format (`FLAC · 24-bit / 96 kHz`) and actual hardware **OUTPUT** path (`48 kHz · 24-bit · PCM Stereo`).
- **Signature Track Inspector**: Deep-dive technical sheet for any song displaying verified source properties, physical audio routing, latency, and DSP status.
- **10-Band Hardware Dynamics Processing (DSP)**: Powered by Android `DynamicsProcessing` with 10 bands (`31Hz` to `16kHz`), PreEQ gain staging, limiter anti-clipping protection, and fallback to Android `Equalizer`.
- **Fast Incremental Scanner**: Powered by `MediaStore` and continuous `ContentObserver` change detection, with candidate validation to protect against partial downloads, and content fingerprinting to survive file renames and moves.
- **SQLite FTS Full-Text Search**: Instant, debounced Unicode-aware search across tracks, albums, artists, and folders.
- **Audio Interruption Matrix**: Automatic ducking during notifications, pause on phone calls, and instant pause on headphone unplug (`ACTION_AUDIO_BECOMING_NOISY`).
- **Sideload GitHub Release Discovery**: Optional, user-triggered update check querying GitHub Releases with SHA-256 hash verification.

---

## Architecture

Built using Clean Architecture and Android modern guidelines:

```
┌────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                   │
│   Jetpack Compose · Material 3 · Obsidian Theme Tokens │
│   Screens: Home · Library · Player · Search · EQ       │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                      DOMAIN LAYER                      │
│   Models: Track · Album · Artist · Playlist · AudioSpec│
└─────────────┬────────────────────────────┬─────────────┘
              │                            │
┌─────────────▼──────────────┐ ┌───────────▼─────────────┐
│         DATA LAYER         │ │      PLATFORM & AUDIO   │
│  Room DB (SQLite + FTS)    │ │  Media3 ExoPlayer       │
│  MediaStore Scanner        │ │  MediaSessionService    │
│  DataStore Preferences     │ │  DspEngine (10-Band EQ) │
│  Repositories              │ │  AudioFocusManager      │
└────────────────────────────┘ └─────────────────────────┘
```

---

## Tech Stack

- **Language**: Kotlin 2.3
- **Build System**: Gradle 9.1 with Kotlin DSL & Version Catalog (`libs.versions.toml`)
- **UI Toolkit**: Jetpack Compose BOM 2026.03 + Material 3
- **Audio Engine**: AndroidX Media3 ExoPlayer 1.5.1 + `MediaSessionService`
- **Database**: Room 2.8.5 with KSP (Kotlin Symbol Processing) + SQLite FTS4
- **Image Loading**: Coil 2.7.0 (with bounded 20% memory cache & 128MB disk cache)
- **Preferences**: Jetpack DataStore Preferences

---

## Building & Testing

### Prerequisites
- JDK 17 or JDK 21 (e.g. Amazon Corretto 21)
- Android SDK (API 26 to API 35)

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## License

All rights reserved. Designed and developed with care.
