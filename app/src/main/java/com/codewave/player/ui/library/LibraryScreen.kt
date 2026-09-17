package com.codewave.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.codewave.player.core.designsystem.component.CWQualityBadge
import com.codewave.player.core.designsystem.component.tactileClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.activity.compose.BackHandler
import com.codewave.player.ui.collection.CollectionDetailSheet
import com.codewave.player.ui.collection.CollectionTarget
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codewave.player.core.designsystem.component.CWAlbumCard
import com.codewave.player.core.designsystem.component.CWButton
import com.codewave.player.core.designsystem.component.CWButtonVariant
import com.codewave.player.core.designsystem.component.CWEmptyState
import com.codewave.player.core.designsystem.component.CWPlayAllButton
import com.codewave.player.core.designsystem.component.CWTrackRow
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.Album
import com.codewave.player.core.model.AlbumSortOption
import com.codewave.player.core.model.Artist
import com.codewave.player.core.model.ArtistSortOption
import com.codewave.player.core.model.SongSortOption
import com.codewave.player.core.model.Track
import com.codewave.player.core.model.ViewMode

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    initialTab: Int = 0,
    onTrackInspect: (Track) -> Unit,
    onTrackOptions: ((Track) -> Unit)? = null,
    onNavigateToAlbum: (Album) -> Unit = {},
    onNavigateToArtist: (Artist) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    val albumGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val albumListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val songsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val artistsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val tabs = listOf("Songs", "Albums", "Artists")

    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()

    val songSort by viewModel.songSortOption.collectAsState()
    val albumSort by viewModel.albumSortOption.collectAsState()
    val artistSort by viewModel.artistSortOption.collectAsState()

    val songViewMode by viewModel.songViewMode.collectAsState()
    val albumViewMode by viewModel.albumViewMode.collectAsState()

    val selectedTrackIds by viewModel.selectedTrackIds.collectAsState()
    val isSelectionMode = viewModel.isSelectionMode

    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
    ) {
        // Multi-Selection Action Header (PRD Section 56)
        if (isSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CWColors.SurfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.clearSelection() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel selection", tint = CWColors.TextPrimary)
                    }
                    Text(
                        text = "${selectedTrackIds.size} SELECTED",
                        style = CWTypography.TechBadge,
                        color = CWColors.AccentCyan
                    )
                }

                Row {
                    IconButton(onClick = { viewModel.selectAll() }) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = CWColors.TextSecondary)
                    }
                }
            }
        } else {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIBRARY",
                    style = CWTypography.AppTypography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CWColors.TextPrimary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // View Mode Switcher for Albums (Grid vs List per PRD Section 54)
                    if (selectedTab == 1) {
                        IconButton(onClick = {
                            viewModel.setAlbumViewMode(
                                if (albumViewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                            )
                        }) {
                            Icon(
                                imageVector = if (albumViewMode == ViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Switch View Mode",
                                tint = CWColors.TextSecondary
                            )
                        }
                    }

                    // Sort Menu Button
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = CWColors.TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier.background(CWColors.SurfaceElevated)
                        ) {
                            when (selectedTab) {
                                0 -> {
                                    SongSortOption.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option.displayName,
                                                    style = CWTypography.AppTypography.bodyMedium,
                                                    color = if (songSort == option) CWColors.AccentCyan else CWColors.TextPrimary
                                                )
                                            },
                                            onClick = {
                                                viewModel.setSongSort(option)
                                                sortMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                                1 -> {
                                    AlbumSortOption.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option.displayName,
                                                    style = CWTypography.AppTypography.bodyMedium,
                                                    color = if (albumSort == option) CWColors.AccentCyan else CWColors.TextPrimary
                                                )
                                            },
                                            onClick = {
                                                viewModel.setAlbumSort(option)
                                                sortMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                                2 -> {
                                    ArtistSortOption.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option.displayName,
                                                    style = CWTypography.AppTypography.bodyMedium,
                                                    color = if (artistSort == option) CWColors.AccentCyan else CWColors.TextPrimary
                                                )
                                            },
                                            onClick = {
                                                viewModel.setArtistSort(option)
                                                sortMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CWColors.Background,
            contentColor = CWColors.AccentCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CWColors.AccentCyan,
                    height = 2.dp
                )
            },
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(CWColors.BorderSubtle)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                val count = when (index) {
                    0 -> songs.size
                    1 -> albums.size
                    2 -> artists.size
                    else -> 0
                }
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title.uppercase(),
                                style = CWTypography.TechBadge,
                                color = if (selectedTab == index) CWColors.AccentCyan else CWColors.TextSecondary
                            )
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($count)",
                                    style = CWTypography.TechBadge,
                                    color = if (selectedTab == index) CWColors.AccentCyan.copy(alpha = 0.8f) else CWColors.TextTertiary
                                )
                            }
                        }
                    }
                )
            }
        }

        // Tab Contents
        when (selectedTab) {
            0 -> SongsTab(
                songs = songs,
                isSelectionMode = isSelectionMode,
                selectedTrackIds = selectedTrackIds,
                onTrackClick = { viewModel.playTrack(it) },
                onSelectToggle = { trackId -> viewModel.toggleTrackSelection(trackId) },
                onFavoriteClick = { viewModel.toggleFavorite(it) },
                onMoreClick = { onTrackOptions?.invoke(it) ?: onTrackInspect(it) },
                onPlayAllClick = { viewModel.playAll() },
                onShuffleClick = { viewModel.shuffleAll() },
                state = songsListState
            )
            1 -> AlbumsTab(
                albums = albums,
                viewMode = albumViewMode,
                onAlbumClick = onNavigateToAlbum,
                gridState = albumGridState,
                listState = albumListState
            )
            2 -> ArtistsTab(
                artists = artists,
                onArtistClick = onNavigateToArtist,
                state = artistsListState
            )
        }
    }
}

