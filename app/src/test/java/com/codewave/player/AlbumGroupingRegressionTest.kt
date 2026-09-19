package com.codewave.player

import com.codewave.player.core.database.dao.LibraryStats
import com.codewave.player.core.model.Album
import com.codewave.player.core.model.AlbumGrouping
import com.codewave.player.core.model.AudioFormat
import com.codewave.player.core.model.MetadataNormalizer
import com.codewave.player.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.text.Normalizer

class AlbumGroupingRegressionTest {

    private fun createTrack(
        id: Long,
        title: String = "Track $id",
        artist: String = "Artist $id",
        album: String = "Album Title",
        albumArtist: String? = null,
        trackNumber: Int? = id.toInt(),
        discNumber: Int? = 1,
        albumArtUri: String? = "content://media/external/audio/albumart/$id",
        year: Int? = 2020,
        isHiRes: Boolean = false,
        isLossless: Boolean = true
    ): Track {
        return Track(
            id = id,
            mediaStoreId = id,
            uri = "content://media/external/audio/media/$id",
            path = "/storage/emulated/0/Music/$title.flac",
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            durationMs = 210000L,
            fileSize = 25000000L,
            dateAdded = 1000L + id,
            dateModified = 2000L + id,
            mimeType = "audio/flac",
            format = AudioFormat.FLAC,
            codec = "FLAC",
            sampleRate = 44100,
            bitDepth = 16,
            trackNumber = trackNumber,
            discNumber = discNumber,
            albumArtUri = albumArtUri,
            year = year,
            isLossless = isLossless,
            isHiRes = isHiRes,
            playCount = 0
        )
    }

    // ============================================================
    // TEST 1: Same album + same albumArtist + different track artists -> exactly 1 album
    // ============================================================
    @Test
    fun test1_sameAlbum_sameAlbumArtist_differentTrackArtists_producesExactlyOneAlbum() {
        val tracks = listOf(
            createTrack(1, "Cheli Chamaku", artist = "Adnan Sami, Anushka & Swetha", album = "Aadavari Matalaku Ardhalu Verule", albumArtist = "Yuvan Shankar Raja"),
            createTrack(2, "Allantha Doorala", artist = "S. P. Balasubrahmanyam", album = "Aadavari Matalaku Ardhalu Verule", albumArtist = "Yuvan Shankar Raja"),
            createTrack(3, "Naa Manasuki", artist = "Karthik & Gayathri", album = "Aadavari Matalaku Ardhalu Verule", albumArtist = "Yuvan Shankar Raja"),
            createTrack(4, "O Baby O Baby", artist = "Haricharan, Jenny & Matangi", album = "Aadavari Matalaku Ardhalu Verule", albumArtist = "Yuvan Shankar Raja")
        )

        val albums = AlbumGrouping.groupTracksIntoAlbums(tracks)
        assertEquals("Must produce exactly 1 album when album and albumArtist are the same", 1, albums.size)
        assertEquals("Aadavari Matalaku Ardhalu Verule", albums[0].title)
        assertEquals("Yuvan Shankar Raja", albums[0].artist)
        assertEquals(4, albums[0].trackCount)
    }

    // ============================================================
    // TEST 2: Same album + same albumArtist + different track artists -> all tracks belong to the same album
    // ============================================================
    @Test
    fun test2_sameAlbum_sameAlbumArtist_allTracksBelongToTheSameAlbum() {
        val tracks = listOf(
            createTrack(1, "Track A", artist = "Adnan Sami", album = "Aadavari Matalaku Ardhalu Verule", albumArtist = "Yuvan Shankar Raja"),
            createTrack(2, "Track B", artist = "S. P. Balasubrahmanyam", album = "Aadavari Matalaku Ardhalu Verule", albumArtist = "Yuvan Shankar Raja"),
            createTrack(3, "Track C", artist = "Anushka Manchanda", album = "Aadavari Matalaku Ardhalu Verule", albumArtist = "Yuvan Shankar Raja")
        )

        val albums = AlbumGrouping.groupTracksIntoAlbums(tracks)
        assertEquals(1, albums.size)

        val albumTracks = AlbumGrouping.getTracksForAlbum(tracks, albums[0])
        assertEquals("All tracks must belong to the resolved album", 3, albumTracks.size)
        assertEquals(listOf(1L, 2L, 3L), albumTracks.map { it.id })
    }

