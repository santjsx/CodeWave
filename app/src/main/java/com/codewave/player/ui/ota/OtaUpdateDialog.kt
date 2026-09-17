package com.codewave.player.ui.ota

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.component.tactilePress
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.ota.UpdateInfo
import com.codewave.player.core.ota.UpdateStatus
import java.io.File
import java.util.Locale

@Composable
fun OtaUpdateDialog(
    status: UpdateStatus,
    onDownload: (UpdateInfo) -> Unit,
    onInstallNow: (File) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            // Prevent accidental dismissal only while actively downloading
            if (status !is UpdateStatus.Downloading) {
                onDismiss()
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CWColors.AccentCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (status) {
                                is UpdateStatus.ReadyToInstall -> Icons.Default.CheckCircle
                                is UpdateStatus.Downloading -> Icons.Default.Download
                                else -> Icons.Default.SystemUpdate
                            },
                            contentDescription = null,
                            tint = when (status) {
                                is UpdateStatus.ReadyToInstall -> CWColors.Success
                                else -> CWColors.AccentCyan
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = when (status) {
                                is UpdateStatus.ReadyToInstall -> "UPDATE READY"
                                is UpdateStatus.Downloading -> "DOWNLOADING OTA"
                                else -> "NEW UPDATE AVAILABLE"
                            },
                            style = CWTypography.TechBadge,
                            color = CWColors.TextPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "CodeWave System Updater",
                            style = CWTypography.AppTypography.bodySmall,
                            color = CWColors.TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (status is UpdateStatus.UpdateAvailable) {
                    CWTechnicalBadge(
                        text = "v${status.info.latestVersion}",
                        textColor = CWColors.AccentCyan
                    )
                }
            }
        },
        text = {
            val stage = when (status) {
                is UpdateStatus.UpdateAvailable -> 0
                is UpdateStatus.Downloading -> 1
                is UpdateStatus.ReadyToInstall -> 2
                else -> -1
            }

            AnimatedContent(
                targetState = stage,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "ota_dialog_content"
            ) { currentStage ->
                when (currentStage) {
                    0 -> {
                        val info = (status as? UpdateStatus.UpdateAvailable)?.info
                        if (info != null) {
                            UpdateAvailableContent(
                                info = info
                            )
                        }
                    }
                    1 -> {
                        val downloading = status as? UpdateStatus.Downloading
                        DownloadingContent(
                            progressPercent = downloading?.progressPercent ?: 0,
                            downloadedMb = downloading?.downloadedMb ?: 0.0,
                            totalMb = downloading?.totalMb ?: 4.5,
                            bytesPerSec = downloading?.bytesPerSec ?: 0L
                        )
                    }
                    2 -> {
                        ReadyToInstallContent()
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            when (status) {
                is UpdateStatus.UpdateAvailable -> {
                    Button(
                        onClick = { onDownload(status.info) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CWColors.AccentCyan,
                            contentColor = CWColors.Background
                        ),
                        shape = RoundedCornerShape(CWShapes.RadiusSmall),
                        modifier = Modifier.tactilePress()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DOWNLOAD",
                            style = CWTypography.TechBadge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                is UpdateStatus.ReadyToInstall -> {
                    Button(
                        onClick = { onInstallNow(status.apkFile) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CWColors.Success,
                            contentColor = CWColors.Background
                        ),
                        shape = RoundedCornerShape(CWShapes.RadiusSmall),
                        modifier = Modifier.tactilePress()
                    ) {
                        Icon(
                            imageVector = Icons.Default.InstallMobile,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "INSTALL NOW",
                            style = CWTypography.TechBadge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> Unit
            }
        },
        dismissButton = {
            when (status) {
                is UpdateStatus.UpdateAvailable -> {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.tactilePress()
                    ) {
                        Text(
                            text = "LATER",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextSecondary
                        )
                    }
                }
                is UpdateStatus.ReadyToInstall -> {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.tactilePress()
                    ) {
                        Text(
                            text = "INSTALL LATER",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextSecondary
                        )
                    }
                }
                is UpdateStatus.Downloading -> {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.tactilePress()
                    ) {
                        Text(
                            text = "CANCEL",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextTertiary
                        )
                    }
                }
                else -> Unit
            }
        },
        containerColor = CWColors.SurfaceOverlay,
        shape = RoundedCornerShape(CWShapes.RadiusLarge)
    )
}

@Composable
private fun UpdateAvailableContent(info: UpdateInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Version info banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                .background(CWColors.SurfaceElevated)
                .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusSmall))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = "RELEASE",
                    style = CWTypography.TechBadge,
                    color = CWColors.TextTertiary,
                    fontSize = 9.sp
                )
                Text(
                    text = info.releaseTitle.ifBlank { "CodeWave v${info.latestVersion}" },
                    style = CWTypography.AppTypography.bodyMedium,
                    color = CWColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "PACKAGE SIZE",
                    style = CWTypography.TechBadge,
                    color = CWColors.TextTertiary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = String.format(Locale.US, "%.1f MB", info.apkSizeMb),
                    style = CWTypography.TechTelemetry,
                    color = CWColors.AccentCyan,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Changelogs box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                .background(CWColors.SurfacePrimary)
                .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusSmall))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NewReleases,
                    contentDescription = null,
                    tint = CWColors.AccentCyan,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "WHAT'S NEW IN THIS VERSION",
                    style = CWTypography.TechBadge,
                    color = CWColors.AccentCyan,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = info.releaseNotes.ifBlank { "Performance improvements, UI refinements, and bug fixes." },
                    style = CWTypography.AppTypography.bodySmall,
                    color = CWColors.TextSecondary,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp
                )
            }
        }

        Text(
            text = "Direct verified APK delivery from CodeWave GitHub CDN.",
            style = CWTypography.TechTelemetry,
            color = CWColors.TextTertiary,
            fontSize = 9.5.sp
        )
    }
}

