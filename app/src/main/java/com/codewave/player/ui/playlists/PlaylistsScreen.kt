package com.codewave.player.ui.playlists

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewave.player.core.designsystem.component.tactileClickable
import com.codewave.player.core.designsystem.component.tactilePress
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.Playlist
import com.codewave.player.core.model.Track
import com.codewave.player.ui.collection.CollectionDetailSheet
import com.codewave.player.ui.collection.CollectionTarget
import kotlin.math.abs

private enum class PlaylistCategory(val title: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    CUSTOM("Custom"),
    SMART("Smart Query")
}

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    onTrackInspect: (Track) -> Unit,
    onTrackOptions: ((Track) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val favorites by viewModel.favoriteTracks.collectAsState()
    val playlistCovers by viewModel.playlistCovers.collectAsState()

    var selectedCategory by remember { mutableStateOf(PlaylistCategory.ALL) }
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
            onTrackOptions = onTrackOptions,
            modifier = modifier
        )
        return
    }

    // Filter items based on selected category
    val showFavorites = selectedCategory == PlaylistCategory.ALL || selectedCategory == PlaylistCategory.FAVORITES
    val filteredPlaylists = when (selectedCategory) {
        PlaylistCategory.ALL -> playlists
        PlaylistCategory.FAVORITES -> emptyList()
        PlaylistCategory.CUSTOM -> playlists.filter { !it.isSmart }
        PlaylistCategory.SMART -> playlists.filter { it.isSmart }
    }

    val favoriteCoverUris = remember(favorites) {
        favorites.mapNotNull { it.albumArtUri }.distinct().take(3)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
    ) {
        // 1. Header (Title + Add Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Playlists",
                    style = CWTypography.AppTypography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CWColors.TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "${playlists.size + if (favorites.isNotEmpty()) 1 else 0} Collections in Library",
                    style = CWTypography.TechTelemetry,
                    color = CWColors.TextTertiary,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CWColors.SurfaceElevated)
                    .tactilePress()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Playlist",
                    tint = CWColors.AccentCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 2. Category Filter Chips (Inspired by "My Books" All, Romance, Fantasy...)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CWShapes.RadiusFull))
                        .background(
                            if (isSelected) CWColors.TextPrimary else CWColors.SurfaceElevated
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CWColors.TextPrimary else CWColors.BorderSubtle,
                            shape = RoundedCornerShape(CWShapes.RadiusFull)
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.title,
                        style = CWTypography.AppTypography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) CWColors.Background else CWColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. 2-Column Physical Pocket Grid (The "My Books" Shelf Aesthetic)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Favorites Card (Rendered as physical shelf pocket card)
            if (showFavorites) {
                item(key = "collection_favorites") {
                    CollectionPocketCard(
                        title = "Favorites",
                        subtitle = "Favorites · ${favorites.size} songs",
                        coverUris = favoriteCoverUris,
                        isFavorite = true,
                        isSmart = false,
                        paletteSeed = 9999,
                        onClick = { selectedTarget = CollectionTarget.FavoritesTarget },
                        onOptionsClick = null
                    )
                }
            }

            // Playlist Items
            items(filteredPlaylists, key = { it.id }) { playlist ->
                val covers = playlistCovers[playlist.id] ?: emptyList()
                CollectionPocketCard(
                    title = playlist.name,
                    subtitle = if (playlist.isSmart) "Smart · ${playlist.trackCount} songs" else "Custom · ${playlist.trackCount} songs",
                    coverUris = covers,
                    isFavorite = false,
                    isSmart = playlist.isSmart,
                    paletteSeed = playlist.name.hashCode(),
                    onClick = { selectedTarget = CollectionTarget.PlaylistTarget(playlist) },
                    onOptionsClick = {
                        playlistToRename = null
                        playlistToDelete = null
                        // Trigger menu options
                        renameText = playlist.name
                        playlistToRename = playlist
                    },
                    onDeleteClick = {
                        playlistToDelete = playlist
                    }
                )
            }

            // Empty state if filtered results are empty
            if (!showFavorites && filteredPlaylists.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "NO COLLECTIONS IN THIS VIEW",
                                style = CWTypography.TechBadge,
                                color = CWColors.TextTertiary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap '+' to create your first playlist.",
                                style = CWTypography.AppTypography.bodySmall,
                                color = CWColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog: Create Playlist
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

    // Dialog: Rename Playlist
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

    // Dialog: Delete Playlist
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

/**
 * Replicates the "My Books" physical display shelf aesthetic:
 * - 3 fanned-out album/book sleeves emerging from behind a pocket
 *   (Left: -13° tilt, Center: upright, Right: +13° tilt)
 * - Lower half covered by a frosted acrylic pocket
 * - 4 metallic corner pins/screws/rivets on the pocket corners
 * - Title and track metadata cleanly formatted below
 */
@Composable
private fun CollectionPocketCard(
    title: String,
    subtitle: String,
    coverUris: List<String>,
    isFavorite: Boolean,
    isSmart: Boolean,
    paletteSeed: Int,
    onClick: () -> Unit,
    onOptionsClick: (() -> Unit)?,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .tactileClickable(onClick = onClick)
    ) {
        // Physical pocket & fanned sleeves container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // --- BACK LAYER: 3 FAN-SHAPED ALBUM SLEEVES ---
            val safeGradients = remember(paletteSeed) {
                getAestheticBookGradients(paletteSeed, isFavorite)
            }

            // Left Sleeve (-13° tilt)
            AlbumSleeve(
                coverUri = coverUris.getOrNull(0),
                gradient = safeGradients[0],
                title = title,
                rotationZ = -13f,
                offsetX = (-20).dp,
                offsetY = (-14).dp,
                modifier = Modifier.align(Alignment.Center)
            )

            // Right Sleeve (+13° tilt)
            AlbumSleeve(
                coverUri = coverUris.getOrNull(2) ?: coverUris.getOrNull(0),
                gradient = safeGradients[2],
                title = title,
                rotationZ = 13f,
                offsetX = 20.dp,
                offsetY = (-14).dp,
                modifier = Modifier.align(Alignment.Center)
            )

            // Center Sleeve (Upright 0°, drawn in front of left and right)
            AlbumSleeve(
                coverUri = coverUris.getOrNull(1) ?: coverUris.getOrNull(0),
                gradient = safeGradients[1],
                title = title,
                rotationZ = 0f,
                offsetX = 0.dp,
                offsetY = (-18).dp,
                isCenter = true,
                modifier = Modifier.align(Alignment.Center)
            )

            // --- FRONT LAYER: FROSTED ACRYLIC POCKET WITH 4 METALLIC PINS ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                    .background(CWColors.SurfaceElevated.copy(alpha = 0.68f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(CWShapes.RadiusMedium)
                    )
                    .padding(6.dp)
            ) {
                // Pin 1: Top-Left
                MetallicRivet(modifier = Modifier.align(Alignment.TopStart))

                // Pin 2: Top-Right
                MetallicRivet(modifier = Modifier.align(Alignment.TopEnd))

                // Pin 3: Bottom-Left
                MetallicRivet(modifier = Modifier.align(Alignment.BottomStart))

                // Pin 4: Bottom-Right
                MetallicRivet(modifier = Modifier.align(Alignment.BottomEnd))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- BOTTOM METADATA (Title & Subtitle) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = CWTypography.AppTypography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CWColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = CWTypography.TechTelemetry,
                    color = CWColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onOptionsClick != null || onDeleteClick != null) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = CWColors.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(CWColors.SurfaceElevated)
                    ) {
                        if (onOptionsClick != null) {
                            DropdownMenuItem(
                                text = { Text("Rename", color = CWColors.TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = CWColors.AccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOptionsClick()
                                }
                            )
                        }
                        if (onDeleteClick != null) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = CWColors.Danger) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = CWColors.Danger,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual album/book sleeve designed with soft borders, drop shadows,
 * and high-fidelity artwork or rich fallback gradient jacket.
 */
@Composable
private fun AlbumSleeve(
    coverUri: String?,
    gradient: Brush,
    title: String,
    rotationZ: Float,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    isCenter: Boolean = false,
    modifier: Modifier = Modifier
) {
    val width = if (isCenter) 82.dp else 78.dp
    val height = if (isCenter) 112.dp else 106.dp

    Box(
        modifier = modifier
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer {
                this.rotationZ = rotationZ
                cameraDistance = 12f * density
            }
            .size(width = width, height = height)
            .shadow(
                elevation = if (isCenter) 8.dp else 4.dp,
                shape = RoundedCornerShape(CWShapes.RadiusSmall),
                clip = false
            )
            .clip(RoundedCornerShape(CWShapes.RadiusSmall))
            .background(gradient)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.22f),
                shape = RoundedCornerShape(CWShapes.RadiusSmall)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!coverUri.isNullOrBlank()) {
            AsyncImage(
                model = coverUri,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant stylized book/vinyl jacket placeholder
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title.take(8).uppercase(),
                    style = CWTypography.TechBadge,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 7.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 3D Metallic Rivet/Pin: Recreates the physical silver standoff pins
 * securing the acrylic pocket corners in the "My Books" reference.
 */
@Composable
private fun MetallicRivet(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFA6AFB8),
                        Color(0xFF4A525D)
                    )
                )
            )
            .border(0.5.dp, Color(0xFF282E36), CircleShape)
    ) {
        // Specular dot
        Box(
            modifier = Modifier
                .size(1.5.dp)
                .offset(x = 1.dp, y = 1.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.95f))
        )
    }
}

