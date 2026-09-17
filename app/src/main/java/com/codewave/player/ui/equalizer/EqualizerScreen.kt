package com.codewave.player.ui.equalizer

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import java.io.File
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.component.CWVerticalFader
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

    val context = LocalContext.current
    val irsPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) cursor.getString(nameIndex) else null
                } ?: "custom_${System.currentTimeMillis()}.irs"

                val irsDir = File(context.filesDir, "irs").apply { mkdirs() }
                val destFile = File(irsDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.setIrsName(fileName)
                viewModel.toggleConvolver(true)
            } catch (_: Exception) {}
        }
    }

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

        // 3. Seamless 10-Band Studio Console Chassis (Uniform, Non-Patchy Backplate)
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

        // 4. Studio Master Preamp Gain Console (Bipolar Center-Zero Hardware Slider)
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
                        text = if (config.preampGainDb == 0f) "0.0 dB" else "%+.1f dB".format(config.preampGainDb),
                        style = CWTypography.TechTelemetry,
                        color = when {
                            !config.isEnabled -> CWColors.TextTertiary
                            config.preampGainDb > 0f -> CWColors.AccentCyan
                            config.preampGainDb < 0f -> Color(0xFFFF8A65)
                            else -> CWColors.TextPrimary
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom Studio Bipolar Slider
                StudioBipolarSlider(
                    value = config.preampGainDb,
                    onValueChange = { viewModel.setPreampGain(it) },
                    enabled = config.isEnabled
                )

                // Quick dB Jump Chips (Fixed Row, Zero Horizontal Scrolling)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickGains = listOf(-6.0f, -3.0f, 0.0f, 3.0f, 6.0f)
                    quickGains.forEach { targetGain ->
                        val isCurrent = kotlin.math.abs(config.preampGainDb - targetGain) < 0.25f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                                .background(
                                    if (isCurrent && config.isEnabled) CWColors.AccentCyan.copy(alpha = 0.15f)
                                    else CWColors.SurfaceElevated
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isCurrent && config.isEnabled) CWColors.AccentCyan else CWColors.BorderSubtle,
                                    shape = RoundedCornerShape(CWShapes.RadiusSmall)
                                )
                                .clickable(enabled = config.isEnabled) {
                                    viewModel.setPreampGain(targetGain)
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (targetGain == 0f) "0 dB" else "%+.0f dB".format(targetGain),
                                style = CWTypography.TechBadge,
                                color = if (isCurrent && config.isEnabled) CWColors.AccentCyan else CWColors.TextSecondary,
                                fontSize = 9.5.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Limiter Anti-Clipping Guard (ZERO Truncation with weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            Text(
                                text = "Limiter Anti-Clipping Guard",
                                style = CWTypography.AppTypography.bodyMedium,
                                color = CWColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (config.isLimiterEnabled && config.isEnabled) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(CWShapes.RadiusFull))
                                        .background(CWColors.Success.copy(alpha = 0.15f))
                                        .border(0.5.dp, CWColors.Success.copy(alpha = 0.5f), RoundedCornerShape(CWShapes.RadiusFull))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PROTECTED",
                                        style = CWTypography.TechBadge,
                                        color = CWColors.Success,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
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

        Spacer(modifier = Modifier.height(14.dp))

        // 5. ViPERFX Acoustic Suite Console (Rootless Studio-Grade DSP Engine)
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
                // ViPER Header Console
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
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = CWColors.AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "VIPERFX ACOUSTIC SUITE",
                                style = CWTypography.TechBadge,
                                color = CWColors.TextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "DYNAMIC BASS · VOCAL CLARITY · IRS CONVOLVER",
                                style = CWTypography.TechTelemetry,
                                color = CWColors.AccentCyan.copy(alpha = 0.7f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(CWShapes.RadiusFull))
                            .background(CWColors.AccentCyan.copy(alpha = 0.12f))
                            .border(0.5.dp, CWColors.AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(CWShapes.RadiusFull))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "VIPER4ANDROID",
                            style = CWTypography.TechBadge,
                            color = CWColors.AccentCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- A. ViPER Dynamic Bass ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Dynamic Bass",
                                style = CWTypography.AppTypography.bodyMedium,
                                color = CWColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (config.isBassEnabled && config.isEnabled) {
                                Text(
                                    text = "%+.1f dB".format(config.bassGainDb),
                                    style = CWTypography.TechTelemetry,
                                    color = CWColors.AccentCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = "80Hz Butterworth sub-harmonic synthesis & 5Hz DC filter",
                            style = CWTypography.AppTypography.bodySmall,
                            color = CWColors.TextTertiary,
                            fontSize = 10.5.sp
                        )
                    }

                    Switch(
                        checked = config.isBassEnabled,
                        onCheckedChange = { viewModel.toggleBass(it) },
                        enabled = config.isEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CWColors.Background,
                            checkedTrackColor = CWColors.AccentCyan,
                            uncheckedThumbColor = CWColors.TextTertiary,
                            uncheckedTrackColor = CWColors.SurfaceOverlay
                        )
                    )
                }

                if (config.isBassEnabled && config.isEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    StudioUnipolarSlider(
                        value = config.bassGainDb,
                        onValueChange = { viewModel.setBassGain(it) },
                        enabled = config.isEnabled && config.isBassEnabled,
                        activeColor = CWColors.AccentCyan
                    )

                    // Quick dB Jump Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val bassChips = listOf(2.0f, 4.0f, 6.0f, 8.0f, 12.0f)
                        bassChips.forEach { targetGain ->
                            val isCurrent = kotlin.math.abs(config.bassGainDb - targetGain) < 0.25f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                                    .background(
                                        if (isCurrent) CWColors.AccentCyan.copy(alpha = 0.15f)
                                        else CWColors.SurfaceElevated
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isCurrent) CWColors.AccentCyan else CWColors.BorderSubtle,
                                        shape = RoundedCornerShape(CWShapes.RadiusSmall)
                                    )
                                    .clickable { viewModel.setBassGain(targetGain) }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+%.0fdB".format(targetGain),
                                    style = CWTypography.TechBadge,
                                    color = if (isCurrent) CWColors.AccentCyan else CWColors.TextSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- B. ViPER Clarity ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Viper Clarity",
                                style = CWTypography.AppTypography.bodyMedium,
                                color = CWColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (config.isClarityEnabled && config.isEnabled) {
                                Text(
                                    text = "%+.1f dB".format(config.clarityGainDb),
                                    style = CWTypography.TechTelemetry,
                                    color = Color(0xFF64B5F6),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = "3.5kHz vocal acoustic restorer with sibilance guard",
                            style = CWTypography.AppTypography.bodySmall,
                            color = CWColors.TextTertiary,
                            fontSize = 10.5.sp
                        )
                    }

                    Switch(
                        checked = config.isClarityEnabled,
                        onCheckedChange = { viewModel.toggleClarity(it) },
                        enabled = config.isEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CWColors.Background,
                            checkedTrackColor = Color(0xFF64B5F6),
                            uncheckedThumbColor = CWColors.TextTertiary,
                            uncheckedTrackColor = CWColors.SurfaceOverlay
                        )
                    )
                }

                if (config.isClarityEnabled && config.isEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    StudioUnipolarSlider(
                        value = config.clarityGainDb,
                        onValueChange = { viewModel.setClarityGain(it) },
                        enabled = config.isEnabled && config.isClarityEnabled,
                        activeColor = Color(0xFF64B5F6)
                    )

                    // Quick dB Jump Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val clarityChips = listOf(2.0f, 3.0f, 5.0f, 8.0f, 12.0f)
                        clarityChips.forEach { targetGain ->
                            val isCurrent = kotlin.math.abs(config.clarityGainDb - targetGain) < 0.25f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                                    .background(
                                        if (isCurrent) Color(0xFF64B5F6).copy(alpha = 0.15f)
                                        else CWColors.SurfaceElevated
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isCurrent) Color(0xFF64B5F6) else CWColors.BorderSubtle,
                                        shape = RoundedCornerShape(CWShapes.RadiusSmall)
                                    )
                                    .clickable { viewModel.setClarityGain(targetGain) }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+%.0fdB".format(targetGain),
                                    style = CWTypography.TechBadge,
                                    color = if (isCurrent) Color(0xFF64B5F6) else CWColors.TextSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- C. ViPER Convolver (IRS Acoustic Matrix) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "IRS Convolver Matrix",
                            style = CWTypography.AppTypography.bodyMedium,
                            color = CWColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Partitioned impulse response convolution (.irs / .wav)",
                            style = CWTypography.AppTypography.bodySmall,
                            color = CWColors.TextTertiary,
                            fontSize = 10.5.sp
                        )
                    }

                    Switch(
                        checked = config.isConvolverEnabled,
                        onCheckedChange = { viewModel.toggleConvolver(it) },
                        enabled = config.isEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CWColors.Background,
                            checkedTrackColor = CWColors.AccentCyan,
                            uncheckedThumbColor = CWColors.TextTertiary,
                            uncheckedTrackColor = CWColors.SurfaceOverlay
                        )
                    )
                }

                if (config.isConvolverEnabled && config.isEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                            .background(CWColors.SurfaceElevated)
                            .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusSmall))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "ACTIVE IRS PROFILE",
                                    style = CWTypography.TechBadge,
                                    color = CWColors.TextTertiary,
                                    fontSize = 8.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = config.irsName ?: "No profile loaded (Bypassed)",
                                    style = CWTypography.AppTypography.bodySmall,
                                    color = if (config.irsName != null) CWColors.AccentCyan else CWColors.TextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (config.irsName != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                                            .background(CWColors.SurfacePrimary)
                                            .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusSmall))
                                            .clickable { viewModel.setIrsName(null) }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "CLEAR",
                                            style = CWTypography.TechBadge,
                                            color = CWColors.Danger,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                                        .background(CWColors.AccentCyan.copy(alpha = 0.15f))
                                        .border(0.5.dp, CWColors.AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(CWShapes.RadiusSmall))
                                        .clickable { irsPickerLauncher.launch("*/*") }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "IMPORT IRS",
                                        style = CWTypography.TechBadge,
                                        color = CWColors.AccentCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Studio Sound Profile Hub (ZERO Horizontal Scrolling, Full Visibility)
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
                    .border(
                        1.dp,
                        if (config.isEnabled) CWColors.AccentCyan.copy(alpha = 0.45f) else CWColors.BorderSubtle,
                        RoundedCornerShape(CWShapes.RadiusLarge)
                    )
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
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CWColors.AccentCyan.copy(alpha = 0.15f))
                                .border(1.dp, CWColors.AccentCyan.copy(alpha = 0.3f), CircleShape),
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ACTIVE SOUND PROFILE",
                                    style = CWTypography.TechBadge,
                                    color = CWColors.TextTertiary,
                                    fontSize = 8.5.sp
                                )
                                val activePreset = presets.find { it.name == config.activePresetName }
                                if (activePreset?.isBuiltIn == true) {
                                    Text(
                                        text = "STUDIO",
                                        style = CWTypography.TechBadge,
                                        color = CWColors.AccentCyan,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = config.activePresetName,
                                style = CWTypography.AppTypography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CWColors.TextPrimary
                            )
                            val activePreset = presets.find { it.name == config.activePresetName }
                            if (activePreset != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = activePreset.profileSubtitle,
                                    style = CWTypography.AppTypography.bodySmall,
                                    color = CWColors.TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    softWrap = true,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Browse All action trigger
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                            .background(CWColors.SurfaceElevated)
                            .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusSmall))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "BROWSE (${presets.size})",
                                style = CWTypography.TechBadge,
                                color = CWColors.AccentCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = CWColors.AccentCyan,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Fixed Studio Preset Matrix (Row 1: 3 items, Row 2: 3 items — ZERO Horizontal Scrolling!)
            val row1 = listOf("Flat", "Bass Boost", "Vocal Clarity")
            val row2 = listOf("Electronic", "Rock")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row1.forEach { presetName ->
                    val preset = presets.find { it.name.equals(presetName, ignoreCase = true) }
                    val isSelected = config.activePresetName.equals(presetName, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                            .background(
                                if (isSelected) CWColors.AccentCyan.copy(alpha = 0.18f) else CWColors.SurfacePrimary
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CWColors.AccentCyan else CWColors.BorderSubtle,
                                shape = RoundedCornerShape(CWShapes.RadiusMedium)
                            )
                            .clickable {
                                preset?.let { viewModel.applyPreset(it) }
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CWColors.AccentCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = presetName,
                                style = CWTypography.TechBadge,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CWColors.AccentCyan else CWColors.TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row2.forEach { presetName ->
                    val preset = presets.find { it.name.equals(presetName, ignoreCase = true) }
                    val isSelected = config.activePresetName.equals(presetName, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                            .background(
                                if (isSelected) CWColors.AccentCyan.copy(alpha = 0.18f) else CWColors.SurfacePrimary
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CWColors.AccentCyan else CWColors.BorderSubtle,
                                shape = RoundedCornerShape(CWShapes.RadiusMedium)
                            )
                            .clickable {
                                preset?.let { viewModel.applyPreset(it) }
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CWColors.AccentCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = presetName,
                                style = CWTypography.TechBadge,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CWColors.AccentCyan else CWColors.TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 6th Slot: "MORE (12)..." opens bottom sheet
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                        .background(CWColors.SurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = CWColors.BorderSubtle,
                            shape = RoundedCornerShape(CWShapes.RadiusMedium)
                        )
                        .clickable { isPresetSheetOpen = true }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MORE (${presets.size})...",
                        style = CWTypography.TechBadge,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CWColors.AccentCyan,
                        maxLines = 1
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
 * Studio Bipolar Slider:
 * - Calibrated center-zero (0.0 dB) detent notch
 * - Cuts (< 0 dB) fill left in warm Coral (0xFFFF8A65)
 * - Boosts (> 0 dB) fill right in studio Cyan (0xFF00E5FF)
 * - Double-tap anywhere snaps immediately to 0.0 dB
 * - Smooth drag with 0.5 dB quantization
 */
@Composable
private fun StudioBipolarSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = -12f..12f,
    enabled: Boolean = true
) {
    val density = LocalDensity.current
    var localVal by remember(value) { mutableFloatStateOf(value) }
    LaunchedEffect(value) {
        localVal = value
    }

    val minVal = valueRange.start
    val maxVal = valueRange.endInclusive
    val span = maxVal - minVal

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onDoubleTap = {
                        localVal = 0f
                        onValueChange(0f)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val knobDiameterDp = 22.dp
        val knobRadiusPx = with(density) { (knobDiameterDp / 2).toPx() }
        val usableWidthPx = (widthPx - knobRadiusPx * 2).coerceAtLeast(1f)
        val centerRatio = (-minVal) / span
        val centerPosPx = knobRadiusPx + centerRatio * usableWidthPx

        val currentRatio = ((localVal - minVal) / span).coerceIn(0f, 1f)
        val currentKnobPx = knobRadiusPx + currentRatio * usableWidthPx

        // Track Canvas: Groove, center detent tick, and dynamic bipolar fill
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val cy = h / 2f
            val trackHeight = 6.dp.toPx()
            val trackCorner = 3.dp.toPx()

            // Groove background
            drawRoundRect(
                color = Color(0xFF0F1318),
                topLeft = Offset(knobRadiusPx, cy - trackHeight / 2),
                size = androidx.compose.ui.geometry.Size(usableWidthPx, trackHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackCorner, trackCorner)
            )

            // Groove border
            drawRoundRect(
                color = Color.White.copy(alpha = 0.1f),
                topLeft = Offset(knobRadiusPx, cy - trackHeight / 2),
                size = androidx.compose.ui.geometry.Size(usableWidthPx, trackHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackCorner, trackCorner),
                style = Stroke(width = 1f)
            )

            // Center 0 dB Detent Notch
            drawLine(
                color = if (localVal == 0f && enabled) CWColors.AccentCyan else Color.White.copy(alpha = 0.35f),
                start = Offset(centerPosPx, cy - 8.dp.toPx()),
                end = Offset(centerPosPx, cy + 8.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Bipolar Fill from Center to Knob
            if (enabled && localVal != 0f) {
                if (localVal > 0f) {
                    val fillStart = centerPosPx
                    val fillWidth = (currentKnobPx - centerPosPx).coerceAtLeast(0f)
                    if (fillWidth > 1f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(CWColors.AccentCyan.copy(alpha = 0.7f), CWColors.AccentCyan),
                                startX = fillStart,
                                endX = fillStart + fillWidth
                            ),
                            topLeft = Offset(fillStart, cy - trackHeight / 2),
                            size = androidx.compose.ui.geometry.Size(fillWidth, trackHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackCorner, trackCorner)
                        )
                    }
                } else {
                    val fillStart = currentKnobPx
                    val fillWidth = (centerPosPx - currentKnobPx).coerceAtLeast(0f)
                    if (fillWidth > 1f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF8A65), Color(0xFFFF8A65).copy(alpha = 0.7f)),
                                startX = fillStart,
                                endX = fillStart + fillWidth
                            ),
                            topLeft = Offset(fillStart, cy - trackHeight / 2),
                            size = androidx.compose.ui.geometry.Size(fillWidth, trackHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackCorner, trackCorner)
                        )
                    }
                }
            }
        }

        // Hardware Knob
        val knobOffsetDp = with(density) { (currentKnobPx - knobRadiusPx).toDp() }
        Box(
            modifier = Modifier
                .offset(x = knobOffsetDp)
                .size(knobDiameterDp)
                .clip(CircleShape)
                .background(
                    if (!enabled) CWColors.SurfacePrimary
                    else Color(0xFF1E2530)
                )
                .border(
                    width = 1.5.dp,
                    color = when {
                        !enabled -> CWColors.BorderSubtle
                        localVal > 0f -> CWColors.AccentCyan
                        localVal < 0f -> Color(0xFFFF8A65)
                        else -> Color.White.copy(alpha = 0.4f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Illuminated Center LED Indicator
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !enabled -> CWColors.TextTertiary
                            localVal > 0f -> CWColors.AccentCyan
                            localVal < 0f -> Color(0xFFFF8A65)
                            else -> Color.White.copy(alpha = 0.6f)
                        }
                    )
            )
        }

        // Touch Gesture Capture (Drag and Tap)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val startX = down.position.x
                        var isDragging = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!isDragging && System.currentTimeMillis() - downTime < 300L) {
                                    val relX = (startX - knobRadiusPx).coerceIn(0f, usableWidthPx)
                                    val frac = relX / usableWidthPx
                                    val targetVal = minVal + frac * span
                                    val snapped = ((targetVal * 2).roundToInt() / 2f).coerceIn(minVal, maxVal)
                                    localVal = snapped
                                    onValueChange(snapped)
                                }
                                break
                            }

                            if (!isDragging && kotlin.math.abs(change.position.x - startX) > 4f) {
                                isDragging = true
                            }

                            if (isDragging) {
                                change.consume()
                                val relX = (change.position.x - knobRadiusPx).coerceIn(0f, usableWidthPx)
                                val frac = relX / usableWidthPx
                                val targetVal = minVal + frac * span
                                val snapped = ((targetVal * 2).roundToInt() / 2f).coerceIn(minVal, maxVal)
                                if (snapped != localVal) {
                                    localVal = snapped
                                    onValueChange(snapped)
                                }
                            }
                        }
                    }
                }
        )
    }
}

