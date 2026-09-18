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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.codewave.player.core.scanner.ScanProgress
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onOpenThemes: (() -> Unit)? = null,
    onOpenCommandPalette: (() -> Unit)? = null,
    onTrackOptions: ((Track) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val lastPositionMs by viewModel.lastPlayedPositionMs.collectAsState()
    val lastTrackId by viewModel.lastPlayedTrackId.collectAsState()
    val continueListeningTrack by viewModel.continueListeningTrack.collectAsState()

    var showBanner by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var rememberedTotalCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(scanProgress.isScanning) {
        if (scanProgress.isScanning) {
            showBanner = true
            isCompleted = false
        } else if (showBanner) {
            if (rememberedTotalCount > 0) {
                isCompleted = true
                delay(1200)
            }
            showBanner = false
            isCompleted = false
        }
    }

    LaunchedEffect(scanProgress.totalCount) {
        if (scanProgress.totalCount > 0) {
            rememberedTotalCount = scanProgress.totalCount
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
    ) {
        // Sticky Header: Pinned at the top of the HomeScreen (never scrolls away)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CWColors.Background)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = if (onOpenCommandPalette != null) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenCommandPalette() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    } else Modifier
                ) {
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
                    if (onOpenCommandPalette != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CWTechnicalBadge(text = "CMD", textColor = CWColors.AccentCyan)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenThemes != null) {
                        IconButton(onClick = onOpenThemes) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Themes",
                                tint = CWColors.AccentCyan
                            )
                        }
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

            // Library Scan Banner (smooth expansion, spring damping & graceful completion hold)
            AnimatedVisibility(
                visible = showBanner,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                IndexingBannerCard(
                    scanProgress = scanProgress,
                    isCompleted = isCompleted,
                    finalTotalCount = rememberedTotalCount
                )
            }

            // High-Density Telemetry Strip (PRD Section 8)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(CWShapes.RadiusLarge),
                colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TelemetryColumn(
                        value = "${stats.trackCount}",
                        label = "TRACKS"
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(CWColors.BorderSubtle)
                    )
                    TelemetryColumn(
                        value = "${stats.losslessCount}",
                        label = "LOSSLESS",
                        highlight = stats.losslessCount > 0
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(CWColors.BorderSubtle)
                    )
                    TelemetryColumn(
                        value = "${stats.hiResCount}",
                        label = "HI-RES",
                        highlight = stats.hiResCount > 0
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(CWColors.BorderSubtle)
                    )
                    TelemetryColumn(
                        value = "${stats.albumCount}",
                        label = "ALBUMS"
                    )
                }
            }

            // Quick Navigation Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
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

            Spacer(modifier = Modifier.height(4.dp))
        }

        // Scrollable Content List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Continue Listening Section (PRD Section 9)
            val continueTrack = continueListeningTrack ?: recentlyPlayed.firstOrNull()
            if (continueTrack != null) {
                val resumePosition = if (lastTrackId == continueTrack.id && lastPositionMs > 0L) lastPositionMs else 0L
                val resumeBadgeText = if (resumePosition > 0L) {
                    val remMins = resumePosition / 60000
                    val remSecs = (resumePosition % 60000) / 1000
                    String.format("RESUME AT %02d:%02d", remMins, remSecs)
                } else null

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONTINUE LISTENING",
                                style = CWTypography.TechBadge,
                                color = CWColors.TextSecondary
                            )
                            if (resumeBadgeText != null) {
                                CWTechnicalBadge(
                                    text = resumeBadgeText,
                                    textColor = CWColors.AccentCyan
                                )
                            }
                        }
                        CWTrackRow(
                            track = continueTrack,
                            onTrackClick = {
                                val queue = if (recentlyPlayed.any { it.id == continueTrack.id }) recentlyPlayed else listOf(continueTrack)
                                viewModel.playTrack(continueTrack, queue, resumePosition)
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(continueTrack) },
                            onMoreClick = { onTrackOptions?.invoke(continueTrack) ?: onTrackInspect(continueTrack) }
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
                        onMoreClick = { onTrackOptions?.invoke(track) ?: onTrackInspect(track) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp)) // Mini player bottom padding
            }
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

