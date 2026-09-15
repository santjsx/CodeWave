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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.DSPStatus

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
                    text = "10-BAND DYNAMICS PROCESSING",
                    style = CWTypography.TechBadge,
                    color = CWColors.TextSecondary
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
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HARDWARE DSP STATUS",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextSecondary
                    )
                    Text(
                        text = when (dspStatus) {
                            DSPStatus.ACTIVE -> "DynamicsProcessing active (low-latency float)"
                            DSPStatus.LIMITED -> "Legacy Equalizer active (fallback mode)"
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
                        DSPStatus.UNAVAILABLE -> CWColors.Danger
                    }
                )
            }
        }

        // Presets Horizontal Bar (PRD Section 38)
        Text(
            text = "PRESETS",
            style = CWTypography.TechBadge,
            color = CWColors.TextSecondary,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = config.activePresetName == preset.name
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                        .background(if (isSelected) CWColors.AccentCyan else CWColors.SurfaceElevated)
                        .border(
                            1.dp,
                            if (isSelected) CWColors.AccentCyan else CWColors.BorderSubtle,
                            RoundedCornerShape(CWShapes.RadiusMedium)
                        )
                        .clickable { viewModel.applyPreset(preset) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = CWTypography.AppTypography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) CWColors.Background else CWColors.TextPrimary
                    )
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

                Slider(
                    value = config.preampGainDb,
                    onValueChange = { viewModel.setPreampGain(it) },
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
            text = "10-BAND FREQUENCY SPECTRUM",
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
                BandSliderColumn(
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

@Composable
private fun BandSliderColumn(
    label: String,
    gainDb: Float,
    enabled: Boolean,
    onGainChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
            .background(CWColors.SurfacePrimary)
            .border(0.75.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "%+.1f".format(gainDb),
            style = CWTypography.TechTelemetry,
            fontSize = 10.sp,
            color = if (gainDb != 0f) CWColors.AccentCyan else CWColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Simple vertical-representing slider using Slider
        Slider(
            value = gainDb,
            onValueChange = onGainChange,
            valueRange = -12f..12f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = CWColors.AccentCyan,
                activeTrackColor = CWColors.AccentCyan,
                inactiveTrackColor = CWColors.SurfaceOverlay
            ),
            modifier = Modifier
                .height(130.dp)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = CWTypography.TechBadge,
            color = CWColors.TextPrimary,
            textAlign = TextAlign.Center
        )
    }
}
