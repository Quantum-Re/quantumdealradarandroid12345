package com.example.util

import com.example.data.PipelineStatus
import com.example.data.Property
import org.junit.Assert.*
import org.junit.Test

class PropertyComparisonReportCalculatorTest {

    private fun createProperty(
        id: Long,
        title: String,
        address: String,
        price: Double,
        reno: Double,
        exitVal: Double,
        sqm: Int,
        rent: Double
    ): Property {
        return Property(
            id = id,
            title = title,
            address = address,
            price = price,
            surfaceSqm = sqm,
            propertyType = "Appartamento",
            distressStatus = "Libero",
            estimatedRenovationCost = reno,
            targetResalePrice = exitVal,
            estimatedMarketValue = exitVal,
            projectedRentalIncome = rent,
            pipelineStatus = PipelineStatus.ANALYZED.key
        )
    }

    @Test
    fun calculate_comparesTwoPropertiesAccurately() {
        val propA = createProperty(
            id = 1L,
            title = "Bilocale Duomo",
            address = "Via Dante 10 Milano",
            price = 200000.0,
            reno = 30000.0,
            exitVal = 290000.0,
            sqm = 70,
            rent = 1200.0
        )

        val propB = createProperty(
            id = 2L,
            title = "Trilocale Navigli",
            address = "Ripa di Porta Ticinese 15 Milano",
            price = 250000.0,
            reno = 50000.0,
            exitVal = 340000.0,
            sqm = 90,
            rent = 1400.0
        )

        val data = PropertyComparisonReportCalculator.calculate(propA, propB)

        // Basis A = 200k + 30k = 230k
        assertEquals(230000.0, data.totalInvestedBasisA, 0.01)
        // Profit A = 290k - 230k = 60k
        assertEquals(60000.0, data.projectedGrossProfitA, 0.01)
        // ROI A = (60k / 230k) * 100 = ~26.08%
        assertEquals(26.08, data.projectedRoiPercentA, 0.1)

        // Basis B = 250k + 50k = 300k
        assertEquals(300000.0, data.totalInvestedBasisB, 0.01)
        // Profit B = 340k - 300k = 40k
        assertEquals(40000.0, data.projectedGrossProfitB, 0.01)
        // ROI B = (40k / 300k) * 100 = ~13.33%
        assertEquals(13.33, data.projectedRoiPercentB, 0.1)

        // Price per Sqm:
        // A: 200,000 / 70 = 2857.14 €/m²
        // B: 250,000 / 90 = 2777.77 €/m² -> B has lower entry price per sqm
        assertEquals("B", data.winnerEntryPricePerSqm)

        // Total Capital Outlay: A (230k) < B (300k)
        assertEquals("A", data.winnerLowestTotalCapital)

        // Profit: A (60k) > B (40k)
        assertEquals("A", data.winnerMaxProfit)

        // ROI: A (26.08%) > B (13.33%)
        assertEquals("A", data.winnerMaxRoi)

        // Rental Yield:
        // A: (1200 * 12) / 230k = 14400 / 230000 = ~6.26%
        // B: (1400 * 12) / 300k = 16800 / 300000 = ~5.60%
        // A has higher yield
        assertEquals("A", data.winnerMaxRentalYield)

        // Renovation ratio:
        // A: 30k / 200k = 15%
        // B: 50k / 250k = 20%
        // A has lower capex risk
        assertEquals("A", data.winnerLowerCapExRisk)

        assertTrue(data.verdictTitle.contains("Immobile A") || data.verdictTitle.contains("Bilocale Duomo"))
        assertTrue(data.keyTakeaways.isNotEmpty())
    }
}
