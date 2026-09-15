package com.codewave.player.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codewave.player.core.data.LibraryRepository
import com.codewave.player.core.data.PlaybackRepository
import com.codewave.player.core.model.Playlist
import com.codewave.player.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistsViewModel(
    private val libraryRepository: LibraryRepository,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = libraryRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<Track>> = libraryRepository.getFavoriteTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            libraryRepository.createPlaylist(name.trim())
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            libraryRepository.deletePlaylist(playlist.id)
        }
    }

    fun playFavorites() {
        val favs = favoriteTracks.value
        if (favs.isNotEmpty()) {
            playbackRepository.playQueue(favs, 0)
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
                return PlaylistsViewModel(libraryRepository, playbackRepository) as T
            }
        }
    }
}
