package com.example.util

import kotlin.math.roundToInt

/**
 * Senior Real Estate Valuation Engine (Metodo Stima Peritale Senior / MCA UNI 10750)
 * Simulates a professional real estate appraiser with 20+ years of experience in the Italian market.
 */
object SeniorValuationEngine {

    enum class PropertyCondition(val label: String, val factor: Double) {
        NEEDS_TOTAL_RENOVATION("Da Ristrutturare Completamente", 0.75),
        HABITABLE_ORIGINAL("Abitabile / Stato Originale", 0.90),
        GOOD_CONDITION("Buono Stato / Parzialmente Ristrutturato", 1.00),
        RENOVATED_NEW("Ristrutturato a Nuovo", 1.15),
        LUXURY_FINISHES("Finiture di Lusso / Nuova Costruzione", 1.25)
    }

    enum class FloorLevel(val label: String, val factorWithElevator: Double, val factorNoElevator: Double) {
        BASEMENT("Seminterrato / Interrato", 0.75, 0.75),
        GROUND_FLOOR("Piano Terra", 0.85, 0.85),
        MEZZANINE("Piano Rialzato / Ammezzato", 0.90, 0.90),
        INTERMEDIATE("Piano Intermedio (1°-4°)", 1.00, 0.95),
        HIGH_FLOOR("Piano Alto (5°+)", 1.05, 0.85),
        PENTHOUSE("Attico / Ultimo Piano", 1.20, 0.90)
    }

    enum class EnergyClass(val label: String, val factor: Double) {
        CLASS_G("Classe G (Elevata Vetustà)", 0.90),
        CLASS_F_E("Classe F - E", 0.95),
        CLASS_D_C("Classe D - C", 1.00),
        CLASS_B_A1("Classe B - A1", 1.06),
        CLASS_A2_A4("Classe A2 - A4 (NZEB Green)", 1.12)
    }

    enum class OccupancyStatus(val label: String, val discountFactor: Double) {
        VACANT("Libero da Persone e Cose", 1.00),
        RENTED_REGULAR("Locato con Contratto Regolare a Rendita", 0.90),
        OCCUPIED_DEBTOR("Occupato dall'Esecutato / Proprietario", 0.85),
        OCCUPIED_NO_TITLE("Occupato Senza Titolo / Contratto Non Opponibile", 0.75)
    }

    data class ValuationInput(
        val mainCoveredSqM: Double,
        val balconyTerraceSqM: Double = 0.0,
        val cellarAtticSqM: Double = 0.0,
        val gardenSqM: Double = 0.0,
        val baseZonePricePerSqM: Double,
        val condition: PropertyCondition = PropertyCondition.GOOD_CONDITION,
        val floorLevel: FloorLevel = FloorLevel.INTERMEDIATE,
        val hasElevator: Boolean = true,
        val energyClass: EnergyClass = EnergyClass.CLASS_D_C,
        val occupancyStatus: OccupancyStatus = OccupancyStatus.VACANT,
        val hasGarageOrParking: Boolean = false,
        val hasPanoramicView: Boolean = false,
        val estimatedSanatoriaCost: Double = 0.0
    )

    data class ValuationResult(
        val commercialSqM: Double,
        val baseMarketValueSqM: Double,
        val adjustedPricePerSqM: Double,
        val totalEstimatedMarketValue: Double,
        val minFairValue: Double,
        val maxFairValue: Double,
        val recommendedAuctionBid20Margin: Double,
        val recommendedAuctionBid30Margin: Double,
        val totalCorrectionPercentage: Double,
        val breakdownFactors: List<Pair<String, String>>,
        val appraisalSummary: String
    )

