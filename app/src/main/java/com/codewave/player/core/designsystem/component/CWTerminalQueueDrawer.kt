package com.codewave.player.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.QueueWorkspace
import com.codewave.player.core.model.Track

enum class TerminalTab {
    QUEUE,
    RELATED,
    ABOUT
}

@Composable
fun CWTerminalQueueDrawer(
    queue: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackClick: (Track) -> Unit,
    onTrackOptions: ((Track) -> Unit)? = null,
    onClose: () -> Unit = {},
    isExpanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    onOpenFullQueue: (() -> Unit)? = null,
    workspaces: List<QueueWorkspace> = emptyList(),
    viewingWorkspaceId: String = "main",
    onSelectWorkspace: (String) -> Unit = {},
    onCreateWorkspace: (String) -> Unit = {},
    onDeleteWorkspace: (String) -> Unit = {},
    onClearWorkspace: (String) -> Unit = {},
    onPlayWorkspace: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(TerminalTab.QUEUE) }
    var internalExpanded by remember { mutableStateOf(false) }
    var showCreateWorkspaceDialog by remember { mutableStateOf(false) }
    var newWorkspaceName by remember { mutableStateOf("") }

    val activeViewingWorkspace = workspaces.find { it.id == viewingWorkspaceId } ?: workspaces.firstOrNull()
    val effectiveQueue = if (workspaces.isNotEmpty()) (activeViewingWorkspace?.tracks ?: queue) else queue

    val effectiveExpanded = if (onToggleExpand != null) isExpanded else internalExpanded
    val toggleExpand: () -> Unit = {
        if (onToggleExpand != null) {
            onToggleExpand()
        } else {
            internalExpanded = !internalExpanded
        }
    }

    val contentHeight by animateDpAsState(
        targetValue = if (effectiveExpanded) 380.dp else 145.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "TerminalQueueContentHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF10141B))
            .border(1.dp, Color(0xFF21262D), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Panel Header: Tabs & Close Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .border(
                        width = 0.5.dp,
                        color = Color(0xFF21262D),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tabs: Queue | Related | About
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val queueLabel = if (queue.isNotEmpty()) "Queue (${queue.size})" else "Queue"
                    TerminalTabItem(
                        title = queueLabel,
                        icon = Icons.Outlined.Menu,
                        isSelected = selectedTab == TerminalTab.QUEUE,
                        onClick = { selectedTab = TerminalTab.QUEUE }
                    )
                    TerminalTabItem(
                        title = "Related",
                        isSelected = selectedTab == TerminalTab.RELATED,
                        onClick = { selectedTab = TerminalTab.RELATED }
                    )
                    TerminalTabItem(
                        title = "About",
                        isSelected = selectedTab == TerminalTab.ABOUT,
                        onClick = { selectedTab = TerminalTab.ABOUT }
                    )
                }

                // Header Action Buttons: Expand/Collapse & Close
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Expand / Collapse Drawer Button
                    IconButton(
                        onClick = toggleExpand,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (effectiveExpanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                            contentDescription = if (effectiveExpanded) "Collapse Queue" else "Expand Queue",
                            tint = if (effectiveExpanded) CWColors.AccentCyan else Color(0xFF8B949E),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    if (onOpenFullQueue != null) {
                        Spacer(modifier = Modifier.width(2.dp))
                        IconButton(
                            onClick = onOpenFullQueue,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Full Screen Queue",
                                tint = Color(0xFF8B949E),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Close / Collapse Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Terminal",
                            tint = Color(0xFF8B949E),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                TerminalTab.QUEUE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(contentHeight)
                    ) {
                        // Workspaces Editor Tabs Row (VS Code Tabs)
                        if (workspaces.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0D1117))
                                    .border(0.5.dp, Color(0xFF21262D))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(workspaces, key = { it.id }) { ws ->
                                    val isSelected = ws.id == (activeViewingWorkspace?.id ?: "main")
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) Color(0xFF1F242C) else Color(0xFF161B22))
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) CWColors.AccentCyan else Color(0xFF30363D),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable { onSelectWorkspace(ws.id) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (ws.isPlaybackActive) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(CWColors.AccentCyan)
                                            )
                                        }
                                        Text(
                                            text = "${ws.name} (${ws.tracks.size})",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) CWColors.AccentCyan else Color(0xFF8B949E)
                                        )
                                        if (ws.id != "main") {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close Tab",
                                                tint = Color(0xFF6E7681),
                                                modifier = Modifier
                                                    .size(11.dp)
                                                    .clickable { onDeleteWorkspace(ws.id) }
                                            )
                                        }
                                    }
                                }

                                // [+] Add New Workspace Tab
                                item {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF161B22))
                                            .border(0.5.dp, Color(0xFF30363D), RoundedCornerShape(4.dp))
                                            .clickable {
                                                newWorkspaceName = "QUEUE ${workspaces.size + 1}"
                                                showCreateWorkspaceDialog = true
                                            }
                                            .padding(horizontal = 7.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New Workspace",
                                            tint = CWColors.AccentCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Secondary Workspace Action Prompt (if viewing non-playback workspace)
                        if (activeViewingWorkspace != null && !activeViewingWorkspace.isPlaybackActive && effectiveQueue.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x1F388BFD))
                                    .border(0.5.dp, Color(0xFF388BFD).copy(alpha = 0.3f))
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Viewing: ${activeViewingWorkspace.name}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF58A6FF)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(CWColors.AccentCyan)
                                            .clickable { onPlayWorkspace(activeViewingWorkspace.id) }
                                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color(0xFF0D1117),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "PLAY",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0D1117)
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF21262D))
                                            .clickable { onClearWorkspace(activeViewingWorkspace.id) }
                                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    ) {
                                        Text(
                                            text = "CLEAR",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = Color(0xFFFF8A65)
                                        )
                                    }
                                }
                            }
                        }

                        // Queue list with code line numbers
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (effectiveQueue.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "// Workspace '${activeViewingWorkspace?.name ?: "Queue"}' is empty",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF6E7681)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    itemsIndexed(effectiveQueue, key = { index, item -> "${item.id}_${index}_${activeViewingWorkspace?.id ?: "main"}" }) { index, track ->
                                        val isCurrent = track.id == currentTrack?.id
                                        TerminalQueueRow(
                                            index = index + 1,
                                            track = track,
                                            isCurrent = isCurrent && (activeViewingWorkspace?.isPlaybackActive != false),
                                            isPlaying = isPlaying && isCurrent && (activeViewingWorkspace?.isPlaybackActive != false),
                                            onClick = {
                                                if (activeViewingWorkspace?.isPlaybackActive != false) {
                                                    onTrackClick(track)
                                                } else {
                                                    onPlayWorkspace(activeViewingWorkspace.id)
                                                }
                                            },
                                            onOptionsClick = { onTrackOptions?.invoke(track) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                TerminalTab.RELATED -> {
                    // Related artist or album info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(contentHeight)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "// ARTIST DISCOVERY",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CWColors.AccentCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Artist: ${currentTrack?.artist ?: "Unknown"}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFC9D1D9)
                        )
                        Text(
                            text = "Album: ${currentTrack?.album ?: "Unknown"}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF8B949E)
                        )
                        Text(
                            text = "Format: ${currentTrack?.format?.displayName?.uppercase() ?: "AUDIO"} • ${currentTrack?.technicalSummary ?: ""}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF58A6FF)
                        )
                    }
                }
                TerminalTab.ABOUT -> {
                    // Technical about track
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(contentHeight)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "// FILE TELEMETRY",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CWColors.AccentCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bit depth: ${if (currentTrack?.isHiRes == true) "24-bit / Hi-Res" else if (currentTrack?.isLossless == true) "16-bit / Lossless" else "Lossy 320kbps"}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFC9D1D9)
                        )
                        Text(
                            text = "Sample Rate: ${currentTrack?.sampleRate?.let { "${it / 1000} kHz" } ?: "44.1 kHz"}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF8B949E)
                        )
                        Text(
                            text = "Engine: ViperFX DSP & ExoPlayer Core",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF58A6FF)
                        )
                    }
                }
            }

            // Bottom Terminal Prompt Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C1015))
                    .border(0.5.dp, Color(0xFF21262D))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (effectiveExpanded) ">_ QUEUE: ${queue.size} TRACKS" else ">_",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = CWColors.AccentCyan
                )

                Text(
                    text = "GOOD SONGS. BETTER DAYS.",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 8.5.sp,
                    letterSpacing = 1.2.sp,
                    color = Color(0xFF6E7681)
                )
            }
        }
    }

    if (showCreateWorkspaceDialog) {
        AlertDialog(
            onDismissRequest = { showCreateWorkspaceDialog = false },
            title = {
                Text(
                    text = "NEW QUEUE WORKSPACE",
                    style = CWTypography.TechBadge,
                    color = CWColors.AccentCyan,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Create an independent queue workspace tab (e.g. RADIO, SCRATCHPAD, FOCUS).",
                        style = CWTypography.AppTypography.bodySmall,
                        color = Color(0xFF8B949E)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newWorkspaceName,
                        onValueChange = { newWorkspaceName = it },
                        placeholder = { Text("Workspace name...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onCreateWorkspace(newWorkspaceName)
                    showCreateWorkspaceDialog = false
                }) {
                    Text("CREATE", color = CWColors.AccentCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateWorkspaceDialog = false }) {
                    Text("CANCEL", color = Color(0xFF8B949E))
                }
            }
        )
    }
}

