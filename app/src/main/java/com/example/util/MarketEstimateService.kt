package com.example.util

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Modello dati per le stime dei KPI di mercato immobiliare (Province e Comuni Italiani).
 */
data class ProvinceScrapedKpi(
    val locationName: String,
    val province: String,
    val region: String,
    val avgSalePriceSqM: Double? = null,
    val minSalePriceSqM: Double? = null,
    val maxSalePriceSqM: Double? = null,
    val saleTrendYoY: Double? = null,
    val avgRentPriceSqM: Double? = null,
    val rentTrendYoY: Double? = null,
    val grossRentalYield: Double? = null,
    val marketSaturationScore: Int? = null, // 0 (Alta domanda) a 100 (Saturo)
    val avgDaysOnMarket: Int? = null,
    val absorptionRatePercent: Double? = null,
    val hotMicroZones: List<Pair<String, Double>> = emptyList(),
    val sourceUrl: String = "",
    val sourceCitations: List<String> = emptyList(),
    val scrapedAt: Long = System.currentTimeMillis(),
    val isLiveScraped: Boolean = false,
    val marketSummary: String = "",
    val historicalTrends: List<RegionalTrendPoint> = emptyList(),
    val historicalDatasetVerified: Boolean = false,
    val historicalSampleSize: Int = 0,
    val usedFallbackData: Boolean = false,
    val scrapeLatencyMs: Long = 0L,
    val completeness: Double = 0.0,
    val sourceReliability: Double? = null,
    val valuationConfidence: Double? = null,
    val mitigationEngineStatus: String = "Normal"
)

/**
 * Punto serie temporale storica o regionale per grafici di trend.
 */
data class RegionalTrendPoint(
    val yearLabel: String, // es. "2021", "2022", "2023", "2024", "2025", "2026 (Live)"
    val avgSalePriceSqM: Double,
    val avgRentPriceSqM: Double,
    val grossYieldPercent: Double,
    val marketSaturation: Int,
    val daysOnMarket: Int
)

/**
 * KPI aggregati per Macro-Regioni Italiane.
 */
data class RegionalAggregateKpi(
    val regionName: String,
    val avgSalePriceSqM: Double,
    val avgRentPriceSqM: Double,
    val avgGrossYield: Double,
    val avgMarketSaturation: Int,
    val avgDaysOnMarket: Int,
    val saleTrendYoY: Double,
    val rentTrendYoY: Double,
    val provinceCount: Int,
    val topProvinces: List<ProvinceScrapedKpi> = emptyList(),
    val historicalTimeline: List<RegionalTrendPoint> = emptyList()
)

/**
 * Telemetria e metriche di stato per il servizio di stima tramite Gemini API.
 */
data class ScraperDiagnostics(
    val totalRequests: Long,
    val successfulLiveScrapes: Long,
    val cacheHits: Long,
    val quotaExceededCount: Long,
    val rateLimitBackoffsApplied: Long,
    val avgLatencyMs: Long,
    val lastActiveUserAgent: String
)

/**
 * Servizio di stima dei KPI di mercato immobiliare.
 * Interroga l'API di Google Gemini con Google Search grounding per ottenere stime
 * di mercato aggiornate, con fallback su valori di riferimento predefiniti in caso di quota esaurita o errore.
 */
object MarketEstimateService {
    private const val TAG = "MarketEstimateService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    // Cache in memoria con TTL
    private val cache = ConcurrentHashMap<String, ProvinceScrapedKpi>()

    // Concorrenza & Rate Limiting Mutex
    private val rateLimitMutex = Mutex()
    private var lastRequestTime = 0L
    private val requestTimestamps = ArrayDeque<Long>()
    private const val MAX_REQUESTS_PER_WINDOW = 6
    private const val RATE_WINDOW_MS = 10_000L // 10 secondi finestra scorrevole

