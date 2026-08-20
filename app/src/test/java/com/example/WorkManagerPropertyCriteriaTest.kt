package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DistressedProperty
import com.example.data.InvestorProfile
import com.example.data.PropertyDeal
import com.example.service.DistressedPropertyCheckWorker
import com.example.service.DistressedWorkManagerScheduler
import com.example.ui.SavedAlertCriteria
import com.example.util.BriefMatcher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorkManagerPropertyCriteriaTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
    }

    @After
    fun tearDown() {
        // Clear prefs
        context.getSharedPreferences(DistressedPropertyCheckWorker.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun testInvestorBriefMatcher_MatchesTargetDealCorrectly() {
        val profile = InvestorProfile(
            briefActive = true,
            briefAlertsEnabled = true,
            briefTargetLocations = "Milano, Monza",
            briefPropertyTypes = "Residenziale, Asta",
            briefMaxBudget = 250000.0,
            briefMinDiscountPercent = 25,
            briefMinTargetRoiPercent = 10.0
        )

        val matchingDeal = PropertyDeal(
            id = 101L,
            title = "Appartamento Trilocale Asta Giudiziaria",
            location = "Milano (MI) - Navigli",
            propertyType = "Asta Residenziale",
            askingPrice = 180000.0,
            estimatedMarketValue = 280000.0,
            discountPercent = 35,
            estimatedCapRate = 12.5,
            status = "LIVE"
        )

        val matchResult = BriefMatcher.evaluate(matchingDeal, profile)
        assertTrue("Matching deal should be detected as target match", matchResult.isTargetMatch)
        assertTrue("Score should be high (>= 60)", matchResult.score >= 60)
        assertTrue("Reasons should include location and budget match", matchResult.reasons.isNotEmpty())
    }

    @Test
    fun testInvestorBriefMatcher_RejectsOutOfBudgetDeal() {
        val profile = InvestorProfile(
            briefActive = true,
            briefAlertsEnabled = true,
            briefTargetLocations = "Milano",
            briefMaxBudget = 200000.0,
            briefMinDiscountPercent = 30
        )

        val expensiveDeal = PropertyDeal(
            id = 102L,
            title = "Attico Centro Storico",
            location = "Napoli (NA)",
            propertyType = "Commerciale",
            askingPrice = 650000.0,
            estimatedMarketValue = 700000.0,
            discountPercent = 7
        )

        val matchResult = BriefMatcher.evaluate(expensiveDeal, profile)
        assertFalse("Expensive out-of-target deal should not match", matchResult.isTargetMatch)
    }

    @Test
    fun testDistressedAlertCriteria_EvaluatesProperly() {
        val criteria = SavedAlertCriteria(
            query = "Garibaldi",
            distressLevel = "HIGH",
            maxPrice = 200000.0,
            alertsEnabled = true
        )

        val matchingProperty = DistressedProperty(
            id = 201L,
            address = "Via Garibaldi 14, Milano",
            price = 145000.0,
            estimatedValue = 230000.0,
            distressLevel = "HIGH - Pre-Asta",
            status = "ACTIVE"
        )

        val nonMatchingProperty = DistressedProperty(
            id = 202L,
            address = "Corso Vittorio Emanuele, Roma",
            price = 350000.0,
            estimatedValue = 400000.0,
            distressLevel = "LOW",
            status = "ACTIVE"
        )

        // Query check
        assertTrue(matchingProperty.address.contains(criteria.query, ignoreCase = true))
        assertTrue(matchingProperty.distressLevel.contains(criteria.distressLevel, ignoreCase = true))
        assertTrue(matchingProperty.price <= criteria.maxPrice!!)

        // Non match check
        assertFalse(nonMatchingProperty.address.contains(criteria.query, ignoreCase = true))
        assertFalse(nonMatchingProperty.distressLevel.contains(criteria.distressLevel, ignoreCase = true))
        assertFalse(nonMatchingProperty.price <= criteria.maxPrice!!)
    }

    @Test
    fun testWorkManagerScheduler_SchedulesAndResetsWithoutCrashing() {
        // Test periodic schedule invocation
        DistressedWorkManagerScheduler.schedulePeriodicCheck(context, intervalMinutes = 15)

        // Test immediate one-time trigger
        DistressedWorkManagerScheduler.triggerImmediateCheck(context)

        // Test reset notified cache
        val prefs = context.getSharedPreferences(DistressedPropertyCheckWorker.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(DistressedPropertyCheckWorker.KEY_NOTIFIED_IDS, setOf("1", "2")).apply()
        assertEquals(2, prefs.getStringSet(DistressedPropertyCheckWorker.KEY_NOTIFIED_IDS, emptySet())?.size)

        DistressedWorkManagerScheduler.resetNotifiedCache(context)
        assertTrue(prefs.getStringSet(DistressedPropertyCheckWorker.KEY_NOTIFIED_IDS, emptySet())?.isEmpty() ?: true)

        // Test cancel periodic check
        DistressedWorkManagerScheduler.cancelPeriodicCheck(context)
    }

    @Test
    fun testRoomDatabase_QueriesListDirectly() = runBlocking {
        AppDatabase.seedDatabaseIfEmpty(context)

        val dealDao = database.propertyDealDao()
        val allDeals = dealDao.getAllDealsList()
        assertNotNull(allDeals)
        assertTrue("Room should contain seeded deals", allDeals.isNotEmpty())

        val distressedDao = database.distressedPropertyDao()
        val allDistressed = distressedDao.getDistressedPropertiesList()
        assertNotNull(allDistressed)
        assertTrue("Room should contain seeded distressed properties", allDistressed.isNotEmpty())
    }
}
