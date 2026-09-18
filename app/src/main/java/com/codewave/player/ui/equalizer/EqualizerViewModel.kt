package com.codewave.player.ui.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codewave.player.core.audio.AudioDeviceManager
import com.codewave.player.core.audio.AutoEqManager
import com.codewave.player.core.audio.ConnectedAudioDevice
import com.codewave.player.core.data.EqualizerRepository
import com.codewave.player.core.data.PlaybackRepository
import com.codewave.player.core.data.SettingsRepository
import com.codewave.player.core.model.AutoEqModel
import com.codewave.player.core.model.DSPStatus
import com.codewave.player.core.model.EQPreset
import com.codewave.player.core.model.EqualizerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EqualizerViewModel(
    private val equalizerRepository: EqualizerRepository,
    private val playbackRepository: PlaybackRepository,
    private val autoEqManager: AutoEqManager,
    private val audioDeviceManager: AudioDeviceManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val config: StateFlow<EqualizerConfig> = equalizerRepository.equalizerConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqualizerConfig())

    val presets: StateFlow<List<EQPreset>> = equalizerRepository.presets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EQPreset.PRESETS_10_BAND)

    val dspStatus: StateFlow<DSPStatus> = playbackRepository.playbackState
        .map { it.dspStatus }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DSPStatus.BYPASSED)

    // --- AutoEq States ---
    private val _autoEqBrands = MutableStateFlow<List<String>>(listOf("All"))
    val autoEqBrands: StateFlow<List<String>> = _autoEqBrands.asStateFlow()

    private val _autoEqModels = MutableStateFlow<List<AutoEqModel>>(emptyList())
    val autoEqModels: StateFlow<List<AutoEqModel>> = _autoEqModels.asStateFlow()

    private val _selectedBrandFilter = MutableStateFlow("All")
    val selectedBrandFilter: StateFlow<String> = _selectedBrandFilter.asStateFlow()

    private val _autoEqSearchQuery = MutableStateFlow("")
    val autoEqSearchQuery: StateFlow<String> = _autoEqSearchQuery.asStateFlow()

    val activeAutoEqModelId: StateFlow<String?> = settingsRepository.activeAutoEqModelId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Per-Device Audio Profiles ---
    val currentConnectedDevice: StateFlow<ConnectedAudioDevice> = audioDeviceManager.currentDevice

    val autoSwitchProfiles: StateFlow<Boolean> = settingsRepository.autoSwitchDeviceProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _boundProfileForCurrentDevice = MutableStateFlow<String?>(null)
    val boundProfileForCurrentDevice: StateFlow<String?> = _boundProfileForCurrentDevice.asStateFlow()

    private val _routingNotice = MutableStateFlow<String?>(null)
    val routingNotice: StateFlow<String?> = _routingNotice.asStateFlow()

    init {
        loadAutoEqBrandsAndModels()
        observeDeviceChanges()
    }

    private fun loadAutoEqBrandsAndModels() {
        viewModelScope.launch {
            _autoEqBrands.value = autoEqManager.getBrands()
            refreshAutoEqModels()
        }
    }

    private suspend fun refreshAutoEqModels() {
        _autoEqModels.value = autoEqManager.getModels(
            brandFilter = _selectedBrandFilter.value,
            query = _autoEqSearchQuery.value
        )
    }

    fun setAutoEqBrandFilter(brand: String) {
        _selectedBrandFilter.value = brand
        viewModelScope.launch { refreshAutoEqModels() }
    }

    fun setAutoEqSearchQuery(query: String) {
        _autoEqSearchQuery.value = query
        viewModelScope.launch { refreshAutoEqModels() }
    }

    fun applyAutoEqModel(model: AutoEqModel) {
        viewModelScope.launch {
            val preset = model.toEQPreset()
            equalizerRepository.applyPreset(preset)
            settingsRepository.setActiveAutoEqModelId(model.id)
        }
    }

    fun clearAutoEq() {
        viewModelScope.launch {
            settingsRepository.setActiveAutoEqModelId(null)
            val flatPreset = presets.value.find { it.name.equals("Flat", ignoreCase = true) }
                ?: EQPreset.PRESETS_10_BAND.first()
            equalizerRepository.applyPreset(flatPreset)
        }
    }

    private fun observeDeviceChanges() {
        viewModelScope.launch {
            currentConnectedDevice.collect { device ->
                val boundProfile = settingsRepository.getDeviceProfile(device.id).firstOrNull()
                _boundProfileForCurrentDevice.value = boundProfile

                if (autoSwitchProfiles.value && boundProfile != null) {
                    val matchingPreset = presets.value.find { it.name.equals(boundProfile, ignoreCase = true) }
                    if (matchingPreset != null) {
                        equalizerRepository.applyPreset(matchingPreset)
                        _routingNotice.value = "Auto-applied profile '${matchingPreset.name}' for ${device.name}"
                    } else {
                        // Check if boundProfile corresponds to an AutoEq model ID
                        val autoEqModel = autoEqManager.getModelById(boundProfile)
                        if (autoEqModel != null) {
                            applyAutoEqModel(autoEqModel)
                            _routingNotice.value = "Auto-applied AutoEq '${autoEqModel.displayName}' for ${device.name}"
                        }
                    }
                }
            }
        }
    }

    fun bindCurrentProfileToDevice(deviceId: String) {
        val currentPreset = config.value.activePresetName
        viewModelScope.launch {
            settingsRepository.setDeviceProfile(deviceId, currentPreset)
            _boundProfileForCurrentDevice.value = currentPreset
            _routingNotice.value = "Bound '$currentPreset' to current output"
        }
    }

    fun clearDeviceProfile(deviceId: String) {
        viewModelScope.launch {
            settingsRepository.clearDeviceProfile(deviceId)
            _boundProfileForCurrentDevice.value = null
            _routingNotice.value = "Cleared device profile binding"
        }
    }

    fun setAutoSwitchProfiles(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSwitchDeviceProfiles(enabled)
        }
    }

    fun dismissRoutingNotice() {
        _routingNotice.value = null
    }

    fun toggleEnabled(enabled: Boolean) {
        viewModelScope.launch { equalizerRepository.setEnabled(enabled) }
    }

    fun setPreampGain(gainDb: Float) {
        viewModelScope.launch { equalizerRepository.setPreampGain(gainDb) }
    }

    fun toggleLimiter(enabled: Boolean) {
        viewModelScope.launch { equalizerRepository.setLimiterEnabled(enabled) }
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        viewModelScope.launch { equalizerRepository.setBandGain(bandIndex, gainDb) }
    }

    fun applyPreset(preset: EQPreset) {
        viewModelScope.launch {
            equalizerRepository.applyPreset(preset)
            settingsRepository.setActiveAutoEqModelId(null)
        }
    }

    fun toggleBass(enabled: Boolean) {
        viewModelScope.launch { equalizerRepository.setBassEnabled(enabled) }
    }

    fun setBassGain(gainDb: Float) {
        viewModelScope.launch { equalizerRepository.setBassGain(gainDb) }
    }

    fun toggleClarity(enabled: Boolean) {
        viewModelScope.launch { equalizerRepository.setClarityEnabled(enabled) }
    }

    fun setClarityGain(gainDb: Float) {
        viewModelScope.launch { equalizerRepository.setClarityGain(gainDb) }
    }

    fun toggleConvolver(enabled: Boolean) {
        viewModelScope.launch { equalizerRepository.setConvolverEnabled(enabled) }
    }

    fun setIrsName(name: String?) {
        viewModelScope.launch { equalizerRepository.setIrsName(name) }
    }

    fun saveCustomPreset(name: String, subtitle: String = "") {
        val current = config.value
        val gains = current.bands.map { it.gainDb }
        viewModelScope.launch {
            equalizerRepository.saveCustomPreset(name, current.preampGainDb, gains)
        }
    }

    fun deletePreset(preset: EQPreset) {
        viewModelScope.launch {
            equalizerRepository.deletePreset(preset)
        }
    }

    fun deletePreset(presetId: Long) {
        val preset = presets.value.find { it.id == presetId } ?: return
        viewModelScope.launch {
            equalizerRepository.deletePreset(preset)
        }
    }

    fun resetPresetsToDefaults() {
        viewModelScope.launch {
            equalizerRepository.resetPresetsToDefaults()
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            settingsRepository.setActiveAutoEqModelId(null)
            val flatPreset = presets.value.find { it.name.equals("Flat", ignoreCase = true) }
                ?: EQPreset.PRESETS_10_BAND.first()
            equalizerRepository.applyPreset(flatPreset)
            equalizerRepository.setPreampGain(0f)
            equalizerRepository.setLimiterEnabled(true)
            equalizerRepository.setBassGain(4.0f)
            equalizerRepository.setBassEnabled(false)
            equalizerRepository.setClarityGain(3.0f)
            equalizerRepository.setClarityEnabled(false)
            equalizerRepository.setConvolverEnabled(false)
        }
    }

    companion object {
        fun provideFactory(
            equalizerRepository: EqualizerRepository,
            playbackRepository: PlaybackRepository,
            autoEqManager: AutoEqManager,
            audioDeviceManager: AudioDeviceManager,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EqualizerViewModel(
                    equalizerRepository,
                    playbackRepository,
                    autoEqManager,
                    audioDeviceManager,
                    settingsRepository
                ) as T
            }
        }
    }
}
