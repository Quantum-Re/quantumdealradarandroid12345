package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class AuctionDateStatus {
    object NoDate : AuctionDateStatus()
    data class Expired(val dateFormatted: String) : AuctionDateStatus()
    data class Today(val dateFormatted: String) : AuctionDateStatus()
    data class Upcoming(val dateFormatted: String, val daysLeft: Int) : AuctionDateStatus()
}

object AuctionDateUtils {
    private val dateFormats = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.ITALY),
        SimpleDateFormat("dd-MM-yyyy", Locale.ITALY),
        SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
    )

    fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        val cleanStr = dateStr.trim()
        if (cleanStr.equals("Oggi", ignoreCase = true)) return Date()
        for (format in dateFormats) {
            try {
                format.isLenient = false
                return format.parse(cleanStr)
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun isExpired(dateStr: String?): Boolean {
        val date = parseDate(dateStr) ?: return false
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        return date.before(today)
    }

    fun getStatus(dateStr: String?): AuctionDateStatus {
        val date = parseDate(dateStr) ?: return AuctionDateStatus.NoDate
        val formatOut = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
        val formattedDate = formatOut.format(date)

        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calAuction = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffMs = calAuction.timeInMillis - calToday.timeInMillis
        val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

        return when {
            diffDays < 0 -> AuctionDateStatus.Expired(formattedDate)
            diffDays == 0 -> AuctionDateStatus.Today(formattedDate)
            else -> AuctionDateStatus.Upcoming(formattedDate, diffDays)
        }
    }
}
