package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Property
import com.example.ui.theme.*
import com.example.util.AppCurrency
import com.example.util.CurrencyExchangeRateService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.PropertyOpportunityEvaluation
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Data holder for aggregated portfolio equity, yield, and investment health calculations
 * computed using live or cached scraped market comps from Immobiliare.it.
 */
data class PortfolioHealthMetrics(
    val totalPropertiesCount: Int,
    val totalAcquisitionCost: Double,
    val totalRenovationCost: Double,
    val totalInvestedBasis: Double,
    val totalScrapedMarketValue: Double,
    val totalEquity: Double,
    val equityGrowthPercent: Double,
    val averageGrossYieldPercent: Double,
    val totalAnnualRentalPotential: Double,
    val totalMonthlyRentalPotential: Double,
    val averageOpportunityScore: Int,
    val averageDaysOnMarket: Int,
    val healthScore: Int,
    val healthTierLabel: String,
    val healthTierBadge: String,
    val healthColor: Color,
    val healthDiagnosticText: String,
    val topEquityProperties: List<Pair<Property, Double>>,
    val benchmarkYieldSpread: Double // difference vs 5.8% national average
)

object PortfolioMetricsCalculator {
    const val ITALIAN_NATIONAL_YIELD_BENCHMARK = 5.8 // % gross average in Italy

