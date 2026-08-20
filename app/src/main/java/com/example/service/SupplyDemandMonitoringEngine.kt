package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.*
import com.example.util.MarketEstimateService
import com.example.util.ProvinceScrapedKpi
import com.example.util.SupplyDemandNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.abs

object SupplyDemandMonitoringEngine {
    private const val TAG = "SupplyDemandEngine"
    private const val PREFS_NAME = "supply_demand_monitoring_prefs"
    private const val KEY_SETTINGS_JSON = "monitoring_settings"
    private const val KEY_SNAPSHOTS_JSON = "location_snapshots"
    private const val KEY_ALERT_HISTORY_JSON = "alert_history"

    private val mutex = Mutex()

    private val _alertsFlow = MutableStateFlow<List<SupplyDemandAlertRecord>>(emptyList())
    val alertsFlow: StateFlow<List<SupplyDemandAlertRecord>> = _alertsFlow.asStateFlow()

    private val _isScanningFlow = MutableStateFlow(false)
    val isScanningFlow: StateFlow<Boolean> = _isScanningFlow.asStateFlow()

    private val _settingsFlow = MutableStateFlow(SupplyDemandMonitoringSettings())
    val settingsFlow: StateFlow<SupplyDemandMonitoringSettings> = _settingsFlow.asStateFlow()

    private val _monitoredZonesFlow = MutableStateFlow<List<SupplyDemandSnapshot>>(emptyList())
    val monitoredZonesFlow: StateFlow<List<SupplyDemandSnapshot>> = _monitoredZonesFlow.asStateFlow()

    fun initialize(context: Context) {
        val settings = loadSettings(context)
        _settingsFlow.value = settings
        _alertsFlow.value = loadAlertHistory(context)
        _monitoredZonesFlow.value = loadSnapshots(context).values.toList()
        SupplyDemandNotificationManager.ensureNotificationChannel(context)
    }

