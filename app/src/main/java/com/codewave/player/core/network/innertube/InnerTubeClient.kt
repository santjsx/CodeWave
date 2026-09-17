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
import java.util.regex.Pattern

data class ResolvedStreamInfo(
    val streamUrl: String,
    val mimeType: String,
    val bitrateKbps: Int,
    val sampleRate: Int,
    val expiryEpochMs: Long,
    val mediaHeaders: Map<String, String> = emptyMap()
)

class InnerTubeClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val YOUTUBE_MUSIC_BASE_URL = "https://music.youtube.com/youtubei/v1"
        private const val YOUTUBE_BASE_URL = "https://www.youtube.com/youtubei/v1"
        private const val VISITOR_DATA_URL = "https://www.youtube.com/sw.js_data"
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val VISITOR_DATA_REGEX = Pattern.compile("""Cg[A-Za-z0-9_%-]{40,}""")
    }

    @Volatile
    private var cachedVisitorData: String? = null

    /**
     * Obtains a fresh or cached YouTube visitorData token from sw.js_data.
     * Prevents automated bot-traffic flags and allows anonymous stream minting.
     */
    suspend fun getOrFetchVisitorData(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        if (!forceRefresh && !cachedVisitorData.isNullOrBlank()) {
            return@withContext cachedVisitorData
        }

        try {
            val request = Request.Builder()
                .url(VISITOR_DATA_URL)
                .header("User-Agent", BROWSER_USER_AGENT)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val matcher = VISITOR_DATA_REGEX.matcher(body)
                if (matcher.find()) {
                    val token = matcher.group(0)
                    cachedVisitorData = token
                    return@withContext token
                }
            }
        } catch (_: Exception) {
            // Ignore failure, proceed anonymously
        }
        return@withContext cachedVisitorData
    }

    private suspend fun createBaseClientJson(client: PlayerClient): JSONObject {
        val clientObj = JSONObject().apply {
            put("clientName", client.clientName)
            put("clientVersion", client.clientVersion)
            client.osName?.let { put("osName", it) }
            client.osVersion?.let { put("osVersion", it) }
            client.deviceMake?.let { put("deviceMake", it) }
            client.deviceModel?.let { put("deviceModel", it) }
            client.androidSdkVersion?.let { put("androidSdkVersion", it) }
            put("hl", "en")
            put("gl", "US")
            val vData = getOrFetchVisitorData()
            if (!vData.isNullOrBlank()) {
                put("visitorData", vData)
            }
        }

        return JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", clientObj)
            })
        }
    }

    /**
     * Search YouTube Music for tracks, albums, or artists with resilient JSON traversal.
     */
    suspend fun search(query: String): Result<List<StreamTrack>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())

        try {
            val client = PlayerClient.ANDROID_MUSIC
            val payload = createBaseClientJson(client).apply {
                put("query", query)
                put("params", "Eg-KAQwIARAAGAAgACgAMABqChAGEAkQChAMEAo%3D") // Filter by Songs
            }

            val vData = cachedVisitorData
            val reqBuilder = Request.Builder()
                .url("$YOUTUBE_MUSIC_BASE_URL/search")
                .header("User-Agent", client.userAgent)
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

            if (!vData.isNullOrBlank()) {
                reqBuilder.header("X-Goog-Visitor-Id", vData)
            }

            val response = okHttpClient.newCall(reqBuilder.build()).execute()
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
            val client = PlayerClient.ANDROID_MUSIC
            val payload = createBaseClientJson(client).apply {
                put("browseId", "FEmusic_charts")
            }

            val vData = cachedVisitorData
            val reqBuilder = Request.Builder()
                .url("$YOUTUBE_MUSIC_BASE_URL/browse")
                .header("User-Agent", client.userAgent)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

            if (!vData.isNullOrBlank()) {
                reqBuilder.header("X-Goog-Visitor-Id", vData)
            }

            val response = okHttpClient.newCall(reqBuilder.build()).execute()
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
     * Uses multi-client rotation (prioritizing ANDROID_VR which bypasses PO token requirements)
     * and performs pre-flight probing to ensure playback succeeds immediately.
     */
    suspend fun getStreamUrl(
        videoId: String,
        targetQuality: StreamQuality = StreamQuality.HIGH
    ): Result<ResolvedStreamInfo> = withContext(Dispatchers.IO) {
        // Ensure visitor data is populated
        if (cachedVisitorData == null) {
            getOrFetchVisitorData(forceRefresh = false)
        }

        var lastException: Exception? = null

        // Walk candidate clients (ANDROID_VR, ANDROID_VR_LEGACY, TVHTML5, ANDROID_MUSIC, IOS)
        for (client in PlayerClient.RESOLUTION_CLIENTS) {
            try {
                val payload = createBaseClientJson(client).apply {
                    put("videoId", videoId)
                    put("contentCheckOk", true)
                    put("racyCheckOk", true)
                }

                val baseUrl = if (client.usesMusicHost) YOUTUBE_MUSIC_BASE_URL else YOUTUBE_BASE_URL
                val reqBuilder = Request.Builder()
                    .url("$baseUrl/player")
                    .header("User-Agent", client.userAgent)
                    .header("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

                client.origin?.let { reqBuilder.header("Origin", it) }
                client.referer?.let { reqBuilder.header("Referer", it) }
                cachedVisitorData?.let { reqBuilder.header("X-Goog-Visitor-Id", it) }

                val response = okHttpClient.newCall(reqBuilder.build()).execute()
                if (!response.isSuccessful) {
                    continue
                }

                val responseBody = response.body?.string().orEmpty()
                val json = JSONObject(responseBody)

                val streamingData = json.optJSONObject("streamingData") ?: continue
                val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
                    ?: streamingData.optJSONArray("formats") ?: continue

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

                val pickedUrl = bestUrl ?: continue

                // Probe the stream URL to ensure it delivers bytes without 403
                val probeOk = probeStream(pickedUrl, client)
                if (!probeOk) {
                    continue
                }

                // Parse expiry timestamp from URL query param `expire`
                val expirySeconds = Uri.parse(pickedUrl).getQueryParameter("expire")?.toLongOrNull()
                val expiryEpochMs = if (expirySeconds != null) {
                    expirySeconds * 1000L
                } else {
                    System.currentTimeMillis() + (6 * 3600 * 1000L)
                }

                return@withContext Result.success(
                    ResolvedStreamInfo(
                        streamUrl = pickedUrl,
                        mimeType = bestMime,
                        bitrateKbps = if (bestBitrate > 0) bestBitrate / 1000 else 160,
                        sampleRate = sampleRate,
                        expiryEpochMs = expiryEpochMs,
                        mediaHeaders = client.mediaHeaders()
                    )
                )
            } catch (e: Exception) {
                lastException = e
            }
        }

        Result.failure(lastException ?: IOException("Could not resolve playable audio stream for videoId '$videoId' across all player clients"))
    }

    /**
     * Probes the first chunk of a stream URL using the required client media headers.
     * Prevents handing dead or 403-forbidden URLs to ExoPlayer.
     */
    private fun probeStream(url: String, client: PlayerClient): Boolean {
        return try {
            val reqBuilder = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-1024")
                .header("User-Agent", client.userAgent)

            client.origin?.let { reqBuilder.header("Origin", it) }
            client.referer?.let { reqBuilder.header("Referer", it) }

            val probeClient = okHttpClient.newBuilder()
                .callTimeout(6, TimeUnit.SECONDS)
                .build()

            val response = probeClient.newCall(reqBuilder.build()).execute()
            val isSuccess = response.code in 200..299 || response.code == 416
            val isAudioOrStream = response.header("Content-Type")?.let {
                it.startsWith("audio/") || it.contains("octet-stream")
            } ?: true

            response.close()
            isSuccess && isAudioOrStream
        } catch (_: Exception) {
            false
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
