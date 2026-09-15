package com.codewave.player.core.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
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

@Composable
fun CodeWaveTheme(
    themeId: String = "obsidian",
    content: @Composable () -> Unit
) {
    val scheme = remember(themeId) {
        CWColors.applyTheme(themeId)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = scheme.background.toArgb()
            window.navigationBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    val darkMaterialColorScheme = darkColorScheme(
        primary = scheme.accent,
        onPrimary = scheme.background,
        primaryContainer = scheme.surfaceElevated,
        onPrimaryContainer = scheme.textPrimary,
        secondary = scheme.accentSecondary,
        onSecondary = scheme.textPrimary,
        background = scheme.background,
        onBackground = scheme.textPrimary,
        surface = scheme.surfacePrimary,
        onSurface = scheme.textPrimary,
        surfaceVariant = scheme.surfaceElevated,
        onSurfaceVariant = scheme.textSecondary,
        outline = scheme.borderSubtle,
        error = scheme.danger,
        onError = scheme.textPrimary
    )

    CompositionLocalProvider(
        LocalCodeWaveColors provides scheme
    ) {
        MaterialTheme(
            colorScheme = darkMaterialColorScheme,
            typography = CWTypography.AppTypography,
            shapes = CWShapes.AppShapes,
            content = content
        )
    }
}
