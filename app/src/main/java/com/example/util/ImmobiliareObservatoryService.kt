package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.DataProvenance

/**
 * Data structures representing internal Real Estate Market Observatory benchmark metrics.
 */
data class SubZoneBenchmark(
    val name: String,
    val avgSalePricePerSqM: Double,
    val avgRentPricePerSqMMonth: Double,
    val trendYoY: Double,
    val grossYield: Double = ((avgRentPricePerSqMMonth * 12.0) / avgSalePricePerSqM) * 100.0
)

data class MunicipalityMarketData(
    val municipalityName: String,
    val province: String,
    val region: String,
    val officialUrl: String = "",
    val avgSalePricePerSqM: Double,
    val minSalePricePerSqM: Double,
    val maxSalePricePerSqM: Double,
    val avgRentPricePerSqMMonth: Double,
    val minRentPricePerSqMMonth: Double,
    val maxRentPricePerSqMMonth: Double,
    val trendSaleYoY: Double, // e.g. +2.9%
    val trendRentYoY: Double, // e.g. +4.1%
    val averageDaysOnMarket: Int, // DOM (e.g. 102 days)
    val grossRentalYield: Double = ((avgRentPricePerSqMMonth * 12.0) / avgSalePricePerSqM) * 100.0,
    val subZones: List<SubZoneBenchmark> = emptyList(),
    val notes: String = "",
    val provenance: String = DataProvenance.CURATED_FALLBACK.name,
    val isGenericFallback: Boolean = false
)

data class AutomatedMarketValuation(
    val municipality: MunicipalityMarketData,
    val matchedSubZone: SubZoneBenchmark?,
    val propertyLocation: String,
    val surfaceSqM: Double,
    val askingPrice: Double,
    val askingPricePerSqM: Double,
    val zoneAvgPricePerSqM: Double,
    val zoneMinPricePerSqM: Double,
    val zoneMaxPricePerSqM: Double,
    val discountVsMarketPercent: Double,
    val estimatedMarketValueAsIs: Double,
    val estimatedMarketValueRenovated: Double,
    val estimatedMonthlyRent: Double,
    val estimatedAnnualGrossRent: Double,
    val grossRentalYieldPercent: Double,
    val capRatePercent: Double,
    val estimatedFlipGrossProfit: Double,
    val estimatedFlipRoiPercent: Double,
    val liquidityRating: String, // e.g. "Molto Liquido (68 gg)", "Buona Liquidità (102 gg)"
    val dealGrade: MarketDealGrade,
    val isValid: Boolean = true,
    val missingFields: List<String> = emptyList()
)

enum class MarketDealGrade(val title: String, val isPositive: Boolean) {
    EXCELLENT_OPPORTUNITY("🔥 Super Opportunità Sotto-Mercato", true),
    GREAT_VALUE("⭐ Ottimo Prezzo con Alto Margine", true),
    FAIR_MARKET("✅ Allineato ai Valori Medi di Zona", true),
    ABOVE_MARKET("⚠️ Prezzo Superiore alla Media di Zona", false),
    NOT_EVALUATED("❓ Valutazione Non Disponibile", false)
}

object ImmobiliareObservatoryService {

