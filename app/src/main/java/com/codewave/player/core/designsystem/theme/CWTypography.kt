package com.codewave.player.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object CWTypography {
    val Monospace = FontFamily.Monospace

    val AppTypography = Typography(
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            color = CWColors.TextPrimary
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            color = CWColors.TextPrimary
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = CWColors.TextPrimary
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = CWColors.TextPrimary
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = CWColors.TextSecondary
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = CWColors.TextPrimary
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = CWColors.TextSecondary
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
            color = CWColors.TextSecondary
        )
    )

    // Technical monospaced text styles for audio telemetry
    val TechBadge = TextStyle(
        fontFamily = Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.8.sp
    )

    val TechTelemetry = TextStyle(
        fontFamily = Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = CWColors.TextTechnical
    )

    val TechInspectorHeader = TextStyle(
        fontFamily = Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.2.sp,
        color = CWColors.AccentCyan
    )

    val TechInspectorValue = TextStyle(
        fontFamily = Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = CWColors.TextPrimary
    )

    // VS Code Developer Typography Tokens
    val CodeLineNumber = TextStyle(
        fontFamily = Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = CWColors.TextSecondary
    )

    val CodeComment = TextStyle(
        fontFamily = Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp,
        color = CWColors.TextSecondary
    )

    val IdeTab = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.3.sp
    )

    val TerminalPrompt = TextStyle(
        fontFamily = Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        color = CWColors.AccentCyan
    )
}
