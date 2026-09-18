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
import android.util.Log
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScanProgress(
    val isScanning: Boolean = false,
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val currentFile: String = ""
)

enum class ScanAuthority {
    AUTHORITATIVE_SUCCESS,
    EMPTY_PROVEN_SUCCESS,
    UNTRUSTWORTHY_ANOMALY,
    FAILED_PERMISSION,
    FAILED_STORAGE,
    FAILED_QUERY,
    INCOMPLETE_ITERATION,
    CANCELLED
}

sealed class ScanResult {
    data class Success(val scannedCount: Int, val updatedCount: Int, val purgedCount: Int) : ScanResult()
    data class EmptySuccess(val message: String = "Library is empty") : ScanResult()
    data class AnomalyPreserved(val reason: String, val preservedCount: Int) : ScanResult()
    data class Failed(val authority: ScanAuthority, val reason: String, val cause: Throwable? = null) : ScanResult()
    data object Cancelled : ScanResult()
}

object ScanSafetyPolicy {
    /**
     * Resolves the authoritative state of a scan based on all environmental and execution preconditions.
     *
     * Invariant:
     * Destructive orphan reconciliation is ONLY permitted when authority is AUTHORITATIVE_SUCCESS.
     *
     * In particular, if existingLibraryCount > 0 and scannedMediaStoreIds.isEmpty():
     * Even if query returned non-null cursor with 0 rows and storage is reported mounted,
     * this condition is classified as UNTRUSTWORTHY_ANOMALY (Case 5 Attack protection).
     */
    fun evaluateScanAuthority(
        hasPermission: Boolean,
        isStorageMounted: Boolean,
        querySucceeded: Boolean,
        iterationCompletedNormally: Boolean,
        isCancelled: Boolean,
        scannedMediaStoreIds: Set<Long>,
        existingLibraryCount: Int
    ): ScanAuthority {
        if (isCancelled) return ScanAuthority.CANCELLED
        if (!hasPermission) return ScanAuthority.FAILED_PERMISSION
        if (!isStorageMounted) return ScanAuthority.FAILED_STORAGE
        if (!querySucceeded) return ScanAuthority.FAILED_QUERY
        if (!iterationCompletedNormally) return ScanAuthority.INCOMPLETE_ITERATION

        if (scannedMediaStoreIds.isEmpty()) {
            return if (existingLibraryCount == 0) {
                ScanAuthority.EMPTY_PROVEN_SUCCESS
            } else {
                ScanAuthority.UNTRUSTWORTHY_ANOMALY
            }
        }

        return ScanAuthority.AUTHORITATIVE_SUCCESS
    }

