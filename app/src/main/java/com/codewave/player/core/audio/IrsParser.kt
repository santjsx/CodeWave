package com.codewave.player.core.audio

import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class IrsData(
    val samples: FloatArray,
    val sampleRate: Int,
    val channels: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IrsData
        return sampleRate == other.sampleRate && channels == other.channels && samples.contentEquals(other.samples)
    }

    override fun hashCode(): Int {
        var result = sampleRate
        result = 31 * result + channels
        result = 31 * result + samples.contentHashCode()
        return result
    }
}

/**
 * Parser for .irs and .wav impulse response files.
 * Handles standard RIFF/WAVE containers with 16-bit, 24-bit, and 32-bit float audio.
 */
object IrsParser {

    fun parse(file: File): IrsData? {
        if (!file.exists() || file.length() < 44) return null
        return try {
            file.inputStream().use { parse(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun parse(inputStream: InputStream): IrsData? {
        val bytes = inputStream.readBytes()
        if (bytes.size < 44) return null

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Check RIFF header
        val riff = ByteArray(4)
        buffer.get(riff)
        if (String(riff) != "RIFF") return null

        buffer.int // Skip fileSize

        val wave = ByteArray(4)
        buffer.get(wave)
        if (String(wave) != "WAVE") return null

        var audioFormat = 1 // 1 = PCM, 3 = IEEE Float
        var numChannels = 1
        var sampleRate = 44100
        var bitsPerSample = 16
        var dataOffset = -1
        var dataSize = 0

        // Parse chunks
        while (buffer.remaining() >= 8) {
            val chunkId = ByteArray(4)
            buffer.get(chunkId)
            val chunkSize = buffer.int
            val chunkName = String(chunkId)

            if (chunkName == "fmt ") {
                audioFormat = buffer.short.toInt()
                numChannels = buffer.short.toInt()
                sampleRate = buffer.int
                buffer.int // byteRate
                buffer.short // blockAlign
                bitsPerSample = buffer.short.toInt()
                // Skip any remaining fmt subchunk bytes
                val remainingFmt = chunkSize - 16
                if (remainingFmt > 0 && buffer.remaining() >= remainingFmt) {
                    buffer.position(buffer.position() + remainingFmt)
                }
            } else if (chunkName == "data") {
                dataOffset = buffer.position()
                dataSize = chunkSize.coerceAtMost(buffer.remaining())
                break
            } else {
                // Skip unknown chunk
                if (chunkSize > 0 && buffer.remaining() >= chunkSize) {
                    buffer.position(buffer.position() + chunkSize)
                } else {
                    break
                }
            }
        }

        if (dataOffset < 0 || dataSize <= 0) return null

        buffer.position(dataOffset)
        val frameSize = numChannels * (bitsPerSample / 8)
        if (frameSize <= 0) return null
        val totalFrames = dataSize / frameSize
        val floatSamples = FloatArray(totalFrames * numChannels)

        var floatIdx = 0
        when (bitsPerSample) {
            16 -> {
                for (i in 0 until totalFrames * numChannels) {
                    val sample = buffer.short.toFloat() / 32768.0f
                    floatSamples[floatIdx++] = sample.coerceIn(-1.0f, 1.0f)
                }
            }
            24 -> {
                for (i in 0 until totalFrames * numChannels) {
                    val b0 = buffer.get().toInt() and 0xFF
                    val b1 = buffer.get().toInt() and 0xFF
                    val b2 = buffer.get().toInt() // sign extended
                    val sampleInt = (b2 shl 16) or (b1 shl 8) or b0
                    val sample = sampleInt.toFloat() / 8388608.0f
                    floatSamples[floatIdx++] = sample.coerceIn(-1.0f, 1.0f)
                }
            }
            32 -> {
                if (audioFormat == 3) {
                    // 32-bit IEEE Float
                    for (i in 0 until totalFrames * numChannels) {
                        floatSamples[floatIdx++] = buffer.float.coerceIn(-1.0f, 1.0f)
                    }
                } else {
                    // 32-bit Integer PCM
                    for (i in 0 until totalFrames * numChannels) {
                        val sample = buffer.int.toFloat() / 2147483648.0f
                        floatSamples[floatIdx++] = sample.coerceIn(-1.0f, 1.0f)
                    }
                }
            }
            else -> return null
        }

        return IrsData(floatSamples, sampleRate, numChannels)
    }
}
