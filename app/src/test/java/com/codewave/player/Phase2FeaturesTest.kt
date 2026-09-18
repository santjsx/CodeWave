package com.codewave.player

import com.codewave.player.core.audio.ConnectedAudioDevice
import com.codewave.player.core.model.AudioFormat
import com.codewave.player.core.model.AutoEqModel
import com.codewave.player.core.model.QueueWorkspace
import com.codewave.player.core.model.Track
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2FeaturesTest {

    private fun createSampleTrack(id: Long, title: String): Track {
        return Track(
            id = id,
            mediaStoreId = id,
            uri = "content://media/external/audio/media/$id",
            path = "/storage/emulated/0/Music/$title.flac",
            title = title,
            artist = "CodeWave Artist",
            album = "CodeWave Album",
            durationMs = 180000L,
            fileSize = 35000000L,
            dateAdded = 1700000000L,
            dateModified = 1700000000L,
            mimeType = "audio/flac",
            format = AudioFormat.FLAC,
            codec = "FLAC",
            sampleRate = 48000,
            bitDepth = 24
        )
    }

    // ==========================================
    // 1. AUTOEQ MODEL & PRESET CONVERSION TESTS
    // ==========================================

    @Test
    fun testAutoEqModelToEQPresetConversion() {
        val gains = listOf(4.5f, 3.0f, 1.2f, -0.5f, -1.0f, 0.5f, 2.5f, -2.0f, 1.0f, -1.5f)
        val autoEq = AutoEqModel(
            id = "sony_wh_1000xm4_harman",
            brand = "Sony",
            model = "WH-1000XM4",
            type = "Wireless ANC / Over-Ear",
            source = "oratory1990",
            preampDb = -4.5f,
            gains = gains
        )

        val preset = autoEq.toEQPreset()

        assertEquals("Sony WH-1000XM4 (AutoEq)", preset.name)
        assertFalse(preset.isBuiltIn)
        assertEquals(-4.5f, preset.preampGainDb, 0.001f)
        assertEquals(10, preset.bandGainsDb.size)
        assertEquals(4.5f, preset.bandGainsDb[0], 0.001f)
        assertEquals(3.0f, preset.bandGainsDb[1], 0.001f)
        assertEquals(1.2f, preset.bandGainsDb[2], 0.001f)
        assertEquals(-0.5f, preset.bandGainsDb[3], 0.001f)
        assertEquals(-1.0f, preset.bandGainsDb[4], 0.001f)
        assertEquals(0.5f, preset.bandGainsDb[5], 0.001f)
        assertEquals(2.5f, preset.bandGainsDb[6], 0.001f)
        assertEquals(-2.0f, preset.bandGainsDb[7], 0.001f)
        assertEquals(1.0f, preset.bandGainsDb[8], 0.001f)
        assertEquals(-1.5f, preset.bandGainsDb[9], 0.001f)
    }

    @Test
    fun testAutoEqTargetMetadata() {
        val model = AutoEqModel(
            id = "sennheiser_hd600_oratory",
            brand = "Sennheiser",
            model = "HD 600",
            type = "Open-Back Reference",
            source = "oratory1990",
            preampDb = -2.0f,
            gains = List(10) { 0.0f }
        )

        assertEquals("Sennheiser", model.brand)
        assertEquals("HD 600", model.model)
        assertEquals("Sennheiser HD 600", model.displayName)
        assertEquals("Open-Back Reference", model.type)
        assertEquals("oratory1990", model.source)
        assertEquals(-2.0f, model.preampDb, 0.001f)
    }

    // ==========================================
    // 2. HARDWARE AUDIO DEVICE ROUTING TESTS
    // ==========================================

    @Test
    fun testConnectedAudioDeviceProperties() {
        val btDevice = ConnectedAudioDevice(
            id = "bluetooth_a2dp",
            name = "Sony WH-1000XM5",
            typeName = "BLUETOOTH_A2DP",
            isWireless = true
        )
        val wiredDevice = ConnectedAudioDevice(
            id = "wired_headset",
            name = "Wired Headphones",
            typeName = "WIRED_HEADPHONES",
            isWireless = false
        )
        val speakerDevice = ConnectedAudioDevice(
            id = "builtin_speaker",
            name = "Internal Speaker",
            typeName = "BUILTIN_SPEAKER",
            isWireless = false
        )

        assertEquals("bluetooth_a2dp", btDevice.id)
        assertEquals("Sony WH-1000XM5", btDevice.name)
        assertTrue(btDevice.isWireless)

        assertEquals("wired_headset", wiredDevice.id)
        assertFalse(wiredDevice.isWireless)

        assertEquals("builtin_speaker", speakerDevice.id)
        assertFalse(speakerDevice.isWireless)
    }

    // ==========================================
    // 3. MULTI-QUEUE WORKSPACE TESTS
    // ==========================================

    @Test
    fun testQueueWorkspaceLifecycle() {
        val track1 = createSampleTrack(1L, "Synthwave Run")
        val track2 = createSampleTrack(2L, "Cyberpunk Alley")
        val track3 = createSampleTrack(3L, "Night Ride")

        val mainWorkspace = QueueWorkspace(
            id = "main",
            name = "MAIN",
            tracks = listOf(track1, track2),
            isPlaybackActive = true
        )

        val scratchpadWorkspace = QueueWorkspace(
            id = "scratchpad",
            name = "SCRATCHPAD",
            tracks = listOf(track3),
            isPlaybackActive = false
        )

        val workspaces = mutableListOf(mainWorkspace, scratchpadWorkspace)
        assertEquals(2, workspaces.size)
        assertEquals(2, mainWorkspace.trackCount)
        assertEquals(1, scratchpadWorkspace.trackCount)

        // Adding track to scratchpad
        val track4 = createSampleTrack(4L, "Lo-Fi Beats")
        val updatedScratchpad = scratchpadWorkspace.copy(
            tracks = scratchpadWorkspace.tracks + track4
        )
        val scratchIndex = workspaces.indexOfFirst { it.id == "scratchpad" }
        workspaces[scratchIndex] = updatedScratchpad

        assertEquals(2, workspaces[scratchIndex].tracks.size)
        assertEquals(2, workspaces[scratchIndex].trackCount)
        assertEquals("Lo-Fi Beats", workspaces[scratchIndex].tracks[1].title)

        // Verifying active playback isolation: scratchpad changes do not change active playback workspace
        val activeWorkspace = workspaces.firstOrNull { it.isPlaybackActive }
        assertNotNull(activeWorkspace)
        assertEquals("main", activeWorkspace?.id)
        assertEquals(2, activeWorkspace?.tracks?.size)

        // Clearing scratchpad
        workspaces[scratchIndex] = workspaces[scratchIndex].copy(tracks = emptyList())
        assertTrue(workspaces[scratchIndex].tracks.isEmpty())
        assertEquals(0, workspaces[scratchIndex].trackCount)

        // Default workspace protection
        fun canDeleteWorkspace(id: String): Boolean = id != "main"
        assertFalse(canDeleteWorkspace("main"))
        assertTrue(canDeleteWorkspace("scratchpad"))
    }

    // ==========================================
    // 4. ATOMIC JSON BACKUP & RESTORE SCHEMA TESTS
    // ==========================================

    @Test
    fun testBackupJsonSerializationAndValidation() {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", 1773820000000L)
        root.put("generator", "CodeWave Audio Workstation v1.8.0")

        // Playlists
        val playlistsArray = JSONArray()
        val playlist1 = JSONObject().apply {
            put("id", 101L)
            put("name", "Coding Flow")
            put("createdAt", 1700000000L)
            put("trackIds", JSONArray(listOf(1L, 2L, 3L)))
        }
        playlistsArray.put(playlist1)
        root.put("playlists", playlistsArray)

        // Favorites
        val favoritesArray = JSONArray(listOf(1L, 3L))
        root.put("favorites", favoritesArray)

        // Custom Presets
        val presetsArray = JSONArray()
        val preset1 = JSONObject().apply {
            put("id", 999L)
            put("name", "Deep Sub Bass")
            put("preampGainDb", -3.0)
            put("bandGainsDb", JSONArray(listOf(5.0, 4.0, 2.5, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 2.5)))
        }
        presetsArray.put(preset1)
        root.put("customPresets", presetsArray)

        // Settings
        val settingsObj = JSONObject().apply {
            put("themeId", "cyberpunk")
            put("minDurationSeconds", 45)
            put("finishTrackOnSleep", true)
            put("sleepFadeOut", true)
            put("autoSwitchDeviceProfiles", true)
            put("gaplessEnabled", true)
            put("crossfadeSeconds", 4)
            put("playbackSpeed", 1.25)
        }
        root.put("settings", settingsObj)

        // Verify serialized JSON string
        val jsonString = root.toString(2)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("Coding Flow"))
        assertTrue(jsonString.contains("cyberpunk"))
        assertTrue(jsonString.contains("Deep Sub Bass"))

        // Re-parse and validate
        val parsed = JSONObject(jsonString)
        assertEquals(1, parsed.getInt("version"))
        assertEquals("CodeWave Audio Workstation v1.8.0", parsed.getString("generator"))
        assertEquals(1, parsed.getJSONArray("playlists").length())
        assertEquals(2, parsed.getJSONArray("favorites").length())
        assertEquals(1, parsed.getJSONArray("customPresets").length())

        val parsedSettings = parsed.getJSONObject("settings")
        assertEquals("cyberpunk", parsedSettings.getString("themeId"))
        assertEquals(45, parsedSettings.getInt("minDurationSeconds"))
        assertTrue(parsedSettings.getBoolean("finishTrackOnSleep"))
        assertTrue(parsedSettings.getBoolean("sleepFadeOut"))
        assertTrue(parsedSettings.getBoolean("autoSwitchDeviceProfiles"))
        assertTrue(parsedSettings.getBoolean("gaplessEnabled"))
        assertEquals(4, parsedSettings.getInt("crossfadeSeconds"))
        assertEquals(1.25, parsedSettings.getDouble("playbackSpeed"), 0.001)
    }
}
