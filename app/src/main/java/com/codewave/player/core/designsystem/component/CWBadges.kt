package com.codewave.player.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWTypography

@Composable
fun CWQualityBadge(
    text: String,
    modifier: Modifier = Modifier,
    isHiRes: Boolean = false,
    isLossless: Boolean = false
) {
    val (bgColor, textColor, borderColor) = when {
        isHiRes -> Triple(CWColors.BadgeHiResBg, CWColors.BadgeHiResText, CWColors.BadgeHiResText.copy(alpha = 0.4f))
        isLossless -> Triple(CWColors.BadgeLosslessBg, CWColors.BadgeLosslessText, CWColors.BadgeLosslessText.copy(alpha = 0.3f))
        else -> Triple(CWColors.BadgeLossyBg, CWColors.BadgeLossyText, Color.Transparent)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(3.dp))
            .border(0.75.dp, borderColor, RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = CWTypography.TechBadge,
            color = textColor
        )
    }
}

@Composable
fun CWTechnicalBadge(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = CWColors.TextSecondary
) {
    Box(
        modifier = modifier
            .background(CWColors.SurfaceOverlay, RoundedCornerShape(3.dp))
            .border(0.5.dp, CWColors.BorderSubtle, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = CWTypography.TechBadge,
            color = textColor
        )
    }
}
