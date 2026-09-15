package com.codewave.player.ui.player

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codewave.player.core.data.PlaybackRepository
import com.codewave.player.core.designsystem.component.CWQualityBadge
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.component.TrackInspectorSheet
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.PlaybackState
import com.codewave.player.core.model.RepeatMode
import com.codewave.player.core.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackRepository: PlaybackRepository,
    onCollapse: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by playbackRepository.playbackState.collectAsState()
    val track = state.currentTrack ?: return

    var isInspectorOpen by remember { mutableStateOf(false) }
    var isQueueOpen by remember { mutableStateOf(false) }
    var isSleepTimerOpen by remember { mutableStateOf(false) }
    var isSpeedSelectorOpen by remember { mutableStateOf(false) }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragProgressMs by remember { mutableFloatStateOf(0f) }

    val currentPosition = if (isDraggingSlider) dragProgressMs.toLong() else state.positionMs
    val duration = state.durationMs.coerceAtLeast(1L)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
            .padding(horizontal = 24.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
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

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PLAYING FROM LIBRARY",
                    style = CWTypography.TechBadge,
                    color = CWColors.TextSecondary
                )
                Text(
                    text = track.album,
                    style = CWTypography.AppTypography.bodyMedium,
                    color = CWColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

        Spacer(modifier = Modifier.height(16.dp))

        // Center: Album Art Card (Square)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
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
                    Text(
                        text = "> CW_",
                        style = CWTypography.TechInspectorHeader,
                        color = CWColors.AccentCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CODEWAVE WORKSTATION",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Song Title, Artist & Favorite Button
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
                    text = track.artist,
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
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Quality & Technical Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (track.isHiRes) {
                CWQualityBadge(text = "HI-RES", isHiRes = true)
            } else if (track.isLossless) {
                CWQualityBadge(text = "LOSSLESS", isLossless = true)
            }
            CWTechnicalBadge(text = "${track.format.displayName} · ${track.bitDepth?.let { "$it-BIT / " } ?: ""}${track.sampleRate / 1000}kHz")
            if (track.bitrateKbps > 0) {
                CWTechnicalBadge(text = "${track.bitrateKbps} kbps")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrubber / Slider
        Slider(
            value = currentPosition.toFloat(),
            onValueChange = {
                isDraggingSlider = true
                dragProgressMs = it
            },
            onValueChangeFinished = {
                playbackRepository.seekTo(dragProgressMs.toLong())
                isDraggingSlider = false
            },
            valueRange = 0f..duration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = CWColors.AccentCyan,
                activeTrackColor = CWColors.AccentCyan,
                inactiveTrackColor = CWColors.SurfaceOverlay
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Timestamp row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPosition),
                style = CWTypography.TechTelemetry
            )
            Text(
                text = formatDuration(duration),
                style = CWTypography.TechTelemetry
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playback Controls Row (Shuffle, Prev, Play/Pause, Next, Repeat)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle Button
            IconButton(onClick = { playbackRepository.setShuffle(!state.shuffleMode) }) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.shuffleMode) CWColors.AccentCyan else CWColors.TextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Previous
            IconButton(
                onClick = { playbackRepository.skipPrevious() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = CWColors.TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Play / Pause Floating Circle
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(CWColors.AccentCyan)
                    .clickable { playbackRepository.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = CWColors.Background,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Next
            IconButton(
                onClick = { playbackRepository.skipNext() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = CWColors.TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Repeat Mode Button
            IconButton(onClick = {
                val nextMode = when (state.repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                }
                playbackRepository.setRepeatMode(nextMode)
            }) {
                Icon(
                    imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (state.repeatMode != RepeatMode.OFF) CWColors.AccentCyan else CWColors.TextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Tool Bar: Speed, Sleep Timer, Queue
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { isSpeedSelectorOpen = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Playback Speed",
                        tint = CWColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${state.playbackSpeed}x",
                        style = CWTypography.TechTelemetry
                    )
                }
            }

            IconButton(onClick = { isSleepTimerOpen = true }) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Sleep Timer",
                    tint = CWColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = { isQueueOpen = true }) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = "Queue",
                    tint = CWColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
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
                LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
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
            title = { Text("Playback Speed", style = CWTypography.TechBadge, color = CWColors.AccentCyan) },
            text = {
                Column {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playbackRepository.setPlaybackSpeed(speed)
                                    isSpeedSelectorOpen = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${speed}x", style = CWTypography.AppTypography.bodyLarge, color = CWColors.TextPrimary)
                            if (state.playbackSpeed == speed) {
                                Text(text = "ACTIVE", style = CWTypography.TechBadge, color = CWColors.AccentCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isSpeedSelectorOpen = false }) {
                    Text("Close", color = CWColors.AccentCyan)
                }
            },
            containerColor = CWColors.SurfaceElevated
        )
    }

    // Dialog: Sleep Timer
    if (isSleepTimerOpen) {
        AlertDialog(
            onDismissRequest = { isSleepTimerOpen = false },
            title = { Text("Sleep Timer", style = CWTypography.TechBadge, color = CWColors.AccentCyan) },
            text = {
                Column {
                    listOf(15, 30, 45, 60, 90).forEach { mins ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isSleepTimerOpen = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(text = "$mins Minutes", style = CWTypography.AppTypography.bodyLarge, color = CWColors.TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isSleepTimerOpen = false }) {
                    Text("Cancel", color = CWColors.AccentCyan)
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