@Composable
private fun IndexingBannerCard(
    scanProgress: ScanProgress,
    isCompleted: Boolean,
    finalTotalCount: Int
) {
    val isDiscovering = scanProgress.totalCount == 0 && !isCompleted
    val targetProgress = when {
        isCompleted -> 1f
        scanProgress.totalCount > 0 -> (scanProgress.processedCount.toFloat() / scanProgress.totalCount.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(
            durationMillis = if (isCompleted) 250 else 200,
            easing = LinearOutSlowInEasing
        ),
        label = "indexing_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "indexing_anim")

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Animated 3-bar audio spectrum icon
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val borderColor = when {
        isCompleted -> CWColors.Success.copy(alpha = 0.6f)
        else -> CWColors.AccentCyan.copy(alpha = pulseAlpha)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(CWShapes.RadiusMedium),
        colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(CWColors.Success.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CWColors.Success,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        // Animated 3-bar audio spectrum icon
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.height(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(bar1Height.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(CWColors.AccentCyan)
                            )
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(bar2Height.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(Color(0xFF2979FF))
                            )
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(bar3Height.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(Color(0xFF9D4EDD))
                            )
                        }
                    }

                    Text(
                        text = when {
                            isCompleted -> "LIBRARY SYNCHRONIZED"
                            isDiscovering -> "DISCOVERING AUDIO FILES..."
                            else -> "INDEXING AUDIO PIPELINE..."
                        },
                        style = CWTypography.TechBadge,
                        color = if (isCompleted) CWColors.Success else CWColors.AccentCyan,
                        letterSpacing = 0.75.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when {
                            isCompleted -> "100%"
                            isDiscovering -> "SCANNING"
                            else -> "${(animatedProgress * 100).toInt()}%"
                        },
                        style = CWTypography.TechBadge,
                        color = if (isCompleted) CWColors.Success else CWColors.AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isCompleted -> "$finalTotalCount tracks"
                            isDiscovering -> "..."
                            else -> "${scanProgress.processedCount}/${scanProgress.totalCount}"
                        },
                        style = CWTypography.TechTelemetry,
                        color = CWColors.TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Specular Progress Bar with Indeterminate and Determinate Modes
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            ) {
                val totalWidth = size.width
                val barHeight = size.height

                // Track background
                drawRoundRect(
                    color = Color(0xFF161B22),
                    size = size,
                    cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                )

                if (isDiscovering) {
                    // Sweeping radar beam across the entire track
                    val radarBeamWidth = totalWidth * 0.4f
                    val radarStart = (shimmerOffset * totalWidth) - (radarBeamWidth / 2f)
                    val radarBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00E5FF).copy(alpha = 0.8f),
                            Color(0xFF2979FF).copy(alpha = 0.8f),
                            Color.Transparent
                        ),
                        startX = radarStart,
                        endX = radarStart + radarBeamWidth
                    )
                    drawRoundRect(
                        brush = radarBrush,
                        size = size,
                        cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                    )
                } else {
                    val filledWidth = totalWidth * animatedProgress
                    if (filledWidth > 0f) {
                        val fillBrush = Brush.horizontalGradient(
                            colors = if (isCompleted) {
                                listOf(Color(0xFF00E676), Color(0xFF00B0FF))
                            } else {
                                listOf(
                                    Color(0xFF00E5FF), // Cyan
                                    Color(0xFF2979FF), // Vivid Blue
                                    Color(0xFF9D4EDD)  // Purple
                                )
                            },
                            startX = 0f,
                            endX = totalWidth
                        )

                        drawRoundRect(
                            brush = fillBrush,
                            size = Size(filledWidth, barHeight),
                            cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                        )

                        if (!isCompleted) {
                            // Sweeping Specular Light Wave across filled region
                            val shimmerWidth = (filledWidth * 0.5f).coerceIn(30.dp.toPx(), 120.dp.toPx())
                            val shimmerStart = (shimmerOffset * filledWidth) - (shimmerWidth / 2f)
                            val shimmerBrush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.55f),
                                    Color.Transparent
                                ),
                                startX = shimmerStart,
                                endX = shimmerStart + shimmerWidth
                            )

                            drawRoundRect(
                                brush = shimmerBrush,
                                size = Size(filledWidth, barHeight),
                                cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                            )

                            // Glowing Head Pulse Dot
                            if (filledWidth > barHeight * 0.5f) {
                                val clampedCenterX = filledWidth.coerceIn(barHeight / 2f, totalWidth - barHeight / 2f)
                                drawCircle(
                                    color = Color(0xFF00E5FF).copy(alpha = pulseAlpha * 0.45f),
                                    radius = barHeight * 1.5f,
                                    center = Offset(clampedCenterX, barHeight / 2f)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = barHeight * 0.65f,
                                    center = Offset(clampedCenterX, barHeight / 2f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle status ticker & current file activity
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        isCompleted -> "All acoustic tags, bitrates & album art ready"
                        isDiscovering -> "Analyzing local storage for audio files..."
                        scanProgress.currentFile.isNotBlank() -> scanProgress.currentFile
                        else -> "Extracting sample rates & FLAC/ALAC tags..."
                    },
                    style = CWTypography.TechTelemetry,
                    color = if (isCompleted) CWColors.Success.copy(alpha = 0.8f) else CWColors.TextTertiary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) CWColors.Success else CWColors.AccentCyan.copy(alpha = pulseAlpha)
                        )
                )
            }
        }
    }
}
