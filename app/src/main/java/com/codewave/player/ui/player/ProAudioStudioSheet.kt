package com.codewave.player.ui.player

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.component.CWButton
import com.codewave.player.core.designsystem.component.CWButtonVariant
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.ABLoopState
import com.codewave.player.core.model.EqualizerConfig
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProAudioStudioSheet(
    abLoopState: ABLoopState,
    currentPositionMs: Long,
    playbackSpeed: Float,
    pitchSemitones: Int,
    equalizerConfig: EqualizerConfig,
    onSetPointA: (Long) -> Unit,
    onSetPointB: (Long) -> Unit,
    onClearLoop: () -> Unit,
    onToggleLoop: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetPitch: (Int) -> Unit,
    onToggleVocalRemover: (Boolean) -> Unit,
    onSetVocalRemoverLevel: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CWColors.Background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CWColors.SurfacePrimary)
                    .border(width = 0.5.dp, color = CWColors.BorderSubtle)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CWColors.AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "PRO AUDIO STUDIO",
                        style = CWTypography.AppTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CWColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CWTechnicalBadge(text = "DSP v1.9", textColor = CWColors.AccentCyan)
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = CWColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // SECTION 1: A-B SEAMLESS LOOPER
            // ==========================================
            SectionHeader(title = "A-B SEAMLESS LOOPER", icon = Icons.Default.Repeat)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(CWShapes.RadiusMedium),
                colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Status Telemetry
                    val loopStatusText = when {
                        abLoopState.isEnabled && abLoopState.isConfigured -> {
                            "ACTIVE: ${formatMs(abLoopState.pointA ?: 0L)} → ${formatMs(abLoopState.pointB ?: 0L)} (${formatMs(abLoopState.durationMs ?: 0L)})"
                        }
                        abLoopState.pointA != null && abLoopState.pointB == null -> {
                            "POINT A SET: ${formatMs(abLoopState.pointA)} • PLAY & TAP [SET B]"
                        }
                        abLoopState.pointA != null && abLoopState.pointB != null -> {
                            "PAUSED: ${formatMs(abLoopState.pointA)} → ${formatMs(abLoopState.pointB)} (TAP TOGGLE)"
                        }
                        else -> "INACTIVE • TAP [SET A (CURRENT)] AT LOOP START"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                            .background(CWColors.SurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = if (abLoopState.isEnabled) CWColors.AccentCyan else CWColors.BorderSubtle,
                                shape = RoundedCornerShape(CWShapes.RadiusSmall)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = loopStatusText,
                            style = CWTypography.TechTelemetry,
                            color = if (abLoopState.isEnabled) CWColors.AccentCyan else CWColors.TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (abLoopState.isConfigured) {
                            Switch(
                                checked = abLoopState.isEnabled,
                                onCheckedChange = { onToggleLoop() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CWColors.Background,
                                    checkedTrackColor = CWColors.AccentCyan,
                                    uncheckedThumbColor = CWColors.TextTertiary,
                                    uncheckedTrackColor = CWColors.SurfaceOverlay
                                ),
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CWButton(
                            text = "SET A (${formatMs(currentPositionMs)})",
                            onClick = { onSetPointA(currentPositionMs) },
                            variant = if (abLoopState.pointA != null) CWButtonVariant.SOLID else CWButtonVariant.OUTLINED,
                            modifier = Modifier.weight(1f)
                        )
                        CWButton(
                            text = "SET B (${formatMs(currentPositionMs)})",
                            onClick = { onSetPointB(currentPositionMs) },
                            variant = if (abLoopState.pointB != null) CWButtonVariant.SOLID else CWButtonVariant.OUTLINED,
                            modifier = Modifier.weight(1f)
                        )
                        CWButton(
                            text = "CLEAR",
                            onClick = onClearLoop,
                            variant = CWButtonVariant.GHOST,
                            modifier = Modifier.width(72.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // SECTION 2: PITCH TRANSPOSER (SEMITONES)
            // ==========================================
            SectionHeader(title = "PITCH SHIFTER (KEY TRANSPOSITION)", icon = Icons.Default.GraphicEq)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(CWShapes.RadiusMedium),
                colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val multiplier = 2.0.pow(pitchSemitones / 12.0).toFloat()
                    val pitchLabel = when {
                        pitchSemitones > 0 -> "+$pitchSemitones ST"
                        pitchSemitones < 0 -> "$pitchSemitones ST"
                        else -> "ORIGINAL (0 ST)"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Key Transposition",
                            style = CWTypography.AppTypography.titleMedium,
                            color = CWColors.TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pitchLabel,
                                style = CWTypography.TechBadge,
                                color = if (pitchSemitones != 0) CWColors.AccentCyan else CWColors.TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format(Locale.US, "(%.1f%%)", multiplier * 100f),
                                style = CWTypography.TechTelemetry,
                                color = CWColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = pitchSemitones.toFloat(),
                        onValueChange = { onSetPitch(it.roundToInt()) },
                        valueRange = -12f..12f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = CWColors.AccentCyan,
                            activeTrackColor = CWColors.AccentCyan,
                            inactiveTrackColor = CWColors.SurfaceOverlay
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CWButton(
                            text = "-1 ST",
                            onClick = { onSetPitch((pitchSemitones - 1).coerceAtLeast(-12)) },
                            variant = CWButtonVariant.OUTLINED,
                            modifier = Modifier.weight(1f)
                        )
                        CWButton(
                            text = "RESET (0)",
                            onClick = { onSetPitch(0) },
                            variant = if (pitchSemitones == 0) CWButtonVariant.SOLID else CWButtonVariant.GHOST,
                            modifier = Modifier.weight(1f)
                        )
                        CWButton(
                            text = "+1 ST",
                            onClick = { onSetPitch((pitchSemitones + 1).coerceAtMost(12)) },
                            variant = CWButtonVariant.OUTLINED,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // SECTION 3: TIME-STRETCH TEMPO
            // ==========================================
            SectionHeader(title = "TIME-STRETCH TEMPO (SPEED)", icon = Icons.Default.Speed)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(CWShapes.RadiusMedium),
                colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Playback Rate (Pitch-Preserved)",
                            style = CWTypography.AppTypography.titleMedium,
                            color = CWColors.TextPrimary
                        )
                        CWTechnicalBadge(
                            text = String.format(Locale.US, "%.2fx", playbackSpeed),
                            textColor = if (playbackSpeed != 1.0f) CWColors.AccentCyan else CWColors.TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val speedPresets = listOf(0.5f, 0.75f, 0.9f, 1.0f, 1.1f, 1.25f, 1.5f, 2.0f)
                    val rows = speedPresets.chunked(4)

                    rows.forEachIndexed { rowIndex, rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { speed ->
                                val isSelected = (playbackSpeed - speed).let { if (it < 0) -it else it } < 0.02f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                                        .background(if (isSelected) CWColors.AccentCyan.copy(alpha = 0.15f) else CWColors.SurfaceElevated)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) CWColors.AccentCyan else CWColors.BorderSubtle,
                                            shape = RoundedCornerShape(CWShapes.RadiusSmall)
                                        )
                                        .clickable { onSetSpeed(speed) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CWColors.AccentCyan else CWColors.TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        if (rowIndex < rows.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // SECTION 4: VOCAL REMOVER & KARAOKE ISOLATION
            // ==========================================
            SectionHeader(title = "VOCAL REMOVER & KARAOKE ISOLATION", icon = Icons.Default.MicOff)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(CWShapes.RadiusMedium),
                colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vocal Suppression Filter",
                                style = CWTypography.AppTypography.titleMedium,
                                color = CWColors.TextPrimary
                            )
                            Text(
                                text = "Center-channel differential phase cancellation with sub-bass (< 160Hz) preservation",
                                style = CWTypography.AppTypography.bodySmall,
                                color = CWColors.TextSecondary
                            )
                        }

                        Switch(
                            checked = equalizerConfig.isVocalRemoverEnabled,
                            onCheckedChange = { onToggleVocalRemover(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CWColors.Background,
                                checkedTrackColor = CWColors.AccentCyan,
                                uncheckedThumbColor = CWColors.TextTertiary,
                                uncheckedTrackColor = CWColors.SurfaceOverlay
                            )
                        )
                    }

                    if (equalizerConfig.isVocalRemoverEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Suppression Depth",
                                style = CWTypography.TechTelemetry,
                                color = CWColors.TextSecondary
                            )
                            Text(
                                text = "${(equalizerConfig.vocalRemoverLevel * 100).roundToInt()}%",
                                style = CWTypography.TechBadge,
                                color = CWColors.AccentCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = equalizerConfig.vocalRemoverLevel,
                            onValueChange = { onSetVocalRemoverLevel(it) },
                            valueRange = 0.2f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = CWColors.AccentCyan,
                                activeTrackColor = CWColors.AccentCyan,
                                inactiveTrackColor = CWColors.SurfaceOverlay
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CWTechnicalBadge(text = "ZERO-LATENCY DSP", textColor = CWColors.Success)
                        CWTechnicalBadge(text = "BASS PRESERVED", textColor = CWColors.AccentCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CWColors.AccentCyan,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = CWTypography.TechBadge,
            color = CWColors.AccentCyan,
            fontSize = 11.sp
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 100
    return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, millis)
}
