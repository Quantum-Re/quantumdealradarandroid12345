package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Property
import com.example.data.PropertyDeal
import com.example.util.GeminiAiHubService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberTerminalScreen(
    deals: List<PropertyDeal>,
    onBackClick: () -> Unit,
    onAcquireDeal: (Property) -> Unit,
    onOpenRoiCalculator: (PropertyDeal) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var ludicrousMode by remember { mutableStateOf(true) }
    var selectedDealIndex by remember { mutableIntStateOf(0) }

    val activeDeal = remember(deals, selectedDealIndex) {
        if (deals.isNotEmpty()) deals[selectedDealIndex.coerceIn(0, deals.size - 1)] else null
    }

    // First Principles Physics & Cost Tuning State
    var materialOptimizationPercent by remember { mutableFloatStateOf(15f) }
    var energyStorageUnits by remember { mutableIntStateOf(2) } // Powerwalls
    var evChargerStalls by remember { mutableIntStateOf(2) }

    // Hardcore Mode Stress-Testing Sliders
    var rateShockBps by remember { mutableFloatStateOf(300f) } // +3.0%
    var marketPriceDropPercent by remember { mutableFloatStateOf(20f) } // -20%
    var materialInflationPercent by remember { mutableFloatStateOf(25f) } // +25%
    var vacancyMonths by remember { mutableFloatStateOf(4f) } // 4 months

    // AI First Principles Deep Audit State
    var isRunningCyberAudit by remember { mutableStateOf(false) }
    var cyberAuditReport by remember { mutableStateOf<String?>(null) }
    var auditSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Colors: Cyber Dark Obsidian, Neon Amber, Electric Cyan, Laser Green
    val cyberBg = if (ludicrousMode) Color(0xFF0D0F14) else Color(0xFF16181F)
    val cyberCardBg = if (ludicrousMode) Color(0xFF131720) else Color(0xFF1F2430)
    val cyberBorder = if (ludicrousMode) Color(0xFF00E5FF).copy(alpha = 0.35f) else Color(0xFF374151)
    val neonCyan = Color(0xFF00E5FF)
    val neonAmber = Color(0xFFFF9100)
    val laserGreen = Color(0xFF00E676)
    val dangerRed = Color(0xFFFF1744)

    // Pulsing glow animation for Ludicrous mode
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF0A0C10),
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1F2430), CircleShape)
                                    .testTag("cyber_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Indietro",
                                    tint = Color.White
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "CYBER TERMINAL",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = neonCyan,
                                        letterSpacing = 1.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (ludicrousMode) neonAmber.copy(alpha = 0.2f) else Color(0xFF374151),
                                        border = BorderStroke(
                                            1.dp,
                                            if (ludicrousMode) neonAmber.copy(alpha = glowAlpha) else Color.Gray
                                        )
                                    ) {
                                        Text(
                                            text = if (ludicrousMode) "⚡ LUDICROUS" else "STANDARD",
                                            color = if (ludicrousMode) neonAmber else Color.LightGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "FIRST-PRINCIPLES DEAL ENGINE // BLACK SWAN STRESS-TEST",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Ludicrous Mode Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (ludicrousMode) neonAmber else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Switch(
                                checked = ludicrousMode,
                                onCheckedChange = { ludicrousMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = neonAmber,
                                    checkedTrackColor = Color(0xFF3E2723),
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color(0xFF263238)
                                ),
                                modifier = Modifier.testTag("ludicrous_mode_switch")
                            )
                        }
                    }
                }
            }
        },
        containerColor = cyberBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("cyber_terminal_content"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live Telemetry HUD Bar
            item {
                CyberTelemetryHud(
                    ludicrousMode = ludicrousMode,
                    neonCyan = neonCyan,
                    laserGreen = laserGreen,
                    neonAmber = neonAmber
                )
            }

            // 2. Deal Selector Ribbon
            item {
                Text(
                    text = "TARGET ASSET SELECTION [${deals.size} AVAILABLE]",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (deals.isNotEmpty()) {
                    ScrollableDealSelector(
                        deals = deals,
                        selectedIndex = selectedDealIndex,
                        onSelectIndex = { 
                            selectedDealIndex = it
                            cyberAuditReport = null
                        },
                        neonCyan = neonCyan,
                        cardBg = cyberCardBg
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = cyberCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, cyberBorder)
                    ) {
                        Text(
                            text = "Nessun deal disponibile nel database. Caricamento parametri predefiniti...",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (activeDeal != null) {
                val surfaceSqm = activeDeal.surfaceSqm.coerceAtLeast(45)
                val askingPrice = activeDeal.askingPrice.coerceAtLeast(30000.0)
                val marketValue = if (activeDeal.estimatedMarketValue > 0) activeDeal.estimatedMarketValue else askingPrice * 1.35
                val pricePerSqm = askingPrice / surfaceSqm

                // Physics / Atomic Cost Math
                val rawMaterialCostSqm = 620.0 * (1.0 - (materialOptimizationPercent / 100.0 * 0.4))
                val laborAssemblyCostSqm = 430.0 * (1.0 - (materialOptimizationPercent / 100.0 * 0.3))
                val physicalCostTotal = (rawMaterialCostSqm + laborAssemblyCostSqm) * surfaceSqm
                val landLocationResidual = (askingPrice - physicalCostTotal).coerceAtLeast(10000.0)
                val bureaucraticFrictionTax = ((askingPrice - physicalCostTotal) / askingPrice * 100.0).coerceIn(5.0, 75.0)

                // Clean Energy & Solar Physics
                val solarRoofArea = (surfaceSqm * 0.55).coerceIn(20.0, 300.0)
                val annualSolarKwh = (solarRoofArea * 175.0).toInt() // kWh / year in sunny Italy
                val annualEnergySavingsEur = (annualSolarKwh * 0.28).toInt() // €0.28 / kWh
                val batteryStorageKwh = energyStorageUnits * 13.5 // Tesla Powerwall 13.5 kWh
                val evAnnualRevenueEur = evChargerStalls * 1450 // charging gross profit

                // Hardcore Stress Test Calculations
                val stressRateImpact = askingPrice * 0.75 * (rateShockBps / 10000.0) // extra debt interest/yr
                val distressedAssetValuePostShock = marketValue * (1.0 - (marketPriceDropPercent / 100.0))
                val shockSafetyMarginEur = (distressedAssetValuePostShock - askingPrice)
                val stressScore = (
                    (shockSafetyMarginEur / askingPrice * 50.0) +
                    (100.0 - (rateShockBps / 100.0 * 6.0)) +
                    (100.0 - (marketPriceDropPercent * 1.2)) +
                    (100.0 - (vacancyMonths * 4.0))
                ) / 3.2

                val clampedStressScore = stressScore.coerceIn(10.0, 99.9)

                // 3. Section: Atomic Physics & Raw Material Deconstruction
                item {
                    FirstPrinciplesDeconstructionCard(
                        deal = activeDeal,
                        surfaceSqm = surfaceSqm,
                        askingPrice = askingPrice,
                        pricePerSqm = pricePerSqm,
                        rawMaterialCostSqm = rawMaterialCostSqm,
                        laborAssemblyCostSqm = laborAssemblyCostSqm,
                        physicalCostTotal = physicalCostTotal,
                        landLocationResidual = landLocationResidual,
                        bureaucraticFrictionTax = bureaucraticFrictionTax,
                        materialOptimizationPercent = materialOptimizationPercent,
                        onOptimizationChange = { materialOptimizationPercent = it },
                        cardBg = cyberCardBg,
                        border = cyberBorder,
                        neonCyan = neonCyan,
                        neonAmber = neonAmber,
                        laserGreen = laserGreen
                    )
                }

                // 4. Section: Clean Energy Autonomy & Grid-Zero Yield
                item {
                    EnergyAutonomyCard(
                        solarRoofArea = solarRoofArea,
                        annualSolarKwh = annualSolarKwh,
                        annualEnergySavingsEur = annualEnergySavingsEur,
                        energyStorageUnits = energyStorageUnits,
                        batteryStorageKwh = batteryStorageKwh,
                        evChargerStalls = evChargerStalls,
                        evAnnualRevenueEur = evAnnualRevenueEur,
                        onStorageUnitsChange = { energyStorageUnits = it },
                        onEvStallsChange = { evChargerStalls = it },
                        cardBg = cyberCardBg,
                        border = cyberBorder,
                        neonCyan = neonCyan,
                        laserGreen = laserGreen
                    )
                }

                // 5. Section: Hardcore Mode (Black Swan Stress-Test)
                item {
                    HardcoreStressTestCard(
                        stressScore = clampedStressScore,
                        rateShockBps = rateShockBps,
                        marketPriceDropPercent = marketPriceDropPercent,
                        materialInflationPercent = materialInflationPercent,
                        vacancyMonths = vacancyMonths,
                        shockSafetyMarginEur = shockSafetyMarginEur,
                        stressRateImpact = stressRateImpact,
                        onRateShockChange = { rateShockBps = it },
                        onMarketDropChange = { marketPriceDropPercent = it },
                        onInflationChange = { materialInflationPercent = it },
                        onVacancyChange = { vacancyMonths = it },
                        cardBg = cyberCardBg,
                        border = cyberBorder,
                        neonAmber = neonAmber,
                        dangerRed = dangerRed,
                        laserGreen = laserGreen
                    )
                }

                // 6. Section: Optimus AI Telemetry Inspector (Gemini First Principles Deep Audit)
                item {
                    OptimusInspectorCard(
                        deal = activeDeal,
                        isRunningAudit = isRunningCyberAudit,
                        auditReport = cyberAuditReport,
                        onRunAudit = {
                            coroutineScope.launch {
                                isRunningCyberAudit = true
                                val res = GeminiAiHubService.performFirstPrinciplesCyberAudit(
                                    address = activeDeal.location.ifBlank { activeDeal.title },
                                    price = activeDeal.askingPrice,
                                    surfaceSqm = activeDeal.surfaceSqm,
                                    propertyType = activeDeal.propertyType
                                )
                                isRunningCyberAudit = false
                                if (res.isSuccess) {
                                    cyberAuditReport = res.getOrNull()
                                } else {
                                    cyberAuditReport = "Audit fallito: ${res.exceptionOrNull()?.localizedMessage}"
                                }
                            }
                        },
                        cardBg = cyberCardBg,
                        border = cyberBorder,
                        neonCyan = neonCyan,
                        neonAmber = neonAmber
                    )
                }

                // 7. Ludicrous Execution & Pipeline Action
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0F141C),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = neonAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "AUTONOMOUS EXECUTION & UNDERWRITING",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = "Porta istantaneamente questo asset nel tuo Pipeline con underwriting First-Principles pre-calcolato e avvia l'acquisizione a velocità massima.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )

                            if (auditSuccessMessage != null) {
                                Surface(
                                    color = Color(0xFF1B5E20).copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, laserGreen)
                                ) {
                                    Text(
                                        text = auditSuccessMessage!!,
                                        color = laserGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val newProperty = Property(
                                            title = "[CYBER] ${activeDeal.title}",
                                            address = activeDeal.location,
                                            price = activeDeal.askingPrice,
                                            estimatedMarketValue = activeDeal.estimatedMarketValue,
                                            estimatedRenovationCost = physicalCostTotal * 0.15,
                                            targetResalePrice = activeDeal.estimatedMarketValue * 1.05,
                                            projectedRentalIncome = (activeDeal.askingPrice * 0.0065),
                                            surfaceSqm = activeDeal.surfaceSqm,
                                            propertyType = activeDeal.propertyType,
                                            distressStatus = "First-Principles Validated",
                                            strategyTags = "Cyber / First-Principles",
                                            notes = "Underwritten via Cyber Terminal (Material Basis: €${physicalCostTotal.toInt()}, Clean Energy: ${annualSolarKwh} kWh/yr, Anti-Fragility: ${clampedStressScore.toInt()}/100)"
                                        )
                                        onAcquireDeal(newProperty)
                                        auditSuccessMessage = "✓ Immobile esportato con successo in 'I Miei Immobili'!"
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cyber_port_pipeline_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = neonCyan,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddHome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PORT TO PIPELINE",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onOpenRoiCalculator(activeDeal) },
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .testTag("cyber_open_roi_button"),
                                    border = BorderStroke(1.dp, neonAmber),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = neonAmber
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DEBT & ROI",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Cyber Telemetry HUD
// -------------------------------------------------------------
@Composable
private fun CyberTelemetryHud(
    ludicrousMode: Boolean,
    neonCyan: Color,
    laserGreen: Color,
    neonAmber: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0F1218),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (ludicrousMode) neonCyan.copy(alpha = 0.5f) else Color(0xFF374151))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COMPUTE VELOCITY",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = if (ludicrousMode) "3.84 TFLOPS // MAX" else "1.20 TFLOPS // ECO",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (ludicrousMode) neonCyan else Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NEURAL UNDERWRITER",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(laserGreen, CircleShape)
                    )
                    Text(
                        text = "ONLINE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = laserGreen
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "SATELLITE RADAR",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "STARLINK SYNCED",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = neonAmber
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Scrollable Deal Selector Ribbon
// -------------------------------------------------------------
@Composable
private fun ScrollableDealSelector(
    deals: List<PropertyDeal>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    neonCyan: Color,
    cardBg: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        deals.take(4).forEachIndexed { index, deal ->
            val isSelected = index == selectedIndex
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectIndex(index) }
                    .testTag("cyber_deal_select_$index"),
                color = if (isSelected) Color(0xFF1E293B) else cardBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) neonCyan else Color(0xFF334155)
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = deal.location.ifBlank { deal.title }.take(14),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) neonCyan else Color.White,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Text(
                        text = "€${(deal.askingPrice / 1000).toInt()}k",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "-${deal.discountPercent}% OFF",
                        fontSize = 9.sp,
                        color = Color(0xFF4ADE80),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: First-Principles Deconstruction Card
// -------------------------------------------------------------
@Composable
private fun FirstPrinciplesDeconstructionCard(
    deal: PropertyDeal,
    surfaceSqm: Int,
    askingPrice: Double,
    pricePerSqm: Double,
    rawMaterialCostSqm: Double,
    laborAssemblyCostSqm: Double,
    physicalCostTotal: Double,
    landLocationResidual: Double,
    bureaucraticFrictionTax: Double,
    materialOptimizationPercent: Float,
    onOptimizationChange: (Float) -> Unit,
    cardBg: Color,
    border: Color,
    neonCyan: Color,
    neonAmber: Color,
    laserGreen: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = neonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "1. ATOMIC PHYSICAL DECONSTRUCTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
                Text(
                    text = "€${pricePerSqm.toInt()}/m²",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = neonCyan
                )
            }

            Text(
                text = "Decomposizione del prezzo richiesto in pura fisica e materie prime di costruzione:",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )

            // Metrics grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C0E14), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• Materie Prime (Acciaio, Cemento, Vetro):",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "€${rawMaterialCostSqm.toInt()}/m²  [€${(rawMaterialCostSqm * surfaceSqm).toInt()}]",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• Assemblaggio & Manodopera Meccanica:",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "€${laborAssemblyCostSqm.toInt()}/m²  [€${(laborAssemblyCostSqm * surfaceSqm).toInt()}]",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• Valore Residuo Suolo & Posizione:",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "€${landLocationResidual.toInt()}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = neonAmber
                    )
                }

                HorizontalDivider(color = Color(0xFF263238), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TASSA BUROCRATICA / FRICTION TAX:",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8A80)
                    )
                    Text(
                        text = "${bureaucraticFrictionTax.toInt()}%",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF8A80)
                    )
                }
            }

            // Slider: Modular & Pre-fab Optimization
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ottimizzazione Modulare Prefabbricata:",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "+${materialOptimizationPercent.toInt()}% Efficienza",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = laserGreen
                    )
                }
                Slider(
                    value = materialOptimizationPercent,
                    onValueChange = onOptimizationChange,
                    valueRange = 0f..40f,
                    colors = SliderDefaults.colors(
                        thumbColor = laserGreen,
                        activeTrackColor = laserGreen,
                        inactiveTrackColor = Color(0xFF374151)
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Clean Energy Autonomy & Grid-Zero Card
// -------------------------------------------------------------
@Composable
private fun EnergyAutonomyCard(
    solarRoofArea: Double,
    annualSolarKwh: Int,
    annualEnergySavingsEur: Int,
    energyStorageUnits: Int,
    batteryStorageKwh: Double,
    evChargerStalls: Int,
    evAnnualRevenueEur: Int,
    onStorageUnitsChange: (Int) -> Unit,
    onEvStallsChange: (Int) -> Unit,
    cardBg: Color,
    border: Color,
    neonCyan: Color,
    laserGreen: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SolarPower,
                        contentDescription = null,
                        tint = laserGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "2. CLEAN ENERGY & GRID AUTONOMY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
                Text(
                    text = "NET ZERO ROI",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = laserGreen
                )
            }

            // Energy Yield Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF0C1318),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E3A2F))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "SOLAR OUTPUT", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Text(text = "$annualSolarKwh kWh/a", fontSize = 13.sp, fontWeight = FontWeight.Black, color = laserGreen, fontFamily = FontFamily.Monospace)
                        Text(text = "€$annualEnergySavingsEur/anno", fontSize = 10.sp, color = Color.LightGray)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF0C1318),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E3A2F))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "STORAGE ($energyStorageUnits PW)", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Text(text = "${batteryStorageKwh.toInt()} kWh", fontSize = 13.sp, fontWeight = FontWeight.Black, color = neonCyan, fontFamily = FontFamily.Monospace)
                        Text(text = "Arbitraggio Off-Peak", fontSize = 10.sp, color = Color.LightGray)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF0C1318),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E3A2F))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "EV CHARGING ($evChargerStalls)", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Text(text = "€$evAnnualRevenueEur/a", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD54F), fontFamily = FontFamily.Monospace)
                        Text(text = "Ricavi Extra", fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }

            // Quick Steppers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Powerwall Units:", fontSize = 11.sp, color = Color.LightGray)
                    IconButton(
                        onClick = { if (energyStorageUnits > 0) onStorageUnitsChange(energyStorageUnits - 1) },
                        modifier = Modifier.size(24.dp).background(Color(0xFF263238), CircleShape)
                    ) {
                        Text("-", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "$energyStorageUnits", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(
                        onClick = { if (energyStorageUnits < 6) onStorageUnitsChange(energyStorageUnits + 1) },
                        modifier = Modifier.size(24.dp).background(Color(0xFF263238), CircleShape)
                    ) {
                        Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "EV Stalls:", fontSize = 11.sp, color = Color.LightGray)
                    IconButton(
                        onClick = { if (evChargerStalls > 0) onEvStallsChange(evChargerStalls - 1) },
                        modifier = Modifier.size(24.dp).background(Color(0xFF263238), CircleShape)
                    ) {
                        Text("-", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "$evChargerStalls", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(
                        onClick = { if (evChargerStalls < 8) onEvStallsChange(evChargerStalls + 1) },
                        modifier = Modifier.size(24.dp).background(Color(0xFF263238), CircleShape)
                    ) {
                        Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Hardcore Stress-Test Matrix
// -------------------------------------------------------------
@Composable
private fun HardcoreStressTestCard(
    stressScore: Double,
    rateShockBps: Float,
    marketPriceDropPercent: Float,
    materialInflationPercent: Float,
    vacancyMonths: Float,
    shockSafetyMarginEur: Double,
    stressRateImpact: Double,
    onRateShockChange: (Float) -> Unit,
    onMarketDropChange: (Float) -> Unit,
    onInflationChange: (Float) -> Unit,
    onVacancyChange: (Float) -> Unit,
    cardBg: Color,
    border: Color,
    neonAmber: Color,
    dangerRed: Color,
    laserGreen: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = neonAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "3. HARDCORE BLACK SWAN STRESS-TEST",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (stressScore > 75) laserGreen.copy(alpha = 0.2f) else if (stressScore > 50) neonAmber.copy(alpha = 0.2f) else dangerRed.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (stressScore > 75) laserGreen else if (stressScore > 50) neonAmber else dangerRed)
                ) {
                    Text(
                        text = "SCORE: ${stressScore.toInt()} / 100",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = if (stressScore > 75) laserGreen else if (stressScore > 50) neonAmber else dangerRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Simulazione shock macroeconomico estremo (aumento tassi, crollo liquidità, inflazione cantieri):",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )

            // Sliders Grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Rate Shock
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Shock Tassi Interesse:", fontSize = 10.sp, color = Color.LightGray)
                    Text(text = "+${(rateShockBps / 100).toInt()}.${((rateShockBps % 100) / 10).toInt()}% bps", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = neonAmber)
                }
                Slider(
                    value = rateShockBps,
                    onValueChange = onRateShockChange,
                    valueRange = 0f..800f,
                    colors = SliderDefaults.colors(thumbColor = neonAmber, activeTrackColor = neonAmber)
                )

                // Market Liquidity Drop
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Crollo Valore Mercato / Liquidità:", fontSize = 10.sp, color = Color.LightGray)
                    Text(text = "-${marketPriceDropPercent.toInt()}%", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = dangerRed)
                }
                Slider(
                    value = marketPriceDropPercent,
                    onValueChange = onMarketDropChange,
                    valueRange = 0f..50f,
                    colors = SliderDefaults.colors(thumbColor = dangerRed, activeTrackColor = dangerRed)
                )

                // Vacancy Months
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Sfitto / Ritardo Esecuzione:", fontSize = 10.sp, color = Color.LightGray)
                    Text(text = "${vacancyMonths.toInt()} Mesi", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Slider(
                    value = vacancyMonths,
                    onValueChange = onVacancyChange,
                    valueRange = 0f..12f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.LightGray)
                )
            }

            // Results summary box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0A0D12),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF263238))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Margine di Sicurezza Post-Shock:", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = "€${shockSafetyMarginEur.toInt()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (shockSafetyMarginEur > 0) laserGreen else dangerRed
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Impatto Extra Debito:", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = "+€${stressRateImpact.toInt()}/anno",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = neonAmber
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Optimus AI Drone Inspector Card
// -------------------------------------------------------------
@Composable
private fun OptimusInspectorCard(
    deal: PropertyDeal,
    isRunningAudit: Boolean,
    auditReport: String?,
    onRunAudit: () -> Unit,
    cardBg: Color,
    border: Color,
    neonCyan: Color,
    neonAmber: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = neonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "4. OPTIMUS AI TELEMETRY INSPECTOR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                if (isRunningAudit) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = neonCyan
                    )
                }
            }

            Text(
                text = "Scansione autonoma First-Principles con Gemini AI (analisi strutturale atomica, potenziale retrofit e anti-fragilità):",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )

            if (auditReport == null) {
                Button(
                    onClick = onRunAudit,
                    enabled = !isRunningAudit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cyber_run_audit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = neonCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunningAudit) "ESECUZIONE SCANSIONE AI..." else "AVVIA AUDIT FIRST-PRINCIPLES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0A0D14),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF263238))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TELEMETRY AUDIT REPORT // GENERATED",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = neonAmber
                        )
                        Text(
                            text = auditReport,
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 16.sp
                        )
                        Button(
                            onClick = onRunAudit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(text = "Riesegui Audit", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
