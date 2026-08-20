package com.example.util

import com.example.data.Property
import com.example.data.PipelineStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalculationEnginesUnitTest {

    @Before
    fun setUp() {
        MarketEstimateService.clearCache()
    }

    // ==========================================
    // 1. PROPERTY OPPORTUNITY ENGINE TESTS
    // ==========================================

    @Test
    fun `test PropertyOpportunityEngine - un immobile sotto mercato riceve tier alto (ULTRA o HIGH)`() {
        // Immobile a Milano: Prezzo €150.000 per 100m² = €1.500/m²
        // Media mercato Milano = €5.380/m² -> Sottoquotato di oltre il 70%
        val underMarketProperty = Property(
            id = 101L,
            title = "Appartamento Sotto Mercato",
            address = "Via della Moscova 15, Milano",
            price = 150000.0,
            surfaceSqm = 100,
            estimatedRenovationCost = 20000.0,
            distressStatus = "Asta Giudiziaria",
            pipelineStatus = PipelineStatus.ANALYZED.key
        )

        val evaluation = PropertyOpportunityEngine.evaluateProperty(underMarketProperty)

        // Verifiche quantitative
        assertEquals("Milano", evaluation.location)
        assertEquals(1500.0, evaluation.propertyPricePerSqm, 0.01)
        assertEquals(538000.0, evaluation.scrapedMarketValue, 0.01)
        assertTrue("Undervalued percent deve essere > 60%", evaluation.undervaluedPercent > 60.0)
        assertTrue("Opportunity score deve essere alto (>= 65)", evaluation.opportunityScore >= 65)
        assertTrue("Tier deve essere ULTRA o HIGH", evaluation.tier == OpportunityTier.ULTRA || evaluation.tier == OpportunityTier.HIGH)
    }

    @Test
    fun `test PropertyOpportunityEngine - un immobile sopra mercato riceve tier LOW (AVOID)`() {
        // Immobile a Milano: Prezzo €900.000 per 100m² = €9.000/m²
        // Media mercato Milano = €5.380/m² -> Sopra mercato (sconto negativo / 0)
        val overMarketProperty = Property(
            id = 102L,
            title = "Appartamento Sopra Mercato",
            address = "Via Ripamonti 200, Milano",
            price = 900000.0,
            surfaceSqm = 100,
            estimatedRenovationCost = 50000.0,
            distressStatus = "Libero",
            pipelineStatus = PipelineStatus.ANALYZED.key
        )

        val evaluation = PropertyOpportunityEngine.evaluateProperty(overMarketProperty)

        // Verifiche quantitative
        assertEquals("Milano", evaluation.location)
        assertEquals(9000.0, evaluation.propertyPricePerSqm, 0.01)
        assertTrue("Undervalued percent deve essere <= 0", evaluation.undervaluedPercent <= 0.0)
        assertTrue("Opportunity score per immobile fuori prezzo deve essere < 50", evaluation.opportunityScore < 50)
        assertEquals(OpportunityTier.AVOID, evaluation.tier)
    }

    @Test
    fun `test PropertyOpportunityEngine - Milano Via Roma 12 NON viene associato al mercato di Roma`() {
        // Un indirizzo con "Via Roma" ma nella città di Milano deve estrarre "Milano" e non "Roma"
        val address = "Via Roma 12, Milano"
        val extractedCity = PropertyOpportunityEngine.extractLocationName(address)

        assertEquals("Milano", extractedCity)
        assertNotEquals("Roma", extractedCity)

        val prop = Property(
            id = 103L,
            title = "Immobile Milano Via Roma",
            address = address,
            price = 300000.0,
            surfaceSqm = 80
        )
        val eval = PropertyOpportunityEngine.evaluateProperty(prop)
        assertEquals("Milano", eval.location)
        assertEquals(5380.0, eval.liveMarketPricePerSqm, 0.01) // Media Milano, non Roma (che è 3450.0)
    }

    // ==========================================
    // 2. ITALIAN PROPERTY TAX ENGINE TESTS
    // ==========================================

    @Test
    fun `test ItalianPropertyTaxEngine - imposta di registro 2% Prima Casa vs 9% Seconda Casa`() {
        val purchasePrice = 200000.0
        val cadastralValue = 80000.0 // Valore catastale esplicito

        // 1. Prima Casa (2% su valore catastale, min 1000€ + 100€ fisse)
        val firstHomeBreakdown = ItalianPropertyTaxEngine.calculateAcquisitionCosts(
            purchasePrice = purchasePrice,
            cadastralValue = cadastralValue,
            acquisitionType = ItalianAcquisitionType.PRIVATE_FIRST_HOME,
            hasMortgage = false
        )
        // Imposta registro: 80.000 * 2% = 1.600€
        assertEquals(1600.0, firstHomeBreakdown.registrationOrVatTax, 0.01)
        assertEquals(100.0, firstHomeBreakdown.fixedRegistryIpoCatTaxes, 0.01)
        assertEquals(1700.0, firstHomeBreakdown.totalTaxes, 0.01)

        // 2. Seconda Casa (9% su valore catastale, min 1000€ + 100€ fisse)
        val secondHomeBreakdown = ItalianPropertyTaxEngine.calculateAcquisitionCosts(
            purchasePrice = purchasePrice,
            cadastralValue = cadastralValue,
            acquisitionType = ItalianAcquisitionType.PRIVATE_SECOND_HOME,
            hasMortgage = false
        )
        // Imposta registro: 80.000 * 9% = 7.200€
        assertEquals(7200.0, secondHomeBreakdown.registrationOrVatTax, 0.01)
        assertEquals(100.0, secondHomeBreakdown.fixedRegistryIpoCatTaxes, 0.01)
        assertEquals(7300.0, secondHomeBreakdown.totalTaxes, 0.01)
    }

    @Test
    fun `test ItalianPropertyTaxEngine - regimi di locazione calcolano imposta attesa su canone noto`() {
        val annualGrossRent = 12000.0 // 1.000€ al mese

        // Cedolare Secca 21% -> 12000 * 0.21 = 2520.0
        val tax21 = ItalianPropertyTaxEngine.calculateRentalTax(annualGrossRent, RentalTaxRegime.CEDOLARE_SECCA_21)
        assertEquals(2520.0, tax21, 0.01)

        // Cedolare Secca 10% (Canone Concordato) -> 12000 * 0.10 = 1200.0
        val tax10 = ItalianPropertyTaxEngine.calculateRentalTax(annualGrossRent, RentalTaxRegime.CEDOLARE_SECCA_10)
        assertEquals(1200.0, tax10, 0.01)

        // Regime Ordinario IRPEF (~28% su 95% del canone) -> (12000 * 0.95) * 0.28 = 3192.0
        val taxIrpef = ItalianPropertyTaxEngine.calculateRentalTax(annualGrossRent, RentalTaxRegime.REGIME_ORDINARIO_IRPEF)
        assertEquals(3192.0, taxIrpef, 0.01)

        // Esente / Lordo -> 0.0
        val taxZero = ItalianPropertyTaxEngine.calculateRentalTax(annualGrossRent, RentalTaxRegime.ESENTE_LORDO)
        assertEquals(0.0, taxZero, 0.001)
    }

    @Test
    fun `test ItalianPropertyTaxEngine - terzo immobile in affitto breve attiva regime d impresa`() {
        val annualRentPerUnit = 15000.0

        // 1° Immobile: Cedolare secca 21%, no regime impresa
        val unit1 = ItalianPropertyTaxEngine.calculateShortTermRentalTax(annualRentPerUnit, 1)
        assertFalse(unit1.isRegimeImpresa)
        assertEquals(21.0, unit1.appliedTaxRatePercent, 0.01)
        assertEquals(3150.0, unit1.totalTaxAmount, 0.01)

        // 2° Immobile: Cedolare secca 26%, no regime impresa
        val unit2 = ItalianPropertyTaxEngine.calculateShortTermRentalTax(annualRentPerUnit, 2)
        assertFalse(unit2.isRegimeImpresa)
        assertEquals(26.0, unit2.appliedTaxRatePercent, 0.01)
        assertEquals(3900.0, unit2.totalTaxAmount, 0.01)

        // 3° Immobile: Scatta il regime d'impresa
        val unit3 = ItalianPropertyTaxEngine.calculateShortTermRentalTax(annualRentPerUnit, 3)
        assertTrue("Dal 3° immobile in poi deve scattare il regime d'impresa", unit3.isRegimeImpresa)
        assertEquals(28.0, unit3.appliedTaxRatePercent, 0.01)
        // Tassazione impresa su 95% canone = 15000 * 0.95 * 0.28 = 3990.0
        assertEquals(3990.0, unit3.totalTaxAmount, 0.01)
    }

    // ==========================================
    // 3. MARKET ESTIMATE SERVICE TESTS
    // ==========================================

    @Test
    fun `test MarketEstimateService - senza API key o con fallback restituisce sourceUrl vuoto e isLiveScraped false`() {
        val kpi = MarketEstimateService.getCuratedProvinceKpi("Bologna")

        assertEquals("Bologna", kpi.locationName)
        assertEquals("BO", kpi.province)
        assertEquals("Emilia-Romagna", kpi.region)
        assertEquals(3450.0, kpi.avgSalePriceSqM!!, 0.01)
        assertEquals(17.80, kpi.avgRentPriceSqM!!, 0.01)
        assertEquals("", kpi.sourceUrl)
        assertFalse(kpi.isLiveScraped)
        assertTrue(kpi.usedFallbackData)
    }

    @Test
    fun `test MarketEstimateService - cache restituisce il valore memorizzato entro la finestra di validita`() = runBlocking {
        val customKpi = ProvinceScrapedKpi(
            locationName = "Torino",
            province = "TO",
            region = "Piemonte",
            avgSalePriceSqM = 2999.0,
            minSalePriceSqM = 1500.0,
            maxSalePriceSqM = 4000.0,
            saleTrendYoY = 3.0,
            avgRentPriceSqM = 12.0,
            rentTrendYoY = 4.0,
            grossRentalYield = 5.5,
            marketSaturationScore = 30,
            avgDaysOnMarket = 80,
            absorptionRatePercent = 75.0,
            sourceUrl = "https://cached-test.com",
            marketSummary = "Cached entry",
            isLiveScraped = true,
            scrapedAt = System.currentTimeMillis() // Appena inserito
        )

        // Inserimento in cache
        MarketEstimateService.putInCache("Torino", customKpi)

        // Richiesta scrapeMarketKpis per la stessa città
        val result = MarketEstimateService.scrapeMarketKpis("Torino")
        assertTrue(result.isSuccess)

        val retrieved = result.getOrNull()
        assertNotNull(retrieved)
        assertEquals(2999.0, retrieved!!.avgSalePriceSqM!!, 0.01)
        assertEquals("https://cached-test.com", retrieved.sourceUrl)
        assertEquals("Cached entry", retrieved.marketSummary)
        assertTrue(retrieved.isLiveScraped)
    }

    // ==========================================
    // 4. SENIOR VALUATION ENGINE TESTS
    // ==========================================

    @Test
    fun `test SeniorValuationEngine - calcolo superficie commerciale UNI 10750 e rettifiche peritali`() {
        val input = SeniorValuationEngine.ValuationInput(
            mainCoveredSqM = 100.0,
            balconyTerraceSqM = 20.0,   // 20 * 0.35 = 7 sqm
            cellarAtticSqM = 10.0,      // 10 * 0.25 = 2.5 sqm
            gardenSqM = 50.0,           // 50 * 0.10 = 5 sqm
            baseZonePricePerSqM = 3000.0,
            condition = SeniorValuationEngine.PropertyCondition.GOOD_CONDITION, // factor 1.00
            floorLevel = SeniorValuationEngine.FloorLevel.INTERMEDIATE,          // factor 1.00
            hasElevator = true,
            energyClass = SeniorValuationEngine.EnergyClass.CLASS_D_C,          // factor 1.00
            occupancyStatus = SeniorValuationEngine.OccupancyStatus.VACANT,     // factor 1.00
            hasGarageOrParking = true,                                          // factor 1.05
            hasPanoramicView = false,                                           // factor 1.00
            estimatedSanatoriaCost = 5000.0
        )

        val result = SeniorValuationEngine.calculateValuation(input)

        // Superficie commerciale: 100 + 7 + 2.5 + 5 = 114.5 m²
        assertEquals(114.5, result.commercialSqM, 0.01)

        // Prezzo al m² corretto: 3000 * 1.05 = 3150.0 €/m²
        assertEquals(3150.0, result.adjustedPricePerSqM, 0.1)

        // Valore lordo: 114.5 * 3150 = 360.675€ - 5.000€ sanatoria = 355.675€
        assertEquals(355675.0, result.totalEstimatedMarketValue, 1.0)

        // Forchetta +/- 7.5%
        assertEquals(355675.0 * 0.925, result.minFairValue, 1.0)
        assertEquals(355675.0 * 1.075, result.maxFairValue, 1.0)

        // Offerte consigliate (Sconto 20% e 30%)
        assertEquals(355675.0 * 0.80, result.recommendedAuctionBid20Margin, 1.0)
        assertEquals(355675.0 * 0.70, result.recommendedAuctionBid30Margin, 1.0)
    }

    @Test
    fun `test SeniorValuationEngine - sottrazione sanatoria con valore al limite zero`() {
        val input = SeniorValuationEngine.ValuationInput(
            mainCoveredSqM = 30.0,
            baseZonePricePerSqM = 1000.0,
            condition = SeniorValuationEngine.PropertyCondition.NEEDS_TOTAL_RENOVATION, // 0.75
            estimatedSanatoriaCost = 100000.0 // Supera il valore dell'immobile
        )

        val result = SeniorValuationEngine.calculateValuation(input)
        // Il valore stimato non deve mai essere negativo
        assertEquals(0.0, result.totalEstimatedMarketValue, 0.001)
        assertEquals(0.0, result.minFairValue, 0.001)
        assertEquals(0.0, result.maxFairValue, 0.001)
    }
}
