package com.codewave.player.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.media.LyricLine
import com.codewave.player.core.media.LyricsResult
import com.codewave.player.core.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CWFullscreenLyricsSheet(
    track: Track,
    lyricsResult: LyricsResult,
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit,
    userOffsetMs: Long = 0L,
    onAdjustOffset: ((Long) -> Unit)? = null,
    onResetOffset: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var showTimestamps by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF090D14),
        dragHandle = null,
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Artwork Ambient Glow / Blur
            if (track.albumArtUri != null) {
                AsyncImage(
                    model = track.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                        .alpha(0.20f)
                )
            }

            // Dark Scrim Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xEE090D14),
                                Color(0xF5090D14),
                                Color(0xFF090D14)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Top Header: Drag handle pill, Title, Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF30363D))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFF0F6FC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = Color(0xFF8B949E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Micro-Tuner Offset Adjuster
                        if (lyricsResult is LyricsResult.Synchronized && onAdjustOffset != null && onResetOffset != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF161B22))
                                    .border(0.5.dp, Color(0xFF30363D), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            onAdjustOffset(-100L)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        .padding(horizontal = 5.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "-100",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color(0xFF8B949E)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            onResetOffset()
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        .padding(horizontal = 5.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (userOffsetMs == 0L) "SYNC" else "${if (userOffsetMs > 0) "+" else ""}${userOffsetMs}ms",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (userOffsetMs == 0L) CWColors.AccentCyan else Color(0xFFFFBD2E)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            onAdjustOffset(100L)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        .padding(horizontal = 5.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+100",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color(0xFF8B949E)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Toggle Timestamps Button
                        IconButton(
                            onClick = {
                                showTimestamps = !showTimestamps
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (showTimestamps) CWColors.AccentCyan.copy(alpha = 0.2f) else Color(0xFF161B22))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Toggle Timestamps",
                                tint = if (showTimestamps) CWColors.AccentCyan else Color(0xFF8B949E),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF161B22))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF8B949E),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Middle Lyrics Content (Fills available space)
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
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "STREAMING LRC SYNC BUFFER...",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp,
                                        color = Color(0xFF8B949E)
                                    )
                                }
                            }
                        }

                        is LyricsResult.Synchronized -> {
                            val effectivePositionMs = (currentPositionMs - userOffsetMs).coerceAtLeast(0L)
                            FullscreenSyncLyricsList(
                                lyrics = lyricsResult.lines,
                                currentPositionMs = effectivePositionMs,
                                showTimestamps = showTimestamps,
                                onSeekTo = onSeekTo
                            )
                        }

                        is LyricsResult.Plain -> {
                            FullscreenPlainLyricsList(lines = lyricsResult.lines)
                        }

                        else -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No lyrics found for this track.",
                                    fontFamily = FontFamily.Default,
                                    fontSize = 14.sp,
                                    color = Color(0xFF6E7681)
                                )
                            }
                        }
                    }

                    // Top & Bottom gradient fade masks
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF090D14), Color.Transparent)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xFF090D14))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Compact Playback Controls & Progress Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF11151C))
                        .border(1.dp, Color(0xFF21262D), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    CWPlayTimeBar(
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        onSeek = onSeekTo,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onSkipPrevious,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color(0xFFC9D1D9),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CWColors.AccentCyan)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color(0xFFC9D1D9),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenSyncLyricsList(
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
            val targetIndex = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(lyrics, key = { idx, line -> "${line.timestampMs}_$idx" }) { index, line ->
            val isActive = index == activeIndex && activeIndex != -1

            val textColor by animateColorAsState(
                targetValue = if (isActive) Color(0xFFFFFFFF) else Color(0xFF8B949E),
                animationSpec = tween(durationMillis = 200),
                label = "fsTextColor"
            )

            val textAlpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.40f,
                animationSpec = tween(durationMillis = 200),
                label = "fsTextAlpha"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isActive) CWColors.AccentCyan.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSeekTo(line.timestampMs)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showTimestamps) {
                    val minutes = line.timestampMs / 60000
                    val seconds = (line.timestampMs % 60000) / 1000
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (isActive) CWColors.AccentCyan else Color(0xFF484F58),
                        modifier = Modifier.width(46.dp)
                    )
                }

                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(width = 3.5.dp, height = 24.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(CWColors.AccentCyan)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Text(
                    text = line.text,
                    fontFamily = FontFamily.Default,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = if (isActive) 23.sp else 18.sp,
                    lineHeight = if (isActive) 30.sp else 24.sp,
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
private fun FullscreenPlainLyricsList(lines: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(lines) { index, line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = String.format("%02d", index + 1),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFF484F58),
                    modifier = Modifier.width(32.dp)
                )
                Text(
                    text = line,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFFC9D1D9),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
