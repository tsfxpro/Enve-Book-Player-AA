package com.enve.app.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
internal object PlaybackStreamCache {
    private const val DIRECTORY = "playback-stream"
    private const val MAX_BYTES = 256L * 1024L * 1024L
    private val caches = mutableMapOf<File, SimpleCache>()

    @Synchronized
    fun forProfileCache(context: Context, cacheDirectory: File): Cache {
        val directory = File(cacheDirectory, DIRECTORY)
        return caches.getOrPut(directory) {
            SimpleCache(
                directory,
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                StandaloneDatabaseProvider(context.applicationContext),
            )
        }
    }
}
