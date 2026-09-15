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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
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

    Column(
        modifier = modifier
            .width(58.dp)
            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
            .background(CWColors.SurfacePrimary)
            .border(0.75.dp, if (localGain != 0f && enabled) CWColors.BorderFocus else CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onDoubleTap = {
                        localGain = 0f
                        onGainChange(0f)
                    }
                )
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gain Readout (Double tap anywhere on fader card to reset to 0 dB)
        Text(
            text = if (localGain == 0f) "0.0" else "%+.1f".format(localGain),
            style = CWTypography.TechTelemetry,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (localGain != 0f && enabled) CWColors.AccentCyan else CWColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Fader Track & Knob Container with Unified Gestures
        BoxWithConstraints(
            modifier = Modifier
                .height(134.dp)
                .width(46.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackHeightPx = constraints.maxHeight.toFloat()
            val knobHeightDp = 22.dp
            val knobHeightPx = with(density) { knobHeightDp.toPx() }
            val travelRangePx = (trackHeightPx - knobHeightPx).coerceAtLeast(1f)

            // Center vertical track groove
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(134.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CWColors.SurfaceElevated)
                    .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(2.dp))
            )

            // Center 0 dB Reference Tick Notch
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(1.5.dp)
                    .background(CWColors.BorderFocus.copy(alpha = 0.6f))
            )

            // Fader Knob
            val normPos = ((maxGainDb - localGain) / (maxGainDb - minGainDb)).coerceIn(0f, 1f)
            val knobOffsetPx = (normPos * travelRangePx).roundToInt()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, knobOffsetPx - (travelRangePx / 2).roundToInt()) }
                    .height(knobHeightDp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (enabled) CWColors.SurfaceOverlay else CWColors.SurfaceElevated)
                    .border(
                        1.dp,
                        if (enabled) CWColors.BorderFocus else CWColors.BorderSubtle,
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Tactile grip line
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (enabled) CWColors.AccentCyan else CWColors.TextTertiary)
                )
            }

            // Dedicated Unified Touch Gesture Overlay
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
                                    // Pointer released
                                    if (!isDragging && System.currentTimeMillis() - downTime < 300L) {
                                        // Tap on track groove directly positions the knob
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
                                if (!isDragging && kotlin.math.abs(change.position.y - startY) > 6f) {
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

        Spacer(modifier = Modifier.height(8.dp))

        // Frequency Label
        Text(
            text = label,
            style = CWTypography.TechBadge,
            color = if (enabled) CWColors.TextPrimary else CWColors.TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}
