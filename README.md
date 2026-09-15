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

### Build Production Release APK
```bash
./gradlew assembleRelease
```
The optimized, minified (R8), and signed release APK will be located at:
```
release/v1.0.0/codewave-v1.0.0-release.apk
app/build/outputs/apk/release/app-release.apk
```

### Release Integrity & Verification (v1.0.0)
- **Min SDK**: API 26 (Android 8.0) · **Target SDK**: API 35 (Android 15)
- **Size**: `4.25 MB` (4,254,377 bytes — 83% reduction via R8 & resource shrinking)
- **Signatures**: Verified APK Signature Scheme v2 & Scheme v3
- **Signer SHA-256**: `9dac2bf05606ca9c9408debabb90d1b81ecdd45c1b55c57b18e4b08ca10a81db`
- **Package SHA-256**:
  ```
  57819a22b5d6c69b246501a95a69aaa4ef3bc298de470b25b3d3696c8d73cf8c
  ```

---

## License

All rights reserved. Designed and developed with care.
