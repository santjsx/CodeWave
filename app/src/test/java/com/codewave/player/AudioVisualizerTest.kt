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
        val sampleCount = 2048
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
    fun testVisualizerPauseResetsToBaseline() {
        val visualizer = AudioVisualizerProcessor()

        // Feed some loud audio
        val loud = FloatArray(1024) { 0.9f }
        visualizer.feedAudio(loud, 1024)

        // Call onPause
        visualizer.onPause()

        val bands = visualizer.waveformBands.value
        for (b in bands) {
            assertEquals("Paused band should be at baseline 0.04f", 0.04f, b, 0.001f)
        }
    }
}
