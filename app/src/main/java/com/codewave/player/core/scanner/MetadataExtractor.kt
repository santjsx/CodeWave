package com.codewave.player.core.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.codewave.player.core.model.AudioFormat
import java.io.File

data class ExtractedAudioMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String?,
    val genre: String?,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val durationMs: Long,
    val sampleRate: Int,
    val bitDepth: Int?,
    val channels: Int,
    val bitrateKbps: Int,
    val codec: String,
    val format: AudioFormat,
    val isLossless: Boolean,
    val isHiRes: Boolean
)

object MetadataExtractor {

    fun extract(
        context: Context,
        uri: Uri,
        path: String,
        mimeType: String?,
        fallbackTitle: String,
        fallbackArtist: String,
        fallbackAlbum: String,
        fallbackDuration: Long
    ): ExtractedAudioMetadata {
        val retriever = MediaMetadataRetriever()
        var sampleRate = 44100
        var bitDepth: Int? = null
        var channels = 2
        var bitrate = 0
        var codec = ""
        var title = fallbackTitle
        var artist = fallbackArtist
        var album = fallbackAlbum
        var albumArtist: String? = null
        var genre: String? = null
        var year: Int? = null
        var trackNumber: Int? = null
        var discNumber: Int? = null
        var duration = fallbackDuration

        try {
            retriever.setDataSource(context, uri)

            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { if (it.isNotBlank()) title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { if (it.isNotBlank()) artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let { if (it.isNotBlank()) album = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.let { albumArtist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.let { genre = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()?.let { year = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.let {
                trackNumber = it.substringBefore('/').toIntOrNull()
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.let {
                discNumber = it.substringBefore('/').toIntOrNull()
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                if (it > 0) duration = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.let {
                bitrate = it / 1000
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let {
                codec = it
            }

            // Extract sample rate if available (API 31+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()?.let {
                    if (it > 0) sampleRate = it
                }
            }
        } catch (_: Exception) {
            // Graceful fallback to MediaStore passed values
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        val format = AudioFormat.fromMimeOrExtension(mimeType, path)
        val isLossless = format.isLossless

        if (codec.isBlank()) {
            codec = format.displayName
        }

        // Detect Bit Depth heuristic for known lossless headers if unspecified
        if (isLossless) {
            if (sampleRate >= 88200) {
                bitDepth = 24
            } else if (bitDepth == null) {
                bitDepth = 16
            }
        }

        // Factual Hi-Res qualification policy (PRD Section 11, 35)
        val isHiRes = isLossless && (sampleRate >= 96000 || (bitDepth != null && bitDepth >= 24 && sampleRate >= 48000))

        return ExtractedAudioMetadata(
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            genre = genre,
            year = year,
            trackNumber = trackNumber,
            discNumber = discNumber,
            durationMs = duration,
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            channels = channels,
            bitrateKbps = bitrate,
            codec = codec,
            format = format,
            isLossless = isLossless,
            isHiRes = isHiRes
        )
    }
}