@Composable
private fun TerminalTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) CWColors.AccentCyan else Color(0xFF8B949E),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = title,
                fontFamily = FontFamily.Default,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.5.sp,
                color = if (isSelected) Color(0xFFF0F6FC) else Color(0xFF8B949E)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Blue Underline Indicator
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(1.8.dp)
                .background(if (isSelected) CWColors.AccentCyan else Color.Transparent)
        )
    }
}

@Composable
private fun TerminalQueueRow(
    index: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isCurrent) Color(0x1F388BFD) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Line Number in Monospace e.g. "01", "02"
        val lineNumStr = String.format("%02d", index)
        Text(
            text = lineNumStr,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = if (isCurrent) CWColors.AccentCyan else Color(0xFF484F58),
            modifier = Modifier.width(22.dp)
        )

        // Micro-EQ indicator for current playing track
        if (isCurrent) {
            MicroEqBars(
                isPlaying = isPlaying,
                modifier = Modifier
                    .size(width = 12.dp, height = 11.dp)
                    .padding(end = 4.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Track Title
        Text(
            text = track.title,
            fontFamily = FontFamily.Default,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 12.sp,
            color = if (isCurrent) Color(0xFF58A6FF) else Color(0xFFC9D1D9),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Monospace Duration
        Text(
            text = track.durationFormatted,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.5.sp,
            color = Color(0xFF8B949E)
        )

        // Track Context Menu Options Trigger
        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = Color(0xFF6E7681),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun MicroEqBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MicroEq")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val bars = listOf(h1, h2, h3)
        bars.forEach { factor ->
            val finalFactor = if (isPlaying) factor else 0.4f
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height((11 * finalFactor).dp)
                    .background(CWColors.AccentCyan)
            )
        }
    }
}
