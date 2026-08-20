package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScraperSourceDao {
    @Query("SELECT * FROM scraper_sources ORDER BY name ASC")
    fun getAllSources(): Flow<List<ScraperSource>>

    @Query("SELECT * FROM scraper_sources WHERE id = :id")
    suspend fun getSourceById(id: String): ScraperSource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: ScraperSource)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<ScraperSource>)

    @Update
    suspend fun updateSource(source: ScraperSource)

    @Query("UPDATE scraper_sources SET configStatus = :status WHERE id = :id")
    suspend fun updateConfigStatus(id: String, status: String)

    @Query("UPDATE scraper_sources SET activeParserRulesJson = :rulesJson WHERE id = :id")
    suspend fun updateParserRules(id: String, rulesJson: String)
}
