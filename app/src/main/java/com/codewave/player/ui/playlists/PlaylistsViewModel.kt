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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PlaylistsViewModel(
    val libraryRepository: LibraryRepository,
    val playbackRepository: PlaybackRepository
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = libraryRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<Track>> = libraryRepository.getFavoriteTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTracks: StateFlow<List<Track>> = libraryRepository.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiResLosslessTracks: StateFlow<List<Track>> = allTracks.map { list ->
        list.filter { it.isLossless || it.isHiRes || it.sampleRate >= 48000 || (it.bitDepth ?: 0) >= 24 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heavyRotationTracks: StateFlow<List<Track>> = allTracks.map { list ->
        list.filter { it.playCount >= 2 }.sortedByDescending { it.playCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAddedTracks: StateFlow<List<Track>> = allTracks.map { list ->
        list.sortedByDescending { it.dateAdded }.take(50)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlistCovers: StateFlow<Map<Long, List<String>>> = playlists
        .flatMapLatest { playlistList ->
            if (playlistList.isEmpty()) {
                flowOf(emptyMap())
            } else {
                val flows = playlistList.map { pl ->
                    libraryRepository.getTracksForPlaylist(pl.id).map { tracks ->
                        pl.id to tracks.mapNotNull { it.albumArtUri }.distinct().take(3)
                    }
                }
                combine(flows) { pairs ->
                    pairs.toMap()
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            libraryRepository.createPlaylist(name.trim())
        }
    }

    fun renamePlaylist(playlist: Playlist, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            libraryRepository.renamePlaylist(playlist.id, newName.trim())
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

    fun playHiResLossless() {
        val list = hiResLosslessTracks.value
        if (list.isNotEmpty()) {
            playbackRepository.playQueue(list, 0)
        }
    }

    fun playHeavyRotation() {
        val list = heavyRotationTracks.value
        if (list.isNotEmpty()) {
            playbackRepository.playQueue(list, 0)
        }
    }

    fun playRecentlyAdded() {
        val list = recentlyAddedTracks.value
        if (list.isNotEmpty()) {
            playbackRepository.playQueue(list, 0)
        }
    }

    fun playTrack(track: Track, queue: List<Track>) {
        playbackRepository.playTrack(track, queue)
    }

    fun toggleFavorite(track: Track) {
        playbackRepository.toggleFavorite(track)
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