    fun loadSettings(context: Context): SupplyDemandMonitoringSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_SETTINGS_JSON, null) ?: return SupplyDemandMonitoringSettings()
        return try {
            val json = JSONObject(jsonStr)
            SupplyDemandMonitoringSettings(
                isEnabled = json.optBoolean("isEnabled", true),
                pushNotificationsEnabled = json.optBoolean("pushNotificationsEnabled", true),
                sensitivityThresholdPercent = json.optDouble("sensitivityThresholdPercent", 10.0),
                checkIntervalMinutes = json.optLong("checkIntervalMinutes", 15L),
                monitorSavedPortfolio = json.optBoolean("monitorSavedPortfolio", true),
                monitorSavedDeals = json.optBoolean("monitorSavedDeals", true),
                lastScanTimestamp = json.optLong("lastScanTimestamp", 0L),
                lastScanResultsCount = json.optInt("lastScanResultsCount", 0)
            )
        } catch (e: Exception) {
            SupplyDemandMonitoringSettings()
        }
    }

    fun saveSettings(context: Context, settings: SupplyDemandMonitoringSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val json = JSONObject().apply {
                put("isEnabled", settings.isEnabled)
                put("pushNotificationsEnabled", settings.pushNotificationsEnabled)
                put("sensitivityThresholdPercent", settings.sensitivityThresholdPercent)
                put("checkIntervalMinutes", settings.checkIntervalMinutes)
                put("monitorSavedPortfolio", settings.monitorSavedPortfolio)
                put("monitorSavedDeals", settings.monitorSavedDeals)
                put("lastScanTimestamp", settings.lastScanTimestamp)
                put("lastScanResultsCount", settings.lastScanResultsCount)
            }
            prefs.edit().putString(KEY_SETTINGS_JSON, json.toString()).apply()
            _settingsFlow.value = settings
            Log.d(TAG, "Saved monitoring settings: isEnabled=${settings.isEnabled}, threshold=${settings.sensitivityThresholdPercent}%")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving settings", e)
        }
    }

    /**
     * Calculates the composite Supply-Demand Ratio Index (0 to 100).
     * High value (70-100) = Heavy demand pressure, tight inventory (Seller's Market).
     * Medium value (40-69) = Balanced equilibrium.
     * Low value (0-39) = Oversupply / Glut, sluggish demand (Buyer's Market).
     */
    fun computeSupplyDemandRatio(
        saturationScore: Int?,
        daysOnMarket: Int?,
        absorptionRate: Double?,
        saleTrendYoY: Double?,
        rentTrendYoY: Double?
    ): Double {
        val safeAbsorption = (absorptionRate ?: 60.0).coerceIn(10.0, 95.0)
        val safeDom = (daysOnMarket ?: 90).coerceIn(30, 240)
        val safeSat = (saturationScore ?: 50).coerceIn(5, 95)

        // Absorption component (0 to 40 pts)
        val absorptionScore = (safeAbsorption / 95.0) * 40.0

        // DOM speed component (0 to 35 pts) - shorter DOM yields higher demand pressure
        val domSpeedScore = ((240.0 - safeDom) / 210.0).coerceIn(0.0, 1.0) * 35.0

        // Inverted saturation component (0 to 25 pts) - lower saturation = less supply glut
        val supplyTightnessScore = ((100.0 - safeSat) / 100.0).coerceIn(0.0, 1.0) * 25.0

        return (absorptionScore + domSpeedScore + supplyTightnessScore).coerceIn(5.0, 98.0)
    }

    fun getTensionLabel(sdrIndex: Double): String {
        return when {
            sdrIndex >= 75.0 -> "🔥 Shock di Domanda / Forte Scarsità Offerta"
            sdrIndex >= 60.0 -> "⚡ Mercato Teso (Seller's Market Attivo)"
            sdrIndex >= 42.0 -> "⚖️ Mercato Bilanciato (Equilibrio Fisiologico)"
            sdrIndex >= 28.0 -> "⚠️ Rallentamento / Eccesso di Offerta (Buyer's Market)"
            else -> "🧊 Stallo / Saturazione Severa"
        }
    }

    suspend fun performProactiveMonitoringScan(context: Context): List<SupplyDemandAlertRecord> = withContext(Dispatchers.IO) {
        mutex.withLock {
            _isScanningFlow.value = true
            val triggeredAlerts = mutableListOf<SupplyDemandAlertRecord>()

            try {
                val settings = loadSettings(context)
                if (!settings.isEnabled) {
                    Log.d(TAG, "Monitoring is disabled in settings. Skipping scan.")
                    _isScanningFlow.value = false
                    return@withContext emptyList()
                }

                val db = AppDatabase.getDatabase(context)
                val portfolioProperties: List<Property> = if (settings.monitorSavedPortfolio) {
                    db.propertyDao().getAllPropertiesList()
                } else emptyList()

                val savedDeals: List<PropertyDeal> = if (settings.monitorSavedDeals) {
                    db.propertyDealDao().getAllDealsList().filter { it.isBookmarked || it.dealStage != "PROSPECTING" }
                } else emptyList()

                // Extract all unique monitored locations
                val rawLocations = mutableSetOf<String>()
                portfolioProperties.forEach { rawLocations.add(it.address) }
                savedDeals.forEach { rawLocations.add(it.location) }

                // Always include core investment hubs if list is small
                if (rawLocations.size < 3) {
                    rawLocations.add("Paderno Dugnano")
                    rawLocations.add("Milano")
                    rawLocations.add("Monza")
                    rawLocations.add("Bologna")
                    rawLocations.add("Roma")
                }

                val normalizedLocations = rawLocations.map { normalizeLocation(it) }.distinct().filter { it.isNotBlank() }
                Log.d(TAG, "Scanning ${normalizedLocations.size} target locations for supply-demand shifts: $normalizedLocations")

                val previousSnapshots = loadSnapshots(context).toMutableMap()
                val updatedSnapshots = mutableMapOf<String, SupplyDemandSnapshot>()

                for (location in normalizedLocations) {
                    val kpiResult = MarketEstimateService.scrapeMarketKpis(location)
                    val kpi = kpiResult.getOrNull() ?: continue

                    val currentSdr = computeSupplyDemandRatio(
                        saturationScore = kpi.marketSaturationScore,
                        daysOnMarket = kpi.avgDaysOnMarket,
                        absorptionRate = kpi.absorptionRatePercent,
                        saleTrendYoY = kpi.saleTrendYoY,
                        rentTrendYoY = kpi.rentTrendYoY
                    )

                    val currentSnapshot = SupplyDemandSnapshot(
                        location = kpi.locationName.ifBlank { location },
                        timestamp = System.currentTimeMillis(),
                        marketSaturation = kpi.marketSaturationScore ?: 50,
                        daysOnMarket = kpi.avgDaysOnMarket ?: 90,
                        absorptionRatePercent = kpi.absorptionRatePercent ?: 60.0,
                        avgSalePriceSqM = kpi.avgSalePriceSqM ?: 2000.0,
                        avgRentPriceSqM = kpi.avgRentPriceSqM ?: 10.0,
                        saleTrendYoY = kpi.saleTrendYoY ?: 2.0,
                        rentTrendYoY = kpi.rentTrendYoY ?: 3.0,
                        supplyDemandRatioIndex = currentSdr,
                        tensionLabel = getTensionLabel(currentSdr)
                    )
                    updatedSnapshots[location.lowercase()] = currentSnapshot

                    val prevSnapshot = previousSnapshots[location.lowercase()]
                    if (prevSnapshot != null) {
                        // Compare snapshots to detect drastic shift
                        val alert = evaluateShift(
                            location = currentSnapshot.location,
                            prev = prevSnapshot,
                            curr = currentSnapshot,
                            thresholdPercent = settings.sensitivityThresholdPercent,
                            portfolio = portfolioProperties,
                            deals = savedDeals,
                            sourceUrl = kpi.sourceUrl
                        )
                        if (alert != null) {
                            triggeredAlerts.add(alert)
                        }
                    } else {
                        // Initial baseline registration
                        Log.d(TAG, "Baseline established for $location: SDR=$currentSdr, DOM=${kpi.avgDaysOnMarket}gg")
                    }
                }

                // Persist updated snapshots
                saveSnapshots(context, updatedSnapshots)
                _monitoredZonesFlow.value = updatedSnapshots.values.toList()

                // Save and dispatch alerts
                if (triggeredAlerts.isNotEmpty()) {
                    val currentHistory = loadAlertHistory(context).toMutableList()
                    triggeredAlerts.forEach { alert ->
                        currentHistory.add(0, alert)
                        if (settings.pushNotificationsEnabled) {
                            SupplyDemandNotificationManager.sendShiftAlertNotification(context, alert)
                        }
                    }
                    val trimmedHistory = currentHistory.take(50)
                    saveAlertHistory(context, trimmedHistory)
                    _alertsFlow.value = trimmedHistory
                }

                // Update settings metadata
                val updatedSettings = settings.copy(
                    lastScanTimestamp = System.currentTimeMillis(),
                    lastScanResultsCount = triggeredAlerts.size
                )
                saveSettings(context, updatedSettings)

            } catch (e: Exception) {
                Log.e(TAG, "Error during proactive monitoring scan", e)
            } finally {
                _isScanningFlow.value = false
            }

            return@withContext triggeredAlerts
        }
    }

    private fun evaluateShift(
        location: String,
        prev: SupplyDemandSnapshot,
        curr: SupplyDemandSnapshot,
        thresholdPercent: Double,
        portfolio: List<Property>,
        deals: List<PropertyDeal>,
        sourceUrl: String
    ): SupplyDemandAlertRecord? {
        val sdrDelta = curr.supplyDemandRatioIndex - prev.supplyDemandRatioIndex
        val sdrDeltaPercent = if (prev.supplyDemandRatioIndex > 0) {
            (sdrDelta / prev.supplyDemandRatioIndex) * 100.0
        } else 0.0

        val domDelta = curr.daysOnMarket - prev.daysOnMarket
        val satDelta = curr.marketSaturation - prev.marketSaturation
        val rentGrowthGap = curr.rentTrendYoY - curr.saleTrendYoY

        // Find affected properties in the local area
        val affectedPortfolio = portfolio.filter {
            it.address.contains(location, ignoreCase = true) || location.contains(it.address, ignoreCase = true)
        }
        val affectedDeals = deals.filter {
            it.location.contains(location, ignoreCase = true) || location.contains(it.location, ignoreCase = true)
        }

        val affectedIds = (affectedPortfolio.map { it.id } + affectedDeals.map { it.id }).distinct()
        val affectedTitles = (affectedPortfolio.map { it.title.ifBlank { it.address } } + affectedDeals.map { it.title }).distinct()

        // 1. Supply Squeeze Shift (Bullish Demand Surge)
        if (sdrDeltaPercent >= thresholdPercent || (domDelta <= -15 && satDelta <= -8)) {
            val isCritical = sdrDeltaPercent >= 20.0 || domDelta <= -25
            return SupplyDemandAlertRecord(
                id = UUID.randomUUID().toString(),
                location = location,
                affectedPropertyIds = affectedIds,
                affectedPropertyTitles = affectedTitles,
                shiftType = SupplyDemandShiftType.SUPPLY_SQUEEZE,
                severity = if (isCritical) ShiftSeverity.CRITICAL else ShiftSeverity.MODERATE,
                headline = "Forte contrazione dell'offerta e crollo tempi di vendita in $location",
                description = "I giorni sul mercato sono scesi da ${prev.daysOnMarket}gg a ${curr.daysOnMarket}gg, mentre l'indice di saturazione è sceso a ${curr.marketSaturation}/100.",
                strategicRecommendation = "Finestra ideale per liquidare asset a premio (+5% / +10% sul target resale) oppure velocizzare la fase di cantiere Fix & Flip per sfruttare la scarsità di immobili sul mercato.",
                previousRatio = prev.supplyDemandRatioIndex,
                currentRatio = curr.supplyDemandRatioIndex,
                ratioDeltaPercent = sdrDeltaPercent,
                previousDom = prev.daysOnMarket,
                currentDom = curr.daysOnMarket,
                previousSaturation = prev.marketSaturation,
                currentSaturation = curr.marketSaturation,
                sourceUrl = sourceUrl
            )
        }

        // 2. Supply Glut / Demand Slowdown Shift (Bearish Risk)
        if (sdrDeltaPercent <= -thresholdPercent || (domDelta >= 20 && satDelta >= 12)) {
            val isCritical = sdrDeltaPercent <= -20.0 || domDelta >= 35
            return SupplyDemandAlertRecord(
                id = UUID.randomUUID().toString(),
                location = location,
                affectedPropertyIds = affectedIds,
                affectedPropertyTitles = affectedTitles,
                shiftType = SupplyDemandShiftType.SUPPLY_GLUT,
                severity = if (isCritical) ShiftSeverity.CRITICAL else ShiftSeverity.MODERATE,
                headline = "Accumulo di stock invenduto e allungamento DOM in $location",
                description = "La saturazione degli annunci è salita a ${curr.marketSaturation}/100 (+${satDelta}) e i giorni medi di vendita sono aumentati da ${prev.daysOnMarket}gg a ${curr.daysOnMarket}gg.",
                strategicRecommendation = "Rischio stallo delle vendite: ricalibrare i prezzi di richiesta, implementare home staging aggressivo, o considerare la messa a reddito transitoria/studenti.",
                previousRatio = prev.supplyDemandRatioIndex,
                currentRatio = curr.supplyDemandRatioIndex,
                ratioDeltaPercent = sdrDeltaPercent,
                previousDom = prev.daysOnMarket,
                currentDom = curr.daysOnMarket,
                previousSaturation = prev.marketSaturation,
                currentSaturation = curr.marketSaturation,
                sourceUrl = sourceUrl
            )
        }

        // 3. Rental Yield Divergence Shift
        if (rentGrowthGap >= 4.0 && curr.rentTrendYoY >= 5.0) {
            return SupplyDemandAlertRecord(
                id = UUID.randomUUID().toString(),
                location = location,
                affectedPropertyIds = affectedIds,
                affectedPropertyTitles = affectedTitles,
                shiftType = SupplyDemandShiftType.RENTAL_YIELD_DIVERGENCE,
                severity = ShiftSeverity.MODERATE,
                headline = "Divergenza canoni di locazione in $location (+${String.format(java.util.Locale.US, "%.1f", curr.rentTrendYoY)}% YoY)",
                description = "La crescita dei canoni di locazione sta surclassando i prezzi di acquisto. I rendimenti lordi sono saliti a ${String.format(java.util.Locale.US, "%.1f", (curr.avgRentPriceSqM * 12 / curr.avgSalePriceSqM) * 100)}%.",
                strategicRecommendation = "Opportunità di conversione strategica: anziché rivendere subito l'immobile, valuta la messa a reddito (Buy & Hold) con flusso di cassa mensile maggiorato.",
                previousRatio = prev.supplyDemandRatioIndex,
                currentRatio = curr.supplyDemandRatioIndex,
                ratioDeltaPercent = sdrDeltaPercent,
                previousDom = prev.daysOnMarket,
                currentDom = curr.daysOnMarket,
                previousSaturation = prev.marketSaturation,
                currentSaturation = curr.marketSaturation,
                sourceUrl = sourceUrl
            )
        }

        return null
    }

    /**
     * Simulates an immediate drastic area shift for testing and live demonstrations.
     */
    suspend fun simulateImmediateAreaShift(
        context: Context,
        location: String,
        shiftType: SupplyDemandShiftType
    ): SupplyDemandAlertRecord = withContext(Dispatchers.IO) {
        val cleanLoc = normalizeLocation(location).ifBlank { "Paderno Dugnano" }
        val db = AppDatabase.getDatabase(context)
        val portfolio: List<Property> = db.propertyDao().getAllPropertiesList()
        val deals: List<PropertyDeal> = db.propertyDealDao().getAllDealsList()

        val affectedPortfolio = portfolio.filter {
            it.address.contains(cleanLoc, ignoreCase = true) || cleanLoc.contains(it.address, ignoreCase = true)
        }
        val affectedDeals = deals.filter {
            it.location.contains(cleanLoc, ignoreCase = true) || cleanLoc.contains(it.location, ignoreCase = true)
        }

        val affectedIds = (affectedPortfolio.map { it.id } + affectedDeals.map { it.id }).distinct()
        val affectedTitles = (affectedPortfolio.map { it.title.ifBlank { it.address } } + affectedDeals.map { it.title }).distinct()

        val alert = when (shiftType) {
            SupplyDemandShiftType.SUPPLY_SQUEEZE -> {
                SupplyDemandAlertRecord(
                    id = UUID.randomUUID().toString(),
                    location = cleanLoc,
                    affectedPropertyIds = affectedIds,
                    affectedPropertyTitles = affectedTitles,
                    shiftType = SupplyDemandShiftType.SUPPLY_SQUEEZE,
                    severity = ShiftSeverity.CRITICAL,
                    headline = "🚨 SQUEEZE RECORD: Scarsità improvvisa di annunci in $cleanLoc",
                    description = "I giorni medi sul mercato sono crollati da 105gg a 62gg (-41%) con assorbimento record delle offerte.",
                    strategicRecommendation = "Aumenta il prezzo di uscita del tuo immobile (+8%) e contatta subito gli acquirenti in lista d'attesa.",
                    previousRatio = 54.0,
                    currentRatio = 82.5,
                    ratioDeltaPercent = 52.8,
                    previousDom = 105,
                    currentDom = 62,
                    previousSaturation = 58,
                    currentSaturation = 24
                )
            }
            SupplyDemandShiftType.SUPPLY_GLUT -> {
                SupplyDemandAlertRecord(
                    id = UUID.randomUUID().toString(),
                    location = cleanLoc,
                    affectedPropertyIds = affectedIds,
                    affectedPropertyTitles = affectedTitles,
                    shiftType = SupplyDemandShiftType.SUPPLY_GLUT,
                    severity = ShiftSeverity.CRITICAL,
                    headline = "⚠️ ALLARME GLUT: Picco di nuove offerte invendute in $cleanLoc",
                    description = "La saturazione è balzata a 76/100 con aumento dei tempi di permanenza annunci (+35gg).",
                    strategicRecommendation = "Valuta una rinegoziazione del prezzo o converti l'operazione in locazione transitoria/studenti.",
                    previousRatio = 65.0,
                    currentRatio = 38.0,
                    ratioDeltaPercent = -41.5,
                    previousDom = 75,
                    currentDom = 110,
                    previousSaturation = 32,
                    currentSaturation = 76
                )
            }
            SupplyDemandShiftType.RENTAL_YIELD_DIVERGENCE -> {
                SupplyDemandAlertRecord(
                    id = UUID.randomUUID().toString(),
                    location = cleanLoc,
                    affectedPropertyIds = affectedIds,
                    affectedPropertyTitles = affectedTitles,
                    shiftType = SupplyDemandShiftType.RENTAL_YIELD_DIVERGENCE,
                    severity = ShiftSeverity.MODERATE,
                    headline = "📈 BOOM CANONI: Richieste di locazione in forte ascesa in $cleanLoc (+8.4% YoY)",
                    description = "La domanda di affitto batte nettamente le vendite. Il Cap Rate stimato per la zona sale al 7.8% lordo.",
                    strategicRecommendation = "Ottimizza il rendimento impostando una locazione a canone concordato o affitto a stanze.",
                    previousRatio = 58.0,
                    currentRatio = 74.0,
                    ratioDeltaPercent = 27.6,
                    previousDom = 90,
                    currentDom = 75,
                    previousSaturation = 45,
                    currentSaturation = 38
                )
            }
            SupplyDemandShiftType.MICRO_ZONE_HEATWAVE -> {
                SupplyDemandAlertRecord(
                    id = UUID.randomUUID().toString(),
                    location = cleanLoc,
                    affectedPropertyIds = affectedIds,
                    affectedPropertyTitles = affectedTitles,
                    shiftType = SupplyDemandShiftType.MICRO_ZONE_HEATWAVE,
                    severity = ShiftSeverity.MODERATE,
                    headline = "⚡ HOT ZONE: Pressione acquirenti concentrata nel quadrante $cleanLoc",
                    description = "La micro-zona specifica dell'immobile registra il 30% in più di contatti per annuncio rispetto alla media provinciale.",
                    strategicRecommendation = "Puntare su finiture di pregio per massimizzare il target resale nella micro-zona.",
                    previousRatio = 60.0,
                    currentRatio = 78.0,
                    ratioDeltaPercent = 30.0,
                    previousDom = 88,
                    currentDom = 68,
                    previousSaturation = 40,
                    currentSaturation = 30
                )
            }
        }

        // Add to history and send push notification
        val currentHistory = loadAlertHistory(context).toMutableList()
        currentHistory.add(0, alert)
        saveAlertHistory(context, currentHistory)
        _alertsFlow.value = currentHistory

        SupplyDemandNotificationManager.sendShiftAlertNotification(context, alert)
        return@withContext alert
    }

    fun markAlertAsRead(context: Context, alertId: String) {
        val history = loadAlertHistory(context).map {
            if (it.id == alertId) it.copy(isRead = true) else it
        }
        saveAlertHistory(context, history)
        _alertsFlow.value = history
    }

    fun clearAlertHistory(context: Context) {
        saveAlertHistory(context, emptyList())
        _alertsFlow.value = emptyList()
    }

    private fun normalizeLocation(addressOrLocation: String): String {
        return addressOrLocation
            .replace("Via ", "", ignoreCase = true)
            .replace("Viale ", "", ignoreCase = true)
            .replace("Corso ", "", ignoreCase = true)
            .replace("Piazza ", "", ignoreCase = true)
            .split(",")
            .firstOrNull()
            ?.replace("\\(.*?\\)".toRegex(), "")
            ?.trim()
            ?: addressOrLocation.trim()
    }

    private fun loadSnapshots(context: Context): Map<String, SupplyDemandSnapshot> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_SNAPSHOTS_JSON, null) ?: return emptyMap()
        val result = mutableMapOf<String, SupplyDemandSnapshot>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val loc = obj.getString("location")
                result[loc.lowercase()] = SupplyDemandSnapshot(
                    location = loc,
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    marketSaturation = obj.optInt("marketSaturation", 50),
                    daysOnMarket = obj.optInt("daysOnMarket", 90),
                    absorptionRatePercent = obj.optDouble("absorptionRatePercent", 60.0),
                    avgSalePriceSqM = obj.optDouble("avgSalePriceSqM", 2000.0),
                    avgRentPriceSqM = obj.optDouble("avgRentPriceSqM", 10.0),
                    saleTrendYoY = obj.optDouble("saleTrendYoY", 2.0),
                    rentTrendYoY = obj.optDouble("rentTrendYoY", 3.0),
                    supplyDemandRatioIndex = obj.optDouble("supplyDemandRatioIndex", 50.0),
                    tensionLabel = obj.optString("tensionLabel", "Normale")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading snapshots", e)
        }
        return result
    }

    private fun saveSnapshots(context: Context, snapshots: Map<String, SupplyDemandSnapshot>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val jsonArray = JSONArray()
            snapshots.values.forEach { s ->
                val obj = JSONObject().apply {
                    put("location", s.location)
                    put("timestamp", s.timestamp)
                    put("marketSaturation", s.marketSaturation)
                    put("daysOnMarket", s.daysOnMarket)
                    put("absorptionRatePercent", s.absorptionRatePercent)
                    put("avgSalePriceSqM", s.avgSalePriceSqM)
                    put("avgRentPriceSqM", s.avgRentPriceSqM)
                    put("saleTrendYoY", s.saleTrendYoY)
                    put("rentTrendYoY", s.rentTrendYoY)
                    put("supplyDemandRatioIndex", s.supplyDemandRatioIndex)
                    put("tensionLabel", s.tensionLabel)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_SNAPSHOTS_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving snapshots", e)
        }
    }

    private fun loadAlertHistory(context: Context): List<SupplyDemandAlertRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_ALERT_HISTORY_JSON, null) ?: return emptyList()
        val list = mutableListOf<SupplyDemandAlertRecord>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val propIds = mutableListOf<Long>()
                val propTitles = mutableListOf<String>()
                obj.optJSONArray("affectedPropertyIds")?.let { arr ->
                    for (j in 0 until arr.length()) propIds.add(arr.getLong(j))
                }
                obj.optJSONArray("affectedPropertyTitles")?.let { arr ->
                    for (j in 0 until arr.length()) propTitles.add(arr.getString(j))
                }

                list.add(
                    SupplyDemandAlertRecord(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        location = obj.optString("location", ""),
                        affectedPropertyIds = propIds,
                        affectedPropertyTitles = propTitles,
                        shiftType = SupplyDemandShiftType.fromKey(obj.optString("shiftType", "SUPPLY_SQUEEZE")),
                        severity = ShiftSeverity.fromLevel(obj.optInt("severityLevel", 2)),
                        headline = obj.optString("headline", ""),
                        description = obj.optString("description", ""),
                        strategicRecommendation = obj.optString("strategicRecommendation", ""),
                        previousRatio = obj.optDouble("previousRatio", 50.0),
                        currentRatio = obj.optDouble("currentRatio", 50.0),
                        ratioDeltaPercent = obj.optDouble("ratioDeltaPercent", 0.0),
                        previousDom = obj.optInt("previousDom", 90),
                        currentDom = obj.optInt("currentDom", 90),
                        previousSaturation = obj.optInt("previousSaturation", 50),
                        currentSaturation = obj.optInt("currentSaturation", 50),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false),
                        sourceUrl = obj.optString("sourceUrl", "")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading alert history", e)
        }
        return list
    }

    private fun saveAlertHistory(context: Context, alerts: List<SupplyDemandAlertRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val jsonArray = JSONArray()
            alerts.forEach { alert ->
                val obj = JSONObject().apply {
                    put("id", alert.id)
                    put("location", alert.location)
                    put("affectedPropertyIds", JSONArray(alert.affectedPropertyIds))
                    put("affectedPropertyTitles", JSONArray(alert.affectedPropertyTitles))
                    put("shiftType", alert.shiftType.name)
                    put("severityLevel", alert.severity.level)
                    put("headline", alert.headline)
                    put("description", alert.description)
                    put("strategicRecommendation", alert.strategicRecommendation)
                    put("previousRatio", alert.previousRatio)
                    put("currentRatio", alert.currentRatio)
                    put("ratioDeltaPercent", alert.ratioDeltaPercent)
                    put("previousDom", alert.previousDom)
                    put("currentDom", alert.currentDom)
                    put("previousSaturation", alert.previousSaturation)
                    put("currentSaturation", alert.currentSaturation)
                    put("timestamp", alert.timestamp)
                    put("isRead", alert.isRead)
                    put("sourceUrl", alert.sourceUrl)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_ALERT_HISTORY_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving alert history", e)
        }
    }
}
