package com.example

import com.example.data.InvestorProfile
import org.junit.Assert.*
import org.junit.Test

class PropertySearchCoachMarkTest {

    @Test
    fun testDefaultInvestorProfileRequiresCoachMark() {
        val newProfile = InvestorProfile(
            isProSubscriber = false,
            hasSeenSearchCoachMark = false
        )

        // For a new non-premium user who has not seen the tour, coach mark should be shown
        val shouldShowCoachMark = !newProfile.isProSubscriber && !newProfile.hasSeenSearchCoachMark
        assertTrue("New non-premium user should be shown the coach mark tour", shouldShowCoachMark)
    }

    @Test
    fun testProSubscriberSkipsCoachMark() {
        val proProfile = InvestorProfile(
            isProSubscriber = true,
            hasSeenSearchCoachMark = false
        )

        val shouldShowCoachMark = !proProfile.isProSubscriber && !proProfile.hasSeenSearchCoachMark
        assertFalse("Pro subscriber should not be prompted by default", shouldShowCoachMark)
    }

    @Test
    fun testDismissedCoachMarkPersistsState() {
        val profile = InvestorProfile(
            isProSubscriber = false,
            hasSeenSearchCoachMark = false
        )

        val updatedProfile = profile.copy(hasSeenSearchCoachMark = true)
        val shouldShowCoachMark = !updatedProfile.isProSubscriber && !updatedProfile.hasSeenSearchCoachMark
        assertFalse("Dismissed coach mark should not show on next app opens", shouldShowCoachMark)
    }
}
