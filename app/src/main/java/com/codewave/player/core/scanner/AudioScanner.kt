package com.codewave.player.core.scanner

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.codewave.player.core.database.dao.TrackDao
import com.codewave.player.core.database.entity.TrackEntity
import com.codewave.player.core.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScanProgress(
    val isScanning: Boolean = false,
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val currentFile: String = ""
)

class AudioScanner(
    private val context: Context,
    private val trackDao: TrackDao,
    private val settingsRepository: SettingsRepository? = null
) {
    companion object {
        private val BLACKLISTED_PATH_PATTERNS = listOf(
            "/whatsapp/media/whatsapp audio",
            "/whatsapp/media/whatsapp voice notes",
            "/telegram/telegram audio",
            "/recordings/call",
            "/notifications",
            "/ringtones",
            "/alarms"
        )
    }

    private fun isPathBlacklisted(path: String): Boolean {
        val lowerPath = path.lowercase().replace('\\', '/')
        return BLACKLISTED_PATH_PATTERNS.any { lowerPath.contains(it) }
    }

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val scannerScope = CoroutineScope(Dispatchers.IO + Job())
    private var scanJob: Job? = null

    suspend fun scanLibrary(): Int = withContext(Dispatchers.IO) {
        if (_scanProgress.value.isScanning) return@withContext 0

        _scanProgress.value = ScanProgress(isScanning = true, processedCount = 0, totalCount = 0)

        val minDurationSecs = settingsRepository?.minDurationSeconds?.firstOrNull() ?: 30
        val minDurationMs = (minDurationSecs * 1000L).coerceAtLeast(1000L)

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE
        )

        // Filter out tiny ringtones / notification sounds & voice notes
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= $minDurationMs"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val candidates = mutableListOf<CandidateMedia>()
        val mediaStoreIds = mutableListOf<Long>()

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val mimeTypeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (c.moveToNext()) {
                    val mediaStoreId = c.getLong(idCol)
                    val path = c.getString(dataCol).orEmpty()
                    val title = c.getString(titleCol) ?: "Unknown Track"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val albumId = c.getLong(albumIdCol)
                    val duration = c.getLong(durationCol)
                    val size = c.getLong(sizeCol)
                    val dateAdded = c.getLong(dateAddedCol)
                    val dateModified = c.getLong(dateModifiedCol)
                    val mimeType = c.getString(mimeTypeCol)
                    if (isPathBlacklisted(path)) {
                        continue
                    }

                    mediaStoreIds.add(mediaStoreId)

                    if (CandidateValidator.isCandidateReady(path, size)) {
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            mediaStoreId
                        )
                        val albumArtUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()

                        candidates.add(
                            CandidateMedia(
                                mediaStoreId = mediaStoreId,
                                uri = uri,
                                path = path,
                                fallbackTitle = title,
                                fallbackArtist = artist,
                                fallbackAlbum = album,
                                albumArtUri = albumArtUri,
                                duration = duration,
                                size = size,
                                dateAdded = dateAdded,
                                dateModified = dateModified,
                                mimeType = mimeType
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Handle permission or resolver exceptions
        }

        _scanProgress.value = _scanProgress.value.copy(totalCount = candidates.size)

        // Reconcile and extract
        val batchEntities = mutableListOf<TrackEntity>()
        var processed = 0

        var lastProgressEmit = 0L

        for (candidate in candidates) {
            // Check if track modified or already present
            val existing = trackDao.getTrackByMediaStoreId(candidate.mediaStoreId)
            if (existing != null && existing.dateModified == candidate.dateModified && existing.fileSize == candidate.size) {
                processed++
                val now = System.currentTimeMillis()
                if (processed == candidates.size || processed % 10 == 0 || now - lastProgressEmit >= 50L) {
                    lastProgressEmit = now
                    _scanProgress.value = _scanProgress.value.copy(
                        processedCount = processed,
                        currentFile = candidate.fallbackTitle
                    )
                }
                continue
            }

            val metadata = MetadataExtractor.extract(
                context = context,
                uri = candidate.uri,
                path = candidate.path,
                mimeType = candidate.mimeType,
                fallbackTitle = candidate.fallbackTitle,
                fallbackArtist = candidate.fallbackArtist,
                fallbackAlbum = candidate.fallbackAlbum,
                fallbackDuration = candidate.duration
            )

            val fingerprint = AudioFingerprint.compute(
                fileSize = candidate.size,
                durationMs = metadata.durationMs,
                title = metadata.title,
                artist = metadata.artist
            )

            val entity = TrackEntity(
                id = existing?.id ?: 0,
                mediaStoreId = candidate.mediaStoreId,
                uri = candidate.uri.toString(),
                path = candidate.path,
                title = metadata.title,
                artist = metadata.artist,
                album = metadata.album,
                albumArtist = metadata.albumArtist,
                genre = metadata.genre,
                year = metadata.year,
                trackNumber = metadata.trackNumber,
                discNumber = metadata.discNumber,
                durationMs = metadata.durationMs,
                fileSize = candidate.size,
                dateAdded = candidate.dateAdded,
                dateModified = candidate.dateModified,
                mimeType = candidate.mimeType.orEmpty(),
                format = metadata.format.name,
                codec = metadata.codec,
                sampleRate = metadata.sampleRate,
                bitDepth = metadata.bitDepth,
                channels = metadata.channels,
                bitrateKbps = metadata.bitrateKbps,
                isLossless = metadata.isLossless,
                isHiRes = metadata.isHiRes,
                albumArtUri = candidate.albumArtUri,
                fingerprint = fingerprint,
                isFavorite = existing?.isFavorite ?: false,
                playCount = existing?.playCount ?: 0,
                lastPlayedTimestamp = existing?.lastPlayedTimestamp
            )

            batchEntities.add(entity)
            processed++

            val now = System.currentTimeMillis()
            if (processed == candidates.size || processed % 10 == 0 || now - lastProgressEmit >= 50L) {
                lastProgressEmit = now
                _scanProgress.value = _scanProgress.value.copy(
                    processedCount = processed,
                    currentFile = metadata.title
                )
            }

            // Commit in batches of 50 to maintain UI interactivity (PRD Section 42, 52)
            if (batchEntities.size >= 50) {
                trackDao.insertTracks(batchEntities)
                batchEntities.clear()
            }
        }

        if (batchEntities.isNotEmpty()) {
            trackDao.insertTracks(batchEntities)
            batchEntities.clear()
        }

        _scanProgress.value = _scanProgress.value.copy(
            processedCount = candidates.size,
            totalCount = candidates.size,
            isScanning = false
        )
        candidates.size
    }

    fun startObservingMediaStore(coroutineScope: CoroutineScope) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            private var debounceJob: Job? = null

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                debounceJob?.cancel()
                debounceJob = coroutineScope.launch {
                    delay(1500) // Debounce rapid filesystem notifications (PRD Section 43)
                    scanLibrary()
                }
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (_: Exception) {}
    }

    private data class CandidateMedia(
        val mediaStoreId: Long,
        val uri: Uri,
        val path: String,
        val fallbackTitle: String,
        val fallbackArtist: String,
        val fallbackAlbum: String,
        val albumArtUri: String,
        val duration: Long,
        val size: Long,
        val dateAdded: Long,
        val dateModified: Long,
        val mimeType: String?
    )
}
