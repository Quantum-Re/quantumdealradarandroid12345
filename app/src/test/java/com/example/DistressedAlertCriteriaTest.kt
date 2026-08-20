package com.example

import com.example.data.DistressedProperty
import com.example.ui.SavedAlertCriteria
import org.junit.Assert.*
import org.junit.Test

class DistressedAlertCriteriaTest {

    private fun matches(criteria: SavedAlertCriteria, property: DistressedProperty): Boolean {
        if (!criteria.alertsEnabled) return false

        val matchesQuery = criteria.query.isBlank() ||
                property.address.contains(criteria.query, ignoreCase = true) ||
                property.distressLevel.contains(criteria.query, ignoreCase = true)

        val matchesLevel = criteria.distressLevel.equals("ALL", ignoreCase = true) ||
                property.distressLevel.equals(criteria.distressLevel, ignoreCase = true) ||
                property.distressLevel.contains(criteria.distressLevel, ignoreCase = true)

        val matchesPrice = criteria.maxPrice == null || property.price <= criteria.maxPrice

        return matchesQuery && matchesLevel && matchesPrice
    }

    @Test
    fun testAlertDisabled_ReturnsFalse() {
        val criteria = SavedAlertCriteria(alertsEnabled = false)
        val property = DistressedProperty(
            address = "Via Roma 1",
            price = 100000.0,
            distressLevel = "Foreclosure"
        )
        assertFalse(matches(criteria, property))
    }

    @Test
    fun testAllMatches_ReturnsTrue() {
        val criteria = SavedAlertCriteria(
            query = "Roma",
            distressLevel = "Foreclosure",
            maxPrice = 120000.0,
            alertsEnabled = true
        )
        val property = DistressedProperty(
            address = "Via Roma 10, Milano",
            price = 95000.0,
            distressLevel = "Foreclosure"
        )
        assertTrue(matches(criteria, property))
    }

    @Test
    fun testExceedsPrice_ReturnsFalse() {
        val criteria = SavedAlertCriteria(
            query = "",
            distressLevel = "ALL",
            maxPrice = 100000.0,
            alertsEnabled = true
        )
        val property = DistressedProperty(
            address = "Via Garibaldi 5",
            price = 150000.0,
            distressLevel = "Auction"
        )
        assertFalse(matches(criteria, property))
    }

    @Test
    fun testLevelMismatch_ReturnsFalse() {
        val criteria = SavedAlertCriteria(
            query = "",
            distressLevel = "Tax Lien",
            maxPrice = null,
            alertsEnabled = true
        )
        val property = DistressedProperty(
            address = "Via Dante 12",
            price = 80000.0,
            distressLevel = "Foreclosure"
        )
        assertFalse(matches(criteria, property))
    }

    @Test
    fun testPropertyNotesUpdate() {
        val property = DistressedProperty(
            id = 10,
            address = "Corso Italia 4",
            price = 110000.0,
            distressLevel = "Auction",
            notes = ""
        )
        val updatedProperty = property.copy(notes = "Sopralluogo programmato per giovedì")
        assertEquals("Sopralluogo programmato per giovedì", updatedProperty.notes)
    }
}
