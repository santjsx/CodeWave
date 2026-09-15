package com.codewave.player.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codewave.player.core.data.LibraryRepository
import com.codewave.player.core.data.PlaybackRepository
import com.codewave.player.core.database.dao.LibraryStats
import com.codewave.player.core.model.Track
import com.codewave.player.core.scanner.ScanProgress
import com.codewave.player.core.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val stats: LibraryStats = LibraryStats(0, 0, 0, 0, 0),
    val recentlyAdded: List<Track> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList(),
    val continueListeningTrack: Track? = null
)

class HomeViewModel(
    private val libraryRepository: LibraryRepository,
    private val playbackRepository: PlaybackRepository,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    val scanProgress: StateFlow<ScanProgress> = libraryRepository.scanProgress

    val lastPlayedPositionMs: StateFlow<Long> = (settingsRepository?.lastPlayedPositionMs ?: kotlinx.coroutines.flow.flowOf(0L))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val lastPlayedTrackId: StateFlow<Long?> = (settingsRepository?.lastPlayedTrackId ?: kotlinx.coroutines.flow.flowOf(null))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val continueListeningTrack: StateFlow<Track?> = if (settingsRepository != null) {
        kotlinx.coroutines.flow.combine(
            settingsRepository.lastPlayedTrackId,
            libraryRepository.getAllTracks()
        ) { trackId, tracks ->
            if (trackId != null) {
                tracks.find { it.id == trackId }
            } else {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        kotlinx.coroutines.flow.MutableStateFlow(null)
    }

    val stats: StateFlow<LibraryStats> = libraryRepository.getLibraryStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryStats(0, 0, 0, 0, 0)
        )

    val recentlyAdded: StateFlow<List<Track>> = libraryRepository.getRecentlyAdded()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentlyPlayed: StateFlow<List<Track>> = libraryRepository.getRecentlyPlayed()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun scanLibrary() {
        viewModelScope.launch {
            libraryRepository.scanLibrary()
        }
    }

    fun playTrack(track: Track, queue: List<Track>, startPositionMs: Long = 0L) {
        playbackRepository.playTrack(track, queue, startPositionMs)
    }

    fun toggleFavorite(track: Track) {
        playbackRepository.toggleFavorite(track)
    }

    companion object {
        fun provideFactory(
            libraryRepository: LibraryRepository,
            playbackRepository: PlaybackRepository,
            settingsRepository: SettingsRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(libraryRepository, playbackRepository, settingsRepository) as T
            }
        }
    }
}
