package com.example.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.YieldBenchmarkEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class YieldBenchmarkUiState(
    val macroData: MacroEconomicData = MacroFinancialApiService.getInternalRateBaseline(),
    val isRefreshing: Boolean = false,
    val selectedDealId: Long? = null,
    val customPurchasePriceStr: String = "185000",
    val customRenovationCostStr: String = "20000",
    val customMonthlyRentStr: String = "1250",
    val customMonthlyExpensesStr: String = "180",
    val customDownPaymentPercent: Double = 20.0,
    val overrideInflationRate: Double? = null,
    val overrideBtpYield: Double? = null,
    val overrideHurdleSpreadBps: Int? = null,
    val normalizedResult: NormalizedRoiResult? = null,
    val availableDeals: List<PropertyDeal> = emptyList(),
    val statusMessage: String? = null
)

class YieldBenchmarkViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val macroDao = database.macroBenchmarkDao()
    private val dealDao = database.propertyDealDao()

    private val _uiState = MutableStateFlow(YieldBenchmarkUiState())
    val uiState: StateFlow<YieldBenchmarkUiState> = _uiState.asStateFlow()

    init {
        // Observe cached macro data from Room
        viewModelScope.launch {
            macroDao.getMacroDataFlow().collect { cached ->
                if (cached != null) {
                    _uiState.update { it.copy(macroData = cached) }
                    recalculateMetrics()
                } else {
                    // Seed initial baseline into DB
                    val baseline = MacroFinancialApiService.getInternalRateBaseline()
                    macroDao.insertOrUpdate(baseline)
                    _uiState.update { it.copy(macroData = baseline) }
                    recalculateMetrics()
                }
            }
        }

        // Observe deals for selector
        viewModelScope.launch {
            dealDao.getAllDeals().collect { deals ->
                _uiState.update { it.copy(availableDeals = deals) }
                if (_uiState.value.selectedDealId == null && deals.isNotEmpty()) {
                    selectDeal(deals.first())
                }
            }
        }

        // Attempt live update on startup
        refreshMacroData()
    }

    fun refreshMacroData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, statusMessage = "Verifica connettività parametri macro...") }
            try {
                val liveData = MacroFinancialApiService.fetchLatestMacroEconomicData()
                macroDao.insertOrUpdate(liveData)
                _uiState.update {
                    it.copy(
                        macroData = liveData,
                        isRefreshing = false,
                        statusMessage = if (liveData.isLiveFetched) "Dati macro aggiornati da fonte esterna" else "Valori di riferimento interni applicati"
                    )
                }
                recalculateMetrics()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        statusMessage = "Valori di riferimento interni applicati"
                    )
                }
                recalculateMetrics()
            }
        }
    }

    fun selectDeal(deal: PropertyDeal) {
        val estimatedRent = if (deal.estimatedMarketValue > 0) (deal.estimatedMarketValue * 0.055) / 12.0 else (deal.askingPrice * 0.06) / 12.0
        val estimatedExpenses = estimatedRent * 0.15

        _uiState.update {
            it.copy(
                selectedDealId = deal.id,
                customPurchasePriceStr = deal.askingPrice.toInt().toString(),
                customRenovationCostStr = if (deal.sourceKey == "ASTE_GIUDIZIARIE") "25000" else "15000",
                customMonthlyRentStr = estimatedRent.toInt().toString(),
                customMonthlyExpensesStr = estimatedExpenses.toInt().toString()
            )
        }
        recalculateMetrics()
    }

    fun updatePurchasePrice(value: String) {
        _uiState.update { it.copy(customPurchasePriceStr = value, selectedDealId = null) }
        recalculateMetrics()
    }

    fun updateRenovationCost(value: String) {
        _uiState.update { it.copy(customRenovationCostStr = value) }
        recalculateMetrics()
    }

    fun updateMonthlyRent(value: String) {
        _uiState.update { it.copy(customMonthlyRentStr = value) }
        recalculateMetrics()
    }

    fun updateMonthlyExpenses(value: String) {
        _uiState.update { it.copy(customMonthlyExpensesStr = value) }
        recalculateMetrics()
    }

    fun updateDownPaymentPercent(percent: Double) {
        _uiState.update { it.copy(customDownPaymentPercent = percent) }
        recalculateMetrics()
    }

    fun setOverrideInflation(rate: Double?) {
        _uiState.update { it.copy(overrideInflationRate = rate) }
        recalculateMetrics()
    }

    fun setOverrideBtpYield(yield: Double?) {
        _uiState.update { it.copy(overrideBtpYield = yield) }
        recalculateMetrics()
    }

    fun setOverrideHurdleSpread(spreadBps: Int?) {
        _uiState.update { it.copy(overrideHurdleSpreadBps = spreadBps) }
        recalculateMetrics()
    }

    fun resetMacroOverrides() {
        _uiState.update {
            it.copy(
                overrideInflationRate = null,
                overrideBtpYield = null,
                overrideHurdleSpreadBps = null
            )
        }
        recalculateMetrics()
    }

    private fun recalculateMetrics() {
        val state = _uiState.value
        val price = state.customPurchasePriceStr.toDoubleOrNull() ?: 185000.0
        val reno = state.customRenovationCostStr.toDoubleOrNull() ?: 20000.0
        val rent = state.customMonthlyRentStr.toDoubleOrNull() ?: 1250.0
        val expenses = state.customMonthlyExpensesStr.toDoubleOrNull() ?: 180.0

        val effectiveMacro = state.macroData.copy(
            italyHicpInflationRate = state.overrideInflationRate ?: state.macroData.italyHicpInflationRate,
            italianBtp10YYield = state.overrideBtpYield ?: state.macroData.italianBtp10YYield,
            targetHurdleSpreadBps = state.overrideHurdleSpreadBps ?: state.macroData.targetHurdleSpreadBps
        )

        val result = YieldBenchmarkEngine.normalizeRoi(
            purchasePrice = price,
            renovationCost = reno,
            legalAuctionFees = 2500.0,
            monthlyRent = rent,
            monthlyOperatingExpenses = expenses,
            downPaymentPercent = state.customDownPaymentPercent,
            macroData = effectiveMacro
        )

        _uiState.update { it.copy(normalizedResult = result) }
    }
}
