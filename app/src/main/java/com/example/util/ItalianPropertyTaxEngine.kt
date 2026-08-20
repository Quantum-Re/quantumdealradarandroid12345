package com.example.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

/**
 * Italian Property Acquisition Transaction Types (Regime Fiscale Compravendita Immobiliare)
 */
enum class ItalianAcquisitionType(val label: String, val description: String) {
    PRIVATE_SECOND_HOME(
        "Privato: Seconda Casa / Investimento",
        "Imposta di registro 9% su valore catastale (min €1.000) + €100 ipo/catastale fisse"
    ),
    PRIVATE_FIRST_HOME(
        "Privato: Prima Casa (Agevolata)",
        "Imposta di registro 2% su valore catastale (min €1.000) + €100 ipo/catastale fisse"
    ),
    COMPANY_VAT_STANDARD(
        "Impresa Costruttrice: IVA 10%",
        "IVA al 10% sul prezzo di compravendita + €600 imposte fisse (registro, ipotecaria, catastale)"
    ),
    COMPANY_VAT_FIRST_HOME(
        "Impresa Costruttrice: IVA 4% (Prima Casa)",
        "IVA agevolata al 4% sul prezzo + €600 imposte fisse"
    ),
    AUCTION_JUDICIAL(
        "Asta Giudiziaria / Esecuzioni",
        "Imposta di registro 9% (o 2%) + spese delegate cancellazione formalità (~€1.500)"
    )
}

/**
 * Italian Rental Taxation Regimes (Regime Fiscale Locazioni Immobiliari)
 */
enum class RentalTaxRegime(val label: String, val ratePercent: Double, val description: String) {
    CEDOLARE_SECCA_21("Cedolare Secca 21%", 21.0, "Imposta sostitutiva fissa 21% sul canone lordo annuo (contratto libero 4+4)"),
    CEDOLARE_SECCA_10("Cedolare Secca 10%", 10.0, "Aliquota agevolata 10% per canone concordato (3+2) nei comuni ad alta tensione"),
    REGIME_ORDINARIO_IRPEF("Regime IRPEF Ordinario (~28%)", 28.0, "Tassazione a scaglioni IRPEF su 95% del canone (aliquota media stimata)"),
    ESENTE_LORDO("Lordo Pre-Imposte (0%)", 0.0, "Nessuna detrazione fiscale (valutazione lorda)")
}

/**
 * Detailed breakdown of Italian property acquisition taxes, notary fees, and transaction costs.
 */
data class ItalianAcquisitionCostBreakdown(
    val purchasePrice: Double,
    val estimatedCadastralValue: Double,
    val acquisitionType: ItalianAcquisitionType,
    val registrationOrVatTax: Double,
    val fixedRegistryIpoCatTaxes: Double,
    val totalTaxes: Double,
    val notaryDeedSaleFee: Double,
    val notaryDeedMortgageFee: Double,
    val notaryExpensesAndVat: Double,
    val totalNotaryFees: Double,
    val agencyCommission: Double,
    val loanSubstituteTax: Double,
    val totalAncillaryCosts: Double
) {
    val totalTaxesAndNotaryOnly: Double get() = totalTaxes + totalNotaryFees + loanSubstituteTax
}

/**
 * Detailed breakdown of Flip Capital Gain (Plusvalenza Immobiliare) under Italian Art. 67 TUIR.
 */
data class ItalianFlipTaxBreakdown(
    val resalePrice: Double,
    val totalCostBasis: Double,
    val grossCapitalGain: Double,
    val isSubjectToPlusvalenza: Boolean,
    val plusvalenzaTaxRatePercent: Double,
    val plusvalenzaTaxAmount: Double,
    val netFlipProfit: Double,
    val netFlipRoiPercent: Double
)

/**
 * Core calculation engine for Italian Property Transaction Taxes, Notary Tariffs, and Net ROI Projections.
 */
object ItalianPropertyTaxEngine {

