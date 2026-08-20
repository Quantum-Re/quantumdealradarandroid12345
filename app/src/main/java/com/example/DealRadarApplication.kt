package com.example

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.google.firebase.FirebaseApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * DealRadarApplication configures the global Application context, including
 * a high-performance, memory-leak-safe Coil ImageLoader with LRU memory caching,
 * disk caching, and hardware bitmap optimization for smooth scrolling on lower-end devices.
 */
class DealRadarApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Throwable) {
            Log.w("DealRadarApplication", "FirebaseApp initialization: ${e.message}")
        }
    }

    override fun newImageLoader(): ImageLoader {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = activityManager?.isLowRamDevice ?: false

        // Allocate 15% of available RAM for memory cache on low-RAM devices, 25% on standard devices
        val memoryCachePercent = if (isLowRam) 0.15 else 0.25

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(memoryCachePercent)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("deal_radar_image_cache"))
                    .maxSizeBytes(64L * 1024L * 1024L) // 64 MB disk cache limit
                    .build()
            }
            .respectCacheHeaders(false) // Cache images aggressively to prevent UI stutters
            .crossfade(true)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isLowRam)
            .allowRgb565(isLowRam) // Use 16-bit RGB_565 to save 50% memory per bitmap on lower-end hardware
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
