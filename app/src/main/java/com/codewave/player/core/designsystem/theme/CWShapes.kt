package com.codewave.player.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object CWShapes {
    val RadiusSmall = 4.dp
    val RadiusMedium = 8.dp
    val RadiusLarge = 12.dp
    val RadiusFull = 999.dp

    val AppShapes = Shapes(
        small = RoundedCornerShape(RadiusSmall),
        medium = RoundedCornerShape(RadiusMedium),
        large = RoundedCornerShape(RadiusLarge)
    )

    // Spacing scale
    val Space2 = 2.dp
    val Space4 = 4.dp
    val Space8 = 8.dp
    val Space12 = 12.dp
    val Space16 = 16.dp
    val Space20 = 20.dp
    val Space24 = 24.dp
    val Space32 = 32.dp
}