    fun calculate(
        properties: List<Property>,
        evaluations: Map<Long, PropertyOpportunityEvaluation>
    ): PortfolioHealthMetrics {
        if (properties.isEmpty()) {
            return PortfolioHealthMetrics(
                totalPropertiesCount = 0,
                totalAcquisitionCost = 0.0,
                totalRenovationCost = 0.0,
                totalInvestedBasis = 0.0,
                totalScrapedMarketValue = 0.0,
                totalEquity = 0.0,
                equityGrowthPercent = 0.0,
                averageGrossYieldPercent = 0.0,
                totalAnnualRentalPotential = 0.0,
                totalMonthlyRentalPotential = 0.0,
                averageOpportunityScore = 0,
                averageDaysOnMarket = 0,
                healthScore = 0,
                healthTierLabel = "Nessun Immobile",
                healthTierBadge = "IN ATTESA DI DATI",
                healthColor = TextMutedDark,
                healthDiagnosticText = "Aggiungi o importa immobili per calcolare l'equity e il rendimento medio del tuo portafoglio.",
                topEquityProperties = emptyList(),
                benchmarkYieldSpread = 0.0
            )
        }

        val totalAcquisition = properties.sumOf { it.price }
        val totalReno = properties.sumOf {
            if (it.actualRenovationCost > 0) it.actualRenovationCost else it.estimatedRenovationCost
        }
        val totalInvested = totalAcquisition + totalReno

        var sumScrapedValue = 0.0
        var sumAnnualRent = 0.0
        val yieldList = mutableListOf<Double>()
        val domList = mutableListOf<Int>()
        val scoreList = mutableListOf<Int>()
        val equityPerProperty = mutableListOf<Pair<Property, Double>>()

        for (prop in properties) {
            val eval = evaluations[prop.id]
            val effectiveMarketVal = eval?.scrapedMarketValue ?: if (prop.targetResalePrice > 0) {
                prop.targetResalePrice
            } else if (prop.estimatedMarketValue > prop.price) {
                prop.estimatedMarketValue
            } else {
                prop.price * 1.25
            }
            sumScrapedValue += effectiveMarketVal

            val propInvested = prop.price + (if (prop.actualRenovationCost > 0) prop.actualRenovationCost else prop.estimatedRenovationCost)
            val propEquity = effectiveMarketVal - propInvested
            equityPerProperty.add(prop to propEquity)

            // Rental Yield
            val propYield = eval?.grossRentalYieldPotential ?: if (prop.price > 0 && prop.projectedRentalIncome > 0) {
                (prop.projectedRentalIncome * 12.0 / prop.price) * 100.0
            } else {
                6.2
            }
            yieldList.add(propYield)

            val annualRent = if (prop.projectedRentalIncome > 0) {
                prop.projectedRentalIncome * 12.0
            } else {
                (propYield / 100.0) * prop.price
            }
            sumAnnualRent += annualRent

            if (eval != null) {
                domList.add(eval.daysOnMarket)
                scoreList.add(eval.opportunityScore)
            }
        }

        val totalEquity = sumScrapedValue - totalInvested
        val equityGrowthPercent = if (totalInvested > 0) (totalEquity / totalInvested) * 100.0 else 0.0
        val avgYield = if (yieldList.isNotEmpty()) yieldList.average() else 0.0
        val avgDom = if (domList.isNotEmpty()) domList.average().roundToInt() else 110
        val avgScore = if (scoreList.isNotEmpty()) scoreList.average().roundToInt() else 72
        val yieldSpread = avgYield - ITALIAN_NATIONAL_YIELD_BENCHMARK

        // Calculate Comprehensive Health Score (0 - 100)
        val equityScore = ((equityGrowthPercent / 30.0) * 40.0).coerceIn(0.0, 40.0)
        val yieldScore = ((avgYield / 8.5) * 30.0).coerceIn(0.0, 30.0)
        val liquidityScore = ((180.0 - avgDom.coerceIn(40, 180)) / 140.0 * 20.0).coerceIn(0.0, 20.0)
        val qualityScore = (avgScore / 100.0 * 10.0).coerceIn(0.0, 10.0)

        val healthScore = (equityScore + yieldScore + liquidityScore + qualityScore).roundToInt().coerceIn(10, 99)

        val (tierLabel, tierBadge, healthColor, diagnostic) = when {
            healthScore >= 80 -> HealthTierInfo(
                "💎 Salute Eccellente",
                "ECCELLENTE",
                EmeraldGreen,
                "Portafoglio ad altissimo valore intrinseco. L'equity alpha generata sui prezzi di mercato live (+${String.format(Locale.ITALY, "%.1f", equityGrowthPercent)}%) e il rendimento medio (${String.format(Locale.ITALY, "%.1f", avgYield)}%) superano ampiamente i parametri di mercato."
            )
            healthScore >= 65 -> HealthTierInfo(
                "⚡ Solido & Redditizio",
                "BUONA SOLIDITÀ",
                CyanAccent,
                "Ottimo equilibrio patrimoniale. Buona protezione da ribassi con un cuscinetto equity positivo e rendimenti da locazione superiori alla media nazionale."
            )
            healthScore >= 50 -> HealthTierInfo(
                "⚖️ Equilibrato / Fair Value",
                "MODERATO",
                AmberGold,
                "Valori allineati al mercato medio di zona. La marginalità netta dipende in gran parte dall'ottimizzazione del piano di cantiere e dall'efficienza locativa."
            )
            else -> HealthTierInfo(
                "⚠️ Margini Ristretti",
                "DA OTTIMIZZARE",
                RoseRed,
                "Costi di carico vicini alle valutazioni medie live di zona. Valutare strategie di valorizzazione o rinegoziazione dei canoni per massimizzare la resa."
            )
        }

        val topProperties = equityPerProperty.sortedByDescending { it.second }.take(3)

        return PortfolioHealthMetrics(
            totalPropertiesCount = properties.size,
            totalAcquisitionCost = totalAcquisition,
            totalRenovationCost = totalReno,
            totalInvestedBasis = totalInvested,
            totalScrapedMarketValue = sumScrapedValue,
            totalEquity = totalEquity,
            equityGrowthPercent = equityGrowthPercent,
            averageGrossYieldPercent = avgYield,
            totalAnnualRentalPotential = sumAnnualRent,
            totalMonthlyRentalPotential = sumAnnualRent / 12.0,
            averageOpportunityScore = avgScore,
            averageDaysOnMarket = avgDom,
            healthScore = healthScore,
            healthTierLabel = tierLabel,
            healthTierBadge = tierBadge,
            healthColor = healthColor,
            healthDiagnosticText = diagnostic,
            topEquityProperties = topProperties,
            benchmarkYieldSpread = yieldSpread
        )
    }
}

private data class HealthTierInfo(
    val tierLabel: String,
    val tierBadge: String,
    val healthColor: Color,
    val diagnostic: String
)

/**
 * High-level Portfolio Overview Widget displaying aggregated equity, average yield,
 * and investment health metrics with real-time multi-currency FX switching.
 */
