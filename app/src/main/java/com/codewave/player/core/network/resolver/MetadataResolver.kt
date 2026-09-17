package com.codewave.player.core.network.resolver

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ResolvedMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUrl: String?,
    val isrc: String? = null,
    val sourceService: String = "Spotify"
)

class MetadataResolver(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private val SPOTIFY_TRACK_PATTERN = Pattern.compile("spotify\\.com/track/([a-zA-Z0-9]+)")
        private val YOUTUBE_PATTERN = Pattern.compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|music/watch\\?v=))([a-zA-Z0-9_-]{11})")
    }

    fun isSpotifyUrl(url: String): Boolean = SPOTIFY_TRACK_PATTERN.matcher(url).find()
    fun isYouTubeUrl(url: String): Boolean = YOUTUBE_PATTERN.matcher(url).find()

    fun extractYouTubeId(url: String): String? {
        val matcher = YOUTUBE_PATTERN.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    /**
     * Resolves Spotify link into clean, structured metadata without requiring any user account/login.
     * Prioritizes Spotify open embed JSON payload (__NEXT_DATA__) for exact artists, duration, and cover art.
     * Falls back to OpenGraph meta tags and oEmbed.
     */
    suspend fun resolveSpotifyTrack(url: String): Result<ResolvedMetadata> = withContext(Dispatchers.IO) {
        val matcher = SPOTIFY_TRACK_PATTERN.matcher(url)
        if (!matcher.find()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Spotify track URL"))
        }

        val trackId = matcher.group(1) ?: return@withContext Result.failure(IllegalArgumentException("Missing track ID"))

        try {
            // Priority 1: Spotify open embed page with rich __NEXT_DATA__ JSON payload
            val embedUrl = "https://open.spotify.com/embed/track/$trackId"
            val embedReq = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36")
                .build()

            val embedRes = okHttpClient.newCall(embedReq).execute()
            if (embedRes.isSuccessful) {
                val html = embedRes.body?.string().orEmpty()
                val nextDataMatcher = Pattern.compile("<script id=\"__NEXT_DATA__\"[^>]*>(.*?)</script>").matcher(html)
                if (nextDataMatcher.find()) {
                    val jsonStr = nextDataMatcher.group(1).orEmpty()
                    val nextJson = JSONObject(jsonStr)
                    val entity = nextJson.optJSONObject("props")
                        ?.optJSONObject("pageProps")
                        ?.optJSONObject("state")
                        ?.optJSONObject("data")
                        ?.optJSONObject("entity")

                    if (entity != null) {
                        val title = entity.optString("name").ifBlank { entity.optString("title") }
                        val artistsList = mutableListOf<String>()
                        val artistsArray = entity.optJSONArray("artists")
                        if (artistsArray != null) {
                            for (i in 0 until artistsArray.length()) {
                                val aName = artistsArray.optJSONObject(i)?.optString("name")
                                if (!aName.isNullOrBlank()) artistsList.add(aName)
                            }
                        }
                        val artist = if (artistsList.isNotEmpty()) artistsList.joinToString(", ") else "Unknown Artist"
                        val durationMs = entity.optLong("duration", 180000L)
                        val visualIdentity = entity.optJSONObject("visualIdentity")
                        val coverArt = visualIdentity?.optJSONArray("image")?.optJSONObject(0)?.optString("url")
                            ?: visualIdentity?.optJSONObject("image")?.optString("url")

                        if (title.isNotBlank()) {
                            return@withContext Result.success(
                                ResolvedMetadata(
                                    title = title,
                                    artist = artist,
                                    album = title,
                                    durationMs = durationMs,
                                    artworkUrl = coverArt,
                                    sourceService = "Spotify"
                                )
                            )
                        }
                    }
                }
            }

            // Priority 2: Spotify public oEmbed endpoint
            val oembedUrl = "https://open.spotify.com/oembed?url=https://open.spotify.com/track/$trackId"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string().orEmpty())
                val titleRaw = json.optString("title")
                val thumbnail = json.optString("thumbnail_url")

                var title = titleRaw
                var artist = "Unknown Artist"

                if (titleRaw.contains(" by ")) {
                    val split = titleRaw.split(" by ")
                    title = split[0].trim()
                    artist = split.getOrNull(1)?.trim() ?: "Unknown Artist"
                }

                // If artist still unknown, attempt track page scraping for og:description (e.g. "Listen on Spotify. Artist · Song · 2024")
                if (artist == "Unknown Artist") {
                    try {
                        val pageReq = Request.Builder()
                            .url("https://open.spotify.com/track/$trackId")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .build()
                        val pageRes = okHttpClient.newCall(pageReq).execute()
                        if (pageRes.isSuccessful) {
                            val pageHtml = pageRes.body?.string().orEmpty()
                            val ogDescMatcher = Pattern.compile("<meta\\s+property=\"og:description\"\\s+content=\"(.*?)\"").matcher(pageHtml)
                            if (ogDescMatcher.find()) {
                                val desc = ogDescMatcher.group(1).orEmpty()
                                // Example: "Listen to Never Gonna Give You Up on Spotify. Rick Astley · Song · 1987."
                                val dotSplit = desc.split("·")
                                if (dotSplit.isNotEmpty()) {
                                    val candidate = dotSplit[0].substringAfter("Spotify.").trim()
                                    if (candidate.isNotBlank()) artist = candidate
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                return@withContext Result.success(
                    ResolvedMetadata(
                        title = title,
                        artist = artist,
                        album = title,
                        durationMs = 180000L,
                        artworkUrl = thumbnail.ifEmpty { null },
                        sourceService = "Spotify"
                    )
                )
            }

            Result.failure(IOException("Spotify resolution failed with code ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cleans noise from track titles (e.g. "(Official Audio)", "[Remastered 2021]", "ft. XYZ")
     * to maximize match rates when querying lossless FLAC databases.
     */
    fun cleanSearchQuery(title: String, artist: String): String {
        val cleanedTitle = title
            .replace(Regex("(?i)\\((?:official\\s*)?(?:music\\s*video|lyric\\s*video|video|audio|lyrics?)\\)"), "")
            .replace(Regex("(?i)\\[(?:official\\s*)?(?:music\\s*video|lyric\\s*video|video|audio|lyrics?)\\]"), "")
            .replace(Regex("(?i)\\((?:remaster(?:ed)?|live|acoustic|deluxe).*?\\)"), "")
            .replace(Regex("(?i)\\[(?:remaster(?:ed)?|live|acoustic|deluxe).*?\\]"), "")
            .replace(Regex("(?i)ft\\..*|feat\\..*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val cleanArtist = if (artist.equals("Unknown Artist", ignoreCase = true) || artist.isBlank()) {
            ""
        } else {
            artist.trim()
        }

        return if (cleanArtist.isNotEmpty()) "$cleanedTitle $cleanArtist".trim() else cleanedTitle
    }
}
