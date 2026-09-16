<p align="center">
  <img src="app-icon.png" width="128" height="128" alt="CodeWave App Icon" />
</p>

<h1 align="center">CodeWave</h1>

<p align="center">
  <strong>Industry-Grade Offline Hi-Res Audio Workstation for Android</strong>
</p>

<p align="center">
  <a href="https://github.com/santjsx/CodeWave/releases/latest"><img src="https://img.shields.io/github/v/release/santjsx/CodeWave?color=00E5FF&label=Release&style=flat-square" alt="Latest Release" /></a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-00E676?style=flat-square" alt="Platform" />
  <img src="https://img.shields.io/badge/Audio%20Engine-Media3%20ExoPlayer-2979FF?style=flat-square" alt="Audio Engine" />
  <img src="https://img.shields.io/badge/DSP-Float32%20DynamicsProcessing-8A2BE2?style=flat-square" alt="DSP" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20Offline%20%C2%B7%20Zero%20Ads-FFD600?style=flat-square" alt="Privacy" />
</p>

---

## ⚡ Why CodeWave?

There are hundreds of music players on Android. Most are either ad-bloated generic clones or complex legacy apps with cluttered, outdated UIs. **CodeWave was built to solve this:**

1. **Unfiltered Audio Transparency (Zero Placebo)**  
   Most players slap a "Hi-Res" badge on a lossy MP3 or hide that Android is downsampling your 96 kHz FLAC to 48 kHz. CodeWave’s **Signature Track Inspector** reveals the real technical pipeline: true file **SOURCE** decoding vs. actual hardware **OUTPUT** device routing, sample rate, bit depth, and buffer latency.
2. **True 32-Bit Studio Dynamics Processing (DSP)**  
   No muddy software EQ filters. CodeWave drives Android's hardware `DynamicsProcessing` engine with 10 studio bands (`31 Hz` to `16 kHz`), a calibrated center-zero Master Preamp gain, and transparent limiter anti-clipping protection.
3. **100% Offline Ownership & Absolute Privacy**  
   No accounts, no cloud sync, no tracking, no analytics, and zero ads. Your music library remains strictly yours on your device.
4. **Developer-Grade Obsidian Aesthetic**  
   Designed with an IDE workstation theme: deep dark palette (`#0B0D10`), illuminated neon telemetry, physical console faders, and 3D glass vitrine playlist containers.
5. **Instant SQLite FTS4 Search & Smart Library**  
   Sub-millisecond full-text search across thousands of tracks, with auto-generated dynamic smart collections (*Lossless & Hi-Res*, *Favorites*, *Recently Added*).

---

## 🎧 Core Features

- **Float32 Master DSP Console**: 10-band studio graphic EQ, center-zero bipolar master preamp slider (`-12 dB` to `+12 dB`), zero patchy fader artifacts, and 12 tuned acoustic sound profiles with real-time mini-curves.
- **Glass Vitrine Playlist Containers**: Physical shelf display aesthetic featuring album sleeves nested deeply inside photorealistic frosted glass containers with specular reflections and chrome hardware.
- **Track Inspector Sheet**: Deep technical inspection for every song showing audio container format, bit depth, physical routing, and DSP processing status.
- **Incremental MediaStore Scanner**: Rapid background discovery with ContentObserver updates, surviving file renames, SD card moves, and partial downloads.
- **Seamless In-App OTA Updater**: GitHub Releases discovery with specular download progress bar, live speed telemetry (`MB/s`), and direct APK installation.

---

## 🛠️ Architecture & Tech Stack

| Layer | Technologies |
|---|---|
| **UI & Presentation** | Jetpack Compose (BOM 2026), Material 3, Custom Canvas Shaders, Obsidian Design System |
| **Audio Engine** | AndroidX Media3 ExoPlayer 1.5.1, `MediaSessionService`, `AudioFocusManager` |
| **DSP & Equalizer** | Android `DynamicsProcessing` (32-bit float), PreEQ Gain Staging, Limiter Guard |
| **Database & Cache** | Room 2.8.5 with SQLite FTS4, DataStore Preferences, Coil 2.7 |
| **Language & Build** | Kotlin 2.3, Gradle 9.1 Kotlin DSL, R8 Full Mode Shrinking (`~4.4 MB` APK) |

---

## 📥 Download & Installation

Grab the latest signed production APK directly from **[GitHub Releases](https://github.com/santjsx/CodeWave/releases/latest)**:

1. Download **`app-release.apk`**.
2. Tap the downloaded file to install (enable "Install unknown apps" if prompted).
3. CodeWave updates itself seamlessly through its built-in OTA updater.

---

## 🏗️ Building from Source

```bash
# Clone repository
git clone https://github.com/santjsx/CodeWave.git
cd CodeWave

# Build debug APK
./gradlew assembleDebug

# Build optimized release APK
./gradlew assembleRelease
```

---

## 📄 License

Designed and developed with care. Open source under the Apache License 2.0.
