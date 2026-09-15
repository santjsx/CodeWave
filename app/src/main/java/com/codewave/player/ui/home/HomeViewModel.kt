package com.codewave.player.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codewave.player.core.data.LibraryRepository
import com.codewave.player.core.data.PlaybackRepository
import com.codewave.player.core.database.dao.LibraryStats
import com.codewave.player.core.model.Track
import com.codewave.player.core.scanner.ScanProgress
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
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    val scanProgress: StateFlow<ScanProgress> = libraryRepository.scanProgress

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

    fun playTrack(track: Track, queue: List<Track>) {
        playbackRepository.playTrack(track, queue)
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            libraryRepository.setFavorite(track.id, !track.isFavorite)
        }
    }

    companion object {
        fun provideFactory(
            libraryRepository: LibraryRepository,
            playbackRepository: PlaybackRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(libraryRepository, playbackRepository) as T
            }
        }
    }
}
