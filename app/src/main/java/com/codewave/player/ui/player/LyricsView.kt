package com.codewave.player.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.media.LyricLine
import com.codewave.player.core.media.LyricsResult

@Composable
fun LyricsView(
    lyricsResult: LyricsResult,
    currentPositionMs: Long,
    trackTitle: String,
    trackArtist: String,
    onSeekTo: (Long) -> Unit,
    userOffsetMs: Long = 0L,
    onAdjustOffset: ((Long) -> Unit)? = null,
    onResetOffset: (() -> Unit)? = null,
    onToggleFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showTimestamps by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current

    val lineCount = when (lyricsResult) {
        is LyricsResult.Synchronized -> lyricsResult.lines.size
        is LyricsResult.Plain -> lyricsResult.lines.size
        else -> 0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D1117))
            .border(1.dp, Color(0xFF21262D), RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null) {}
            .padding(12.dp)
    ) {
        // Modern Studio / IDE Terminal Header
        StudioLyricsHeader(
            lyricsResult = lyricsResult,
            lineCount = lineCount,
            showTimestamps = showTimestamps,
            userOffsetMs = userOffsetMs,
            onAdjustOffset = onAdjustOffset?.let { callback ->
                { delta ->
                    callback(delta)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            onResetOffset = onResetOffset?.let { callback ->
                {
                    callback()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            onToggleTimestamps = {
                showTimestamps = !showTimestamps
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onToggleFullscreen = onToggleFullscreen
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Center Lyrics Canvas with Top & Bottom Dissolve Masks
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (lyricsResult) {
                is LyricsResult.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = CWColors.AccentCyan,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "STREAMING LRC SYNC BUFFER...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.8.sp,
                                color = Color(0xFF8B949E)
                            )
                        }
                    }
                }

                is LyricsResult.Unavailable -> {
                    StudioNoLyricsState(trackTitle = trackTitle, trackArtist = trackArtist)
                }

                is LyricsResult.Error -> {
                    StudioNoLyricsState(
                        trackTitle = trackTitle,
                        trackArtist = trackArtist,
                        statusMessage = "IO_ERROR: lyrics not available"
                    )
                }

                is LyricsResult.Plain -> {
                    StudioPlainLyricsView(lines = lyricsResult.lines)
                }

                is LyricsResult.Synchronized -> {
                    val effectivePositionMs = (currentPositionMs - userOffsetMs).coerceAtLeast(0L)
                    StudioSynchronizedLyricsView(
                        lyrics = lyricsResult.lines,
                        currentPositionMs = effectivePositionMs,
                        showTimestamps = showTimestamps,
                        onSeekTo = onSeekTo
                    )
                }
            }

            // Top Gradient Dissolve Edge Mask
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0D1117), Color.Transparent)
                        )
                    )
            )

            // Bottom Gradient Dissolve Edge Mask
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xFF0D1117))
                        )
                    )
            )
        }
    }
}

