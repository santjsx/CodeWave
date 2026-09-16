package com.codewave.player.ui.collection

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewave.player.core.designsystem.component.CWQualityBadge
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.component.tactileClickable
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionMenuSheet(
    track: Track,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onInspectTrack: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CWColors.SurfaceElevated,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            // Track Header Preview Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                    .background(CWColors.SurfacePrimary)
                    .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art Thumbnail
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                        .background(CWColors.SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (!track.albumArtUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = track.albumArtUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "CW",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextTechnical
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = CWTypography.AppTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CWColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.artist} · ${track.album}",
                        style = CWTypography.AppTypography.bodyMedium,
                        color = CWColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (track.isHiRes) {
                            CWQualityBadge(text = "HI-RES", isHiRes = true)
                        } else if (track.isLossless) {
                            CWQualityBadge(text = "LOSSLESS", isLossless = true)
                        } else {
                            CWQualityBadge(text = track.format.displayName)
                        }
                        CWTechnicalBadge(text = track.technicalSummary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Items
            ActionMenuItem(
                title = "Play Next",
                subtitle = "Insert immediately after the current song",
                icon = Icons.Default.PlaylistPlay,
                iconTint = CWColors.AccentCyan,
                onClick = {
                    onPlayNext()
                    onDismiss()
                }
            )

            ActionMenuItem(
                title = "Add to Queue",
                subtitle = "Append to the end of the playback queue",
                icon = Icons.Default.QueueMusic,
                iconTint = CWColors.AccentCyan,
                onClick = {
                    onAddToQueue()
                    onDismiss()
                }
            )

            ActionMenuItem(
                title = "Add to Playlist",
                subtitle = "Save to custom or new playlist",
                icon = Icons.Default.PlaylistAdd,
                iconTint = CWColors.AccentCyan,
                onClick = {
                    onDismiss()
                    onAddToPlaylist()
                }
            )

            ActionMenuItem(
                title = "Go to Album",
                subtitle = track.album,
                icon = Icons.Default.Album,
                iconTint = CWColors.AccentCyan,
                onClick = {
                    onDismiss()
                    onGoToAlbum()
                }
            )

            ActionMenuItem(
                title = "Go to Artist",
                subtitle = track.artist,
                icon = Icons.Default.Person,
                iconTint = CWColors.AccentCyan,
                onClick = {
                    onDismiss()
                    onGoToArtist()
                }
            )

            ActionMenuItem(
                title = "Audio Specs & Telemetry",
                subtitle = "View bit-depth, sample rate & DSP pipeline",
                icon = Icons.Default.Info,
                iconTint = CWColors.TextSecondary,
                onClick = {
                    onDismiss()
                    onInspectTrack()
                }
            )
        }
    }
}

@Composable
private fun ActionMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
            .tactileClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = CWTypography.AppTypography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = CWColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = CWTypography.AppTypography.bodySmall,
                color = CWColors.TextTertiary,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
