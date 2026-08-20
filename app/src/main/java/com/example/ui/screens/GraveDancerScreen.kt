package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Property
import com.example.data.PropertyDeal
import com.example.util.GeminiAiHubService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraveDancerScreen(
    deals: List<PropertyDeal>,
    onBackClick: () -> Unit,
    onAcquireDeal: (Property) -> Unit,
    onOpenRoiCalculator: (PropertyDeal) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedDealIndex by remember { mutableIntStateOf(0) }

    // Filter deals to highlight maximum distress (Auctions, NPL, high discount)
    val distressedDeals = remember(deals) {
        val filtered = deals.filter {
            it.discountPercent >= 25 ||
            it.propertyType.contains("Asta", ignoreCase = true) ||
            it.title.contains("NPL", ignoreCase = true) ||
            it.notes.contains("fallimento", ignoreCase = true) ||
            it.notes.contains("stralcio", ignoreCase = true)
        }
        if (filtered.isNotEmpty()) filtered else deals
    }

    val activeDeal = remember(distressedDeals, selectedDealIndex) {
        if (distressedDeals.isNotEmpty()) distressedDeals[selectedDealIndex.coerceIn(0, distressedDeals.size - 1)] else null
    }

    // Sam Zell Underwriting Parameters
    var replacementCostSqmBenchmark by remember { mutableFloatStateOf(2850f) } // Standard new build replacement cost €/m²
    var supplyInelasticityScore by remember { mutableFloatStateOf(85f) } // 0 - 100
    var debtAmortizationYears by remember { mutableFloatStateOf(20f) } // Long term vs short term
    var stressVacancyTolerance by remember { mutableFloatStateOf(45f) } // % vacancy tolerance before cash bleed

    // Contrarian AI Memo State
    var isGeneratingMemo by remember { mutableStateOf(false) }
    var contrarianMemoText by remember { mutableStateOf<String?>(null) }
    var acquisitionSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Palette: Wall Street Contrarian Burgundy, Vintage Parchment, Deep Gold, Slate Navy
    val zellBurgundy = Color(0xFF4A0E17)
    val zellGold = Color(0xFFD4AF37)
    val zellCardBg = Color(0xFF1B181E)
    val zellBorder = Color(0xFFD4AF37).copy(alpha = 0.4f)
    val zellSurfaceDark = Color(0xFF121015)
    val zellGreen = Color(0xFF2E7D32)
    val zellRed = Color(0xFFC62828)

    val zellQuotes = listOf(
        "\"I dance on the skeletons of other people's mistakes — but only when the downside is completely protected.\"",
        "\"Look at the downside first. If you can live with the worst-case scenario, the upside will take care of itself.\"",
        "\"Supply is the only metric that truly matters. Demand is fickle, but supply constraints create monopolies.\"",
        "\"Never buy an asset above replacement cost. If nobody can build cheaper than your basis, you own the market.\"",
        "\"Liquidity equals freedom and survival in a real estate panic.\""
    )
    var quoteIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Surface(
                color = zellBurgundy,
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, zellGold.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                    .background(Color(0xFF2B0A10), CircleShape)
                                    .testTag("grave_dancer_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Indietro",
                                    tint = zellGold
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "THE GRAVE DANCER",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = zellGold,
                                        letterSpacing = 1.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = zellGold.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, zellGold)
                                    ) {
                                        Text(
                                            text = "SAM ZELL MODE",
                                            color = zellGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "CONTRARIAN DISTRESS & REPLACEMENT COST ARBITRAGE",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFF3E5F5).copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Skull / Contrarian Icon
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2B0A10),
                            border = BorderStroke(1.dp, zellGold),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Gavel,
                                    contentDescription = null,
                                    tint = zellGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = zellSurfaceDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("grave_dancer_content"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Sam Zell Maxim Ticker Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { quoteIndex = (quoteIndex + 1) % zellQuotes.size },
                    color = Color(0xFF22171E),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, zellGold.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = zellGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ZELL INVESTMENT MAXIM:",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = zellGold
                            )
                            Text(
                                text = zellQuotes[quoteIndex],
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFFEDE7F6),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // 2. Distressed Opportunities Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONTRARIAN DISTRESSED DEALS [${distressedDeals.size} OPPORTUNITIES]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = zellGold
                    )
                    Text(
                        text = "DEEP DISCOUNTS ONLY",
                        fontSize = 9.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (distressedDeals.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        distressedDeals.take(4).forEachIndexed { index, deal ->
                            val isSelected = index == selectedDealIndex
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedDealIndex = index
                                        contrarianMemoText = null
                                    }
                                    .testTag("grave_dancer_deal_$index"),
                                color = if (isSelected) zellBurgundy else zellCardBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) zellGold else Color(0xFF3E2723)
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
                                        color = if (isSelected) zellGold else Color.White,
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
                                        color = zellGold,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (activeDeal != null) {
                val surfaceSqm = activeDeal.surfaceSqm.coerceAtLeast(45)
                val askingPrice = activeDeal.askingPrice.coerceAtLeast(30000.0)
                val pricePerSqm = askingPrice / surfaceSqm
                val currentReplacementCostPerSqm = replacementCostSqmBenchmark.toDouble()
                val totalReplacementCost = currentReplacementCostPerSqm * surfaceSqm

                // Replacement Cost Arbitrage %
                val discountToReplacementPercent = ((totalReplacementCost - askingPrice) / totalReplacementCost * 100.0)
                val safetySpreadEur = (totalReplacementCost - askingPrice).coerceAtLeast(0.0)

                // Downside Protection & Breakeven Occupancy
                val estimatedGrossAnnualRent = askingPrice * 0.08
                val annualDebtAndOpEx = askingPrice * 0.70 * 0.055 + (surfaceSqm * 25.0) // 5.5% debt + €25/m2 OpEx
                val breakevenOccupancyRate = ((annualDebtAndOpEx / estimatedGrossAnnualRent) * 100.0).coerceIn(15.0, 95.0)

                // 3. Pillar 1: Replacement Cost Arbitrage Card
                item {
                    ReplacementCostArbitrageCard(
                        deal = activeDeal,
                        surfaceSqm = surfaceSqm,
                        askingPrice = askingPrice,
                        pricePerSqm = pricePerSqm,
                        replacementCostSqmBenchmark = replacementCostSqmBenchmark,
                        totalReplacementCost = totalReplacementCost,
                        discountToReplacementPercent = discountToReplacementPercent,
                        safetySpreadEur = safetySpreadEur,
                        onBenchmarkChange = { replacementCostSqmBenchmark = it },
                        cardBg = zellCardBg,
                        border = zellBorder,
                        gold = zellGold,
                        green = zellGreen,
                        red = zellRed
                    )
                }

                // 4. Pillar 2: Supply Moat & Inelasticity Radar
                item {
                    SupplyMoatCard(
                        deal = activeDeal,
                        supplyInelasticityScore = supplyInelasticityScore,
                        onScoreChange = { supplyInelasticityScore = it },
                        cardBg = zellCardBg,
                        border = zellBorder,
                        gold = zellGold
                    )
                }

                // 5. Pillar 3: Downside-First Debt & Liquidity Buffer
                item {
                    DownsideFirstCard(
                        breakevenOccupancyRate = breakevenOccupancyRate,
                        debtAmortizationYears = debtAmortizationYears,
                        stressVacancyTolerance = stressVacancyTolerance,
                        onAmortizationChange = { debtAmortizationYears = it },
                        onVacancyToleranceChange = { stressVacancyTolerance = it },
                        cardBg = zellCardBg,
                        border = zellBorder,
                        gold = zellGold,
                        green = zellGreen
                    )
                }

                // 6. Pillar 4: Sam Zell Contrarian Investment Memo (Gemini AI)
                item {
                    GraveDancerAiMemoCard(
                        deal = activeDeal,
                        isGenerating = isGeneratingMemo,
                        memoText = contrarianMemoText,
                        onGenerateMemo = {
                            coroutineScope.launch {
                                isGeneratingMemo = true
                                val res = GeminiAiHubService.performGraveDancerContrarianAudit(
                                    address = activeDeal.location.ifBlank { activeDeal.title },
                                    price = activeDeal.askingPrice,
                                    surfaceSqm = activeDeal.surfaceSqm,
                                    propertyType = activeDeal.propertyType,
                                    distressLevel = activeDeal.status.ifBlank { "Asta / Procedura Esecutiva" }
                                )
                                isGeneratingMemo = false
                                if (res.isSuccess) {
                                    contrarianMemoText = res.getOrNull()
                                } else {
                                    contrarianMemoText = "Audit fallito: ${res.exceptionOrNull()?.localizedMessage}"
                                }
                            }
                        },
                        cardBg = zellCardBg,
                        border = zellBorder,
                        gold = zellGold,
                        burgundy = zellBurgundy
                    )
                }

                // 7. Contrarian Acquisition Execution Button
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF24151C),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, zellGold)
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
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = zellGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "CONTRARIAN ACQUISITION PIPELINE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = "Acquisisci questo asset distressed con margine di sicurezza garantito rispetto al costo di rimpiazzo e aggiungilo al tuo portafoglio.",
                                fontSize = 11.sp,
                                color = Color(0xFFD1C4E9)
                            )

                            if (acquisitionSuccessMessage != null) {
                                Surface(
                                    color = zellGreen.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, zellGreen)
                                ) {
                                    Text(
                                        text = acquisitionSuccessMessage!!,
                                        color = Color(0xFFA5D6A7),
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
                                            title = "[ZELL CONTRARIAN] ${activeDeal.title}",
                                            address = activeDeal.location,
                                            price = activeDeal.askingPrice,
                                            estimatedMarketValue = totalReplacementCost,
                                            estimatedRenovationCost = surfaceSqm * 250.0,
                                            targetResalePrice = totalReplacementCost * 0.95,
                                            projectedRentalIncome = (activeDeal.askingPrice * 0.0075),
                                            surfaceSqm = activeDeal.surfaceSqm,
                                            propertyType = activeDeal.propertyType,
                                            distressStatus = "Replacement Cost Arbitrage (${discountToReplacementPercent.toInt()}% Off)",
                                            strategyTags = "Grave Dancer / Contrarian",
                                            notes = "Underwritten via Sam Zell Mode: Basis €${pricePerSqm.toInt()}/m² vs Replacement Cost €${replacementCostSqmBenchmark.toInt()}/m². Breakeven Occupancy: ${breakevenOccupancyRate.toInt()}%."
                                        )
                                        onAcquireDeal(newProperty)
                                        acquisitionSuccessMessage = "✓ Deal aggiunto al Portafoglio con Margin of Safety validato!"
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("grave_dancer_acquire_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = zellGold,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddBusiness,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ACQUIRE AT SPREAD",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onOpenRoiCalculator(activeDeal) },
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .testTag("grave_dancer_open_roi_button"),
                                    border = BorderStroke(1.dp, zellGold),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = zellGold
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
                                        text = "DOWNSIDE ROI",
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
// Component: Pillar 1 - Replacement Cost Arbitrage Card
// -------------------------------------------------------------
@Composable
private fun ReplacementCostArbitrageCard(
    deal: PropertyDeal,
    surfaceSqm: Int,
    askingPrice: Double,
    pricePerSqm: Double,
    replacementCostSqmBenchmark: Float,
    totalReplacementCost: Double,
    discountToReplacementPercent: Double,
    safetySpreadEur: Double,
    onBenchmarkChange: (Float) -> Unit,
    cardBg: Color,
    border: Color,
    gold: Color,
    green: Color,
    red: Color
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
                        imageVector = Icons.Default.Construction,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "1. REPLACEMENT COST ARBITRAGE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (discountToReplacementPercent > 20) green.copy(alpha = 0.2f) else gold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (discountToReplacementPercent > 20) green else gold)
                ) {
                    Text(
                        text = "${discountToReplacementPercent.toInt()}% SOTTO COSTO COSTRUZIONE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (discountToReplacementPercent > 20) Color(0xFFA5D6A7) else gold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Regola di Sam Zell: Nessun concorrente può costruire un nuovo edificio a un prezzo inferiore al tuo costo base.",
                fontSize = 10.sp,
                color = Color(0xFFB0BEC5)
            )

            // Comparison Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF0F0E12),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF37474F))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "TUO PREZZO D'ACQUISTO", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Text(text = "€${pricePerSqm.toInt()}/m²", fontSize = 14.sp, fontWeight = FontWeight.Black, color = gold, fontFamily = FontFamily.Monospace)
                        Text(text = "Totale: €${askingPrice.toInt()}", fontSize = 10.sp, color = Color.LightGray)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF0F0E12),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF37474F))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "COSTO RICOSTRUZIONE OGGI", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Text(text = "€${replacementCostSqmBenchmark.toInt()}/m²", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = FontFamily.Monospace)
                        Text(text = "Totale: €${totalReplacementCost.toInt()}", fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }

            // Margin of safety banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF142416),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, green)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Margine di Sicurezza da Rimpiazzo:", fontSize = 10.sp, color = Color(0xFFA5D6A7))
                        Text(
                            text = "+€${safetySpreadEur.toInt()}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF69F0AE)
                        )
                    }
                    Text(
                        text = "Vantaggio Insuperabile",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5D6A7)
                    )
                }
            }

            // Benchmark Adjuster Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Benchmark Costo Costruzione Nuova nella Zona:", fontSize = 10.sp, color = Color.LightGray)
                    Text(text = "€${replacementCostSqmBenchmark.toInt()}/m²", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = gold)
                }
                Slider(
                    value = replacementCostSqmBenchmark,
                    onValueChange = onBenchmarkChange,
                    valueRange = 1800f..4500f,
                    colors = SliderDefaults.colors(thumbColor = gold, activeTrackColor = gold)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Pillar 2 - Supply Moat Card
// -------------------------------------------------------------
@Composable
private fun SupplyMoatCard(
    deal: PropertyDeal,
    supplyInelasticityScore: Float,
    onScoreChange: (Float) -> Unit,
    cardBg: Color,
    border: Color,
    gold: Color
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
                        imageVector = Icons.Default.ShieldMoon,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "2. SUPPLY MOATS & BARRIERS TO ENTRY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Text(
                    text = "MOAT: ${supplyInelasticityScore.toInt()}/100",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = gold
                )
            }

            Text(
                text = "Zell: \"L'offerta è l'unica cosa certa. La domanda cambia, ma se l'offerta di nuovi immobili è bloccata dalla burocrazia o dalla geografia, mantieni il controllo.\"",
                fontSize = 10.sp,
                color = Color(0xFFB0BEC5)
            )

            Slider(
                value = supplyInelasticityScore,
                onValueChange = onScoreChange,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = gold, activeTrackColor = gold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Offerta Iper-Inflazionata (Rischio Alto)", fontSize = 8.sp, color = Color.Gray)
                Text(text = "Offerta Bloccata / Barriere Rigide", fontSize = 8.sp, color = gold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Pillar 3 - Downside-First Card
// -------------------------------------------------------------
@Composable
private fun DownsideFirstCard(
    breakevenOccupancyRate: Double,
    debtAmortizationYears: Float,
    stressVacancyTolerance: Float,
    onAmortizationChange: (Float) -> Unit,
    onVacancyToleranceChange: (Float) -> Unit,
    cardBg: Color,
    border: Color,
    gold: Color,
    green: Color
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
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "3. DOWNSIDE-FIRST LIQUIDITY & DEBT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Text(
                    text = "BREAKEVEN: ${breakevenOccupancyRate.toInt()}%",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81C784)
                )
            }

            Text(
                text = "Tolleranza minima di occupazione richiesta per pagare debito e spese senza intaccare la liquidità:",
                fontSize = 10.sp,
                color = Color(0xFFB0BEC5)
            )

            // Progress bar
            LinearProgressIndicator(
                progress = { (breakevenOccupancyRate / 100f).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (breakevenOccupancyRate < 50) green else gold,
                trackColor = Color(0xFF37474F)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Anche con il ${100 - breakevenOccupancyRate.toInt()}% di sfitto, l'immobile si auto-finanzia.", fontSize = 10.sp, color = Color(0xFFA5D6A7), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Pillar 4 - AI Contrarian Memo
// -------------------------------------------------------------
@Composable
private fun GraveDancerAiMemoCard(
    deal: PropertyDeal,
    isGenerating: Boolean,
    memoText: String?,
    onGenerateMemo: () -> Unit,
    cardBg: Color,
    border: Color,
    gold: Color,
    burgundy: Color
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
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "4. THE GRAVE DANCER'S AUDIT MEMO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = gold
                    )
                }
            }

            Text(
                text = "Genera un memo d'investimento contrarian con Gemini AI nello stile diretto e senza compromessi di Sam Zell:",
                fontSize = 10.sp,
                color = Color(0xFFB0BEC5)
            )

            if (memoText == null) {
                Button(
                    onClick = onGenerateMemo,
                    enabled = !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grave_dancer_generate_memo_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = burgundy,
                        contentColor = gold
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, gold)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isGenerating) "STESURA MEMO SAM ZELL IN CORSO..." else "GENERA MEMO THE GRAVE DANCER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0F0E12),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, gold.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "INVESTMENT COMMITTEE MEMO // SAM ZELL",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = gold
                        )
                        Text(
                            text = memoText,
                            fontSize = 11.sp,
                            color = Color(0xFFEDE7F6),
                            lineHeight = 16.sp
                        )
                        Button(
                            onClick = onGenerateMemo,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = burgundy,
                                contentColor = gold
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(text = "Rielabora Memo", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