    fun calculateValuation(input: ValuationInput): ValuationResult {
        // 1. Calculate Commercial Surface Area (UNI 10750)
        val commercialSqM = input.mainCoveredSqM +
                (input.balconyTerraceSqM * 0.35) +
                (input.cellarAtticSqM * 0.25) +
                (input.gardenSqM * 0.10)

        // 2. Determine Micro-location & Building Correction Factors
        val conditionFactor = input.condition.factor
        val floorFactor = if (input.hasElevator) input.floorLevel.factorWithElevator else input.floorLevel.factorNoElevator
        val energyFactor = input.energyClass.factor
        val garageFactor = if (input.hasGarageOrParking) 1.05 else 1.00
        val viewFactor = if (input.hasPanoramicView) 1.05 else 1.00
        val occupancyFactor = input.occupancyStatus.discountFactor

        // Combined Multiplicative Coefficient
        val totalCombinedFactor = conditionFactor * floorFactor * energyFactor * garageFactor * viewFactor * occupancyFactor
        val totalCorrectionPercentage = ((totalCombinedFactor - 1.0) * 1000).roundToInt() / 10.0

        // 3. Adjusted Price per SqM
        val adjustedPricePerSqM = input.baseZonePricePerSqM * totalCombinedFactor

        // 4. Gross Market Value minus Sanatoria/Irregularities Costs
        val rawMarketValue = commercialSqM * adjustedPricePerSqM
        val totalEstimatedMarketValue = (rawMarketValue - input.estimatedSanatoriaCost).coerceAtLeast(0.0)

        // 5. Value Range (+/- 7.5% peritale confidence interval)
        val minFairValue = totalEstimatedMarketValue * 0.925
        val maxFairValue = totalEstimatedMarketValue * 1.075

        // 6. Recommended Auction/Distressed Purchase Target (20% and 30% Net Investor Margin)
        val recommendedAuctionBid20Margin = totalEstimatedMarketValue * 0.80
        val recommendedAuctionBid30Margin = totalEstimatedMarketValue * 0.70

        // Breakdown for transparent expert report
        val breakdown = listOf(
            "Superficie Commerciale Ragguagliata" to "${(commercialSqM * 10).roundToInt() / 10.0} m²",
            "Stato Conservativo" to "${input.condition.label} (${formatFactor(conditionFactor)})",
            "Piano ed Ascensore" to "${input.floorLevel.label} (${formatFactor(floorFactor)})",
            "Prestazione Energetica" to "${input.energyClass.label} (${formatFactor(energyFactor)})",
            "Stato Occupazionale" to "${input.occupancyStatus.label} (${formatFactor(occupancyFactor)})",
            "Pertinenze & Vista" to "Garage: ${if (input.hasGarageOrParking) "+5%" else "Assente"}, Vista: ${if (input.hasPanoramicView) "+5%" else "Standard"}",
            "Detrazione Difformità / Sanatorie" to if (input.estimatedSanatoriaCost > 0) "-€${input.estimatedSanatoriaCost.toInt()}" else "Nessuna rilevata",
            "Variazione Complessiva Peritale" to "${if (totalCorrectionPercentage >= 0) "+" else ""}$totalCorrectionPercentage%"
        )

        val summary = """
            PROSPETTO DI STIMA PERITALE SENIOR (Metodo MCA)
            ------------------------------------------------
            Superficie Commerciale: ${(commercialSqM * 10).roundToInt() / 10.0} m²
            Valore Unitario Corretto: €${adjustedPricePerSqM.roundToInt()}/m²
            Valore di Mercato Stimato: €${totalEstimatedMarketValue.roundToInt()}
            Forchetta di Valore Appropriato: €${minFairValue.roundToInt()} - €${maxFairValue.roundToInt()}
            
            Target Offerta Consigliata (Sconto 20%): €${recommendedAuctionBid20Margin.roundToInt()}
            Target Offerta Opportunistica (Sconto 30%): €${recommendedAuctionBid30Margin.roundToInt()}
        """.trimIndent()

        return ValuationResult(
            commercialSqM = (commercialSqM * 10).roundToInt() / 10.0,
            baseMarketValueSqM = input.baseZonePricePerSqM,
            adjustedPricePerSqM = (adjustedPricePerSqM * 10).roundToInt() / 10.0,
            totalEstimatedMarketValue = totalEstimatedMarketValue,
            minFairValue = minFairValue,
            maxFairValue = maxFairValue,
            recommendedAuctionBid20Margin = recommendedAuctionBid20Margin,
            recommendedAuctionBid30Margin = recommendedAuctionBid30Margin,
            totalCorrectionPercentage = totalCorrectionPercentage,
            breakdownFactors = breakdown,
            appraisalSummary = summary
        )
    }

    enum class TaxRegime(val label: String, val taxRate: Double) {
        CEDOLARE_SECCA_CONCORDATO("Cedolare Secca Agevolata (10%)", 0.10),
        CEDOLARE_SECCA_LIBERO("Cedolare Secca Ordinaria (21%)", 0.21),
        ORDINARIA_IRPEF_SOCIETA("Ordinario IRPEF / Società (28%)", 0.28)
    }

