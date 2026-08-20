package com.example.data

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DealRadarRepository(
    private val dealDao: PropertyDealDao,
    private val sourceDao: ScraperSourceDao,
    private val historyDao: PriceHistoryDao,
    private val investorDao: InvestorProfileDao,
    private val propertyDao: PropertyDao? = null,
    private val distressedPropertyDao: DistressedPropertyDao? = null,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    // Centralized StateFlows providing a single source of truth across the app
    val allDeals: StateFlow<List<PropertyDeal>> = dealDao.getAllDeals()
        .distinctUntilChanged()
        .flowOn(ioDispatcher)
        .onEach { list ->
            Log.d("DealRadarRepository", "Centralized allDeals emitted ${list.size} property deals from Room DB")
        }
        .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedDeals: StateFlow<List<PropertyDeal>> = dealDao.getBookmarkedDeals()
        .distinctUntilChanged()
        .flowOn(ioDispatcher)
        .onEach { list ->
            Log.d("DealRadarRepository", "Centralized bookmarkedDeals emitted ${list.size} items")
        }
        .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyViewedDeals: StateFlow<List<PropertyDeal>> = dealDao.getRecentlyViewedDeals()
        .distinctUntilChanged()
        .flowOn(ioDispatcher)
        .onEach { list ->
            Log.d("DealRadarRepository", "Centralized recentlyViewedDeals emitted ${list.size} items")
        }
        .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSources: StateFlow<List<ScraperSource>> = sourceDao.getAllSources()
        .distinctUntilChanged()
        .flowOn(ioDispatcher)
        .onEach { list ->
            Log.d("DealRadarRepository", "Centralized allSources emitted ${list.size} sources")
        }
        .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val investorProfile: StateFlow<InvestorProfile?> = investorDao.getProfileFlow()
        .distinctUntilChanged()
        .flowOn(ioDispatcher)
        .onEach { profile ->
            Log.d("DealRadarRepository", "Centralized investorProfile updated: ${profile?.companyName ?: "Personal"}")
        }
        .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), null)

    val allSavedProperties: StateFlow<List<Property>> = (propertyDao?.getAllProperties() ?: flowOf(emptyList()))
        .distinctUntilChanged()
        .flowOn(ioDispatcher)
        .onEach { list ->
            Log.d("DealRadarRepository", "Centralized allSavedProperties emitted ${list.size} items")
        }
        .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDistressedProperties: StateFlow<List<DistressedProperty>> = (distressedPropertyDao?.getAllDistressedProperties() ?: flowOf(emptyList()))
        .distinctUntilChanged()
        .flowOn(ioDispatcher)
        .onEach { list ->
            Log.d("DealRadarRepository", "Centralized allDistressedProperties emitted ${list.size} items")
        }
        .stateIn(externalScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getDealsPaged(limit: Int, offset: Int): Flow<List<PropertyDeal>> {
        Log.d("DealRadarRepository", "getDealsPaged(limit=$limit, offset=$offset) called")
        return dealDao.getDealsPaged(limit, offset)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
            .onEach { list ->
                Log.d("DealRadarRepository", "getDealsPaged(limit=$limit, offset=$offset) emitted ${list.size} items")
            }
    }

    suspend fun getDealsPagedList(limit: Int, offset: Int): List<PropertyDeal> = withContext(ioDispatcher) {
        dealDao.getDealsPagedList(limit, offset)
    }

    fun getDealsCount(): Flow<Int> {
        return dealDao.getDealsCount()
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    fun getBookmarkedDealsPaged(limit: Int, offset: Int): Flow<List<PropertyDeal>> {
        return dealDao.getBookmarkedDealsPaged(limit, offset)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    fun getDealsBySourcePaged(sourceKey: String, limit: Int, offset: Int): Flow<List<PropertyDeal>> {
        return dealDao.getDealsBySourcePaged(sourceKey, limit, offset)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    fun getPriceHistoryForDealLimited(dealId: Long, limit: Int = 10): Flow<List<PriceHistory>> {
        return historyDao.getHistoryForDealLimited(dealId, limit)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    suspend fun insertSavedProperty(property: Property): Long = withContext(ioDispatcher) {
        propertyDao?.insertProperty(property) ?: 0L
    }

    suspend fun updateSavedProperty(property: Property) = withContext(ioDispatcher) {
        propertyDao?.updateProperty(property)
    }

    suspend fun updateSavedPropertyPipelineStatus(propertyId: Long, newStatus: String) = withContext(ioDispatcher) {
        propertyDao?.updatePipelineStatus(propertyId, newStatus)
    }

    suspend fun deleteSavedProperty(property: Property) = withContext(ioDispatcher) {
        propertyDao?.deleteProperty(property)
    }

    suspend fun checkAndSeedDatabase() = withContext(ioDispatcher) {
        Log.d("DealRadarRepository", "checkAndSeedDatabase() initialization started")
        
        val currentSources = sourceDao.getAllSources().first()
        if (currentSources.isEmpty() || currentSources.size < InitialSeedData.initialSources.size) {
            Log.d("DealRadarRepository", "Seeding initial sources (${InitialSeedData.initialSources.size})...")
            sourceDao.insertSources(InitialSeedData.initialSources)
        } else {
            Log.d("DealRadarRepository", "Sources database initialized with ${currentSources.size} sources.")
        }

        val currentDeals = dealDao.getAllDeals().first()
        if (currentDeals.isEmpty()) {
            Log.d("DealRadarRepository", "Seeding initial property deals (${InitialSeedData.initialDeals.size})...")
            dealDao.insertDeals(InitialSeedData.initialDeals.map { it.copy(provenance = DataProvenance.SYNTHETIC_DEMO.name) })
            historyDao.insertHistories(InitialSeedData.initialHistories)
        } else {
            Log.d("DealRadarRepository", "Property deals database initialized with ${currentDeals.size} deals.")
        }

        if (propertyDao != null) {
            val currentProps = propertyDao.getAllProperties().first()
            if (currentProps.isEmpty()) {
                Log.d("DealRadarRepository", "Seeding initial properties (${InitialSeedData.initialProperties.size})...")
                propertyDao.insertProperties(InitialSeedData.initialProperties.map { it.copy(provenance = DataProvenance.SYNTHETIC_DEMO.name) })
            } else {
                Log.d("DealRadarRepository", "Property table contains ${currentProps.size} items.")
            }
        }

        if (distressedPropertyDao != null) {
            val currentDistressed = distressedPropertyDao.getAllDistressedProperties().first()
            if (currentDistressed.isEmpty()) {
                Log.d("DealRadarRepository", "Seeding initial distressed properties (${InitialSeedData.initialDistressedProperties.size})...")
                distressedPropertyDao.insertDistressedProperties(InitialSeedData.initialDistressedProperties.map { it.copy(provenance = DataProvenance.SYNTHETIC_DEMO.name) })
            } else {
                Log.d("DealRadarRepository", "Distressed property table contains ${currentDistressed.size} items.")
            }
        }

        val currentProfile = investorDao.getProfile()
        if (currentProfile == null) {
            Log.d("DealRadarRepository", "Seeding default investor profile...")
            investorDao.insertOrUpdateProfile(InvestorProfile())
        }
        Log.d("DealRadarRepository", "checkAndSeedDatabase() completed successfully.")
    }

    suspend fun getDealById(dealId: Long): PropertyDeal? = withContext(ioDispatcher) {
        dealDao.getDealById(dealId)
    }

    suspend fun saveInvestorProfile(profile: InvestorProfile) = withContext(ioDispatcher) {
        investorDao.insertOrUpdateProfile(profile)
    }

    suspend fun toggleBookmark(dealId: Long, currentStatus: Boolean) = withContext(ioDispatcher) {
        dealDao.setBookmarked(dealId, !currentStatus)
        dealDao.updateLastViewedAt(dealId, System.currentTimeMillis())
    }

    suspend fun markDealAsViewed(dealId: Long) = withContext(ioDispatcher) {
        dealDao.updateLastViewedAt(dealId, System.currentTimeMillis())
    }

    suspend fun clearRecentlyViewedHistory() = withContext(ioDispatcher) {
        dealDao.clearRecentlyViewedHistory()
    }

    suspend fun updateDealNotes(dealId: Long, notes: String) = withContext(ioDispatcher) {
        dealDao.updateNotes(dealId, notes)
    }

    suspend fun updatePriceAlertThreshold(dealId: Long, threshold: Double?) = withContext(ioDispatcher) {
        dealDao.updatePriceAlertThreshold(dealId, threshold)
    }

    suspend fun updateDealStage(dealId: Long, stage: String) = withContext(ioDispatcher) {
        dealDao.updateDealStage(dealId, stage)
    }

    suspend fun recordPriceDrop(
        dealId: Long,
        newPrice: Double,
        eventLabel: String,
        dateRecorded: String,
        onPriceDropBelowThreshold: ((PropertyDeal, Double, Double, Double) -> Unit)? = null
    ) = withContext(ioDispatcher) {
        val deal = dealDao.getDealById(dealId) ?: return@withContext
        val oldPrice = deal.askingPrice
        val newDiscount = if (deal.estimatedMarketValue > 0) {
            (((deal.estimatedMarketValue - newPrice) / deal.estimatedMarketValue) * 100).toInt()
        } else deal.discountPercent

        val updatedDeal = deal.copy(
            askingPrice = newPrice,
            discountPercent = newDiscount,
            status = "PRICE_CUT"
        )
        dealDao.updateDeal(updatedDeal)

        historyDao.insertHistory(
            PriceHistory(
                dealId = dealId,
                price = newPrice,
                dateRecorded = dateRecorded,
                eventLabel = eventLabel
            )
        )

        val threshold = deal.priceAlertThreshold
        if (threshold != null && newPrice <= threshold) {
            onPriceDropBelowThreshold?.invoke(updatedDeal, oldPrice, newPrice, threshold)
        }
    }

    suspend fun addDeal(deal: PropertyDeal) = withContext(ioDispatcher) {
        val newId = dealDao.insertDeal(deal)
        historyDao.insertHistory(
            PriceHistory(
                dealId = newId,
                price = deal.askingPrice,
                dateRecorded = "Oggi",
                eventLabel = "Inserimento Manuale/Parsed"
            )
        )
    }

    suspend fun deleteDeal(deal: PropertyDeal) = withContext(ioDispatcher) {
        dealDao.deleteDeal(deal)
    }

    suspend fun updateSourceConfigStatus(sourceId: String, newStatus: String) = withContext(ioDispatcher) {
        sourceDao.updateConfigStatus(sourceId, newStatus)
    }

    suspend fun updateSourceParserRules(sourceId: String, rulesJson: String) = withContext(ioDispatcher) {
        sourceDao.updateParserRules(sourceId, rulesJson)
    }

    /**
     * Validates all scraper source configurations: iterates through configured sources,
     * verifies activeParserRulesJson syntax and container selector presence, and updates DB config status.
     */
    suspend fun validateSourceConfigurations(): CalibrationSummary = withContext(ioDispatcher) {
        val sources = sourceDao.getAllSources().first()
        val logs = mutableListOf<String>()
        var successCount = 0
        var warningCount = 0

        logs.add("=== AVVIO VALIDAZIONE CONFIGURAZIONI FONTI ===")
        logs.add("Totale Fonti Configurate: ${sources.size}")

        sources.forEachIndexed { index, source ->
            logs.add("\n[${index + 1}/${sources.size}] Validazione configurazione: ${source.name} (${source.id})")
            logs.add("  • Target URL configurato: ${source.url}")

            val isRobotsOk = source.robotsStatus == "CONSENTITO" || source.robotsStatus == "NESSUN_ROBOTS"
            logs.add("  • Stato robots.txt registrato in configurazione: ${source.robotsStatus} (non ri-verificato in questa sessione)")

            var isJsonValid = false
            try {
                val json = JSONObject(source.activeParserRulesJson)
                val listSel = json.optString("listSelector")
                if (listSel.isNotEmpty()) {
                    isJsonValid = true
                    logs.add("  • Selettori JSON in configurazione: VALIDI (Container '$listSel')")
                } else {
                    logs.add("  • Selettori JSON in configurazione: MANCANTE listSelector")
                }
            } catch (e: Exception) {
                logs.add("  • Selettori JSON in configurazione: ERRORE SINTASSI (${e.message})")
            }

            val newStatus = when {
                source.configStatus == "SOLO_CONTATTO" -> "SOLO_CONTATTO"
                isRobotsOk && isJsonValid -> "CONSENTITO"
                else -> "DA_VERIFICARE"
            }

            sourceDao.updateConfigStatus(source.id, newStatus)
            logs.add("  • Stato Configurazione Aggiornato -> $newStatus")

            if (newStatus == "CONSENTITO" || newStatus == "SOLO_CONTATTO") {
                successCount++
            } else {
                warningCount++
            }
        }

        logs.add("\n=== VALIDAZIONE CONFIGURAZIONI COMPLETATA ===")
        logs.add("Esito: $successCount Fonti Valide, $warningCount da Verificare.")

        CalibrationSummary(
            totalSources = sources.size,
            activeCount = successCount,
            warningCount = warningCount,
            logs = logs
        )
    }

    fun getPriceHistoryForDeal(dealId: Long): Flow<List<PriceHistory>> {
        return historyDao.getHistoryForDeal(dealId)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    /**
     * Import deals extracted from a scraper test run into the database
     */
    suspend fun importScrapedDeals(deals: List<PropertyDeal>) = withContext(ioDispatcher) {
        dealDao.insertDeals(deals)
        deals.forEach { deal ->
            historyDao.insertHistory(
                PriceHistory(
                    dealId = deal.id,
                    price = deal.askingPrice,
                    dateRecorded = "Oggi",
                    eventLabel = "Importato da Scraper ${deal.sourceName}"
                )
            )
        }
    }

    /**
     * Tests JSON parser rules against a provided sample HTML or JSON payload in sandbox.
     * Does not execute any network calls. If regex extraction fails, fields remain null.
     */
    fun simulateParserTest(
        source: ScraperSource,
        sampleHtmlOrJson: String,
        rulesJsonStr: String
    ): ScraperTestResult {
        return try {
            val jsonObject = JSONObject(if (rulesJsonStr.isNotBlank()) rulesJsonStr else source.activeParserRulesJson)
            val listSelector = jsonObject.optString("listSelector", ".property-card")
            val titleSelector = jsonObject.optString("titleSelector", ".card-title")
            val priceSelector = jsonObject.optString("priceSelector", ".price-value")
            val marketValueSelector = jsonObject.optString("marketValueSelector", ".valuation-badge")

            val logs = mutableListOf<String>()
            logs.add("Esecuzione test selettori su snippet sandbox fornito...")
            logs.add("Selettore contenitore: '$listSelector'")

            // Dynamic Extraction from Sample HTML/JSON
            var title: String? = null
            var price: Double? = null
            var valPrice: Double? = null
            var location: String? = null
            var sqm: Int? = null

            val titleMatch = Regex("""<[^>]*class=["']?[^"']*${titleSelector.removePrefix(".")}[^"']*["']?[^>]*>(.*?)</""", RegexOption.IGNORE_CASE).find(sampleHtmlOrJson)
            if (titleMatch != null && titleMatch.groupValues[1].isNotBlank()) {
                title = titleMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
            }

            val priceMatch = Regex("""[€\$\s]?(\d{1,3}(?:\.\d{3})+|\d+)[,\.]?(\d{2})?""").find(sampleHtmlOrJson)
            if (priceMatch != null) {
                val cleanPrice = priceMatch.groupValues[1].replace(".", "").toDoubleOrNull()
                if (cleanPrice != null && cleanPrice > 1000) {
                    price = cleanPrice
                }
            }

            val valMatch = Regex("""<[^>]*class=["']?[^"']*${marketValueSelector.removePrefix(".")}[^"']*["']?[^>]*>(.*?)</""", RegexOption.IGNORE_CASE).find(sampleHtmlOrJson)
            if (valMatch != null) {
                val cleanVal = Regex("""(\d{1,3}(?:\.\d{3})+|\d+)""").find(valMatch.groupValues[1])?.groupValues?.get(1)?.replace(".", "")?.toDoubleOrNull()
                if (cleanVal != null) {
                    valPrice = cleanVal
                }
            }

            val sqmMatch = Regex("""(\d{2,4})\s*mq""", RegexOption.IGNORE_CASE).find(sampleHtmlOrJson)
            if (sqmMatch != null) {
                sqm = sqmMatch.groupValues[1].toIntOrNull()
            }

            val cityMatch = Regex("""class=["']?[^"']*city-label[^"']*["']?[^>]*>(.*?)</""", RegexOption.IGNORE_CASE).find(sampleHtmlOrJson)
            if (cityMatch != null && cityMatch.groupValues[1].isNotBlank()) {
                location = cityMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
            }

            if (title == null || price == null) {
                logs.add("Estrazione incompleta dallo snippet:")
                if (title == null) logs.add("  - Titolo ('$titleSelector'): NON TROVATO (null)") else logs.add("  - Titolo: '$title'")
                if (price == null) logs.add("  - Prezzo ('$priceSelector'): NON TROVATO (null)") else logs.add("  - Prezzo: € $price")
                return ScraperTestResult(
                    isSuccess = false,
                    extractedTitle = title,
                    extractedPrice = price,
                    extractedMarketValue = valPrice,
                    discountPercent = 0,
                    logs = logs,
                    extractedDeals = emptyList()
                )
            }

            val finalValPrice = valPrice ?: (price * 1.30)
            val discount = if (finalValPrice > price) {
                (((finalValPrice - price) / finalValPrice) * 100).toInt()
            } else 0

            logs.add("Estrazione completata dallo snippet:")
            logs.add("  - Titolo: '$title'")
            logs.add("  - Prezzo Richiesta: € ${String.format(java.util.Locale.US, "%.2f", price)}")
            if (valPrice != null) {
                logs.add("  - Valore Stimato: € ${String.format(java.util.Locale.US, "%.2f", valPrice)}")
                logs.add("  - Sconto: -$discount%")
            }
            if (sqm != null) logs.add("  - Superficie: $sqm mq")
            if (location != null) logs.add("  - Località: $location")

            val parsedDeal = PropertyDeal(
                id = System.currentTimeMillis() % 100000 + 200,
                title = title,
                sourceKey = source.id,
                sourceName = source.name,
                sourceUrl = source.url,
                location = location ?: "N/D",
                propertyType = "Distressed / Asta",
                askingPrice = price,
                estimatedMarketValue = finalValPrice,
                surfaceSqm = sqm ?: 0,
                discountPercent = discount,
                estimatedCapRate = 0.0,
                latitude = 0.0,
                longitude = 0.0,
                provenance = DataProvenance.SYNTHETIC_DEMO.name,
                notes = "Dato estratto in sandbox da snippet HTML/JSON di test."
            )

            ScraperTestResult(
                isSuccess = true,
                extractedTitle = title,
                extractedPrice = price,
                extractedMarketValue = valPrice,
                discountPercent = discount,
                logs = logs,
                extractedDeals = listOf(parsedDeal)
            )
        } catch (e: Exception) {
            ScraperTestResult(
                isSuccess = false,
                extractedTitle = null,
                extractedPrice = null,
                extractedMarketValue = null,
                discountPercent = 0,
                logs = listOf(
                    "Errore durante l'esecuzione del parser sandbox: ${e.message}",
                    "Verificare la sintassi JSON delle regole selettori o la struttura del payload HTML."
                ),
                extractedDeals = emptyList()
            )
        }
    }

    /**
     * Ingestion is unavailable in this build: no real data source connected.
     */
    suspend fun executeLiveBatchScrapeAllSources(): CalibrationSummary = withContext(ioDispatcher) {
        val sources = sourceDao.getAllSources().first()
        val message = "Ingestione dati non disponibile: questa build non è collegata ad alcuna fonte reale. Nessun immobile è stato importato."
        CalibrationSummary(
            totalSources = sources.size,
            activeCount = 0,
            warningCount = sources.size,
            logs = listOf(message)
        )
    }

    suspend fun unlockDeal(dealId: Long): Boolean = withContext(ioDispatcher) {
        val currentProfile = investorDao.getProfile() ?: InvestorProfile()
        val currentUnlocked = currentProfile.getUnlockedDealIdsList().toMutableSet()
        currentUnlocked.add(dealId)
        val updatedCsv = currentUnlocked.joinToString(",")
        val updated = currentProfile.copy(unlockedDealIdsCsv = updatedCsv)
        investorDao.insertOrUpdateProfile(updated)
        true
    }

    suspend fun useTokenToUnlockDeal(dealId: Long): Boolean = withContext(ioDispatcher) {
        val currentProfile = investorDao.getProfile() ?: InvestorProfile()
        if (currentProfile.availableUnlockTokens > 0) {
            val currentUnlocked = currentProfile.getUnlockedDealIdsList().toMutableSet()
            currentUnlocked.add(dealId)
            val updatedCsv = currentUnlocked.joinToString(",")
            val updated = currentProfile.copy(
                unlockedDealIdsCsv = updatedCsv,
                availableUnlockTokens = (currentProfile.availableUnlockTokens - 1).coerceAtLeast(0)
            )
            investorDao.insertOrUpdateProfile(updated)
            true
        } else {
            unlockDeal(dealId)
        }
    }

    suspend fun setBlindModeActive(active: Boolean): Unit = withContext(ioDispatcher) {
        val currentProfile = investorDao.getProfile() ?: InvestorProfile()
        investorDao.insertOrUpdateProfile(currentProfile.copy(isBlindModeActive = active))
    }

    suspend fun setProSubscriber(isPro: Boolean): Unit = withContext(ioDispatcher) {
        val currentProfile = investorDao.getProfile() ?: InvestorProfile()
        val updated = currentProfile.copy(
            isProSubscriber = isPro,
            subscriptionPlan = if (isPro) "ANNUAL" else "FREE",
            customClaimsRole = if (isPro) "pro_investor" else "investor"
        )
        investorDao.insertOrUpdateProfile(updated)
    }

    suspend fun updateSubscriptionPlan(
        plan: String, // "ANNUAL", "MONTHLY", "FREE"
        isPro: Boolean,
        billingCycle: String = if (plan == "ANNUAL") "ANNUAL" else if (plan == "MONTHLY") "MONTHLY" else "NONE",
        renewalDate: String = "18/09/2026",
        claimsRole: String = if (isPro) "pro_investor" else "investor"
    ): Unit = withContext(ioDispatcher) {
        val currentProfile = investorDao.getProfile() ?: InvestorProfile()
        val updated = currentProfile.copy(
            subscriptionPlan = plan,
            isProSubscriber = isPro,
            subscriptionBillingCycle = billingCycle,
            subscriptionRenewalDate = renewalDate,
            customClaimsRole = claimsRole
        )
        investorDao.insertOrUpdateProfile(updated)
    }

    suspend fun addUnlockTokens(count: Int): Unit = withContext(ioDispatcher) {
        val currentProfile = investorDao.getProfile() ?: InvestorProfile()
        investorDao.insertOrUpdateProfile(currentProfile.copy(availableUnlockTokens = currentProfile.availableUnlockTokens + count))
    }

    suspend fun setHasSeenSearchCoachMark(seen: Boolean): Unit = withContext(ioDispatcher) {
        val currentProfile = investorDao.getProfile() ?: InvestorProfile()
        investorDao.insertOrUpdateProfile(currentProfile.copy(hasSeenSearchCoachMark = seen))
    }
}

data class ScraperTestResult(
    val isSuccess: Boolean,
    val extractedTitle: String?,
    val extractedPrice: Double?,
    val extractedMarketValue: Double?,
    val discountPercent: Int,
    val logs: List<String>,
    val extractedDeals: List<PropertyDeal> = emptyList()
)

data class CalibrationSummary(
    val totalSources: Int,
    val activeCount: Int,
    val warningCount: Int,
    val logs: List<String>
)
