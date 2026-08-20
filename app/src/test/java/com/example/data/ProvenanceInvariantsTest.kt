package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.util.MarketEstimateService
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
@Config(sdk = [34])
class ProvenanceInvariantsTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DealRadarRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DealRadarRepository(
            dealDao = db.propertyDealDao(),
            sourceDao = db.scraperSourceDao(),
            historyDao = db.priceHistoryDao(),
            investorDao = db.investorProfileDao(),
            propertyDao = db.propertyDao(),
            distressedPropertyDao = db.distressedPropertyDao()
        )
        MarketEstimateService.clearCache()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `1_il default di provenance non e mai REAL_SOURCE`() {
        val property = Property(
            address = "Via Roma 1",
            price = 100000.0
        )
        val deal = PropertyDeal(
            title = "Deal Default Test",
            location = "Milano (MI)",
            propertyType = "Residenziale",
            askingPrice = 120000.0,
            estimatedMarketValue = 180000.0
        )
        val distressed = DistressedProperty(
            address = "Via Torino 10"
        )

        assertEquals(DataProvenance.LEGACY_UNKNOWN.name, property.provenance)
        assertEquals(DataProvenance.LEGACY_UNKNOWN.name, deal.provenance)
        assertEquals(DataProvenance.LEGACY_UNKNOWN.name, distressed.provenance)

        assertNotEquals(DataProvenance.REAL_SOURCE.name, property.provenance)
        assertNotEquals(DataProvenance.REAL_SOURCE.name, deal.provenance)
        assertNotEquals(DataProvenance.REAL_SOURCE.name, distressed.provenance)
    }

    @Test
    fun `2_nessun metodo di ingestione produce record`() = runBlocking {
        // Seeding database iniziale
        repository.checkAndSeedDatabase()
        val countBefore = db.propertyDealDao().getAllDeals().first().size

        // Invocazione metodo di ingestione batch
        val summary = repository.executeLiveBatchScrapeAllSources()

        val countAfter = db.propertyDealDao().getAllDeals().first().size

        assertEquals(countBefore, countAfter)
        assertEquals(0, summary.activeCount)
        assertTrue(summary.logs.any { it.contains("Ingestione dati non disponibile", ignoreCase = true) })
    }

    @Test
    fun `3_un parser che non estrae nulla fallisce`() {
        val source = ScraperSource(
            id = "test_source",
            name = "Test Source",
            url = "https://example.com",
            robotsStatus = "CONSENTITO",
            configStatus = "CONSENTITO",
            activeParserRulesJson = """
                {
                    "listSelector": ".property-card",
                    "titleSelector": ".title",
                    "priceSelector": ".price",
                    "marketValueSelector": ".val"
                }
            """.trimIndent()
        )

        val result = repository.simulateParserTest(
            source = source,
            sampleHtmlOrJson = "", // HTML vuoto
            rulesJsonStr = ""
        )

        assertFalse("isSuccess deve essere false su HTML vuoto", result.isSuccess)
        assertTrue("extractedDeals deve essere vuota", result.extractedDeals.isEmpty())
        assertNull(result.extractedTitle)
        assertNotEquals("Attico Vista Duomo - Asta Dismissione BPER", result.extractedTitle)
    }

    @Test
    fun `4_i dati seed sono marcati come dimostrativi`() = runBlocking {
        repository.checkAndSeedDatabase()

        val deals = db.propertyDealDao().getAllDeals().first()
        val properties = db.propertyDao().getAllProperties().first()
        val distressed = db.distressedPropertyDao().getAllDistressedProperties().first()

        assertTrue("Deals non devono essere vuoti", deals.isNotEmpty())
        assertTrue("Properties non devono essere vuote", properties.isNotEmpty())
        assertTrue("Distressed properties non devono essere vuote", distressed.isNotEmpty())

        deals.forEach { deal ->
            assertEquals(
                "Deal '${deal.title}' deve avere provenance SYNTHETIC_DEMO",
                DataProvenance.SYNTHETIC_DEMO.name,
                deal.provenance
            )
        }

        properties.forEach { property ->
            assertEquals(
                "Property '${property.title}' deve avere provenance SYNTHETIC_DEMO",
                DataProvenance.SYNTHETIC_DEMO.name,
                property.provenance
            )
        }

        distressed.forEach { dist ->
            assertEquals(
                "Distressed '${dist.address}' deve avere provenance SYNTHETIC_DEMO",
                DataProvenance.SYNTHETIC_DEMO.name,
                dist.provenance
            )
        }
    }

    @Test
    fun `5_un fallback non alza la qualita del dato`() = runBlocking {
        val kpiResult = MarketEstimateService.scrapeMarketKpis("Milano")
        assertTrue(kpiResult.isSuccess)
        val kpi = kpiResult.getOrThrow()

        assertEquals("", kpi.sourceUrl)
        assertFalse("isLiveScraped deve essere false per fallback", kpi.isLiveScraped)
        assertNull("sourceReliability deve essere null: nessuna fonte esterna consultata", kpi.sourceReliability)
        assertNull("valuationConfidence deve essere null: nessun campione di comparabili", kpi.valuationConfidence)
        assertTrue("usedFallbackData deve essere true", kpi.usedFallbackData)
    }

    @Test
    fun `6_nessun valore non affidabile e marcato affidabile`() {
        for (provenance in DataProvenance.values()) {
            if (provenance != DataProvenance.REAL_SOURCE && provenance != DataProvenance.USER_ENTERED) {
                assertFalse(
                    "DataProvenance.${provenance.name} non deve essere marcato come affidabile",
                    provenance.isTrustworthy
                )
            } else {
                assertTrue(
                    "DataProvenance.${provenance.name} deve essere marcato come affidabile",
                    provenance.isTrustworthy
                )
            }
        }
    }
}
