package com.codewave.player.core.media

import com.codewave.player.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LrclibLyricsProvider(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Fetches real-time synchronized lyrics from LRCLIB for a given track.
     * Returns parsed [LyricsResult] containing line-by-line timestamps.
     */
    suspend fun fetchLyrics(track: Track): LyricsResult = withContext(Dispatchers.IO) {
        try {
            // First attempt: Exact match query
            val titleEncoded = URLEncoder.encode(cleanTitle(track.title), "UTF-8")
            val artistEncoded = URLEncoder.encode(track.artist, "UTF-8")
            val durationSec = track.durationMs / 1000

            val exactUrl = "https://lrclib.net/api/get?artist_name=$artistEncoded&track_name=$titleEncoded&duration=$durationSec"
            val request = Request.Builder()
                .url(exactUrl)
                .header("User-Agent", "CodeWave/1.4.0 (https://github.com/santjsx/CodeWave)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string().orEmpty())
                val parsed = parseLyricsFromJson(json)
                if (parsed != null) return@withContext parsed
            }

            // Second attempt: Search query
            val searchUrl = "https://lrclib.net/api/search?q=" + URLEncoder.encode("${cleanTitle(track.title)} ${track.artist}", "UTF-8")
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "CodeWave/1.4.0 (https://github.com/santjsx/CodeWave)")
                .build()

            val searchResponse = okHttpClient.newCall(searchRequest).execute()
            if (searchResponse.isSuccessful) {
                val array = JSONArray(searchResponse.body?.string().orEmpty())
                if (array.length() > 0) {
                    val firstMatch = array.getJSONObject(0)
                    val parsed = parseLyricsFromJson(firstMatch)
                    if (parsed != null) return@withContext parsed
                }
            }

            LyricsResult.Unavailable
        } catch (e: Exception) {
            LyricsResult.Error("Could not retrieve online lyrics: ${e.localizedMessage}")
        }
    }

    private fun parseLyricsFromJson(json: JSONObject): LyricsResult? {
        val syncedLyrics = json.optString("syncedLyrics")
        if (syncedLyrics.isNotBlank()) {
            val lines = LrcParser.parse(syncedLyrics)
            if (lines.isNotEmpty()) {
                return LyricsResult.Synchronized(lines)
            }
        }

        val plainLyrics = json.optString("plainLyrics")
        if (plainLyrics.isNotBlank()) {
            val plainLines = plainLyrics.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (plainLines.isNotEmpty()) {
                return LyricsResult.Plain(plainLines)
            }
        }

        return null
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\(.*\\)"), "")
            .replace(Regex("(?i)\\[.*\\]"), "")
            .trim()
    }
}
