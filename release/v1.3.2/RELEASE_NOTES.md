# CodeWave v1.3.2 Release Notes

**Release Version**: 1.3.2  
**Release Channel**: Stable Production  
**Build Target**: Android 8.0+ (API 26+) / Target API 35 (Android 15)  
**Binary Architecture**: Universal Release APK (ARM64-v8a, ARMv7a, x86_64)  
**Checksum (SHA-256)**: `917ccf16a99fc9b76a9d83bb7cb77bfe3e31f281bf6f7a63ed2bed7c480fae22`  

---

### Highlights & Fixes in v1.3.2

#### 1. Real-Time Equalizer & DSP Status Synchronization
- **Fixed Hardcoded Active State**: Previously, `PlaybackRepository` hardcoded `dspStatus = DSPStatus.ACTIVE` regardless of whether the equalizer was enabled or disabled, and `TrackInspectorDrawer` displayed "DSP ACTIVE" unconditionally.
- **BYPASSED State**: Introduced `DSPStatus.BYPASSED`. Injected `EqualizerRepository` into `DefaultPlaybackRepository` to bind playback state dynamically to active equalizer state.
- **Unified Telemetry**: When the equalizer is toggled OFF, both the Now Playing Track Inspector and the Equalizer Screen instantly update to show `BYPASSED` and "Direct Bit-Perfect Pass-Through" (or `ACTIVE` / `LIMITED` with actual processing telemetry when enabled).

#### 2. Rewritten 120 FPS Vertical Faders & Preamp Control
- **Eliminated Gesture Contention**: `CWVerticalFader` previously stacked two competing `pointerInput` modifiers (`detectTapGestures` + `detectVerticalDragGestures`). The tap detector consumed initial pointer-down events, starving the drag detector and freezing slider movement.
- **Unified Gesture Loop**: Replaced with a single, high-performance `awaitEachGesture` detector. Once vertical motion exceeds the touch slop (> 6px), motion events are consumed to cleanly lock out parent horizontal scroll rows.
- **Ultra-Smooth Interaction**: Features local float tracking for 120 FPS fluid thumb animation, tap-to-reposition anywhere along the groove, and double-tap to snap immediately back to 0.0 dB unity gain.
- **Synchronous In-Memory State**: Converted `DefaultEqualizerRepository` to maintain an in-memory `MutableStateFlow<EqualizerConfig>`, delivering < 1ms synchronous UI response while persisting changes to DataStore asynchronously in background coroutines.
- **Smooth Preamp Slider**: Wired local transient float state in the master preamp slider to eliminate stuttering during rapid dragging.

#### 3. Studio-Grade Audio Quality & Anti-Clipping Engine
- **Eliminated Distortion & Muffling**: Previous configuration declared `mbcInUse = true` with 10 bands in `DynamicsProcessing.Config.Builder` without initializing multiband compression parameters, which triggered phase cancellation and audio HAL muffling on many Android devices. Set `mbcInUse = false` to guarantee transparent, crystal-clear 10-band Pre-EQ response.
- **True-Peak Studio Limiter**: Tuned `DynamicsProcessing.Limiter` with transparent studio mastering settings:
  - Threshold: `-0.2 dBFS`
  - Attack: `2.0 ms`
  - Release: `60.0 ms`
  - Ratio: `10:1`
  This guarantees distortion-free audio even when heavy band boosts (+12 dB) or high preamp gains are applied, preserving full dynamic punch without clipping.

---

### Verification & Asset Info
- **File**: `CodeWave-v1.3.2.apk`
- **Package Name**: `com.codewave.player`
- **Version Code**: 6
- **Size**: 4.35 MB (4,354,511 bytes)
- **Signature**: Signed with release keystore using v1, v2 & v3 schemes.
