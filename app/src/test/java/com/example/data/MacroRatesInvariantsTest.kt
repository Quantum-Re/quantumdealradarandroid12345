package com.example.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MacroRatesInvariantsTest {

    @Test
    fun `7_i tassi non live non sono attribuiti alla BCE`() = runBlocking {
        // Verifica baseline interno
        val baseline = MacroFinancialApiService.getInternalRateBaseline()
        assertFalse("isLiveFetched deve essere false", baseline.isLiveFetched)
        assertFalse("sourceProvider non deve contenere 'BCE'", baseline.sourceProvider.contains("BCE", ignoreCase = true))
        assertFalse("sourceProvider non deve contenere 'Eurostat'", baseline.sourceProvider.contains("Eurostat", ignoreCase = true))
        assertFalse("sourceProvider non deve contenere 'Live'", baseline.sourceProvider.contains("Live", ignoreCase = true))

        // Verifica chiamata API in fallback
        val macroFromService = MacroFinancialApiService.fetchLatestMacroEconomicData()
        assertFalse("isLiveFetched deve essere false per fallback", macroFromService.isLiveFetched)
        assertFalse("sourceProvider non deve contenere 'BCE'", macroFromService.sourceProvider.contains("BCE", ignoreCase = true))
        assertFalse("sourceProvider non deve contenere 'Eurostat'", macroFromService.sourceProvider.contains("Eurostat", ignoreCase = true))
        assertFalse("sourceProvider non deve contenere 'Live'", macroFromService.sourceProvider.contains("Live", ignoreCase = true))
    }
}
