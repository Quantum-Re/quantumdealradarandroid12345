package com.example.ui.screens

import android.graphics.Paint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertyDeal
import com.example.ui.DealRadarViewModel
import com.example.ui.theme.*
import com.example.util.MarketEstimateService
import com.example.util.ProvinceScrapedKpi
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.*

/**
 * Metric to visualize on the D3 Heatmap.
 */
enum class HeatmapMetric(
    val title: String,
    val unit: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
) {
    PRICE_DENSITY("Densità Prezzo", "€/m²", Icons.Default.MonetizationOn, "Valore medio di vendita al metro quadro"),
    GROSS_YIELD("Rendimento Lordo", "%", Icons.Default.Percent, "Rendimento annuo da locazione stimato"),
    MARKET_SENTIMENT("Sentiment Mercato", "pt", Icons.Default.Speed, "Indice di vivacità, domanda acquirenti e liquidità"),
    DEAL_OPPORTUNITY("Opportunità Deal", "n°", Icons.Default.LocalFireDepartment, "Densità di affari e immobili a sconto tracciati")
}

/**
 * D3-inspired color schemes for interpolation.
 */
enum class D3ColorScheme(val label: String, val colors: List<Color>) {
    D3_TURBO(
        "D3 Turbo (Rainbow)",
        listOf(
            Color(0xFF30123B),
            Color(0xFF4164FA),
            Color(0xFF1DD3A8),
            Color(0xFFA4FC3C),
            Color(0xFFFABA39),
            Color(0xFFE94412),
            Color(0xFF7A0403)
        )
    ),
    D3_VIRIDIS(
        "D3 Viridis",
        listOf(
            Color(0xFF440154),
            Color(0xFF482878),
            Color(0xFF3E4A89),
            Color(0xFF31688E),
            Color(0xFF26828E),
            Color(0xFF1F9E89),
            Color(0xFF35B779),
            Color(0xFF6DCD59),
            Color(0xFFB4DE2C),
            Color(0xFFFDE725)
        )
    ),
    D3_PLASMA(
        "D3 Plasma (Thermal)",
        listOf(
            Color(0xFF0D0887),
            Color(0xFF46039F),
            Color(0xFF7201A8),
            Color(0xFF9C179E),
            Color(0xFFBD3782),
            Color(0xFFD8576B),
            Color(0xFFED7953),
            Color(0xFFFA9E3B),
            Color(0xFFFDC926),
            Color(0xFFF0F921)
        )
    ),
    D3_MAGMA(
        "D3 Magma",
        listOf(
            Color(0xFF000004),
            Color(0xFF180F3E),
            Color(0xFF450F5F),
            Color(0xFF721F81),
            Color(0xFF9F2F7F),
            Color(0xFFCD4071),
            Color(0xFFF1605D),
            Color(0xFFFD9567),
            Color(0xFFFEC98D),
            Color(0xFFFCFDBF)
        )
    ),
    D3_SPECTRAL(
        "D3 Spectral",
        listOf(
            Color(0xFF9E0142),
            Color(0xFFD53E4F),
            Color(0xFFF46D43),
            Color(0xFFFDAE61),
            Color(0xFFFEE08B),
            Color(0xFFE6F598),
            Color(0xFFABDDA4),
            Color(0xFF66C2A5),
            Color(0xFF3288BD),
            Color(0xFF5E4FA2)
        )
    )
}

/**
 * Geo data point representing an Italian province with spatial and market sentiment data.
 */
