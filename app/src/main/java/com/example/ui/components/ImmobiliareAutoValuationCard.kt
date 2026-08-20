package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.AutomatedMarketValuation
import com.example.util.ImmobiliareObservatoryService
import com.example.util.MarketDealGrade
import com.example.util.SeniorValuationEngine
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ImmobiliareAutoValuationCard(
    valuation: AutomatedMarketValuation,
    onApplyRoi: (() -> Unit)? = null,
    onOpenObservatory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 } }
    var isExpanded by remember { mutableStateOf(true) }
    var showStressTestSection by remember { mutableStateOf(false) }
    var selectedTaxRegime by remember { mutableStateOf(SeniorValuationEngine.TaxRegime.CEDOLARE_SECCA_CONCORDATO) }

    val underwritingResult = remember(valuation.askingPrice, valuation.surfaceSqM, valuation.estimatedMonthlyRent, selectedTaxRegime) {
        SeniorValuationEngine.performAdvancedUnderwriting(
            SeniorValuationEngine.AdvancedUnderwritingInput(
                purchasePrice = valuation.askingPrice,
                renovationCost = valuation.surfaceSqM * 400.0,
                estimatedMonthlyRent = valuation.estimatedMonthlyRent,
                resaleTargetPrice = valuation.estimatedMarketValueRenovated,
                taxRegime = selectedTaxRegime
            )
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardBgLight),
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(
                    CyanAccent.copy(alpha = 0.5f),
                    SurfaceCardBorder,
                    BentoBlueOnContainer.copy(alpha = 0.3f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("immobiliare_auto_valuation_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Bar
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Valutazione Benchmark",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CyanAccent.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = valuation.municipality.provenance,
                                    color = CyanAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${valuation.municipality.municipalityName} ${if (valuation.matchedSubZone != null) "• ${valuation.matchedSubZone.name}" else ""} (+${valuation.municipality.trendSaleYoY}% YoY)",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                if (valuation.isValid) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondaryDark
                        )
                    }
                }
            }

            // Generic Fallback Warning Banner
            if (valuation.municipality.isGenericFallback) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AmberGold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Nessun dato disponibile per questo comune: valore nazionale generico",
                            fontSize = 10.sp,
                            color = AmberGold,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!valuation.isValid) {
                // Invalid State Banner - Missing Fields
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RoseRed.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, RoseRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = RoseRed, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Dati insufficienti per il calcolo della valutazione",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseRed
                            )
                        }
                        Text(
                            text = "Campi mancanti: ${valuation.missingFields.joinToString(", ")}",
                            fontSize = 11.sp,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Inserisci la superficie e il prezzo richiesto per abilitare le stime di mercato e la simulazione ROI.",
                            fontSize = 10.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            } else {
                // Deal Grade Pill Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (valuation.dealGrade.isPositive) EmeraldGreen.copy(alpha = 0.12f) else RoseRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (valuation.dealGrade.isPositive) EmeraldGreen.copy(alpha = 0.35f) else RoseRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = valuation.dealGrade.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (valuation.dealGrade.isPositive) EmeraldGreen else RoseRed
                        )
                        Text(
                            text = if (valuation.discountVsMarketPercent >= 0) {
                                "-${String.format(Locale.ITALY, "%.1f", valuation.discountVsMarketPercent)}% vs Media"
                            } else {
                                "+${String.format(Locale.ITALY, "%.1f", -valuation.discountVsMarketPercent)}% vs Media"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (valuation.discountVsMarketPercent >= 0) EmeraldGreen else AmberGold
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Spread Metrics Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceCardDark,
                                border = BorderStroke(1.dp, SurfaceCardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Prezzo Immobile", fontSize = 10.sp, color = TextMutedDark)
                                    Text("€${valuation.askingPricePerSqM.toInt()}/m²", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                    Text("Richiesta: ${currencyFormat.format(valuation.askingPrice)}", fontSize = 9.sp, color = TextSecondaryDark)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceCardDark,
                                border = BorderStroke(1.dp, SurfaceCardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Media Zona", fontSize = 10.sp, color = TextMutedDark)
                                    Text("€${valuation.zoneAvgPricePerSqM.toInt()}/m²", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                                    Text("Stima: ${currencyFormat.format(valuation.estimatedMarketValueAsIs)}", fontSize = 9.sp, color = TextSecondaryDark)
                                }
                            }
                        }

                        // 2 Strategy KPI Cards: Fix & Flip vs Buy & Hold
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Fix & Flip Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceCardDark,
                                border = BorderStroke(1.dp, BentoBlueOnContainer.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                        Text("Fix & Flip", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                                    }
                                    Text(
                                        text = "Profitto: +${currencyFormat.format(valuation.estimatedFlipGrossProfit)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldGreen
                                    )
                                    Text(
                                        text = "ROI Stimato: ${String.format(Locale.ITALY, "%.1f", valuation.estimatedFlipRoiPercent)}%",
                                        fontSize = 10.sp,
                                        color = TextSecondaryDark
                                    )
                                    Text(
                                        text = "Rivendita: €${valuation.zoneMaxPricePerSqM.toInt()}/m²",
                                        fontSize = 9.sp,
                                        color = TextMutedDark
                                    )
                                }
                            }

                            // Buy & Hold Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceCardDark,
                                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                                        Text("Buy & Hold", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                                    }
                                    Text(
                                        text = "${currencyFormat.format(valuation.estimatedMonthlyRent)}/mese",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AmberGold
                                    )
                                    Text(
                                        text = "Gross Yield: ${String.format(Locale.ITALY, "%.2f", valuation.grossRentalYieldPercent)}%",
                                        fontSize = 10.sp,
                                        color = TextSecondaryDark
                                    )
                                    Text(
                                        text = "Cap Rate: ~${String.format(Locale.ITALY, "%.2f", valuation.capRatePercent)}%",
                                        fontSize = 9.sp,
                                        color = TextMutedDark
                                    )
                                }
                            }
                        }

                        // Liquidity & Days on Market footnote
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱️ ${valuation.liquidityRating}",
                                fontSize = 10.sp,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = "Affitti: €${String.format(Locale.ITALY, "%.2f", valuation.municipality.avgRentPricePerSqMMonth)}/m²/m",
                                fontSize = 10.sp,
                                color = TextSecondaryDark
                            )
                        }

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onOpenObservatory != null || valuation.municipality.officialUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        if (onOpenObservatory != null) {
                                            onOpenObservatory()
                                        } else {
                                            ImmobiliareObservatoryService.openOfficialObservatory(context, valuation.municipality.officialUrl)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_card_open_observatory")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dati Zona", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (onApplyRoi != null) {
                                Button(
                                    onClick = onApplyRoi,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .testTag("btn_card_apply_roi")
                                ) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simula ROI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

                    // Senior Underwriting Expander Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceCardDark,
                        border = BorderStroke(1.dp, BentoBlueOnContainer.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStressTestSection = !showStressTestSection }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Analisi Underwriting & Stress Test Senior",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }
                            Icon(
                                imageVector = if (showStressTestSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showStressTestSection) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceCardDark.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Regime Fiscale & Parametri Finanziari",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )

                            // Tax regime selector tabs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SeniorValuationEngine.TaxRegime.values().forEach { regime ->
                                    val isSelected = selectedTaxRegime == regime
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) CyanAccent else SurfaceCardDark,
                                        border = BorderStroke(1.dp, if (isSelected) CyanAccent else SurfaceCardBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedTaxRegime = regime }
                                    ) {
                                        Text(
                                            text = if (regime == SeniorValuationEngine.TaxRegime.CEDOLARE_SECCA_CONCORDATO) "Cedolare 10%"
                                            else if (regime == SeniorValuationEngine.TaxRegime.CEDOLARE_SECCA_LIBERO) "Cedolare 21%"
                                            else "IRPEF 28%",
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.Black else TextSecondaryDark,
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // 4 Institutional Metrics Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BentoCardBgLight,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("Levered CoC", fontSize = 9.sp, color = TextMutedDark)
                                        Text(
                                            "${String.format(Locale.ITALY, "%.1f", underwritingResult.leveredCashOnCashPercent)}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BentoCardBgLight,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("5Y IRR Stimato", fontSize = 9.sp, color = TextMutedDark)
                                        Text(
                                            "${String.format(Locale.ITALY, "%.1f", underwritingResult.fiveYearIrrPercent)}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanAccent
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BentoCardBgLight,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("DSCR Mutuo", fontSize = 9.sp, color = TextMutedDark)
                                        Text(
                                            "${String.format(Locale.ITALY, "%.2f", underwritingResult.dscr)}x",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (underwritingResult.dscr >= 1.25) EmeraldGreen else AmberGold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BentoCardBgLight,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("Break-Even", fontSize = 9.sp, color = TextMutedDark)
                                        Text(
                                            "${String.format(Locale.ITALY, "%.0f", underwritingResult.breakEvenOccupancyPercent)}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                    }
                                }
                            }

                            // 3 Stress Test Scenarios
                            Text(
                                text = "Matrice di Stress Test & Scenari",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            underwritingResult.stressTests.forEach { scenario ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BentoCardBgLight,
                                    border = BorderStroke(0.8.dp, if (scenario.isViable) SurfaceCardBorder else RoseRed.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = scenario.name,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark
                                            )
                                            Text(
                                                text = if (scenario.flipNetProfit >= 0) "+${currencyFormat.format(scenario.flipNetProfit)}" else currencyFormat.format(scenario.flipNetProfit),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (scenario.flipNetProfit >= 0) EmeraldGreen else RoseRed
                                            )
                                        }
                                        Text(
                                            text = scenario.scenarioDescription,
                                            fontSize = 9.sp,
                                            color = TextSecondaryDark
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Cash Flow Netto: ${currencyFormat.format(scenario.netAnnualCashFlow)}/anno",
                                                fontSize = 9.sp,
                                                color = if (scenario.netAnnualCashFlow >= 0) EmeraldGreen else RoseRed
                                            )
                                            Text(
                                                text = "CoC: ${String.format(Locale.ITALY, "%.1f", scenario.leveredCashOnCashPercent)}%",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyanAccent
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
