package com.example

import com.example.data.Property
import com.example.data.PropertyDeal
import com.example.util.MarketEstimateService
import com.example.util.PredictiveDealAlertEngine
import com.example.util.ProvinceScrapedKpi
import org.junit.Assert.*
import org.junit.Test

/**
 * Invarianti del ranking predittivo.
 *
 * Contratto attuale: percentili, P90/P95 e i badge "Top 5% / Top 10%" esistono
 * SOLO se il KPI porta uno storico verificato con almeno 12 osservazioni.
 * I valori interni di ripiego non hanno storico, quindi il ranking non deve
 * essere prodotto: non deve essere stimato, degradato o sostituito.
 *
 * Questi test falliscono se qualcuno reintroduce una serie storica sintetica.
 */
class PredictiveDealAlertEngineTest {

    @Test
    fun `senza storico verificato le statistiche di provincia non sono disponibili`() {
        val kpi = MarketEstimateService.getCuratedProvinceKpi("Milano")

        assertFalse(
            "un valore interno di ripiego non può dichiarare uno storico verificato",
            kpi.historicalDatasetVerified
        )

        val stats = PredictiveDealAlertEngine.computeProvinceHistoricalYieldStats(kpi)

        assertEquals("Milano", stats.locationName)
        assertFalse("isAvailable deve essere false senza storico verificato", stats.isAvailable)

        assertNull("nessuna media senza campione", stats.meanHistoricalYield)
        assertNull("nessuna deviazione standard senza campione", stats.stdDevHistoricalYield)
        assertNull("nessun P10 senza campione", stats.p10HistoricalYield)
        assertNull("nessun P50 senza campione", stats.p50HistoricalYield)
        assertNull("nessun P90 senza campione", stats.p90HistoricalYield)
        assertNull("nessun P95 senza campione", stats.p95HistoricalYield)
    }

    @Test
    fun `un immobile fortemente scontato non riceve un percentile inventato`() {
        val kpi = MarketEstimateService.getCuratedProvinceKpi("Milano")

        val discountedProperty = Property(
            id = 101L,
            title = "Appartamento Sfitto da Ristrutturare",
            address = "Via Padova 50, Milano",
            price = 180000.0,
            surfaceSqm = 80,
            estimatedRenovationCost = 25000.0,
            targetResalePrice = 380000.0
        )

        val evaluation = PredictiveDealAlertEngine.evaluateProperty(discountedProperty, kpi)

        assertFalse("il ranking non è disponibile senza storico", evaluation.isRankingAvailable)
        assertNotNull("deve essere dichiarato il motivo", evaluation.unavailabilityReason)
        assertNull("nessun percentile", evaluation.dealPercentile)
        assertFalse("nessun badge Top 10%", evaluation.isTop10Percentile)
        assertNull("nessuna etichetta di rango", evaluation.topTierRankLabel)
        assertNull("nessuno spread rispetto a un P90 inesistente", evaluation.yieldSpreadVsP90)
        assertNull("nessun prezzo obiettivo per un percentile inesistente", evaluation.targetPriceForTop10Percentile)
    }

    @Test
    fun `un immobile sopra mercato non riceve comunque un percentile`() {
        val kpi = MarketEstimateService.getCuratedProvinceKpi("Milano")

        val overpricedProperty = Property(
            id = 102L,
            title = "Attico Extra Lusso Sovrapprezzato",
            address = "Corso Como 1, Milano",
            price = 650000.0,
            surfaceSqm = 80,
            estimatedRenovationCost = 0.0,
            targetResalePrice = 600000.0
        )

        val evaluation = PredictiveDealAlertEngine.evaluateProperty(overpricedProperty, kpi)

        assertFalse(evaluation.isRankingAvailable)
        assertNull(evaluation.dealPercentile)
        assertFalse(evaluation.isTop10Percentile)
    }

    @Test
    fun `i dati oggettivi del deal restano disponibili anche senza ranking`() {
        val deal = PropertyDeal(
            id = 201L,
            title = "Asta Giudiziaria Trilocale Monza",
            location = "Monza",
            propertyType = "Residenziale",
            askingPrice = 95000.0,
            estimatedMarketValue = 180000.0,
            surfaceSqm = 90,
            isBookmarked = false,
            estimatedCapRate = 8.5
        )

        val evaluation = PredictiveDealAlertEngine.evaluatePropertyDeal(deal)

        // Il prezzo e la superficie vengono dal deal: sono dati veri, restano.
        assertEquals(95000.0, evaluation.currentListPrice, 0.01)
        assertEquals(90, evaluation.surfaceSqm)

        // Il ranking invece no.
        assertFalse(evaluation.isRankingAvailable)
        assertNull(evaluation.dealPercentile)
    }

    @Test
    fun `senza trend di vendita e affitto verificati la proiezione 12 e 24 mesi non viene prodotta`() {
        val kpiSenzaTrend = ProvinceScrapedKpi(
            locationName = "Milano",
            province = "MI",
            region = "Lombardia",
            avgSalePriceSqM = 3000.0,
            avgRentPriceSqM = 15.0,
            saleTrendYoY = null,
            rentTrendYoY = null
        )

        val property = Property(
            id = 301L,
            title = "Bilocale Centro",
            address = "Via Torino 10, Milano",
            price = 200000.0,
            surfaceSqm = 60,
            estimatedRenovationCost = 0.0,
            targetResalePrice = 250000.0
        )

        val evaluation = PredictiveDealAlertEngine.evaluateProperty(property, kpiSenzaTrend)

        assertNull(
            "nessun valore inventato ?: 2.0 per il trend di vendita mancante",
            evaluation.predicted12mMarketValue
        )
        assertNull(
            "nessun valore inventato per la proiezione a 24 mesi senza trend verificati",
            evaluation.predicted24mMarketValue
        )
        assertNull(
            "nessuna equity gain calcolata su una proiezione inesistente",
            evaluation.predicted12mEquityGain
        )
        assertNull(
            "nessun valore inventato ?: 3.0 per il trend di affitto mancante",
            evaluation.predicted12mYield
        )
    }

    @Test
    fun `con trend di vendita e affitto verificati la proiezione 12 e 24 mesi viene prodotta`() {
        val kpiConTrend = ProvinceScrapedKpi(
            locationName = "Milano",
            province = "MI",
            region = "Lombardia",
            avgSalePriceSqM = 3000.0,
            avgRentPriceSqM = 15.0,
            saleTrendYoY = 4.0,
            rentTrendYoY = 5.0
        )

        val property = Property(
            id = 302L,
            title = "Bilocale Centro",
            address = "Via Torino 10, Milano",
            price = 200000.0,
            surfaceSqm = 60,
            estimatedRenovationCost = 0.0,
            targetResalePrice = 250000.0
        )

        val evaluation = PredictiveDealAlertEngine.evaluateProperty(property, kpiConTrend)

        assertNotNull(
            "con trend verificati la proiezione a 12 mesi deve essere prodotta",
            evaluation.predicted12mMarketValue
        )
        assertNotNull(
            "con trend verificati la proiezione a 24 mesi deve essere prodotta",
            evaluation.predicted24mMarketValue
        )
        assertNotNull(evaluation.predicted12mEquityGain)
        assertNotNull(evaluation.predicted12mYield)

        // 60 mq * 3000 EUR/mq * 1.04 = 187200.0
        assertEquals(187200.0, evaluation.predicted12mMarketValue!!, 0.01)
    }
}
