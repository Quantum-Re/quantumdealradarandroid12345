package com.example.ui

import com.example.data.PipelineStatus
import com.example.data.Property
import com.example.ui.components.PortfolioMetricsCalculator
import com.example.util.OpportunityTier
import com.example.util.PropertyOpportunityEvaluation
import org.junit.Assert.*
import org.junit.Test

class PortfolioOverviewCalculationTest {

    private fun createTestProperty(
        id: Long,
        title: String,
        address: String,
        price: Double,
        renoCost: Double = 0.0,
        surfaceSqm: Int = 80,
        projectedRent: Double = 0.0,
        targetResale: Double = 0.0
    ): Property {
        return Property(
            id = id,
            title = title,
            address = address,
            price = price,
            surfaceSqm = surfaceSqm,
            propertyType = "Appartamento",
            distressStatus = "Libero",
            estimatedRenovationCost = renoCost,
            targetResalePrice = targetResale,
            projectedRentalIncome = projectedRent,
            pipelineStatus = PipelineStatus.ANALYZED.key,
            estimatedMarketValue = price * 1.2
        )
    }

    private fun createTestEvaluation(
        propertyId: Long,
        scrapedMarketVal: Double,
        grossYield: Double,
        oppScore: Int,
        daysOnMarket: Int = 90
    ): PropertyOpportunityEvaluation {
        return PropertyOpportunityEvaluation(
            propertyId = propertyId,
            location = "Milano",
            surfaceSqm = 80,
            acquisitionPrice = 200000.0,
            propertyPricePerSqm = 2500.0,
            liveMarketPricePerSqm = 3500.0,
            scrapedMarketValue = scrapedMarketVal,
            totalInvested = 230000.0,
            undervaluedPercent = 25.0,
            alphaEquityGain = scrapedMarketVal - 230000.0,
            potentialRoiPercent = 28.0,
            grossRentalYieldPotential = grossYield,
            daysOnMarket = daysOnMarket,
            marketSaturationScore = 30,
            absorptionRatePercent = 75.0,
            opportunityScore = oppScore,
            tier = OpportunityTier.fromScore(oppScore),
            headline = "Test Headline",
            actionableInsight = "Test Insight"
        )
    }

    @Test
    fun calculate_emptyPortfolio_returnsZeroMetrics() {
        val metrics = PortfolioMetricsCalculator.calculate(emptyList(), emptyMap())

        assertEquals(0, metrics.totalPropertiesCount)
        assertEquals(0.0, metrics.totalEquity, 0.01)
        assertEquals(0.0, metrics.averageGrossYieldPercent, 0.01)
        assertEquals(0, metrics.healthScore)
    }

    @Test
    fun calculate_withScrapedEvaluations_computesAccurateEquityAndYield() {
        val prop1 = createTestProperty(1L, "Appartamento 1", "Via Roma 10 Milano", 200000.0, renoCost = 30000.0, surfaceSqm = 80)
        val prop2 = createTestProperty(2L, "Appartamento 2", "Via Garibaldi 5 Bologna", 150000.0, renoCost = 20000.0, surfaceSqm = 60)

        val eval1 = createTestEvaluation(1L, scrapedMarketVal = 290000.0, grossYield = 7.5, oppScore = 85, daysOnMarket = 75)
        val eval2 = createTestEvaluation(2L, scrapedMarketVal = 210000.0, grossYield = 8.1, oppScore = 80, daysOnMarket = 85)

        val evals = mapOf(1L to eval1, 2L to eval2)
        val metrics = PortfolioMetricsCalculator.calculate(listOf(prop1, prop2), evals)

        // Total Invested = (200k + 30k) + (150k + 20k) = 400k
        assertEquals(400000.0, metrics.totalInvestedBasis, 0.01)
        // Total Scraped Market Value = 290k + 210k = 500k
        assertEquals(500000.0, metrics.totalScrapedMarketValue, 0.01)
        // Total Equity = 500k - 400k = 100k
        assertEquals(100000.0, metrics.totalEquity, 0.01)
        // Equity growth % = (100k / 400k) * 100 = 25%
        assertEquals(25.0, metrics.equityGrowthPercent, 0.01)

        // Average Yield = (7.5 + 8.1) / 2 = 7.8%
        assertEquals(7.8, metrics.averageGrossYieldPercent, 0.01)
        // Spread vs benchmark (5.8%) = 2.0%
        assertEquals(2.0, metrics.benchmarkYieldSpread, 0.01)

        // Health score should be high (>= 75)
        assertTrue("Health score should be >= 75, was ${metrics.healthScore}", metrics.healthScore >= 75)
        assertEquals(2, metrics.topEquityProperties.size)
    }
}
