package com.codewave.player.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.Track

@Composable
fun CWMiniPlayer(
    track: Track?,
    isPlaying: Boolean,
    progress: Float, // 0.0f to 1.0f
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        if (track == null) return@AnimatedVisibility

        var totalDrag by remember { mutableFloatStateOf(0f) }
        val draggableState = rememberDraggableState { delta ->
            totalDrag += delta
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                .background(CWColors.SurfaceElevated)
                .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (totalDrag > 120f) {
                            onPrevClick()
                        } else if (totalDrag < -120f) {
                            onNextClick()
                        }
                        totalDrag = 0f
                    }
                )
                .clickable(onClick = onClick)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artwork Thumbnail
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                            .background(CWColors.SurfacePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!track.albumArtUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = track.albumArtUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(44.dp)
                            )
                        } else {
                            Text(
                                text = "CW",
                                style = CWTypography.TechBadge,
                                color = CWColors.TextTechnical
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Title & Artist
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = track.title,
                            style = CWTypography.AppTypography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = CWColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (track.isHiRes) {
                                CWQualityBadge(text = "HI-RES", isHiRes = true, modifier = Modifier.padding(end = 4.dp))
                            } else if (track.isLossless) {
                                CWQualityBadge(text = "LOSSLESS", isLossless = true, modifier = Modifier.padding(end = 4.dp))
                            }
                            Text(
                                text = track.artist,
                                style = CWTypography.AppTypography.bodyMedium,
                                color = CWColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Prev Button
                    IconButton(
                        onClick = onPrevClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = CWColors.TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Play / Pause Button
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = CWColors.AccentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Next Button
                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next track",
                            tint = CWColors.TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // High-Definition Progress Bar (Prominent, High-Contrast & Beautiful)
                val safeProgress = progress.coerceIn(0f, 1f)
                val accentColor = CWColors.AccentCyan
                val secondaryAccent = CWColors.AccentBlue

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(bottomStart = CWShapes.RadiusMedium, bottomEnd = CWShapes.RadiusMedium))
                        .background(Color(0xFF0D1117))
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val barWidth = size.width * safeProgress
                        if (barWidth > 0f) {
                            // Glowing gradient fill
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(secondaryAccent, accentColor)
                                ),
                                size = Size(barWidth, size.height)
                            )

                            // Leading edge glow pulse
                            drawCircle(
                                color = Color.White.copy(alpha = 0.85f),
                                radius = size.height * 0.75f,
                                center = Offset(barWidth, size.height / 2f)
                            )
                        }
                    }
                }
            }
        }
    }
}
