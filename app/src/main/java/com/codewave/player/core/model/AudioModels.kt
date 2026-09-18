package com.codewave.player.core.model

enum class AudioFormat(val displayName: String, val isLossless: Boolean) {
    FLAC("FLAC", true),
    WAV("WAV", true),
    ALAC("ALAC", true),
    AIFF("AIFF", true),
    APE("APE", true),
    MP3("MP3", false),
    AAC("AAC", false),
    OGG("OGG", false),
    OPUS("Opus", false),
    UNKNOWN("Unknown", false);

    companion object {
        fun fromMimeOrExtension(mimeType: String?, path: String?): AudioFormat {
            val ext = path?.substringAfterLast('.', "")?.lowercase().orEmpty()
            val mime = mimeType?.lowercase().orEmpty()
            return when {
                mime.contains("flac") || ext == "flac" -> FLAC
                mime.contains("wav") || ext == "wav" -> WAV
                mime.contains("alac") || ext == "alac" || ext == "m4a" && mime.contains("alac") -> ALAC
                ext == "aiff" || ext == "aif" -> AIFF
                ext == "ape" -> APE
                mime.contains("mpeg") || mime.contains("mp3") || ext == "mp3" -> MP3
                mime.contains("aac") || mime.contains("mp4") || ext == "aac" || ext == "m4a" -> AAC
                mime.contains("ogg") || mime.contains("vorbis") || ext == "ogg" -> OGG
                mime.contains("opus") || ext == "opus" -> OPUS
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Factual audio source representation extracted from file headers/metadata.
 * Never conflated with physical hardware output (PRD Section 2, 33).
 */
data class AudioSourceInfo(
    val format: AudioFormat,
    val codec: String,
    val sampleRateHz: Int,
    val bitDepth: Int?,
    val channels: Int,
    val bitrateKbps: Int,
    val isLossless: Boolean,
    val isHiRes: Boolean
) {
    val sampleRateDisplay: String
        get() = if (sampleRateHz >= 1000) "${sampleRateHz / 1000.0} kHz" else "$sampleRateHz Hz"

    val bitDepthDisplay: String
        get() = bitDepth?.let { "$it-bit" } ?: "PCM"

    val channelsDisplay: String
        get() = when (channels) {
            1 -> "Mono"
            2 -> "Stereo"
            6 -> "5.1 Surround"
            8 -> "7.1 Surround"
            else -> "$channels Ch"
        }
}

/**
 * Physical audio output information verified from AudioTrack routing.
 * When hardware path cannot be verified with certainty, [isAvailable] is false.
 */
data class AudioOutputInfo(
    val isAvailable: Boolean,
    val sampleRateHz: Int = 48000,
    val bitDepth: Int = 16,
    val routeName: String = "Internal Speaker",
    val encodingName: String = "PCM 16-bit",
    val latencyMs: Int = 0
) {
    companion object {
        val UNAVAILABLE = AudioOutputInfo(
            isAvailable = false,
            routeName = "Output info unavailable"
        )
    }
}

enum class DSPStatus {
    ACTIVE,
    LIMITED,
    BYPASSED,
    UNAVAILABLE
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class ABLoopState(
    val pointA: Long? = null,
    val pointB: Long? = null,
    val isEnabled: Boolean = false
) {
    val durationMs: Long?
        get() = if (pointA != null && pointB != null && pointB > pointA) pointB - pointA else null

    val isConfigured: Boolean
        get() = pointA != null && pointB != null && pointB > pointA

    val isActive: Boolean
        get() = isEnabled && isConfigured
}
