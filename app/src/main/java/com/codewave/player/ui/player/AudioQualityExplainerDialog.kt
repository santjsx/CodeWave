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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.component.CWQualityBadge
import com.codewave.player.core.designsystem.component.tactilePress
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = null,
                        tint = CWColors.AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AUDIO SPECIFICATIONS",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                if (track.isHiRes) {
                    CWQualityBadge(text = "HI-RES", isHiRes = true)
                } else if (track.isLossless) {
                    CWQualityBadge(text = "LOSSLESS", isLossless = true)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Technical specs 2x3 telemetry grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                        .background(CWColors.SurfacePrimary)
                        .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpecCell(
                            label = "CONTAINER",
                            value = track.format.displayName,
                            modifier = Modifier.weight(1f)
                        )
                        SpecCell(
                            label = "ENCODING",
                            value = if (track.isLossless) "Bit-Perfect" else "Compressed",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpecCell(
                            label = "SAMPLE RATE",
                            value = if (track.sampleRate > 0) "${track.sampleRate / 1000.0} kHz" else "44.1 kHz",
                            modifier = Modifier.weight(1f)
                        )
                        SpecCell(
                            label = "BIT DEPTH",
                            value = track.bitDepth?.let { "${it}-Bit" } ?: "16-Bit",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpecCell(
                            label = "BITRATE",
                            value = if (track.bitrateKbps > 0) "${track.bitrateKbps} kbps" else "Variable",
                            modifier = Modifier.weight(1f)
                        )
                        SpecCell(
                            label = "AUDIO DSP",
                            value = "32-Bit Float",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Minimal status bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                        .background(CWColors.SurfaceElevated)
                        .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusSmall))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROCESSING HEADROOM",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "ZERO CLIPPING",
                        style = CWTypography.TechTelemetry,
                        color = CWColors.Success,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.tactilePress()
            ) {
                Text(
                    text = "DISMISS",
                    style = CWTypography.TechBadge,
                    color = CWColors.AccentCyan
                )
            }
        },
        containerColor = CWColors.SurfaceOverlay,
        shape = RoundedCornerShape(CWShapes.RadiusLarge)
    )
}

@Composable
private fun SpecCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = CWTypography.TechBadge,
            color = CWColors.TextTertiary,
            fontSize = 9.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = CWTypography.AppTypography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = CWColors.TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
    }
}
