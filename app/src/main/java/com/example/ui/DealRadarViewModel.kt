package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.util.AuctionDateUtils
import com.example.util.BriefMatcher
import com.example.util.ConnectivityStatus
import com.example.util.NetworkConnectivityObserver

enum class MainTab {
    RADAR_FEED,
    MY_PROPERTIES,
    MARKET_INSIGHTS,
    MARKET_KPI_DASHBOARD,
    DISTRESSED,
    MAP_VIEW,
    INVESTOR_BRIEF,
    PARSER_SANDBOX,
    ROI_CALCULATOR,
    ANALYTICS,
    CYBER_TERMINAL,
    GRAVE_DANCER,
    SUPPLY_DEMAND_MONITOR,
    NOTIFICATION_CONFIG,
    REGIONAL_HEATMAP,
    SUBSCRIPTION,
    YIELD_BENCHMARKING,
    RENOVATION_SIMULATOR
}

enum class FilterCategory {
    ALL,
    IN_TARGET_BRIEF,
    HIGH_DISCOUNT,
    AUCTION_NPL,
    BOOKMARKED,
    RECENTLY_VIEWED
}

@Immutable
data class UiState(
    val appLanguage: com.example.util.AppLanguage = com.example.util.AppLanguage.IT,
    val themeMode: com.example.ui.theme.ThemeMode = com.example.ui.theme.ThemeMode.DARK,
    val searchQuery: String = "",
    val selectedMainTab: MainTab = MainTab.RADAR_FEED,
    val selectedFilterCategory: FilterCategory = FilterCategory.ALL,
    val selectedSourceFilter: String = "ALL", // "ALL" or sourceId
    val selectedPropertyTypeFilter: String = "ALL", // "ALL", "Residenziale", "Commerciale", etc.
    val hideExpiredAuctions: Boolean = false,
    val isNetworkAvailable: Boolean = true,
    val isOfflineModeForced: Boolean = false,
    val lastSyncedTimestamp: Long = 0L,
    val pendingSyncCount: Int = 0,
    val selectedDealForDetail: PropertyDeal? = null,
    val selectedSourceForParser: ScraperSource? = null,
    val isAddDealDialogOpen: Boolean = false,
    val isParserEditorOpen: Boolean = false,
    val isRunningParserTest: Boolean = false,
    val parserTestLogs: List<String> = emptyList(),
    val parserTestResult: ScraperTestResult? = null,
    val isMarketTrendDrawerOpen: Boolean = false,
    val selectedRegionForReport: String = "Milano",
    val isGeneratingMarketReport: Boolean = false,
    val marketReportContent: String? = null,
    val marketReportError: String? = null,
    val selectedComparisonDealIds: Set<Long> = emptySet(),
    val isComparisonSheetOpen: Boolean = false
)

// ROI Calculator State
@Immutable
data class RoiCalculatorState(
    val purchasePriceStr: String = "185000",
    val renovationCostStr: String = "25000",
    val legalAuctionFeesStr: String = "8500",
    val monthlyRentStr: String = "1200",
    val monthlyExpensesStr: String = "200",
    val expectedResaleStr: String = "260000",
    val downPaymentPercentStr: String = "20",
    val mortgageRateStr: String = "3.2",
    val loanTermYearsStr: String = "25"
) {
    val purchasePrice: Double get() = purchasePriceStr.toDoubleOrNull() ?: 0.0
    val renovationCost: Double get() = renovationCostStr.toDoubleOrNull() ?: 0.0
    val legalFees: Double get() = legalAuctionFeesStr.toDoubleOrNull() ?: 0.0
    val monthlyRent: Double get() = monthlyRentStr.toDoubleOrNull() ?: 0.0
    val monthlyExpenses: Double get() = monthlyExpensesStr.toDoubleOrNull() ?: 0.0
    val expectedResale: Double get() = expectedResaleStr.toDoubleOrNull() ?: 0.0
    val downPaymentPercent: Double get() = downPaymentPercentStr.toDoubleOrNull() ?: 20.0
    val mortgageRate: Double get() = mortgageRateStr.toDoubleOrNull() ?: 0.0
    val loanTermYears: Int get() = loanTermYearsStr.toIntOrNull() ?: 25

    val LTVPercentStr: String get() = (100.0 - downPaymentPercent).toInt().coerceIn(0, 100).toString()

    // Total Project Cost
    val totalProjectCost: Double get() = purchasePrice + renovationCost + legalFees
    val totalInvestment: Double get() = totalProjectCost

    // Down payment & Loan amount
    val downPaymentAmount: Double get() = purchasePrice * (downPaymentPercent / 100.0)
    val loanAmount: Double get() = (purchasePrice - downPaymentAmount).coerceAtLeast(0.0)

    // Initial Out-of-Pocket Cash Required (CoC denominator)
    val initialCashRequired: Double get() = downPaymentAmount + renovationCost + legalFees

    // Annual Income & Expenses
    val annualGrossRent: Double get() = monthlyRent * 12.0
    val annualExpenses: Double get() = monthlyExpenses * 12.0
    val netOperatingIncome: Double get() = (annualGrossRent - annualExpenses).coerceAtLeast(0.0)

    // Monthly Mortgage Payment
    val monthlyMortgagePayment: Double get() {
        if (loanAmount <= 0.0) return 0.0
        if (mortgageRate <= 0.0) return loanAmount / (loanTermYears * 12).toDouble().coerceAtLeast(1.0)
        val monthlyRate = (mortgageRate / 100.0) / 12.0
        val n = (loanTermYears * 12).toDouble()
        val compound = Math.pow(1.0 + monthlyRate, n)
        if (compound == 1.0) return 0.0
        return loanAmount * (monthlyRate * compound) / (compound - 1.0)
    }

    val annualDebtService: Double get() = monthlyMortgagePayment * 12.0
    val annualNetCashFlow: Double get() = netOperatingIncome - annualDebtService
    val monthlyNetCashFlow: Double get() = annualNetCashFlow / 12.0

    // Cash-on-Cash Return (%)
    val cashOnCashReturnPercent: Double
        get() = if (initialCashRequired > 0.0) (annualNetCashFlow / initialCashRequired) * 100.0 else 0.0

    // Cap Rate (%)
    val netCapRatePercent: Double
        get() = if (totalProjectCost > 0.0) (netOperatingIncome / totalProjectCost) * 100.0 else 0.0

    // Gross Yield (%)
    val grossAnnualYieldPercent: Double
        get() = if (totalProjectCost > 0.0) (annualGrossRent / totalProjectCost) * 100.0 else 0.0

    // Flip Profit & ROI
    val totalFlipProfit: Double get() = expectedResale - totalProjectCost
    val flipROIPercent: Double
        get() = if (totalProjectCost > 0.0) (totalFlipProfit / totalProjectCost) * 100.0 else 0.0
}

class DealRadarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = DealRadarRepository(
        database.propertyDealDao(),
        database.scraperSourceDao(),
        database.priceHistoryDao(),
        database.investorProfileDao(),
        database.propertyDao(),
        database.distressedPropertyDao()
    )

    private val authManager = com.example.auth.FirebaseAuthManager(application)
    private val preferencesDataStore = com.example.data.UserPreferencesDataStore.getInstance(application)
    private val syncOutboxManager = com.example.data.SyncOutboxManager.getInstance(application)
    private val connectivityObserver = com.example.util.NetworkConnectivityObserver(application)

    val isFirebaseConfigured: Boolean
        get() = authManager.isFirebaseConfigured()

    val currentAuthUserEmail: String?
        get() = authManager.getCurrentUserEmail()

    val currentAuthUserId: String?
        get() = authManager.getCurrentUserId()

    val customClaims: StateFlow<com.example.auth.FirebaseCustomClaims> = authManager.customClaimsFlow

    val userSessionData: StateFlow<com.example.data.UserSessionData> = preferencesDataStore.sessionDataFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.example.data.UserSessionData())

    val dashboardFilterPreferences: StateFlow<com.example.data.DashboardFilterPreferences> = preferencesDataStore.dashboardFilterFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.example.data.DashboardFilterPreferences())

    private val themePrefs = getApplication<Application>().getSharedPreferences("deal_radar_theme_prefs", android.content.Context.MODE_PRIVATE)
    private val initialThemeMode = try {
        com.example.ui.theme.ThemeMode.valueOf(
            themePrefs.getString("theme_mode", com.example.ui.theme.ThemeMode.DARK.name) ?: com.example.ui.theme.ThemeMode.DARK.name
        )
    } catch (e: Exception) {
        com.example.ui.theme.ThemeMode.DARK
    }

    private val _uiState = MutableStateFlow(UiState(themeMode = initialThemeMode))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _roiCalculatorState = MutableStateFlow(RoiCalculatorState())
    val roiCalculatorState: StateFlow<RoiCalculatorState> = _roiCalculatorState.asStateFlow()

    val allDeals: StateFlow<List<PropertyDeal>> = repository.allDeals
    val allSources: StateFlow<List<ScraperSource>> = repository.allSources
    val allSavedProperties: StateFlow<List<Property>> = repository.allSavedProperties
    val recentlyViewedDeals: StateFlow<List<PropertyDeal>> = repository.recentlyViewedDeals
    val savedDeals: StateFlow<List<PropertyDeal>> = repository.bookmarkedDeals
    val investorProfile: StateFlow<InvestorProfile?> = repository.investorProfile

    private val recentSearchRepository = com.example.data.RecentSearchRepository(
        database.recentSearchDao()
    )

    val recentSearches: StateFlow<List<com.example.data.RecentSearchQuery>> = recentSearchRepository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Onboarding Carousel State Management
    private val onboardingPrefs = getApplication<Application>().getSharedPreferences("deal_radar_onboarding_prefs", android.content.Context.MODE_PRIVATE)
    private val _showOnboardingCarousel = MutableStateFlow(
        !onboardingPrefs.getBoolean("has_seen_onboarding_carousel_v1", false)
    )
    val showOnboardingCarousel: StateFlow<Boolean> = _showOnboardingCarousel.asStateFlow()

    fun openOnboardingCarousel() {
        _showOnboardingCarousel.value = true
    }

    fun dismissOnboardingCarousel(dontShowAgain: Boolean) {
        _showOnboardingCarousel.value = false
        if (dontShowAgain) {
            onboardingPrefs.edit().putBoolean("has_seen_onboarding_carousel_v1", true).apply()
            viewModelScope.launch {
                preferencesDataStore.updateHasSeenOnboarding(true)
            }
        }
    }

    fun completeOnboardingAndStartRegistration() {
        _showOnboardingCarousel.value = false
        onboardingPrefs.edit().putBoolean("has_seen_onboarding_carousel_v1", true).apply()
        viewModelScope.launch {
            preferencesDataStore.updateHasSeenOnboarding(true)
        }
        setMainTab(MainTab.INVESTOR_BRIEF)
    }

    fun resetOnboardingForTesting() {
        onboardingPrefs.edit().putBoolean("has_seen_onboarding_carousel_v1", false).apply()
        viewModelScope.launch {
            preferencesDataStore.updateHasSeenOnboarding(false)
        }
        _showOnboardingCarousel.value = true
    }

    // Granular Notification State Flows
    private val _granularPropertyAlerts = MutableStateFlow<Map<Long, GranularPropertyAlert>>(
        com.example.util.GranularNotificationManager.loadPropertyAlerts(getApplication())
    )
    val granularPropertyAlerts: StateFlow<Map<Long, GranularPropertyAlert>> = _granularPropertyAlerts.asStateFlow()

    private val _globalNotificationSettings = MutableStateFlow(
        com.example.util.GranularNotificationManager.loadGlobalSettings(getApplication())
    )
    val globalNotificationSettings: StateFlow<GlobalNotificationSettings> = _globalNotificationSettings.asStateFlow()

    private val _granularAlertHistory = MutableStateFlow(
        com.example.util.GranularNotificationManager.loadAlertHistory(getApplication())
    )
    val granularAlertHistory: StateFlow<List<GranularAlertHistoryEvent>> = _granularAlertHistory.asStateFlow()

    // Firebase Cloud Messaging (FCM) Real-Time Push States
    private val _fcmToken = MutableStateFlow<String?>(com.example.util.FcmPushManager.getCachedToken(getApplication()))
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _fcmMasterEnabled = MutableStateFlow(com.example.util.FcmPushManager.isMasterPushEnabled(getApplication()))
    val fcmMasterEnabled: StateFlow<Boolean> = _fcmMasterEnabled.asStateFlow()

    private val _fcmSubscribedTopics = MutableStateFlow<Set<String>>(com.example.util.FcmPushManager.loadSubscribedTopics(getApplication()))
    val fcmSubscribedTopics: StateFlow<Set<String>> = _fcmSubscribedTopics.asStateFlow()

    private val _fcmPushHistory = MutableStateFlow<List<FcmPushAlert>>(com.example.util.FcmPushManager.loadPushHistory(getApplication()))
    val fcmPushHistory: StateFlow<List<FcmPushAlert>> = _fcmPushHistory.asStateFlow()

    private val _showFcmPushCenterDialog = MutableStateFlow(false)
    val showFcmPushCenterDialog: StateFlow<Boolean> = _showFcmPushCenterDialog.asStateFlow()

    init {
        // Fetch and refresh FCM token on ViewModel initialization
        viewModelScope.launch {
            com.example.util.FcmPushManager.fetchToken(getApplication()) { token ->
                _fcmToken.value = token
            }
        }
    }

    // Filtered deals flow based on search and selected chips
    val filteredDeals: StateFlow<List<PropertyDeal>> = combine(
        allDeals,
        uiState,
        investorProfile
    ) { deals, state, profile ->
        Log.d("DealRadarViewModel", "filteredDeals combine triggered with ${deals.size} total deals from DB")
        val searchTokens = state.searchQuery.trim().let { query ->
            java.text.Normalizer.normalize(query, java.text.Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase()
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
        }

        val filtered = deals.filter { deal ->
            val matchesSearch = if (searchTokens.isEmpty()) {
                true
            } else {
                val rawText = listOf(
                    deal.title,
                    deal.location,
                    deal.sourceName,
                    deal.sourceKey,
                    deal.propertyType,
                    deal.notes,
                    deal.status,
                    deal.dealStage,
                    "€${deal.askingPrice.toInt()}",
                    "${deal.askingPrice.toInt()}",
                    "${deal.surfaceSqm} mq",
                    "${deal.surfaceSqm}",
                    "-${deal.discountPercent}%",
                    "${deal.discountPercent}%",
                    deal.auctionDate ?: ""
                ).joinToString(" ")

                val searchableText = java.text.Normalizer.normalize(rawText, java.text.Normalizer.Form.NFD)
                    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                    .lowercase()

                searchTokens.all { token -> searchableText.contains(token) }
            }

            val matchesCategory = when (state.selectedFilterCategory) {
                FilterCategory.ALL -> true
                FilterCategory.IN_TARGET_BRIEF -> BriefMatcher.evaluate(deal, profile).isTargetMatch
                FilterCategory.HIGH_DISCOUNT -> deal.discountPercent >= 35
                FilterCategory.AUCTION_NPL -> deal.propertyType.contains("Asta", ignoreCase = true) || deal.title.contains("NPL", ignoreCase = true) || deal.notes.contains("NPL", ignoreCase = true) || deal.notes.contains("Asta", ignoreCase = true)
                FilterCategory.BOOKMARKED -> deal.isBookmarked
                FilterCategory.RECENTLY_VIEWED -> deal.lastViewedAt > 0
            }

            val matchesSource = state.selectedSourceFilter == "ALL" || deal.sourceKey == state.selectedSourceFilter
            val matchesType = state.selectedPropertyTypeFilter == "ALL" || deal.propertyType.contains(state.selectedPropertyTypeFilter, ignoreCase = true)
            val matchesActive = !state.hideExpiredAuctions || !AuctionDateUtils.isExpired(deal.auctionDate)

            matchesSearch && matchesCategory && matchesSource && matchesType && matchesActive
        }

        val result = if (state.selectedFilterCategory == FilterCategory.RECENTLY_VIEWED) {
            filtered.sortedByDescending { it.lastViewedAt }
        } else {
            filtered
        }
        Log.d("DealRadarViewModel", "filteredDeals produced ${result.size} filtered deals (Category=${state.selectedFilterCategory}, Search='${state.searchQuery}')")
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        Log.d("DealRadarViewModel", "DealRadarViewModel init started, launching database seed check...")
        
        // Observe real-time network connectivity
        viewModelScope.launch {
            connectivityObserver.observe().collect { status ->
                val isOnline = (status == com.example.util.ConnectivityStatus.AVAILABLE)
                _uiState.update { it.copy(isNetworkAvailable = isOnline) }
            }
        }

        // Restore persisted user session settings from DataStore
        viewModelScope.launch {
            preferencesDataStore.sessionDataFlow.take(1).collect { session ->
                _uiState.update { current ->
                    current.copy(
                        themeMode = session.themeMode,
                        appLanguage = session.appLanguage,
                        selectedMainTab = session.selectedMainTab,
                        isOfflineModeForced = session.isOfflineModeForced,
                        lastSyncedTimestamp = session.lastSyncedTimestamp
                    )
                }
                if (session.hasSeenOnboarding) {
                    _showOnboardingCarousel.value = false
                }
            }
        }

        // Restore persisted dashboard filter preferences from DataStore
        viewModelScope.launch {
            preferencesDataStore.dashboardFilterFlow.take(1).collect { filters ->
                _uiState.update { current ->
                    current.copy(
                        searchQuery = filters.searchQuery,
                        selectedFilterCategory = filters.selectedFilterCategory,
                        selectedSourceFilter = filters.selectedSourceFilter,
                        selectedPropertyTypeFilter = filters.selectedPropertyTypeFilter,
                        hideExpiredAuctions = filters.hideExpiredAuctions,
                        selectedRegionForReport = filters.selectedRegionForReport
                    )
                }
            }
        }

        // Observe pending sync actions in outbox
        viewModelScope.launch {
            syncOutboxManager.pendingCountFlow.collect { count ->
                _uiState.update { it.copy(pendingSyncCount = count) }
            }
        }

        viewModelScope.launch {
            val seedResult = AppDatabase.seedDatabaseIfEmpty(getApplication())
            Log.d("DealRadarViewModel", "Database seed completed with result: $seedResult")
            repository.checkAndSeedDatabase()
            Log.d("DealRadarViewModel", "DealRadarViewModel database seed check completed successfully.")
        }
        com.example.service.DistressedWorkManagerScheduler.schedulePeriodicCheck(getApplication())
        com.example.service.SupplyDemandMonitoringEngine.initialize(getApplication())
        com.example.service.SupplyDemandWorkManagerScheduler.schedulePeriodicCheck(getApplication())
    }

    fun triggerWorkManagerCriteriaScan() {
        com.example.service.DistressedWorkManagerScheduler.triggerImmediateCheck(getApplication())
    }

    fun resetWorkManagerNotifiedCache() {
        com.example.service.DistressedWorkManagerScheduler.resetNotifiedCache(getApplication())
    }

    fun openDealById(dealId: Long) {
        viewModelScope.launch {
            val deal = repository.getDealById(dealId)
            if (deal != null) {
                setSelectedDealForDetail(deal)
            }
        }
    }

    fun saveInvestorProfile(profile: InvestorProfile) {
        viewModelScope.launch {
            repository.saveInvestorProfile(profile)
        }
    }

    fun signUpInvestor(
        email: String,
        pass: String,
        fullName: String,
        company: String,
        tier: String,
        capital: Double,
        onResult: (com.example.auth.AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = authManager.signUpWithEmail(email, pass)
            when (res) {
                is com.example.auth.AuthResult.Success -> {
                    val profile = (investorProfile.value ?: InvestorProfile()).copy(
                        fullName = fullName,
                        companyName = company,
                        email = res.email,
                        investorTier = tier,
                        availableCapital = capital,
                        isRegistered = true
                    )
                    repository.saveInvestorProfile(profile)
                }
                is com.example.auth.AuthResult.RequiresVerification -> {
                    val profile = (investorProfile.value ?: InvestorProfile()).copy(
                        fullName = fullName,
                        companyName = company,
                        email = res.email,
                        investorTier = tier,
                        availableCapital = capital,
                        isRegistered = false
                    )
                    repository.saveInvestorProfile(profile)
                }
                is com.example.auth.AuthResult.Error -> {
                    // Handled in UI
                }
            }
            onResult(res)
        }
    }

    fun signInInvestor(
        email: String,
        pass: String,
        onResult: (com.example.auth.AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = authManager.signInWithEmail(email, pass)
            when (res) {
                is com.example.auth.AuthResult.Success -> {
                    val profile = (investorProfile.value ?: InvestorProfile()).copy(
                        email = res.email,
                        isRegistered = true
                    )
                    repository.saveInvestorProfile(profile)
                }
                is com.example.auth.AuthResult.RequiresVerification -> {
                    val profile = (investorProfile.value ?: InvestorProfile()).copy(
                        email = res.email,
                        isRegistered = false
                    )
                    repository.saveInvestorProfile(profile)
                }
                is com.example.auth.AuthResult.Error -> {
                    // Handled in UI
                }
            }
            onResult(res)
        }
    }

    fun sendEmailVerification(
        onResult: (com.example.auth.AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = authManager.sendEmailVerification()
            onResult(res)
        }
    }

    fun checkEmailVerificationStatus(
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val verified = authManager.reloadUserAndCheckVerification()
            if (verified) {
                val current = investorProfile.value ?: InvestorProfile()
                repository.saveInvestorProfile(current.copy(isRegistered = true))
            }
            onResult(verified)
        }
    }

    fun simulateEmailVerificationComplete() {
        viewModelScope.launch {
            authManager.setSimulatedEmailVerified(true)
            val current = investorProfile.value ?: InvestorProfile()
            repository.saveInvestorProfile(current.copy(isRegistered = true))
        }
    }

    fun sendPasswordResetEmail(
        email: String,
        onResult: (com.example.auth.AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = authManager.sendPasswordResetEmail(email)
            onResult(res)
        }
    }

    fun signOutInvestor() {
        viewModelScope.launch {
            authManager.signOut()
            val profile = (investorProfile.value ?: InvestorProfile()).copy(
                isRegistered = false
            )
            repository.saveInvestorProfile(profile)
        }
    }

    fun setMainTab(tab: MainTab) {
        _uiState.update { it.copy(selectedMainTab = tab) }
        viewModelScope.launch {
            preferencesDataStore.updateSelectedMainTab(tab)
        }
    }

    fun toggleLanguage() {
        val nextLang = if (_uiState.value.appLanguage == com.example.util.AppLanguage.IT) com.example.util.AppLanguage.EN else com.example.util.AppLanguage.IT
        setLanguage(nextLang)
    }

    fun setLanguage(lang: com.example.util.AppLanguage) {
        _uiState.update { it.copy(appLanguage = lang) }
        viewModelScope.launch {
            preferencesDataStore.updateLanguage(lang)
        }
    }

    fun toggleThemeMode() {
        val nextMode = when (_uiState.value.themeMode) {
            com.example.ui.theme.ThemeMode.DARK -> com.example.ui.theme.ThemeMode.LIGHT
            com.example.ui.theme.ThemeMode.LIGHT -> com.example.ui.theme.ThemeMode.DARK
            com.example.ui.theme.ThemeMode.SYSTEM -> com.example.ui.theme.ThemeMode.DARK
        }
        setThemeMode(nextMode)
    }

    fun setThemeMode(mode: com.example.ui.theme.ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        themePrefs.edit().putString("theme_mode", mode.name).apply()
        viewModelScope.launch {
            preferencesDataStore.updateThemeMode(mode)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            preferencesDataStore.updateDashboardFilters(searchQuery = query)
        }
    }

    fun submitSearchQuery(query: String) {
        setSearchQuery(query)
        viewModelScope.launch {
            recentSearchRepository.saveSearchQuery(query)
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            recentSearchRepository.removeSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            recentSearchRepository.clearAll()
        }
    }

    fun setFilterCategory(category: FilterCategory) {
        _uiState.update { it.copy(selectedFilterCategory = category) }
        viewModelScope.launch {
            preferencesDataStore.updateDashboardFilters(filterCategory = category)
        }
    }

    fun setSourceFilter(sourceKey: String) {
        _uiState.update { it.copy(selectedSourceFilter = sourceKey) }
        viewModelScope.launch {
            preferencesDataStore.updateDashboardFilters(sourceFilter = sourceKey)
        }
    }

    fun setPropertyTypeFilter(type: String) {
        _uiState.update { it.copy(selectedPropertyTypeFilter = type) }
        viewModelScope.launch {
            preferencesDataStore.updateDashboardFilters(propertyTypeFilter = type)
        }
    }

    fun toggleHideExpiredAuctions() {
        val next = !_uiState.value.hideExpiredAuctions
        _uiState.update { it.copy(hideExpiredAuctions = next) }
        viewModelScope.launch {
            preferencesDataStore.updateDashboardFilters(hideExpiredAuctions = next)
        }
    }

    fun resetDashboardFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                selectedFilterCategory = FilterCategory.ALL,
                selectedSourceFilter = "ALL",
                selectedPropertyTypeFilter = "ALL",
                hideExpiredAuctions = false
            )
        }
        viewModelScope.launch {
            preferencesDataStore.clearDashboardFilters()
        }
    }

    fun toggleOfflineModeForced() {
        val next = !_uiState.value.isOfflineModeForced
        _uiState.update { it.copy(isOfflineModeForced = next) }
        viewModelScope.launch {
            preferencesDataStore.updateOfflineModeForced(next)
        }
    }

    fun recordPriceDrop(dealId: Long, newPrice: Double, eventLabel: String, dateRecorded: String) {
        viewModelScope.launch {
            val oldDeal = allDeals.value.find { it.id == dealId }
            val oldPrice = oldDeal?.askingPrice ?: newPrice

            repository.recordPriceDrop(
                dealId = dealId,
                newPrice = newPrice,
                eventLabel = eventLabel,
                dateRecorded = dateRecorded,
                onPriceDropBelowThreshold = { updatedDeal, previousPrice, currentPrice, threshold ->
                    com.example.util.PriceAlertNotificationManager.sendPriceDropAlertNotification(
                        context = getApplication(),
                        deal = updatedDeal,
                        oldPrice = previousPrice,
                        newPrice = currentPrice,
                        thresholdPrice = threshold
                    )
                }
            )

            // Granular Alert Evaluation
            val alertConfig = _granularPropertyAlerts.value[dealId]
            if (alertConfig != null && alertConfig.isAlertEnabled) {
                if (newPrice <= alertConfig.effectiveTriggerPrice) {
                    com.example.util.GranularNotificationManager.sendGranularPriceDropNotification(
                        context = getApplication(),
                        alertConfig = alertConfig,
                        oldPrice = oldPrice,
                        newPrice = newPrice
                    )
                    refreshGranularAlerts()
                }
            }

            val currentDetail = _uiState.value.selectedDealForDetail
            if (currentDetail?.id == dealId) {
                val newDiscount = if (currentDetail.estimatedMarketValue > 0) {
                    (((currentDetail.estimatedMarketValue - newPrice) / currentDetail.estimatedMarketValue) * 100).toInt()
                } else currentDetail.discountPercent
                _uiState.update {
                    it.copy(
                        selectedDealForDetail = currentDetail.copy(
                            askingPrice = newPrice,
                            discountPercent = newDiscount,
                            status = "PRICE_CUT"
                        )
                    )
                }
            }
        }
    }

    fun setPriceAlertThreshold(dealId: Long, threshold: Double?) {
        viewModelScope.launch {
            repository.updatePriceAlertThreshold(dealId, threshold)
            if (_uiState.value.selectedDealForDetail?.id == dealId) {
                _uiState.update {
                    it.copy(selectedDealForDetail = it.selectedDealForDetail?.copy(priceAlertThreshold = threshold))
                }
            }
        }
    }

    fun updateDealStage(dealId: Long, newStage: String) {
        viewModelScope.launch {
            repository.updateDealStage(dealId, newStage)
            syncOutboxManager.enqueueAction(
                actionType = com.example.data.SyncActionType.UPDATE_DEAL_STAGE,
                targetEntityId = dealId,
                payloadJson = "{\"dealStage\":\"$newStage\"}"
            )
            if (_uiState.value.selectedDealForDetail?.id == dealId) {
                _uiState.update {
                    it.copy(selectedDealForDetail = it.selectedDealForDetail?.copy(dealStage = newStage))
                }
            }
        }
    }

    fun forceSyncOutboxNow() {
        viewModelScope.launch {
            syncOutboxManager.flushPendingActions()
        }
    }

    fun triggerTestNotification(deal: PropertyDeal, targetThreshold: Double) {
        setPriceAlertThreshold(deal.id, targetThreshold)
        // Send immediate test alert notification
        com.example.util.PriceAlertNotificationManager.sendTestAlertNotification(
            context = getApplication(),
            dealTitle = deal.title,
            targetPrice = targetThreshold
        )
    }

    fun setSelectedDealForDetail(deal: PropertyDeal?) {
        _uiState.update { it.copy(selectedDealForDetail = deal) }
        if (deal != null) {
            viewModelScope.launch {
                repository.markDealAsViewed(deal.id)
            }
        }
    }

    fun clearRecentlyViewedHistory() {
        viewModelScope.launch {
            repository.clearRecentlyViewedHistory()
        }
    }

    fun toggleBookmark(deal: PropertyDeal) {
        viewModelScope.launch {
            val newBookmarkState = !deal.isBookmarked
            repository.toggleBookmark(deal.id, deal.isBookmarked)
            syncOutboxManager.enqueueAction(
                actionType = com.example.data.SyncActionType.TOGGLE_BOOKMARK,
                targetEntityId = deal.id,
                payloadJson = "{\"isBookmarked\":$newBookmarkState}"
            )
            // update selected deal view if open
            if (_uiState.value.selectedDealForDetail?.id == deal.id) {
                _uiState.update {
                    it.copy(selectedDealForDetail = deal.copy(isBookmarked = newBookmarkState))
                }
            }
        }
    }

    fun toggleBlindMode(active: Boolean) {
        viewModelScope.launch {
            repository.setBlindModeActive(active)
        }
    }

    fun unlockSingleDeal(dealId: Long) {
        viewModelScope.launch {
            repository.unlockDeal(dealId)
        }
    }

    fun useTokenToUnlockDeal(dealId: Long) {
        viewModelScope.launch {
            repository.useTokenToUnlockDeal(dealId)
        }
    }

    fun activateProMembership(plan: String = "ANNUAL") {
        viewModelScope.launch {
            authManager.setSubscriptionPlanAndClaims(
                plan = plan,
                role = "pro_investor"
            )
            repository.updateSubscriptionPlan(
                plan = plan,
                isPro = true,
                billingCycle = plan,
                renewalDate = if (plan == "ANNUAL") "18/08/2027" else "18/09/2026",
                claimsRole = "pro_investor"
            )
        }
    }

    fun setSubscriptionPlan(plan: String, isAnnual: Boolean = true) {
        viewModelScope.launch {
            val isPro = plan != "FREE"
            val billingCycle = if (plan == "ANNUAL") "ANNUAL" else if (plan == "MONTHLY") "MONTHLY" else "NONE"
            val renewalDate = if (plan == "ANNUAL") "18/08/2027" else if (plan == "MONTHLY") "18/09/2026" else "N/A"
            val role = if (isPro) "pro_investor" else "investor"

            authManager.setSubscriptionPlanAndClaims(
                plan = plan,
                role = role
            )
            repository.updateSubscriptionPlan(
                plan = plan,
                isPro = isPro,
                billingCycle = billingCycle,
                renewalDate = renewalDate,
                claimsRole = role
            )
        }
    }

    fun refreshFirebaseClaims(force: Boolean = true) {
        viewModelScope.launch {
            val claims = authManager.fetchCustomClaims(forceRefresh = force)
            val isPro = claims.isPremium
            repository.updateSubscriptionPlan(
                plan = claims.plan,
                isPro = isPro,
                billingCycle = if (claims.plan == "ANNUAL") "ANNUAL" else if (claims.plan == "MONTHLY") "MONTHLY" else "NONE",
                renewalDate = claims.formattedValidUntil,
                claimsRole = claims.role
            )
        }
    }

    fun updateSimulatedServerClaims(plan: String, isPremium: Boolean, role: String) {
        viewModelScope.launch {
            authManager.setSubscriptionPlanAndClaims(
                plan = plan,
                role = role
            )
            repository.updateSubscriptionPlan(
                plan = plan,
                isPro = isPremium,
                billingCycle = if (plan == "ANNUAL") "ANNUAL" else if (plan == "MONTHLY") "MONTHLY" else "NONE",
                renewalDate = if (plan == "ANNUAL") "18/08/2027" else if (plan == "MONTHLY") "18/09/2026" else "N/A",
                claimsRole = role
            )
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            authManager.setSubscriptionPlanAndClaims(
                plan = "FREE",
                role = "investor"
            )
            repository.updateSubscriptionPlan(
                plan = "FREE",
                isPro = false,
                billingCycle = "NONE",
                renewalDate = "Cancellato",
                claimsRole = "investor"
            )
        }
    }

    fun dismissSearchCoachMark() {
        viewModelScope.launch {
            repository.setHasSeenSearchCoachMark(true)
        }
    }

    fun resetSearchCoachMark() {
        viewModelScope.launch {
            repository.setHasSeenSearchCoachMark(false)
        }
    }

    fun submitOfferForDeal(dealId: Long, offerAmount: Double, newStage: String, notes: String) {
        viewModelScope.launch {
            repository.updateDealStage(dealId, newStage)
            val currentDeal = repository.getDealById(dealId)
            if (currentDeal != null) {
                val updatedNotes = if (currentDeal.notes.isBlank()) notes else "${currentDeal.notes}\n\n$notes"
                repository.updateDealNotes(dealId, updatedNotes)
            }
            if (_uiState.value.selectedDealForDetail?.id == dealId) {
                _uiState.update {
                    it.copy(selectedDealForDetail = it.selectedDealForDetail?.copy(dealStage = newStage))
                }
            }
        }
    }

    fun updateDealNotes(dealId: Long, notes: String) {
        viewModelScope.launch {
            repository.updateDealNotes(dealId, notes)
            if (_uiState.value.selectedDealForDetail?.id == dealId) {
                _uiState.update {
                    it.copy(selectedDealForDetail = it.selectedDealForDetail?.copy(notes = notes))
                }
            }
        }
    }

    fun setAddDealDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isAddDealDialogOpen = open) }
    }

    fun setSelectedSourceForParser(source: ScraperSource?) {
        _uiState.update { it.copy(selectedSourceForParser = source, parserTestResult = null) }
    }

    fun setParserEditorOpen(open: Boolean) {
        _uiState.update { it.copy(isParserEditorOpen = open) }
    }

    fun updateSourceConfigStatus(sourceId: String, status: String) {
        viewModelScope.launch {
            repository.updateSourceConfigStatus(sourceId, status)
        }
    }

    fun runParserTest(source: ScraperSource, sampleContent: String, rulesJson: String) {
        _uiState.update { it.copy(isRunningParserTest = true) }
        viewModelScope.launch {
            val result = repository.simulateParserTest(source, sampleContent, rulesJson)
            _uiState.update {
                it.copy(
                    isRunningParserTest = false,
                    parserTestResult = result,
                    parserTestLogs = result.logs
                )
            }
        }
    }

    fun importScrapedDeals(deals: List<PropertyDeal>) {
        viewModelScope.launch {
            repository.importScrapedDeals(deals)
            _uiState.update {
                it.copy(
                    parserTestLogs = it.parserTestLogs + ">>> SUCCESS: ${deals.size} Immobili Estratti dallo Scraper Importati nel Radar Feed!"
                )
            }
        }
    }

    fun executeLiveBatchScrape() {
        _uiState.update { it.copy(isRunningParserTest = true) }
        viewModelScope.launch {
            val summary = repository.executeLiveBatchScrapeAllSources()
            _uiState.update {
                it.copy(
                    isRunningParserTest = false,
                    parserTestResult = ScraperTestResult(
                        isSuccess = false,
                        extractedTitle = "Ingestione Dati Non Disponibile",
                        extractedPrice = null,
                        extractedMarketValue = null,
                        discountPercent = 0,
                        logs = summary.logs
                    ),
                    parserTestLogs = summary.logs
                )
            }
        }
    }

    fun validateSourceConfigurations() {
        _uiState.update { it.copy(isRunningParserTest = true) }
        viewModelScope.launch {
            val summary = repository.validateSourceConfigurations()
            _uiState.update {
                it.copy(
                    isRunningParserTest = false,
                    parserTestResult = ScraperTestResult(
                        isSuccess = summary.warningCount == 0,
                        extractedTitle = "Validazione Configurazioni (${summary.activeCount}/${summary.totalSources} Valide)",
                        extractedPrice = null,
                        extractedMarketValue = null,
                        discountPercent = 0,
                        logs = summary.logs
                    ),
                    parserTestLogs = summary.logs
                )
            }
        }
    }

    fun saveParserRules(sourceId: String, rulesJson: String) {
        viewModelScope.launch {
            repository.updateSourceParserRules(sourceId, rulesJson)
            setParserEditorOpen(false)
        }
    }

    fun addNewDeal(
        title: String,
        sourceKey: String,
        sourceName: String,
        location: String,
        propertyType: String,
        askingPrice: Double,
        marketValue: Double?,
        sqm: Int,
        auctionDate: String?
    ) {
        viewModelScope.launch {
            val rawDeal = PropertyDeal(
                title = title.ifBlank { "Immobile Opportunità Radar" },
                sourceKey = sourceKey,
                sourceName = sourceName.ifBlank { "Fonte Personalizzata" },
                sourceUrl = "https://www.quimmo.it/annunci-immobiliari",
                location = location.ifBlank { "Milano (MI)" },
                propertyType = propertyType,
                askingPrice = askingPrice,
                // Se l'utente non inserisce una stima di mercato, non se ne
                // inventa una: si riflette il prezzo richiesto, l'unico dato reale.
                estimatedMarketValue = marketValue ?: askingPrice,
                surfaceSqm = sqm,
                discountPercent = 0,
                estimatedCapRate = 7.0,
                auctionDate = auctionDate,
                status = "LIVE",
                imageUrl = "https://images.unsplash.com/photo-1560518883-ce09059eeffa?auto=format&fit=crop&w=800&q=80",
                notes = "Aggiunto manualmente a Quantum Deal Radar."
            )

            val validatedDeal = when (val validation = com.example.util.DealDataValidator.validateAndSanitize(rawDeal)) {
                is com.example.util.DealValidationResult.Valid -> validation.sanitizedDeal
                is com.example.util.DealValidationResult.Invalid -> rawDeal
            }

            repository.addDeal(validatedDeal)
            syncOutboxManager.enqueueAction(
                actionType = com.example.data.SyncActionType.UPDATE_PROPERTY_STATUS,
                targetEntityId = validatedDeal.id,
                payloadJson = "{\"action\":\"CREATE_DEAL\",\"title\":\"${validatedDeal.title}\"}"
            )
            setAddDealDialogOpen(false)
        }
    }

    fun deleteDeal(deal: PropertyDeal) {
        viewModelScope.launch {
            repository.deleteDeal(deal)
            if (_uiState.value.selectedDealForDetail?.id == deal.id) {
                _uiState.update { it.copy(selectedDealForDetail = null) }
            }
        }
    }

    // ROI Calculator updates
    fun updateRoiPrice(valStr: String) {
        _roiCalculatorState.update { it.copy(purchasePriceStr = valStr) }
    }

    fun updateRoiRenovation(valStr: String) {
        _roiCalculatorState.update { it.copy(renovationCostStr = valStr) }
    }

    fun updateRoiLegalFees(valStr: String) {
        _roiCalculatorState.update { it.copy(legalAuctionFeesStr = valStr) }
    }

    fun updateRoiMonthlyRent(valStr: String) {
        _roiCalculatorState.update { it.copy(monthlyRentStr = valStr) }
    }

    fun updateRoiMonthlyExpenses(valStr: String) {
        _roiCalculatorState.update { it.copy(monthlyExpensesStr = valStr) }
    }

    fun updateRoiDownPayment(valStr: String) {
        _roiCalculatorState.update { it.copy(downPaymentPercentStr = valStr) }
    }

    fun updateRoiMortgageRate(valStr: String) {
        _roiCalculatorState.update { it.copy(mortgageRateStr = valStr) }
    }

    fun updateRoiLoanTerm(valStr: String) {
        _roiCalculatorState.update { it.copy(loanTermYearsStr = valStr) }
    }

    fun updateRoiResale(valStr: String) {
        _roiCalculatorState.update { it.copy(expectedResaleStr = valStr) }
    }

    fun applyRoiPreset(downPayment: String, mortgageRate: String, loanTerm: String) {
        _roiCalculatorState.update {
            it.copy(
                downPaymentPercentStr = downPayment,
                mortgageRateStr = mortgageRate,
                loanTermYearsStr = loanTerm
            )
        }
    }

    fun loadDealIntoCalculator(deal: PropertyDeal) {
        _roiCalculatorState.update {
            it.copy(
                purchasePriceStr = deal.askingPrice.toInt().toString(),
                renovationCostStr = (deal.surfaceSqm * 350).toString(),
                legalAuctionFeesStr = (deal.askingPrice * 0.04).toInt().toString(),
                monthlyRentStr = (deal.estimatedMarketValue * 0.005).toInt().toString(),
                monthlyExpensesStr = (deal.estimatedMarketValue * 0.001).toInt().toString(),
                expectedResaleStr = deal.estimatedMarketValue.toInt().toString()
            )
        }
        setMainTab(MainTab.ROI_CALCULATOR)
    }

    fun getPriceHistoryForDeal(dealId: Long): Flow<List<PriceHistory>> {
        return repository.getPriceHistoryForDeal(dealId)
    }

    fun openMarketTrendDrawer(region: String = "Milano") {
        _uiState.update {
            it.copy(
                isMarketTrendDrawerOpen = true,
                selectedRegionForReport = if (region.isNotBlank()) region else it.selectedRegionForReport
            )
        }
    }

    fun closeMarketTrendDrawer() {
        _uiState.update { it.copy(isMarketTrendDrawerOpen = false) }
    }

    fun setSelectedRegionForReport(region: String) {
        _uiState.update { it.copy(selectedRegionForReport = region) }
        viewModelScope.launch {
            preferencesDataStore.updateDashboardFilters(regionForReport = region)
        }
    }

    fun generateMarketTrendReport(region: String) {
        val regionName = if (region.isNotBlank()) region else _uiState.value.selectedRegionForReport
        _uiState.update {
            it.copy(
                selectedRegionForReport = regionName,
                isGeneratingMarketReport = true,
                marketReportContent = null,
                marketReportError = null
            )
        }

        viewModelScope.launch {
            val allDealsList = allDeals.value
            val regionDeals = allDealsList.filter {
                it.location.contains(regionName, ignoreCase = true) ||
                        it.title.contains(regionName, ignoreCase = true)
            }

            val result = com.example.util.GeminiMarketReportService.generateMonthlyMarketReport(
                regionName = regionName,
                regionDeals = regionDeals
            )

            result.fold(
                onSuccess = { content ->
                    _uiState.update {
                        it.copy(
                            isGeneratingMarketReport = false,
                            marketReportContent = content,
                            marketReportError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isGeneratingMarketReport = false,
                            marketReportContent = null,
                            marketReportError = error.message ?: "Errore sconosciuto durante la generazione del report."
                        )
                    }
                }
            )
        }
    }

    // --- Offline Map Room Caching ---
    val offlineMapRepository by lazy {
        OfflineMapCacheRepository(AppDatabase.getDatabase(getApplication()).mapTileCacheDao())
    }

    val cachedTileCount = offlineMapRepository.cachedTileCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    val totalMapCacheSizeBytes = offlineMapRepository.totalCacheSizeBytes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )

    val offlineRegions = offlineMapRepository.offlineRegions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val isDownloadingMapRegion = MutableStateFlow(false)
    val mapDownloadProgress = MutableStateFlow(Pair(0, 0))

    fun downloadOfflineMapForDeal(deal: PropertyDeal, onComplete: () -> Unit = {}) {
        val coords = deal.effectiveLatitude?.let { lat -> deal.effectiveLongitude?.let { lng -> com.example.util.GeoUtils.LatLng(lat, lng) } }
            ?: com.example.util.GeoUtils.getCoordinatesForLocation(deal.location)
        if (coords == null) {
            onComplete()
            return
        }
        viewModelScope.launch {
            isDownloadingMapRegion.value = true
            mapDownloadProgress.value = Pair(0, 50)
            offlineMapRepository.downloadRegionForOfflineVisit(
                regionName = "Sopralluogo: ${deal.title.take(20)} (${deal.location})",
                propertyId = deal.id,
                centerLat = coords.latitude,
                centerLng = coords.longitude,
                radiusKm = 1.5,
                minZoom = 12,
                maxZoom = 16
            ) { downloaded, total ->
                mapDownloadProgress.value = Pair(downloaded, total)
            }
            isDownloadingMapRegion.value = false
            onComplete()
        }
    }

    fun downloadOfflineMapForCoordinates(
        regionName: String,
        lat: Double,
        lng: Double,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            isDownloadingMapRegion.value = true
            mapDownloadProgress.value = Pair(0, 50)
            offlineMapRepository.downloadRegionForOfflineVisit(
                regionName = regionName,
                propertyId = null,
                centerLat = lat,
                centerLng = lng,
                radiusKm = 2.0,
                minZoom = 12,
                maxZoom = 16
            ) { downloaded, total ->
                mapDownloadProgress.value = Pair(downloaded, total)
            }
            isDownloadingMapRegion.value = false
            onComplete()
        }
    }

    fun clearAllOfflineMapCache() {
        viewModelScope.launch {
            offlineMapRepository.clearAllCache()
        }
    }

    fun deleteOfflineMapRegion(id: Long) {
        viewModelScope.launch {
            offlineMapRepository.deleteRegion(id)
        }
    }

    // Property Comparison Management
    fun toggleDealComparison(dealId: Long) {
        _uiState.update { current ->
            val newSet = if (current.selectedComparisonDealIds.contains(dealId)) {
                current.selectedComparisonDealIds - dealId
            } else {
                current.selectedComparisonDealIds + dealId
            }
            current.copy(selectedComparisonDealIds = newSet)
        }
    }

    fun clearComparisonSelection() {
        _uiState.update { it.copy(selectedComparisonDealIds = emptySet(), isComparisonSheetOpen = false) }
    }

    fun setComparisonSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isComparisonSheetOpen = open) }
    }

    fun removeDealFromComparison(dealId: Long) {
        _uiState.update { current ->
            val newSet = current.selectedComparisonDealIds - dealId
            current.copy(
                selectedComparisonDealIds = newSet,
                isComparisonSheetOpen = if (newSet.size < 2) false else current.isComparisonSheetOpen
            )
        }
    }

    // My Properties Pipeline Actions
    fun addDealToMyProperties(
        deal: PropertyDeal,
        pipelineStatus: PipelineStatus = PipelineStatus.ANALYZED,
        renovationCost: Double = 0.0,
        targetResale: Double = 0.0,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val property = Property(
                title = deal.title,
                address = deal.location,
                price = deal.askingPrice,
                distressStatus = deal.status,
                propertyType = deal.propertyType,
                estimatedMarketValue = deal.estimatedMarketValue,
                surfaceSqm = deal.surfaceSqm,
                notes = deal.notes.ifBlank { "Analizzato tramite Radar Deals (${deal.sourceName})" },
                pipelineStatus = pipelineStatus.key,
                estimatedRenovationCost = renovationCost,
                targetResalePrice = if (targetResale > 0) targetResale else deal.estimatedMarketValue,
                latitude = deal.effectiveLatitude,
                longitude = deal.effectiveLongitude,
                geolocationPrecision = deal.effectiveGeolocationPrecision.name
            )
            repository.insertSavedProperty(property)
            onSuccess()
        }
    }

    fun updateSavedPropertyPipelineStatus(propertyId: Long, newStatus: PipelineStatus) {
        viewModelScope.launch {
            repository.updateSavedPropertyPipelineStatus(propertyId, newStatus.key)
        }
    }

    // ==========================================
    // Granular Notification Methods
    // ==========================================

    fun refreshGranularAlerts() {
        _granularPropertyAlerts.value = com.example.util.GranularNotificationManager.loadPropertyAlerts(getApplication())
        _granularAlertHistory.value = com.example.util.GranularNotificationManager.loadAlertHistory(getApplication())
        _globalNotificationSettings.value = com.example.util.GranularNotificationManager.loadGlobalSettings(getApplication())
    }

    fun saveGranularPropertyAlert(alert: GranularPropertyAlert) {
        viewModelScope.launch {
            com.example.util.GranularNotificationManager.savePropertyAlert(getApplication(), alert)
            // Also sync the priceAlertThreshold on property_deals DB if applicable
            repository.updatePriceAlertThreshold(alert.dealId, alert.effectiveTriggerPrice)
            refreshGranularAlerts()
        }
    }

    fun saveGlobalNotificationSettings(settings: GlobalNotificationSettings) {
        viewModelScope.launch {
            com.example.util.GranularNotificationManager.saveGlobalSettings(getApplication(), settings)
            _globalNotificationSettings.value = settings
        }
    }

    fun applyAlertPresetToAll(dropPercent: Double, isAuctionOnly: Boolean = false) {
        viewModelScope.launch {
            val currentAlerts = _granularPropertyAlerts.value.toMutableMap()
            allDeals.value.forEach { deal ->
                val isAuction = deal.propertyType.contains("Asta", ignoreCase = true) || deal.sourceKey.contains("asta", ignoreCase = true)
                if (!isAuctionOnly || isAuction) {
                    val existing = currentAlerts[deal.id]
                    val updated = (existing ?: GranularPropertyAlert(
                        dealId = deal.id,
                        dealTitle = deal.title,
                        dealLocation = deal.location,
                        currentAskingPrice = deal.askingPrice,
                        originalAskingPrice = deal.askingPrice,
                        estimatedMarketValue = deal.estimatedMarketValue,
                        propertyType = deal.propertyType,
                        imageUrl = deal.imageUrl
                    )).copy(
                        isAlertEnabled = true,
                        triggerMode = AlertTriggerMode.PERCENTAGE_DROP,
                        dropPercentThreshold = dropPercent,
                        targetPriceThreshold = deal.askingPrice * (1.0 - (dropPercent / 100.0))
                    )
                    currentAlerts[deal.id] = updated
                    repository.updatePriceAlertThreshold(deal.id, updated.effectiveTriggerPrice)
                }
            }
            com.example.util.GranularNotificationManager.saveAllPropertyAlerts(getApplication(), currentAlerts)
            refreshGranularAlerts()
        }
    }

    fun toggleAllAlerts(enable: Boolean) {
        viewModelScope.launch {
            val currentAlerts = _granularPropertyAlerts.value.toMutableMap()
            allDeals.value.forEach { deal ->
                val existing = currentAlerts[deal.id] ?: GranularPropertyAlert(
                    dealId = deal.id,
                    dealTitle = deal.title,
                    dealLocation = deal.location,
                    currentAskingPrice = deal.askingPrice,
                    originalAskingPrice = deal.askingPrice,
                    estimatedMarketValue = deal.estimatedMarketValue,
                    propertyType = deal.propertyType,
                    imageUrl = deal.imageUrl
                )
                currentAlerts[deal.id] = existing.copy(isAlertEnabled = enable)
            }
            com.example.util.GranularNotificationManager.saveAllPropertyAlerts(getApplication(), currentAlerts)
            refreshGranularAlerts()
        }
    }

    fun sendTestNotificationForProperty(alert: GranularPropertyAlert) {
        com.example.util.GranularNotificationManager.sendTestGranularNotification(getApplication(), alert)
    }

    fun simulatePriceDropForDeal(dealId: Long, newPrice: Double) {
        recordPriceDrop(
            dealId = dealId,
            newPrice = newPrice,
            eventLabel = "Ribasso Simulato / Esperimento Asta",
            dateRecorded = "Oggi"
        )
    }

    fun clearGranularAlertHistory() {
        com.example.util.GranularNotificationManager.clearAlertHistory(getApplication())
        _granularAlertHistory.value = emptyList()
    }

    fun markGranularAlertAsRead(alertId: String) {
        com.example.util.GranularNotificationManager.markHistoryEventAsRead(getApplication(), alertId)
        _granularAlertHistory.value = com.example.util.GranularNotificationManager.loadAlertHistory(getApplication())
    }

    // ==========================================
    // FCM Real-Time Push Notification Controls
    // ==========================================

    fun openFcmPushCenter() {
        _showFcmPushCenterDialog.value = true
        refreshFcmData()
    }

    fun dismissFcmPushCenter() {
        _showFcmPushCenterDialog.value = false
    }

    fun refreshFcmData() {
        viewModelScope.launch {
            _fcmToken.value = com.example.util.FcmPushManager.getCachedToken(getApplication())
            _fcmMasterEnabled.value = com.example.util.FcmPushManager.isMasterPushEnabled(getApplication())
            _fcmSubscribedTopics.value = com.example.util.FcmPushManager.loadSubscribedTopics(getApplication())
            _fcmPushHistory.value = com.example.util.FcmPushManager.loadPushHistory(getApplication())

            com.example.util.FcmPushManager.fetchToken(getApplication()) { token ->
                _fcmToken.value = token
            }
        }
    }

    fun toggleFcmMasterEnabled(enabled: Boolean) {
        com.example.util.FcmPushManager.setMasterPushEnabled(getApplication(), enabled)
        _fcmMasterEnabled.value = enabled
    }

    fun toggleFcmTopicSubscription(topicId: String, isSubscribed: Boolean) {
        com.example.util.FcmPushManager.setTopicSubscribed(getApplication(), topicId, isSubscribed)
        _fcmSubscribedTopics.value = com.example.util.FcmPushManager.loadSubscribedTopics(getApplication())
    }

    fun simulateFcmPushAlert(
        type: com.example.data.FcmPushType,
        dealId: Long? = null,
        propertyTitle: String? = null,
        address: String? = null,
        city: String? = null,
        oldPrice: Double? = null,
        newPrice: Double? = null,
        oldStatus: String? = null,
        newStatus: String? = null,
        customNote: String? = null
    ) {
        val targetDeal = dealId?.let { id -> allDeals.value.find { it.id == id } } ?: allDeals.value.firstOrNull()
        
        val pTitle = propertyTitle ?: targetDeal?.title ?: "Quadrilocale Panoramico con Terrazzo"
        val pAddress = address ?: targetDeal?.location ?: "Via Tortona 35"
        val pCity = city ?: targetDeal?.location?.split(",")?.lastOrNull()?.trim() ?: "Milano"
        val pOldPrice = oldPrice ?: (targetDeal?.askingPrice?.times(1.25) ?: 320000.0)
        val pNewPrice = newPrice ?: (targetDeal?.askingPrice ?: 240000.0)
        val pOldStatus = oldStatus ?: "In Corso"
        val pNewStatus = newStatus ?: "2° Incanto Deserto - Ribasso -25%"
        val pNote = customNote ?: "Algoritmo Quantum Radar: Sottocosto del 38% rispetto ai valori OMI."

        com.example.util.FcmPushManager.simulatePushAlert(
            context = getApplication(),
            type = type,
            dealId = targetDeal?.id ?: 101L,
            propertyTitle = pTitle,
            address = pAddress,
            city = pCity,
            oldPrice = pOldPrice,
            newPrice = pNewPrice,
            oldStatus = pOldStatus,
            newStatus = pNewStatus,
            customNote = pNote
        )

        _fcmPushHistory.value = com.example.util.FcmPushManager.loadPushHistory(getApplication())
    }

    fun markFcmPushAsRead(alertId: String) {
        com.example.util.FcmPushManager.markPushAsRead(getApplication(), alertId)
        _fcmPushHistory.value = com.example.util.FcmPushManager.loadPushHistory(getApplication())
    }

    fun clearFcmPushHistory() {
        com.example.util.FcmPushManager.clearPushHistory(getApplication())
        _fcmPushHistory.value = emptyList()
    }
}
