package com.example.data

import java.util.UUID

enum class AlertTriggerMode(
    val key: String,
    val titleIt: String,
    val titleEn: String,
    val icon: String,
    val description: String
) {
    PERCENTAGE_DROP("PERCENTAGE_DROP", "Ribasso Percentuale (%)", "Percentage Drop (%)", "📉", "Notifica se il prezzo cala di almeno X%"),
    TARGET_PRICE("TARGET_PRICE", "Prezzo Bersaglio (€)", "Target Price (€)", "🎯", "Notifica se il prezzo scende sotto la cifra esatta €"),
    MIN_CAP_RATE("MIN_CAP_RATE", "Rendimento Minimo (Cap Rate)", "Minimum Cap Rate", "📈", "Notifica se il Cap Rate supera la soglia desiderata"),
    TARGET_DISCOUNT_OMI("TARGET_DISCOUNT_OMI", "Sconto OMI Minimo (%)", "Target OMI Discount (%)", "🏷️", "Notifica se lo sconto rispetto al mercato supera X%")
}

enum class AlertFrequencyPreference(
    val key: String,
    val labelIt: String,
    val labelEn: String
) {
    INSTANT_PUSH("INSTANT_PUSH", "Push Immediata", "Instant Push"),
    HOURLY_DIGEST("HOURLY_DIGEST", "Riepilogo Orario", "Hourly Digest"),
    DAILY_SUMMARY("DAILY_SUMMARY", "Report Giornaliero", "Daily Summary")
}

data class GranularPropertyAlert(
    val dealId: Long,
    val dealTitle: String,
    val dealLocation: String,
    val currentAskingPrice: Double,
    val originalAskingPrice: Double = currentAskingPrice,
    val estimatedMarketValue: Double = currentAskingPrice,
    val propertyType: String = "Residenziale",
    val imageUrl: String = "",
    val isAlertEnabled: Boolean = true,
    val triggerMode: AlertTriggerMode = AlertTriggerMode.PERCENTAGE_DROP,
    val dropPercentThreshold: Double = 10.0, // e.g. 10.0%
    val targetPriceThreshold: Double = currentAskingPrice * 0.90, // e.g. 90% of price
    val minCapRateThreshold: Double = 8.0, // e.g. 8.0%
    val targetDiscountOmiThreshold: Int = 30, // e.g. 30%
    val isHighPriority: Boolean = true,
    val notifyOnAuctionDeserta: Boolean = true, // Auto alert on -25% ribasso for auctions
    val alertFrequency: AlertFrequencyPreference = AlertFrequencyPreference.INSTANT_PUSH,
    val customNotes: String = "",
    val totalAlertsTriggered: Int = 0,
    val lastTriggeredAt: Long? = null,
    val lastTriggeredPrice: Double? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Computes the effective target price at which the alert will fire based on the active trigger mode.
     */
    val effectiveTriggerPrice: Double
        get() = when (triggerMode) {
            AlertTriggerMode.PERCENTAGE_DROP -> {
                val base = if (originalAskingPrice > 0) originalAskingPrice else currentAskingPrice
                (base * (1.0 - (dropPercentThreshold / 100.0))).coerceAtLeast(1000.0)
            }
            AlertTriggerMode.TARGET_PRICE -> targetPriceThreshold.coerceAtLeast(1000.0)
            AlertTriggerMode.TARGET_DISCOUNT_OMI -> {
                if (estimatedMarketValue > 0) {
                    (estimatedMarketValue * (1.0 - (targetDiscountOmiThreshold / 100.0))).coerceAtLeast(1000.0)
                } else {
                    currentAskingPrice * 0.85
                }
            }
            AlertTriggerMode.MIN_CAP_RATE -> {
                // If asking price drops, cap rate increases
                val estimatedRent = (surfaceSqmApprox() * 13.5 * 12.0).coerceAtLeast(7000.0)
                if (minCapRateThreshold > 0) {
                    (estimatedRent / (minCapRateThreshold / 100.0)).coerceAtLeast(1000.0)
                } else {
                    currentAskingPrice * 0.90
                }
            }
        }

    private fun surfaceSqmApprox(): Double = 85.0

    val savingsAmount: Double
        get() = (currentAskingPrice - effectiveTriggerPrice).coerceAtLeast(0.0)

    val effectiveDropPercentFromCurrent: Double
        get() = if (currentAskingPrice > 0) {
            (((currentAskingPrice - effectiveTriggerPrice) / currentAskingPrice) * 100.0).coerceAtLeast(0.0)
        } else {
            0.0
        }
}

data class GlobalNotificationSettings(
    val masterPushEnabled: Boolean = true,
    val soundAndVibrationEnabled: Boolean = true,
    val headsUpHighPriorityEnabled: Boolean = true,
    val autoAuctionDesertaAlerts: Boolean = true,
    val defaultPriceDropPercent: Double = 10.0,
    val defaultMinAbsoluteDropEuros: Double = 5000.0,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 7
)

data class GranularAlertHistoryEvent(
    val id: String = UUID.randomUUID().toString(),
    val dealId: Long,
    val dealTitle: String,
    val dealLocation: String,
    val alertTypeTitle: String,
    val triggerMode: AlertTriggerMode,
    val oldPrice: Double,
    val newPrice: Double,
    val dropAmount: Double,
    val dropPercent: Double,
    val targetThresholdPrice: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
