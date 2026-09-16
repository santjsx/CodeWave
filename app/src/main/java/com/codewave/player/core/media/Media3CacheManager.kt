package com.codewave.player.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
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
     * when available, or streams over HTTP while caching chunks in background.
     */
    fun createCacheDataSourceFactory(context: Context): DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("CodeWave/1.4.0 (Android; Hi-Res Audio Workstation)")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val cache = getCache(context)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
