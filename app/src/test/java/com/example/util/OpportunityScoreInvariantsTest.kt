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
        absorptionRatePercent = 80.0,
        marketSaturationScore = 40
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

    // ------------------------------------------------ tutti e sei i campi di misura

    /**
     * Il difetto originale è stato corretto un campo alla volta, e ogni volta è
     * rientrato da un altro campo. Questo test copre tutti e sei insieme: se
     * qualcuno reintroduce una costante di ripiego su uno qualsiasi, fallisce.
     */
    @Test
    fun `nessuno dei sei campi di misura viene sostituito da una costante`() {
        val eval = PropertyOpportunityEngine.evaluateProperty(
            immobile(300000.0),
            ProvinceScrapedKpi(
                locationName = "Milano",
                province = "MI",
                region = "Lombardia",
                avgSalePriceSqM = 4000.0
                // gli altri cinque restano null
            )
        )

        assertNull("giorni sul mercato non deve diventare 90", eval.daysOnMarket)
        assertNull("saturazione non deve diventare 50", eval.marketSaturationScore)
        assertNull("assorbimento non deve diventare 60.0", eval.absorptionRatePercent)
        assertNull("il rendimento lordo non si calcola senza canone", eval.grossRentalYieldPotential)

        assertFalse("con quattro dati mancanti il punteggio non è affidabile", eval.scoreAffidabile)
        assertEquals(
            "devono essere dichiarati tutti e quattro i dati mancanti",
            4, eval.missingMarketData.size
        )
    }

    @Test
    fun `un immobile non distressed non riceve punti per non esserlo`() {
        val conDistress = PropertyOpportunityEngine.evaluateProperty(
            immobile(200000.0, distress = "ASTA"), kpiMercato(4000.0)
        )
        val senzaDistress = PropertyOpportunityEngine.evaluateProperty(
            immobile(200000.0, distress = ""), kpiMercato(4000.0)
        )

        assertTrue(
            "il punteggio con distress deve superare quello senza di almeno 10 punti: " +
                "se la differenza è piccola, il ramo 'else' sta ancora regalando punti",
            conDistress.opportunityScore - senzaDistress.opportunityScore >= 10
        )
    }

    /**
     * La rinormalizzazione deve essere neutra: togliere un dato non deve
     * premiare né penalizzare, deve solo ridurre il numero di elementi su cui
     * il giudizio si basa.
     */
    @Test
    fun `togliere un dato non fa crollare il punteggio`() {
        val completo = PropertyOpportunityEngine.evaluateProperty(
            immobile(200000.0, distress = "ASTA"), kpiMercato(4000.0)
        )
        val senzaVelocita = PropertyOpportunityEngine.evaluateProperty(
            immobile(200000.0, distress = "ASTA"),
            kpiMercato(4000.0).copy(avgDaysOnMarket = null, absorptionRatePercent = null)
        )

        val scarto = kotlin.math.abs(completo.opportunityScore - senzaVelocita.opportunityScore)
        assertTrue(
            "scarto di $scarto punti: la rinormalizzazione non è neutra",
            scarto <= 12
        )
        assertFalse(
            "va comunque dichiarato che il punteggio è parziale",
            senzaVelocita.scoreAffidabile
        )
    }
}