/**
 * Generates rich, curated book/vinyl aesthetic gradients so fallback
 * sleeves always look like luxury book covers from the reference.
 */
private fun getAestheticBookGradients(seed: Int, isFavorite: Boolean): List<Brush> {
    if (isFavorite) {
        return listOf(
            Brush.verticalGradient(listOf(Color(0xFF3B0B14), Color(0xFF7A1C2C))),
            Brush.verticalGradient(listOf(Color(0xFF991B33), Color(0xFFE53935))),
            Brush.verticalGradient(listOf(Color(0xFF4A0E1A), Color(0xFF2A080F)))
        )
    }

    val paletteList = listOf(
        listOf(
            Brush.verticalGradient(listOf(Color(0xFF132F38), Color(0xFF1E5B6E))),
            Brush.verticalGradient(listOf(Color(0xFF1D5A6D), Color(0xFF00E5FF))),
            Brush.verticalGradient(listOf(Color(0xFF0C242C), Color(0xFF153F4C)))
        ),
        listOf(
            Brush.verticalGradient(listOf(Color(0xFF2B1B3D), Color(0xFF55327B))),
            Brush.verticalGradient(listOf(Color(0xFF6B3E9C), Color(0xFFB388FF))),
            Brush.verticalGradient(listOf(Color(0xFF1D122A), Color(0xFF3C2357)))
        ),
        listOf(
            Brush.verticalGradient(listOf(Color(0xFF142B20), Color(0xFF255B42))),
            Brush.verticalGradient(listOf(Color(0xFF2E6F52), Color(0xFF00E676))),
            Brush.verticalGradient(listOf(Color(0xFF0D1E16), Color(0xFF193D2C)))
        ),
        listOf(
            Brush.verticalGradient(listOf(Color(0xFF3D2614), Color(0xFF754722))),
            Brush.verticalGradient(listOf(Color(0xFF9E5F2E), Color(0xFFFF9100))),
            Brush.verticalGradient(listOf(Color(0xFF29190C), Color(0xFF4F3017)))
        )
    )

    val index = abs(seed) % paletteList.size
    return paletteList[index]
}
