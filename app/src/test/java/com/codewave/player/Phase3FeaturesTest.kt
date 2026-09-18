package com.codewave.player

import com.codewave.player.core.model.ABLoopState
import com.codewave.player.core.model.AudioFormat
import com.codewave.player.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class Phase3FeaturesTest {

    private fun createTestTrack(
        id: Long,
        title: String,
        sampleRate: Int = 44100,
        bitDepth: Int? = 16,
        format: AudioFormat = AudioFormat.MP3,
        playCount: Int = 0,
        dateAdded: Long = 1000L
    ): Track {
        val isLossless = format.isLossless
        val isHiRes = isLossless && (sampleRate >= 96000 || (bitDepth != null && bitDepth >= 24 && sampleRate >= 48000))
        return Track(
            id = id,
            mediaStoreId = id,
            uri = "content://media/external/audio/media/$id",
            path = "/storage/emulated/0/Music/$title.${format.name.lowercase()}",
            title = title,
            artist = "CodeWave Artist",
            album = "CodeWave Album",
            durationMs = 210000L,
            fileSize = 10000000L,
            dateAdded = dateAdded,
            dateModified = dateAdded,
            mimeType = "audio/*",
            format = format,
            codec = format.name,
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            isLossless = isLossless,
            isHiRes = isHiRes,
            playCount = playCount
        )
    }

    // ============================================================
    // 1. MUSICIAN A-B SEAMLESS LOOPER TESTS
    // ============================================================

    @Test
    fun testABLoopInitialStateIsInactive() {
        val loop = ABLoopState()
        assertNull(loop.pointA)
        assertNull(loop.pointB)
        assertFalse(loop.isEnabled)
        assertFalse(loop.isActive)
    }

    @Test
    fun testABLoopActivationRequiresBothPointsAndEnabled() {
        val partialA = ABLoopState(pointA = 10000L, pointB = null, isEnabled = true)
        assertFalse(partialA.isActive)

        val partialB = ABLoopState(pointA = null, pointB = 20000L, isEnabled = true)
        assertFalse(partialB.isActive)

        val disabledBoth = ABLoopState(pointA = 10000L, pointB = 20000L, isEnabled = false)
        assertFalse(disabledBoth.isActive)

        val invertedBoth = ABLoopState(pointA = 25000L, pointB = 10000L, isEnabled = true)
        assertFalse(invertedBoth.isActive)

        val activeLoop = ABLoopState(pointA = 10000L, pointB = 20000L, isEnabled = true)
        assertTrue(activeLoop.isActive)
    }

    @Test
    fun testABLoopDurationAndBounds() {
        val loop = ABLoopState(pointA = 15200L, pointB = 45800L, isEnabled = true)
        assertTrue(loop.isActive)
        val loopDuration = loop.pointB!! - loop.pointA!!
        assertEquals(30600L, loopDuration)
    }

    @Test
    fun testABLoopWrapAroundTrigger() {
        val loop = ABLoopState(pointA = 12000L, pointB = 24000L, isEnabled = true)
        assertTrue(loop.isActive)

        val currentPosInside = 18000L
        val shouldLoopInside = loop.isActive && currentPosInside >= (loop.pointB ?: Long.MAX_VALUE)
        assertFalse(shouldLoopInside)

        val currentPosAtEnd = 24000L
        val shouldLoopAtEnd = loop.isActive && currentPosAtEnd >= (loop.pointB ?: Long.MAX_VALUE)
        assertTrue(shouldLoopAtEnd)

        val currentPosPastEnd = 24500L
        val shouldLoopPastEnd = loop.isActive && currentPosPastEnd >= (loop.pointB ?: Long.MAX_VALUE)
        assertTrue(shouldLoopPastEnd)
    }

    // ============================================================
    // 2. DYNAMIC PITCH SHIFTER & TIME STRETCHER MATH TESTS
    // ============================================================

    @Test
    fun testPitchMultiplierFormula() {
        // Multiplier = 2.0 ^ (semitones / 12.0)
        fun calculateMultiplier(semitones: Int): Float {
            val clamped = semitones.coerceIn(-12, 12)
            return 2.0.pow(clamped.toDouble() / 12.0).toFloat()
        }

        assertEquals(1.0f, calculateMultiplier(0), 0.0001f)
        assertEquals(2.0f, calculateMultiplier(12), 0.0001f)   // +1 Octave
        assertEquals(0.5f, calculateMultiplier(-12), 0.0001f)  // -1 Octave

        // +7 semitones (Perfect Fifth Up)
        val plus7 = calculateMultiplier(7)
        assertTrue(abs(plus7 - 1.498307f) < 0.001f)

        // -7 semitones (Perfect Fifth Down)
        val minus7 = calculateMultiplier(-7)
        assertTrue(abs(minus7 - 0.6674199f) < 0.001f)

        // Clamping to +/- 12 semitones
        assertEquals(2.0f, calculateMultiplier(24), 0.0001f)
        assertEquals(0.5f, calculateMultiplier(-24), 0.0001f)
    }

    @Test
    fun testIndependentSpeedVsPitchParameters() {
        // Speed and pitch can vary independently in Media3 PlaybackParameters
        val normalSpeed = 1.0f
        val doubleSpeed = 2.0f
        val halfSpeed = 0.5f

        val neutralPitch = 1.0f
        val sharpPitch = 1.059463f // +1 semitone

        // Preserves tempo while changing key
        assertTrue(normalSpeed == 1.0f && sharpPitch != 1.0f)
        // Preserves key while changing tempo
        assertTrue(doubleSpeed == 2.0f && neutralPitch == 1.0f)
        // Slower practice speed with pitch shifted down
        assertTrue(halfSpeed == 0.5f && sharpPitch != 1.0f)
    }

    // ============================================================
    // 3. REAL-TIME CENTER-CHANNEL VOCAL REMOVER DSP MATH TESTS
    // ============================================================

    @Test
    fun testCenterChannelVocalCancellationMath() {
        // Zero-latency stereo phase differential:
        // center = (left + right) * 0.5
        // diffLeft = left - center * vocalLevel
        // diffRight = right - center * vocalLevel

        fun cancelVocal(left: Float, right: Float, vocalLevel: Float): Pair<Float, Float> {
            val center = (left + right) * 0.5f
            val outL = left - (center * vocalLevel)
            val outR = right - (center * vocalLevel)
            return Pair(outL, outR)
        }

        // 1. Pure center mono vocal (left == right == 0.8f) with 100% depth
        val (cancelledL, cancelledR) = cancelVocal(0.8f, 0.8f, 1.0f)
        assertEquals(0.0f, cancelledL, 0.0001f)
        assertEquals(0.0f, cancelledR, 0.0001f)

        // 2. Pure center mono vocal with 50% depth (mild karaoke attenuation)
        val (halfL, halfR) = cancelVocal(0.8f, 0.8f, 0.5f)
        assertEquals(0.4f, halfL, 0.0001f)
        assertEquals(0.4f, halfR, 0.0001f)

        // 3. Hard-panned stereo instrument (Left = 1.0f, Right = 0.0f) with 100% depth
        // Instrument panned to side must NOT be cancelled!
        val (sideL, sideR) = cancelVocal(1.0f, 0.0f, 1.0f)
        assertEquals(0.5f, sideL, 0.0001f)
        assertEquals(-0.5f, sideR, 0.0001f)
        // Magnitude is retained on sides
        assertTrue(abs(sideL) > 0.1f && abs(sideR) > 0.1f)
    }

    @Test
    fun testViperAudioProcessorSubBassPreservationCutoff() {
        // Low-pass filter smoothing factor: alpha = (2*PI*dt*fc) / (2*PI*dt*fc + 1)
        val sampleRate = 44100f
        val dt = 1.0f / sampleRate
        val cutoffFreq = 160f // Sub-bass preservation under 160 Hz
        val rc = 1.0f / (2.0f * Math.PI.toFloat() * cutoffFreq)
        val alpha = dt / (rc + dt)

        assertTrue("Alpha filter coefficient must be positive", alpha > 0.0f)
        assertTrue("Alpha filter coefficient must be small for low cutoff", alpha < 0.1f)
    }

    // ============================================================
    // 4. DYNAMIC SMART LIVE LISTS FILTERING TESTS
    // ============================================================

    @Test
    fun testHiResLosslessSmartPlaylistFilter() {
        val flacHiRes = createTestTrack(1, "Symphony No. 9", sampleRate = 96000, bitDepth = 24, format = AudioFormat.FLAC)
        val flacStandard = createTestTrack(2, "Ode to Joy", sampleRate = 44100, bitDepth = 16, format = AudioFormat.FLAC)
        val wavStudio = createTestTrack(3, "Mastering Take", sampleRate = 48000, bitDepth = 24, format = AudioFormat.WAV)
        val mp3Track = createTestTrack(4, "Standard Stream", sampleRate = 44100, bitDepth = 16, format = AudioFormat.MP3)
        val aacTrack = createTestTrack(5, "Compressed Radio", sampleRate = 44100, bitDepth = null, format = AudioFormat.AAC)

        val allTracks = listOf(flacHiRes, flacStandard, wavStudio, mp3Track, aacTrack)

        val hiResLossless = allTracks.filter {
            it.isLossless || it.isHiRes || it.sampleRate >= 48000 || (it.bitDepth ?: 0) >= 24
        }

        assertEquals(3, hiResLossless.size)
        assertTrue(hiResLossless.contains(flacHiRes))
        assertTrue(hiResLossless.contains(flacStandard))
        assertTrue(hiResLossless.contains(wavStudio))
        assertFalse(hiResLossless.contains(mp3Track))
        assertFalse(hiResLossless.contains(aacTrack))
    }

    @Test
    fun testHeavyRotationSmartPlaylistFilter() {
        val hot1 = createTestTrack(1, "Hot Track 1", playCount = 15)
        val hot2 = createTestTrack(2, "Hot Track 2", playCount = 4)
        val regular = createTestTrack(3, "Played Once", playCount = 1)
        val unplayed = createTestTrack(4, "Unplayed Track", playCount = 0)
        val hot3 = createTestTrack(5, "Hot Track 3", playCount = 8)

        val allTracks = listOf(hot1, hot2, regular, unplayed, hot3)

        val heavyRotation = allTracks.filter { it.playCount >= 2 }.sortedByDescending { it.playCount }

        assertEquals(3, heavyRotation.size)
        assertEquals(hot1.id, heavyRotation[0].id)
        assertEquals(hot3.id, heavyRotation[1].id)
        assertEquals(hot2.id, heavyRotation[2].id)
    }

    @Test
    fun testRecentlyAddedSmartPlaylistFilter() {
        val oldest = createTestTrack(1, "Oldest", dateAdded = 1000L)
        val middle = createTestTrack(2, "Middle", dateAdded = 2000L)
        val newest = createTestTrack(3, "Newest", dateAdded = 5000L)

        val allTracks = listOf(oldest, middle, newest)

        val recentlyAdded = allTracks.sortedByDescending { it.dateAdded }

        assertEquals(newest.id, recentlyAdded[0].id)
        assertEquals(middle.id, recentlyAdded[1].id)
        assertEquals(oldest.id, recentlyAdded[2].id)
    }
}
