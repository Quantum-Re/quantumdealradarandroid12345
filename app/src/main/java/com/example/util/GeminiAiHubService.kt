package com.example.util

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.DistressedProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiAiHubService {
    private const val TAG = "GeminiAiHubService"

    // Model identifiers specified in user directives
    private const val MODEL_SEARCH_GROUNDING = "gemini-3.5-flash"
    private const val MODEL_MAPS_GROUNDING = "gemini-3.5-flash"
    private const val MODEL_VOICE_LIVE = "gemini-3.1-flash-live-preview"
    private const val MODEL_IMAGE_PRO = "gemini-3.1-pro-preview"
    private const val MODEL_FLASH_LITE = "gemini-3.1-flash-lite"
    private const val MODEL_VIDEO_PRO = "gemini-3.1-pro-preview"
    private const val MODEL_THINKING_HIGH = "gemini-3.1-pro-preview"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNull_or_blank() || key == "YOUR_GEMINI_API_KEY") "" else key
        } catch (e: Exception) {
            ""
        }
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

    // 1. Google Search Grounded Research (gemini-3.5-flash with googleSearch tool)
    suspend fun performSearchGroundedResearch(
        query: String,
        location: String = "Milan, Italy"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.success(
                "🔍 **[Market & Legal Research]**: \"$query\"\n\n" +
                "• **Status**: Grounded Google Search research unavailable (API Key missing).\n" +
                "• **Action**: Verify local auction calendars and OMI benchmarks manually via official portals."
            )
        }

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Perform live real estate research for property in $location: $query")
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

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_SEARCH_GROUNDING:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            val parsedText = parseGeminiTextResponse(resStr)
            Result.success(parsedText)
        } catch (e: Exception) {
            Log.e(TAG, "Search grounding error: ${e.message}")
            Result.failure(e)
        }
    }

    // 2. Google Maps Grounded Location Analysis (gemini-3.5-flash with googleMaps tool)
    suspend fun performMapsGroundedInspection(
        address: String,
        lat: Double,
        lng: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.success(
                "🗺️ **[Location Analysis]**: \"$address\"\n\n" +
                "• **Status**: Live Maps-grounded neighborhood inspection unavailable.\n" +
                "• **Note**: Please consult Google Maps directly to verify transit access and local amenities."
            )
        }

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Analyze location amenities, transit access, and neighborhood quality for property at address $address (lat: $lat, lng: $lng).")
                            })
                        })
                    })
                })
                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleMaps", JSONObject())
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_MAPS_GROUNDING:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            val parsedText = parseGeminiTextResponse(resStr)
            Result.success(parsedText)
        } catch (e: Exception) {
            Log.e(TAG, "Maps grounding error: ${e.message}")
            Result.failure(e)
        }
    }

    // 3. Low-Latency Sub-Second Quick Screener (gemini-3.1-flash-lite)
    suspend fun performLowLatencyQuickScreen(
        property: DistressedProperty
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.success(
                "⚡ **[Quick Screener]**\n" +
                "• **Status**: AI-driven triage unavailable (API Key missing).\n" +
                "• **Manual Check Required**: Inspect court dossier and legal status independently."
            )
        }

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Provide an instant 3-bullet deal triage assessment for distressed property at ${property.address}, price €${property.price.toInt()}, estimated as-is €${property.estimatedValue.toInt()}. Keep response concise under 60 words.")
                            })
                        })
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_FLASH_LITE:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            val parsedText = parseGeminiTextResponse(resStr)
            Result.success(parsedText)
        } catch (e: Exception) {
            Log.e(TAG, "Flash-lite error: ${e.message}")
            Result.failure(e)
        }
    }

    // 4. Photo Image Understanding (gemini-3.1-pro-preview)
    suspend fun analyzePropertyPhoto(
        bitmap: Bitmap,
        userPrompt: String = "Analyze this property photo for physical distress, structural defects, and estimated renovation scope."
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        // Convert bitmap to Base64 JPEG
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        if (apiKey.isEmpty()) {
            return@withContext Result.success(
                "📸 **[Photo Inspection]**\n\n" +
                "• **Status**: Multimodal visual analysis unavailable.\n" +
                "• **Action**: Manual site visit recommended to assess physical condition and renovation needs."
            )
        }

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userPrompt)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", imageBase64)
                                })
                            })
                        })
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_IMAGE_PRO:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            val parsedText = parseGeminiTextResponse(resStr)
            Result.success(parsedText)
        } catch (e: Exception) {
            Log.e(TAG, "Image pro error: ${e.message}")
            Result.failure(e)
        }
    }

    // 5. Video Content Walkthrough Analysis (gemini-3.1-pro-preview)
    suspend fun analyzeWalkthroughVideo(
        videoNotes: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.success(
                "🎥 **[Video Walkthrough Analysis]**\n\n" +
                "• **Status**: Automated structural video assessment unavailable.\n" +
                "• **Action**: Review footage manually to itemize renovation scope and potential defects."
            )
        }

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Perform a comprehensive room-by-room video walkthrough inspection and structural assessment based on video footage details: $videoNotes")
                            })
                        })
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_VIDEO_PRO:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            val parsedText = parseGeminiTextResponse(resStr)
            Result.success(parsedText)
        } catch (e: Exception) {
            Log.e(TAG, "Video pro error: ${e.message}")
            Result.failure(e)
        }
    }

    // 6. High Thinking Mode (gemini-3.1-pro-preview with thinkingLevel HIGH, no maxOutputTokens)
    suspend fun performHighThinkingAnalysis(
        property: DistressedProperty,
        complexQuery: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.success(
                "🧠 **[Financial Analysis]**\n\n" +
                "• **Status**: Deep reasoning multi-scenario analysis unavailable.\n" +
                "• **Action**: Use 'Financial Engine' for manual scenario modelling."
            )
        }

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Perform a high-level deep financial thinking analysis for property at ${property.address} (price €${property.price.toInt()}, distress ${property.distressLevel}). Query details: $complexQuery")
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "HIGH")
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_THINKING_HIGH:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            val parsedText = parseGeminiTextResponse(resStr)
            Result.success(parsedText)
        } catch (e: Exception) {
            Log.e(TAG, "Thinking high error: ${e.message}")
            Result.failure(e)
        }
    }

    // 7. Live Voice Conversation Assistant (gemini-3.1-flash-live-preview)
    suspend fun sendVoiceAssistantTurn(
        userVoiceText: String,
        propertyContext: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.success(
                "🎙️ **[Voice Assistant]**\n" +
                "• **Status**: Live conversational voice AI unavailable (API Key missing).\n" +
                "• **Action**: Consult your own due diligence checklist and a legal advisor for bidding strategy on $propertyContext."
            )
        }

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "You are a live conversational voice AI real estate advisor. Respond naturally to the user's voice message: '$userVoiceText' in context of property: $propertyContext")
                            })
                        })
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_VOICE_LIVE:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            val parsedText = parseGeminiTextResponse(resStr)
            Result.success(parsedText)
        } catch (e: Exception) {
            Log.e(TAG, "Voice live error: ${e.message}")
            Result.failure(e)
        }
    }

    // 8. First-Principles & Hardcore Engineering Audit (Physics, Material Cost, Energy Autonomy & Optimus Telemetry)
    suspend fun performFirstPrinciplesCyberAudit(
        address: String,
        price: Double,
        surfaceSqm: Int,
        propertyType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.success(
                "⚡ **[FIRST-PRINCIPLES CYBER-RADAR]**\n\n" +
                "• **1. Physical Cost Analysis**: UNAVAILABLE\n" +
                "• **2. Energy Autonomy Potential**: UNAVAILABLE\n" +
                "• **3. Anti-Fragility Score**: UNVERIFIED_ESTIMATE\n" +
                "• **Status**: Deep engineering audit requires Gemini API connection."
            )
        }

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Perform a First-Principles Real Estate & Engineering Deep Audit in Elon Musk / Tesla / SpaceX style for property at '$address' ($propertyType, ${surfaceSqm} sqm, €${price.toInt()}). Deconstruct into: 1. Atomic Raw Material & Physical Construction cost vs asking price, 2. Energy Autonomy / Solar kWh potential & Net Zero ROI, 3. Hardcore Stress Test resistance (+500bps rate shock, -30% market liquidity), 4. Optimus / Automation Retrofit Potential.")
                            })
                        })
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_FLASH_LITE:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            val parsedText = parseGeminiTextResponse(resStr)
            Result.success(parsedText)
        } catch (e: Exception) {
            Log.e(TAG, "First principles cyber audit error: ${e.message}")
            Result.failure(e)
        }
    }

    // 9. Sam Zell "The Grave Dancer" Contrarian Audit (Replacement Cost, Supply Moats, Downside Protection & Debt Traps)
    suspend fun performGraveDancerContrarianAudit(
        address: String,
        price: Double,
        surfaceSqm: Int,
        propertyType: String,
        distressLevel: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        if (apiKey.isEmpty()) {
            return@withContext Result.success(
                "💀 **[THE GRAVE DANCER'S UNDERWRITING MEMO]**\n\n" +
                "• **1. Replacement Cost Arbitrage**: UNVERIFIED_ESTIMATE\n" +
                "  - Basis vs. Replacement Cost analysis requires verified submarket data.\n\n" +
                "• **2. Supply & Demand Barrier**: UNAVAILABLE\n\n" +
                "• **3. Downside-First Debt Analysis**: UNAVAILABLE\n\n" +
                "• **4. Contrarian Verdict**: PENDING DATA GROUNDING\n" +
                "  - *Status*: Contrarian audit suspended due to lack of live market grounding."
            )
        }

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Perform a ruthless, contrarian real estate investment audit in the authentic voice and investment framework of legendary investor Sam Zell ('The Grave Dancer') for property at '$address' ($propertyType, ${surfaceSqm} sqm, asking €${price.toInt()}, distress: '$distressLevel'). Deconstruct into: 1. Discount to Replacement Cost (comparing price/sqm vs new build cost), 2. Supply Moats and zoning barriers, 3. Downside-first risk analysis (breakeven occupancy, debt structure and refinancing survival in a 5-year freeze), 4. Actionable Contrarian Verdict with a classic Sam Zell quote.")
                            })
                        })
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_FLASH_LITE:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            val parsedText = parseGeminiTextResponse(resStr)
            Result.success(parsedText)
        } catch (e: Exception) {
            Log.e(TAG, "Grave dancer audit error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseGeminiTextResponse(rawJson: String): String {
        return try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val cand = candidates.getJSONObject(0)
                val content = cand.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val p = parts.getJSONObject(i)
                        val text = p.optString("text", "")
                        if (text.isNotEmpty()) {
                            sb.append(text)
                        }
                    }
                    if (sb.isNotEmpty()) return sb.toString()
                }
            }
            "Analysis complete."
        } catch (e: Exception) {
            "Response received."
        }
    }
}
