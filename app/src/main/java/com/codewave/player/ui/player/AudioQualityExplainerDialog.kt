package com.codewave.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.Track

@Composable
fun AudioQualityExplainerDialog(
    track: Track,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = null,
                    tint = CWColors.AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "AUDIO ARCHITECTURE & QUALITY",
                    style = CWTypography.TechInspectorHeader
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Current Track Telemetry
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                        .background(CWColors.SurfacePrimary)
                        .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "CURRENT STREAM TELEMETRY",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextSecondary
                        )
                        Text(
                            text = "${track.format.displayName} · ${track.bitDepth?.let { "$it-Bit / " } ?: ""}${track.sampleRate / 1000.0} kHz",
                            style = CWTypography.AppTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CWColors.AccentCyan
                        )
                        if (track.bitrateKbps > 0) {
                            Text(
                                text = "Bitrate: ${track.bitrateKbps} kbps · Lossless: ${if (track.isLossless) "YES" else "NO"}",
                                style = CWTypography.TechTelemetry,
                                color = CWColors.TextPrimary
                            )
                        }
                    }
                }

                // Educational Items
                ExplainerSection(
                    icon = Icons.Default.HighQuality,
                    title = "HI-RES AUDIO CERTIFIED",
                    badge = if (track.isHiRes) "ACTIVE" else "STANDARD",
                    badgeColor = if (track.isHiRes) CWColors.Warning else CWColors.TextTertiary,
                    description = "Hi-Res music reproduces audio sampled higher than CD quality (44.1kHz / 16-bit). Standard recordings cannot capture ultrasonic harmonics and high dynamic range present in studio 24-bit/96kHz+ masters."
                )

                ExplainerSection(
                    icon = Icons.Default.GraphicEq,
                    title = "LOSSLESS BIT-PERFECT ENCODING",
                    badge = if (track.isLossless) "BIT-PERFECT" else "COMPRESSED",
                    badgeColor = if (track.isLossless) CWColors.AccentCyan else CWColors.TextTertiary,
                    description = "Lossless formats like FLAC and ALAC preserve 100% of original master acoustic data without the lossy discard methods of MP3 or AAC."
                )

                ExplainerSection(
                    icon = Icons.Default.Speed,
                    title = "CODEWAVE 32-BIT FLOAT DSP",
                    badge = "ACTIVE",
                    badgeColor = CWColors.Success,
                    description = "Audio is routed through Android DynamicsProcessing with 32-bit floating point precision, providing zero-clipping headroom and instant parametric equalization."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", style = CWTypography.TechBadge, color = CWColors.AccentCyan)
            }
        },
        containerColor = CWColors.SurfaceElevated,
        shape = RoundedCornerShape(CWShapes.RadiusLarge)
    )
}

@Composable
private fun ExplainerSection(
    icon: ImageVector,
    title: String,
    badge: String,
    badgeColor: androidx.compose.ui.graphics.Color,
    description: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CWColors.AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = CWTypography.TechBadge,
                    color = CWColors.TextPrimary
                )
            }
            CWTechnicalBadge(text = badge, textColor = badgeColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = CWTypography.AppTypography.bodyMedium,
            color = CWColors.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
