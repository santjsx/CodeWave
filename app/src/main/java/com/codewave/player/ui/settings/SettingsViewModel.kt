package com.codewave.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codewave.player.core.data.LibraryRepository
import com.codewave.player.core.data.SettingsRepository
import com.codewave.player.core.ota.OtaUpdateManager
import com.codewave.player.core.ota.UpdateInfo
import com.codewave.player.core.ota.UpdateStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    private val otaUpdateManager: OtaUpdateManager
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
            otaUpdateManager: OtaUpdateManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsRepository, libraryRepository, otaUpdateManager) as T
            }
        }
    }
}
