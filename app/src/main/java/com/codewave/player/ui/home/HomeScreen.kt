package com.codewave.player.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codewave.player.core.designsystem.component.CWButton
import com.codewave.player.core.designsystem.component.CWButtonVariant
import com.codewave.player.core.designsystem.component.CWQualityBadge
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.component.CWTrackRow
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.Track

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToLibrary: (initialTab: Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onTrackInspect: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
    ) {
        // Top App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ">",
                        style = CWTypography.TechInspectorHeader,
                        color = CWColors.AccentCyan,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "CODEWAVE",
                        style = CWTypography.AppTypography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = CWColors.TextPrimary
                    )
                }

                Row {
                    IconButton(onClick = { viewModel.scanLibrary() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Library",
                            tint = if (scanProgress.isScanning) CWColors.AccentCyan else CWColors.TextSecondary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = CWColors.TextSecondary
                        )
                    }
                }
            }
        }

        // Scanning Status Bar (PRD Section 98)
        if (scanProgress.isScanning) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                        .background(CWColors.SurfaceElevated)
                        .border(1.dp, CWColors.BorderFocus, RoundedCornerShape(CWShapes.RadiusMedium))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "INDEXING LIBRARY",
                            style = CWTypography.TechBadge,
                            color = CWColors.AccentCyan
                        )
                        Text(
                            text = "${scanProgress.processedCount} / ${scanProgress.totalCount.coerceAtLeast(1)}",
                            style = CWTypography.TechTelemetry
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (scanProgress.totalCount > 0)
                                scanProgress.processedCount.toFloat() / scanProgress.totalCount.toFloat()
                            else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = CWColors.AccentCyan,
                        trackColor = CWColors.SurfaceOverlay
                    )
                    if (scanProgress.currentFile.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = scanProgress.currentFile,
                            style = CWTypography.TechTelemetry,
                            maxLines = 1,
                            color = CWColors.TextSecondary
                        )
                    }
                }
            }
        }

        // Library Status Telemetry Card (PRD Section 9 & 37)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(CWShapes.RadiusMedium),
                colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYSTEM TELEMETRY",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextSecondary
                        )
                        CWTechnicalBadge(
                            text = if (scanProgress.isScanning) "SCANNING" else "LIBRARY READY",
                            textColor = if (scanProgress.isScanning) CWColors.Warning else CWColors.Success
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        TelemetryColumn(value = "${stats.trackCount}", label = "TRACKS")
                        TelemetryColumn(value = "${stats.albumCount}", label = "ALBUMS")
                        TelemetryColumn(value = "${stats.artistCount}", label = "ARTISTS")
                        TelemetryColumn(value = "${stats.hiResCount}", label = "HI-RES", highlight = true)
                    }
                }
            }
        }

        // Quick Navigation Buttons (PRD Section 9)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    title = "Songs",
                    icon = Icons.Default.Audiotrack,
                    onClick = { onNavigateToLibrary(0) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "Albums",
                    icon = Icons.Default.Album,
                    onClick = { onNavigateToLibrary(1) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "Artists",
                    icon = Icons.Default.Person,
                    onClick = { onNavigateToLibrary(2) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Continue Listening Section (PRD Section 9)
        if (recentlyPlayed.isNotEmpty()) {
            val lastTrack = recentlyPlayed.first()
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "CONTINUE LISTENING",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    CWTrackRow(
                        track = lastTrack,
                        onTrackClick = { viewModel.playTrack(lastTrack, recentlyPlayed) },
                        onFavoriteClick = { viewModel.toggleFavorite(lastTrack) },
                        onMoreClick = { onTrackInspect(lastTrack) }
                    )
                }
            }
        }

        // Recently Added Section
        if (recentlyAdded.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENTLY ADDED",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextSecondary
                    )
                    Text(
                        text = "${recentlyAdded.size} TRACKS",
                        style = CWTypography.TechTelemetry
                    )
                }
            }

            items(recentlyAdded.take(15), key = { "added_${it.id}" }) { track ->
                CWTrackRow(
                    track = track,
                    onTrackClick = { viewModel.playTrack(track, recentlyAdded) },
                    onFavoriteClick = { viewModel.toggleFavorite(track) },
                    onMoreClick = { onTrackInspect(track) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp)) // Mini player bottom padding
        }
    }
}

@Composable
private fun TelemetryColumn(
    value: String,
    label: String,
    highlight: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = CWTypography.AppTypography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (highlight) CWColors.AccentCyan else CWColors.TextPrimary
        )
        Text(
            text = label,
            style = CWTypography.TechBadge,
            color = CWColors.TextSecondary
        )
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
            .background(CWColors.SurfacePrimary)
            .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CWColors.AccentCyan,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = title,
                style = CWTypography.AppTypography.titleSmall,
                color = CWColors.TextPrimary
            )
        }
    }
}
