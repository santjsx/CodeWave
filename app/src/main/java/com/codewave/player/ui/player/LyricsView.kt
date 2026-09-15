package com.codewave.player.ui.player

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(CWShapes.RadiusLarge))
            .background(CWColors.SurfacePrimary.copy(alpha = 0.85f))
            .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusLarge))
            .padding(14.dp)
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
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading lyrics...",
                            style = CWTypography.AppTypography.bodyMedium,
                            color = CWColors.TextSecondary
                        )
                    }
                }
            }
            is LyricsResult.Unavailable -> {
                NoLyricsState(trackTitle = trackTitle, trackArtist = trackArtist)
            }
            is LyricsResult.Error -> {
                NoLyricsState(
                    trackTitle = trackTitle,
                    trackArtist = trackArtist,
                    message = "Lyrics not available for this track"
                )
            }
            is LyricsResult.Plain -> {
                PlainLyricsView(lines = lyricsResult.lines)
            }
            is LyricsResult.Synchronized -> {
                SynchronizedLyricsView(
                    lyrics = lyricsResult.lines,
                    currentPositionMs = currentPositionMs,
                    onSeekTo = onSeekTo
                )
            }
        }
    }
}

@Composable
private fun SynchronizedLyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit
) {
    val listState = rememberLazyListState()

    val activeIndex by remember(lyrics, currentPositionMs) {
        derivedStateOf {
            val idx = lyrics.indexOfLast { it.timestampMs <= currentPositionMs }
            if (idx == -1 && lyrics.isNotEmpty()) 0 else idx
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            val targetIndex = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    // Header
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CWColors.AccentCyan)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "LYRICS",
                style = CWTypography.TechBadge,
                color = CWColors.TextPrimary
            )
        }

        CWTechnicalBadge(
            text = "LRC SYNCED",
            textColor = CWColors.AccentCyan
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(lyrics, key = { idx, line -> "${line.timestampMs}_$idx" }) { index, line ->
            val isActive = index == activeIndex
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.40f,
                animationSpec = tween(durationMillis = 200),
                label = "lyricAlpha"
            )
            val textColor by animateColorAsState(
                targetValue = if (isActive) CWColors.AccentCyan else CWColors.TextSecondary,
                animationSpec = tween(durationMillis = 200),
                label = "lyricColor"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                    .clickable { onSeekTo(line.timestampMs) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timestamp
                val minutes = line.timestampMs / 60000
                val seconds = (line.timestampMs % 60000) / 1000
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    style = CWTypography.TechTelemetry,
                    color = if (isActive) CWColors.AccentCyan.copy(alpha = 0.8f) else CWColors.TextTertiary,
                    modifier = Modifier.width(46.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = line.text,
                    style = if (isActive) {
                        CWTypography.AppTypography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            lineHeight = 24.sp
                        )
                    } else {
                        CWTypography.AppTypography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    },
                    color = textColor,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(alpha)
                )
            }
        }
    }
}

@Composable
private fun PlainLyricsView(lines: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CWColors.AccentCyan)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "LYRICS",
                style = CWTypography.TechBadge,
                color = CWColors.TextPrimary
            )
        }

        CWTechnicalBadge(
            text = "PLAIN TEXT",
            textColor = CWColors.TextSecondary
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(lines) { _, line ->
            Text(
                text = line,
                style = CWTypography.AppTypography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                color = CWColors.TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun NoLyricsState(
    trackTitle: String,
    trackArtist: String,
    message: String = "No Lyrics Available"
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                    .background(CWColors.SurfaceElevated)
                    .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lyrics,
                    contentDescription = null,
                    tint = CWColors.TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = CWTypography.AppTypography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CWColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$trackTitle • $trackArtist",
                style = CWTypography.AppTypography.bodyMedium,
                color = CWColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
