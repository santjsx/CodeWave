package com.codewave.player.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.codewave.player.core.model.AlbumSortOption
import com.codewave.player.core.model.ArtistSortOption
import com.codewave.player.core.model.SongSortOption
import com.codewave.player.core.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "codewave_settings")

interface SettingsRepository {
    val songSortOption: Flow<SongSortOption>
    val albumSortOption: Flow<AlbumSortOption>
    val artistSortOption: Flow<ArtistSortOption>
    val songViewMode: Flow<ViewMode>
    val albumViewMode: Flow<ViewMode>
    val gaplessEnabled: Flow<Boolean>
    val crossfadeSeconds: Flow<Int>
    val playbackSpeed: Flow<Float>
    val themeId: Flow<String>

    suspend fun setSongSortOption(sort: SongSortOption)
    suspend fun setAlbumSortOption(sort: AlbumSortOption)
    suspend fun setArtistSortOption(sort: ArtistSortOption)
    suspend fun setSongViewMode(mode: ViewMode)
    suspend fun setAlbumViewMode(mode: ViewMode)
    suspend fun setGaplessEnabled(enabled: Boolean)
    suspend fun setCrossfadeSeconds(seconds: Int)
    suspend fun setPlaybackSpeed(speed: Float)
    suspend fun setThemeId(themeId: String)
}

class DefaultSettingsRepository(private val context: Context) : SettingsRepository {

    private object Keys {
        val SONG_SORT = stringPreferencesKey("song_sort")
        val ALBUM_SORT = stringPreferencesKey("album_sort")
        val ARTIST_SORT = stringPreferencesKey("artist_sort")
        val SONG_VIEW_MODE = stringPreferencesKey("song_view_mode")
        val ALBUM_VIEW_MODE = stringPreferencesKey("album_view_mode")
        val GAPLESS = booleanPreferencesKey("gapless_playback")
        val CROSSFADE = intPreferencesKey("crossfade_seconds")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val THEME_ID = stringPreferencesKey("theme_id")
    }

    override val songSortOption: Flow<SongSortOption> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.SONG_SORT]?.let {
            try { SongSortOption.valueOf(it) } catch (_: Exception) { null }
        } ?: SongSortOption.DATE_ADDED_DESC
    }

    override val albumSortOption: Flow<AlbumSortOption> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.ALBUM_SORT]?.let {
            try { AlbumSortOption.valueOf(it) } catch (_: Exception) { null }
        } ?: AlbumSortOption.DATE_ADDED_DESC
    }

    override val artistSortOption: Flow<ArtistSortOption> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.ARTIST_SORT]?.let {
            try { ArtistSortOption.valueOf(it) } catch (_: Exception) { null }
        } ?: ArtistSortOption.NAME_ASC
    }

    override val songViewMode: Flow<ViewMode> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.SONG_VIEW_MODE]?.let {
            try { ViewMode.valueOf(it) } catch (_: Exception) { null }
        } ?: ViewMode.LIST
    }

    override val albumViewMode: Flow<ViewMode> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.ALBUM_VIEW_MODE]?.let {
            try { ViewMode.valueOf(it) } catch (_: Exception) { null }
        } ?: ViewMode.GRID
    }

    override val gaplessEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.GAPLESS] ?: true
    }

    override val crossfadeSeconds: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.CROSSFADE] ?: 0
    }

    override val playbackSpeed: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.PLAYBACK_SPEED] ?: 1.0f
    }

    override val themeId: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.THEME_ID] ?: "obsidian"
    }

    override suspend fun setSongSortOption(sort: SongSortOption) {
        context.settingsDataStore.edit { it[Keys.SONG_SORT] = sort.name }
    }

    override suspend fun setAlbumSortOption(sort: AlbumSortOption) {
        context.settingsDataStore.edit { it[Keys.ALBUM_SORT] = sort.name }
    }

    override suspend fun setArtistSortOption(sort: ArtistSortOption) {
        context.settingsDataStore.edit { it[Keys.ARTIST_SORT] = sort.name }
    }

    override suspend fun setSongViewMode(mode: ViewMode) {
        context.settingsDataStore.edit { it[Keys.SONG_VIEW_MODE] = mode.name }
    }

    override suspend fun setAlbumViewMode(mode: ViewMode) {
        context.settingsDataStore.edit { it[Keys.ALBUM_VIEW_MODE] = mode.name }
    }

    override suspend fun setGaplessEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.GAPLESS] = enabled }
    }

    override suspend fun setCrossfadeSeconds(seconds: Int) {
        context.settingsDataStore.edit { it[Keys.CROSSFADE] = seconds }
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        context.settingsDataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
    }

    override suspend fun setThemeId(themeId: String) {
        context.settingsDataStore.edit { it[Keys.THEME_ID] = themeId }
    }
}