    /**
     * Estimate cadastral value (Valore Catastale) from purchase price using Italian standard coefficient.
     * In Italy, cadastral value typically represents between 35% and 55% of actual market transaction price.
     */
    fun estimateCadastralValue(purchasePrice: Double, acquisitionType: ItalianAcquisitionType): Double {
        if (purchasePrice <= 0.0) return 0.0
        val ratio = when (acquisitionType) {
            ItalianAcquisitionType.PRIVATE_FIRST_HOME,
            ItalianAcquisitionType.COMPANY_VAT_FIRST_HOME -> 0.40
            else -> 0.45
        }
        return (purchasePrice * ratio).coerceAtLeast(10000.0)
    }

    /**
     * Calculate comprehensive Italian acquisition taxes and notary tariffs.
     */
    fun calculateAcquisitionCosts(
        purchasePrice: Double,
        cadastralValue: Double? = null,
        acquisitionType: ItalianAcquisitionType = ItalianAcquisitionType.PRIVATE_SECOND_HOME,
        hasMortgage: Boolean = true,
        loanAmount: Double = 0.0,
        includeAgencyFee: Boolean = true,
        agencyFeePercent: Double = 3.0,
        customNotaryFee: Double? = null,
        customTaxFee: Double? = null
    ): ItalianAcquisitionCostBreakdown {
        val safePrice = purchasePrice.coerceAtLeast(0.0)
        val safeCadastralValue = if (cadastralValue != null && cadastralValue > 0.0) {
            cadastralValue
        } else {
            estimateCadastralValue(safePrice, acquisitionType)
        }

        // 1. Taxes (Imposta di Registro / IVA / Ipotecaria / Catastale)
        val (calcRegOrVat, calcFixedTaxes) = when (acquisitionType) {
            ItalianAcquisitionType.PRIVATE_SECOND_HOME -> {
                // Imposta di registro 9% sul valore catastale (minimo €1.000), + €50 ipotecaria + €50 catastale
                val reg = max(safeCadastralValue * 0.09, 1000.0)
                val fixed = 100.0
                Pair(reg, fixed)
            }
            ItalianAcquisitionType.PRIVATE_FIRST_HOME -> {
                // Imposta di registro 2% sul valore catastale (minimo €1.000), + €50 ipotecaria + €50 catastale
                val reg = max(safeCadastralValue * 0.02, 1000.0)
                val fixed = 100.0
                Pair(reg, fixed)
            }
            ItalianAcquisitionType.COMPANY_VAT_STANDARD -> {
                // IVA 10% sul prezzo reale d'acquisto + €200 registro + €200 ipotecaria + €200 catastale (€600 totali)
                val vat = safePrice * 0.10
                val fixed = 600.0
                Pair(vat, fixed)
            }
            ItalianAcquisitionType.COMPANY_VAT_FIRST_HOME -> {
                // IVA 4% Prima Casa sul prezzo + €600 imposte fisse
                val vat = safePrice * 0.04
                val fixed = 600.0
                Pair(vat, fixed)
            }
            ItalianAcquisitionType.AUCTION_JUDICIAL -> {
                // Asta: Registro 9% su prezzo d'aggiudicazione o catastale + spese cancellazione gravami (~€1.200)
                val reg = max(safeCadastralValue * 0.09, 1000.0)
                val fixed = 1200.0
                Pair(reg, fixed)
            }
        }

        val totalTaxes = customTaxFee ?: (calcRegOrVat + calcFixedTaxes)

        // 2. Notary Fees (Tariffario Notarile Nazionale + IVA 22% + Bolli)
        val (calcDeedFee, calcMortgageFee, calcNotaryExpensesAndVat) = if (customNotaryFee != null) {
            Triple(customNotaryFee * 0.6, customNotaryFee * 0.2, customNotaryFee * 0.2)
        } else {
            val baseDeed = calculateNotaryDeedFee(safePrice)
            val baseMortgage = if (hasMortgage && loanAmount > 0) calculateNotaryMortgageFee(loanAmount) else 0.0
            val taxableTotal = baseDeed + baseMortgage
            val vat22 = taxableTotal * 0.22
            val bolliCassaNotariato = 380.0
            Triple(baseDeed, baseMortgage, vat22 + bolliCassaNotariato)
        }

        val totalNotaryFees = customNotaryFee ?: (calcDeedFee + calcMortgageFee + calcNotaryExpensesAndVat)

        // 3. Real Estate Agency Commission (Provvigione Agenzia Immobiliare)
        val agencyCommission = if (includeAgencyFee && acquisitionType != ItalianAcquisitionType.AUCTION_JUDICIAL) {
            val baseCommission = safePrice * (agencyFeePercent / 100.0)
            baseCommission * 1.22 // + IVA 22% on agency invoice
        } else {
            0.0
        }

        // 4. Imposta Sostitutiva sul Mutuo (Banca)
        val loanSubstituteTax = if (hasMortgage && loanAmount > 0.0) {
            when (acquisitionType) {
                ItalianAcquisitionType.PRIVATE_FIRST_HOME,
                ItalianAcquisitionType.COMPANY_VAT_FIRST_HOME -> loanAmount * 0.0025 // 0.25%
                else -> loanAmount * 0.02 // 2.00% for second homes/investments
            }
        } else {
            0.0
        }

        val totalAncillary = totalTaxes + totalNotaryFees + agencyCommission + loanSubstituteTax

        return ItalianAcquisitionCostBreakdown(
            purchasePrice = safePrice,
            estimatedCadastralValue = safeCadastralValue,
            acquisitionType = acquisitionType,
            registrationOrVatTax = calcRegOrVat,
            fixedRegistryIpoCatTaxes = calcFixedTaxes,
            totalTaxes = totalTaxes,
            notaryDeedSaleFee = calcDeedFee,
            notaryDeedMortgageFee = calcMortgageFee,
            notaryExpensesAndVat = calcNotaryExpensesAndVat,
            totalNotaryFees = totalNotaryFees,
            agencyCommission = agencyCommission,
            loanSubstituteTax = loanSubstituteTax,
            totalAncillaryCosts = totalAncillary
        )
    }

