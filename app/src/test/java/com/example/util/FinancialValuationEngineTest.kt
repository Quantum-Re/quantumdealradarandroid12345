package com.example.util

import com.example.data.Property
import com.example.data.PipelineStatus
import com.example.ui.components.PortfolioMetricsCalculator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinancialValuationEngineTest {

    // ==========================================
    // 1. SENIOR VALUATION ENGINE TESTS
    // ==========================================

    @Test
    fun `test SeniorValuationEngine standard apartment calculation`() {
        val input = SeniorValuationEngine.ValuationInput(
            mainCoveredSqM = 100.0,
            balconyTerraceSqM = 20.0,   // 20 * 0.35 = 7 sqm
            cellarAtticSqM = 10.0,      // 10 * 0.25 = 2.5 sqm
            gardenSqM = 50.0,           // 50 * 0.10 = 5 sqm
            baseZonePricePerSqM = 3000.0,
            condition = SeniorValuationEngine.PropertyCondition.GOOD_CONDITION, // 1.00
            floorLevel = SeniorValuationEngine.FloorLevel.INTERMEDIATE,          // 1.00
            hasElevator = true,
            energyClass = SeniorValuationEngine.EnergyClass.CLASS_D_C,          // 1.00
            occupancyStatus = SeniorValuationEngine.OccupancyStatus.VACANT,     // 1.00
            hasGarageOrParking = true,                                          // 1.05
            hasPanoramicView = false,                                           // 1.00
            estimatedSanatoriaCost = 5000.0
        )

        val result = SeniorValuationEngine.calculateValuation(input)

        // Commercial sq m = 100 + 7 + 2.5 + 5 = 114.5
        assertEquals(114.5, result.commercialSqM, 0.01)

        // Adjusted price per sqm = 3000 * 1.05 = 3150.0
        assertEquals(3150.0, result.adjustedPricePerSqM, 0.1)

        // Total gross = 114.5 * 3150 = 360,675. Minus sanatoria (5000) = 355,675.
        assertEquals(355675.0, result.totalEstimatedMarketValue, 1.0)

        // Verifying fair value range (+/- 7.5%)
        assertEquals(355675.0 * 0.925, result.minFairValue, 1.0)
        assertEquals(355675.0 * 1.075, result.maxFairValue, 1.0)

        // Verifying auction margins
        assertEquals(355675.0 * 0.80, result.recommendedAuctionBid20Margin, 1.0)
        assertEquals(355675.0 * 0.70, result.recommendedAuctionBid30Margin, 1.0)
    }

    @Test
    fun `test SeniorValuationEngine edge case - sanatoria cost exceeds gross value`() {
        val input = SeniorValuationEngine.ValuationInput(
            mainCoveredSqM = 30.0,
            baseZonePricePerSqM = 1000.0,
            condition = SeniorValuationEngine.PropertyCondition.NEEDS_TOTAL_RENOVATION,
            estimatedSanatoriaCost = 100000.0 // higher than property value
        )

        val result = SeniorValuationEngine.calculateValuation(input)
        // Value must never be negative
        assertEquals(0.0, result.totalEstimatedMarketValue, 0.001)
        assertEquals(0.0, result.minFairValue, 0.001)
    }

    @Test
    fun `test SeniorValuationEngine advanced underwriting stress testing`() {
        val input = SeniorValuationEngine.AdvancedUnderwritingInput(
            purchasePrice = 150000.0,
            renovationCost = 30000.0,
            estimatedMonthlyRent = 1000.0,
            resaleTargetPrice = 240000.0,
            holdingPeriodYears = 5,
            mortgageLtvPercent = 70.0,
            mortgageInterestRatePercent = 3.5,
            mortgageDurationYears = 20
        )

        val result = SeniorValuationEngine.performAdvancedUnderwriting(input)

        // Gross annual rent: 1000 * 12 = 12,000
        assertEquals(12000.0, result.grossAnnualRent, 0.01)

        // Borrowed debt: 150,000 * 70% = 105,000
        assertEquals(105000.0, result.borrowedDebt, 0.01)

        // Initial outlay = 150k + 30k + (150k * 0.05) = 187,500
        assertEquals(187500.0, result.totalInitialOutlay, 0.01)

        // Equity invested = 187,500 - 105,000 = 82,500
        assertEquals(82500.0, result.equityInvested, 0.01)

        // Must generate 3 stress test scenarios (Worst, Base, Best)
        assertEquals(3, result.stressTests.size)
        assertTrue(result.stressTests.any { it.name.contains("Stress") || it.name.contains("Worst") })
        assertTrue(result.stressTests.any { it.name.contains("Base") })
        assertTrue(result.stressTests.any { it.name.contains("Bull") || it.name.contains("Best") })
    }

    // ==========================================
    // 2. PROPERTY OPPORTUNITY ENGINE TESTS
    // ==========================================

    @Test
    fun `test PropertyOpportunityEngine extractLocationName with various address formats`() {
        assertEquals("Milano", PropertyOpportunityEngine.extractLocationName("Via Dante Alighieri 12, Milano (MI)"))
        assertEquals("Roma", PropertyOpportunityEngine.extractLocationName("Viale delle Milizie 45, Roma"))
        assertEquals("Torino", PropertyOpportunityEngine.extractLocationName("Corso Francia 100, Torino (TO)"))
        assertEquals("Bologna", PropertyOpportunityEngine.extractLocationName("Piazza Maggiore 1, Bologna"))
        assertEquals("Firenze", PropertyOpportunityEngine.extractLocationName("Firenze"))
    }

    @Test
    fun `test OpportunityTier classification thresholds`() {
        assertEquals(OpportunityTier.ULTRA, OpportunityTier.fromScore(100))
        assertEquals(OpportunityTier.ULTRA, OpportunityTier.fromScore(80))
        assertEquals(OpportunityTier.HIGH, OpportunityTier.fromScore(79))
        assertEquals(OpportunityTier.HIGH, OpportunityTier.fromScore(65))
        assertEquals(OpportunityTier.MODERATE, OpportunityTier.fromScore(64))
        assertEquals(OpportunityTier.MODERATE, OpportunityTier.fromScore(50))
        assertEquals(OpportunityTier.LOW, OpportunityTier.fromScore(49))
        assertEquals(OpportunityTier.LOW, OpportunityTier.fromScore(25))
        assertEquals(OpportunityTier.AVOID, OpportunityTier.fromScore(24))
        assertEquals(OpportunityTier.AVOID, OpportunityTier.fromScore(0))
    }

    // ==========================================
    // 3. PORTFOLIO METRICS CALCULATOR TESTS
    // ==========================================

    @Test
    fun `test PortfolioMetricsCalculator empty portfolio`() {
        val metrics = PortfolioMetricsCalculator.calculate(emptyList(), emptyMap())

        assertEquals(0, metrics.totalPropertiesCount)
        assertEquals(0.0, metrics.totalEquity, 0.001)
        assertEquals(0.0, metrics.totalAcquisitionCost, 0.001)
        assertEquals(0.0, metrics.averageGrossYieldPercent, 0.001)
        assertEquals("Nessun Immobile", metrics.healthTierLabel)
    }

    @Test
    fun `test PortfolioMetricsCalculator multi-property aggregation`() {
        val prop1 = Property(
            id = 1L,
            title = "Bilocale Navigli",
            address = "Via Corsico 4, Milano",
            price = 200000.0,
            estimatedRenovationCost = 25000.0,
            actualRenovationCost = 25000.0,
            surfaceSqm = 60,
            pipelineStatus = PipelineStatus.RENTED.key
        )
        val prop2 = Property(
            id = 2L,
            title = "Trilocale San Donato",
            address = "Via Roma 10, Milano",
            price = 150000.0,
            estimatedRenovationCost = 15000.0,
            actualRenovationCost = 0.0, // uses estimated
            surfaceSqm = 80,
            pipelineStatus = PipelineStatus.ANALYZED.key
        )

        val eval1 = PropertyOpportunityEvaluation(
            propertyId = 1L,
            location = "Milano",
            surfaceSqm = 60,
            acquisitionPrice = 200000.0,
            propertyPricePerSqm = 3333.3,
            liveMarketPricePerSqm = 4500.0,
            scrapedMarketValue = 270000.0,
            totalInvested = 225000.0,
            undervaluedPercent = 20.0,
            alphaEquityGain = 45000.0,
            potentialRoiPercent = 20.0,
            grossRentalYieldPotential = 6.5,
            daysOnMarket = 30,
            marketSaturationScore = 40,
            absorptionRatePercent = 75.0,
            opportunityScore = 82,
            scoreAffidabile = true,
            missingMarketData = emptyList(),
            tier = OpportunityTier.ULTRA,
            headline = "Ultra",
            actionableInsight = "Buy"
        )

        val eval2 = PropertyOpportunityEvaluation(
            propertyId = 2L,
            location = "Milano",
            surfaceSqm = 80,
            acquisitionPrice = 150000.0,
            propertyPricePerSqm = 1875.0,
            liveMarketPricePerSqm = 2500.0,
            scrapedMarketValue = 200000.0,
            totalInvested = 165000.0,
            undervaluedPercent = 17.5,
            alphaEquityGain = 35000.0,
            potentialRoiPercent = 21.2,
            grossRentalYieldPotential = 7.0,
            daysOnMarket = 45,
            marketSaturationScore = 50,
            absorptionRatePercent = 65.0,
            opportunityScore = 78,
            scoreAffidabile = true,
            missingMarketData = emptyList(),
            tier = OpportunityTier.HIGH,
            headline = "High",
            actionableInsight = "Buy"
        )

        val metrics = PortfolioMetricsCalculator.calculate(
            properties = listOf(prop1, prop2),
            evaluations = mapOf(1L to eval1, 2L to eval2)
        )

        assertEquals(2, metrics.totalPropertiesCount)
        assertEquals(350000.0, metrics.totalAcquisitionCost, 0.01)
        assertEquals(40000.0, metrics.totalRenovationCost, 0.01)
        assertEquals(390000.0, metrics.totalInvestedBasis, 0.01)
        assertEquals(470000.0, metrics.totalScrapedMarketValue, 0.01)

        // Total equity = 470,000 - 390,000 = 80,000
        assertEquals(80000.0, metrics.totalEquity, 0.01)

        // Equity growth percent = (80000 / 390000) * 100 = ~20.51%
        assertEquals(20.51, metrics.equityGrowthPercent, 0.1)

        // Average yield = (6.5 + 7.0) / 2 = 6.75%
        assertEquals(6.75, metrics.averageGrossYieldPercent, 0.01)

        // Benchmark spread vs 5.8% = +0.95%
        assertEquals(0.95, metrics.benchmarkYieldSpread, 0.01)
    }

    // ==========================================
    // 4. CURRENCY EXCHANGE RATE SERVICE TESTS
    // ==========================================

    @Test
    fun `test CurrencyExchangeRateService conversions`() {
        val eurAmount = 100000.0

        // EUR to USD
        val usdAmount = CurrencyExchangeRateService.convertFromEur(eurAmount, AppCurrency.USD)
        assertTrue(usdAmount > 0)

        // EUR to GBP
        val gbpAmount = CurrencyExchangeRateService.convertFromEur(eurAmount, AppCurrency.GBP)
        assertTrue(gbpAmount > 0)

        // EUR to CHF
        val chfAmount = CurrencyExchangeRateService.convertFromEur(eurAmount, AppCurrency.CHF)
        assertTrue(chfAmount > 0)

        // EUR to JPY
        val jpyAmount = CurrencyExchangeRateService.convertFromEur(eurAmount, AppCurrency.JPY)
        assertTrue(jpyAmount > eurAmount) // JPY exchange rate is > 100

        // Inverse conversion back to EUR
        val convertedBack = CurrencyExchangeRateService.convertToEur(usdAmount, AppCurrency.USD)
        assertEquals(eurAmount, convertedBack, 0.1)

        // Zero or negative amounts
        assertEquals(0.0, CurrencyExchangeRateService.convertFromEur(0.0, AppCurrency.USD), 0.001)
        assertEquals(-500.0 * CurrencyExchangeRateService.getRate(AppCurrency.USD), CurrencyExchangeRateService.convertFromEur(-500.0, AppCurrency.USD), 0.001)
    }
}
