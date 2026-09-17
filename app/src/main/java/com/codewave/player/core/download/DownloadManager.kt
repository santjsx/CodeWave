package com.codewave.player.core.download

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.codewave.player.core.database.dao.DownloadDao
import com.codewave.player.core.database.entity.DownloadTaskEntity
import com.codewave.player.core.model.DownloadStatus
import com.codewave.player.core.model.DownloadTask
import com.codewave.player.core.model.TargetAudioFormat
import com.codewave.player.core.network.downloader.LosslessSourceProvider
import com.codewave.player.core.scanner.AudioScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val losslessSourceProvider: LosslessSourceProvider,
    private val audioScanner: AudioScanner,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    val allDownloads: Flow<List<DownloadTask>> = downloadDao.getAllDownloadsFlow()
        .map { list -> list.map { it.toDomain() } }

    val activeDownloads: Flow<List<DownloadTask>> = downloadDao.getActiveDownloadsFlow()
        .map { list -> list.map { it.toDomain() } }

    val completedDownloads: Flow<List<DownloadTask>> = downloadDao.getCompletedDownloadsFlow()
        .map { list -> list.map { it.toDomain() } }

    fun enqueueDownload(
        id: String,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
        artworkUri: String?,
        isrc: String? = null,
        targetFormat: TargetAudioFormat = TargetAudioFormat.FLAC,
        customExtensionUrl: String? = null
    ) {
        val task = DownloadTask(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            artworkUri = artworkUri,
            isrc = isrc,
            targetFormat = targetFormat,
            status = DownloadStatus.PENDING
        )

        scope.launch {
            downloadDao.insertDownload(DownloadTaskEntity.fromDomain(task))
            startDownloadService()
            processTask(task, customExtensionUrl)
        }
    }

    fun cancelDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        scope.launch {
            downloadDao.updateStatus(id, DownloadStatus.CANCELLED.name)
        }
    }

    fun removeDownload(id: String) {
        cancelDownload(id)
        scope.launch {
            downloadDao.deleteDownloadById(id)
        }
    }

    fun clearCompleted() {
        scope.launch {
            downloadDao.clearCompletedDownloads()
        }
    }

    private fun processTask(task: DownloadTask, customExtensionUrl: String?) {
        val job = scope.launch {
            try {
                downloadDao.updateStatus(task.id, DownloadStatus.CONNECTING.name)

                // 1. Resolve highest quality download source (FLAC with cascade)
                val sourceResult = losslessSourceProvider.resolveDownloadSource(
                    title = task.title,
                    artist = task.artist,
                    album = task.album,
                    isrc = task.isrc,
                    customExtensionUrl = customExtensionUrl,
                    preferredFormat = task.targetFormat
                ).getOrThrow()

                downloadDao.updateStatus(task.id, DownloadStatus.DOWNLOADING.name)

                // 2. Download audio stream chunked to cache file
                val sanitizedTitle = task.title.replace(Regex("[/\\\\?%*:|\"<>]"), "_")
                val sanitizedArtist = task.artist.replace(Regex("[/\\\\?%*:|\"<>]"), "_")
                val filename = "$sanitizedArtist - $sanitizedTitle.${sourceResult.format.extension}"

                val tempFile = File(context.cacheDir, "${task.id}_download.tmp")
                downloadStream(sourceResult.downloadUrl, tempFile, task.id, sourceResult.requestHeaders)

                // 3. Tag FLAC with audiophile metadata if target is FLAC
                downloadDao.updateStatus(task.id, DownloadStatus.TAGGING.name)
                var artworkBytes: ByteArray? = null
                if (!task.artworkUri.isNullOrBlank()) {
                    try {
                        val artReq = Request.Builder().url(task.artworkUri).build()
                        val artRes = okHttpClient.newCall(artReq).execute()
                        if (artRes.isSuccessful) {
                            artworkBytes = artRes.body?.bytes()
                        }
                    } catch (_: Exception) {}
                }

                if (sourceResult.format == TargetAudioFormat.FLAC) {
                    FlacTagger.tagFlacFile(
                        file = tempFile,
                        title = task.title,
                        artist = task.artist,
                        album = task.album,
                        isrc = task.isrc,
                        artworkBytes = artworkBytes
                    )
                }

                // 4. Save to destination (Music/CodeWave) via MediaStore or SAF
                val finalFileUri = saveToPublicMusicFolder(tempFile, filename, sourceResult.format)
                tempFile.delete()

                // 5. Mark as completed in Room database
                downloadDao.markCompleted(
                    id = task.id,
                    completedAt = System.currentTimeMillis(),
                    localUri = finalFileUri.toString()
                )

                // 6. Trigger instantaneous MediaStore scan into CodeWave library
                audioScanner.scanSingleFile(context, finalFileUri)

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    downloadDao.updateStatus(task.id, DownloadStatus.CANCELLED.name)
                } else {
                    downloadDao.updateStatus(task.id, DownloadStatus.FAILED.name, e.localizedMessage ?: "Unknown error")
                }
            } finally {
                activeJobs.remove(task.id)
                val tempFile = File(context.cacheDir, "${task.id}_download.tmp")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }
        activeJobs[task.id] = job
    }

    private suspend fun downloadStream(
        url: String,
        targetFile: File,
        taskId: String,
        requestHeaders: Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder().url(url)

        val effectiveHeaders = mutableMapOf<String, String>()
        if (url.contains("googlevideo.com")) {
            effectiveHeaders.putAll(com.codewave.player.core.network.innertube.PlayerClient.forStreamUrl(url).mediaHeaders())
        }
        effectiveHeaders.putAll(requestHeaders)

        if (effectiveHeaders.isEmpty() || !effectiveHeaders.containsKey("User-Agent")) {
            effectiveHeaders["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"
        }

        effectiveHeaders.forEach { (name, value) ->
            requestBuilder.header(name, value)
        }

        val request = requestBuilder.build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("HTTP ${response.code} downloading audio stream")

        val body = response.body ?: throw Exception("Empty response body")
        val totalBytes = body.contentLength()
        var downloadedBytes = 0L

        body.byteStream().use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var lastSpeedCalcTime = System.currentTimeMillis()
                var bytesSinceLastCalc = 0L
                var currentSpeed = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    coroutineContext.ensureActive()
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesSinceLastCalc += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDelta = now - lastSpeedCalcTime
                    if (timeDelta >= 500) {
                        currentSpeed = (bytesSinceLastCalc * 1000L) / timeDelta
                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f
                        downloadDao.updateProgress(taskId, progress, downloadedBytes, currentSpeed)
                        bytesSinceLastCalc = 0L
                        lastSpeedCalcTime = now
                    }
                }
            }
        }
    }

    private fun saveToPublicMusicFolder(sourceFile: File, filename: String, format: TargetAudioFormat): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                put(MediaStore.Audio.Media.MIME_TYPE, format.mimeType)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/CodeWave")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = context.contentResolver.insert(collection, contentValues)
                ?: throw Exception("Could not create MediaStore entry")

            try {
                context.contentResolver.openOutputStream(itemUri)?.use { outStream ->
                    sourceFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                context.contentResolver.update(itemUri, contentValues, null, null)
                itemUri
            } catch (e: Exception) {
                try {
                    context.contentResolver.delete(itemUri, null, null)
                } catch (_: Exception) {}
                throw e
            }
        } else {
            val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "CodeWave").apply {
                if (!exists()) mkdirs()
            }
            val destFile = File(musicDir, filename)
            sourceFile.copyTo(destFile, overwrite = true)
            Uri.fromFile(destFile)
        }
    }

    private fun startDownloadService() {
        try {
            val intent = Intent(context, DownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {}
    }
}
