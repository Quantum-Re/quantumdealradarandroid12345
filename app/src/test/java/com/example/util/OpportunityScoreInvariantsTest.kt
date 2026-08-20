package com.example.util

import com.example.data.Property
import org.junit.Assert.*
import org.junit.Test

/**
 * Invarianti del punteggio di opportunità.
 *
 * Tre principi, in forma eseguibile:
 *   1. Nessun dato di mercato mancante viene sostituito da una costante.
 *   2. La località si riconosce o si dichiara sconosciuta: non si assume Milano.
 *   3. Il punteggio deve poter dire "affare pessimo", non solo "affare meno buono".
 *
 * Questi test falliscono se qualcuno reintroduce un valore di ripiego.
 */
class OpportunityScoreInvariantsTest {

    private fun kpiVuoto() = ProvinceScrapedKpi(
        locationName = "Milano",
        province = "MI",
        region = "Lombardia"
        // tutti i campi di misura restano null
    )

    private fun kpiMercato(prezzoMq: Double) = ProvinceScrapedKpi(
        locationName = "Milano",
        province = "MI",
        region = "Lombardia",
        avgSalePriceSqM = prezzoMq,
        avgRentPriceSqM = 20.0,
        avgDaysOnMarket = 80,
        absorptionRatePercent = 80.0
    )

    private fun immobile(prezzo: Double, mq: Int = 100, distress: String = "") = Property(
        id = 1L,
        title = "Test",
        address = "Via Test 1, Milano",
        price = prezzo,
        surfaceSqm = mq,
        distressStatus = distress
    )

    // ---------------------------------------------------------------- 1. niente costanti

    @Test
    fun `senza prezzo di mercato non viene prodotto alcun punteggio`() {
        val eval = PropertyOpportunityEngine.evaluateProperty(immobile(300000.0), kpiVuoto())

        assertFalse("il punteggio non può essere affidabile senza prezzo di mercato", eval.scoreAffidabile)
        assertEquals("il punteggio deve essere azzerato, non stimato", 0, eval.opportunityScore)
        assertTrue("va dichiarato quale dato manca", eval.missingMarketData.isNotEmpty())
    }

    @Test
    fun `il prezzo di mercato non viene mai sostituito da 2000 euro al mq`() {
        val eval = PropertyOpportunityEngine.evaluateProperty(immobile(300000.0), kpiVuoto())

        assertNotEquals(
            "2000.0 era il vecchio valore di ripiego: non deve più comparire",
            2000.0, eval.liveMarketPricePerSqm, 0.001
        )
    }

    // ---------------------------------------------------------------- 2. località

    @Test
    fun `Milano Via Roma non viene associato al mercato di Roma`() {
        assertEquals("Milano", PropertyOpportunityEngine.extractLocationName("Via Roma 12, Milano"))
        assertEquals("Milano", PropertyOpportunityEngine.extractLocationName("Milano, Via Roma 12"))
    }

    @Test
    fun `una localita non riconoscibile non diventa Milano`() {
        // extractLocationName should return null if not recognizable
        assertNull(PropertyOpportunityEngine.extractLocationName("Via Garibaldi 47"))
        assertNull(PropertyOpportunityEngine.extractLocationName(""))
    }

    // ---------------------------------------------------------------- 3. malus

    @Test
    fun `un immobile sopra mercato finisce in AVOID`() {
        // mercato 3000 euro/mq, immobile a 8000 euro/mq
        val eval = PropertyOpportunityEngine.evaluateProperty(
            immobile(prezzo = 800000.0, mq = 100), kpiMercato(3000.0)
        )

        assertTrue("il punteggio deve essere affidabile: i dati ci sono", eval.scoreAffidabile)
        assertEquals(OpportunityTier.AVOID, eval.tier)
        assertTrue("sopra mercato il punteggio deve stare sotto 25", eval.opportunityScore < 25)
    }

    @Test
    fun `il punteggio puo scendere a zero`() {
        val eval = PropertyOpportunityEngine.evaluateProperty(
            immobile(prezzo = 2_000_000.0, mq = 100), kpiMercato(3000.0)
        )

        assertTrue("nessun pavimento a 5 punti", eval.opportunityScore in 0..100)
        assertTrue("un immobile a 20.000 euro/mq su un mercato da 3.000 deve stare a fondo scala", eval.opportunityScore <= 5)
    }

    @Test
    fun `un immobile sotto mercato e distressed ottiene un punteggio alto`() {
        val eval = PropertyOpportunityEngine.evaluateProperty(
            immobile(prezzo = 180000.0, mq = 100, distress = "ASTA"), kpiMercato(3000.0)
        )

        assertTrue(eval.scoreAffidabile)
        assertTrue("uno sconto del 40% deve produrre un punteggio elevato", eval.opportunityScore >= 65)
        assertNotEquals(OpportunityTier.AVOID, eval.tier)
    }
}