    /**
     * Standard Italian Notary Deed Fee based on transaction brackets.
     */
    private fun calculateNotaryDeedFee(purchasePrice: Double): Double {
        return when {
            purchasePrice <= 50000.0 -> 1100.0
            purchasePrice <= 100000.0 -> 1450.0
            purchasePrice <= 175000.0 -> 1800.0
            purchasePrice <= 250000.0 -> 2150.0
            purchasePrice <= 400000.0 -> 2600.0
            purchasePrice <= 600000.0 -> 3100.0
            else -> 3100.0 + ((purchasePrice - 600000.0) * 0.0025)
        }
    }

    /**
     * Standard Italian Notary Mortgage Deed (Atto di Mutuo) Fee.
     */
    private fun calculateNotaryMortgageFee(loanAmount: Double): Double {
        return when {
            loanAmount <= 50000.0 -> 800.0
            loanAmount <= 100000.0 -> 1050.0
            loanAmount <= 200000.0 -> 1350.0
            loanAmount <= 350000.0 -> 1650.0
            else -> 1950.0
        }
    }

    /**
     * Calculate Plusvalenza Tax on Flip (Art. 67 D.P.R. 917/1986 - TUIR).
     * If the property is sold within 5 years, the capital gain (Resale - (Cost + Taxes + Notary + Renovation + Agency))
     * is taxed with an Imposta Sostitutiva of 26%. If held for >5 years, it is tax exempt (0%).
     */
    fun calculateFlipCapitalGainTax(
        purchasePrice: Double,
        resalePrice: Double,
        renovationCost: Double,
        ancillaryCosts: Double,
        holdingPeriodYears: Int = 1,
        isPrimaryResidence: Boolean = false
    ): ItalianFlipTaxBreakdown {
        val totalCostBasis = purchasePrice + renovationCost + ancillaryCosts
        val grossCapitalGain = (resalePrice - totalCostBasis).coerceAtLeast(0.0)

        // Exempt if held > 5 years or if it was primary residence for >50% of the period
        val isSubject = holdingPeriodYears <= 5 && !isPrimaryResidence && grossCapitalGain > 0.0
        val taxRate = if (isSubject) 26.0 else 0.0
        val taxAmount = if (isSubject) grossCapitalGain * 0.26 else 0.0

        val grossProfit = resalePrice - totalCostBasis
        val netProfit = grossProfit - taxAmount
        val netRoiPercent = if (totalCostBasis > 0.0) (netProfit / totalCostBasis) * 100.0 else 0.0

        return ItalianFlipTaxBreakdown(
            resalePrice = resalePrice,
            totalCostBasis = totalCostBasis,
            grossCapitalGain = grossCapitalGain,
            isSubjectToPlusvalenza = isSubject,
            plusvalenzaTaxRatePercent = taxRate,
            plusvalenzaTaxAmount = taxAmount,
            netFlipProfit = netProfit,
            netFlipRoiPercent = netRoiPercent
        )
    }

