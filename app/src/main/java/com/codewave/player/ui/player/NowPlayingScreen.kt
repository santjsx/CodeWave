package com.codewave.player.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewave.player.core.data.PlaybackRepository
import com.codewave.player.core.designsystem.component.CWPlayTimeBar
import com.codewave.player.core.designsystem.component.CWQualityBadge
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.component.TrackInspectorSheet
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.media.LrcParser
import com.codewave.player.core.model.PlaybackState
import com.codewave.player.core.model.RepeatMode
import com.codewave.player.core.model.Track

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.semantics.Role
import com.codewave.player.core.designsystem.component.contentColor
import com.codewave.player.core.designsystem.component.tactileClickable
import com.codewave.player.core.designsystem.component.tactilePress
import com.codewave.player.core.media.LyricsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class NowPlayingCenterView {
    ARTWORK,
    LYRICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackRepository: PlaybackRepository,
    onCollapse: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler { onCollapse() }

    val state by playbackRepository.playbackState.collectAsState()
    val sleepTimerRemainingMs by playbackRepository.sleepTimerRemainingMs.collectAsState()
    val track = state.currentTrack ?: return

    var centerView by remember { mutableStateOf(NowPlayingCenterView.ARTWORK) }
    var isInspectorOpen by remember { mutableStateOf(false) }
    var isQualityExplainerOpen by remember { mutableStateOf(false) }
    var isQueueOpen by remember { mutableStateOf(false) }
    var isSleepTimerOpen by remember { mutableStateOf(false) }
    var isSpeedSelectorOpen by remember { mutableStateOf(false) }

    val duration = state.durationMs.coerceAtLeast(1L)
    var lyricsResult by remember(track.id) { mutableStateOf<LyricsResult>(LyricsResult.Loading) }

    androidx.compose.runtime.LaunchedEffect(track.id, track.path) {
        lyricsResult = LyricsResult.Loading
        withContext(Dispatchers.IO) {
            val res = LrcParser.loadLyricsForTrack(track.path)
            lyricsResult = res
        }
    }

    val rootInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
            .clickable(interactionSource = rootInteractionSource, indication = null) {}
            .padding(horizontal = 20.dp)
    ) {
        // Top Navigation & Track Inspector Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = CWColors.TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // View Toggle Chips [ ARTWORK | LYRICS ]
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                    .background(CWColors.SurfaceElevated)
                    .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                        .background(if (centerView == NowPlayingCenterView.ARTWORK) CWColors.AccentCyan else Color.Transparent)
                        .clickable { centerView = NowPlayingCenterView.ARTWORK }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "COVER",
                        style = CWTypography.TechBadge,
                        color = if (centerView == NowPlayingCenterView.ARTWORK) CWColors.Background else CWColors.TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                        .background(if (centerView == NowPlayingCenterView.LYRICS) CWColors.AccentCyan else Color.Transparent)
                        .clickable { centerView = NowPlayingCenterView.LYRICS }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "LYRICS",
                        style = CWTypography.TechBadge,
                        color = if (centerView == NowPlayingCenterView.LYRICS) CWColors.Background else CWColors.TextSecondary
                    )
                }
            }

            IconButton(onClick = { isInspectorOpen = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Track Inspector",
                    tint = CWColors.AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center Area: Crossfade between Artwork and Lyrics View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Crossfade(targetState = centerView, label = "np_center_view") { view ->
                when (view) {
                    NowPlayingCenterView.ARTWORK -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(CWShapes.RadiusLarge))
                                .background(CWColors.SurfacePrimary)
                                .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusLarge)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!track.albumArtUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = track.albumArtUri,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = CWColors.AccentCyan,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "> CODEWAVE_STUDIO",
                                        style = CWTypography.TechInspectorHeader,
                                        color = CWColors.AccentCyan
                                    )
                                    Text(
                                        text = "HI-FI AUDIO ENGINE",
                                        style = CWTypography.TechBadge,
                                        color = CWColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    NowPlayingCenterView.LYRICS -> {
                        LyricsView(
                            lyricsResult = lyricsResult,
                            currentPositionMs = state.positionMs,
                            trackTitle = track.title,
                            trackArtist = track.artist,
                            onSeekTo = { posMs -> playbackRepository.seekTo(posMs) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Song Title, Artist & Real-time Favorite Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = CWTypography.AppTypography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = CWColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} · ${track.album}",
                    style = CWTypography.AppTypography.bodyLarge,
                    color = CWColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { onToggleFavorite(track) }) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (track.isFavorite) CWColors.Danger else CWColors.TextSecondary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Clickable Quality & Technical Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (track.isHiRes) {
                CWQualityBadge(
                    text = "HI-RES",
                    isHiRes = true,
                    onClick = { isQualityExplainerOpen = true }
                )
            } else if (track.isLossless) {
                CWQualityBadge(
                    text = "LOSSLESS",
                    isLossless = true,
                    onClick = { isQualityExplainerOpen = true }
                )
            } else {
                CWQualityBadge(
                    text = track.format.displayName,
                    onClick = { isQualityExplainerOpen = true }
                )
            }

            CWTechnicalBadge(
                text = "${track.bitDepth?.let { "$it-BIT · " } ?: ""}${track.sampleRate / 1000}kHz",
                onClick = { isQualityExplainerOpen = true }
            )

            if (track.bitrateKbps > 0) {
                CWTechnicalBadge(text = "${track.bitrateKbps} kbps")
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Scrubber / Progress Bar using CWPlayTimeBar
        CWPlayTimeBar(
            currentPositionMs = state.positionMs,
            durationMs = duration,
            onSeek = { targetMs -> playbackRepository.seekTo(targetMs) }
        )

        // Timestamp Readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(state.positionMs),
                style = CWTypography.TechTelemetry
            )
            Text(
                text = formatDuration(duration),
                style = CWTypography.TechTelemetry
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Transport Controls Row (Shuffle, Prev, Play/Pause, Next, Repeat)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = { playbackRepository.setShuffle(!state.shuffleMode) },
                modifier = Modifier.tactilePress()
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.shuffleMode) CWColors.AccentCyan else CWColors.TextTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous
            IconButton(
                onClick = { playbackRepository.skipPrevious() },
                modifier = Modifier.size(48.dp).tactilePress()
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = CWColors.TextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Play / Pause Circle
            val playPauseContentColor = CWColors.AccentCyan.contentColor()
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(CWColors.AccentCyan)
                    .tactileClickable(role = Role.Button, targetScale = 0.94f) { playbackRepository.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = playPauseContentColor,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Next
            IconButton(
                onClick = { playbackRepository.skipNext() },
                modifier = Modifier.size(48.dp).tactilePress()
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = CWColors.TextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Repeat Mode
            IconButton(
                onClick = {
                    val nextMode = when (state.repeatMode) {
                        RepeatMode.OFF -> RepeatMode.ALL
                        RepeatMode.ALL -> RepeatMode.ONE
                        RepeatMode.ONE -> RepeatMode.OFF
                    }
                    playbackRepository.setRepeatMode(nextMode)
                },
                modifier = Modifier.tactilePress()
            ) {
                Icon(
                    imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (state.repeatMode != RepeatMode.OFF) CWColors.AccentCyan else CWColors.TextTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Aligned, Equal-width Auxiliary Controls: Speed, Sleep Timer, Queue with generous breathing room
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Chip (Equal flex weight)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                    .background(CWColors.SurfaceElevated)
                    .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                    .tactileClickable { isSpeedSelectorOpen = true },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed",
                        tint = CWColors.AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${state.playbackSpeed}x",
                        style = CWTypography.TechBadge,
                        fontWeight = FontWeight.SemiBold,
                        color = CWColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Sleep Timer Chip (Equal flex weight)
            val remainingMs = sleepTimerRemainingMs
            val isTimerActive = remainingMs > 0L
            val timerText = when {
                remainingMs <= 0L -> "Timer"
                remainingMs >= 60_000L -> {
                    val remMins = (remainingMs + 59999L) / 60000L
                    "${remMins}m"
                }
                else -> {
                    val remSecs = (remainingMs + 999L) / 1000L
                    "${remSecs}s"
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                    .background(if (isTimerActive) CWColors.AccentCyan.copy(alpha = 0.15f) else CWColors.SurfaceElevated)
                    .border(
                        1.dp,
                        if (isTimerActive) CWColors.AccentCyan else CWColors.BorderSubtle,
                        RoundedCornerShape(CWShapes.RadiusMedium)
                    )
                    .tactileClickable { isSleepTimerOpen = true },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = if (isTimerActive) CWColors.AccentCyan else CWColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timerText,
                        style = CWTypography.TechBadge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isTimerActive) CWColors.AccentCyan else CWColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Queue Chip (Equal flex weight)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                    .background(CWColors.SurfaceElevated)
                    .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                    .tactileClickable { isQueueOpen = true },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = CWColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Queue (${state.queue.size})",
                        style = CWTypography.TechBadge,
                        fontWeight = FontWeight.SemiBold,
                        color = CWColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    // Modal: Track Inspector
    if (isInspectorOpen) {
        TrackInspectorSheet(
            track = track,
            outputInfo = state.outputInfo,
            dspStatus = state.dspStatus,
            onDismiss = { isInspectorOpen = false }
        )
    }

    // Modal: Audio Quality Explainer
    if (isQualityExplainerOpen) {
        AudioQualityExplainerDialog(
            track = track,
            onDismiss = { isQualityExplainerOpen = false }
        )
    }

    // Modal: Queue Sheet
    if (isQueueOpen) {
        ModalBottomSheet(
            onDismissRequest = { isQueueOpen = false },
            containerColor = CWColors.SurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "PLAYBACK QUEUE (${state.queue.size} TRACKS)",
                    style = CWTypography.TechInspectorHeader,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                    itemsIndexed(state.queue, key = { idx, t -> "${idx}_${t.id}" }) { idx, t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (idx == state.queueIndex) CWColors.SurfaceOverlay else CWColors.SurfaceElevated)
                                .clickable { playbackRepository.playTrack(t, state.queue) }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "%02d".format(idx + 1),
                                style = CWTypography.TechTelemetry,
                                color = if (idx == state.queueIndex) CWColors.AccentCyan else CWColors.TextTertiary,
                                modifier = Modifier.width(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t.title,
                                    style = CWTypography.AppTypography.bodyLarge,
                                    fontWeight = if (idx == state.queueIndex) FontWeight.Bold else FontWeight.Normal,
                                    color = if (idx == state.queueIndex) CWColors.AccentCyan else CWColors.TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = t.artist,
                                    style = CWTypography.AppTypography.bodyMedium,
                                    color = CWColors.TextSecondary,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = t.durationFormatted,
                                style = CWTypography.TechTelemetry,
                                color = CWColors.TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog: Speed Selector
    if (isSpeedSelectorOpen) {
        AlertDialog(
            onDismissRequest = { isSpeedSelectorOpen = false },
            title = {
                Text(
                    text = "PLAYBACK SPEED",
                    style = CWTypography.TechInspectorHeader
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.5f, 0.75f, 0.9f, 1.0f, 1.1f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        val isCurrent = state.playbackSpeed == speed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                                .background(if (isCurrent) CWColors.SurfaceOverlay else Color.Transparent)
                                .clickable {
                                    playbackRepository.setPlaybackSpeed(speed)
                                    isSpeedSelectorOpen = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${speed}x",
                                style = CWTypography.AppTypography.bodyLarge,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) CWColors.AccentCyan else CWColors.TextPrimary
                            )
                            if (isCurrent) {
                                CWTechnicalBadge(text = "ACTIVE", textColor = CWColors.AccentCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isSpeedSelectorOpen = false }) {
                    Text("CLOSE", style = CWTypography.TechBadge, color = CWColors.AccentCyan)
                }
            },
            containerColor = CWColors.SurfaceElevated
        )
    }

    // Dialog: Sleep Timer
    if (isSleepTimerOpen) {
        AlertDialog(
            onDismissRequest = { isSleepTimerOpen = false },
            title = {
                Text(
                    text = "SLEEP TIMER",
                    style = CWTypography.TechInspectorHeader
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val remainingMs = sleepTimerRemainingMs
                    if (remainingMs != null && remainingMs > 0L) {
                        val totalSecs = (remainingMs + 999L) / 1000L
                        val mins = totalSecs / 60L
                        val secs = totalSecs % 60L
                        val remLabel = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                                .background(CWColors.Danger.copy(alpha = 0.15f))
                                .clickable {
                                    playbackRepository.stopSleepTimer()
                                    isSleepTimerOpen = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Turn Off Timer",
                                    style = CWTypography.AppTypography.bodyLarge,
                                    color = CWColors.Danger,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$remLabel remaining",
                                    style = CWTypography.TechTelemetry,
                                    color = CWColors.TextSecondary
                                )
                            }
                            CWTechnicalBadge(text = "STOP", textColor = CWColors.Danger)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    listOf(15, 30, 45, 60, 90).forEach { mins ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                                .clickable {
                                    playbackRepository.startSleepTimer(mins)
                                    isSleepTimerOpen = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$mins Minutes",
                                style = CWTypography.AppTypography.bodyLarge,
                                color = CWColors.TextPrimary
                            )
                            CWTechnicalBadge(text = "+$mins min")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isSleepTimerOpen = false }) {
                    Text("CLOSE", style = CWTypography.TechBadge, color = CWColors.AccentCyan)
                }
            },
            containerColor = CWColors.SurfaceElevated
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