    data class AdvancedUnderwritingInput(
        val purchasePrice: Double,
        val renovationCost: Double,
        val estimatedMonthlyRent: Double,
        val resaleTargetPrice: Double,
        val holdingPeriodYears: Int = 5,
        val mortgageLtvPercent: Double = 70.0,
        val mortgageInterestRatePercent: Double = 3.65,
        val mortgageDurationYears: Int = 20,
        val taxRegime: TaxRegime = TaxRegime.CEDOLARE_SECCA_CONCORDATO,
        val annualImuTaxes: Double = 650.0,
        val annualCondoExpenses: Double = 900.0,
        val vacancyRatePercent: Double = 5.0,
        val propertyAppreciationPerYearPercent: Double = 2.0
    )

    data class StressTestCase(
        val name: String,
        val scenarioDescription: String,
        val netAnnualCashFlow: Double,
        val leveredCashOnCashPercent: Double,
        val fiveYearIrrPercent: Double,
        val flipNetProfit: Double,
        val flipRoiPercent: Double,
        val isViable: Boolean
    )

    data class AdvancedUnderwritingResult(
        val totalInitialOutlay: Double,
        val equityInvested: Double,
        val borrowedDebt: Double,
        val monthlyMortgagePayment: Double,
        val annualDebtService: Double,
        val grossAnnualRent: Double,
        val effectiveGrossRent: Double, // adjusted for vacancy
        val totalAnnualOperatingExpenses: Double,
        val netOperatingIncome: Double, // NOI
        val annualPreTaxCashFlow: Double,
        val annualIncomeTaxes: Double,
        val annualNetCashFlow: Double,
        val monthlyNetCashFlow: Double,
        val unleveredCashOnCashPercent: Double,
        val leveredCashOnCashPercent: Double,
        val grossRentalYieldPercent: Double,
        val capRatePercent: Double,
        val dscr: Double?, // Debt Service Coverage Ratio; null = nessun debito da coprire (non indefinibile a 99)
        val breakEvenOccupancyPercent: Double,
        val fiveYearIrrPercent: Double,
        val tenYearIrrPercent: Double,
        val flipNetProfit: Double,
        val flipRoiPercent: Double,
        val stressTests: List<StressTestCase>
    )