    /**
     * Curated dataset calibrated on internal market benchmark KPIs.
     */
    val PADERNO_DUGNANO = MunicipalityMarketData(
        municipalityName = "Paderno Dugnano",
        province = "Milano (MI)",
        region = "Lombardia",
        officialUrl = "",
        avgSalePricePerSqM = 2120.0,
        minSalePricePerSqM = 1680.0,
        maxSalePricePerSqM = 2450.0,
        avgRentPricePerSqMMonth = 10.80,
        minRentPricePerSqMMonth = 8.50,
        maxRentPricePerSqMMonth = 13.00,
        trendSaleYoY = 2.9,
        trendRentYoY = 4.2,
        averageDaysOnMarket = 102,
        subZones = listOf(
            SubZoneBenchmark("Centro / Stazione FN", 2250.0, 11.40, 3.2),
            SubZoneBenchmark("Calderara", 2180.0, 10.90, 2.8),
            SubZoneBenchmark("Palazzolo Milanese", 2090.0, 10.60, 2.5),
            SubZoneBenchmark("Cassina Amata", 2020.0, 10.20, 2.1),
            SubZoneBenchmark("Incirano / Villaggio Ambrosiano", 1980.0, 9.90, 2.0)
        ),
        notes = "Forte domanda di locazione grazie ai collegamenti diretti con Milano Cadorna/Garibaldi (Ferrovie Nord). Ideale per strategie Buy & Hold e valorizzazione Fix & Flip su bilocali e trilocali.",
        provenance = DataProvenance.CURATED_FALLBACK.name,
        isGenericFallback = false
    )

    val MILANO = MunicipalityMarketData(
        municipalityName = "Milano",
        province = "Milano (MI)",
        region = "Lombardia",
        officialUrl = "",
        avgSalePricePerSqM = 5380.0,
        minSalePricePerSqM = 2850.0,
        maxSalePricePerSqM = 10500.0,
        avgRentPricePerSqMMonth = 23.40,
        minRentPricePerSqMMonth = 16.00,
        maxRentPricePerSqMMonth = 36.00,
        trendSaleYoY = 3.6,
        trendRentYoY = 6.8,
        averageDaysOnMarket = 68,
        subZones = listOf(
            SubZoneBenchmark("Centro Storico / Duomo / Brera", 10200.0, 34.50, 4.1),
            SubZoneBenchmark("Porta Nuova / Isola / Garibaldi", 7200.0, 27.80, 5.2),
            SubZoneBenchmark("Città Studi / Piola", 4950.0, 22.10, 3.8),
            SubZoneBenchmark("Navigli / Bocconi", 6100.0, 25.40, 4.0),
            SubZoneBenchmark("Bande Nere / San Siro", 4150.0, 18.50, 2.7),
            SubZoneBenchmark("Lambrate / NoLo / Udine", 3800.0, 18.90, 4.5)
        ),
        notes = "Mercato altamente liquido con elevatissima pressione di affitto per studenti e giovani professionisti.",
        provenance = DataProvenance.CURATED_FALLBACK.name,
        isGenericFallback = false
    )

    val MONZA = MunicipalityMarketData(
        municipalityName = "Monza",
        province = "Monza e Brianza (MB)",
        region = "Lombardia",
        officialUrl = "",
        avgSalePricePerSqM = 2850.0,
        minSalePricePerSqM = 2100.0,
        maxSalePricePerSqM = 4200.0,
        avgRentPricePerSqMMonth = 13.20,
        minRentPricePerSqMMonth = 10.00,
        maxRentPricePerSqMMonth = 18.50,
        trendSaleYoY = 3.1,
        trendRentYoY = 5.0,
        averageDaysOnMarket = 88,
        subZones = listOf(
            SubZoneBenchmark("Centro Storico / Parco", 3850.0, 16.20, 3.5),
            SubZoneBenchmark("Triante / San Biagio", 2950.0, 13.80, 2.9),
            SubZoneBenchmark("San Fruttuoso", 2550.0, 12.10, 2.7),
            SubZoneBenchmark("San Rocco / Cederna", 2200.0, 10.90, 3.0)
        ),
        notes = "Polo d'eccellenza per famiglie e pendolari verso Milano, trainato dal prolungamento M5.",
        provenance = DataProvenance.CURATED_FALLBACK.name,
        isGenericFallback = false
    )

