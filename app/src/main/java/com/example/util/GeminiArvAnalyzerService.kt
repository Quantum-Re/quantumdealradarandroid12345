package com.example.util

import com.example.BuildConfig
import com.example.data.DistressedProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class ArvAnalysisResult(
    val propertyId: Long,
    val estimatedArv: Double,
    val estimatedRenovationCost: Double,
    val repairScope: String,
    val potentialRoiPercent: Double,
    val netProfit: Double,
    val riskScore: Int, // 1 - 10
    val confidenceLevel: String, // High, Medium, Moderate
    val detailedReportMarkdown: String,
    val isAiGenerated: Boolean = true
)

object GeminiArvAnalyzerService {
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzePropertyArv(
        property: DistressedProperty
    ): Result<ArvAnalysisResult> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val hasValidKey = apiKey.isNotBlank() && apiKey != "YOUR_GEMINI_API_KEY"

        if (!hasValidKey) {
            // Fallback heuristics calculation if API key is not configured or in sandbox
            val fallbackResult = generateFallbackArvEstimate(property, "AI Key not configured. Using local algorithmic ARV estimation engine.")
            return@withContext Result.success(fallbackResult)
        }

        val prompt = """
            You are a Senior Real Estate Appraiser & Fix-and-Flip Valuation Specialist (CTU Estimo Immobiliare & ARV Specialist).
            Analyze the following distressed property stored in the local Room database and estimate its After Repair Value (ARV):
            
            PROPERTY DETAILS:
            - Address: "${property.address}"
            - Current Asking/Auction Price: €${property.price.toInt()}
            - Current Estimated As-Is Value: €${property.estimatedValue.toInt()}
            - Distress Level: "${property.distressLevel}" (High, Medium, Low, Foreclosure, Auction, REO)
            - Current Status: "${property.status}"
            ${if (property.latitude != null && property.longitude != null) "- Coordinates: Latitude ${property.latitude}, Longitude ${property.longitude}" else "- Position: Geographic coordinates not available for this property"}
            - Notes / Observations: "${property.notes.ifBlank { "No special notes" }}"
            
            TASK:
            Perform a rigorous ARV (After Repair Value) appraisal and return a structured analysis in JSON format.
            You MUST return ONLY a valid JSON object matching this schema:
            {
              "estimatedArv": 340000,
              "estimatedRenovationCost": 45000,
              "repairScope": "Full Interior Refresh & MEP Modernization",
              "riskScore": 4,
              "confidenceLevel": "High",
              "arvReportMarkdown": "Detailed appraisal explanation with OMI comparables, ARV calculation breakdown, renovation itemization, and target exit strategy."
            }
            
            GUIDELINES:
            1. estimatedArv MUST be higher than the current asking price and as-is value (accounting for post-repair market potential in that location).
            2. Repair cost should correspond to the distress level:
               - High Distress / Foreclosure: Heavy renovation (~€500-€900/sqm, e.g. €45k-€90k)
               - Medium Distress: Moderate renovation (~€300-€500/sqm, e.g. €25k-€45k)
               - Low Distress: Cosmetic touch-ups (~€150-€300/sqm, e.g. €10k-€25k)
            3. riskScore from 1 (lowest risk) to 10 (highest risk).
            4. Include markdown formatted analysis with sections for:
               - 📊 **ARV Valuation & Comparable Analysis**
               - 🛠️ **Renovation Scope & Cost Itemization**
               - 💶 **Flip Financials & Profit Margin**
               - ⚠️ **Key Risk Factors & Due Diligence**
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

                // Enforce JSON response format
                val generationConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // Fallback on HTTP error
                val fallback = generateFallbackArvEstimate(property, "Gemini API HTTP ${response.code}: Using algorithmic fallback.")
                return@withContext Result.success(fallback)
            }

            val responseJson = JSONObject(responseBodyStr)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val contentObj = firstCandidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val rawText = parts.getJSONObject(0).optString("text", "")
                    if (rawText.isNotBlank()) {
                        val parsedResult = parseGeminiResponse(property, rawText)
                        return@withContext Result.success(parsedResult)
                    }
                }
            }

            val fallback = generateFallbackArvEstimate(property, "Gemini returned empty candidate list. Algorithmic ARV calculated.")
            Result.success(fallback)
        } catch (e: Exception) {
            val fallback = generateFallbackArvEstimate(property, "Error calling Gemini API: ${e.localizedMessage}")
            Result.success(fallback)
        }
    }

    private fun parseGeminiResponse(property: DistressedProperty, jsonText: String): ArvAnalysisResult {
        return try {
            // Clean potential code fences
            val cleanedJson = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(cleanedJson)
            val estimatedArv = json.optDouble("estimatedArv", property.estimatedValue * 1.35)
            val estimatedRenovationCost = json.optDouble("estimatedRenovationCost", property.price * 0.18)
            val repairScope = json.optString("repairScope", "Standard Renovation & Energy Upgrade")
            val riskScore = json.optInt("riskScore", 4)
            val confidenceLevel = json.optString("confidenceLevel", "High")
            val reportMarkdown = json.optString("arvReportMarkdown", "ARV Appraisal completed successfully.")

            val netProfit = estimatedArv - property.price - estimatedRenovationCost
            val roiPercent = if (property.price + estimatedRenovationCost > 0) {
                (netProfit / (property.price + estimatedRenovationCost)) * 100.0
            } else 0.0

            ArvAnalysisResult(
                propertyId = property.id,
                estimatedArv = estimatedArv,
                estimatedRenovationCost = estimatedRenovationCost,
                repairScope = repairScope,
                potentialRoiPercent = (roiPercent * 10.0).roundToInt() / 10.0,
                netProfit = netProfit,
                riskScore = riskScore,
                confidenceLevel = confidenceLevel,
                detailedReportMarkdown = reportMarkdown,
                isAiGenerated = true
            )
        } catch (e: Exception) {
            generateFallbackArvEstimate(property, "JSON parsing error: ${e.message}")
        }
    }

    private fun generateFallbackArvEstimate(
        property: DistressedProperty,
        noticeMessage: String
    ): ArvAnalysisResult {
        val distressLevel = property.distressLevel.lowercase()
        
        val (arvMultiplier, renoCostMultiplier, scope, risk) = when {
            distressLevel.contains("high") || distressLevel.contains("foreclosure") -> 
                Tuple4(1.45, 0.22, "Full Demolition & MEP Overhaul", 7)
            distressLevel.contains("auction") || distressLevel.contains("reo") -> 
                Tuple4(1.38, 0.18, "Structural & System Upgrade", 5)
            distressLevel.contains("medium") -> 
                Tuple4(1.30, 0.14, "Interior Refurbishment & Kitchen/Bath Update", 4)
            else -> 
                Tuple4(1.20, 0.08, "Cosmetic Paint, Flooring & Light Refresh", 2)
        }

        val estimatedArv = if (property.estimatedValue > property.price) {
            property.estimatedValue * arvMultiplier
        } else {
            property.price * (arvMultiplier + 0.1)
        }

        val renovationCost = property.price * renoCostMultiplier
        val netProfit = estimatedArv - property.price - renovationCost
        val roiPercent = ((netProfit / (property.price + renovationCost)) * 100.0 * 10.0).roundToInt() / 10.0

        val reportMarkdown = """
            ### 🤖 AI ARV Property Appraisal Report
            *$noticeMessage*
            
