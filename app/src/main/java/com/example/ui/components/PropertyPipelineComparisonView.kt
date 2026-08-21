package com.example.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PipelineStatus
import com.example.data.Property
import com.example.ui.theme.*
import com.example.util.ImageUtils
import java.text.NumberFormat
import java.util.Locale

/**
 * Sensitivity Scenario Model for Real-Time Projection Simulation
 */
enum class ComparisonScenario(
    val title: String,
    val description: String,
    val saleMultiplier: Double,
    val renoMultiplier: Double
) {
    BASELINE("Scenario Base", "Stime attuali di mercato e budget preventivati", 1.0, 1.0),
    CONSERVATIVE("Prudenziale", "-10% Rivendita | +15% Costi Lavori", 0.90, 1.15),
    OPTIMISTIC("Ottimista", "+10% Rivendita | Lavori in Budget", 1.10, 1.0)
}

/**
 * Calculated Projected Metrics for a Property under a given scenario.
 */
data class PropertyProjectedKpi(
    val property: Property,
    val purchasePrice: Double,
    val renoCost: Double,
    val totalCostBasis: Double,
    val exitValue: Double,
    val grossProfit: Double,
    val roiPercent: Double,
    val pricePerSqm: Double,
    val exitPricePerSqm: Double,
    val rentalIncomeMonthly: Double,
    val grossRentalYield: Double,
    val paybackMonths: Double,
    val renoToPurchaseRatio: Double
)

/**
 * Side-by-Side Property Comparison View for 'My Properties'.
 * Allows real-time side-by-side contrasting of KPIs, Cost Bases, and ROI Projections.
 */