    val ROMA = MunicipalityMarketData(
        municipalityName = "Roma",
        province = "Roma (RM)",
        region = "Lazio",
        officialUrl = "",
        avgSalePricePerSqM = 3380.0,
        minSalePricePerSqM = 1850.0,
        maxSalePricePerSqM = 7800.0,
        avgRentPricePerSqMMonth = 16.50,
        minRentPricePerSqMMonth = 11.00,
        maxRentPricePerSqMMonth = 28.00,
        trendSaleYoY = 1.9,
        trendRentYoY = 5.4,
        averageDaysOnMarket = 95,
        subZones = listOf(
            SubZoneBenchmark("Centro Storico / Trastevere", 6800.0, 26.50, 2.8),
            SubZoneBenchmark("Prati / Flaminio", 5200.0, 21.00, 2.2),
            SubZoneBenchmark("San Giovanni / Appio Latino", 3750.0, 16.80, 2.5),
            SubZoneBenchmark("Tuscolano / Cinecittà", 2700.0, 13.50, 1.8),
            SubZoneBenchmark("EUR / Montagnola", 3650.0, 15.80, 1.5)
        ),
        notes = "Forte ripresa delle locazioni turistiche e transitorie; ottime opportunità di ristrutturazione frazionata.",
        provenance = DataProvenance.CURATED_FALLBACK.name,
        isGenericFallback = false
    )

    val TORINO = MunicipalityMarketData(
        municipalityName = "Torino",
        province = "Torino (TO)",
        region = "Piemonte",
        officialUrl = "",
        avgSalePricePerSqM = 1980.0,
        minSalePricePerSqM = 1100.0,
        maxSalePricePerSqM = 3900.0,
        avgRentPricePerSqMMonth = 11.10,
        minRentPricePerSqMMonth = 7.50,
        maxRentPricePerSqMMonth = 17.00,
        trendSaleYoY = 2.4,
        trendRentYoY = 4.8,
        averageDaysOnMarket = 98,
        subZones = listOf(
            SubZoneBenchmark("Centro Storico / Quadrilatero", 3450.0, 15.90, 3.1),
            SubZoneBenchmark("Crocetta / San Salvario", 2650.0, 13.40, 2.9),
            SubZoneBenchmark("San Donato / Campidoglio", 1950.0, 10.80, 2.2),
            SubZoneBenchmark("Santa Rita / Mirafiori Nord", 1680.0, 9.40, 1.9)
        ),
        notes = "Capitale del rendimento lordo (Gross Yield > 6.7%) per locazioni a studenti universitari Politecnico/UniTO.",
        provenance = DataProvenance.CURATED_FALLBACK.name,
        isGenericFallback = false
    )

    val BOLOGNA = MunicipalityMarketData(
        municipalityName = "Bologna",
        province = "Bologna (BO)",
        region = "Emilia-Romagna",
        officialUrl = "",
        avgSalePricePerSqM = 3450.0,
        minSalePricePerSqM = 2300.0,
        maxSalePricePerSqM = 5400.0,
        avgRentPricePerSqMMonth = 18.20,
        minRentPricePerSqMMonth = 13.00,
        maxRentPricePerSqMMonth = 25.00,
        trendSaleYoY = 3.8,
        trendRentYoY = 7.2,
        averageDaysOnMarket = 62,
        subZones = listOf(
            SubZoneBenchmark("Centro Storico / Santo Stefano", 4750.0, 22.50, 4.2),
            SubZoneBenchmark("Murri / Mazzini", 3600.0, 18.00, 3.5),
            SubZoneBenchmark("Bolognina / Navile", 2950.0, 16.50, 4.9),
            SubZoneBenchmark("San Donato / San Vitale", 2850.0, 15.80, 3.9)
        ),
        notes = "Mercato con tasso di sfitto praticamente a zero; canoni in fortissima ascesa.",
        provenance = DataProvenance.CURATED_FALLBACK.name,
        isGenericFallback = false
    )

    val ALL_PRESETS = listOf(PADERNO_DUGNANO, MILANO, MONZA, ROMA, TORINO, BOLOGNA)

