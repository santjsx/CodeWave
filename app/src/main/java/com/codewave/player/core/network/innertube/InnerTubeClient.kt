package com.codewave.player.core.network.innertube

import android.net.Uri
import com.codewave.player.core.model.AudioFormat
import com.codewave.player.core.model.AudioSourceType
import com.codewave.player.core.model.StreamQuality
import com.codewave.player.core.model.StreamTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ResolvedStreamInfo(
    val streamUrl: String,
    val mimeType: String,
    val bitrateKbps: Int,
    val sampleRate: Int,
    val expiryEpochMs: Long
)

class InnerTubeClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val BASE_URL = "https://music.youtube.com/youtubei/v1"
        private const val USER_AGENT = "com.google.android.apps.youtube.music/6.41.52 (Linux; U; Android 14; en_US) gzip"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun createBasePayload(): JSONObject {
            return JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID_MUSIC")
                        put("clientVersion", "6.41.52")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
            }
        }
    }

    /**
     * Search YouTube Music for tracks, albums, or artists with resilient JSON traversal.
     */
    suspend fun search(query: String): Result<List<StreamTrack>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())

        try {
            val payload = createBasePayload().apply {
                put("query", query)
                put("params", "Eg-KAQwIARAAGAAgACgAMABqChAGEAkQChAMEAo%3D") // Filter by Songs
            }

            val request = Request.Builder()
                .url("$BASE_URL/search")
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("InnerTube Search HTTP ${response.code}"))
            }

            val responseBody = response.body?.string().orEmpty()
            val json = JSONObject(responseBody)
            val tracks = parseSearchTracks(json)
            Result.success(tracks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches trending / explore charts for CodeWave's online exploration shelf.
     */
    suspend fun getExploreCharts(): Result<List<StreamTrack>> = withContext(Dispatchers.IO) {
        try {
            val payload = createBasePayload().apply {
                put("browseId", "FEmusic_charts")
            }

            val request = Request.Builder()
                .url("$BASE_URL/browse")
                .header("User-Agent", USER_AGENT)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("InnerTube Explore HTTP ${response.code}"))
            }

            val responseBody = response.body?.string().orEmpty()
            val json = JSONObject(responseBody)
            val tracks = parseExploreTracks(json)
            Result.success(tracks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves direct, playable audio stream URL for a given YouTube video/song ID.
     * Selects highest bitrate Opus (251) or AAC (140) adaptive format stream.
     */
    suspend fun getStreamUrl(videoId: String, targetQuality: StreamQuality = StreamQuality.HIGH): Result<ResolvedStreamInfo> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID_MUSIC")
                        put("clientVersion", "6.41.52")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("videoId", videoId)
            }

            val request = Request.Builder()
                .url("$BASE_URL/player")
                .header("User-Agent", USER_AGENT)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("InnerTube Player HTTP ${response.code}"))
            }

            val responseBody = response.body?.string().orEmpty()
            val json = JSONObject(responseBody)

            val streamingData = json.optJSONObject("streamingData")
                ?: return@withContext Result.failure(IllegalStateException("No streamingData found"))

            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
                ?: streamingData.optJSONArray("formats")
                ?: return@withContext Result.failure(IllegalStateException("No adaptiveFormats available"))

            var bestUrl: String? = null
            var bestBitrate = 0
            var bestMime = "audio/webm"
            var sampleRate = 48000

            for (i in 0 until adaptiveFormats.length()) {
                val format = adaptiveFormats.getJSONObject(i)
                val mimeType = format.optString("mimeType")
                if (!mimeType.startsWith("audio/")) continue

                val url = format.optString("url")
                if (url.isBlank()) continue

                val bitrate = format.optInt("bitrate", 0)
                val itag = format.optInt("itag", 0)

                // Prioritize Opus 160k (itag 251) or high bitrate audio
                val isOpusPreferred = itag == 251 || (mimeType.contains("opus") && bitrate >= bestBitrate)
                val isQualityMatch = bitrate > bestBitrate

                if (bestUrl == null || isOpusPreferred || isQualityMatch) {
                    bestUrl = url
                    bestBitrate = bitrate
                    bestMime = mimeType.substringBefore(";")
                    sampleRate = format.optInt("audioSampleRate", 48000)
                }
            }

            if (bestUrl == null) {
                return@withContext Result.failure(IllegalStateException("Could not extract playable audio stream"))
            }

            // Parse expiry timestamp from URL query param `expire`
            val expirySeconds = Uri.parse(bestUrl).getQueryParameter("expire")?.toLongOrNull()
            val expiryEpochMs = if (expirySeconds != null) expirySeconds * 1000L else System.currentTimeMillis() + (6 * 3600 * 1000L)

            Result.success(
                ResolvedStreamInfo(
                    streamUrl = bestUrl,
                    mimeType = bestMime,
                    bitrateKbps = bestBitrate / 1000,
                    sampleRate = sampleRate,
                    expiryEpochMs = expiryEpochMs
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseSearchTracks(json: JSONObject): List<StreamTrack> {
        val results = mutableListOf<StreamTrack>()
        try {
            val contents = json.optJSONObject("contents")
                ?.optJSONObject("tabbedSearchResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return results

            for (i in 0 until contents.length()) {
                val section = contents.optJSONObject(i) ?: continue
                val shelfRenderer = section.optJSONObject("musicShelfRenderer")
                    ?: section.optJSONObject("musicCardShelfRenderer") ?: continue

                val items = shelfRenderer.optJSONArray("contents") ?: continue
                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j) ?: continue
                    val renderer = item.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                    parseItemRenderer(renderer)?.let { results.add(it) }
                }
            }
        } catch (_: Exception) {
            // Defensively swallow malformed sections
        }
        return results
    }

    private fun parseExploreTracks(json: JSONObject): List<StreamTrack> {
        val results = mutableListOf<StreamTrack>()
        try {
            val sections = json.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return results

            for (i in 0 until sections.length()) {
                val section = sections.optJSONObject(i) ?: continue
                val shelf = section.optJSONObject("musicCarouselShelfRenderer")
                    ?: section.optJSONObject("musicShelfRenderer") ?: continue

                val contents = shelf.optJSONArray("contents") ?: continue
                for (j in 0 until contents.length()) {
                    val item = contents.optJSONObject(j) ?: continue
                    val renderer = item.optJSONObject("musicResponsiveListItemRenderer")
                        ?: item.optJSONObject("musicTwoRowItemRenderer") ?: continue
                    parseItemRenderer(renderer)?.let { results.add(it) }
                }
            }
        } catch (_: Exception) {
            // Return whatever parsed cleanly
        }
        return results
    }

    private fun parseItemRenderer(renderer: JSONObject): StreamTrack? {
        return try {
            val videoId = renderer.optJSONObject("playlistItemData")?.optString("videoId")
                ?: renderer.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
                ?: return null

            if (videoId.isBlank()) return null

            val flexColumns = renderer.optJSONArray("flexColumns") ?: return null
            if (flexColumns.length() == 0) return null

            // Title
            val titleCol = flexColumns.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
            val title = titleCol?.optJSONObject(0)?.optString("text").orEmpty()
            if (title.isBlank()) return null

            // Artist, Album, Duration
            var artist = "Unknown Artist"
            var album = "Unknown Album"
            var durationMs = 180000L // 3 min default fallback

            if (flexColumns.length() > 1) {
                val subtitleRuns = flexColumns.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")

                if (subtitleRuns != null) {
                    val textList = mutableListOf<String>()
                    for (k in 0 until subtitleRuns.length()) {
                        val runText = subtitleRuns.optJSONObject(k)?.optString("text").orEmpty().trim()
                        if (runText.isNotEmpty() && runText != "•") {
                            textList.add(runText)
                        }
                    }

                    if (textList.isNotEmpty()) artist = textList[0]
                    if (textList.size > 1) album = textList[1]
                    if (textList.size > 2 && textList.last().contains(":")) {
                        durationMs = parseDurationMs(textList.last())
                    }
                }
            }

            // Thumbnail / Artwork
            val thumbnails = renderer.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")

            val artworkUri = if (thumbnails != null && thumbnails.length() > 0) {
                thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url")
            } else null

            StreamTrack(
                id = videoId,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                artworkUri = artworkUri,
                sourceType = AudioSourceType.YOUTUBE_MUSIC,
                approximateBitrateKbps = 160,
                audioFormat = AudioFormat.OPUS
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDurationMs(durationStr: String): Long {
        return try {
            val parts = durationStr.split(":").map { it.trim().toLong() }
            when (parts.size) {
                2 -> (parts[0] * 60 + parts[1]) * 1000L
                3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
                else -> 180000L
            }
        } catch (_: Exception) {
            180000L
        }
    }
}
