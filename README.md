# CodeWave

<div align="center">

### Studio-Grade Hi-Res Music Player & Audio Workstation for Android

[![Latest Release](https://img.shields.io/github/v/release/santjsx/CodeWave?style=flat-square&color=6366f1&label=Release)](https://github.com/santjsx/CodeWave/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--35)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://github.com/santjsx/CodeWave)
[![Language](https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Native DSP](https://img.shields.io/badge/C%2B%2B-20%20%7C%20NDK-00599C?style=flat-square&logo=c%2B%2B&logoColor=white)](https://developer.android.com/ndk)
[![Audio Engine](https://img.shields.io/badge/Engine-Media3%20ExoPlayer%201.5.1-FF6F00?style=flat-square)](https://developer.android.com/media/media3)
[![UI Toolkit](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-gray?style=flat-square)](LICENSE)

<p align="center">
  <b>CodeWave</b> is an offline-first, studio-grade Hi-Res music player and audio workstation engineered for audiophiles, producers, and developers. Built with a developer-workstation aesthetic ("Obsidian" dark theme, monospace telemetry, and the signature Track Inspector), it fuses bit-perfect local playback, a native 32-bit floating-point ViPERFX DSP engine, and seamless online stream exploration into a single cohesive experience.
</p>

[Download Latest Release](https://github.com/santjsx/CodeWave/releases/latest) • [Key Features](#-key-features) • [Why CodeWave?](#-why-codewave) • [Architecture](#-architecture) • [Building & Testing](#-building--testing)

</div>

---

## 💡 Why CodeWave?

In an era dominated by subscription streaming platforms, music listening has become heavily commercialized, compressed, and monitored. Most mobile music players are either ad-riddled, reliant on bloated web wrappers, or apply opaque system equalizers that distort your audio.

**CodeWave was engineered from the ground up to solve these fundamental problems:**

| Challenge with Common Players | The CodeWave Standard |
| :--- | :--- |
| **Track Degradation & Resampling Lies**<br>Many players claim "Hi-Res" playback while silently downsampling audio through the standard Android OS mixer. | **Factual Audio Transparency**<br>CodeWave never lies about audio resolution. The signature Track Inspector displays a verified distinction between the exact file **SOURCE** (`FLAC · 24-bit / 96 kHz`) and the physical **HARDWARE OUTPUT** path (`48 kHz · 24-bit · PCM Stereo`). |
| **Crude System Equalizers**<br>Stock Android `Equalizer` APIs often produce digital clipping, volume jumps, and phase distortion when bands are boosted. | **Native 32-Bit Float ViPERFX DSP Engine**<br>Directly embedded in the Media3 audio pipeline via C++20 NDK. Delivers 10-band equalization, harmonic bass synthesis, exciter clarity, IRS convolution matrix, and calibrated soft limiting. |
| **Tracking, Accounts & Intrusive Ads**<br>Commercial apps continuously harvest listening analytics and interrupt your focus with banners or audio promotions. | **Zero Telemetry & True Ownership**<br>No accounts, no cloud sync, no tracking, and zero ads. Your library, playback metrics, and custom presets stay private and stored locally on your device. |
| **Generic, Cookie-Cutter Interfaces**<br>Lacks technical depth, precision faders, and granular audio telemetry. | **Developer Workstation Aesthetic**<br>Curated Obsidian theme (`#0B0D10`), responsive tactile faders, real-time dB readouts, one-tap default resets, and monospace technical typography. |
| **Fragmented Offline vs. Online**<br>Users are forced to choose between offline local music and streaming libraries. | **Unified Hybrid Ecosystem**<br>Enjoy your local lossless library offline, or explore and discover high-bitrate streams with integrated batch background downloads and automatic ID3 metadata tagging. |

---

## ⚡ Key Features

### 🎚️ 1. Studio Equalizer & ViPERFX DSP Suite
- **10-Band Precision EQ**: Frequency centers ranging from `31 Hz` to `16 kHz` with $\pm 12\text{ dB}$ range, $0.5\text{ dB}$ sub-pixel touch steps, and real-time frequency-response curve rendering.
- **12 Studio Presets**: Factory-calibrated profiles (Acoustic, Bass Boost, Classical, Dance, Deep, Electronic, Hip-Hop, Jazz, Pop, Rock, Vocal Clarity, Flat) with automatic database self-healing and zero duplicate profiles.
- **Dedicated Default Controls**: One-tap `DEFAULT` reset buttons on every individual fader, master preamp gain, dynamic bass, clarity exciter, limiter, and preset selector.
- **ViPER Dynamic Bass**: Bass enhancement with harmonic synthesis ($20\text{ Hz}$–$100\text{ Hz}$), adding clean sub-bass weight without muddying the midrange.
- **ViPER Clarity**: High-frequency presence exciter that restores air, breath, and spatial separation to compressed tracks.
- **IRS Convolver Matrix**: Real-time impulse response convolution engine capable of loading acoustic models and studio hardware characteristics.
- **Soft Limiter Anti-Clipping Guard**: Mathematically calibrated dynamic headroom curve that prevents digital inter-sample clipping while maintaining full dynamic volume.

### 🔍 2. Audio Transparency & Track Inspector
- **Verified Audio Telemetry**: Instant breakdown of codec, container, sample rate, bit depth, bit rate, channel layout, and lossless classification.
- **Signal Path Inspection**: Real-time visual route tracing from source file decoding through the DSP processing chain to physical output (USB DAC, Bluetooth LDAC/aptX, 3.5mm Headphone Jack, or Internal Speaker).

### 🌐 3. Online Stream Explore & Offline Downloader
- **Curated Exploration**: Search and browse trending tracks, charts, moods, and genres powered by InnerTube integration.
- **High-Bitrate Direct Playback**: Smooth, adaptive streaming with zero interstitial interruptions.
- **Background Downloader**: Download tracks directly into your local library with automatic cover art fetching, artist attribution, and format tagging.

### 📁 4. Local Library Management & Instant Search
- **Incremental Fast Scanner**: Fast local storage scanning backed by Android `MediaStore` with `ContentObserver` change detection and file fingerprinting to survive renames or directory reorganizations.
- **SQLite FTS4 Full-Text Search**: Instant, debounced Unicode-aware search across titles, artists, albums, and folder paths.
- **Smart Filtering**: Fast one-tap filters for **Lossless (FLAC/WAV/ALAC)**, **Hi-Res Audio (24-bit/96kHz+)**, Favorites, and Recently Added tracks.

### 🎧 5. Robust Background Engine & Interruption Handling
- **Jetpack Media3 ExoPlayer Architecture**: Operates inside a foreground `MediaSessionService` ensuring flawless background playback without OS battery-killer termination.
- **Audio Interruption Matrix**: Automatic audio ducking during GPS/voice notifications, seamless pause on phone calls, and instant safety pause on headphone disconnection (`ACTION_AUDIO_BECOMING_NOISY`).
- **Lock Screen & Notification Controls**: Complete media controls with dynamic palette-tinted notification art and playback progress.

---

## 🏛️ Architecture

CodeWave is architected following Clean Architecture and modern Android development guidelines:

```
┌────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                           │
│     Jetpack Compose · Material 3 · Obsidian Theme Design System        │
│     Screens: Home · Library · Player · Search · EQ Console · Stream    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                              DOMAIN LAYER                              │
│       Models: Track · Album · Artist · Playlist · EQPreset · AudioSpec │
│       Repository Contracts & State Flows                               │
└───────────────────┬────────────────────────────────┬───────────────────┘
                    │                                │
┌───────────────────▼──────────────┐   ┌─────────────▼───────────────────┐
│            DATA LAYER            │   │         AUDIO & PLATFORM        │
│  • Room DB (SQLite + FTS4)       │   │  • Jetpack Media3 ExoPlayer     │
│  • MediaStore Incremental Scanner│   │  • MediaSessionService          │
│  • DataStore Preferences         │   │  • C++ NDK ViPERFX DSP Engine   │
│  • Stream & Download Repository  │   │  • AudioFocus & Noise Observer  │
└──────────────────────────────────┘   └─────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Domain | Technology / Library | Description |
| :--- | :--- | :--- |
| **Core Language** | [Kotlin 2.3](https://kotlinlang.org/) | 100% modern Kotlin with coroutines and reactive flows |
| **Native DSP** | [C++20](https://en.cppreference.com/w/cpp/20) / Android NDK | High-performance 32-bit floating-point audio processing |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) | Declarative UI with Material 3 & Obsidian theme tokens |
| **Playback Engine** | [Media3 ExoPlayer 1.5.1](https://developer.android.com/media/media3) | Modern Android audio playback and background media session |
| **Persistence** | [Room 2.8.5](https://developer.android.com/training/data-storage/room) | Type-safe SQLite database with FTS4 full-text search indexing |
| **Preferences** | [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) | Asynchronous, transactional key-value configuration storage |
| **Image Loading** | [Coil 2.7.0](https://coil-kt.github.io/coil/) | Memory-efficient cover art caching and bitmap rendering |
| **Network & Streams** | [OkHttp 4.12](https://square.github.io/okhttp/) & InnerTube | Efficient streaming, stream URL resolution, and file downloads |
| **Build Tooling** | Gradle 9.1 & KSP | Version Catalog (`libs.versions.toml`) with Kotlin Symbol Processing |

---

## 🚀 Getting Started & Building

### Prerequisites
- **JDK**: Version 17 or 21 (e.g., [Amazon Corretto 21](https://aws.amazon.com/corretto/))
- **Android SDK**: API Level 26 (Android 8.0) up to API Level 35 (Android 15)
- **Android NDK**: Version 26+ (configured in Android Studio / SDK Manager)
- **CMake**: Version 3.22.1+

### 1. Clone the Repository
```bash
git clone https://github.com/santjsx/CodeWave.git
cd CodeWave
```

### 2. Run Unit Tests
```bash
# Run local unit tests (Audio models, DSP mathematics, Sorting, and Deduplication)
./gradlew testDebugUnitTest
```

### 3. Assemble Debug APK
```bash
# Assemble debug build for local testing and inspection
./gradlew assembleDebug
```
The debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 4. Assemble Release APK
```bash
# Assemble optimized, R8-minified, and signed release APK
./gradlew assembleRelease
```
The optimized release APK will be generated at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## 📲 Updates & Sideload Integrity

CodeWave includes a built-in, user-triggered OTA update mechanism:
- **Release Channel**: Checks directly against [GitHub Releases](https://github.com/santjsx/CodeWave/releases).
- **Cryptographic Verification**: Verifies SHA-256 package checksums prior to prompt.
- **No Play Store Lock-In**: Complete freedom to sideload and self-host updates.

---

## 📄 License

Copyright © 2026. All rights reserved.  
Engineered with precision for true music ownership and audio fidelity.