@Composable
private fun StudioLyricsHeader(
    lyricsResult: LyricsResult,
    lineCount: Int,
    showTimestamps: Boolean,
    userOffsetMs: Long,
    onAdjustOffset: ((Long) -> Unit)?,
    onResetOffset: (() -> Unit)?,
    onToggleTimestamps: () -> Unit,
    onToggleFullscreen: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B22), RoundedCornerShape(8.dp))
            .border(0.5.dp, Color(0xFF30363D), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Studio Status Lights & File Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5F56))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFBD2E))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF27C93F))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "lyrics.lrc",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                color = Color(0xFFF0F6FC)
            )

            if (lineCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• $lineCount lines",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color(0xFF8B949E)
                )
            }
        }

        // Right: Micro-Tuner & Mode Badge & Quick Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Micro-Tuner Offset Adjuster (PRD Feature 1.1)
            if (lyricsResult is LyricsResult.Synchronized && onAdjustOffset != null && onResetOffset != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0D1117))
                        .border(0.5.dp, Color(0xFF30363D), RoundedCornerShape(4.dp))
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clickable { onAdjustOffset(-100L) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "-100",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8B949E)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clickable { onResetOffset() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userOffsetMs == 0L) "SYNC" else "${if (userOffsetMs > 0) "+" else ""}${userOffsetMs}ms",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (userOffsetMs == 0L) CWColors.AccentCyan else Color(0xFFFFBD2E)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clickable { onAdjustOffset(100L) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+100",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8B949E)
                        )
                    }
                }
            }

            val badgeText = when (lyricsResult) {
                is LyricsResult.Synchronized -> "SYNCED"
                is LyricsResult.Plain -> "PLAIN"
                is LyricsResult.Loading -> "BUFFER"
                else -> "OFFLINE"
            }
            val badgeColor = when (lyricsResult) {
                is LyricsResult.Synchronized -> CWColors.AccentCyan
                is LyricsResult.Plain -> Color(0xFF58A6FF)
                is LyricsResult.Loading -> Color(0xFFE3B341)
                else -> Color(0xFF6E7681)
            }

            CWTechnicalBadge(
                text = badgeText,
                textColor = badgeColor
            )

            // Timestamp Toggle Button
            IconButton(
                onClick = onToggleTimestamps,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Toggle Timestamps",
                    tint = if (showTimestamps) CWColors.AccentCyan else Color(0xFF8B949E),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Fullscreen Expand Button
            if (onToggleFullscreen != null) {
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Fullscreen Lyrics",
                        tint = Color(0xFF8B949E),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioSynchronizedLyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    showTimestamps: Boolean,
    onSeekTo: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    val activeIndex by remember(lyrics, currentPositionMs) {
        derivedStateOf {
            lyrics.indexOfLast { it.timestampMs <= currentPositionMs }
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            val targetIndex = (activeIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        } else if (activeIndex == -1) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(lyrics, key = { idx, line -> "${line.timestampMs}_$idx" }) { index, line ->
            val isActive = index == activeIndex && activeIndex != -1

            val textColor by animateColorAsState(
                targetValue = if (isActive) Color(0xFFFFFFFF) else Color(0xFF8B949E),
                animationSpec = tween(durationMillis = 180),
                label = "lyricTextColor"
            )

            val textAlpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.40f,
                animationSpec = tween(durationMillis = 180),
                label = "lyricAlpha"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) CWColors.AccentCyan.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSeekTo(line.timestampMs)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Optional Timestamp Indicator
                if (showTimestamps) {
                    val minutes = line.timestampMs / 60000
                    val seconds = (line.timestampMs % 60000) / 1000
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (isActive) CWColors.AccentCyan else Color(0xFF484F58),
                        modifier = Modifier.width(44.dp)
                    )
                }

                // Active Left Glowing Accent Bar
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(width = 3.dp, height = 20.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(CWColors.AccentCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Lyric Text (Using natural, modern typography)
                Text(
                    text = line.text,
                    fontFamily = FontFamily.Default,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = if (isActive) 19.sp else 15.sp,
                    lineHeight = if (isActive) 26.sp else 21.sp,
                    color = textColor,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(textAlpha)
                )
            }
        }
    }
}

@Composable
private fun StudioPlainLyricsView(lines: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(lines) { index, line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = String.format("%02d", index + 1),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF484F58),
                    modifier = Modifier.width(28.dp)
                )
                Text(
                    text = line,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = Color(0xFFC9D1D9),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StudioNoLyricsState(
    trackTitle: String,
    trackArtist: String,
    statusMessage: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CWColors.AccentCyan.copy(alpha = 0.10f))
                    .border(1.dp, CWColors.AccentCyan.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = CWColors.AccentCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "NO LYRICS AVAILABLE",
                style = CWTypography.TechBadge,
                letterSpacing = 1.2.sp,
                color = Color(0xFFF0F6FC)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Place a matching .lrc file in your audio directory or embed ID3 lyrics tags.",
                style = CWTypography.AppTypography.bodyMedium,
                color = Color(0xFF8B949E),
                textAlign = TextAlign.Center,
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CWTechnicalBadge(text = ".LRC", textColor = CWColors.AccentCyan)
                CWTechnicalBadge(text = ".TXT", textColor = Color(0xFF8B949E))
                CWTechnicalBadge(text = "ID3 USLT", textColor = Color(0xFF8B949E))
            }
        }
    }
}
