package com.codewave.player.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWTypography

enum class ActivityBarItem {
    EXPLORER,
    SEARCH,
    SOURCE_CONTROL,
    PLAY,
    EXTENSIONS
}

@Composable
fun CWActivityBar(
    activeItem: ActivityBarItem = ActivityBarItem.PLAY,
    onExplorerClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSourceControlClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    onExtensionsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .background(Color(0xFF090C10))
    ) {
        // Right border divider
        Canvas(modifier = Modifier.fillMaxHeight().width(1.dp).align(Alignment.CenterEnd)) {
            drawLine(
                color = CWColors.BorderSubtle,
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 1f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: VS Code Ribbon Logo & Activity Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // VS Code Stylized Logo
                VsCodeRibbonLogo(
                    modifier = Modifier
                        .size(28.dp)
                        .padding(bottom = 6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Activity Icons
                ActivityBarIconButton(
                    icon = Icons.Outlined.Description,
                    contentDescription = "Explorer",
                    isActive = activeItem == ActivityBarItem.EXPLORER,
                    onClick = onExplorerClick
                )

                ActivityBarIconButton(
                    icon = Icons.Outlined.Search,
                    contentDescription = "Search",
                    isActive = activeItem == ActivityBarItem.SEARCH,
                    onClick = onSearchClick
                )

                ActivityBarIconButton(
                    icon = Icons.Outlined.AccountTree,
                    contentDescription = "Source Control",
                    isActive = activeItem == ActivityBarItem.SOURCE_CONTROL,
                    onClick = onSourceControlClick
                )

                ActivityBarIconButton(
                    icon = Icons.Outlined.PlayCircleOutline,
                    contentDescription = "Run and Debug / Now Playing",
                    isActive = activeItem == ActivityBarItem.PLAY,
                    onClick = onPlayClick
                )

                ActivityBarIconButton(
                    icon = Icons.Outlined.GridView,
                    contentDescription = "Extensions / DSP & EQ",
                    isActive = activeItem == ActivityBarItem.EXTENSIONS,
                    onClick = onExtensionsClick
                )
            }

            // Bottom Section: Developer Tagline "EAT CODE MUSIC REPEAT —"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val words = listOf("EAT", "CODE", "MUSIC", "REPEAT", "—")
                words.forEach { word ->
                    Text(
                        text = word,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 8.5.sp,
                        lineHeight = 11.sp,
                        color = Color(0xFF485160),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Active indicator on left edge (VS Code indicator pill)
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(2.5.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                    .background(CWColors.AccentCyan)
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) CWColors.AccentCyan else Color(0xFF6E7681),
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
fun VsCodeRibbonLogo(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF007ACC)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Geometric stylized VS Code fold ribbon
        val path1 = Path().apply {
            moveTo(w * 0.72f, h * 0.08f)
            lineTo(w * 0.94f, h * 0.22f)
            lineTo(w * 0.94f, h * 0.78f)
            lineTo(w * 0.72f, h * 0.92f)
            lineTo(w * 0.35f, h * 0.62f)
            lineTo(w * 0.12f, h * 0.76f)
            lineTo(w * 0.05f, h * 0.70f)
            lineTo(w * 0.25f, h * 0.50f)
            lineTo(w * 0.05f, h * 0.30f)
            lineTo(w * 0.12f, h * 0.24f)
            lineTo(w * 0.35f, h * 0.38f)
            close()
        }

        drawPath(
            path = path1,
            color = color
        )

        // Accent fold shadow line
        val foldPath = Path().apply {
            moveTo(w * 0.72f, h * 0.08f)
            lineTo(w * 0.35f, h * 0.38f)
            lineTo(w * 0.35f, h * 0.62f)
            lineTo(w * 0.72f, h * 0.92f)
        }

        drawPath(
            path = foldPath,
            color = Color(0xFF1F8AD2),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
