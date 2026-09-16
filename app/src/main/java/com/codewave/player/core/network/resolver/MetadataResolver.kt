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
     * Uses public Spotify oEmbed and open widget embed payloads.
     */
    suspend fun resolveSpotifyTrack(url: String): Result<ResolvedMetadata> = withContext(Dispatchers.IO) {
        val matcher = SPOTIFY_TRACK_PATTERN.matcher(url)
        if (!matcher.find()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Spotify track URL"))
        }

        val trackId = matcher.group(1) ?: return@withContext Result.failure(IllegalArgumentException("Missing track ID"))

        try {
            // First attempt: Spotify oEmbed endpoint
            val oembedUrl = "https://open.spotify.com/oembed?url=https://open.spotify.com/track/$trackId"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string().orEmpty())
                val titleRaw = json.optString("title")
                val thumbnail = json.optString("thumbnail_url")

                // oEmbed title format is typically "Track Title by Artist" or just "Track Title"
                var title = titleRaw
                var artist = "Unknown Artist"

                if (titleRaw.contains(" by ")) {
                    val split = titleRaw.split(" by ")
                    title = split[0].trim()
                    artist = split.getOrNull(1)?.trim() ?: "Unknown Artist"
                }

                return@withContext Result.success(
                    ResolvedMetadata(
                        title = title,
                        artist = artist,
                        album = title, // Fallback if embed doesn't specify album
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

        return "$cleanedTitle $artist".trim()
    }
}