    /**
     * Compute institutional-grade underwriting metrics for real estate investors.
     * Restituisce null se prezzo d'acquisto, costo di ristrutturazione o canone mensile
     * mancano o non sono validi: senza queste tre grandezze non si può produrre una
     * perizia, e restituire un valore di ripiego produrrebbe una perizia falsa.
     * Un costo di ristrutturazione pari a zero è legittimo (nessuna ristrutturazione);
     * solo un valore negativo è invalido.
     */
    fun performAdvancedUnderwriting(input: AdvancedUnderwritingInput): AdvancedUnderwritingResult? {
        if (input.purchasePrice <= 0 || input.renovationCost < 0 || input.estimatedMonthlyRent <= 0) {
            return null
        }

        val safePurchase = input.purchasePrice
        val safeRenov = input.renovationCost
        val safeRentMonthly = input.estimatedMonthlyRent

        val acquisitionClosingCosts = safePurchase * 0.05 // 5% legal, taxes, notary, broker
        val totalInitialOutlay = safePurchase + safeRenov + acquisitionClosingCosts

        val borrowedDebt = safePurchase * (input.mortgageLtvPercent / 100.0)
        val equityInvested = totalInitialOutlay - borrowedDebt

        // Mortgage Calculation (French Amortization)
        val monthlyRate = (input.mortgageInterestRatePercent / 100.0) / 12.0
        val totalPayments = input.mortgageDurationYears * 12
        val monthlyMortgagePayment = if (borrowedDebt > 0 && monthlyRate > 0 && totalPayments > 0) {
            val factor = Math.pow(1.0 + monthlyRate, totalPayments.toDouble())
            (borrowedDebt * (monthlyRate * factor) / (factor - 1.0))
        } else {
            0.0
        }
        val annualDebtService = monthlyMortgagePayment * 12.0

        // Rental Income Operations
        val grossAnnualRent = safeRentMonthly * 12.0
        val vacancyLoss = grossAnnualRent * (input.vacancyRatePercent / 100.0)
        val effectiveGrossRent = (grossAnnualRent - vacancyLoss).coerceAtLeast(0.0)

        val totalAnnualOperatingExpenses = input.annualImuTaxes + input.annualCondoExpenses + (grossAnnualRent * 0.04) // 4% maintenance reserve
        val netOperatingIncome = (effectiveGrossRent - totalAnnualOperatingExpenses).coerceAtLeast(0.0)

        val annualPreTaxCashFlow = netOperatingIncome - annualDebtService
        val taxableIncome = (effectiveGrossRent * 0.95).coerceAtLeast(0.0) // 5% flat deduction or direct regime
        val annualIncomeTaxes = taxableIncome * input.taxRegime.taxRate
        val annualNetCashFlow = annualPreTaxCashFlow - annualIncomeTaxes
        val monthlyNetCashFlow = annualNetCashFlow / 12.0

        // Ratios
        val grossRentalYieldPercent = (grossAnnualRent / safePurchase) * 100.0
        val capRatePercent = if (totalInitialOutlay > 0) (netOperatingIncome / totalInitialOutlay) * 100.0 else 0.0
        val unleveredCashOnCashPercent = if (totalInitialOutlay > 0) ((netOperatingIncome - annualIncomeTaxes) / totalInitialOutlay) * 100.0 else 0.0
        val leveredCashOnCashPercent = if (equityInvested > 0) (annualNetCashFlow / equityInvested) * 100.0 else 0.0

        // DSCR indefinito senza debito: non è "eccellente" (99), è semplicemente non applicabile.
        val dscr = if (annualDebtService > 0) netOperatingIncome / annualDebtService else null
        val breakEvenOccupancyPercent = if (grossAnnualRent > 0) ((totalAnnualOperatingExpenses + annualDebtService) / grossAnnualRent) * 100.0 else 0.0

        // Fix & Flip metrics
        val resale = if (input.resaleTargetPrice > 0) input.resaleTargetPrice else (safePurchase * 1.35)
        val flipSellingCosts = resale * 0.04 // 4% agency & transaction
        val flipGrossProfit = resale - totalInitialOutlay - flipSellingCosts
        val flipTaxes = (flipGrossProfit.coerceAtLeast(0.0) * 0.26) // 26% capital gain tax if sold within 5 years
        val flipNetProfit = flipGrossProfit - flipTaxes
        val flipRoiPercent = if (totalInitialOutlay > 0) (flipNetProfit / totalInitialOutlay) * 100.0 else 0.0

        // 5-Year and 10-Year Approximate IRR
        val futureValue5Y = safePurchase * Math.pow(1.0 + (input.propertyAppreciationPerYearPercent / 100.0), 5.0)
        val loanBalance5Y = borrowedDebt * 0.82 // approx amortized balance after 5y
        val equityAtExit5Y = futureValue5Y - loanBalance5Y
        val totalNetCashFlows5Y = (annualNetCashFlow * 5) + equityAtExit5Y
        val fiveYearIrrPercent = if (equityInvested > 0) {
            ((Math.pow(totalNetCashFlows5Y / equityInvested, 1.0 / 5.0) - 1.0) * 100.0).coerceIn(-30.0, 99.0)
        } else 0.0

        val tenYearIrrPercent = (fiveYearIrrPercent * 0.92).coerceIn(-30.0, 99.0)

        // Generate 3 Stress Test Scenarios
        val stressWorst = runStressScenario(
            title = "🔴 Scenario Stress Test (Mercato Orso)",
            desc = "Prezzi rivendita -10%, Lavori +20%, Sfitti 2 mesi/anno, Tassi +1.5%",
            baseInput = input,
            priceMult = 0.90,
            renoMult = 1.20,
            vacancyPct = 16.6,
            rateDelta = 1.5
        )

        val stressBase = runStressScenario(
            title = "🟡 Scenario Base (Immobiliare.it Benchmark)",
            desc = "Parametri di mercato standard allineati alle quotazioni OMI di zona",
            baseInput = input,
            priceMult = 1.00,
            renoMult = 1.00,
            vacancyPct = input.vacancyRatePercent,
            rateDelta = 0.0
        )

        val stressBest = runStressScenario(
            title = "🟢 Scenario Bull (Ottimizzazione Target)",
            desc = "Rivendita al massimo di zona (+8%), Lavori nei tempi, Sfitti minimi (0%)",
            baseInput = input,
            priceMult = 1.08,
            renoMult = 0.95,
            vacancyPct = 0.0,
            rateDelta = -0.3
        )

        return AdvancedUnderwritingResult(
            totalInitialOutlay = totalInitialOutlay,
            equityInvested = equityInvested,
            borrowedDebt = borrowedDebt,
            monthlyMortgagePayment = monthlyMortgagePayment,
            annualDebtService = annualDebtService,
            grossAnnualRent = grossAnnualRent,
            effectiveGrossRent = effectiveGrossRent,
            totalAnnualOperatingExpenses = totalAnnualOperatingExpenses,
            netOperatingIncome = netOperatingIncome,
            annualPreTaxCashFlow = annualPreTaxCashFlow,
            annualIncomeTaxes = annualIncomeTaxes,
            annualNetCashFlow = annualNetCashFlow,
            monthlyNetCashFlow = monthlyNetCashFlow,
            unleveredCashOnCashPercent = unleveredCashOnCashPercent,
            leveredCashOnCashPercent = leveredCashOnCashPercent,
            grossRentalYieldPercent = grossRentalYieldPercent,
            capRatePercent = capRatePercent,
            dscr = dscr,
            breakEvenOccupancyPercent = breakEvenOccupancyPercent,
            fiveYearIrrPercent = fiveYearIrrPercent,
            tenYearIrrPercent = tenYearIrrPercent,
            flipNetProfit = flipNetProfit,
            flipRoiPercent = flipRoiPercent,
            stressTests = listOf(stressWorst, stressBase, stressBest)
        )
    }

