package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode

data class FinancialAnalysisOutput(
    val purchasePrice: Double,
    val totalProjectCost: Double,
    val initialCashRequired: Double,
    val loanAmount: Double,
    val monthlyMortgage: Double,
    val annualGrossRent: Double,
    val annualOperatingExpenses: Double,
    val netOperatingIncome: Double,
    val annualNetCashFlow: Double,
    val monthlyNetCashFlow: Double,
    val cashOnCashReturnPercent: Double,
    val capRatePercent: Double,
    val grossYieldPercent: Double,
    val dscr: Double,
    val breakEvenOccupancyPercent: Double,
    val flipProfit: Double,
    val flipRoiPercent: Double
)

object RoiFinancialCalculatorEngine {

    fun calculateDetailedMetrics(
        purchasePrice: Double,
        renovationCost: Double = 0.0,
        legalAuctionFees: Double = 0.0,
        monthlyRent: Double = 0.0,
        monthlyExpenses: Double = 0.0,
        expectedResale: Double = 0.0,
        downPaymentPercent: Double = 20.0,
        mortgageRatePercent: Double = 3.2,
        loanTermYears: Int = 25,
        estimatedVacancyPercent: Double = 5.0
    ): FinancialAnalysisOutput {
        val pPrice = purchasePrice.coerceAtLeast(0.0)
        val reno = renovationCost.coerceAtLeast(0.0)
        val fees = legalAuctionFees.coerceAtLeast(0.0)
        val mRent = monthlyRent.coerceAtLeast(0.0)
        val mExpenses = monthlyExpenses.coerceAtLeast(0.0)
        val resale = expectedResale.coerceAtLeast(0.0)
        val dpPct = downPaymentPercent.coerceIn(0.0, 100.0)
        val mRate = mortgageRatePercent.coerceAtLeast(0.0)
        val termYears = loanTermYears.coerceIn(1, 40)
        val vacancyPct = estimatedVacancyPercent.coerceIn(0.0, 100.0)

        // Total Capital / Project Cost
        val totalCost = pPrice + reno + fees
        val downPayment = pPrice * (dpPct / 100.0)
        val loanAmount = (pPrice - downPayment).coerceAtLeast(0.0)
        val initialCash = downPayment + reno + fees

        // Mortgage Amortization (French Amortization Schedule standard)
        val monthlyMortgage = if (loanAmount <= 0.0) {
            0.0
        } else if (mRate <= 0.0) {
            loanAmount / (termYears * 12).toDouble()
        } else {
            val monthlyR = (mRate / 100.0) / 12.0
            val nMonths = (termYears * 12).toDouble()
            val compound = Math.pow(1.0 + monthlyR, nMonths)
            if (compound == 1.0) 0.0 else loanAmount * (monthlyR * compound) / (compound - 1.0)
        }

        val annualDebtService = monthlyMortgage * 12.0
        val rawGrossRent = mRent * 12.0
        val vacancyLoss = rawGrossRent * (vacancyPct / 100.0)
        val effectiveGrossIncome = (rawGrossRent - vacancyLoss).coerceAtLeast(0.0)
        val annualExpenses = mExpenses * 12.0

        val noi = (effectiveGrossIncome - annualExpenses).coerceAtLeast(0.0)
        val annualCashFlow = noi - annualDebtService
        val monthlyCashFlow = annualCashFlow / 12.0

        val cocPercent = if (initialCash > 0.0) (annualCashFlow / initialCash) * 100.0 else 0.0
        val capRate = if (totalCost > 0.0) (noi / totalCost) * 100.0 else 0.0
        val grossYield = if (totalCost > 0.0) (rawGrossRent / totalCost) * 100.0 else 0.0

        val dscr = if (annualDebtService > 0.0) noi / annualDebtService else 99.0
        val breakEvenOccupancy = if (rawGrossRent > 0.0) {
            ((annualDebtService + annualExpenses) / rawGrossRent) * 100.0
        } else {
            0.0
        }

        val flipProfit = resale - totalCost
        val flipRoi = if (totalCost > 0.0) (flipProfit / totalCost) * 100.0 else 0.0

        return FinancialAnalysisOutput(
            purchasePrice = round2(pPrice),
            totalProjectCost = round2(totalCost),
            initialCashRequired = round2(initialCash),
            loanAmount = round2(loanAmount),
            monthlyMortgage = round2(monthlyMortgage),
            annualGrossRent = round2(rawGrossRent),
            annualOperatingExpenses = round2(annualExpenses),
            netOperatingIncome = round2(noi),
            annualNetCashFlow = round2(annualCashFlow),
            monthlyNetCashFlow = round2(monthlyCashFlow),
            cashOnCashReturnPercent = round1(cocPercent),
            capRatePercent = round1(capRate),
            grossYieldPercent = round1(grossYield),
            dscr = round2(dscr),
            breakEvenOccupancyPercent = round1(breakEvenOccupancy),
            flipProfit = round2(flipProfit),
            flipRoiPercent = round1(flipRoi)
        )
    }

    private fun round2(v: Double): Double {
        return BigDecimal(v).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    private fun round1(v: Double): Double {
        return BigDecimal(v).setScale(1, RoundingMode.HALF_UP).toDouble()
    }
}
