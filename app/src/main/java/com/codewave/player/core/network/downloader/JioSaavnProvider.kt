package com.codewave.player.core.network.downloader

import android.util.Base64
import com.codewave.player.core.model.TargetAudioFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class JioSaavnStreamResult(
    val streamUrl: String,
    val bitrateKbps: Int,
    val sampleRate: Int = 44100,
    val format: TargetAudioFormat = TargetAudioFormat.OPUS, // MP4/AAC container
    val is320k: Boolean = false
)

/**
 * JioSaavn High-Bitrate (320kbps MP4/AAC) audio stream extractor.
 * Ported from BitChord's JioSaavnService with DES ECB hardware decryption (key: 38346591).
 */
class JioSaavnProvider(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val BASE_URL = "https://www.jiosaavn.com/api.php"
        private const val DES_KEY = "38346591"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
    }

    /**
     * Searches JioSaavn for a matching song and decrypts its 320kbps/160kbps MP4 CDN URL.
     */
    suspend fun resolveStream(title: String, artist: String): Result<JioSaavnStreamResult> = withContext(Dispatchers.IO) {
        try {
            val query = "$title $artist".trim()
            val searchUrl = "$BASE_URL?__call=autocomplete.get&query=${URLEncoder.encode(query, "UTF-8")}&_format=json&_marker=0&ctx=android"

            val searchReq = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            val searchRes = okHttpClient.newCall(searchReq).execute()
            if (!searchRes.isSuccessful) {
                return@withContext Result.failure(Exception("JioSaavn search HTTP ${searchRes.code}"))
            }

            val searchJson = JSONObject(searchRes.body?.string().orEmpty())
            val songsArray = searchJson.optJSONObject("songs")?.optJSONArray("data")
            if (songsArray == null || songsArray.length() == 0) {
                return@withContext Result.failure(NoSuchElementException("No songs found on JioSaavn for '$query'"))
            }

            val firstSong = songsArray.getJSONObject(0)
            val songId = firstSong.optString("id")
            if (songId.isBlank()) {
                return@withContext Result.failure(NoSuchElementException("Missing song ID from JioSaavn result"))
            }

            // Fetch details to get encrypted media URL
            val detailsUrl = "$BASE_URL?__call=song.getDetails&pids=$songId&_format=json&_marker=0&ctx=android"
            val detailsReq = Request.Builder()
                .url(detailsUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            val detailsRes = okHttpClient.newCall(detailsReq).execute()
            if (!detailsRes.isSuccessful) {
                return@withContext Result.failure(Exception("JioSaavn details HTTP ${detailsRes.code}"))
            }

            val detailsJson = JSONObject(detailsRes.body?.string().orEmpty())
            val songDetails = detailsJson.optJSONObject(songId)
                ?: return@withContext Result.failure(NoSuchElementException("Missing song details from JioSaavn for ID $songId"))

            val moreInfo = songDetails.optJSONObject("more_info")
                ?: return@withContext Result.failure(NoSuchElementException("Missing more_info from JioSaavn for ID $songId"))

            val encryptedUrl = moreInfo.optString("encrypted_media_url")
            if (encryptedUrl.isBlank()) {
                return@withContext Result.failure(NoSuchElementException("No encrypted_media_url in JioSaavn response"))
            }

            val decryptedUrl = decryptUrl(encryptedUrl)
            if (decryptedUrl.isBlank()) {
                return@withContext Result.failure(Exception("Failed to decrypt JioSaavn media URL"))
            }

            val supports320 = moreInfo.optString("320kbps").equals("true", ignoreCase = true)
            val finalUrl = if (supports320) {
                decryptedUrl.replace(".mp4", "_320.mp4")
            } else {
                decryptedUrl
            }

            val bitrate = if (supports320) 320 else 160

            Result.success(
                JioSaavnStreamResult(
                    streamUrl = finalUrl,
                    bitrateKbps = bitrate,
                    sampleRate = 44100,
                    format = TargetAudioFormat.OPUS, // progressive audio stream
                    is320k = supports320
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decryptUrl(encryptedUrl: String): String {
        return try {
            val secretKey = SecretKeySpec(DES_KEY.toByteArray(Charsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = try {
                java.util.Base64.getDecoder().decode(encryptedUrl.trim())
            } catch (_: Throwable) {
                android.util.Base64.decode(encryptedUrl.trim(), android.util.Base64.DEFAULT)
            }
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8).trim()
        } catch (_: Exception) {
            ""
        }
    }
}
