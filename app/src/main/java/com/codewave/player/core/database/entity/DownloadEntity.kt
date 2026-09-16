package com.codewave.player.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.codewave.player.core.model.DownloadStatus
import com.codewave.player.core.model.DownloadTask
import com.codewave.player.core.model.TargetAudioFormat

@Entity(
    tableName = "download_tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUri: String?,
    val isrc: String? = null,
    val targetFormat: String = TargetAudioFormat.FLAC.name,
    val status: String = DownloadStatus.PENDING.name,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val errorMessage: String? = null,
    val localUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    fun toDomain(): DownloadTask = DownloadTask(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        artworkUri = artworkUri,
        isrc = isrc,
        targetFormat = try { TargetAudioFormat.valueOf(targetFormat) } catch (_: Exception) { TargetAudioFormat.FLAC },
        status = try { DownloadStatus.valueOf(status) } catch (_: Exception) { DownloadStatus.FAILED },
        progress = progress,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        speedBytesPerSec = speedBytesPerSec,
        errorMessage = errorMessage,
        localUri = localUri,
        createdAt = createdAt,
        completedAt = completedAt
    )

    companion object {
        fun fromDomain(task: DownloadTask): DownloadTaskEntity = DownloadTaskEntity(
            id = task.id,
            title = task.title,
            artist = task.artist,
            album = task.album,
            durationMs = task.durationMs,
            artworkUri = task.artworkUri,
            isrc = task.isrc,
            targetFormat = task.targetFormat.name,
            status = task.status.name,
            progress = task.progress,
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes,
            speedBytesPerSec = task.speedBytesPerSec,
            errorMessage = task.errorMessage,
            localUri = task.localUri,
            createdAt = task.createdAt,
            completedAt = task.completedAt
        )
    }
}
