package com.codewave.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codewave.player.CodeWaveApplication
import com.codewave.player.core.designsystem.component.CWMiniPlayer
import com.codewave.player.core.designsystem.component.TrackInspectorSheet
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.Track
import com.codewave.player.core.ota.UpdateStatus
import com.codewave.player.ui.equalizer.EqualizerScreen
import com.codewave.player.ui.equalizer.EqualizerViewModel
import com.codewave.player.ui.home.HomeScreen
import com.codewave.player.ui.home.HomeViewModel
import com.codewave.player.ui.library.LibraryScreen
import com.codewave.player.ui.library.LibraryViewModel
import com.codewave.player.ui.navigation.Screen
import com.codewave.player.ui.ota.OtaUpdateDialog
import com.codewave.player.ui.player.NowPlayingScreen
import com.codewave.player.ui.playlists.PlaylistsScreen
import com.codewave.player.ui.playlists.PlaylistsViewModel
import com.codewave.player.ui.search.SearchScreen
import com.codewave.player.ui.search.SearchViewModel
import com.codewave.player.ui.settings.SettingsScreen
import com.codewave.player.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeWaveApp(
    application: CodeWaveApplication,
    modifier: Modifier = Modifier
) {
    val container = application.container

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(
            container.libraryRepository,
            container.playbackRepository,
            container.settingsRepository
        )
    )
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.provideFactory(
            container.libraryRepository,
            container.playbackRepository,
            container.settingsRepository
        )
    )
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.provideFactory(container.libraryRepository, container.playbackRepository)
    )
    val playlistsViewModel: PlaylistsViewModel = viewModel(
        factory = PlaylistsViewModel.provideFactory(container.libraryRepository, container.playbackRepository)
    )
    val equalizerViewModel: EqualizerViewModel = viewModel(
        factory = EqualizerViewModel.provideFactory(container.equalizerRepository, container.playbackRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            container.settingsRepository,
            container.libraryRepository,
            container.otaUpdateManager
        )
    )

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var inspectedTrack by remember { mutableStateOf<Track?>(null) }
    var libraryInitialTab by remember { mutableIntStateOf(0) }
    var isThemeSheetOpen by remember { mutableStateOf(false) }

    val currentThemeId by container.settingsRepository.themeId.collectAsState(initial = "obsidian")
    val playbackState by container.playbackRepository.playbackState.collectAsState()
    val otaStatus by container.otaUpdateManager.updateStatus.collectAsState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Back handling (PRD Section 96)
    BackHandler(enabled = isNowPlayingExpanded || currentScreen != Screen.Home) {
        if (isNowPlayingExpanded) {
            isNowPlayingExpanded = false
        } else if (currentScreen != Screen.Home) {
            currentScreen = Screen.Home
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Persistent Mini Player above Bottom Bar (PRD Section 20)
                if (!isNowPlayingExpanded && playbackState.currentTrack != null) {
                    val progress = if (playbackState.durationMs > 0)
                        playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()
                    else 0f

                    CWMiniPlayer(
                        track = playbackState.currentTrack,
                        isPlaying = playbackState.isPlaying,
                        progress = progress,
                        onPlayPauseClick = { container.playbackRepository.togglePlayPause() },
                        onNextClick = { container.playbackRepository.skipNext() },
                        onPrevClick = { container.playbackRepository.skipPrevious() },
                        onClick = { isNowPlayingExpanded = true }
                    )
                }

                NavigationBar(
                    containerColor = CWColors.SurfacePrimary,
                    tonalElevation = 0.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val isSelected = !isNowPlayingExpanded && currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentScreen == Screen.Library && screen == Screen.Library) {
                                    libraryInitialTab = 0
                                }
                                currentScreen = screen
                                isNowPlayingExpanded = false
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = CWTypography.TechBadge
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CWColors.AccentCyan,
                                selectedTextColor = CWColors.AccentCyan,
                                indicatorColor = CWColors.SurfaceElevated,
                                unselectedIconColor = CWColors.TextSecondary,
                                unselectedTextColor = CWColors.TextSecondary
                            )
                        )
                    }
                }
            }
        },
        containerColor = CWColors.Background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToLibrary = { tab ->
                        libraryInitialTab = tab
                        currentScreen = Screen.Library
                    },
                    onNavigateToSettings = { currentScreen = Screen.Settings },
                    onTrackInspect = { inspectedTrack = it },
                    onOpenThemes = { isThemeSheetOpen = true }
                )
                Screen.Library -> LibraryScreen(
                    viewModel = libraryViewModel,
                    initialTab = libraryInitialTab,
                    onTrackInspect = { inspectedTrack = it }
                )
                Screen.Search -> SearchScreen(
                    viewModel = searchViewModel,
                    onTrackInspect = { inspectedTrack = it }
                )
                Screen.Playlists -> PlaylistsScreen(
                    viewModel = playlistsViewModel,
                    onTrackInspect = { inspectedTrack = it }
                )
                Screen.Equalizer -> EqualizerScreen(
                    viewModel = equalizerViewModel
                )
                Screen.Settings -> SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { currentScreen = Screen.Home }
                )
            }

            // Fullscreen Now Playing Overlay
            AnimatedVisibility(
                visible = isNowPlayingExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NowPlayingScreen(
                    playbackRepository = container.playbackRepository,
                    onCollapse = { isNowPlayingExpanded = false },
                    onToggleFavorite = { track -> homeViewModel.toggleFavorite(track) }
                )
            }

            // Track Inspector Modal
            inspectedTrack?.let { track ->
                TrackInspectorSheet(
                    track = track,
                    outputInfo = playbackState.outputInfo,
                    dspStatus = playbackState.dspStatus,
                    onDismiss = { inspectedTrack = null }
                )
            }

            // Theme Selector Bottom Sheet
            if (isThemeSheetOpen) {
                com.codewave.player.ui.settings.ThemeSelectorSheet(
                    activeThemeId = currentThemeId,
                    onSelectTheme = { selectedTheme ->
                        settingsViewModel.setThemeId(selectedTheme)
                        isThemeSheetOpen = false
                    },
                    onDismiss = { isThemeSheetOpen = false }
                )
            }

            // In-App OTA Update Dialog (PRD: Popup on app open with changelogs, download progress, install options)
            when (val status = otaStatus) {
                is UpdateStatus.UpdateAvailable,
                is UpdateStatus.Downloading,
                is UpdateStatus.ReadyToInstall -> {
                    OtaUpdateDialog(
                        status = status,
                        onDownload = { info ->
                            coroutineScope.launch {
                                container.otaUpdateManager.downloadAndInstall(info)
                            }
                        },
                        onInstallNow = { apkFile ->
                            container.otaUpdateManager.installApk(apkFile)
                        },
                        onDismiss = {
                            container.otaUpdateManager.dismissUpdate()
                        }
                    )
                }
                else -> Unit
            }
        }
    }
}
