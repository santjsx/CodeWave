package com.codewave.player.core.data

import com.codewave.player.core.download.DownloadManager
import com.codewave.player.core.model.DownloadTask
import com.codewave.player.core.model.StreamTrack
import com.codewave.player.core.model.TargetAudioFormat
import com.codewave.player.core.network.resolver.ResolvedMetadata
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    val allDownloads: Flow<List<DownloadTask>>
    val activeDownloads: Flow<List<DownloadTask>>
    val completedDownloads: Flow<List<DownloadTask>>

    fun enqueueStreamDownload(
        streamTrack: StreamTrack,
        targetFormat: TargetAudioFormat = TargetAudioFormat.FLAC
    )

    fun enqueueMetadataDownload(
        metadata: ResolvedMetadata,
        targetFormat: TargetAudioFormat = TargetAudioFormat.FLAC
    )

    fun cancelDownload(id: String)
    fun removeDownload(id: String)
    fun clearCompleted()
}

class DefaultDownloadRepository(
    private val downloadManager: DownloadManager,
    private val settingsRepository: SettingsRepository? = null
) : DownloadRepository {

    override val allDownloads: Flow<List<DownloadTask>> = downloadManager.allDownloads
    override val activeDownloads: Flow<List<DownloadTask>> = downloadManager.activeDownloads
    override val completedDownloads: Flow<List<DownloadTask>> = downloadManager.completedDownloads

    override fun enqueueStreamDownload(
        streamTrack: StreamTrack,
        targetFormat: TargetAudioFormat
    ) {
        downloadManager.enqueueDownload(
            id = "dl_${streamTrack.id}_${System.currentTimeMillis()}",
            title = streamTrack.title,
            artist = streamTrack.artist,
            album = streamTrack.album,
            durationMs = streamTrack.durationMs,
            artworkUri = streamTrack.artworkUri,
            isrc = streamTrack.isrc,
            targetFormat = targetFormat
        )
    }

    override fun enqueueMetadataDownload(
        metadata: ResolvedMetadata,
        targetFormat: TargetAudioFormat
    ) {
        val uniqueId = "dl_meta_${metadata.title.hashCode()}_${System.currentTimeMillis()}"
        downloadManager.enqueueDownload(
            id = uniqueId,
            title = metadata.title,
            artist = metadata.artist,
            album = metadata.album,
            durationMs = metadata.durationMs,
            artworkUri = metadata.artworkUrl,
            isrc = metadata.isrc,
            targetFormat = targetFormat
        )
    }

    override fun cancelDownload(id: String) {
        downloadManager.cancelDownload(id)
    }

    override fun removeDownload(id: String) {
        downloadManager.removeDownload(id)
    }

    override fun clearCompleted() {
        downloadManager.clearCompleted()
    }
}
