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
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
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

        Spacer(modifier = Modifier.height(14.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        // Error message banner if any
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

        // Search Results or Explore Charts
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
            // Explore / Trending Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = CWColors.AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TRENDING & EXPLORE",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextSecondary
                    )
                }

                IconButton(
                    onClick = { viewModel.loadExploreCharts() },
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

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoadingCharts) {
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
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(uiState.exploreCharts, key = { it.id }) { track ->
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
        }
    }
}

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
