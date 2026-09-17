package com.codewave.player.ui.stream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codewave.player.core.data.DownloadRepository
import com.codewave.player.core.data.PlaybackRepository
import com.codewave.player.core.data.StreamRepository
import com.codewave.player.core.model.ExploreSection
import com.codewave.player.core.model.StreamTrack
import com.codewave.player.core.model.TargetAudioFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StreamUiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<StreamTrack> = emptyList(),
    val exploreCharts: List<StreamTrack> = emptyList(),
    val exploreSections: List<ExploreSection> = emptyList(),
    val selectedGenreFilter: String = "All",
    val availableGenreFilters: List<String> = listOf("All", "Telugu", "Tamil", "Hindi", "Global", "Lo-Fi"),
    val isLoadingCharts: Boolean = false,
    val activeResolvingTrackId: String? = null,
    val errorMessage: String? = null
)

class StreamViewModel(
    private val streamRepository: StreamRepository,
    private val playbackRepository: PlaybackRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreamUiState())
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadExploreSections("All")
    }

    fun selectGenreFilter(genre: String) {
        if (_uiState.value.selectedGenreFilter == genre) return
        _uiState.update { it.copy(selectedGenreFilter = genre) }
        loadExploreSections(genre)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, errorMessage = null) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350) // Debounce rapid keystrokes to prevent rate limiting
            _uiState.update { it.copy(isSearching = true) }
            streamRepository.search(query).collect { result ->
                result.fold(
                    onSuccess = { tracks ->
                        _uiState.update { it.copy(searchResults = tracks, isSearching = false) }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                errorMessage = "Search failed: ${error.localizedMessage ?: "Network error"}"
                            )
                        }
                    }
                )
            }
        }
    }

    fun loadExploreSections(genre: String = _uiState.value.selectedGenreFilter) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCharts = true, errorMessage = null) }
            streamRepository.getExploreSections(genre).collect { result ->
                result.fold(
                    onSuccess = { sections ->
                        val allTracks = sections.flatMap { it.tracks }.distinctBy { it.id }
                        _uiState.update {
                            it.copy(
                                exploreSections = sections,
                                exploreCharts = allTracks,
                                isLoadingCharts = false
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoadingCharts = false,
                                errorMessage = "Failed to load explore feed: ${error.localizedMessage}"
                            )
                        }
                    }
                )
            }
        }
    }

    fun loadExploreCharts() {
        loadExploreSections()
    }

    fun playStreamTrack(streamTrack: StreamTrack) {
        viewModelScope.launch {
            _uiState.update { it.copy(activeResolvingTrackId = streamTrack.id) }
            val result = streamRepository.resolveStreamTrack(streamTrack)
            _uiState.update { it.copy(activeResolvingTrackId = null) }

            result.fold(
                onSuccess = { playableTrack ->
                    playbackRepository.playTrack(playableTrack)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = "Could not stream '${streamTrack.title}': ${error.localizedMessage}")
                    }
                }
            )
        }
    }

    fun downloadTrack(streamTrack: StreamTrack, format: TargetAudioFormat = TargetAudioFormat.FLAC) {
        downloadRepository.enqueueStreamDownload(streamTrack, format)
    }

    companion object {
        fun provideFactory(
            streamRepository: StreamRepository,
            playbackRepository: PlaybackRepository,
            downloadRepository: DownloadRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StreamViewModel(
                    streamRepository = streamRepository,
                    playbackRepository = playbackRepository,
                    downloadRepository = downloadRepository
                ) as T
            }
        }
    }
}
