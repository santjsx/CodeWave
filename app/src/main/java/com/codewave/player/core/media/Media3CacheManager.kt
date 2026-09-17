package com.codewave.player.core.media

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.codewave.player.core.network.innertube.PlayerClient
import java.io.File

@OptIn(UnstableApi::class)
object Media3CacheManager {

    private const val MAX_STREAM_CACHE_BYTES = 512L * 1024L * 1024L // 512 MB LRU Cache

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val cacheDir = File(context.cacheDir, "stream_cache").apply {
                    if (!exists()) mkdirs()
                }
                val evictor = LeastRecentlyUsedCacheEvictor(MAX_STREAM_CACHE_BYTES)
                val databaseProvider = StandaloneDatabaseProvider(context)
                SimpleCache(cacheDir, evictor, databaseProvider).also {
                    simpleCache = it
                }
            }
        }
    }

    /**
     * Builds a Media3 CacheDataSource factory that serves streamed audio from disk cache
     * when available, or streams over HTTP while dynamically configuring the required
     * PlayerClient headers for googlevideo CDN stream requests.
     */
    fun createCacheDataSourceFactory(context: Context): DataSource.Factory {
        val dynamicHttpFactory = DataSource.Factory {
            DynamicHttpDataSource()
        }

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, dynamicHttpFactory)
        val cache = getCache(context)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Custom DataSource that inspects the destination URI.
     * If the target is YouTube/googlevideo, it dresses the request with the exact
     * PlayerClient User-Agent, Origin, and Referer that minted the URL, preventing
     * HTTP 403 Forbidden errors and stream throttling.
     */
    private class DynamicHttpDataSource(
        private val connectTimeoutMs: Int = 15000,
        private val readTimeoutMs: Int = 20000
    ) : DataSource {

        private var activeHttpDataSource: HttpDataSource? = null
        private val listeners = mutableListOf<TransferListener>()

        override fun addTransferListener(transferListener: TransferListener) {
            listeners.add(transferListener)
            activeHttpDataSource?.addTransferListener(transferListener)
        }

        override fun open(dataSpec: DataSpec): Long {
            val url = dataSpec.uri.toString()
            val isGoogleVideo = url.contains("googlevideo.com") || url.contains("youtube.com")

            val factory = DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(connectTimeoutMs)
                .setReadTimeoutMs(readTimeoutMs)
                .setAllowCrossProtocolRedirects(true)

            if (isGoogleVideo) {
                val client = PlayerClient.forStreamUrl(url)
                factory.setUserAgent(client.userAgent)
                factory.setDefaultRequestProperties(client.mediaHeaders())
            } else {
                factory.setUserAgent("CodeWave/1.5.3 (Android; Hi-Res Audio Workstation)")
            }

            val ds = factory.createDataSource()
            for (listener in listeners) {
                ds.addTransferListener(listener)
            }
            activeHttpDataSource = ds
            return ds.open(dataSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            return activeHttpDataSource?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
        }

        override fun getUri(): Uri? = activeHttpDataSource?.uri

        override fun getResponseHeaders(): Map<String, List<String>> =
            activeHttpDataSource?.responseHeaders.orEmpty()

        override fun close() {
            activeHttpDataSource?.close()
            activeHttpDataSource = null
        }
    }
}
