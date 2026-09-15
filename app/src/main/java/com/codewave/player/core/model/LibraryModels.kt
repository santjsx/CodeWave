package com.codewave.player.core.model

data class Track(
    val id: Long,
    val mediaStoreId: Long,
    val uri: String,
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val durationMs: Long,
    val fileSize: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val mimeType: String,
    val format: AudioFormat,
    val codec: String,
    val sampleRate: Int,
    val bitDepth: Int? = null,
    val channels: Int = 2,
    val bitrateKbps: Int = 0,
    val isLossless: Boolean = false,
    val isHiRes: Boolean = false,
    val albumArtUri: String? = null,
    val fingerprint: String = "",
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long? = null
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    val technicalSummary: String
        get() {
            val rate = if (sampleRate > 0) "${sampleRate / 1000}kHz" else ""
            val depth = bitDepth?.let { "${it}bit" } ?: ""
            return listOf(format.displayName, depth, rate)
                .filter { it.isNotEmpty() }
                .joinToString(" · ")
        }
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val year: Int? = null,
    val artworkUri: String? = null,
    val isHiRes: Boolean = false,
    val isLossless: Boolean = false
)

data class Artist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val artworkUri: String? = null
)

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val createdAt: Long,
    val modifiedAt: Long,
    val isSmart: Boolean = false,
    val smartType: String? = null
)

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val shuffleMode: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = -1,
    val outputInfo: AudioOutputInfo = AudioOutputInfo.UNAVAILABLE,
    val dspStatus: DSPStatus = DSPStatus.BYPASSED
)