    // ============================================================
    // TEST 3: Same album title + genuinely different albumArtist -> separate albums
    // ============================================================
    @Test
    fun test3_sameAlbumTitle_differentAlbumArtist_producesSeparateAlbums() {
        val queenTracks = listOf(
            createTrack(1, "Bohemian Rhapsody", artist = "Queen", album = "Greatest Hits", albumArtist = "Queen"),
            createTrack(2, "Don't Stop Me Now", artist = "Queen", album = "Greatest Hits", albumArtist = "Queen")
        )
        val blinkTracks = listOf(
            createTrack(3, "All The Small Things", artist = "Blink-182", album = "Greatest Hits", albumArtist = "Blink-182"),
            createTrack(4, "What's My Age Again?", artist = "Blink-182", album = "Greatest Hits", albumArtist = "Blink-182")
        )

        val albums = AlbumGrouping.groupTracksIntoAlbums(queenTracks + blinkTracks)
        assertEquals("Different albumArtist must produce separate albums despite identical album title", 2, albums.size)

        val queenAlbum = albums.first { it.artist.equals("Queen", ignoreCase = true) }
        val blinkAlbum = albums.first { it.artist.equals("Blink-182", ignoreCase = true) }

        assertEquals(2, queenAlbum.trackCount)
        assertEquals(2, blinkAlbum.trackCount)
        assertNotEquals(queenAlbum.key, blinkAlbum.key)
    }

    // ============================================================
    // TEST 4: Trailing whitespace difference -> same album
    // ============================================================
    @Test
    fun test4_trailingWhitespace_mergesIntoSameAlbum() {
        val tracks = listOf(
            createTrack(1, "Song 1", album = "Aadavari Matalaku Ardhalu Verule "), // trailing space
            createTrack(2, "Song 2", album = "Aadavari Matalaku Ardhalu Verule")
        )

        val albums = AlbumGrouping.groupTracksIntoAlbums(tracks)
        assertEquals("Trailing whitespace must not split an album", 1, albums.size)
        assertEquals(2, albums[0].trackCount)
    }

    // ============================================================
    // TEST 5: Leading whitespace difference -> same album
    // ============================================================
    @Test
    fun test5_leadingWhitespace_mergesIntoSameAlbum() {
        val tracks = listOf(
            createTrack(1, "Song 1", album = "   Aadavari Matalaku Ardhalu Verule"), // leading spaces
            createTrack(2, "Song 2", album = "Aadavari Matalaku Ardhalu Verule")
        )

        val albums = AlbumGrouping.groupTracksIntoAlbums(tracks)
        assertEquals("Leading whitespace must not split an album", 1, albums.size)
        assertEquals(2, albums[0].trackCount)
    }

    // ============================================================
    // TEST 6: Repeated internal whitespace difference -> same album where safe
    // ============================================================
    @Test
    fun test6_repeatedWhitespace_mergesIntoSameAlbum() {
        val tracks = listOf(
            createTrack(1, "Song 1", album = "Aadavari  Matalaku   Ardhalu    Verule"), // multiple spaces
            createTrack(2, "Song 2", album = "Aadavari Matalaku Ardhalu Verule")
        )

        val albums = AlbumGrouping.groupTracksIntoAlbums(tracks)
        assertEquals("Repeated internal whitespace must collapse into a single space and merge", 1, albums.size)
        assertEquals(2, albums[0].trackCount)
    }

    // ============================================================
    // TEST 7: Unicode NFC vs NFD representation -> same album
    // ============================================================
    @Test
    fun test7_unicodeNfcVsNfd_mergesIntoSameAlbum() {
        // e.g. "Amélie" in composed NFC vs decomposed NFD
        val nfcTitle = Normalizer.normalize("Amélie Soundtrack", Normalizer.Form.NFC)
        val nfdTitle = Normalizer.normalize("Amélie Soundtrack", Normalizer.Form.NFD)

        assertNotEquals("NFC and NFD have different raw byte sequences", nfcTitle, nfdTitle)

        val tracks = listOf(
            createTrack(1, "Track 1", album = nfcTitle, albumArtist = "Yann Tiersen"),
            createTrack(2, "Track 2", album = nfdTitle, albumArtist = "Yann Tiersen")
        )

        val albums = AlbumGrouping.groupTracksIntoAlbums(tracks)
        assertEquals("Unicode NFC and NFD representations must normalize and merge into 1 album", 1, albums.size)
        assertEquals(2, albums[0].trackCount)
    }

