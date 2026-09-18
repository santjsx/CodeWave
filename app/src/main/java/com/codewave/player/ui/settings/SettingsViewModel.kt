package com.codewave.player.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codewave.player.core.backup.BackupRestoreManager
import com.codewave.player.core.data.LibraryRepository
import com.codewave.player.core.data.SettingsRepository
import com.codewave.player.core.ota.OtaUpdateManager
import com.codewave.player.core.ota.UpdateInfo
import com.codewave.player.core.ota.UpdateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    private val otaUpdateManager: OtaUpdateManager,
    private val backupRestoreManager: BackupRestoreManager
) : ViewModel() {

    val gaplessEnabled: StateFlow<Boolean> = settingsRepository.gaplessEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val crossfadeSeconds: StateFlow<Int> = settingsRepository.crossfadeSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val themeId: StateFlow<String> = settingsRepository.themeId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "obsidian")

    val minDurationSeconds: StateFlow<Int> = settingsRepository.minDurationSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val otaUpdateStatus: StateFlow<UpdateStatus> = otaUpdateManager.updateStatus

    private val _backupOperationStatus = MutableStateFlow<String?>(null)
    val backupOperationStatus: StateFlow<String?> = _backupOperationStatus.asStateFlow()

    fun setThemeId(themeId: String) {
        viewModelScope.launch { settingsRepository.setThemeId(themeId) }
    }

    fun toggleGapless(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGaplessEnabled(enabled) }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setCrossfadeSeconds(seconds) }
    }

    fun setMinDurationSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setMinDurationSeconds(seconds)
            libraryRepository.scanLibrary()
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch { libraryRepository.scanLibrary() }
    }

    fun exportBackup(context: Context, destinationUri: Uri) {
        viewModelScope.launch {
            _backupOperationStatus.value = "Exporting workstation configuration..."
            backupRestoreManager.exportBackup(context, destinationUri)
                .onSuccess { summary ->
                    _backupOperationStatus.value = "Exported ${summary.playlistCount} playlists, ${summary.favoriteCount} favorites, ${summary.customEqCount} EQ presets successfully!"
                }
                .onFailure { error ->
                    _backupOperationStatus.value = "Export failed: ${error.localizedMessage}"
                }
        }
    }

    fun importBackup(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            _backupOperationStatus.value = "Restoring workstation configuration..."
            backupRestoreManager.importBackup(context, sourceUri)
                .onSuccess { summary ->
                    _backupOperationStatus.value = "Restored ${summary.playlistsRestored} playlists, ${summary.favoritesRestored} favorites, ${summary.presetsRestored} EQ presets!"
                    libraryRepository.scanLibrary()
                }
                .onFailure { error ->
                    _backupOperationStatus.value = "Restore failed: ${error.localizedMessage}"
                }
        }
    }

    fun clearBackupStatus() {
        _backupOperationStatus.value = null
    }

    /**
     * Explicit user-triggered OTA update check via GitHub Releases.
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            otaUpdateManager.checkForUpdates(showNotificationIfAvailable = false)
        }
    }

    /**
     * Download and automatically trigger package installation.
     */
    fun downloadAndInstall(info: UpdateInfo) {
        viewModelScope.launch {
            otaUpdateManager.downloadAndInstall(info)
        }
    }

    /**
     * Trigger package installer for downloaded APK.
     */
    fun installApk(file: File) {
        otaUpdateManager.installApk(file)
    }

    /**
     * Reset status back to idle.
     */
    fun resetOtaStatus() {
        otaUpdateManager.resetToIdle()
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            libraryRepository: LibraryRepository,
            otaUpdateManager: OtaUpdateManager,
            backupRestoreManager: BackupRestoreManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsRepository, libraryRepository, otaUpdateManager, backupRestoreManager) as T
            }
        }
    }
}
