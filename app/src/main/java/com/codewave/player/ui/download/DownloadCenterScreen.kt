package com.codewave.player.ui.download

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.DownloadStatus
import com.codewave.player.core.model.DownloadTask
import com.codewave.player.core.model.TargetAudioFormat

@Composable
fun DownloadCenterScreen(
    viewModel: DownloadViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val completedDownloads by viewModel.completedDownloads.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CWColors.SurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = CWColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "LOSSLESS DOWNLOAD CENTER",
                    style = CWTypography.AppTypography.headlineSmall,
                    color = CWColors.Success
                )
                Text(
                    text = "Lossless Audio Engine · 24-Bit / 96 kHz FLAC",
                    style = CWTypography.AppTypography.labelSmall,
                    color = CWColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Link Paste & Resolver Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                .background(CWColors.SurfacePrimary)
                .border(1.dp, CWColors.BorderFocus, RoundedCornerShape(CWShapes.RadiusMedium))
                .padding(12.dp)
        ) {
            Text(
                text = "RESOLVE FROM URL",
                style = CWTypography.TechBadge,
                color = CWColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = uiState.pastedLinkText,
                    onValueChange = { viewModel.onLinkTextChanged(it) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    placeholder = {
                        Text(
                            text = "Paste Spotify, Tidal, or YouTube link...",
                            style = CWTypography.AppTypography.labelSmall,
                            color = CWColors.TextTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = CWColors.Success,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CWColors.SurfaceElevated,
                        unfocusedContainerColor = CWColors.SurfaceElevated,
                        focusedTextColor = CWColors.TextPrimary,
                        unfocusedTextColor = CWColors.TextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { viewModel.resolvePastedLink() },
                    enabled = !uiState.isResolvingLink && uiState.pastedLinkText.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CWColors.Success,
                        contentColor = Color.Black
                    )
                ) {
                    if (uiState.isResolvingLink) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "Resolve", style = CWTypography.TechBadge)
                    }
                }
            }

            // Resolved Preview Card
            uiState.resolvedMetadata?.let { metadata ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CWColors.SurfaceElevated)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!metadata.artworkUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = metadata.artworkUrl,
                            contentDescription = metadata.title,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = metadata.title,
                            style = CWTypography.AppTypography.bodyMedium,
                            color = CWColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${metadata.artist} · ${metadata.sourceService}",
                            style = CWTypography.AppTypography.labelSmall,
                            color = CWColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            viewModel.downloadResolvedMetadata(TargetAudioFormat.FLAC)
                            Toast.makeText(context, "Downloading FLAC for '${metadata.title}'", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CWColors.AccentCyan,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(text = "Download FLAC", style = CWTypography.TechBadge)
                    }
                }
            }

            uiState.errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = CWTypography.AppTypography.labelSmall,
                    color = CWColors.Danger
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Tasks & Completed List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Active Section
            if (activeDownloads.isNotEmpty()) {
                item {
                    Text(
                        text = "ACTIVE DOWNLOADS (${activeDownloads.size})",
                        style = CWTypography.TechBadge,
                        color = CWColors.AccentCyan
                    )
                }

                items(activeDownloads, key = { it.id }) { task ->
                    ActiveDownloadCard(
                        task = task,
                        onCancel = { viewModel.cancelDownload(task.id) },
                        onRetry = { viewModel.retryDownload(task) }
                    )
                }
            }

            // Completed Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COMPLETED LOSSLESS FILES (${completedDownloads.size})",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextSecondary
                    )

                    if (completedDownloads.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearCompleted() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear Completed",
                                tint = CWColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (completedDownloads.isEmpty() && activeDownloads.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active or completed downloads.\nPaste a link above or tap download in Stream mode.",
                            style = CWTypography.AppTypography.bodyMedium,
                            color = CWColors.TextTertiary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(completedDownloads, key = { it.id }) { task ->
                    CompletedDownloadRow(
                        task = task,
                        onRemove = { viewModel.removeDownload(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveDownloadCard(
    task: DownloadTask,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
            .background(CWColors.SurfacePrimary)
            .border(1.dp, if (task.status == DownloadStatus.FAILED) CWColors.Danger.copy(alpha = 0.5f) else CWColors.BorderFocus, RoundedCornerShape(CWShapes.RadiusMedium))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(CWColors.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (!task.artworkUri.isNullOrBlank()) {
                    AsyncImage(
                        model = task.artworkUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = if (task.status == DownloadStatus.FAILED) CWColors.Danger else CWColors.AccentCyan
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = CWTypography.AppTypography.bodyMedium,
                    color = CWColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${task.artist} · ${task.targetFormat.name}",
                    style = CWTypography.AppTypography.labelSmall,
                    color = CWColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = CWColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (task.status == DownloadStatus.FAILED && !task.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = task.errorMessage,
                style = CWTypography.AppTypography.labelSmall,
                color = CWColors.Danger,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress bar with glowing cyan/green (or red if failed)
        LinearProgressIndicator(
            progress = { if (task.status == DownloadStatus.FAILED) 1f else task.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (task.status == DownloadStatus.FAILED) CWColors.Danger else CWColors.AccentCyan,
            trackColor = CWColors.SurfaceElevated
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (task.status == DownloadStatus.FAILED) "FAILED" else "${task.status.name} (${task.progressPercent}%)",
                style = CWTypography.TechTelemetry,
                color = when (task.status) {
                    DownloadStatus.DOWNLOADING -> CWColors.AccentCyan
                    DownloadStatus.TAGGING -> CWColors.Warning
                    DownloadStatus.FAILED -> CWColors.Danger
                    else -> CWColors.TextSecondary
                }
            )

            if (task.status == DownloadStatus.FAILED) {
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry Download",
                        tint = CWColors.AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = CWColors.Success,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.speedFormatted,
                        style = CWTypography.TechTelemetry,
                        color = CWColors.Success
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedDownloadRow(
    task: DownloadTask,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
            .background(CWColors.SurfacePrimary)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = CWColors.Success,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = CWTypography.AppTypography.bodyMedium,
                color = CWColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${task.artist} · ${task.album}",
                style = CWTypography.AppTypography.labelSmall,
                color = CWColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(CWColors.Success.copy(alpha = 0.12f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "FLAC 24-BIT",
                style = CWTypography.TechBadge,
                color = CWColors.Success
            )
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = CWColors.TextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
