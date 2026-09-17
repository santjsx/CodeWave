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
        assertTrue("Must provide at least 12 studio presets", presets.size >= 12)

        presets.forEach { preset ->
            assertEquals("Preset ${preset.name} must have 10 bands", 10, preset.bandGainsDb.size)
            // Preamp gain should be conservative (<= 0 dB) to prevent clipping when boosting bands (PRD Section 38 & 39)
            assertTrue("Preset ${preset.name} preamp must prevent clipping", preset.preampGainDb <= 0f)
            assertTrue("Preset ${preset.name} must have a non-blank subtitle", preset.profileSubtitle.isNotBlank())
        }
    }

    @Test
    fun testCoreStudioProfilesPresence() {
        val presets = EQPreset.PRESETS_10_BAND
        val coreProfiles = listOf("Flat", "Bass Boost", "Vocal Clarity", "Electronic", "Rock", "Acoustic", "Dance")
        for (profile in coreProfiles) {
            assertTrue("Preset list must contain $profile", presets.any { it.name.equals(profile, ignoreCase = true) })
        }
    }

    @Test
    fun testEqualizerConfigDefaultValues() {
        val config = EqualizerConfig()
        assertTrue("Limiter should be enabled by default to prevent digital clipping", config.isLimiterEnabled)
        assertEquals("Preamp gain should default to 0 dB", 0.0f, config.preampGainDb, 0.001f)
        assertEquals("Bass gain should default to +4.0 dB", 4.0f, config.bassGainDb, 0.001f)
        assertEquals("Clarity gain should default to +3.0 dB", 3.0f, config.clarityGainDb, 0.001f)
        assertEquals("Default active preset should be Flat", "Flat", config.activePresetName)
        assertEquals(10, config.bands.size)
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

    @Test
    fun testPresetIdDeterminismAndUniqueness() {
        val presets = EQPreset.PRESETS_10_BAND
        val ids = presets.map { it.id }
        val names = presets.map { it.name.lowercase() }

        assertEquals("Preset IDs must all be unique", ids.distinct().size, ids.size)
        assertEquals("Preset names must all be unique (case-insensitive)", names.distinct().size, names.size)
        assertEquals("There must be exactly 12 built-in presets", 12, presets.size)

        presets.forEach { preset ->
            assertTrue("Preset ID must be >= 1", preset.id >= 1L)
            assertTrue("Preset ID must be <= 12", preset.id <= 12L)
        }
    }

    @Test
    fun testPresetDeduplicationLogic() {
        // Simulate a database with 3x duplicates (36 items total)
        val duplicatedPresets = (EQPreset.PRESETS_10_BAND + EQPreset.PRESETS_10_BAND + EQPreset.PRESETS_10_BAND)
            .mapIndexed { index, preset ->
                preset.copy(id = index + 1L)
            }
        assertEquals(36, duplicatedPresets.size)

        // Apply deduplication pattern used in EqualizerRepository
        val deduplicated = duplicatedPresets.distinctBy { it.name.lowercase() }
        assertEquals("Deduplicated list must contain exactly 12 profiles", 12, deduplicated.size)
        assertEquals(
            "All profile names must be distinct",
            12,
            deduplicated.map { it.name.lowercase() }.toSet().size
        )
    }
}
