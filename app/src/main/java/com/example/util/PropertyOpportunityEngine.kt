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
    ),
    AVOID(
        label = "❌ DA EVITARE",
        shortBadge = "FUORI MERCATO",
        description = "Prezzo di acquisto superiore ai valori medi di zona. Rischio di minusvalenza o tempi di rivendita indefiniti.",
        colorHex = 0xFF991B1B // Dark Red
    );

    companion object {
        fun fromScore(score: Int): OpportunityTier {
            return when {
                score >= 80 -> ULTRA
                score >= 65 -> HIGH
                score >= 50 -> MODERATE
                score >= 25 -> LOW
                else -> AVOID
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
    val grossRentalYieldPotential: Double?,
    val daysOnMarket: Int?,
    val marketSaturationScore: Int?,
    val absorptionRatePercent: Double?,
    val opportunityScore: Int, // 0 - 100
    val scoreAffidabile: Boolean,
    val missingMarketData: List<String>,
    val tier: OpportunityTier,
    val headline: String,
    val actionableInsight: String,
    val sourceUrl: String = ""
)

object PropertyOpportunityEngine {

    /**
     * Extracts normalized city/municipality name from full property address.
     * Returns null if no valid city can be determined to avoid incorrect defaults.
     */
    fun extractLocationName(address: String): String? {
        if (address.isBlank()) return null

        val recognizedCities = listOf(
            "Milano", "Monza", "Paderno Dugnano", "Bergamo", "Brescia",
            "Roma", "Torino", "Bologna", "Firenze", "Napoli",
            "Verona", "Genova", "Palermo", "Bari"
        )

        val prefixes = listOf(
            "Via", "Viale", "Corso", "Piazza", "Piazzale",
            "Largo", "Vicolo", "Strada", "Località"
        )

        val segments = address.split(",")
        for (segment in segments) {
            val trimmed = segment.trim()
            // Remove provincial codes in parentheses, e.g. "(MI)"
            val withoutProvince = trimmed.replace("\\(.*?\\)".toRegex(), "").trim()

            if (withoutProvince.isBlank()) continue

            // Check if it starts with a street prefix followed by space or dot
            val startsWithPrefix = prefixes.any { prefix ->
                withoutProvince.startsWith("$prefix ", ignoreCase = true) ||
                withoutProvince.startsWith("$prefix.", ignoreCase = true)
            }

            if (startsWithPrefix) continue

            // Compare with recognized cities (whole word match, case insensitive)
            val matchedCity = recognizedCities.find { city ->
                withoutProvince.equals(city, ignoreCase = true)
            }

            if (matchedCity != null) return matchedCity
        }

        return null
    }

    /**
     * Calculates the real-time Opportunity Score for a single property
     * using live scraped or fallback Immobiliare.it market KPIs.
     */
    fun evaluateProperty(
        property: Property,
        kpi: ProvinceScrapedKpi? = null
    ): PropertyOpportunityEvaluation {
        val extractedLoc = extractLocationName(property.address)
        val location = extractedLoc ?: "Sconosciuta"
        val scrapedKpi = kpi ?: MarketEstimateService.getCuratedProvinceKpi(location)

        val missing = mutableListOf<String>()
        if (scrapedKpi.avgSalePriceSqM == null) missing.add("avgSalePriceSqM")
        
        val surface = max(1, property.surfaceSqm)
        val propertyPricePerSqm = property.price / surface
        
        // REGOLA 1: Se mancano dati di mercato essenziali, score = 0 e inaffidabile
        if (scrapedKpi.avgSalePriceSqM == null) {
            return PropertyOpportunityEvaluation(
                propertyId = property.id,
                location = location,
                surfaceSqm = surface,
                acquisitionPrice = property.price,
                propertyPricePerSqm = propertyPricePerSqm,
                liveMarketPricePerSqm = 0.0,
                scrapedMarketValue = 0.0,
                totalInvested = property.price + property.estimatedRenovationCost,
                undervaluedPercent = 0.0,
                alphaEquityGain = 0.0,
                potentialRoiPercent = 0.0,
                grossRentalYieldPotential = 0.0,
                daysOnMarket = 0,
                marketSaturationScore = 0,
                absorptionRatePercent = 0.0,
                opportunityScore = 0,
                scoreAffidabile = false,
                missingMarketData = missing,
                tier = OpportunityTier.AVOID,
                headline = "⚠️ Dati di mercato mancanti per $location",
                actionableInsight = "Non è possibile calcolare un punteggio affidabile senza le quotazioni medie di zona."
            )
        }

        val liveMarketPricePerSqm = scrapedKpi.avgSalePriceSqM!!
        val scrapedMarketValue = liveMarketPricePerSqm * surface

        val totalInvested = property.price + property.estimatedRenovationCost
        val alphaEquityGain = scrapedMarketValue - totalInvested

        // % undervalued vs live market average price
        val undervaluedPercent = ((scrapedMarketValue - property.price) / scrapedMarketValue) * 100.0

        // Potential ROI comparing target exit or live scraped market value against total invested
        val exitValue = if (property.targetResalePrice > 0) property.targetResalePrice else scrapedMarketValue
        val potentialRoiPercent = if (totalInvested > 0) {
            ((exitValue - totalInvested) / totalInvested) * 100.0
        } else 0.0

        val rentPriceSqM = scrapedKpi.avgRentPriceSqM
        val grossRentalYieldPotential = if (rentPriceSqM != null && property.price > 0) {
            (rentPriceSqM * surface * 12.0 / property.price) * 100.0
        } else null
        val yieldPerScore = grossRentalYieldPotential ?: 0.0

        // 1. Undervaluation Spread Score (0 - 50 points)
        val undervalueScore = ((undervaluedPercent / 35.0) * 50.0).coerceIn(-50.0, 50.0)

        // 2. Distress Strategy & Pricing Advantage (0 - 20 points)
        val isDistressed = property.distressStatus.isNotBlank() &&
                !property.distressStatus.equals("Libero", ignoreCase = true) &&
                !property.distressStatus.equals("Nessuno", ignoreCase = true)
        val distressScore = if (isDistressed) 15.0 else 0.0
        val targetMarginBonus = if (property.targetResalePrice > scrapedMarketValue) 5.0 else 0.0

        // 3. Zone Velocity & Liquidity (0 - 20 punti)
        // Se il dato manca, il blocco non contribuisce e i suoi punti escono dal
        // massimo teorico: il punteggio viene rinormalizzato sul massimo
        // effettivamente ottenibile. Un dato mancante non vale zero punti
        // (sarebbe una penalizzazione inventata) e non vale il valore medio
        // (sarebbe un dato inventato): esce dal calcolo.
        val dom = scrapedKpi.avgDaysOnMarket
        val domScore = dom?.let { ((200.0 - it.coerceIn(30, 200)) / 170.0).coerceIn(0.0, 1.0) * 12.0 }
        val absorption = scrapedKpi.absorptionRatePercent
        val absorptionScore = absorption?.let { (it / 90.0).coerceIn(0.0, 1.0) * 8.0 }

        // 4. Rental Yield Alternative Safety (0 - 10 points)
        val yieldScore = (yieldPerScore / 9.0).coerceIn(0.0, 1.0) * 10.0

        // Massimo teorico per blocco: undervalue 50, distress 15, margine 5,
        // dom 12, assorbimento 8, rendimento 10 = 100.
        // I blocchi senza dato escono sia dal numeratore sia dal denominatore.
        var puntiOttenuti = undervalueScore + distressScore + targetMarginBonus + yieldScore
        var massimoOttenibile = 50.0 + 15.0 + 5.0 + 10.0

        val datiMancanti = mutableListOf<String>()

        if (domScore != null) {
            puntiOttenuti += domScore
            massimoOttenibile += 12.0
        } else {
            datiMancanti.add("giorni medi di permanenza sul mercato")
        }

        if (absorptionScore != null) {
            puntiOttenuti += absorptionScore
            massimoOttenibile += 8.0
        } else {
            datiMancanti.add("tasso di assorbimento della zona")
        }

        if (scrapedKpi.marketSaturationScore == null) {
            datiMancanti.add("indice di saturazione della zona")
        }
        if (scrapedKpi.avgRentPriceSqM == null) {
            datiMancanti.add("canone medio al m²")
        }

        val rawTotal = (puntiOttenuti / massimoOttenibile) * 100.0
        val opportunityScore = rawTotal.roundToInt().coerceIn(0, 99)
        val tier = OpportunityTier.fromScore(opportunityScore)

        val formattedSpread = String.format(Locale.US, "%.1f", undervaluedPercent)
        val formattedPpsqm = String.format(Locale.ITALY, "%,.0f", propertyPricePerSqm)
        val formattedMarketPpsqm = String.format(Locale.ITALY, "%,.0f", liveMarketPricePerSqm)

        val headline = when (tier) {
            OpportunityTier.ULTRA -> "🔥 Sottoquotato del $formattedSpread% rispetto al prezzo medio di $location"
            OpportunityTier.HIGH -> "⚡ Ottimo sconto sul mercato live di $location (-$formattedSpread%)"
            OpportunityTier.MODERATE -> "⚖️ Valore coerente con i parametri medi di $location"
            OpportunityTier.LOW -> "⚠️ Prezzo di carico elevato rispetto alla media di zona"
            OpportunityTier.AVOID -> "❌ Asset fuori mercato o pericoloso"
        }

        val domText = scrapedKpi.avgDaysOnMarket?.let { "${it}gg" } ?: "N/D"
        val absorptionText = scrapedKpi.absorptionRatePercent?.let { "${it.toInt()}%" } ?: "N/D"

        val actionableInsight = when (tier) {
            OpportunityTier.ULTRA -> "Prezzo di acquisto a €$formattedPpsqm/m² contro una media di mercato di €$formattedMarketPpsqm/m². Con tempi medi di vendita a soli $domText, puoi anticipare l'uscita con un margine extra o impostare un prezzo resale più aggressivo."
            OpportunityTier.HIGH -> "Il differenziale positivo garantisce un cuscinetto di sicurezza di oltre €${String.format(Locale.ITALY, "%,.0f", alphaEquityGain)} rispetto ai costi totali previsti. Ottima liquidità di zona (assorbimento $absorptionText)."
            OpportunityTier.MODERATE -> "In linea con i valori di mercato (€$formattedPpsqm/m² vs €$formattedMarketPpsqm/m²). La redditività dipende interamente dall'ottimizzazione del capitolato di ristrutturazione."
            OpportunityTier.LOW -> {
                val yieldText = grossRentalYieldPotential?.let { String.format(Locale.ITALY, "%.1f%%", it) } ?: "N/D"
                "Il costo di carico (€$formattedPpsqm/m²) è vicino al prezzo medio di vendita. Ricalibrare i costi di cantiere o valutare la conversione in locazione ad alta resa ($yieldText lordo)."
            }
            OpportunityTier.AVOID -> "Prezzo per m² (€$formattedPpsqm) nettamente superiore ai valori di mercato (€$formattedMarketPpsqm). Operazione ad alto rischio di minusvalenza."
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
            daysOnMarket = scrapedKpi.avgDaysOnMarket,
            marketSaturationScore = scrapedKpi.marketSaturationScore,
            absorptionRatePercent = scrapedKpi.absorptionRatePercent,
            opportunityScore = opportunityScore,
            scoreAffidabile = datiMancanti.isEmpty(),
            missingMarketData = datiMancanti.toList(),
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
            val loc = extractLocationName(prop.address) ?: "Sconosciuta"
            val kpi = kpiMap[loc.lowercase()] ?: kpiMap[prop.address.lowercase()]
            prop.id to evaluateProperty(prop, kpi)
        }
    }
}