    /**
     * THE SINGLE SAFETY GATE for orphan computation.
     *
     * Invariant:
     * Only when authority == AUTHORITATIVE_SUCCESS can orphan IDs be returned for purging.
     * For all other states (EMPTY_PROVEN_SUCCESS, UNTRUSTWORTHY_ANOMALY, FAILED_*, CANCELLED),
     * returns an empty list.
     */
    fun computeOrphansToPurge(
        authority: ScanAuthority,
        existingMediaStoreIds: Set<Long>,
        scannedMediaStoreIds: Set<Long>
    ): List<Long> {
        if (authority != ScanAuthority.AUTHORITATIVE_SUCCESS) {
            return emptyList()
        }
        return existingMediaStoreIds.filter { it !in scannedMediaStoreIds }
    }
}

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

    private val _lastScanResult = MutableStateFlow<ScanResult>(ScanResult.EmptySuccess())
    val lastScanResult: StateFlow<ScanResult> = _lastScanResult.asStateFlow()

    private val scannerScope = CoroutineScope(Dispatchers.IO + Job())
    private var scanJob: Job? = null
    private val scanMutex = kotlinx.coroutines.sync.Mutex()

    fun hasAudioPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun isExternalStorageMounted(): Boolean {
        return android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED
    }

    suspend fun scanLibrary(): Int {
        val result = scanLibraryDetailed()
        return when (result) {
            is ScanResult.Success -> result.scannedCount
            is ScanResult.AnomalyPreserved -> result.preservedCount
            else -> 0
        }
    }

    suspend fun scanLibraryDetailed(): ScanResult = withContext(Dispatchers.IO) {
        if (!scanMutex.tryLock()) {
            return@withContext _lastScanResult.value
        }
        try {
            if (!hasAudioPermission()) {
                Log.w("AudioScanner", "Audio permission not granted; aborting scan without modifying database.")
                _scanProgress.value = _scanProgress.value.copy(isScanning = false)
                val res = ScanResult.Failed(ScanAuthority.FAILED_PERMISSION, "Permission not granted")
                _lastScanResult.value = res
                return@withContext res
            }

            if (!isExternalStorageMounted()) {
                Log.w("AudioScanner", "External storage not mounted; aborting scan without modifying database.")
                _scanProgress.value = _scanProgress.value.copy(isScanning = false)
                val res = ScanResult.Failed(ScanAuthority.FAILED_STORAGE, "External storage not mounted")
                _lastScanResult.value = res
                return@withContext res
            }

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

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= $minDurationMs"
            val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

            val candidates = mutableListOf<CandidateMedia>()
            val mediaStoreIds = mutableListOf<Long>()

            var querySucceeded = false
            var iterationCompletedNormally = false

            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    sortOrder
                )

                if (cursor != null) {
                    cursor.use { c ->
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
                            if (!coroutineContext.isActive) {
                                break
                            }
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
                    querySucceeded = true
                    iterationCompletedNormally = coroutineContext.isActive
                } else {
                    Log.w("AudioScanner", "MediaStore query returned null cursor (storage unmounted or permission denied).")
                }
            } catch (e: Exception) {
                Log.e("AudioScanner", "MediaStore query failed with exception: ${e.message}", e)
            }

            // Preload existing tracks for comparison and safety evaluation
            val existingTracksMap = trackDao.getAllTracks().associateBy { it.mediaStoreId }
            val scannedIdSet = mediaStoreIds.toSet()

            // Evaluate authority using the single ScanSafetyPolicy
            val authority = ScanSafetyPolicy.evaluateScanAuthority(
                hasPermission = hasAudioPermission(),
                isStorageMounted = isExternalStorageMounted(),
                querySucceeded = querySucceeded,
                iterationCompletedNormally = iterationCompletedNormally,
                isCancelled = !coroutineContext.isActive,
                scannedMediaStoreIds = scannedIdSet,
                existingLibraryCount = existingTracksMap.size
            )

            // NON-NEGOTIABLE SAFETY INVARIANT:
            // Failed, cancelled, incomplete, or untrustworthy scans must NEVER mutate or purge the database.
            if (authority != ScanAuthority.AUTHORITATIVE_SUCCESS) {
                _scanProgress.value = _scanProgress.value.copy(isScanning = false)
                val scanResult = when (authority) {
                    ScanAuthority.EMPTY_PROVEN_SUCCESS -> ScanResult.EmptySuccess()
                    ScanAuthority.UNTRUSTWORTHY_ANOMALY -> {
                        Log.w("AudioScanner", "Scan anomaly detected: MediaStore reported 0 tracks while database contains ${existingTracksMap.size} tracks. Preserving database intact.")
                        ScanResult.AnomalyPreserved("Zero tracks reported for non-empty library; database preserved.", existingTracksMap.size)
                    }
                    ScanAuthority.CANCELLED -> ScanResult.Cancelled
                    else -> ScanResult.Failed(authority, "Scan did not reach authoritative completion: $authority")
                }
                _lastScanResult.value = scanResult
                return@withContext scanResult
            }

            _scanProgress.value = _scanProgress.value.copy(totalCount = candidates.size)

            // BUILD COMPLETE TEMPORARY SNAPSHOT IN MEMORY BEFORE ANY DATABASE WRITE (SCANNER ATOMICITY)
            val snapshotEntities = mutableListOf<TrackEntity>()
            var processed = 0
            var lastProgressEmit = 0L

            for (candidate in candidates) {
                if (!coroutineContext.isActive) {
                    Log.w("AudioScanner", "Scan cancelled during metadata extraction; aborting with zero database writes.")
                    _scanProgress.value = _scanProgress.value.copy(isScanning = false)
                    val res = ScanResult.Cancelled
                    _lastScanResult.value = res
                    return@withContext res
                }

                val existing = existingTracksMap[candidate.mediaStoreId]
                if (existing != null && existing.dateModified == candidate.dateModified && existing.fileSize == candidate.size && existing.path == candidate.path) {
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

                snapshotEntities.add(entity)
                processed++

                val now = System.currentTimeMillis()
                if (processed == candidates.size || processed % 10 == 0 || now - lastProgressEmit >= 50L) {
                    lastProgressEmit = now
                    _scanProgress.value = _scanProgress.value.copy(
                        processedCount = processed,
                        currentFile = metadata.title
                    )
                }
            }

            // ATOMIC DATABASE RECONCILIATION
            // Computes orphans via the single gate and applies inserts + deletes inside a single Room @Transaction
            val orphanIds = purgeOrphans(authority, existingTracksMap, scannedIdSet)

            if (snapshotEntities.isNotEmpty()) {
                snapshotEntities.chunked(500).forEach { batch ->
                    trackDao.insertTracks(batch)
                }
            }

            _scanProgress.value = _scanProgress.value.copy(
                processedCount = candidates.size,
                totalCount = candidates.size,
                isScanning = false
            )

            val res = ScanResult.Success(
                scannedCount = candidates.size,
                updatedCount = snapshotEntities.size,
                purgedCount = orphanIds
            )
            _lastScanResult.value = res
            return@withContext res
        } finally {
            scanMutex.unlock()
        }
    }

    /**
     * THE SINGLE GATE FOR DESTRUCTIVE TRACK RECONCILIATION.
     *
     * Invariant: Destructive orphan purge can ONLY execute when authority is AUTHORITATIVE_SUCCESS.
     * Any other state guarantees ZERO track deletions.
     *
     * Returns the number of purged tracks.
     */
    suspend fun purgeOrphans(
        authority: ScanAuthority,
        existingTracks: Map<Long, TrackEntity>,
        scannedMediaStoreIds: Set<Long>
    ): Int {
        val orphanIds = ScanSafetyPolicy.computeOrphansToPurge(
            authority = authority,
            existingMediaStoreIds = existingTracks.keys,
            scannedMediaStoreIds = scannedMediaStoreIds
        )
        if (orphanIds.isEmpty()) return 0

        if (!isExternalStorageMounted()) {
            Log.w("AudioScanner", "External storage unmounted; skipping destructive orphan purge.")
            return 0
        }

        orphanIds.chunked(500).forEach { batch ->
            trackDao.deleteTracksAndCleanReferences(batch)
        }
        return orphanIds.size
    }

    private val observerLock = Any()
    private var mediaStoreObserver: ContentObserver? = null
    private var debounceJob: Job? = null

    fun startObservingMediaStore(coroutineScope: CoroutineScope) {
        synchronized(observerLock) {
            if (mediaStoreObserver != null) {
                // Idempotent: already observing, do not attach duplicate observers
                return
            }
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
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
                mediaStoreObserver = observer
            } catch (e: Exception) {
                Log.w("AudioScanner", "Failed to register MediaStore content observer: ${e.message}")
            }
        }
    }

    fun stopObservingMediaStore() {
        synchronized(observerLock) {
            debounceJob?.cancel()
            debounceJob = null
            mediaStoreObserver?.let { observer ->
                try {
                    context.contentResolver.unregisterContentObserver(observer)
                } catch (_: Exception) {}
                mediaStoreObserver = null
            }
        }
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
