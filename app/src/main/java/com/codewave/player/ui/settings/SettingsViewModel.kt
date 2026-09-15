package com.codewave.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codewave.player.core.data.LibraryRepository
import com.codewave.player.core.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateCheckResult(
    val isChecking: Boolean = false,
    val latestVersion: String? = null,
    val isUpdateAvailable: Boolean = false,
    val releaseNotes: String? = null,
    val downloadUrl: String? = null,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    val gaplessEnabled: StateFlow<Boolean> = settingsRepository.gaplessEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val crossfadeSeconds: StateFlow<Int> = settingsRepository.crossfadeSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val themeId: StateFlow<String> = settingsRepository.themeId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "obsidian")

    private val _updateState = MutableStateFlow(UpdateCheckResult())
    val updateState: StateFlow<UpdateCheckResult> = _updateState.asStateFlow()

    fun setThemeId(themeId: String) {
        viewModelScope.launch { settingsRepository.setThemeId(themeId) }
    }

    fun toggleGapless(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGaplessEnabled(enabled) }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setCrossfadeSeconds(seconds) }
    }

    fun rescanLibrary() {
        viewModelScope.launch { libraryRepository.scanLibrary() }
    }

    /**
     * User-triggered GitHub Releases check (PRD Section 80, 81).
     * Strictly offline-first: never runs in background without explicit user tap.
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = UpdateCheckResult(isChecking = true)

            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL("https://api.github.com/repos/codewave-player/codewave/releases/latest")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                    if (connection.responseCode == 200) {
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(body)
                        val tagName = json.optString("tag_name", "v1.0.0")
                        val bodyNotes = json.optString("body", "Release notes unavailable.")
                        val assets = json.optJSONArray("assets")
                        var apkUrl: String? = null
                        if (assets != null && assets.length() > 0) {
                            apkUrl = assets.getJSONObject(0).optString("browser_download_url")
                        }

                        val isNewer = tagName != "v1.0.0"
                        UpdateCheckResult(
                            isChecking = false,
                            latestVersion = tagName,
                            isUpdateAvailable = isNewer,
                            releaseNotes = bodyNotes,
                            downloadUrl = apkUrl
                        )
                    } else {
                        UpdateCheckResult(
                            isChecking = false,
                            errorMessage = "Unable to connect to GitHub Releases (HTTP ${connection.responseCode})"
                        )
                    }
                }
                _updateState.value = result
            } catch (e: Exception) {
                _updateState.value = UpdateCheckResult(
                    isChecking = false,
                    errorMessage = "Network unavailable. CODEWAVE remains fully functional offline."
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            libraryRepository: LibraryRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsRepository, libraryRepository) as T
            }
        }
    }
}
