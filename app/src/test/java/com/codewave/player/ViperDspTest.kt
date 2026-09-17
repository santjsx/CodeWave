package com.codewave.player

import com.codewave.player.core.audio.DolbyLevelEqualizer
import com.codewave.player.core.audio.IrsParser
import com.codewave.player.core.audio.PremiumBiquadFilter
import com.codewave.player.core.audio.ViperAudioProcessor
import com.codewave.player.core.model.EqualizerBand
import com.codewave.player.core.model.EqualizerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class ViperDspTest {

    @Test
    fun testBiquadFilterStabilityAndSmoothing() {
        val filter = PremiumBiquadFilter(sampleRate = 48000.0)
        filter.updateCoefficients(centerFreq = 1000.0, gainDb = 6.0, qFactor = 1.414)

        // Process stereo buffer
        val buffer = floatArrayOf(0.5f, -0.5f, 0.2f, -0.2f)
        filter.processStereoPair(buffer, 0)
        filter.processStereoPair(buffer, 2)

        // Output must be finite and within valid bounds
        for (sample in buffer) {
            assertTrue("Sample must be finite", sample.isFinite())
            assertTrue("Sample must not blow up", abs(sample) < 2.0f)
        }

        // Test filter reset
        filter.reset()
        val zeroBuffer = floatArrayOf(0f, 0f)
        filter.processStereoPair(zeroBuffer, 0)
        assertEquals(0f, zeroBuffer[0], 0.001f)
        assertEquals(0f, zeroBuffer[1], 0.001f)
    }

    @Test
    fun testDolbyEqualizerHeadroomGuardCalculation() {
        val eq = DolbyLevelEqualizer(sampleRate = 48000.0)
        
        // Initial state: 0 dB across all bands -> headroom factor is 1.0 (0 dB attenuation)
        assertEquals(1.0f, eq.preAmpHeadroomFactor, 0.001f)

        // Moderate boost: +3 dB (<= 4 dB threshold) -> no artificial attenuation needed, factor stays 1.0
        eq.setBandGain(5, 3.0)
        assertEquals(1.0f, eq.preAmpHeadroomFactor, 0.001f)

        // Boost 1kHz band by +6 dB -> guardDb = (6.0 - 4.0) * 0.45 = 0.9 dB
        eq.setBandGain(5, 6.0)
        val expected6dB = Math.pow(10.0, -((6.0 - 4.0) * 0.45) / 20.0).toFloat()
        assertEquals(expected6dB, eq.preAmpHeadroomFactor, 0.005f)

        // Boost another band by +12 dB -> max boost is 12 dB -> guardDb = (12.0 - 4.0) * 0.45 = 3.6 dB
        eq.setBandGain(0, 12.0)
        val expected12dB = Math.pow(10.0, -((12.0 - 4.0) * 0.45) / 20.0).toFloat()
        assertEquals(expected12dB, eq.preAmpHeadroomFactor, 0.005f)

        // Cut band back to 0 dB and verify headroom relaxes to 1.0
        eq.setBandGain(0, 0.0)
        eq.setBandGain(5, 0.0)
        assertEquals(1.0f, eq.preAmpHeadroomFactor, 0.001f)
    }

    @Test
    fun testDolbyEqualizerSoftLimiter() {
        val eq = DolbyLevelEqualizer(sampleRate = 48000.0)
        eq.isEnabled = true
        // Set user preamp to max boost to force hot signal
        eq.setUserPreampDb(12f)

        val hotBuffer = floatArrayOf(1.8f, -1.8f, 2.5f, -2.5f)
        eq.processAudioInterleaved(hotBuffer, 2)

        // Soft limiter ensures all samples remain within [-1.0, 1.0] without hard clipping clicks
        for (sample in hotBuffer) {
            assertTrue("Sample must be finite", sample.isFinite())
            assertTrue("Soft limiter must keep sample within [-1.0, 1.0], was $sample", abs(sample) <= 1.0f)
        }
    }

    @Test
    fun testLimiterBypassToggle() {
        val eq = DolbyLevelEqualizer(sampleRate = 48000.0)
        eq.isEnabled = true
        eq.isLimiterEnabled = false
        eq.setUserPreampDb(12f)

        val hotBuffer = floatArrayOf(1.8f, -1.8f)
        eq.processAudioInterleaved(hotBuffer, 1)

        // When limiter is disabled, hot samples hit the digital ceiling and are hard-clipped at exactly 1.0f
        assertEquals("Limiter bypassed should hard-clip at exactly 1.0f", 1.0f, abs(hotBuffer[0]), 0.001f)

        // Enable soft limiter
        eq.isLimiterEnabled = true
        val limitedBuffer = floatArrayOf(1.8f, -1.8f)
        eq.processAudioInterleaved(limitedBuffer, 1)
        // Soft limiter smoothly compresses the knee below 1.0f without harsh digital flat-topping
        assertTrue("Limiter enabled should smoothly compress below 1.0f", abs(limitedBuffer[0]) < 1.0f)
        assertTrue("Limiter enabled should preserve dynamic range above threshold", abs(limitedBuffer[0]) >= 0.95f)
    }

    @Test
    fun testIrsParserWithSyntheticWav() {
        // Build a minimal valid 16-bit PCM mono 44100Hz WAV with 4 samples
        val numSamples = 4
        val byteRate = 44100 * 1 * 2
        val dataSize = numSamples * 2
        val totalSize = 36 + dataSize

        val byteBuffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.put("RIFF".toByteArray())
        byteBuffer.putInt(totalSize)
        byteBuffer.put("WAVE".toByteArray())
        byteBuffer.put("fmt ".toByteArray())
        byteBuffer.putInt(16) // Subchunk1Size for PCM
        byteBuffer.putShort(1) // AudioFormat 1 = PCM
        byteBuffer.putShort(1) // NumChannels 1 = Mono
        byteBuffer.putInt(44100) // SampleRate
        byteBuffer.putInt(byteRate) // ByteRate
        byteBuffer.putShort(2) // BlockAlign
        byteBuffer.putShort(16) // BitsPerSample
        byteBuffer.put("data".toByteArray())
        byteBuffer.putInt(dataSize)

        // Samples: impulse 16384 (0.5), then 0, 0, 0
        byteBuffer.putShort(16384.toShort())
        byteBuffer.putShort(0.toShort())
        byteBuffer.putShort(0.toShort())
        byteBuffer.putShort(0.toShort())

        val wavBytes = byteBuffer.array()
        val irsData = IrsParser.parse(ByteArrayInputStream(wavBytes))

        assertNotNull("IRS Parser should successfully parse valid WAV header", irsData)
        assertEquals(44100, irsData!!.sampleRate)
        assertEquals(1, irsData.channels)
        assertEquals(numSamples, irsData.samples.size)
        // Energy normalization may scale peak to 1.0f
        assertTrue("Impulse sample should be positive", irsData.samples[0] > 0.4f)
    }

    @Test
    fun testIrsParserCorruptedDataSafety() {
        // Truncated bytes
        val truncated = ByteArray(20)
        assertNull(IrsParser.parse(ByteArrayInputStream(truncated)))

        // Bad magic header
        val badHeader = "NOT_A_WAV_FILE_HEADER_PADDING_PADDING_PADDING".toByteArray()
        assertNull(IrsParser.parse(ByteArrayInputStream(badHeader)))
    }

    @Test
    fun testViperAudioProcessorPipeline() {
        val processor = ViperAudioProcessor()

        val format = androidx.media3.common.audio.AudioProcessor.AudioFormat(
            44100,
            2,
            androidx.media3.common.C.ENCODING_PCM_16BIT
        )
        processor.configure(format)
        processor.flush()

        val config = EqualizerConfig(
            isEnabled = true,
            preampGainDb = 0f,
            isLimiterEnabled = true,
            activePresetName = "Test",
            bands = EqualizerConfig.defaultBands(),
            isBassEnabled = true,
            bassGainDb = 4.0f,
            isClarityEnabled = true,
            clarityGainDb = 3.0f,
            isConvolverEnabled = false
        )

        processor.applyConfig(config)
        assertTrue("Processor should be active after configuration", processor.isActive)

        // Queue input buffer and verify output
        val input = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until 16) {
            input.putShort((i * 500).toShort())
        }
        input.flip()

        processor.queueInput(input)
        val output = processor.output
        assertTrue("Output buffer should have remaining bytes", output.remaining() > 0)
    }
}
