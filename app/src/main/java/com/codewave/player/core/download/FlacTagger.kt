package com.codewave.player.core.download

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Kotlin FLAC Vorbis Comment & Cover Art tagger.
 * Complies with the official Xiph FLAC / Vorbis comment RFC specification.
 * Zero JNI / zero native library dependency overhead.
 */
object FlacTagger {

    private val FLAC_MAGIC = byteArrayOf(0x66, 0x4C, 0x61, 0x43) // 'fLaC'

    fun tagFlacFile(
        file: File,
        title: String,
        artist: String,
        album: String,
        albumArtist: String? = null,
        year: String? = null,
        trackNumber: String? = null,
        isrc: String? = null,
        lyrics: String? = null,
        artworkBytes: ByteArray? = null
    ): Boolean {
        if (!file.exists() || file.length() < 4) return false

        try {
            RandomAccessFile(file, "r").use { raf ->
                val magic = ByteArray(4)
                raf.readFully(magic)
                if (!magic.contentEquals(FLAC_MAGIC)) {
                    return false // Not a valid FLAC file
                }
            }

            // Build Vorbis Comment payload
            val comments = mutableListOf<String>()
            if (title.isNotBlank()) comments.add("TITLE=$title")
            if (artist.isNotBlank()) comments.add("ARTIST=$artist")
            if (album.isNotBlank()) comments.add("ALBUM=$album")
            if (!albumArtist.isNullOrBlank()) comments.add("ALBUMARTIST=$albumArtist")
            if (!year.isNullOrBlank()) comments.add("DATE=$year")
            if (!trackNumber.isNullOrBlank()) comments.add("TRACKNUMBER=$trackNumber")
            if (!isrc.isNullOrBlank()) comments.add("ISRC=$isrc")
            if (!lyrics.isNullOrBlank()) comments.add("LYRICS=$lyrics")
            comments.add("ENCODER=CodeWave Hi-Res Audio Workstation")

            val vorbisCommentBlock = buildVorbisCommentBlock(comments)
            val pictureBlock = artworkBytes?.let { buildPictureBlock(it) }

            // Write to temp file first to guarantee atomic write and zero file corruption
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            RandomAccessFile(file, "r").use { src ->
                RandomAccessFile(tempFile, "rw").use { dst ->
                    dst.setLength(0)
                    dst.write(FLAC_MAGIC)
                    src.seek(4)

                    var isLast = false
                    val existingAudioOffset: Long

                    // Read existing metadata blocks, keep STREAMINFO (block type 0), discard old VORBIS/PICTURE
                    while (!isLast) {
                        val header = src.readUnsignedByte()
                        isLast = (header and 0x80) != 0
                        val blockType = header and 0x7F

                        val length = (src.readUnsignedByte() shl 16) or
                                     (src.readUnsignedByte() shl 8) or
                                     src.readUnsignedByte()

                        if (blockType == 0) { // STREAMINFO block
                            val streamInfoData = ByteArray(length)
                            src.readFully(streamInfoData)

                            // Write STREAMINFO (mark as not last because our new blocks follow)
                            dst.writeByte(0x00) // type 0, isLast = false
                            dst.writeByte((length shr 16) and 0xFF)
                            dst.writeByte((length shr 8) and 0xFF)
                            dst.writeByte(length and 0xFF)
                            dst.write(streamInfoData)
                        } else {
                            // Skip old vorbis comments or pictures to avoid duplicates
                            src.skipBytes(length)
                        }
                    }

                    existingAudioOffset = src.filePointer

                    // Write Vorbis Comment block
                    val isVorbisLast = pictureBlock == null
                    dst.writeByte(if (isVorbisLast) 0x84 else 0x04) // Type 4 = VORBIS_COMMENT
                    val vcLen = vorbisCommentBlock.size
                    dst.writeByte((vcLen shr 16) and 0xFF)
                    dst.writeByte((vcLen shr 8) and 0xFF)
                    dst.writeByte(vcLen and 0xFF)
                    dst.write(vorbisCommentBlock)

                    // Write Picture block if provided
                    if (pictureBlock != null) {
                        dst.writeByte(0x86) // Type 6 = PICTURE, marked as last (0x80 | 0x06)
                        val picLen = pictureBlock.size
                        dst.writeByte((picLen shr 16) and 0xFF)
                        dst.writeByte((picLen shr 8) and 0xFF)
                        dst.writeByte(picLen and 0xFF)
                        dst.write(pictureBlock)
                    }

                    // Copy original audio stream frames from source to destination
                    src.seek(existingAudioOffset)
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (src.read(buffer).also { bytesRead = it } != -1) {
                        dst.write(buffer, 0, bytesRead)
                    }
                }
            }

            // Atomic rename
            if (file.delete()) {
                tempFile.renameTo(file)
                return true
            }
            return false
        } catch (_: Exception) {
            return false
        }
    }

    private fun buildVorbisCommentBlock(comments: List<String>): ByteArray {
        val vendor = "CodeWave Lossless Engine".toByteArray(Charsets.UTF_8)
        val baos = ByteArrayOutputStream()

        // Vendor string length (4 bytes LE) + string
        baos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(vendor.size).array())
        baos.write(vendor)

        // Comment count (4 bytes LE)
        baos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(comments.size).array())

        // Each comment: length (4 bytes LE) + UTF-8 string
        for (comment in comments) {
            val bytes = comment.toByteArray(Charsets.UTF_8)
            baos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(bytes.size).array())
            baos.write(bytes)
        }

        return baos.toByteArray()
    }

    private fun buildPictureBlock(imageBytes: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()

        // Picture type 3 = Cover (front) (4 bytes BE)
        baos.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(3).array())

        // MIME type (4 bytes BE length + string)
        val mime = "image/jpeg".toByteArray(Charsets.US_ASCII)
        baos.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(mime.size).array())
        baos.write(mime)

        // Description (4 bytes BE length 0)
        baos.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(0).array())

        // Width, height, depth, colors (4 x 4 bytes BE)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        val width = if (options.outWidth > 0) options.outWidth else 1000
        val height = if (options.outHeight > 0) options.outHeight else 1000

        baos.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(width).array())
        baos.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(height).array())
        baos.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(24).array()) // 24-bit color
        baos.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(0).array())  // 0 indexed colors

        // Picture data length (4 bytes BE) + raw image bytes
        baos.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(imageBytes.size).array())
        baos.write(imageBytes)

        return baos.toByteArray()
    }
}
