package com.codewave.player.core.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real-time Audio Spectrum & Waveform Analyzer.
 * Intercepts raw PCM float streams from [ViperAudioProcessor], executes a 512-point
 * Cooley-Tukey Radix-2 FFT with Hann windowing, maps 256 frequency bins into 24
 * perceptual frequency bands, and projects them symmetrically across 48 visualizer bars.
 *
 * Employs asymmetric ballistics (instant attack on transients, smooth exponential decay)
 * and dynamic auto-gain so all music genres (from acoustic to EDM) produce vibrant,
 * organic visual feedback.
 */
class AudioVisualizerProcessor {

    companion object {
        const val FFT_SIZE = 512
        const val HALF_FFT = FFT_SIZE / 2 // 256 positive bins
        const val NUM_BANDS = 24
        const val NUM_BARS = 48
        private const val MIN_UPDATE_INTERVAL_MS = 20L // ~50 FPS update rate
    }

    // Pre-calculated tables for zero-allocation FFT execution
    private val hannWindow = FloatArray(FFT_SIZE) { i ->
        (0.5f * (1.0f - cos(2.0 * PI * i / (FFT_SIZE - 1)))).toFloat()
    }

    private val bitReverseIndices = IntArray(FFT_SIZE) { i ->
        var rev = 0
        var temp = i
        for (b in 0 until 9) { // 2^9 = 512
            rev = (rev shl 1) or (temp and 1)
            temp = temp shr 1
        }
        rev
    }

    private val cosTable = FloatArray(FFT_SIZE) { i ->
        cos(-2.0 * PI * i / FFT_SIZE).toFloat()
    }

    private val sinTable = FloatArray(FFT_SIZE) { i ->
        sin(-2.0 * PI * i / FFT_SIZE).toFloat()
    }

    // Envelope tapering curve (matches visualizer bell envelope)
    private val envelopeCurve = FloatArray(NUM_BARS) { i ->
        val norm = i.toFloat() / (NUM_BARS - 1)
        (0.20f + 0.80f * sin(norm * PI).toFloat()).coerceIn(0.1f, 1.0f)
    }

    // Frequency weighting curve (compensates for 1/f spectral tilt)
    private val bandWeighting = FloatArray(NUM_BANDS) { i ->
        1.0f + 0.055f * i
    }

    // Working buffers (zero GC allocations on audio thread)
    private val ringBuffer = FloatArray(FFT_SIZE)
    private var ringWritePos = 0
    private var samplesReceived = 0

    private val fftReal = FloatArray(FFT_SIZE)
    private val fftImag = FloatArray(FFT_SIZE)
    private val magnitudes = FloatArray(HALF_FFT)
    private val bandEnergies = FloatArray(NUM_BANDS)
    private val smoothedBands = FloatArray(NUM_BARS)
    private val displayBands = FloatArray(NUM_BARS)

    private var peakEnergy = 0.25f
    private var lastProcessTimeMs = 0L

    private val _waveformBands = MutableStateFlow(FloatArray(NUM_BARS))
    val waveformBands: StateFlow<FloatArray> = _waveformBands.asStateFlow()

    /**
     * Ingests decoded audio PCM Float32 samples from [ViperAudioProcessor].
     * Downmixes interleaved stereo to mono, stores in circular buffer, and triggers FFT
     * analysis throttled to ~50 FPS.
     */
    fun feedAudio(samples: FloatArray, count: Int) {
        if (count <= 0) return

        // Downmix stereo to mono and buffer
        var i = 0
        while (i < count) {
            val left = samples[i]
            val right = if (i + 1 < count) samples[i + 1] else left
            val mono = (left + right) * 0.5f

            ringBuffer[ringWritePos] = mono
            ringWritePos = (ringWritePos + 1) % FFT_SIZE
            samplesReceived++
            i += 2
        }

        val now = System.currentTimeMillis()
        if (samplesReceived >= FFT_SIZE && (now - lastProcessTimeMs) >= MIN_UPDATE_INTERVAL_MS) {
            lastProcessTimeMs = now
            processFft()
        }
    }

