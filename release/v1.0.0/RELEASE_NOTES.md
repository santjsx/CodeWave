# CodeWave v1.0.0 Release Notes

**CodeWave** — Developer-Grade Offline Hi-Res Music Player for Android.

### Highlights
- **100% Offline & Private**: Zero trackers, zero analytics, zero network dependency for audio playback.
- **Audiophile DSP Engine**: 10-band equalizer (31 Hz – 16 kHz), Bass Boost, Virtualizer, Reverb presets, and Realtime AudioTrack latency tracking.
- **Developer Workstation Aesthetic**: Obsidian `#0B0D10` dark theme, JetBrains Mono typography, status bars, and audio telemetry diagnostics (`FLAC · 24-bit · 96 kHz · Lossless`).
- **Instant MediaStore Indexing**: Full local library scanning for Tracks, Albums, Artists, Folders, and Playlists.
- **Media3 ExoPlayer Pipeline**: Foreground service playback with Lockscreen Media Controls, Notification controls, and Android AudioFocus ducking/pause handling.
- **Sideload Update Verification**: Cryptographically verified releases via SHA-256 and production v2/v3 signatures.

### Package Details
- **Application ID**: `com.codewave.player`
- **Version Name**: `1.0.0`
- **Version Code**: `1`
- **Min SDK**: `26` (Android 8.0 Oreo)
- **Target SDK**: `35` (Android 15)
- **Package Size**: `4.25 MB` (4,254,377 bytes)
- **Signature Schemes**: APK Signature Scheme v2, Scheme v3
- **Signer SHA-256**: `9dac2bf05606ca9c9408debabb90d1b81ecdd45c1b55c57b18e4b08ca10a81db`
- **Package SHA-256 Checksum**:
  `57819a22b5d6c69b246501a95a69aaa4ef3bc298de470b25b3d3696c8d73cf8c`

### Verification Instructions
To verify the integrity of your downloaded APK:
```bash
# Windows PowerShell
Get-FileHash codewave-v1.0.0-release.apk -Algorithm SHA256

# Linux / macOS
sha256sum codewave-v1.0.0-release.apk
```
Ensure the hash matches:
`57819a22b5d6c69b246501a95a69aaa4ef3bc298de470b25b3d3696c8d73cf8c`
