package com.example.util

import com.example.data.Property
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class OpportunityTier(
    val label: String,
    val shortBadge: String,
    val description: String,
    val colorHex: Long
) {
    ULTRA(
        label = "🔥 ULTRA OPPORTUNITY",
        shortBadge = "ULTRA SOTTOQUOTATO",
        description = "Eccezionale sconto rispetto ai valori medi live di zona. Margine di sicurezza ed equity alpha elevatissimi.",
        colorHex = 0xFF10B981 // Emerald Green
    ),
    HIGH(
        label = "⚡ ALTA CONVENIENZA",
        shortBadge = "SOTTOQUOTATO",
        description = "Prezzo al m² sensibilmente inferiore al mercato locale. Ottima liquidità e tempi di assorbimento rapidi.",
        colorHex = 0xFF06B6D4 // Cyan
    ),
    MODERATE(
        label = "⚖️ FAIR VALUE",
        shortBadge = "IN LINEA COL MERCATO",
        description = "Prezzo coerente con le quotazioni di zona. Il profitto dipende principalmente dal valore aggiunto della ristrutturazione.",
        colorHex = 0xFFF59E0B // Amber
    ),
    LOW(
        label = "⚠️ MARGINE TIGHT",
        shortBadge = "PREZZO PIENO",
        description = "Costo di carico vicino o superiore ai prezzi medi degli annunci locali. Richiede massima cautela sui costi di cantiere.",
        colorHex = 0xFFEF4444 // Red
    );

    companion object {
        fun fromScore(score: Int): OpportunityTier {
            return when {
                score >= 80 -> ULTRA
                score >= 65 -> HIGH
                score >= 50 -> MODERATE
                else -> LOW
            }
        }
    }
}

data class PropertyOpportunityEvaluation(
    val propertyId: Long,
    val location: String,
    val surfaceSqm: Int,
    val acquisitionPrice: Double,
    val propertyPricePerSqm: Double,
    val liveMarketPricePerSqm: Double,
    val scrapedMarketValue: Double,
    val totalInvested: Double,
    val undervaluedPercent: Double,
    val alphaEquityGain: Double,
    val potentialRoiPercent: Double,
    val grossRentalYieldPotential: Double,
    val daysOnMarket: Int,
    val marketSaturationScore: Int,
    val absorptionRatePercent: Double,
    val opportunityScore: Int, // 0 - 100
    val tier: OpportunityTier,
    val headline: String,
    val actionableInsight: String,
    val sourceUrl: String = ""
)

object PropertyOpportunityEngine {

    /**
     * Extracts normalized city/municipality name from full property address.
     */
    fun extractLocationName(address: String): String {
        val cleaned = address
            .replace("Via ", "", ignoreCase = true)
            .replace("Viale ", "", ignoreCase = true)
            .replace("Corso ", "", ignoreCase = true)
            .replace("Piazza ", "", ignoreCase = true)
            .replace("Largo ", "", ignoreCase = true)
            .replace("Vicolo ", "", ignoreCase = true)
            .replace("Strada ", "", ignoreCase = true)
            .replace("Località ", "", ignoreCase = true)

        val parts = cleaned.split(",")
        return if (parts.size > 1) {
            parts.last().replace("\\(.*?\\)".toRegex(), "").trim()
        } else {
            cleaned.split(" ").lastOrNull()?.trim() ?: "Milano"
        }
    }

