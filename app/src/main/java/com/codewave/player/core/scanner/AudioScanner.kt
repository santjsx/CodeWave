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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val trackDao: TrackDao
) {
    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val scannerScope = CoroutineScope(Dispatchers.IO + Job())
    private var scanJob: Job? = null

    suspend fun scanLibrary(): Int = withContext(Dispatchers.IO) {
        if (_scanProgress.value.isScanning) return@withContext 0

        _scanProgress.value = ScanProgress(isScanning = true, processedCount = 0, totalCount = 0)

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

        // Filter out tiny ringtones / notification sounds < 5 seconds
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
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

    /**
     * Immediately scans a single newly-downloaded audio file into the Room database
     * so it surfaces in the user's library without waiting for a full MediaStore batch scan.
     */
    suspend fun scanSingleFile(context: Context, fileUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            var mediaStoreId = -1L
            var path = fileUri.path.orEmpty()
            var titleFallback = "Downloaded Audio"
            var artistFallback = "Unknown Artist"
            var albumFallback = "Unknown Album"
            var size = 0L
            var dateAdded = System.currentTimeMillis() / 1000
            var mimeType = "audio/flac"

            var albumId = -1L

            if (fileUri.scheme == "content") {
                val proj = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.MIME_TYPE
                )
                context.contentResolver.query(fileUri, proj, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        mediaStoreId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                        path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)).orEmpty()
                        titleFallback = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)).orEmpty()
                        artistFallback = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)).orEmpty()
                        albumFallback = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)).orEmpty()
                        val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                        if (albumIdCol != -1) albumId = cursor.getLong(albumIdCol)
                        size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                        mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)).orEmpty()
                    }
                }
            } else if (fileUri.scheme == "file") {
                val file = java.io.File(fileUri.path.orEmpty())
                if (file.exists()) {
                    size = file.length()
                    titleFallback = file.nameWithoutExtension
                }
            }

            val metadata = MetadataExtractor.extract(
                context = context,
                uri = fileUri,
                path = path,
                mimeType = mimeType,
                fallbackTitle = titleFallback,
                fallbackArtist = artistFallback,
                fallbackAlbum = albumFallback,
                fallbackDuration = 0L
            )

            val fingerprint = AudioFingerprint.compute(
                fileSize = size,
                durationMs = metadata.durationMs,
                title = metadata.title,
                artist = metadata.artist
            )

            val albumArtUri = if (albumId > 0L) {
                ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()
            } else null

            val entity = TrackEntity(
                mediaStoreId = if (mediaStoreId != -1L) mediaStoreId else System.currentTimeMillis(),
                uri = fileUri.toString(),
                path = path,
                title = metadata.title,
                artist = metadata.artist,
                album = metadata.album,
                albumArtist = metadata.albumArtist,
                genre = metadata.genre,
                year = metadata.year,
                trackNumber = metadata.trackNumber,
                discNumber = metadata.discNumber,
                durationMs = metadata.durationMs,
                fileSize = size,
                dateAdded = dateAdded,
                dateModified = dateAdded,
                mimeType = mimeType,
                format = metadata.format.name,
                codec = metadata.codec,
                sampleRate = metadata.sampleRate,
                bitDepth = metadata.bitDepth,
                channels = metadata.channels,
                bitrateKbps = metadata.bitrateKbps,
                isLossless = metadata.isLossless,
                isHiRes = metadata.isHiRes,
                albumArtUri = albumArtUri,
                fingerprint = fingerprint,
                isFavorite = false,
                playCount = 0,
                lastPlayedTimestamp = null
            )

            trackDao.insertTrack(entity)
            true
        } catch (_: Exception) {
            false
        }
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
