package com.codewave.player

import com.codewave.player.core.download.FlacTagger
import com.codewave.player.core.model.DownloadStatus
import com.codewave.player.core.model.StreamQuality
import com.codewave.player.core.model.TargetAudioFormat
import com.codewave.player.core.network.resolver.MetadataResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StreamAndDownloadTests {

    private val resolver = MetadataResolver()

    @Test
    fun testTargetAudioFormatMimeAndExtensions() {
        assertEquals("flac", TargetAudioFormat.FLAC.extension)
        assertEquals("audio/flac", TargetAudioFormat.FLAC.mimeType)
        assertTrue(TargetAudioFormat.FLAC.isLossless)

        assertEquals("opus", TargetAudioFormat.OPUS.extension)
        assertEquals("audio/ogg", TargetAudioFormat.OPUS.mimeType)
        assertFalse(TargetAudioFormat.OPUS.isLossless)

        assertEquals("m4a", TargetAudioFormat.M4A.extension)
        assertEquals("audio/mp4", TargetAudioFormat.M4A.mimeType)
        assertFalse(TargetAudioFormat.M4A.isLossless)
    }

    @Test
    fun testDownloadStatusEnumValues() {
        val statuses = DownloadStatus.entries.map { it.name }
        assertTrue(statuses.contains("PENDING"))
        assertTrue(statuses.contains("CONNECTING"))
        assertTrue(statuses.contains("DOWNLOADING"))
        assertTrue(statuses.contains("TAGGING"))
        assertTrue(statuses.contains("COMPLETED"))
        assertTrue(statuses.contains("FAILED"))
        assertTrue(statuses.contains("CANCELLED"))
    }

    @Test
    fun testStreamQualityBitrates() {
        assertEquals(160, StreamQuality.HIGH.approxKbps)
        assertEquals(128, StreamQuality.NORMAL.approxKbps)
        assertEquals(64, StreamQuality.LOW.approxKbps)
        assertEquals(1411, StreamQuality.HI_RES.approxKbps)
    }

    @Test
    fun testMetadataResolverServiceDetection() {
        // Spotify detection
        assertTrue(resolver.isSpotifyUrl("https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT"))
        assertFalse(resolver.isSpotifyUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))

        // YouTube / YouTube Music detection
        assertTrue(resolver.isYouTubeUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(resolver.isYouTubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(resolver.isYouTubeUrl("https://youtu.be/dQw4w9WgXcQ"))
        assertFalse(resolver.isYouTubeUrl("https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT"))

        // Extract YouTube ID
        assertEquals("dQw4w9WgXcQ", resolver.extractYouTubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", resolver.extractYouTubeId("https://youtu.be/dQw4w9WgXcQ"))
        assertNull(resolver.extractYouTubeId("https://example.com/not-youtube"))
    }

    @Test
    fun testCleanSearchQueryEdgeCases() {
        // Noise removal
        val cleaned1 = resolver.cleanSearchQuery("Starboy (Official Music Video)", "The Weeknd")
        assertEquals("Starboy The Weeknd", cleaned1)

        val cleaned2 = resolver.cleanSearchQuery("Blinding Lights [Official Audio]", "The Weeknd")
        assertEquals("Blinding Lights The Weeknd", cleaned2)

        val cleaned3 = resolver.cleanSearchQuery("Hotel California (Remaster 2013)", "Eagles")
        assertEquals("Hotel California Eagles", cleaned3)

        val cleaned4 = resolver.cleanSearchQuery("Stay (Lyric Video)", "The Kid LAROI")
        assertEquals("Stay The Kid LAROI", cleaned4)
    }

    @Test
    fun testFlacTaggerRejectsNonFlacFile() {
        val tempDummyFile = File.createTempFile("test_non_flac", ".mp3")
        try {
            tempDummyFile.writeBytes(byteArrayOf(0x49, 0x44, 0x33, 0x03)) // ID3 header
            val result = FlacTagger.tagFlacFile(
                file = tempDummyFile,
                title = "Test Title",
                artist = "Test Artist",
                album = "Test Album"
            )
            assertFalse("FlacTagger must reject files that do not have 'fLaC' header", result)
        } finally {
            tempDummyFile.delete()
        }
    }

    @Test
    fun testFlacTaggerRejectsNonExistentFile() {
        val missingFile = File("non_existent_file_path_xyz.flac")
        val result = FlacTagger.tagFlacFile(
            file = missingFile,
            title = "Test",
            artist = "Test",
            album = "Test"
        )
        assertFalse(result)
    }
}
