# CodeWave v1.3.3 Release Notes

**Release Version**: 1.3.3  
**Release Channel**: Stable Production  
**Build Target**: Android 8.0+ (API 26+) / Target API 35 (Android 15)  
**Binary Architecture**: Universal Release APK (ARM64-v8a, ARMv7a, x86_64)  
**Checksum (SHA-256)**: `1f740f3d73217969891cee4267c62b01e55cac4b71f9f9aaf2b46e207531a825`  

---

### Highlights & Fixes in v1.3.3

#### 1. Hardware DSP Status & "BYPASSED" Badge Alignment
- **Root Cause Fixed**: In the Equalizer screen's Hardware DSP Status card, the text column had no layout weight, allowing long description strings to push the technical badge against the card's right border. Furthermore, `CWTechnicalBadge` allowed soft text wrapping, causing the word "BYPASSED" to break character-by-character into an 8-row vertical stack (`B \n Y \n P \n A \n S \n S \n E \n D`).
- **Single-Line Horizontal Layout**:
  - `CWTechnicalBadge` now enforces `maxLines = 1` and `softWrap = false`, guaranteeing badge text never wraps vertically regardless of parent layout pressure.
  - The DSP card's text column now uses `Modifier.weight(1f).padding(end = 12.dp)`, giving the badge natural, unconstrained horizontal breathing space.
  - Added a live circular status dot (green for `ACTIVE`, amber for `LIMITED`, neutral slate for `BYPASSED`) next to the section title.

#### 2. Modern 3-Column Presets Grid & Active Profile Spotlight
- **Eliminated Horizontal Scrolling**:
  - Replaced the easily missed single horizontal scroll row with an organized, high-visibility **3-column presets grid**.
  - All 12 studio presets (`Flat`, `Acoustic`, `Bass Boost`, `Bass Reducer`, `Classical`, `Dance`, `Electronic`, `Hip-Hop`, `Jazz`, `Pop`, `Rock`, `Vocal Clarity`) are instantly visible on screen at once and selectable in a single tap without any horizontal scrolling.
- **Active Profile Spotlight**:
  - Added a dedicated card spotlighting the currently active profile name with an equalizer glyph, profile preamp offset badge (e.g. `PROFILE PREAMP -3.0 dB`), and total available profiles counter (`12 PROFILES`).
  - Active presets illuminate with vibrant cyan accents and bold contrast; inactive presets use sleek elevated surfaces with crisp typography.

#### 3. New 3D Glowing Blue Musical Note App Icon
- **High-Fidelity Branding**: Upgraded the app launcher icon across all Android formats with the new 3D folded blue ribbon musical note and glassmorphic squircle emblem.
- **Adaptive Safe Zone**: The squircle emblem is centered within the 290px safe area of a 432x432 px canvas in `drawable-nodpi/ic_avatar_art.webp`, ensuring OEM launcher masks (circular, rounded square, squircle, pebble) never clip the icon artwork or ambient glow.
- **Multi-Density Mipmaps**: Generated pixel-perfect standard and round icons across all screen densities:
  - `mipmap-mdpi` (48x48)
  - `mipmap-hdpi` (72x72)
  - `mipmap-xhdpi` (96x96)
  - `mipmap-xxhdpi` (144x144)
  - `mipmap-xxxhdpi` (192x192)
  - `release/icon.png` (512x512 high-resolution store asset)

---

### Verification & Asset Info
- **File**: `CodeWave-v1.3.3.apk`
- **Package Name**: `com.codewave.player`
- **Version Code**: 7
- **Size**: 4.40 MB (4,396,384 bytes)
- **Signature**: Signed with release keystore using v1, v2 & v3 schemes.
