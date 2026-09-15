package com.codewave.player.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Library : Screen("library", "Library", Icons.Default.LibraryMusic)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object Playlists : Screen("playlists", "Playlists", Icons.Default.PlaylistPlay)
    data object Equalizer : Screen("equalizer", "DSP / EQ", Icons.Default.Equalizer)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Home, Library, Search, Playlists, Equalizer)
    }
}
