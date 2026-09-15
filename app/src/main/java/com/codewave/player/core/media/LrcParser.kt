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
     * 1. Adjacent .lrc files (case-insensitive in same folder or /lyrics subfolder)
     * 2. Embedded ID3v2 (USLT/SYLT/COMM), FLAC Vorbis comments (LYRICS/UNSYNCEDLYRICS), and MP4 (©lyr) tags
     * 3. Adjacent .txt files (plain text lyrics)
     */
    fun loadLyricsForTrack(filePath: String): LyricsResult {
        if (filePath.isBlank()) return LyricsResult.Unavailable
        try {
            val audioFile = File(filePath)
            if (!audioFile.exists() || !audioFile.canRead()) {
                return LyricsResult.Unavailable
            }

            // 1. Check external .lrc in same directory (case-insensitive)
            val externalLrc = findAdjacentFile(audioFile.parentFile, audioFile.nameWithoutExtension, listOf("lrc"))
            if (externalLrc != null) {
                val text = externalLrc.readText()
                val parsed = parse(text)
                if (parsed.isNotEmpty()) return LyricsResult.Synchronized(parsed)
                val plainLines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (plainLines.isNotEmpty()) return LyricsResult.Plain(plainLines)
            }

            // 2. Check lyrics subfolder .lrc
            val lyricsDir = File(audioFile.parentFile, "lyrics")
            if (lyricsDir.exists() && lyricsDir.isDirectory) {
                val subLrc = findAdjacentFile(lyricsDir, audioFile.nameWithoutExtension, listOf("lrc"))
                if (subLrc != null) {
                    val text = subLrc.readText()
                    val parsed = parse(text)
                    if (parsed.isNotEmpty()) return LyricsResult.Synchronized(parsed)
                    val plainLines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    if (plainLines.isNotEmpty()) return LyricsResult.Plain(plainLines)
                }
            }

            // 3. Check embedded tags inside the audio file (ID3v2 USLT/SYLT, FLAC Vorbis comments, MP4 ©lyr)
            val embeddedText = extractEmbeddedLyrics(audioFile)
            if (!embeddedText.isNullOrBlank()) {
                val parsed = parse(embeddedText)
                if (parsed.isNotEmpty()) return LyricsResult.Synchronized(parsed)
                val plainLines = embeddedText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (plainLines.isNotEmpty()) return LyricsResult.Plain(plainLines)
            }

            // 4. Check adjacent .txt file
            val externalTxt = findAdjacentFile(audioFile.parentFile, audioFile.nameWithoutExtension, listOf("txt"))
            if (externalTxt != null) {
                val text = externalTxt.readText()
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

    private fun findAdjacentFile(parentDir: File?, baseName: String, extensions: List<String>): File? {
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory) return null
        val files = parentDir.listFiles() ?: return null
        for (ext in extensions) {
            val candidate = files.firstOrNull { file ->
                file.isFile &&
                file.nameWithoutExtension.equals(baseName, ignoreCase = true) &&
                file.extension.equals(ext, ignoreCase = true)
            }
            if (candidate != null && candidate.canRead()) return candidate
        }
        return null
    }

    /**
     * Extracts embedded lyrics from MP3 (ID3v2), FLAC (Vorbis comments), and MP4/M4A (©lyr atom).
     */
    fun extractEmbeddedLyrics(file: File): String? {
        if (!file.exists() || file.length() < 16) return null

        val ext = file.extension.lowercase()
        return try {
            when (ext) {
                "mp3" -> extractId3Lyrics(file) ?: extractVorbisLyrics(file)
                "flac", "ogg" -> extractVorbisLyrics(file) ?: extractId3Lyrics(file)
                "m4a", "mp4", "aac" -> extractMp4Lyrics(file) ?: extractId3Lyrics(file)
                else -> extractId3Lyrics(file) ?: extractVorbisLyrics(file) ?: extractMp4Lyrics(file)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts lyrics from ID3v2 USLT / SYLT / COMM frames.
     */
    private fun extractId3Lyrics(file: File): String? {
        file.inputStream().use { input ->
            val header = ByteArray(10)
            if (input.read(header) != 10) return null

            // Check "ID3" identifier
            if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
                return null
            }

            val majorVersion = header[3].toInt() and 0xFF
            val flags = header[5].toInt() and 0xFF
            val isExtendedHeader = (flags and 0x40) != 0

            // Syncsafe size calculation: 4 bytes * 7 bits
            val tagSize = ((header[6].toInt() and 0x7F) shl 21) or
                          ((header[7].toInt() and 0x7F) shl 14) or
                          ((header[8].toInt() and 0x7F) shl 7) or
                          (header[9].toInt() and 0x7F)

            val tagBytes = ByteArray(tagSize)
            var bytesRead = 0
            while (bytesRead < tagSize) {
                val read = input.read(tagBytes, bytesRead, tagSize - bytesRead)
                if (read <= 0) break
                bytesRead += read
            }

            var offset = 0
            if (isExtendedHeader && bytesRead >= 4) {
                val extSize = if (majorVersion == 4) {
                    ((tagBytes[0].toInt() and 0x7F) shl 21) or
                    ((tagBytes[1].toInt() and 0x7F) shl 14) or
                    ((tagBytes[2].toInt() and 0x7F) shl 7) or
                    (tagBytes[3].toInt() and 0x7F)
                } else {
                    ((tagBytes[0].toInt() and 0xFF) shl 24) or
                    ((tagBytes[1].toInt() and 0xFF) shl 16) or
                    ((tagBytes[2].toInt() and 0xFF) shl 8) or
                    (tagBytes[3].toInt() and 0xFF)
                }
                offset = extSize.coerceAtMost(bytesRead)
            }

            while (offset + 10 <= bytesRead) {
                val frameId = String(tagBytes, offset, 4, Charsets.US_ASCII)
                if (frameId.all { it == '\u0000' } || !frameId.all { it in 'A'..'Z' || it in '0'..'9' }) {
                    break
                }

                val frameSize = if (majorVersion == 4) {
                    ((tagBytes[offset + 4].toInt() and 0x7F) shl 21) or
                    ((tagBytes[offset + 5].toInt() and 0x7F) shl 14) or
                    ((tagBytes[offset + 6].toInt() and 0x7F) shl 7) or
                    (tagBytes[offset + 7].toInt() and 0x7F)
                } else {
                    ((tagBytes[offset + 4].toInt() and 0xFF) shl 24) or
                    ((tagBytes[offset + 5].toInt() and 0xFF) shl 16) or
                    ((tagBytes[offset + 6].toInt() and 0xFF) shl 8) or
                    (tagBytes[offset + 7].toInt() and 0xFF)
                }

                offset += 10
                if (frameSize <= 0 || offset + frameSize > bytesRead) break

                if (frameId == "USLT" || frameId == "SYLT") {
                    val encoding = tagBytes[offset].toInt() and 0xFF
                    val charset = when (encoding) {
                        1 -> Charsets.UTF_16
                        2 -> Charsets.UTF_16BE
                        3 -> Charsets.UTF_8
                        else -> Charsets.ISO_8859_1
                    }

                    // USLT format: [1 byte encoding][3 bytes lang][descriptor (null-terminated)][lyrics]
                    if (frameId == "USLT" && frameSize > 4) {
                        var bodyOffset = offset + 4 // Skip encoding and 3 bytes language
                        val bodyEnd = offset + frameSize

                        // Find null terminator for descriptor
                        if (encoding == 1 || encoding == 2) {
                            while (bodyOffset + 1 < bodyEnd) {
                                if (tagBytes[bodyOffset] == 0.toByte() && tagBytes[bodyOffset + 1] == 0.toByte()) {
                                    bodyOffset += 2
                                    break
                                }
                                bodyOffset += 2
                            }
                        } else {
                            while (bodyOffset < bodyEnd) {
                                if (tagBytes[bodyOffset] == 0.toByte()) {
                                    bodyOffset += 1
                                    break
                                }
                                bodyOffset += 1
                            }
                        }

                        if (bodyOffset < bodyEnd) {
                            val lyrics = String(tagBytes, bodyOffset, bodyEnd - bodyOffset, charset).trim()
                            if (lyrics.isNotEmpty()) return lyrics
                        }
                    }
                }

                offset += frameSize
            }
        }
        return null
    }

    /**
     * Extracts lyrics from FLAC Vorbis Comment metadata blocks.
     */
    private fun extractVorbisLyrics(file: File): String? {
        file.inputStream().use { input ->
            val magic = ByteArray(4)
            if (input.read(magic) != 4) return null
            if (String(magic, Charsets.US_ASCII) != "fLaC") return null

            var isLast = false
            while (!isLast) {
                val header = ByteArray(4)
                if (input.read(header) != 4) break
                val blockType = header[0].toInt() and 0x7F
                isLast = (header[0].toInt() and 0x80) != 0
                val blockLength = ((header[1].toInt() and 0xFF) shl 16) or
                                  ((header[2].toInt() and 0xFF) shl 8) or
                                  (header[3].toInt() and 0xFF)

                if (blockType == 4) { // VORBIS_COMMENT
                    val blockData = ByteArray(blockLength)
                    var read = 0
                    while (read < blockLength) {
                        val n = input.read(blockData, read, blockLength - read)
                        if (n <= 0) break
                        read += n
                    }

                    if (read >= 8) {
                        var offset = 0
                        val vendorLen = readLittleEndianInt(blockData, offset)
                        offset += 4 + vendorLen
                        if (offset + 4 <= read) {
                            val commentCount = readLittleEndianInt(blockData, offset)
                            offset += 4

                            for (i in 0 until commentCount) {
                                if (offset + 4 > read) break
                                val commentLen = readLittleEndianInt(blockData, offset)
                                offset += 4
                                if (commentLen < 0 || offset + commentLen > read) break

                                val comment = String(blockData, offset, commentLen, Charsets.UTF_8)
                                offset += commentLen

                                val eqIdx = comment.indexOf('=')
                                if (eqIdx > 0) {
                                    val key = comment.substring(0, eqIdx).uppercase()
                                    if (key in listOf("LYRICS", "UNSYNCEDLYRICS", "SYNCEDLYRICS", "LYRICS_SYNCED")) {
                                        val value = comment.substring(eqIdx + 1).trim()
                                        if (value.isNotEmpty()) return value
                                    }
                                }
                            }
                        }
                    }
                    break
                } else {
                    input.skip(blockLength.toLong())
                }
            }
        }
        return null
    }

    /**
     * Extracts lyrics from MP4/M4A ilst atom (©lyr box).
     */
    private fun extractMp4Lyrics(file: File): String? {
        val maxScanBytes = (512 * 1024).coerceAtMost(file.length().toInt())
        val buffer = ByteArray(maxScanBytes)
        file.inputStream().use { input ->
            var read = 0
            while (read < maxScanBytes) {
                val n = input.read(buffer, read, maxScanBytes - read)
                if (n <= 0) break
                read += n
            }
        }

        // Search for '©lyr' atom: bytes 0xA9, 'l', 'y', 'r' or 'lyr\0'
        for (i in 0 until buffer.size - 16) {
            if ((buffer[i] == 0xA9.toByte() || buffer[i] == 0xC2.toByte()) &&
                buffer[i + 1] == 'l'.code.toByte() &&
                buffer[i + 2] == 'y'.code.toByte() &&
                buffer[i + 3] == 'r'.code.toByte()
            ) {
                // Find subsequent 'data' atom within 32 bytes
                for (j in i + 4 until (i + 36).coerceAtMost(buffer.size - 8)) {
                    if (buffer[j] == 'd'.code.toByte() &&
                        buffer[j + 1] == 'a'.code.toByte() &&
                        buffer[j + 2] == 't'.code.toByte() &&
                        buffer[j + 3] == 'a'.code.toByte()
                    ) {
                        // Data header: 4 bytes size (before 'data'), 4 bytes 'data', 4 bytes type, 4 bytes locale
                        val dataSize = ((buffer[j - 4].toInt() and 0xFF) shl 24) or
                                       ((buffer[j - 3].toInt() and 0xFF) shl 16) or
                                       ((buffer[j - 2].toInt() and 0xFF) shl 8) or
                                       (buffer[j - 1].toInt() and 0xFF)
                        val payloadLen = (dataSize - 16).coerceAtLeast(0)
                        val textStart = j + 12
                        if (payloadLen > 0 && textStart + payloadLen <= buffer.size) {
                            val lyrics = String(buffer, textStart, payloadLen, Charsets.UTF_8).trim()
                            if (lyrics.isNotEmpty()) return lyrics
                        }
                    }
                }
            }
        }
        return null
    }

    private fun readLittleEndianInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return (bytes[offset].toInt() and 0xFF) or
               ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
               ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
               ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }
}
