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

class PropertyRepository(
    private val propertyDao: PropertyDao,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val allProperties: StateFlow<List<Property>> = propertyDao.getAllProperties()
        .distinctUntilChanged()
        .flowOn(ioDispatcher)
        .onEach { list ->
            Log.d("PropertyRepository", "Centralized allProperties emitted ${list.size} properties from database")
        }
        .stateIn(externalScope, SharingStarted.Eagerly, emptyList())

    suspend fun getAllPropertiesList(): List<Property> = withContext(ioDispatcher) {
        propertyDao.getAllPropertiesList()
    }

    fun getPropertiesPaged(limit: Int, offset: Int): Flow<List<Property>> {
        Log.d("PropertyRepository", "getPropertiesPaged(limit=$limit, offset=$offset) called")
        return propertyDao.getPropertiesPaged(limit, offset)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
            .onEach { list ->
                Log.d("PropertyRepository", "getPropertiesPaged(limit=$limit, offset=$offset) emitted ${list.size} items")
            }
    }

    suspend fun getPropertiesPagedList(limit: Int, offset: Int): List<Property> = withContext(ioDispatcher) {
        propertyDao.getPropertiesPagedList(limit, offset)
    }

    fun getPropertiesCount(): Flow<Int> {
        return propertyDao.getPropertiesCount()
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    fun getPropertiesByDistressStatus(status: String): Flow<List<Property>> {
        Log.d("PropertyRepository", "getPropertiesByDistressStatus(status=$status) called")
        return propertyDao.getPropertiesByDistressStatus(status)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
            .onEach { list ->
                Log.d("PropertyRepository", "getPropertiesByDistressStatus(status=$status) emitted ${list.size} items")
            }
    }

    fun getPropertiesByDistressStatusPaged(status: String, limit: Int, offset: Int): Flow<List<Property>> {
        Log.d("PropertyRepository", "getPropertiesByDistressStatusPaged(status=$status, limit=$limit, offset=$offset) called")
        return propertyDao.getPropertiesByDistressStatusPaged(status, limit, offset)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    fun getPropertiesByPipelineStatus(status: String): Flow<List<Property>> {
        Log.d("PropertyRepository", "getPropertiesByPipelineStatus(status=$status) called")
        return propertyDao.getPropertiesByPipelineStatus(status)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
            .onEach { list ->
                Log.d("PropertyRepository", "getPropertiesByPipelineStatus(status=$status) emitted ${list.size} items")
            }
    }

    fun getPropertiesByPipelineStatusPaged(status: String, limit: Int, offset: Int): Flow<List<Property>> {
        Log.d("PropertyRepository", "getPropertiesByPipelineStatusPaged(status=$status, limit=$limit, offset=$offset) called")
        return propertyDao.getPropertiesByPipelineStatusPaged(status, limit, offset)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    suspend fun updatePipelineStatus(id: Long, newStatus: String) = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "updatePipelineStatus(id=$id, newStatus=$newStatus) called")
        propertyDao.updatePipelineStatus(id, newStatus)
    }

    suspend fun updateRenovationProgress(id: Long, progress: Int, actualCost: Double) = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "updateRenovationProgress(id=$id, progress=$progress, actualCost=$actualCost) called")
        propertyDao.updateRenovationProgress(id, progress, actualCost)
    }

    suspend fun getPropertyById(id: Long): Property? = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "getPropertyById(id=$id) called")
        val result = propertyDao.getPropertyById(id)
        Log.d("PropertyRepository", "getPropertyById(id=$id) result: ${result?.title ?: "null"}")
        result
    }

    suspend fun insertProperty(property: Property): Long = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "insertProperty called for title=${property.title}")
        val id = propertyDao.insertProperty(property)
        Log.d("PropertyRepository", "insertProperty succeeded with id=$id")
        id
    }

    suspend fun insertProperties(properties: List<Property>) = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "insertProperties called with ${properties.size} items")
        propertyDao.insertProperties(properties)
        Log.d("PropertyRepository", "insertProperties inserted ${properties.size} items")
    }

    suspend fun updateProperty(property: Property) = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "updateProperty called for id=${property.id}")
        propertyDao.updateProperty(property)
    }

    suspend fun deleteProperty(property: Property) = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "deleteProperty called for id=${property.id}")
        propertyDao.deleteProperty(property)
    }

    suspend fun deletePropertiesByIds(ids: List<Long>): Int = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext 0
        Log.d("PropertyRepository", "deletePropertiesByIds called for ${ids.size} properties")
        propertyDao.deletePropertiesByIds(ids)
    }

    suspend fun updatePipelineStatusForMultiple(ids: List<Long>, newStatus: String): Int = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext 0
        Log.d("PropertyRepository", "updatePipelineStatusForMultiple called for ${ids.size} properties with newStatus=$newStatus")
        propertyDao.updatePipelineStatusForMultiple(ids, newStatus)
    }

    suspend fun archiveProperties(ids: List<Long>): Int = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext 0
        Log.d("PropertyRepository", "archiveProperties called for ${ids.size} properties")
        propertyDao.updatePipelineStatusForMultiple(ids, PipelineStatus.ARCHIVED.key)
    }

    suspend fun clearAll() = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "clearAll() called")
        propertyDao.clearAll()
    }

    suspend fun syncPortfolioProperties(
        importedProperties: List<Property>,
        syncMode: com.example.util.CsvSyncMode
    ): com.example.util.CsvSyncSummary = withContext(ioDispatcher) {
        var inserted = 0
        var updated = 0

        when (syncMode) {
            com.example.util.CsvSyncMode.REPLACE_ALL -> {
                propertyDao.clearAll()
                propertyDao.insertProperties(importedProperties)
                inserted = importedProperties.size
            }
            com.example.util.CsvSyncMode.APPEND_ONLY -> {
                propertyDao.insertProperties(importedProperties)
                inserted = importedProperties.size
            }
            com.example.util.CsvSyncMode.MERGE_UPDATE -> {
                val existingList = propertyDao.getAllPropertiesList()
                val toInsert = mutableListOf<Property>()
                val toUpdate = mutableListOf<Property>()

                for (imported in importedProperties) {
                    val match = existingList.firstOrNull { existing ->
                        (existing.address.isNotBlank() && existing.address.equals(imported.address, ignoreCase = true)) ||
                        (existing.title.isNotBlank() && existing.title.equals(imported.title, ignoreCase = true))
                    }

                    if (match != null) {
                        toUpdate.add(
                            imported.copy(
                                id = match.id,
                                createdAt = match.createdAt
                            )
                        )
                    } else {
                        toInsert.add(imported)
                    }
                }

                if (toInsert.isNotEmpty()) {
                    propertyDao.insertProperties(toInsert)
                    inserted = toInsert.size
                }
                for (item in toUpdate) {
                    propertyDao.updateProperty(item)
                    updated++
                }
            }
        }

        Log.d("PropertyRepository", "Portfolio sync completed: mode=$syncMode, inserted=$inserted, updated=$updated")
        com.example.util.CsvSyncSummary(
            totalRowsParsed = importedProperties.size,
            validProperties = importedProperties,
            insertedCount = inserted,
            updatedCount = updated
        )
    }
}

