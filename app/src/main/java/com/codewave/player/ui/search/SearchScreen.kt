package com.codewave.player.ui.search

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewave.player.core.designsystem.component.CWAlbumCard
import com.codewave.player.core.designsystem.component.CWEmptyState
import com.codewave.player.core.designsystem.component.CWQualityBadge
import com.codewave.player.core.designsystem.component.CWTrackRow
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.Album
import com.codewave.player.core.model.Artist
import com.codewave.player.core.model.Track
import kotlinx.coroutines.delay

enum class SearchCategory(val title: String) {
    ALL("All"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists")
}

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onTrackInspect: (Track) -> Unit,
    onTrackOptions: ((Track) -> Unit)? = null,
    onNavigateToAlbum: ((Album) -> Unit)? = null,
    onNavigateToArtist: ((Artist) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val query by viewModel.query.collectAsState()
    val result by viewModel.searchResult.collectAsState()
    var selectedCategory by remember { mutableStateOf(SearchCategory.ALL) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(120)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
            .padding(top = 16.dp)
    ) {
        // Search Input Field with terminal prompt aesthetic
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                .background(CWColors.SurfaceElevated)
                .border(
                    1.dp,
                    if (query.isNotEmpty()) CWColors.BorderFocus else CWColors.BorderSubtle,
                    RoundedCornerShape(CWShapes.RadiusMedium)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ">",
                    style = CWTypography.TechInspectorHeader,
                    color = CWColors.AccentCyan,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search tracks, artists, albums...",
                            style = CWTypography.AppTypography.bodyLarge,
                            color = CWColors.TextTertiary
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        textStyle = CWTypography.AppTypography.bodyLarge.copy(color = CWColors.TextPrimary),
                        cursorBrush = SolidColor(CWColors.AccentCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearQuery() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = CWColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Category Filter Chips (All, Songs, Albums, Artists)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category
                val countBadge = if (query.isNotEmpty()) {
                    when (category) {
                        SearchCategory.ALL -> " (${result.tracks.size + result.albums.size + result.artists.size})"
                        SearchCategory.SONGS -> " (${result.tracks.size})"
                        SearchCategory.ALBUMS -> " (${result.albums.size})"
                        SearchCategory.ARTISTS -> " (${result.artists.size})"
                    }
                } else ""

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CWShapes.RadiusFull))
                        .background(if (isSelected) CWColors.AccentCyan else CWColors.SurfaceElevated)
                        .border(
                            1.dp,
                            if (isSelected) CWColors.AccentCyan else CWColors.BorderSubtle,
                            RoundedCornerShape(CWShapes.RadiusFull)
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${category.title.uppercase()}$countBadge",
                        style = CWTypography.TechBadge,
                        color = if (isSelected) CWColors.Background else CWColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val totalMatches = result.tracks.size + result.albums.size + result.artists.size

        if (query.isEmpty()) {
            CWEmptyState(
                title = "SEARCH LIBRARY",
                message = "Instant Unicode-aware search across all indexed audio files, albums, and artists.",
                icon = Icons.Default.Search
            )
        } else if (totalMatches == 0) {
            CWEmptyState(
                title = "NO MATCHES",
                message = "No tracks, albums, or artists matched '$query'. Try another search term."
            )
        } else {
            when (selectedCategory) {
                SearchCategory.ALL -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        // 1. Matching Songs Preview
                        if (result.tracks.isNotEmpty()) {
                            item {
                                SearchSectionHeader(
                                    title = "SONGS",
                                    count = result.tracks.size,
                                    onSeeAll = if (result.tracks.size > 4) {
                                        { selectedCategory = SearchCategory.SONGS }
                                    } else null
                                )
                            }
                            items(result.tracks.take(4), key = { "all_track_${it.id}" }) { track ->
                                CWTrackRow(
                                    track = track,
                                    onTrackClick = { viewModel.playTrack(track) },
                                    onFavoriteClick = { viewModel.toggleFavorite(track) },
                                    onMoreClick = { onTrackOptions?.invoke(track) ?: onTrackInspect(track) }
                                )
                            }
                        }

                        // 2. Matching Albums Preview
                        if (result.albums.isNotEmpty()) {
                            item {
                                SearchSectionHeader(
                                    title = "ALBUMS",
                                    count = result.albums.size,
                                    onSeeAll = if (result.albums.size > 3) {
                                        { selectedCategory = SearchCategory.ALBUMS }
                                    } else null
                                )
                            }
                            items(result.albums.take(3), key = { "all_album_${it.id}" }) { album ->
                                SearchAlbumRow(
                                    album = album,
                                    onClick = { onNavigateToAlbum?.invoke(album) }
                                )
                            }
                        }

                        // 3. Matching Artists Preview
                        if (result.artists.isNotEmpty()) {
                            item {
                                SearchSectionHeader(
                                    title = "ARTISTS",
                                    count = result.artists.size,
                                    onSeeAll = if (result.artists.size > 3) {
                                        { selectedCategory = SearchCategory.ARTISTS }
                                    } else null
                                )
                            }
                            items(result.artists.take(3), key = { "all_artist_${it.id}" }) { artist ->
                                SearchArtistRow(
                                    artist = artist,
                                    onClick = { onNavigateToArtist?.invoke(artist) }
                                )
                            }
                        }
                    }
                }

                SearchCategory.SONGS -> {
                    if (result.tracks.isEmpty()) {
                        CWEmptyState(
                            title = "NO MATCHING SONGS",
                            message = "No songs matched '$query'."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            item {
                                SearchSectionHeader(
                                    title = "ALL MATCHING SONGS",
                                    count = result.tracks.size
                                )
                            }
                            items(result.tracks, key = { "songs_${it.id}" }) { track ->
                                CWTrackRow(
                                    track = track,
                                    onTrackClick = { viewModel.playTrack(track) },
                                    onFavoriteClick = { viewModel.toggleFavorite(track) },
                                    onMoreClick = { onTrackOptions?.invoke(track) ?: onTrackInspect(track) }
                                )
                            }
                        }
                    }
                }

                SearchCategory.ALBUMS -> {
                    if (result.albums.isEmpty()) {
                        CWEmptyState(
                            title = "NO MATCHING ALBUMS",
                            message = "No albums matched '$query'."
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 100.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(result.albums, key = { "album_${it.id}" }) { album ->
                                CWAlbumCard(
                                    album = album,
                                    onClick = { onNavigateToAlbum?.invoke(album) }
                                )
                            }
                        }
                    }
                }

                SearchCategory.ARTISTS -> {
                    if (result.artists.isEmpty()) {
                        CWEmptyState(
                            title = "NO MATCHING ARTISTS",
                            message = "No artists matched '$query'."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            item {
                                SearchSectionHeader(
                                    title = "ALL MATCHING ARTISTS",
                                    count = result.artists.size
                                )
                            }
                            items(result.artists, key = { "artists_${it.id}" }) { artist ->
                                SearchArtistRow(
                                    artist = artist,
                                    onClick = { onNavigateToArtist?.invoke(artist) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    count: Int,
    onSeeAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title ($count)",
            style = CWTypography.TechBadge,
            color = CWColors.TextSecondary
        )
        if (onSeeAll != null) {
            Text(
                text = "SEE ALL",
                style = CWTypography.TechBadge,
                color = CWColors.AccentCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SearchAlbumRow(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                .background(CWColors.SurfaceElevated)
                .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusSmall)),
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
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = CWTypography.AppTypography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = CWColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${album.artist} · ${album.trackCount} tracks" + (album.year?.let { " · $it" } ?: ""),
                style = CWTypography.AppTypography.bodySmall,
                color = CWColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

@Composable
private fun SearchArtistRow(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(CWColors.SurfaceElevated)
                .border(1.dp, CWColors.BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!artist.artworkUri.isNullOrEmpty()) {
                AsyncImage(
                    model = artist.artworkUri,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = CWColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = CWTypography.AppTypography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = CWColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${artist.trackCount} tracks · ${artist.albumCount} albums",
                style = CWTypography.TechTelemetry,
                color = CWColors.TextSecondary,
                fontSize = 11.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "View Artist",
            tint = CWColors.TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}