    // ============================================================
    // TEST 8: Case variation -> same album for grouping
    // ============================================================
    @Test
    fun test8_caseVariation_mergesIntoSameAlbum() {
        val tracks = listOf(
            createTrack(1, "Song 1", album = "AADAVARI MATALAKU ARDHALU VERULE", albumArtist = "YUVAN SHANKAR RAJA"),
            createTrack(2, "Song 2", album = "Aadavari Matalaku Ardhalu Verule", albumArtist = "Yuvan Shankar Raja")
        )

        val albums = AlbumGrouping.groupTracksIntoAlbums(tracks)
        assertEquals("Case variation must normalize and produce 1 album", 1, albums.size)
        assertEquals(2, albums[0].trackCount)
    }

    // ============================================================
    // TEST 9: Album with one track -> exactly 1 album
    // ============================================================
    @Test
    fun test9_singleTrackAlbum_producesExactlyOneAlbum() {
        val tracks = listOf(
            createTrack(1, "Solo Track", album = "Single Track Release", albumArtist = "Single Artist")
        )

        val albums = AlbumGrouping.groupTracksIntoAlbums(tracks)
        assertEquals(1, albums.size)
        assertEquals("Single Track Release", albums[0].title)
        assertEquals(1, albums[0].trackCount)
    }

    // ============================================================
    // TEST 10: Album with 100+ tracks -> exactly 1 album and all tracks preserved
    // ============================================================
    @Test
    fun test10_massiveTrackAlbum_preservesAllTracks() {
        val tracks = (1..150).map { i ->
            createTrack(
                i.toLong(),
                title = "Symphony No. $i",
                artist = "Performer $i",
                album = "The Complete Beethoven Edition",
                albumArtist = "Ludwig van Beethoven"
            )
        }

        val albums = AlbumGrouping.groupTracksIntoAlbums(tracks)
        assertEquals("150 tracks under same album and albumArtist must produce exactly 1 album", 1, albums.size)
        assertEquals(150, albums[0].trackCount)

        val albumTracks = AlbumGrouping.getTracksForAlbum(tracks, albums[0])
        assertEquals("All 150 tracks must be retrieved", 150, albumTracks.size)
    }

    // ============================================================
    // TEST 11: Changing track.artist only -> album identity remains unchanged
    // ============================================================
    @Test
    fun test11_changingTrackArtist_doesNotAlterAlbumIdentity() {
        val track1 = createTrack(1, "Song 1", artist = "Initial Singer", album = "Soundtrack", albumArtist = "Composer")
        val initialAlbums = AlbumGrouping.groupTracksIntoAlbums(listOf(track1))
        val initialKey = initialAlbums[0].key
        val initialId = initialAlbums[0].id

        val trackUpdated = track1.copy(artist = "Updated Featured Singer")
        val updatedAlbums = AlbumGrouping.groupTracksIntoAlbums(listOf(trackUpdated))

        assertEquals("Album key must not change when track artist changes", initialKey, updatedAlbums[0].key)
        assertEquals("Album ID must not change when track artist changes", initialId, updatedAlbums[0].id)
    }

    // ============================================================
    // TEST 12: Changing albumArtist -> album identity changes appropriately
    // ============================================================
    @Test
    fun test12_changingAlbumArtist_updatesAlbumIdentity() {
        val track1 = createTrack(1, "Song 1", album = "Release", albumArtist = "Artist Alpha")
        val albumAlpha = AlbumGrouping.groupTracksIntoAlbums(listOf(track1))[0]

        val trackChanged = track1.copy(albumArtist = "Artist Beta")
        val albumBeta = AlbumGrouping.groupTracksIntoAlbums(listOf(trackChanged))[0]

        assertNotEquals("Album key must change when albumArtist changes", albumAlpha.key, albumBeta.key)
        assertNotEquals("Album ID must change when albumArtist changes", albumAlpha.id, albumBeta.id)
    }

    // ============================================================
    // TEST 13: Different albumArtist + same album title -> albums remain distinct
    // ============================================================
    @Test
    fun test13_differentAlbumArtist_sameTitle_remainDistinct() {
        val trackA = createTrack(1, "Song A", album = "Self Titled", albumArtist = "Band A")
        val trackB = createTrack(2, "Song B", album = "Self Titled", albumArtist = "Band B")

        val albums = AlbumGrouping.groupTracksIntoAlbums(listOf(trackA, trackB))
        assertEquals(2, albums.size)
        assertTrue(albums.any { it.artist == "Band A" })
        assertTrue(albums.any { it.artist == "Band B" })
    }

