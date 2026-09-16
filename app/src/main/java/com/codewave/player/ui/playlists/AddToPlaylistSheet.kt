package com.codewave.player.ui.playlists

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.data.LibraryRepository
import com.codewave.player.core.designsystem.component.tactileClickable
import com.codewave.player.core.designsystem.component.tactilePress
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.Playlist
import com.codewave.player.core.model.Track
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    track: Track,
    libraryRepository: LibraryRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playlists by libraryRepository.getAllPlaylists().collectAsState(initial = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val customPlaylists = remember(playlists) {
        playlists.filter { !it.isSmart }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CWColors.SurfaceElevated,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ADD TO PLAYLIST",
                        style = CWTypography.TechBadge,
                        color = CWColors.AccentCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = track.title,
                        style = CWTypography.AppTypography.bodyMedium,
                        color = CWColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CWColors.AccentCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = null,
                        tint = CWColors.AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Option to Create New Playlist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                    .background(CWColors.SurfacePrimary)
                    .border(1.dp, CWColors.AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(CWShapes.RadiusMedium))
                    .tactileClickable { showCreateDialog = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CWColors.AccentCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Playlist",
                        tint = CWColors.Background,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Create New Playlist",
                        style = CWTypography.AppTypography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CWColors.TextPrimary
                    )
                    Text(
                        text = "Save and add '${track.title.take(18)}' directly",
                        style = CWTypography.AppTypography.bodySmall,
                        color = CWColors.TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "EXISTING PLAYLISTS",
                style = CWTypography.TechBadge,
                color = CWColors.TextTertiary,
                fontSize = 9.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )

            // Playlists List
            if (customPlaylists.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((customPlaylists.size * 58).coerceAtMost(320).dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(customPlaylists, key = { it.id }) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                                .background(CWColors.SurfacePrimary)
                                .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                                .tactileClickable {
                                    coroutineScope.launch {
                                        libraryRepository.addTrackToPlaylist(playlist.id, track.id)
                                        Toast.makeText(context, "Added to '${playlist.name}'", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    tint = CWColors.AccentCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = playlist.name,
                                        style = CWTypography.AppTypography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CWColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${playlist.trackCount} songs",
                                        style = CWTypography.TechTelemetry,
                                        color = CWColors.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = CWColors.TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No custom playlists found. Create one above!",
                        style = CWTypography.AppTypography.bodySmall,
                        color = CWColors.TextTertiary
                    )
                }
            }
        }
    }

    // Quick Dialog: Create Playlist and Add Track
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "New Playlist",
                    style = CWTypography.TechBadge,
                    color = CWColors.AccentCyan
                )
            },
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
                        val name = newPlaylistName.trim()
                        if (name.isNotBlank()) {
                            coroutineScope.launch {
                                val newId = libraryRepository.createPlaylist(name)
                                libraryRepository.addTrackToPlaylist(newId, track.id)
                                Toast.makeText(context, "Created '$name' & added track", Toast.LENGTH_SHORT).show()
                                showCreateDialog = false
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.tactilePress()
                ) {
                    Text("Create & Add", color = CWColors.AccentCyan, fontWeight = FontWeight.Bold)
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
}