    /**
     * Resolve municipality data either from exact match, subzone match, or URL parsing.
     */
    fun findMarketData(queryOrUrl: String): MunicipalityMarketData {
        val clean = queryOrUrl.trim().lowercase()

        // Match URL pattern
        if (clean.contains("paderno") || clean.contains("paderno-dugnano")) {
            return PADERNO_DUGNANO
        }
        if (clean.contains("milano") || clean.contains("/milano/")) {
            return MILANO
        }
        if (clean.contains("monza")) {
            return MONZA
        }
        if (clean.contains("roma")) {
            return ROMA
        }
        if (clean.contains("torino")) {
            return TORINO
        }
        if (clean.contains("bologna")) {
            return BOLOGNA
        }

        // Generic fallback generated with dynamic calculation
        val extractedName = if (clean.contains("immobiliare.it")) {
            clean.substringAfterLast("/").replace("-", " ").capitalizeWords()
        } else {
            queryOrUrl.trim().capitalizeWords()
        }

        return MunicipalityMarketData(
            municipalityName = if (extractedName.isNotBlank()) extractedName else "Comune Selezionato",
            province = "Italia",
            region = "Nazionale",
            officialUrl = "",
            avgSalePricePerSqM = 2200.0,
            minSalePricePerSqM = 1600.0,
            maxSalePricePerSqM = 2900.0,
            avgRentPricePerSqMMonth = 11.00,
            minRentPricePerSqMMonth = 8.50,
            maxRentPricePerSqMMonth = 14.50,
            trendSaleYoY = 2.5,
            trendRentYoY = 4.0,
            averageDaysOnMarket = 95,
            notes = "Valore di riferimento interno non verificato: nessun dato dell'Osservatorio è stato consultato.",
            provenance = DataProvenance.CURATED_FALLBACK.name,
            isGenericFallback = true
        )
    }