    private fun runStressScenario(
        title: String,
        desc: String,
        baseInput: AdvancedUnderwritingInput,
        priceMult: Double,
        renoMult: Double,
        vacancyPct: Double,
        rateDelta: Double
    ): StressTestCase {
        val stressedResale = (if (baseInput.resaleTargetPrice > 0) baseInput.resaleTargetPrice else baseInput.purchasePrice * 1.35) * priceMult
        val stressedReno = baseInput.renovationCost * renoMult
        val stressedOutlay = baseInput.purchasePrice + stressedReno + (baseInput.purchasePrice * 0.05)

        val grossRent = baseInput.estimatedMonthlyRent * 12.0
        val effRent = (grossRent * (1.0 - (vacancyPct / 100.0))).coerceAtLeast(0.0)
        val opex = baseInput.annualImuTaxes + baseInput.annualCondoExpenses + (grossRent * 0.04)
        val noi = (effRent - opex).coerceAtLeast(0.0)

        val borrowed = baseInput.purchasePrice * (baseInput.mortgageLtvPercent / 100.0)
        val equity = stressedOutlay - borrowed
        val stressedRate = ((baseInput.mortgageInterestRatePercent + rateDelta) / 100.0) / 12.0
        val n = baseInput.mortgageDurationYears * 12
        val monthlyPay = if (borrowed > 0 && stressedRate > 0) {
            val factor = Math.pow(1.0 + stressedRate, n.toDouble())
            borrowed * (stressedRate * factor) / (factor - 1.0)
        } else 0.0
        val debtService = monthlyPay * 12.0

        val preTaxCash = noi - debtService
        val taxes = (effRent * 0.95 * baseInput.taxRegime.taxRate)
        val netCashFlow = preTaxCash - taxes
        val coc = if (equity > 0) (netCashFlow / equity) * 100.0 else 0.0

        val flipSelling = stressedResale * 0.04
        val flipProfitGross = stressedResale - stressedOutlay - flipSelling
        val flipProfitNet = flipProfitGross - (flipProfitGross.coerceAtLeast(0.0) * 0.26)
        val flipRoi = if (stressedOutlay > 0) (flipProfitNet / stressedOutlay) * 100.0 else 0.0

        val irrApprox = (coc * 0.7) + (flipRoi * 0.3)

        return StressTestCase(
            name = title,
            scenarioDescription = desc,
            netAnnualCashFlow = netCashFlow,
            leveredCashOnCashPercent = coc,
            fiveYearIrrPercent = irrApprox,
            flipNetProfit = flipProfitNet,
            flipRoiPercent = flipRoi,
            isViable = netCashFlow >= 0 || flipProfitNet >= 0
        )
    }

    private fun formatFactor(factor: Double): String {
        val pct = ((factor - 1.0) * 100).roundToInt()
        return if (pct >= 0) "+$pct%" else "$pct%"
    }
}
