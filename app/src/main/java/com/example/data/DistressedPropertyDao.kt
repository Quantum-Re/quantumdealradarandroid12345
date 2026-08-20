package com.example.data

import android.util.Log
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

@Dao
abstract class DistressedPropertyDao {
    @Query("SELECT * FROM distressed_properties ORDER BY lastUpdated DESC")
    abstract fun getAllDistressedPropertiesRaw(): Flow<List<DistressedProperty>>

    @Query("SELECT * FROM distressed_properties ORDER BY lastUpdated DESC LIMIT :limit OFFSET :offset")
    abstract fun getDistressedPropertiesPagedRaw(limit: Int, offset: Int): Flow<List<DistressedProperty>>

    @Query("SELECT * FROM distressed_properties ORDER BY lastUpdated DESC LIMIT :limit OFFSET :offset")
    abstract suspend fun getDistressedPropertiesPagedList(limit: Int, offset: Int): List<DistressedProperty>

    @Query("SELECT COUNT(*) FROM distressed_properties")
    abstract fun getDistressedPropertiesCountRaw(): Flow<Int>

    @Query("SELECT * FROM distressed_properties ORDER BY lastUpdated DESC")
    abstract suspend fun getDistressedPropertiesList(): List<DistressedProperty>

    fun getAllDistressedProperties(): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyDao", "getAllDistressedProperties() query initiated")
        return getAllDistressedPropertiesRaw().onEach { list ->
            Log.d("DistressedPropertyDao", "getAllDistressedProperties() emitted ${list.size} items from Room database")
        }
    }

    fun getDistressedPropertiesPaged(limit: Int, offset: Int): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyDao", "getDistressedPropertiesPaged(limit=$limit, offset=$offset) query initiated")
        return getDistressedPropertiesPagedRaw(limit, offset).onEach { list ->
            Log.d("DistressedPropertyDao", "getDistressedPropertiesPaged(limit=$limit, offset=$offset) emitted ${list.size} items")
        }
    }

    fun getDistressedPropertiesCount(): Flow<Int> {
        return getDistressedPropertiesCountRaw()
    }

    @Query("SELECT * FROM distressed_properties WHERE id = :id")
    abstract suspend fun getDistressedPropertyByIdRaw(id: Long): DistressedProperty?

    suspend fun getDistressedPropertyById(id: Long): DistressedProperty? {
        Log.d("DistressedPropertyDao", "getDistressedPropertyById(id=$id) executed")
        val result = getDistressedPropertyByIdRaw(id)
        Log.d("DistressedPropertyDao", "getDistressedPropertyById(id=$id) returned: ${result?.address ?: "null"}")
        return result
    }

    @Query("SELECT * FROM distressed_properties WHERE distressLevel = :level ORDER BY lastUpdated DESC")
    abstract fun getDistressedPropertiesByLevelRaw(level: String): Flow<List<DistressedProperty>>

    fun getDistressedPropertiesByLevel(level: String): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyDao", "getDistressedPropertiesByLevel(level=$level) query initiated")
        return getDistressedPropertiesByLevelRaw(level).onEach { list ->
            Log.d("DistressedPropertyDao", "getDistressedPropertiesByLevel(level=$level) emitted ${list.size} items")
        }
    }

    @Query("SELECT * FROM distressed_properties WHERE status = :status ORDER BY lastUpdated DESC")
    abstract fun getDistressedPropertiesByStatusRaw(status: String): Flow<List<DistressedProperty>>

    fun getDistressedPropertiesByStatus(status: String): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyDao", "getDistressedPropertiesByStatus(status=$status) query initiated")
        return getDistressedPropertiesByStatusRaw(status).onEach { list ->
            Log.d("DistressedPropertyDao", "getDistressedPropertiesByStatus(status=$status) emitted ${list.size} items")
        }
    }

    @Query("""
        SELECT * FROM distressed_properties 
        WHERE (:level = 'ALL' OR distressLevel LIKE '%' || :level || '%')
          AND (:minPrice IS NULL OR price >= :minPrice)
          AND (:maxPrice IS NULL OR price <= :maxPrice)
        ORDER BY lastUpdated DESC
    """)
    abstract fun getFilteredDistressedPropertiesRaw(
        level: String,
        minPrice: Double?,
        maxPrice: Double?
    ): Flow<List<DistressedProperty>>

    @Query("""
        SELECT * FROM distressed_properties 
        WHERE (:level = 'ALL' OR distressLevel LIKE '%' || :level || '%')
          AND (:minPrice IS NULL OR price >= :minPrice)
          AND (:maxPrice IS NULL OR price <= :maxPrice)
        ORDER BY lastUpdated DESC
        LIMIT :limit OFFSET :offset
    """)
    abstract fun getFilteredDistressedPropertiesPagedRaw(
        level: String,
        minPrice: Double?,
        maxPrice: Double?,
        limit: Int,
        offset: Int
    ): Flow<List<DistressedProperty>>

    fun getFilteredDistressedProperties(
        level: String,
        minPrice: Double?,
        maxPrice: Double?
    ): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyDao", "getFilteredDistressedProperties(level=$level, minPrice=$minPrice, maxPrice=$maxPrice) initiated")
        return getFilteredDistressedPropertiesRaw(level, minPrice, maxPrice).onEach { list ->
            Log.d("DistressedPropertyDao", "getFilteredDistressedProperties emitted ${list.size} items from Room")
        }
    }

    fun getFilteredDistressedPropertiesPaged(
        level: String,
        minPrice: Double?,
        maxPrice: Double?,
        limit: Int,
        offset: Int
    ): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyDao", "getFilteredDistressedPropertiesPaged(level=$level, minPrice=$minPrice, maxPrice=$maxPrice, limit=$limit, offset=$offset) initiated")
        return getFilteredDistressedPropertiesPagedRaw(level, minPrice, maxPrice, limit, offset).onEach { list ->
            Log.d("DistressedPropertyDao", "getFilteredDistressedPropertiesPaged emitted ${list.size} items from Room")
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDistressedPropertyRaw(distressedProperty: DistressedProperty): Long

    suspend fun insertDistressedProperty(distressedProperty: DistressedProperty): Long {
        Log.d("DistressedPropertyDao", "insertDistressedProperty called for address=${distressedProperty.address}")
        val id = insertDistressedPropertyRaw(distressedProperty)
        Log.d("DistressedPropertyDao", "insertDistressedProperty success with id=$id")
        return id
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDistressedPropertiesRaw(distressedProperties: List<DistressedProperty>)

    suspend fun insertDistressedProperties(distressedProperties: List<DistressedProperty>) {
        Log.d("DistressedPropertyDao", "insertDistressedProperties called with ${distressedProperties.size} items")
        insertDistressedPropertiesRaw(distressedProperties)
        Log.d("DistressedPropertyDao", "insertDistressedProperties inserted ${distressedProperties.size} items into Room")
    }

    @Update
    abstract suspend fun updateDistressedPropertyRaw(distressedProperty: DistressedProperty)

    suspend fun updateDistressedProperty(distressedProperty: DistressedProperty) {
        Log.d("DistressedPropertyDao", "updateDistressedProperty called for id=${distressedProperty.id}")
        updateDistressedPropertyRaw(distressedProperty)
    }

    @Delete
    abstract suspend fun deleteDistressedPropertyRaw(distressedProperty: DistressedProperty)

    suspend fun deleteDistressedProperty(distressedProperty: DistressedProperty) {
        Log.d("DistressedPropertyDao", "deleteDistressedProperty called for id=${distressedProperty.id}")
        deleteDistressedPropertyRaw(distressedProperty)
    }

    @Query("DELETE FROM distressed_properties")
    abstract suspend fun clearAllRaw()

    suspend fun clearAll() {
        Log.d("DistressedPropertyDao", "clearAll() executed on distressed_properties table")
        clearAllRaw()
    }
}

