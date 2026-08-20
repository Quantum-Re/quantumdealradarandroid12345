package com.example.util

import com.example.data.MacroEconomicData
import com.example.data.MacroYieldVerdict
import com.example.data.NormalizedRoiResult
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

object YieldBenchmarkEngine {

    /**
     * Normalizes ROI projections for a property deal or custom investment against current macroeconomic indicators.
     */
    fun normalizeRoi(
        purchasePrice: Double,
        renovationCost: Double = 0.0,
        legalAuctionFees: Double = 0.0,
        monthlyRent: Double,
        monthlyOperatingExpenses: Double = 0.0,
        downPaymentPercent: Double = 20.0,
        macroData: MacroEconomicData
    ): NormalizedRoiResult {
        val pPrice = purchasePrice.coerceAtLeast(1000.0)
        val reno = renovationCost.coerceAtLeast(0.0)
        val fees = legalAuctionFees.coerceAtLeast(0.0)
        val totalCost = pPrice + reno + fees
        val annualGrossRent = (monthlyRent * 12.0).coerceAtLeast(0.0)
        val annualExpenses = (monthlyOperatingExpenses * 12.0).coerceAtLeast(0.0)
        val noi = (annualGrossRent - annualExpenses).coerceAtLeast(0.0)

        // Nominal yields
        val nominalGrossYield = if (totalCost > 0.0) (annualGrossRent / totalCost) * 100.0 else 0.0
        val nominalCapRate = if (totalCost > 0.0) (noi / totalCost) * 100.0 else 0.0

        // Loan calculations using live macro mortgage rates
        val mortgageRate = macroData.avgMortgageFixedRate
        val downPayment = pPrice * (downPaymentPercent.coerceIn(0.0, 100.0) / 100.0)
        val loanAmount = (pPrice - downPayment).coerceAtLeast(0.0)
        val initialCash = downPayment + reno + fees

        val monthlyMortgage = if (loanAmount > 0.0 && mortgageRate > 0.0) {
            val r = (mortgageRate / 100.0) / 12.0
            val n = 25 * 12.0
            val compound = Math.pow(1.0 + r, n)
            loanAmount * (r * compound) / (compound - 1.0)
        } else 0.0

        val annualDebtService = monthlyMortgage * 12.0
        val annualNetCashFlow = noi - annualDebtService
        val nominalCoC = if (initialCash > 0.0) (annualNetCashFlow / initialCash) * 100.0 else 0.0

        // 1. Fisher Equation: Real Yield = (Nominal - Inflation) / (1 + Inflation)
        val inflationDecimal = (macroData.italyHicpInflationRate / 100.0)
        val nominalCapDecimal = (nominalCapRate / 100.0)
        val nominalCoCDecimal = (nominalCoC / 100.0)

        val realCapRate = if (inflationDecimal > -1.0) {
            ((nominalCapDecimal - inflationDecimal) / (1.0 + inflationDecimal)) * 100.0
        } else nominalCapRate

        val realCoC = if (inflationDecimal > -1.0) {
            ((nominalCoCDecimal - inflationDecimal) / (1.0 + inflationDecimal)) * 100.0
        } else nominalCoC

        // 2. Risk-Free Spread over 10-Year Italian BTP
        val spreadBtpBps = ((nominalCapRate - macroData.italianBtp10YYield) * 100.0).roundToInt()
        val spreadEuriborBps = ((nominalCoC - macroData.euribor12M) * 100.0).roundToInt()
        val spreadMortgage = nominalCapRate - mortgageRate

        // 3. Hurdle Rate Check
        val hurdleRate = macroData.italianBtp10YYield + (macroData.targetHurdleSpreadBps / 100.0)
        val clearsHurdle = nominalCapRate >= hurdleRate
        val hurdleDiff = nominalCapRate - hurdleRate

        // 4. Purchasing Power Preservation Score (0 - 100)
        var score = 50
        if (realCapRate > 4.0) score += 30
        else if (realCapRate > 2.0) score += 15
        else if (realCapRate < 0.0) score -= 30

        if (spreadBtpBps >= 300) score += 20
        else if (spreadBtpBps >= 150) score += 10
        else if (spreadBtpBps < 0) score -= 25

        if (spreadMortgage > 1.5) score += 10
        else if (spreadMortgage < 0.0) score -= 15
        val preservationScore = score.coerceIn(5, 98)

        // 5. 5-Year Cumulative Cash Flow Projections (Nominal vs Deflated Real)
        var cumulativeNominal = 0.0
        var cumulativeReal = 0.0
        val istatRentIndexationFactor = 0.75 // Legge equo canone / contratti 3+2 (75% ISTAT)

        for (year in 1..5) {
            val indexedRent = annualGrossRent * Math.pow(1.0 + (inflationDecimal * istatRentIndexationFactor), year.toDouble())
            val indexedExpenses = annualExpenses * Math.pow(1.0 + inflationDecimal, year.toDouble())
            val yearNoi = indexedRent - indexedExpenses
            val yearCashFlow = yearNoi - annualDebtService
            cumulativeNominal += yearCashFlow

            // Deflate back to today's purchasing power using total compound inflation
            val discountDeflator = Math.pow(1.0 + inflationDecimal, year.toDouble())
            cumulativeReal += (yearCashFlow / discountDeflator)
        }

        val inflationDrag = (cumulativeNominal - cumulativeReal).coerceAtLeast(0.0)

        // 6. Macro Stress-Testing Scenarios
        // Scenario A: Interest Rate Hike +100 bps (Refinancing/Variable debt shock)
        val stressedMortgage100 = if (loanAmount > 0.0) {
            val r = ((mortgageRate + 1.0) / 100.0) / 12.0
            val n = 25 * 12.0
            val c = Math.pow(1.0 + r, n)
            loanAmount * (r * c) / (c - 1.0)
        } else 0.0
        val stressedCoC100 = if (initialCash > 0.0) ((noi - (stressedMortgage100 * 12)) / initialCash) * 100.0 else 0.0

        // Scenario B: Severe Hike +200 bps
        val stressedMortgage200 = if (loanAmount > 0.0) {
            val r = ((mortgageRate + 2.0) / 100.0) / 12.0
            val n = 25 * 12.0
            val c = Math.pow(1.0 + r, n)
            loanAmount * (r * c) / (c - 1.0)
        } else 0.0
        val stressedCoC200 = if (initialCash > 0.0) ((noi - (stressedMortgage200 * 12)) / initialCash) * 100.0 else 0.0

        // Scenario C: Stagflation Surge (Inflation jumps to 4.5%)
        val stressedInflation = 0.045
        val stressedRealCap = ((nominalCapDecimal - stressedInflation) / (1.0 + stressedInflation)) * 100.0

        // 7. Verdict
        val (verdict, title, explanation) = when {
            nominalCapRate <= macroData.italyHicpInflationRate -> {
                Triple(
                    MacroYieldVerdict.NEGATIVE_REAL_YIELD,
                    "Erosione da Inflazione (Rendimento Reale Negativo)",
                    "Il Cap Rate (${round1(nominalCapRate)}%) non supera l'inflazione attuale (${round1(macroData.italyHicpInflationRate)}%). Il capitale perde potere d'acquisto reale."
                )
            }
            spreadMortgage < 0.0 -> {
                Triple(
                    MacroYieldVerdict.DEBT_DRAG_RISK,
                    "Rischio Leva Negativa (Costo Debito > Rendimento)",
                    "Il costo del mutuo (${round2(mortgageRate)}%) supera il Cap Rate (${round1(nominalCapRate)}%). L'indebitamento distrugge valore anziché amplificarlo."
                )
            }
            nominalCapRate >= hurdleRate && realCapRate >= 3.5 -> {
                Triple(
                    MacroYieldVerdict.STRONG_OUTPERFORM,
                    "Eccellenza Macro (Alpha Generativo)",
                    "L'immobile supera agevolmente l'Hurdle Rate (${round1(hurdleRate)}%) e offre uno spread di +$spreadBtpBps bps sul BTP 10Y, garantendo ottima protezione dall'inflazione."
                )
            }
            nominalCapRate >= macroData.italianBtp10YYield + 1.5 -> {
                Triple(
                    MacroYieldVerdict.HEALTHY_SPREAD,
                    "Premio al Rischio Solido",
                    "Offre un rendimento reale positivo (${round1(realCapRate)}%) con uno spread adeguato (+${spreadBtpBps} bps) rispetto ai titoli di stato a tasso fisso."
                )
            }
            else -> {
                Triple(
                    MacroYieldVerdict.NEUTRAL_MARGINAL,
                    "Spread Marginale sul Risk-Free",
                    "Il premio al rischio (+${spreadBtpBps} bps sul BTP 10Y) è ridotto rispetto al rischio operativo e di illiquidità del mercato immobiliare."
                )
            }
        }

        return NormalizedRoiResult(
            purchasePrice = round2(pPrice),
            totalInvestmentCost = round2(totalCost),
            nominalGrossYieldPercent = round1(nominalGrossYield),
            nominalCapRatePercent = round1(nominalCapRate),
            nominalCashOnCashPercent = round1(nominalCoC),
            realCapRatePercent = round1(realCapRate),
            realCashOnCashPercent = round1(realCoC),
            spreadOverBtp10YBps = spreadBtpBps,
            spreadOverEuriborBps = spreadEuriborBps,
            spreadOverMortgageRatePercent = round2(spreadMortgage),
            investorHurdleRatePercent = round1(hurdleRate),
            clearsHurdleRate = clearsHurdle,
            hurdleDifferencePercent = round1(hurdleDiff),
            purchasingPowerPreservationScore = preservationScore,
            nominal5YearCumulativeCashFlow = round2(cumulativeNominal),
            real5YearCumulativeCashFlow = round2(cumulativeReal),
            fiveYearInflationDragEuros = round2(inflationDrag),
            stressTestRateHike100BpsCapRate = round1(stressedCoC100),
            stressTestRateHike200BpsCapRate = round1(stressedCoC200),
            stressTestInflationSurgeRealCapRate = round1(stressedRealCap),
            macroVerdict = verdict,
            macroVerdictTitle = title,
            macroVerdictExplanation = explanation
        )
    }

    private fun round2(v: Double): Double = BigDecimal(v).setScale(2, RoundingMode.HALF_UP).toDouble()
    private fun round1(v: Double): Double = BigDecimal(v).setScale(1, RoundingMode.HALF_UP).toDouble()
}
