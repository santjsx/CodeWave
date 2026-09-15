package com.codewave.player.core.media

import java.io.File
import java.util.regex.Pattern

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

sealed interface LyricsResult {
    data object Loading : LyricsResult
    data class Synchronized(val lines: List<LyricLine>) : LyricsResult
    data class Plain(val lines: List<String>) : LyricsResult
    data object Unavailable : LyricsResult
    data class Error(val message: String) : LyricsResult
}

object LrcParser {
    // Regex matches [mm:ss.xx] or [mm:ss.xxx] or [mm:ss]
    private val TIME_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?]")

    fun parse(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        if (lrcContent.isBlank()) return emptyList()

        lrcContent.lineSequence().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach

            val matcher = TIME_PATTERN.matcher(trimmed)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (matcher.find()) {
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                val millisPart = matcher.group(3)
                val millis = when {
                    millisPart == null -> 0L
                    millisPart.length == 1 -> (millisPart.toLong() * 100)
                    millisPart.length == 2 -> (millisPart.toLong() * 10)
                    else -> millisPart.take(3).toLong()
                }

                val totalMs = (minutes * 60 + seconds) * 1000 + millis
                timestamps.add(totalMs)
                lastMatchEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val lyricText = trimmed.substring(lastMatchEnd).trim()
                if (lyricText.isNotEmpty()) {
                    for (ts in timestamps) {
                        lines.add(LyricLine(timestampMs = ts, text = lyricText))
                    }
                }
            }
        }

        return lines.sortedBy { it.timestampMs }
    }

    /**
     * Resolves lyrics for a track file by inspecting:
     * 1. Adjacent .lrc files (same folder or /lyrics subfolder)
     * 2. Embedded ID3/MP4/FLAC metadata tags (via MediaMetadataRetriever)
     * 3. Adjacent .txt files (plain text lyrics)
     */
    fun loadLyricsForTrack(filePath: String): LyricsResult {
        if (filePath.isBlank()) return LyricsResult.Unavailable
        try {
            val audioFile = File(filePath)
            if (!audioFile.exists() || !audioFile.canRead()) {
                return LyricsResult.Unavailable
            }

            // 1. Check external .lrc in same directory
            val sameDirLrc = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
            if (sameDirLrc.exists() && sameDirLrc.canRead()) {
                val text = sameDirLrc.readText()
                val parsed = parse(text)
                if (parsed.isNotEmpty()) return LyricsResult.Synchronized(parsed)
                val plainLines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (plainLines.isNotEmpty()) return LyricsResult.Plain(plainLines)
            }

            // 2. Check lyrics subfolder .lrc
            val lyricsDir = File(audioFile.parentFile, "lyrics")
            if (lyricsDir.exists() && lyricsDir.isDirectory) {
                val subLrc = File(lyricsDir, "${audioFile.nameWithoutExtension}.lrc")
                if (subLrc.exists() && subLrc.canRead()) {
                    val text = subLrc.readText()
                    val parsed = parse(text)
                    if (parsed.isNotEmpty()) return LyricsResult.Synchronized(parsed)
                    val plainLines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    if (plainLines.isNotEmpty()) return LyricsResult.Plain(plainLines)
                }
            }


            // 4. Check adjacent .txt file
            val sameDirTxt = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.txt")
            if (sameDirTxt.exists() && sameDirTxt.canRead()) {
                val text = sameDirTxt.readText()
                val parsed = parse(text)
                if (parsed.isNotEmpty()) return LyricsResult.Synchronized(parsed)
                val plainLines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (plainLines.isNotEmpty()) return LyricsResult.Plain(plainLines)
            }
        } catch (e: Exception) {
            return LyricsResult.Error(e.message ?: "Failed to read lyrics")
        }

        return LyricsResult.Unavailable
    }
}
