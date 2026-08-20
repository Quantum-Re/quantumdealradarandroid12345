package com.example.util

import com.example.data.PipelineStatus
import org.junit.Assert.*
import org.junit.Test

class CsvPortfolioSyncServiceTest {

    @Test
    fun parseCsvToProperties_standardCommaSeparated_parsesSuccessfully() {
        val csv = """
            Titolo,Indirizzo,Prezzo,Superficie,Stato Pipeline,Ristrutturazione,Target Rivendita,Canone Affitto,Note
            Bilocale Porta Venezia,Via Tadino 24 Milano,185000,65,In Ristrutturazione,25000,270000,950,Ottima posizione centrale
            Attico Vista Mare,Corso Italia 12 Genova,240000,110,A Reddito,15000,320000,1400,Locato a trasfertisti
        """.trimIndent()

        val summary = CsvPortfolioSyncService.parseCsvToProperties(csv)

        assertEquals(2, summary.totalRowsParsed)
        assertEquals(2, summary.validProperties.size)
        assertEquals(0, summary.errors.size)

        val prop1 = summary.validProperties[0]
        assertEquals("Bilocale Porta Venezia", prop1.title)
        assertEquals("Via Tadino 24 Milano", prop1.address)
        assertEquals(185000.0, prop1.price, 0.01)
        assertEquals(65, prop1.surfaceSqm)
        assertEquals(PipelineStatus.RENOVATING.key, prop1.pipelineStatus)
        assertEquals(25000.0, prop1.estimatedRenovationCost, 0.01)
        assertEquals(270000.0, prop1.targetResalePrice, 0.01)
        assertEquals(950.0, prop1.projectedRentalIncome, 0.01)
        assertEquals("Ottima posizione centrale", prop1.notes)

        val prop2 = summary.validProperties[1]
        assertEquals(PipelineStatus.RENTED.key, prop2.pipelineStatus)
    }

    @Test
    fun parseCsvToProperties_semicolonSeparatedWithCurrencySymbols_parsesCorrectly() {
        val csv = """
            Title;Address;Price;Surface;Status;Renovation;ARV;Rent;Notes
            Trilocale Sempione;Via Cenisio 8 Milano;€ 220.000,00;85 mq;ANALYZED;€ 40.000,00;€ 340.000,00;€ 1.200,00;Da frazionare
        """.trimIndent()

        val summary = CsvPortfolioSyncService.parseCsvToProperties(csv)

        assertEquals(1, summary.validProperties.size)
        val prop = summary.validProperties[0]
        assertEquals("Trilocale Sempione", prop.title)
        assertEquals("Via Cenisio 8 Milano", prop.address)
        assertEquals(220000.0, prop.price, 0.01)
        assertEquals(85, prop.surfaceSqm)
        assertEquals(40000.0, prop.estimatedRenovationCost, 0.01)
        assertEquals(340000.0, prop.targetResalePrice, 0.01)
        assertEquals(1200.0, prop.projectedRentalIncome, 0.01)
        assertEquals(PipelineStatus.ANALYZED.key, prop.pipelineStatus)
    }

    @Test
    fun parseCsvToProperties_quotedValuesWithCommas_handlesCorrectly() {
        val csv = """
            "Titolo","Indirizzo","Prezzo","Superficie","Note"
            "Appartamento, Centro Storico","Via Roma, 45, Torino","130000","70","Immobile luminoso, ottimo investimento"
        """.trimIndent()

        val summary = CsvPortfolioSyncService.parseCsvToProperties(csv)

        assertEquals(1, summary.validProperties.size)
        val prop = summary.validProperties[0]
        assertEquals("Appartamento, Centro Storico", prop.title)
        assertEquals("Via Roma, 45, Torino", prop.address)
        assertEquals(130000.0, prop.price, 0.01)
        assertEquals(70, prop.surfaceSqm)
        assertEquals("Immobile luminoso, ottimo investimento", prop.notes)
    }

    @Test
    fun parseCsvToProperties_fallbackAddressToTitleWhenTitleMissing() {
        val csv = """
            Address,Price,Surface
            Via Nazionale 100 Roma,350000,95
        """.trimIndent()

        val summary = CsvPortfolioSyncService.parseCsvToProperties(csv)

        assertEquals(1, summary.validProperties.size)
        val prop = summary.validProperties[0]
        assertEquals("Via Nazionale 100 Roma", prop.title)
        assertEquals("Via Nazionale 100 Roma", prop.address)
        assertEquals(350000.0, prop.price, 0.01)
        assertEquals(95, prop.surfaceSqm)
    }

    @Test
    fun generateSampleCsvTemplate_producesValidCsv() {
        val template = CsvPortfolioSyncService.generateSampleCsvTemplate()
        assertNotNull(template)
        assertTrue(template.contains("Titolo"))
        assertTrue(template.contains("Indirizzo"))

        val summary = CsvPortfolioSyncService.parseCsvToProperties(template)
        assertTrue(summary.validProperties.isNotEmpty())
        assertEquals(5, summary.validProperties.size)
    }

    @Test
    fun parseCsvToProperties_setsUserEnteredProvenanceAndHandlesCoordinatesProperly() {
        val csv = """
            Titolo,Indirizzo,Prezzo
            Casa Milano,Via Torino 1 Milano,200000
            Casa Ignota,Contrada Sconosciuta 99,100000
        """.trimIndent()

        val summary = CsvPortfolioSyncService.parseCsvToProperties(csv)
        assertEquals(2, summary.validProperties.size)

        val milanoProp = summary.validProperties[0]
        assertEquals(com.example.data.DataProvenance.USER_ENTERED.name, milanoProp.provenance)
        assertNotNull(milanoProp.retrievedAt)
        assertEquals(45.4642, milanoProp.latitude!!, 0.0001) // Milano exact centroid without jitter
        assertEquals(9.1900, milanoProp.longitude!!, 0.0001)
        assertEquals("posizione approssimata al comune", milanoProp.evidenceRef)

        val unknownProp = summary.validProperties[1]
        assertEquals(com.example.data.DataProvenance.USER_ENTERED.name, unknownProp.provenance)
        assertNotNull(unknownProp.retrievedAt)
        assertEquals(0.0, unknownProp.latitude!!, 0.0)
        assertEquals(0.0, unknownProp.longitude!!, 0.0)
        assertNull(unknownProp.evidenceRef)
    }
}
