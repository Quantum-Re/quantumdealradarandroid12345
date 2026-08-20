package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DatabaseSeeder
import com.example.data.PropertyDeal
import com.example.ui.DealRadarViewModel
import com.example.ui.DistressedPropertyViewModel
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
class SearchValidationTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            DatabaseSeeder.seedDatabaseIfEmpty(db)
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDealSearchQueriesValidation() = runBlocking {
        val dealDao = db.propertyDealDao()
        val allDeals = dealDao.getAllDeals().first()
        assertTrue("Seeded deals should not be empty", allDeals.isNotEmpty())

        // Trial 1: Single token "milano"
        val queryMilano = "milano"
        val matchesMilano = filterDeals(allDeals, queryMilano)
        println("Trial 1 ('milano'): ${matchesMilano.size} deals found out of ${allDeals.size}")
        assertTrue("Should find deals matching 'milano'", matchesMilano.isNotEmpty())
        assertTrue("All results for 'milano' must contain 'milano' in searchable text",
            matchesMilano.all { dealContainsToken(it, "milano") })

        // Trial 2: Accent insensitive search "citta" vs "Città"
        val queryCitta = "citta"
        val matchesCitta = filterDeals(allDeals, queryCitta)
        println("Trial 2 ('citta'): ${matchesCitta.size} deals found")
        assertTrue("Accent normalization should match 'Città' or 'citta'",
            matchesCitta.all { dealContainsToken(it, "citta") })

        // Trial 3: Multi-token query "milano trilocale"
        val queryMulti = "milano trilocale"
        val matchesMulti = filterDeals(allDeals, queryMulti)
        println("Trial 3 ('milano trilocale'): ${matchesMulti.size} deals found")
        assertTrue("Multi-token search must satisfy both 'milano' AND 'trilocale'",
            matchesMulti.all { dealContainsToken(it, "milano") && dealContainsToken(it, "trilocale") })

        // Trial 4: Search by source name or numeric price/surface e.g. "tribunale"
        val queryTribunale = "tribunale"
        val matchesTribunale = filterDeals(allDeals, queryTribunale)
        println("Trial 4 ('tribunale'): ${matchesTribunale.size} deals found")

        // Trial 5: Non-matching query "xyznonexistent"
        val queryEmpty = "xyznonexistent999"
        val matchesEmpty = filterDeals(allDeals, queryEmpty)
        println("Trial 5 ('xyznonexistent999'): ${matchesEmpty.size} deals found")
        assertEquals(0, matchesEmpty.size)
    }

    @Test
    fun testDistressedPropertySearchValidation() = runBlocking {
        val distressedDao = db.distressedPropertyDao()
        val allDistressed = distressedDao.getAllDistressedProperties().first()
        assertTrue("Seeded distressed properties should not be empty", allDistressed.isNotEmpty())

        // Trial 1: Search by distress keyword or location
        val queryDistress = "roma"
        val matchesRoma = allDistressed.filter { p ->
            val searchableText = java.text.Normalizer.normalize(
                "${p.address} ${p.distressLevel} ${p.notes} ${p.price.toInt()}",
                java.text.Normalizer.Form.NFD
            ).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
            searchableText.contains("roma")
        }
        println("Distressed Trial 1 ('roma'): ${matchesRoma.size} items found")
        assertTrue("Should find distressed properties in Roma", matchesRoma.isNotEmpty())
    }

    private fun filterDeals(deals: List<PropertyDeal>, query: String): List<PropertyDeal> {
        val tokens = query.trim().let { q ->
            java.text.Normalizer.normalize(q, java.text.Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase()
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
        }

        if (tokens.isEmpty()) return deals

        return deals.filter { deal ->
            val rawText = listOf(
                deal.title,
                deal.location,
                deal.sourceName,
                deal.propertyType,
                deal.status,
                "-${deal.discountPercent}%",
                "${deal.discountPercent}%",
                "${deal.askingPrice.toInt()} €",
                "${deal.surfaceSqm} mq",
                deal.auctionDate ?: ""
            ).joinToString(" ")

            val searchableText = java.text.Normalizer.normalize(rawText, java.text.Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase()

            tokens.all { token -> searchableText.contains(token) }
        }
    }

    private fun dealContainsToken(deal: PropertyDeal, token: String): Boolean {
        val rawText = listOf(
            deal.title,
            deal.location,
            deal.sourceName,
            deal.propertyType,
            deal.status,
            "-${deal.discountPercent}%",
            "${deal.discountPercent}%",
            "${deal.askingPrice.toInt()} €",
            "${deal.surfaceSqm} mq",
            deal.auctionDate ?: ""
        ).joinToString(" ")

        val searchableText = java.text.Normalizer.normalize(rawText, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()

        val normalizedToken = java.text.Normalizer.normalize(token, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()

        return searchableText.contains(normalizedToken)
    }
}
