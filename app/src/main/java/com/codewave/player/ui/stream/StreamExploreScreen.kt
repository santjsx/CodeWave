package com.codewave.player.ui.stream

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.ExploreSection
import com.codewave.player.core.model.StreamTrack
import com.codewave.player.core.model.TargetAudioFormat

@Composable
fun StreamExploreScreen(
    viewModel: StreamViewModel,
    onNavigateToDownloads: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ONLINE STREAM",
                    style = CWTypography.AppTypography.headlineSmall,
                    color = CWColors.AccentCyan
                )
                Text(
                    text = "InnerTube 160k Opus · Float32 DSP",
                    style = CWTypography.AppTypography.labelSmall,
                    color = CWColors.TextSecondary
                )
            }

            IconButton(
                onClick = onNavigateToDownloads,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CWColors.SurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Downloads",
                    tint = CWColors.AccentCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar with Cyan Obsidian Styling
        TextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                .border(1.dp, CWColors.BorderFocus, RoundedCornerShape(CWShapes.RadiusMedium)),
            placeholder = {
                Text(
                    text = "Search YouTube Music & Artists...",
                    style = CWTypography.AppTypography.bodyMedium,
                    color = CWColors.TextTertiary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = CWColors.AccentCyan
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = CWColors.TextSecondary
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CWColors.SurfaceElevated,
                unfocusedContainerColor = CWColors.SurfacePrimary,
                focusedTextColor = CWColors.TextPrimary,
                unfocusedTextColor = CWColors.TextPrimary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Spotify-style Genre Filter Pills Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(uiState.availableGenreFilters) { genre ->
                val isSelected = genre.equals(uiState.selectedGenreFilter, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) CWColors.AccentCyan else CWColors.SurfaceElevated)
                        .border(
                            1.dp,
                            if (isSelected) CWColors.AccentCyan else CWColors.BorderSubtle,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.selectGenreFilter(genre) }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = genre,
                        style = CWTypography.AppTypography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) CWColors.Background else CWColors.TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Error banner if any
        uiState.errorMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CWColors.Danger.copy(alpha = 0.15f))
                    .border(1.dp, CWColors.Danger.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = msg,
                    style = CWTypography.AppTypography.labelSmall,
                    color = CWColors.Danger
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Search Results or Spotify Explore Feeds
        if (uiState.isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CWColors.AccentCyan,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (uiState.searchQuery.isNotBlank()) {
            if (uiState.searchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No online stream results found for '${uiState.searchQuery}'",
                        style = CWTypography.AppTypography.bodyMedium,
                        color = CWColors.TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Text(
                            text = "RESULTS (${uiState.searchResults.size})",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextTertiary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(uiState.searchResults, key = { it.id }) { track ->
                        StreamTrackRow(
                            track = track,
                            isResolving = uiState.activeResolvingTrackId == track.id,
                            onPlayClick = { viewModel.playStreamTrack(track) },
                            onDownloadClick = {
                                viewModel.downloadTrack(track, TargetAudioFormat.FLAC)
                                Toast.makeText(context, "Queued FLAC download: ${track.title}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        } else {
            // Spotify-Inspired Explore Experience
            if (uiState.isLoadingCharts && uiState.exploreSections.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = CWColors.AccentCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. Spotify 2-Column Quick Picks Grid (Top 6 Tracks)
                    val quickPicks = uiState.exploreCharts.take(6)
                    if (quickPicks.isNotEmpty()) {
                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Quick Picks",
                                        style = CWTypography.AppTypography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CWColors.TextPrimary
                                    )
                                    IconButton(
                                        onClick = { viewModel.loadExploreSections() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh",
                                            tint = CWColors.TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 2-Column x 3-Row Grid
                                val chunked = quickPicks.chunked(2)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    chunked.forEach { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            pair.forEach { track ->
                                                SpotifyQuickPickCard(
                                                    track = track,
                                                    isResolving = uiState.activeResolvingTrackId == track.id,
                                                    onPlayClick = { viewModel.playStreamTrack(track) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (pair.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Spotify Horizontal Carousels for Each Section
                    items(uiState.exploreSections, key = { it.id }) { section ->
                        ExploreSectionCarousel(
                            section = section,
                            activeResolvingTrackId = uiState.activeResolvingTrackId,
                            onPlayTrack = { viewModel.playStreamTrack(it) },
                            onDownloadTrack = {
                                viewModel.downloadTrack(it, TargetAudioFormat.FLAC)
                                Toast.makeText(context, "Queued FLAC download: ${it.title}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Spotify-Style 2-Column Compact Quick Pick Card
 */
@Composable
private fun SpotifyQuickPickCard(
    track: StreamTrack,
    isResolving: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CWColors.SurfaceElevated)
            .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onPlayClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(CWColors.SurfacePrimary),
            contentAlignment = Alignment.Center
        ) {
            if (!track.artworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = track.artworkUri,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "♪",
                    style = CWTypography.TechBadge,
                    color = CWColors.AccentCyan
                )
            }

            if (isResolving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = CWColors.AccentCyan,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = track.title,
            style = CWTypography.AppTypography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = CWColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
    }
}

/**
 * Spotify-Style Horizontal Carousel Section with Title, Subtitle, and Cards
 */
@Composable
private fun ExploreSectionCarousel(
    section: ExploreSection,
    activeResolvingTrackId: String?,
    onPlayTrack: (StreamTrack) -> Unit,
    onDownloadTrack: (StreamTrack) -> Unit
) {
    Column {
        Text(
            text = section.title,
            style = CWTypography.AppTypography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CWColors.TextPrimary
        )

        if (!section.subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = section.subtitle,
                style = CWTypography.AppTypography.labelSmall,
                color = CWColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(section.tracks, key = { "${section.id}_${it.id}" }) { track ->
                SpotifyCard(
                    track = track,
                    isResolving = activeResolvingTrackId == track.id,
                    onPlayClick = { onPlayTrack(track) },
                    onDownloadClick = { onDownloadTrack(track) }
                )
            }
        }
    }
}

/**
 * Spotify Square Music Card with Cover Artwork, Badges, and Quick Play/Download Actions
 */
@Composable
private fun SpotifyCard(
    track: StreamTrack,
    isResolving: Boolean,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(142.dp)
            .clickable(onClick = onPlayClick)
    ) {
        Box(
            modifier = Modifier
                .size(142.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CWColors.SurfacePrimary)
                .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(12.dp))
        ) {
            if (!track.artworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = track.artworkUri,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♪",
                        fontSize = 32.sp,
                        color = CWColors.AccentCyan
                    )
                }
            }

            // Subtle gradient scrim at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                            startY = 60f
                        )
                    )
            )

            // Play Button Indicator overlay at bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CWColors.AccentCyan)
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                if (isResolving) {
                    CircularProgressIndicator(
                        color = CWColors.Background,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = CWColors.Background,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Download Icon Button at bottom-left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(onClick = onDownloadClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download FLAC",
                    tint = CWColors.Success,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = track.title,
            style = CWTypography.AppTypography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = CWColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle / Artist
        Text(
            text = track.artist,
            style = CWTypography.AppTypography.labelSmall,
            color = CWColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Clean Single Track Row for Search Results
 */
@Composable
private fun StreamTrackRow(
    track: StreamTrack,
    isResolving: Boolean,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
            .background(CWColors.SurfacePrimary)
            .clickable(onClick = onPlayClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(CWColors.SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (!track.artworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = track.artworkUri,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "YT",
                    style = CWTypography.TechBadge,
                    color = CWColors.AccentCyan
                )
            }

            if (isResolving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = CWColors.AccentCyan,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = CWTypography.AppTypography.bodyMedium,
                color = CWColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.artist,
                    style = CWTypography.AppTypography.labelSmall,
                    color = CWColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Bitrate Tech Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(CWColors.AccentCyan.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "OPUS 160K",
                        style = CWTypography.TechBadge,
                        color = CWColors.AccentCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Action: 1-Click Stream Play
        IconButton(
            onClick = onPlayClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Stream",
                tint = CWColors.AccentCyan,
                modifier = Modifier.size(22.dp)
            )
        }

        // Action: 1-Click Lossless Download
        IconButton(
            onClick = onDownloadClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download FLAC",
                tint = CWColors.Success,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
