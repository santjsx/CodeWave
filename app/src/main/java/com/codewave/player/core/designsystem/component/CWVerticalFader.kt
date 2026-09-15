package com.codewave.player.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    Column(
        modifier = modifier
            .width(56.dp)
            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
            .background(CWColors.SurfacePrimary)
            .border(0.75.dp, CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gain Readout
        Text(
            text = if (gainDb == 0f) "0.0" else "%+.1f".format(gainDb),
            style = CWTypography.TechTelemetry,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (gainDb != 0f && enabled) CWColors.AccentCyan else CWColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Fader Track & Knob Container
        BoxWithConstraints(
            modifier = Modifier
                .height(130.dp)
                .width(44.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onDoubleTap = {
                            onGainChange(0f)
                        },
                        onTap = { offset ->
                            val trackHeight = size.height.toFloat()
                            val fraction = (offset.y / trackHeight).coerceIn(0f, 1f)
                            val newGain = maxGainDb - fraction * (maxGainDb - minGainDb)
                            onGainChange(((newGain * 2).roundToInt() / 2f).coerceIn(minGainDb, maxGainDb))
                        }
                    )
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val trackHeight = size.height.toFloat()
                        val deltaDb = -(dragAmount / trackHeight) * (maxGainDb - minGainDb)
                        val updatedGain = (gainDb + deltaDb).coerceIn(minGainDb, maxGainDb)
                        onGainChange(((updatedGain * 2).roundToInt() / 2f).coerceIn(minGainDb, maxGainDb))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val trackHeightPx = constraints.maxHeight.toFloat()
            val knobHeightDp = 22.dp
            val knobHeightPx = with(density) { knobHeightDp.toPx() }
            val travelRangePx = trackHeightPx - knobHeightPx

            // Center vertical track groove
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CWColors.SurfaceElevated)
                    .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(2.dp))
            )

            // Center 0 dB Reference Tick Notch
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(1.5.dp)
                    .background(CWColors.BorderFocus.copy(alpha = 0.6f))
            )

            // Fader Knob
            val normPos = (maxGainDb - gainDb) / (maxGainDb - minGainDb) // 0 at top (+12), 1 at bottom (-12)
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
