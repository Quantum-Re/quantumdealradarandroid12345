package com.example.util

import com.example.data.Property
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Data model containing all computed side-by-side financial metrics, deltas,
 * winner badges, and strategic verdict for two compared properties.
 */
data class PropertyComparisonReportData(
    val propertyA: Property,
    val propertyB: Property,
    val surfaceSqmA: Int,
    val surfaceSqmB: Int,
    val purchasePriceA: Double,
    val purchasePriceB: Double,
    val pricePerSqmA: Double,
    val pricePerSqmB: Double,
    val renovationCostA: Double,
    val renovationCostB: Double,
    val renoPerSqmA: Double,
    val renoPerSqmB: Double,
    val renoToPurchaseRatioA: Double,
    val renoToPurchaseRatioB: Double,
    val totalInvestedBasisA: Double,
    val totalInvestedBasisB: Double,
    val totalInvestedPerSqmA: Double,
    val totalInvestedPerSqmB: Double,
    val effectiveExitValueA: Double,
    val effectiveExitValueB: Double,
    val projectedGrossProfitA: Double,
    val projectedGrossProfitB: Double,
    val projectedRoiPercentA: Double,
    val projectedRoiPercentB: Double,
    val monthlyRentalIncomeA: Double,
    val monthlyRentalIncomeB: Double,
    val annualGrossRentalIncomeA: Double,
    val annualGrossRentalIncomeB: Double,
    val grossRentalYieldA: Double,
    val grossRentalYieldB: Double,
    val paybackPeriodYearsA: Double,
    val paybackPeriodYearsB: Double,

    // Deltas (A minus B)
    val deltaPurchasePrice: Double,
    val deltaPricePerSqm: Double,
    val deltaRenovationCost: Double,
    val deltaTotalInvested: Double,
    val deltaExitValue: Double,
    val deltaGrossProfit: Double,
    val deltaRoiPercent: Double,
    val deltaRentalMonthly: Double,
    val deltaGrossYield: Double,

    // Winners: "A", "B", or "TIE"
    val winnerEntryPricePerSqm: String,
    val winnerLowestTotalCapital: String,
    val winnerMaxProfit: String,
    val winnerMaxRoi: String,
    val winnerMaxRentalYield: String,
    val winnerLowerCapExRisk: String,

    // Strategic Verdict
    val verdictTitle: String,
    val verdictSummary: String,
    val keyTakeaways: List<String>
)

object PropertyComparisonReportCalculator {

