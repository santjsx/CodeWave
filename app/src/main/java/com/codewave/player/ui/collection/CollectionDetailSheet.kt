package com.codewave.player.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codewave.player.core.data.LibraryRepository
import com.codewave.player.core.data.PlaybackRepository
import com.codewave.player.core.designsystem.component.CWButton
import com.codewave.player.core.designsystem.component.CWButtonVariant
import com.codewave.player.core.designsystem.component.CWEmptyState
import com.codewave.player.core.designsystem.component.CWPlayAllButton
import com.codewave.player.core.designsystem.component.CWQualityBadge
import com.codewave.player.core.designsystem.component.CWTrackRow
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.Album
import com.codewave.player.core.model.Artist
import com.codewave.player.core.model.Playlist
import com.codewave.player.core.model.Track

sealed interface CollectionTarget {
    data class AlbumTarget(val album: Album) : CollectionTarget
    data class ArtistTarget(val artist: Artist) : CollectionTarget
    data class PlaylistTarget(val playlist: Playlist) : CollectionTarget
    data object FavoritesTarget : CollectionTarget
}

@Composable
fun CollectionDetailSheet(
    target: CollectionTarget,
    libraryRepository: LibraryRepository,
    playbackRepository: PlaybackRepository,
    onBack: () -> Unit,
    onTrackInspect: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val tracksFlow = when (target) {
        is CollectionTarget.AlbumTarget -> libraryRepository.getTracksByAlbum(target.album.title)
        is CollectionTarget.ArtistTarget -> libraryRepository.getTracksByArtist(target.artist.name)
        is CollectionTarget.PlaylistTarget -> libraryRepository.getTracksForPlaylist(target.playlist.id)
        is CollectionTarget.FavoritesTarget -> libraryRepository.getFavoriteTracks()
    }

    val tracks by tracksFlow.collectAsState(initial = emptyList())

    val (title, subtitle, icon, artworkUri) = when (target) {
        is CollectionTarget.AlbumTarget -> {
            Quadruple(
                target.album.title,
                target.album.artist + (target.album.year?.let { " · $it" } ?: ""),
                Icons.Default.Album,
                target.album.artworkUri
            )
        }
        is CollectionTarget.ArtistTarget -> {
            Quadruple(
                target.artist.name,
                "${target.artist.trackCount} tracks · ${target.artist.albumCount} albums",
                Icons.Default.Person,
                null
            )
        }
        is CollectionTarget.PlaylistTarget -> {
            Quadruple(
                target.playlist.name,
                if (target.playlist.isSmart) "Smart Playlist · ${tracks.size} tracks" else "Custom Playlist · ${tracks.size} tracks",
                Icons.Default.PlaylistPlay,
                null
            )
        }
        is CollectionTarget.FavoritesTarget -> {
            Quadruple(
                "Favorites",
                "${tracks.size} favorite songs",
                Icons.Default.Favorite,
                null
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CWColors.TextPrimary
                )
            }
            Text(
                text = when (target) {
                    is CollectionTarget.AlbumTarget -> "ALBUM"
                    is CollectionTarget.ArtistTarget -> "ARTIST"
                    is CollectionTarget.PlaylistTarget -> "PLAYLIST"
                    is CollectionTarget.FavoritesTarget -> "FAVORITES"
                },
                style = CWTypography.TechBadge,
                color = CWColors.AccentCyan,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header Banner Item
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artwork / Icon Box
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                            .background(CWColors.SurfaceElevated)
                            .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artworkUri != null) {
                            AsyncImage(
                                model = artworkUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (target is CollectionTarget.FavoritesTarget) CWColors.Danger else CWColors.AccentCyan,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = CWTypography.AppTypography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CWColors.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = CWTypography.AppTypography.bodyMedium,
                            color = CWColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Badges (Hi-Res / Lossless if present)
                        val hasHiRes = tracks.any { it.isHiRes }
                        val hasLossless = tracks.any { it.isLossless }
                        if (hasHiRes || hasLossless) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (hasHiRes) {
                                    CWQualityBadge(text = "HI-RES", isHiRes = true)
                                } else if (hasLossless) {
                                    CWQualityBadge(text = "LOSSLESS", isLossless = true)
                                }
                            }
                        }
                    }
                }

                // Action Buttons: Play All & Shuffle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CWPlayAllButton(
                        text = "Play All",
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                playbackRepository.setShuffle(false)
                                playbackRepository.playQueue(tracks, 0)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    CWPlayAllButton(
                        text = "Shuffle",
                        isShuffle = true,
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                playbackRepository.setShuffle(true)
                                playbackRepository.playQueue(tracks.shuffled(), 0)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Divider Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRACKS (${tracks.size})",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextSecondary
                    )
                }
            }

            if (tracks.isEmpty()) {
                item {
                    CWEmptyState(
                        title = "NO TRACKS IN COLLECTION",
                        message = "This collection does not contain any songs.",
                        modifier = Modifier.padding(vertical = 40.dp)
                    )
                }
            } else {
                items(tracks, key = { it.id }) { track ->
                    CWTrackRow(
                        track = track,
                        onTrackClick = {
                            playbackRepository.playTrack(track, tracks)
                        },
                        onFavoriteClick = {
                            playbackRepository.toggleFavorite(track)
                        },
                        onMoreClick = { onTrackInspect(track) }
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
