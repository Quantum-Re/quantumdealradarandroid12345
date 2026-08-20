package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.FilterCategory
import com.example.ui.MainTab
import com.example.ui.theme.ThemeMode
import com.example.util.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "quantum_deal_radar_user_preferences")

data class UserSessionData(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val appLanguage: AppLanguage = AppLanguage.IT,
    val selectedMainTab: MainTab = MainTab.RADAR_FEED,
    val hasSeenOnboarding: Boolean = false,
    val hasSeenSearchCoachMark: Boolean = false,
    val lastActiveEmail: String? = null,
    val isOfflineModeForced: Boolean = false,
    val lastSyncedTimestamp: Long = 0L
)

data class DashboardFilterPreferences(
    val searchQuery: String = "",
    val selectedFilterCategory: FilterCategory = FilterCategory.ALL,
    val selectedSourceFilter: String = "ALL",
    val selectedPropertyTypeFilter: String = "ALL",
    val hideExpiredAuctions: Boolean = false,
    val selectedRegionForReport: String = "Milano"
)

data class PropertyPipelineFilterPreferences(
    val searchQuery: String = "",
    val selectedStatusFilter: String = "ALL",
    val selectedPipelineFilter: String = "ALL",
    val selectedSortOption: PropertySortOption = PropertySortOption.DATE_ADDED_DESC,
    val showOnlyDistressed: Boolean = false,
    val showOnlyUndervalued: Boolean = false,
    val selectedStrategies: Set<String> = emptySet()
)

class UserPreferencesDataStore private constructor(private val context: Context) {

    private val dataStore = context.userPreferencesDataStore

    private object PreferencesKeys {
        // Session keys
        val THEME_MODE = stringPreferencesKey("session_theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("session_app_language")
        val SELECTED_MAIN_TAB = stringPreferencesKey("session_selected_main_tab")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("session_has_seen_onboarding")
        val HAS_SEEN_SEARCH_COACH_MARK = booleanPreferencesKey("session_has_seen_coach_mark")
        val LAST_ACTIVE_EMAIL = stringPreferencesKey("session_last_active_email")
        val IS_OFFLINE_MODE_FORCED = booleanPreferencesKey("session_is_offline_mode_forced")
        val LAST_SYNCED_TIMESTAMP = longPreferencesKey("session_last_synced_timestamp")

        // Dashboard & Feed Filter keys
        val FEED_SEARCH_QUERY = stringPreferencesKey("feed_search_query")
        val FEED_FILTER_CATEGORY = stringPreferencesKey("feed_filter_category")
        val FEED_SOURCE_FILTER = stringPreferencesKey("feed_source_filter")
        val FEED_PROPERTY_TYPE_FILTER = stringPreferencesKey("feed_property_type_filter")
        val FEED_HIDE_EXPIRED_AUCTIONS = booleanPreferencesKey("feed_hide_expired_auctions")
        val FEED_SELECTED_REGION_FOR_REPORT = stringPreferencesKey("feed_selected_region_report")

        // Property Pipeline Filter keys
        val PIPELINE_SEARCH_QUERY = stringPreferencesKey("pipeline_search_query")
        val PIPELINE_STATUS_FILTER = stringPreferencesKey("pipeline_status_filter")
        val PIPELINE_STAGE_FILTER = stringPreferencesKey("pipeline_stage_filter")
        val PIPELINE_SORT_OPTION = stringPreferencesKey("pipeline_sort_option")
        val PIPELINE_SHOW_ONLY_DISTRESSED = booleanPreferencesKey("pipeline_show_only_distressed")
        val PIPELINE_SHOW_ONLY_UNDERVALUED = booleanPreferencesKey("pipeline_show_only_undervalued")
        val PIPELINE_SELECTED_STRATEGIES = stringSetPreferencesKey("pipeline_selected_strategies")
    }

    val sessionDataFlow: Flow<UserSessionData> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val themeStr = prefs[PreferencesKeys.THEME_MODE]
            val themeMode = try {
                if (themeStr != null) ThemeMode.valueOf(themeStr) else ThemeMode.DARK
            } catch (e: Exception) {
                ThemeMode.DARK
            }

