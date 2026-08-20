package com.example.util

import com.example.data.Property
import com.example.data.PropertyDeal
import java.util.Locale
import kotlin.math.*

/**
 * Historical yield distribution and statistical parameters for an Italian Province.
 */
data class ProvinceHistoricalYieldStats(
    val locationName: String,
    val provinceCode: String,
    val region: String,
    val isAvailable: Boolean = false,
    val historicalPoints: List<RegionalTrendPoint> = emptyList(),
    val meanHistoricalYield: Double? = null,
    val stdDevHistoricalYield: Double? = null,
    val minHistoricalYield: Double? = null,
    val maxHistoricalYield: Double? = null,
    val p10HistoricalYield: Double? = null,
    val p50HistoricalYield: Double? = null,
    val p90HistoricalYield: Double? = null,
    val p95HistoricalYield: Double? = null,
    val currentAvgSalePriceSqM: Double? = null,
    val currentAvgRentPriceSqM: Double? = null,
    val saleTrendYoY: Double? = null,
    val rentTrendYoY: Double? = null,
    val avgDaysOnMarket: Int? = null,
    val absorptionRatePercent: Double? = null
)

/**
 * Complete evaluation of a property's predictive deal potential against provincial historical yield trends.
 */
data class PredictiveDealEvaluation(
    val propertyId: Long,
    val propertyTitle: String,
    val location: String,
    val provinceCode: String,
    val currentListPrice: Double,
    val surfaceSqm: Int,
    val pricePerSqm: Double,
    val liveMarketPricePerSqm: Double,
    val valuationDiscountPercent: Double,
    val propertyImpliedGrossYield: Double,
    val provinceHistoricalStats: ProvinceHistoricalYieldStats,
    val isRankingAvailable: Boolean = false,
    val unavailabilityReason: String? = null,
    val dealPercentile: Double? = null, // 0.0 to 100.0 (e.g. 94.2 = Top 5.8%)
    val isTop10Percentile: Boolean = false,
    val topTierRankLabel: String? = null, // e.g. "Top 5% Alpha Deal", "Top 10% Super Deal", etc.
    val yieldSpreadVsP90: Double? = null, // propertyImpliedGrossYield - p90HistoricalYield
    val targetPriceForTop10Percentile: Double? = null, // Maximum price to enter 90th percentile
    val targetPricePerSqmForTop10: Double? = null,
    val priceBufferVsTop10: Double? = null, // Positive if already in top 10% (margin of safety)
    val predicted12mMarketValue: Double,
    val predicted24mMarketValue: Double,
    val predicted12mEquityGain: Double,
    val predicted12mYield: Double,
    val headline: String,
    val actionableSummary: String
)

/**
 * Predictive Alert Engine that analyses live list prices against scraped provincial historical KPI trends.
 */
object PredictiveDealAlertEngine {

