package com.example.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Real-time Foreign Exchange (FX) Rate Service for international real estate investors.
 * Provides live exchange rates against the base Euro (EUR) currency with automated fetching,
 * offline fallback, caching, and multi-currency formatting utilities.
 */
object CurrencyExchangeRateService {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // Default reference rates based on latest ECB benchmarks
    private val defaultRates = AppCurrency.entries.associateWith { it.rateFromEur }

    private val _liveRates = MutableStateFlow<Map<AppCurrency, Double>>(defaultRates)
    val liveRates: StateFlow<Map<AppCurrency, Double>> = _liveRates.asStateFlow()

    private val _selectedGlobalCurrency = MutableStateFlow(AppCurrency.EUR)
    val selectedGlobalCurrency: StateFlow<AppCurrency> = _selectedGlobalCurrency.asStateFlow()

    private val _isFetchingRates = MutableStateFlow(false)
    val isFetchingRates: StateFlow<Boolean> = _isFetchingRates.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _rateSourceLabel = MutableStateFlow("Tassi BCE / Open Exchange Live")
    val rateSourceLabel: StateFlow<String> = _rateSourceLabel.asStateFlow()

    private val _isLiveOnline = MutableStateFlow(false)
    val isLiveOnline: StateFlow<Boolean> = _isLiveOnline.asStateFlow()

    init {
        // Initial async background fetch of real-time rates
        fetchLiveRatesAsync()
    }

    fun setSelectedCurrency(currency: AppCurrency) {
        _selectedGlobalCurrency.value = currency
    }

    /**
     * Triggers asynchronous background update of exchange rates from open exchange API endpoints.
     */
    fun fetchLiveRatesAsync(onCompleted: ((Boolean) -> Unit)? = null) {
        serviceScope.launch {
            val success = fetchLiveRatesInternal()
            withContext(Dispatchers.Main) {
                onCompleted?.invoke(success)
            }
        }
    }