    // Contatori diagnostici di telemetria
    private val counterTotalRequests = AtomicLong(0)
    private val counterSuccessfulLive = AtomicLong(0)
    private val counterCacheHits = AtomicLong(0)
    private val counterQuotaExceeded = AtomicLong(0)
    private val counterRateLimitBackoffs = AtomicLong(0)
    private val totalLatencyAccumulator = AtomicLong(0)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isBlank() || key == "YOUR_GEMINI_API_KEY") "" else key
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Restituisce la diagnostica e telemetria corrente delle chiamate API.
     */
    fun getDiagnostics(): ScraperDiagnostics {
        val total = counterTotalRequests.get()
        val avgLat = if (total > 0) totalLatencyAccumulator.get() / total else 0L
        return ScraperDiagnostics(
            totalRequests = total,
            successfulLiveScrapes = counterSuccessfulLive.get(),
            cacheHits = counterCacheHits.get(),
            quotaExceededCount = counterQuotaExceeded.get(),
            rateLimitBackoffsApplied = counterRateLimitBackoffs.get(),
            avgLatencyMs = avgLat,
            lastActiveUserAgent = "Gemini-API-Client"
        )
    }

    /**
     * Stima o recupera dalla cache i KPI di mercato per un comune o provincia italiana,
     * effettuando una richiesta a Gemini API con ricerca web o ripiegando sui dati interni.
     */
    suspend fun scrapeMarketKpis(location: String): Result<ProvinceScrapedKpi> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        counterTotalRequests.incrementAndGet()

        val cleanLocation = cleanLocationQuery(location)
        val cacheKey = cleanLocation.lowercase().trim()

        // 1. Controllo cache in memoria (TTL 30 minuti)
        cache[cacheKey]?.let { cached ->
            if (System.currentTimeMillis() - cached.scrapedAt < 30 * 60 * 1000) {
                counterCacheHits.incrementAndGet()
                return@withContext Result.success(cached)
            }
        }

        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            Log.d(TAG, "Nessuna GEMINI_API_KEY trovata: uso valori fittizi interni per $cleanLocation")
            val fallback = getCuratedProvinceKpi(cleanLocation)
            cache[cacheKey] = fallback
            return@withContext Result.success(fallback)
        }

        // 2. Controllo frequenza richieste (rate limiting locale)
        applyAdaptiveRateLimitingAndJitter()

        // 3. Esecuzione con tentativi multipli e backoff esponenziale
        var lastException: Exception? = null
        val maxAttempts = 3

        for (attempt in 1..maxAttempts) {
            try {
                if (attempt > 1) {
                    val backoffDelay = calculateExponentialBackoffWithJitter(attempt)
                    counterRateLimitBackoffs.incrementAndGet()
                    Log.d(TAG, "Applicazione backoff di ${backoffDelay}ms (tentativo $attempt/$maxAttempts)")
                    delay(backoffDelay)
                }

                val prompt = buildSearchPrompt(cleanLocation, attempt)

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("tools", JSONArray().apply {
                        put(JSONObject().apply {
                            put("googleSearch", JSONObject())
                        })
                    })
                }

                val request = Request.Builder()
                    .url("$BASE_URL?key=$apiKey")
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val callStartTime = System.currentTimeMillis()
                val response = okHttpClient.newCall(request).execute()
                val latency = System.currentTimeMillis() - callStartTime

                if (response.code == 429) {
                    // HTTP 429: Quota Gemini API esaurita
                    counterQuotaExceeded.incrementAndGet()
                    Log.w(TAG, "HTTP 429 Quota API esaurita. Applicazione backoff esponenziale.")
                    response.close()
                    continue
                }

                if (response.code in 500..599) {
                    Log.w(TAG, "Errore server HTTP ${response.code} al tentativo $attempt.")
                    response.close()
                    continue
                }

                if (!response.isSuccessful) {
                    Log.w(TAG, "Chiamata API fallita (HTTP ${response.code}), ripiegamento sui valori interni")
                    response.close()
                    break
                }

                val responseBody = response.body?.string() ?: ""

                val jsonResponse = JSONObject(responseBody)
                val parsedKpi = parseGeminiSearchResponse(cleanLocation, jsonResponse, latency)

                // 4. Validazione e sanitizzazione dati
                val sanitizedKpi = sanitizeAndValidateKpi(cleanLocation, parsedKpi, latency)

                cache[cacheKey] = sanitizedKpi
                counterSuccessfulLive.incrementAndGet()
                totalLatencyAccumulator.addAndGet(System.currentTimeMillis() - startTime)

                return@withContext Result.success(sanitizedKpi)
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Tentativo $attempt fallito: ${e.message}")
            }
        }

        // 5. Fallback in caso di tentativi esauriti
        Log.d(TAG, "Chiamata AI fallita: uso valori fittizi interni per $cleanLocation")
        val fallback = getCuratedProvinceKpi(cleanLocation).copy(
            usedFallbackData = true,
            scrapeLatencyMs = System.currentTimeMillis() - startTime,
            mitigationEngineStatus = "Valori fittizi interni"
        )
        cache[cacheKey] = fallback
        Result.success(fallback)
    }

    /**
     * Regola la frequenza delle richieste con una finestra temporale e jitter.
     */
    private suspend fun applyAdaptiveRateLimitingAndJitter() = rateLimitMutex.withLock {
        val now = System.currentTimeMillis()

        while (requestTimestamps.isNotEmpty() && (now - requestTimestamps.first()) > RATE_WINDOW_MS) {
            requestTimestamps.removeFirst()
        }

        if (requestTimestamps.size >= MAX_REQUESTS_PER_WINDOW) {
            val oldest = requestTimestamps.first()
            val waitTime = (RATE_WINDOW_MS - (now - oldest)).coerceAtLeast(300L) + Random.nextLong(150, 450)
            Log.d(TAG, "Limite di frequenza raggiunto: attesa ${waitTime}ms")
            delay(waitTime)
        }

        val elapsedSinceLast = now - lastRequestTime
        val desiredPacing = Random.nextLong(400, 850)
        if (elapsedSinceLast < desiredPacing) {
            val pacingDelay = desiredPacing - elapsedSinceLast
            delay(pacingDelay)
        }

        val requestInstant = System.currentTimeMillis()
        requestTimestamps.addLast(requestInstant)
        lastRequestTime = requestInstant
    }

    /**
     * Calcola il ritardo per il backoff esponenziale con jitter.
     */
    private fun calculateExponentialBackoffWithJitter(attempt: Int): Long {
        val baseDelay = 1200.0
        val maxDelay = 8000.0
        val exp = min(maxDelay, baseDelay * 2.0.pow(attempt - 1))
        val jitter = Random.nextDouble(0.6, 1.4)
        return (exp * jitter).toLong()
    }

    /**
     * Costruisce il prompt per Gemini con ricerca web.
     */
    private fun buildSearchPrompt(location: String, attempt: Int): String {
        val searchSyntax = when (attempt % 3) {
            1 -> "mercato immobiliare $location prezzi metro quadro affitti quotazioni 2026"
            2 -> "osservatorio quotazioni $location andamento prezzi vendita locazione OMI"
            else -> "costo medio vendita affitto mq $location rendimento lordo giorni medi"
        }

        return """
            Sei un assistente di analisi immobiliare sui dati del mercato italiano e dell'Osservatorio del Mercato Immobiliare (OMI).
            Usa lo strumento Google Search con la query: "$searchSyntax" per stimare le quotazioni e i KPI di mercato immobiliare attuali per: "$location".
            
            Estrai con precisione i seguenti dati numerici e strutturali:
            - COMUNE_PROVINCIA: Nome esatto del comune e sigla provincia (es. Paderno Dugnano (MI))
            - REGIONE: (es. Lombardia)
            - PREZZO_VENDITA_MEDIO_MQ: Prezzo medio di vendita al metro quadro in euro (numero puro, es. 2120)
            - PREZZO_VENDITA_MIN_MQ: Prezzo minimo al metro quadro (es. 1650)
            - PREZZO_VENDITA_MAX_MQ: Prezzo massimo al metro quadro (es. 2850)
            - TREND_VENDITA_YOY: Variazione percentuale annua vendita (es. +4.2 o -1.5)
            - PREZZO_AFFITTO_MEDIO_MQ: Canone medio di locazione mensile al metro quadro in euro (es. 12.80)
            - TREND_AFFITTO_YOY: Variazione percentuale annua locazione (es. +6.5)
            - RENDIMENTO_LORDO_PERCENTUALE: Gross rental yield stimato (es. 7.2)
            - INDICE_SATURAZIONE_MERCATO: Punteggio da 0 (mercato caldissimo/alta domanda) a 100 (mercato saturo/lento) (es. 38)
            - GIORNI_MEDI_VENDITA: Giorni medi sul mercato (DOM - Days on Market) (es. 95)
            - TASSO_ASSORBIMENTO: Percentuale di immobili venduti entro 6 mesi (es. 68.0)
            - MICRO_ZONE: Elenco di 2-4 quartieri/frazioni con relativi prezzi medi al mq nel formato "NomeZona:PrezzoMq" (es. Centro:2250, Palazzolo:1980, Calderara:1850)
            - URL_FONTE_IMMOBILIARE: URL di riferimento della pagina o osservatorio
            - SINTESI_MERCATO: Breve sintesi di 2 frasi sui trend attuali.
            
            Formatta la risposta in modo chiaro con i campi indicati.
        """.trimIndent()
    }

    /**
     * Effettua il parsing della risposta da Gemini con Google Search Grounding.
     */
    private fun parseGeminiSearchResponse(queryLocation: String, json: JSONObject, latency: Long): ProvinceScrapedKpi {
        var textContent = ""
        val citations = mutableListOf<String>()

        try {
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        sb.append(parts.getJSONObject(i).optString("text", "")).append("\n")
                    }
                    textContent = sb.toString()
                }

                val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
                if (groundingMetadata != null) {
                    val searchChunks = groundingMetadata.optJSONArray("groundingChunks")
                    if (searchChunks != null) {
                        for (i in 0 until searchChunks.length()) {
                            val chunk = searchChunks.getJSONObject(i)
                            val web = chunk.optJSONObject("web")
                            if (web != null) {
                                val uri = web.optString("uri", "")
                                if (uri.isNotBlank()) {
                                    citations.add(uri)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Errore parsing candidates/grounding: ${e.message}")
        }

        if (textContent.isBlank()) {
            return getCuratedProvinceKpi(queryLocation)
        }

        val avgSale = extractDouble(textContent, "(?:PREZZO_VENDITA_MEDIO_MQ|Prezzo medio vendita)[:\\s]+€?\\s*([0-9.,]+)")
        val minSale = extractDouble(textContent, "(?:PREZZO_VENDITA_MIN_MQ|Prezzo minimo)[:\\s]+€?\\s*([0-9.,]+)")
        val maxSale = extractDouble(textContent, "(?:PREZZO_VENDITA_MAX_MQ|Prezzo massimo)[:\\s]+€?\\s*([0-9.,]+)")
        val saleTrend = extractDouble(textContent, "(?:TREND_VENDITA_YOY|Trend vendita)[:\\s]+([+-]?[0-9.,]+)%?")
        val avgRent = extractDouble(textContent, "(?:PREZZO_AFFITTO_MEDIO_MQ|Affitto medio)[:\\s]+€?\\s*([0-9.,]+)")
        val rentTrend = extractDouble(textContent, "(?:TREND_AFFITTO_YOY|Trend affitto)[:\\s]+([+-]?[0-9.,]+)%?")
        val grossYield = if (avgRent != null && avgSale != null && avgSale > 0.0) {
            (avgRent * 12.0 / avgSale) * 100.0
        } else null
        val saturation = extractInt(textContent, "(?:INDICE_SATURAZIONE_MERCATO|Saturazione)[:\\s]+([0-9]+)")
        val dom = extractInt(textContent, "(?:GIORNI_MEDI_VENDITA|Giorni medi|DOM)[:\\s]+([0-9]+)")
        val absorption = extractDouble(textContent, "(?:TASSO_ASSORBIMENTO|Assorbimento)[:\\s]+([0-9.,]+)%?")
        val url = extractString(textContent, "(?:URL_FONTE_IMMOBILIARE|Fonte|URL)[:\\s]+(https?://[^\\s]+)") ?: ""
        val summary = extractString(textContent, "(?:SINTESI_MERCATO|Sintesi)[:\\s]+([^\n]+)") ?: ""

        val microZones = parseMicroZones(textContent)
        val provinceGuess = detectProvinceCode(queryLocation)

        val measurementFields = listOf(
            avgSale,
            minSale,
            maxSale,
            saleTrend,
            avgRent,
            rentTrend,
            grossYield,
            saturation,
            dom,
            absorption
        )
        val completeness = measurementFields.count { it != null }.toDouble() / measurementFields.size.toDouble()

        val hasGrounding = citations.isNotEmpty() || textContent.contains("Fonte:", ignoreCase = true)
        
        return ProvinceScrapedKpi(
            locationName = queryLocation.capitalizeWords(),
            province = provinceGuess,
            region = "Italia",
            avgSalePriceSqM = avgSale,
            minSalePriceSqM = minSale,
            maxSalePriceSqM = maxSale,
            saleTrendYoY = saleTrend,
            avgRentPriceSqM = avgRent,
            rentTrendYoY = rentTrend,
            grossRentalYield = grossYield,
            marketSaturationScore = saturation,
            avgDaysOnMarket = dom,
            absorptionRatePercent = absorption,
            hotMicroZones = microZones,
            sourceUrl = url,
            sourceCitations = citations.distinct().take(5),
            scrapedAt = System.currentTimeMillis(),
            isLiveScraped = hasGrounding,
            marketSummary = summary,
            historicalTrends = emptyList(),
            historicalDatasetVerified = false,
            historicalSampleSize = 0,
            usedFallbackData = false,
            scrapeLatencyMs = latency,
            completeness = completeness,
            sourceReliability = null,
            valuationConfidence = null,
            mitigationEngineStatus = if (hasGrounding) "Live Gemini Search Grounding" else "AI Inference (Ungrounded)"
        )
    }

    /**
     * Sanitizza i campi numerici per mantenere coerenza e range plausibili.
     */
    private fun sanitizeAndValidateKpi(
        queryLocation: String,
        raw: ProvinceScrapedKpi,
        latency: Long
    ): ProvinceScrapedKpi {
        val safeSale = raw.avgSalePriceSqM?.coerceIn(400.0, 25000.0)
        val safeMin = raw.minSalePriceSqM?.let { min ->
            val maxBound = safeSale ?: 35000.0
            min.coerceIn(300.0, maxBound)
        }
        val safeMax = raw.maxSalePriceSqM?.let { max ->
            val minBound = safeSale ?: 300.0
            max.coerceIn(minBound, 35000.0)
        }
        val safeRent = raw.avgRentPriceSqM?.coerceIn(3.0, 60.0)
        val safeYield = if (safeRent != null && safeSale != null && safeSale > 0.0) {
            ((safeRent * 12.0) / safeSale) * 100.0
        } else null
        val safeSat = raw.marketSaturationScore?.coerceIn(5, 95)
        val safeDom = raw.avgDaysOnMarket?.coerceIn(25, 365)
        val safeAbsorption = raw.absorptionRatePercent?.coerceIn(10.0, 99.0)

        val measurementFields = listOf(
            safeSale,
            safeMin,
            safeMax,
            raw.saleTrendYoY,
            safeRent,
            raw.rentTrendYoY,
            safeYield,
            safeSat,
            safeDom,
            safeAbsorption
        )
        val completeness = measurementFields.count { it != null }.toDouble() / measurementFields.size.toDouble()

        return raw.copy(
            avgSalePriceSqM = safeSale,
            minSalePriceSqM = safeMin,
            maxSalePriceSqM = safeMax,
            avgRentPriceSqM = safeRent,
            grossRentalYield = safeYield,
            marketSaturationScore = safeSat,
            avgDaysOnMarket = safeDom,
            absorptionRatePercent = safeAbsorption,
            historicalTrends = raw.historicalTrends,
            scrapeLatencyMs = latency,
            completeness = completeness,
            sourceReliability = null,
            valuationConfidence = null
        )
    }

    private fun extractDouble(text: String, patternStr: String): Double? {
        return try {
            val pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val raw = matcher.group(1)?.replace(".", "")?.replace(",", ".")?.trim()
                raw?.toDoubleOrNull()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractInt(text: String, patternStr: String): Int? {
        return try {
            val pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val raw = matcher.group(1)?.trim()
                raw?.toIntOrNull()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractString(text: String, patternStr: String): String? {
        return try {
            val pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                matcher.group(1)?.trim()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMicroZones(text: String): List<Pair<String, Double>> {
        val list = mutableListOf<Pair<String, Double>>()
        try {
            val pattern = Pattern.compile("([A-Za-zÀ-ÿ\\s]+)[:=-]\\s*€?\\s*([0-9.,]+)\\s*(?:€?/m²|€?/mq)?", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val name = matcher.group(1)?.trim() ?: ""
                val price = matcher.group(2)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull()
                if (name.length in 3..25 && price != null && price in 500.0..15000.0 && !name.contains("PREZZO", ignoreCase = true)) {
                    list.add(name to price)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return list.take(5)
    }

    private fun cleanLocationQuery(raw: String): String {
        return raw.replace(Regex("(?i)via|viale|corso|piazza|largo|strada|n\\.?\\s*\\d+|\\d+"), "")
            .replace(",", " ")
            .trim()
            .ifBlank { "Milano" }
    }

    private fun detectProvinceCode(location: String): String {
        val lower = location.lowercase()
        return when {
            lower.contains("paderno") || lower.contains("milano") || lower.contains("sesto") || lower.contains("cinisello") || lower.contains("rho") -> "MI"
            lower.contains("monza") || lower.contains("lissone") || lower.contains("desio") || lower.contains("seregno") -> "MB"
            lower.contains("roma") || lower.contains("tivoli") || lower.contains("fiumicino") -> "RM"
            lower.contains("torino") || lower.contains("moncalieri") || lower.contains("rivoli") -> "TO"
            lower.contains("bologna") || lower.contains("imola") || lower.contains("casalecchio") -> "BO"
            lower.contains("firenze") || lower.contains("scandicci") || lower.contains("sesto fiorentino") -> "FI"
            lower.contains("napoli") || lower.contains("pozzuoli") || lower.contains("giugliano") -> "NA"
            lower.contains("bergamo") -> "BG"
            lower.contains("brescia") -> "BS"
            lower.contains("verona") -> "VR"
            lower.contains("genova") -> "GE"
            lower.contains("palermo") -> "PA"
            lower.contains("bari") -> "BA"
            lower.contains("catania") -> "CT"
            else -> "IT"
        }
    }

    /**
     * Valori di riferimento fittizi inseriti nel codice in assenza di connessione o quota.
     */
    fun getCuratedProvinceKpi(location: String): ProvinceScrapedKpi {
        val lower = location.lowercase()
        return when {
            lower.contains("paderno") -> ProvinceScrapedKpi(
                locationName = "Paderno Dugnano",
                province = "MI",
                region = "Lombardia",
                avgSalePriceSqM = 2120.0,
                minSalePriceSqM = 1680.0,
                maxSalePriceSqM = 2850.0,
                saleTrendYoY = 4.6,
                avgRentPriceSqM = 12.80,
                rentTrendYoY = 6.8,
                grossRentalYield = 7.25,
                marketSaturationScore = 34,
                avgDaysOnMarket = 92,
                absorptionRatePercent = 74.0,
                hotMicroZones = listOf("Centro" to 2250.0, "Palazzolo Milanese" to 2180.0, "Dugnano" to 2050.0, "Calderara" to 1920.0),
                sourceUrl = "",
                marketSummary = "Forte domanda di residenziale da pendolari verso Milano con ottimi rendimenti e tempi di vendita rapidi (<95gg).",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
            lower.contains("monza") -> ProvinceScrapedKpi(
                locationName = "Monza",
                province = "MB",
                region = "Lombardia",
                avgSalePriceSqM = 2850.0,
                minSalePriceSqM = 2100.0,
                maxSalePriceSqM = 4400.0,
                saleTrendYoY = 5.2,
                avgRentPriceSqM = 14.50,
                rentTrendYoY = 7.1,
                grossRentalYield = 6.10,
                marketSaturationScore = 41,
                avgDaysOnMarket = 98,
                absorptionRatePercent = 71.0,
                hotMicroZones = listOf("Centro Storico" to 3950.0, "Parco / Villa Reale" to 3600.0, "San Biagio" to 2900.0, "Triante" to 2600.0),
                sourceUrl = "",
                marketSummary = "Mercato solido spinto dalla qualità urbana e dal prolungamento M5 in arrivo.",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
            lower.contains("roma") -> ProvinceScrapedKpi(
                locationName = "Roma",
                province = "RM",
                region = "Lazio",
                avgSalePriceSqM = 3350.0,
                minSalePriceSqM = 1850.0,
                maxSalePriceSqM = 8200.0,
                saleTrendYoY = 2.4,
                avgRentPriceSqM = 16.20,
                rentTrendYoY = 8.5,
                grossRentalYield = 5.80,
                marketSaturationScore = 52,
                avgDaysOnMarket = 125,
                absorptionRatePercent = 62.0,
                hotMicroZones = listOf("Centro Storico" to 7600.0, "Prati / Parioli" to 5800.0, "Trastevere" to 5400.0, "Eur / Laurentina" to 3400.0, "Tuscolano" to 2750.0),
                sourceUrl = "",
                marketSummary = "Forte spinta locativa Jubilee 2025/2026 e transizione short-rent su aree centrali.",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
            lower.contains("torino") -> ProvinceScrapedKpi(
                locationName = "Torino",
                province = "TO",
                region = "Piemonte",
                avgSalePriceSqM = 1980.0,
                minSalePriceSqM = 1100.0,
                maxSalePriceSqM = 4200.0,
                saleTrendYoY = 3.8,
                avgRentPriceSqM = 11.40,
                rentTrendYoY = 5.9,
                grossRentalYield = 6.91,
                marketSaturationScore = 38,
                avgDaysOnMarket = 108,
                absorptionRatePercent = 69.0,
                hotMicroZones = listOf("Centro" to 3800.0, "Crocetta" to 3100.0, "San Salvario" to 2350.0, "Santa Rita" to 1750.0, "Barriera di Milano" to 1050.0),
                sourceUrl = "",
                marketSummary = "Ottimo mercato per investimenti a resa per studenti universitari (Politecnico) e riqualificazioni.",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
            lower.contains("bologna") -> ProvinceScrapedKpi(
                locationName = "Bologna",
                province = "BO",
                region = "Emilia-Romagna",
                avgSalePriceSqM = 3450.0,
                minSalePriceSqM = 2300.0,
                maxSalePriceSqM = 5600.0,
                saleTrendYoY = 4.1,
                avgRentPriceSqM = 17.80,
                rentTrendYoY = 9.2,
                grossRentalYield = 6.19,
                marketSaturationScore = 22,
                avgDaysOnMarket = 74,
                absorptionRatePercent = 84.0,
                hotMicroZones = listOf("Centro Storico" to 4850.0, "Saragozza" to 3900.0, "Mazzini / Savena" to 2950.0, "Navile / Bolognina" to 2750.0),
                sourceUrl = "",
                marketSummary = "Altissima domanda locativa studentesca e corporate, tasso di sfitto prossimo allo zero (<75gg sul mercato).",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
            lower.contains("firenze") -> ProvinceScrapedKpi(
                locationName = "Firenze",
                province = "FI",
                region = "Toscana",
                avgSalePriceSqM = 4250.0,
                minSalePriceSqM = 2600.0,
                maxSalePriceSqM = 7800.0,
                saleTrendYoY = 3.6,
                avgRentPriceSqM = 19.50,
                rentTrendYoY = 7.8,
                grossRentalYield = 5.51,
                marketSaturationScore = 48,
                avgDaysOnMarket = 112,
                absorptionRatePercent = 66.0,
                hotMicroZones = listOf("Centro Storico" to 6100.0, "Campo di Marte" to 3800.0, "Rifredi / Novoli" to 2950.0, "Isolotto" to 2800.0),
                sourceUrl = "",
                marketSummary = "Dominato dalla locazione turistica internazionale e acquirenti esteri su immobili di pregio.",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
            lower.contains("napoli") -> ProvinceScrapedKpi(
                locationName = "Napoli",
                province = "NA",
                region = "Campania",
                avgSalePriceSqM = 2850.0,
                minSalePriceSqM = 1400.0,
                maxSalePriceSqM = 6200.0,
                saleTrendYoY = 5.8,
                avgRentPriceSqM = 13.90,
                rentTrendYoY = 11.2,
                grossRentalYield = 5.85,
                marketSaturationScore = 32,
                avgDaysOnMarket = 88,
                absorptionRatePercent = 76.0,
                hotMicroZones = listOf("Posillipo / Chiaia" to 5400.0, "Vomero" to 4200.0, "Centro Storico / Toledo" to 2900.0, "Fuorigrotta" to 2100.0),
                sourceUrl = "",
                marketSummary = "Boom turistico senza precedenti nel centro storico con fortissima rivalutazione dei canoni (+11.2%).",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
            lower.contains("bergamo") -> ProvinceScrapedKpi(
                locationName = "Bergamo",
                province = "BG",
                region = "Lombardia",
                avgSalePriceSqM = 2450.0,
                minSalePriceSqM = 1600.0,
                maxSalePriceSqM = 4500.0,
                saleTrendYoY = 4.8,
                avgRentPriceSqM = 12.50,
                rentTrendYoY = 6.4,
                grossRentalYield = 6.12,
                marketSaturationScore = 36,
                avgDaysOnMarket = 89,
                absorptionRatePercent = 75.0,
                hotMicroZones = listOf("Città Alta" to 4600.0, "Centro / Sentierone" to 3200.0, "Borgo Palazzo" to 2100.0, "Colognola" to 1750.0),
                sourceUrl = "",
                marketSummary = "Crescita costante post Capitale della Cultura e forte appeal aeroportuale Orio.",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
            lower.contains("brescia") -> ProvinceScrapedKpi(
                locationName = "Brescia",
                province = "BS",
                region = "Lombardia",
                avgSalePriceSqM = 2150.0,
                minSalePriceSqM = 1450.0,
                maxSalePriceSqM = 3600.0,
                saleTrendYoY = 3.9,
                avgRentPriceSqM = 11.20,
                rentTrendYoY = 5.8,
                grossRentalYield = 6.25,
                marketSaturationScore = 40,
                avgDaysOnMarket = 96,
                absorptionRatePercent = 70.0,
                hotMicroZones = listOf("Centro Storico" to 2950.0, "Crocifissa di Rosa" to 2300.0, "Ring / Stazione" to 1750.0, "Lamarmora" to 1600.0),
                sourceUrl = "",
                marketSummary = "Polo industriale solido con ottima redditività locativa periferica.",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
            lower.contains("verona") -> ProvinceScrapedKpi(
                locationName = "Verona",
                province = "VR",
                region = "Veneto",
                avgSalePriceSqM = 2650.0,
                minSalePriceSqM = 1650.0,
                maxSalePriceSqM = 4800.0,
                saleTrendYoY = 4.3,
                avgRentPriceSqM = 12.90,
                rentTrendYoY = 6.9,
                grossRentalYield = 5.84,
                marketSaturationScore = 35,
                avgDaysOnMarket = 91,
                absorptionRatePercent = 73.0,
                hotMicroZones = listOf("Centro Storico / Bra" to 4200.0, "Borgo Trento" to 3100.0, "Borgo Venezia" to 2050.0, "Golosine" to 1550.0),
                sourceUrl = "",
                marketSummary = "Hub logistico ed eventi/fiere (Vinitaly) con mercato residenziale molto attivo.",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
            lower.contains("genova") -> ProvinceScrapedKpi(
                locationName = "Genova",
                province = "GE",
                region = "Liguria",
                avgSalePriceSqM = 1680.0,
                minSalePriceSqM = 850.0,
                maxSalePriceSqM = 3900.0,
                saleTrendYoY = 1.9,
                avgRentPriceSqM = 9.80,
                rentTrendYoY = 4.2,
                grossRentalYield = 7.00,
                marketSaturationScore = 55,
                avgDaysOnMarket = 135,
                absorptionRatePercent = 58.0,
                hotMicroZones = listOf("Albaro / Foce" to 3400.0, "Castelletto / Carignano" to 2400.0, "San Fruttuoso" to 1450.0, "Sampierdarena" to 950.0),
                sourceUrl = "",
                marketSummary = "Forte forbice tra quartieri costieri signorili e zone collinari ad alto rendimento lordo.",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
            lower.contains("palermo") -> ProvinceScrapedKpi(
                locationName = "Palermo",
                province = "PA",
                region = "Sicilia",
                avgSalePriceSqM = 1450.0,
                minSalePriceSqM = 750.0,
                maxSalePriceSqM = 2900.0,
                saleTrendYoY = 2.1,
                avgRentPriceSqM = 8.60,
                rentTrendYoY = 5.5,
                grossRentalYield = 7.12,
                marketSaturationScore = 46,
                avgDaysOnMarket = 120,
                absorptionRatePercent = 60.0,
                hotMicroZones = listOf("Politeama / Libertà" to 2600.0, "Centro Storico / Kalsa" to 1950.0, "Notarbartolo" to 1650.0, "Zisa" to 1050.0),
                sourceUrl = "",
                marketSummary = "Prezzi d'ingresso bassi con yield lordi superiori al 7% in centro e zone universitarie.",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
            lower.contains("bari") -> ProvinceScrapedKpi(
                locationName = "Bari",
                province = "BA",
                region = "Puglia",
                avgSalePriceSqM = 2050.0,
                minSalePriceSqM = 1200.0,
                maxSalePriceSqM = 3800.0,
                saleTrendYoY = 4.5,
                avgRentPriceSqM = 11.10,
                rentTrendYoY = 7.6,
                grossRentalYield = 6.50,
                marketSaturationScore = 37,
                avgDaysOnMarket = 94,
                absorptionRatePercent = 72.0,
                hotMicroZones = listOf("Murat / Borgo Antico" to 3300.0, "Poggiofranco" to 2650.0, "Madonnella" to 1950.0, "Libertà" to 1350.0),
                sourceUrl = "",
                marketSummary = "Hub tecnologico del Mezzogiorno con forte crescita dei canoni di locazione.",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
            else -> ProvinceScrapedKpi(
                locationName = "Milano",
                province = "MI",
                region = "Lombardia",
                avgSalePriceSqM = 5380.0,
                minSalePriceSqM = 2900.0,
                maxSalePriceSqM = 11500.0,
                saleTrendYoY = 4.2,
                avgRentPriceSqM = 23.40,
                rentTrendYoY = 8.1,
                grossRentalYield = 5.22,
                marketSaturationScore = 28,
                avgDaysOnMarket = 68,
                absorptionRatePercent = 88.0,
                hotMicroZones = listOf("Centro Storico / Brera" to 10800.0, "Porta Nuova / Garibaldi" to 9200.0, "Navigli / Porta Genova" to 6400.0, "NoLo / Loreto" to 4250.0, "Baggio / San Siro" to 2950.0),
                sourceUrl = "",
                marketSummary = "Capitale finanziaria e polo attrattivo internazionale con massima liquidità di mercato e assorbimento rapido.",
                historicalTrends = emptyList(),
                historicalDatasetVerified = false,
                historicalSampleSize = 0,
                usedFallbackData = true,
                isLiveScraped = false,
                completeness = 1.0,
                sourceReliability = null,
                valuationConfidence = null,
                mitigationEngineStatus = "Valori interni di riferimento (Non verificati)"
            )
        }
    }

    /**
     * Restituisce tutte le province principali con valori di riferimento interni.
     */
    fun getAllCuratedProvinceKpis(): List<ProvinceScrapedKpi> {
        val list = listOf(
            "Milano",
            "Monza",
            "Paderno Dugnano",
            "Bergamo",
            "Brescia",
            "Roma",
            "Torino",
            "Bologna",
            "Firenze",
            "Napoli",
            "Verona",
            "Genova",
            "Palermo",
            "Bari"
        )
        return list.map { getCuratedProvinceKpi(it) }
    }

    /**
     * Aggrega tutte le province per Regione.
     */
    fun getAllRegionalAggregates(): List<RegionalAggregateKpi> {
        val provinces = getAllCuratedProvinceKpis()
        val byRegion = provinces.groupBy { it.region }

        return byRegion.map { (region, provs) ->
            val avgSale = provs.mapNotNull { it.avgSalePriceSqM }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
            val avgRent = provs.mapNotNull { it.avgRentPriceSqM }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
            val avgYield = provs.mapNotNull { it.grossRentalYield }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
            val avgSat = provs.mapNotNull { it.marketSaturationScore }.takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0
            val avgDom = provs.mapNotNull { it.avgDaysOnMarket }.takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0
            val saleTrend = provs.mapNotNull { it.saleTrendYoY }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
            val rentTrend = provs.mapNotNull { it.rentTrendYoY }.takeIf { it.isNotEmpty() }?.average() ?: 0.0

            RegionalAggregateKpi(
                regionName = region,
                avgSalePriceSqM = avgSale,
                avgRentPriceSqM = avgRent,
                avgGrossYield = avgYield,
                avgMarketSaturation = avgSat,
                avgDaysOnMarket = avgDom,
                saleTrendYoY = saleTrend,
                rentTrendYoY = rentTrend,
                provinceCount = provs.size,
                topProvinces = provs,
                historicalTimeline = emptyList()
            )
        }.sortedByDescending { it.avgSalePriceSqM }
    }

    /**
     * Svuota la cache in memoria (utilizzato anche nei test unitari).
     */
    fun clearCache() {
        cache.clear()
    }

    /**
     * Inserisce direttamente un elemento in cache (utile per test e precaricamento).
     */
    fun putInCache(location: String, kpi: ProvinceScrapedKpi) {
        val cleanLocation = cleanLocationQuery(location)
        val cacheKey = cleanLocation.lowercase().trim()
        cache[cacheKey] = kpi
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
