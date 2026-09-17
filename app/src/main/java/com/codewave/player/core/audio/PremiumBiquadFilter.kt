package com.codewave.player.core.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-precision stereo parametric biquad filter.
 * Features Direct Form I difference equations with independent stereo history buffers
 * and exponential coefficient smoothing to eliminate zipper noise.
 */
class PremiumBiquadFilter(
    val type: FilterType = FilterType.PEAKING,
    var sampleRate: Double = 44100.0
) {
    enum class FilterType { LOW_SHELF, PEAKING, HIGH_SHELF }

    // Target coefficients
    var b0: Double = 1.0; var b1: Double = 0.0; var b2: Double = 0.0
    var a1: Double = 0.0; var a2: Double = 0.0

    // Current active coefficients for smooth interpolation
    var curB0: Double = 1.0; var curB1: Double = 0.0; var curB2: Double = 0.0
    var curA1: Double = 0.0; var curA2: Double = 0.0

    // Independent stereo delay states (0 = Left, 1 = Right)
    private val x1 = DoubleArray(2) { 0.0 }
    private val x2 = DoubleArray(2) { 0.0 }
    private val y1 = DoubleArray(2) { 0.0 }
    private val y2 = DoubleArray(2) { 0.0 }

    companion object {
        const val SMOOTHING_FACTOR: Double = 0.005
    }

    fun reset() {
        x1.fill(0.0)
        x2.fill(0.0)
        y1.fill(0.0)
        y2.fill(0.0)
        curB0 = b0; curB1 = b1; curB2 = b2
        curA1 = a1; curA2 = a2
    }

    fun updateCoefficients(centerFreq: Double, gainDb: Double, qFactor: Double = 1.414) {
        val nyquist = 0.49 * sampleRate
        val f0 = centerFreq.coerceIn(10.0, nyquist)
        val q = qFactor.coerceIn(0.1, 10.0)

        val a = 10.0.pow(gainDb / 40.0)
        val omega = 2.0 * PI * f0 / sampleRate
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2.0 * q)

        val normB0: Double
        val normB1: Double
        val normB2: Double
        val normA0: Double
        val normA1: Double
        val normA2: Double

        when (type) {
            FilterType.PEAKING -> {
                normB0 = 1.0 + alpha * a
                normB1 = -2.0 * cs
                normB2 = 1.0 - alpha * a
                normA0 = 1.0 + alpha / a
                normA1 = -2.0 * cs
                normA2 = 1.0 - alpha / a
            }
            FilterType.LOW_SHELF -> {
                val aMin1 = a - 1.0
                val aPlus1 = a + 1.0
                val beta = 2.0 * sqrt(a) * alpha

                normB0 = a * (aPlus1 - aMin1 * cs + beta)
                normB1 = 2.0 * a * (aMin1 - aPlus1 * cs)
                normB2 = a * (aPlus1 - aMin1 * cs - beta)
                normA0 = aPlus1 + aMin1 * cs + beta
                normA1 = -2.0 * (aMin1 + aPlus1 * cs)
                normA2 = aPlus1 + aMin1 * cs - beta
            }
            FilterType.HIGH_SHELF -> {
                val aMin1 = a - 1.0
                val aPlus1 = a + 1.0
                val beta = 2.0 * sqrt(a) * alpha

                normB0 = a * (aPlus1 + aMin1 * cs + beta)
                normB1 = -2.0 * a * (aMin1 + aPlus1 * cs)
                normB2 = a * (aPlus1 + aMin1 * cs - beta)
                normA0 = aPlus1 - aMin1 * cs + beta
                normA1 = 2.0 * (aMin1 - aPlus1 * cs)
                normA2 = aPlus1 - aMin1 * cs - beta
            }
        }

        if (kotlin.math.abs(normA0) > 1e-12) {
            b0 = normB0 / normA0
            b1 = normB1 / normA0
            b2 = normB2 / normA0
            a1 = normA1 / normA0
            a2 = normA2 / normA0
        }
    }

    /**
     * Process an interleaved stereo sample pair in place.
     * @param buffer Interleaved stereo array [L, R, L, R, ...]
     * @param offset Offset in buffer where current frame starts
     */
    fun processStereoPair(buffer: FloatArray, offset: Int) {
        // Anti-zipper coefficient smoothing
        curB0 += (b0 - curB0) * SMOOTHING_FACTOR
        curB1 += (b1 - curB1) * SMOOTHING_FACTOR
        curB2 += (b2 - curB2) * SMOOTHING_FACTOR
        curA1 += (a1 - curA1) * SMOOTHING_FACTOR
        curA2 += (a2 - curA2) * SMOOTHING_FACTOR

        // Left Channel
        val inL = buffer[offset].toDouble()
        var outL = (curB0 * inL) + (curB1 * x1[0]) + (curB2 * x2[0]) - (curA1 * y1[0]) - (curA2 * y2[0])
        if (!outL.isFinite()) outL = 0.0
        x2[0] = x1[0]; x1[0] = inL
        y2[0] = y1[0]; y1[0] = outL
        buffer[offset] = outL.toFloat()

        // Right Channel
        val inR = buffer[offset + 1].toDouble()
        var outR = (curB0 * inR) + (curB1 * x1[1]) + (curB2 * x2[1]) - (curA1 * y1[1]) - (curA2 * y2[1])
        if (!outR.isFinite()) outR = 0.0
        x2[1] = x1[1]; x1[1] = inR
        y2[1] = y1[1]; y1[1] = outR
        buffer[offset + 1] = outR.toFloat()
    }
}
