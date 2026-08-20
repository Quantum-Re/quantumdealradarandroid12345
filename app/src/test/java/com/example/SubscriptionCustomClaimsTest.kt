package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.FirebaseAuthManager
import com.example.auth.FirebaseCustomClaims
import com.example.data.InvestorProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SubscriptionCustomClaimsTest {

    private lateinit var context: Context
    private lateinit var authManager: FirebaseAuthManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        authManager = FirebaseAuthManager(context)
    }

    @Test
    fun testDefaultClaimsAreFreeTier() = runBlocking {
        val claims = authManager.fetchCustomClaims()
        assertNotNull(claims)
        assertEquals("investor", claims.role)
    }

    @Test
    fun testSettingAnnualSubscriptionUpdatesClaimsAndExpiration() = runBlocking {
        val updatedClaims = authManager.setSubscriptionPlanAndClaims(
            plan = "ANNUAL",
            role = "pro_investor"
        )

        assertTrue(updatedClaims.isPremium)
        assertEquals("ANNUAL", updatedClaims.plan)
        assertEquals("pro_investor", updatedClaims.role)
        assertEquals(999, updatedClaims.maxUnlockedDeals)
        assertTrue(updatedClaims.validUntilTimestamp > System.currentTimeMillis())
        assertNotNull(updatedClaims.formattedValidUntil)
        assertNotEquals("Nessuna scadenza attiva", updatedClaims.formattedValidUntil)
    }

    @Test
    fun testSettingMonthlySubscriptionUpdatesClaims() = runBlocking {
        val updatedClaims = authManager.setSubscriptionPlanAndClaims(
            plan = "MONTHLY",
            role = "pro_investor"
        )

        assertTrue(updatedClaims.isPremium)
        assertEquals("MONTHLY", updatedClaims.plan)
        assertEquals("pro_investor", updatedClaims.role)
    }

    @Test
    fun testCancellationResetsToFreeTier() = runBlocking {
        authManager.setSubscriptionPlanAndClaims("ANNUAL", "pro_investor")
        val resetClaims = authManager.setSubscriptionPlanAndClaims("FREE", "investor")

        assertFalse(resetClaims.isPremium)
        assertEquals("FREE", resetClaims.plan)
        assertEquals("investor", resetClaims.role)
    }

    @Test
    fun testInvestorProfileWithSubscriptionClaims() {
        val profile = InvestorProfile(
            isProSubscriber = true,
            subscriptionPlan = "ANNUAL",
            subscriptionBillingCycle = "ANNUAL",
            customClaimsRole = "pro_investor"
        )

        assertTrue(profile.isProSubscriber)
        assertEquals("ANNUAL", profile.subscriptionPlan)
        assertEquals("pro_investor", profile.customClaimsRole)
    }
}
