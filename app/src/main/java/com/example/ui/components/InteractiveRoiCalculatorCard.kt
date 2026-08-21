package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.util.MarketEstimateService
import com.example.util.ImmobiliareObservatoryService
import com.example.util.ProvinceScrapedKpi
import com.example.util.SeniorValuationEngine
import com.example.util.AppCurrency
import com.example.util.CurrencyExchangeRateService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import java.text.NumberFormat
import java.util.Locale

import com.example.util.ItalianAcquisitionCostBreakdown
import com.example.util.ItalianAcquisitionType
import com.example.util.ItalianFlipTaxBreakdown
import com.example.util.ItalianPropertyTaxEngine
import com.example.util.RentalTaxRegime

/**
 * Data model representing the inputs and derived financial calculations for the ROI Calculator,
 * including precise Italian property transaction tax and notary fee regulations.
 */
data class RoiCalculationData(
    val purchasePrice: Double = 180000.0,
    val renovationCost: Double = 25000.0,
    val legalFees: Double = 0.0,
    val estimatedMonthlyRent: Double = 1200.0,
    val monthlyExpenses: Double = 180.0,
    val downPaymentPercent: Double = 20.0,
    val mortgageRatePercent: Double = 3.2,
    val loanTermYears: Int = 25,
    val expectedResalePrice: Double = 260000.0,
    val taxFees: Double? = null,
    val notaryFees: Double? = null,
    val acquisitionType: ItalianAcquisitionType = ItalianAcquisitionType.PRIVATE_SECOND_HOME,
    val cadastralValue: Double = 0.0,
    val includeAgencyFee: Boolean = false,
    val agencyFeePercent: Double = 3.0,
    val rentalTaxRegime: RentalTaxRegime = RentalTaxRegime.ESENTE_LORDO,
    val flipHoldingPeriodYears: Int = 1,
    val isPrimaryResidence: Boolean = false
) {
    val downPaymentAmount: Double get() = purchasePrice * (downPaymentPercent / 100.0)
    val loanAmount: Double get() = (purchasePrice - downPaymentAmount).coerceAtLeast(0.0)

    // Italian Tax & Notary Breakdown
    val taxBreakdown: ItalianAcquisitionCostBreakdown get() = ItalianPropertyTaxEngine.calculateAcquisitionCosts(
        purchasePrice = purchasePrice,
        cadastralValue = if (cadastralValue > 0.0) cadastralValue else null,
        acquisitionType = acquisitionType,
        hasMortgage = downPaymentPercent < 100.0,
        loanAmount = loanAmount,
        includeAgencyFee = includeAgencyFee,
        agencyFeePercent = agencyFeePercent,
        customNotaryFee = notaryFees,
        customTaxFee = taxFees
    )

    val effectiveTaxFees: Double get() = taxFees ?: taxBreakdown.totalTaxes
    val effectiveNotaryFees: Double get() = notaryFees ?: taxBreakdown.totalNotaryFees
    val totalAncillaryFees: Double get() = (taxFees ?: 0.0) + (notaryFees ?: 0.0) + legalFees + (if (taxFees == null && notaryFees == null && legalFees == 0.0) taxBreakdown.totalAncillaryCosts else 0.0)

    val totalProjectCost: Double get() = purchasePrice + renovationCost + totalAncillaryFees
    val initialCashRequired: Double get() = downPaymentAmount + renovationCost + totalAncillaryFees

    val annualGrossRent: Double get() = estimatedMonthlyRent * 12.0
    val annualExpenses: Double get() = monthlyExpenses * 12.0
    val netOperatingIncome: Double get() = (annualGrossRent - annualExpenses).coerceAtLeast(0.0)

    val annualRentalTax: Double get() = ItalianPropertyTaxEngine.calculateRentalTax(annualGrossRent, rentalTaxRegime)

    val monthlyMortgagePayment: Double get() {
        if (loanAmount <= 0.0) return 0.0
        if (mortgageRatePercent <= 0.0) return loanAmount / (loanTermYears * 12).toDouble().coerceAtLeast(1.0)
        val monthlyRate = (mortgageRatePercent / 100.0) / 12.0
        val n = (loanTermYears * 12).toDouble()
        val compound = Math.pow(1.0 + monthlyRate, n)
        if (compound == 1.0) return 0.0
        return loanAmount * (monthlyRate * compound) / (compound - 1.0)
    }

    val annualDebtService: Double get() = monthlyMortgagePayment * 12.0

    // Pre-Tax Cash Flow
    val annualNetCashFlowPreTax: Double get() = netOperatingIncome - annualDebtService
    val monthlyNetCashFlowPreTax: Double get() = annualNetCashFlowPreTax / 12.0

    // Post-Tax Cash Flow
    val annualNetCashFlow: Double get() = netOperatingIncome - annualRentalTax - annualDebtService
    val monthlyNetCashFlow: Double get() = annualNetCashFlow / 12.0

    // Yield Metrics
    val grossYieldPercent: Double
        get() = if (totalProjectCost > 0.0) (annualGrossRent / totalProjectCost) * 100.0 else 0.0

    val netYieldCapRatePercent: Double
        get() = if (totalProjectCost > 0.0) ((netOperatingIncome - annualRentalTax) / totalProjectCost) * 100.0 else 0.0

    val cashOnCashReturnPercent: Double
        get() = if (initialCashRequired > 0.0) (annualNetCashFlow / initialCashRequired) * 100.0 else 0.0

    val breakEvenYears: Double
        get() = if (annualNetCashFlow > 0.0) initialCashRequired / annualNetCashFlow else 0.0

    // Flip Plusvalenza & Net Profit Breakdown
    val flipTaxBreakdown: ItalianFlipTaxBreakdown get() = ItalianPropertyTaxEngine.calculateFlipCapitalGainTax(
        purchasePrice = purchasePrice,
        resalePrice = expectedResalePrice,
        renovationCost = renovationCost,
        ancillaryCosts = totalAncillaryFees,
        holdingPeriodYears = flipHoldingPeriodYears,
        isPrimaryResidence = isPrimaryResidence
    )

    val totalFlipProfitGross: Double get() = expectedResalePrice - totalProjectCost
    val grossFlipProfit: Double get() = expectedResalePrice - totalProjectCost
    val flipRoiGrossPercent: Double get() = if (totalProjectCost > 0.0) (grossFlipProfit / totalProjectCost) * 100.0 else 0.0
    val totalFlipProfit: Double get() = flipTaxBreakdown.netFlipProfit
    val flipRoiPercent: Double
        get() = if (totalProjectCost > 0.0) (totalFlipProfit / totalProjectCost) * 100.0 else 0.0

    val yieldRating: YieldGrade
        get() = when {
            cashOnCashReturnPercent >= 12.0 || grossYieldPercent >= 10.0 -> YieldGrade.EXCELLENT
            cashOnCashReturnPercent >= 8.0 || grossYieldPercent >= 7.5 -> YieldGrade.VERY_GOOD
            cashOnCashReturnPercent >= 5.0 || grossYieldPercent >= 5.5 -> YieldGrade.GOOD
            else -> YieldGrade.MODERATE
        }
}