@Composable
fun PropertyPipelineComparisonView(
    allProperties: List<Property>,
    selectedPropertyIds: Set<Long>,
    onTogglePropertySelection: (Long) -> Unit,
    onSelectAllProperties: () -> Unit,
    onClearSelection: () -> Unit,
    euroFormat: NumberFormat,
    onCalculateRoiClick: (Property) -> Unit,
    onEditFinancialsClick: (Property) -> Unit,
    onUpdateStatusClick: (Property) -> Unit,
    onUpdateProgressClick: (Property) -> Unit,
    onSimulatePriceDropClick: (Property) -> Unit,
    onExportPdfClick: (Property) -> Unit,
    onDeleteClick: (Property) -> Unit,
    onOpenComparisonPdfDialog: ((Property?, Property?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentScenario by remember { mutableStateOf(ComparisonScenario.BASELINE) }
    var selectedStrategyFilter by remember { mutableStateOf("TUTTE") }

    // Properties to compare: if specific properties are selected in multi-select, compare those;
    // otherwise compare all properties filtered by strategy, up to 6 properties.
    val comparedProperties = remember(allProperties, selectedPropertyIds, selectedStrategyFilter) {
        val baseList = if (selectedPropertyIds.isNotEmpty()) {
            allProperties.filter { selectedPropertyIds.contains(it.id) }
        } else {
            allProperties
        }

        if (selectedStrategyFilter == "TUTTE") {
            baseList
        } else {
            baseList.filter { it.strategyTags.contains(selectedStrategyFilter, ignoreCase = true) }
        }
    }

    // Calculate Scenario-adjusted KPIs for each compared property
    val projectedKpis = remember(comparedProperties, currentScenario) {
        comparedProperties.map { prop ->
            val baseReno = if (prop.actualRenovationCost > 0) prop.actualRenovationCost else prop.estimatedRenovationCost
            val adjustedReno = baseReno * currentScenario.renoMultiplier
            val totalCost = prop.price + adjustedReno
            val baseExit = prop.effectiveExitValue
            val adjustedExit = baseExit * currentScenario.saleMultiplier
            val profit = adjustedExit - totalCost
            val roi = if (totalCost > 0) (profit / totalCost) * 100.0 else 0.0
            val sqm = if (prop.surfaceSqm > 0) prop.surfaceSqm else 90
            val pricePerSqm = if (sqm > 0) prop.price / sqm else 0.0
            val exitPerSqm = if (sqm > 0) adjustedExit / sqm else 0.0
            val rentMonthly = prop.projectedRentalIncome
            val rentYield = if (totalCost > 0 && rentMonthly > 0) (rentMonthly * 12.0 / totalCost) * 100.0 else 0.0
            val payback = if (rentMonthly > 0) (totalCost / rentMonthly) else 0.0
            val renoRatio = if (prop.price > 0) (adjustedReno / prop.price) * 100.0 else 0.0

            PropertyProjectedKpi(
                property = prop,
                purchasePrice = prop.price,
                renoCost = adjustedReno,
                totalCostBasis = totalCost,
                exitValue = adjustedExit,
                grossProfit = profit,
                roiPercent = roi,
                pricePerSqm = pricePerSqm,
                exitPricePerSqm = exitPerSqm,
                rentalIncomeMonthly = rentMonthly,
                grossRentalYield = rentYield,
                paybackMonths = payback,
                renoToPurchaseRatio = renoRatio
            )
        }
    }

    // Benchmark Leaders
    val maxRoiProp = remember(projectedKpis) { projectedKpis.maxByOrNull { it.roiPercent } }
    val maxProfitProp = remember(projectedKpis) { projectedKpis.maxByOrNull { it.grossProfit } }
    val minCostProp = remember(projectedKpis) { projectedKpis.minByOrNull { it.totalCostBasis } }
    val minPriceSqmProp = remember(projectedKpis) { projectedKpis.filter { it.pricePerSqm > 0 }.minByOrNull { it.pricePerSqm } }
    val maxYieldProp = remember(projectedKpis) { projectedKpis.filter { it.grossRentalYield > 0 }.maxByOrNull { it.grossRentalYield } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("property_pipeline_comparison_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Selector Bar & Portfolio Asset Filter Pills
        ComparisonAssetSelectorCard(
            allProperties = allProperties,
            selectedIds = selectedPropertyIds,
            selectedStrategy = selectedStrategyFilter,
            onSelectStrategy = { selectedStrategyFilter = it },
            onToggleSelection = onTogglePropertySelection,
            onSelectAll = onSelectAllProperties,
            onClearSelection = onClearSelection
        )

        if (comparedProperties.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = null,
                        tint = BentoPurpleOnContainer,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Nessun Immobile da Confrontare",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Seleziona almeno 2 immobili dalla barra in alto per avviare il confronto side-by-side.",
                        fontSize = 13.sp,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // 2. Scenario Simulation Selector Bar (Baseline / Conservative / Optimistic)
            ScenarioSensitivitySelectorBar(
                currentScenario = currentScenario,
                onSelectScenario = { currentScenario = it }
            )

            // 2b. PDF Comparison Report Action Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceCardDark,
                border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BentoPurpleHeader,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = BentoPurpleOnContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Dossier di Confronto PDF (Side-by-Side)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = if (comparedProperties.size >= 2) "Genera foglio comparativo istituzionale per ${comparedProperties[0].title.ifBlank { "Immobile A" }} vs ${comparedProperties[1].title.ifBlank { "Immobile B" }}" else "Seleziona 2 immobili per esportare il dossier comparativo",
                                fontSize = 11.sp,
                                color = TextSecondaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val propA = comparedProperties.getOrNull(0)
                            val propB = comparedProperties.getOrNull(1)
                            onOpenComparisonPdfDialog?.invoke(propA, propB)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOnContainer),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("export_comparison_pdf_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Esporta PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Top Executive Comparison Highlights Banner
            if (projectedKpis.size >= 2) {
                ComparisonHighlightsBanner(
                    maxRoiProp = maxRoiProp,
                    maxProfitProp = maxProfitProp,
                    minCostProp = minCostProp,
                    maxYieldProp = maxYieldProp,
                    euroFormat = euroFormat
                )
            }

            // 4. Side-by-Side Comparison Matrix Table
            ComparisonMatrixTable(
                kpis = projectedKpis,
                maxRoiPropId = maxRoiProp?.property?.id,
                maxProfitPropId = maxProfitProp?.property?.id,
                minCostPropId = minCostProp?.property?.id,
                minPriceSqmPropId = minPriceSqmProp?.property?.id,
                maxYieldPropId = maxYieldProp?.property?.id,
                euroFormat = euroFormat,
                onCalculateRoiClick = onCalculateRoiClick,
                onEditFinancialsClick = onEditFinancialsClick,
                onUpdateStatusClick = onUpdateStatusClick,
                onUpdateProgressClick = onUpdateProgressClick,
                onSimulatePriceDropClick = onSimulatePriceDropClick,
                onExportPdfClick = onExportPdfClick,
                onDeleteClick = onDeleteClick,
                onRemoveFromComparison = { propId ->
                    onTogglePropertySelection(propId)
                }
            )
        }
    }
}

/**
 * Top Selector Card allowing users to pick which properties or strategies to include in comparison.
 */
@Composable
private fun ComparisonAssetSelectorCard(
    allProperties: List<Property>,
    selectedIds: Set<Long>,
    selectedStrategy: String,
    onSelectStrategy: (String) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(BentoPurpleHeader, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Confronto Immobili",
                            tint = BentoPurpleOnContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Selettore Immobili a Confronto",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (selectedIds.isNotEmpty()) "${selectedIds.size} selezionati manualmente" else "Tutti gli immobili visualizzati (${allProperties.size})",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (selectedIds.isNotEmpty()) {
                        TextButton(
                            onClick = onClearSelection,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Deseleziona", fontSize = 11.sp, color = RoseRed)
                        }
                    } else {
                        TextButton(
                            onClick = onSelectAll,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Seleziona Tutti", fontSize = 11.sp, color = BentoPurpleOnContainer)
                        }
                    }
                }
            }

            // Strategy Filter Chips
            val strategies = listOf("TUTTE", "Fix & Flip", "Buy & Hold", "Frazionamento", "Affitto Breve")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                strategies.forEach { strat ->
                    val isSelected = selectedStrategy.equals(strat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectStrategy(strat) },
                        label = { Text(strat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BentoPurpleHeader,
                            selectedLabelColor = BentoPurpleOnContainer,
                            containerColor = Color(0xFF1E2230),
                            labelColor = TextSecondaryDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder
                        )
                    )
                }
            }

            // Asset Quick Toggle Chips Row
            Text(
                text = "Tocca per includere/escludere dal confronto:",
                fontSize = 11.sp,
                color = TextSecondaryDark
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allProperties.forEach { prop ->
                    val isIncluded = selectedIds.isEmpty() || selectedIds.contains(prop.id)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isIncluded) BentoPurpleContainer.copy(alpha = 0.25f) else Color(0xFF1B1F2A),
                        border = BorderStroke(
                            width = if (isIncluded) 1.2.dp else 0.8.dp,
                            color = if (isIncluded) BentoPurpleOnContainer else SurfaceCardBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onToggleSelection(prop.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isIncluded) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (isIncluded) "Incluso" else "Escluso",
                                tint = if (isIncluded) BentoPurpleOnContainer else TextMutedDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = prop.title.ifBlank { prop.address },
                                fontSize = 11.sp,
                                fontWeight = if (isIncluded) FontWeight.Bold else FontWeight.Medium,
                                color = if (isIncluded) TextPrimaryDark else TextSecondaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Scenario Sensitivity Selector Bar
 */
@Composable
private fun ScenarioSensitivitySelectorBar(
    currentScenario: ComparisonScenario,
    onSelectScenario: (ComparisonScenario) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2030)),
        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("comparison_scenario_bar")
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
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Simulatore di Sensibilità ROI Proiettato",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyanAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = currentScenario.title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Scenario Segmented Selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                ComparisonScenario.values().forEachIndexed { index, scenario ->
                    val isSelected = currentScenario == scenario
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onSelectScenario(scenario) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ComparisonScenario.values().size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = CyanAccent.copy(alpha = 0.2f),
                            activeContentColor = CyanAccent,
                            inactiveContainerColor = Color(0xFF141824),
                            inactiveContentColor = TextSecondaryDark
                        )
                    ) {
                        Text(
                            text = scenario.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Text(
                text = currentScenario.description,
                fontSize = 10.sp,
                color = TextSecondaryDark
            )
        }
    }
}

