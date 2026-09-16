package com.codewave.player.ui.equalizer

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.MusicNote
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
        // 1. Studio Master Header Console
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CODEWAVE STUDIO DSP",
                    style = CWTypography.TechBadge,
                    color = CWColors.TextSecondary,
                    letterSpacing = 1.2.sp,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
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
                            DSPStatus.ACTIVE -> "FLOAT32 MASTER DSP · ACTIVE"
                            DSPStatus.LIMITED -> "LEGACY EQUALIZER FALLBACK"
                            DSPStatus.BYPASSED -> "BYPASSED (BIT-PERFECT DIRECT)"
                            DSPStatus.UNAVAILABLE -> "HARDWARE DSP UNAVAILABLE"
                        },
                        style = CWTypography.TechTelemetry,
                        color = if (config.isEnabled) CWColors.AccentCyan else CWColors.TextTertiary,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Flat / Reset Action
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                        .background(CWColors.SurfacePrimary)
                        .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusSmall))
                        .clickable { viewModel.resetAll() }
                        .padding(horizontal = 9.dp, vertical = 6.dp),
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
                            text = "FLAT",
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

        // 2. Studio Parametric Frequency Visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(CWShapes.RadiusLarge))
                .background(Color(0xFF0D1117))
                .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusLarge))
                .padding(top = 10.dp, bottom = 8.dp, start = 12.dp, end = 12.dp)
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
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = CWColors.AccentCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "PARAMETRIC FREQUENCY RESPONSE",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextSecondary,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "±12 dB DYNAMIC RANGE",
                        style = CWTypography.TechTelemetry,
                        color = CWColors.AccentCyan.copy(alpha = 0.8f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                RealTimeEQCurveCanvas(
                    bands = config.bands,
                    enabled = config.isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Unified 10-Band Studio Console Chassis (No Cluttered 10 Individual Box Outlines)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(CWShapes.RadiusLarge))
                .background(CWColors.SurfacePrimary)
                .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusLarge))
                .padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Sound Profile & Preset Selection (ZERO Text Truncation)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Active Sound Profile Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CWShapes.RadiusLarge))
                    .background(CWColors.SurfacePrimary)
                    .border(1.dp, CWColors.AccentCyan.copy(alpha = 0.45f), RoundedCornerShape(CWShapes.RadiusLarge))
                    .clickable { isPresetSheetOpen = true }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CWColors.AccentCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = CWColors.AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ACTIVE SOUND PROFILE",
                                style = CWTypography.TechBadge,
                                color = CWColors.TextTertiary,
                                fontSize = 9.sp
                            )
                            Text(
                                text = config.activePresetName,
                                style = CWTypography.AppTypography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CWColors.TextPrimary
                            )
                            val activePreset = presets.find { it.name == config.activePresetName }
                            if (activePreset != null) {
                                Text(
                                    text = activePreset.profileSubtitle,
                                    style = CWTypography.AppTypography.bodySmall,
                                    color = CWColors.TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Browse All trigger button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                            .background(CWColors.SurfaceElevated)
                            .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusSmall))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "BROWSE ALL",
                                style = CWTypography.TechBadge,
                                color = CWColors.AccentCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
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

            // Horizontal Scrollable Sound Profile Strip (Every name is 100% visible, zero truncation!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEach { preset ->
                    val isSelected = config.activePresetName == preset.name
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(CWShapes.RadiusFull))
                            .background(
                                if (isSelected) CWColors.AccentCyan else CWColors.SurfacePrimary
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CWColors.AccentCyan else CWColors.BorderSubtle,
                                shape = RoundedCornerShape(CWShapes.RadiusFull)
                            )
                            .clickable { viewModel.applyPreset(preset) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CWColors.Background,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = preset.name,
                                style = CWTypography.TechBadge,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CWColors.Background else CWColors.TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Preamp Gain & Limiter Protection Strip (Studio Hardware Rack)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(CWShapes.RadiusLarge))
                .background(CWColors.SurfacePrimary)
                .border(1.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusLarge))
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        .padding(top = 4.dp),
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
                            text = "Protects 32-bit floating-point audio engine against digital clipping",
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

    // Modal Sheet: Full Presets List with Mini Curve Previews
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
                        color = CWColors.AccentCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Tap to audition profile",
                        style = CWTypography.TechTelemetry,
                        color = CWColors.TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(presets, key = { it.id }) { preset ->
                        val isSelected = config.activePresetName == preset.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                                .background(if (isSelected) CWColors.AccentCyan.copy(alpha = 0.15f) else CWColors.SurfacePrimary)
                                .border(
                                    1.dp,
                                    if (isSelected) CWColors.AccentCyan else CWColors.BorderSubtle,
                                    RoundedCornerShape(CWShapes.RadiusMedium)
                                )
                                .clickable {
                                    viewModel.applyPreset(preset)
                                    isPresetSheetOpen = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = preset.name,
                                        style = CWTypography.AppTypography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CWColors.AccentCyan else CWColors.TextPrimary
                                    )
                                    if (preset.isBuiltIn) {
                                        Text(
                                            text = "STUDIO",
                                            style = CWTypography.TechBadge,
                                            color = CWColors.TextTertiary,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                                Text(
                                    text = preset.profileSubtitle,
                                    style = CWTypography.AppTypography.bodySmall,
                                    color = CWColors.TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PresetMiniCurve(
                                    bandGains = preset.bandGainsDb,
                                    isSelected = isSelected,
                                    modifier = Modifier
                                        .width(52.dp)
                                        .height(26.dp)
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = CWColors.AccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Studio Parametric Curve Canvas:
 * - Acoustic frequency zones (SUB, BASS, MID, HIGH-MID, PRESENCE, AIR)
 * - Dotted gridlines at +12, +6, 0, -6, -12 dB
 * - Smooth cubic Bezier response curve with glowing vertical gradient illumination
 * - 10 illuminated frequency node points along the curve with dynamic halo on active gains
 */
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

        // Acoustic Frequency Zones Guidelines
        val zoneLines = listOf(0.18f, 0.38f, 0.62f, 0.82f)
        zoneLines.forEach { frac ->
            drawLine(
                color = Color.White.copy(alpha = 0.04f),
                start = Offset(w * frac, 0f),
                end = Offset(w * frac, h),
                strokeWidth = 0.75f
            )
        }

        // Horizontal dB Gridlines (+12, +6, 0, -6, -12 dB)
        val gridLevels = listOf(0.08f, 0.28f, 0.5f, 0.72f, 0.92f)
        gridLevels.forEachIndexed { idx, frac ->
            val y = h * frac
            val isCenter = idx == 2
            drawLine(
                color = if (isCenter) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = if (isCenter) 1.2f else 0.6f
            )
        }

        if (bands.isEmpty()) return@Canvas

        val n = bands.size
        val points = mutableListOf<Offset>()

        for (i in 0 until n) {
            val band = bands[i]
            val x = (i + 0.5f) * (w / n.toFloat())
            val normGain = (band.gainDb / 12f).coerceIn(-1f, 1f)
            val y = centerY - (normGain * (centerY - 8f))
            points.add(Offset(x, y))
        }

        // Smooth Cubic Bezier Path
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

        // Gradient Fill Under Response Curve
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(w, h)
        fillPath.lineTo(0f, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (enabled) 0.32f else 0.05f),
                    secondaryColor.copy(alpha = if (enabled) 0.12f else 0.02f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = h
            )
        )

        // Neon Glow Stroke
        if (enabled) {
            drawPath(
                path = path,
                color = accentColor.copy(alpha = 0.35f),
                style = Stroke(width = 5.5f, cap = StrokeCap.Round)
            )
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(secondaryColor, accentColor)),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // Frequency Node Points
        points.forEachIndexed { i, pt ->
            val gain = bands.getOrNull(i)?.gainDb ?: 0f
            val isBandActive = gain != 0f && enabled

            if (isBandActive) {
                // Outer illuminated halo
                drawCircle(
                    color = accentColor.copy(alpha = 0.35f),
                    radius = 6.dp.toPx(),
                    center = pt
                )
            }

            drawCircle(
                color = if (isBandActive) accentColor else Color.White.copy(alpha = 0.5f),
                radius = if (isBandActive) 3.5.dp.toPx() else 2.dp.toPx(),
                center = pt
            )

            drawCircle(
                color = Color(0xFF0D1117),
                radius = 1.2.dp.toPx(),
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

private val EQPreset.profileSubtitle: String
    get() = when (name.lowercase()) {
        "flat" -> "Neutral studio reference response"
        "acoustic" -> "Enhanced acoustic timbre and warm mids"
        "bass boost" -> "Sub-bass elevation with heavy kick punch"
        "bass reducer" -> "Attenuated low end for vocal clarity"
        "classical" -> "Orchestral separation and wide dynamics"
        "dance" -> "Pumping low end with crisp top-end presence"
        "electronic" -> "Synthesizer focus with extended sub-bass"
        "hip-hop" -> "Deep low-end rumble and highlighted punch"
        "jazz" -> "Warm natural tone with smooth horn response"
        "pop" -> "Radio vocal lift with tight low-end groove"
        "rock" -> "Aggressive midrange edge and punchy rhythm"
        "vocal clarity" -> "High dialogue intelligibility & presence"
        else -> "${if (preampGainDb != 0f) "${preampGainDb} dB · " else ""}10-band tuned profile"
    }
