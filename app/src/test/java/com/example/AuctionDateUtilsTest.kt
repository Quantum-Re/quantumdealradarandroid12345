package com.example.util

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AuctionDateUtilsTest {

    @Test
    fun testPastDateIsExpired() {
        val pastDate = "01/01/2020"
        assertTrue(AuctionDateUtils.isExpired(pastDate))
        val status = AuctionDateUtils.getStatus(pastDate)
        assertTrue(status is AuctionDateStatus.Expired)
    }

    @Test
    fun testFutureDateIsUpcoming() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 30)
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
        val futureDate = format.format(cal.time)

        assertFalse(AuctionDateUtils.isExpired(futureDate))
        val status = AuctionDateUtils.getStatus(futureDate)
        assertTrue(status is AuctionDateStatus.Upcoming)
        assertEquals(30, (status as AuctionDateStatus.Upcoming).daysLeft)
    }

    @Test
    fun testNullOrEmptyDate() {
        assertFalse(AuctionDateUtils.isExpired(null))
        val status = AuctionDateUtils.getStatus(null)
        assertTrue(status is AuctionDateStatus.NoDate)
    }
}
