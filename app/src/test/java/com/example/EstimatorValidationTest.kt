package com.example

import com.example.data.PropertyDeal
import com.example.util.AppCurrency
import com.example.util.AreaUnit
import com.example.util.CurrencyUnitConverter
import org.junit.Assert.*
import org.junit.Test

class EstimatorValidationTest {

    @Test
    fun testCurrencyUnitConverter_StandardEURComputation() {
        val priceEur = 100_000.0
        val arvEur = 150_000.0
        val surfaceSqm = 100.0

        val metrics = CurrencyUnitConverter.computeMetrics(
            priceEur = priceEur,
            arvEur = arvEur,
            surfaceSqm = surfaceSqm,
            currency = AppCurrency.EUR,
            unit = AreaUnit.SQ_METERS
        )

        // Renovation cost = 15% of 100,000 = 15,000
        // Net profit = 150,000 - 100,000 - 15,000 = 35,000
        // Total invested = 115,000
        // ROI = (35,000 / 115,000) * 100 = 30.43478%
        assertEquals(100_000.0, metrics.convertedPrice, 0.01)
        assertEquals(150_000.0, metrics.convertedArv, 0.01)
        assertEquals(15_000.0, metrics.convertedRenovationCost, 0.01)
        assertEquals(35_000.0, metrics.convertedNetProfit, 0.01)
        assertEquals(1_000.0, metrics.pricePerUnit, 0.01) // 100,000 / 100 sqm
        assertEquals(1_500.0, metrics.arvPerUnit, 0.01) // 150,000 / 100 sqm
        assertEquals(30.434, metrics.roiPercent, 0.01)
    }

    @Test
    fun testCurrencyUnitConverter_CurrencyAndUnitConversion() {
        val priceEur = 200_000.0
        val arvEur = 300_000.0
        val surfaceSqm = 100.0

        // Convert to USD and SQFT
        val metrics = CurrencyUnitConverter.computeMetrics(
            priceEur = priceEur,
            arvEur = arvEur,
            surfaceSqm = surfaceSqm,
            currency = AppCurrency.USD,
            unit = AreaUnit.SQ_FEET
        )

        val expectedUsdPrice = 200_000.0 * 1.09 // 218,000
        val expectedSqft = 100.0 * 10.7639 // 1,076.39

        assertEquals(expectedUsdPrice, metrics.convertedPrice, 0.1)
        assertEquals(expectedSqft, metrics.surfaceInUnit, 0.1)
        assertEquals(expectedUsdPrice / expectedSqft, metrics.pricePerUnit, 0.1)
    }

    @Test
    fun testCurrencyUnitConverter_DefaultArvFallback() {
        val priceEur = 100_000.0

        // When arvEur is null, should fallback to 1.35 * price = 135,000
        val metrics = CurrencyUnitConverter.computeMetrics(
            priceEur = priceEur,
            arvEur = null,
            surfaceSqm = 80.0
        )

        assertEquals(135_000.0, metrics.convertedArv, 0.01)
        assertTrue("Net profit should be positive with default ARV multiplier", metrics.convertedNetProfit > 0)
    }

    @Test
    fun testCurrencyUnitConverter_ZeroAndBoundaryValues() {
        val metrics = CurrencyUnitConverter.computeMetrics(
            priceEur = 0.0,
            arvEur = 0.0,
            surfaceSqm = 0.0
        )

        assertEquals(0.0, metrics.basePriceEur, 0.01)
        assertEquals(0.0, metrics.convertedPrice, 0.01)
        assertEquals(0.0, metrics.roiPercent, 0.01)
        assertTrue("Surface area should be coerced to minimum 10.0 sqm to avoid zero division", metrics.surfaceInSqm >= 10.0)
    }

    @Test
    fun testSearchResultsEstimationAggregation() {
        val searchResults = listOf(
            PropertyDeal(
                id = 1,
                title = "Trilocale Centro",
                sourceKey = "quimmo",
                sourceName = "Quimmo",
                sourceUrl = "https://example.com/1",
                location = "Milano",
                propertyType = "Residenziale",
                askingPrice = 120_000.0,
                estimatedMarketValue = 200_000.0,
                surfaceSqm = 90,
                discountPercent = 40,
                estimatedCapRate = 7.5,
                auctionDate = "2026-10-10"
            ),
            PropertyDeal(
                id = 2,
                title = "Attico Navigli",
                sourceKey = "pvp",
                sourceName = "PVP",
                sourceUrl = "https://example.com/2",
                location = "Milano",
                propertyType = "Residenziale",
                askingPrice = 250_000.0,
                estimatedMarketValue = 350_000.0,
                surfaceSqm = 120,
                discountPercent = 28,
                estimatedCapRate = 6.0,
                auctionDate = "2026-11-15"
            )
        )

        val totalAskingPrice = searchResults.sumOf { it.askingPrice }
        val totalMarketValue = searchResults.sumOf { it.estimatedMarketValue }
        val totalPotentialProfit = searchResults.sumOf { it.estimatedMarketValue - it.askingPrice }
        val avgDiscount = searchResults.map { it.discountPercent }.average()
        val avgCapRate = searchResults.map { it.estimatedCapRate }.average()

        assertEquals(370_000.0, totalAskingPrice, 0.01)
        assertEquals(550_000.0, totalMarketValue, 0.01)
        assertEquals(180_000.0, totalPotentialProfit, 0.01)
        assertEquals(34.0, avgDiscount, 0.01)
        assertEquals(6.75, avgCapRate, 0.01)
    }
}