data class ProvinceHeatmapPoint(
    val name: String,
    val code: String,
    val region: String,
    val macroArea: String, // "Nord", "Centro", "Sud", "Isole"
    val latitude: Double,
    val longitude: Double,
    val avgPriceSqM: Double,
    val minPriceSqM: Double,
    val maxPriceSqM: Double,
    val grossYieldPercent: Double,
    val marketSentimentScore: Int, // 0 - 100
    val sentimentLabel: String,
    val absorptionRate: Double,
    val avgDaysOnMarket: Int,
    val priceTrendYoY: Double,
    val rentTrendYoY: Double,
    val trackedDealsCount: Int = 0,
    val totalVolumeTracked: Double = 0.0,
    val topHotspot: String = "Centro Storico"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionalHeatmapScreen(
    viewModel: DealRadarViewModel,
    onNavigateBack: () -> Unit,
    onOpenDealDetail: ((PropertyDeal) -> Unit)? = null
) {
    val allDeals by viewModel.allDeals.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 } }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Province Database with accurate Geo coordinates and Scraped/OMI KPIs
    val provinces = remember(allDeals) {
        getEnrichedProvinceHeatmapData(allDeals)
    }

    // Controls and View States
    var selectedMetric by remember { mutableStateOf(HeatmapMetric.PRICE_DENSITY) }
    var selectedColorScheme by remember { mutableStateOf(D3ColorScheme.D3_TURBO) }
    var selectedMacroArea by remember { mutableStateOf("TUTTI") } // "TUTTI", "Nord", "Centro", "Sud", "Isole"
    var selectedProvince by remember { mutableStateOf<ProvinceHeatmapPoint?>(provinces.find { it.code == "MI" }) }
    var searchQuery by remember { mutableStateOf("") }
    var showContourRings by remember { mutableStateOf(true) }
    var showGridOverlay by remember { mutableStateOf(true) }
    var heatRadiusMultiplier by remember { mutableStateOf(1.0f) }
    var blurIntensity by remember { mutableStateOf(0.85f) }
    var isControlsExpanded by remember { mutableStateOf(false) }

    // Pulsing animation for selected point
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Filtered provinces for table / search
    val filteredProvinces = remember(provinces, selectedMacroArea, searchQuery) {
        provinces.filter { p ->
            val matchesArea = (selectedMacroArea == "TUTTI" || p.macroArea.equals(selectedMacroArea, ignoreCase = true))
            val matchesSearch = if (searchQuery.isBlank()) true else {
                p.name.contains(searchQuery, ignoreCase = true) ||
                        p.code.contains(searchQuery, ignoreCase = true) ||
                        p.region.contains(searchQuery, ignoreCase = true)
            }
            matchesArea && matchesSearch
        }
    }

    // Min & Max bounds for normalization
    val (minVal, maxVal) = remember(provinces, selectedMetric) {
        when (selectedMetric) {
            HeatmapMetric.PRICE_DENSITY -> (provinces.minOfOrNull { it.avgPriceSqM } ?: 800.0) to (provinces.maxOfOrNull { it.avgPriceSqM } ?: 5500.0)
            HeatmapMetric.GROSS_YIELD -> (provinces.minOfOrNull { it.grossYieldPercent } ?: 3.5) to (provinces.maxOfOrNull { it.grossYieldPercent } ?: 9.5)
            HeatmapMetric.MARKET_SENTIMENT -> 20.0 to 95.0
            HeatmapMetric.DEAL_OPPORTUNITY -> 0.0 to (provinces.maxOfOrNull { it.trackedDealsCount.toDouble() }?.coerceAtLeast(5.0) ?: 10.0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Heatmap Regionale D3",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BentoPurpleHeader,
                                border = BorderStroke(0.6.dp, BentoPurpleOnContainer)
                            ) {
                                Text(
                                    text = "D3.js Overlays",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BentoPurpleOnContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Densità prezzi, rendimenti medi & sentiment di mercato provinciale",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("regional_heatmap_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isControlsExpanded = !isControlsExpanded },
                        modifier = Modifier.testTag("toggle_heatmap_controls_btn")
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Configura Parametri D3",
                            tint = if (isControlsExpanded) BentoPurpleOnContainer else TextSecondaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSlateBg)
            )
        },
        containerColor = DarkSlateBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Metric Switcher Chips
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(HeatmapMetric.values()) { metric ->
                            val isSelected = (selectedMetric == metric)
                            Surface(
                                color = if (isSelected) BentoPurpleHeader else SurfaceCardDark,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder
                                ),
                                modifier = Modifier.clickable { selectedMetric = metric }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = metric.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) BentoPurpleOnContainer else TextSecondaryDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = metric.title,
                                            color = if (isSelected) BentoPurpleOnContainer else TextPrimaryDark,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = metric.unit,
                                            color = if (isSelected) BentoPurpleOnContainer.copy(alpha = 0.8f) else TextMutedDark,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Expandable D3 Shader / Interpolator Controls Panel
            item {
                AnimatedVisibility(
                    visible = isControlsExpanded,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Surface(
                        color = SurfaceCardDark,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "🎨 Configurazione Shader D3 & Densità",
                                    color = TextPrimaryDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Kernel Gaussiano Attivo",
                                    color = EmeraldGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Color Scheme Selector
                            Text("Scala Cromatica D3:", color = TextSecondaryDark, fontSize = 11.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(D3ColorScheme.values()) { scheme ->
                                    val isSelected = (selectedColorScheme == scheme)
                                    Surface(
                                        color = if (isSelected) BentoPurpleHeader else DarkSlateBg,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder
                                        ),
                                        modifier = Modifier.clickable { selectedColorScheme = scheme }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                scheme.label,
                                                color = if (isSelected) BentoPurpleOnContainer else TextSecondaryDark,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            // Palette bar preview
                                            Row(
                                                modifier = Modifier
                                                    .width(64.dp)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                            ) {
                                                scheme.colors.forEach { c ->
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxHeight()
                                                            .background(c)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Sliders for Heat Radius and Blur
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Raggio Kernel (${String.format(Locale.ITALY, "%.1fx", heatRadiusMultiplier)})", color = TextSecondaryDark, fontSize = 10.sp)
                                    Slider(
                                        value = heatRadiusMultiplier,
                                        onValueChange = { heatRadiusMultiplier = it },
                                        valueRange = 0.5f..2.2f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = BentoPurpleOnContainer,
                                            activeTrackColor = BentoPurpleOnContainer
                                        )
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Intensità Sfocatura (${(blurIntensity * 100).toInt()}%)", color = TextSecondaryDark, fontSize = 10.sp)
                                    Slider(
                                        value = blurIntensity,
                                        onValueChange = { blurIntensity = it },
                                        valueRange = 0.3f..1.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = CyanAccent,
                                            activeTrackColor = CyanAccent
                                        )
                                    )
                                }
                            }

                            // Toggles for Contour & Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Checkbox(
                                        checked = showContourRings,
                                        onCheckedChange = { showContourRings = it },
                                        colors = CheckboxDefaults.colors(checkedColor = BentoPurpleOnContainer)
                                    )
                                    Text("Anelli Iso-Densità", color = TextSecondaryDark, fontSize = 11.sp)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Checkbox(
                                        checked = showGridOverlay,
                                        onCheckedChange = { showGridOverlay = it },
                                        colors = CheckboxDefaults.colors(checkedColor = CyanAccent)
                                    )
                                    Text("Griglia Spaziale OMI", color = TextSecondaryDark, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Macro-Area Filter Tabs (All, North, Center, South, Islands)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("TUTTI", "Nord", "Centro", "Sud", "Isole").forEach { area ->
                        val isSelected = (selectedMacroArea == area)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMacroArea = area },
                            label = { Text(area, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BentoPurpleHeader,
                                selectedLabelColor = BentoPurpleOnContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder
                            )
                        )
                    }
                }
            }

            // ==========================================
            // INTERACTIVE D3 REGIONAL HEATMAP CANVAS
            // ==========================================
            item {
                Surface(
                    color = SurfaceCardDark,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SurfaceCardBorder),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .testTag("d3_regional_heatmap_canvas_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Canvas Header Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Mappa Termica Italia • ${selectedMetric.title}",
                                    color = TextPrimaryDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tocca una provincia per l'analisi dettagliata di rendimento",
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp
                                )
                            }

                            // Dynamic Color Legend
                            D3HeatmapLegend(
                                minVal = minVal,
                                maxVal = maxVal,
                                unit = selectedMetric.unit,
                                scheme = selectedColorScheme
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // The Interactive Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0D121D))
                        ) {
                            D3ItalyHeatmapCanvas(
                                provinces = provinces,
                                selectedProvince = selectedProvince,
                                selectedMetric = selectedMetric,
                                colorScheme = selectedColorScheme,
                                minVal = minVal,
                                maxVal = maxVal,
                                heatRadiusMultiplier = heatRadiusMultiplier,
                                blurIntensity = blurIntensity,
                                showContourRings = showContourRings,
                                showGridOverlay = showGridOverlay,
                                pulseScale = pulseScale,
                                pulseAlpha = pulseAlpha,
                                onSelectProvince = { selectedProvince = it }
                            )

                            // Floating Quick Zoom Reset / Compass badge
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Explore, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(12.dp))
                                    Text("Proiezione Mercator IT", color = TextSecondaryDark, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SELECTED PROVINCE DETAILED KPI CARD
            // ==========================================
            selectedProvince?.let { province ->
                item {
                    ProvinceDetailSentimentCard(
                        province = province,
                        allDeals = allDeals,
                        currencyFormat = currencyFormat,
                        onOpenDealDetail = onOpenDealDetail,
                        onSimulateRoi = {
                            val sampleDeal = allDeals.find { it.location.contains(province.name, ignoreCase = true) }
                            if (sampleDeal != null) {
                                onOpenDealDetail?.invoke(sampleDeal)
                            }
                        }
                    )
                }
            }

            // ==========================================
            // MARKET SENTIMENT & OPPORTUNITIES RANKING
            // ==========================================
            item {
                MarketSentimentRankingSection(
                    provinces = provinces,
                    selectedMetric = selectedMetric,
                    currencyFormat = currencyFormat,
                    onSelectProvince = { selectedProvince = it }
                )
            }

            // Search Bar for Province List
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Analisi di Dettaglio Province Italiane (${filteredProvinces.size})",
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filtra per provincia, regione o sigla...", color = TextMutedDark, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancella", tint = TextSecondaryDark)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoPurpleOnContainer,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedContainerColor = SurfaceCardDark,
                            unfocusedContainerColor = SurfaceCardDark,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("heatmap_province_search_input")
                    )
                }
            }

            // Province Data Table Rows
            items(filteredProvinces, key = { it.code }) { p ->
                ProvinceRowItem(
                    province = p,
                    isSelected = (selectedProvince?.code == p.code),
                    selectedMetric = selectedMetric,
                    currencyFormat = currencyFormat,
                    onClick = { selectedProvince = p }
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

/**
 * Custom Canvas drawing Italy's geospatial shape, continuous D3 Gaussian Heatmap gradient field,
 * contour iso-density rings, and interactive province pin markers.
 */
@Composable
fun D3ItalyHeatmapCanvas(
    provinces: List<ProvinceHeatmapPoint>,
    selectedProvince: ProvinceHeatmapPoint?,
    selectedMetric: HeatmapMetric,
    colorScheme: D3ColorScheme,
    minVal: Double,
    maxVal: Double,
    heatRadiusMultiplier: Float,
    blurIntensity: Float,
    showContourRings: Boolean,
    showGridOverlay: Boolean,
    pulseScale: Float,
    pulseAlpha: Float,
    onSelectProvince: (ProvinceHeatmapPoint) -> Unit
) {
    // Italy Bounding Box Coordinates calibrated for projection
    // Latitude: 36.4°N (Sicily) to 47.1°N (Alps)
    // Longitude: 6.6°E (Piemonte) to 18.5°E (Puglia)
    val minLat = 36.2
    val maxLat = 47.2
    val minLon = 6.4
    val maxLon = 18.6

    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Pre-allocated Paint objects to prevent thousands of GC allocations per second during rendering
    val selectedTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
        }
    }
    val unselectedTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = 22f
            isFakeBoldText = false
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 0f, 2f, android.graphics.Color.BLACK)
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(provinces, canvasSize) {
                detectTapGestures { tapOffset ->
                    if (canvasSize.width > 0 && canvasSize.height > 0) {
                        // Find closest province to tap
                        var closest: ProvinceHeatmapPoint? = null
                        var minDistance = Float.MAX_VALUE

                        provinces.forEach { p ->
                            val pt = projectGeoToCanvas(p.latitude, p.longitude, minLat, maxLat, minLon, maxLon, canvasSize)
                            val dist = sqrt((pt.x - tapOffset.x).pow(2) + (pt.y - tapOffset.y).pow(2))
                            if (dist < 45.dp.toPx() && dist < minDistance) {
                                minDistance = dist
                                closest = p
                            }
                        }

                        closest?.let { onSelectProvince(it) }
                    }
                }
            }
    ) {
        canvasSize = size
        val w = size.width
        val h = size.height

        if (w <= 0 || h <= 0) return@Canvas

        // 1. Draw subtle background coordinate grid
        if (showGridOverlay) {
            val gridPaint = Color(0xFF1E2638).copy(alpha = 0.4f)
            val stepX = w / 8f
            val stepY = h / 10f
            for (i in 1..8) {
                drawLine(gridPaint, Offset(i * stepX, 0f), Offset(i * stepX, h), strokeWidth = 0.8f)
            }
            for (j in 1..10) {
                drawLine(gridPaint, Offset(0f, j * stepY), Offset(w, j * stepY), strokeWidth = 0.8f)
            }
        }

        // 2. Draw Italy Coastal Schematic Boundaries (North, Center, South, Sicily, Sardinia)
        drawItalySchematicOutline(this, minLat, maxLat, minLon, maxLon, w, h)

        // 3. Draw Continuous D3 Gaussian Heatmap Nodes & Iso-Density Fields
        provinces.forEach { p ->
            val pt = projectGeoToCanvas(p.latitude, p.longitude, minLat, maxLat, minLon, maxLon, size)
            val rawVal = when (selectedMetric) {
                HeatmapMetric.PRICE_DENSITY -> p.avgPriceSqM
                HeatmapMetric.GROSS_YIELD -> p.grossYieldPercent
                HeatmapMetric.MARKET_SENTIMENT -> p.marketSentimentScore.toDouble()
                HeatmapMetric.DEAL_OPPORTUNITY -> p.trackedDealsCount.toDouble()
            }

            val norm = ((rawVal - minVal) / (maxVal - minVal).coerceAtLeast(0.001)).coerceIn(0.0, 1.0).toFloat()
            val heatColor = interpolateD3Color(norm, colorScheme.colors)

            val baseRadius = (32.dp.toPx() + (norm * 28.dp.toPx())) * heatRadiusMultiplier

            // Radial Gradient Heat Blob
            val radialBrush = Brush.radialGradient(
                colors = listOf(
                    heatColor.copy(alpha = 0.45f * blurIntensity),
                    heatColor.copy(alpha = 0.22f * blurIntensity),
                    heatColor.copy(alpha = 0.06f * blurIntensity),
                    Color.Transparent
                ),
                center = pt,
                radius = baseRadius
            )
            drawCircle(
                brush = radialBrush,
                radius = baseRadius,
                center = pt
            )

            // Optional Contour Iso-Density Rings
            if (showContourRings && norm > 0.35f) {
                drawCircle(
                    color = heatColor.copy(alpha = 0.35f),
                    radius = baseRadius * 0.55f,
                    center = pt,
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
                )
            }
        }

        // 4. Draw Province Node Markers & Labels
        provinces.forEach { p ->
            val pt = projectGeoToCanvas(p.latitude, p.longitude, minLat, maxLat, minLon, maxLon, size)
            val isSelected = (selectedProvince?.code == p.code)

            val rawVal = when (selectedMetric) {
                HeatmapMetric.PRICE_DENSITY -> p.avgPriceSqM
                HeatmapMetric.GROSS_YIELD -> p.grossYieldPercent
                HeatmapMetric.MARKET_SENTIMENT -> p.marketSentimentScore.toDouble()
                HeatmapMetric.DEAL_OPPORTUNITY -> p.trackedDealsCount.toDouble()
            }
            val norm = ((rawVal - minVal) / (maxVal - minVal).coerceAtLeast(0.001)).coerceIn(0.0, 1.0).toFloat()
            val nodeColor = interpolateD3Color(norm, colorScheme.colors)

            if (isSelected) {
                // Animated Pulsing Ripple
                drawCircle(
                    color = BentoPurpleOnContainer.copy(alpha = pulseAlpha),
                    radius = 18.dp.toPx() * pulseScale,
                    center = pt
                )
                drawCircle(
                    color = BentoPurpleOnContainer,
                    radius = 8.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = pt
                )
            } else {
                // Regular Node
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = 6.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = nodeColor,
                    radius = 4.5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 2.dp.toPx(),
                    center = pt
                )
            }

            // Province Code Badge & Metric Text
            drawIntoCanvas { canvas ->
                val paint = if (isSelected) selectedTextPaint else unselectedTextPaint
                canvas.nativeCanvas.drawText(p.code, pt.x, pt.y - 10.dp.toPx(), paint)
            }
        }
    }
}