    private fun processFft() {
        // Copy unrolled ring buffer to FFT input with Hann window
        val startPos = ringWritePos
        for (i in 0 until FFT_SIZE) {
            val idx = (startPos + i) % FFT_SIZE
            fftReal[i] = ringBuffer[idx] * hannWindow[i]
            fftImag[i] = 0f
        }

        // Execute In-Place Radix-2 Cooley-Tukey FFT
        runFft(fftReal, fftImag)

        // Calculate positive frequency bin magnitudes
        var frameMaxEnergy = 0.001f
        for (i in 0 until HALF_FFT) {
            val r = fftReal[i]
            val im = fftImag[i]
            val mag = sqrt(r * r + im * im)
            magnitudes[i] = mag
            if (mag > frameMaxEnergy) frameMaxEnergy = mag
        }

        // Auto-Gain Control (smooth tracking peak follower)
        peakEnergy = max(frameMaxEnergy, peakEnergy * 0.985f).coerceIn(0.06f, 2.5f)

        // Map 256 bins into 24 perceptual frequency bands
        aggregateBands()

        // Symmetrically project to 48 visualizer bars
        mapSymmetricalBars()

        // Emit updated bar magnitudes
        _waveformBands.value = displayBands.copyOf()
    }

    private fun runFft(real: FloatArray, imag: FloatArray) {
        // 1. Bit-reversal permutation
        for (i in 0 until FFT_SIZE) {
            val j = bitReverseIndices[i]
            if (j > i) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }

        // 2. Cooley-Tukey Radix-2 butterfly stages
        var size = 2
        while (size <= FFT_SIZE) {
            val halfSize = size / 2
            val step = FFT_SIZE / size
            var i = 0
            while (i < FFT_SIZE) {
                for (j in 0 until halfSize) {
                    val k = j * step
                    val c = cosTable[k]
                    val s = sinTable[k]
                    val uR = real[i + j]
                    val uI = imag[i + j]
                    val vR = real[i + j + halfSize] * c - imag[i + j + halfSize] * s
                    val vI = real[i + j + halfSize] * s + imag[i + j + halfSize] * c

                    real[i + j] = uR + vR
                    imag[i + j] = uI + vI
                    real[i + j + halfSize] = uR - vR
                    imag[i + j + halfSize] = uI - vI
                }
                i += size
            }
            size *= 2
        }
    }

    private fun aggregateBands() {
        // Logarithmically distribute 256 bins into 24 frequency bands
        // Low bins (sub-bass / kick) receive fine single-bin resolution; higher bands aggregate wider spans
        val binSplit = intArrayOf(
            1, 2, 3, 4, 5, 7, 9, 12,
            16, 21, 27, 34, 43, 54, 67, 82,
            100, 121, 145, 172, 201, 225, 245, 255
        )

        var startBin = 1
        for (b in 0 until NUM_BANDS) {
            val endBin = binSplit[b].coerceAtMost(HALF_FFT - 1)
            var sum = 0f
            var count = 0
            for (bin in startBin..endBin) {
                sum += magnitudes[bin]
                count++
            }
            val avg = if (count > 0) sum / count else 0f
            bandEnergies[b] = avg * bandWeighting[b]
            startBin = endBin + 1
        }
    }

    private fun mapSymmetricalBars() {
        // Map 24 bands symmetrically from center outwards:
        // Center bars (23, 24) = Band 0 (Sub-bass / Kick)
        // Mid bars (12..22 and 25..35) = Vocal & Instrument Mids
        // Outer bars (0..11 and 36..47) = Highs & Treble Shimmer
        for (b in 0 until NUM_BANDS) {
            val rawNorm = (bandEnergies[b] / peakEnergy).coerceIn(0f, 1f)

            val leftIdx = 23 - b
            val rightIdx = 24 + b

            applyBallistics(leftIdx, rawNorm)
            applyBallistics(rightIdx, rawNorm)
        }
    }

    private fun applyBallistics(index: Int, targetVal: Float) {
        val shapedTarget = targetVal * envelopeCurve[index]
        val current = smoothedBands[index]

        // Fast attack (instant punch on beats), smooth exponential decay
        val updated = if (shapedTarget > current) {
            current * 0.30f + shapedTarget * 0.70f
        } else {
            (current * 0.85f).coerceAtLeast(0.04f)
        }

        smoothedBands[index] = updated
        displayBands[index] = updated
    }

    /**
     * Resets visualizer state to calm baseline resting state on pause/stop.
     */
    fun onPause() {
        for (i in 0 until NUM_BARS) {
            smoothedBands[i] = 0.04f
            displayBands[i] = 0.04f
        }
        _waveformBands.value = displayBands.copyOf()
    }
}
