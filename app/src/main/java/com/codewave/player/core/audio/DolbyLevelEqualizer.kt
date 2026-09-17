package com.codewave.player.core.audio

import kotlin.math.pow

/**
 * Pure Kotlin Dolby-Grade Parametric Equalizer.
 * Manages 10 biquad filter bands, dynamic pre-amp gain auto-reduction,
 * user preamp scaling, and a transparent soft limiter.
 */
class DolbyLevelEqualizer(var sampleRate: Double = 44100.0) {

    val centerFrequencies = doubleArrayOf(
        31.25, 62.5, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0
    )

    private val bands = Array(10) { index ->
        val type = when (index) {
            0 -> PremiumBiquadFilter.FilterType.LOW_SHELF
            9 -> PremiumBiquadFilter.FilterType.HIGH_SHELF
            else -> PremiumBiquadFilter.FilterType.PEAKING
        }
        PremiumBiquadFilter(type, sampleRate)
    }

    private val currentGainsDb = DoubleArray(10) { 0.0 }
    var preAmpHeadroomFactor: Float = 1.0f
        internal set
    private var userPreampFactor = 1.0f
    var isEnabled: Boolean = true

    init {
        recalculateFilters()
    }

    fun updateSampleRate(newRate: Double) {
        if (newRate <= 0.0 || newRate == sampleRate) return
        sampleRate = newRate
        for (i in bands.indices) {
            bands[i].sampleRate = newRate
            bands[i].updateCoefficients(centerFrequencies[i], currentGainsDb[i], 1.414)
            bands[i].reset()
        }
    }

    fun reset() {
        for (band in bands) {
            band.reset()
        }
    }

    fun setBandGain(bandIndex: Int, gainDb: Double) {
        if (bandIndex in bands.indices) {
            currentGainsDb[bandIndex] = gainDb
            bands[bandIndex].updateCoefficients(centerFrequencies[bandIndex], gainDb, 1.414)
            recalculatePreAmpGain()
        }
    }

    fun setUserPreampDb(preampDb: Float) {
        userPreampFactor = 10.0f.pow(preampDb.coerceIn(-15f, 15f) / 20.0f)
    }

    private fun recalculateFilters() {
        for (i in bands.indices) {
            bands[i].updateCoefficients(centerFrequencies[i], currentGainsDb[i], 1.414)
        }
        recalculatePreAmpGain()
    }

    private fun recalculatePreAmpGain() {
        val maxBoost = currentGainsDb.maxOrNull() ?: 0.0
        preAmpHeadroomFactor = if (maxBoost > 0.0) {
            10.0.pow(-maxBoost / 20.0).toFloat()
        } else {
            1.0f
        }
    }

    fun processAudioInterleaved(floatBuffer: FloatArray, frameCount: Int) {
        if (!isEnabled) return

        val totalGain = preAmpHeadroomFactor * userPreampFactor

        for (frame in 0 until frameCount) {
            val offset = frame * 2

            // 1. Headroom attenuation
            if (totalGain != 1.0f) {
                floatBuffer[offset] *= totalGain
                floatBuffer[offset + 1] *= totalGain
            }

            // 2. Cascade through 10-band biquad filters
            for (band in bands) {
                band.processStereoPair(floatBuffer, offset)
            }

            // 3. Transparent Soft Limiter to prevent clipping
            applySoftLimiter(floatBuffer, offset)
            applySoftLimiter(floatBuffer, offset + 1)
        }
    }

    private fun applySoftLimiter(buffer: FloatArray, index: Int) {
        val sample = buffer[index]
        val threshold = 0.95f
        if (sample > threshold) {
            val excess = sample - threshold
            buffer[index] = (threshold + excess / (1.0f + (excess / (1.0f - threshold)).pow(2))).coerceIn(-1.0f, 1.0f)
        } else if (sample < -threshold) {
            val excess = -sample - threshold
            buffer[index] = -(threshold + excess / (1.0f + (excess / (1.0f - threshold)).pow(2))).coerceIn(-1.0f, 1.0f)
        }
    }
}
