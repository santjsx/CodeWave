package com.codewave.player.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.codewave.player.core.model.Album

@Composable
fun CWAlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        // Artwork Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                .background(CWColors.SurfaceElevated)
                .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium)),
            contentAlignment = Alignment.Center
        ) {
            if (!album.artworkUri.isNullOrEmpty()) {
                AsyncImage(
                    model = album.artworkUri,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "CODEWAVE",
                    style = CWTypography.TechBadge,
                    color = CWColors.TextTechnical
                )
            }

            if (album.isHiRes) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    CWQualityBadge(text = "HI-RES", isHiRes = true)
                }
            } else if (album.isLossless) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    CWQualityBadge(text = "LOSSLESS", isLossless = true)
                }
            }
        }

        // Album Title
        Text(
            text = album.title,
            style = CWTypography.AppTypography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = CWColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Artist and Track Count
        Row(
            modifier = Modifier.padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = album.artist,
                style = CWTypography.AppTypography.bodyMedium,
                color = CWColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Text(
                text = " • ${album.trackCount} trks",
                style = CWTypography.TechTelemetry,
                color = CWColors.TextTertiary,
                maxLines = 1
            )
        }
    }
}
