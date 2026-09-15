package com.codewave.player.core.scanner

import java.io.File
import java.security.MessageDigest

object AudioFingerprint {

    /**
     * Computes lightweight stable fingerprint to identify tracks even after
     * rename, folder relocation, or MediaStore ID re-assignment (PRD Section 45).
     */
    fun compute(
        fileSize: Long,
        durationMs: Long,
        title: String,
        artist: String
    ): String {
        val raw = "$fileSize:$durationMs:${title.lowercase().trim()}:${artist.lowercase().trim()}"
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val bytes = digest.digest(raw.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            raw.hashCode().toString()
        }
    }
}

object CandidateValidator {

    /**
     * Ensures partially downloaded files or zero-byte corrupted files
     * are not indexed into the active library (PRD Section 44).
     */
    fun isCandidateReady(path: String, fileSize: Long): Boolean {
        if (fileSize <= 1024L) return false // Exclude zero or tiny broken files
        val file = File(path)
        if (!file.exists()) return true // MediaStore entry might be content-based / scoped storage
        return file.length() > 0 && file.canRead()
    }
}
