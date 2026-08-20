package com.example.data

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class DistressedPropertyRepository(
    private val distressedPropertyDao: DistressedPropertyDao,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val allDistressedProperties: StateFlow<List<DistressedProperty>> = 
        distressedPropertyDao.getAllDistressedProperties()
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
            .onEach { list ->
                Log.d("DistressedPropertyRepository", "Centralized allDistressedProperties emitted ${list.size} distressed properties")
            }
            .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getDistressedPropertiesPaged(limit: Int, offset: Int): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyRepository", "getDistressedPropertiesPaged(limit=$limit, offset=$offset) called")
        return distressedPropertyDao.getDistressedPropertiesPaged(limit, offset)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
            .onEach { list ->
                Log.d("DistressedPropertyRepository", "getDistressedPropertiesPaged(limit=$limit, offset=$offset) emitted ${list.size} items")
            }
    }

    suspend fun getDistressedPropertiesPagedList(limit: Int, offset: Int): List<DistressedProperty> = withContext(ioDispatcher) {
        distressedPropertyDao.getDistressedPropertiesPagedList(limit, offset)
    }

    fun getDistressedPropertiesCount(): Flow<Int> {
        return distressedPropertyDao.getDistressedPropertiesCount()
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    fun getDistressedPropertiesByLevel(level: String): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyRepository", "getDistressedPropertiesByLevel(level=$level) called")
        return distressedPropertyDao.getDistressedPropertiesByLevel(level)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
            .onEach { list ->
                Log.d("DistressedPropertyRepository", "getDistressedPropertiesByLevel(level=$level) flow emitted ${list.size} items")
            }
    }

    fun getDistressedPropertiesByStatus(status: String): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyRepository", "getDistressedPropertiesByStatus(status=$status) called")
        return distressedPropertyDao.getDistressedPropertiesByStatus(status)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
            .onEach { list ->
                Log.d("DistressedPropertyRepository", "getDistressedPropertiesByStatus(status=$status) flow emitted ${list.size} items")
            }
    }

    fun getFilteredDistressedProperties(level: String, minPrice: Double?, maxPrice: Double?): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyRepository", "getFilteredDistressedProperties(level=$level, minPrice=$minPrice, maxPrice=$maxPrice) called")
        return distressedPropertyDao.getFilteredDistressedProperties(level, minPrice, maxPrice)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    fun getFilteredDistressedPropertiesPaged(level: String, minPrice: Double?, maxPrice: Double?, limit: Int, offset: Int): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyRepository", "getFilteredDistressedPropertiesPaged(level=$level, minPrice=$minPrice, maxPrice=$maxPrice, limit=$limit, offset=$offset) called")
        return distressedPropertyDao.getFilteredDistressedPropertiesPaged(level, minPrice, maxPrice, limit, offset)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    suspend fun getDistressedPropertyById(id: Long): DistressedProperty? = withContext(ioDispatcher) {
        Log.d("DistressedPropertyRepository", "getDistressedPropertyById(id=$id) called")
        val result = distressedPropertyDao.getDistressedPropertyById(id)
        Log.d("DistressedPropertyRepository", "getDistressedPropertyById(id=$id) result: ${result?.address ?: "null"}")
        result
    }

    suspend fun insertDistressedProperty(distressedProperty: DistressedProperty): Long = withContext(ioDispatcher) {
        Log.d("DistressedPropertyRepository", "insertDistressedProperty called for address=${distressedProperty.address}")
        val id = distressedPropertyDao.insertDistressedProperty(distressedProperty)
        Log.d("DistressedPropertyRepository", "insertDistressedProperty succeeded with id=$id")
        id
    }

    suspend fun insertDistressedProperties(distressedProperties: List<DistressedProperty>) = withContext(ioDispatcher) {
        Log.d("DistressedPropertyRepository", "insertDistressedProperties called with ${distressedProperties.size} items")
        distressedPropertyDao.insertDistressedProperties(distressedProperties)
        Log.d("DistressedPropertyRepository", "insertDistressedProperties inserted ${distressedProperties.size} items")
    }

    suspend fun updateDistressedProperty(distressedProperty: DistressedProperty) = withContext(ioDispatcher) {
        Log.d("DistressedPropertyRepository", "updateDistressedProperty called for id=${distressedProperty.id}")
        distressedPropertyDao.updateDistressedProperty(distressedProperty)
    }

    suspend fun deleteDistressedProperty(distressedProperty: DistressedProperty) = withContext(ioDispatcher) {
        Log.d("DistressedPropertyRepository", "deleteDistressedProperty called for id=${distressedProperty.id}")
        distressedPropertyDao.deleteDistressedProperty(distressedProperty)
    }

    suspend fun clearAll() = withContext(ioDispatcher) {
        Log.d("DistressedPropertyRepository", "clearAll() called")
        distressedPropertyDao.clearAll()
    }
}

