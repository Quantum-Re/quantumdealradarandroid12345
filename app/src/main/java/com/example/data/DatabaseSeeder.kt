package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Utility helper to seed the Room database with mock distressed property data
 * and property deals on app startup if the database is empty.
 * This guarantees map rendering works independently of external data sources.
 */
object DatabaseSeeder {

    suspend fun seedDatabaseIfEmpty(database: AppDatabase): SeedResult = withContext(Dispatchers.IO) {
        Log.d("DatabaseSeeder", "Evaluating database state for startup seeding...")

        var distressedSeeded = 0
        var dealsSeeded = 0
        var propertiesSeeded = 0
        var sourcesSeeded = 0

        // 1. Distressed Properties
        val distressedDao = database.distressedPropertyDao()
        val currentDistressed = distressedDao.getAllDistressedProperties().first()
        if (currentDistressed.isEmpty()) {
            val demoDistressed = InitialSeedData.initialDistressedProperties.map {
                it.copy(
                    provenance = DataProvenance.SYNTHETIC_DEMO.name,
                    retrievedAt = null
                )
            }
            Log.d("DatabaseSeeder", "Distressed properties table is EMPTY. Seeding ${demoDistressed.size} mock entries...")
            distressedDao.insertDistressedProperties(demoDistressed)
            distressedSeeded = demoDistressed.size
            Log.d("DatabaseSeeder", "Successfully seeded $distressedSeeded mock distressed properties into Room DB.")
        } else {
            Log.d("DatabaseSeeder", "Distressed properties table already contains ${currentDistressed.size} items.")
        }

        // 2. Property Deals
        val dealDao = database.propertyDealDao()
        val currentDeals = dealDao.getAllDeals().first()
        if (currentDeals.isEmpty()) {
            val demoDeals = InitialSeedData.initialDeals.map {
                it.copy(
                    provenance = DataProvenance.SYNTHETIC_DEMO.name,
                    retrievedAt = null
                )
            }
            Log.d("DatabaseSeeder", "Property deals table is EMPTY. Seeding ${demoDeals.size} mock deals...")
            dealDao.insertDeals(demoDeals)
            database.priceHistoryDao().insertHistories(InitialSeedData.initialHistories)
            dealsSeeded = demoDeals.size
            Log.d("DatabaseSeeder", "Successfully seeded $dealsSeeded mock property deals into Room DB.")
        } else {
            Log.d("DatabaseSeeder", "Property deals table already contains ${currentDeals.size} items.")
        }

        // 3. Properties
        val propertyDao = database.propertyDao()
        val currentProperties = propertyDao.getAllProperties().first()
        if (currentProperties.isEmpty()) {
            val demoProperties = InitialSeedData.initialProperties.map {
                it.copy(
                    provenance = DataProvenance.SYNTHETIC_DEMO.name,
                    retrievedAt = null
                )
            }
            Log.d("DatabaseSeeder", "Properties table is EMPTY. Seeding ${demoProperties.size} mock properties...")
            propertyDao.insertProperties(demoProperties)
            propertiesSeeded = demoProperties.size
            Log.d("DatabaseSeeder", "Successfully seeded $propertiesSeeded mock properties into Room DB.")
        } else {
            Log.d("DatabaseSeeder", "Properties table already contains ${currentProperties.size} items.")
        }

        // 4. Scraper Sources
        val sourceDao = database.scraperSourceDao()
        val currentSources = sourceDao.getAllSources().first()
        if (currentSources.isEmpty()) {
            Log.d("DatabaseSeeder", "Scraper sources table is EMPTY. Seeding ${InitialSeedData.initialSources.size} sources...")
            sourceDao.insertSources(InitialSeedData.initialSources)
            sourcesSeeded = InitialSeedData.initialSources.size
            Log.d("DatabaseSeeder", "Successfully seeded $sourcesSeeded scraper sources into Room DB.")
        } else {
            Log.d("DatabaseSeeder", "Scraper sources table already contains ${currentSources.size} items.")
        }

        // 5. Investor Profile
        val investorDao = database.investorProfileDao()
        if (investorDao.getProfile() == null) {
            investorDao.insertOrUpdateProfile(InvestorProfile())
            Log.d("DatabaseSeeder", "Seeded default investor profile.")
        }

        // 6. Recent Searches
        val recentSearchDao = database.recentSearchDao()
        val currentSearches = recentSearchDao.getRecentSearches().first()
        if (currentSearches.isEmpty()) {
            val initialSearches = listOf("Milano", "Roma", "Torino", "Bologna", "Napoli")
            val now = System.currentTimeMillis()
            initialSearches.forEachIndexed { idx, q ->
                recentSearchDao.insertSearch(RecentSearchQuery(query = q, timestamp = now - (idx * 100000L)))
            }
            Log.d("DatabaseSeeder", "Seeded initial recent searches into Room DB.")
        }

        val result = SeedResult(
            distressedSeeded = distressedSeeded,
            dealsSeeded = dealsSeeded,
            propertiesSeeded = propertiesSeeded,
            sourcesSeeded = sourcesSeeded
        )
        Log.d("DatabaseSeeder", "DatabaseSeeder completed: $result")
        result
    }
}

data class SeedResult(
    val distressedSeeded: Int,
    val dealsSeeded: Int,
    val propertiesSeeded: Int,
    val sourcesSeeded: Int
)
