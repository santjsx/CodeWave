package com.codewave.player.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.theme.CWColors
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CWAudioWaveformBox(
    isPlaying: Boolean,
    volumePercent: Int,
    waveformBands: FloatArray? = null,
    onVolumeChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnimation")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing)
        ),
        label = "WaveformPhase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF11151C))
            .border(1.dp, Color(0xFF21262D), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Header: Audio Output Status & Volume
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Clean Technical Status Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) CWColors.AccentCyan else Color(0xFF6E7681))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "AUDIO OUTPUT • ACTIVE" else "AUDIO OUTPUT • PAUSED",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.5.sp,
                        letterSpacing = 0.8.sp,
                        color = if (isPlaying) Color(0xFFC9D1D9) else Color(0xFF6E7681)
                    )
                }

                // Right: Volume Readout & Visual Line with optional tap / drag gesture
                val haptic = LocalHapticFeedback.current
                val barWidthDp = 64.dp

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    Text(
                        text = if (volumePercent <= 0) "MUTED" else "VOLUME $volumePercent%",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp,
                        color = if (volumePercent > 0) Color(0xFF8B949E) else Color(0xFFE5534B)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(barWidthDp)
                            .height(10.dp)
                            .pointerInput(onVolumeChange) {
                                if (onVolumeChange != null) {
                                    detectTapGestures { offset ->
                                        val percent = ((offset.x / size.width.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onVolumeChange(percent)
                                    }
                                }
                            }
                            .pointerInput(onVolumeChange) {
                                if (onVolumeChange != null) {
                                    detectHorizontalDragGestures { change, _ ->
                                        val percent = ((change.position.x / size.width.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
                                        onVolumeChange(percent)
                                    }
                                }
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Background track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color(0xFF21262D))
                        ) {
                            // Active volume fill
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (volumePercent / 100f).coerceIn(0f, 1f))
                                    .height(2.5.dp)
                                    .background(CWColors.AccentCyan)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Center Waveform Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                val barCount = 48
                val barWidth = 2.dp.toPx()
                val totalBarsWidth = barCount * barWidth
                val spacing = (size.width - totalBarsWidth) / (barCount - 1).coerceAtLeast(1)
                val midY = size.height / 2f

                val hasLiveBands = isPlaying && waveformBands != null && waveformBands.isNotEmpty()
                val minBarHeightPx = 3.dp.toPx()

                for (i in 0 until barCount) {
                    val normalizedIndex = i.toFloat() / barCount
                    // Bell envelope shape (higher in middle, tapering at sides)
                    val envelope = sin(normalizedIndex * Math.PI).toFloat()

                    val barHeight = if (hasLiveBands) {
                        val bandMagnitude = waveformBands.getOrNull(i) ?: 0.05f
                        ((size.height * 0.95f) * bandMagnitude).coerceIn(minBarHeightPx, size.height)
                    } else if (isPlaying) {
                        // Fallback procedural wave animation if audio buffer not yet populated
                        val w1 = sin(normalizedIndex * 12.0 + phase).toFloat()
                        val w2 = sin(normalizedIndex * 6.0 - phase * 1.5).toFloat()
                        val waveModifier = 0.4f + 0.35f * (w1 * 0.6f + w2 * 0.4f + 1f)
                        ((size.height * 0.9f) * envelope * waveModifier).coerceAtLeast(minBarHeightPx)
                    } else {
                        // Calm resting dots when paused
                        (minBarHeightPx + (size.height * 0.08f) * envelope)
                    }

                    val x = i * (barWidth + spacing)
                    val y = midY - barHeight / 2f

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF58A6FF),
                                Color(0xFF388BFD),
                                Color(0xFF1F6FEB)
                            ),
                            startY = y,
                            endY = y + barHeight
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}
