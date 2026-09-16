package com.codewave.player.core.data

import android.content.Context
import com.codewave.player.core.database.CodeWaveDatabase
import com.codewave.player.core.scanner.AudioScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
    val database: CodeWaveDatabase
    val audioScanner: AudioScanner
    val libraryRepository: LibraryRepository
    val equalizerRepository: EqualizerRepository
    val settingsRepository: SettingsRepository
    val playbackRepository: PlaybackRepository
    val otaUpdateManager: com.codewave.player.core.ota.OtaUpdateManager
    val streamRepository: StreamRepository
    val downloadRepository: DownloadRepository
    val downloadManager: com.codewave.player.core.download.DownloadManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val database: CodeWaveDatabase by lazy {
        CodeWaveDatabase.getInstance(context)
    }

    override val audioScanner: AudioScanner by lazy {
        AudioScanner(
            context = context,
            trackDao = database.trackDao()
        ).also { scanner ->
            scanner.startObservingMediaStore(applicationScope)
        }
    }

    override val libraryRepository: LibraryRepository by lazy {
        DefaultLibraryRepository(
            trackDao = database.trackDao(),
            playlistDao = database.playlistDao(),
            audioScanner = audioScanner
        )
    }

    override val equalizerRepository: EqualizerRepository by lazy {
        DefaultEqualizerRepository(
            context = context,
            eqPresetDao = database.eqPresetDao()
        )
    }

    override val settingsRepository: SettingsRepository by lazy {
        DefaultSettingsRepository(context)
    }

    override val playbackRepository: PlaybackRepository by lazy {
        DefaultPlaybackRepository(
            context = context,
            libraryRepository = libraryRepository,
            settingsRepository = settingsRepository,
            equalizerRepository = equalizerRepository
        )
    }

    override val otaUpdateManager: com.codewave.player.core.ota.OtaUpdateManager by lazy {
        com.codewave.player.core.ota.OtaUpdateManager(context)
    }

    private val innerTubeClient by lazy {
        com.codewave.player.core.network.innertube.InnerTubeClient()
    }

    private val metadataResolver by lazy {
        com.codewave.player.core.network.resolver.MetadataResolver()
    }

    private val lyricsProvider by lazy {
        com.codewave.player.core.media.LrclibLyricsProvider()
    }

    private val losslessSourceProvider by lazy {
        com.codewave.player.core.network.downloader.LosslessSourceProvider(
            innerTubeClient = innerTubeClient,
            metadataResolver = metadataResolver
        )
    }

    override val downloadManager: com.codewave.player.core.download.DownloadManager by lazy {
        com.codewave.player.core.download.DownloadManager(
            context = context,
            downloadDao = database.downloadDao(),
            losslessSourceProvider = losslessSourceProvider,
            audioScanner = audioScanner
        )
    }

    override val streamRepository: StreamRepository by lazy {
        DefaultStreamRepository(
            innerTubeClient = innerTubeClient,
            metadataResolver = metadataResolver,
            lyricsProvider = lyricsProvider
        )
    }

    override val downloadRepository: DownloadRepository by lazy {
        DefaultDownloadRepository(
            downloadManager = downloadManager,
            settingsRepository = settingsRepository
        )
    }
}
