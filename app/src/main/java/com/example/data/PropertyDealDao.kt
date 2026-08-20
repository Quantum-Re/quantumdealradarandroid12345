package com.example.data

import android.util.Log
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

@Dao
abstract class PropertyDealDao {
    @Query("SELECT * FROM property_deals ORDER BY createdAt DESC")
    abstract fun getAllDealsRaw(): Flow<List<PropertyDeal>>

    @Query("SELECT * FROM property_deals ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    abstract fun getDealsPagedRaw(limit: Int, offset: Int): Flow<List<PropertyDeal>>

    @Query("SELECT * FROM property_deals ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    abstract suspend fun getDealsPagedList(limit: Int, offset: Int): List<PropertyDeal>

    @Query("SELECT COUNT(*) FROM property_deals")
    abstract fun getDealsCountRaw(): Flow<Int>

    @Query("SELECT * FROM property_deals ORDER BY createdAt DESC")
    abstract suspend fun getAllDealsList(): List<PropertyDeal>

    fun getAllDeals(): Flow<List<PropertyDeal>> {
        Log.d("PropertyDealDao", "getAllDeals() query initiated")
        return getAllDealsRaw().onEach { list ->
            Log.d("PropertyDealDao", "getAllDeals() emitted ${list.size} deals from Room database")
        }
    }

    fun getDealsPaged(limit: Int, offset: Int): Flow<List<PropertyDeal>> {
        Log.d("PropertyDealDao", "getDealsPaged(limit=$limit, offset=$offset) query initiated")
        return getDealsPagedRaw(limit, offset).onEach { list ->
            Log.d("PropertyDealDao", "getDealsPaged(limit=$limit, offset=$offset) emitted ${list.size} deals")
        }
    }

    fun getDealsCount(): Flow<Int> {
        return getDealsCountRaw()
    }

    @Query("SELECT * FROM property_deals WHERE isBookmarked = 1 ORDER BY lastViewedAt DESC, createdAt DESC")
    abstract fun getBookmarkedDealsRaw(): Flow<List<PropertyDeal>>

    @Query("SELECT * FROM property_deals WHERE isBookmarked = 1 ORDER BY lastViewedAt DESC, createdAt DESC LIMIT :limit OFFSET :offset")
    abstract fun getBookmarkedDealsPagedRaw(limit: Int, offset: Int): Flow<List<PropertyDeal>>

    fun getBookmarkedDeals(): Flow<List<PropertyDeal>> {
        Log.d("PropertyDealDao", "getBookmarkedDeals() query initiated")
        return getBookmarkedDealsRaw().onEach { list ->
            Log.d("PropertyDealDao", "getBookmarkedDeals() emitted ${list.size} deals")
        }
    }

    fun getBookmarkedDealsPaged(limit: Int, offset: Int): Flow<List<PropertyDeal>> {
        Log.d("PropertyDealDao", "getBookmarkedDealsPaged(limit=$limit, offset=$offset) query initiated")
        return getBookmarkedDealsPagedRaw(limit, offset).onEach { list ->
            Log.d("PropertyDealDao", "getBookmarkedDealsPaged(limit=$limit, offset=$offset) emitted ${list.size} deals")
        }
    }

    @Query("SELECT * FROM property_deals WHERE lastViewedAt > 0 ORDER BY lastViewedAt DESC LIMIT 20")
    abstract fun getRecentlyViewedDealsRaw(): Flow<List<PropertyDeal>>

    fun getRecentlyViewedDeals(): Flow<List<PropertyDeal>> {
        Log.d("PropertyDealDao", "getRecentlyViewedDeals() query initiated")
        return getRecentlyViewedDealsRaw().onEach { list ->
            Log.d("PropertyDealDao", "getRecentlyViewedDeals() emitted ${list.size} deals")
        }
    }

    @Query("UPDATE property_deals SET lastViewedAt = :timestamp WHERE id = :id")
    abstract suspend fun updateLastViewedAtRaw(id: Long, timestamp: Long)

    suspend fun updateLastViewedAt(id: Long, timestamp: Long) {
        Log.d("PropertyDealDao", "updateLastViewedAt(id=$id, timestamp=$timestamp) executed")
        updateLastViewedAtRaw(id, timestamp)
    }

    @Query("UPDATE property_deals SET lastViewedAt = 0")
    abstract suspend fun clearRecentlyViewedHistoryRaw()

    suspend fun clearRecentlyViewedHistory() {
        Log.d("PropertyDealDao", "clearRecentlyViewedHistory() executed")
        clearRecentlyViewedHistoryRaw()
    }

    @Query("SELECT * FROM property_deals WHERE sourceKey = :sourceKey ORDER BY createdAt DESC")
    abstract fun getDealsBySourceRaw(sourceKey: String): Flow<List<PropertyDeal>>

    @Query("SELECT * FROM property_deals WHERE sourceKey = :sourceKey ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    abstract fun getDealsBySourcePagedRaw(sourceKey: String, limit: Int, offset: Int): Flow<List<PropertyDeal>>

    fun getDealsBySource(sourceKey: String): Flow<List<PropertyDeal>> {
        Log.d("PropertyDealDao", "getDealsBySource(sourceKey=$sourceKey) query initiated")
        return getDealsBySourceRaw(sourceKey).onEach { list ->
            Log.d("PropertyDealDao", "getDealsBySource(sourceKey=$sourceKey) emitted ${list.size} deals")
        }
    }

    fun getDealsBySourcePaged(sourceKey: String, limit: Int, offset: Int): Flow<List<PropertyDeal>> {
        Log.d("PropertyDealDao", "getDealsBySourcePaged(sourceKey=$sourceKey, limit=$limit, offset=$offset) query initiated")
        return getDealsBySourcePagedRaw(sourceKey, limit, offset).onEach { list ->
            Log.d("PropertyDealDao", "getDealsBySourcePaged(sourceKey=$sourceKey, limit=$limit, offset=$offset) emitted ${list.size} deals")
        }
    }

    @Query("SELECT * FROM property_deals WHERE id = :id")
    abstract suspend fun getDealByIdRaw(id: Long): PropertyDeal?

    suspend fun getDealById(id: Long): PropertyDeal? {
        Log.d("PropertyDealDao", "getDealById(id=$id) executed")
        val deal = getDealByIdRaw(id)
        Log.d("PropertyDealDao", "getDealById(id=$id) returned: ${deal?.title ?: "null"}")
        return deal
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDealRaw(deal: PropertyDeal): Long

    suspend fun insertDeal(deal: PropertyDeal): Long {
        Log.d("PropertyDealDao", "insertDeal called for title=${deal.title}")
        val id = insertDealRaw(deal)
        Log.d("PropertyDealDao", "insertDeal success with id=$id")
        return id
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDealsRaw(deals: List<PropertyDeal>)

    suspend fun insertDeals(deals: List<PropertyDeal>) {
        Log.d("PropertyDealDao", "insertDeals called with ${deals.size} deals")
        insertDealsRaw(deals)
        Log.d("PropertyDealDao", "insertDeals inserted ${deals.size} deals into Room")
    }

    @Update
    abstract suspend fun updateDealRaw(deal: PropertyDeal)

    suspend fun updateDeal(deal: PropertyDeal) {
        Log.d("PropertyDealDao", "updateDeal called for id=${deal.id}")
        updateDealRaw(deal)
    }

    @Query("UPDATE property_deals SET isBookmarked = :isBookmarked WHERE id = :id")
    abstract suspend fun setBookmarkedRaw(id: Long, isBookmarked: Boolean)

    suspend fun setBookmarked(id: Long, isBookmarked: Boolean) {
        Log.d("PropertyDealDao", "setBookmarked(id=$id, isBookmarked=$isBookmarked) executed")
        setBookmarkedRaw(id, isBookmarked)
    }

    @Query("UPDATE property_deals SET notes = :notes WHERE id = :id")
    abstract suspend fun updateNotesRaw(id: Long, notes: String)

    suspend fun updateNotes(id: Long, notes: String) {
        Log.d("PropertyDealDao", "updateNotes(id=$id) executed")
        updateNotesRaw(id, notes)
    }

    @Query("UPDATE property_deals SET priceAlertThreshold = :threshold WHERE id = :id")
    abstract suspend fun updatePriceAlertThresholdRaw(id: Long, threshold: Double?)

    suspend fun updatePriceAlertThreshold(id: Long, threshold: Double?) {
        Log.d("PropertyDealDao", "updatePriceAlertThreshold(id=$id, threshold=$threshold) executed")
        updatePriceAlertThresholdRaw(id, threshold)
    }

    @Query("UPDATE property_deals SET dealStage = :stage WHERE id = :id")
    abstract suspend fun updateDealStageRaw(id: Long, stage: String)

    suspend fun updateDealStage(id: Long, stage: String) {
        Log.d("PropertyDealDao", "updateDealStage(id=$id, stage=$stage) executed")
        updateDealStageRaw(id, stage)
    }

    @Delete
    abstract suspend fun deleteDealRaw(deal: PropertyDeal)

    suspend fun deleteDeal(deal: PropertyDeal) {
        Log.d("PropertyDealDao", "deleteDeal called for id=${deal.id}")
        deleteDealRaw(deal)
    }

    @Query("DELETE FROM property_deals")
    abstract suspend fun clearAllRaw()

    suspend fun clearAll() {
        Log.d("PropertyDealDao", "clearAll() executed on property_deals table")
        clearAllRaw()
    }
}