@Composable
fun PortfolioOverviewWidget(
    properties: List<Property>,
    evaluations: Map<Long, PropertyOpportunityEvaluation>,
    isRefreshing: Boolean,
    onRefreshMarketData: () -> Unit,
    euroFormat: NumberFormat,
    modifier: Modifier = Modifier,
    onOpenPropertyDetail: ((Property) -> Unit)? = null,
    currentCurrency: AppCurrency = AppCurrency.EUR,
    onCurrencyChange: ((AppCurrency) -> Unit)? = null
) {
    if (properties.isEmpty()) return

    val metrics = remember(properties, evaluations) {
        PortfolioMetricsCalculator.calculate(properties, evaluations)
    }

    val liveRates by CurrencyExchangeRateService.liveRates.collectAsStateWithLifecycle()
    val globalCurrency by CurrencyExchangeRateService.selectedGlobalCurrency.collectAsStateWithLifecycle()
    var selectedCurrency by rememberSaveable { mutableStateOf(currentCurrency) }

    // Synchronize if global currency changes
    LaunchedEffect(globalCurrency) {
        selectedCurrency = globalCurrency
    }

    var isExpanded by rememberSaveable { mutableStateOf(false) }

    // Helper formatting lambda using live exchange rates
    val formatMoney: (Double) -> String = { amountEur ->
        CurrencyExchangeRateService.formatFromEur(
            amountEur = amountEur,
            target = selectedCurrency,
            ratesMap = liveRates,
            includeDecimals = false,
            includeSymbol = true
        )
    }

    // Rotation animation for refresh button
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("portfolio_overview_widget")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Widget Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BentoPurpleHeader,
                        border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Portfolio Overview",
                                tint = BentoPurpleOnContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Panoramica Portafoglio",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldGainBg,
                                border = BorderStroke(1.dp, EmeraldGainBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldGainText)
                                    )
                                    Text(
                                        text = "LIVE MARKET",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldGainText
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Equity & Rendimento medio calcolati sui comparabili di zona",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onRefreshMarketData,
                        enabled = !isRefreshing,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("portfolio_market_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Aggiorna quotazioni di mercato",
                            tint = if (isRefreshing) CyanAccent else BentoPurpleOnContainer,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (isRefreshing) spinAngle else 0f)
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("toggle_portfolio_overview_expand_btn")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Comprimi dettagli portafoglio" else "Espandi dettagli portafoglio",
                            tint = TextMutedDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Real-Time Currency Switcher Bar
            CurrencySwitcherBar(
                selectedCurrency = selectedCurrency,
                onCurrencySelected = { currency ->
                    selectedCurrency = currency
                    onCurrencyChange?.invoke(currency)
                },
                isCompact = true,
                showRatePill = true
            )

            // Primary 2-Column Bento Grid: Total Equity & Average Yield
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // CARD 1: Total Equity
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (metrics.totalEquity >= 0) Color(0xFF0D2818) else Color(0xFF2C1014)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (metrics.totalEquity >= 0) EmeraldGreen.copy(alpha = 0.5f) else RoseRed.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("portfolio_total_equity_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL EQUITY (${selectedCurrency.code})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = if (metrics.totalEquity >= 0) EmeraldGreen else RoseRed
                            )
                            Icon(
                                imageVector = if (metrics.totalEquity >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (metrics.totalEquity >= 0) EmeraldGreen else RoseRed,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Text(
                            text = (if (metrics.totalEquity >= 0) "+" else "") + formatMoney(metrics.totalEquity),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (metrics.totalEquity >= 0) EmeraldGreen.copy(alpha = 0.2f) else RoseRed.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = String.format(Locale.ITALY, "%s%.1f%% vs investito", if (metrics.equityGrowthPercent >= 0) "+" else "", metrics.equityGrowthPercent),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (metrics.totalEquity >= 0) EmeraldGreen else RoseRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "Valore stimato: ${formatMoney(metrics.totalScrapedMarketValue)}",
                            fontSize = 9.sp,
                            color = TextMutedDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // CARD 2: Average Yield
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2E)),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("portfolio_average_yield_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RENDIMENTO MEDIO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = CyanAccent
                            )
                            Icon(
                                imageVector = Icons.Default.Percent,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Text(
                            text = String.format(Locale.ITALY, "%.1f%%", metrics.averageGrossYieldPercent),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimaryDark
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (metrics.benchmarkYieldSpread >= 0) CyanAccent.copy(alpha = 0.2f) else AmberGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = String.format(Locale.ITALY, "%s%.1f%% vs media IT (5.8%%)", if (metrics.benchmarkYieldSpread >= 0) "+" else "", metrics.benchmarkYieldSpread),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (metrics.benchmarkYieldSpread >= 0) CyanAccent else AmberGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "Canone est: ${formatMoney(metrics.totalMonthlyRentalPotential)}/mese",
                            fontSize = 9.sp,
                            color = TextMutedDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Investment Health Bar & Overall Status Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = metrics.healthColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, metrics.healthColor.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portfolio_health_status_banner")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = "Salute Portafoglio",
                                tint = metrics.healthColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = metrics.healthTierLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = metrics.healthColor,
                            modifier = Modifier.testTag("portfolio_health_score_badge")
                        ) {
                            Text(
                                text = "${metrics.healthScore}/100",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { (metrics.healthScore / 100f).coerceIn(0.1f, 1.0f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = metrics.healthColor,
                        trackColor = SurfaceCardBorder
                    )

                    Text(
                        text = metrics.healthDiagnosticText,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        lineHeight = 15.sp
                    )
                }
            }

            // Quick Diagnostic Indicators (3 Badges)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickHealthPill(
                    icon = Icons.Default.ShowChart,
                    label = "Alpha Margine",
                    value = if (metrics.totalEquity > 0) "+${formatMoney(metrics.totalEquity)}" else "0 ${selectedCurrency.symbol}",
                    color = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
                QuickHealthPill(
                    icon = Icons.Default.Timer,
                    label = "Tempo Vendita",
                    value = "~${metrics.averageDaysOnMarket} gg",
                    color = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
                QuickHealthPill(
                    icon = Icons.Default.Savings,
                    label = "Flusso Annuo",
                    value = formatMoney(metrics.totalAnnualRentalPotential),
                    color = AmberGold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Expandable Detailed Breakdown Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = SurfaceCardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dettaglio Valutazione & Contributo Immobili",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        if (selectedCurrency != AppCurrency.EUR) {
                            Text(
                                text = "Convertito in ${selectedCurrency.code}",
                                fontSize = 10.sp,
                                color = CyanAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Financial Summary Breakdown Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BreakdownRow(
                                label = "Costo Totale di Acquisto",
                                value = formatMoney(metrics.totalAcquisitionCost),
                                subtext = "${metrics.totalPropertiesCount} immobili in gestione"
                            )
                            BreakdownRow(
                                label = "Budget / Costi Ristrutturazione",
                                value = formatMoney(metrics.totalRenovationCost),
                                subtext = "CapEx previsto o sostenuto"
                            )
                            BreakdownRow(
                                label = "Base di Costo Totale (Investito)",
                                value = formatMoney(metrics.totalInvestedBasis),
                                isBold = true
                            )
                            HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.5f))
                            BreakdownRow(
                                label = "Valore di Mercato Live (Scraping)",
                                value = formatMoney(metrics.totalScrapedMarketValue),
                                valueColor = BentoPurpleOnContainer,
                                isBold = true,
                                subtext = "Basato sui prezzi medi/m² di zona"
                            )
                            BreakdownRow(
                                label = "Plusvalenza / Equity Alpha Netta",
                                value = (if (metrics.totalEquity >= 0) "+" else "") + formatMoney(metrics.totalEquity),
                                valueColor = if (metrics.totalEquity >= 0) EmeraldGreen else RoseRed,
                                isBold = true,
                                subtext = String.format(Locale.ITALY, "+%.1f%% sul capitale investito", metrics.equityGrowthPercent)
                            )
                        }
                    }

                    // Top Equity Generating Properties
                    if (metrics.topEquityProperties.isNotEmpty()) {
                        Text(
                            text = "Migliori Contributori di Equity:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondaryDark
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            metrics.topEquityProperties.forEachIndexed { index, (prop, equity) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DarkSlateBg,
                                    border = BorderStroke(1.dp, SurfaceCardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(
                                            onClickLabel = "Dettagli ${prop.title}",
                                            onClick = { onOpenPropertyDetail?.invoke(prop) }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = BentoPurpleHeader,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "#${index + 1}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BentoPurpleOnContainer
                                                    )
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = prop.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimaryDark,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = prop.address,
                                                    fontSize = 10.sp,
                                                    color = TextMutedDark,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = (if (equity >= 0) "+" else "") + formatMoney(equity),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (equity >= 0) EmeraldGreen else RoseRed
                                            )
                                            Text(
                                                text = "Equity",
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
        }
    }
}

@Composable
private fun QuickHealthPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(12.dp))
                Text(label, fontSize = 9.sp, color = TextMutedDark)
            }
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    subtext: String? = null,
    valueColor: Color = TextPrimaryDark,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = if (isBold) 12.sp else 11.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = if (isBold) TextPrimaryDark else TextSecondaryDark
            )
            if (subtext != null) {
                Text(text = subtext, fontSize = 9.sp, color = TextMutedDark)
            }
        }
        Text(
            text = value,
            fontSize = if (isBold) 13.sp else 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}
