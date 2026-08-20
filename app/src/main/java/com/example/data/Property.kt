package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PipelineStatus(
    val key: String,
    val labelIt: String,
    val labelEn: String,
    val description: String
) {
    ANALYZED("ANALYZED", "Analizzato", "Analyzed", "Valutazione completata, in attesa di offerta"),
    IN_ESCROW("IN_ESCROW", "In Trattativa / Escrow", "In Escrow", "Offerta accettata, caparra o rogito in corso"),
    RENOVATING("RENOVATING", "In Ristrutturazione", "Renovating", "Cantiere aperto, lavori in esecuzione"),
    LISTED("LISTED", "In Vendita", "Listed", "Immobile sul mercato per la rivendita"),
    RENTED("RENTED", "A Reddito", "Rented", "Immobile locato con rendita mensile"),
    SOLD("SOLD", "Venduto / Concluso", "Sold", "Operazione completata con profitto realizzato"),
    ARCHIVED("ARCHIVED", "Archiviato", "Archived", "Immobile archiviato dallo storico attivo");

    companion object {
        fun fromKey(key: String): PipelineStatus {
            return values().find { it.key.equals(key, ignoreCase = true) } ?: ANALYZED
        }
    }
}

@Entity(tableName = "properties")
data class Property(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val price: Double,
    val distressStatus: String = "ASTA",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geolocationPrecision: String = GeolocationPrecision.UNKNOWN.name,
    val title: String = "",
    val propertyType: String = "Residenziale",
    val estimatedMarketValue: Double = price,
    val surfaceSqm: Int = 0,
    val notes: String = "",
    val photoUri: String? = null,
    val strategyTags: String = "Fix & Flip",
    val pipelineStatus: String = "ANALYZED", // "ANALYZED", "IN_ESCROW", "RENOVATING", "LISTED", "RENTED", "SOLD"
    val estimatedRenovationCost: Double = 0.0,
    val actualRenovationCost: Double = 0.0,
    val targetResalePrice: Double = 0.0,
    val actualSalePrice: Double = 0.0,
    val projectedRentalIncome: Double = 0.0,
    val escrowClosingDate: String = "",
    val renovationProgressPercent: Int = 0,
    val contractorNotes: String = "",
    val provenance: String = DataProvenance.LEGACY_UNKNOWN.name,
    val sourceRef: String? = null,       // id o URL della fonte, se esiste
    val retrievedAt: Long? = null,       // quando il dato è stato ottenuto
    val confidence: Double? = null,      // 0.0-1.0, null se non misurabile
    val evidenceRef: String? = null,     // riferimento verificabile (URL, RGE, doc)
    val createdAt: Long = System.currentTimeMillis()
) {
    val currentStatus: PipelineStatus
        get() = PipelineStatus.fromKey(pipelineStatus)

    // Financial calculations
    val totalCostBasis: Double
        get() = price + (if (actualRenovationCost > 0) actualRenovationCost else estimatedRenovationCost)

    val effectiveExitValue: Double
        get() = when {
            actualSalePrice > 0 -> actualSalePrice
            targetResalePrice > 0 -> targetResalePrice
            estimatedMarketValue > 0 -> estimatedMarketValue
            else -> price
        }

    val projectedProfit: Double
        get() = effectiveExitValue - totalCostBasis

    val projectedRoiPercent: Double
        get() {
            val cost = totalCostBasis
            return if (cost > 0) (projectedProfit / cost) * 100.0 else 0.0
        }
}