/**
 * Projects Geo latitude and longitude into 2D Canvas space with proper Mercator aspect ratio.
 */
private fun projectGeoToCanvas(
    lat: Double,
    lon: Double,
    minLat: Double,
    maxLat: Double,
    minLon: Double,
    maxLon: Double,
    canvasSize: Size
): Offset {
    val paddingHorizontal = canvasSize.width * 0.08f
    val paddingVertical = canvasSize.height * 0.06f
    val usableWidth = canvasSize.width - (paddingHorizontal * 2)
    val usableHeight = canvasSize.height - (paddingVertical * 2)

    val xNorm = ((lon - minLon) / (maxLon - minLon)).coerceIn(0.0, 1.0)
    // Invert Y because latitude goes South -> North (increasing), whereas Canvas Y goes Top -> Down
    val yNorm = 1.0 - ((lat - minLat) / (maxLat - minLat)).coerceIn(0.0, 1.0)

    val x = paddingHorizontal + (xNorm * usableWidth).toFloat()
    val y = paddingVertical + (yNorm * usableHeight).toFloat()

    return Offset(x, y)
}

/**
 * Draws schematic coastlines & regions for the Italian peninsula, Sicily, and Sardinia.
 */
private fun drawItalySchematicOutline(
    scope: DrawScope,
    minLat: Double,
    maxLat: Double,
    minLon: Double,
    maxLon: Double,
    w: Float,
    h: Float
) {
    val outlineColor = Color(0xFF26334D).copy(alpha = 0.55f)
    val strokeWidth = with(scope) { 1.5.dp.toPx() }

    // 1. Northern Alpine Arc (Turin -> Milan -> Verona -> Venice -> Trieste)
    val northPath = Path().apply {
        val p1 = projectGeoToCanvas(45.1, 7.6, minLat, maxLat, minLon, maxLon, Size(w, h)) // TO
        val p2 = projectGeoToCanvas(45.9, 8.5, minLat, maxLat, minLon, maxLon, Size(w, h)) // Alps N
        val p3 = projectGeoToCanvas(46.1, 11.1, minLat, maxLat, minLon, maxLon, Size(w, h)) // Trento
        val p4 = projectGeoToCanvas(45.6, 13.8, minLat, maxLat, minLon, maxLon, Size(w, h)) // Trieste
        val p5 = projectGeoToCanvas(45.4, 12.3, minLat, maxLat, minLon, maxLon, Size(w, h)) // Venice
        val p6 = projectGeoToCanvas(44.4, 8.9, minLat, maxLat, minLon, maxLon, Size(w, h)) // Genoa
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        lineTo(p5.x, p5.y)
        lineTo(p6.x, p6.y)
        close()
    }
    scope.drawPath(northPath, outlineColor, style = Stroke(width = strokeWidth))

    // 2. Peninsula Spine (Genoa -> Florence -> Rome -> Naples -> Calabria -> Puglia -> Ancona -> Bologna)
    val peninsulaPath = Path().apply {
        val p1 = projectGeoToCanvas(44.4, 8.9, minLat, maxLat, minLon, maxLon, Size(w, h)) // Genoa
        val p2 = projectGeoToCanvas(43.8, 11.2, minLat, maxLat, minLon, maxLon, Size(w, h)) // Firenze
        val p3 = projectGeoToCanvas(41.9, 12.5, minLat, maxLat, minLon, maxLon, Size(w, h)) // Rome
        val p4 = projectGeoToCanvas(40.8, 14.2, minLat, maxLat, minLon, maxLon, Size(w, h)) // Naples
        val p5 = projectGeoToCanvas(38.1, 15.6, minLat, maxLat, minLon, maxLon, Size(w, h)) // Reggio Cal
        val p6 = projectGeoToCanvas(40.5, 17.2, minLat, maxLat, minLon, maxLon, Size(w, h)) // Taranto
        val p7 = projectGeoToCanvas(41.1, 16.8, minLat, maxLat, minLon, maxLon, Size(w, h)) // Bari
        val p8 = projectGeoToCanvas(42.4, 14.2, minLat, maxLat, minLon, maxLon, Size(w, h)) // Pescara
        val p9 = projectGeoToCanvas(43.6, 13.5, minLat, maxLat, minLon, maxLon, Size(w, h)) // Ancona
        val p10 = projectGeoToCanvas(44.5, 11.3, minLat, maxLat, minLon, maxLon, Size(w, h)) // Bologna
        val p11 = projectGeoToCanvas(44.2, 10.3, minLat, maxLat, minLon, maxLon, Size(w, h)) // La Spezia

        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        lineTo(p5.x, p5.y)
        lineTo(p6.x, p6.y)
        lineTo(p7.x, p7.y)
        lineTo(p8.x, p8.y)
        lineTo(p9.x, p9.y)
        lineTo(p10.x, p10.y)
        lineTo(p11.x, p11.y)
        close()
    }
    scope.drawPath(peninsulaPath, outlineColor, style = Stroke(width = strokeWidth))

    // 3. Sicily
    val sicilyPath = Path().apply {
        val s1 = projectGeoToCanvas(38.1, 13.3, minLat, maxLat, minLon, maxLon, Size(w, h)) // Palermo
        val s2 = projectGeoToCanvas(38.2, 15.5, minLat, maxLat, minLon, maxLon, Size(w, h)) // Messina
        val s3 = projectGeoToCanvas(37.5, 15.1, minLat, maxLat, minLon, maxLon, Size(w, h)) // Catania
        val s4 = projectGeoToCanvas(36.9, 14.7, minLat, maxLat, minLon, maxLon, Size(w, h)) // Ragusa
        val s5 = projectGeoToCanvas(37.8, 12.4, minLat, maxLat, minLon, maxLon, Size(w, h)) // Trapani
        moveTo(s1.x, s1.y)
        lineTo(s2.x, s2.y)
        lineTo(s3.x, s3.y)
        lineTo(s4.x, s4.y)
        lineTo(s5.x, s5.y)
        close()
    }
    scope.drawPath(sicilyPath, outlineColor, style = Stroke(width = strokeWidth))

    // 4. Sardinia
    val sardiniaPath = Path().apply {
        val sa1 = projectGeoToCanvas(40.9, 9.5, minLat, maxLat, minLon, maxLon, Size(w, h)) // Olbia
        val sa2 = projectGeoToCanvas(39.2, 9.1, minLat, maxLat, minLon, maxLon, Size(w, h)) // Cagliari
        val sa3 = projectGeoToCanvas(39.9, 8.6, minLat, maxLat, minLon, maxLon, Size(w, h)) // Oristano
        val sa4 = projectGeoToCanvas(40.7, 8.5, minLat, maxLat, minLon, maxLon, Size(w, h)) // Sassari
        moveTo(sa1.x, sa1.y)
        lineTo(sa2.x, sa2.y)
        lineTo(sa3.x, sa3.y)
        lineTo(sa4.x, sa4.y)
        close()
    }
    scope.drawPath(sardiniaPath, outlineColor, style = Stroke(width = strokeWidth))
}

