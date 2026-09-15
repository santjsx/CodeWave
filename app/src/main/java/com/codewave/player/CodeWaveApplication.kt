package com.codewave.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.codewave.player.core.data.AppContainer
import com.codewave.player.core.data.DefaultAppContainer

class CodeWaveApplication : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = DefaultAppContainer(this)
    }

    /**
     * Bounded memory and disk cache for Coil to safely support 100,000+ track libraries
     * without OutOfMemory crashes (PRD Section 53, 67, 106).
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("album_art_cache"))
                    .maxSizeBytes(128L * 1024 * 1024) // 128 MB disk cache cap
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }

    companion object {
        lateinit var instance: CodeWaveApplication
            private set
    }
}
