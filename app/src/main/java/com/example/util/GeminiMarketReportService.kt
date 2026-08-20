package com.example.util

import com.example.BuildConfig
import com.example.data.PropertyDeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiMarketReportService {
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateMonthlyMarketReport(
        regionName: String,
        regionDeals: List<PropertyDeal> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
            // Return informative notice with fallback report
            return@withContext Result.failure(
                IllegalStateException("API Key Gemini non configurata. Imposta GEMINI_API_KEY nei Secrets di AI Studio per abilitare la generazione in tempo reale.")
            )
        }

        val dealCount = regionDeals.size
        val avgPrice = if (regionDeals.isNotEmpty()) regionDeals.map { it.askingPrice }.average().toInt() else 0
        val avgDiscount = if (regionDeals.isNotEmpty()) regionDeals.map { it.discountPercent }.average().toInt() else 0
        val avgCapRate = if (regionDeals.isNotEmpty()) String.format("%.1f", regionDeals.map { it.estimatedCapRate }.average()) else "0.0"

        val prompt = """
            Sei un PERITO ESTIMATORE SENIOR iscritto al Ruolo dei Periti ed Esperti con oltre 20 ANNI DI ESPERIENZA in estimo immobiliare, CTU per i tribunali italiani e Market Comparison Approach (UNI 10750).
            Genera un report e perizia di stima per la zona di: "$regionName".
            
            Dati reali aggregati dal database DealRadar per $regionName:
            - Numero di opportunita monitorate: $dealCount
            - Prezzo medio richiesto/asta: $avgPrice €
            - Sconto medio perizia: $avgDiscount%
            - Cap Rate stimato medio: $avgCapRate%

            Fornisci una stima e un report strutturato in lingua italiana diviso nelle seguenti sezioni con emoji e formattazione markdown pulita:
            
            1. 📊 **SINTESI TREND MENSILE ED ESTIMATIVA ($regionName)**
               - Panoramica del trend dei prezzi e volume delle transazioni negli ultimi 30 giorni.
               
            2. 🏛️ **PERIZIA COMPARATIVA OMI (€/mq) E COEFFICIENTI**
               - Stima dei valori medi OMI al metro quadro per centro e periferia.
               - Coefficienti di vetustà, piano, classe energetica e stato manutentivo applicabili.
               
            3. 💶 **RENDIMENTI E CAP RATE DA LOCAZIONE**
               - Rendimenti lordi e netti medi (Cap Rate) previsti per questa zona.
               
            4. 🎯 **OPPORTUNITÀ TARGET ED ELEVATO ROI**
               - Tipologie immobiliari più redditizie (es. Trilocali, Bilocali da ristrutturare, Aste NPL, Saldo e Stralcio).
               
            5. ⚠️ **CHECKLIST RIGOROSA PERITO SENIOR (DUE DILIGENCE)**
               - 3 raccomandazioni strategiche concrete su verifche urbanistiche, conformità catastale e stato occupativo prima di formulare la proposta.

            Mantieni un tono autorevole, peritale, rigoroso e analitico.
        """.trimIndent()

        try {
            val jsonBody = JSONObject().apply {
                val contentsArray = org.json.JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = org.json.JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Errore API Gemini (${response.code}): $responseBodyStr")
                )
            }

            val responseJson = JSONObject(responseBodyStr)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val contentObj = firstCandidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) {
                        return@withContext Result.success(text)
                    }
                }
            }

            Result.failure(Exception("Risposta Gemini non valida o vuota."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
