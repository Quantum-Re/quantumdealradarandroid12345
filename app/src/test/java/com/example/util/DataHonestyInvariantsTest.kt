package com.example.util

import com.example.data.MarketInsightTopic
import com.example.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataHonestyInvariantsTest {

    private fun isApiKeyMissing(): Boolean {
        val key = try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        return key.isBlank() || key == "YOUR_GEMINI_API_KEY"
    }

    @Test
    fun `test GeminiMarketInsightsService Baseline Dates Are Honest`() {
        val report = GeminiMarketInsightsService.getCuratedBaselineReport(MarketInsightTopic.ALL)
        
        for (item in report.headlineItems) {
            val date = item.publishedDateStr
            assertFalse("Date should not contain 'Oggi' in baseline", date.contains("Oggi", ignoreCase = true))
            assertFalse("Date should not contain 'Ieri' in baseline", date.contains("Ieri", ignoreCase = true))
            assertFalse("Date should not contain 'ore fa' in baseline", date.contains("ore fa", ignoreCase = true))
            assertEquals("ARCHIVE_BASELINE", date)
        }
        assertFalse("Baseline report must have isGroundedWithGoogleSearch = false", report.isGroundedWithGoogleSearch)
    }

    @Test
    fun `test GeminiAiHubService Fallbacks Are Honest`() = runBlocking {
        if (!isApiKeyMissing()) {
            return@runBlocking
        }
        
        // Tests fallbacks when API key is missing
        val locationResult = GeminiAiHubService.performSearchGroundedResearch("test query", "Milano")
        val locationText = locationResult.getOrThrow()
        assertTrue("Text was: $locationText", locationText.contains("Grounded Google Search research unavailable"))

        val mapsResult = GeminiAiHubService.performMapsGroundedInspection("Via Roma 1", 45.0, 9.0)
        val mapsText = mapsResult.getOrThrow()
        assertTrue(mapsText.contains("Live Maps-grounded neighborhood inspection unavailable"))

        val firstPrinciplesResult = GeminiAiHubService.performFirstPrinciplesCyberAudit("Via Roma 1", 200000.0, 100, "Appartamento")
        val fpText = firstPrinciplesResult.getOrThrow()
        assertTrue(fpText.contains("UNAVAILABLE"))

        val graveDancerResult = GeminiAiHubService.performGraveDancerContrarianAudit("Via Roma 1", 200000.0, 100, "Appartamento", "High")
        val gdText = graveDancerResult.getOrThrow()
        assertTrue(gdText.contains("UNAVAILABLE"))
        assertTrue(gdText.contains("PENDING DATA GROUNDING"))
    }

    @Test
    fun `il fallback algoritmico ARV non si dichiara mai una perizia AI verificata`() = runBlocking {
        if (!isApiKeyMissing()) {
            return@runBlocking
        }

        val property = com.example.data.DistressedProperty(
            id = 1L,
            address = "Via Test 1, Milano",
            price = 150000.0,
            estimatedValue = 180000.0,
            distressLevel = "Medium",
            status = "Attivo",
            latitude = null,
            longitude = null,
            notes = ""
        )

        val result = GeminiArvAnalyzerService.analyzePropertyArv(property)
        val arv = result.getOrThrow()

        assertFalse(
            "il fallback algoritmico non deve dichiararsi AI-generated",
            arv.isAiGenerated
        )
        assertFalse(
            "il fallback non deve dichiararsi una confidenza 'High' come se fosse una perizia AI verificata",
            arv.confidenceLevel.equals("High", ignoreCase = true)
        )
        assertTrue(
            "il report deve dichiarare esplicitamente di essere una stima preliminare algoritmica",
            arv.detailedReportMarkdown.contains("Preliminary Scenario Estimate", ignoreCase = true)
        )
    }

    @Test
    fun `un fallback curato non puo dichiararsi grounded anche se la ricerca ha prodotto grounding metadata`() {
        // La ricerca Google Search ha davvero prodotto una fonte grounded, ma il testo di
        // risposta non contiene alcun blocco TITOLO/FONTE strutturato: il parser non estrae
        // nessuna notizia reale, quindi il report deve ricadere sulle notizie curated e NON
        // può dichiararsi isGroundedWithGoogleSearch = true, anche se groundingSources non è vuoto.
        val rawJsonWithGroundingButNoParsableHeadlines = """
            {
              "candidates": [
                {
                  "content": { "parts": [ { "text": "Nessun dato strutturato disponibile in questa risposta." } ] },
                  "groundingMetadata": {
                    "webSearchQueries": ["mercato immobiliare italia"],
                    "groundingChunks": [
                      { "web": { "uri": "https://example.com/source", "title": "Fonte Esempio" } }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val report = GeminiMarketInsightsService.parseGroundedResponse(
            rawJsonWithGroundingButNoParsableHeadlines,
            MarketInsightTopic.ALL,
            "query di test"
        )

        assertTrue("il grounding metadata era presente", report.groundingSources.isNotEmpty())
        assertFalse(
            "un report che mostra notizie curated di riserva non può dichiararsi grounded",
            report.isGroundedWithGoogleSearch
        )
        assertTrue(
            "le headline devono essere quelle curated, non una lista vuota",
            report.headlineItems.isNotEmpty()
        )
    }

    @Test
    fun `nessuna keyword nel testo puo generare un indicatore macro hardcoded`() {
        val textWithTriggerKeywords = "La BCE ha tagliato i tassi. Rendimento in crescita. Le Aste giudiziarie aumentano."
        val indicators = GeminiMarketInsightsService.parseMacroIndicators(textWithTriggerKeywords)
        assertTrue(
            "senza estrazione strutturata reale, nessun indicatore macro deve essere inventato dalla presenza di parole chiave",
            indicators.isEmpty()
        )
    }
}
