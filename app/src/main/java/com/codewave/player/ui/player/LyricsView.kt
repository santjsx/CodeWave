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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.component.tactileClickable
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
    // Touch absorption prevents gestures from leaking through to underlying views
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(CWShapes.RadiusLarge))
            .background(Color(0xFF090D12))
            .border(1.dp, CWColors.AccentCyan.copy(alpha = 0.25f), RoundedCornerShape(CWShapes.RadiusLarge))
            .clickable(interactionSource = interactionSource, indication = null) {}
            .padding(12.dp)
    ) {
        // Terminal Window Header Bar
        TerminalHeader(
            badgeText = when (lyricsResult) {
                is LyricsResult.Synchronized -> "LRC_SYNCED"
                is LyricsResult.Plain -> "PLAIN_TEXT"
                is LyricsResult.Loading -> "BUFFERING"
                else -> "IDLE"
            },
            badgeColor = when (lyricsResult) {
                is LyricsResult.Synchronized -> CWColors.AccentCyan
                is LyricsResult.Plain -> CWColors.TextSecondary
                is LyricsResult.Loading -> CWColors.Warning
                else -> CWColors.TextTertiary
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                            text = "$ stream --stdin /lyrics.lrc ...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = CWColors.TextSecondary
                        )
                    }
                }
            }
            is LyricsResult.Unavailable -> {
                TerminalNoLyricsState(trackTitle = trackTitle, trackArtist = trackArtist)
            }
            is LyricsResult.Error -> {
                TerminalNoLyricsState(
                    trackTitle = trackTitle,
                    trackArtist = trackArtist,
                    statusMessage = "IO_ERROR: lyrics not available"
                )
            }
            is LyricsResult.Plain -> {
                TerminalPlainLyricsView(lines = lyricsResult.lines)
            }
            is LyricsResult.Synchronized -> {
                TerminalSynchronizedLyricsView(
                    lyrics = lyricsResult.lines,
                    currentPositionMs = currentPositionMs,
                    onSeekTo = onSeekTo
                )
            }
        }
    }
}

@Composable
private fun TerminalHeader(
    badgeText: String,
    badgeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5F56))
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFBD2E))
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF27C93F))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "sh: codewave --lyrics",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = CWColors.TextTertiary
            )
        }

        CWTechnicalBadge(
            text = badgeText,
            textColor = badgeColor
        )
    }
}

@Composable
private fun TerminalSynchronizedLyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit
) {
    val listState = rememberLazyListState()

    val activeIndex by remember(lyrics, currentPositionMs) {
        derivedStateOf {
            lyrics.indexOfLast { it.timestampMs <= currentPositionMs }
        }
    }

    // Terminal blinking block cursor for active line
    val infiniteTransition = rememberInfiniteTransition(label = "terminalCursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )

    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            val targetIndex = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        } else if (activeIndex == -1) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(lyrics, key = { idx, line -> "${line.timestampMs}_$idx" }) { index, line ->
            val isActive = index == activeIndex && activeIndex != -1
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.40f,
                animationSpec = tween(durationMillis = 160),
                label = "lyricAlpha"
            )
            val textColor by animateColorAsState(
                targetValue = if (isActive) CWColors.AccentCyan else CWColors.TextSecondary,
                animationSpec = tween(durationMillis = 160),
                label = "lyricColor"
            )

            val minutes = line.timestampMs / 60000
            val seconds = (line.timestampMs % 60000) / 1000
            val centis = (line.timestampMs % 1000) / 10

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                    .background(if (isActive) CWColors.AccentCyan.copy(alpha = 0.10f) else Color.Transparent)
                    .tactileClickable(targetScale = 0.98f) { onSeekTo(line.timestampMs) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timestamp in terminal brackets
                Text(
                    text = String.format("[%02d:%02d.%02d]", minutes, seconds, centis),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (isActive) CWColors.AccentCyan.copy(alpha = 0.85f) else CWColors.TextTertiary,
                    modifier = Modifier.width(76.dp)
                )

                // Terminal prompt on active line
                Text(
                    text = if (isActive) "❯ " else "  ",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CWColors.AccentCyan,
                    modifier = Modifier.width(18.dp)
                )

                Text(
                    text = line.text,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontSize = if (isActive) 15.sp else 13.sp,
                    lineHeight = 20.sp,
                    color = textColor,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(alpha)
                )

                if (isActive) {
                    Text(
                        text = " ▋",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = CWColors.AccentCyan,
                        modifier = Modifier.alpha(cursorAlpha)
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalPlainLyricsView(lines: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(lines) { index, line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = String.format("%02d | ", index + 1),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = CWColors.TextTertiary,
                    modifier = Modifier.width(36.dp)
                )
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = CWColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TerminalNoLyricsState(
    trackTitle: String,
    trackArtist: String,
    statusMessage: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(CWColors.AccentCyan.copy(alpha = 0.12f))
                    .border(1.dp, CWColors.AccentCyan.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = CWColors.AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NO LYRICS AVAILABLE",
                style = CWTypography.TechBadge,
                letterSpacing = 1.2.sp,
                color = CWColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Add a .lrc file in the track folder or add embedded ID3 lyrics tags.",
                style = CWTypography.AppTypography.bodyMedium,
                color = CWColors.TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CWTechnicalBadge(text = ".LRC", textColor = CWColors.AccentCyan)
                CWTechnicalBadge(text = ".TXT", textColor = CWColors.TextSecondary)
                CWTechnicalBadge(text = "ID3 USLT", textColor = CWColors.TextSecondary)
            }
        }
    }
}

