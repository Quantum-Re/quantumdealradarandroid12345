package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DataProvenance
import com.example.data.MacroYieldVerdict
import com.example.data.NormalizedRoiResult
import com.example.ui.YieldBenchmarkViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YieldBenchmarkingScreen(
    viewModel: YieldBenchmarkViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    var showCustomMacroControls by remember { mutableStateOf(false) }
    var selectedStressScenario by remember { mutableIntStateOf(0) } // 0: Base, 1: +100bps, 2: +200bps, 3: High Inflation

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.ITALY) }
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Yield Benchmarking",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Surface(
                                color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "MACRO AI",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Normalizzazione ROI vs Parametri Macro & Inflazione",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("yield_benchmarking_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = colors.textPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshMacroData() },
                        enabled = !uiState.isRefreshing,
                        modifier = Modifier.testTag("refresh_macro_rates_button")
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CyanAccent)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Aggiorna Parametri Macro", tint = CyanAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surfaceElevated
                )
            )
        },
        containerColor = colors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
        ) {
            // 1. Live Macro Rates Ticker Banner
            item {
                MacroRatesTickerCard(
                    macroData = uiState.macroData,
                    isLive = uiState.macroData.isLiveFetched,
                    lastUpdatedFormatted = dateFormat.format(Date(uiState.macroData.lastUpdatedTimestamp)),
                    sourceProvider = uiState.macroData.sourceProvider,
                    onToggleCustom = { showCustomMacroControls = !showCustomMacroControls },
                    isCustomOpen = showCustomMacroControls
                )
            }

            // 2. Custom Macro Overrides Expandable Section
            if (showCustomMacroControls) {
                item {
                    MacroOverridesCard(
                        macroData = uiState.macroData,
                        overrideInflation = uiState.overrideInflationRate,
                        overrideBtp = uiState.overrideBtpYield,
                        overrideSpread = uiState.overrideHurdleSpreadBps,
                        onInflationChange = { viewModel.setOverrideInflation(it) },
                        onBtpChange = { viewModel.setOverrideBtpYield(it) },
                        onSpreadChange = { viewModel.setOverrideHurdleSpread(it) },
                        onReset = { viewModel.resetMacroOverrides() }
                    )
                }
            }

            // 3. Property Deal Selector Pill Bar
            if (uiState.availableDeals.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Seleziona Immobile dal Radar per Benchmarking:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.availableDeals) { deal ->
                                val isSelected = deal.id == uiState.selectedDealId
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else colors.surfaceElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) CyanAccent else colors.surfaceBorder
                                    ),
                                    modifier = Modifier
                                        .clickable { viewModel.selectDeal(deal) }
                                        .testTag("select_deal_chip_${deal.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = deal.location.take(12),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) CyanAccent else colors.textPrimary
                                        )
                                        Text(
                                            text = "€${(deal.askingPrice / 1000).toInt()}k",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Quick Inputs Card (Purchase Price, Rent, Renovation)
            item {
                QuickInputsCard(
                    price = uiState.customPurchasePriceStr,
                    rent = uiState.customMonthlyRentStr,
                    reno = uiState.customRenovationCostStr,
                    expenses = uiState.customMonthlyExpensesStr,
                    downPaymentPct = uiState.customDownPaymentPercent,
                    onPriceChange = { viewModel.updatePurchasePrice(it) },
                    onRentChange = { viewModel.updateMonthlyRent(it) },
                    onRenoChange = { viewModel.updateRenovationCost(it) },
                    onExpensesChange = { viewModel.updateMonthlyExpenses(it) },
                    onDownPaymentChange = { viewModel.updateDownPaymentPercent(it) }
                )
            }

            // 5. Normalized ROI Results Breakdown
            uiState.normalizedResult?.let { result ->
                item {
                    MacroVerdictHeroCard(result = result)
                }

                item {
                    KeyNormalizationMetricsGrid(result = result, macroData = uiState.macroData)
                }

                item {
                    FisherEquationExplanationCard(result = result, inflationRate = uiState.macroData.italyHicpInflationRate)
                }

                item {
                    FiveYearPurchasingPowerCard(result = result, currencyFormat = currencyFormat)
                }

                item {
                    MacroStressTestingMatrixCard(
                        result = result,
                        selectedScenario = selectedStressScenario,
                        onSelectScenario = { selectedStressScenario = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroRatesTickerCard(
    macroData: com.example.data.MacroEconomicData,
    isLive: Boolean,
    lastUpdatedFormatted: String,
    sourceProvider: String,
    onToggleCustom: () -> Unit,
    isCustomOpen: Boolean
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0A192F),
        border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("macro_rates_ticker_card")
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
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isLive) EmeraldGreen else AmberGold)
                    )
                    Text(
                        text = if (isLive) "FEED ESTERNO COLLEGATO" else "PARAMETRI DI RIFERIMENTO INTERNI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isLive) EmeraldGreen else AmberGold
                    )
                }

                TextButton(
                    onClick = onToggleCustom,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("toggle_custom_macro_button")
                ) {
                    Icon(
                        imageVector = if (isCustomOpen) Icons.Default.Tune else Icons.Default.Edit,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCustomOpen) "Chiudi Override" else "Simula Scenari",
                        fontSize = 11.sp,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val provLabel = DataProvenance.fromString(macroData.provenance).label

            // Rate Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RateItemPill(
                    title = "Tasso Rifin.",
                    value = "${macroData.ecbMainRefinancingRate}%",
                    subtitle = provLabel,
                    color = Color(0xFF60A5FA),
                    modifier = Modifier.weight(1f)
                )
                RateItemPill(
                    title = "BTP 10Y",
                    value = "${macroData.italianBtp10YYield}%",
                    subtitle = provLabel,
                    color = Color(0xFFFBBF24),
                    modifier = Modifier.weight(1f)
                )
                RateItemPill(
                    title = "Euribor 12M",
                    value = "${macroData.euribor12M}%",
                    subtitle = provLabel,
                    color = Color(0xFFA78BFA),
                    modifier = Modifier.weight(1f)
                )
                RateItemPill(
                    title = "Inflazione",
                    value = "${macroData.italyHicpInflationRate}%",
                    subtitle = provLabel,
                    color = Color(0xFFF87171),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = sourceProvider,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "Agg: $lastUpdatedFormatted",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun RateItemPill(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = color)
            Text(text = subtitle, fontSize = 8.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun MacroOverridesCard(
    macroData: com.example.data.MacroEconomicData,
    overrideInflation: Double?,
    overrideBtp: Double?,
    overrideSpread: Int?,
    onInflationChange: (Double?) -> Unit,
    onBtpChange: (Double?) -> Unit,
    onSpreadChange: (Int?) -> Unit,
    onReset: () -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
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
                Text(
                    text = "⚙️ Override Parametri Macroeconomici",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                TextButton(
                    onClick = onReset,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Ripristina Default", fontSize = 11.sp, color = AmberGold)
                }
            }

            // Inflation Slider
            val currentInf = overrideInflation ?: macroData.italyHicpInflationRate
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tasso Inflazione Annuo Stimato:", fontSize = 12.sp, color = colors.textSecondary)
                    Text("${String.format(Locale.US, "%.1f", currentInf)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                }
                Slider(
                    value = currentInf.toFloat(),
                    onValueChange = { onInflationChange(Math.round(it * 10.0) / 10.0) },
                    valueRange = 0.0f..8.0f,
                    steps = 15,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFF87171), activeTrackColor = Color(0xFFF87171))
                )
            }

            // BTP 10Y Yield Slider
            val currentBtp = overrideBtp ?: macroData.italianBtp10YYield
            val provLabel = DataProvenance.fromString(macroData.provenance).label
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Rendimento BTP 10Y ($provLabel):", fontSize = 12.sp, color = colors.textSecondary)
                    Text("${String.format(Locale.US, "%.2f", currentBtp)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                }
                Slider(
                    value = currentBtp.toFloat(),
                    onValueChange = { onBtpChange(Math.round(it * 100.0) / 100.0) },
                    valueRange = 1.5f..6.5f,
                    steps = 20,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFBBF24), activeTrackColor = Color(0xFFFBBF24))
                )
            }
        }
    }
}