    /**
     * Calculate annual rental tax based on the selected tax regime.
     */
    fun calculateRentalTax(annualGrossRent: Double, regime: RentalTaxRegime): Double {
        if (annualGrossRent <= 0.0) return 0.0
        return when (regime) {
            RentalTaxRegime.CEDOLARE_SECCA_21 -> annualGrossRent * 0.21
            RentalTaxRegime.CEDOLARE_SECCA_10 -> annualGrossRent * 0.10
            RentalTaxRegime.REGIME_ORDINARIO_IRPEF -> (annualGrossRent * 0.95) * 0.28 // 95% taxable base at ~28% IRPEF
            RentalTaxRegime.ESENTE_LORDO -> 0.0
        }
    }

    /**
     * Risultato calcolo regime locazioni brevi (Art. 1 c. 595 L. 178/2020 e s.m.i. / Legge di Bilancio).
     * Oltre 2 immobili in affitto breve nello stesso periodo d'imposta (dal 3° o 4° immobile in poi),
     * scatta la presunzione di attività svolta in forma imprenditoriale (regime d'impresa con P.IVA).
     * La prima unità abitativa a scelta può beneficiare della cedolare al 21%, mentre le successive al 26%.
     */
    data class ShortTermRentalTaxResult(
        val propertyCount: Int,
        val isRegimeImpresa: Boolean,
        val appliedTaxRatePercent: Double,
        val totalTaxAmount: Double,
        val description: String
    )

    /**
     * Calcola la tassazione per gli affitti brevi tenendo conto del numero di immobili gestiti.
     * In Italia (dal 2024):
     * - Fino a 2 unità: cedolare secca 21% (prima unità) o 26% (seconda unità).
     * - A partire dal 3° immobile (o > 2 unità secondo configurazione/presunzione fiscale), scatta il regime d'impresa.
     */
    fun calculateShortTermRentalTax(annualGrossRent: Double, propertyIndexOrTotalCount: Int): ShortTermRentalTaxResult {
        if (annualGrossRent <= 0.0) {
            return ShortTermRentalTaxResult(
                propertyCount = propertyIndexOrTotalCount,
                isRegimeImpresa = propertyIndexOrTotalCount >= 3,
                appliedTaxRatePercent = 0.0,
                totalTaxAmount = 0.0,
                description = "Nessun canone imponibile"
            )
        }

        // Se l'investitore opera con 3 o più unità in affitto breve, scatta il regime d'impresa
        val isEnterprise = propertyIndexOrTotalCount >= 3

        return if (isEnterprise) {
            // Regime d'impresa: IRPEF/IRES progressiva con contabilità aziendale (stimata ~28%)
            val taxAmount = (annualGrossRent * 0.95) * 0.28
            ShortTermRentalTaxResult(
                propertyCount = propertyIndexOrTotalCount,
                isRegimeImpresa = true,
                appliedTaxRatePercent = 28.0,
                totalTaxAmount = taxAmount,
                description = "Regime d'impresa obbligatorio per attività oltre la soglia forfettaria (P.IVA e contabilità ordinaria)"
            )
        } else {
            // Cedolare secca affitti brevi (21% prima casa, 26% seconda)
            val rate = if (propertyIndexOrTotalCount <= 1) 21.0 else 26.0
            val taxAmount = annualGrossRent * (rate / 100.0)
            ShortTermRentalTaxResult(
                propertyCount = propertyIndexOrTotalCount,
                isRegimeImpresa = false,
                appliedTaxRatePercent = rate,
                totalTaxAmount = taxAmount,
                description = "Cedolare secca locazioni brevi ($rate%)"
            )
        }
    }
}
