package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertyDeal
import com.example.ui.RoiCalculatorState
import com.example.ui.components.InteractiveRoiCalculatorCard
import com.example.ui.components.SeniorAppraisalDialog
import com.example.ui.components.ImmobiliareObservatoryDialog
import com.example.ui.theme.*
import com.example.util.PropertyPdfGenerator
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RoiCalculatorScreen(
    state: RoiCalculatorState,
    onPurchasePriceChange: (String) -> Unit,
    onRenovationChange: (String) -> Unit,
    onLegalFeesChange: (String) -> Unit,
    onMonthlyRentChange: (String) -> Unit,
    onMonthlyExpensesChange: (String) -> Unit,
    onDownPaymentPercentChange: (String) -> Unit,
    onMortgageRateChange: (String) -> Unit,
    onLoanTermYearsChange: (String) -> Unit,
    onResaleChange: (String) -> Unit,
    onApplyPreset: (downPayment: String, mortgageRate: String, loanTerm: String) -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
            maximumFractionDigits = 0
        }
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Rental / Cash-on-Cash, 1 = Fix & Flip
    var showSeniorAppraisalDialog by remember { mutableStateOf(false) }
    var showImmobiliareObservatoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(EmeraldGreen, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column {
                Text(
                    text = "Calcolatore Rendimento & Cash-on-Cash",
                    color = TextPrimaryDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Analisi finanziaria completa per investimenti immobiliari",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }
        }

        // Interactive ROI Calculator Component
        InteractiveRoiCalculatorCard(
            initialPurchasePrice = state.purchasePrice,
            initialRenovationCost = state.renovationCost,
            initialEstimatedRent = state.monthlyRent,
            initialLegalFees = state.legalFees,
            initialMonthlyExpenses = state.monthlyExpenses,
            initialDownPaymentPercent = state.downPaymentPercent,
            initialMortgageRate = state.mortgageRate,
            initialLoanTermYears = state.loanTermYears,
            onCalculateCompleted = { calcData ->
                // Keep parent state in sync if changed
                if (calcData.purchasePrice.toInt().toString() != state.purchasePriceStr && calcData.purchasePrice > 0) {
                    onPurchasePriceChange(calcData.purchasePrice.toInt().toString())
                }
                if (calcData.renovationCost.toInt().toString() != state.renovationCostStr) {
                    onRenovationChange(calcData.renovationCost.toInt().toString())
                }
                if (calcData.estimatedMonthlyRent.toInt().toString() != state.monthlyRentStr && calcData.estimatedMonthlyRent > 0) {
                    onMonthlyRentChange(calcData.estimatedMonthlyRent.toInt().toString())
                }
            },
            modifier = Modifier.testTag("interactive_roi_calculator_main_card")
        )

        // Preset Scenario Chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Preset Finanziamento Rapido:",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = { onApplyPreset("100", "0.0", "25") },
                    label = { Text("100% Cash (Senza Mutuo)", fontSize = 11.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = SurfaceCardDark,
                        labelColor = TextPrimaryDark
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = SurfaceCardBorder
                    ),
                    modifier = Modifier.testTag("preset_chip_cash")
                )
                SuggestionChip(
                    onClick = { onApplyPreset("20", "3.2", "25") },
                    label = { Text("Mutuo Standard (80% LTV, 3.2%)", fontSize = 11.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = SurfaceCardDark,
                        labelColor = TextPrimaryDark
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = SurfaceCardBorder
                    ),
                    modifier = Modifier.testTag("preset_chip_standard")
                )
                SuggestionChip(
                    onClick = { onApplyPreset("30", "3.8", "20") },
                    label = { Text("Mutuo Investor (70% LTV, 3.8%)", fontSize = 11.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = SurfaceCardDark,
                        labelColor = TextPrimaryDark
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = SurfaceCardBorder
                    ),
                    modifier = Modifier.testTag("preset_chip_investor")
                )
            }
        }

        // Mode Switcher (Tab)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceCardDark,
            contentColor = EmeraldGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = EmeraldGreen
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Messa a Reddito (CoC & Yield)",
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                    )
                },
                modifier = Modifier.testTag("tab_rental_strategy")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Fix & Flip (Rivendita)",
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                    )
                },
                modifier = Modifier.testTag("tab_flip_strategy")
            )
        }

        // Section: Hero KPI Summary Dashboard Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = BentoPurpleContainer.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) "Rendimento Locazione (Cash-on-Cash)" else "Rendimento Operazione Flip",
                        color = BentoPurpleOnContainer,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = BentoPurpleHeader
                    ) {
                        Text(
                            text = "CAPITALE PROP.: ${currencyFormat.format(state.initialCashRequired)}",
                            color = BentoPurpleOnContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = SurfaceCardBorder)

                if (selectedTab == 0) {
                    // Primary Highlight: Cash-on-Cash Return
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.5.dp, EmeraldGreen, RoundedCornerShape(16.dp)),
                            color = Color.Black.copy(alpha = 0.4f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Cash-on-Cash Return (CoC)",
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", state.cashOnCashReturnPercent)}%",
                                    color = EmeraldGreen,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Ritorno annuo sul capitale proprio versato (€${currencyFormat.format(state.initialCashRequired)})",
                                    color = TextMutedDark,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            color = Color.Black.copy(alpha = 0.4f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Cash Flow Netto Mensile",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val cfColor = if (state.monthlyNetCashFlow >= 0) CyanAccent else AmberGold
                                Text(
                                    text = currencyFormat.format(state.monthlyNetCashFlow),
                                    color = cfColor,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Affitto - Spese - Rata Mutuo (${currencyFormat.format(state.annualNetCashFlow)}/anno)",
                                    color = TextMutedDark,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Secondary Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricResultCard(
                            title = "Rendimento Lordo",
                            value = "${String.format(Locale.US, "%.1f", state.grossAnnualYieldPercent)}%",
                            subtext = "Canone annuo / Costo tot.",
                            color = TextPrimaryDark,
                            modifier = Modifier.weight(1f)
                        )
                        MetricResultCard(
                            title = "Cap Rate Netto",
                            value = "${String.format(Locale.US, "%.1f", state.netCapRatePercent)}%",
                            subtext = "NOI / Costo totale",
                            color = PurpleIndigo,
                            modifier = Modifier.weight(1f)
                        )
                        MetricResultCard(
                            title = "Rata Mutuo",
                            value = currencyFormat.format(state.monthlyMortgagePayment),
                            subtext = "Quota mensile banca",
                            color = AmberGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Fix & Flip View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricResultCard(
                            title = "Profitto Netto Flip",
                            value = currencyFormat.format(state.totalFlipProfit),
                            subtext = "Capital gain atteso da rivendita",
                            color = AmberGold,
                            modifier = Modifier.weight(1f)
                        )
                        MetricResultCard(
                            title = "ROI Totale Operazione",
                            value = "${String.format(Locale.US, "%.1f", state.flipROIPercent)}%",
                            subtext = "Profitto / Costo Totale Progetto",
                            color = PurpleIndigo,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section: Purchase & Renovation Parameters Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Text(
                            text = "1. Acquisto & Costi Iniziali",
                            color = CyanAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { showImmobiliareObservatoryDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_roi_immobiliare_observatory")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Immobiliare.it",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }

                        OutlinedButton(
                            onClick = { showSeniorAppraisalDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_senior_appraisal")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Perizia UNI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoiInputField(
                        label = "Prezzo Acquisto / Asta (€)",
                        value = state.purchasePriceStr,
                        onValueChange = onPurchasePriceChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roi_input_price")
                    )
                    RoiInputField(
                        label = "Costo Ristrutturazione (€)",
                        value = state.renovationCostStr,
                        onValueChange = onRenovationChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roi_input_renovation")
                    )
                }

                RoiInputField(
                    label = "Spese Legali, Notarili & Asta (€)",
                    value = state.legalAuctionFeesStr,
                    onValueChange = onLegalFeesChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roi_input_fees")
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSlateBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Costo Totale Progetto (Acquisto+Lavori+Spese):",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = currencyFormat.format(state.totalProjectCost),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                }
            }
        }

        // Section: Financing & Loan Parameters Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                    Text(
                        text = "2. Finanziamento & Mutuo",
                        color = EmeraldGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoiInputField(
                        label = "Acconto Cap. Proprio (%)",
                        value = state.downPaymentPercentStr,
                        onValueChange = onDownPaymentPercentChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roi_input_downpayment")
                    )
                    RoiInputField(
                        label = "Tasso d'Interesse Anno (%)",
                        value = state.mortgageRateStr,
                        onValueChange = onMortgageRateChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roi_input_rate")
                    )
                    RoiInputField(
                        label = "Durata (Anni)",
                        value = state.loanTermYearsStr,
                        onValueChange = onLoanTermYearsChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roi_input_term")
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSlateBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Importo Mutuo: ${currencyFormat.format(state.loanAmount)} (${state.LTVPercentStr}% LTV)",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = "Capitale Iniziale Versato (Equity+Lavori): ${currencyFormat.format(state.initialCashRequired)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldGreen
                            )
                        }
                        Text(
                            text = "${currencyFormat.format(state.monthlyMortgagePayment)}/m",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold
                        )
                    }
                }
            }
        }

        // Section: Income & Operating Expenses Card (For Rental Mode)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                    Text(
                        text = "3. Affitto & Spese Operative Mensili",
                        color = AmberGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoiInputField(
                        label = "Canone Affitto (€/mese)",
                        value = state.monthlyRentStr,
                        onValueChange = onMonthlyRentChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roi_input_rent")
                    )
                    RoiInputField(
                        label = "Spese Gestione/IMU (€/mese)",
                        value = state.monthlyExpensesStr,
                        onValueChange = onMonthlyExpensesChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roi_input_expenses")
                    )
                }

                RoiInputField(
                    label = "Target Prezzo Rivendita / Flip (€)",
                    value = state.expectedResaleStr,
                    onValueChange = onResaleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roi_input_resale")
                )
            }
        }

        // Section: Cash Flow Waterfall Breakdown Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Dettaglio Flusso di Cassa (Cash Flow Waterfall)",
                    color = TextPrimaryDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(color = SurfaceCardBorder)

                WaterfallRow("Canone Affitto Lordo Annuo", "+ ${currencyFormat.format(state.annualGrossRent)}", EmeraldGreen)
                WaterfallRow("Spese Operative (IMU, Condominio, Gestione)", "- ${currencyFormat.format(state.annualExpenses)}", AmberGold)
                WaterfallRow("Net Operating Income (NOI)", "= ${currencyFormat.format(state.netOperatingIncome)}", CyanAccent)
                WaterfallRow("Servizio del Debito (Rate Mutuo Annue)", "- ${currencyFormat.format(state.annualDebtService)}", AmberGold)

                HorizontalDivider(color = SurfaceCardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cash Flow Netto Annuo:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = currencyFormat.format(state.annualNetCashFlow),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (state.annualNetCashFlow >= 0) EmeraldGreen else Color(0xFFFF5252)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Formula Cash-on-Cash:",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "${currencyFormat.format(state.annualNetCashFlow)} / ${currencyFormat.format(state.initialCashRequired)} = ${String.format(Locale.US, "%.2f", state.cashOnCashReturnPercent)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                }
            }
        }

        // Export PDF Dossier Section
        val context = LocalContext.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Esporta Report Simulatone PDF",
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Genera e invia via email un report PDF professionale basato sui parametri finanziari inseriti in questo calcolatore.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val simulatedDeal = PropertyDeal(
                                id = 999L,
                                title = "Simulazione Investimento Immobiliare",
                                sourceKey = "roi_calc",
                                sourceName = "Calcolatore ROI",
                                sourceUrl = "",
                                location = "Italia",
                                propertyType = "Residenziale",
                                askingPrice = state.purchasePrice,
                                estimatedMarketValue = state.expectedResale.takeIf { it > 0 } ?: (state.purchasePrice * 1.2),
                                surfaceSqm = 85,
                                discountPercent = 15,
                                estimatedCapRate = state.cashOnCashReturnPercent.coerceAtLeast(1.0),
                                status = "SIMULATED",
                                notes = "Simulazione ROI: Cash Required ${currencyFormat.format(state.initialCashRequired)}, Monthly Rent ${currencyFormat.format(state.monthlyRent)}"
                            )
                            PropertyPdfGenerator.generateAndSharePdf(context, simulatedDeal, emailOnly = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roi_export_pdf_email_btn")
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Invia via Email", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val simulatedDeal = PropertyDeal(
                                id = 999L,
                                title = "Simulazione Investimento Immobiliare",
                                sourceKey = "roi_calc",
                                sourceName = "Calcolatore ROI",
                                sourceUrl = "",
                                location = "Italia",
                                propertyType = "Residenziale",
                                askingPrice = state.purchasePrice,
                                estimatedMarketValue = state.expectedResale.takeIf { it > 0 } ?: (state.purchasePrice * 1.2),
                                surfaceSqm = 85,
                                discountPercent = 15,
                                estimatedCapRate = state.cashOnCashReturnPercent.coerceAtLeast(1.0),
                                status = "SIMULATED",
                                notes = "Simulazione ROI: Cash Required ${currencyFormat.format(state.initialCashRequired)}, Monthly Rent ${currencyFormat.format(state.monthlyRent)}"
                            )
                            PropertyPdfGenerator.generateAndSharePdf(context, simulatedDeal, emailOnly = false)
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roi_share_pdf_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Condividi PDF", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showSeniorAppraisalDialog) {
            val initialPrice = state.purchasePrice.takeIf { it > 0 } ?: 200000.0
            SeniorAppraisalDialog(
                initialCoveredSqM = 85.0,
                initialPricePerSqM = (initialPrice / 85.0),
                onDismissRequest = { showSeniorAppraisalDialog = false },
                onApplyValuation = { estimatedValue, _ ->
                    onPurchasePriceChange(estimatedValue.toInt().toString())
                    onResaleChange((estimatedValue * 1.15).toInt().toString())
                }
            )
        }

        if (showImmobiliareObservatoryDialog) {
            ImmobiliareObservatoryDialog(
                initialMunicipality = "Paderno Dugnano",
                onDismissRequest = { showImmobiliareObservatoryDialog = false },
                onApplyPricePerSqM = { pricePerSqM, _ ->
                    val estPurchase = (85.0 * pricePerSqM * 0.80).toInt() // 20% discount target
                    val estResale = (85.0 * pricePerSqM).toInt()
                    val estRent = (85.0 * (pricePerSqM * 0.005)).toInt()
                    onPurchasePriceChange(estPurchase.toString())
                    onResaleChange(estResale.toString())
                    onMonthlyRentChange(estRent.toString())
                    showImmobiliareObservatoryDialog = false
                }
            )
        }
    }
}

@Composable
private fun WaterfallRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = TextSecondaryDark)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun RoiInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = TextSecondaryDark, fontSize = 11.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
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
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MetricResultCard(
    title: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
        color = Color.Black.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = title, color = TextSecondaryDark, fontSize = 10.sp)
            Text(text = value, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = subtext, color = TextMutedDark, fontSize = 9.sp)
        }
    }
}
