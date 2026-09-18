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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.codewave.player.core.model.Album
import com.codewave.player.core.model.Artist
import com.codewave.player.ui.collection.CollectionDetailSheet
import com.codewave.player.ui.collection.CollectionTarget
import com.codewave.player.ui.collection.TrackActionMenuSheet
import com.codewave.player.core.model.EQPreset
import com.codewave.player.ui.command.CommandPaletteDialog
import com.codewave.player.ui.command.PaletteCommand
import com.codewave.player.ui.playlists.AddToPlaylistSheet
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
        factory = EqualizerViewModel.provideFactory(
            container.equalizerRepository,
            container.playbackRepository,
            container.autoEqManager,
            container.audioDeviceManager,
            container.settingsRepository
        )
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            container.settingsRepository,
            container.libraryRepository,
            container.otaUpdateManager,
            container.backupRestoreManager
        )
    )

    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var inspectedTrack by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }
    var activeCollectionTarget by remember { mutableStateOf<CollectionTarget?>(null) }
    var libraryInitialTab by remember { mutableIntStateOf(0) }
    var isThemeSheetOpen by remember { mutableStateOf(false) }
    var isCommandPaletteOpen by remember { mutableStateOf(false) }

    val currentThemeId by container.settingsRepository.themeId.collectAsState(initial = "obsidian")
    val playbackState by container.playbackRepository.playbackState.collectAsState()
    val otaStatus by container.otaUpdateManager.updateStatus.collectAsState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Back handling (PRD Section 96)
    BackHandler(enabled = isCommandPaletteOpen || isNowPlayingExpanded || activeCollectionTarget != null || currentScreen != Screen.Home) {
        if (isCommandPaletteOpen) {
            isCommandPaletteOpen = false
        } else if (isNowPlayingExpanded) {
            isNowPlayingExpanded = false
        } else if (activeCollectionTarget != null) {
            activeCollectionTarget = null
        } else if (currentScreen != Screen.Home) {
            currentScreen = Screen.Home
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Persistent Mini Player above Bottom Bar (PRD Section 20)
                val activeTrack = if (!isNowPlayingExpanded) playbackState.currentTrack else null
                val progress = if (playbackState.durationMs > 0)
                    playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()
                else 0f

                CWMiniPlayer(
                    track = activeTrack,
                    isPlaying = playbackState.isPlaying,
                    progress = progress,
                    onPlayPauseClick = { container.playbackRepository.togglePlayPause() },
                    onNextClick = { container.playbackRepository.skipNext() },
                    onPrevClick = { container.playbackRepository.skipPrevious() },
                    onClick = { isNowPlayingExpanded = true }
                )

                NavigationBar(
                    containerColor = CWColors.SurfacePrimary,
                    tonalElevation = 0.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val isSelected = !isNowPlayingExpanded && currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                activeCollectionTarget = null
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
                    onOpenThemes = { isThemeSheetOpen = true },
                    onOpenCommandPalette = { isCommandPaletteOpen = true },
                    onTrackOptions = { selectedTrackForOptions = it }
                )
                Screen.Library -> LibraryScreen(
                    viewModel = libraryViewModel,
                    initialTab = libraryInitialTab,
                    onTrackInspect = { inspectedTrack = it },
                    onTrackOptions = { selectedTrackForOptions = it },
                    onNavigateToAlbum = { album -> activeCollectionTarget = CollectionTarget.AlbumTarget(album) },
                    onNavigateToArtist = { artist -> activeCollectionTarget = CollectionTarget.ArtistTarget(artist) }
                )
                Screen.Search -> SearchScreen(
                    viewModel = searchViewModel,
                    onTrackInspect = { inspectedTrack = it },
                    onTrackOptions = { selectedTrackForOptions = it },
                    onNavigateToAlbum = { album -> activeCollectionTarget = CollectionTarget.AlbumTarget(album) },
                    onNavigateToArtist = { artist -> activeCollectionTarget = CollectionTarget.ArtistTarget(artist) }
                )
                Screen.Playlists -> PlaylistsScreen(
                    viewModel = playlistsViewModel,
                    onTrackInspect = { inspectedTrack = it },
                    onTrackOptions = { selectedTrackForOptions = it },
                    onNavigateToCollection = { target -> activeCollectionTarget = target }
                )
                Screen.Equalizer -> EqualizerScreen(
                    viewModel = equalizerViewModel
                )
                Screen.Settings -> SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { currentScreen = Screen.Home }
                )
            }

            // Global Collection Detail Overlay (Album / Artist target from anywhere in app)
            activeCollectionTarget?.let { target ->
                BackHandler(enabled = !isNowPlayingExpanded) { activeCollectionTarget = null }
                CollectionDetailSheet(
                    target = target,
                    libraryRepository = container.libraryRepository,
                    playbackRepository = container.playbackRepository,
                    onBack = { activeCollectionTarget = null },
                    onTrackInspect = { inspectedTrack = it },
                    onTrackOptions = { selectedTrackForOptions = it }
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
                    settingsRepository = container.settingsRepository,
                    onCollapse = { isNowPlayingExpanded = false },
                    onToggleFavorite = { track -> homeViewModel.toggleFavorite(track) },
                    onOpenTrackOptions = { selectedTrackForOptions = it },
                    onNavigateToEqualizer = {
                        currentScreen = Screen.Equalizer
                        isNowPlayingExpanded = false
                    },
                    onNavigateToSearch = {
                        currentScreen = Screen.Search
                        isNowPlayingExpanded = false
                    },
                    onNavigateToLibrary = {
                        currentScreen = Screen.Library
                        isNowPlayingExpanded = false
                    }
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

            // Track Action Options Bottom Sheet (Play Next, Add to Queue, Add to Playlist, Go to Album, Go to Artist, Specs)
            selectedTrackForOptions?.let { track ->
                TrackActionMenuSheet(
                    track = track,
                    onPlayNext = {
                        container.playbackRepository.playNext(track)
                        Toast.makeText(context, "Playing next: ${track.title}", Toast.LENGTH_SHORT).show()
                        selectedTrackForOptions = null
                    },
                    onAddToQueue = {
                        container.playbackRepository.addToQueue(track)
                        Toast.makeText(context, "Added to queue: ${track.title}", Toast.LENGTH_SHORT).show()
                        selectedTrackForOptions = null
                    },
                    onAddToPlaylist = {
                        val t = track
                        selectedTrackForOptions = null
                        selectedTrackForPlaylist = t
                    },
                    onGoToAlbum = {
                        val album = Album(
                            id = 0L,
                            title = track.album,
                            artist = track.artist,
                            trackCount = 1,
                            year = track.year,
                            artworkUri = track.albumArtUri,
                            isHiRes = track.isHiRes,
                            isLossless = track.isLossless
                        )
                        selectedTrackForOptions = null
                        isNowPlayingExpanded = false
                        activeCollectionTarget = CollectionTarget.AlbumTarget(album)
                    },
                    onGoToArtist = {
                        val artist = Artist(
                            id = 0L,
                            name = track.artist,
                            trackCount = 1,
                            albumCount = 1,
                            artworkUri = track.albumArtUri
                        )
                        selectedTrackForOptions = null
                        isNowPlayingExpanded = false
                        activeCollectionTarget = CollectionTarget.ArtistTarget(artist)
                    },
                    onInspectTrack = {
                        val t = track
                        selectedTrackForOptions = null
                        inspectedTrack = t
                    },
                    onDismiss = { selectedTrackForOptions = null }
                )
            }

            // Add To Playlist Sheet
            selectedTrackForPlaylist?.let { track ->
                AddToPlaylistSheet(
                    track = track,
                    libraryRepository = container.libraryRepository,
                    onDismiss = { selectedTrackForPlaylist = null }
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

            // VS Code Command Palette Modal Dialog (Feature 1.2)
            if (isCommandPaletteOpen) {
                val paletteCommands = remember(currentThemeId, playbackState) {
                    listOf(
                        PaletteCommand(
                            id = "theme_obsidian",
                            category = "THEME",
                            title = "Switch Theme: VS Code Dark+ (Obsidian)",
                            commandText = "> theme obsidian",
                            description = "Default dark slate developer theme",
                            action = { settingsViewModel.setThemeId("obsidian") }
                        ),
                        PaletteCommand(
                            id = "theme_synthwave",
                            category = "THEME",
                            title = "Switch Theme: Synthwave '84",
                            commandText = "> theme synthwave",
                            description = "Neon violet and hot pink 80s aesthetic",
                            action = { settingsViewModel.setThemeId("synthwave") }
                        ),
                        PaletteCommand(
                            id = "theme_tokyo_night",
                            category = "THEME",
                            title = "Switch Theme: Tokyo Night Storm",
                            commandText = "> theme tokyo_night",
                            description = "Deep indigo and vibrant neon cyan",
                            action = { settingsViewModel.setThemeId("tokyo_night") }
                        ),
                        PaletteCommand(
                            id = "theme_dracula",
                            category = "THEME",
                            title = "Switch Theme: Dracula Pro",
                            commandText = "> theme dracula_pro",
                            description = "Gothic purple and vampire pastel accents",
                            action = { settingsViewModel.setThemeId("dracula_pro") }
                        ),
                        PaletteCommand(
                            id = "theme_monokai",
                            category = "THEME",
                            title = "Switch Theme: Monokai Pro",
                            commandText = "> theme monokai_pro",
                            description = "Warm espresso and vivid spectrum accents",
                            action = { settingsViewModel.setThemeId("monokai_pro") }
                        ),
                        PaletteCommand(
                            id = "eq_flat",
                            category = "EQ",
                            title = "Equalizer Preset: Flat (Studio Reference)",
                            commandText = "> eq flat",
                            description = "0.0 dB across all 10 frequency bands",
                            action = {
                                EQPreset.PRESETS_10_BAND.find { it.name.equals("Flat", ignoreCase = true) }?.let {
                                    equalizerViewModel.applyPreset(it)
                                }
                            }
                        ),
                        PaletteCommand(
                            id = "eq_bass",
                            category = "EQ",
                            title = "Equalizer Preset: Bass Boost",
                            commandText = "> eq bass",
                            description = "+6.0 dB low-end shelf boost",
                            action = {
                                EQPreset.PRESETS_10_BAND.find { it.name.equals("Bass Boost", ignoreCase = true) }?.let {
                                    equalizerViewModel.applyPreset(it)
                                }
                            }
                        ),
                        PaletteCommand(
                            id = "eq_rock",
                            category = "EQ",
                            title = "Equalizer Preset: Rock & Metal",
                            commandText = "> eq rock",
                            description = "V-shaped curve punchy bass and treble",
                            action = {
                                EQPreset.PRESETS_10_BAND.find { it.name.equals("Rock", ignoreCase = true) }?.let {
                                    equalizerViewModel.applyPreset(it)
                                }
                            }
                        ),
                        PaletteCommand(
                            id = "eq_electronic",
                            category = "EQ",
                            title = "Equalizer Preset: Electronic / EDM",
                            commandText = "> eq electronic",
                            description = "Elevated sub-bass and crisp air",
                            action = {
                                EQPreset.PRESETS_10_BAND.find { it.name.equals("Electronic", ignoreCase = true) }?.let {
                                    equalizerViewModel.applyPreset(it)
                                }
                            }
                        ),
                        PaletteCommand(
                            id = "eq_vocal",
                            category = "EQ",
                            title = "Equalizer Preset: Vocal Focus",
                            commandText = "> eq vocal",
                            description = "+3.5 dB mid-range presence for vocals",
                            action = {
                                EQPreset.PRESETS_10_BAND.find { it.name.equals("Vocal", ignoreCase = true) }?.let {
                                    equalizerViewModel.applyPreset(it)
                                }
                            }
                        ),
                        PaletteCommand(
                            id = "eq_clear",
                            category = "EQ",
                            title = "Equalizer Preset: Clear Voice",
                            commandText = "> eq clear",
                            description = "High clarity treble and vocal intelligibility",
                            action = {
                                EQPreset.PRESETS_10_BAND.find { it.name.equals("Clear Voice", ignoreCase = true) }?.let {
                                    equalizerViewModel.applyPreset(it)
                                }
                            }
                        ),
                        PaletteCommand(
                            id = "sleep_15",
                            category = "SLEEP",
                            title = "Sleep Timer: 15 Minutes",
                            commandText = "> sleep 15",
                            description = "Start countdown for 15 minutes with fade-out",
                            action = { container.playbackRepository.startSleepTimer(15) }
                        ),
                        PaletteCommand(
                            id = "sleep_30",
                            category = "SLEEP",
                            title = "Sleep Timer: 30 Minutes",
                            commandText = "> sleep 30",
                            description = "Start countdown for 30 minutes with fade-out",
                            action = { container.playbackRepository.startSleepTimer(30) }
                        ),
                        PaletteCommand(
                            id = "sleep_60",
                            category = "SLEEP",
                            title = "Sleep Timer: 60 Minutes",
                            commandText = "> sleep 60",
                            description = "Start countdown for 1 hour with fade-out",
                            action = { container.playbackRepository.startSleepTimer(60) }
                        ),
                        PaletteCommand(
                            id = "sleep_end",
                            category = "SLEEP",
                            title = "Sleep Timer: End of Current Song",
                            commandText = "> sleep end",
                            description = "Pause audio cleanly when current track completes",
                            action = { container.playbackRepository.startSleepTimer(0, finishCurrentTrack = true) }
                        ),
                        PaletteCommand(
                            id = "sleep_off",
                            category = "SLEEP",
                            title = "Sleep Timer: Turn Off",
                            commandText = "> sleep off",
                            description = "Cancel active sleep timer",
                            action = { container.playbackRepository.stopSleepTimer() }
                        ),
                        PaletteCommand(
                            id = "nav_now_playing",
                            category = "NAV",
                            title = "Open Now Playing Screen",
                            commandText = "> now playing",
                            description = "Expand turntable vinyl and studio lyrics console",
                            action = { if (playbackState.currentTrack != null) isNowPlayingExpanded = true }
                        ),
                        PaletteCommand(
                            id = "nav_equalizer",
                            category = "NAV",
                            title = "Go to 10-Band Parametric Equalizer",
                            commandText = "> equalizer",
                            description = "Open ViPERFX acoustic suite and Bézier curve visualizer",
                            action = {
                                currentScreen = Screen.Equalizer
                                isNowPlayingExpanded = false
                            }
                        ),
                        PaletteCommand(
                            id = "nav_library",
                            category = "NAV",
                            title = "Go to Audio Library",
                            commandText = "> library",
                            description = "Browse Songs, Albums, Artists, Folders",
                            action = {
                                currentScreen = Screen.Library
                                isNowPlayingExpanded = false
                            }
                        ),
                        PaletteCommand(
                            id = "nav_search",
                            category = "NAV",
                            title = "Go to Terminal Search Prompt",
                            commandText = "> search",
                            description = "Search all tracks, artists, and albums",
                            action = {
                                currentScreen = Screen.Search
                                isNowPlayingExpanded = false
                            }
                        ),
                        PaletteCommand(
                            id = "nav_playlists",
                            category = "NAV",
                            title = "Go to Playlists & Favorites",
                            commandText = "> playlists",
                            description = "Manage playlists and dynamic collections",
                            action = {
                                currentScreen = Screen.Playlists
                                isNowPlayingExpanded = false
                            }
                        ),
                        PaletteCommand(
                            id = "nav_settings",
                            category = "NAV",
                            title = "Go to App Settings",
                            commandText = "> settings",
                            description = "Configure gapless audio, themes, and library exclusions",
                            action = {
                                currentScreen = Screen.Settings
                                isNowPlayingExpanded = false
                            }
                        ),
                        PaletteCommand(
                            id = "pb_toggle",
                            category = "AUDIO",
                            title = if (playbackState.isPlaying) "Pause Playback" else "Resume Playback",
                            commandText = "> play/pause",
                            description = "Toggle active playback state",
                            action = { container.playbackRepository.togglePlayPause() }
                        ),
                        PaletteCommand(
                            id = "pb_next",
                            category = "AUDIO",
                            title = "Skip to Next Track",
                            commandText = "> next",
                            description = "Play next track in active queue",
                            action = { container.playbackRepository.skipNext() }
                        ),
                        PaletteCommand(
                            id = "pb_prev",
                            category = "AUDIO",
                            title = "Skip to Previous Track",
                            commandText = "> previous",
                            description = "Play previous track or restart current",
                            action = { container.playbackRepository.skipPrevious() }
                        ),
                        PaletteCommand(
                            id = "lib_scan",
                            category = "LIBRARY",
                            title = "Rescan Device Audio Storage",
                            commandText = "> scan",
                            description = "Index new audio files and update metadata",
                            action = {
                                coroutineScope.launch {
                                    val count = container.libraryRepository.scanLibrary()
                                    Toast.makeText(context, "Scan complete: $count audio files", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    )
                }
                CommandPaletteDialog(
                    commands = paletteCommands,
                    onDismiss = { isCommandPaletteOpen = false }
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
