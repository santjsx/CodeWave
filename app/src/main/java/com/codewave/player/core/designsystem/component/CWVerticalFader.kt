package com.codewave.player.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CWVerticalFader(
    label: String,
    gainDb: Float,
    enabled: Boolean,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    minGainDb: Float = -12f,
    maxGainDb: Float = 12f
) {
    val density = LocalDensity.current
    var localGain by remember(gainDb) { mutableFloatStateOf(gainDb) }

    LaunchedEffect(gainDb) {
        localGain = gainDb
    }

    val isActive = localGain != 0f && enabled

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CWShapes.RadiusSmall))
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onDoubleTap = {
                        localGain = 0f
                        onGainChange(0f)
                    }
                )
            }
            .padding(vertical = 4.dp, horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gain Readout (Studio monospace telemetry with color coding)
        Text(
            text = if (localGain == 0f) "0.0" else "%+.1f".format(localGain),
            style = CWTypography.TechTelemetry,
            fontSize = 8.5.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = when {
                !enabled -> CWColors.TextTertiary
                localGain > 0f -> CWColors.AccentCyan
                localGain < 0f -> Color(0xFFFF8A65)
                else -> CWColors.TextTertiary
            },
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Fader Track & Knob Container with Unified Gestures
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackHeightPx = constraints.maxHeight.toFloat()
            val knobHeightDp = 18.dp
            val knobHeightPx = with(density) { knobHeightDp.toPx() }
            val travelRangePx = (trackHeightPx - knobHeightPx).coerceAtLeast(1f)

            // Center vertical track groove
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF0F1318))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
            )

            // 0 dB Center Zero Line Notch
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(1.dp)
                    .background(CWColors.BorderFocus.copy(alpha = 0.8f))
            )

            val normPos = ((maxGainDb - localGain) / (maxGainDb - minGainDb)).coerceIn(0f, 1f)
            val knobOffsetPx = (normPos * travelRangePx).roundToInt()

            // Active LED Level Fill from 0 dB to Knob Position
            val centerPosPx = travelRangePx / 2f
            val currentKnobCenterPx = knobOffsetPx.toFloat()
            val fillHeightPx = abs(currentKnobCenterPx - centerPosPx)
            val fillOffsetPx = (minOf(currentKnobCenterPx, centerPosPx) - (travelRangePx / 2f)).roundToInt()

            if (isActive && fillHeightPx > 2f) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(with(density) { fillHeightPx.toDp() })
                        .offset { IntOffset(0, fillOffsetPx + with(density) { (fillHeightPx / 2f).toDp().roundToPx() }) }
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(
                            if (localGain > 0f) Brush.verticalGradient(listOf(CWColors.AccentCyan, CWColors.AccentCyan.copy(alpha = 0.3f)))
                            else Brush.verticalGradient(listOf(Color(0xFFFF8A65).copy(alpha = 0.3f), Color(0xFFFF8A65)))
                        )
                )
            }

            // High-End Studio Fader Cap
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .offset { IntOffset(0, knobOffsetPx - (travelRangePx / 2).roundToInt()) }
                    .height(knobHeightDp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (!enabled) CWColors.SurfacePrimary
                        else if (isActive) Color(0xFF1E2530)
                        else Color(0xFF181C22)
                    )
                    .border(
                        1.dp,
                        if (isActive && enabled) CWColors.AccentCyan else Color.White.copy(alpha = 0.18f),
                        RoundedCornerShape(3.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Precision Illuminated Center Tick
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            when {
                                !enabled -> CWColors.TextTertiary
                                localGain > 0f -> CWColors.AccentCyan
                                localGain < 0f -> Color(0xFFFF8A65)
                                else -> Color.White.copy(alpha = 0.6f)
                            }
                        )
                )
            }

            // Touch Gesture Overlay (Full Column Capture)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downTime = System.currentTimeMillis()
                            var currentY = down.position.y
                            val startY = down.position.y
                            var isDragging = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    if (!isDragging && System.currentTimeMillis() - downTime < 280L) {
                                        val knobRadius = knobHeightPx / 2f
                                        val clampedY = (startY - knobRadius).coerceIn(0f, travelRangePx)
                                        val fraction = (clampedY / travelRangePx).coerceIn(0f, 1f)
                                        val targetGain = maxGainDb - fraction * (maxGainDb - minGainDb)
                                        val snapped = ((targetGain * 2).roundToInt() / 2f).coerceIn(minGainDb, maxGainDb)
                                        localGain = snapped
                                        onGainChange(snapped)
                                    }
                                    break
                                }

                                val deltaY = change.position.y - currentY
                                if (!isDragging && abs(change.position.y - startY) > 5f) {
                                    isDragging = true
                                }

                                if (isDragging) {
                                    change.consume()
                                    val deltaDb = -(deltaY / travelRangePx) * (maxGainDb - minGainDb)
                                    localGain = (localGain + deltaDb).coerceIn(minGainDb, maxGainDb)
                                    val snapped = ((localGain * 2).roundToInt() / 2f).coerceIn(minGainDb, maxGainDb)
                                    onGainChange(snapped)
                                    currentY = change.position.y
                                }
                            }
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Frequency Label (VS Code syntax tag)
        Text(
            text = label,
            style = CWTypography.TechBadge,
            color = if (isActive) CWColors.AccentCyan else CWColors.TextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