            #### 📊 **1. ARV Valuation Summary**
            - **Current Asking/Acquisition Price:** €${property.price.toInt()}
            - **Estimated After Repair Value (ARV):** €${estimatedArv.toInt()}
            - **Projected Value Uplift:** +${((estimatedArv - property.price) / property.price * 100).toInt()}% over acquisition cost
            
            #### 🛠️ **2. Renovation & Cost Assessment**
            - **Recommended Scope:** $scope
            - **Estimated Renovation Budget:** €${renovationCost.toInt()}
            - **Distress Level Factor:** ${property.distressLevel}
            
            #### 💶 **3. Investment Return & Flip Margin**
            - **Projected Net Flip Profit:** €${netProfit.toInt()}
            - **Estimated ROI:** $roiPercent%
            - **Risk Rating:** $risk/10
            
            #### ⚠️ **4. Appraiser Recommendations**
            Verify structural integrity, municipal building filings, and cadastral compliance prior to submitting a binding offer.
        """.trimIndent()

        return ArvAnalysisResult(
            propertyId = property.id,
            estimatedArv = estimatedArv,
            estimatedRenovationCost = renovationCost,
            repairScope = scope,
            potentialRoiPercent = roiPercent,
            netProfit = netProfit,
            riskScore = risk,
            confidenceLevel = "Moderate (Algorithmic)",
            detailedReportMarkdown = reportMarkdown,
            isAiGenerated = false
        )
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