@Composable
private fun DownloadingContent(
    progressPercent: Int,
    downloadedMb: Double,
    totalMb: Double,
    bytesPerSec: Long = 0L
) {
    val targetProgress = (progressPercent / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing),
        label = "ota_download_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ota_anim")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ota_shimmer"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ota_pulse"
    )

    val speedText = when {
        bytesPerSec <= 0L && progressPercent < 100 -> "CONNECTING..."
        bytesPerSec < 1024 * 1024 -> String.format(Locale.US, "%.0f KB/s", bytesPerSec / 1024.0)
        else -> String.format(Locale.US, "%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Downloading the latest CodeWave release package...",
            style = CWTypography.AppTypography.bodyMedium,
            color = CWColors.TextSecondary
        )

        // Progress Chassis Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                .background(CWColors.SurfaceElevated)
                .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusSmall))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PROGRESS:",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = CWTypography.TechBadge,
                        color = CWColors.AccentCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = String.format(Locale.US, "%.1f / %.1f MB", downloadedMb, totalMb),
                    style = CWTypography.TechTelemetry,
                    color = CWColors.TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            // Specular Shimmer Animated Progress Bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                val totalWidth = size.width
                val barHeight = size.height
                val filledWidth = totalWidth * animatedProgress

                // Track background
                drawRoundRect(
                    color = Color(0xFF161B22),
                    size = size,
                    cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                )

                if (filledWidth > 0f) {
                    // Filled Neon Gradient
                    val fillBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00E5FF), // Cyan
                            Color(0xFF2979FF)  // Vivid Blue
                        ),
                        startX = 0f,
                        endX = totalWidth
                    )
                    drawRoundRect(
                        brush = fillBrush,
                        size = Size(filledWidth, barHeight),
                        cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                    )

                    // Sweeping Specular Light Wave
                    val shimmerWidth = (filledWidth * 0.5f).coerceIn(40.dp.toPx(), 140.dp.toPx())
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

                    // Glowing Head Pulse Dot at leading edge
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

            // Status ticker & Speed telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        progressPercent >= 100 -> "Verifying release package..."
                        downloadedMb > 0.05 -> "Streaming payload from CDN..."
                        else -> "Connecting to GitHub Releases..."
                    },
                    style = CWTypography.TechTelemetry,
                    color = CWColors.TextTertiary,
                    fontSize = 10.sp
                )
                Text(
                    text = speedText,
                    style = CWTypography.TechBadge,
                    color = CWColors.AccentCyan,
                    fontSize = 10.sp
                )
            }
        }

        Text(
            text = "Please keep CodeWave open while the package downloads.",
            style = CWTypography.TechTelemetry,
            color = CWColors.TextTertiary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ReadyToInstallContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                .background(CWColors.Success.copy(alpha = 0.1f))
                .border(1.dp, CWColors.Success.copy(alpha = 0.3f), RoundedCornerShape(CWShapes.RadiusSmall))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
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
            Column {
                Text(
                    text = "VERIFIED PACKAGE DOWNLOADED",
                    style = CWTypography.TechBadge,
                    color = CWColors.Success,
                    fontSize = 10.sp
                )
                Text(
                    text = "Integrity check passed. Ready to install.",
                    style = CWTypography.AppTypography.bodySmall,
                    color = CWColors.TextPrimary,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = "Tap 'INSTALL NOW' to open the Android system installer, or 'INSTALL LATER' to apply this update whenever you want from Settings.",
            style = CWTypography.AppTypography.bodyMedium,
            color = CWColors.TextSecondary,
            lineHeight = 20.sp
        )
    }
}
