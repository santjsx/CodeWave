package com.codewave.player.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

object CWColors {
    // Obsidian Theme Palette (PRD Section 4)
    val Background = Color(0xFF0B0D10)
    val SurfacePrimary = Color(0xFF11151A)
    val SurfaceElevated = Color(0xFF171C22)
    val SurfaceOverlay = Color(0xFF1F2630)
    val BorderSubtle = Color(0xFF212830)
    val BorderFocus = Color(0xFF38444D)

    // Accents
    val AccentCyan = Color(0xFF00E5FF)       // Primary: Electric Cyan
    val AccentViolet = Color(0xFF8A2BE2)     // Secondary: Muted Violet
    val AccentBlue = Color(0xFF2979FF)       // Active states

    // Functional
    val Success = Color(0xFF00E676)
    val Warning = Color(0xFFFFD600)
    val Danger = Color(0xFFFF1744)

    // Text & Metadata
    val TextPrimary = Color(0xFFF0F3F6)
    val TextSecondary = Color(0xFF8B949E)
    val TextTertiary = Color(0xFF484F58)
    val TextTechnical = Color(0xFF58A6FF)     // Developer monospaced telemetry

    // Quality Indicators
    val BadgeLosslessBg = Color(0x2600E5FF)
    val BadgeLosslessText = Color(0xFF00E5FF)
    val BadgeHiResBg = Color(0x33FFD600)
    val BadgeHiResText = Color(0xFFFFD600)
    val BadgeLossyBg = Color(0x1FFFFFFF)
    val BadgeLossyText = Color(0xFF8B949E)
}

@Immutable
data class CodeWaveColorScheme(
    val background: Color = CWColors.Background,
    val surfacePrimary: Color = CWColors.SurfacePrimary,
    val surfaceElevated: Color = CWColors.SurfaceElevated,
    val borderSubtle: Color = CWColors.BorderSubtle,
    val accent: Color = CWColors.AccentCyan,
    val accentSecondary: Color = CWColors.AccentViolet,
    val textPrimary: Color = CWColors.TextPrimary,
    val textSecondary: Color = CWColors.TextSecondary,
    val textTechnical: Color = CWColors.TextTechnical,
    val success: Color = CWColors.Success,
    val warning: Color = CWColors.Warning,
    val danger: Color = CWColors.Danger
)
