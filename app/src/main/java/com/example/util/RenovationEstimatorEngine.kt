package com.example.util

import java.io.Serializable
import java.util.Locale

/**
 * Livello di finitura / capitolato per la ristrutturazione.
 */
enum class RenovationQualityLevel(
    val label: String,
    val description: String,
    val costMultiplier: Double,
    val estimatedAppreciationBoostPercent: Double
) {
    ECONOMY(
        label = "Standard / Economico",
        description = "Materiali base, pavimenti in laminato/gres standard, sanitari tradizionali, verniciatura base",
        costMultiplier = 0.85,
        estimatedAppreciationBoostPercent = 12.0
    ),
    MEDIUM_PREMIUM(
        label = "Medio / Moderno",
        description = "Parquet/gres porcellanato prima scelta, sanitari a filo muro, luci LED integrate, predisposizione AC",
        costMultiplier = 1.00,
        estimatedAppreciationBoostPercent = 22.0
    ),
    HIGH_LUXURY(
        label = "Alto / Luxury",
        description = "Domotica BTicino/KNX, climatizzazione canalizzata, resine, boiserie, marmi pregiati, design custom",
        costMultiplier = 1.45,
        estimatedAppreciationBoostPercent = 35.0
    )
}

/**
 * Singola voce del computo metrico estimativo.
 */
data class RenovationBudgetItem(
    val id: String,
    val categoryName: String,
    val description: String,
    val unit: String, // es. "m²", "a corpo", "cad", "punti"
    val baseQuantity: Double,
    val baseUnitPrice: Double,
    val isEnabled: Boolean = true,
    val isTaxDeductible: Boolean = true,
    val taxDeductionRate: Double = 0.50 // 50% Bonus Ristrutturazioni Casa
) : Serializable {
    val totalGrossCost: Double
        get() = if (isEnabled) baseQuantity * baseUnitPrice else 0.0

    val totalDeductibleAmount: Double
        get() = if (isEnabled && isTaxDeductible) totalGrossCost * taxDeductionRate else 0.0
}

/**
 * Dati di input per il simulatore di ristrutturazione.
 */
data class RenovationSimulatorInput(
    val propertyTitle: String = "Immobile Target",
    val propertyAddress: String = "Milano (MI)",
    val surfaceSqm: Double = 75.0,
    val acquisitionPrice: Double = 180000.0,
    val currentMarketValuePerSqm: Double = 2800.0,
    val qualityLevel: RenovationQualityLevel = RenovationQualityLevel.MEDIUM_PREMIUM,
    val hasBonusRistrutturazioni50: Boolean = true,
    val hasEcobonus65: Boolean = true,
    val contingencyBufferPercent: Double = 10.0, // 10% imprevisti cantiere
    val contractorSafetyCostsPercent: Double = 5.0, // 5% sicurezza cantiere & oneri tecnici
    val items: List<RenovationBudgetItem> = emptyList()
) : Serializable

/**
 * Output del calcolo del computo metrico e analisi finanziaria Flip vs Buy&Hold.
 */
data class RenovationSimulatorResult(
    val surfaceSqm: Double,
    val acquisitionPrice: Double,
    val qualityLevel: RenovationQualityLevel,
    val itemsBreakdown: List<RenovationBudgetItem>,
    val subtotalDirectWorks: Double,
    val contingencyBufferCost: Double,
    val contractorAndTechFeesCost: Double,
    val totalGrossRenovationCost: Double,
    val grossCostPerSqm: Double,
    // Detrazioni Fiscali
    val totalTaxDeduction10Years: Double,
    val yearlyTaxDeductionDeducted: Double,
    val netEffectiveRenovationCost: Double,
    val netEffectiveCostPerSqm: Double,
    // Finanziario & Strategie
    val totalCapitalInvestedGross: Double, // Prezzo + Ristrutturazione Lorda
    val totalCapitalInvestedNet: Double,   // Prezzo + Ristrutturazione Netta
    // Stima Post-Lavori (ARV)
    val estimatedPostRenoValuePerSqm: Double,
    val estimatedArvMarketValue: Double,
    val capitalGainFlipGross: Double,
    val roiFlipGrossPercent: Double,
    val capitalGainFlipNet: Double, // Con detrazioni recuperate
    val roiFlipNetPercent: Double,
    // Scenario Messa a Reddito (Buy & Hold)
    val estimatedMonthlyRentPostReno: Double,
    val annualGrossRentPostReno: Double,
    val grossRentalYieldPostRenoPercent: Double,
    val netRentalYieldPostRenoPercent: Double
) : Serializable

object RenovationEstimatorEngine {

