package com.example.data

import android.util.Log
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

@Dao
abstract class PropertyDao {
    @Query("SELECT * FROM properties ORDER BY createdAt DESC")
    abstract fun getAllPropertiesRaw(): Flow<List<Property>>

    @Query("SELECT * FROM properties ORDER BY createdAt DESC")
    abstract suspend fun getAllPropertiesList(): List<Property>

    @Query("SELECT * FROM properties ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    abstract fun getPropertiesPagedRaw(limit: Int, offset: Int): Flow<List<Property>>

    @Query("SELECT * FROM properties ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    abstract suspend fun getPropertiesPagedList(limit: Int, offset: Int): List<Property>

    @Query("SELECT COUNT(*) FROM properties")
    abstract fun getPropertiesCountRaw(): Flow<Int>

    fun getAllProperties(): Flow<List<Property>> {
        Log.d("PropertyDao", "getAllProperties() query initiated")
        return getAllPropertiesRaw().onEach { list ->
            Log.d("PropertyDao", "getAllProperties() emitted ${list.size} items from Room database")
        }
    }

    fun getPropertiesPaged(limit: Int, offset: Int): Flow<List<Property>> {
        Log.d("PropertyDao", "getPropertiesPaged(limit=$limit, offset=$offset) query initiated")
        return getPropertiesPagedRaw(limit, offset).onEach { list ->
            Log.d("PropertyDao", "getPropertiesPaged(limit=$limit, offset=$offset) emitted ${list.size} items")
        }
    }

    fun getPropertiesCount(): Flow<Int> {
        return getPropertiesCountRaw()
    }

    @Query("SELECT * FROM properties WHERE id = :id")
    abstract suspend fun getPropertyByIdRaw(id: Long): Property?

    suspend fun getPropertyById(id: Long): Property? {
        Log.d("PropertyDao", "getPropertyById(id=$id) executed")
        val result = getPropertyByIdRaw(id)
        Log.d("PropertyDao", "getPropertyById(id=$id) returned: ${result?.title ?: "null"}")
        return result
    }

    @Query("SELECT * FROM properties WHERE distressStatus = :status ORDER BY createdAt DESC")
    abstract fun getPropertiesByDistressStatusRaw(status: String): Flow<List<Property>>

    @Query("SELECT * FROM properties WHERE distressStatus = :status ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    abstract fun getPropertiesByDistressStatusPagedRaw(status: String, limit: Int, offset: Int): Flow<List<Property>>

    fun getPropertiesByDistressStatus(status: String): Flow<List<Property>> {
        Log.d("PropertyDao", "getPropertiesByDistressStatus(status=$status) query initiated")
        return getPropertiesByDistressStatusRaw(status).onEach { list ->
            Log.d("PropertyDao", "getPropertiesByDistressStatus(status=$status) emitted ${list.size} items")
        }
    }

    fun getPropertiesByDistressStatusPaged(status: String, limit: Int, offset: Int): Flow<List<Property>> {
        Log.d("PropertyDao", "getPropertiesByDistressStatusPaged(status=$status, limit=$limit, offset=$offset) query initiated")
        return getPropertiesByDistressStatusPagedRaw(status, limit, offset).onEach { list ->
            Log.d("PropertyDao", "getPropertiesByDistressStatusPaged(status=$status, limit=$limit, offset=$offset) emitted ${list.size} items")
        }
    }

    @Query("SELECT * FROM properties WHERE pipelineStatus = :pipelineStatus ORDER BY createdAt DESC")
    abstract fun getPropertiesByPipelineStatusRaw(pipelineStatus: String): Flow<List<Property>>

    @Query("SELECT * FROM properties WHERE pipelineStatus = :pipelineStatus ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    abstract fun getPropertiesByPipelineStatusPagedRaw(pipelineStatus: String, limit: Int, offset: Int): Flow<List<Property>>

    fun getPropertiesByPipelineStatus(pipelineStatus: String): Flow<List<Property>> {
        Log.d("PropertyDao", "getPropertiesByPipelineStatus(pipelineStatus=$pipelineStatus) query initiated")
        return getPropertiesByPipelineStatusRaw(pipelineStatus).onEach { list ->
            Log.d("PropertyDao", "getPropertiesByPipelineStatus(pipelineStatus=$pipelineStatus) emitted ${list.size} items")
        }
    }

    fun getPropertiesByPipelineStatusPaged(pipelineStatus: String, limit: Int, offset: Int): Flow<List<Property>> {
        Log.d("PropertyDao", "getPropertiesByPipelineStatusPaged(pipelineStatus=$pipelineStatus, limit=$limit, offset=$offset) query initiated")
        return getPropertiesByPipelineStatusPagedRaw(pipelineStatus, limit, offset).onEach { list ->
            Log.d("PropertyDao", "getPropertiesByPipelineStatusPaged(pipelineStatus=$pipelineStatus, limit=$limit, offset=$offset) emitted ${list.size} items")
        }
    }

    @Query("UPDATE properties SET pipelineStatus = :newStatus WHERE id = :id")
    abstract suspend fun updatePipelineStatusRaw(id: Long, newStatus: String)

    suspend fun updatePipelineStatus(id: Long, newStatus: String) {
        Log.d("PropertyDao", "updatePipelineStatus(id=$id, newStatus=$newStatus) called")
        updatePipelineStatusRaw(id, newStatus)
    }

    @Query("UPDATE properties SET renovationProgressPercent = :progress, actualRenovationCost = :actualCost WHERE id = :id")
    abstract suspend fun updateRenovationProgressRaw(id: Long, progress: Int, actualCost: Double)

    suspend fun updateRenovationProgress(id: Long, progress: Int, actualCost: Double) {
        Log.d("PropertyDao", "updateRenovationProgress(id=$id, progress=$progress%, cost=$actualCost) called")
        updateRenovationProgressRaw(id, progress, actualCost)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPropertyRaw(property: Property): Long

    suspend fun insertProperty(property: Property): Long {
        Log.d("PropertyDao", "insertProperty called for title=${property.title}")
        val id = insertPropertyRaw(property)
        Log.d("PropertyDao", "insertProperty success with id=$id")
        return id
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPropertiesRaw(properties: List<Property>)

    suspend fun insertProperties(properties: List<Property>) {
        Log.d("PropertyDao", "insertProperties called with ${properties.size} items")
        insertPropertiesRaw(properties)
        Log.d("PropertyDao", "insertProperties inserted ${properties.size} items into Room")
    }

    @Update
    abstract suspend fun updatePropertyRaw(property: Property)

    suspend fun updateProperty(property: Property) {
        Log.d("PropertyDao", "updateProperty called for id=${property.id}")
        updatePropertyRaw(property)
    }

    @Delete
    abstract suspend fun deletePropertyRaw(property: Property)

    suspend fun deleteProperty(property: Property) {
        Log.d("PropertyDao", "deleteProperty called for id=${property.id}")
        deletePropertyRaw(property)
    }

    @Query("DELETE FROM properties WHERE id IN (:ids)")
    abstract suspend fun deletePropertiesByIdsRaw(ids: List<Long>): Int

    suspend fun deletePropertiesByIds(ids: List<Long>): Int {
        Log.d("PropertyDao", "deletePropertiesByIds called with ${ids.size} IDs: $ids")
        val count = deletePropertiesByIdsRaw(ids)
        Log.d("PropertyDao", "deletePropertiesByIds deleted $count rows")
        return count
    }

    @Query("UPDATE properties SET pipelineStatus = :newStatus WHERE id IN (:ids)")
    abstract suspend fun updatePipelineStatusForMultipleRaw(ids: List<Long>, newStatus: String): Int

    suspend fun updatePipelineStatusForMultiple(ids: List<Long>, newStatus: String): Int {
        Log.d("PropertyDao", "updatePipelineStatusForMultiple called for ${ids.size} IDs with newStatus=$newStatus")
        val count = updatePipelineStatusForMultipleRaw(ids, newStatus)
        Log.d("PropertyDao", "updatePipelineStatusForMultiple updated $count rows")
        return count
    }

    @Query("DELETE FROM properties")
    abstract suspend fun clearAllRaw()

    suspend fun clearAll() {
        Log.d("PropertyDao", "clearAll() executed on properties table")
        clearAllRaw()
    }
}

