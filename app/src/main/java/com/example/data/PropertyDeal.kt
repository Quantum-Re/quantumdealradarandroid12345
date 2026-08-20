package com.example.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.GeoUtils

@Immutable
@Entity(tableName = "property_deals")
data class PropertyDeal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceKey: String = "astalegale", // e.g. "quimmo", "iqera", "re_impresa", "bper_leasing", "demanio", "astalegale"
    val sourceName: String = "AstaLegale.net",
    val sourceUrl: String = "",
    val location: String, // e.g. "Milano (MI)"
    val propertyType: String, // "Residenziale", "Commerciale", "Industriale", "Asta Giudiziaria"
    val askingPrice: Double, // Current asking price or auction base price in €
    val estimatedMarketValue: Double, // Market valuation in €
    val surfaceSqm: Int = 80, // m²
    val discountPercent: Int = 0, // e.g. 35 -> 35% below market
    val estimatedCapRate: Double = 7.0, // e.g. 8.4% gross yield
    val auctionDate: String? = null, // e.g. "15/09/2026"
    val status: String = "LIVE", // "LIVE", "PRICE_CUT", "AUCTION_PENDING", "BOOKMARKED"
    val imageUrl: String = "",
    val notes: String = "",
    val isBookmarked: Boolean = false,
    val priceAlertThreshold: Double? = null,
    val dealStage: String = "PROSPECTING", // "PROSPECTING", "UNDER_CONTRACT", "CLOSING", "CLOSED"
    val lastViewedAt: Long = 0L,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val geolocationPrecision: String = GeolocationPrecision.UNKNOWN.name,
    val provenance: String = DataProvenance.LEGACY_UNKNOWN.name,
    val sourceRef: String? = null,       // id o URL della fonte, se esiste
    val retrievedAt: Long? = null,       // quando il dato è stato ottenuto
    val confidence: Double? = null,      // 0.0-1.0, null se non misurabile
    val evidenceRef: String? = null,     // riferimento verificabile (URL, RGE, doc)
    val createdAt: Long = System.currentTimeMillis()
) {
    val effectiveLatitude: Double?
        get() = when {
            latitude != 0.0 -> latitude
            else -> GeoUtils.getCoordinatesForLocation(location)?.latitude
        }

    val effectiveLongitude: Double?
        get() = when {
            longitude != 0.0 -> longitude
            else -> GeoUtils.getCoordinatesForLocation(location)?.longitude
        }

    val effectiveGeolocationPrecision: GeolocationPrecision
        get() = when {
            geolocationPrecision != GeolocationPrecision.UNKNOWN.name -> {
                GeolocationPrecision.fromString(geolocationPrecision)
            }
            latitude != 0.0 && longitude != 0.0 -> GeolocationPrecision.SOURCE_COORDINATES
            GeoUtils.getCoordinatesForLocation(location) != null -> GeolocationPrecision.CITY_CENTROID
            else -> GeolocationPrecision.UNKNOWN
        }
}

enum class DealStage(
    val key: String,
    val labelIt: String,
    val labelEn: String,
    val description: String
) {
    PROSPECTING("PROSPECTING", "Prospecting", "Prospecting", "In Valutazione / Prima Analisi"),
    UNDER_CONTRACT("UNDER_CONTRACT", "In Trattativa", "Under Contract", "Offerta Presentata / Trattativa"),
    CLOSING("CLOSING", "In Chiusura", "Closing", "Due Diligence / Rogito Imminente"),
    CLOSED("CLOSED", "Acquisito / Chiuso", "Closed", "Operazione Conclusa con Successo");

    companion object {
        fun fromKey(key: String): DealStage {
            return values().find { it.key.equals(key, ignoreCase = true) } ?: PROSPECTING
        }
    }
}