    /**
     * Genera le voci del computo metrico parametrico basate sui mq dell'immobile.
     */
    fun generateDefaultMetricComputation(
        surfaceSqm: Double,
        quality: RenovationQualityLevel = RenovationQualityLevel.MEDIUM_PREMIUM
    ): List<RenovationBudgetItem> {
        val sqm = surfaceSqm.coerceAtLeast(15.0)
        val bathroomsCount = if (sqm > 85) 2 else 1
        val roomsCount = (sqm / 25).toInt().coerceAtLeast(2)
        val windowsCount = (sqm / 15).toInt().coerceIn(3, 10)
        val multiplier = quality.costMultiplier

        return listOf(
            RenovationBudgetItem(
                id = "demolitions",
                categoryName = "Demolizioni & Smaltimento",
                description = "Rimozione tramezzi, pavimenti, battiscopa, sanitari e trasporto macerie a discarica autorizzata",
                unit = "m²",
                baseQuantity = sqm,
                baseUnitPrice = 38.0 * multiplier,
                isEnabled = true,
                isTaxDeductible = true,
                taxDeductionRate = 0.50
            ),
            RenovationBudgetItem(
                id = "masonry",
                categoryName = "Opere Murarie & Intonaci",
                description = "Nuove partizioni forati/cartongesso acustico, rasatura pareti a gesso e stuccature",
                unit = "m² pareti",
                baseQuantity = sqm * 2.8,
                baseUnitPrice = 24.0 * multiplier,
                isEnabled = true,
                isTaxDeductible = true,
                taxDeductionRate = 0.50
            ),
            RenovationBudgetItem(
                id = "electrical",
                categoryName = "Impianto Elettrico & Smart Home",
                description = "Quadro generale salvavita, certificazione DiRi/DiCo, serie civile e predisposizione rete dati RJ45",
                unit = "punti luce",
                baseQuantity = (roomsCount * 14.0 + 15.0).coerceAtLeast(40.0),
                baseUnitPrice = 42.0 * multiplier,
                isEnabled = true,
                isTaxDeductible = true,
                taxDeductionRate = 0.50
            ),
            RenovationBudgetItem(
                id = "plumbing",
                categoryName = "Impianto Idrico-Sanitario",
                description = "Nuovo collettore, tubazioni multistrato, scarichi fonoassorbenti per bagni e cucina",
                unit = "punti acqua",
                baseQuantity = (bathroomsCount * 6.0 + 4.0),
                baseUnitPrice = 175.0 * multiplier,
                isEnabled = true,
                isTaxDeductible = true,
                taxDeductionRate = 0.50
            ),
            RenovationBudgetItem(
                id = "heating_cooling",
                categoryName = "Climatizzazione & Riscaldamento",
                description = "Impianto a pompa di calore dual/trial split inverter A+++ con detrazione Ecobonus",
                unit = "a corpo",
                baseQuantity = 1.0,
                baseUnitPrice = (2800.0 + (roomsCount * 650.0)) * multiplier,
                isEnabled = true,
                isTaxDeductible = true,
                taxDeductionRate = 0.65 // Ecobonus 65%
            ),
            RenovationBudgetItem(
                id = "flooring",
                categoryName = "Pavimenti & Rivestimenti",
                description = "Posa massetto autolivellante, gres porcellanato rettificato grande formato o parquet prefinito",
                unit = "m²",
                baseQuantity = sqm,
                baseUnitPrice = 55.0 * multiplier,
                isEnabled = true,
                isTaxDeductible = true,
                taxDeductionRate = 0.50
            ),
            RenovationBudgetItem(
                id = "bathroom_fixtures",
                categoryName = "Sanitari & Rubinetterie",
                description = "Box doccia cristallo 8mm, piatto doccia ultrapiatto, sanitari sospesi con sedile soft-close",
                unit = "bagni",
                baseQuantity = bathroomsCount.toDouble(),
                baseUnitPrice = 2400.0 * multiplier,
                isEnabled = true,
                isTaxDeductible = true,
                taxDeductionRate = 0.50
            ),
            RenovationBudgetItem(
                id = "windows_fixtures",
                categoryName = "Infissi PVC/Alluminio & Cassonetti",
                description = "Serramenti basso emissivo triplo vetro, isolamento termico cassonetti e zanzariere",
                unit = "infissi",
                baseQuantity = windowsCount.toDouble(),
                baseUnitPrice = 750.0 * multiplier,
                isEnabled = true,
                isTaxDeductible = true,
                taxDeductionRate = 0.50 // o 65% Ecobonus
            ),
            RenovationBudgetItem(
                id = "doors",
                categoryName = "Porte Interne & Blindato",
                description = "Porte a battente/scorrevoli tamburate laccate opaco e porta blindata Classe 3 con defender",
                unit = "a corpo",
                baseQuantity = 1.0,
                baseUnitPrice = (roomsCount * 380.0 + 1300.0) * multiplier,
                isEnabled = true,
                isTaxDeductible = true,
                taxDeductionRate = 0.50
            ),
            RenovationBudgetItem(
                id = "painting",
                categoryName = "Tinteggiatura & Finiture",
                description = "Fissativo isolante, doppia mano idropittura traspirante antimuffa su pareti e soffitti",
                unit = "m²",
                baseQuantity = sqm * 3.0,
                baseUnitPrice = 11.0 * multiplier,
                isEnabled = true,
                isTaxDeductible = true,
                taxDeductionRate = 0.50
            )
        )
    }

