package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.PropertyDeal
import com.example.data.RecentSearchQuery
import com.example.data.RecentSearchRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.Normalizer
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SearchAndStressTest {

    private lateinit var db: AppDatabase
    private lateinit var recentSearchRepo: RecentSearchRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        recentSearchRepo = RecentSearchRepository(db.recentSearchDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testRecentSearchRepository_MaxFiveLimitAndDeduplication() = runBlocking {
        // Save 6 search queries
        recentSearchRepo.saveSearchQuery("Milano")
        recentSearchRepo.saveSearchQuery("Roma")
        recentSearchRepo.saveSearchQuery("Torino")
        recentSearchRepo.saveSearchQuery("Bologna")
        recentSearchRepo.saveSearchQuery("Napoli")
        recentSearchRepo.saveSearchQuery("Firenze") // Should cause "Milano" to be pruned if limits hold

        val searches = recentSearchRepo.recentSearches.first()
        assertEquals(5, searches.size)
        assertEquals("Firenze", searches[0].query)
        assertEquals("Napoli", searches[1].query)

        // Test deduplication - re-searching "Torino" should bring it to top
        recentSearchRepo.saveSearchQuery("torino")
        val updatedSearches = recentSearchRepo.recentSearches.first()
        assertEquals(5, updatedSearches.size)
        assertEquals("torino", updatedSearches[0].query)

        // Test deletion
        recentSearchRepo.removeSearch("torino")
        val afterRemove = recentSearchRepo.recentSearches.first()
        assertEquals(4, afterRemove.size)
        assertFalse(afterRemove.any { it.query.equals("torino", ignoreCase = true) })

        // Test clear all
        recentSearchRepo.clearAll()
        val afterClear = recentSearchRepo.recentSearches.first()
        assertTrue(afterClear.isEmpty())
    }

    @Test
    fun testUnicodeNormalizationMatching() {
        fun normalize(input: String): String {
            return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase()
        }

        val textWithAccents = "Superba Villa a Milano Città in Zona Periferia Società Agricola"
        val normalizedText = normalize(textWithAccents)

        val searchQuery1 = "citta"
        val searchQuery2 = "societa"
        val searchQuery3 = "periferia"

        assertTrue(normalizedText.contains(normalize(searchQuery1)))
        assertTrue(normalizedText.contains(normalize(searchQuery2)))
        assertTrue(normalizedText.contains(normalize(searchQuery3)))
    }

    @Test
    fun testStressFilterPerformance_10000Items() {
        // Generate 10,000 mock deals
        val mockDeals = List(10_000) { index ->
            PropertyDeal(
                id = index.toLong(),
                title = "Appartamento $index a Milano Città Centro",
                sourceKey = "quimmo",
                sourceName = "Tribunale di Milano",
                sourceUrl = "https://example.com/deal/$index",
                location = if (index % 2 == 0) "Milano Via Roma $index" else "Torino Corso Francia $index",
                propertyType = if (index % 3 == 0) "Residenziale" else "Commerciale",
                askingPrice = 100_000.0 + (index * 10),
                estimatedMarketValue = 150_000.0 + (index * 10),
                surfaceSqm = 80,
                discountPercent = 33,
                estimatedCapRate = 8.5,
                auctionDate = "15/09/2026",
                isBookmarked = index % 5 == 0,
                lastViewedAt = if (index % 10 == 0) System.currentTimeMillis() else 0L
            )
        }

        fun normalize(input: String): String {
            return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase()
        }

        val query = "citta 100"
        val searchTokens = normalize(query).split("\\s+".toRegex()).filter { it.isNotBlank() }

        var matchesCount = 0
        val executionTimeMs = measureTimeMillis {
            matchesCount = mockDeals.count { deal ->
                val rawText = listOf(
                    deal.title,
                    deal.location,
                    deal.sourceName,
                    deal.propertyType,
                    deal.status,
                    "${deal.askingPrice.toInt()} €",
                    "${deal.surfaceSqm} mq"
                ).joinToString(" ")

                val searchableText = normalize(rawText)
                searchTokens.all { token -> searchableText.contains(token) }
            }
        }

        println("Stress Test Result: Filtered 10,000 items in $executionTimeMs ms. Matches found: $matchesCount")
        assertTrue("Should find matching items for 'citta 100'", matchesCount > 0)
    }
}
