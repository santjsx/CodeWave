package com.codewave.player.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography

enum class CWButtonVariant {
    SOLID,
    OUTLINED,
    GHOST
}

fun Color.contentColor(): Color {
    val lum = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return if (lum > 0.42f) Color(0xFF0D1117) else Color.White
}

@Composable
fun CWButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CWButtonVariant = CWButtonVariant.SOLID,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    when (variant) {
        CWButtonVariant.SOLID -> {
            val contentColor = CWColors.AccentCyan.contentColor()
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = RoundedCornerShape(CWShapes.RadiusMedium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CWColors.AccentCyan,
                    contentColor = contentColor,
                    disabledContainerColor = CWColors.SurfaceElevated,
                    disabledContentColor = CWColors.TextTertiary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = text,
                    style = CWTypography.AppTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
            }
        }
        CWButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = RoundedCornerShape(CWShapes.RadiusMedium),
                border = BorderStroke(1.dp, if (enabled) CWColors.BorderFocus else CWColors.BorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CWColors.TextPrimary,
                    disabledContentColor = CWColors.TextTertiary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (enabled) CWColors.TextPrimary else CWColors.TextTertiary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = text,
                    style = CWTypography.AppTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (enabled) CWColors.TextPrimary else CWColors.TextTertiary
                )
            }
        }
        CWButtonVariant.GHOST -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = RoundedCornerShape(CWShapes.RadiusMedium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = CWColors.TextSecondary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = CWColors.TextSecondary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Text(
                    text = text,
                    style = CWTypography.AppTypography.bodyMedium,
                    color = CWColors.TextSecondary
                )
            }
        }
    }
}

@Composable
fun CWPlayAllButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Play All",
    isShuffle: Boolean = false,
    leadingIcon: ImageVector = if (isShuffle) Icons.Default.Shuffle else Icons.Default.PlayArrow,
    enabled: Boolean = true
) {
    val containerColor = if (isShuffle) CWColors.SurfaceElevated else CWColors.AccentCyan
    val contentColor = if (isShuffle) CWColors.TextPrimary else CWColors.AccentCyan.contentColor()
    val borderStroke = if (isShuffle) BorderStroke(1.dp, CWColors.SurfaceOverlay) else null

    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(CWShapes.RadiusMedium),
        border = borderStroke,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = CWColors.SurfaceElevated,
            disabledContentColor = CWColors.TextTertiary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 6.dp)
        )
        Text(
            text = text,
            style = CWTypography.AppTypography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = contentColor
        )
    }
}

@Composable
fun CWIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = CWColors.TextSecondary,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = tint,
            disabledContentColor = CWColors.TextTertiary
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}
