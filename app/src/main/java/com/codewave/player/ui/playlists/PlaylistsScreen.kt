package com.codewave.player.ui.playlists

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codewave.player.core.designsystem.component.CWButton
import com.codewave.player.core.designsystem.component.CWButtonVariant
import com.codewave.player.core.designsystem.component.CWTrackRow
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.Playlist
import com.codewave.player.core.model.Track

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    onTrackInspect: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val favorites by viewModel.favoriteTracks.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLAYLISTS & FAVORITES",
                    style = CWTypography.AppTypography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CWColors.TextPrimary
                )
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Playlist",
                        tint = CWColors.AccentCyan
                    )
                }
            }
        }

        // Favorites Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(CWShapes.RadiusMedium),
                colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                                .background(CWColors.Danger.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = CWColors.Danger,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(
                                text = "Favorites",
                                style = CWTypography.AppTypography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CWColors.TextPrimary
                            )
                            Text(
                                text = "${favorites.size} songs",
                                style = CWTypography.TechTelemetry,
                                color = CWColors.TextSecondary
                            )
                        }
                    }

                    if (favorites.isNotEmpty()) {
                        IconButton(onClick = { viewModel.playFavorites() }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Favorites",
                                tint = CWColors.AccentCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // Playlists List
        if (playlists.isNotEmpty()) {
            item {
                Text(
                    text = "COLLECTIONS",
                    style = CWTypography.TechBadge,
                    color = CWColors.TextSecondary,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
                )
            }

            items(playlists, key = { it.id }) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                        .background(CWColors.SurfaceElevated)
                        .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlaylistPlay,
                            contentDescription = null,
                            tint = CWColors.AccentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = playlist.name,
                                style = CWTypography.AppTypography.titleMedium,
                                color = CWColors.TextPrimary
                            )
                            if (playlist.isSmart) {
                                Text(
                                    text = "SMART QUERY",
                                    style = CWTypography.TechBadge,
                                    color = CWColors.TextTertiary
                                )
                            }
                        }
                    }
                    Text(
                        text = ">",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextTertiary
                    )
                }
            }
        }

        // Favorite Songs preview
        if (favorites.isNotEmpty()) {
            item {
                Text(
                    text = "FAVORITE SONGS",
                    style = CWTypography.TechBadge,
                    color = CWColors.TextSecondary,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
                )
            }

            items(favorites.take(10), key = { "fav_${it.id}" }) { track ->
                CWTrackRow(
                    track = track,
                    onTrackClick = { viewModel.playTrack(track, favorites) },
                    onFavoriteClick = { viewModel.toggleFavorite(track) },
                    onMoreClick = { onTrackInspect(track) }
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", style = CWTypography.TechBadge, color = CWColors.AccentCyan) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createPlaylist(newPlaylistName)
                    newPlaylistName = ""
                    showCreateDialog = false
                }) {
                    Text("Create", color = CWColors.AccentCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = CWColors.TextSecondary)
                }
            },
            containerColor = CWColors.SurfaceElevated
        )
    }
}
