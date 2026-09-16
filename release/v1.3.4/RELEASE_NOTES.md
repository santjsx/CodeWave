# CodeWave v1.3.4 Release Notes

**Release Version**: 1.3.4  
**Release Channel**: Stable Production  
**Build Target**: Android 8.0+ (API 26+) / Target API 35 (Android 15)  
**Binary Architecture**: Universal Release APK (ARM64-v8a, ARMv7a, x86_64)  
**Checksum (SHA-256)**: `a2127d9e2bd7a8225560baf9669fc16fe1dd55ad94a2a04aaf14a3fa9dde46c9`  

---

### Highlights & Fixes in v1.3.4

#### 1. Audio Quality & Lossless Explainer Dialog
- **Plain-English Explanations**: Added understandable explanations answering *"What does this mean for your music?"* tailored dynamically to the active format (Lossless, Hi-Res, Compressed).
  - *Lossless*: Explains how 100% of the original studio recording data is preserved without trimming frequencies, delivering bit-perfect sound with original studio fidelity, wider soundstage, and zero compression distortion.
  - *Hi-Res*: Explains that sample rate and bit depth exceed standard CD resolution (e.g. 24-bit / 96kHz vs 16-bit / 44.1kHz), capturing ultra-fine harmonics and acoustic room nuances.
  - *Compressed*: Explains how lossy compression trims data to save space, and how CodeWave's 32-bit floating-point engine renders it with maximum possible clarity.
- **Audio Resolution Tier Ladder**: 3-step visual ladder (`LOSSY (≤ 320 kbps)` ➔ `LOSSLESS (1,411 kbps)` ➔ `HI-RES (Studio Master)`) with active tier highlighting.
- **Full Technical Telemetry**: Container, Encoding, Sample Rate, Bit Depth, Bitrate, and 32-Bit Float Headroom margin in a responsive, scroll-safe container.

#### 2. App Launcher Icon Cropping & Adaptive Framing
- **Eliminated Black Borders & Double-Squircles**: Replaced the previous obsidian black vector background with an edge-to-edge electric blue studio gradient (`#0E3470` ➔ `#051842` ➔ `#01091F`) with a central acoustic radial glow.
- **Safe-Zone Centered Foreground**: Sized and centered the 3D folded ribbons and glowing cyan musical note safely within the adaptive safe zone, leaving comfortable ~50px breathing margins.
- **Verified Across OEM Launchers**: Zero clipping and zero black gaps on OnePlus/Xiaomi squircles, Samsung OneUI squircles, and Google Pixel pure circles. Refreshed all density mipmaps and `release/icon.png` (512x512).

#### 3. MiniPlayer Progress Bar Visibility & Polish
- **Subtle Card Border**: Changed miniplayer card border from overpowering `BorderFocus` to `BorderSubtle`, allowing the progress indicator to stand out cleanly.
- **High-Definition Progress Bar**: Increased thickness from 2dp to **3.5dp** with a deep contrast track (`#0D1117`), horizontal cyan-blue gradient fill, and a luminous circular glow tip at the leading edge.
- **Seamless Geometry**: Curved along bottom corners (`RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)`) without clipping.

#### 4. Clean VS Code-Inspired Equalizer Workstation
- **ZERO Horizontal Scrolling for Sliders**: Redesigned `CWVerticalFader` to use proportional column weights (`Modifier.weight(1f)`). All 10 frequency bands (`31`, `62`, `125`, `250`, `500`, `1k`, `2k`, `4k`, `8k`, `16k`) are visible simultaneously across the screen.
- **Full-Column Touch Gestures**: Tap groove to jump directly, drag vertically with 0.5 dB precision, double-tap anywhere on the column to reset to `0.0 dB`.
- **Real-Time Parametric Response Curve**: Dynamic cubic Bezier curve canvas above the sliders that morphs smoothly in real-time as sliders or presets are changed, featuring a glowing gradient fill, 0 dB center reference line, and frequency node indicators.
- **ZERO Horizontal Scrolling for Presets**: Replaced the 12 bulky buttons with an interactive **VS Code Command Bar** (`[ ⚡ SOUND PROFILE: Bass Boost | -3.0 dB | SELECT ▾ ]`) that opens a quick-pick modal sheet, plus a compact 2-row micro-chip matrix for instant 1-tap switching.
- **Unified Preamp & Limiter Panel**: Master Preamp Gain slider (-12.0 dB to +12.0 dB) with live numeric telemetry and Anti-Clipping Limiter toggle.

---

### Verification & Asset Info
- **File**: `CodeWave-v1.3.4.apk`
- **Package Name**: `com.codewave.player`
- **Version Code**: 8
- **Size**: 4.38 MB (4,380,041 bytes)
- **Signature**: Signed with release keystore using v1, v2 & v3 schemes.
