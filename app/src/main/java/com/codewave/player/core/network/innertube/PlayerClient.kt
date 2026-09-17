package com.codewave.player.core.network.innertube

import android.net.Uri
import java.util.Locale

/**
 * Encapsulates YouTube InnerTube player client identities and required media request headers.
 * Sourced from verified BitChord player client definitions to bypass Proof-of-Origin (PO)
 * token blocks and signature requirements.
 */
data class PlayerClient(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val userAgent: String,
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceMake: String? = null,
    val deviceModel: String? = null,
    val androidSdkVersion: Int? = null,
    val origin: String? = null,
    val needsSignatureTimestamp: Boolean = false
) {
    val referer: String? get() = origin?.let { "$it/" }
    val usesMusicHost: Boolean get() = origin == MUSIC_ORIGIN

    /**
     * Headers the media/chunk request MUST carry for a URL this client minted.
     * googlevideo.com treats a mismatch between client identity and media request
     * User-Agent as unauthorized, resulting in HTTP 403 Forbidden.
     */
    fun mediaHeaders(): Map<String, String> = buildMap {
        put("User-Agent", userAgent)
        origin?.let { put("Origin", it) }
        referer?.let { put("Referer", it) }
    }

    companion object {
        const val MUSIC_ORIGIN = "https://music.youtube.com"
        const val YOUTUBE_ORIGIN = "https://www.youtube.com"

        /**
         * Quest YouTube App client: Serves direct, unciphered Opus 160k and AAC HTTPS stream URLs
         * without requiring Proof of Origin (PO) tokens or login.
         */
        val ANDROID_VR = PlayerClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.65.10",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            osName = "Android",
            osVersion = "12L",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            androidSdkVersion = 32
        )

        /**
         * Legacy Quest YouTube App client fallback.
         */
        val ANDROID_VR_LEGACY = ANDROID_VR.copy(
            clientVersion = "1.43.32",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.43.32 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/107.0.5284.2)"
        )

        /**
         * TV Cobalt v7 client: Highly reliable endpoint for smart TV requests without PO token.
         */
        val TVHTML5 = PlayerClient(
            clientName = "TVHTML5",
            clientVersion = "7.20260707.07.00",
            clientId = "7",
            userAgent = "Mozilla/5.0(SMART-TV; Linux; Tizen 4.0.0.2) AppleWebkit/605.1.15 (KHTML, like Gecko) SamsungBrowser/9.2 TV Safari/605.1.15",
            origin = YOUTUBE_ORIGIN
        )

        /**
         * iOS YouTube App client.
         */
        val IOS = PlayerClient(
            clientName = "IOS",
            clientVersion = "21.26.4",
            clientId = "5",
            userAgent = "com.google.ios.youtube/21.26.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
            osName = "iPhone",
            osVersion = "18.3.2.22D82",
            deviceMake = "Apple",
            deviceModel = "iPhone16,2"
        )

        val IOS_RECENT = IOS.copy(
            clientVersion = "21.29.1",
            userAgent = "com.google.ios.youtube/21.29.1 (iPhone16,2; U; CPU iOS 18_5 like Mac OS X;)",
            osVersion = "18.5.22F70"
        )

        /**
         * YouTube Music Android app client (updated to 8.39.42).
         */
        val ANDROID_MUSIC = PlayerClient(
            clientName = "ANDROID_MUSIC",
            clientVersion = "8.39.42",
            clientId = "21",
            userAgent = "com.google.android.apps.youtube.music/8.39.42 (Linux; U; Android 15; en_US; Pixel 9 Pro; Build/AP4A.250205.002) gzip",
            osName = "Android",
            osVersion = "15",
            deviceMake = "Google",
            deviceModel = "Pixel 9 Pro",
            androidSdkVersion = 35
        )

        /**
         * Ordered candidate list for player resolution.
         * Prioritizes ANDROID_VR because it is the most reliable client serving unciphered HTTPS URLs.
         */
        val RESOLUTION_CLIENTS = listOf(
            ANDROID_VR,
            ANDROID_VR_LEGACY,
            TVHTML5,
            ANDROID_MUSIC,
            IOS
        )

        /**
         * Extracts the client identity that minted a googlevideo.com URL, or falls back to ANDROID_VR.
         */
        fun forStreamUrl(url: String): PlayerClient {
            val clientParam = Regex("[?&]c=([^&]+)").find(url)?.groupValues?.get(1)?.uppercase(Locale.ROOT)
                ?: return ANDROID_VR
            val versionParam = Regex("[?&]cver=([^&]+)").find(url)?.groupValues?.get(1)
            return when {
                clientParam == "ANDROID_VR" ->
                    if (versionParam == ANDROID_VR_LEGACY.clientVersion) ANDROID_VR_LEGACY else ANDROID_VR
                clientParam == "TVHTML5" -> TVHTML5
                clientParam.startsWith("IOS") ->
                    if (versionParam == IOS_RECENT.clientVersion) IOS_RECENT else IOS
                clientParam == "ANDROID_MUSIC" -> ANDROID_MUSIC
                else -> ANDROID_VR
            }
        }
    }
}
