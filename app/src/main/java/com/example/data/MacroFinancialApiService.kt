package com.example.data

import android.util.Log
import com.example.util.ScraperCircuitBreaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object MacroFinancialApiService {
    private const val TAG = "MacroFinancialApiService"
    private const val SOURCE_KEY = "ECB_EUROSTAT_PUBLIC_MACRO"

    /**
     * Tenta la verifica di connettività di rete. Poiché l'endpoint non fornisce i tassi BTP/Euribor reali,
     * restituisce sempre il baseline di riferimento interno marcato come non live.
     */
    suspend fun fetchLatestMacroEconomicData(): MacroEconomicData = withContext(Dispatchers.IO) {
        val result = ScraperCircuitBreaker.execute(
            sourceKey = SOURCE_KEY,
            fallback = { getInternalRateBaseline() }
        ) {
            fetchFromPublicFinancialFeeds()
        }

        result.getOrElse { getInternalRateBaseline() }
    }

    private fun fetchFromPublicFinancialFeeds(): MacroEconomicData {
        try {
            // Verifica di connettività verso endpoint pubblico
            val url = URL("https://api.frankfurter.app/latest?from=EUR&to=USD")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "QuantumDealRadar-MacroEngine/1.0")
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                conn.disconnect()

                val json = JSONObject(sb.toString())
                Log.d(TAG, "Connettività di rete verificata (data endpoint: ${json.optString("date")}), ma l'endpoint risponde senza fornire i tassi BTP/Euribor utilizzati: applicazione baseline interno.")

                return getInternalRateBaseline()
            } else {
                throw IllegalStateException("HTTP Response: $responseCode")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Errore connessione di rete (${e.message}): utilizzo baseline interno.")
            return getInternalRateBaseline()
        }
    }

    /**
     * Valori di riferimento interni per tassi BTP, Euribor e parametri macroeconomici.
     * Data di fissazione: data di fissazione ignota. Autore: autore ignoto.
     * Provenienza: CURATED_FALLBACK (non collegato a fonti esterne live).
     */
    fun getInternalRateBaseline(): MacroEconomicData {
        return MacroEconomicData(
            id = 1,
            ecbMainRefinancingRate = 3.75,
            euribor3M = 3.55,
            euribor12M = 3.42,
            italianBtp10YYield = 3.65,
            italyHicpInflationRate = 1.90,
            eurozoneInflationRate = 2.20,
            avgMortgageFixedRate = 3.35,
            avgMortgageVariableRate = 4.10,
            targetHurdleSpreadBps = 300,
            lastUpdatedTimestamp = System.currentTimeMillis(),
            sourceProvider = "Valore di riferimento interno - non aggiornato da fonte esterna",
            isLiveFetched = false,
            provenance = DataProvenance.CURATED_FALLBACK.name
        )
    }

    fun getCuratedEcbEurostatBaseline(): MacroEconomicData = getInternalRateBaseline()
}
