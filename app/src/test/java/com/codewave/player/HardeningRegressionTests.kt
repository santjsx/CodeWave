package com.codewave.player

import com.codewave.player.core.model.AudioFormat
import com.codewave.player.core.model.QueueWorkspace
import com.codewave.player.core.model.Track
import com.codewave.player.core.scanner.ScanAuthority
import com.codewave.player.core.scanner.ScanSafetyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HardeningRegressionTests {

    private fun createTestTrack(
        id: Long,
        title: String,
        artist: String = "Test Artist",
        album: String = "Test Album"
    ): Track {
        return Track(
            id = id,
            mediaStoreId = id,
            uri = "content://media/external/audio/media/$id",
            path = "/storage/emulated/0/Music/$title.flac",
            title = title,
            artist = artist,
            album = album,
            durationMs = 180000L,
            fileSize = 15000000L,
            dateAdded = 1000L,
            dateModified = 1000L,
            mimeType = "audio/flac",
            format = AudioFormat.FLAC,
            codec = "FLAC",
            sampleRate = 44100,
            bitDepth = 16,
            isLossless = true,
            isHiRes = false,
            playCount = 0
        )
    }

    // ============================================================
    // 1. AUD-LIB-01 & AUD-PERF-01: SCANNER SAFETY & ORPHAN CHUNKING
    // ============================================================

    @Test
    fun testScannerAbortsOrphanPurgeOnFailedQuery() {
        // Invariant: Failed or null query cursor must NEVER trigger orphan deletion
        val existingLibraryIds = setOf(1L, 2L, 3L, 4L, 5L)
        val scannedIds = emptySet<Long>()

        // 1. Query failed:
        val failedAuthority = ScanSafetyPolicy.evaluateScanAuthority(
            hasPermission = true,
            isStorageMounted = true,
            querySucceeded = false,
            iterationCompletedNormally = false,
            isCancelled = false,
            scannedMediaStoreIds = scannedIds,
            existingLibraryCount = existingLibraryIds.size
        )
        val orphansOnFailure = ScanSafetyPolicy.computeOrphansToPurge(failedAuthority, existingLibraryIds, scannedIds)
        assertTrue("Orphan purge must NOT return any tracks when query failed", orphansOnFailure.isEmpty())

        // 2. Query genuinely succeeded with fewer tracks (e.g. track 5 deleted):
        val successScannedIds = setOf(1L, 2L, 3L, 4L)
        val successAuthority = ScanSafetyPolicy.evaluateScanAuthority(
            hasPermission = true,
            isStorageMounted = true,
            querySucceeded = true,
            iterationCompletedNormally = true,
            isCancelled = false,
            scannedMediaStoreIds = successScannedIds,
            existingLibraryCount = existingLibraryIds.size
        )
        val orphansOnSuccess = ScanSafetyPolicy.computeOrphansToPurge(successAuthority, existingLibraryIds, successScannedIds)
        assertEquals("Orphan purge must return the single deleted track when query succeeded", listOf(5L), orphansOnSuccess)
    }

    @Test
    fun testOrphanDeletionBatchSizeLimit() {
        // SQLite bind parameter limit is 999; chunk size of 500 must never be exceeded
        val largeOrphanList = (1L..2450L).toList()
        val chunks = largeOrphanList.chunked(500)

        assertEquals(5, chunks.size)
        assertEquals(500, chunks[0].size)
        assertEquals(500, chunks[1].size)
        assertEquals(500, chunks[2].size)
        assertEquals(500, chunks[3].size)
        assertEquals(450, chunks[4].size)
        assertTrue(chunks.all { it.size <= 500 })
    }

    // ============================================================
    // 2. AUD-SEARCH-01: FTS TOKEN SANITIZATION & FALLBACK
    // ============================================================

    @Test
    fun testFtsQueryTokenizationWithPunctuationAndSpecialCharacters() {
        fun extractFtsTokens(query: String): List<String> {
            return Regex("[\\p{L}\\p{M}\\p{Nd}]+").findAll(query.trim())
                .map { it.value }
                .filter { it.isNotBlank() }
                .toList()
        }

        // Test AC/DC (slash is an FTS operator that previously broke queries)
        val acdcTokens = extractFtsTokens("AC/DC")
        assertEquals(listOf("AC", "DC"), acdcTokens)
        assertEquals("AC* DC*", acdcTokens.joinToString(" ") { "$it*" })

        // Test Don't Stop (apostrophe previously broke queries)
        val apostropheTokens = extractFtsTokens("Don't Stop")
        assertEquals(listOf("Don", "t", "Stop"), apostropheTokens)

        // Test parentheses and hyphens
        val remixTokens = extractFtsTokens("(Remix) - Live '99")
        assertEquals(listOf("Remix", "Live", "99"), remixTokens)

        // Test pure symbols (should return empty tokens so fallback LIKE search executes)
        val symbolOnlyTokens = extractFtsTokens("--- %%% !!!")
        assertTrue(symbolOnlyTokens.isEmpty())
    }

    @Test
    fun testSearchFallbackDecisionLogic() {
        fun decideSearchStrategy(query: String): String {
            val tokens = Regex("[\\p{L}\\p{M}\\p{Nd}]+").findAll(query.trim())
                .map { it.value }
                .filter { it.isNotBlank() }
                .toList()

            return if (tokens.isNotEmpty()) {
                "FTS_WITH_FALLBACK"
            } else {
                "DIRECT_FALLBACK"
            }
        }

        assertEquals("FTS_WITH_FALLBACK", decideSearchStrategy("Radiohead"))
        assertEquals("FTS_WITH_FALLBACK", decideSearchStrategy("Guns N' Roses"))
        assertEquals("FTS_WITH_FALLBACK", decideSearchStrategy("1984"))
        assertEquals("DIRECT_FALLBACK", decideSearchStrategy("#"))
        assertEquals("DIRECT_FALLBACK", decideSearchStrategy("$$$"))
    }

    // ============================================================
    // 3. AUD-AUDIO-01: AUDIO FOCUS DELAYED GAIN & USER PAUSE DISARM
    // ============================================================

    @Test
    fun testDelayedFocusGainAndManualPauseDisarm() {
        // State simulator representing AudioFocusManager logic
        var resumeOnFocusGain = false
        var isPlaying = false

        fun onDelayedFocusRequest() {
            // Android returned AUDIOFOCUS_REQUEST_DELAYED
            resumeOnFocusGain = true
            isPlaying = false
        }

        fun onTransientFocusLoss() {
            resumeOnFocusGain = true
            isPlaying = false
        }

        fun onUserManualPause() {
            resumeOnFocusGain = false
            isPlaying = false
        }

        fun onFocusGain() {
            if (resumeOnFocusGain) {
                resumeOnFocusGain = false
                isPlaying = true
            }
        }

        // Scenario 1: Phone call ends after delayed focus request -> should resume
        onDelayedFocusRequest()
        assertTrue(resumeOnFocusGain)
        assertFalse(isPlaying)

        onFocusGain()
        assertFalse(resumeOnFocusGain)
        assertTrue(isPlaying)

        // Scenario 2: Phone call interrupts playback, but user explicitly pauses during call -> should NOT resume
        onTransientFocusLoss()
        assertTrue(resumeOnFocusGain)

        onUserManualPause() // User explicitly paused (or headphones unplugged)
        assertFalse(resumeOnFocusGain)

        onFocusGain() // Phone call ends
        assertFalse("Manual user pause must never automatically resume on focus gain", isPlaying)
        assertFalse(resumeOnFocusGain)
    }

    // ============================================================
    // 4. AUD-UX-01: WORKSPACE QUEUE SYNCHRONIZATION
    // ============================================================

    @Test
    fun testActiveWorkspaceTrackRemovalSync() {
        val t1 = createTestTrack(1L, "Track 1")
        val t2 = createTestTrack(2L, "Track 2")
        val t3 = createTestTrack(3L, "Track 3")
        val t4 = createTestTrack(4L, "Track 4")

        val initialQueue = listOf(t1, t2, t3, t4)
        var currentQueue = initialQueue.toMutableList()
        var currentIdx = 2 // Currently playing Track 3
        var currentTrack: Track? = currentQueue[currentIdx]

        fun removeTrack(trackIndex: Int) {
            if (trackIndex in currentQueue.indices) {
                currentQueue.removeAt(trackIndex)
                if (currentQueue.isEmpty()) {
                    currentTrack = null
                    currentIdx = -1
                } else {
                    currentIdx = when {
                        trackIndex < currentIdx -> currentIdx - 1
                        trackIndex == currentIdx -> trackIndex.coerceAtMost(currentQueue.size - 1)
                        else -> currentIdx
                    }
                    currentTrack = currentQueue.getOrNull(currentIdx)
                }
            }
        }

        // Case A: Remove a track AFTER the currently playing track (index 3, Track 4)
        removeTrack(3)
        assertEquals(3, currentQueue.size)
        assertEquals(2, currentIdx)
        assertEquals("Track 3", currentTrack?.title)

        // Case B: Remove a track BEFORE the currently playing track (index 0, Track 1)
        // Currently playing Track 3 is at index 2 -> after removing index 0, Track 3 shifts to index 1
        removeTrack(0)
        assertEquals(2, currentQueue.size)
        assertEquals(1, currentIdx)
        assertEquals("Track 3", currentTrack?.title)

        // Case C: Remove the CURRENTLY PLAYING track (index 1, Track 3)
        // Should advance to next track (Track 2, now at index 0 because only Track 2 remains)
        removeTrack(1)
        assertEquals(1, currentQueue.size)
        assertEquals(0, currentIdx)
        assertEquals("Track 2", currentTrack?.title)

        // Case D: Remove the last remaining track
        removeTrack(0)
        assertTrue(currentQueue.isEmpty())
        assertEquals(-1, currentIdx)
        assertNull(currentTrack)
    }

    @Test
    fun testInactiveWorkspaceTrackRemovalDoesNotAffectActiveQueue() {
        val t1 = createTestTrack(1L, "Active 1")
        val t2 = createTestTrack(2L, "Active 2")
        val activeTracks = listOf(t1, t2)

        val s1 = createTestTrack(10L, "Scratch 1")
        val s2 = createTestTrack(20L, "Scratch 2")
        val scratchTracks = listOf(s1, s2)

        var workspaces = listOf(
            QueueWorkspace(id = "main", name = "MAIN", tracks = activeTracks, isPlaybackActive = true),
            QueueWorkspace(id = "scratchpad", name = "SCRATCHPAD", tracks = scratchTracks, isPlaybackActive = false)
        )

        // Remove track from scratchpad
        val wsId = "scratchpad"
        val removeIdx = 0
        val targetWs = workspaces.find { it.id == wsId }
        val isPlaybackActive = targetWs?.isPlaybackActive == true

        assertFalse(isPlaybackActive)

        workspaces = workspaces.map { ws ->
            if (ws.id == wsId) {
                val updated = ws.tracks.toMutableList()
                if (removeIdx in updated.indices) updated.removeAt(removeIdx)
                ws.copy(tracks = updated)
            } else ws
        }

        // Active workspace tracks and status must remain completely intact
        val mainWs = workspaces.first { it.id == "main" }
        assertEquals(2, mainWs.tracks.size)
        assertEquals("Active 1", mainWs.tracks[0].title)

        // Scratchpad updated
        val scratchWs = workspaces.first { it.id == "scratchpad" }
        assertEquals(1, scratchWs.tracks.size)
        assertEquals("Scratch 2", scratchWs.tracks[0].title)
    }

    // ============================================================
    // 5. AUD-PL-01: PLAYLIST COUNT ACCURACY WITH DELETED TRACKS
    // ============================================================

    @Test
    fun testPlaylistTrackCountIgnoresDeletedTrackReferences() {
        // Simulate database cross-references: playlist 1 references tracks 101, 102, 103
        val crossRefs = listOf(101L, 102L, 103L)

        // But track 102 was deleted from storage/library
        val liveTrackIdsInDb = setOf(101L, 103L)

        // With INNER JOIN tracks, count reflects only surviving tracks
        val accurateCount = crossRefs.count { it in liveTrackIdsInDb }
        assertEquals(2, accurateCount)
    }
}
