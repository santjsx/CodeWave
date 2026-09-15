package com.codewave.player.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography

enum class CWButtonVariant {
    SOLID,
    OUTLINED,
    GHOST
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
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = RoundedCornerShape(CWShapes.RadiusMedium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CWColors.AccentCyan,
                    contentColor = CWColors.Background,
                    disabledContainerColor = CWColors.SurfaceElevated,
                    disabledContentColor = CWColors.TextTertiary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = text,
                    style = CWTypography.AppTypography.titleSmall
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
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = text,
                    style = CWTypography.AppTypography.titleSmall
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
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Text(
                    text = text,
                    style = CWTypography.AppTypography.bodyMedium
                )
            }
        }
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
