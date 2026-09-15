package com.codewave.player.ui.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.component.CWVerticalFader
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.DSPStatus
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val dspStatus by viewModel.dspStatus.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DSP & EQUALIZER",
                    style = CWTypography.AppTypography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CWColors.TextPrimary
                )
                Text(
                    text = if (config.isEnabled) "STATUS: PROCESSING ACTIVE" else "STATUS: BYPASSED",
                    style = CWTypography.TechBadge,
                    color = if (config.isEnabled) CWColors.AccentCyan else CWColors.TextTertiary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                        .background(CWColors.SurfaceElevated)
                        .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusSmall))
                        .clickable { viewModel.resetAll() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "FLAT / RESET",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextPrimary
                    )
                }

                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { viewModel.toggleEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CWColors.Background,
                        checkedTrackColor = CWColors.AccentCyan,
                        uncheckedThumbColor = CWColors.TextTertiary,
                        uncheckedTrackColor = CWColors.SurfaceOverlay
                    )
                )
            }
        }

        // DSP Telemetry Banner (PRD Section 40)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(CWShapes.RadiusMedium),
            colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when (dspStatus) {
                                        DSPStatus.ACTIVE -> CWColors.Success
                                        DSPStatus.LIMITED -> CWColors.Warning
                                        DSPStatus.BYPASSED -> CWColors.TextTertiary
                                        DSPStatus.UNAVAILABLE -> CWColors.Danger
                                    }
                                )
                        )
                        Text(
                            text = "HARDWARE DSP STATUS",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (dspStatus) {
                            DSPStatus.ACTIVE -> "DynamicsProcessing active (low-latency float)"
                            DSPStatus.LIMITED -> "Legacy Equalizer active (fallback mode)"
                            DSPStatus.BYPASSED -> "DSP hardware bypassed (pure bit-perfect pass-through)"
                            DSPStatus.UNAVAILABLE -> "Hardware effects unavailable"
                        },
                        style = CWTypography.AppTypography.bodyMedium,
                        color = CWColors.TextPrimary
                    )
                }

                CWTechnicalBadge(
                    text = dspStatus.name,
                    textColor = when (dspStatus) {
                        DSPStatus.ACTIVE -> CWColors.Success
                        DSPStatus.LIMITED -> CWColors.Warning
                        DSPStatus.BYPASSED -> CWColors.TextTertiary
                        DSPStatus.UNAVAILABLE -> CWColors.Danger
                    }
                )
            }
        }

        // Presets Header & Telemetry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SOUND PROFILES & PRESETS",
                style = CWTypography.TechBadge,
                color = CWColors.TextSecondary
            )
            val activePreset = presets.find { it.name == config.activePresetName }
            if (activePreset != null && activePreset.preampGainDb != 0f) {
                Text(
                    text = "PROFILE PREAMP %+.1f dB".format(activePreset.preampGainDb),
                    style = CWTypography.TechBadge,
                    color = CWColors.AccentCyan
                )
            }
        }

        // Active Profile Spotlight Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(CWShapes.RadiusMedium),
            colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                            .background(CWColors.SurfaceElevated)
                            .border(1.dp, CWColors.AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(CWShapes.RadiusSmall)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = CWColors.AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "CURRENT PROFILE",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextTertiary
                        )
                        Text(
                            text = config.activePresetName,
                            style = CWTypography.AppTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CWColors.TextPrimary
                        )
                    }
                }

                CWTechnicalBadge(
                    text = "${presets.size} PROFILES",
                    textColor = CWColors.AccentCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3-Column Presets Grid (All 12 presets visible at a glance without horizontal scroll)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.chunked(3).forEach { rowPresets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPresets.forEach { preset ->
                        val isSelected = config.activePresetName == preset.name
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                                .background(if (isSelected) CWColors.AccentCyan else CWColors.SurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) CWColors.AccentCyan else CWColors.BorderSubtle,
                                    RoundedCornerShape(CWShapes.RadiusMedium)
                                )
                                .clickable { viewModel.applyPreset(preset) }
                                .padding(horizontal = 4.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.name,
                                style = CWTypography.AppTypography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CWColors.Background else CWColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    repeat(3 - rowPresets.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Preamp & Limiter Section (PRD Section 39)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                        text = "PREAMP GAIN",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextSecondary
                    )
                    Text(
                        text = "%+.1f dB".format(config.preampGainDb),
                        style = CWTypography.TechTelemetry,
                        color = CWColors.AccentCyan
                    )
                }

                var localPreamp by remember(config.preampGainDb) { mutableFloatStateOf(config.preampGainDb) }
                LaunchedEffect(config.preampGainDb) {
                    localPreamp = config.preampGainDb
                }

                Slider(
                    value = localPreamp,
                    onValueChange = {
                        localPreamp = (it * 2).roundToInt() / 2f
                        viewModel.setPreampGain(localPreamp)
                    },
                    valueRange = -12f..12f,
                    enabled = config.isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = CWColors.AccentCyan,
                        activeTrackColor = CWColors.AccentCyan,
                        inactiveTrackColor = CWColors.SurfaceOverlay
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Limiter Protection (Anti-Clipping)",
                        style = CWTypography.AppTypography.bodyMedium,
                        color = CWColors.TextPrimary
                    )
                    Switch(
                        checked = config.isLimiterEnabled,
                        onCheckedChange = { viewModel.toggleLimiter(it) },
                        enabled = config.isEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CWColors.Background,
                            checkedTrackColor = CWColors.AccentCyan,
                            uncheckedThumbColor = CWColors.TextTertiary,
                            uncheckedTrackColor = CWColors.SurfaceOverlay
                        )
                    )
                }
            }
        }

        // 10-Band Graphic Equalizer Section (PRD Section 37)
        Text(
            text = "10-BAND FREQUENCY SPECTRUM (TAP/DRAG OR DOUBLE-TAP TO ZERO)",
            style = CWTypography.TechBadge,
            color = CWColors.TextSecondary,
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            config.bands.forEachIndexed { index, band ->
                CWVerticalFader(
                    label = band.frequencyLabel,
                    gainDb = band.gainDb,
                    enabled = config.isEnabled,
                    onGainChange = { newGain ->
                        viewModel.setBandGain(index, newGain)
                    }
                )
            }
        }
    }
}
