package com.codewave.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codewave.player.core.data.LibraryRepository
import com.codewave.player.core.data.PlaybackRepository
import com.codewave.player.core.data.SettingsRepository
import com.codewave.player.core.model.Album
import com.codewave.player.core.model.AlbumSortOption
import com.codewave.player.core.model.Artist
import com.codewave.player.core.model.ArtistSortOption
import com.codewave.player.core.model.SongSortOption
import com.codewave.player.core.model.Track
import com.codewave.player.core.model.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    val libraryRepository: LibraryRepository,
    val playbackRepository: PlaybackRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val songSortOption: StateFlow<SongSortOption> = settingsRepository.songSortOption
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SongSortOption.DATE_ADDED_DESC)

    val albumSortOption: StateFlow<AlbumSortOption> = settingsRepository.albumSortOption
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlbumSortOption.DATE_ADDED_DESC)

    val artistSortOption: StateFlow<ArtistSortOption> = settingsRepository.artistSortOption
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArtistSortOption.NAME_ASC)

    val songViewMode: StateFlow<ViewMode> = settingsRepository.songViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST)

    val albumViewMode: StateFlow<ViewMode> = settingsRepository.albumViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.GRID)

    val songs: StateFlow<List<Track>> = combine(
        libraryRepository.getAllTracks(),
        songSortOption
    ) { tracks, sort ->
        when (sort) {
            SongSortOption.DATE_ADDED_DESC -> tracks.sortedByDescending { it.dateAdded }
            SongSortOption.TITLE_ASC -> tracks.sortedBy { it.title.lowercase() }
            SongSortOption.TITLE_DESC -> tracks.sortedByDescending { it.title.lowercase() }
            SongSortOption.ARTIST_ASC -> tracks.sortedBy { it.artist.lowercase() }
            SongSortOption.ALBUM_ASC -> tracks.sortedBy { it.album.lowercase() }
            SongSortOption.DURATION_DESC -> tracks.sortedByDescending { it.durationMs }
            SongSortOption.DURATION_ASC -> tracks.sortedBy { it.durationMs }
            SongSortOption.FILE_SIZE_DESC -> tracks.sortedByDescending { it.fileSize }
            SongSortOption.YEAR_DESC -> tracks.sortedByDescending { it.year ?: 0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = combine(
        libraryRepository.getAllAlbums(),
        albumSortOption
    ) { albumList, sort ->
        when (sort) {
            AlbumSortOption.DATE_ADDED_DESC -> albumList
            AlbumSortOption.TITLE_ASC -> albumList.sortedBy { it.title.lowercase() }
            AlbumSortOption.ARTIST_ASC -> albumList.sortedBy { it.artist.lowercase() }
            AlbumSortOption.YEAR_DESC -> albumList.sortedByDescending { it.year ?: 0 }
            AlbumSortOption.TRACK_COUNT_DESC -> albumList.sortedByDescending { it.trackCount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = combine(
        libraryRepository.getAllArtists(),
        artistSortOption
    ) { artistList, sort ->
        when (sort) {
            ArtistSortOption.NAME_ASC -> artistList.sortedBy { it.name.lowercase() }
            ArtistSortOption.NAME_DESC -> artistList.sortedByDescending { it.name.lowercase() }
            ArtistSortOption.TRACK_COUNT_DESC -> artistList.sortedByDescending { it.trackCount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Multi-Selection State (PRD Section 56)
    private val _selectedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTrackIds: StateFlow<Set<Long>> = _selectedTrackIds.asStateFlow()

    val isSelectionMode: Boolean
        get() = _selectedTrackIds.value.isNotEmpty()

    fun toggleTrackSelection(trackId: Long) {
        _selectedTrackIds.value = if (_selectedTrackIds.value.contains(trackId)) {
            _selectedTrackIds.value - trackId
        } else {
            _selectedTrackIds.value + trackId
        }
    }

    fun clearSelection() {
        _selectedTrackIds.value = emptySet()
    }

    fun selectAll() {
        _selectedTrackIds.value = songs.value.map { it.id }.toSet()
    }

    fun playTrack(track: Track) {
        playbackRepository.playTrack(track, songs.value)
    }

    fun playAll(startIndex: Int = 0) {
        if (songs.value.isNotEmpty()) {
            playbackRepository.playQueue(songs.value, startIndex)
        }
    }

    fun shuffleAll() {
        if (songs.value.isNotEmpty()) {
            playbackRepository.setShuffle(true)
            playbackRepository.playQueue(songs.value.shuffled(), 0)
        }
    }

    fun toggleFavorite(track: Track) {
        playbackRepository.toggleFavorite(track)
    }

    fun setSongSort(sort: SongSortOption) {
        viewModelScope.launch { settingsRepository.setSongSortOption(sort) }
    }

    fun setAlbumSort(sort: AlbumSortOption) {
        viewModelScope.launch { settingsRepository.setAlbumSortOption(sort) }
    }

    fun setArtistSort(sort: ArtistSortOption) {
        viewModelScope.launch { settingsRepository.setArtistSortOption(sort) }
    }

    fun setSongViewMode(mode: ViewMode) {
        viewModelScope.launch { settingsRepository.setSongViewMode(mode) }
    }

    fun setAlbumViewMode(mode: ViewMode) {
        viewModelScope.launch { settingsRepository.setAlbumViewMode(mode) }
    }

    companion object {
        fun provideFactory(
            libraryRepository: LibraryRepository,
            playbackRepository: PlaybackRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(libraryRepository, playbackRepository, settingsRepository) as T
            }
        }
    }
}