/**
 * Top Executive Highlights Banner identifying standout assets.
 */
@Composable
private fun ComparisonHighlightsBanner(
    maxRoiProp: PropertyProjectedKpi?,
    maxProfitProp: PropertyProjectedKpi?,
    minCostProp: PropertyProjectedKpi?,
    maxYieldProp: PropertyProjectedKpi?,
    euroFormat: NumberFormat
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        border = BorderStroke(1.dp, BentoPurpleHeader),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AmberGold,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Sintesi Comparativa & Leader di Portafoglio",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                maxRoiProp?.let { leader ->
                    ComparisonLeaderCard(
                        title = "Miglior ROI",
                        badge = String.format(Locale.getDefault(), "%.1f%%", leader.roiPercent),
                        propertyName = leader.property.title.ifBlank { leader.property.address },
                        accentColor = EmeraldGreen,
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                }

                maxProfitProp?.let { leader ->
                    ComparisonLeaderCard(
                        title = "Max Plusvalenza",
                        badge = euroFormat.format(leader.grossProfit),
                        propertyName = leader.property.title.ifBlank { leader.property.address },
                        accentColor = AmberGold,
                        icon = Icons.Default.MonetizationOn,
                        modifier = Modifier.weight(1f)
                    )
                }

                minCostProp?.let { leader ->
                    ComparisonLeaderCard(
                        title = "Minor Capitale",
                        badge = euroFormat.format(leader.totalCostBasis),
                        propertyName = leader.property.title.ifBlank { leader.property.address },
                        accentColor = CyanAccent,
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonLeaderCard(
    title: String,
    badge: String,
    propertyName: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accentColor.copy(alpha = 0.12f),
        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Text(
                text = badge,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimaryDark
            )
            Text(
                text = propertyName,
                fontSize = 9.sp,
                color = TextSecondaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Side-by-Side Comparison Matrix with sticky left column labels and scrollable property columns.
 */
@Composable
private fun ComparisonMatrixTable(
    kpis: List<PropertyProjectedKpi>,
    maxRoiPropId: Long?,
    maxProfitPropId: Long?,
    minCostPropId: Long?,
    minPriceSqmPropId: Long?,
    maxYieldPropId: Long?,
    euroFormat: NumberFormat,
    onCalculateRoiClick: (Property) -> Unit,
    onEditFinancialsClick: (Property) -> Unit,
    onUpdateStatusClick: (Property) -> Unit,
    onUpdateProgressClick: (Property) -> Unit,
    onSimulatePriceDropClick: (Property) -> Unit,
    onExportPdfClick: (Property) -> Unit,
    onDeleteClick: (Property) -> Unit,
    onRemoveFromComparison: (Long) -> Unit
) {
    val horizontalScrollState = rememberScrollState()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                // Fixed/Pinned Left Metric Labels Column
                Column(
                    modifier = Modifier
                        .width(135.dp)
                        .padding(end = 8.dp)
                ) {
                    // Header Placeholder matching property cards
                    Spacer(modifier = Modifier.height(210.dp))

                    // SECTION 1: ACQUISTO & CAPITALE
                    ComparisonSectionHeaderRow("CAPITALE & LAVORI")
                    ComparisonLabelRow("Prezzo Acquisto")
                    ComparisonLabelRow("Superficie (m²)")
                    ComparisonLabelRow("Prezzo al m²")
                    ComparisonLabelRow("Budget Lavori")
                    ComparisonLabelRow("Incidenza Lavori")
                    ComparisonLabelRow("Costo Totale (Basis)")

                    // SECTION 2: RIVENDITA & MARGINE
                    ComparisonSectionHeaderRow("EXIT & PLUSVALENZA")
                    ComparisonLabelRow("Target Rivendita")
                    ComparisonLabelRow("Target €/m²")
                    ComparisonLabelRow("Plusvalenza Lorda")
                    ComparisonLabelRow("Sconto su Mercato")

                    // SECTION 3: ROI & PROIEZIONI
                    ComparisonSectionHeaderRow("RENDIMENTO & ROI")
                    ComparisonLabelRow("ROI Lordo %")
                    ComparisonLabelRow("Affitto Mensile")
                    ComparisonLabelRow("Rendimento Affitto")
                    ComparisonLabelRow("Rientro Capitale")

                    // SECTION 4: OPERATIVITÀ
                    ComparisonSectionHeaderRow("STATO & STRATEGIA")
                    ComparisonLabelRow("Stato Pipeline")
                    ComparisonLabelRow("Avanzamento SAL")
                    ComparisonLabelRow("Strategia")

                    // SECTION 5: AZIONI
                    ComparisonSectionHeaderRow("AZIONI RAPIDE")
                    Spacer(modifier = Modifier.height(220.dp))
                }

                // Property Item Columns
                kpis.forEach { item ->
                    val prop = item.property
                    val isMaxRoi = prop.id == maxRoiPropId
                    val isMaxProfit = prop.id == maxProfitPropId
                    val isMinCost = prop.id == minCostPropId
                    val isMinPriceSqm = prop.id == minPriceSqmPropId
                    val isMaxYield = prop.id == maxYieldPropId

                    Column(
                        modifier = Modifier
                            .width(230.dp)
                            .padding(horizontal = 4.dp)
                            .background(
                                color = if (isMaxRoi) BentoPurpleContainer.copy(alpha = 0.12f) else Color(0xFF171A24),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = if (isMaxRoi) 1.5.dp else 1.dp,
                                color = if (isMaxRoi) BentoPurpleOnContainer else SurfaceCardBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(10.dp)
                    ) {
                        // 1. Property Header Card (Thumbnail, Title, Address, Strategy Badge)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            val context = LocalContext.current
                            AsyncImage(
                                model = ImageUtils.buildOptimizedImageRequest(
                                    context = context,
                                    data = prop.photoUri?.ifBlank { "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=800" }
                                        ?: "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=800",
                                    targetWidthPx = 400,
                                    targetHeightPx = 250
                                ),
                                contentDescription = prop.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                            )

                            // Close / Remove from comparison button
                            IconButton(
                                onClick = { onRemoveFromComparison(prop.id) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Rimuovi dal confronto",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            // Strategy Badge Top Left
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BentoPurpleHeader,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = prop.strategyTags,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPurpleOnContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Title & Address Bottom
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = prop.title.ifBlank { prop.address },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = prop.address,
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick ROI Summary Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                item.roiPercent >= 20.0 -> EmeraldGreen.copy(alpha = 0.18f)
                                item.roiPercent >= 10.0 -> AmberGold.copy(alpha = 0.18f)
                                else -> RoseRed.copy(alpha = 0.18f)
                            },
                            border = BorderStroke(
                                0.8.dp,
                                when {
                                    item.roiPercent >= 20.0 -> EmeraldGreen
                                    item.roiPercent >= 10.0 -> AmberGold
                                    else -> RoseRed
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ROI Proiettato:",
                                    fontSize = 10.sp,
                                    color = TextPrimaryDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", item.roiPercent),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        item.roiPercent >= 20.0 -> EmeraldGreen
                                        item.roiPercent >= 10.0 -> AmberGold
                                        else -> RoseRed
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. VALUES GRID

                        // SECTION 1: CAPITALE & LAVORI
                        ComparisonSectionHeaderSpacer()
                        ComparisonValueCell(euroFormat.format(item.purchasePrice), isWinner = false)
                        ComparisonValueCell(
                            prop.surfaceSqm.takeIf { it > 0 }?.let { "$it m²" } ?: "N/D",
                            isWinner = false
                        )
                        ComparisonValueCell(
                            valueText = if (item.pricePerSqm > 0) "${euroFormat.format(item.pricePerSqm)}/m²" else "N/D",
                            isWinner = isMinPriceSqm,
                            winnerText = "Best €/m²"
                        )
                        ComparisonValueCell(euroFormat.format(item.renoCost), isWinner = false)
                        ComparisonValueCell(String.format(Locale.getDefault(), "%.1f%%", item.renoToPurchaseRatio), isWinner = false)
                        ComparisonValueCell(
                            valueText = euroFormat.format(item.totalCostBasis),
                            isWinner = isMinCost,
                            winnerText = "Min Capitale"
                        )

                        // SECTION 2: EXIT & PLUSVALENZA
                        ComparisonSectionHeaderSpacer()
                        ComparisonValueCell(euroFormat.format(item.exitValue), isWinner = false)
                        ComparisonValueCell(
                            if (item.exitPricePerSqm > 0) "${euroFormat.format(item.exitPricePerSqm)}/m²" else "N/D",
                            isWinner = false
                        )
                        ComparisonValueCell(
                            valueText = euroFormat.format(item.grossProfit),
                            isWinner = isMaxProfit,
                            winnerText = "Max Utile"
                        )
                        ComparisonValueCell(
                            valueText = if (item.exitValue > item.purchasePrice && item.exitValue > 0) {
                                String.format(Locale.getDefault(), "%.0f%%", ((item.exitValue - item.purchasePrice) / item.exitValue) * 100)
                            } else "0%",
                            isWinner = false
                        )

                        // SECTION 3: RENDIMENTO & ROI
                        ComparisonSectionHeaderSpacer()
                        ComparisonValueCell(
                            valueText = String.format(Locale.getDefault(), "%.1f%%", item.roiPercent),
                            isWinner = isMaxRoi,
                            winnerText = "Leader ROI"
                        )
                        ComparisonValueCell(
                            if (item.rentalIncomeMonthly > 0) "${euroFormat.format(item.rentalIncomeMonthly)}/m" else "N/D",
                            isWinner = false
                        )
                        ComparisonValueCell(
                            valueText = if (item.grossRentalYield > 0) String.format(Locale.getDefault(), "%.1f%%", item.grossRentalYield) else "N/D",
                            isWinner = isMaxYield,
                            winnerText = "Top Yield"
                        )
                        ComparisonValueCell(
                            valueText = if (item.paybackMonths > 0) "${(item.paybackMonths / 12.0).toInt()}a ${(item.paybackMonths % 12).toInt()}m" else "N/D",
                            isWinner = false
                        )

                        // SECTION 4: STATO & STRATEGIA
                        ComparisonSectionHeaderSpacer()
                        ComparisonValueCell(prop.currentStatus.labelIt, isWinner = false)
                        ComparisonValueCell("${prop.renovationProgressPercent}%", isWinner = false)
                        ComparisonValueCell(prop.strategyTags, isWinner = false)

                        // SECTION 5: AZIONI RAPIDE
                        ComparisonSectionHeaderSpacer()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onCalculateRoiClick(prop) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BentoPurpleHeader,
                                    contentColor = BentoPurpleOnContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = "Simula ROI", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Simula ROI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { onEditFinancialsClick(prop) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF262C3D),
                                    contentColor = CyanAccent
                                ),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifica Finanze", modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Modifica Finanze", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { onUpdateStatusClick(prop) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                                border = BorderStroke(0.8.dp, SurfaceCardBorder),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Cambia Stato", modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cambia Stato", fontSize = 10.sp)
                            }

                            OutlinedButton(
                                onClick = { onExportPdfClick(prop) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                                border = BorderStroke(0.8.dp, AmberGold.copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Dossier PDF", tint = AmberGold, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dossier PDF", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonSectionHeaderRow(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(top = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BentoPurpleOnContainer,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ComparisonSectionHeaderSpacer() {
    Spacer(modifier = Modifier.height(28.dp))
}

@Composable
private fun ComparisonLabelRow(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondaryDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ComparisonValueCell(
    valueText: String,
    isWinner: Boolean,
    winnerText: String = "Migliore"
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(vertical = 2.dp)
            .background(
                color = if (isWinner) EmeraldGreen.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = valueText,
                fontSize = 11.sp,
                fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isWinner) EmeraldGreen else TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isWinner) {
                Surface(
                    color = EmeraldGreen,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Text(
                        text = winnerText,
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
