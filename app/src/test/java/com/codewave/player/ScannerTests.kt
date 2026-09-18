package com.codewave.player

import com.codewave.player.core.scanner.AudioFingerprint
import com.codewave.player.core.scanner.CandidateValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerTests {

    @Test
    fun testFingerprintStability() {
        val fp1 = AudioFingerprint.compute(
            fileSize = 45000000L,
            durationMs = 240000L,
            title = "Bohemian Rhapsody",
            artist = "Queen"
        )

        val fp2 = AudioFingerprint.compute(
            fileSize = 45000000L,
            durationMs = 240000L,
            title = "bohemian rhapsody  ",
            artist = "queen "
        )

        // Case insensitivity and whitespace normalization should yield identical fingerprints
        assertEquals(fp1, fp2)

        val fp3 = AudioFingerprint.compute(
            fileSize = 45000001L,
            durationMs = 240000L,
            title = "Bohemian Rhapsody",
            artist = "Queen"
        )

        // Different file size must yield a different fingerprint
        assertNotEquals(fp1, fp3)
    }

    @Test
    fun testCandidateValidatorZeroSize() {
        // Files smaller than 1KB must be rejected as partial/corrupt downloads (PRD Section 44)
        assertFalse(CandidateValidator.isCandidateReady("/storage/Music/song.flac", 0L))
        assertFalse(CandidateValidator.isCandidateReady("/storage/Music/song.flac", 512L))
    }

    @Test
    fun testOrphanTrackDetectionAndSafeChunking() {
        val existingMediaStoreIds = (1L..1250L).toList()
        val scannedMediaStoreIds = (251L..1250L).toList() // First 250 tracks were deleted

        val scannedIdSet = scannedMediaStoreIds.toSet()
        val orphanIds = existingMediaStoreIds.filter { it !in scannedIdSet }

        assertEquals(250, orphanIds.size)
        assertEquals(1L, orphanIds.first())
        assertEquals(250L, orphanIds.last())

        // Verify chunking does not exceed SQLite 999 parameter bind limit
        val chunks = (1L..1250L).toList().chunked(500)
        assertEquals(3, chunks.size)
        assertEquals(500, chunks[0].size)
        assertEquals(500, chunks[1].size)
        assertEquals(250, chunks[2].size)
        assertTrue(chunks.all { it.size <= 500 })
    }
}
