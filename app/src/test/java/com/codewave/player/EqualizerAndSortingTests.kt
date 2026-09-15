package com.codewave.player

import com.codewave.player.core.model.AudioFormat
import com.codewave.player.core.model.EQPreset
import com.codewave.player.core.model.EqualizerConfig
import com.codewave.player.core.model.SongSortOption
import com.codewave.player.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerAndSortingTests {

    @Test
    fun testDefaultEqualizerBands() {
        val bands = EqualizerConfig.defaultBands()
        assertEquals(10, bands.size)
        assertEquals(31, bands[0].centerFreqHz)
        assertEquals(16000, bands[9].centerFreqHz)
        assertEquals("31", bands[0].frequencyLabel)
        assertEquals("1k", bands[5].frequencyLabel)
        assertEquals("16k", bands[9].frequencyLabel)
    }

    @Test
    fun testBuiltInPresetsConsistency() {
        val presets = EQPreset.PRESETS_10_BAND
        assertTrue(presets.isNotEmpty())

        presets.forEach { preset ->
            assertEquals("Preset ${preset.name} must have 10 bands", 10, preset.bandGainsDb.size)
            // Preamp gain should be conservative (<= 0 dB) to prevent clipping when boosting bands (PRD Section 38 & 39)
            assertTrue("Preset ${preset.name} preamp must prevent clipping", preset.preampGainDb <= 0f)
        }
    }

    @Test
    fun testSongSortingLogic() {
        val t1 = createMockTrack(id = 1, title = "Apple", artist = "Zebra", dateAdded = 100L, durationMs = 300000L)
        val t2 = createMockTrack(id = 2, title = "Banana", artist = "Alpha", dateAdded = 300L, durationMs = 120000L)
        val t3 = createMockTrack(id = 3, title = "Cherry", artist = "Beta", dateAdded = 200L, durationMs = 180000L)

        val list = listOf(t1, t2, t3)

        val byDate = list.sortedByDescending { it.dateAdded }
        assertEquals(listOf(t2, t3, t1), byDate)

        val byTitle = list.sortedBy { it.title }
        assertEquals(listOf(t1, t2, t3), byTitle)

        val byArtist = list.sortedBy { it.artist }
        assertEquals(listOf(t2, t3, t1), byArtist)

        val byDuration = list.sortedBy { it.durationMs }
        assertEquals(listOf(t2, t3, t1), byDuration)
    }

    private fun createMockTrack(
        id: Long,
        title: String,
        artist: String,
        dateAdded: Long,
        durationMs: Long
    ): Track {
        return Track(
            id = id,
            mediaStoreId = id,
            uri = "content://media/external/audio/media/$id",
            path = "/storage/emulated/0/Music/$title.flac",
            title = title,
            artist = artist,
            album = "Test Album",
            durationMs = durationMs,
            fileSize = 25000000L,
            dateAdded = dateAdded,
            dateModified = dateAdded,
            mimeType = "audio/flac",
            format = AudioFormat.FLAC,
            codec = "FLAC",
            sampleRate = 96000,
            bitDepth = 24,
            isLossless = true,
            isHiRes = true
        )
    }
}
