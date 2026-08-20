package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.GeminiMarketInsightsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class MarketInsightsUiState(
    val isLoading: Boolean = false,
    val report: MarketInsightsReport = GeminiMarketInsightsService.getCuratedBaselineReport(MarketInsightTopic.ALL),
    val selectedTopic: MarketInsightTopic = MarketInsightTopic.ALL,
    val selectedSentimentFilter: MarketSentiment? = null,
    val searchQuery: String = "",
    val activeSearchFilter: String = "",
    val selectedInsightForDetail: MarketInsightItem? = null,
    val bookmarkedInsightIds: Set<String> = emptySet(),
    val isGroundingSheetOpen: Boolean = false,
    val errorMessage: String? = null
)

class MarketInsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MarketInsightsUiState())
    val uiState: StateFlow<MarketInsightsUiState> = _uiState.asStateFlow()

    init {
        // Initial load with curated baseline and trigger background fresh fetch
        loadInitialInsights()
    }

    fun loadInitialInsights() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = GeminiMarketInsightsService.fetchMarketInsights(
                topic = _uiState.value.selectedTopic,
                customQuery = null
            )
            result.onSuccess { newReport ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        report = newReport,
                        errorMessage = newReport.errorMessage
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Impossibile recuperare notizie in tempo reale"
                    )
                }
            }
        }
    }

    fun selectTopic(topic: MarketInsightTopic) {
        if (_uiState.value.selectedTopic == topic) return
        _uiState.update { it.copy(selectedTopic = topic, searchQuery = "", activeSearchFilter = "") }
        fetchForTopic(topic)
    }

    private fun fetchForTopic(topic: MarketInsightTopic) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = GeminiMarketInsightsService.fetchMarketInsights(topic = topic, customQuery = null)
            result.onSuccess { newReport ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        report = newReport,
                        errorMessage = newReport.errorMessage
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun executeSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(activeSearchFilter = "", searchQuery = "") }
            fetchForTopic(_uiState.value.selectedTopic)
            return
        }

        _uiState.update { it.copy(activeSearchFilter = trimmed, searchQuery = trimmed, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = GeminiMarketInsightsService.fetchMarketInsights(
                topic = _uiState.value.selectedTopic,
                customQuery = trimmed
            )
            result.onSuccess { newReport ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        report = newReport,
                        errorMessage = newReport.errorMessage
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage
                    )
                }
            }
        }
    }

    fun refresh() {
        val currentQuery = _uiState.value.activeSearchFilter.ifBlank { null }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = GeminiMarketInsightsService.fetchMarketInsights(
                topic = _uiState.value.selectedTopic,
                customQuery = currentQuery
            )
            result.onSuccess { newReport ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        report = newReport,
                        errorMessage = newReport.errorMessage
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage
                    )
                }
            }
        }
    }

    fun setSentimentFilter(sentiment: MarketSentiment?) {
        _uiState.update {
            val newSentiment = if (it.selectedSentimentFilter == sentiment) null else sentiment
            it.copy(selectedSentimentFilter = newSentiment)
        }
    }

    fun toggleBookmark(insightId: String) {
        _uiState.update {
            val current = it.bookmarkedInsightIds.toMutableSet()
            if (current.contains(insightId)) {
                current.remove(insightId)
            } else {
                current.add(insightId)
            }
            it.copy(bookmarkedInsightIds = current)
        }
    }

    fun setSelectedInsight(insight: MarketInsightItem?) {
        _uiState.update { it.copy(selectedInsightForDetail = insight) }
    }

    fun setGroundingSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isGroundingSheetOpen = open) }
    }
}
