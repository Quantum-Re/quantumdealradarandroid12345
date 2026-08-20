package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a cached base map tile for offline Google Maps access
 * during site visits and property inspections.
 */
@Entity(tableName = "map_tile_cache")
data class MapTileCache(
    @PrimaryKey val tileKey: String, // Format: "$mapType-$zoom-$x-$y"
    val mapType: String = "NORMAL",  // "NORMAL", "SATELLITE", "TERRAIN"
    val zoom: Int,
    val x: Int,
    val y: Int,
    val tileData: ByteArray,          // PNG or JPEG binary blob
    val sizeBytes: Long,
    val cachedAt: Long = System.currentTimeMillis(),
    val propertyId: Long? = null,     // Optional associated Property ID
    val regionName: String = "Generale" // Region descriptor e.g. "Roma / Sopralluogo"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapTileCache

        if (tileKey != other.tileKey) return false
        if (!tileData.contentEquals(other.tileData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tileKey.hashCode()
        result = 31 * result + tileData.contentHashCode()
        return result
    }
}

/**
 * Room Entity tracking saved offline map regions downloaded for property visits.
 */
@Entity(tableName = "map_offline_regions")
data class MapOfflineRegion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val regionName: String,
    val propertyId: Long? = null,
    val centerLat: Double,
    val centerLng: Double,
    val radiusKm: Double = 2.0,
    val minZoom: Int = 12,
    val maxZoom: Int = 16,
    val tileCount: Int = 0,
    val totalSizeBytes: Long = 0,
    val downloadedAt: Long = System.currentTimeMillis(),
    val status: String = "CACHED" // "CACHED", "DOWNLOADING", "EXPIRED"
)
