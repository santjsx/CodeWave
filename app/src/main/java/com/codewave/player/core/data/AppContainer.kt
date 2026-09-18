package com.codewave.player.core.data

import android.content.Context
import com.codewave.player.core.database.CodeWaveDatabase
import com.codewave.player.core.scanner.AudioScanner
import com.codewave.player.core.audio.AutoEqManager
import com.codewave.player.core.audio.AudioDeviceManager
import com.codewave.player.core.audio.ViperAudioProcessor
import com.codewave.player.core.backup.BackupRestoreManager
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
    val viperAudioProcessor: ViperAudioProcessor
    val autoEqManager: AutoEqManager
    val audioDeviceManager: AudioDeviceManager
    val backupRestoreManager: BackupRestoreManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val database: CodeWaveDatabase by lazy {
        CodeWaveDatabase.getInstance(context)
    }

    override val audioScanner: AudioScanner by lazy {
        AudioScanner(
            context = context,
            trackDao = database.trackDao(),
            settingsRepository = settingsRepository
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
            equalizerRepository = equalizerRepository,
            viperAudioProcessor = viperAudioProcessor
        )
    }

    override val otaUpdateManager: com.codewave.player.core.ota.OtaUpdateManager by lazy {
        com.codewave.player.core.ota.OtaUpdateManager(context)
    }

    override val viperAudioProcessor: ViperAudioProcessor by lazy {
        ViperAudioProcessor()
    }

    override val autoEqManager: AutoEqManager by lazy {
        AutoEqManager(context)
    }

    override val audioDeviceManager: AudioDeviceManager by lazy {
        AudioDeviceManager(context)
    }

    override val backupRestoreManager: BackupRestoreManager by lazy {
        BackupRestoreManager(database, settingsRepository)
    }
}