    private suspend fun fetchLiveRatesInternal(): Boolean = withContext(Dispatchers.IO) {
        _isFetchingRates.value = true
        var isSuccess = false
        try {
            // Primary Endpoint: open.er-api.com (Reliable, free open exchange rates)
            val primaryUrl = "https://open.er-api.com/v6/latest/EUR"
            val request = Request.Builder()
                .url(primaryUrl)
                .header("User-Agent", "DealRadar-RealEstate-Analytics/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonBody = response.body?.string()
                if (!jsonBody.isNullOrBlank()) {
                    val root = JSONObject(jsonBody)
                    val result = root.optString("result")
                    if (result == "success" || root.has("rates")) {
                        val ratesObj = root.getJSONObject("rates")
                        val updatedMap = mutableMapOf<AppCurrency, Double>()

                        for (currency in AppCurrency.entries) {
                            if (currency == AppCurrency.EUR) {
                                updatedMap[currency] = 1.0
                            } else if (ratesObj.has(currency.code)) {
                                val rate = ratesObj.getDouble(currency.code)
                                if (rate > 0.0) {
                                    updatedMap[currency] = rate
                                } else {
                                    updatedMap[currency] = currency.rateFromEur
                                }
                            } else {
                                updatedMap[currency] = currency.rateFromEur
                            }
                        }

                        _liveRates.value = updatedMap
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                        _rateSourceLabel.value = "Open Exchange Rates (Live)"
                        _isLiveOnline.value = true
                        isSuccess = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Try Secondary Endpoint: api.frankfurter.app (ECB Reference)
            try {
                val fallbackUrl = "https://api.frankfurter.app/latest?from=EUR"
                val fbRequest = Request.Builder()
                    .url(fallbackUrl)
                    .header("User-Agent", "DealRadar-RealEstate-Analytics/1.0")
                    .build()

                val fbResponse = httpClient.newCall(fbRequest).execute()
                if (fbResponse.isSuccessful) {
                    val fbBody = fbResponse.body?.string()
                    if (!fbBody.isNullOrBlank()) {
                        val root = JSONObject(fbBody)
                        val ratesObj = root.getJSONObject("rates")
                        val updatedMap = mutableMapOf<AppCurrency, Double>()

                        for (currency in AppCurrency.entries) {
                            if (currency == AppCurrency.EUR) {
                                updatedMap[currency] = 1.0
                            } else if (ratesObj.has(currency.code)) {
                                val rate = ratesObj.getDouble(currency.code)
                                if (rate > 0.0) {
                                    updatedMap[currency] = rate
                                } else {
                                    updatedMap[currency] = currency.rateFromEur
                                }
                            } else {
                                updatedMap[currency] = currency.rateFromEur
                            }
                        }

                        _liveRates.value = updatedMap
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                        _rateSourceLabel.value = "Banca Centrale Europea (Live)"
                        _isLiveOnline.value = true
                        isSuccess = true
                    }
                }
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
                // Graceful fallback to built-in benchmark rates
                _rateSourceLabel.value = "BCE Benchmark (Offline Cache)"
                _isLiveOnline.value = false
            }
        } finally {
            _isFetchingRates.value = false
        }
        isSuccess
    }

    /**
     * Retrieves current effective rate for the specified currency.
     */
    fun getRate(currency: AppCurrency, ratesMap: Map<AppCurrency, Double>? = null): Double {
        if (currency == AppCurrency.EUR) return 1.0
        val map = ratesMap ?: _liveRates.value
        return map[currency] ?: currency.rateFromEur
    }

    /**
     * Converts an amount in base EUR to the specified target currency.
     */
    fun convertFromEur(amountEur: Double, target: AppCurrency, ratesMap: Map<AppCurrency, Double>? = null): Double {
        val rate = getRate(target, ratesMap)
        return amountEur * rate
    }

    /**
     * Converts an amount from a foreign currency back to base EUR.
     */
    fun convertToEur(amountInForeign: Double, from: AppCurrency, ratesMap: Map<AppCurrency, Double>? = null): Double {
        val rate = getRate(from, ratesMap)
        return if (rate > 0.0) amountInForeign / rate else amountInForeign
    }

    /**
     * Formats an amount given in base EUR into a localized string for the target currency.
     */
    fun formatFromEur(
        amountEur: Double,
        target: AppCurrency,
        ratesMap: Map<AppCurrency, Double>? = null,
        includeDecimals: Boolean = false,
        includeSymbol: Boolean = true
    ): String {
        val converted = convertFromEur(amountEur, target, ratesMap)
        return formatRawAmount(converted, target, includeDecimals, includeSymbol)
    }

    /**
     * Formats an already converted raw amount for the given currency.
     */
    fun formatRawAmount(
        amount: Double,
        currency: AppCurrency,
        includeDecimals: Boolean = false,
        includeSymbol: Boolean = true
    ): String {
        val locale = when (currency) {
            AppCurrency.EUR -> Locale.ITALY
            AppCurrency.USD, AppCurrency.CAD, AppCurrency.AUD, AppCurrency.AED -> Locale.US
            AppCurrency.GBP -> Locale.UK
            AppCurrency.CHF -> Locale.GERMANY
            AppCurrency.JPY -> Locale.JAPAN
        }

        val formatter = NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = if (includeDecimals) 2 else 0
            minimumFractionDigits = if (includeDecimals) 2 else 0
        }

        val formattedNumber = formatter.format(if (includeDecimals) amount else amount.toInt())

        if (!includeSymbol) return formattedNumber

        return if (currency.isSymbolPrefix) {
            "${currency.symbol}$formattedNumber"
        } else {
            "$formattedNumber ${currency.symbol}"
        }
    }

    /**
     * Returns a human-friendly exchange rate indicator text (e.g., "1 EUR = 1.09 USD").
     */
    fun getExchangeRatePillText(target: AppCurrency, ratesMap: Map<AppCurrency, Double>? = null): String {
        if (target == AppCurrency.EUR) {
            return "Valuta Base: EUR (€)"
        }
        val rate = getRate(target, ratesMap)
        val formattedRate = String.format(Locale.US, "%.3f", rate)
        return "1 € = $formattedRate ${target.code}"
    }

    /**
     * Returns formatted timestamp of last synchronization.
     */
    fun getFormattedLastSync(): String {
        val date = Date(_lastSyncTimestamp.value)
        val sdf = SimpleDateFormat("HH:mm", Locale.ITALY)
        return sdf.format(date)
    }
}