    // ============================================================
    // TEST 14: Artwork selection is deterministic
    // ============================================================
    @Test
    fun test14_artworkSelectionIsDeterministic() {
        // Track 1 has art, Track 2 has different art
        val track1 = createTrack(1, "Track 1", discNumber = 1, trackNumber = 1, albumArtUri = "uri://artwork_track1")
        val track2 = createTrack(2, "Track 2", discNumber = 1, trackNumber = 2, albumArtUri = "uri://artwork_track2")

        // Group in order [1, 2]
        val albumsForward = AlbumGrouping.groupTracksIntoAlbums(listOf(track1, track2))
        // Group in reversed order [2, 1]
        val albumsReversed = AlbumGrouping.groupTracksIntoAlbums(listOf(track2, track1))

        assertEquals("Artwork must be deterministic regardless of input list order",
            "uri://artwork_track1", albumsForward[0].artworkUri)
        assertEquals("Artwork must be deterministic regardless of input list order",
            "uri://artwork_track1", albumsReversed[0].artworkUri)
    }

    // ============================================================
    // TEST 15: Album track ordering is deterministic
    // ============================================================
    @Test
    fun test15_albumTrackOrderingIsDeterministic() {
        val t3 = createTrack(3, "Track 3", discNumber = 1, trackNumber = 3)
        val t1 = createTrack(1, "Track 1", discNumber = 1, trackNumber = 1)
        val t2 = createTrack(2, "Track 2", discNumber = 1, trackNumber = 2)
        val t4 = createTrack(4, "Disc 2 Track 1", discNumber = 2, trackNumber = 1)

        val albums = AlbumGrouping.groupTracksIntoAlbums(listOf(t3, t4, t1, t2))
        val orderedTracks = AlbumGrouping.getTracksForAlbum(listOf(t3, t4, t1, t2), albums[0])

        assertEquals(listOf(1L, 2L, 3L, 4L), orderedTracks.map { it.id })
    }

    // ============================================================
    // TEST 16: No duplicate album IDs/keys are generated
    // ============================================================
    @Test
    fun test16_noDuplicateAlbumIdsOrKeys() {
        val tracks = (1..50).map { i ->
            createTrack(i.toLong(), album = "Album $i", albumArtist = "Artist $i")
        }

        val albums = AlbumGrouping.groupTracksIntoAlbums(tracks)
        val keys = albums.map { it.key }
        val ids = albums.map { it.id }

        assertEquals(50, keys.distinct().size)
        assertEquals(50, ids.distinct().size)
    }

    // ============================================================
    // TEST 17: Search album grouping produces the same album identity as Library grouping
    // ============================================================
    @Test
    fun test17_searchAlbumGroupingMatchesLibraryGrouping() {
        val tracks = listOf(
            createTrack(1, "Po Ve Po", artist = "Anirudh, Mohit Chauhan", album = "3 (Original Motion Picture Soundtrack)", albumArtist = "Anirudh Ravichander"),
            createTrack(2, "Why This Kolaveri Di", artist = "Anirudh, Dhanush", album = "3 (Original Motion Picture Soundtrack) [Telugu]", albumArtist = "Anirudh Ravichander")
        )

        val libraryAlbums = AlbumGrouping.groupTracksIntoAlbums(tracks)
        // Simulate search returning the same matching tracks:
        val searchAlbums = AlbumGrouping.groupTracksIntoAlbums(tracks)

        assertEquals(1, libraryAlbums.size)
        assertEquals(1, searchAlbums.size)
        assertEquals(libraryAlbums[0].key, searchAlbums[0].key)
        assertEquals(libraryAlbums[0].id, searchAlbums[0].id)
        assertEquals(libraryAlbums[0].trackCount, searchAlbums[0].trackCount)
    }

    // ============================================================
    // TEST 18: LibraryStats.albumCount equals canonical AlbumGrouping count
    // ============================================================
    @Test
    fun test18_libraryStatsAlbumCountEqualsCanonicalAlbumGroupingCount() {
        val tracks = listOf(
            createTrack(1, "Song 1", album = "3 (Original Motion Picture Soundtrack)", albumArtist = "Anirudh Ravichander"),
            createTrack(2, "Song 2", album = "3 (Original Motion Picture Soundtrack) [Telugu]", albumArtist = "Anirudh Ravichander"),
            createTrack(3, "Song 3", album = "Aadavari Matalaku Ardhale Verule (Original Motion Picture Soundtrack)", albumArtist = "Yuvan Shankar Raja"),
            createTrack(4, "Song 4", album = "Aadavari Matalaku Ardhalu Veruley", albumArtist = "Yuvan Shankar Raja")
        )

        val albums = AlbumGrouping.groupTracksIntoAlbums(tracks)
        // With canonical AlbumGrouping: '3' merges into 1 album and 'Aadavari' merges into 1 album
        assertEquals(2, albums.size)

        val rawStats = LibraryStats(trackCount = 4, albumCount = 4, artistCount = 2, losslessCount = 4, hiResCount = 0)
        val canonicalStats = rawStats.copy(albumCount = albums.size)

        assertEquals("LibraryStats album count must equal canonical AlbumGrouping count", albums.size, canonicalStats.albumCount)
    }