    /**
     * Calculates the real-time Opportunity Score for a single property
     * using live scraped or fallback Immobiliare.it market KPIs.
     */
    fun evaluateProperty(
        property: Property,
        kpi: ProvinceScrapedKpi? = null
    ): PropertyOpportunityEvaluation {
        val location = extractLocationName(property.address).ifBlank { "Milano" }
        val scrapedKpi = kpi ?: MarketEstimateService.getCuratedProvinceKpi(location)

        val surface = max(1, property.surfaceSqm)
        val propertyPricePerSqm = property.price / surface
        val liveMarketPricePerSqm = (scrapedKpi.avgSalePriceSqM ?: 2000.0).coerceAtLeast(800.0)
        val scrapedMarketValue = liveMarketPricePerSqm * surface

        val totalInvested = property.price + property.estimatedRenovationCost
        val alphaEquityGain = scrapedMarketValue - totalInvested

        // % undervalued vs live market average price
        val undervaluedPercent = if (scrapedMarketValue > 0) {
            ((scrapedMarketValue - property.price) / scrapedMarketValue) * 100.0
        } else 0.0

        // Potential ROI comparing target exit or live scraped market value against total invested
        val exitValue = if (property.targetResalePrice > 0) property.targetResalePrice else scrapedMarketValue
        val potentialRoiPercent = if (totalInvested > 0) {
            ((exitValue - totalInvested) / totalInvested) * 100.0
        } else 0.0

        val rentPriceSqM = scrapedKpi.avgRentPriceSqM ?: 12.0
        val grossRentalYieldPotential = if (property.price > 0) {
            (rentPriceSqM * surface * 12.0 / property.price) * 100.0
        } else 0.0

        // 1. Undervaluation Spread Score (0 - 50 points)
        val undervalueScore = ((undervaluedPercent / 35.0) * 50.0).coerceIn(0.0, 50.0)

        // 2. Distress Strategy & Pricing Advantage (0 - 20 points)
        val isDistressed = property.distressStatus.isNotBlank() &&
                !property.distressStatus.equals("Libero", ignoreCase = true) &&
                !property.distressStatus.equals("Nessuno", ignoreCase = true)
        val distressScore = if (isDistressed) 15.0 else 8.0
        val targetMarginBonus = if (property.targetResalePrice > scrapedMarketValue) 5.0 else 2.0

        // 3. Zone Velocity & Liquidity (0 - 20 points)
        val safeDom = (scrapedKpi.avgDaysOnMarket ?: 90).coerceIn(30, 200)
        val domScore = ((200.0 - safeDom) / 170.0).coerceIn(0.0, 1.0) * 12.0
        val safeAbsorption = (scrapedKpi.absorptionRatePercent ?: 60.0)
        val absorptionScore = (safeAbsorption / 90.0).coerceIn(0.0, 1.0) * 8.0

        // 4. Rental Yield Alternative Safety (0 - 10 points)
        val yieldScore = (grossRentalYieldPotential / 9.0).coerceIn(0.0, 1.0) * 10.0

        val rawTotal = undervalueScore + distressScore + targetMarginBonus + domScore + absorptionScore + yieldScore
        val opportunityScore = rawTotal.roundToInt().coerceIn(5, 99)
        val tier = OpportunityTier.fromScore(opportunityScore)

        val formattedSpread = String.format(Locale.US, "%.1f", undervaluedPercent)
        val formattedPpsqm = String.format(Locale.ITALY, "%,.0f", propertyPricePerSqm)
        val formattedMarketPpsqm = String.format(Locale.ITALY, "%,.0f", liveMarketPricePerSqm)

        val headline = when (tier) {
            OpportunityTier.ULTRA -> "🔥 Sottoquotato del $formattedSpread% rispetto al prezzo medio di $location"
            OpportunityTier.HIGH -> "⚡ Ottimo sconto sul mercato live di $location (-$formattedSpread%)"
            OpportunityTier.MODERATE -> "⚖️ Valore coerente con i parametri medi di $location"
            OpportunityTier.LOW -> "⚠️ Prezzo di carico elevato rispetto alla media di zona"
        }

        val domText = scrapedKpi.avgDaysOnMarket?.let { "${it}gg" } ?: "N/D"
        val absorptionText = scrapedKpi.absorptionRatePercent?.let { "${it.toInt()}%" } ?: "N/D"

        val actionableInsight = when (tier) {
            OpportunityTier.ULTRA -> "Prezzo di acquisto a €$formattedPpsqm/m² contro una media di mercato di €$formattedMarketPpsqm/m². Con tempi medi di vendita a soli $domText, puoi anticipare l'uscita con un margine extra o impostare un prezzo resale più aggressivo."
            OpportunityTier.HIGH -> "Il differenziale positivo garantisce un cuscinetto di sicurezza di oltre €${String.format(Locale.ITALY, "%,.0f", alphaEquityGain)} rispetto ai costi totali previsti. Ottima liquidità di zona (assorbimento $absorptionText)."
            OpportunityTier.MODERATE -> "In linea con i valori di mercato (€$formattedPpsqm/m² vs €$formattedMarketPpsqm/m²). La redditività dipende interamente dall'ottimizzazione del capitolato di ristrutturazione."
            OpportunityTier.LOW -> "Il costo di carico (€$formattedPpsqm/m²) è vicino al prezzo medio di vendita. Ricalibrare i costi di cantiere o valutare la conversione in locazione ad alta resa (€${String.format(Locale.ITALY, "%.1f", grossRentalYieldPotential)}% lordo)."
        }

        return PropertyOpportunityEvaluation(
            propertyId = property.id,
            location = location,
            surfaceSqm = surface,
            acquisitionPrice = property.price,
            propertyPricePerSqm = propertyPricePerSqm,
            liveMarketPricePerSqm = liveMarketPricePerSqm,
            scrapedMarketValue = scrapedMarketValue,
            totalInvested = totalInvested,
            undervaluedPercent = undervaluedPercent,
            alphaEquityGain = alphaEquityGain,
            potentialRoiPercent = potentialRoiPercent,
            grossRentalYieldPotential = grossRentalYieldPotential,
            daysOnMarket = scrapedKpi.avgDaysOnMarket ?: 90,
            marketSaturationScore = scrapedKpi.marketSaturationScore ?: 50,
            absorptionRatePercent = scrapedKpi.absorptionRatePercent ?: 60.0,
            opportunityScore = opportunityScore,
            tier = tier,
            headline = headline,
            actionableInsight = actionableInsight,
            sourceUrl = scrapedKpi.sourceUrl
        )
    }

    /**
     * Evaluates a list of properties and returns a map from propertyId to its Opportunity evaluation.
     */
    fun evaluateAllProperties(
        properties: List<Property>,
        kpiMap: Map<String, ProvinceScrapedKpi> = emptyMap()
    ): Map<Long, PropertyOpportunityEvaluation> {
        return properties.associate { prop ->
            val loc = extractLocationName(prop.address)
            val kpi = kpiMap[loc.lowercase()] ?: kpiMap[prop.address.lowercase()]
            prop.id to evaluateProperty(prop, kpi)
        }
    }
}