/**
 * Custom Studio Hardware Unipolar Slider (0 dB to +12 dB) for ViPER Bass and Clarity.
 * Features illuminated progressive fill, precision detent ticks, and smooth 0.5 dB drag quantization.
 */
@Composable
private fun StudioUnipolarSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..12f,
    enabled: Boolean = true,
    activeColor: Color = CWColors.AccentCyan
) {
    val density = LocalDensity.current
    var localVal by remember(value) { mutableFloatStateOf(value) }
    LaunchedEffect(value) {
        localVal = value
    }

    val minVal = valueRange.start
    val maxVal = valueRange.endInclusive
    val span = maxVal - minVal

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onDoubleTap = {
                        localVal = minVal
                        onValueChange(minVal)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val knobDiameterDp = 22.dp
        val knobRadiusPx = with(density) { (knobDiameterDp / 2).toPx() }
        val usableWidthPx = (widthPx - knobRadiusPx * 2).coerceAtLeast(1f)

        val currentRatio = ((localVal - minVal) / span).coerceIn(0f, 1f)
        val currentKnobPx = knobRadiusPx + currentRatio * usableWidthPx

        // Track Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val cy = h / 2f
            val trackHeight = 6.dp.toPx()
            val trackCorner = 3.dp.toPx()

            // Groove background
            drawRoundRect(
                color = Color(0xFF0F1318),
                topLeft = Offset(knobRadiusPx, cy - trackHeight / 2),
                size = androidx.compose.ui.geometry.Size(usableWidthPx, trackHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackCorner, trackCorner)
            )

            // Groove border
            drawRoundRect(
                color = Color.White.copy(alpha = 0.1f),
                topLeft = Offset(knobRadiusPx, cy - trackHeight / 2),
                size = androidx.compose.ui.geometry.Size(usableWidthPx, trackHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackCorner, trackCorner),
                style = Stroke(width = 1f)
            )

            // Progressive Fill from Left to Knob
            if (enabled && localVal > minVal) {
                val fillWidth = (currentKnobPx - knobRadiusPx).coerceAtLeast(0f)
                if (fillWidth > 1f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(activeColor.copy(alpha = 0.4f), activeColor),
                            startX = knobRadiusPx,
                            endX = currentKnobPx
                        ),
                        topLeft = Offset(knobRadiusPx, cy - trackHeight / 2),
                        size = androidx.compose.ui.geometry.Size(fillWidth, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackCorner, trackCorner)
                    )
                }
            }
        }

        // Hardware Knob
        val knobOffsetDp = with(density) { (currentKnobPx - knobRadiusPx).toDp() }
        Box(
            modifier = Modifier
                .offset(x = knobOffsetDp)
                .size(knobDiameterDp)
                .clip(CircleShape)
                .background(
                    if (!enabled) CWColors.SurfacePrimary
                    else Color(0xFF1E2530)
                )
                .border(
                    width = 1.5.dp,
                    color = if (enabled) activeColor else CWColors.BorderSubtle,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled) activeColor
                        else CWColors.TextTertiary
                    )
            )
        }

        // Touch Gesture Capture (Drag and Tap)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val startX = down.position.x
                        var isDragging = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!isDragging && System.currentTimeMillis() - downTime < 300L) {
                                    val relX = (startX - knobRadiusPx).coerceIn(0f, usableWidthPx)
                                    val frac = relX / usableWidthPx
                                    val targetVal = minVal + frac * span
                                    val snapped = ((targetVal * 2).roundToInt() / 2f).coerceIn(minVal, maxVal)
                                    localVal = snapped
                                    onValueChange(snapped)
                                }
                                break
                            }

                            if (!isDragging && kotlin.math.abs(change.position.x - startX) > 4f) {
                                isDragging = true
                            }

                            if (isDragging) {
                                change.consume()
                                val relX = (change.position.x - knobRadiusPx).coerceIn(0f, usableWidthPx)
                                val frac = relX / usableWidthPx
                                val targetVal = minVal + frac * span
                                val snapped = ((targetVal * 2).roundToInt() / 2f).coerceIn(minVal, maxVal)
                                if (snapped != localVal) {
                                    localVal = snapped
                                    onValueChange(snapped)
                                }
                            }
                        }
                    }
                }
        )
    }
}

/**
 * Studio Parametric Curve Canvas:
 * - Acoustic frequency zones (SUB, BASS, MID, HIGH-MID, PRESENCE, AIR)
 * - Horizontal dB gridlines (+12, +6, 0, -6, -12 dB)
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
