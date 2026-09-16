package com.codewave.player.core.model

/**
 * Service origin of audio content.
 */
enum class AudioSourceType {
    LOCAL,
    YOUTUBE_MUSIC,
    SPOTIFY_RESOLVED,
    TIDAL_LOSSLESS,
    QOBUZ_LOSSLESS,
    DEEZER_LOSSLESS,
    COMMUNITY_FLAC
}

/**
 * Audio streaming bitrate & container quality options.
 */
enum class StreamQuality(val displayName: String, val approxKbps: Int) {
    LOW("Low (64k Opus)", 64),
    NORMAL("Normal (128k AAC)", 128),
    HIGH("High (160k Opus)", 160),
    HI_RES("Hi-Res (Lossless FLAC)", 1411)
}

/**
 * Online streaming track model originating from InnerTube / YouTube Music.
 */
data class StreamTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUri: String?,
    val isExplicit: Boolean = false,
    val sourceType: AudioSourceType = AudioSourceType.YOUTUBE_MUSIC,
    val approximateBitrateKbps: Int = 160,
    val audioFormat: AudioFormat = AudioFormat.OPUS,
    val streamUrl: String? = null,
    val streamUrlExpiryMs: Long = 0L,
    val isrc: String? = null
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    /**
     * Converts a streaming track to CodeWave's primary [Track] domain model
     * for seamless playback via [PlaybackRepository] and Media3 ExoPlayer.
     */
    fun toTrack(resolvedPlaybackUrl: String): Track {
        return Track(
            id = id.hashCode().toLong(),
            mediaStoreId = -1L,
            uri = resolvedPlaybackUrl,
            path = "stream://$id",
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            fileSize = 0L,
            dateAdded = System.currentTimeMillis() / 1000,
            dateModified = System.currentTimeMillis() / 1000,
            mimeType = if (audioFormat == AudioFormat.FLAC) "audio/flac" else "audio/webm",
            format = audioFormat,
            codec = if (audioFormat == AudioFormat.FLAC) "FLAC" else "Opus",
            sampleRate = 48000,
            bitDepth = if (audioFormat == AudioFormat.FLAC) 24 else 16,
            channels = 2,
            bitrateKbps = approximateBitrateKbps,
            isLossless = audioFormat.isLossless,
            isHiRes = audioFormat == AudioFormat.FLAC,
            albumArtUri = artworkUri,
            fingerprint = "stream_$id"
        )
    }
}

/**
 * State of a background lossless download task.
 */
enum class DownloadStatus {
    PENDING,
    CONNECTING,
    DOWNLOADING,
    TAGGING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Audio container target format for download.
 */
enum class TargetAudioFormat(val extension: String, val mimeType: String, val isLossless: Boolean) {
    FLAC("flac", "audio/flac", true),
    OPUS("opus", "audio/ogg", false),
    M4A("m4a", "audio/mp4", false)
}

/**
 * Represents a download task in the queue.
 */
data class DownloadTask(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUri: String?,
    val isrc: String? = null,
    val targetFormat: TargetAudioFormat = TargetAudioFormat.FLAC,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val errorMessage: String? = null,
    val localUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val speedFormatted: String
        get() {
            val mbPerSec = speedBytesPerSec.toDouble() / (1024.0 * 1024.0)
            return if (mbPerSec >= 0.1) {
                "%.1f MB/s".format(mbPerSec)
            } else {
                val kbPerSec = speedBytesPerSec.toDouble() / 1024.0
                "%.0f KB/s".format(kbPerSec)
            }
        }

    val progressPercent: Int
        get() = (progress * 100).toInt().coerceIn(0, 100)
}
