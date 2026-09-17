package com.codewave.player.core.network.resolver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class PlatformLinks(
    val spotifyUrl: String? = null,
    val tidalUrl: String? = null,
    val tidalId: String? = null,
    val deezerUrl: String? = null,
    val deezerId: String? = null,
    val qobuzUrl: String? = null,
    val qobuzId: String? = null,
    val youtubeUrl: String? = null,
    val youtubeId: String? = null
)

data class EnrichedTrackMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val isrc: String? = null,
    val bpm: Double? = null,
    val releaseDate: String? = null,
    val recordLabel: String? = null,
    val coverUrl: String? = null,
    val durationSeconds: Int = 0
)

/**
 * Universal cross-platform track link and metadata resolver.
 * Replicates SpotiFLAC's Songlink/Odesli and Deezer metadata resolution pipeline
 * to connect Spotify/ISRC identifiers to Tidal, Qobuz, Deezer, and YouTube Music.
 */
class PlatformResolver(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
        private val TIDAL_PATTERN = Pattern.compile("""https?://(?:listen\.)?tidal\.com/(?:browse/)?track/(\d+)""")
        private val DEEZER_PATTERN = Pattern.compile("""https?://(?:www\.)?deezer\.com/(?:[a-z]{2}/)?track/(\d+)""")
        private val QOBUZ_PATTERN = Pattern.compile("""https?://(?:open|play)\.qobuz\.com/track/([a-zA-Z0-9]+)""")
        private val YOUTUBE_PATTERN = Pattern.compile("""https?://(?:music\.)?youtube\.com/watch\?v=([a-zA-Z0-9_-]{11})""")
    }

    /**
     * Resolves matching streaming platform links from a Spotify URL or track ID using Songlink/Odesli.
     */
    suspend fun resolvePlatformLinks(spotifyUrlOrId: String): Result<PlatformLinks> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = if (spotifyUrlOrId.startsWith("http")) {
                "https://song.link/" + URLEncoder.encode(spotifyUrlOrId, "UTF-8")
            } else {
                "https://song.link/s/$spotifyUrlOrId"
            }

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Songlink returned HTTP ${response.code}"))
            }

            val html = response.body?.string().orEmpty()
            Result.success(extractPlatformLinksFromHtml(html, spotifyUrlOrId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun extractPlatformLinksFromHtml(html: String, spotifyUrlOrId: String? = null): PlatformLinks {
        var tidalUrl: String? = null
        var tidalId: String? = null
        val tidalMatcher = TIDAL_PATTERN.matcher(html)
        if (tidalMatcher.find()) {
            tidalUrl = tidalMatcher.group(0)
            tidalId = tidalMatcher.group(1)
        }

        var deezerUrl: String? = null
        var deezerId: String? = null
        val deezerMatcher = DEEZER_PATTERN.matcher(html)
        if (deezerMatcher.find()) {
            deezerUrl = deezerMatcher.group(0)
            deezerId = deezerMatcher.group(1)
        }

        var qobuzUrl: String? = null
        var qobuzId: String? = null
        val qobuzMatcher = QOBUZ_PATTERN.matcher(html)
        if (qobuzMatcher.find()) {
            qobuzUrl = qobuzMatcher.group(0)
            qobuzId = qobuzMatcher.group(1)
        }

        var youtubeUrl: String? = null
        var youtubeId: String? = null
        val ytMatcher = YOUTUBE_PATTERN.matcher(html)
        if (ytMatcher.find()) {
            youtubeUrl = ytMatcher.group(0)
            youtubeId = ytMatcher.group(1)
        }

        return PlatformLinks(
            spotifyUrl = spotifyUrlOrId,
            tidalUrl = tidalUrl,
            tidalId = tidalId,
            deezerUrl = deezerUrl,
            deezerId = deezerId,
            qobuzUrl = qobuzUrl,
            qobuzId = qobuzId,
            youtubeUrl = youtubeUrl,
            youtubeId = youtubeId
        )
    }

    /**
     * Enriches track metadata by querying the Deezer public API with a Deezer track ID.
     */
    suspend fun fetchDeezerTrackMetadata(deezerTrackId: String): Result<EnrichedTrackMetadata> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.deezer.com/track/$deezerTrackId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Deezer API returned HTTP ${response.code}"))
            }

            val json = JSONObject(response.body?.string().orEmpty())
            if (json.has("error")) {
                return@withContext Result.failure(Exception(json.optJSONObject("error")?.optString("message") ?: "Deezer error"))
            }

            val title = json.optString("title")
            val artist = json.optJSONObject("artist")?.optString("name") ?: "Unknown Artist"
            val albumObj = json.optJSONObject("album")
            val album = albumObj?.optString("title") ?: title
            val isrc = json.optString("isrc").takeIf { it.isNotBlank() }
            val bpm = json.optDouble("bpm").takeIf { !it.isNaN() && it > 0 }
            val releaseDate = json.optString("release_date").takeIf { it.isNotBlank() }
            val label = json.optString("label").takeIf { it.isNotBlank() }
            val coverUrl = albumObj?.optString("cover_xl")?.takeIf { it.isNotBlank() }
                ?: albumObj?.optString("cover_big")?.takeIf { it.isNotBlank() }
            val duration = json.optInt("duration", 0)

            Result.success(
                EnrichedTrackMetadata(
                    title = title,
                    artist = artist,
                    album = album,
                    isrc = isrc,
                    bpm = bpm,
                    releaseDate = releaseDate,
                    recordLabel = label,
                    coverUrl = coverUrl,
                    durationSeconds = duration
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
