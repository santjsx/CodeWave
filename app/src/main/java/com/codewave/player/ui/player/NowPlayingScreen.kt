package com.codewave.player.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.data.PlaybackRepository
import com.codewave.player.core.designsystem.component.CWAudioWaveformBox
import com.codewave.player.core.designsystem.component.CWPlayTimeBar
import com.codewave.player.core.designsystem.component.CWQueueSheet
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.component.CWTerminalQueueDrawer
import com.codewave.player.core.designsystem.component.CWVinylRecordArt
import com.codewave.player.core.designsystem.component.TrackInspectorSheet
import com.codewave.player.core.designsystem.component.tactileClickable
import com.codewave.player.core.designsystem.component.tactilePress
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.media.LrcParser
import com.codewave.player.core.media.LyricsResult
import com.codewave.player.core.model.RepeatMode
import com.codewave.player.core.model.Track
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
    onOpenTrackOptions: ((Track) -> Unit)? = null,
    onNavigateToEqualizer: (() -> Unit)? = null,
    onNavigateToSearch: (() -> Unit)? = null,
    onNavigateToLibrary: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler { onCollapse() }

    val state by playbackRepository.playbackState.collectAsState()
    val sleepTimerRemainingMs by playbackRepository.sleepTimerRemainingMs.collectAsState()
    val track = state.currentTrack ?: return

    var centerView by remember { mutableStateOf(NowPlayingCenterView.ARTWORK) }
    var isInspectorOpen by remember { mutableStateOf(false) }
    var isQualityExplainerOpen by remember { mutableStateOf(false) }
    var isSleepTimerOpen by remember { mutableStateOf(false) }
    var isSpeedSelectorOpen by remember { mutableStateOf(false) }
    var isQueueExpanded by remember { mutableStateOf(false) }
    var isQueueSheetOpen by remember { mutableStateOf(false) }

    val duration = state.durationMs.coerceAtLeast(1L)
    var lyricsResult by remember(track.id) { mutableStateOf<LyricsResult>(LyricsResult.Loading) }

    LaunchedEffect(track.id, track.path) {
        lyricsResult = LyricsResult.Loading
        withContext(Dispatchers.IO) {
            val res = LrcParser.loadLyricsForTrack(track.path)
            lyricsResult = res
        }
    }

    val rootInteractionSource = remember { MutableInteractionSource() }
    val editorScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
            .clickable(interactionSource = rootInteractionSource, indication = null) {}
            .verticalScroll(editorScrollState)
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        // 1. Top Bar: Collapse Arrow, Editor Tabs ("Now Playing" / "Lyrics"), and Options Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.size(38.dp).tactilePress()
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = Color(0xFFC9D1D9),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Editor Tabs Strip
            Row(verticalAlignment = Alignment.CenterVertically) {
                VsCodeEditorTab(
                    title = "Now Playing",
                    isSelected = centerView == NowPlayingCenterView.ARTWORK,
                    onClick = { centerView = NowPlayingCenterView.ARTWORK }
                )
                Spacer(modifier = Modifier.width(6.dp))
                VsCodeEditorTab(
                    title = "Lyrics",
                    isSelected = centerView == NowPlayingCenterView.LYRICS,
                    onClick = { centerView = NowPlayingCenterView.LYRICS }
                )
            }

            IconButton(
                onClick = { onOpenTrackOptions?.invoke(track) },
                modifier = Modifier.size(38.dp).tactilePress()
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color(0xFFC9D1D9),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 2. Center Content: Vinyl Record Peek-Out OR Synced Lyrics Code View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = centerView, label = "np_center_crossfade") { view ->
                when (view) {
                    NowPlayingCenterView.ARTWORK -> {
                        CWVinylRecordArt(
                            albumArtUri = track.albumArtUri,
                            isPlaying = state.isPlaying,
                            sleeveSize = 200,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                        NowPlayingCenterView.LYRICS -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp)
                            ) {
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Track Metadata & Favorite Blue Heart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFFF0F6FC),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = Color(0xFF8B949E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.album,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.5.sp,
                        color = Color(0xFF6E7681),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite(track) },
                    modifier = Modifier.size(38.dp).tactilePress()
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (track.isFavorite) Color(0xFF388BFD) else Color(0xFF6E7681),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4. Progress Seek Bar & Monospace Timestamps
            CWPlayTimeBar(
                currentPositionMs = state.positionMs,
                durationMs = duration,
                onSeek = { targetMs -> playbackRepository.seekTo(targetMs) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(state.positionMs),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF8B949E)
                )
                Text(
                    text = formatDuration(duration),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF8B949E)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Playback Transport Controls (Shuffle, Prev, Play/Pause Circle with Neon Ring, Next, Repeat)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(
                    onClick = { playbackRepository.setShuffle(!state.shuffleMode) },
                    modifier = Modifier.size(38.dp).tactilePress()
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.shuffleMode) CWColors.AccentCyan else Color(0xFF6E7681),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Skip Previous
                IconButton(
                    onClick = { playbackRepository.skipPrevious() },
                    modifier = Modifier.size(44.dp).tactilePress()
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color(0xFFE6EDF3),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Circular Play/Pause Button with Glowing Electric Blue Ring
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161B22))
                        .border(2.dp, Color(0xFF388BFD), CircleShape)
                        .tactileClickable(role = Role.Button, targetScale = 0.93f) {
                            playbackRepository.togglePlayPause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color(0xFF388BFD),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Skip Next
                IconButton(
                    onClick = { playbackRepository.skipNext() },
                    modifier = Modifier.size(44.dp).tactilePress()
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color(0xFFE6EDF3),
                        modifier = Modifier.size(28.dp)
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
                    modifier = Modifier.size(38.dp).tactilePress()
                ) {
                    Icon(
                        imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF) CWColors.AccentCyan else Color(0xFF6E7681),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Modular Quick-Action Tiles: Lossless Specs, Equalizer, Sleep Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tile 1: Lossless Audio Specs
                ModularActionTile(
                    icon = Icons.Default.GraphicEq,
                    title = if (track.isHiRes) "Hi-Res" else if (track.isLossless) "Lossless" else "Audio",
                    subtitle = "${track.bitDepth?.let { "$it-BIT • " } ?: ""}${track.sampleRate / 1000}kHz",
                    onClick = { isQualityExplainerOpen = true },
                    modifier = Modifier.weight(1f)
                )

                // Tile 2: Equalizer
                ModularActionTile(
                    icon = Icons.Default.Tune,
                    title = "Equalizer",
                    subtitle = "Viper DSP",
                    onClick = { onNavigateToEqualizer?.invoke() },
                    modifier = Modifier.weight(1f)
                )

                // Tile 3: Sleep Timer
                val remainingMs = sleepTimerRemainingMs
                val isTimerActive = remainingMs > 0L
                val timerSubtitle = if (isTimerActive) {
                    val remMins = (remainingMs + 59999L) / 60000L
                    "${remMins}m left"
                } else {
                    "Off"
                }
                ModularActionTile(
                    icon = Icons.Default.Timer,
                    title = "Timer",
                    subtitle = timerSubtitle,
                    isActive = isTimerActive,
                    onClick = { isSleepTimerOpen = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Waveform Visualizer Box with Developer Comment Header (Real System Volume)
            CWAudioWaveformBox(
                isPlaying = state.isPlaying,
                volumePercent = state.volumePercent,
                onVolumeChange = { newPercent -> playbackRepository.setVolumePercent(newPercent) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 8. VS Code Embedded Terminal / Output Drawer (Queue with Code Line Numbers & Expand Support)
            CWTerminalQueueDrawer(
                queue = state.queue,
                currentTrack = track,
                isPlaying = state.isPlaying,
                onTrackClick = { t -> playbackRepository.playTrack(t, state.queue) },
                onTrackOptions = { t -> onOpenTrackOptions?.invoke(t) },
                isExpanded = isQueueExpanded,
                onToggleExpand = { isQueueExpanded = !isQueueExpanded },
                onOpenFullQueue = { isQueueSheetOpen = true },
                onClose = {
                    if (isQueueExpanded) {
                        isQueueExpanded = false
                    } else {
                        onCollapse()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
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

    // Modal: Full Queue Sheet
    if (isQueueSheetOpen) {
        CWQueueSheet(
            queue = state.queue,
            currentTrack = track,
            isPlaying = state.isPlaying,
            onTrackClick = { t -> playbackRepository.playTrack(t, state.queue) },
            onTrackOptions = { t -> onOpenTrackOptions?.invoke(t) },
            onDismiss = { isQueueSheetOpen = false }
        )
    }

    // Modal: Audio Quality Explainer
    if (isQualityExplainerOpen) {
        AudioQualityExplainerDialog(
            track = track,
            onDismiss = { isQualityExplainerOpen = false }
        )
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
                    if (remainingMs > 0L) {
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

@Composable
private fun VsCodeEditorTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontFamily = FontFamily.Default,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 12.5.sp,
            color = if (isSelected) Color(0xFFF0F6FC) else Color(0xFF8B949E)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(2.dp)
                .background(if (isSelected) Color(0xFF388BFD) else Color.Transparent)
        )
    }
}

@Composable
private fun ModularActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161B22))
            .border(
                1.dp,
                if (isActive) Color(0xFF388BFD) else Color(0xFF21262D),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) Color(0xFF388BFD) else Color(0xFF58A6FF),
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = title,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = Color(0xFFF0F6FC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    fontSize = 8.5.sp,
                    color = Color(0xFF8B949E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
