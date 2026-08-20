package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MapTileCacheDao {

    @Query("SELECT * FROM map_tile_cache WHERE tileKey = :tileKey LIMIT 1")
    suspend fun getTile(tileKey: String): MapTileCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTile(tile: MapTileCache)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTiles(tiles: List<MapTileCache>)

    @Query("SELECT * FROM map_tile_cache ORDER BY cachedAt DESC")
    fun getAllTiles(): Flow<List<MapTileCache>>

    @Query("SELECT COUNT(*) FROM map_tile_cache")
    fun getCachedTileCount(): Flow<Int>

    @Query("SELECT SUM(sizeBytes) FROM map_tile_cache")
    fun getTotalCacheSizeBytes(): Flow<Long?>

    @Query("DELETE FROM map_tile_cache")
    suspend fun clearAllCache()

    @Query("DELETE FROM map_tile_cache WHERE regionName = :regionName")
    suspend fun deleteTilesForRegion(regionName: String)

    // Offline Regions Management
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegion(region: MapOfflineRegion): Long

    @Query("SELECT * FROM map_offline_regions ORDER BY downloadedAt DESC")
    fun getAllRegions(): Flow<List<MapOfflineRegion>>

    @Query("DELETE FROM map_offline_regions WHERE id = :id")
    suspend fun deleteRegion(id: Long)
}
