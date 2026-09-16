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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
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
                        modifier = Modifier.size(22.dp)
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
                } else {
                    CWQualityBadge(text = track.format.displayName)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Plain-English Explainer Card ("What does this mean for you?")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                        .background(CWColors.SurfacePrimary)
                        .border(1.dp, CWColors.BorderFocus.copy(alpha = 0.5f), RoundedCornerShape(CWShapes.RadiusMedium))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(CWColors.AccentCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CWColors.AccentCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = if (track.isHiRes) "WHAT IS HI-RES AUDIO?"
                            else if (track.isLossless) "WHAT DOES LOSSLESS MEAN?"
                            else "COMPRESSED AUDIO FORMAT",
                            style = CWTypography.TechBadge,
                            color = CWColors.AccentCyan,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (track.isHiRes) {
                            "Hi-Res audio is studio-master quality exceeding standard CD resolution. It captures ultra-fine acoustic harmonics, room reverb, and high dynamic range so you hear music with lifelike presence."
                        } else if (track.isLossless) {
                            "Lossless audio preserves 100% of the original sound recorded in the studio without stripping away any frequencies. Unlike compressed MP3s, every instrument and vocal nuance is bit-for-bit identical to the master."
                        } else {
                            "Standard compressed audio saves file space by trimming frequencies outside typical human hearing. CodeWave's 32-bit floating-point audio engine renders it with maximum possible clarity and zero clipping."
                        },
                        style = CWTypography.AppTypography.bodyMedium,
                        color = CWColors.TextPrimary,
                        lineHeight = 20.sp
                    )
                }

                // 2. Visual Audio Quality Tier Ladder
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                        .background(CWColors.SurfacePrimary)
                        .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "AUDIO RESOLUTION TIER",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextTertiary,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QualityTierChip(
                            title = "LOSSY",
                            subtitle = "≤ 320 kbps",
                            isActive = !track.isLossless && !track.isHiRes,
                            modifier = Modifier.weight(1f)
                        )
                        QualityTierChip(
                            title = "LOSSLESS",
                            subtitle = "1,411 kbps",
                            isActive = track.isLossless && !track.isHiRes,
                            modifier = Modifier.weight(1f)
                        )
                        QualityTierChip(
                            title = "HI-RES",
                            subtitle = "Studio Master",
                            isActive = track.isHiRes,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. Technical Specs 2x3 Telemetry Grid
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
                            value = if (track.isLossless) "Bit-Perfect" else "Lossy Compressed",
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

                // 4. Processing Headroom & Fidelity Status
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CWColors.Success)
                        )
                        Text(
                            text = "HEADROOM MARGIN",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = "ZERO CLIPPING · 32-BIT FP",
                        style = CWTypography.TechTelemetry,
                        color = CWColors.Success,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
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
private fun QualityTierChip(
    title: String,
    subtitle: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CWShapes.RadiusSmall))
            .background(if (isActive) CWColors.AccentCyan.copy(alpha = 0.15f) else CWColors.SurfaceElevated)
            .border(
                1.dp,
                if (isActive) CWColors.AccentCyan else CWColors.BorderSubtle,
                RoundedCornerShape(CWShapes.RadiusSmall)
            )
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = CWColors.AccentCyan,
                        modifier = Modifier.size(10.dp)
                    )
                }
                Text(
                    text = title,
                    style = CWTypography.TechBadge,
                    color = if (isActive) CWColors.AccentCyan else CWColors.TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = CWTypography.AppTypography.bodySmall,
                color = if (isActive) CWColors.TextPrimary else CWColors.TextTertiary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
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
