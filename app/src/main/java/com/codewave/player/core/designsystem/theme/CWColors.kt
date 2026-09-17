package com.codewave.player.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class PlayBarType {
    NEON_SLIM,
    DUAL_NEON,
    CLEAN_PILL,
    SEGMENTED,
    WARM_AMBER
}

data class CWThemeDefinition(
    val id: String,
    val name: String,
    val description: String,
    val playBarType: PlayBarType,
    val accentColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val previewColors: List<Color>
)

object CWColors {
    var activeThemeId: String by mutableStateOf("obsidian")
        private set

    // Active colors (mutated on theme change to immediately propagate everywhere)
    var Background: Color by mutableStateOf(Color(0xFF0D1117))
    var SurfacePrimary: Color by mutableStateOf(Color(0xFF161B22))
    var SurfaceElevated: Color by mutableStateOf(Color(0xFF1F242C))
    var SurfaceOverlay: Color by mutableStateOf(Color(0xFF262C36))
    var BorderSubtle: Color by mutableStateOf(Color(0xFF21262D))
    var BorderFocus: Color by mutableStateOf(Color(0xFF388BFD))

    var AccentCyan: Color by mutableStateOf(Color(0xFF388BFD))
    var AccentViolet: Color by mutableStateOf(Color(0xFF8A2BE2))
    var AccentBlue: Color by mutableStateOf(Color(0xFF58A6FF))

    var Success: Color by mutableStateOf(Color(0xFF2EA043))
    var Warning: Color by mutableStateOf(Color(0xFFD29922))
    var Danger: Color by mutableStateOf(Color(0xFFF85149))

    var TextPrimary: Color by mutableStateOf(Color(0xFFF0F6FC))
    var TextSecondary: Color by mutableStateOf(Color(0xFF8B949E))
    var TextTertiary: Color by mutableStateOf(Color(0xFF484F58))
    var TextTechnical: Color by mutableStateOf(Color(0xFF58A6FF))

    var BadgeLosslessBg: Color by mutableStateOf(Color(0x26388BFD))
    var BadgeLosslessText: Color by mutableStateOf(Color(0xFF58A6FF))
    var BadgeHiResBg: Color by mutableStateOf(Color(0x33FFD600))
    var BadgeHiResText: Color by mutableStateOf(Color(0xFFFFD600))
    var BadgeLossyBg: Color by mutableStateOf(Color(0x1FFFFFFF))
    var BadgeLossyText: Color by mutableStateOf(Color(0xFF8B949E))

    var CurrentPlayBarType: PlayBarType by mutableStateOf(PlayBarType.NEON_SLIM)

    val AvailableThemes: List<CWThemeDefinition> = listOf(
        CWThemeDefinition(
            id = "obsidian",
            name = "VS Code Dark+",
            description = "Official VS Code IDE workstation with electric blue accents, deep slate surfaces, and syntax highlighting",
            playBarType = PlayBarType.NEON_SLIM,
            accentColor = Color(0xFF388BFD),
            backgroundColor = Color(0xFF0D1117),
            surfaceColor = Color(0xFF161B22),
            previewColors = listOf(Color(0xFF0D1117), Color(0xFF161B22), Color(0xFF388BFD), Color(0xFF58A6FF))
        ),
        CWThemeDefinition(
            id = "synthwave",
            name = "Synthwave '84",
            description = "Neon cyberpunk outrun with hot pink lasers, sunset gold, and glowing grids",
            playBarType = PlayBarType.DUAL_NEON,
            accentColor = Color(0xFFFF007F),
            backgroundColor = Color(0xFF160E2E),
            surfaceColor = Color(0xFF241544),
            previewColors = listOf(Color(0xFF160E2E), Color(0xFF241544), Color(0xFFFF007F), Color(0xFFFFE600))
        ),
        CWThemeDefinition(
            id = "tokyo_night",
            name = "Tokyo Night Storm",
            description = "Midnight Tokyo reflections with soothing storm blue and purple syntax highlights",
            playBarType = PlayBarType.CLEAN_PILL,
            accentColor = Color(0xFF7AA2F7),
            backgroundColor = Color(0xFF1A1B26),
            surfaceColor = Color(0xFF24283B),
            previewColors = listOf(Color(0xFF1A1B26), Color(0xFF24283B), Color(0xFF7AA2F7), Color(0xFFBB9AF7))
        ),
        CWThemeDefinition(
            id = "dracula_pro",
            name = "Dracula Pro",
            description = "Classic hacker vampire dark mode with vibrant electric green and purple syntax",
            playBarType = PlayBarType.SEGMENTED,
            accentColor = Color(0xFF50FA7B),
            backgroundColor = Color(0xFF1E1F29),
            surfaceColor = Color(0xFF282A36),
            previewColors = listOf(Color(0xFF1E1F29), Color(0xFF282A36), Color(0xFF50FA7B), Color(0xFFBD93F9))
        ),
        CWThemeDefinition(
            id = "monokai_pro",
            name = "Monokai Pro",
            description = "Warm expressive spectrum designed for long development sessions and focus",
            playBarType = PlayBarType.WARM_AMBER,
            accentColor = Color(0xFFFFD866),
            backgroundColor = Color(0xFF19181A),
            surfaceColor = Color(0xFF221F22),
            previewColors = listOf(Color(0xFF19181A), Color(0xFF221F22), Color(0xFFFFD866), Color(0xFFFF6188))
        )
    )

