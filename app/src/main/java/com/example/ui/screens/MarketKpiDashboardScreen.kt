package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.MarketEstimateService
import com.example.util.PredictiveDealAlertEngine
import com.example.util.PredictiveDealEvaluation
import com.example.util.PredictiveDealNotificationManager
import com.example.util.ProvinceScrapedKpi
import com.example.util.RegionalAggregateKpi
import com.example.util.RegionalTrendPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

enum class ChartMetricType(val label: String, val unit: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SALE_PRICE("Prezzo Vendita", "€/m²", Icons.Default.MonetizationOn),
    RENT_PRICE("Canone Locazione", "€/m²/m", Icons.Default.Key),
    GROSS_YIELD("Gross Yield", "%", Icons.Default.Percent),
    DAYS_ON_MARKET("Tempo Vendita", "gg", Icons.Default.Timer),
    SATURATION("Saturazione", "/100", Icons.Default.Speed)
}

enum class ChartVisualMode(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    LINE_SPLINE("Recharts Spline", Icons.AutoMirrored.Filled.TrendingUp),
    BAR_CHART("Bar Chart", Icons.Default.BarChart),
    MULTI_COMPARE("Multi-Città", Icons.Default.CompareArrows)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketKpiDashboardScreen(
    onNavigateBack: () -> Unit,
    onOpenRegionalHeatmap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Base Curated Province & Regional Data
    val allProvinces = remember { MarketEstimateService.getAllCuratedProvinceKpis() }
    val allRegions = remember { MarketEstimateService.getAllRegionalAggregates() }

    // Selected state
    var selectedLocationName by remember { mutableStateOf("Milano") }
    var selectedMetric by remember { mutableStateOf(ChartMetricType.SALE_PRICE) }
    var chartVisualMode by remember { mutableStateOf(ChartVisualMode.LINE_SPLINE) }
    var comparisonLocations by remember { mutableStateOf(listOf("Milano", "Roma", "Bologna", "Paderno Dugnano")) }

    // Scraper live search query state
    var searchQuery by remember { mutableStateOf("") }
    var isScrapingLive by remember { mutableStateOf(false) }
    var scrapedCustomKpi by remember { mutableStateOf<ProvinceScrapedKpi?>(null) }
    var liveScrapeStatusMessage by remember { mutableStateOf<String?>(null) }

    // Current active Province KPI
    val activeKpi: ProvinceScrapedKpi = remember(selectedLocationName, scrapedCustomKpi) {
        if (scrapedCustomKpi != null && scrapedCustomKpi!!.locationName.equals(selectedLocationName, ignoreCase = true)) {
            scrapedCustomKpi!!
        } else {
            allProvinces.find { it.locationName.equals(selectedLocationName, ignoreCase = true) }
                ?: MarketEstimateService.getCuratedProvinceKpi(selectedLocationName)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = SurfaceCardLight,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("btn_dashboard_back")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = TextPrimaryDark)
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Osservatorio KPI di Mercato",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (activeKpi.isLiveScraped) CyanAccent.copy(alpha = 0.2f) else RoseRed.copy(alpha = 0.15f),
                                        border = BorderStroke(0.6.dp, if (activeKpi.isLiveScraped) CyanAccent else RoseRed)
                                    ) {
                                        Text(
                                            text = if (activeKpi.isLiveScraped) "STIMA AI" else "DATO FITTIZIO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (activeKpi.isLiveScraped) CyanAccent else RoseRed,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (activeKpi.isLiveScraped) "Stima generata da AI con ricerca web - non verificata" else "Valore di esempio inserito nel codice - NON è un dato di mercato",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        // Share / Export Button
                        IconButton(
                            onClick = {
                                val saleStr = activeKpi.avgSalePriceSqM?.let { "€${it.toInt()}/m²" } ?: "N/D"
                                val yoyStr = activeKpi.saleTrendYoY?.let { "YoY $it%" } ?: "YoY N/D"
                                val rentStr = activeKpi.avgRentPriceSqM?.let { "€${String.format(Locale.ITALY, "%.2f", it)}/m²/mese" } ?: "N/D"
                                val yieldStr = activeKpi.grossRentalYield?.let { "${String.format(Locale.ITALY, "%.2f", it)}%" } ?: "N/D"
                                val domStr = activeKpi.avgDaysOnMarket?.let { "$it gg" } ?: "N/D"
                                val satStr = activeKpi.marketSaturationScore?.let { "$it/100" } ?: "N/D"
                                val summary = """
                                    📊 Osservatorio Mercato: ${activeKpi.locationName} (${activeKpi.province})
                                    🏷️ Prezzo Medio: $saleStr ($yoyStr)
                                    🔑 Canone Affitto: $rentStr
                                    💰 Gross Yield: $yieldStr
                                    ⏱️ Days on Market: $domStr
                                    🌡️ Indice Saturazione: $satStr
                                    Fonte: Stime di mercato DealRadar
                                """.trimIndent()
                                clipboardManager.setText(AnnotatedString(summary))
                                Toast.makeText(context, "Report copiato negli appunti!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("btn_export_kpi_dashboard")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Condividi", tint = CyanAccent)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live Scraper Search & Trigger Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cerca comune o provincia (es. Paderno Dugnano, Roma, Monza)...", fontSize = 12.sp, color = TextMutedDark) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = SurfaceCardBorder,
                                focusedContainerColor = Color(0xFFF7F5FA),
                                unfocusedContainerColor = Color(0xFFF7F5FA)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("input_dashboard_search_city")
                        )

                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank() && !isScrapingLive) {
                                    coroutineScope.launch {
                                        isScrapingLive = true
                                        liveScrapeStatusMessage = "Stima quote per $searchQuery..."
                                        val res = MarketEstimateService.scrapeMarketKpis(searchQuery)
                                        res.onSuccess {
                                            scrapedCustomKpi = it
                                            selectedLocationName = it.locationName
                                            isScrapingLive = false
                                            liveScrapeStatusMessage = "Dati aggiornati per ${it.locationName}!"
                                        }.onFailure {
                                            isScrapingLive = false
                                            liveScrapeStatusMessage = "Errore stima, caricati benchmark OMI."
                                        }
                                    }
                                }
                            },
                            enabled = searchQuery.isNotBlank() && !isScrapingLive,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_run_live_scraper_dashboard")
                        ) {
                            if (isScrapingLive) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.TravelExplore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scrape", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (liveScrapeStatusMessage != null) {
                        Text(
                            text = liveScrapeStatusMessage!!,
                            fontSize = 10.sp,
                            color = if (isScrapingLive) AmberGold else EmeraldGreen,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }
            }
        },
        containerColor = DarkSlateBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("market_kpi_dashboard_content"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Regional Heatmap D3 Banner Callout
            if (onOpenRegionalHeatmap != null) {
                item {
                    Surface(
                        color = BentoPurpleHeader,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BentoPurpleOnContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRegionalHeatmap() }
                            .testTag("btn_open_regional_heatmap_from_kpi")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BentoPurpleOnContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Heatmap Regionale D3",
                                        color = BentoPurpleOnContainer,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = EmeraldGreen.copy(alpha = 0.2f),
                                        border = BorderStroke(0.6.dp, EmeraldGreen)
                                    ) {
                                        Text(
                                            text = "INTERATTIVA",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EmeraldGreen,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Mappa termica con shader D3, densità prezzi (€/m²), rendimenti medi e sentiment",
                                    color = TextPrimaryDark,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = BentoPurpleOnContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 1. Regional / Provincial Selector Chips Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELEZIONE CITTA' & PROVINCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMutedDark,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${allProvinces.size} Province Mappate",
                            fontSize = 10.sp,
                            color = CyanAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allProvinces) { prov ->
                            val isSelected = prov.locationName.equals(selectedLocationName, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) CyanAccent else SurfaceCardDark,
                                border = BorderStroke(1.dp, if (isSelected) CyanAccent else SurfaceCardBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedLocationName = prov.locationName
                                    }
                                    .testTag("chip_province_${prov.province}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = prov.province,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.Black else TextSecondaryDark
                                    )
                                    Text(
                                        text = prov.locationName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else TextPrimaryDark
                                    )
                                    Text(
                                        text = if (prov.avgSalePriceSqM != null) "€${prov.avgSalePriceSqM.toInt()}" else "N/D",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.Black.copy(alpha = 0.8f) else AmberGold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Active City Snapshot Hero Banner Card
            item {
                ActiveLocationSnapshotCard(
                    kpi = activeKpi,
                    onOpenImmobiliareWeb = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeKpi.sourceUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                )
            }

            // 3. Interactive Recharts-style Line & Area Chart Container
            item {
                InteractiveRechartsContainer(
                    kpi = activeKpi,
                    allProvinces = allProvinces,
                    selectedMetric = selectedMetric,
                    onMetricSelected = { selectedMetric = it },
                    visualMode = chartVisualMode,
                    onVisualModeSelected = { chartVisualMode = it },
                    comparisonLocations = comparisonLocations,
                    onToggleComparison = { loc ->
                        comparisonLocations = if (comparisonLocations.contains(loc)) {
                            if (comparisonLocations.size > 1) comparisonLocations - loc else comparisonLocations
                        } else {
                            comparisonLocations + loc
                        }
                    }
                )
            }

            // 3b. Predictive Deal Radar: Top 10% Historical Yield Deals in this Province
            item {
                ProvinceTop10DealsPredictiveCard(kpi = activeKpi)
            }

            // 4. Hot Micro-Zones Pricing Breakdown Card (if present)
            if (activeKpi.hotMicroZones.isNotEmpty()) {
                item {
                    MicroZonesDistributionCard(
                        locationName = activeKpi.locationName,
                        baseCityPrice = activeKpi.avgSalePriceSqM,
                        microZones = activeKpi.hotMicroZones
                    )
                }
            }

            // 5. Regional Benchmark Comparison Matrix
            item {
                RegionalComparisonMatrixCard(
                    regions = allRegions,
                    onSelectRegion = { reg ->
                        reg.topProvinces.firstOrNull()?.let {
                            selectedLocationName = it.locationName
                        }
                    }
                )
            }

            // 6. Market Liquidity & Investment Attractiveness Scorecard
            item {
                MarketAttractivenessScorecard(kpi = activeKpi)
            }

            // Spacer
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Active Location Hero Snapshot Card
 */
@Composable
fun ActiveLocationSnapshotCard(
    kpi: ProvinceScrapedKpi,
    onOpenImmobiliareWeb: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BentoCardBgLight,
        border = BorderStroke(1.dp, if (kpi.isLiveScraped) EmeraldGreen.copy(alpha = 0.6f) else CyanAccent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${kpi.locationName} (${kpi.province})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "• ${kpi.region}",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (kpi.isLiveScraped) CyanAccent.copy(alpha = 0.2f) else RoseRed.copy(alpha = 0.15f),
                                border = BorderStroke(0.6.dp, if (kpi.isLiveScraped) CyanAccent else RoseRed)
                            ) {
                                Text(
                                    text = if (kpi.isLiveScraped) "STIMA AI" else "DATO FITTIZIO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (kpi.isLiveScraped) CyanAccent else RoseRed,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (kpi.isLiveScraped) "Stima generata da AI con ricerca web - non verificata" else "Valore di esempio inserito nel codice - NON è un dato di mercato",
                            fontSize = 10.sp,
                            color = if (kpi.isLiveScraped) TextMutedDark else AmberGold
                        )
                    }
                }

                if (kpi.sourceUrl.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceCardDark,
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenImmobiliareWeb() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Fonte Web", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = CyanAccent)
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            // 4 Grid Metric Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    title = "Prezzo Vendita",
                    value = if (kpi.avgSalePriceSqM != null) "€${kpi.avgSalePriceSqM.toInt()}/m²" else "N/D",
                    subtext = if (kpi.saleTrendYoY != null) {
                        if (kpi.saleTrendYoY >= 0) "+${kpi.saleTrendYoY}% YoY" else "${kpi.saleTrendYoY}% YoY"
                    } else "N/D",
                    subtextColor = if (kpi.saleTrendYoY != null) {
                        if (kpi.saleTrendYoY >= 0) EmeraldGreen else RoseRed
                    } else TextMutedDark,
                    modifier = Modifier.weight(1f)
                )

                MetricTile(
                    title = "Canone Affitto",
                    value = if (kpi.avgRentPriceSqM != null) "€${String.format(Locale.ITALY, "%.2f", kpi.avgRentPriceSqM)}/m²" else "N/D",
                    subtext = if (kpi.grossRentalYield != null) "Yield: ${String.format(Locale.ITALY, "%.1f", kpi.grossRentalYield)}%" else "Yield: N/D",
                    subtextColor = if (kpi.grossRentalYield != null) EmeraldGreen else TextMutedDark,
                    modifier = Modifier.weight(1f)
                )

                MetricTile(
                    title = "DOM Liquidità",
                    value = if (kpi.avgDaysOnMarket != null) "${kpi.avgDaysOnMarket} gg" else "N/D",
                    subtext = if (kpi.absorptionRatePercent != null) "Assorb.: ${kpi.absorptionRatePercent.toInt()}%" else "Assorb.: N/D",
                    subtextColor = if (kpi.absorptionRatePercent != null) AmberGold else TextMutedDark,
                    modifier = Modifier.weight(1f)
                )

                MetricTile(
                    title = "Saturazione",
                    value = if (kpi.marketSaturationScore != null) "${kpi.marketSaturationScore}/100" else "N/D",
                    subtext = if (kpi.marketSaturationScore != null) {
                        if (kpi.marketSaturationScore < 45) "Alta Domanda" else "Mercato Pieno"
                    } else "N/D",
                    subtextColor = if (kpi.marketSaturationScore != null) {
                        if (kpi.marketSaturationScore < 45) EmeraldGreen else RoseRed
                    } else TextMutedDark,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quality & Completeness Measures
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceCardDark,
                border = BorderStroke(0.6.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Completezza dati: ${(kpi.completeness * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        if (kpi.scrapeLatencyMs > 0) {
                            Text(
                                text = "${kpi.scrapeLatencyMs}ms",
                                fontSize = 10.sp,
                                color = AmberGold,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Affidabilità fonte: ${kpi.sourceReliability?.let { "${(it * 100).toInt()}%" } ?: "non misurabile"}",
                            fontSize = 10.sp,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = "Confidenza stima: ${kpi.valuationConfidence?.let { "${(it * 100).toInt()}%" } ?: "non misurabile"}",
                            fontSize = 10.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            // Status Strip Telemetria Stima
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E2230),
                border = BorderStroke(0.6.dp, CyanAccent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = if (!kpi.usedFallbackData) EmeraldGreen else AmberGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Stato Servizio: ${kpi.mitigationEngineStatus}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                    }
                }
            }

            if (kpi.marketSummary.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceCardDark.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 ${kpi.marketSummary}",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    subtext: String,
    subtextColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCardDark,
        border = BorderStroke(0.6.dp, SurfaceCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = title, fontSize = 9.sp, color = TextMutedDark, maxLines = 1)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark, maxLines = 1)
            Text(text = subtext, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = subtextColor, maxLines = 1)
        }
    }
}

/**
 * Interactive Recharts-style Chart Container with Bezier Curves, Tooltip Scrubber & Multi-Metric comparison
 */
@Composable
fun InteractiveRechartsContainer(
    kpi: ProvinceScrapedKpi,
    allProvinces: List<ProvinceScrapedKpi>,
    selectedMetric: ChartMetricType,
    onMetricSelected: (ChartMetricType) -> Unit,
    visualMode: ChartVisualMode,
    onVisualModeSelected: (ChartVisualMode) -> Unit,
    comparisonLocations: List<String>,
    onToggleComparison: (String) -> Unit
) {
    val timeline = remember(kpi) { kpi.historicalTrends }
    var activeHoverIndex by remember { mutableStateOf<Int?>(null) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCardLight,
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Chart Header & Visual Mode Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Trend Storico & Proiezioni (2021-2026)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                    Text(
                        text = "Curve interattive stile Recharts con scrubber a tocco",
                        fontSize = 10.sp,
                        color = TextSecondaryDark
                    )
                }

                // Mode Selector Segmented Row
                Row(
                    modifier = Modifier
                        .background(DarkSlateBg, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ChartVisualMode.entries.forEach { mode ->
                        val isModeSelected = visualMode == mode
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isModeSelected) CyanAccent else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onVisualModeSelected(mode) }
                        ) {
                            Text(
                                text = mode.label,
                                fontSize = 9.sp,
                                fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isModeSelected) Color.Black else TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Metric Selector Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ChartMetricType.entries.toTypedArray()) { metric ->
                    val isMetricSelected = selectedMetric == metric
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMetricSelected) BentoPurpleHeader else SurfaceCardDark,
                        border = BorderStroke(1.dp, if (isMetricSelected) BentoPurpleOnContainer else SurfaceCardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onMetricSelected(metric) }
                            .testTag("metric_chip_${metric.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = metric.icon,
                                contentDescription = null,
                                tint = if (isMetricSelected) BentoPurpleOnContainer else TextSecondaryDark,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = metric.label,
                                fontSize = 11.sp,
                                fontWeight = if (isMetricSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isMetricSelected) BentoPurpleOnContainer else TextPrimaryDark
                            )
                        }
                    }
                }
            }

            // Multi-city comparison selector row if in Multi-compare mode
            if (visualMode == ChartVisualMode.MULTI_COMPARE) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Città in sovrapposizione:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryDark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val availableCities = listOf("Milano", "Roma", "Bologna", "Torino", "Paderno Dugnano", "Firenze", "Napoli", "Verona", "Bari")
                        val colors = listOf(CyanAccent, AmberGold, EmeraldGreen, PurpleIndigo, RoseRed, Color(0xFF00E5FF), Color(0xFFFF4081), Color(0xFFFFD600), Color(0xFF76FF03))

                        availableCities.forEachIndexed { index, city ->
                            val isChecked = comparisonLocations.contains(city)
                            val color = colors[index % colors.size]
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isChecked) color.copy(alpha = 0.2f) else SurfaceCardDark,
                                border = BorderStroke(1.dp, if (isChecked) color else SurfaceCardBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onToggleComparison(city) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                                    Text(
                                        text = city,
                                        fontSize = 10.sp,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChecked) TextPrimaryDark else TextSecondaryDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // The Recharts Interactive Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(DarkSlateBg, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                if (visualMode == ChartVisualMode.BAR_CHART) {
                    RechartsBarChart(
                        timeline = timeline,
                        selectedMetric = selectedMetric,
                        activeHoverIndex = activeHoverIndex,
                        onHoverIndexChanged = { activeHoverIndex = it }
                    )
                } else if (visualMode == ChartVisualMode.MULTI_COMPARE) {
                    RechartsMultiLineChart(
                        comparisonLocations = comparisonLocations,
                        allProvinces = allProvinces,
                        selectedMetric = selectedMetric,
                        activeHoverIndex = activeHoverIndex,
                        onHoverIndexChanged = { activeHoverIndex = it }
                    )
                } else {
                    RechartsSplineLineChart(
                        timeline = timeline,
                        selectedMetric = selectedMetric,
                        activeHoverIndex = activeHoverIndex,
                        onHoverIndexChanged = { activeHoverIndex = it }
                    )
                }
            }

            // Interactive Tooltip Callout Box
            if (timeline.isNotEmpty()) {
                val index = activeHoverIndex ?: (timeline.size - 1)
                val safePoint = timeline.getOrNull(index) ?: timeline.last()

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BentoCardBgLight,
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Punto Selezionato: Anno ${safePoint.yearLabel}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Tocca e scorri sul grafico per esaminare lo storico",
                                fontSize = 9.sp,
                                color = TextSecondaryDark
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = when (selectedMetric) {
                                        ChartMetricType.SALE_PRICE -> "€${safePoint.avgSalePriceSqM.toInt()}/m²"
                                        ChartMetricType.RENT_PRICE -> "€${String.format(Locale.ITALY, "%.2f", safePoint.avgRentPriceSqM)}/m²"
                                        ChartMetricType.GROSS_YIELD -> "${String.format(Locale.ITALY, "%.2f", safePoint.grossYieldPercent)}%"
                                        ChartMetricType.DAYS_ON_MARKET -> "${safePoint.daysOnMarket} giorni"
                                        ChartMetricType.SATURATION -> "${safePoint.marketSaturation}/100"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyanAccent
                                )
                                Text(
                                    text = selectedMetric.label,
                                    fontSize = 9.sp,
                                    color = TextMutedDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts Spline Line Chart with Smooth Bezier & Gradient Fill
 */
@Composable
fun RechartsSplineLineChart(
    timeline: List<RegionalTrendPoint>,
    selectedMetric: ChartMetricType,
    activeHoverIndex: Int?,
    onHoverIndexChanged: (Int?) -> Unit
) {
    if (timeline.isEmpty()) return

    val values = timeline.map { point ->
        when (selectedMetric) {
            ChartMetricType.SALE_PRICE -> point.avgSalePriceSqM
            ChartMetricType.RENT_PRICE -> point.avgRentPriceSqM
            ChartMetricType.GROSS_YIELD -> point.grossYieldPercent
            ChartMetricType.DAYS_ON_MARKET -> point.daysOnMarket.toDouble()
            ChartMetricType.SATURATION -> point.marketSaturation.toDouble()
        }
    }

    val minVal = (values.minOrNull() ?: 0.0) * 0.92
    val maxVal = (values.maxOrNull() ?: 100.0) * 1.08
    val range = (maxVal - minVal).coerceAtLeast(1.0)

    val primaryColor = when (selectedMetric) {
        ChartMetricType.SALE_PRICE -> CyanAccent
        ChartMetricType.RENT_PRICE -> AmberGold
        ChartMetricType.GROSS_YIELD -> EmeraldGreen
        ChartMetricType.DAYS_ON_MARKET -> Color(0xFF00E5FF)
        ChartMetricType.SATURATION -> RoseRed
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(timeline) {
                detectTapGestures { offset ->
                    val stepX = size.width / (timeline.size - 1).coerceAtLeast(1)
                    val idx = (offset.x / stepX).roundToInt().coerceIn(0, timeline.size - 1)
                    onHoverIndexChanged(idx)
                }
            }
            .pointerInput(timeline) {
                detectDragGestures { change, _ ->
                    val stepX = size.width / (timeline.size - 1).coerceAtLeast(1)
                    val idx = (change.position.x / stepX).roundToInt().coerceIn(0, timeline.size - 1)
                    onHoverIndexChanged(idx)
                }
            }
    ) {
        val w = size.width
        val h = size.height - 30.dp.toPx()
        val topPadding = 10.dp.toPx()
        val count = timeline.size

        if (count < 2) return@Canvas

        val stepX = w / (count - 1)
        val points = values.mapIndexed { index, value ->
            val x = index * stepX
            val normalizedY = ((value - minVal) / range).toFloat()
            val y = topPadding + (1f - normalizedY) * h
            Offset(x, y)
        }

        // 1. Draw horizontal grid lines
        for (i in 0..4) {
            val gridY = topPadding + (h / 4f) * i
            drawLine(
                color = SurfaceCardBorder.copy(alpha = 0.5f),
                start = Offset(0f, gridY),
                end = Offset(w, gridY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. Build smooth cubic bezier path
        val path = Path()
        val fillPath = Path()

        path.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

            path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
            fillPath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
        }

        fillPath.lineTo(points.last().x, topPadding + h)
        fillPath.lineTo(points.first().x, topPadding + h)
        fillPath.close()

        // 3. Draw gradient area underneath curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.35f), primaryColor.copy(alpha = 0.02f)),
                startY = topPadding,
                endY = topPadding + h
            )
        )

        // 4. Draw glowing stroke curve
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 5. Draw data points and active scrubber line
        val hoverIdx = activeHoverIndex ?: (count - 1)

        points.forEachIndexed { index, point ->
            val isHovered = index == hoverIdx

            // Outer point ring
            drawCircle(
                color = if (isHovered) Color.White else primaryColor,
                radius = if (isHovered) 6.dp.toPx() else 4.dp.toPx(),
                center = point
            )
            drawCircle(
                color = if (isHovered) primaryColor else Color(0xFF1E1E2E),
                radius = if (isHovered) 3.5.dp.toPx() else 2.dp.toPx(),
                center = point
            )

            // Active scrubber vertical line
            if (isHovered) {
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(point.x, topPadding),
                    end = Offset(point.x, topPadding + h),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }
        }
    }
}

/**
 * Recharts Bar Chart View for Year-over-Year comparison
 */
@Composable
fun RechartsBarChart(
    timeline: List<RegionalTrendPoint>,
    selectedMetric: ChartMetricType,
    activeHoverIndex: Int?,
    onHoverIndexChanged: (Int?) -> Unit
) {
    if (timeline.isEmpty()) return

    val values = timeline.map { point ->
        when (selectedMetric) {
            ChartMetricType.SALE_PRICE -> point.avgSalePriceSqM
            ChartMetricType.RENT_PRICE -> point.avgRentPriceSqM
            ChartMetricType.GROSS_YIELD -> point.grossYieldPercent
            ChartMetricType.DAYS_ON_MARKET -> point.daysOnMarket.toDouble()
            ChartMetricType.SATURATION -> point.marketSaturation.toDouble()
        }
    }

    val maxVal = (values.maxOrNull() ?: 100.0) * 1.15

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(timeline) {
                detectTapGestures { offset ->
                    val slotWidth = size.width / timeline.size
                    val idx = (offset.x / slotWidth).toInt().coerceIn(0, timeline.size - 1)
                    onHoverIndexChanged(idx)
                }
            }
    ) {
        val w = size.width
        val h = size.height - 25.dp.toPx()
        val topPadding = 10.dp.toPx()
        val count = timeline.size
        val slotWidth = w / count
        val barWidth = slotWidth * 0.55f

        // Grid lines
        for (i in 0..3) {
            val gridY = topPadding + (h / 3f) * i
            drawLine(
                color = SurfaceCardBorder.copy(alpha = 0.5f),
                start = Offset(0f, gridY),
                end = Offset(w, gridY),
                strokeWidth = 1.dp.toPx()
            )
        }

        val hoverIdx = activeHoverIndex ?: (count - 1)

        values.forEachIndexed { index, value ->
            val barHeight = ((value / maxVal) * h).toFloat()
            val left = index * slotWidth + (slotWidth - barWidth) / 2f
            val top = topPadding + (h - barHeight)
            val isHovered = index == hoverIdx

            // Bar fill
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (isHovered) {
                        listOf(CyanAccent, AmberGold)
                    } else {
                        listOf(CyanAccent.copy(alpha = 0.8f), PurpleIndigo.copy(alpha = 0.7f))
                    },
                    startY = top,
                    endY = top + barHeight
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
        }
    }
}

/**
 * Recharts Multi-Line Comparison Chart across multiple Italian Cities
 */
@Composable
fun RechartsMultiLineChart(
    comparisonLocations: List<String>,
    allProvinces: List<ProvinceScrapedKpi>,
    selectedMetric: ChartMetricType,
    activeHoverIndex: Int?,
    onHoverIndexChanged: (Int?) -> Unit
) {
    val cityColors = listOf(CyanAccent, AmberGold, EmeraldGreen, PurpleIndigo, RoseRed, Color(0xFF00E5FF), Color(0xFFFF4081))

    val selectedKpis = comparisonLocations.map { loc ->
        allProvinces.find { it.locationName.equals(loc, ignoreCase = true) }
            ?: MarketEstimateService.getCuratedProvinceKpi(loc)
    }

    if (selectedKpis.isEmpty()) return

    val allPoints = selectedKpis.flatMap { k ->
        k.historicalTrends.map { pt ->
            when (selectedMetric) {
                ChartMetricType.SALE_PRICE -> pt.avgSalePriceSqM
                ChartMetricType.RENT_PRICE -> pt.avgRentPriceSqM
                ChartMetricType.GROSS_YIELD -> pt.grossYieldPercent
                ChartMetricType.DAYS_ON_MARKET -> pt.daysOnMarket.toDouble()
                ChartMetricType.SATURATION -> pt.marketSaturation.toDouble()
            }
        }
    }

    val minVal = (allPoints.minOrNull() ?: 0.0) * 0.9
    val maxVal = (allPoints.maxOrNull() ?: 100.0) * 1.1
    val range = (maxVal - minVal).coerceAtLeast(1.0)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(selectedKpis) {
                detectTapGestures { offset ->
                    val count = 6
                    val stepX = size.width / (count - 1)
                    val idx = (offset.x / stepX).roundToInt().coerceIn(0, count - 1)
                    onHoverIndexChanged(idx)
                }
            }
    ) {
        val w = size.width
        val h = size.height - 30.dp.toPx()
        val topPadding = 10.dp.toPx()
        val count = 6
        val stepX = w / (count - 1)

        // Grid lines
        for (i in 0..4) {
            val gridY = topPadding + (h / 4f) * i
            drawLine(
                color = SurfaceCardBorder.copy(alpha = 0.4f),
                start = Offset(0f, gridY),
                end = Offset(w, gridY),
                strokeWidth = 1.dp.toPx()
            )
        }

        selectedKpis.forEachIndexed { cityIndex, kpi ->
            val color = cityColors[cityIndex % cityColors.size]
            val values = kpi.historicalTrends.map { pt ->
                when (selectedMetric) {
                    ChartMetricType.SALE_PRICE -> pt.avgSalePriceSqM
                    ChartMetricType.RENT_PRICE -> pt.avgRentPriceSqM
                    ChartMetricType.GROSS_YIELD -> pt.grossYieldPercent
                    ChartMetricType.DAYS_ON_MARKET -> pt.daysOnMarket.toDouble()
                    ChartMetricType.SATURATION -> pt.marketSaturation.toDouble()
                }
            }

            val points = values.mapIndexed { idx, v ->
                val x = idx * stepX
                val normalizedY = ((v - minVal) / range).toFloat()
                val y = topPadding + (1f - normalizedY) * h
                Offset(x, y)
            }

            val path = Path()
            path.moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val c1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                val c2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                path.cubicTo(c1.x, c1.y, c2.x, c2.y, p1.x, p1.y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEach { pt ->
                drawCircle(color = color, radius = 3.dp.toPx(), center = pt)
            }
        }
    }
}

/**
 * Micro-Zones Pricing Breakdown Card
 */
@Composable
fun MicroZonesDistributionCard(
    locationName: String,
    baseCityPrice: Double?,
    microZones: List<Pair<String, Double>>
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCardLight,
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.PinDrop, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Micro-Zone & Quartieri ($locationName)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                Text(
                    text = if (baseCityPrice != null) "Media: €${baseCityPrice.toInt()}/m²" else "Media: N/D",
                    fontSize = 11.sp,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val fallbackBase = baseCityPrice ?: 2000.0
            val maxZonePrice = microZones.maxOfOrNull { it.second } ?: (fallbackBase * 1.5)

            microZones.forEach { (zone, price) ->
                val ratio = (price / maxZonePrice).toFloat().coerceIn(0.1f, 1f)
                val diffPercent = if (baseCityPrice != null && baseCityPrice > 0) {
                    ((price - baseCityPrice) / baseCityPrice) * 100
                } else null

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = zone, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (diffPercent != null) {
                                    if (diffPercent >= 0) "+${diffPercent.toInt()}% vs media" else "${diffPercent.toInt()}% vs media"
                                } else "N/D vs media",
                                fontSize = 10.sp,
                                color = if (diffPercent != null) {
                                    if (diffPercent >= 0) EmeraldGreen else RoseRed
                                } else TextMutedDark,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = "€${price.toInt()}/m²", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                        }
                    }

                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(DarkSlateBg, RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .fillMaxHeight()
                                .background(
                                    brush = Brush.horizontalGradient(listOf(CyanAccent, AmberGold)),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Regional Comparison Matrix Card
 */
@Composable
fun RegionalComparisonMatrixCard(
    regions: List<RegionalAggregateKpi>,
    onSelectRegion: (RegionalAggregateKpi) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCardLight,
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Benchmark Macro-Regioni Italiane",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                Text(
                    text = "Aggregato 2026",
                    fontSize = 10.sp,
                    color = TextSecondaryDark
                )
            }

            // List of regions
            regions.forEach { reg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceCardDark,
                    border = BorderStroke(0.6.dp, SurfaceCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectRegion(reg) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = reg.regionName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = BentoPurpleHeader
                                ) {
                                    Text(
                                        text = "${reg.provinceCount} Prov.",
                                        fontSize = 9.sp,
                                        color = BentoPurpleOnContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "DOM: ${reg.avgDaysOnMarket} gg • Saturazione: ${reg.avgMarketSaturation}/100",
                                fontSize = 10.sp,
                                color = TextSecondaryDark
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "€${reg.avgSalePriceSqM.toInt()}/m²", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                Text(
                                    text = if (reg.saleTrendYoY >= 0) "+${String.format(Locale.ITALY, "%.1f", reg.saleTrendYoY)}%" else "${String.format(Locale.ITALY, "%.1f", reg.saleTrendYoY)}%",
                                    fontSize = 9.sp,
                                    color = if (reg.saleTrendYoY >= 0) EmeraldGreen else RoseRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "${String.format(Locale.ITALY, "%.1f", reg.avgGrossYield)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                Text(text = "Yield Lordo", fontSize = 9.sp, color = TextMutedDark)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Market Attractiveness & Liquidity Scorecard
 */
@Composable
fun MarketAttractivenessScorecard(kpi: ProvinceScrapedKpi) {
    val liquidityScore = if (kpi.marketSaturationScore != null && kpi.absorptionRatePercent != null) {
        (100 - kpi.marketSaturationScore + (kpi.absorptionRatePercent * 0.5).toInt()).coerceIn(0, 100)
    } else null
    val yieldScore = kpi.grossRentalYield?.let { (it * 12.0).toInt().coerceIn(0, 100) }
    val growthScore = if (kpi.saleTrendYoY != null && kpi.rentTrendYoY != null) {
        ((kpi.saleTrendYoY + kpi.rentTrendYoY) * 6.5).toInt().coerceIn(0, 100)
    } else null
    val globalScore = if (liquidityScore != null && yieldScore != null && growthScore != null) {
        ((liquidityScore * 0.35) + (yieldScore * 0.35) + (growthScore * 0.30)).roundToInt()
    } else null

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BentoCardBgLight,
        border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = BentoPurpleOnContainer, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Score Attrattività Investimento",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldGreen.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, EmeraldGreen)
                ) {
                    Text(
                        text = if (globalScore != null) "$globalScore / 100" else "N/D",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // 3 Sub-scores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScoreColumnItem(title = "Liquidità Mercato", score = liquidityScore, color = CyanAccent, modifier = Modifier.weight(1f))
                ScoreColumnItem(title = "Rendimento Yield", score = yieldScore, color = EmeraldGreen, modifier = Modifier.weight(1f))
                ScoreColumnItem(title = "Crescita Capitale", score = growthScore, color = AmberGold, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ScoreColumnItem(
    title: String,
    score: Int?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCardDark,
        border = BorderStroke(0.6.dp, SurfaceCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, fontSize = 9.sp, color = TextMutedDark, maxLines = 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = if (score != null) "$score%" else "N/D", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (score != null) color else TextMutedDark)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(DarkSlateBg, RoundedCornerShape(2.dp))
            ) {
                if (score != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(score / 100f)
                            .fillMaxHeight()
                            .background(color, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

/**
 * Predictive Deal Potential Card for Top 10% Deals in the active province
 */
@Composable
fun ProvinceTop10DealsPredictiveCard(
    kpi: ProvinceScrapedKpi
) {
    val context = LocalContext.current
    val euroFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
    }

    val stats = remember(kpi) {
        PredictiveDealAlertEngine.computeProvinceHistoricalYieldStats(kpi)
    }

    // Benchmark sample deals for this province to demonstrate predictive scoring
    val sampleDeals = remember(kpi.locationName, kpi.avgSalePriceSqM, kpi.avgRentPriceSqM) {
        val baseP = kpi.avgSalePriceSqM ?: 2000.0
        listOf(
            com.example.data.Property(
                id = 901L,
                title = "Trilocale Ristrutturato con Box",
                address = "Via Roma 42, ${kpi.locationName}",
                price = (baseP * 0.72 * 85),
                surfaceSqm = 85,
                estimatedRenovationCost = 15000.0,
                targetResalePrice = (baseP * 1.05 * 85)
            ),
            com.example.data.Property(
                id = 902L,
                title = "Bilocale da Frazionare Zona Centro",
                address = "Corso Garibaldi 18, ${kpi.locationName}",
                price = (baseP * 0.80 * 65),
                surfaceSqm = 65,
                estimatedRenovationCost = 22000.0,
                targetResalePrice = (baseP * 1.10 * 65)
            ),
            com.example.data.Property(
                id = 903L,
                title = "Quadrilocale Ultimo Piano Vista Aperta",
                address = "Piazza Matteotti 5, ${kpi.locationName}",
                price = (baseP * 0.98 * 115),
                surfaceSqm = 115,
                estimatedRenovationCost = 8000.0,
                targetResalePrice = (baseP * 1.02 * 115)
            )
        )
    }

    val evaluations = remember(sampleDeals, kpi) {
        sampleDeals.map { prop ->
            PredictiveDealAlertEngine.evaluateProperty(prop, kpi)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        border = BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF10B981)))),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("province_top10_predictive_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF06B6D4).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "RADAR PREDITTIVO TOP 10% DEAL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanAccent,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = EmeraldGreen.copy(alpha = 0.2f),
                                border = BorderStroke(0.6.dp, EmeraldGreen)
                            ) {
                                Text(
                                    text = "SOGLIA P90",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Benchmark Provinciale ${stats.locationName} • Soglia Yield Top 10%: ${String.format(Locale.ITALY, "%.2f%%", stats.p90HistoricalYield)}",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val topEval = evaluations.firstOrNull { it.isTop10Percentile } ?: evaluations.first()
                        PredictiveDealNotificationManager.sendTop10DealAlertNotification(context, topEval)
                        Toast.makeText(context, "Allarme Top 10% Deal testato con successo!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(SurfaceElevatedDark, CircleShape)
                        .testTag("btn_test_top10_alert")
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = "Testa Notifica Allarme",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Text(
                text = "Il motore predittivo correla le quotazioni storiche quinquennali (2021-2026) di ${stats.locationName} per identificare istantaneamente quando un immobile entra nel 10% superiore di convenienza (Percentile ≥ 90°).",
                fontSize = 11.sp,
                color = TextSecondaryDark,
                lineHeight = 15.sp
            )

            // Statistical Distribution Summary Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Media Storica (P50)", fontSize = 10.sp, color = TextMutedDark)
                    Text(String.format(Locale.ITALY, "%.2f%%", stats.p50HistoricalYield), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Soglia Top 10% (P90)", fontSize = 10.sp, color = TextMutedDark)
                    Text(String.format(Locale.ITALY, "%.2f%%", stats.p90HistoricalYield), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = CyanAccent)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Super Alpha (P95)", fontSize = 10.sp, color = TextMutedDark)
                    Text(String.format(Locale.ITALY, "%.2f%%", stats.p95HistoricalYield), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGreen)
                }
            }

            // Deals List in Active Province
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                evaluations.forEach { eval ->
                    val isQualifying = eval.isTop10Percentile
                    val itemBorder = if (isQualifying) {
                        BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f))
                    } else {
                        BorderStroke(0.6.dp, SurfaceCardBorder)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceElevatedDark,
                        border = itemBorder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = eval.propertyTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isQualifying) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = EmeraldGreen.copy(alpha = 0.2f),
                                            border = BorderStroke(0.6.dp, EmeraldGreen)
                                        ) {
                                            Text(
                                                text = "TOP 10%",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = EmeraldGreen,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Prezzo: ${euroFormat.format(eval.currentListPrice)} (${euroFormat.format(eval.pricePerSqm)}/m²) • ${eval.surfaceSqm} m²",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )

                                Text(
                                    text = "Yield Lordo: ${String.format(Locale.ITALY, "%.2f%%", eval.propertyImpliedGrossYield)} (Sconto ${String.format(Locale.ITALY, "%.1f%%", eval.valuationDiscountPercent)})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isQualifying) EmeraldGreen else AmberGold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.0f°", eval.dealPercentile),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isQualifying) EmeraldGreen else TextSecondaryDark
                                )
                                Text(
                                    text = "Percentile",
                                    fontSize = 10.sp,
                                    color = TextMutedDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