    /**
     * Valuta un immobile a scopo dimostrativo rispetto a parametri interni e benchmark di zona.
     * Restituisce isValid = false se mancano superficie o prezzo richiesto.
     */
    fun evaluateProperty(
        location: String,
        surfaceSqM: Double,
        askingPrice: Double,
        renovationBudget: Double = 0.0
    ): AutomatedMarketValuation {
        val marketData = findMarketData(location)

        // Find subzone match if any
        val lowerLoc = location.lowercase()
        val matchedSubZone = marketData.subZones.firstOrNull { sub ->
            lowerLoc.contains(sub.name.lowercase().substringBefore("/").trim())
        }

        val zoneAvg = matchedSubZone?.avgSalePricePerSqM ?: marketData.avgSalePricePerSqM
        val zoneMin = marketData.minSalePricePerSqM
        val zoneMax = marketData.maxSalePricePerSqM
        val zoneRentSqM = matchedSubZone?.avgRentPricePerSqMMonth ?: marketData.avgRentPricePerSqMMonth

        if (surfaceSqM <= 0 || askingPrice <= 0) {
            val missing = mutableListOf<String>()
            if (surfaceSqM <= 0) missing.add("Superficie (m²)")
            if (askingPrice <= 0) missing.add("Prezzo richiesto (€)")
            return AutomatedMarketValuation(
                municipality = marketData,
                matchedSubZone = matchedSubZone,
                propertyLocation = location,
                surfaceSqM = surfaceSqM,
                askingPrice = askingPrice,
                askingPricePerSqM = 0.0,
                zoneAvgPricePerSqM = zoneAvg,
                zoneMinPricePerSqM = zoneMin,
                zoneMaxPricePerSqM = zoneMax,
                discountVsMarketPercent = 0.0,
                estimatedMarketValueAsIs = 0.0,
                estimatedMarketValueRenovated = 0.0,
                estimatedMonthlyRent = 0.0,
                estimatedAnnualGrossRent = 0.0,
                grossRentalYieldPercent = 0.0,
                capRatePercent = 0.0,
                estimatedFlipGrossProfit = 0.0,
                estimatedFlipRoiPercent = 0.0,
                liquidityRating = "Dati insufficienti",
                dealGrade = MarketDealGrade.NOT_EVALUATED,
                isValid = false,
                missingFields = missing
            )
        }

        val askingPricePerSqM = askingPrice / surfaceSqM
        val discountPercent = ((zoneAvg - askingPricePerSqM) / zoneAvg) * 100.0

        val estimatedMarketValueAsIs = surfaceSqM * zoneAvg
        // Renovated value achieves the upper echelon of the zone market (max - 5%)
        val estimatedMarketValueRenovated = surfaceSqM * (zoneMax * 0.95)

        val estimatedMonthlyRent = surfaceSqM * zoneRentSqM
        val estimatedAnnualGrossRent = estimatedMonthlyRent * 12.0
        val grossRentalYieldPercent = (estimatedAnnualGrossRent / askingPrice) * 100.0
        val estimatedNetNoi = (estimatedAnnualGrossRent * 0.82) // 18% taxes + management
        val capRatePercent = (estimatedNetNoi / (askingPrice + 5000.0)) * 100.0

        // Flip Financials
        val safeRenov = if (renovationBudget > 0) renovationBudget else (surfaceSqM * 400.0) // 400€/m2 standard renovation
        val totalFlipCost = askingPrice + safeRenov + (askingPrice * 0.05) // 5% legal & closing fees
        val flipProfit = estimatedMarketValueRenovated - totalFlipCost
        val flipRoiPercent = if (totalFlipCost > 0) (flipProfit / totalFlipCost) * 100.0 else 0.0

        val liquidityRating = when {
            marketData.averageDaysOnMarket <= 75 -> "Alta Liquidità (${marketData.averageDaysOnMarket} gg medi)"
            marketData.averageDaysOnMarket <= 110 -> "Buona Liquidità (${marketData.averageDaysOnMarket} gg medi)"
            else -> "Mercato Moderato (${marketData.averageDaysOnMarket} gg medi)"
        }

        val dealGrade = when {
            discountPercent >= 28.0 -> MarketDealGrade.EXCELLENT_OPPORTUNITY
            discountPercent >= 15.0 -> MarketDealGrade.GREAT_VALUE
            discountPercent >= -5.0 -> MarketDealGrade.FAIR_MARKET
            else -> MarketDealGrade.ABOVE_MARKET
        }

        return AutomatedMarketValuation(
            municipality = marketData,
            matchedSubZone = matchedSubZone,
            propertyLocation = location,
            surfaceSqM = surfaceSqM,
            askingPrice = askingPrice,
            askingPricePerSqM = askingPricePerSqM,
            zoneAvgPricePerSqM = zoneAvg,
            zoneMinPricePerSqM = zoneMin,
            zoneMaxPricePerSqM = zoneMax,
            discountVsMarketPercent = discountPercent,
            estimatedMarketValueAsIs = estimatedMarketValueAsIs,
            estimatedMarketValueRenovated = estimatedMarketValueRenovated,
            estimatedMonthlyRent = estimatedMonthlyRent,
            estimatedAnnualGrossRent = estimatedAnnualGrossRent,
            grossRentalYieldPercent = grossRentalYieldPercent,
            capRatePercent = capRatePercent,
            estimatedFlipGrossProfit = flipProfit,
            estimatedFlipRoiPercent = flipRoiPercent,
            liquidityRating = liquidityRating,
            dealGrade = dealGrade,
            isValid = true,
            missingFields = emptyList()
        )
    }

    /**
     * Open official observatory page in browser via Android Intent securely.
     */
    fun openOfficialObservatory(context: Context, url: String) {
        try {
            val cleanUrl = url.trim()
            if (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true)) {
                return
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Browser not available or invalid url
        }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
