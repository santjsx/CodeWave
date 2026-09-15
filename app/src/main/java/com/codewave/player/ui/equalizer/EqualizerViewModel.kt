package com.codewave.player.ui.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codewave.player.core.data.EqualizerRepository
import com.codewave.player.core.data.PlaybackRepository
import com.codewave.player.core.model.DSPStatus
import com.codewave.player.core.model.EQPreset
import com.codewave.player.core.model.EqualizerConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EqualizerViewModel(
    private val equalizerRepository: EqualizerRepository,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    val config: StateFlow<EqualizerConfig> = equalizerRepository.equalizerConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqualizerConfig())

    val presets: StateFlow<List<EQPreset>> = equalizerRepository.presets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EQPreset.PRESETS_10_BAND)

    val dspStatus: StateFlow<DSPStatus> = playbackRepository.playbackState
        .map { it.dspStatus }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DSPStatus.ACTIVE)

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
        viewModelScope.launch { equalizerRepository.applyPreset(preset) }
    }

    companion object {
        fun provideFactory(
            equalizerRepository: EqualizerRepository,
            playbackRepository: PlaybackRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EqualizerViewModel(equalizerRepository, playbackRepository) as T
            }
        }
    }
}
