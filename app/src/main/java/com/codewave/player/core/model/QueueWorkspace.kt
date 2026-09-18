package com.codewave.player.core.model

data class QueueWorkspace(
    val id: String,
    val name: String,
    val tracks: List<Track> = emptyList(),
    val isPlaybackActive: Boolean = false
) {
    val trackCount: Int
        get() = tracks.size

    val durationFormatted: String
        get() {
            val totalSeconds = tracks.sumOf { it.durationMs } / 1000
            val mins = totalSeconds / 60
            val secs = totalSeconds % 60
            return "%d:%02d".format(mins, secs)
        }
}
