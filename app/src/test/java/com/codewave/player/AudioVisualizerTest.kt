package com.codewave.player

import com.codewave.player.core.audio.AudioVisualizerProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AudioVisualizerTest {

    @Test
    fun testVisualizerInitialState() {
        val visualizer = AudioVisualizerProcessor()
        val bands = visualizer.waveformBands.value

        assertEquals(48, bands.size)
        // All initial bands should be finite and non-negative
        for (b in bands) {
            assertTrue("Band must be finite", b.isFinite())
            assertTrue("Band must be non-negative", b >= 0f)
        }
    }

    @Test
    fun testVisualizerProcessesAudioAndProducesValidFrequencies() {
        val visualizer = AudioVisualizerProcessor()

        // Generate synthetic stereo audio with 100 Hz bass sine wave at 44.1 kHz
        val sampleCount = 4096
        val sampleRate = 44100.0
        val freq = 100.0 // Bass kick frequency
        val testBuffer = FloatArray(sampleCount)

        for (i in 0 until sampleCount step 2) {
            val t = (i / 2) / sampleRate
            val s = sin(2.0 * PI * freq * t).toFloat() * 0.8f
            testBuffer[i] = s     // Left
            testBuffer[i + 1] = s // Right
        }

        // Feed audio chunks
        visualizer.feedAudio(testBuffer, sampleCount)

        val bands = visualizer.waveformBands.value
        assertEquals(48, bands.size)

        // Center bands (indices 20-27) represent sub-bass and bass
        // They should have high energy compared to baseline
        var centerEnergySum = 0f
        for (i in 20..27) {
            centerEnergySum += bands[i]
            assertTrue("Band $i must be <= 1.0f", bands[i] <= 1.0f)
            assertTrue("Band $i must be >= 0.0f", bands[i] >= 0.0f)
        }

        assertTrue("Bass center bands should react to 100Hz tone", centerEnergySum > 0.1f)
    }

    @Test
    fun testStereoSeparation() {
        val visualizer = AudioVisualizerProcessor()

        // Feed audio with loud tone ONLY in left channel, right channel silent
        val sampleCount = 4096
        val sampleRate = 44100.0
        val freq = 1000.0
        val testBuffer = FloatArray(sampleCount)

        for (i in 0 until sampleCount step 2) {
            val t = (i / 2) / sampleRate
            testBuffer[i] = sin(2.0 * PI * freq * t).toFloat() * 0.9f // Left loud
            testBuffer[i + 1] = 0f                                    // Right silent
        }

        visualizer.feedAudio(testBuffer, sampleCount)

        val bands = visualizer.waveformBands.value
        var leftEnergy = 0f
        var rightEnergy = 0f

        for (i in 0 until 24) {
            leftEnergy += bands[i]
        }
        for (i in 24 until 48) {
            rightEnergy += bands[i]
        }

        assertTrue("Left channel must register significantly higher energy than silent right channel", leftEnergy > rightEnergy)
    }

    @Test
    fun testVisualizerPauseResetsToBaseline() {
        val visualizer = AudioVisualizerProcessor()

        // Feed some loud audio
        val loud = FloatArray(2048) { 0.9f }
        visualizer.feedAudio(loud, 2048)

        // Call onPause
        visualizer.onPause()

        val bands = visualizer.waveformBands.value
        for (b in bands) {
            assertEquals("Paused band should be at baseline 0.04f", 0.04f, b, 0.001f)
        }
    }
}