            val langStr = prefs[PreferencesKeys.APP_LANGUAGE]
            val appLanguage = try {
                if (langStr != null) AppLanguage.valueOf(langStr) else AppLanguage.IT
            } catch (e: Exception) {
                AppLanguage.IT
            }

            val tabStr = prefs[PreferencesKeys.SELECTED_MAIN_TAB]
            val selectedMainTab = try {
                if (tabStr != null) MainTab.valueOf(tabStr) else MainTab.RADAR_FEED
            } catch (e: Exception) {
                MainTab.RADAR_FEED
            }

            UserSessionData(
                themeMode = themeMode,
                appLanguage = appLanguage,
                selectedMainTab = selectedMainTab,
                hasSeenOnboarding = prefs[PreferencesKeys.HAS_SEEN_ONBOARDING] ?: false,
                hasSeenSearchCoachMark = prefs[PreferencesKeys.HAS_SEEN_SEARCH_COACH_MARK] ?: false,
                lastActiveEmail = prefs[PreferencesKeys.LAST_ACTIVE_EMAIL],
                isOfflineModeForced = prefs[PreferencesKeys.IS_OFFLINE_MODE_FORCED] ?: false,
                lastSyncedTimestamp = prefs[PreferencesKeys.LAST_SYNCED_TIMESTAMP] ?: 0L
            )
        }

    val dashboardFilterFlow: Flow<DashboardFilterPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val catStr = prefs[PreferencesKeys.FEED_FILTER_CATEGORY]
            val category = try {
                if (catStr != null) FilterCategory.valueOf(catStr) else FilterCategory.ALL
            } catch (e: Exception) {
                FilterCategory.ALL
            }

