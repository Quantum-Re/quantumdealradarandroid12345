package com.example.data

import androidx.compose.runtime.Immutable
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class MacroYieldVerdict {
    STRONG_OUTPERFORM,   // Real yield > 4% and Spread over BTP > 300 bps
    HEALTHY_SPREAD,      // Real yield > 2% and Spread over BTP > 150 bps
    NEUTRAL_MARGINAL,    // Real yield 0% - 2% or Spread over BTP 0 - 150 bps
    NEGATIVE_REAL_YIELD, // Nominal yield <= Inflation (capital erosion)
    DEBT_DRAG_RISK       // Yield < Borrowing cost (negative leverage)
}

@Immutable
@Entity(tableName = "macro_economic_benchmarks")
data class MacroEconomicData(
    @PrimaryKey val id: Int = 1,
    val ecbMainRefinancingRate: Double = 3.75, // Tasso di Rifinanziamento Principale di riferimento
    val euribor3M: Double = 3.55,              // Euribor 3 Mesi
    val euribor12M: Double = 3.42,             // Euribor 12 Mesi
    val italianBtp10YYield: Double = 3.65,     // Rendimento BTP Decennale Italiano (Benchmark)
    val italyHicpInflationRate: Double = 1.90, // Inflazione Italia NIC/IPCA (%)
    val eurozoneInflationRate: Double = 2.20,  // Inflazione Eurozona media (%)
    val avgMortgageFixedRate: Double = 3.35,   // Tasso Medio Mutuo Fisso Italia (IRS + Spread)
    val avgMortgageVariableRate: Double = 4.10,// Tasso Medio Mutuo Variabile
    val targetHurdleSpreadBps: Int = 300,      // Spread Target Minimo Investitore (300 bps = 3.0%)
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val sourceProvider: String = "Valore di riferimento interno - non aggiornato da fonte esterna",
    val isLiveFetched: Boolean = false,
    val provenance: String = DataProvenance.CURATED_FALLBACK.name
)

@Immutable
data class NormalizedRoiResult(
    val purchasePrice: Double,
    val totalInvestmentCost: Double,
    val nominalGrossYieldPercent: Double,
    val nominalCapRatePercent: Double,
    val nominalCashOnCashPercent: Double,
    // Macro-Normalized Metrics
    val realCapRatePercent: Double,            // Adjusted via Fisher Equation
    val realCashOnCashPercent: Double,         // Adjusted via Fisher Equation
    val spreadOverBtp10YBps: Int,              // Spread rispetto al BTP 10Y (in Basis Points)
    val spreadOverEuriborBps: Int,            // Spread rispetto all'Euribor 12M (in Basis Points)
    val spreadOverMortgageRatePercent: Double, // Cap Rate - Tasso Mutuo (Leva Positiva/Negativa)
    val investorHurdleRatePercent: Double,     // BTP 10Y + Target Spread
    val clearsHurdleRate: Boolean,
    val hurdleDifferencePercent: Double,
    val purchasingPowerPreservationScore: Int, // 0-100 Score
    val nominal5YearCumulativeCashFlow: Double,
    val real5YearCumulativeCashFlow: Double,   // Deflated for compounding inflation
    val fiveYearInflationDragEuros: Double,
    val stressTestRateHike100BpsCapRate: Double,
    val stressTestRateHike200BpsCapRate: Double,
    val stressTestInflationSurgeRealCapRate: Double,
    val macroVerdict: MacroYieldVerdict,
    val macroVerdictTitle: String,
    val macroVerdictExplanation: String
)

@Dao
interface MacroBenchmarkDao {
    @Query("SELECT * FROM macro_economic_benchmarks WHERE id = 1 LIMIT 1")
    fun getMacroDataFlow(): Flow<MacroEconomicData?>

    @Query("SELECT * FROM macro_economic_benchmarks WHERE id = 1 LIMIT 1")
    suspend fun getMacroData(): MacroEconomicData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(data: MacroEconomicData)
}
