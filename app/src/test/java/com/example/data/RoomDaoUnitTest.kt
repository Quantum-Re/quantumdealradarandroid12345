package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomDaoUnitTest {

    private lateinit var db: AppDatabase
    private lateinit var dealDao: PropertyDealDao
    private lateinit var distressedDao: DistressedPropertyDao
    private lateinit var propertyDao: PropertyDao
    private lateinit var priceHistoryDao: PriceHistoryDao
    private lateinit var recentSearchDao: RecentSearchDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dealDao = db.propertyDealDao()
        distressedDao = db.distressedPropertyDao()
        propertyDao = db.propertyDao()
        priceHistoryDao = db.priceHistoryDao()
        recentSearchDao = db.recentSearchDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun createMockDeal(
        id: Long = 0L,
        title: String = "Test Deal",
        sourceKey: String = "quimmo",
        sourceName: String = "Quimmo",
        sourceUrl: String = "https://example.com/test",
        location: String = "Milano",
        propertyType: String = "Residenziale",
        askingPrice: Double = 150000.0,
        estimatedMarketValue: Double = 220000.0,
        surfaceSqm: Int = 85,
        discountPercent: Int = 32,
        estimatedCapRate: Double = 7.2,
        isBookmarked: Boolean = false,
        lastViewedAt: Long = 0L,
        createdAt: Long = System.currentTimeMillis()
    ): PropertyDeal {
        return PropertyDeal(
            id = id,
            title = title,
            sourceKey = sourceKey,
            sourceName = sourceName,
            sourceUrl = sourceUrl,
            location = location,
            propertyType = propertyType,
            askingPrice = askingPrice,
            estimatedMarketValue = estimatedMarketValue,
            surfaceSqm = surfaceSqm,
            discountPercent = discountPercent,
            estimatedCapRate = estimatedCapRate,
            isBookmarked = isBookmarked,
            lastViewedAt = lastViewedAt,
            createdAt = createdAt
        )
    }

    // ==========================================
    // 1. PropertyDealDao Tests
    // ==========================================

    @Test
    fun testPropertyDealDao_InsertAndRetrieveById() = runBlocking {
        val deal = createMockDeal(
            id = 101L,
            title = "Attico Navigli Milano",
            askingPrice = 220000.0
        )

        val insertedId = dealDao.insertDeal(deal)
        assertEquals(101L, insertedId)

        val retrieved = dealDao.getDealById(101L)
        assertNotNull(retrieved)
        assertEquals("Attico Navigli Milano", retrieved?.title)
        assertEquals(220000.0, retrieved?.askingPrice ?: 0.0, 0.01)

        // Edge case: Retrieve non-existent ID
        val missing = dealDao.getDealById(9999L)
        assertNull(missing)
    }

    @Test
    fun testPropertyDealDao_BatchInsertAndGetAllDeals() = runBlocking {
        val now = System.currentTimeMillis()
        val deals = listOf(
            createMockDeal(id = 1L, title = "Deal 1", createdAt = now - 2000),
            createMockDeal(id = 2L, title = "Deal 2 (Newest)", createdAt = now)
        )

        dealDao.insertDeals(deals)

        val allDeals = dealDao.getAllDeals().first()
        assertEquals(2, allDeals.size)
        // Should be ordered by createdAt DESC
        assertEquals("Deal 2 (Newest)", allDeals[0].title)
        assertEquals("Deal 1", allDeals[1].title)
    }

    @Test
    fun testPropertyDealDao_BookmarkedDeals() = runBlocking {
        val now = System.currentTimeMillis()
        val deals = listOf(
            createMockDeal(id = 1L, title = "Unbookmarked", isBookmarked = false),
            createMockDeal(id = 2L, title = "Bookmarked 1", isBookmarked = true, lastViewedAt = now - 5000),
            createMockDeal(id = 3L, title = "Bookmarked 2 (Most Recent)", isBookmarked = true, lastViewedAt = now)
        )

        dealDao.insertDeals(deals)

        var bookmarked = dealDao.getBookmarkedDeals().first()
        assertEquals(2, bookmarked.size)
        assertEquals("Bookmarked 2 (Most Recent)", bookmarked[0].title)

        // Toggle bookmark off for deal 3
        dealDao.setBookmarked(3L, false)
        bookmarked = dealDao.getBookmarkedDeals().first()
        assertEquals(1, bookmarked.size)
        assertEquals("Bookmarked 1", bookmarked[0].title)
    }

    @Test
    fun testPropertyDealDao_RecentlyViewedDeals() = runBlocking {
        val now = System.currentTimeMillis()
        val deals = (1..25).map { index ->
            createMockDeal(
                id = index.toLong(),
                title = "Deal $index",
                lastViewedAt = if (index <= 22) now + index else 0L
            )
        }

        dealDao.insertDeals(deals)

        val recentlyViewed = dealDao.getRecentlyViewedDeals().first()
        // Query has LIMIT 20
        assertEquals(20, recentlyViewed.size)
        // Most recent first (highest index/lastViewedAt)
        assertEquals("Deal 22", recentlyViewed[0].title)

        // Clear recently viewed
        dealDao.clearRecentlyViewedHistory()
        val afterClear = dealDao.getRecentlyViewedDeals().first()
        assertTrue(afterClear.isEmpty())
    }

    @Test
    fun testPropertyDealDao_DealsBySource() = runBlocking {
        val deals = listOf(
            createMockDeal(id = 1L, title = "PVP Deal", sourceKey = "pvp"),
            createMockDeal(id = 2L, title = "Quimmo Deal 1", sourceKey = "quimmo"),
            createMockDeal(id = 3L, title = "Quimmo Deal 2", sourceKey = "quimmo")
        )

        dealDao.insertDeals(deals)

        val quimmoDeals = dealDao.getDealsBySource("quimmo").first()
        assertEquals(2, quimmoDeals.size)

        val pvpDeals = dealDao.getDealsBySource("pvp").first()
        assertEquals(1, pvpDeals.size)

        val emptyDeals = dealDao.getDealsBySource("non_existent_source").first()
        assertTrue(emptyDeals.isEmpty())
    }

    @Test
    fun testPropertyDealDao_UpdatesAndDeletions() = runBlocking {
        val deal = createMockDeal(
            id = 50L,
            title = "Original Title",
            location = "Bologna",
            askingPrice = 180000.0
        ).copy(
            notes = "Initial notes",
            priceAlertThreshold = 170000.0,
            dealStage = "INSPECTION"
        )

        dealDao.insertDeal(deal)

        // Update notes
        dealDao.updateNotes(50L, "Updated inspection notes - OK")
        var fetched = dealDao.getDealById(50L)
        assertEquals("Updated inspection notes - OK", fetched?.notes)

        // Update price alert threshold to null and value
        dealDao.updatePriceAlertThreshold(50L, null)
        fetched = dealDao.getDealById(50L)
        assertNull(fetched?.priceAlertThreshold)

        dealDao.updatePriceAlertThreshold(50L, 160000.0)
        fetched = dealDao.getDealById(50L)
        assertEquals(160000.0, fetched?.priceAlertThreshold ?: 0.0, 0.01)

        // Update deal stage
        dealDao.updateDealStage(50L, "OFFER_SUBMITTED")
        fetched = dealDao.getDealById(50L)
        assertEquals("OFFER_SUBMITTED", fetched?.dealStage)

        // Delete deal
        dealDao.deleteDeal(fetched!!)
        assertNull(dealDao.getDealById(50L))

        // Clear all
        dealDao.insertDeal(deal)
        dealDao.clearAll()
        val allAfterClear = dealDao.getAllDeals().first()
        assertTrue(allAfterClear.isEmpty())
    }

    // ==========================================
    // 2. DistressedPropertyDao Tests
    // ==========================================

    @Test
    fun testDistressedPropertyDao_InsertAndRetrieve() = runBlocking {
        val prop = DistressedProperty(
            id = 10L,
            address = "Via Roma 45, Torino",
            price = 95000.0,
            estimatedValue = 160000.0,
            distressLevel = "HIGH",
            status = "PENDING",
            notes = "Asta imminente"
        )

        distressedDao.insertDistressedProperty(prop)

        val retrieved = distressedDao.getDistressedPropertyById(10L)
        assertNotNull(retrieved)
        assertEquals("Via Roma 45, Torino", retrieved?.address)

        val list = distressedDao.getDistressedPropertiesList()
        assertEquals(1, list.size)
        assertEquals("Via Roma 45, Torino", list[0].address)
    }

    @Test
    fun testDistressedPropertyDao_FilteringByLevelAndStatus() = runBlocking {
        val now = System.currentTimeMillis()
        val props = listOf(
            DistressedProperty(id = 1L, address = "Addr 1", price = 80000.0, estimatedValue = 120000.0, distressLevel = "HIGH", status = "ACTIVE", lastUpdated = now - 1000),
            DistressedProperty(id = 2L, address = "Addr 2", price = 150000.0, estimatedValue = 250000.0, distressLevel = "CRITICAL", status = "ACTIVE", lastUpdated = now),
            DistressedProperty(id = 3L, address = "Addr 3", price = 220000.0, estimatedValue = 300000.0, distressLevel = "HIGH", status = "RESOLVED", lastUpdated = now - 2000)
        )

        distressedDao.insertDistressedProperties(props)

        val highLevelProps = distressedDao.getDistressedPropertiesByLevel("HIGH").first()
        assertEquals(2, highLevelProps.size)

        val activeProps = distressedDao.getDistressedPropertiesByStatus("ACTIVE").first()
        assertEquals(2, activeProps.size)

        // Filtered query with level = "ALL"
        val allLevelFiltered = distressedDao.getFilteredDistressedProperties("ALL", 100000.0, 200000.0).first()
        assertEquals(1, allLevelFiltered.size)
        assertEquals(150000.0, allLevelFiltered[0].price, 0.01)

        // Filtered query with specific level = "HIGH"
        val highFiltered = distressedDao.getFilteredDistressedProperties("HIGH", null, 100000.0).first()
        assertEquals(1, highFiltered.size)
        assertEquals(80000.0, highFiltered[0].price, 0.01)

        // Filtered query matching none
        val emptyFiltered = distressedDao.getFilteredDistressedProperties("CRITICAL", 300000.0, null).first()
        assertTrue(emptyFiltered.isEmpty())
    }

    @Test
    fun testDistressedPropertyDao_UpdateAndDelete() = runBlocking {
        val prop = DistressedProperty(
            id = 5L,
            address = "Corso Vittorio Emanuele 12, Napoli",
            price = 110000.0,
            estimatedValue = 180000.0,
            distressLevel = "MEDIUM",
            status = "PENDING"
        )

        distressedDao.insertDistressedProperty(prop)

        val updatedProp = prop.copy(status = "ACTIVE", price = 105000.0)
        distressedDao.updateDistressedProperty(updatedProp)

        val fetched = distressedDao.getDistressedPropertyById(5L)
        assertEquals("ACTIVE", fetched?.status)
        assertEquals(105000.0, fetched?.price ?: 0.0, 0.01)

        distressedDao.deleteDistressedProperty(fetched!!)
        assertNull(distressedDao.getDistressedPropertyById(5L))

        distressedDao.insertDistressedProperty(prop)
        distressedDao.clearAll()
        val allAfterClear = distressedDao.getAllDistressedProperties().first()
        assertTrue(allAfterClear.isEmpty())
    }

    // ==========================================
    // 3. PropertyDao Tests
    // ==========================================

    @Test
    fun testPropertyDao_CrudOperations() = runBlocking {
        val prop1 = Property(
            id = 1001L,
            title = "Residenza Parco",
            address = "Via Parco 1, Firenze",
            price = 280000.0,
            estimatedMarketValue = 350000.0,
            distressStatus = "CRITICAL",
            createdAt = System.currentTimeMillis() - 1000
        )
        val prop2 = Property(
            id = 1002L,
            title = "Villa San Donà",
            address = "Via San Donà 8, Venezia",
            price = 450000.0,
            estimatedMarketValue = 600000.0,
            distressStatus = "MODERATE",
            createdAt = System.currentTimeMillis()
        )

        propertyDao.insertProperty(prop1)
        propertyDao.insertProperty(prop2)

        val allProps = propertyDao.getAllProperties().first()
        assertEquals(2, allProps.size)
        // Ordered by createdAt DESC
        assertEquals("Villa San Donà", allProps[0].title)

        val criticalProps = propertyDao.getPropertiesByDistressStatus("CRITICAL").first()
        assertEquals(1, criticalProps.size)
        assertEquals("Residenza Parco", criticalProps[0].title)

        val fetched = propertyDao.getPropertyById(1001L)
        assertNotNull(fetched)

        propertyDao.deleteProperty(fetched!!)
        val remaining = propertyDao.getAllProperties().first()
        assertEquals(1, remaining.size)

        propertyDao.clearAll()
        assertTrue(propertyDao.getAllProperties().first().isEmpty())
    }

    // ==========================================
    // 4. PriceHistoryDao Tests
    // ==========================================

    @Test
    fun testPriceHistoryDao_InsertAndRetrieve() = runBlocking {
        val historyList = listOf(
            PriceHistory(id = 1L, dealId = 101L, price = 250000.0, dateRecorded = "2026-01-01", eventLabel = "Base Price"),
            PriceHistory(id = 2L, dealId = 101L, price = 230000.0, dateRecorded = "2026-03-15", eventLabel = "Ribasso -8%"),
            PriceHistory(id = 3L, dealId = 202L, price = 150000.0, dateRecorded = "2026-02-01", eventLabel = "Base Price")
        )

        priceHistoryDao.insertHistories(historyList)

        val deal101History = priceHistoryDao.getHistoryForDeal(101L).first()
        assertEquals(2, deal101History.size)
        assertEquals(250000.0, deal101History[0].price, 0.01)
        assertEquals(230000.0, deal101History[1].price, 0.01)

        val deal202History = priceHistoryDao.getHistoryForDeal(202L).first()
        assertEquals(1, deal202History.size)
    }

    // ==========================================
    // 5. RecentSearchDao Tests
    // ==========================================

    @Test
    fun testRecentSearchDao_FullLifecycleAndPruning() = runBlocking {
        val repo = RecentSearchRepository(recentSearchDao)

        repo.saveSearchQuery("Milano")
        repo.saveSearchQuery("Roma")
        repo.saveSearchQuery("Torino")
        repo.saveSearchQuery("Bologna")
        repo.saveSearchQuery("Firenze")
        repo.saveSearchQuery("Venezia") // Should cause "Milano" to be pruned since limit is 5

        var searches = repo.recentSearches.first()
        assertEquals(5, searches.size)
        assertEquals("Venezia", searches[0].query)
        assertFalse(searches.any { it.query.equals("Milano", ignoreCase = true) })

        // Case-insensitive deletion by query string
        repo.removeSearch("roma")
        searches = repo.recentSearches.first()
        assertEquals(4, searches.size)
        assertFalse(searches.any { it.query.equals("Roma", ignoreCase = true) })

        // Delete by ID
        val targetId = searches[0].id
        recentSearchDao.deleteSearchById(targetId)
        searches = repo.recentSearches.first()
        assertEquals(3, searches.size)

        // Clear all
        repo.clearAll()
        searches = repo.recentSearches.first()
        assertTrue(searches.isEmpty())
    }

    // ==========================================
    // 6. Room Pagination & Result Set Limiting Tests
    // ==========================================

    @Test
    fun testPropertyDealDao_PaginationAndCount() = runBlocking {
        val now = System.currentTimeMillis()
        val deals = (1..15).map { i ->
            createMockDeal(
                id = i.toLong(),
                title = "Deal $i",
                createdAt = now + i
            )
        }
        dealDao.insertDeals(deals)

        val count = dealDao.getDealsCount().first()
        assertEquals(15, count)

        val page1 = dealDao.getDealsPaged(limit = 5, offset = 0).first()
        assertEquals(5, page1.size)
        assertEquals("Deal 15", page1[0].title)

        val page2 = dealDao.getDealsPaged(limit = 5, offset = 5).first()
        assertEquals(5, page2.size)
        assertEquals("Deal 10", page2[0].title)

        val page3 = dealDao.getDealsPaged(limit = 5, offset = 10).first()
        assertEquals(5, page3.size)
        assertEquals("Deal 5", page3[0].title)

        val pagedList = dealDao.getDealsPagedList(limit = 3, offset = 0)
        assertEquals(3, pagedList.size)
        assertEquals("Deal 15", pagedList[0].title)
    }

    @Test
    fun testDistressedPropertyDao_PaginationAndFiltering() = runBlocking {
        val now = System.currentTimeMillis()
        val props = (1..12).map { i ->
            DistressedProperty(
                id = i.toLong(),
                address = "Via Roma $i",
                price = 50000.0 * i,
                estimatedValue = 80000.0 * i,
                distressLevel = if (i % 2 == 0) "HIGH" else "CRITICAL",
                status = "ACTIVE",
                lastUpdated = now + i
            )
        }
        distressedDao.insertDistressedProperties(props)

        val count = distressedDao.getDistressedPropertiesCount().first()
        assertEquals(12, count)

        val page1 = distressedDao.getDistressedPropertiesPaged(limit = 4, offset = 0).first()
        assertEquals(4, page1.size)
        assertEquals("Via Roma 12", page1[0].address)

        val filteredPaged = distressedDao.getFilteredDistressedPropertiesPaged(
            level = "HIGH",
            minPrice = null,
            maxPrice = null,
            limit = 3,
            offset = 0
        ).first()
        assertEquals(3, filteredPaged.size)
        assertTrue(filteredPaged.all { it.distressLevel == "HIGH" })
    }

    @Test
    fun testPropertyDao_Pagination() = runBlocking {
        val now = System.currentTimeMillis()
        val props = (1..8).map { i ->
            Property(
                id = i.toLong(),
                title = "Property $i",
                address = "Corso Garibaldi $i",
                price = 120000.0 * i,
                createdAt = now + i
            )
        }
        props.forEach { propertyDao.insertProperty(it) }

        val count = propertyDao.getPropertiesCount().first()
        assertEquals(8, count)

        val page1 = propertyDao.getPropertiesPaged(limit = 3, offset = 0).first()
        assertEquals(3, page1.size)
        assertEquals("Property 8", page1[0].title)

        val page2 = propertyDao.getPropertiesPaged(limit = 3, offset = 3).first()
        assertEquals(3, page2.size)
        assertEquals("Property 5", page2[0].title)
    }

    @Test
    fun testPriceHistoryDao_LimitedQuery() = runBlocking {
        val histories = (1..15).map { i ->
            PriceHistory(
                id = i.toLong(),
                dealId = 99L,
                price = 200000.0 - (i * 1000),
                dateRecorded = "2026-01-$i",
                eventLabel = "Price Change $i"
            )
        }
        priceHistoryDao.insertHistories(histories)

        val limited = priceHistoryDao.getHistoryForDealLimited(99L, limit = 5).first()
        assertEquals(5, limited.size)
        // Ordered by id DESC
        assertEquals(15L, limited[0].id)
    }
}