    /**
     * Esegue il calcolo completo del simulatore di ristrutturazione, computo metrico e scenari Flip / Buy&Hold.
     */
    fun calculate(input: RenovationSimulatorInput): RenovationSimulatorResult {
        val effectiveItems = if (input.items.isEmpty()) {
            generateDefaultMetricComputation(input.surfaceSqm, input.qualityLevel)
        } else {
            input.items
        }

        // Subtotale opere dirette
        val subtotalDirect = effectiveItems.filter { it.isEnabled }.sumOf { it.totalGrossCost }

        // Buffer imprevisti e oneri tecnici
        val contingencyCost = subtotalDirect * (input.contingencyBufferPercent / 100.0)
        val contractorFeesCost = subtotalDirect * (input.contractorSafetyCostsPercent / 100.0)
        val totalGrossRenovation = subtotalDirect + contingencyCost + contractorFeesCost
        val grossCostPerSqm = if (input.surfaceSqm > 0) totalGrossRenovation / input.surfaceSqm else 0.0

        // Calcolo Detrazioni Fiscali (Recuperabili in 10 quote annuali IRPEF)
        val totalTaxDeduction10Years = effectiveItems.filter { it.isEnabled && it.isTaxDeductible }.sumOf { item ->
            val rate = if (item.taxDeductionRate == 0.65 && !input.hasEcobonus65) 0.50 else item.taxDeductionRate
            item.totalGrossCost * if (input.hasBonusRistrutturazioni50) rate else 0.0
        }
        val yearlyDeduction = totalTaxDeduction10Years / 10.0
        val netEffectiveRenovation = (totalGrossRenovation - totalTaxDeduction10Years).coerceAtLeast(0.0)
        val netCostPerSqm = if (input.surfaceSqm > 0) netEffectiveRenovation / input.surfaceSqm else 0.0

        // Capitali complessivi
        val totalInvestedGross = input.acquisitionPrice + totalGrossRenovation
        val totalInvestedNet = input.acquisitionPrice + netEffectiveRenovation

        // Calcolo ARV (After Repair Value)
        val basePricePerSqm = if (input.currentMarketValuePerSqm > 0) {
            input.currentMarketValuePerSqm
        } else if (input.surfaceSqm > 0) {
            (input.acquisitionPrice / input.surfaceSqm) * 1.15
        } else {
            3000.0
        }

        val appreciationPercent = input.qualityLevel.estimatedAppreciationBoostPercent
        val postRenoPricePerSqm = basePricePerSqm * (1.0 + (appreciationPercent / 100.0))
        val estimatedArv = postRenoPricePerSqm * input.surfaceSqm

        // Flip Strategy Profitability
        val capitalGainGross = estimatedArv - totalInvestedGross
        val roiFlipGross = if (totalInvestedGross > 0) (capitalGainGross / totalInvestedGross) * 100.0 else 0.0

        val capitalGainNet = estimatedArv - totalInvestedNet
        val roiFlipNet = if (totalInvestedNet > 0) (capitalGainNet / totalInvestedNet) * 100.0 else 0.0

        // Buy & Hold Scenario
        // Rendimento canone mensile post ristrutturazione stimato al ~5.5% - 7.5% su valore ARV
        val estimatedMonthlyRent = (estimatedArv * 0.062) / 12.0
        val annualGrossRent = estimatedMonthlyRent * 12.0
        val grossYieldPostReno = if (totalInvestedGross > 0) (annualGrossRent / totalInvestedGross) * 100.0 else 0.0
        val netYieldPostReno = if (totalInvestedNet > 0) ((annualGrossRent * 0.79) / totalInvestedNet) * 100.0 else 0.0 // Cedolare secca 21%

        return RenovationSimulatorResult(
            surfaceSqm = input.surfaceSqm,
            acquisitionPrice = input.acquisitionPrice,
            qualityLevel = input.qualityLevel,
            itemsBreakdown = effectiveItems,
            subtotalDirectWorks = subtotalDirect,
            contingencyBufferCost = contingencyCost,
            contractorAndTechFeesCost = contractorFeesCost,
            totalGrossRenovationCost = totalGrossRenovation,
            grossCostPerSqm = grossCostPerSqm,
            totalTaxDeduction10Years = totalTaxDeduction10Years,
            yearlyTaxDeductionDeducted = yearlyDeduction,
            netEffectiveRenovationCost = netEffectiveRenovation,
            netEffectiveCostPerSqm = netCostPerSqm,
            totalCapitalInvestedGross = totalInvestedGross,
            totalCapitalInvestedNet = totalInvestedNet,
            estimatedPostRenoValuePerSqm = postRenoPricePerSqm,
            estimatedArvMarketValue = estimatedArv,
            capitalGainFlipGross = capitalGainGross,
            roiFlipGrossPercent = roiFlipGross,
            capitalGainFlipNet = capitalGainNet,
            roiFlipNetPercent = roiFlipNet,
            estimatedMonthlyRentPostReno = estimatedMonthlyRent,
            annualGrossRentPostReno = annualGrossRent,
            grossRentalYieldPostRenoPercent = grossYieldPostReno,
            netRentalYieldPostRenoPercent = netYieldPostReno
        )
    }
}