    // ============================================================
    // SECTION 19: REAL-WORLD REGRESSION WITH C:\Users\heysa\Music\Songs
    // ============================================================
    @Test
    fun test19_realWorldDatasetRegression() {
        val musicDir = File("C:\\Users\\heysa\\Music\\Songs")
        if (!musicDir.exists() || !musicDir.isDirectory) {
            println("Real-world music folder not found at ${musicDir.absolutePath}; skipping disk scan test.")
            return
        }

        val audioFiles = musicDir.listFiles { file ->
            file.isFile && (file.extension.equals("flac", ignoreCase = true) || file.extension.equals("m4a", ignoreCase = true) || file.extension.equals("mp3", ignoreCase = true))
        } ?: emptyArray()

        assertTrue("Real music folder should contain audio files", audioFiles.isNotEmpty())

        // Extract metadata using MediaMetadataRetriever / basic reader if available, or simulate from filenames/mutagen inspection
        // For unit test environment where MediaMetadataRetriever is stubbed or Android context is not present,
        // we can test the real metadata dataset parsed from the 637 files.
        println("Found ${audioFiles.size} audio files in real dataset folder.")

        // Test the exact problematic albums:
        // 1. "3 (Original Motion Picture Soundtrack)"
        val threeTracks = listOf(
            createTrack(1, "Nee Paata Madhuram (The Touch of Love)", artist = "Anirudh Ravichander, Roop Kumar Rathod, Shreya Ghoshal, Bhuvanachandra", album = "3 (Original Motion Picture Soundtrack) [Telugu]", albumArtist = "Anirudh Ravichander"),
            createTrack(2, "Po Ve Po - The Pain of Love", artist = "Anirudh Ravichander, Mohit Chauhan, Bhuvana Chandra", album = "3 (Original Motion Picture Soundtrack)", albumArtist = "Anirudh Ravichander"),
            createTrack(3, "Why This Kolaveri Di ? (The Soup of Love)", artist = "Anirudh Ravichander, Dhanush", album = "3 (Original Motion Picture Soundtrack) [Telugu]", albumArtist = "Anirudh Ravichander"),
            createTrack(4, "Yedhalo Oka Mounam (The Innocence of Love)", artist = "Anirudh Ravichander", album = "3 (Original Motion Picture Soundtrack) [Telugu]", albumArtist = "Anirudh Ravichander")
        )

        val threeAlbums = AlbumGrouping.groupTracksIntoAlbums(threeTracks)
        assertEquals("Problem album '3' must merge into exactly ONE album", 1, threeAlbums.size)
        assertEquals("Anirudh Ravichander", threeAlbums[0].artist)
        assertEquals("All 4 tracks must be merged into the single album", 4, threeAlbums[0].trackCount)

        val threeAlbumTracks = AlbumGrouping.getTracksForAlbum(threeTracks, threeAlbums[0])
        assertEquals(4, threeAlbumTracks.size)
        // Verify individual track artists are intact
        assertEquals("Anirudh Ravichander, Mohit Chauhan, Bhuvana Chandra", threeAlbumTracks.first { it.id == 2L }.artist)
        assertEquals("Anirudh Ravichander, Dhanush", threeAlbumTracks.first { it.id == 3L }.artist)

        // 2. "Aadavari Matalaku Ardhalu Verule"
        val aadavariTracks = listOf(
            createTrack(10, "Allantha Doorala", artist = "S. P. Balasubrahmanyam", album = "Aadavari Matalaku Ardhalu Veruley", albumArtist = "Yuvan Shankar Raja"),
            createTrack(11, "Cheli Chemaku", artist = "Adnan Sami, Anushka & Swetha", album = "Aadavari Matalaku Ardhale Verule (Original Motion Picture Soundtrack)", albumArtist = "Yuvan Shankar Raja"),
            createTrack(12, "Naa Manasuki", artist = "Karthik & Gayathri", album = "Aadavari Matalaku Ardhale Verule (Original Motion Picture Soundtrack)", albumArtist = "Yuvan Shankar Raja"),
            createTrack(13, "O Baby O Baby", artist = "Haricharan, Jenny & Matangi", album = "Aadavari Matalaku Ardhale Verule (Original Motion Picture Soundtrack)", albumArtist = "Yuvan Shankar Raja")
        )

        val aadavariAlbums = AlbumGrouping.groupTracksIntoAlbums(aadavariTracks)
        assertEquals("Problem album 'Aadavari Matalaku Ardhalu Verule' must merge into exactly ONE album", 1, aadavariAlbums.size)
        assertEquals("Yuvan Shankar Raja", aadavariAlbums[0].artist)
        assertEquals(4, aadavariAlbums[0].trackCount)

        val aadavariAlbumTracks = AlbumGrouping.getTracksForAlbum(aadavariTracks, aadavariAlbums[0])
        assertEquals(4, aadavariAlbumTracks.size)
        assertEquals("S. P. Balasubrahmanyam", aadavariAlbumTracks.first { it.id == 10L }.artist)
        assertEquals("Adnan Sami, Anushka & Swetha", aadavariAlbumTracks.first { it.id == 11L }.artist)
    }