    /**
     * Calculates statistical distribution of historical yields for a given province scraped KPI.
     * Only calculates statistics if a verified historical dataset is available with >= 12 samples.
     */
    fun computeProvinceHistoricalYieldStats(kpi: ProvinceScrapedKpi): ProvinceHistoricalYieldStats {
        val points = kpi.historicalTrends
        val hasVerifiedHistory = kpi.historicalDatasetVerified &&
                kpi.historicalSampleSize >= 12 &&
                points.size >= 12

        if (!hasVerifiedHistory) {
            return ProvinceHistoricalYieldStats(
                locationName = kpi.locationName,
                provinceCode = kpi.province,
                region = kpi.region,
                isAvailable = false,
                historicalPoints = points,
                meanHistoricalYield = null,
                stdDevHistoricalYield = null,
                minHistoricalYield = null,
                maxHistoricalYield = null,
                p10HistoricalYield = null,
                p50HistoricalYield = null,
                p90HistoricalYield = null,
                p95HistoricalYield = null,
                currentAvgSalePriceSqM = kpi.avgSalePriceSqM,
                currentAvgRentPriceSqM = kpi.avgRentPriceSqM,
                saleTrendYoY = kpi.saleTrendYoY,
                rentTrendYoY = kpi.rentTrendYoY,
                avgDaysOnMarket = kpi.avgDaysOnMarket,
                absorptionRatePercent = kpi.absorptionRatePercent
            )
        }

        val yields = points.map { it.grossYieldPercent }
        val meanYield = yields.average()
        val variance = yields.map { (it - meanYield).pow(2) }.average()
        val stdDev = sqrt(max(0.001, variance))

        val p90 = meanYield + 1.28155 * stdDev
        val p95 = meanYield + 1.64485 * stdDev
        val p10 = max(0.5, meanYield - 1.28155 * stdDev)
        val p50 = meanYield
        val minYield = yields.minOrNull() ?: meanYield
        val maxYield = yields.maxOrNull() ?: meanYield

        return ProvinceHistoricalYieldStats(
            locationName = kpi.locationName,
            provinceCode = kpi.province,
            region = kpi.region,
            isAvailable = true,
            historicalPoints = points,
            meanHistoricalYield = meanYield,
            stdDevHistoricalYield = stdDev,
            minHistoricalYield = minYield,
            maxHistoricalYield = maxYield,
            p10HistoricalYield = p10,
            p50HistoricalYield = p50,
            p90HistoricalYield = p90,
            p95HistoricalYield = p95,
            currentAvgSalePriceSqM = kpi.avgSalePriceSqM,
            currentAvgRentPriceSqM = kpi.avgRentPriceSqM,
            saleTrendYoY = kpi.saleTrendYoY,
            rentTrendYoY = kpi.rentTrendYoY,
            avgDaysOnMarket = kpi.avgDaysOnMarket,
            absorptionRatePercent = kpi.absorptionRatePercent
        )
    }