            DashboardFilterPreferences(
                searchQuery = prefs[PreferencesKeys.FEED_SEARCH_QUERY] ?: "",
                selectedFilterCategory = category,
                selectedSourceFilter = prefs[PreferencesKeys.FEED_SOURCE_FILTER] ?: "ALL",
                selectedPropertyTypeFilter = prefs[PreferencesKeys.FEED_PROPERTY_TYPE_FILTER] ?: "ALL",
                hideExpiredAuctions = prefs[PreferencesKeys.FEED_HIDE_EXPIRED_AUCTIONS] ?: false,
                selectedRegionForReport = prefs[PreferencesKeys.FEED_SELECTED_REGION_FOR_REPORT] ?: "Milano"
            )
        }

    val pipelineFilterFlow: Flow<PropertyPipelineFilterPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val sortKey = prefs[PreferencesKeys.PIPELINE_SORT_OPTION]
            val sortOption = if (sortKey != null) PropertySortOption.fromKey(sortKey) else PropertySortOption.DATE_ADDED_DESC

            PropertyPipelineFilterPreferences(
                searchQuery = prefs[PreferencesKeys.PIPELINE_SEARCH_QUERY] ?: "",
                selectedStatusFilter = prefs[PreferencesKeys.PIPELINE_STATUS_FILTER] ?: "ALL",
                selectedPipelineFilter = prefs[PreferencesKeys.PIPELINE_STAGE_FILTER] ?: "ALL",
                selectedSortOption = sortOption,
                showOnlyDistressed = prefs[PreferencesKeys.PIPELINE_SHOW_ONLY_DISTRESSED] ?: false,
                showOnlyUndervalued = prefs[PreferencesKeys.PIPELINE_SHOW_ONLY_UNDERVALUED] ?: false,
                selectedStrategies = prefs[PreferencesKeys.PIPELINE_SELECTED_STRATEGIES] ?: emptySet()
            )
        }

    suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun updateLanguage(lang: AppLanguage) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_LANGUAGE] = lang.name
        }
    }

    suspend fun updateSelectedMainTab(tab: MainTab) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_MAIN_TAB] = tab.name
        }
    }

    suspend fun updateHasSeenOnboarding(seen: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.HAS_SEEN_ONBOARDING] = seen
        }
    }

    suspend fun updateHasSeenSearchCoachMark(seen: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.HAS_SEEN_SEARCH_COACH_MARK] = seen
        }
    }

    suspend fun updateLastActiveEmail(email: String?) {
        dataStore.edit { prefs ->
            if (email != null) {
                prefs[PreferencesKeys.LAST_ACTIVE_EMAIL] = email
            } else {
                prefs.remove(PreferencesKeys.LAST_ACTIVE_EMAIL)
            }
        }
    }

    suspend fun updateOfflineModeForced(forced: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_OFFLINE_MODE_FORCED] = forced
        }
    }

    suspend fun updateLastSyncedTimestamp(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_SYNCED_TIMESTAMP] = timestamp
        }
    }

    suspend fun updateDashboardFilters(
        searchQuery: String? = null,
        filterCategory: FilterCategory? = null,
        sourceFilter: String? = null,
        propertyTypeFilter: String? = null,
        hideExpiredAuctions: Boolean? = null,
        regionForReport: String? = null
    ) {
        dataStore.edit { prefs ->
            searchQuery?.let { prefs[PreferencesKeys.FEED_SEARCH_QUERY] = it }
            filterCategory?.let { prefs[PreferencesKeys.FEED_FILTER_CATEGORY] = it.name }
            sourceFilter?.let { prefs[PreferencesKeys.FEED_SOURCE_FILTER] = it }
            propertyTypeFilter?.let { prefs[PreferencesKeys.FEED_PROPERTY_TYPE_FILTER] = it }
            hideExpiredAuctions?.let { prefs[PreferencesKeys.FEED_HIDE_EXPIRED_AUCTIONS] = it }
            regionForReport?.let { prefs[PreferencesKeys.FEED_SELECTED_REGION_FOR_REPORT] = it }
        }
    }

    suspend fun clearDashboardFilters() {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.FEED_SEARCH_QUERY] = ""
            prefs[PreferencesKeys.FEED_FILTER_CATEGORY] = FilterCategory.ALL.name
            prefs[PreferencesKeys.FEED_SOURCE_FILTER] = "ALL"
            prefs[PreferencesKeys.FEED_PROPERTY_TYPE_FILTER] = "ALL"
            prefs[PreferencesKeys.FEED_HIDE_EXPIRED_AUCTIONS] = false
        }
    }

    suspend fun updatePipelineFilters(
        searchQuery: String? = null,
        statusFilter: String? = null,
        pipelineFilter: String? = null,
        sortOption: PropertySortOption? = null,
        showOnlyDistressed: Boolean? = null,
        showOnlyUndervalued: Boolean? = null,
        strategies: Set<String>? = null
    ) {
        dataStore.edit { prefs ->
            searchQuery?.let { prefs[PreferencesKeys.PIPELINE_SEARCH_QUERY] = it }
            statusFilter?.let { prefs[PreferencesKeys.PIPELINE_STATUS_FILTER] = it }
            pipelineFilter?.let { prefs[PreferencesKeys.PIPELINE_STAGE_FILTER] = it }
            sortOption?.let { prefs[PreferencesKeys.PIPELINE_SORT_OPTION] = it.key }
            showOnlyDistressed?.let { prefs[PreferencesKeys.PIPELINE_SHOW_ONLY_DISTRESSED] = it }
            showOnlyUndervalued?.let { prefs[PreferencesKeys.PIPELINE_SHOW_ONLY_UNDERVALUED] = it }
            strategies?.let { prefs[PreferencesKeys.PIPELINE_SELECTED_STRATEGIES] = it }
        }
    }

    suspend fun clearPipelineFilters() {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.PIPELINE_SEARCH_QUERY] = ""
            prefs[PreferencesKeys.PIPELINE_STATUS_FILTER] = "ALL"
            prefs[PreferencesKeys.PIPELINE_STAGE_FILTER] = "ALL"
            prefs[PreferencesKeys.PIPELINE_SORT_OPTION] = PropertySortOption.DATE_ADDED_DESC.key
            prefs[PreferencesKeys.PIPELINE_SHOW_ONLY_DISTRESSED] = false
            prefs[PreferencesKeys.PIPELINE_SHOW_ONLY_UNDERVALUED] = false
            prefs[PreferencesKeys.PIPELINE_SELECTED_STRATEGIES] = emptySet()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesDataStore? = null

        fun getInstance(context: Context): UserPreferencesDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