@Composable
private fun SongsTab(
    songs: List<Track>,
    isSelectionMode: Boolean,
    selectedTrackIds: Set<Long>,
    onTrackClick: (Track) -> Unit,
    onSelectToggle: (Long) -> Unit,
    onFavoriteClick: (Track) -> Unit,
    onMoreClick: (Track) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    state: LazyListState
) {
    if (songs.isEmpty()) {
        CWEmptyState(
            title = "NO SONGS FOUND",
            message = "Your local music storage doesn't contain any indexed songs yet. Scan your device to discover audio files."
        )
        return
    }

    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Quick Action Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${songs.size} TRACKS",
                    style = CWTypography.TechTelemetry,
                    color = CWColors.TextSecondary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CWPlayAllButton(
                        text = "Play All",
                        onClick = onPlayAllClick
                    )
                    CWPlayAllButton(
                        text = "Shuffle",
                        isShuffle = true,
                        onClick = onShuffleClick
                    )
                }
            }
        }

        itemsIndexed(songs, key = { _, track -> track.id }) { index, track ->
            CWTrackRow(
                track = track,
                onTrackClick = { onTrackClick(track) },
                lineNumber = index + 1,
                isSelectionMode = isSelectionMode,
                isSelected = selectedTrackIds.contains(track.id),
                onSelectToggle = { onSelectToggle(track.id) },
                onLongClick = { onSelectToggle(track.id) },
                onFavoriteClick = { onFavoriteClick(track) },
                onMoreClick = { onMoreClick(track) }
            )
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<Album>,
    viewMode: ViewMode,
    onAlbumClick: (Album) -> Unit,
    gridState: LazyGridState,
    listState: LazyListState
) {
    if (albums.isEmpty()) {
        CWEmptyState(
            title = "NO ALBUMS",
            message = "No albums discovered yet."
        )
        return
    }

    if (viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(albums, key = { "${it.title}_${it.artist}" }) { album ->
                CWAlbumCard(album = album, onClick = { onAlbumClick(album) })
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(albums, key = { "${it.title}_${it.artist}" }) { album ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactileClickable { onAlbumClick(album) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album Art Thumbnail with fallback
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                            .background(CWColors.SurfaceElevated)
                            .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!album.artworkUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = album.artworkUri,
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = null,
                                tint = CWColors.TextTertiary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = album.title,
                            style = CWTypography.AppTypography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CWColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = album.artist,
                                style = CWTypography.AppTypography.bodySmall,
                                color = CWColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = " • ${album.trackCount} trks",
                                style = CWTypography.TechTelemetry,
                                color = CWColors.TextTertiary,
                                maxLines = 1
                            )
                        }
                    }

                    if (album.isHiRes) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CWQualityBadge(text = "HI-RES", isHiRes = true)
                    } else if (album.isLossless) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CWQualityBadge(text = "LOSSLESS", isLossless = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistsTab(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    state: LazyListState
) {
    if (artists.isEmpty()) {
        CWEmptyState(
            title = "NO ARTISTS",
            message = "No artists discovered yet."
        )
        return
    }

    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(artists, key = { it.name }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artist) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = artist.name,
                        style = CWTypography.AppTypography.titleMedium,
                        color = CWColors.TextPrimary
                    )
                    Text(
                        text = "${artist.albumCount} albums • ${artist.trackCount} tracks",
                        style = CWTypography.AppTypography.bodyMedium,
                        color = CWColors.TextSecondary
                    )
                }
                Text(
                    text = ">",
                    style = CWTypography.TechBadge,
                    color = CWColors.TextTertiary
                )
            }
        }
    }
}
