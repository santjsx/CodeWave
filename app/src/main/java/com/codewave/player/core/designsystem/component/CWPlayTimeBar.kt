package com.codewave.player.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.PlayBarType
import com.codewave.player.core.model.ABLoopState
import kotlin.math.abs

@Composable
fun CWPlayTimeBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    playBarType: PlayBarType = CWColors.CurrentPlayBarType,
    abLoopState: ABLoopState? = null
) {
    val duration = durationMs.coerceAtLeast(1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val activeFraction = if (isDragging) dragFraction else (currentPositionMs.toFloat() / duration).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(duration) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val initialFraction = (down.position.x / size.width).coerceIn(0f, 1f)
                    isDragging = true
                    dragFraction = initialFraction

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed) {
                            val currentX = change.position.x
                            dragFraction = (currentX / size.width).coerceIn(0f, 1f)
                            if (abs(currentX - down.position.x) > 2f) {
                                change.consume()
                            }
                        } else {
                            change.consume()
                            isDragging = false
                            val finalSeekMs = (dragFraction * duration).toLong().coerceIn(0L, duration)
                            onSeek(finalSeekMs)
                            break
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val activeWidth = (width * activeFraction).coerceIn(0f, width)

            when (playBarType) {
                PlayBarType.NEON_SLIM -> {
                    // Obsidian: Precision laser track with cyan glow thumb
                    val trackHeight = 3.dp.toPx()
                    drawRoundRect(
                        color = CWColors.SurfaceOverlay,
                        topLeft = Offset(0f, centerY - trackHeight / 2f),
                        size = Size(width, trackHeight),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                    drawRoundRect(
                        color = CWColors.AccentCyan,
                        topLeft = Offset(0f, centerY - trackHeight / 2f),
                        size = Size(activeWidth, trackHeight),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                    // Thumb
                    drawCircle(
                        color = CWColors.AccentCyan.copy(alpha = 0.25f),
                        radius = 10.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                    drawCircle(
                        color = CWColors.AccentCyan,
                        radius = 3.5.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                }

                PlayBarType.DUAL_NEON -> {
                    // Synthwave: Neon Gradient track with hot pink glow
                    val trackHeight = 5.dp.toPx()
                    val inactiveBrush = Brush.horizontalGradient(
                        colors = listOf(CWColors.SurfaceOverlay, CWColors.SurfaceElevated)
                    )
                    val activeBrush = Brush.horizontalGradient(
                        colors = listOf(CWColors.AccentCyan, CWColors.AccentViolet)
                    )

                    drawRoundRect(
                        brush = inactiveBrush,
                        topLeft = Offset(0f, centerY - trackHeight / 2f),
                        size = Size(width, trackHeight),
                        cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                    )
                    drawRoundRect(
                        brush = activeBrush,
                        topLeft = Offset(0f, centerY - trackHeight / 2f),
                        size = Size(activeWidth, trackHeight),
                        cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                    )
                    // Clean, crisp thumb with theme-matched neon outer ring (no yellow halo)
                    drawCircle(
                        color = CWColors.AccentCyan.copy(alpha = 0.28f),
                        radius = 11.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                    drawCircle(
                        color = CWColors.AccentCyan,
                        radius = 4.5.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                }

                PlayBarType.CLEAN_PILL -> {
                    // Tokyo Night: Pill Track
                    val trackHeight = 7.dp.toPx()
                    drawRoundRect(
                        color = CWColors.SurfaceOverlay,
                        topLeft = Offset(0f, centerY - trackHeight / 2f),
                        size = Size(width, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                    )
                    drawRoundRect(
                        color = CWColors.AccentCyan,
                        topLeft = Offset(0f, centerY - trackHeight / 2f),
                        size = Size(activeWidth, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                    )
                    drawCircle(
                        color = CWColors.AccentCyan.copy(alpha = 0.25f),
                        radius = 10.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5.5.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                }

                PlayBarType.SEGMENTED -> {
                    // Dracula: High-tech segmented laser track
                    val totalSegments = 28
                    val segmentSpacing = 3.dp.toPx()
                    val segmentWidth = (width - (totalSegments - 1) * segmentSpacing) / totalSegments
                    val segHeight = 5.dp.toPx()

                    for (i in 0 until totalSegments) {
                        val segStartX = i * (segmentWidth + segmentSpacing)
                        val isFilled = (segStartX + segmentWidth / 2f) <= activeWidth
                        val segColor = if (isFilled) CWColors.AccentCyan else CWColors.SurfaceOverlay
                        drawRoundRect(
                            color = segColor,
                            topLeft = Offset(segStartX, centerY - segHeight / 2f),
                            size = Size(segmentWidth, segHeight),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                        )
                    }

                    drawCircle(
                        color = CWColors.AccentCyan.copy(alpha = 0.3f),
                        radius = 10.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                    drawCircle(
                        color = CWColors.AccentCyan,
                        radius = 3.5.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                }

                PlayBarType.WARM_AMBER -> {
                    // Monokai Pro: Warm Amber Gradient
                    val trackHeight = 5.dp.toPx()
                    drawRoundRect(
                        color = CWColors.SurfaceOverlay,
                        topLeft = Offset(0f, centerY - trackHeight / 2f),
                        size = Size(width, trackHeight),
                        cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                    )
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(CWColors.AccentViolet, CWColors.AccentCyan)
                        ),
                        topLeft = Offset(0f, centerY - trackHeight / 2f),
                        size = Size(activeWidth, trackHeight),
                        cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                    )
                    drawCircle(
                        color = CWColors.AccentCyan.copy(alpha = 0.28f),
                        radius = 11.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                    drawCircle(
                        color = CWColors.AccentCyan,
                        radius = 4.5.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                }
            }

            // Render A-B Looper Visual Markers on the scrub bar (Feature 3.1)
            if (abLoopState != null && duration > 0L) {
                val a = abLoopState.pointA
                val b = abLoopState.pointB
                val aX = a?.let { (width * (it.toFloat() / duration)).coerceIn(0f, width) }
                val bX = b?.let { (width * (it.toFloat() / duration)).coerceIn(0f, width) }

                // If both points configured, highlight the loop region
                if (aX != null && bX != null && bX > aX) {
                    val regionColor = if (abLoopState.isEnabled) CWColors.AccentCyan.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f)
                    drawRoundRect(
                        color = regionColor,
                        topLeft = Offset(aX, centerY - 6.dp.toPx()),
                        size = Size(bX - aX, 12.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                // Point A tick & pin
                if (aX != null) {
                    drawLine(
                        color = CWColors.AccentCyan,
                        start = Offset(aX, centerY - 8.dp.toPx()),
                        end = Offset(aX, centerY + 8.dp.toPx()),
                        strokeWidth = 2.5.dp.toPx()
                    )
                }

                // Point B tick & pin
                if (bX != null) {
                    drawLine(
                        color = if (abLoopState.isEnabled) CWColors.AccentCyan else Color(0xFFFF9100),
                        start = Offset(bX, centerY - 8.dp.toPx()),
                        end = Offset(bX, centerY + 8.dp.toPx()),
                        strokeWidth = 2.5.dp.toPx()
                    )
                }
            }
        }
    }
}
