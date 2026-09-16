package com.codewave.player.ui.ota

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
            AnimatedContent(
                targetState = status,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ota_dialog_content"
            ) { currentStatus ->
                when (currentStatus) {
                    is UpdateStatus.UpdateAvailable -> {
                        UpdateAvailableContent(
                            info = currentStatus.info
                        )
                    }
                    is UpdateStatus.Downloading -> {
                        DownloadingContent(
                            progressPercent = currentStatus.progressPercent,
                            downloadedMb = currentStatus.downloadedMb,
                            totalMb = currentStatus.totalMb
                        )
                    }
                    is UpdateStatus.ReadyToInstall -> {
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
            Column {
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
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "PACKAGE SIZE",
                    style = CWTypography.TechBadge,
                    color = CWColors.TextTertiary,
                    fontSize = 9.sp
                )
                Text(
                    text = String.format(Locale.US, "%.1f MB", info.apkSizeMb),
                    style = CWTypography.TechTelemetry,
                    color = CWColors.AccentCyan,
                    fontWeight = FontWeight.Bold
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
    totalMb: Double
) {
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

        // Progress Bar
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
                Text(
                    text = "PROGRESS: $progressPercent%",
                    style = CWTypography.TechBadge,
                    color = CWColors.AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = String.format(Locale.US, "%.1f / %.1f MB", downloadedMb, totalMb),
                    style = CWTypography.TechTelemetry,
                    color = CWColors.TextSecondary,
                    fontSize = 11.sp
                )
            }

            val animatedProgress = (progressPercent / 100f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CWColors.AccentCyan,
                trackColor = CWColors.BorderSubtle,
                strokeCap = StrokeCap.Round
            )
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
