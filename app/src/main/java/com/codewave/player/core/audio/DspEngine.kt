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

    var dspStatus: DSPStatus = DSPStatus.UNAVAILABLE
        private set

    /**
     * Attaches audio processing to the specific ExoPlayer audio session ID (PRD Section 36).
     * Global session-0 is deprecated on modern Android.
     */
    fun attachToSession(audioSessionId: Int, config: EqualizerConfig) {
        if (audioSessionId == 0) return
        if (currentSessionId == audioSessionId && (dynamicsProcessing != null || legacyEqualizer != null)) {
            applyConfig(config)
            return
        }

        release()
        currentSessionId = audioSessionId

        // Try DynamicsProcessing (Android 9 / API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val dpConfig = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, // 2 Channels (Stereo)
                    true, // PreEQ
                    10, // 10 bands
                    true, // Multiband Compressor
                    10,
                    false, // PostEQ
                    0,
                    true // Limiter (protect against clipping per PRD Section 39)
                ).build()

                dynamicsProcessing = DynamicsProcessing(0, audioSessionId, dpConfig).apply {
                    enabled = config.isEnabled
                }
                dspStatus = DSPStatus.ACTIVE
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
                dspStatus = DSPStatus.LIMITED
            } catch (_: Exception) {
                legacyEqualizer = null
                dspStatus = DSPStatus.UNAVAILABLE
            }
        }

        applyConfig(config)
    }

    fun applyConfig(config: EqualizerConfig) {
        val dp = dynamicsProcessing
        if (dp != null) {
            try {
                dp.enabled = config.isEnabled
                if (config.isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val preEq = DynamicsProcessing.Eq(true, true, config.bands.size)
                    config.bands.forEachIndexed { index, band ->
                        val eqBand = DynamicsProcessing.EqBand(
                            true,
                            band.centerFreqHz.toFloat(),
                            band.gainDb + config.preampGainDb
                        )
                        preEq.setBand(index, eqBand)
                    }
                    dp.setPreEqAllChannelsTo(preEq)

                    // Configure limiter to protect against clipping (PRD Section 39)
                    if (config.isLimiterEnabled) {
                        val limiter = DynamicsProcessing.Limiter(
                            true,
                            true,
                            0,
                            1.0f,
                            50.0f,
                            10.0f,
                            -0.5f,
                            0.0f
                        )
                        dp.setLimiterAllChannelsTo(limiter)
                    }
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