/**
 * Multi-stop linear color interpolator supporting custom D3 palettes.
 */
fun interpolateD3Color(fraction: Float, colors: List<Color>): Color {
    if (colors.isEmpty()) return Color.Cyan
    if (colors.size == 1) return colors[0]

    val clamped = fraction.coerceIn(0f, 1f)
    val segments = colors.size - 1
    val scaled = clamped * segments
    val index = min(scaled.toInt(), segments - 1)
    val subFraction = scaled - index

    val c1 = colors[index]
    val c2 = colors[index + 1]

    val r = c1.red + (c2.red - c1.red) * subFraction
    val g = c1.green + (c2.green - c1.green) * subFraction
    val b = c1.blue + (c2.blue - c1.blue) * subFraction
    val a = c1.alpha + (c2.alpha - c1.alpha) * subFraction

    return Color(r, g, b, a)
}

/**
 * Color Legend for the D3 Heatmap.
 */
@Composable
fun D3HeatmapLegend(
    minVal: Double,
    maxVal: Double,
    unit: String,
    scheme: D3ColorScheme
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier
                .width(110.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            scheme.colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
        Row(
            modifier = Modifier.width(110.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (unit == "%") String.format(Locale.ITALY, "%.1f%%", minVal) else "${minVal.toInt()} $unit",
                color = TextSecondaryDark,
                fontSize = 8.sp
            )
            Text(
                text = if (unit == "%") String.format(Locale.ITALY, "%.1f%%", maxVal) else "${maxVal.toInt()} $unit",
                color = TextSecondaryDark,
                fontSize = 8.sp
            )
        }
    }
}

