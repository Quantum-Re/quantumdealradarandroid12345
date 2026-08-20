package com.example

import com.example.ui.components.RoiCalculationData
import com.example.ui.components.YieldGrade
import com.example.util.ItalianAcquisitionType
import com.example.util.ItalianPropertyTaxEngine
import com.example.util.RentalTaxRegime
import org.junit.Assert.*
import org.junit.Test

class RoiCalculatorTest {

    @Test
    fun testGrossRentalYieldCalculation() {
        // Purchase: 200,000, Renovation: 20,000, Tax: 3,000, Notary: 2,000, Legal: 0 -> Total: 225,000
        // Rent: 1,500/month -> Annual: 18,000
        // Gross Yield = 18,000 / 225,000 = 8.0%
        val data = RoiCalculationData(
            purchasePrice = 200000.0,
            renovationCost = 20000.0,
            taxFees = 3000.0,
            notaryFees = 2000.0,
            legalFees = 0.0,
            estimatedMonthlyRent = 1500.0
        )

        assertEquals(225000.0, data.totalProjectCost, 0.01)
        assertEquals(18000.0, data.annualGrossRent, 0.01)
        assertEquals(8.0, data.grossYieldPercent, 0.01)
    }

    @Test
    fun testCashOnCashReturnCalculation() {
        // Purchase: 100,000, Renovation: 10,000, Taxes: 3,000, Notary: 2,000
        // 100% cash test (mortgageRate = 0, downPayment = 100%)
        // Monthly Rent = 1,000 (12,000/yr), Expenses = 100 (1,200/yr), Cedolare Secca 21% = 2,520/yr
        // Net Cash Flow = 12,000 - 1,200 - 2,520 = 8,280/yr
        val allCashData = RoiCalculationData(
            purchasePrice = 100000.0,
            renovationCost = 10000.0,
            taxFees = 3000.0,
            notaryFees = 2000.0,
            legalFees = 0.0,
            estimatedMonthlyRent = 1000.0,
            monthlyExpenses = 100.0,
            rentalTaxRegime = RentalTaxRegime.CEDOLARE_SECCA_21,
            downPaymentPercent = 100.0,
            mortgageRatePercent = 0.0
        )

        assertEquals(115000.0, allCashData.totalProjectCost, 0.01)
        assertEquals(115000.0, allCashData.initialCashRequired, 0.01)
        assertEquals(10800.0, allCashData.netOperatingIncome, 0.01)
        assertEquals(2520.0, allCashData.annualRentalTax, 0.01)
        assertEquals(8280.0, allCashData.annualNetCashFlow, 0.01)
        // CoC = 8,280 / 115,000 = 7.2%
        assertEquals(7.20, allCashData.cashOnCashReturnPercent, 0.05)
    }

    @Test
    fun testItalianPropertyTaxEngineAcquisition() {
        // Purchase 150,000€ private second home (9% registration on cadastral value)
        val breakdown = ItalianPropertyTaxEngine.calculateAcquisitionCosts(
            purchasePrice = 150000.0,
            cadastralValue = 70000.0,
            acquisitionType = ItalianAcquisitionType.PRIVATE_SECOND_HOME,
            hasMortgage = true,
            loanAmount = 120000.0,
            includeAgencyFee = false
        )

        // Registro 9% of 70,000 = 6,300€
        // Fixed Ipotecaria + Catastale = 100€ -> Total Taxes = 6,400€
        assertEquals(6300.0, breakdown.registrationOrVatTax, 0.01)
        assertEquals(100.0, breakdown.fixedRegistryIpoCatTaxes, 0.01)
        assertEquals(6400.0, breakdown.totalTaxes, 0.01)

        // Loan substitute tax for 2nd home = 2.0% of 120,000 = 2,400€
        assertEquals(2400.0, breakdown.loanSubstituteTax, 0.01)
        assertTrue("Notary fees should be greater than 0", breakdown.totalNotaryFees > 1500.0)
    }

    @Test
    fun testFixAndFlipPlusvalenzaArt67TUIR() {
        // Purchase: 150,000, Renovation: 30,000, Taxes: 6,000, Notary: 4,000 -> Total Cost: 190,000
        // Expected Resale: 240,000 -> Gross Profit: 50,000
        // Flip in < 5 years: Art. 67 TUIR applies 26% tax on 50,000 = 13,000
        // Net Flip Profit = 50,000 - 13,000 = 37,000
        val shortTermFlip = RoiCalculationData(
            purchasePrice = 150000.0,
            renovationCost = 30000.0,
            taxFees = 6000.0,
            notaryFees = 4000.0,
            legalFees = 0.0,
            expectedResalePrice = 240000.0,
            flipHoldingPeriodYears = 1
        )

        assertEquals(190000.0, shortTermFlip.totalProjectCost, 0.01)
        assertEquals(50000.0, shortTermFlip.grossFlipProfit, 0.01)
        assertTrue(shortTermFlip.flipTaxBreakdown.isSubjectToPlusvalenza)
        assertEquals(13000.0, shortTermFlip.flipTaxBreakdown.plusvalenzaTaxAmount, 0.01)
        assertEquals(37000.0, shortTermFlip.totalFlipProfit, 0.01)
        // ROI Net = (37,000 / 190,000) * 100 = 19.47%
        assertEquals(19.47, shortTermFlip.flipRoiPercent, 0.05)

        // Flip held > 5 years is tax exempt
        val longTermFlip = shortTermFlip.copy(flipHoldingPeriodYears = 6)
        assertFalse(longTermFlip.flipTaxBreakdown.isSubjectToPlusvalenza)
        assertEquals(0.0, longTermFlip.flipTaxBreakdown.plusvalenzaTaxAmount, 0.01)
        assertEquals(50000.0, longTermFlip.totalFlipProfit, 0.01)
        assertEquals(26.32, longTermFlip.flipRoiPercent, 0.05)
    }

    @Test
    fun testYieldGradeClassification() {
        val excellentData = RoiCalculationData(
            purchasePrice = 100000.0,
            renovationCost = 10000.0,
            taxFees = 2000.0,
            notaryFees = 1500.0,
            legalFees = 500.0,
            estimatedMonthlyRent = 1500.0,
            downPaymentPercent = 20.0
        )
        assertEquals(YieldGrade.EXCELLENT, excellentData.yieldRating)
    }
}