private data class MacroVerdictStyle(
    val bgColor: Color,
    val borderColor: Color,
    val accentColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun QuickInputsCard(
    price: String,
    rent: String,
    reno: String,
    expenses: String,
    downPaymentPct: Double,
    onPriceChange: (String) -> Unit,
    onRentChange: (String) -> Unit,
    onRenoChange: (String) -> Unit,
    onExpensesChange: (String) -> Unit,
    onDownPaymentChange: (Double) -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.surfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Parametri Finanziari Immobile",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = price,
                    onValueChange = onPriceChange,
                    label = { Text("Prezzo Acquisto (€)", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("benchmark_input_price")
                )
                OutlinedTextField(
                    value = rent,
                    onValueChange = onRentChange,
                    label = { Text("Affitto Mensile (€)", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("benchmark_input_rent")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = reno,
                    onValueChange = onRenoChange,
                    label = { Text("Ristrutturazione (€)", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("benchmark_input_reno")
                )
                OutlinedTextField(
                    value = expenses,
                    onValueChange = onExpensesChange,
                    label = { Text("Spese/IMU Mese (€)", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("benchmark_input_expenses")
                )
            }
        }
    }
}

@Composable
private fun MacroVerdictHeroCard(result: NormalizedRoiResult) {
    val style = when (result.macroVerdict) {
        MacroYieldVerdict.STRONG_OUTPERFORM -> MacroVerdictStyle(Color(0xFF064E3B), EmeraldGreen, EmeraldGreen, Icons.Default.CheckCircle)
        MacroYieldVerdict.HEALTHY_SPREAD -> MacroVerdictStyle(Color(0xFF0F2E3A), CyanAccent, CyanAccent, Icons.Default.Verified)
        MacroYieldVerdict.NEUTRAL_MARGINAL -> MacroVerdictStyle(Color(0xFF451A03), AmberGold, AmberGold, Icons.Default.WarningAmber)
        MacroYieldVerdict.NEGATIVE_REAL_YIELD -> MacroVerdictStyle(Color(0xFF450A0A), Color(0xFFEF4444), Color(0xFFEF4444), Icons.Default.ErrorOutline)
        MacroYieldVerdict.DEBT_DRAG_RISK -> MacroVerdictStyle(Color(0xFF2E0911), Color(0xFFF43F5E), Color(0xFFF43F5E), Icons.Default.TrendingDown)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = style.bgColor.copy(alpha = 0.85f),
        border = BorderStroke(1.5.dp, style.borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("macro_verdict_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(style.icon, contentDescription = null, tint = style.accentColor, modifier = Modifier.size(24.dp))
                Text(
                    text = result.macroVerdictTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Text(
                text = result.macroVerdictExplanation,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 16.sp
            )

            HorizontalDivider(color = style.borderColor.copy(alpha = 0.3f), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score Protezione Capitale:",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { result.purchasingPowerPreservationScore / 100f },
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = style.accentColor,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "${result.purchasingPowerPreservationScore}/100",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = style.accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyNormalizationMetricsGrid(
    result: NormalizedRoiResult,
    macroData: com.example.data.MacroEconomicData
) {
    val colors = AppTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Metriche Macro-Normalizzate:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Cap Rate Reale",
                value = "${result.realCapRatePercent}%",
                subtitle = "Nominale ${result.nominalCapRatePercent}%",
                badge = if (result.realCapRatePercent > 0) "🟢 Netto Inflaz." else "🔴 Negativo",
                accentColor = if (result.realCapRatePercent >= 3.0) EmeraldGreen else AmberGold,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Spread su BTP 10Y",
                value = "${if (result.spreadOverBtp10YBps >= 0) "+" else ""}${result.spreadOverBtp10YBps} bps",
                subtitle = "vs BTP ${macroData.italianBtp10YYield}% (${DataProvenance.fromString(macroData.provenance).label})",
                badge = if (result.spreadOverBtp10YBps >= 300) "⭐ Top Alpha" else "Risk Premium",
                accentColor = if (result.spreadOverBtp10YBps >= 150) CyanAccent else AmberGold,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Hurdle Rate Target",
                value = "${result.investorHurdleRatePercent}%",
                subtitle = if (result.clearsHurdleRate) "+${result.hurdleDifferencePercent}% Margine" else "${result.hurdleDifferencePercent}% Sotto Target",
                badge = if (result.clearsHurdleRate) "✅ Superato" else "❌ Non Raggiunto",
                accentColor = if (result.clearsHurdleRate) EmeraldGreen else Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Spread Costo Mutuo",
                value = "${if (result.spreadOverMortgageRatePercent >= 0) "+" else ""}${result.spreadOverMortgageRatePercent}%",
                subtitle = "Tasso Mutuo ${macroData.avgMortgageFixedRate}%",
                badge = if (result.spreadOverMortgageRatePercent > 0) "🚀 Leva Positiva" else "⚠️ Leva Negativa",
                accentColor = if (result.spreadOverMortgageRatePercent > 0) Color(0xFF60A5FA) else Color(0xFFF43F5E),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    badge: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.surfaceBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = colors.textSecondary)
                Text(text = badge, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )

            Text(text = subtitle, fontSize = 10.sp, color = colors.textMuted)
        }
    }
}

@Composable
private fun FisherEquationExplanationCard(
    result: NormalizedRoiResult,
    inflationRate: Double
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                Text(
                    text = "Formula di Normalizzazione (Equazione di Fisher)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "r_reale = (r_nominale - inflazione) / (1 + inflazione)",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyanAccent,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Text(
                text = "Con inflazione ISTAT al ${inflationRate}%, il rendimento nominale del ${result.nominalCapRatePercent}% corrisponde a un rendimento netto reale sul potere d'acquisto del ${result.realCapRatePercent}%.",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun FiveYearPurchasingPowerCard(
    result: NormalizedRoiResult,
    currencyFormat: NumberFormat
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.surfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "💰 Proiezione Flussi 5 Anni & Erosione Inflativa",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Flusso Cumulato Nominale", fontSize = 10.sp, color = colors.textSecondary)
                    Text(
                        text = currencyFormat.format(result.nominal5YearCumulativeCashFlow),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF60A5FA)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Valore Reale Deflazionato", fontSize = 10.sp, color = colors.textSecondary)
                    Text(
                        text = currencyFormat.format(result.real5YearCumulativeCashFlow),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF450A0A).copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Perdita Potere d'Acquisto (Inflazione):", fontSize = 11.sp, color = Color(0xFFFCA5A5))
                    Text(
                        text = "-${currencyFormat.format(result.fiveYearInflationDragEuros)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroStressTestingMatrixCard(
    result: NormalizedRoiResult,
    selectedScenario: Int,
    onSelectScenario: (Int) -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.surfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "⚡ Stress Test Shock Macroeconomici",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            // Scenarios Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StressTabButton(
                    title = "+100 bps Tassi",
                    selected = selectedScenario == 1,
                    onClick = { onSelectScenario(1) },
                    modifier = Modifier.weight(1f)
                )
                StressTabButton(
                    title = "+200 bps Shock",
                    selected = selectedScenario == 2,
                    onClick = { onSelectScenario(2) },
                    modifier = Modifier.weight(1f)
                )
                StressTabButton(
                    title = "Inflaz. 4.5%",
                    selected = selectedScenario == 3,
                    onClick = { onSelectScenario(3) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Scenario details output
            when (selectedScenario) {
                1 -> {
                    StressOutputBanner(
                        title = "Scenario Shock Moderato (+100 bps BCE / Mutuo):",
                        resultLabel = "Cash-on-Cash Return Ricalcolato:",
                        resultValue = "${result.stressTestRateHike100BpsCapRate}%",
                        impactText = "In caso di rialzo di 100 bps sui mutui, il flusso di cassa mensile si riduce, ma il rendimento rimane sostenibile."
                    )
                }
                2 -> {
                    StressOutputBanner(
                        title = "Scenario Shock Severo (+200 bps BCE / Mutuo):",
                        resultLabel = "Cash-on-Cash Return Ricalcolato:",
                        resultValue = "${result.stressTestRateHike200BpsCapRate}%",
                        impactText = "Un aumento di 200 bps testa la tenuta del debito. Verifica che il DSCR rimanga > 1.20."
                    )
                }
                3 -> {
                    StressOutputBanner(
                        title = "Scenario Stagflazione (Inflazione al 4.5%):",
                        resultLabel = "Cap Rate Reale Ricalcolato:",
                        resultValue = "${result.stressTestInflationSurgeRealCapRate}%",
                        impactText = "Con impennata inflativa al 4.5%, l'adeguamento ISTAT sui canoni protegge parzialmente il rendimento reale."
                    )
                }
                else -> {
                    Text(
                        text = "Seleziona uno scenario sopra per simulare la resilienza dell'investimento contro shock di tasso o fiammate inflative.",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StressTabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color(0xFF1E293B) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) CyanAccent else Color(0xFF334155)),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) CyanAccent else Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun StressOutputBanner(
    title: String,
    resultLabel: String,
    resultValue: String,
    impactText: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = resultLabel, fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text(text = resultValue, fontSize = 14.sp, fontWeight = FontWeight.Black, color = AmberGold)
            }
            Text(text = impactText, fontSize = 10.sp, color = Color(0xFF64748B))
        }
    }
}
