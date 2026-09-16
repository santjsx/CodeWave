package com.codewave.player.ui.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.component.CWVerticalFader
import com.codewave.player.core.designsystem.component.tactilePress
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.DSPStatus
import com.codewave.player.core.model.EQPreset
import com.codewave.player.core.model.EqualizerBand
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val dspStatus by viewModel.dspStatus.collectAsState()

    var isPresetSheetOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        // 1. VS Code Breadcrumb Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CODEWAVE // EQUALIZER & DSP",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextTertiary,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (!config.isEnabled) CWColors.TextTertiary
                                else when (dspStatus) {
                                    DSPStatus.ACTIVE -> CWColors.Success
                                    DSPStatus.LIMITED -> CWColors.Warning
                                    DSPStatus.BYPASSED -> CWColors.TextTertiary
                                    DSPStatus.UNAVAILABLE -> CWColors.Danger
                                }
                            )
                    )
                    Text(
                        text = if (!config.isEnabled) "BYPASSED (BIT-PERFECT DIRECT)"
                        else when (dspStatus) {
                            DSPStatus.ACTIVE -> "DYNAMICS PROCESSING (ACTIVE · FLOAT32)"
                            DSPStatus.LIMITED -> "LEGACY EQUALIZER (FALLBACK)"
                            DSPStatus.BYPASSED -> "BYPASSED (BIT-PERFECT DIRECT)"
                            DSPStatus.UNAVAILABLE -> "HARDWARE EFFECTS UNAVAILABLE"
                        },
                        style = CWTypography.TechBadge,
                        color = if (config.isEnabled) CWColors.AccentCyan else CWColors.TextTertiary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Flat / Reset Action
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                        .background(CWColors.SurfacePrimary)
                        .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusSmall))
                        .clickable { viewModel.resetAll() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset EQ",
                            tint = CWColors.TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "RESET",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextPrimary,
                            fontSize = 9.sp
                        )
                    }
                }

                // Master Power Switch
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

        // 2. Real-Time Parametric Response Curve Canvas (VS Code Frequency Graph)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                .background(CWColors.SurfacePrimary)
                .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                .padding(top = 10.dp, bottom = 6.dp, start = 12.dp, end = 12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REAL-TIME FREQUENCY RESPONSE CURVE",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextTertiary,
                        fontSize = 8.5.sp
                    )
                    Text(
                        text = "±12 dB DYNAMIC RANGE",
                        style = CWTypography.TechTelemetry,
                        color = CWColors.AccentCyan.copy(alpha = 0.8f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                RealTimeEQCurveCanvas(
                    bands = config.bands,
                    enabled = config.isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. 10-Band Frequency Spectrum (ZERO HORIZONTAL SCROLL - All 10 bands on screen simultaneously!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            config.bands.forEachIndexed { index, band ->
                CWVerticalFader(
                    label = band.frequencyLabel,
                    gainDb = band.gainDb,
                    enabled = config.isEnabled,
                    onGainChange = { newGain ->
                        viewModel.setBandGain(index, newGain)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. VS Code Command Bar & Preset Quick-Pick (ZERO HORIZONTAL SCROLL)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Interactive VS Code Command Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                    .background(CWColors.SurfacePrimary)
                    .border(1.dp, CWColors.AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(CWShapes.RadiusMedium))
                    .clickable { isPresetSheetOpen = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = CWColors.AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "SOUND PROFILE",
                                style = CWTypography.TechBadge,
                                color = CWColors.TextTertiary,
                                fontSize = 8.5.sp
                            )
                            Text(
                                text = config.activePresetName,
                                style = CWTypography.AppTypography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CWColors.TextPrimary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activePreset = presets.find { it.name == config.activePresetName }
                        if (activePreset != null && activePreset.preampGainDb != 0f) {
                            Text(
                                text = "%+.1f dB".format(activePreset.preampGainDb),
                                style = CWTypography.TechTelemetry,
                                color = CWColors.AccentCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                                .background(CWColors.SurfaceElevated)
                                .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusSmall))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "SELECT",
                                    style = CWTypography.TechBadge,
                                    color = CWColors.AccentCyan,
                                    fontSize = 9.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = CWColors.AccentCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Compact 2-Row Micro-Chip Grid (No horizontal scrolling, clean 1-tap switching)
            // Row 1: 6 popular presets
            val row1 = listOf("Flat", "Bass Boost", "Bass Reducer", "Vocal Clarity", "Rock", "Pop")
            // Row 2: 6 genre presets
            val row2 = listOf("Dance", "Electronic", "Jazz", "Acoustic", "Hip-Hop", "Classical")

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PresetChipRow(
                    presetNames = row1,
                    activePreset = config.activePresetName,
                    allPresets = presets,
                    onSelect = { viewModel.applyPreset(it) }
                )
                PresetChipRow(
                    presetNames = row2,
                    activePreset = config.activePresetName,
                    allPresets = presets,
                    onSelect = { viewModel.applyPreset(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Preamp Gain & Limiter Protection Strip (VS Code Inspector Style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                .background(CWColors.SurfacePrimary)
                .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                .padding(14.dp)
        ) {
            Column {
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
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = CWColors.AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "MASTER PREAMP GAIN",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "%+.1f dB".format(config.preampGainDb),
                        style = CWTypography.TechTelemetry,
                        color = CWColors.AccentCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
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
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Limiter Anti-Clipping Guard",
                            style = CWTypography.AppTypography.bodyMedium,
                            color = CWColors.TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Prevents digital distortion at high output levels",
                            style = CWTypography.AppTypography.bodySmall,
                            color = CWColors.TextTertiary,
                            fontSize = 11.sp
                        )
                    }

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
    }

    // Modal Sheet: Full VS Code Command Palette Presets List
    if (isPresetSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isPresetSheetOpen = false },
            containerColor = CWColors.SurfaceElevated,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SOUND PROFILES (${presets.size} AVAILABLE)",
                        style = CWTypography.TechBadge,
                        color = CWColors.TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "SELECT TO APPLY",
                        style = CWTypography.TechBadge,
                        color = CWColors.AccentCyan,
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(presets, key = { it.name }) { preset ->
                        val isSelected = config.activePresetName == preset.name
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                                .background(if (isSelected) CWColors.SurfaceOverlay else CWColors.SurfacePrimary)
                                .border(
                                    1.dp,
                                    if (isSelected) CWColors.AccentCyan else CWColors.BorderSubtle,
                                    RoundedCornerShape(CWShapes.RadiusMedium)
                                )
                                .clickable {
                                    viewModel.applyPreset(preset)
                                    isPresetSheetOpen = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = CWColors.AccentCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Box(modifier = Modifier.size(18.dp))
                                    }

                                    Column {
                                        Text(
                                            text = preset.name,
                                            style = CWTypography.AppTypography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) CWColors.AccentCyan else CWColors.TextPrimary
                                        )
                                        if (preset.preampGainDb != 0f) {
                                            Text(
                                                text = "Preamp Offset: %+.1f dB".format(preset.preampGainDb),
                                                style = CWTypography.TechBadge,
                                                color = CWColors.TextTertiary,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                // Mini Curve Thumbnail for Preset
                                PresetMiniCurve(
                                    bandGains = preset.bandGainsDb,
                                    isSelected = isSelected,
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(26.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PresetChipRow(
    presetNames: List<String>,
    activePreset: String,
    allPresets: List<EQPreset>,
    onSelect: (EQPreset) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        presetNames.forEach { name ->
            val preset = allPresets.find { it.name.equals(name, ignoreCase = true) }
            val isSelected = activePreset.equals(name, ignoreCase = true)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                    .background(if (isSelected) CWColors.AccentCyan else CWColors.SurfacePrimary)
                    .border(
                        0.75.dp,
                        if (isSelected) CWColors.AccentCyan else CWColors.BorderSubtle,
                        RoundedCornerShape(CWShapes.RadiusSmall)
                    )
                    .clickable(enabled = preset != null) {
                        preset?.let { onSelect(it) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.replace(" ", "\n").take(8),
                    style = CWTypography.TechBadge,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) CWColors.Background else CWColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RealTimeEQCurveCanvas(
    bands: List<EqualizerBand>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = if (enabled) CWColors.AccentCyan else CWColors.TextTertiary
    val secondaryColor = if (enabled) CWColors.AccentBlue else CWColors.BorderSubtle

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f

        // Center 0 dB Reference Line
        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(0f, centerY),
            end = Offset(w, centerY),
            strokeWidth = 1f
        )

        // ±12 dB Bound Lines
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(0f, 4f),
            end = Offset(w, 4f),
            strokeWidth = 0.5f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(0f, h - 4f),
            end = Offset(w, h - 4f),
            strokeWidth = 0.5f
        )

        if (bands.isEmpty()) return@Canvas

        val n = bands.size
        val points = mutableListOf<Offset>()

        // Map bands to canvas points
        for (i in 0 until n) {
            val band = bands[i]
            val x = (i + 0.5f) * (w / n.toFloat())
            // gainDb ranges from -12 to +12
            val normGain = (band.gainDb / 12f).coerceIn(-1f, 1f)
            val y = centerY - (normGain * (centerY - 6f))
            points.add(Offset(x, y))
        }

        // Build Smooth Cubic Bezier Path
        val path = Path()
        path.moveTo(0f, points.first().y)
        path.lineTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val cx = (p0.x + p1.x) / 2f
            path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
        }
        path.lineTo(w, points.last().y)

        // Gradient Fill Under Curve
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(w, h)
        fillPath.lineTo(0f, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (enabled) 0.25f else 0.05f),
                    secondaryColor.copy(alpha = 0f)
                ),
                startY = 0f,
                endY = h
            )
        )

        // Stroke Line
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(secondaryColor, accentColor)),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // Frequency Node Indicator Dots
        points.forEach { pt ->
            drawCircle(
                color = accentColor,
                radius = 3.5f,
                center = pt
            )
            drawCircle(
                color = CWColors.SurfacePrimary,
                radius = 1.5f,
                center = pt
            )
        }
    }
}

@Composable
private fun PresetMiniCurve(
    bandGains: List<Float>,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = if (isSelected) CWColors.AccentCyan else CWColors.TextSecondary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f

        if (bandGains.isEmpty()) return@Canvas

        val n = bandGains.size
        val points = mutableListOf<Offset>()

        for (i in 0 until n) {
            val gain = bandGains[i]
            val x = (i + 0.5f) * (w / n.toFloat())
            val normGain = (gain / 12f).coerceIn(-1f, 1f)
            val y = centerY - (normGain * (centerY - 3f))
            points.add(Offset(x, y))
        }

        val path = Path()
        path.moveTo(0f, points.first().y)
        path.lineTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val cx = (p0.x + p1.x) / 2f
            path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
        }
        path.lineTo(w, points.last().y)

        drawPath(
            path = path,
            color = accent,
            style = Stroke(width = 1.5f, cap = StrokeCap.Round)
        )
    }
}
