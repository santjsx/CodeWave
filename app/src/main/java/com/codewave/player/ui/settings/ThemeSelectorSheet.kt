package com.codewave.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWThemeDefinition
import com.codewave.player.core.designsystem.theme.CWTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorSheet(
    activeThemeId: String,
    onSelectTheme: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CWColors.SurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VS CODE THEMES & PALETTES",
                        style = CWTypography.TechInspectorHeader
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Transforms colors, typography, telemetry & playback scrubber",
                        style = CWTypography.AppTypography.bodyMedium,
                        color = CWColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                items(CWColors.AvailableThemes, key = { it.id }) { theme ->
                    val isSelected = theme.id.equals(activeThemeId, ignoreCase = true)
                    ThemeCard(
                        theme = theme,
                        isSelected = isSelected,
                        onClick = { onSelectTheme(theme.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: CWThemeDefinition,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CWShapes.RadiusMedium))
            .background(if (isSelected) CWColors.SurfaceOverlay else CWColors.SurfacePrimary)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) theme.accentColor else CWColors.BorderSubtle,
                shape = RoundedCornerShape(CWShapes.RadiusMedium)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = theme.name,
                        style = CWTypography.AppTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CWColors.TextPrimary
                    )

                    CWTechnicalBadge(
                        text = theme.playBarType.name.replace("_", " "),
                        textColor = theme.accentColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = theme.description,
                    style = CWTypography.AppTypography.bodyMedium,
                    color = CWColors.TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Color Swatches
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    theme.previewColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        )
                    }
                }
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = theme.accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