    /**
     * Standard Normal Cumulative Distribution Function (Gaussian CDF approximation)
     * Returns percentile rank between 0.0001 and 0.9999 for a given z-score.
     */
    private fun normalCdf(z: Double): Double {
        val t = 1.0 / (1.0 + 0.2316419 * abs(z))
        val d = 0.3989422804014327 * exp(-z * z / 2.0)
        val p = d * t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))))
        return if (z >= 0) 1.0 - p else p
    }

    /**
     * Evaluates a Property entity against provincial market historical yield trends.
     */
    fun evaluateProperty(
        property: Property,
        kpi: ProvinceScrapedKpi? = null
    ): PredictiveDealEvaluation {
        val location = PropertyOpportunityEngine.extractLocationName(property.address)?.ifBlank { "Milano" } ?: "Milano"
        val scrapedKpi = kpi ?: MarketEstimateService.getCuratedProvinceKpi(location)
        val provinceStats = computeProvinceHistoricalYieldStats(scrapedKpi)

        val surface = max(1, property.surfaceSqm)
        val price = max(1000.0, property.price)
        val pricePerSqm = price / surface
        val marketPricePerSqm = (provinceStats.currentAvgSalePriceSqM ?: 2000.0).coerceAtLeast(800.0)

        // 1. Valuation Discount % vs Scraped Provincial Baseline
        val valuationDiscountPercent = ((marketPricePerSqm - pricePerSqm) / marketPricePerSqm) * 100.0

        // 2. Implied Gross Rental Yield based on Scraped Provincial Rent / m²
        val rentPerSqm = provinceStats.currentAvgRentPriceSqM ?: 12.0
        val annualRentGross = rentPerSqm * surface * 12.0
        val propertyImpliedGrossYield = (annualRentGross / price) * 100.0

        // 7. Predictive 12M & 24M Forecast based on provincial trends
        val saleTrend = provinceStats.saleTrendYoY ?: 2.0
        val rentTrend = provinceStats.rentTrendYoY ?: 3.0
        val annualGrowthFactor = 1.0 + (saleTrend / 100.0)
        val rentGrowthFactor = 1.0 + (rentTrend / 100.0)

        val predicted12mMarketValue = (surface * marketPricePerSqm) * annualGrowthFactor
        val predicted24mMarketValue = (surface * marketPricePerSqm) * annualGrowthFactor.pow(2)
        val totalCost = price + property.estimatedRenovationCost
        val predicted12mEquityGain = predicted12mMarketValue - totalCost
        val predicted12mYield = ((annualRentGross * rentGrowthFactor) / price) * 100.0

        // Check if verified historical dataset is available for percentile evaluation
        val meanYield = provinceStats.meanHistoricalYield
        val stdDevYield = provinceStats.stdDevHistoricalYield
        val p90Yield = provinceStats.p90HistoricalYield

        if (!provinceStats.isAvailable || meanYield == null || stdDevYield == null || p90Yield == null) {
            val fallbackHeadline = "Ranking predittivo non disponibile: storico verificato insufficiente."
            val fallbackSummary = "Per ${provinceStats.locationName} non è disponibile una serie storica verificata con data certa. I percentili e i benchmark P90/P95 non vengono calcolati per garantire l'accuratezza dei dati."
            return PredictiveDealEvaluation(
                propertyId = property.id,
                propertyTitle = property.title.ifBlank { property.address },
                location = location,
                provinceCode = provinceStats.provinceCode,
                currentListPrice = price,
                surfaceSqm = surface,
                pricePerSqm = pricePerSqm,
                liveMarketPricePerSqm = marketPricePerSqm,
                valuationDiscountPercent = valuationDiscountPercent,
                propertyImpliedGrossYield = propertyImpliedGrossYield,
                provinceHistoricalStats = provinceStats,
                isRankingAvailable = false,
                unavailabilityReason = "Ranking predittivo non disponibile: storico verificato insufficiente.",
                dealPercentile = null,
                isTop10Percentile = false,
                topTierRankLabel = null,
                yieldSpreadVsP90 = null,
                targetPriceForTop10Percentile = null,
                targetPricePerSqmForTop10 = null,
                priceBufferVsTop10 = null,
                predicted12mMarketValue = predicted12mMarketValue,
                predicted24mMarketValue = predicted24mMarketValue,
                predicted12mEquityGain = predicted12mEquityGain,
                predicted12mYield = predicted12mYield,
                headline = fallbackHeadline,
                actionableSummary = fallbackSummary
            )
        }

        // 3. Yield Z-Score against Provincial Historical Yield Distribution
        val yieldZScore = (propertyImpliedGrossYield - meanYield) / stdDevYield
        val rawYieldPercentile = normalCdf(yieldZScore) * 100.0

        // 4. Valuation Discount Z-Score (Mean discount around 0%, std dev around 18%)
        val discountZScore = (valuationDiscountPercent - 0.0) / 18.0
        val rawDiscountPercentile = normalCdf(discountZScore) * 100.0

        // 5. Composite Deal Potential Percentile (Weighted: 65% Yield Alpha vs History, 35% Valuation Discount)
        val compositePercentile = ((rawYieldPercentile * 0.65) + (rawDiscountPercentile * 0.35)).coerceIn(1.0, 99.9)

        val isTop10 = compositePercentile >= 90.0
        val yieldSpreadVsP90 = propertyImpliedGrossYield - p90Yield

        // 6. Calculate the Target Price ceiling to reach P90 (90th percentile)
        val targetPriceForP90Yield = (annualRentGross / (p90Yield / 100.0))
        val targetPriceForDiscount = (marketPricePerSqm * 0.85) * surface
        val targetPriceForTop10 = min(targetPriceForP90Yield, targetPriceForDiscount)
        val targetPricePerSqmForTop10 = targetPriceForTop10 / surface

        val priceBufferVsTop10 = targetPriceForTop10 - price

        val topTierRankLabel = when {
            compositePercentile >= 95.0 -> "🔥 TOP 5% ALPHA DEAL"
            compositePercentile >= 90.0 -> "⭐ TOP 10% SUPER DEAL"
            compositePercentile >= 75.0 -> "⚡ TOP 25% STRONG DEAL"
            compositePercentile >= 50.0 -> "⚖️ FAIR MARKET VALUE"
            else -> "⚠️ SOTTO-MEDIA PROVINCIALE"
        }

        val formattedPercentile = String.format(Locale.US, "%.1f", compositePercentile)
        val formattedYield = String.format(Locale.ITALY, "%.2f", propertyImpliedGrossYield)
        val formattedP90 = String.format(Locale.ITALY, "%.2f", p90Yield)
        val formattedDiscount = String.format(Locale.ITALY, "%.1f", valuationDiscountPercent)

        val headline = if (isTop10) {
            "🚀 Entrato nel ${formattedPercentile}° Percentile Deal ($topTierRankLabel)"
        } else {
            "📊 Posizionato al ${formattedPercentile}° Percentile Provinciale"
        }

        val actionableSummary = if (isTop10) {
            "Rendimento implicito del $formattedYield% lordo, che supera la soglia P90 provinciale ($formattedP90%) con uno sconto sul prezzo di carico del $formattedDiscount% a ${provinceStats.locationName}. Margine di sicurezza ed equity alpha elevati."
        } else {
            val neededDrop = abs(priceBufferVsTop10)
            val formattedDrop = String.format(Locale.ITALY, "€%,.0f", neededDrop)
            "Rendimento stimato al $formattedYield% contro il benchmark P90 di $formattedP90%. Per entrare nella Top 10% provinciale è necessaria una trattativa o ribasso prezzo di $formattedDrop."
        }

        return PredictiveDealEvaluation(
            propertyId = property.id,
            propertyTitle = property.title.ifBlank { property.address },
            location = location,
            provinceCode = provinceStats.provinceCode,
            currentListPrice = price,
            surfaceSqm = surface,
            pricePerSqm = pricePerSqm,
            liveMarketPricePerSqm = marketPricePerSqm,
            valuationDiscountPercent = valuationDiscountPercent,
            propertyImpliedGrossYield = propertyImpliedGrossYield,
            provinceHistoricalStats = provinceStats,
            isRankingAvailable = true,
            unavailabilityReason = null,
            dealPercentile = compositePercentile,
            isTop10Percentile = isTop10,
            topTierRankLabel = topTierRankLabel,
            yieldSpreadVsP90 = yieldSpreadVsP90,
            targetPriceForTop10Percentile = targetPriceForTop10,
            targetPricePerSqmForTop10 = targetPricePerSqmForTop10,
            priceBufferVsTop10 = priceBufferVsTop10,
            predicted12mMarketValue = predicted12mMarketValue,
            predicted24mMarketValue = predicted24mMarketValue,
            predicted12mEquityGain = predicted12mEquityGain,
            predicted12mYield = predicted12mYield,
            headline = headline,
            actionableSummary = actionableSummary
        )
    }

    /**
     * Evaluates a PropertyDeal item from saved deals or radar feed.
     */
    fun evaluatePropertyDeal(
        deal: PropertyDeal,
        kpi: ProvinceScrapedKpi? = null
    ): PredictiveDealEvaluation {
        val dummyProp = Property(
            id = deal.id,
            title = deal.title,
            address = deal.location,
            price = deal.askingPrice,
            surfaceSqm = max(1, deal.surfaceSqm),
            estimatedRenovationCost = 0.0,
            targetResalePrice = deal.estimatedMarketValue,
            distressStatus = deal.propertyType,
            notes = deal.notes
        )
        return evaluateProperty(dummyProp, kpi)
    }
}
