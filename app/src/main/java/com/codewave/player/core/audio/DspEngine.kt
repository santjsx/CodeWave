package com.codewave.player.core.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.os.Build
import com.codewave.player.core.model.DSPStatus
import com.codewave.player.core.model.EqualizerConfig
import java.lang.Exception

class DspEngine {

    private var dynamicsProcessing: DynamicsProcessing? = null
    private var legacyEqualizer: Equalizer? = null
    private var currentSessionId: Int = 0

    private var lastConfig: EqualizerConfig = EqualizerConfig()

    val dspStatus: DSPStatus
        get() {
            if (!lastConfig.isEnabled) return DSPStatus.BYPASSED
            return when {
                dynamicsProcessing != null -> DSPStatus.ACTIVE
                legacyEqualizer != null -> DSPStatus.LIMITED
                else -> DSPStatus.UNAVAILABLE
            }
        }

    /**
     * Attaches audio processing to the specific ExoPlayer audio session ID (PRD Section 36).
     * Global session-0 is deprecated on modern Android.
     */
    fun attachToSession(audioSessionId: Int, config: EqualizerConfig) {
        if (audioSessionId == 0) return
        lastConfig = config
        if (currentSessionId == audioSessionId && (dynamicsProcessing != null || legacyEqualizer != null)) {
            applyConfig(config)
            return
        }

        release()
        currentSessionId = audioSessionId

        // Try DynamicsProcessing (Android 9 / API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                // Disable unused Multiband Compressor (MBC) to eliminate distortion & phase artifacts
                val dpConfig = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, // 2 Channels (Stereo)
                    true, // PreEQ
                    10, // 10 bands
                    false, // Multiband Compressor disabled to preserve original dynamic range
                    0,
                    false, // PostEQ
                    0,
                    true // Limiter (protect against digital clipping)
                ).build()

                dynamicsProcessing = DynamicsProcessing(0, audioSessionId, dpConfig).apply {
                    enabled = config.isEnabled
                }
            } catch (_: Exception) {
                dynamicsProcessing = null
            }
        }

        // Fallback to standard Equalizer if DynamicsProcessing is unavailable (PRD Section 40)
        if (dynamicsProcessing == null) {
            try {
                legacyEqualizer = Equalizer(0, audioSessionId).apply {
                    enabled = config.isEnabled
                }
            } catch (_: Exception) {
                legacyEqualizer = null
            }
        }

        applyConfig(config)
    }

    fun applyConfig(config: EqualizerConfig) {
        lastConfig = config
        val dp = dynamicsProcessing
        if (dp != null) {
            try {
                dp.enabled = config.isEnabled
                if (config.isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Set native master input gain (preamp) across all channels without warping EQ band filters
                    dp.setInputGainAllChannelsTo(config.preampGainDb)

                    val preEq = DynamicsProcessing.Eq(true, true, config.bands.size)
                    config.bands.forEachIndexed { index, band ->
                        val eqBand = DynamicsProcessing.EqBand(
                            true,
                            band.centerFreqHz.toFloat(),
                            band.gainDb
                        )
                        preEq.setBand(index, eqBand)
                    }
                    dp.setPreEqAllChannelsTo(preEq)

                    // Configure transparent studio limiter to protect against clipping (PRD Section 39)
                    val limiter = DynamicsProcessing.Limiter(
                        true,
                        config.isLimiterEnabled,
                        0,
                        2.0f,  // 2ms attack: instant transient protection
                        60.0f, // 60ms release: natural decay without pumping
                        10.0f, // 10:1 ratio: firm safety ceiling
                        -0.2f, // -0.2 dBFS: transparent digital true-peak ceiling
                        0.0f   // 0 dB makeup gain: uncolored pass-through
                    )
                    dp.setLimiterAllChannelsTo(limiter)
                }
            } catch (_: Exception) {}
            return
        }

        val eq = legacyEqualizer
        if (eq != null) {
            try {
                eq.enabled = config.isEnabled
                if (config.isEnabled) {
                    val numBands = eq.numberOfBands.toInt()
                    val (minMb, maxMb) = eq.bandLevelRange
                    for (i in 0 until numBands) {
                        val centerFreq = eq.getCenterFreq(i.toShort()) / 1000 // In Hz
                        // Find closest band
                        val closest = config.bands.minByOrNull { kotlin.math.abs(it.centerFreqHz - centerFreq) }
                        if (closest != null) {
                            val gainDb = closest.gainDb + config.preampGainDb
                            val millibels = (gainDb * 100).toInt().coerceIn(minMb.toInt(), maxMb.toInt()).toShort()
                            eq.setBandLevel(i.toShort(), millibels)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun release() {
        try {
            dynamicsProcessing?.release()
        } catch (_: Exception) {}
        dynamicsProcessing = null

        try {
            legacyEqualizer?.release()
        } catch (_: Exception) {}
        legacyEqualizer = null
        currentSessionId = 0
    }
}
