package com.example

import com.example.data.Property
import com.example.data.PropertyDeal
import com.example.util.MarketEstimateService
import com.example.util.PredictiveDealAlertEngine
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
}
