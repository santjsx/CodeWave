package com.codewave.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codewave.player.core.designsystem.component.CWButton
import com.codewave.player.core.designsystem.component.CWButtonVariant
import com.codewave.player.core.designsystem.component.CWTechnicalBadge
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gapless by viewModel.gaplessEnabled.collectAsState()
    val crossfade by viewModel.crossfadeSeconds.collectAsState()
    val themeId by viewModel.themeId.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    var isThemeSheetOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = CWColors.TextPrimary
                )
            }
            Text(
                text = "SETTINGS",
                style = CWTypography.AppTypography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CWColors.TextPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Section: Appearance & Themes
        SettingsHeader(title = "APPEARANCE & THEMES")

        val currentTheme = CWColors.AvailableThemes.find { it.id.equals(themeId, ignoreCase = true) }
            ?: CWColors.AvailableThemes.first()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(CWShapes.RadiusMedium),
            colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = currentTheme.name,
                                style = CWTypography.AppTypography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CWColors.TextPrimary
                            )
                            CWTechnicalBadge(
                                text = currentTheme.playBarType.name.replace("_", " "),
                                textColor = currentTheme.accentColor
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentTheme.description,
                            style = CWTypography.AppTypography.bodyMedium,
                            color = CWColors.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            currentTheme.previewColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f), CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(CWShapes.RadiusSmall))
                            .background(CWColors.SurfaceElevated)
                            .border(1.dp, CWColors.AccentCyan, RoundedCornerShape(CWShapes.RadiusSmall))
                            .clickable { isThemeSheetOpen = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "CHANGE",
                            style = CWTypography.TechBadge,
                            color = CWColors.AccentCyan
                        )
                    }
                }
            }
        }

        // Section: Playback & Audio
        SettingsHeader(title = "PLAYBACK & AUDIO")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(CWShapes.RadiusMedium),
            colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Gapless Playback Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gapless Playback",
                            style = CWTypography.AppTypography.titleMedium,
                            color = CWColors.TextPrimary
                        )
                        Text(
                            text = "Preserves sample-accurate transitions for live albums and classical music (PRD Section 23)",
                            style = CWTypography.AppTypography.bodyMedium,
                            color = CWColors.TextSecondary
                        )
                    }

                    Switch(
                        checked = gapless,
                        onCheckedChange = { viewModel.toggleGapless(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CWColors.Background,
                            checkedTrackColor = CWColors.AccentCyan,
                            uncheckedThumbColor = CWColors.TextTertiary,
                            uncheckedTrackColor = CWColors.SurfaceOverlay
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Crossfade
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crossfade Duration",
                        style = CWTypography.AppTypography.titleMedium,
                        color = CWColors.TextPrimary
                    )
                    Text(
                        text = if (crossfade == 0) "OFF" else "${crossfade}s",
                        style = CWTypography.TechTelemetry,
                        color = if (crossfade > 0) CWColors.AccentCyan else CWColors.TextSecondary
                    )
                }

                Slider(
                    value = crossfade.toFloat(),
                    onValueChange = { viewModel.setCrossfadeSeconds(it.toInt()) },
                    valueRange = 0f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = CWColors.AccentCyan,
                        activeTrackColor = CWColors.AccentCyan,
                        inactiveTrackColor = CWColors.SurfaceOverlay
                    )
                )
            }
        }

        // Section: Library Maintenance
        SettingsHeader(title = "LIBRARY MAINTENANCE")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(CWShapes.RadiusMedium),
            colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rescan Media Storage",
                            style = CWTypography.AppTypography.titleMedium,
                            color = CWColors.TextPrimary
                        )
                        Text(
                            text = "Forces reconciliation against Android MediaStore to discover newly added audio files",
                            style = CWTypography.AppTypography.bodyMedium,
                            color = CWColors.TextSecondary
                        )
                    }

                    CWButton(
                        text = "Scan",
                        onClick = { viewModel.rescanLibrary() },
                        variant = CWButtonVariant.OUTLINED,
                        leadingIcon = Icons.Default.Refresh
                    )
                }
            }
        }

        // Section: Updates & Sideloading (PRD Section 80, 81)
        SettingsHeader(title = "UPDATES & DISTRIBUTION")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(CWShapes.RadiusMedium),
            colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GitHub Release Updates",
                            style = CWTypography.AppTypography.titleMedium,
                            color = CWColors.TextPrimary
                        )
                        Text(
                            text = "Non-intrusive sideload APK discovery with SHA-256 verification (Zero analytics / telemetry)",
                            style = CWTypography.AppTypography.bodyMedium,
                            color = CWColors.TextSecondary
                        )
                    }

                    CWButton(
                        text = if (updateState.isChecking) "Checking..." else "Check",
                        onClick = { viewModel.checkForUpdates() },
                        enabled = !updateState.isChecking,
                        variant = CWButtonVariant.SOLID,
                        leadingIcon = Icons.Default.SystemUpdate
                    )
                }

                if (updateState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = updateState.errorMessage ?: "",
                        style = CWTypography.AppTypography.bodyMedium,
                        color = CWColors.Warning
                    )
                } else if (updateState.latestVersion != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (updateState.isUpdateAvailable)
                                "Update available: ${updateState.latestVersion}"
                            else "CODEWAVE is up to date (${updateState.latestVersion})",
                            style = CWTypography.TechTelemetry,
                            color = if (updateState.isUpdateAvailable) CWColors.AccentCyan else CWColors.Success
                        )

                        if (updateState.isUpdateAvailable && !updateState.downloadUrl.isNullOrEmpty()) {
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            CWButton(
                                text = "Get Update",
                                onClick = {
                                    try {
                                        uriHandler.openUri(updateState.downloadUrl!!)
                                    } catch (_: Exception) {}
                                },
                                variant = CWButtonVariant.OUTLINED
                            )
                        }
                    }
                }
            }
        }

        // Section: About & Engineering Integrity
        SettingsHeader(title = "ABOUT CODEWAVE")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(CWShapes.RadiusMedium),
            colors = CardDefaults.cardColors(containerColor = CWColors.SurfacePrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, CWColors.BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Version", style = CWTypography.AppTypography.bodyMedium, color = CWColors.TextSecondary)
                    CWTechnicalBadge(text = "1.3.0 (Release)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Audio Engine", style = CWTypography.AppTypography.bodyMedium, color = CWColors.TextSecondary)
                    Text(text = "Jetpack Media3 ExoPlayer", style = CWTypography.TechTelemetry)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Architecture", style = CWTypography.AppTypography.bodyMedium, color = CWColors.TextSecondary)
                    Text(text = "Offline-First Local Workstation", style = CWTypography.TechTelemetry)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Telemetry / Ads", style = CWTypography.AppTypography.bodyMedium, color = CWColors.TextSecondary)
                    CWTechnicalBadge(text = "NONE (ZERO TRACKING)", textColor = CWColors.Success)
                }
            }
        }

        if (isThemeSheetOpen) {
            ThemeSelectorSheet(
                activeThemeId = themeId,
                onSelectTheme = { selectedId ->
                    viewModel.setThemeId(selectedId)
                    isThemeSheetOpen = false
                },
                onDismiss = { isThemeSheetOpen = false }
            )
        }
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = CWTypography.TechBadge,
        color = CWColors.AccentCyan,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}
