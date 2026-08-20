package com.example.data

import java.util.UUID

enum class SupplyDemandShiftType(
    val titleIt: String,
    val shortLabel: String,
    val description: String,
    val isBullish: Boolean
) {
    SUPPLY_SQUEEZE(
        titleIt = "🔥 Shock di Domanda / Squeeze di Offerta",
        shortLabel = "SUPPLY SQUEEZE",
        description = "Forte calo dell'offerta disponibile e crollo dei giorni sul mercato (DOM). Mercato dei venditori in forte accelerazione.",
        isBullish = true
    ),
    SUPPLY_GLUT(
        titleIt = "⚠️ Eccesso di Offerta / Rallentamento Vendite",
        shortLabel = "SUPPLY GLUT",
        description = "Aumento rapido dello stock invenduto, saturazione alta e dilatazione dei tempi di vendita. Rischio svalutazione o stallo.",
        isBullish = false
    ),
    RENTAL_YIELD_DIVERGENCE(
        titleIt = "📈 Boom Canoni di Locazione vs Prezzi Vendita",
        shortLabel = "RENTAL DIVERGENCE",
        description = "La domanda di locazione cresce molto più velocemente dei valori di vendita. Massima convenienza per Buy & Hold a reddito.",
        isBullish = true
    ),
    MICRO_ZONE_HEATWAVE(
        titleIt = "⚡ Accelerazione Termica Micro-Zona",
        shortLabel = "MICRO-ZONE HEAT",
        description = "La specifica micro-zona dell'immobile mostra una sovraperformance e pressione di acquisto nettamente superiore alla media cittadina.",
        isBullish = true
    );

    companion object {
        fun fromKey(key: String): SupplyDemandShiftType {
            return values().find { it.name.equals(key, ignoreCase = true) } ?: SUPPLY_SQUEEZE
        }
    }
}

enum class ShiftSeverity(val labelIt: String, val level: Int) {
    CRITICAL("🚨 CRITICO", 3),
    MODERATE("⚡ SIGNIFICATIVO", 2),
    WATCH("ℹ️ SEGNALE PRECOCE", 1);

    companion object {
        fun fromLevel(level: Int): ShiftSeverity {
            return values().find { it.level == level } ?: MODERATE
        }
    }
}

data class SupplyDemandSnapshot(
    val location: String,
    val timestamp: Long = System.currentTimeMillis(),
    val marketSaturation: Int, // 0 - 100
    val daysOnMarket: Int, // DOM (e.g. 85)
    val absorptionRatePercent: Double, // % (e.g. 68.5%)
    val avgSalePriceSqM: Double,
    val avgRentPriceSqM: Double,
    val saleTrendYoY: Double,
    val rentTrendYoY: Double,
    val supplyDemandRatioIndex: Double, // Composite score 0 - 100 (High = extreme demand pressure, Low = glut)
    val tensionLabel: String
)

data class SupplyDemandAlertRecord(
    val id: String = UUID.randomUUID().toString(),
    val location: String,
    val affectedPropertyIds: List<Long> = emptyList(),
    val affectedPropertyTitles: List<String> = emptyList(),
    val shiftType: SupplyDemandShiftType,
    val severity: ShiftSeverity,
    val headline: String,
    val description: String,
    val strategicRecommendation: String,
    val previousRatio: Double,
    val currentRatio: Double,
    val ratioDeltaPercent: Double,
    val previousDom: Int,
    val currentDom: Int,
    val previousSaturation: Int,
    val currentSaturation: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val sourceUrl: String = ""
)

data class SupplyDemandMonitoringSettings(
    val isEnabled: Boolean = true,
    val pushNotificationsEnabled: Boolean = true,
    val sensitivityThresholdPercent: Double = 10.0, // Shift % trigger (e.g. 10%)
    val checkIntervalMinutes: Long = 15L,
    val monitorSavedPortfolio: Boolean = true,
    val monitorSavedDeals: Boolean = true,
    val lastScanTimestamp: Long = 0L,
    val lastScanResultsCount: Int = 0
)