/**
 * Detailed analysis card for the currently selected province.
 */
@Composable
fun ProvinceDetailSentimentCard(
    province: ProvinceHeatmapPoint,
    allDeals: List<PropertyDeal>,
    currencyFormat: NumberFormat,
    onOpenDealDetail: ((PropertyDeal) -> Unit)?,
    onSimulateRoi: () -> Unit
) {
    val matchingDeals = remember(province.name, allDeals) {
        allDeals.filter { it.location.contains(province.name, ignoreCase = true) || it.location.contains(province.code, ignoreCase = true) }
    }

    Surface(
        color = SurfaceCardDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BentoPurpleOnContainer),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag("selected_province_sentiment_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Province Header with Sentiment Badge
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BentoPurpleHeader),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = province.code,
                            color = BentoPurpleOnContainer,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column {
                        Text(
                            text = "${province.name} (${province.region})",
                            color = TextPrimaryDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Macro-Area: ${province.macroArea} • Hotspot: ${province.topHotspot}",
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }
                }

                // Sentiment Score Badge
                Surface(
                    color = when {
                        province.marketSentimentScore >= 75 -> EmeraldGreen.copy(alpha = 0.2f)
                        province.marketSentimentScore >= 50 -> CyanAccent.copy(alpha = 0.2f)
                        else -> AmberGold.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        0.8.dp,
                        when {
                            province.marketSentimentScore >= 75 -> EmeraldGreen
                            province.marketSentimentScore >= 50 -> CyanAccent
                            else -> AmberGold
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${province.marketSentimentScore}/100",
                            color = if (province.marketSentimentScore >= 75) EmeraldGreen else CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = province.sentimentLabel,
                            color = TextPrimaryDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = SurfaceCardBorder, thickness = 0.8.dp)

            // 4-Quadrant Metric Summary Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Avg Sale Price
                ProvinceKpiMiniTile(
                    title = "Prezzo Medio Vendita",
                    value = "${currencyFormat.format(province.avgPriceSqM)}/m²",
                    subtext = "Range: ${currencyFormat.format(province.minPriceSqM)} - ${currencyFormat.format(province.maxPriceSqM)}",
                    trend = province.priceTrendYoY,
                    accentColor = AmberGold,
                    modifier = Modifier.weight(1f)
                )

                // 2. Gross Rental Yield
                ProvinceKpiMiniTile(
                    title = "Rendimento Lordo (Cap)",
                    value = String.format(Locale.ITALY, "%.1f%%", province.grossYieldPercent),
                    subtext = "Locazione: +${String.format(Locale.ITALY, "%.1f%%", province.rentTrendYoY)} YoY",
                    trend = province.rentTrendYoY,
                    accentColor = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 3. Days on Market & Liquidity
                ProvinceKpiMiniTile(
                    title = "Tempo di Vendita",
                    value = "${province.avgDaysOnMarket} giorni",
                    subtext = "Assorbimento: ${String.format(Locale.ITALY, "%.0f%%", province.absorptionRate)}",
                    trend = -2.5,
                    accentColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                )

                // 4. Tracked Deals in Radar
                ProvinceKpiMiniTile(
                    title = "Affari a Radar",
                    value = "${matchingDeals.size} Immobili",
                    subtext = "Massa: ${currencyFormat.format(matchingDeals.sumOf { it.askingPrice })}",
                    trend = 0.0,
                    accentColor = BentoPurpleOnContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick Actions: Open Deals or Filter
            if (matchingDeals.isNotEmpty()) {
                Text(
                    text = "Affari monitorati in quest'area:",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(matchingDeals.take(4)) { deal ->
                        Surface(
                            color = DarkSlateBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SurfaceCardBorder),
                            modifier = Modifier
                                .width(200.dp)
                                .clickable { onOpenDealDetail?.invoke(deal) }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = deal.title,
                                    color = TextPrimaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currencyFormat.format(deal.askingPrice),
                                        color = AmberGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (deal.discountPercent > 0) {
                                        Text(
                                            text = "-${deal.discountPercent}%",
                                            color = RoseRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProvinceKpiMiniTile(
    title: String,
    value: String,
    subtext: String,
    trend: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSlateBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.8.dp, SurfaceCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, color = TextSecondaryDark, fontSize = 10.sp)
            Text(value, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (trend != 0.0) {
                    Icon(
                        imageVector = if (trend > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (trend > 0) EmeraldGreen else RoseRed,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Text(
                    text = subtext,
                    color = TextMutedDark,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Top Sentiment Opportunities & Market Sentiment Ranking Section.
 */
@Composable
fun MarketSentimentRankingSection(
    provinces: List<ProvinceHeatmapPoint>,
    selectedMetric: HeatmapMetric,
    currencyFormat: NumberFormat,
    onSelectProvince: (ProvinceHeatmapPoint) -> Unit
) {
    val topYieldProvinces = remember(provinces) {
        provinces.sortedByDescending { it.grossYieldPercent }.take(4)
    }
    val topCapitalGrowthProvinces = remember(provinces) {
        provinces.sortedByDescending { it.priceTrendYoY }.take(4)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "🏆 Classifiche Opportunità & Sentiment",
            color = TextPrimaryDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // High Yield Leaders
            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Percent, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                        Text("Top Yield (Cap Rate)", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    topYieldProvinces.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectProvince(p) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${p.name} (${p.code})", color = TextPrimaryDark, fontSize = 11.sp)
                            Text(String.format(Locale.ITALY, "%.1f%%", p.grossYieldPercent), color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Top Capital Growth
            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                        Text("Top Crescita Prezzi", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    topCapitalGrowthProvinces.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectProvince(p) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${p.name} (${p.code})", color = TextPrimaryDark, fontSize = 11.sp)
                            Text("+${String.format(Locale.ITALY, "%.1f%%", p.priceTrendYoY)}", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual row item in the Province comparison table.
 */
@Composable
fun ProvinceRowItem(
    province: ProvinceHeatmapPoint,
    isSelected: Boolean,
    selectedMetric: HeatmapMetric,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) BentoPurpleHeader else SurfaceCardDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clickable { onClick() }
            .testTag("province_row_${province.code}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) BentoPurpleOnContainer else DarkSlateBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = province.code,
                        color = if (isSelected) Color.White else TextPrimaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = province.name,
                        color = TextPrimaryDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${province.region} • ${province.macroArea}",
                        color = TextSecondaryDark,
                        fontSize = 10.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                when (selectedMetric) {
                    HeatmapMetric.PRICE_DENSITY -> {
                        Text(
                            text = "${currencyFormat.format(province.avgPriceSqM)}/m²",
                            color = AmberGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${if (province.priceTrendYoY >= 0) "+" else ""}${String.format(Locale.ITALY, "%.1f%%", province.priceTrendYoY)} YoY",
                            color = if (province.priceTrendYoY >= 0) EmeraldGreen else RoseRed,
                            fontSize = 10.sp
                        )
                    }
                    HeatmapMetric.GROSS_YIELD -> {
                        Text(
                            text = String.format(Locale.ITALY, "%.1f%%", province.grossYieldPercent),
                            color = EmeraldGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Locazione: ${currencyFormat.format(province.avgPriceSqM * 0.007)}/m²/m",
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }
                    HeatmapMetric.MARKET_SENTIMENT -> {
                        Text(
                            text = "${province.marketSentimentScore}/100",
                            color = CyanAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = province.sentimentLabel,
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }
                    HeatmapMetric.DEAL_OPPORTUNITY -> {
                        Text(
                            text = "${province.trackedDealsCount} affari",
                            color = BentoPurpleOnContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${province.avgDaysOnMarket} gg medi",
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Builds the comprehensive dataset of Italian provinces enriched with spatial geo-coordinates,
 * OMI price density, rental yields, and tracked deal counts from the database.
 */
fun getEnrichedProvinceHeatmapData(allDeals: List<PropertyDeal>): List<ProvinceHeatmapPoint> {
    val rawProvinces = listOf(
        // NORTH
        ProvinceHeatmapPoint("Milano", "MI", "Lombardia", "Nord", 45.4642, 9.1900, 5420.0, 3200.0, 11500.0, 5.4, 94, "🔥 Hot High Demand", 88.0, 52, 4.8, 8.2, topHotspot = "Brera / Duomo"),
        ProvinceHeatmapPoint("Monza", "MB", "Lombardia", "Nord", 45.5845, 9.2744, 2950.0, 1800.0, 4800.0, 6.2, 85, "🔥 Fortissima Domanda", 82.0, 68, 3.9, 6.5, topHotspot = "Parco / Centro"),
        ProvinceHeatmapPoint("Bergamo", "BG", "Lombardia", "Nord", 45.6983, 9.6773, 2450.0, 1400.0, 4200.0, 6.8, 82, "📈 Rendimento Solido", 78.0, 75, 3.2, 5.4, topHotspot = "Città Alta"),
        ProvinceHeatmapPoint("Brescia", "BS", "Lombardia", "Nord", 45.5416, 10.2118, 2180.0, 1200.0, 3800.0, 7.1, 79, "📈 Buona Liquidità", 76.0, 80, 2.8, 5.1, topHotspot = "Centro Storico"),
        ProvinceHeatmapPoint("Torino", "TO", "Piemonte", "Nord", 45.0703, 7.6869, 2050.0, 1100.0, 4100.0, 6.9, 81, "💎 High Yield Opportunity", 74.0, 85, 2.5, 6.1, topHotspot = "Crocetta / San Salvario"),
        ProvinceHeatmapPoint("Novara", "NO", "Piemonte", "Nord", 45.4469, 8.6210, 1420.0, 850.0, 2600.0, 7.8, 70, "⚖️ Mercato Stabile", 68.0, 95, 1.8, 3.8, topHotspot = "Centro Storico"),
        ProvinceHeatmapPoint("Genova", "GE", "Liguria", "Nord", 44.4056, 8.9463, 1680.0, 900.0, 3900.0, 7.4, 68, "🏷️ Buyer Discount Zone", 62.0, 115, 0.9, 3.2, topHotspot = "Albaro / Nervi"),
        ProvinceHeatmapPoint("Verona", "VR", "Veneto", "Nord", 45.4384, 10.9916, 2680.0, 1500.0, 4900.0, 6.3, 84, "🔥 Alta Attrattività Turistica", 80.0, 65, 3.8, 7.0, topHotspot = "Borgo Trento"),
        ProvinceHeatmapPoint("Venezia", "VE", "Veneto", "Nord", 45.4408, 12.3155, 3350.0, 1800.0, 7500.0, 6.0, 86, "🏛️ Premium Turistico", 78.0, 70, 3.1, 8.5, topHotspot = "San Marco / Mestre Centro"),
        ProvinceHeatmapPoint("Padova", "PD", "Veneto", "Nord", 45.4064, 11.8768, 2350.0, 1350.0, 4200.0, 6.7, 82, "📈 Polo Universitario", 79.0, 72, 3.4, 6.8, topHotspot = "Prato della Valle"),
        ProvinceHeatmapPoint("Trento", "TN", "Trentino-Alto Adige", "Nord", 46.0748, 11.1217, 3450.0, 2100.0, 5800.0, 5.2, 88, "🛡️ Alta Difensività", 85.0, 60, 4.2, 5.8, topHotspot = "Centro / Piedicastello"),
        ProvinceHeatmapPoint("Bolzano", "BZ", "Trentino-Alto Adige", "Nord", 46.4983, 11.3548, 4850.0, 3100.0, 8200.0, 4.8, 90, "💎 Premium Capital", 89.0, 55, 4.6, 5.5, topHotspot = "Gries / Centro"),
        ProvinceHeatmapPoint("Trieste", "TS", "Friuli-Venezia Giulia", "Nord", 45.6495, 13.7768, 2250.0, 1250.0, 3900.0, 6.8, 78, "📈 Crescita Transfrontaliera", 75.0, 82, 3.5, 6.2, topHotspot = "Barcola / Rive"),

        // CENTER
        ProvinceHeatmapPoint("Bologna", "BO", "Emilia-Romagna", "Centro", 44.4949, 11.3426, 3550.0, 2200.0, 5900.0, 6.2, 92, "🔥 Fortissima Domanda Locativa", 87.0, 58, 4.5, 9.1, topHotspot = "Santo Stefano / Saragozza"),
        ProvinceHeatmapPoint("Modena", "MO", "Emilia-Romagna", "Centro", 44.6471, 10.9252, 2380.0, 1400.0, 3900.0, 6.8, 80, "📈 Distretto Industriale Solido", 76.0, 75, 3.0, 5.2, topHotspot = "Centro / Buon Pastore"),
        ProvinceHeatmapPoint("Parma", "PR", "Emilia-Romagna", "Centro", 44.8015, 10.3279, 2420.0, 1450.0, 3950.0, 6.7, 79, "📈 Alta Qualità della Vita", 75.0, 78, 2.9, 5.0, topHotspot = "Cittadella / Barriera"),
        ProvinceHeatmapPoint("Firenze", "FI", "Toscana", "Centro", 43.7696, 11.2558, 4150.0, 2500.0, 8500.0, 5.6, 89, "🏛️ Premium Storico & Turistico", 84.0, 62, 4.1, 7.8, topHotspot = "Centro / Campo di Marte"),
        ProvinceHeatmapPoint("Pisa", "PI", "Toscana", "Centro", 43.7228, 10.4017, 2480.0, 1500.0, 3800.0, 6.9, 78, "📈 Hub Universitario", 74.0, 80, 2.6, 6.4, topHotspot = "Santa Maria / San Francesco"),
        ProvinceHeatmapPoint("Roma", "RM", "Lazio", "Centro", 41.9028, 12.4964, 3350.0, 1750.0, 9200.0, 5.9, 91, "🔥 Grande Mercato Istituzionale", 86.0, 64, 3.5, 7.4, topHotspot = "Trastevere / Parioli / Prati"),
        ProvinceHeatmapPoint("Latina", "LT", "Lazio", "Centro", 41.4676, 12.9036, 1750.0, 1100.0, 3100.0, 7.1, 68, "⚖️ Mercato Residenziale", 65.0, 105, 1.2, 3.5, topHotspot = "Centro / Q4-Q5"),
        ProvinceHeatmapPoint("Perugia", "PG", "Umbria", "Centro", 43.1107, 12.3908, 1380.0, 850.0, 2500.0, 7.4, 69, "🎓 Polo Studenti Stranieri", 66.0, 110, 1.4, 4.2, topHotspot = "Elce / Centro"),
        ProvinceHeatmapPoint("Ancona", "AN", "Marche", "Centro", 43.6158, 13.5189, 1620.0, 950.0, 3200.0, 7.2, 71, "⚖️ Polo Portuale Adriatico", 69.0, 100, 1.6, 3.9, topHotspot = "Passetto / Centro"),

        // SOUTH
        ProvinceHeatmapPoint("Napoli", "NA", "Campania", "Sud", 40.8518, 14.2681, 2580.0, 1200.0, 6800.0, 6.5, 87, "🔥 Boom Turistico & B&B", 83.0, 69, 4.2, 8.8, topHotspot = "Chiaia / Vomero / Centro Storico"),
        ProvinceHeatmapPoint("Salerno", "SA", "Campania", "Sud", 40.6824, 14.7681, 2350.0, 1300.0, 4600.0, 6.6, 78, "📈 Costiera & Luci d'Artista", 75.0, 80, 3.1, 6.0, topHotspot = "Carmine / Lungomare"),
        ProvinceHeatmapPoint("Bari", "BA", "Puglia", "Sud", 41.1171, 16.8719, 2150.0, 1200.0, 4200.0, 6.9, 83, "🔥 Hub Tecnologico & Turistico", 78.0, 76, 3.6, 6.7, topHotspot = "Murat / Poggiofranco"),
        ProvinceHeatmapPoint("Lecce", "LE", "Puglia", "Sud", 40.3548, 18.1724, 1480.0, 850.0, 2700.0, 7.6, 76, "🏛️ Barocco & Salento", 71.0, 92, 2.8, 5.9, topHotspot = "Centro Storico / Mazzini"),
        ProvinceHeatmapPoint("Taranto", "TA", "Puglia", "Sud", 40.4644, 17.2470, 980.0, 550.0, 1850.0, 8.9, 62, "🏷️ Max Yield & Sconti Elevati", 58.0, 135, 0.5, 2.8, topHotspot = "Borgo Nuovo"),
        ProvinceHeatmapPoint("Pescara", "PE", "Abruzzo", "Sud", 42.4618, 14.2161, 1820.0, 1100.0, 3400.0, 7.0, 75, "🏖️ Residenziale Balneare", 72.0, 88, 2.2, 4.5, topHotspot = "Piazza Salotto / Riviera"),
        ProvinceHeatmapPoint("Reggio Calabria", "RC", "Calabria", "Sud", 38.1113, 15.6473, 920.0, 520.0, 1800.0, 8.6, 58, "🏷️ Sconto Distressed Elevato", 52.0, 145, 0.2, 2.1, topHotspot = "Via Marina / Centro"),

        // ISLANDS
        ProvinceHeatmapPoint("Palermo", "PA", "Sicilia", "Isole", 38.1157, 13.3615, 1480.0, 800.0, 3400.0, 7.8, 77, "💎 Ottimo Rendimento Aste", 73.0, 90, 2.9, 6.5, topHotspot = "Politeama / Libertà"),
        ProvinceHeatmapPoint("Catania", "CT", "Sicilia", "Isole", 37.5079, 15.0873, 1320.0, 750.0, 2900.0, 8.4, 75, "⚡ Etna Valley & Hub Studenti", 70.0, 95, 3.1, 7.2, topHotspot = "Corso Italia / Centro"),
        ProvinceHeatmapPoint("Messina", "ME", "Sicilia", "Isole", 38.1938, 15.5540, 1180.0, 680.0, 2400.0, 8.1, 64, "🏷️ Prezzi Competitivi", 60.0, 120, 1.1, 3.5, topHotspot = "Viale San Martino"),
        ProvinceHeatmapPoint("Cagliari", "CA", "Sardegna", "Isole", 39.2238, 9.1217, 2450.0, 1400.0, 4400.0, 6.6, 81, "🏖️ Capoluogo Isolano Vivace", 77.0, 78, 3.3, 5.8, topHotspot = "Castello / Poetto"),
        ProvinceHeatmapPoint("Sassari", "SS", "Sardegna", "Isole", 40.7259, 8.5556, 1450.0, 850.0, 2600.0, 7.5, 68, "⚖️ Residenziale Stabile", 64.0, 105, 1.5, 3.9, topHotspot = "Centro / Luna e Sole")
    )

    // Enrich with actual deals from DB
    return rawProvinces.map { province ->
        val dealsInProvince = allDeals.filter {
            it.location.contains(province.name, ignoreCase = true) ||
                    it.location.contains(province.code, ignoreCase = true)
        }
        val count = dealsInProvince.size
        val totalVol = dealsInProvince.sumOf { it.askingPrice }

        province.copy(
            trackedDealsCount = count,
            totalVolumeTracked = totalVol
        )
    }
}