    fun calculate(
        propertyA: Property,
        propertyB: Property,
        evalA: PropertyOpportunityEvaluation? = null,
        evalB: PropertyOpportunityEvaluation? = null
    ): PropertyComparisonReportData {
        val sqmA = if (propertyA.surfaceSqm > 0) propertyA.surfaceSqm else 80
        val sqmB = if (propertyB.surfaceSqm > 0) propertyB.surfaceSqm else 80

        val priceA = propertyA.price
        val priceB = propertyB.price

        val priceSqmA = if (sqmA > 0) priceA / sqmA else 0.0
        val priceSqmB = if (sqmB > 0) priceB / sqmB else 0.0

        val renoA = if (propertyA.actualRenovationCost > 0) propertyA.actualRenovationCost else propertyA.estimatedRenovationCost
        val renoB = if (propertyB.actualRenovationCost > 0) propertyB.actualRenovationCost else propertyB.estimatedRenovationCost

        val renoSqmA = if (sqmA > 0) renoA / sqmA else 0.0
        val renoSqmB = if (sqmB > 0) renoB / sqmB else 0.0

        val renoRatioA = if (priceA > 0) (renoA / priceA) * 100.0 else 0.0
        val renoRatioB = if (priceB > 0) (renoB / priceB) * 100.0 else 0.0

        val totalBasisA = priceA + renoA
        val totalBasisB = priceB + renoB

        val totalBasisSqmA = if (sqmA > 0) totalBasisA / sqmA else 0.0
        val totalBasisSqmB = if (sqmB > 0) totalBasisB / sqmB else 0.0

        val exitValA = evalA?.scrapedMarketValue ?: propertyA.effectiveExitValue
        val exitValB = evalB?.scrapedMarketValue ?: propertyB.effectiveExitValue

        val profitA = exitValA - totalBasisA
        val profitB = exitValB - totalBasisB

        val roiA = if (totalBasisA > 0) (profitA / totalBasisA) * 100.0 else 0.0
        val roiB = if (totalBasisB > 0) (profitB / totalBasisB) * 100.0 else 0.0

        val rentA = propertyA.projectedRentalIncome
        val rentB = propertyB.projectedRentalIncome

        val annualRentA = rentA * 12.0
        val annualRentB = rentB * 12.0

        val yieldA = if (totalBasisA > 0 && annualRentA > 0) {
            (annualRentA / totalBasisA) * 100.0
        } else if (evalA?.grossRentalYieldPotential != null && evalA.grossRentalYieldPotential > 0) {
            evalA.grossRentalYieldPotential
        } else if (priceA > 0 && annualRentA > 0) {
            (annualRentA / priceA) * 100.0
        } else 0.0

        val yieldB = if (totalBasisB > 0 && annualRentB > 0) {
            (annualRentB / totalBasisB) * 100.0
        } else if (evalB?.grossRentalYieldPotential != null && evalB.grossRentalYieldPotential > 0) {
            evalB.grossRentalYieldPotential
        } else if (priceB > 0 && annualRentB > 0) {
            (annualRentB / priceB) * 100.0
        } else 0.0

        val paybackA = if (annualRentA > 0) totalBasisA / annualRentA else 0.0
        val paybackB = if (annualRentB > 0) totalBasisB / annualRentB else 0.0

        // Deltas
        val deltaPrice = priceA - priceB
        val deltaPriceSqm = priceSqmA - priceSqmB
        val deltaReno = renoA - renoB
        val deltaInvested = totalBasisA - totalBasisB
        val deltaExit = exitValA - exitValB
        val deltaProfit = profitA - profitB
        val deltaRoi = roiA - roiB
        val deltaRent = rentA - rentB
        val deltaYield = yieldA - yieldB

        // Winners determination
        fun determineWinner(diff: Double, higherIsBetter: Boolean, threshold: Double = 0.01): String {
            return when {
                abs(diff) < threshold -> "TIE"
                higherIsBetter -> if (diff > 0) "A" else "B"
                else -> if (diff < 0) "A" else "B"
            }
        }

        val winnerEntryPricePerSqm = determineWinner(deltaPriceSqm, higherIsBetter = false)
        val winnerLowestTotalCapital = determineWinner(deltaInvested, higherIsBetter = false)
        val winnerMaxProfit = determineWinner(deltaProfit, higherIsBetter = true)
        val winnerMaxRoi = determineWinner(deltaRoi, higherIsBetter = true)
        val winnerMaxRentalYield = determineWinner(deltaYield, higherIsBetter = true)
        val winnerLowerCapExRisk = determineWinner(renoRatioA - renoRatioB, higherIsBetter = false)

        // Generate Comprehensive Verdict and Key Takeaways
        val titleA = propertyA.title.ifBlank { propertyA.address }
        val titleB = propertyB.title.ifBlank { propertyB.address }

        val takeaways = mutableListOf<String>()

        // Takeaway 1: Acquisition & Entry Basis
        if (winnerEntryPricePerSqm == "A") {
            val savingSqm = priceSqmB - priceSqmA
            takeaways.add("Vantaggio di Ingresso: $titleA offre un costo/m² inferiore di ${savingSqm.roundToInt()} €/m² rispetto a $titleB.")
        } else if (winnerEntryPricePerSqm == "B") {
            val savingSqm = priceSqmA - priceSqmB
            takeaways.add("Vantaggio di Ingresso: $titleB offre un costo/m² inferiore di ${savingSqm.roundToInt()} €/m² rispetto a $titleA.")
        } else {
            takeaways.add("Valutazione Ingresso: Entrambi gli immobili presentano un prezzo al m² equivalente (~${priceSqmA.roundToInt()} €/m²).")
        }

        // Takeaway 2: ROI & Capital Gain
        if (winnerMaxRoi == "A") {
            takeaways.add("Rendimento Capitale (Fix & Flip / Exit): $titleA primeggia con un ROI stimato del ${String.format(Locale.ITALY, "%.1f", roiA)}% (+${String.format(Locale.ITALY, "%.1f", deltaRoi)}% rispetto a $titleB).")
        } else if (winnerMaxRoi == "B") {
            takeaways.add("Rendimento Capitale (Fix & Flip / Exit): $titleB primeggia con un ROI stimato del ${String.format(Locale.ITALY, "%.1f", roiB)}% (+${String.format(Locale.ITALY, "%.1f", abs(deltaRoi))}% rispetto a $titleA).")
        } else {
            takeaways.add("Redditività Exit: Entrambi i cespiti generano un ROI equivalente (~${String.format(Locale.ITALY, "%.1f", roiA)}%).")
        }

        // Takeaway 3: Rental Cash Flow & Yield
        if (yieldA > 0 || yieldB > 0) {
            if (winnerMaxRentalYield == "A") {
                takeaways.add("Rendimento Locativo (Buy & Hold): $titleA genera una redditività lorda superiore (${String.format(Locale.ITALY, "%.1f", yieldA)}% vs ${String.format(Locale.ITALY, "%.1f", yieldB)}%).")
            } else if (winnerMaxRentalYield == "B") {
                takeaways.add("Rendimento Locativo (Buy & Hold): $titleB genera una redditività lorda superiore (${String.format(Locale.ITALY, "%.1f", yieldB)}% vs ${String.format(Locale.ITALY, "%.1f", yieldA)}%).")
            } else {
                takeaways.add("Rendimento da Affitto: Entrambi offrono un rendimento locativo allineato (~${String.format(Locale.ITALY, "%.1f", yieldA)}%).")
            }
        }

        // Takeaway 4: CapEx / Reno Risk
        if (renoRatioA > 30.0 || renoRatioB > 30.0) {
            val highRiskProp = if (renoRatioA > renoRatioB) titleA else titleB
            val ratio = if (renoRatioA > renoRatioB) renoRatioA else renoRatioB
            takeaways.add("Rischio Cantiere (CapEx): $highRiskProp assorbe un'incidenza lavori elevata (${ratio.roundToInt()}% del prezzo di acquisto), richiedendo una gestione attenta delle tempistiche e dei preventivi.")
        } else {
            takeaways.add("Struttura Lavori: Entrambe le operazioni mantengono un'incidenza di ristrutturazione contenuta sotto il 30% del capitale di acquisto.")
        }

        // Executive Verdict Synthesis
        val (verdictTitle, verdictSummary) = when {
            winnerMaxRoi == "A" && (winnerMaxProfit == "A" || winnerEntryPricePerSqm == "A") -> {
                Pair(
                    "Immobile A ($titleA) - Scelta Primaria ad Alto Rendimento",
                    "L'analisi comparativa evidenzia una netta superiorità di $titleA in termini di marginalità complessiva, plusvalenza potenziale e ritorno sul capitale investito (ROI). Consigliato come operazione primaria per valorizzazione attiva o trading rapido."
                )
            }
            winnerMaxRoi == "B" && (winnerMaxProfit == "B" || winnerEntryPricePerSqm == "B") -> {
                Pair(
                    "Immobile B ($titleB) - Scelta Primaria ad Alto Rendimento",
                    "L'analisi comparativa evidenzia una netta superiorità di $titleB in termini di marginalità complessiva, plusvalenza potenziale e ritorno sul capitale investito (ROI). Consigliato come operazione primaria per valorizzazione attiva o trading rapido."
                )
            }
            winnerMaxRentalYield == "A" && winnerLowestTotalCapital == "A" -> {
                Pair(
                    "Immobile A ($titleA) - Profilo Cash-Flow & Difensivo",
                    "$titleA rappresenta la soluzione più efficiente per una strategia Buy & Hold ad elevato rendimento da locazione con minor assorbimento di capitale iniziale."
                )
            }
            winnerMaxRentalYield == "B" && winnerLowestTotalCapital == "B" -> {
                Pair(
                    "Immobile B ($titleB) - Profilo Cash-Flow & Difensivo",
                    "$titleB rappresenta la soluzione più efficiente per una strategia Buy & Hold ad elevato rendimento da locazione con minor assorbimento di capitale iniziale."
                )
            }
            else -> {
                Pair(
                    "Confronto Bilanciato: Diversificazione Strategica",
                    "Le due opportunità presentano profili complementari: $titleA offre specifici vantaggi di ${if (winnerMaxRoi == "A") "ritorno sul capitale" else "flusso di cassa"}, mentre $titleB garantisce ${if (winnerLowestTotalCapital == "B") "un minor esborso iniziale" else "una marginalità di rivalutazione alternativa"}."
                )
            }
        }

        return PropertyComparisonReportData(
            propertyA = propertyA,
            propertyB = propertyB,
            surfaceSqmA = sqmA,
            surfaceSqmB = sqmB,
            purchasePriceA = priceA,
            purchasePriceB = priceB,
            pricePerSqmA = priceSqmA,
            pricePerSqmB = priceSqmB,
            renovationCostA = renoA,
            renovationCostB = renoB,
            renoPerSqmA = renoSqmA,
            renoPerSqmB = renoSqmB,
            renoToPurchaseRatioA = renoRatioA,
            renoToPurchaseRatioB = renoRatioB,
            totalInvestedBasisA = totalBasisA,
            totalInvestedBasisB = totalBasisB,
            totalInvestedPerSqmA = totalBasisSqmA,
            totalInvestedPerSqmB = totalBasisSqmB,
            effectiveExitValueA = exitValA,
            effectiveExitValueB = exitValB,
            projectedGrossProfitA = profitA,
            projectedGrossProfitB = profitB,
            projectedRoiPercentA = roiA,
            projectedRoiPercentB = roiB,
            monthlyRentalIncomeA = rentA,
            monthlyRentalIncomeB = rentB,
            annualGrossRentalIncomeA = annualRentA,
            annualGrossRentalIncomeB = annualRentB,
            grossRentalYieldA = yieldA,
            grossRentalYieldB = yieldB,
            paybackPeriodYearsA = paybackA,
            paybackPeriodYearsB = paybackB,
            deltaPurchasePrice = deltaPrice,
            deltaPricePerSqm = deltaPriceSqm,
            deltaRenovationCost = deltaReno,
            deltaTotalInvested = deltaInvested,
            deltaExitValue = deltaExit,
            deltaGrossProfit = deltaProfit,
            deltaRoiPercent = deltaRoi,
            deltaRentalMonthly = deltaRent,
            deltaGrossYield = deltaYield,
            winnerEntryPricePerSqm = winnerEntryPricePerSqm,
            winnerLowestTotalCapital = winnerLowestTotalCapital,
            winnerMaxProfit = winnerMaxProfit,
            winnerMaxRoi = winnerMaxRoi,
            winnerMaxRentalYield = winnerMaxRentalYield,
            winnerLowerCapExRisk = winnerLowerCapExRisk,
            verdictTitle = verdictTitle,
            verdictSummary = verdictSummary,
            keyTakeaways = takeaways
        )
    }
}