    fun applyTheme(themeId: String): CodeWaveColorScheme {
        activeThemeId = themeId
        val scheme = when (themeId.lowercase()) {
            "synthwave" -> {
                Background = Color(0xFF160E2E)
                SurfacePrimary = Color(0xFF241544)
                SurfaceElevated = Color(0xFF2E1B57)
                SurfaceOverlay = Color(0xFF3B236E)
                BorderSubtle = Color(0xFF4A2B8A)
                BorderFocus = Color(0xFFFF007F)
                AccentCyan = Color(0xFFFF007F)      // Neon Magenta Pink
                AccentViolet = Color(0xFFE056FD)    // Neon Electric Violet
                AccentBlue = Color(0xFF00F0FF)      // Cyber Cyan
                Success = Color(0xFF00FF9F)
                Warning = Color(0xFFFFE600)
                Danger = Color(0xFFFF2A6D)
                TextPrimary = Color(0xFFFFF0F5)
                TextSecondary = Color(0xFFB392F0)
                TextTertiary = Color(0xFF6E5494)
                TextTechnical = Color(0xFF00F0FF)
                BadgeLosslessBg = Color(0x33FF007F)
                BadgeLosslessText = Color(0xFFFF007F)
                BadgeHiResBg = Color(0x33FFE600)
                BadgeHiResText = Color(0xFFFFE600)
                CurrentPlayBarType = PlayBarType.DUAL_NEON
                CodeWaveColorScheme(
                    background = Background,
                    surfacePrimary = SurfacePrimary,
                    surfaceElevated = SurfaceElevated,
                    borderSubtle = BorderSubtle,
                    accent = AccentCyan,
                    accentSecondary = AccentViolet,
                    textPrimary = TextPrimary,
                    textSecondary = TextSecondary,
                    textTechnical = TextTechnical,
                    success = Success,
                    warning = Warning,
                    danger = Danger
                )
            }
            "tokyo_night" -> {
                Background = Color(0xFF1A1B26)
                SurfacePrimary = Color(0xFF24283B)
                SurfaceElevated = Color(0xFF2F354D)
                SurfaceOverlay = Color(0xFF394161)
                BorderSubtle = Color(0xFF414868)
                BorderFocus = Color(0xFF7AA2F7)
                AccentCyan = Color(0xFF7AA2F7)      // Tokyo Blue
                AccentViolet = Color(0xFFBB9AF7)    // Purple
                AccentBlue = Color(0xFF7DCFFF)      // Ice Cyan
                Success = Color(0xFF9ECE6A)
                Warning = Color(0xFFE0AF68)
                Danger = Color(0xFFF7768E)
                TextPrimary = Color(0xFFC0CAF5)
                TextSecondary = Color(0xFF7982A9)
                TextTertiary = Color(0xFF565F89)
                TextTechnical = Color(0xFF7DCFFF)
                BadgeLosslessBg = Color(0x337AA2F7)
                BadgeLosslessText = Color(0xFF7AA2F7)
                BadgeHiResBg = Color(0x33E0AF68)
                BadgeHiResText = Color(0xFFE0AF68)
                CurrentPlayBarType = PlayBarType.CLEAN_PILL
                CodeWaveColorScheme(
                    background = Background,
                    surfacePrimary = SurfacePrimary,
                    surfaceElevated = SurfaceElevated,
                    borderSubtle = BorderSubtle,
                    accent = AccentCyan,
                    accentSecondary = AccentViolet,
                    textPrimary = TextPrimary,
                    textSecondary = TextSecondary,
                    textTechnical = TextTechnical,
                    success = Success,
                    warning = Warning,
                    danger = Danger
                )
            }
            "dracula_pro" -> {
                Background = Color(0xFF1E1F29)
                SurfacePrimary = Color(0xFF282A36)
                SurfaceElevated = Color(0xFF343746)
                SurfaceOverlay = Color(0xFF44475A)
                BorderSubtle = Color(0xFF4A4E69)
                BorderFocus = Color(0xFF50FA7B)
                AccentCyan = Color(0xFF50FA7B)      // Vampire Green
                AccentViolet = Color(0xFFBD93F9)    // Dracula Purple
                AccentBlue = Color(0xFF8BE9FD)      // Cyan
                Success = Color(0xFF50FA7B)
                Warning = Color(0xFFF1FA8C)
                Danger = Color(0xFFFF5555)
                TextPrimary = Color(0xFFF8F8F2)
                TextSecondary = Color(0xFF99A4C4)
                TextTertiary = Color(0xFF6272A4)
                TextTechnical = Color(0xFF8BE9FD)
                BadgeLosslessBg = Color(0x3350FA7B)
                BadgeLosslessText = Color(0xFF50FA7B)
                BadgeHiResBg = Color(0x33F1FA8C)
                BadgeHiResText = Color(0xFFF1FA8C)
                CurrentPlayBarType = PlayBarType.SEGMENTED
                CodeWaveColorScheme(
                    background = Background,
                    surfacePrimary = SurfacePrimary,
                    surfaceElevated = SurfaceElevated,
                    borderSubtle = BorderSubtle,
                    accent = AccentCyan,
                    accentSecondary = AccentViolet,
                    textPrimary = TextPrimary,
                    textSecondary = TextSecondary,
                    textTechnical = TextTechnical,
                    success = Success,
                    warning = Warning,
                    danger = Danger
                )
            }
            "monokai_pro" -> {
                Background = Color(0xFF19181A)
                SurfacePrimary = Color(0xFF221F22)
                SurfaceElevated = Color(0xFF2D2A2E)
                SurfaceOverlay = Color(0xFF3D3A3E)
                BorderSubtle = Color(0xFF4B484C)
                BorderFocus = Color(0xFFFFD866)
                AccentCyan = Color(0xFFFFD866)      // Sunburst Yellow
                AccentViolet = Color(0xFFFF6188)    // Monokai Rose
                AccentBlue = Color(0xFF78DCE8)      // Mint Cyan
                Success = Color(0xFFA9DC76)
                Warning = Color(0xFFFFD866)
                Danger = Color(0xFFFF6188)
                TextPrimary = Color(0xFFFCFCFA)
                TextSecondary = Color(0xFF939293)
                TextTertiary = Color(0xFF5B595C)
                TextTechnical = Color(0xFF78DCE8)
                BadgeLosslessBg = Color(0x33FFD866)
                BadgeLosslessText = Color(0xFFFFD866)
                BadgeHiResBg = Color(0x33A9DC76)
                BadgeHiResText = Color(0xFFA9DC76)
                CurrentPlayBarType = PlayBarType.WARM_AMBER
                CodeWaveColorScheme(
                    background = Background,
                    surfacePrimary = SurfacePrimary,
                    surfaceElevated = SurfaceElevated,
                    borderSubtle = BorderSubtle,
                    accent = AccentCyan,
                    accentSecondary = AccentViolet,
                    textPrimary = TextPrimary,
                    textSecondary = TextSecondary,
                    textTechnical = TextTechnical,
                    success = Success,
                    warning = Warning,
                    danger = Danger
                )
            }
            else -> { // Default VS Code Dark+
                Background = Color(0xFF0D1117)
                SurfacePrimary = Color(0xFF161B22)
                SurfaceElevated = Color(0xFF1F242C)
                SurfaceOverlay = Color(0xFF262C36)
                BorderSubtle = Color(0xFF212830)
                BorderFocus = Color(0xFF388BFD)
                AccentCyan = Color(0xFF388BFD)
                AccentViolet = Color(0xFF8A2BE2)
                AccentBlue = Color(0xFF58A6FF)
                Success = Color(0xFF2EA043)
                Warning = Color(0xFFD29922)
                Danger = Color(0xFFF85149)
                TextPrimary = Color(0xFFF0F6FC)
                TextSecondary = Color(0xFF8B949E)
                TextTertiary = Color(0xFF484F58)
                TextTechnical = Color(0xFF58A6FF)
                BadgeLosslessBg = Color(0x26388BFD)
                BadgeLosslessText = Color(0xFF58A6FF)
                BadgeHiResBg = Color(0x33FFD600)
                BadgeHiResText = Color(0xFFFFD600)
                CurrentPlayBarType = PlayBarType.NEON_SLIM
                CodeWaveColorScheme(
                    background = Background,
                    surfacePrimary = SurfacePrimary,
                    surfaceElevated = SurfaceElevated,
                    borderSubtle = BorderSubtle,
                    accent = AccentCyan,
                    accentSecondary = AccentViolet,
                    textPrimary = TextPrimary,
                    textSecondary = TextSecondary,
                    textTechnical = TextTechnical,
                    success = Success,
                    warning = Warning,
                    danger = Danger
                )
            }
        }
        return scheme
    }
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
