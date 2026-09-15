# CodeWave v1.3.1 Release Notes

**Release Version**: 1.3.1  
**Release Channel**: Stable Production  
**Build Target**: Android 8.0+ (API 26+) / Target API 35 (Android 15)  
**Binary Architecture**: Universal Release APK (ARM64-v8a, ARMv7a, x86_64)  
**Checksum (SHA-256)**: `451759fc9983a6a69dac887456d88936168f8328c6a73b91436f284ac410e9a8`  

---

### Highlights & Fixes in v1.3.1

#### 1. Continue Listening Exact Duration Persistence
- **Root Cause Fixed**: Previously, `onMediaItemTransition` in `PlaybackRepository` was resetting `positionMs` to `0L` and executing `settingsRepository.setLastPlayed(track.id, 0L)` during media preparation, which wiped out saved timestamps upon app startup or song loading.
- **Exact Resumption**: `onMediaItemTransition` now retains the active seek position and only updates `settingsRepository` with valid non-zero positions or on natural end-of-track transitions.
- **Direct State Binding**: `HomeViewModel` now exposes a dedicated `continueListeningTrack` StateFlow combining `lastPlayedTrackId` with library tracks, ensuring the "CONTINUE LISTENING" card always displays the exact millisecond resume badge (`RESUME AT mm:ss`) and seeks cleanly to the exact second.

#### 2. Instant Search Autofocus
- **Zero-Tap Querying**: When tapping the Search tab in the bottom navigation bar, the cursor autofocuses immediately in the search input field and the software keyboard automatically pops up, eliminating the friction of an extra manual tap.

#### 3. Zero-Flash Obsidian Window Startup
- **Native Splash / Window Styling**: Replaced the default `Theme.Material.Light.NoActionBar` window parent in `themes.xml` with `Theme.Material.NoActionBar` and configured `codewave_window_background = #0A0E17`.
- **Seamless Launch**: Android OS now paints the native decor views and window background in deep obsidian dark mode before Jetpack Compose inflates, completely eliminating the blinding white screen flash on app startup.

---

### Verification & Asset Info
- **File**: `CodeWave-v1.3.1.apk`
- **Package Name**: `com.codewave.player`
- **Version Code**: 5
- **Size**: 4.35 MB (4,354,522 bytes)
- **Signature**: Signed with release keystore using v1, v2 & v3 schemes.
