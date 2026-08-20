package com.example.data

import android.content.Context
import com.example.data.MapOfflineRegion
import com.example.data.MapTileCache
import com.example.data.MapTileCacheDao
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.PI
import kotlin.math.tan

/**
 * Repository and TileProvider manager for caching Google Maps / OpenStreetMap base tiles into Room database.
 * Used for offline site visits and property due diligence when internet connectivity is limited.
 */
class OfflineMapCacheRepository(private val mapTileCacheDao: MapTileCacheDao) {

    val cachedTileCount: Flow<Int> = mapTileCacheDao.getCachedTileCount()

    val totalCacheSizeBytes: Flow<Long> = mapTileCacheDao.getTotalCacheSizeBytes().map { it ?: 0L }

    val offlineRegions: Flow<List<MapOfflineRegion>> = mapTileCacheDao.getAllRegions()

    /**
     * Retrieves cached tile bytes from Room or fetches from network tile server and caches in Room.
     */
    suspend fun getOrFetchTile(
        mapType: String = "NORMAL",
        zoom: Int,
        x: Int,
        y: Int,
        propertyId: Long? = null,
        regionName: String = "Generale"
    ): ByteArray? = withContext(Dispatchers.IO) {
        val tileKey = "$mapType-$zoom-$x-$y"

        // 1. Check Room Database Cache first
        val cached = mapTileCacheDao.getTile(tileKey)
        if (cached != null && cached.tileData.isNotEmpty()) {
            return@withContext cached.tileData
        }

        // 2. Fetch from Tile Server (e.g. OpenStreetMap / OpenTopoMap or Google Map Matrix)
        val tileBytes = fetchTileFromNetwork(mapType, zoom, x, y)
        if (tileBytes != null && tileBytes.isNotEmpty()) {
            val newCacheEntry = MapTileCache(
                tileKey = tileKey,
                mapType = mapType,
                zoom = zoom,
                x = x,
                y = y,
                tileData = tileBytes,
                sizeBytes = tileBytes.size.toLong(),
                cachedAt = System.currentTimeMillis(),
                propertyId = propertyId,
                regionName = regionName
            )
            mapTileCacheDao.insertTile(newCacheEntry)
            return@withContext tileBytes
        }

        return@withContext null
    }

    /**
     * Downloads and caches all map tiles for a specified geographic region (LatLng + Radius)
     * into Room database for offline site visits.
     */
    suspend fun downloadRegionForOfflineVisit(
        regionName: String,
        propertyId: Long? = null,
        centerLat: Double,
        centerLng: Double,
        radiusKm: Double = 1.0,
        minZoom: Int = 13,
        maxZoom: Int = 16,
        onProgress: (downloaded: Int, total: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val tilesToFetch = mutableListOf<Triple<Int, Int, Int>>() // zoom, x, y

        for (z in minZoom..maxZoom) {
            val centerTileX = latLngToTileX(centerLng, z)
            val centerTileY = latLngToTileY(centerLat, z)

            // Estimate tile radius based on zoom level
            val deltaTiles = when (z) {
                13 -> 1
                14 -> 2
                15 -> 3
                16 -> 4
                else -> 1
            }

            for (dx in -deltaTiles..deltaTiles) {
                for (dy in -deltaTiles..deltaTiles) {
                    val tileX = centerTileX + dx
                    val tileY = centerTileY + dy
                    val maxTileIndex = (1 shl z) - 1
                    if (tileX in 0..maxTileIndex && tileY in 0..maxTileIndex) {
                        tilesToFetch.add(Triple(z, tileX, tileY))
                    }
                }
            }
        }

        val total = tilesToFetch.size
        var downloadedCount = 0
        var totalBytesDownloaded = 0L

        onProgress(0, total)

        tilesToFetch.forEach { (z, x, y) ->
            val bytes = getOrFetchTile("NORMAL", z, x, y, propertyId, regionName)
            if (bytes != null) {
                totalBytesDownloaded += bytes.size
            }
            downloadedCount++
            onProgress(downloadedCount, total)
        }

        // Save region record in Room
        val region = MapOfflineRegion(
            regionName = regionName,
            propertyId = propertyId,
            centerLat = centerLat,
            centerLng = centerLng,
            radiusKm = radiusKm,
            minZoom = minZoom,
            maxZoom = maxZoom,
            tileCount = downloadedCount,
            totalSizeBytes = totalBytesDownloaded,
            status = "CACHED"
        )
        mapTileCacheDao.insertRegion(region)
    }

    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        mapTileCacheDao.clearAllCache()
    }

    suspend fun deleteRegion(id: Long) = withContext(Dispatchers.IO) {
        mapTileCacheDao.deleteRegion(id)
    }

    private fun fetchTileFromNetwork(mapType: String, zoom: Int, x: Int, y: Int): ByteArray? {
        val urlString = when (mapType) {
            "SATELLITE" -> "https://mt1.google.com/vt/lyrs=s&x=$x&y=$y&z=$zoom"
            "TERRAIN" -> "https://mt1.google.com/vt/lyrs=t&x=$x&y=$y&z=$zoom"
            else -> "https://tile.openstreetmap.org/$zoom/$x/$y.png"
        }

        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.setRequestProperty("User-Agent", "REInvestorApp/1.0 (Android)")
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { it.readBytes() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun latLngToTileX(lon: Double, zoom: Int): Int {
            return floor((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
        }

        fun latLngToTileY(lat: Double, zoom: Int): Int {
            val latRad = Math.toRadians(lat)
            return floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)).toInt()
        }
    }
}

/**
 * Custom TileProvider implementation for Google Maps overlay backed by Room DB Cache.
 */
class RoomDatabaseTileProvider(
    private val repository: OfflineMapCacheRepository,
    private val mapType: String = "NORMAL"
) : TileProvider {

    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        return runBlocking {
            val bytes = repository.getOrFetchTile(mapType, zoom, x, y)
            if (bytes != null && bytes.isNotEmpty()) {
                Tile(256, 256, bytes)
            } else {
                TileProvider.NO_TILE
            }
        }
    }
}
