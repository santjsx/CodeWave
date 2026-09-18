package com.codewave.player.core.backup

import android.content.Context
import android.net.Uri
import com.codewave.player.core.data.SettingsRepository
import com.codewave.player.core.database.CodeWaveDatabase
import com.codewave.player.core.database.entity.EQPresetEntity
import com.codewave.player.core.database.entity.PlaylistEntity
import com.codewave.player.core.database.entity.PlaylistTrackCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

data class BackupSummary(
    val playlistCount: Int,
    val favoriteCount: Int,
    val customEqCount: Int,
    val timestamp: Long
)

data class RestoreSummary(
    val playlistsRestored: Int,
    val favoritesRestored: Int,
    val presetsRestored: Int,
    val settingsRestored: Boolean
)

class BackupRestoreManager(
    private val database: CodeWaveDatabase,
    private val settingsRepository: SettingsRepository
) {

    suspend fun exportBackup(context: Context, destinationUri: Uri): Result<BackupSummary> =
        withContext(Dispatchers.IO) {
            try {
                val root = JSONObject()
                root.put("app", "CodeWave")
                root.put("version", 1)
                val timestamp = System.currentTimeMillis()
                root.put("timestamp", timestamp)

                // 1. Playlists
                val playlistDao = database.playlistDao()
                val playlists = playlistDao.getAllPlaylists()
                val playlistsArray = JSONArray()
                for (pl in playlists) {
                    val plObj = JSONObject()
                    plObj.put("name", pl.name)
                    plObj.put("createdAt", pl.createdAt)
                    val tracks = playlistDao.getTracksForPlaylist(pl.id)
                    val tracksArray = JSONArray()
                    tracks.forEach { t ->
                        val trackItem = JSONObject()
                        trackItem.put("path", t.path)
                        trackItem.put("title", t.title)
                        trackItem.put("artist", t.artist)
                        tracksArray.put(trackItem)
                    }
                    plObj.put("tracks", tracksArray)
                    playlistsArray.put(plObj)
                }
                root.put("playlists", playlistsArray)

                // 2. Favorites
                val trackDao = database.trackDao()
                val favorites = trackDao.getFavoriteTracks()
                val favoritesArray = JSONArray()
                favorites.forEach { f ->
                    val favItem = JSONObject()
                    favItem.put("path", f.path)
                    favItem.put("title", f.title)
                    favItem.put("artist", f.artist)
                    favoritesArray.put(favItem)
                }
                root.put("favorites", favoritesArray)

                // 3. Custom EQ Presets
                val eqPresetDao = database.eqPresetDao()
                val allPresets = eqPresetDao.getAllPresets()
                val customPresets = allPresets.filter { !it.isBuiltIn }
                val presetsArray = JSONArray()
                customPresets.forEach { p ->
                    val pObj = JSONObject()
                    pObj.put("name", p.name)
                    pObj.put("preampDb", p.preampGainDb.toDouble())
                    pObj.put("bandGains", p.bandGainsJson)
                    presetsArray.put(pObj)
                }
                root.put("customEqPresets", presetsArray)

                // 4. Core Settings
                val settingsObj = JSONObject()
                settingsObj.put("themeId", settingsRepository.themeId.first())
                settingsObj.put("minDurationSeconds", settingsRepository.minDurationSeconds.first())
                settingsObj.put("finishTrackOnSleep", settingsRepository.finishTrackOnSleep.first())
                settingsObj.put("sleepFadeOut", settingsRepository.sleepFadeOut.first())
                settingsObj.put("autoSwitchDeviceProfiles", settingsRepository.autoSwitchDeviceProfiles.first())
                settingsObj.put("gaplessEnabled", settingsRepository.gaplessEnabled.first())
                settingsObj.put("crossfadeSeconds", settingsRepository.crossfadeSeconds.first())
                settingsObj.put("playbackSpeed", settingsRepository.playbackSpeed.first().toDouble())
                root.put("settings", settingsObj)

                // Write to SAF Uri
                context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                    BufferedWriter(OutputStreamWriter(outStream, StandardCharsets.UTF_8)).use { writer ->
                        writer.write(root.toString(2))
                        writer.flush()
                    }
                } ?: return@withContext Result.failure(Exception("Unable to open output stream for destination"))

                Result.success(
                    BackupSummary(
                        playlistCount = playlists.size,
                        favoriteCount = favorites.size,
                        customEqCount = customPresets.size,
                        timestamp = timestamp
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun importBackup(context: Context, sourceUri: Uri): Result<RestoreSummary> =
        withContext(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                    BufferedReader(InputStreamReader(inStream, StandardCharsets.UTF_8)).readText()
                } ?: return@withContext Result.failure(Exception("Unable to read backup file"))

                val root = JSONObject(jsonString)
                val app = root.optString("app")
                if (app != "CodeWave") {
                    return@withContext Result.failure(Exception("Invalid backup format: Not a CodeWave backup"))
                }

                val trackDao = database.trackDao()
                val playlistDao = database.playlistDao()
                val eqPresetDao = database.eqPresetDao()

                val allTracks = trackDao.getAllTracks()
                val trackByPath = allTracks.associateBy { it.path }
                val trackByTitleArtist = allTracks.associateBy { "${it.title.lowercase()}:::${it.artist.lowercase()}" }

                var restoredPlaylists = 0
                var restoredFavorites = 0
                var restoredPresets = 0

                // 1. Restore Playlists
                val playlistsArray = root.optJSONArray("playlists")
                if (playlistsArray != null) {
                    val existingPlaylists = playlistDao.getAllPlaylists()
                    for (i in 0 until playlistsArray.length()) {
                        val plObj = playlistsArray.getJSONObject(i)
                        val name = plObj.getString("name")
                        val existing = existingPlaylists.find { it.name.equals(name, ignoreCase = true) }
                        val playlistId = existing?.id ?: playlistDao.insertPlaylist(
                            PlaylistEntity(name = name, createdAt = plObj.optLong("createdAt", System.currentTimeMillis()))
                        )

                        val tracksArray = plObj.optJSONArray("tracks")
                        if (tracksArray != null) {
                            for (tIdx in 0 until tracksArray.length()) {
                                val tObj = tracksArray.getJSONObject(tIdx)
                                val path = tObj.optString("path")
                                val title = tObj.optString("title")
                                val artist = tObj.optString("artist")

                                val matchedTrack = trackByPath[path]
                                    ?: trackByTitleArtist["${title.lowercase()}:::${artist.lowercase()}"]

                                if (matchedTrack != null) {
                                    playlistDao.addTrackToPlaylist(
                                        PlaylistTrackCrossRef(
                                            playlistId = playlistId,
                                            trackId = matchedTrack.id,
                                            position = tIdx
                                        )
                                    )
                                }
                            }
                        }
                        restoredPlaylists++
                    }
                }

                // 2. Restore Favorites
                val favoritesArray = root.optJSONArray("favorites")
                if (favoritesArray != null) {
                    for (i in 0 until favoritesArray.length()) {
                        val fObj = favoritesArray.getJSONObject(i)
                        val path = fObj.optString("path")
                        val title = fObj.optString("title")
                        val artist = fObj.optString("artist")

                        val matchedTrack = trackByPath[path]
                            ?: trackByTitleArtist["${title.lowercase()}:::${artist.lowercase()}"]

                        if (matchedTrack != null) {
                            trackDao.setFavorite(matchedTrack.id, true)
                            restoredFavorites++
                        }
                    }
                }

                // 3. Restore Custom EQ Presets
                val presetsArray = root.optJSONArray("customEqPresets")
                if (presetsArray != null) {
                    val existingPresets = eqPresetDao.getAllPresets()
                    for (i in 0 until presetsArray.length()) {
                        val pObj = presetsArray.getJSONObject(i)
                        val name = pObj.getString("name")
                        val preampDb = pObj.optDouble("preampDb", 0.0).toFloat()
                        val bandGains = pObj.getString("bandGains")

                        val existing = existingPresets.find { it.name.equals(name, ignoreCase = true) && !it.isBuiltIn }
                        eqPresetDao.insertPreset(
                            EQPresetEntity(
                                id = existing?.id ?: 0L,
                                name = name,
                                isBuiltIn = false,
                                preampGainDb = preampDb,
                                bandGainsJson = bandGains
                            )
                        )
                        restoredPresets++
                    }
                }

                // 4. Restore Settings
                val settingsObj = root.optJSONObject("settings")
                var settingsRestored = false
                if (settingsObj != null) {
                    if (settingsObj.has("themeId")) {
                        settingsRepository.setThemeId(settingsObj.getString("themeId"))
                    }
                    if (settingsObj.has("minDurationSeconds")) {
                        settingsRepository.setMinDurationSeconds(settingsObj.getInt("minDurationSeconds"))
                    }
                    if (settingsObj.has("finishTrackOnSleep")) {
                        settingsRepository.setFinishTrackOnSleep(settingsObj.getBoolean("finishTrackOnSleep"))
                    }
                    if (settingsObj.has("sleepFadeOut")) {
                        settingsRepository.setSleepFadeOut(settingsObj.getBoolean("sleepFadeOut"))
                    }
                    if (settingsObj.has("autoSwitchDeviceProfiles")) {
                        settingsRepository.setAutoSwitchDeviceProfiles(settingsObj.getBoolean("autoSwitchDeviceProfiles"))
                    }
                    if (settingsObj.has("gaplessEnabled")) {
                        settingsRepository.setGaplessEnabled(settingsObj.getBoolean("gaplessEnabled"))
                    }
                    if (settingsObj.has("crossfadeSeconds")) {
                        settingsRepository.setCrossfadeSeconds(settingsObj.getInt("crossfadeSeconds"))
                    }
                    if (settingsObj.has("playbackSpeed")) {
                        settingsRepository.setPlaybackSpeed(settingsObj.getDouble("playbackSpeed").toFloat())
                    }
                    settingsRestored = true
                }

                Result.success(
                    RestoreSummary(
                        playlistsRestored = restoredPlaylists,
                        favoritesRestored = restoredFavorites,
                        presetsRestored = restoredPresets,
                        settingsRestored = settingsRestored
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
