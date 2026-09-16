package com.codewave.player.ui.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codewave.player.core.data.DownloadRepository
import com.codewave.player.core.data.StreamRepository
import com.codewave.player.core.model.DownloadTask
import com.codewave.player.core.model.TargetAudioFormat
import com.codewave.player.core.network.resolver.ResolvedMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadUiState(
    val pastedLinkText: String = "",
    val isResolvingLink: Boolean = false,
    val resolvedMetadata: ResolvedMetadata? = null,
    val errorMessage: String? = null
)

class DownloadViewModel(
    private val downloadRepository: DownloadRepository,
    private val streamRepository: StreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    val activeDownloads: StateFlow<List<DownloadTask>> = downloadRepository.activeDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedDownloads: StateFlow<List<DownloadTask>> = downloadRepository.completedDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onLinkTextChanged(text: String) {
        _uiState.update { it.copy(pastedLinkText = text, errorMessage = null) }
    }

    fun resolvePastedLink() {
        val link = _uiState.value.pastedLinkText.trim()
        if (link.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingLink = true, errorMessage = null) }
            val result = streamRepository.resolveSpotifyMetadata(link)
            result.fold(
                onSuccess = { metadata ->
                    _uiState.update {
                        it.copy(
                            isResolvingLink = false,
                            resolvedMetadata = metadata
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isResolvingLink = false,
                            errorMessage = "Could not resolve link: ${error.localizedMessage ?: "Invalid URL"}"
                        )
                    }
                }
            )
        }
    }

    fun downloadResolvedMetadata(format: TargetAudioFormat = TargetAudioFormat.FLAC) {
        val metadata = _uiState.value.resolvedMetadata ?: return
        downloadRepository.enqueueMetadataDownload(metadata, format)
        _uiState.update {
            it.copy(
                pastedLinkText = "",
                resolvedMetadata = null
            )
        }
    }

    fun cancelDownload(id: String) {
        downloadRepository.cancelDownload(id)
    }

    fun removeDownload(id: String) {
        downloadRepository.removeDownload(id)
    }

    fun clearCompleted() {
        downloadRepository.clearCompleted()
    }

    companion object {
        fun provideFactory(
            downloadRepository: DownloadRepository,
            streamRepository: StreamRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DownloadViewModel(
                    downloadRepository = downloadRepository,
                    streamRepository = streamRepository
                ) as T
            }
        }
    }
}
