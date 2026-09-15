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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.activity.compose.BackHandler
import com.codewave.player.ui.collection.CollectionDetailSheet
import com.codewave.player.ui.collection.CollectionTarget
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codewave.player.core.designsystem.component.CWButton
import com.codewave.player.core.designsystem.component.CWButtonVariant
import com.codewave.player.core.designsystem.component.tactileClickable
import com.codewave.player.core.designsystem.component.tactilePress
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
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var renameText by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var selectedTarget by remember { mutableStateOf<CollectionTarget?>(null) }

    val currentTarget = selectedTarget
    if (currentTarget != null) {
        BackHandler { selectedTarget = null }
        CollectionDetailSheet(
            target = currentTarget,
            libraryRepository = viewModel.libraryRepository,
            playbackRepository = viewModel.playbackRepository,
            onBack = { selectedTarget = null },
            onTrackInspect = onTrackInspect,
            modifier = modifier
        )
        return
    }

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
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .tactileClickable { selectedTarget = CollectionTarget.FavoritesTarget },
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
                                text = "${favorites.size} songs • Tap to view all",
                                style = CWTypography.TechTelemetry,
                                color = CWColors.TextSecondary
                            )
                        }
                    }

                    if (favorites.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.playFavorites() },
                            modifier = Modifier.tactilePress()
                        ) {
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
        item {
            Text(
                text = "COLLECTIONS",
                style = CWTypography.TechBadge,
                color = CWColors.TextSecondary,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
            )
        }

        if (playlists.isNotEmpty()) {
            items(playlists, key = { it.id }) { playlist ->
                var menuExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                        .background(CWColors.SurfaceElevated)
                        .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                        .tactileClickable { selectedTarget = CollectionTarget.PlaylistTarget(playlist) }
                        .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.tactilePress()
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Playlist Options",
                                tint = CWColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(CWColors.SurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename", color = CWColors.TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = CWColors.AccentCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    renameText = playlist.name
                                    playlistToRename = playlist
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = CWColors.Danger) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = CWColors.Danger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    playlistToDelete = playlist
                                }
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                        .background(CWColors.SurfacePrimary)
                        .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO CUSTOM PLAYLISTS",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextTertiary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap '+' to create your first curated playlist.",
                            style = CWTypography.AppTypography.bodySmall,
                            color = CWColors.TextSecondary
                        )
                    }
                }
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
                TextButton(
                    onClick = {
                        viewModel.createPlaylist(newPlaylistName)
                        newPlaylistName = ""
                        showCreateDialog = false
                    },
                    modifier = Modifier.tactilePress()
                ) {
                    Text("Create", color = CWColors.AccentCyan)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDialog = false },
                    modifier = Modifier.tactilePress()
                ) {
                    Text("Cancel", color = CWColors.TextSecondary)
                }
            },
            containerColor = CWColors.SurfaceElevated,
            shape = RoundedCornerShape(CWShapes.RadiusLarge)
        )
    }

    playlistToRename?.let { targetPlaylist ->
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            title = { Text("Rename Playlist", style = CWTypography.TechBadge, color = CWColors.AccentCyan) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renamePlaylist(targetPlaylist, renameText)
                        playlistToRename = null
                    },
                    modifier = Modifier.tactilePress()
                ) {
                    Text("Save", color = CWColors.AccentCyan)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { playlistToRename = null },
                    modifier = Modifier.tactilePress()
                ) {
                    Text("Cancel", color = CWColors.TextSecondary)
                }
            },
            containerColor = CWColors.SurfaceElevated,
            shape = RoundedCornerShape(CWShapes.RadiusLarge)
        )
    }

    playlistToDelete?.let { targetPlaylist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Delete Playlist?", style = CWTypography.TechBadge, color = CWColors.Danger) },
            text = {
                Text(
                    text = "Are you sure you want to delete '${targetPlaylist.name}'? This playlist and its track associations will be removed.",
                    style = CWTypography.AppTypography.bodyMedium,
                    color = CWColors.TextPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(targetPlaylist)
                        playlistToDelete = null
                    },
                    modifier = Modifier.tactilePress()
                ) {
                    Text("Delete", color = CWColors.Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { playlistToDelete = null },
                    modifier = Modifier.tactilePress()
                ) {
                    Text("Cancel", color = CWColors.TextSecondary)
                }
            },
            containerColor = CWColors.SurfaceElevated,
            shape = RoundedCornerShape(CWShapes.RadiusLarge)
        )
    }
}
