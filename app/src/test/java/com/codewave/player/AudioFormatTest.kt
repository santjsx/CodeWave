package com.codewave.player

import com.codewave.player.core.model.AudioFormat
import com.codewave.player.core.scanner.MetadataExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFormatTest {

    @Test
    fun testAudioFormatClassification() {
        // Lossless formats
        assertTrue(AudioFormat.FLAC.isLossless)
        assertTrue(AudioFormat.WAV.isLossless)
        assertTrue(AudioFormat.ALAC.isLossless)
        assertTrue(AudioFormat.AIFF.isLossless)
        assertTrue(AudioFormat.APE.isLossless)

        // Lossy formats
        assertFalse(AudioFormat.MP3.isLossless)
        assertFalse(AudioFormat.AAC.isLossless)
        assertFalse(AudioFormat.OGG.isLossless)
        assertFalse(AudioFormat.OPUS.isLossless)
    }

    @Test
    fun testFormatDetectionFromExtension() {
        assertEquals(AudioFormat.FLAC, AudioFormat.fromMimeOrExtension(null, "/storage/Music/song.flac"))
        assertEquals(AudioFormat.WAV, AudioFormat.fromMimeOrExtension(null, "/storage/Music/song.wav"))
        assertEquals(AudioFormat.MP3, AudioFormat.fromMimeOrExtension(null, "/storage/Music/song.mp3"))
        assertEquals(AudioFormat.AAC, AudioFormat.fromMimeOrExtension(null, "/storage/Music/song.m4a"))
        assertEquals(AudioFormat.OGG, AudioFormat.fromMimeOrExtension(null, "/storage/Music/song.ogg"))
        assertEquals(AudioFormat.OPUS, AudioFormat.fromMimeOrExtension(null, "/storage/Music/song.opus"))
    }

    @Test
    fun testFormatDetectionFromMimeType() {
        assertEquals(AudioFormat.FLAC, AudioFormat.fromMimeOrExtension("audio/flac", null))
        assertEquals(AudioFormat.WAV, AudioFormat.fromMimeOrExtension("audio/x-wav", null))
        assertEquals(AudioFormat.MP3, AudioFormat.fromMimeOrExtension("audio/mpeg", null))
        assertEquals(AudioFormat.AAC, AudioFormat.fromMimeOrExtension("audio/mp4a-latm", null))
        assertEquals(AudioFormat.OGG, AudioFormat.fromMimeOrExtension("audio/ogg", null))
        assertEquals(AudioFormat.OPUS, AudioFormat.fromMimeOrExtension("audio/opus", null))
    }
}
