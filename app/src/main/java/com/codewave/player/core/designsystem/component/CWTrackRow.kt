package com.codewave.player.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.Track

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CWTrackRow(
    track: Track,
    onTrackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: ((Boolean) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onFavoriteClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null
) {
    val rowBg = when {
        isSelected -> CWColors.SurfaceOverlay
        isPlaying -> CWColors.SurfaceElevated
        else -> CWColors.Background
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBg)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onSelectToggle?.invoke(!isSelected)
                    } else {
                        onTrackClick()
                    }
                },
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = CWColors.AccentCyan,
                    uncheckedColor = CWColors.TextTertiary
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // Album Art Thumbnail or Monogram Fallback (PRD Section 53)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                .background(CWColors.SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (!track.albumArtUri.isNullOrEmpty()) {
                AsyncImage(
                    model = track.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(46.dp)
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

        // Title & Metadata
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                style = CWTypography.AppTypography.titleMedium,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                color = if (isPlaying) CWColors.AccentCyan else CWColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${track.artist} • ${track.album}",
                style = CWTypography.AppTypography.bodyMedium,
                color = CWColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Technical Badges
            Row(
                modifier = Modifier.padding(top = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (track.isHiRes) {
                    CWQualityBadge(
                        text = "HI-RES",
                        isHiRes = true,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                } else if (track.isLossless) {
                    CWQualityBadge(
                        text = "LOSSLESS",
                        isLossless = true,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                CWTechnicalBadge(
                    text = track.technicalSummary
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Duration
        Text(
            text = track.durationFormatted,
            style = CWTypography.TechTelemetry,
            color = CWColors.TextSecondary
        )

        // Favorite Button
        if (onFavoriteClick != null && !isSelectionMode) {
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (track.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (track.isFavorite) CWColors.Danger else CWColors.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // More / Context Menu
        if (onMoreClick != null && !isSelectionMode) {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More actions",
                    tint = CWColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
