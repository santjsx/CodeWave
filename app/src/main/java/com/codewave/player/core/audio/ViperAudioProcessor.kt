package com.codewave.player.core.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.codewave.player.core.model.EqualizerConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Media3 AudioProcessor that intercepts raw PCM streams between ExoPlayer's decoder
 * and the native AudioTrack. Routes audio through the native C++ ViPER/Dolby DSP engine
 * (or pure Kotlin fallback), delivering zero-latency, clipping-free audio processing.
 */
@UnstableApi
class ViperAudioProcessor : BaseAudioProcessor() {

    val visualizer: AudioVisualizerProcessor = AudioVisualizerProcessor()
    private val kotlinFallbackEq = DolbyLevelEqualizer()
    private var floatBuffer = FloatArray(4096)
    private var lastConfig: EqualizerConfig = EqualizerConfig()
    private var isVocalRemoverEnabled: Boolean = false
    private var vocalRemoverLevel: Float = 0.85f
    private var bassFilterState: Float = 0.0f
    private var sampleRateHz: Double = 44100.0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        val sampleRate = inputAudioFormat.sampleRate.toDouble()
        sampleRateHz = sampleRate
        if (ViperJniWrapper.isNativeAvailable) {
            ViperJniWrapper.nativeInit(sampleRate)
        }
        kotlinFallbackEq.updateSampleRate(sampleRate)

        // Ensure stereo processing
        return AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            2,
            inputAudioFormat.encoding
        )
    }

    override fun isActive(): Boolean {
        // Active when configured so dynamic slider changes take effect immediately without sink re-allocations
        return inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remainingBytes = inputBuffer.remaining()
        if (remainingBytes == 0) return

        val is16Bit = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT
        val bytesPerSample = if (is16Bit) 2 else 4
        val bytesPerFrame = bytesPerSample * 2 // Stereo = 2 channels
        val frameCount = remainingBytes / bytesPerFrame
        if (frameCount <= 0) return

        val samplesCount = frameCount * 2
        if (floatBuffer.size < samplesCount) {
            floatBuffer = FloatArray(samplesCount)
        }

        // STEP 1: Translate raw PCM stream into Float32 normalized to [-1.0, 1.0]
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        if (is16Bit) {
            for (i in 0 until samplesCount) {
                floatBuffer[i] = inputBuffer.short.toFloat() / 32768.0f
            }
        } else {
            for (i in 0 until samplesCount) {
                floatBuffer[i] = inputBuffer.float.coerceIn(-1.0f, 1.0f)
            }
        }

        // Feed real-time audio samples to Visualizer for live frequency analysis
        visualizer.feedAudio(floatBuffer, samplesCount)

        // STEP 2: Execute High-Precision DSP Engine (Native C++ with Kotlin fallback)
        if (ViperJniWrapper.isNativeAvailable) {
            ViperJniWrapper.nativeProcessStereo(floatBuffer, frameCount)
        } else {
            kotlinFallbackEq.processAudioInterleaved(floatBuffer, frameCount)
        }

        // STEP 2.5: Real-Time Vocal Suppression / Center-Channel Differential Filter
        if (isVocalRemoverEnabled && vocalRemoverLevel > 0f) {
            // Cutoff frequency ~ 160Hz for sub-bass preservation (kick drum & bassline)
            val dt = 1.0f / sampleRateHz.toFloat().coerceAtLeast(8000f)
            val rc = 1.0f / (2.0f * Math.PI.toFloat() * 160.0f)
            val alpha = (dt / (rc + dt)).coerceIn(0.001f, 0.5f)
            val level = vocalRemoverLevel

            var i = 0
            while (i < samplesCount) {
                val left = floatBuffer[i]
                val right = floatBuffer[i + 1]

                // Center channel (mono components, typical of lead vocals and bass)
                val center = (left + right) * 0.5f

                // IIR Low-pass filter on center channel to isolate sub-bass
                bassFilterState += alpha * (center - bassFilterState)
                val subBass = bassFilterState

                // Subtract center channel from stereo channels (cancelling vocal frequencies)
                // while re-injecting the sub-bass to maintain rhythmic weight and warmth
                val outLeft = (left - center * level) + (subBass * level)
                val outRight = (right - center * level) + (subBass * level)

                floatBuffer[i] = outLeft
                floatBuffer[i + 1] = outRight
                i += 2
            }
        }

        // STEP 3: Convert processed Float32 samples back to output PCM byte buffer
        val outputBuffer = replaceOutputBuffer(frameCount * bytesPerFrame)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        if (is16Bit) {
            for (i in 0 until samplesCount) {
                val clamped = floatBuffer[i].coerceIn(-1.0f, 1.0f)
                val pcmShort = (clamped * 32767.0f).toInt().toShort()
                outputBuffer.putShort(pcmShort)
            }
        } else {
            for (i in 0 until samplesCount) {
                outputBuffer.putFloat(floatBuffer[i].coerceIn(-1.0f, 1.0f))
            }
        }

        outputBuffer.flip()
    }

    override fun onFlush() {
        if (ViperJniWrapper.isNativeAvailable) {
            ViperJniWrapper.nativeReset()
        }
        kotlinFallbackEq.reset()
        bassFilterState = 0.0f
    }

    override fun onReset() {
        onFlush()
    }

    fun applyConfig(config: EqualizerConfig) {
        lastConfig = config
        val isGlobalEnabled = config.isEnabled
        isVocalRemoverEnabled = config.isVocalRemoverEnabled
        vocalRemoverLevel = config.vocalRemoverLevel.coerceIn(0.0f, 1.0f)

        if (ViperJniWrapper.isNativeAvailable) {
            ViperJniWrapper.nativeSetGlobalEnabled(isGlobalEnabled)
            ViperJniWrapper.nativeSetEqEnabled(isGlobalEnabled)
            ViperJniWrapper.nativeSetLimiterEnabled(config.isLimiterEnabled)
            config.bands.forEachIndexed { index, band ->
                ViperJniWrapper.nativeSetEqBand(index, band.gainDb.toDouble())
            }
            ViperJniWrapper.nativeSetUserPreamp(config.preampGainDb)
            ViperJniWrapper.nativeSetBass(config.isBassEnabled, config.bassGainDb)
            ViperJniWrapper.nativeSetClarity(config.isClarityEnabled, config.clarityGainDb)
            ViperJniWrapper.nativeSetConvolverEnabled(config.isConvolverEnabled)
        }

        kotlinFallbackEq.isEnabled = isGlobalEnabled
        kotlinFallbackEq.isLimiterEnabled = config.isLimiterEnabled
        config.bands.forEachIndexed { index, band ->
            kotlinFallbackEq.setBandGain(index, band.gainDb.toDouble())
        }
        kotlinFallbackEq.setUserPreampDb(config.preampGainDb)
    }

    fun loadImpulseResponse(data: IrsData?) {
        if (data != null && ViperJniWrapper.isNativeAvailable) {
            ViperJniWrapper.nativeLoadImpulseResponse(data.samples, data.samples.size, data.channels)
        } else if (ViperJniWrapper.isNativeAvailable) {
            ViperJniWrapper.nativeLoadImpulseResponse(null, 0, 1)
        }
    }
}
