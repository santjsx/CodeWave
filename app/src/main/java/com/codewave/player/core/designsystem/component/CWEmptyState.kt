package com.codewave.player.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWTypography

@Composable
fun CWEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.LibraryMusic,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CWColors.TextTertiary,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title.uppercase(),
            style = CWTypography.TechBadge,
            color = CWColors.AccentCyan,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message,
            style = CWTypography.AppTypography.bodyLarge,
            color = CWColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            CWButton(
                text = actionButtonText,
                onClick = onActionClick,
                variant = CWButtonVariant.SOLID
            )
        }
    }
}
