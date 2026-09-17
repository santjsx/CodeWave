package com.codewave.player.core.audio

import android.util.Log

/**
 * JNI Bridge to the native C++ ViPER DSP Engine (libviper_dsp_engine.so).
 * Includes safe dynamic library loading and graceful fallback support.
 */
object ViperJniWrapper {

    private const val TAG = "ViperJniWrapper"
    var isNativeAvailable: Boolean = false
        private set

    init {
        try {
            System.loadLibrary("viper_dsp_engine")
            isNativeAvailable = true
        } catch (_: Throwable) {
            isNativeAvailable = false
        }

        try {
            if (isNativeAvailable) {
                Log.i(TAG, "Successfully loaded native ViPER DSP engine (libviper_dsp_engine.so)")
            } else {
                Log.w(TAG, "Native ViPER library unavailable, using pure Kotlin DSP engine")
            }
        } catch (_: Throwable) {
            // Safe fallback in JVM test environments where android.util.Log is not mocked
        }
    }

    @JvmStatic
    external fun nativeInit(sampleRate: Double)

    @JvmStatic
    external fun nativeReset()

    @JvmStatic
    external fun nativeSetGlobalEnabled(enabled: Boolean)

    @JvmStatic
    external fun nativeSetEqEnabled(enabled: Boolean)

    @JvmStatic
    external fun nativeSetLimiterEnabled(enabled: Boolean)

    @JvmStatic
    external fun nativeSetEqBand(bandIndex: Int, gainDb: Double)

    @JvmStatic
    external fun nativeSetUserPreamp(preampDb: Float)

    @JvmStatic
    external fun nativeSetBass(enabled: Boolean, gainDb: Float)

    @JvmStatic
    external fun nativeSetClarity(enabled: Boolean, gainDb: Float)

    @JvmStatic
    external fun nativeSetConvolverEnabled(enabled: Boolean)

    @JvmStatic
    external fun nativeLoadImpulseResponse(irData: FloatArray?, length: Int, channels: Int)

    @JvmStatic
    external fun nativeProcessStereo(audioBuffer: FloatArray, frameCount: Int)
}