enum class YieldGrade(val label: String, val color: Color, val description: String) {
    EXCELLENT("⭐ Rendimento Eccezionale", EmeraldGreen, "Rendimento nettamente superiore alla media di mercato"),
    VERY_GOOD("✨ Ottimo Rendimento", CyanAccent, "Ottimo bilanciamento tra cashflow e rendimento"),
    GOOD("👍 Rendimento Buono", AmberGold, "Investimento solido con rendimento in linea col mercato"),
    MODERATE("ℹ️ Rendimento Moderato", TextMutedDark, "Rendimento prudenziale, valuta ottimizazione costi o affitto")
}

/**
 * Interactive ROI & Yield Calculator Component.
 * Allows instant manipulation of Purchase Price, Renovation Costs, and Estimated Rent
 * to calculate real-time investment yields (Gross Yield, Cap Rate, Cash-on-Cash, Monthly Cashflow).
 */
@Composable
fun InteractiveRoiCalculatorCard(
    modifier: Modifier = Modifier,
    initialPurchasePrice: Double = 180000.0,
    initialRenovationCost: Double = 25000.0,
    initialEstimatedRent: Double = 1200.0,
    initialLegalFees: Double = 0.0,
    initialTaxFees: Double? = null,
    initialNotaryFees: Double? = null,
    initialMonthlyExpenses: Double = 180.0,
    initialDownPaymentPercent: Double = 20.0,
    initialMortgageRate: Double = 3.2,
    initialLoanTermYears: Int = 25,
    initialAcquisitionType: ItalianAcquisitionType = ItalianAcquisitionType.PRIVATE_SECOND_HOME,
    initialCadastralValue: Double = 0.0,
    initialIncludeAgencyFee: Boolean = false,
    initialRentalTaxRegime: RentalTaxRegime = RentalTaxRegime.CEDOLARE_SECCA_21,
    initialFlipHoldingPeriodYears: Int = 1,
    propertyTitle: String = "Immobile Target Investimento",
    propertyLocation: String = "Italia",
    surfaceSqm: Int = 80,
    currentCurrency: AppCurrency = AppCurrency.EUR,
    onCurrencyChange: ((AppCurrency) -> Unit)? = null,
    onCalculateCompleted: ((RoiCalculationData) -> Unit)? = null,
    onExportPdfClick: ((RoiCalculationData) -> Unit)? = null
) {
    val liveRates by CurrencyExchangeRateService.liveRates.collectAsStateWithLifecycle()
    val globalCurrency by CurrencyExchangeRateService.selectedGlobalCurrency.collectAsStateWithLifecycle()
    var selectedCurrency by remember(currentCurrency) { mutableStateOf(currentCurrency) }

    LaunchedEffect(globalCurrency) {
        selectedCurrency = globalCurrency
    }

    val formatMoney: (Double) -> String = { amountEur ->
        CurrencyExchangeRateService.formatFromEur(
            amountEur = amountEur,
            target = selectedCurrency,
            ratesMap = liveRates,
            includeDecimals = false,
            includeSymbol = true
        )
    }

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
    }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    var showTaxDetailDialog by remember { mutableStateOf(false) }

    // Scraper Service State
    var isScrapingKpis by remember { mutableStateOf(false) }
    var scrapedKpi by remember { mutableStateOf<ProvinceScrapedKpi?>(null) }
    var scraperError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(propertyLocation) {
        scrapedKpi = MarketEstimateService.getCuratedProvinceKpi(propertyLocation)
    }

    // Internal State
    var purchasePriceStr by remember(initialPurchasePrice) {
        mutableStateOf(initialPurchasePrice.toInt().toString())
    }
    var renovationCostStr by remember(initialRenovationCost) {
        mutableStateOf(initialRenovationCost.toInt().toString())
    }
    var estimatedRentStr by remember(initialEstimatedRent) {
        mutableStateOf(initialEstimatedRent.toInt().toString())
    }
    var legalFeesStr by remember(initialLegalFees) {
        mutableStateOf(if (initialLegalFees > 0) initialLegalFees.toInt().toString() else "0")
    }
    var taxFeesStr by remember(initialTaxFees) {
        mutableStateOf(initialTaxFees?.toInt()?.toString() ?: "")
    }
    var notaryFeesStr by remember(initialNotaryFees) {
        mutableStateOf(initialNotaryFees?.toInt()?.toString() ?: "")
    }
    var cadastralValueStr by remember(initialCadastralValue) {
        mutableStateOf(if (initialCadastralValue > 0) initialCadastralValue.toInt().toString() else "")
    }
    var selectedAcquisitionType by remember(initialAcquisitionType) {
        mutableStateOf(initialAcquisitionType)
    }
    var includeAgencyFee by remember(initialIncludeAgencyFee) {
        mutableStateOf(initialIncludeAgencyFee)
    }
    var selectedRentalTaxRegime by remember(initialRentalTaxRegime) {
        mutableStateOf(initialRentalTaxRegime)
    }
    var flipHoldingPeriodYearsStr by remember(initialFlipHoldingPeriodYears) {
        mutableStateOf(initialFlipHoldingPeriodYears.toString())
    }
    var monthlyExpensesStr by remember(initialMonthlyExpenses) {
        mutableStateOf(initialMonthlyExpenses.toInt().toString())
    }
    var downPaymentPercentStr by remember(initialDownPaymentPercent) {
        mutableStateOf(initialDownPaymentPercent.toInt().toString())
    }
    var mortgageRateStr by remember(initialMortgageRate) {
        mutableStateOf(initialMortgageRate.toString())
    }
    var loanTermYearsStr by remember(initialLoanTermYears) {
        mutableStateOf(initialLoanTermYears.toString())
    }

    var showAdvancedSettings by remember { mutableStateOf(false) }
    var selectedStrategyTab by remember { mutableStateOf(0) } // 0 = Buy & Hold (Rental), 1 = Fix & Flip, 2 = Stress Test

    // Derived Calculation
    val calculationData = remember(
        purchasePriceStr,
        renovationCostStr,
        estimatedRentStr,
        legalFeesStr,
        taxFeesStr,
        notaryFeesStr,
        cadastralValueStr,
        selectedAcquisitionType,
        includeAgencyFee,
        selectedRentalTaxRegime,
        flipHoldingPeriodYearsStr,
        monthlyExpensesStr,
        downPaymentPercentStr,
        mortgageRateStr,
        loanTermYearsStr
    ) {
        val pPrice = purchasePriceStr.toDoubleOrNull() ?: 0.0
        val rCost = renovationCostStr.toDoubleOrNull() ?: 0.0
        val eRent = estimatedRentStr.toDoubleOrNull() ?: 0.0
        val lFees = legalFeesStr.toDoubleOrNull() ?: 0.0
        val tFees = taxFeesStr.toDoubleOrNull()
        val nFees = notaryFeesStr.toDoubleOrNull()
        val cVal = cadastralValueStr.toDoubleOrNull() ?: 0.0
        val flipYears = flipHoldingPeriodYearsStr.toIntOrNull() ?: 1
        val mExp = monthlyExpensesStr.toDoubleOrNull() ?: 0.0
        val dp = downPaymentPercentStr.toDoubleOrNull() ?: 20.0
        val mRate = mortgageRateStr.toDoubleOrNull() ?: 3.2
        val term = loanTermYearsStr.toIntOrNull() ?: 25
        val expectedResale = (pPrice + rCost + (tFees ?: 0.0) + (nFees ?: 0.0) + lFees) * 1.25

        val data = RoiCalculationData(
            purchasePrice = pPrice,
            renovationCost = rCost,
            legalFees = lFees,
            taxFees = tFees,
            notaryFees = nFees,
            acquisitionType = selectedAcquisitionType,
            cadastralValue = cVal,
            includeAgencyFee = includeAgencyFee,
            rentalTaxRegime = selectedRentalTaxRegime,
            flipHoldingPeriodYears = flipYears,
            estimatedMonthlyRent = eRent,
            monthlyExpenses = mExp,
            downPaymentPercent = dp,
            mortgageRatePercent = mRate,
            loanTermYears = term,
            expectedResalePrice = expectedResale
        )
        onCalculateCompleted?.invoke(data)
        data
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar with Title and Yield Rating Badge
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
                            .size(36.dp)
                            .background(CyanAccent, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calcolatore ROI",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Calcolatore Interattivo ROI",
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Simulazione in tempo reale di rendimenti e flussi",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                // Yield Grade Pill
                Surface(
                    color = calculationData.yieldRating.color.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, calculationData.yieldRating.color.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = calculationData.yieldRating.label,
                        color = calculationData.yieldRating.color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Real-Time Global Currency Switcher
            CurrencySwitcherBar(
                selectedCurrency = selectedCurrency,
                onCurrencySelected = { currency ->
                    selectedCurrency = currency
                    onCurrencyChange?.invoke(currency)
                },
                isCompact = true,
                showRatePill = true
            )

            // Strategy Selector Tabs
            TabRow(
                selectedTabIndex = selectedStrategyTab,
                containerColor = DarkSlateBg,
                contentColor = CyanAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedStrategyTab]),
                        color = CyanAccent
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = selectedStrategyTab == 0,
                    onClick = { selectedStrategyTab = 0 },
                    text = {
                        Text(
                            text = "Locazione",
                            fontSize = 11.sp,
                            fontWeight = if (selectedStrategyTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("roi_calc_tab_rental")
                )
                Tab(
                    selected = selectedStrategyTab == 1,
                    onClick = { selectedStrategyTab = 1 },
                    text = {
                        Text(
                            text = "Fix & Flip",
                            fontSize = 11.sp,
                            fontWeight = if (selectedStrategyTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("roi_calc_tab_flip")
                )
                Tab(
                    selected = selectedStrategyTab == 2,
                    onClick = { selectedStrategyTab = 2 },
                    text = {
                        Text(
                            text = "Stress Test & IRR",
                            fontSize = 11.sp,
                            fontWeight = if (selectedStrategyTab == 2) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("roi_calc_tab_stress_test")
                )
            }

            // Key Results Hero Grid
            Surface(
                color = BentoPurpleContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedStrategyTab == 0) {
                        // CoC Return & Monthly Cash Flow Highlight (Net Post-Tax)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroMetricBox(
                                label = "Cash-on-Cash Return Netto",
                                value = "${String.format(Locale.US, "%.2f", calculationData.cashOnCashReturnPercent)}%",
                                subtext = if (calculationData.annualRentalTax > 0) "Post-imposte (${calculationData.rentalTaxRegime.label})" else "Ritorno annuo su ${formatMoney(calculationData.initialCashRequired)} versati",
                                valueColor = EmeraldGreen,
                                testTag = "roi_calc_coc_value",
                                modifier = Modifier.weight(1.1f)
                            )
                            HeroMetricBox(
                                label = "Cash Flow Netto Post-Imposte",
                                value = "${formatMoney(calculationData.monthlyNetCashFlow)}/m",
                                subtext = "${formatMoney(calculationData.annualNetCashFlow)}/anno (Lordo: ${formatMoney(calculationData.monthlyNetCashFlowPreTax)}/m)",
                                valueColor = if (calculationData.monthlyNetCashFlow >= 0) CyanAccent else Color(0xFFFF5252),
                                testTag = "roi_calc_cashflow_value",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Rental Tax Regime Selector
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSlateBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Regime Fiscale Locazione:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                if (calculationData.annualRentalTax > 0) {
                                    Text(
                                        text = "Imposte: -${formatMoney(calculationData.annualRentalTax)}/anno",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberGold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RentalTaxRegime.values().forEach { regime ->
                                    PresetChip(
                                        label = regime.label,
                                        selected = selectedRentalTaxRegime == regime,
                                        onClick = { selectedRentalTaxRegime = regime }
                                    )
                                }
                            }
                        }

                        // Secondary Row: Gross Yield, Cap Rate, Break-even
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniMetricBox(
                                label = "Rendimento Lordo",
                                value = "${String.format(Locale.US, "%.1f", calculationData.grossYieldPercent)}%",
                                tag = "roi_calc_gross_yield_value",
                                modifier = Modifier.weight(1f)
                            )
                            MiniMetricBox(
                                label = "Cap Rate Netto",
                                value = "${String.format(Locale.US, "%.1f", calculationData.netYieldCapRatePercent)}%",
                                tag = "roi_calc_net_yield_value",
                                modifier = Modifier.weight(1f)
                            )
                            MiniMetricBox(
                                label = "Payback Time",
                                value = if (calculationData.breakEvenYears > 0) "${String.format(Locale.US, "%.1f", calculationData.breakEvenYears)} anni" else "N/D",
                                tag = "roi_calc_payback_value",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else if (selectedStrategyTab == 1) {
                        // Flip View with Plusvalenza Tax (Art. 67 TUIR) Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroMetricBox(
                                label = "Utile Netto Flip Reale",
                                value = formatMoney(calculationData.totalFlipProfit),
                                subtext = if (calculationData.flipTaxBreakdown.isSubjectToPlusvalenza) "Detratta Plusvalenza 26% (${formatMoney(calculationData.flipTaxBreakdown.plusvalenzaTaxAmount)})" else "Esente Plusvalenza (Holding >5a)",
                                valueColor = AmberGold,
                                testTag = "roi_calc_flip_profit_value",
                                modifier = Modifier.weight(1f)
                            )
                            HeroMetricBox(
                                label = "ROI Netto Operazione",
                                value = "${String.format(Locale.US, "%.1f", calculationData.flipRoiPercent)}%",
                                subtext = "Lordo: ${String.format(Locale.US, "%.1f", calculationData.flipRoiGrossPercent)}% (Costo: ${formatMoney(calculationData.totalProjectCost)})",
                                valueColor = PurpleIndigo,
                                testTag = "roi_calc_flip_roi_value",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Holding Period & Tax Exemption Toggle for Flip
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSlateBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Plusvalenza Fiscale (Art. 67 TUIR):",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (calculationData.flipTaxBreakdown.isSubjectToPlusvalenza) "Tassata 26%" else "0% (Esente)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (calculationData.flipTaxBreakdown.isSubjectToPlusvalenza) RoseRed else EmeraldGreen
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PresetChip(
                                    label = "Flip < 5 Anni (Tassato 26%)",
                                    selected = flipHoldingPeriodYearsStr == "1",
                                    onClick = { flipHoldingPeriodYearsStr = "1" }
                                )
                                PresetChip(
                                    label = "Holding > 5 Anni (Esente 0%)",
                                    selected = flipHoldingPeriodYearsStr == "6",
                                    onClick = { flipHoldingPeriodYearsStr = "6" }
                                )
                            }
                        }
                    } else {
                        // Senior Underwriting & Stress-Test Tab
                        val underwriting = remember(calculationData) {
                            SeniorValuationEngine.performAdvancedUnderwriting(
                                SeniorValuationEngine.AdvancedUnderwritingInput(
                                    purchasePrice = calculationData.purchasePrice,
                                    renovationCost = calculationData.renovationCost,
                                    estimatedMonthlyRent = calculationData.estimatedMonthlyRent,
                                    resaleTargetPrice = calculationData.expectedResalePrice,
                                    mortgageLtvPercent = (100.0 - calculationData.downPaymentPercent).coerceIn(0.0, 100.0),
                                    mortgageInterestRatePercent = calculationData.mortgageRatePercent,
                                    mortgageDurationYears = calculationData.loanTermYears
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HeroMetricBox(
                                label = "5-Year IRR Stimato",
                                value = "${String.format(Locale.US, "%.1f", underwriting.fiveYearIrrPercent)}%",
                                subtext = "Rendimento interno attualizzato a 5 anni",
                                valueColor = CyanAccent,
                                testTag = "roi_calc_irr_value",
                                modifier = Modifier.weight(1.1f)
                            )
                            HeroMetricBox(
                                label = "DSCR Mutuo",
                                value = "${String.format(Locale.US, "%.2f", underwriting.dscr)}x",
                                subtext = if (underwriting.dscr >= 1.25) "✅ Copertura Debito Solida" else "⚠️ Rischio Finanziario",
                                valueColor = if (underwriting.dscr >= 1.25) EmeraldGreen else AmberGold,
                                testTag = "roi_calc_dscr_value",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Stress Matrix
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Matrice Stress Test Finanziario:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            underwriting.stressTests.forEach { test ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SurfaceCardDark, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(test.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                    Text(
                                        "Cash Flow: ${formatMoney(test.netAnnualCashFlow)}/a • CoC: ${String.format(Locale.ITALY, "%.1f", test.leveredCashOnCashPercent)}%",
                                        fontSize = 9.sp,
                                        color = if (test.isViable) EmeraldGreen else RoseRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = SurfaceCardBorder)

            // Immobiliare.it Live Scraper & Localized Province Benchmarks Card
            val kpi = scrapedKpi ?: MarketEstimateService.getCuratedProvinceKpi(propertyLocation)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BentoCardBgLight,
                border = BorderStroke(1.dp, if (kpi.isLiveScraped) EmeraldGreen.copy(alpha = 0.5f) else CyanAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header with Title, Live Grounding Badge & Scraper Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TravelExplore, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Scraper ${kpi.locationName} (${kpi.province})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (kpi.isLiveScraped) CyanAccent.copy(alpha = 0.2f) else RoseRed.copy(alpha = 0.15f),
                                        border = BorderStroke(0.6.dp, if (kpi.isLiveScraped) CyanAccent else RoseRed)
                                    ) {
                                        Text(
                                            text = if (kpi.isLiveScraped) "STIMA AI" else "DATO FITTIZIO",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (kpi.isLiveScraped) CyanAccent else RoseRed,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (kpi.isLiveScraped) "Stima generata da AI con ricerca web - non verificata" else "Valore di esempio inserito nel codice - NON è un dato di mercato",
                                    fontSize = 9.sp,
                                    color = if (kpi.isLiveScraped) TextSecondaryDark else AmberGold
                                )
                            }
                        }

                        // Live Scraper Action Button
                        Button(
                            onClick = {
                                if (!isScrapingKpis) {
                                    coroutineScope.launch {
                                        isScrapingKpis = true
                                        scraperError = null
                                        val res = MarketEstimateService.scrapeMarketKpis(propertyLocation)
                                        res.onSuccess {
                                            scrapedKpi = it
                                            isScrapingKpis = false
                                        }.onFailure { err ->
                                            scraperError = err.localizedMessage
                                            isScrapingKpis = false
                                        }
                                    }
                                }
                            },
                            enabled = !isScrapingKpis,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                            modifier = Modifier.testTag("btn_trigger_live_scraper")
                        ) {
                            if (isScrapingKpis) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scraping...", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("⚡ Live Scrape", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 4 Localized Scraped Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Metric 1: Prezzo Vendita / m²
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceCardDark,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("Prezzo Medio", fontSize = 8.sp, color = TextMutedDark)
                                Text(
                                    if (kpi.avgSalePriceSqM != null) "€${kpi.avgSalePriceSqM.toInt()}/m²" else "N/D",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (kpi.saleTrendYoY != null) {
                                        if (kpi.saleTrendYoY >= 0) "+${kpi.saleTrendYoY}% YoY" else "${kpi.saleTrendYoY}% YoY"
                                    } else "N/D",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (kpi.saleTrendYoY != null) {
                                        if (kpi.saleTrendYoY >= 0) EmeraldGreen else RoseRed
                                    } else TextMutedDark
                                )
                            }
                        }

                        // Metric 2: Canone Affitto & Gross Yield
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceCardDark,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("Affitto / Yield", fontSize = 8.sp, color = TextMutedDark)
                                Text(
                                    if (kpi.avgRentPriceSqM != null) "€${String.format(Locale.ITALY, "%.2f", kpi.avgRentPriceSqM)}/m²" else "N/D",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberGold
                                )
                                Text(
                                    text = if (kpi.grossRentalYield != null) "Yield: ${String.format(Locale.ITALY, "%.1f", kpi.grossRentalYield)}%" else "Yield: N/D",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (kpi.grossRentalYield != null) EmeraldGreen else TextMutedDark
                                )
                            }
                        }

                        // Metric 3: Saturazione Mercato
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceCardDark,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("Saturazione", fontSize = 8.sp, color = TextMutedDark)
                                Text(
                                    if (kpi.marketSaturationScore != null) "${kpi.marketSaturationScore}/100" else "N/D",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (kpi.marketSaturationScore != null) {
                                        if (kpi.marketSaturationScore < 45) EmeraldGreen else if (kpi.marketSaturationScore < 70) AmberGold else RoseRed
                                    } else TextMutedDark
                                )
                                Text(
                                    text = if (kpi.marketSaturationScore != null) {
                                        if (kpi.marketSaturationScore < 45) "Alta Richiesta" else "Bilanciato"
                                    } else "N/D",
                                    fontSize = 8.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        // Metric 4: Days on Market (DOM)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceCardDark,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("Tempo Vendita", fontSize = 8.sp, color = TextMutedDark)
                                Text(
                                    if (kpi.avgDaysOnMarket != null) "${kpi.avgDaysOnMarket} gg" else "N/D",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (kpi.avgDaysOnMarket != null) CyanAccent else TextMutedDark
                                )
                                Text(
                                    text = if (kpi.absorptionRatePercent != null) "Assorb.: ${kpi.absorptionRatePercent.toInt()}%" else "Assorb.: N/D",
                                    fontSize = 8.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }

                    // Micro-zones preview row if available
                    if (kpi.hotMicroZones.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Micro-zone:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark)
                            kpi.hotMicroZones.forEach { (zone, price) ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SurfaceCardDark.copy(alpha = 0.8f),
                                    border = BorderStroke(0.6.dp, SurfaceCardBorder)
                                ) {
                                    Text(
                                        text = "$zone: €${price.toInt()}/m²",
                                        fontSize = 9.sp,
                                        color = TextPrimaryDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Auto-Calibrate & Benchmark Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val safeSqm = if (surfaceSqm > 0) surfaceSqm.toDouble() else 80.0
                                val rentM2 = kpi.avgRentPriceSqM ?: 0.0
                                val calculatedRent = (safeSqm * rentM2).toInt()
                                val estimatedCapex = (safeSqm * 380.0).toInt()
                                estimatedRentStr = calculatedRent.toString()
                                renovationCostStr = estimatedCapex.toString()
                            },
                            enabled = kpi.avgRentPriceSqM != null,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("btn_apply_scraped_benchmark")
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Calibra ROI con Benchmark", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        if (kpi.sourceUrl.isNotBlank() && (kpi.sourceUrl.startsWith("http://") || kpi.sourceUrl.startsWith("https://"))) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(kpi.sourceUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // fallback
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fonte", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 1: Purchase Price Input & Steppers
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(
                            text = "1. Prezzo di Acquisto / Asta (€)",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (selectedCurrency != AppCurrency.EUR) "${currencyFormat.format(calculationData.purchasePrice)} (≈ ${formatMoney(calculationData.purchasePrice)})" else formatMoney(calculationData.purchasePrice),
                        color = CyanAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = purchasePriceStr,
                    onValueChange = { purchasePriceStr = it.filter { char -> char.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSlateBg,
                        unfocusedContainerColor = DarkSlateBg,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roi_calc_purchase_price_input")
                )

                // Quick Step Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StepChip("- €25.000") {
                        val curr = purchasePriceStr.toDoubleOrNull() ?: 0.0
                        purchasePriceStr = (curr - 25000.0).coerceAtLeast(10000.0).toInt().toString()
                    }
                    StepChip("- €5.000") {
                        val curr = purchasePriceStr.toDoubleOrNull() ?: 0.0
                        purchasePriceStr = (curr - 5000.0).coerceAtLeast(10000.0).toInt().toString()
                    }
                    StepChip("+ €5.000") {
                        val curr = purchasePriceStr.toDoubleOrNull() ?: 0.0
                        purchasePriceStr = (curr + 5000.0).toInt().toString()
                    }
                    StepChip("+ €25.000") {
                        val curr = purchasePriceStr.toDoubleOrNull() ?: 0.0
                        purchasePriceStr = (curr + 25000.0).toInt().toString()
                    }
                }
            }

            // Section 2: Renovation Costs Input & Tier Presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Text(
                            text = "2. Costi di Ristrutturazione (€)",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (selectedCurrency != AppCurrency.EUR) "${currencyFormat.format(calculationData.renovationCost)} (≈ ${formatMoney(calculationData.renovationCost)})" else formatMoney(calculationData.renovationCost),
                        color = AmberGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = renovationCostStr,
                    onValueChange = { renovationCostStr = it.filter { char -> char.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSlateBg,
                        unfocusedContainerColor = DarkSlateBg,
                        focusedBorderColor = AmberGold,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roi_calc_renovation_input")
                )

                // Renovation Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetChip(
                        label = "Nessuna (€0)",
                        selected = renovationCostStr == "0",
                        onClick = { renovationCostStr = "0" }
                    )
                    PresetChip(
                        label = "Rinfresco (€10k)",
                        selected = renovationCostStr == "10000",
                        onClick = { renovationCostStr = "10000" }
                    )
                    PresetChip(
                        label = "Media (€30k)",
                        selected = renovationCostStr == "30000",
                        onClick = { renovationCostStr = "30000" }
                    )
                    PresetChip(
                        label = "Totale (€60k)",
                        selected = renovationCostStr == "60000",
                        onClick = { renovationCostStr = "60000" }
                    )
                }
            }

            // Section 3: Estimated Monthly Rent Input & Strategy Presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Text(
                            text = "3. Canone Affitto Mensile Stimato (€/m)",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (selectedCurrency != AppCurrency.EUR) "${currencyFormat.format(calculationData.estimatedMonthlyRent)}/m (≈ ${formatMoney(calculationData.estimatedMonthlyRent)}/m)" else "${formatMoney(calculationData.estimatedMonthlyRent)}/m",
                        color = EmeraldGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = estimatedRentStr,
                    onValueChange = { estimatedRentStr = it.filter { char -> char.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSlateBg,
                        unfocusedContainerColor = DarkSlateBg,
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roi_calc_rent_input")
                )

                // Rental Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StepChip("- €100") {
                        val curr = estimatedRentStr.toDoubleOrNull() ?: 0.0
                        estimatedRentStr = (curr - 100.0).coerceAtLeast(100.0).toInt().toString()
                    }
                    StepChip("+ €100") {
                        val curr = estimatedRentStr.toDoubleOrNull() ?: 0.0
                        estimatedRentStr = (curr + 100.0).toInt().toString()
                    }
                    PresetChip(
                        label = "Monolocale (€650)",
                        selected = estimatedRentStr == "650",
                        onClick = { estimatedRentStr = "650" }
                    )
                    PresetChip(
                        label = "Bilocale (€950)",
                        selected = estimatedRentStr == "950",
                        onClick = { estimatedRentStr = "950" }
                    )
                    PresetChip(
                        label = "Stanze / Studenti (€1.400)",
                        selected = estimatedRentStr == "1400",
                        onClick = { estimatedRentStr = "1400" }
                    )
                    PresetChip(
                        label = "Short Rent / Airbnb (€1.900)",
                        selected = estimatedRentStr == "1900",
                        onClick = { estimatedRentStr = "1900" }
                    )
                }
            }

            // Advanced Financing, Taxes & Outgoing Expenses Toggle
            Surface(
                color = DarkSlateBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvancedSettings = !showAdvancedSettings }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (showAdvancedSettings) "Nascondi Imposte, Notaio & Mutuo" else "Parametri Avanzati (Imposte IT, Notaio, Mutuo, IMU)",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showAdvancedSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSlateBg.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Italian Tax & Notary Estimator Card
                    Surface(
                        color = SurfaceCardDark,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
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
                                    Icon(Icons.Default.Gavel, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(15.dp))
                                    Text(
                                        text = "Regime Fiscale & Spese Notarili (Italia)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }
                                TextButton(
                                    onClick = { showTaxDetailDialog = true },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("📋 Dettaglio Fiscale", fontSize = 10.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Acquisition Type Selector Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ItalianAcquisitionType.values().forEach { acqType ->
                                    PresetChip(
                                        label = when (acqType) {
                                            ItalianAcquisitionType.PRIVATE_SECOND_HOME -> "Privato 2ª Casa (9%)"
                                            ItalianAcquisitionType.PRIVATE_FIRST_HOME -> "Privato 1ª Casa (2%)"
                                            ItalianAcquisitionType.COMPANY_VAT_STANDARD -> "Impresa (10% IVA)"
                                            ItalianAcquisitionType.COMPANY_VAT_FIRST_HOME -> "Impresa 1ª Casa (4% IVA)"
                                            ItalianAcquisitionType.AUCTION_JUDICIAL -> "Asta Giudiziaria"
                                        },
                                        selected = selectedAcquisitionType == acqType,
                                        onClick = { selectedAcquisitionType = acqType }
                                    )
                                }
                            }

                            // Breakdown Mini-Grid
                            val tb = calculationData.taxBreakdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = DarkSlateBg,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("Imposte Registro/IVA", fontSize = 9.sp, color = TextSecondaryDark)
                                        Text(formatMoney(tb.totalTaxes), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                                    }
                                }
                                Surface(
                                    color = DarkSlateBg,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("Onorari Notaio + IVA", fontSize = 9.sp, color = TextSecondaryDark)
                                        Text(formatMoney(tb.totalNotaryFees), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                                    }
                                }
                                Surface(
                                    color = DarkSlateBg,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("Totale Accessori", fontSize = 9.sp, color = TextSecondaryDark)
                                        Text(formatMoney(calculationData.totalAncillaryFees), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Inputs for Taxes & Fees
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Imposte Acquisto (€)", fontSize = 11.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = taxFeesStr,
                                onValueChange = { taxFeesStr = it.filter { c -> c.isDigit() } },
                                placeholder = { Text(calculationData.taxBreakdown.totalTaxes.toInt().toString(), fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roi_calc_tax_fees_input")
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Spese Notarili (€)", fontSize = 11.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = notaryFeesStr,
                                onValueChange = { notaryFeesStr = it.filter { c -> c.isDigit() } },
                                placeholder = { Text(calculationData.taxBreakdown.totalNotaryFees.toInt().toString(), fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roi_calc_notary_fees_input")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Altre Spese/Legali (€)", fontSize = 11.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = legalFeesStr,
                                onValueChange = { legalFeesStr = it.filter { c -> c.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roi_calc_legal_fees_input")
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Spese Gest./IMU (€/m)", fontSize = 11.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = monthlyExpensesStr,
                                onValueChange = { monthlyExpensesStr = it.filter { c -> c.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roi_calc_expenses_input")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Acconto Equity (%)", fontSize = 11.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = downPaymentPercentStr,
                                onValueChange = { downPaymentPercentStr = it.filter { c -> c.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roi_calc_downpayment_input")
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tasso Mutuo (%)", fontSize = 11.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = mortgageRateStr,
                                onValueChange = { mortgageRateStr = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roi_calc_mortgage_rate_input")
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Durata (Anni)", fontSize = 11.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = loanTermYearsStr,
                                onValueChange = { loanTermYearsStr = it.filter { c -> c.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roi_calc_loan_term_input")
                            )
                        }
                    }
                }
            }

            // Waterfall Cashflow Summary
            Surface(
                color = DarkSlateBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Riepilogo Costi & Finanziamento",
                        color = TextPrimaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Costo Totale Operazione:", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(formatMoney(calculationData.totalProjectCost), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Imposte & Spese Notarili:", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(formatMoney(calculationData.totalAncillaryFees), fontSize = 11.sp, color = AmberGold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Capitale Proprio Versato:", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(formatMoney(calculationData.initialCashRequired), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Importo Mutuo Richiesto:", fontSize = 11.sp, color = TextSecondaryDark)
                        Text("${formatMoney(calculationData.loanAmount)} (${100 - calculationData.downPaymentPercent.toInt()}% LTV)", fontSize = 11.sp, color = AmberGold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rata Mutuo Stimata:", fontSize = 11.sp, color = TextSecondaryDark)
                        Text("${formatMoney(calculationData.monthlyMortgagePayment)}/mese", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                    }
                }
            }

            // Quick Actions: Copy Summary & Export PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val summaryText = buildString {
                            appendLine("📊 SIMULAZIONE INVESTIMENTO IMMOBILIARE")
                            if (selectedCurrency != AppCurrency.EUR) {
                                val rate = liveRates[selectedCurrency] ?: selectedCurrency.rateFromEur
                                appendLine("💱 Valuta: ${selectedCurrency.code} (${selectedCurrency.symbol}) - Tasso FX: 1 EUR = $rate ${selectedCurrency.code}")
                            }
                            appendLine("💶 Prezzo Acquisto: ${formatMoney(calculationData.purchasePrice)}")
                            appendLine("🔨 Ristrutturazione: ${formatMoney(calculationData.renovationCost)}")
                            appendLine("🏢 Costo Totale: ${formatMoney(calculationData.totalProjectCost)}")
                            appendLine("💰 Canone Affitto: ${formatMoney(calculationData.estimatedMonthlyRent)}/m (${formatMoney(calculationData.annualGrossRent)}/anno)")
                            appendLine("📈 Rendimento Lordo: ${String.format(Locale.US, "%.2f", calculationData.grossYieldPercent)}%")
                            appendLine("⚡ Cash-on-Cash Return: ${String.format(Locale.US, "%.2f", calculationData.cashOnCashReturnPercent)}%")
                            appendLine("💵 Cash Flow Netto: ${formatMoney(calculationData.monthlyNetCashFlow)}/mese (${formatMoney(calculationData.annualNetCashFlow)}/anno)")
                            appendLine("⏳ Payback Period: ${String.format(Locale.US, "%.1f", calculationData.breakEvenYears)} anni")
                        }
                        clipboardManager.setText(AnnotatedString(summaryText))
                        android.widget.Toast.makeText(context, "Sintesi ROI copiata negli appunti!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("roi_calc_copy_summary_btn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copia Sintesi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (onExportPdfClick != null) {
                            onExportPdfClick(calculationData)
                        } else {
                            showExportDialog = true
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("roi_calc_export_pdf_btn")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Esporta PDF", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showExportDialog) {
        PdfReportExportDialog(
            calcData = calculationData,
            initialTitle = propertyTitle,
            initialLocation = propertyLocation,
            initialSurfaceSqm = surfaceSqm,
            onDismissRequest = { showExportDialog = false }
        )
    }

    if (showTaxDetailDialog) {
        ItalianTaxDetailDialog(
            calcData = calculationData,
            onDismissRequest = { showTaxDetailDialog = false }
        )
    }
}

@Composable
fun ItalianTaxDetailDialog(
    calcData: RoiCalculationData,
    onDismissRequest: () -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
    }
    val tb = calcData.taxBreakdown

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCardDark,
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(22.dp))
                        Column {
                            Text(
                                text = "Dettaglio Fiscale & Notarile",
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Normativa Fiscale Italiana (TUIR & Tariffario Notarile)",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextSecondaryDark)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SurfaceCardBorder)

                // Scrollable Itemization
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Regime Card
                    Surface(
                        color = DarkSlateBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Regime di Acquisto Selezionato", fontSize = 10.sp, color = TextSecondaryDark, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = when (calcData.acquisitionType) {
                                    ItalianAcquisitionType.PRIVATE_SECOND_HOME -> "Acquisto da Privato (Seconda Casa) - Reg. 9%"
                                    ItalianAcquisitionType.PRIVATE_FIRST_HOME -> "Acquisto da Privato (Prima Casa) - Reg. 2%"
                                    ItalianAcquisitionType.COMPANY_VAT_STANDARD -> "Acquisto da Impresa Costruttrice - IVA 10%"
                                    ItalianAcquisitionType.COMPANY_VAT_FIRST_HOME -> "Acquisto da Impresa (Prima Casa) - IVA 4%"
                                    ItalianAcquisitionType.AUCTION_JUDICIAL -> "Acquisto in Asta Giudiziaria - Registro Proporzionale"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                            Text(
                                text = "Base imponibile: ${currencyFormat.format(tb.estimatedCadastralValue)} (Prezzo Immobile: ${currencyFormat.format(calcData.purchasePrice)})",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    // Section: Imposte di Trasferimento
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("1. Imposte Statali d'Atto (Agenzia delle Entrate)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                        TaxLineItem("Imposta di Registro / IVA", tb.registrationOrVatTax, currencyFormat)
                        TaxLineItem("Imposta Ipotecaria & Catastale Fissa", tb.fixedRegistryIpoCatTaxes, currencyFormat)
                        HorizontalDivider(color = SurfaceCardBorder)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotale Imposte Statali:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text(currencyFormat.format(tb.totalTaxes), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                        }
                    }

                    // Section: Onorari Notarili
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("2. Competenze Notarili (D.M. 140/2012)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        TaxLineItem("Onorario Atto Compravendita", tb.notaryDeedSaleFee, currencyFormat)
                        if (tb.notaryDeedMortgageFee > 0) {
                            TaxLineItem("Onorario Atto di Mutuo", tb.notaryDeedMortgageFee, currencyFormat)
                        }
                        TaxLineItem("Cassa Notariato, Bolli & IVA 22%", tb.notaryExpensesAndVat, currencyFormat)
                        HorizontalDivider(color = SurfaceCardBorder)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotale Spese Notarili (IVA inc.):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text(currencyFormat.format(tb.totalNotaryFees), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        }
                    }

                    // Section: Altre Spese Accessorie
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("3. Oneri di Finanziamento & Agenzia", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        if (tb.loanSubstituteTax > 0) {
                            TaxLineItem("Imposta Sostitutiva Mutuo (0.25% / 2.0%)", tb.loanSubstituteTax, currencyFormat)
                        }
                        if (tb.agencyCommission > 0) {
                            TaxLineItem("Provvigione Agenzia Immobiliare (+IVA 22%)", tb.agencyCommission, currencyFormat)
                        }
                        if (calcData.legalFees > 0) {
                            TaxLineItem("Spese Tecniche / Perizia / Asta", calcData.legalFees, currencyFormat)
                        }
                    }

                    // Grand Total Hero
                    Surface(
                        color = CyanAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Totale Costi Accessori Acquisto:", fontSize = 11.sp, color = TextSecondaryDark)
                                Text("Aggiunti al prezzo d'acquisto", fontSize = 10.sp, color = TextMutedDark)
                            }
                            Text(
                                text = currencyFormat.format(calcData.totalAncillaryFees),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                ) {
                    Text("Ho Capito", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun TaxLineItem(
    label: String,
    amount: Double,
    formatter: NumberFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = TextSecondaryDark)
        Text(text = formatter.format(amount), fontSize = 11.sp, color = TextPrimaryDark, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HeroMetricBox(
    label: String,
    value: String,
    subtext: String,
    valueColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, valueColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, color = TextSecondaryDark, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = value,
                color = valueColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.testTag(testTag)
            )
            Text(text = subtext, color = TextMutedDark, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun MiniMetricBox(
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, color = TextSecondaryDark, fontSize = 9.sp)
            Text(
                text = value,
                color = TextPrimaryDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(tag)
            )
        }
    }
}

@Composable
private fun StepChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = SurfaceCardDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) CyanAccent else SurfaceCardBorder,
        animationSpec = tween(200)
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) CyanAccent.copy(alpha = 0.15f) else SurfaceCardDark,
        animationSpec = tween(200)
    )
    val textColor = if (selected) CyanAccent else TextSecondaryDark

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
