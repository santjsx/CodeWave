package com.codewave.player

import com.codewave.player.core.media.LyricLine
import com.codewave.player.ui.command.PaletteCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1FeaturesTest {

    @Test
    fun testLyricsOffsetMath() {
        val lines = listOf(
            LyricLine(1000L, "First line"),
            LyricLine(3000L, "Second line"),
            LyricLine(5000L, "Third line")
        )

        // Raw position 2800ms: with 0 offset, current line is index 0
        var currentPos = 2800L
        var offsetMs = 0L
        var effectivePos = (currentPos - offsetMs).coerceAtLeast(0L)
        var activeLine = lines.indexOfLast { it.timestampMs <= effectivePos }
        assertEquals(0, activeLine)

        // With +300ms offset (vocals arrived late, advance lyrics):
        // effectivePos = 2800 - (-300) or position - (-offset)
        // In our formula: effectivePos = position - offset
        // If lyrics are ahead by 300ms, user offsets by +300ms:
        offsetMs = 300L
        effectivePos = (currentPos - offsetMs).coerceAtLeast(0L) // 2500ms
        assertEquals(0, lines.indexOfLast { it.timestampMs <= effectivePos })

        // If lyrics were late by 300ms and vocal started early: user sets offset -300ms
        offsetMs = -300L
        effectivePos = (currentPos - offsetMs).coerceAtLeast(0L) // 3100ms -> triggers line 1
        activeLine = lines.indexOfLast { it.timestampMs <= effectivePos }
        assertEquals(1, activeLine)
    }

    @Test
    fun testSmartSleepTimerExponentialFadeOut() {
        // Test exponential volume decay curve: V(t) = (remaining / 10000)^2
        fun calculateVolume(remainingMs: Long): Float {
            val progress = remainingMs.toFloat() / 10000f
            return (progress * progress).coerceIn(0.01f, 1.0f)
        }

        // At 10 seconds remaining, volume is full (1.0f)
        assertEquals(1.0f, calculateVolume(10000L), 0.001f)

        // At 5 seconds remaining, volume is 25% (0.25f)
        assertEquals(0.25f, calculateVolume(5000L), 0.001f)

        // At 1 second remaining, volume is 1% (0.01f)
        assertEquals(0.01f, calculateVolume(1000L), 0.001f)

        // At 0 seconds, volume clamps to minimum floor 0.01f before player pause
        assertEquals(0.01f, calculateVolume(0L), 0.001f)
    }

    @Test
    fun testSmartPathBlacklistFiltering() {
        val blacklistedPatterns = listOf(
            "/whatsapp/media/whatsapp audio",
            "/whatsapp/media/whatsapp voice notes",
            "/telegram/telegram audio",
            "/recordings/call",
            "/notifications",
            "/ringtones",
            "/alarms"
        )

        fun isPathBlacklisted(path: String): Boolean {
            val lower = path.lowercase().replace('\\', '/')
            return blacklistedPatterns.any { lower.contains(it) }
        }

        // Valid music files
        assertFalse(isPathBlacklisted("/storage/emulated/0/Music/Album/Track01.flac"))
        assertFalse(isPathBlacklisted("/storage/emulated/0/Download/Queen - Bohemian Rhapsody.mp3"))

        // Blacklisted voice notes and junk audio
        assertTrue(isPathBlacklisted("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes/PTT-20260918-WA0001.opus"))
        assertTrue(isPathBlacklisted("/storage/emulated/0/WhatsApp/Media/WhatsApp Audio/AUD-2026.m4a"))
        assertTrue(isPathBlacklisted("/storage/emulated/0/Telegram/Telegram Audio/VoiceMessage.ogg"))
        assertTrue(isPathBlacklisted("/storage/emulated/0/Recordings/Call/Call_2026.aac"))
        assertTrue(isPathBlacklisted("/storage/emulated/0/Notifications/Chime.mp3"))
        assertTrue(isPathBlacklisted("/storage/emulated/0/Ringtones/Marimba.mp3"))
    }

    @Test
    fun testDurationFilterThreshold() {
        fun isDurationAcceptable(durationMs: Long, minDurationSecs: Int): Boolean {
            return durationMs >= (minDurationSecs * 1000L)
        }

        val voiceNoteDurationMs = 7000L // 7 seconds
        val notificationDurationMs = 1200L // 1.2 seconds
        val fullSongDurationMs = 215000L // 3m 35s

        // Default 30s threshold
        assertFalse(isDurationAcceptable(voiceNoteDurationMs, 30))
        assertFalse(isDurationAcceptable(notificationDurationMs, 30))
        assertTrue(isDurationAcceptable(fullSongDurationMs, 30))

        // When user sets threshold to 0s (include all)
        assertTrue(isDurationAcceptable(voiceNoteDurationMs, 0))
    }

    @Test
    fun testCommandPaletteQueryMatching() {
        val sampleCommands = listOf(
            PaletteCommand(
                id = "theme_synthwave",
                category = "THEME",
                title = "Switch Theme: Synthwave '84",
                commandText = "> theme synthwave",
                description = "Neon violet and hot pink",
                action = {}
            ),
            PaletteCommand(
                id = "eq_bass",
                category = "EQ",
                title = "Equalizer Preset: Bass Boost",
                commandText = "> eq bass",
                description = "Low-end boost",
                action = {}
            ),
            PaletteCommand(
                id = "sleep_30",
                category = "SLEEP",
                title = "Sleep Timer: 30 Minutes",
                commandText = "> sleep 30",
                description = "Countdown 30m",
                action = {}
            )
        )

        fun filter(query: String): List<PaletteCommand> {
            val q = query.trim().removePrefix(">").trim().lowercase()
            return sampleCommands.filter {
                it.title.lowercase().contains(q) ||
                it.commandText.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
            }
        }

        // Fuzzy queries with or without leading '>'
        assertEquals(1, filter("> theme synth").size)
        assertEquals(1, filter("synthwave").size)
        assertEquals(1, filter("bass").size)
        assertEquals(1, filter("> sleep").size)
        assertEquals(3, filter("").size)
    }
}
