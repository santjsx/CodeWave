package com.codewave.player.core.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LocalCodeWaveColors = staticCompositionLocalOf { CodeWaveColorScheme() }

object CodeWaveTheme {
    val colors: CodeWaveColorScheme
        @Composable
        get() = LocalCodeWaveColors.current
}

private val DarkMaterialColorScheme = darkColorScheme(
    primary = CWColors.AccentCyan,
    onPrimary = CWColors.Background,
    primaryContainer = CWColors.SurfaceElevated,
    onPrimaryContainer = CWColors.TextPrimary,
    secondary = CWColors.AccentViolet,
    onSecondary = CWColors.TextPrimary,
    background = CWColors.Background,
    onBackground = CWColors.TextPrimary,
    surface = CWColors.SurfacePrimary,
    onSurface = CWColors.TextPrimary,
    surfaceVariant = CWColors.SurfaceElevated,
    onSurfaceVariant = CWColors.TextSecondary,
    outline = CWColors.BorderSubtle,
    error = CWColors.Danger,
    onError = CWColors.TextPrimary
)

@Composable
fun CodeWaveTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CWColors.Background.toArgb()
            window.navigationBarColor = CWColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(
        LocalCodeWaveColors provides CodeWaveColorScheme()
    ) {
        MaterialTheme(
            colorScheme = DarkMaterialColorScheme,
            typography = CWTypography.AppTypography,
            shapes = CWShapes.AppShapes,
            content = content
        )
    }
}
