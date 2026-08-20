package com.example.data

import java.util.UUID

enum class MarketSentiment(
    val key: String,
    val labelIt: String,
    val labelEn: String,
    val colorHex: Long
) {
    BULLISH("BULLISH", "Espansione / Positivo", "Bullish", 0xFF2E7D32),
    NEUTRAL("NEUTRAL", "Stabile / Neutro", "Neutral", 0xFF0288D1),
    BEARISH("BEARISH", "Cauto / Ribassista", "Bearish", 0xFFE65100),
    REGULATORY_ALERT("REGULATORY", "Allerta Normativa", "Regulatory Alert", 0xFF6A1B9A);

    companion object {
        fun fromKey(key: String): MarketSentiment {
            return values().find { it.key.equals(key, ignoreCase = true) } ?: NEUTRAL
        }
    }
}

enum class MarketInsightTopic(
    val key: String,
    val titleIt: String,
    val titleEn: String,
    val searchPrompt: String,
    val iconName: String
) {
    ALL(
        "ALL",
        "Tutte le Notizie",
        "All Headlines",
        "ultime notizie mercato immobiliare italia 2026 compravendite prezzi tassi",
        "Newspaper"
    ),
    MORTGAGE_RATES(
        "MORTGAGE_RATES",
        "Tassi Mutui & BCE",
        "Mortgage Rates & ECB",
        "tassi mutui bce euribor eurirs spread banche erogazioni credito italia 2026",
        "TrendingUp"
    ),
    AUCTIONS_NPL(
        "AUCTIONS_NPL",
        "Aste & Crediti NPL",
        "Auctions & NPL",
        "mercato aste giudiziarie immobiliari crediti deteriorati npl ctu tribunali italia 2026",
        "Gavel"
    ),
    HOTSPOTS_CITIES(
        "HOTSPOTS_CITIES",
        "Focus Città & OMI",
        "City Hotspots & OMI",
        "prezzi case milano roma torino bologna firenze napoli osservatorio omi quotazioni 2026",
        "LocationCity"
    ),
    REGULATION_TAX(
        "REGULATION_TAX",
        "Normative & Case Green",
        "Regulation & Green Homes",
        "direttiva europea case green bonus casa ristrutturazioni conformita catastale tasse immobiliari 2026",
        "Eco"
    ),
    YIELDS_INVESTING(
        "YIELDS_INVESTING",
        "Rendimenti & Strategie",
        "Yields & Strategies",
        "rendimenti locazione affitti brevi transitori trading immobiliare frazionamenti flussi di cassa 2026",
        "Paid"
    );

    companion object {
        fun fromKey(key: String): MarketInsightTopic {
            return values().find { it.key.equals(key, ignoreCase = true) } ?: ALL
        }
    }
}

data class GroundingWebSource(
    val title: String,
    val uri: String,
    val domain: String = ""
)

data class MacroEconomicIndicator(
    val label: String,
    val value: String,
    val trendDelta: String,
    val isPositive: Boolean,
    val description: String
)

data class MarketInsightItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sourcePublication: String,
    val publishedDateStr: String,
    val topic: MarketInsightTopic,
    val summary: String,
    val keyTakeaways: List<String> = emptyList(),
    val sentiment: MarketSentiment = MarketSentiment.NEUTRAL,
    val impactRating: Int = 7, // 1 to 10
    val regionTag: String = "Italia",
    val primaryUrl: String = "",
    val webDomain: String = "",
    val isBookmarked: Boolean = false
)

data class MarketInsightsReport(
    val query: String = "",
    val activeTopic: MarketInsightTopic = MarketInsightTopic.ALL,
    val headlineItems: List<MarketInsightItem> = emptyList(),
    val macroIndicators: List<MacroEconomicIndicator> = emptyList(),
    val groundingSources: List<GroundingWebSource> = emptyList(),
    val searchQueriesExecuted: List<String> = emptyList(),
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val isGroundedWithGoogleSearch: Boolean = false,
    val errorMessage: String? = null
)