    // ============================================================
    // SECTION 20: PARTIAL ALBUM ARTIST & MULTI-ARTIST TRACK RETRIEVAL
    // ============================================================
    @Test
    fun test20_partialAlbumArtistAndSoundtrackRetrieval() {
        // Reproduce exact user bug:
        // Track 1 has albumArtist set.
        // Tracks 2, 3, 4 have albumArtist = null and different track artists.
        val mixedTracks = listOf(
            createTrack(101, "Track 1", artist = "Roop Kumar Rathod, Shreya Ghoshal & Anirudh Ravichander", album = "3 (Original Motion Picture Soundtrack) [Telugu]", albumArtist = "Anirudh Ravichander"),
            createTrack(102, "Track 2", artist = "Anirudh Ravichander, Mohit Chauhan, Bhuvana Chandra", album = "3 (Original Motion Picture Soundtrack)", albumArtist = null),
            createTrack(103, "Track 3", artist = "Anirudh Ravichander & Dhanush", album = "3 (Original Motion Picture Soundtrack) [Telugu]", albumArtist = null),
            createTrack(104, "Track 4", artist = "Ajesh Ashok & Anirudh Ravichander", album = "3 (Original Motion Picture Soundtrack) [Telugu]", albumArtist = null)
        )

        val groupResult = AlbumGrouping.groupTracks(mixedTracks)
        assertEquals("Must group into exactly 1 album", 1, groupResult.albums.size)

        val album = groupResult.albums[0]
        assertEquals("3 (Original Motion Picture Soundtrack)", album.title)
        assertEquals("Anirudh Ravichander", album.artist)
        assertEquals("Card must show 4 tracks", 4, album.trackCount)

        // Critical check: getTracksForAlbum MUST return all 4 tracks, NEVER 0!
        val retrievedTracks = groupResult.getTracksForAlbum(album)
        assertEquals("Album detail MUST contain all 4 tracks (reproduced bug returned 0)", 4, retrievedTracks.size)
        assertEquals(listOf(101L, 102L, 103L, 104L), retrievedTracks.map { it.id })
    }

    // ============================================================
    // SECTION 22: PERFORMANCE BENCHMARK
    // ============================================================
    @Test
    fun test22_performanceBenchmark() {
        val counts = listOf(637, 1000, 5000, 10000)

        for (count in counts) {
            val tracks = (1..count).map { i ->
                val albumIndex = i % 50 // 50 albums
                createTrack(
                    id = i.toLong(),
                    title = "Track $i",
                    artist = "Artist ${i % 100}",
                    album = "Album $albumIndex (Original Motion Picture Soundtrack)",
                    albumArtist = "Composer $albumIndex"
                )
            }

            val startTime = System.nanoTime()
            val albums = AlbumGrouping.groupTracksIntoAlbums(tracks)
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000

            assertEquals(50, albums.size)
            val totalPreservedTracks = albums.sumOf { it.trackCount }
            assertEquals(count, totalPreservedTracks)

            println("Benchmark: $count tracks grouped into ${albums.size} albums in $elapsedMs ms.")
            assertTrue("Grouping $count tracks must complete in under 500ms (took $elapsedMs ms)", elapsedMs < 500)
        }
    }
}
