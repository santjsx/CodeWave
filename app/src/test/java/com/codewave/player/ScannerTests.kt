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
}
