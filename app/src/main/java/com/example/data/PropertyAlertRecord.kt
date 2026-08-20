package com.example.data

import java.util.UUID

enum class PropertyAlertType(
    val key: String,
    val titleIt: String,
    val titleEn: String,
    val iconName: String
) {
    PRICE_DROP("PRICE_DROP", "Ribasso Prezzo", "Price Drop", "TrendingDown"),
    STATUS_CHANGE("STATUS_CHANGE", "Cambio Stato Pipeline", "Status Change", "SwapHoriz"),
    DISTRESS_STATUS_CHANGE("DISTRESS_STATUS_CHANGE", "Cambio Procedura Asta/Sofferenza", "Distress Status Change", "Gavel"),
    RENOVATION_MILESTONE("RENOVATION_MILESTONE", "Avanzamento Cantiere", "Renovation Milestone", "Construction")
}

enum class AlertSeverity {
    HIGH,
    MEDIUM,
    INFO
}

data class PropertyAlertRecord(
    val id: String = UUID.randomUUID().toString(),
    val propertyId: Long,
    val propertyTitle: String,
    val propertyAddress: String,
    val alertType: PropertyAlertType,
    val oldValue: String,
    val newValue: String,
    val changeSummary: String,
    val dropAmount: Double = 0.0,
    val dropPercent: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val severity: AlertSeverity = AlertSeverity.HIGH
)

data class MyPropertiesAlertPreferences(
    val notificationsEnabled: Boolean = true,
    val priceDropAlertsEnabled: Boolean = true,
    val priceDropThresholdPercent: Double = 5.0, // Minimum % drop to trigger alert (e.g. 5%)
    val priceDropMinAbsoluteEuros: Double = 2000.0, // Minimum € drop
    val statusChangeAlertsEnabled: Boolean = true,
    val renovationMilestoneAlertsEnabled: Boolean = true
)
