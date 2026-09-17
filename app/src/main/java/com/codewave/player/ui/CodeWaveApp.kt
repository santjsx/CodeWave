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
        factory = EqualizerViewModel.provideFactory(container.equalizerRepository, container.playbackRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            container.settingsRepository,
            container.libraryRepository,
            container.otaUpdateManager
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

    val currentThemeId by container.settingsRepository.themeId.collectAsState(initial = "obsidian")
    val playbackState by container.playbackRepository.playbackState.collectAsState()
    val otaStatus by container.otaUpdateManager.updateStatus.collectAsState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Back handling (PRD Section 96)
    BackHandler(enabled = activeCollectionTarget != null || isNowPlayingExpanded || currentScreen != Screen.Home) {
        if (activeCollectionTarget != null) {
            activeCollectionTarget = null
        } else if (isNowPlayingExpanded) {
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

            // Fullscreen Now Playing Overlay
            AnimatedVisibility(
                visible = isNowPlayingExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NowPlayingScreen(
                    playbackRepository = container.playbackRepository,
                    onCollapse = { isNowPlayingExpanded = false },
                    onToggleFavorite = { track -> homeViewModel.toggleFavorite(track) },
                    onOpenTrackOptions = { selectedTrackForOptions = it }
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

            // Global Collection Detail Overlay (Album / Artist target from anywhere in app)
            activeCollectionTarget?.let { target ->
                BackHandler { activeCollectionTarget = null }
                CollectionDetailSheet(
                    target = target,
                    libraryRepository = container.libraryRepository,
                    playbackRepository = container.playbackRepository,
                    onBack = { activeCollectionTarget = null },
                    onTrackInspect = { inspectedTrack = it },
                    onTrackOptions = { selectedTrackForOptions = it }
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
