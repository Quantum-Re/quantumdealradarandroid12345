package com.example.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenovationSimulatorScreen(
    initialPrice: Double = 180000.0,
    initialSqm: Double = 75.0,
    initialZonePricePerSqm: Double = 2800.0,
    propertyTitle: String = "Immobile Target",
    propertyAddress: String = "Milano (MI)",
    onNavigateBack: () -> Unit,
    onApplyToProperty: ((Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val euroFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 } }

    var surfaceSqmInput by remember { mutableStateOf(initialSqm.toInt().toString()) }
    var acquisitionPriceInput by remember { mutableStateOf(initialPrice.toInt().toString()) }
    var zonePriceInput by remember { mutableStateOf(initialZonePricePerSqm.toInt().toString()) }
    var selectedQuality by remember { mutableStateOf(RenovationQualityLevel.MEDIUM_PREMIUM) }
    var bonusCasaEnabled by remember { mutableStateOf(true) }
    var ecobonusEnabled by remember { mutableStateOf(true) }
    var contingencyPercent by remember { mutableStateOf(10f) }

    // Computo metrico voci modificabili
    var customItems by remember(surfaceSqmInput, selectedQuality) {
        val sqm = surfaceSqmInput.toDoubleOrNull() ?: initialSqm
        mutableStateOf(RenovationEstimatorEngine.generateDefaultMetricComputation(sqm, selectedQuality))
    }

    var activeTab by remember { mutableStateOf(0) } // 0: Computo Metrico, 1: Strategia Flip & ARV, 2: Buy & Hold

    val calculationResult = remember(
        surfaceSqmInput,
        acquisitionPriceInput,
        zonePriceInput,
        selectedQuality,
        bonusCasaEnabled,
        ecobonusEnabled,
        contingencyPercent,
        customItems
    ) {
        val sqm = surfaceSqmInput.toDoubleOrNull() ?: initialSqm
        val acqPrice = acquisitionPriceInput.toDoubleOrNull() ?: initialPrice
        val zonePrice = zonePriceInput.toDoubleOrNull() ?: initialZonePricePerSqm

        val input = RenovationSimulatorInput(
            propertyTitle = propertyTitle,
            propertyAddress = propertyAddress,
            surfaceSqm = sqm,
            acquisitionPrice = acqPrice,
            currentMarketValuePerSqm = zonePrice,
            qualityLevel = selectedQuality,
            hasBonusRistrutturazioni50 = bonusCasaEnabled,
            hasEcobonus65 = ecobonusEnabled,
            contingencyBufferPercent = contingencyPercent.toDouble(),
            items = customItems
        )
        RenovationEstimatorEngine.calculate(input)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Simulatore Computo & CapEx",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "$propertyTitle • ${calculationResult.surfaceSqm.toInt()} m²",
                            fontSize = 12.sp,
                            color = TextSecondaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("renovation_simulator_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    if (onApplyToProperty != null) {
                        FilledTonalButton(
                            onClick = {
                                onApplyToProperty(calculationResult.totalGrossRenovationCost)
                                Toast.makeText(context, "Budget aggiornato: ${euroFormat.format(calculationResult.totalGrossRenovationCost)}", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BentoPurpleOnContainer,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("renovation_simulator_apply_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salva Budget", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceCardDark.copy(alpha = 0.95f),
                    titleContentColor = TextPrimaryDark
                )
            )
        },
        containerColor = DarkSlateBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero KPI Banner con Gradiente
            item {
                RenovationHeroKpiCard(
                    result = calculationResult,
                    euroFormat = euroFormat
                )
            }

            // 2. Parametri Generali & Finitura
            item {
                RenovationGlobalParametersCard(
                    surfaceSqm = surfaceSqmInput,
                    onSurfaceChange = { surfaceSqmInput = it },
                    acquisitionPrice = acquisitionPriceInput,
                    onAcquisitionPriceChange = { acquisitionPriceInput = it },
                    zonePrice = zonePriceInput,
                    onZonePriceChange = { zonePriceInput = it },
                    selectedQuality = selectedQuality,
                    onQualitySelect = { selectedQuality = it },
                    bonusCasaEnabled = bonusCasaEnabled,
                    onBonusCasaToggle = { bonusCasaEnabled = it },
                    ecobonusEnabled = ecobonusEnabled,
                    onEcobonusToggle = { ecobonusEnabled = it },
                    contingencyPercent = contingencyPercent,
                    onContingencyChange = { contingencyPercent = it },
                    onResetDefaults = {
                        val sqm = surfaceSqmInput.toDoubleOrNull() ?: initialSqm
                        customItems = RenovationEstimatorEngine.generateDefaultMetricComputation(sqm, selectedQuality)
                    }
                )
            }

            // 3. Tab Switcher
            item {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = SurfaceCardDark,
                    contentColor = CyanAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = CyanAccent
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("renovation_tab_row")
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("🛠️ Computo Metrico", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("🚀 Flip & Rivendita", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("🏢 Buy & Hold", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // 4. Contenuto del Tab Selezionato
            when (activeTab) {
                0 -> {
                    // Lista Dettaglio Computo Metrico
                    item {
                        Text(
                            text = "VOCI DI CAPITOLATO & PREZZIARI REGIONALI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }

                    items(customItems) { item ->
                        RenovationItemRow(
                            item = item,
                            euroFormat = euroFormat,
                            onToggle = { isEnabled ->
                                customItems = customItems.map { if (it.id == item.id) it.copy(isEnabled = isEnabled) else it }
                            },
                            onQuantityChange = { newQty ->
                                customItems = customItems.map { if (it.id == item.id) it.copy(baseQuantity = newQty) else it }
                            },
                            onUnitPriceChange = { newPrice ->
                                customItems = customItems.map { if (it.id == item.id) it.copy(baseUnitPrice = newPrice) else it }
                            }
                        )
                    }
                }
                1 -> {
                    // Analisi Strategica Flip (Ristruttura e Rivendi)
                    item {
                        RenovationFlipAnalysisCard(
                            result = calculationResult,
                            euroFormat = euroFormat
                        )
                    }
                }
                2 -> {
                    // Analisi Messa a Reddito (Buy & Hold)
                    item {
                        RenovationRentalAnalysisCard(
                            result = calculationResult,
                            euroFormat = euroFormat
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenovationHeroKpiCard(
    result: RenovationSimulatorResult,
    euroFormat: NumberFormat
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("renovation_hero_kpi_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BentoPurpleHeader.copy(alpha = 0.4f),
                            SurfaceCardDark
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                            imageVector = Icons.Default.Handyman,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "STIMA CAPEX RISTRUTTURAZIONE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BentoPurpleOnContainer.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = result.qualityLevel.label,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = euroFormat.format(result.totalGrossRenovationCost),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Costo medio: ${euroFormat.format(result.grossCostPerSqm)} / m²",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Detrazioni 10 Anni",
                            fontSize = 11.sp,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "-${euroFormat.format(result.totalTaxDeduction10Years)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "Netto: ${euroFormat.format(result.netEffectiveRenovationCost)}",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                HorizontalDivider(color = SurfaceCardBorder, thickness = 1.dp)

                // Barra riepilogativa costi diretti + imprevisti + oneri
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RenovationMiniStat(
                        label = "Opere Dirette",
                        value = euroFormat.format(result.subtotalDirectWorks)
                    )
                    RenovationMiniStat(
                        label = "Imprevisti Cantiere",
                        value = euroFormat.format(result.contingencyBufferCost)
                    )
                    RenovationMiniStat(
                        label = "Spese Tecniche/DL",
                        value = euroFormat.format(result.contractorAndTechFeesCost)
                    )
                }
            }
        }
    }
}

@Composable
private fun RenovationMiniStat(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = TextMutedDark)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
    }
}

@Composable
private fun RenovationGlobalParametersCard(
    surfaceSqm: String,
    onSurfaceChange: (String) -> Unit,
    acquisitionPrice: String,
    onAcquisitionPriceChange: (String) -> Unit,
    zonePrice: String,
    onZonePriceChange: (String) -> Unit,
    selectedQuality: RenovationQualityLevel,
    onQualitySelect: (RenovationQualityLevel) -> Unit,
    bonusCasaEnabled: Boolean,
    onBonusCasaToggle: (Boolean) -> Unit,
    ecobonusEnabled: Boolean,
    onEcobonusToggle: (Boolean) -> Unit,
    contingencyPercent: Float,
    onContingencyChange: (Float) -> Unit,
    onResetDefaults: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("renovation_global_params_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Parametri Immobile & Finiture",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                TextButton(
                    onClick = onResetDefaults,
                    modifier = Modifier.testTag("renovation_reset_defaults_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyanAccent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ripristina Prezziari", fontSize = 11.sp, color = CyanAccent)
                }
            }

            // Input Row: Mq, Prezzo Acquisto, Valore Zona
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = surfaceSqm,
                    onValueChange = onSurfaceChange,
                    label = { Text("Superficie (m²)", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.weight(1f).testTag("renovation_sqm_input")
                )

                OutlinedTextField(
                    value = acquisitionPrice,
                    onValueChange = onAcquisitionPriceChange,
                    label = { Text("Prezzo Acquisto (€)", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.weight(1.3f).testTag("renovation_acq_price_input")
                )

                OutlinedTextField(
                    value = zonePrice,
                    onValueChange = onZonePriceChange,
                    label = { Text("Quota Zona (€/m²)", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.weight(1.2f).testTag("renovation_zone_price_input")
                )
            }

            // Scelta Capitolato / Finitura
            Text(text = "Livello Capitolato & Finiture:", fontSize = 12.sp, color = TextSecondaryDark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RenovationQualityLevel.values().forEach { q ->
                    val isSelected = (selectedQuality == q)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) BentoPurpleOnContainer else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onQualitySelect(q) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = q.label.split(" / ")[0],
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextPrimaryDark
                            )
                            Text(
                                text = "+${q.estimatedAppreciationBoostPercent.toInt()}% Valore",
                                fontSize = 10.sp,
                                color = if (isSelected) AmberGold else TextMutedDark
                            )
                        }
                    }
                }
            }

            // Fiscal Incentives Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = bonusCasaEnabled,
                        onCheckedChange = onBonusCasaToggle,
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen)
                    )
                    Text("Bonus Casa 50% IRPEF", fontSize = 11.sp, color = TextPrimaryDark)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = ecobonusEnabled,
                        onCheckedChange = onEcobonusToggle,
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen)
                    )
                    Text("Ecobonus 65% Clima", fontSize = 11.sp, color = TextPrimaryDark)
                }
            }

            // Slider Imprevisti Cantiere
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Margine Imprevisti di Cantiere:", fontSize = 11.sp, color = TextSecondaryDark)
                    Text("${contingencyPercent.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                }
                Slider(
                    value = contingencyPercent,
                    onValueChange = onContingencyChange,
                    valueRange = 0f..25f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanAccent,
                        activeTrackColor = CyanAccent,
                        inactiveTrackColor = Color(0xFF1E293B)
                    )
                )
            }
        }
    }
}

@Composable
private fun RenovationItemRow(
    item: RenovationBudgetItem,
    euroFormat: NumberFormat,
    onToggle: (Boolean) -> Unit,
    onQuantityChange: (Double) -> Unit,
    onUnitPriceChange: (Double) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (item.isEnabled) SurfaceCardDark else SurfaceCardDark.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, if (item.isEnabled) SurfaceCardBorder else SurfaceCardBorder.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Switch(
                        checked = item.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanAccent,
                            checkedTrackColor = Color(0xFF0C4A6E)
                        ),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.categoryName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isEnabled) TextPrimaryDark else TextMutedDark
                        )
                        Text(
                            text = item.description,
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = euroFormat.format(item.totalGrossCost),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (item.isEnabled) Color.White else TextMutedDark
                    )
                    if (item.isEnabled && item.isTaxDeductible) {
                        Text(
                            text = "Detr. ${(item.taxDeductionRate * 100).toInt()}%",
                            fontSize = 10.sp,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (item.isEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quantità: ${String.format(Locale.US, "%.1f", item.baseQuantity)} ${item.unit}",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                    Text(
                        text = "PU: ${euroFormat.format(item.baseUnitPrice)} / ${item.unit}",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                }
            }
        }
    }
}

@Composable
private fun RenovationFlipAnalysisCard(
    result: RenovationSimulatorResult,
    euroFormat: NumberFormat
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                Text("STRATEGIA FLIP & RIVENDITA POST-OPERE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmberGold)
            }

            Text(
                text = "Simulazione di compravendita a breve termine. L'immobile viene acquistato, ristrutturato con finiture di livello '${result.qualityLevel.label}' e rivenduto al valore ARV (After Repair Value).",
                fontSize = 12.sp,
                color = TextSecondaryDark,
                lineHeight = 16.sp
            )

            HorizontalDivider(color = SurfaceCardBorder, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Valore di Rivendita Stimato (ARV):", fontSize = 11.sp, color = TextSecondaryDark)
                    Text(euroFormat.format(result.estimatedArvMarketValue), fontSize = 22.sp, fontWeight = FontWeight.Black, color = CyanAccent)
                    Text("Quotazione: ${euroFormat.format(result.estimatedPostRenoValuePerSqm)} / m²", fontSize = 11.sp, color = TextMutedDark)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Capitale Totale Lordo:", fontSize = 11.sp, color = TextSecondaryDark)
                    Text(euroFormat.format(result.totalCapitalInvestedGross), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Text("(Acquisto + CapEx)", fontSize = 10.sp, color = TextMutedDark)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (result.roiFlipGrossPercent >= 15.0) EmeraldGreen.copy(alpha = 0.15f) else Color(0xFF1E293B),
                border = BorderStroke(1.dp, if (result.roiFlipGrossPercent >= 15.0) EmeraldGreen.copy(alpha = 0.4f) else SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Plusvalenza Lorda Operazione:", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(
                            text = "+${euroFormat.format(result.capitalGainFlipGross)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (result.capitalGainFlipGross > 0) EmeraldGreen else RoseRed
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("ROI Lordo su Capitale:", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", result.roiFlipGrossPercent)}%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (result.roiFlipGrossPercent >= 15.0) EmeraldGreen else AmberGold
                        )
                    }
                }
            }

            if (result.totalTaxDeduction10Years > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Includendo le detrazioni fiscali portate a compensazione (-${euroFormat.format(result.totalTaxDeduction10Years)}), il ROI Netto sale al ${String.format(Locale.US, "%.1f", result.roiFlipNetPercent)}%.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenovationRentalAnalysisCard(
    result: RenovationSimulatorResult,
    euroFormat: NumberFormat
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Apartment, contentDescription = null, tint = BentoPurpleOnContainer, modifier = Modifier.size(20.dp))
                Text("STRATEGIA BUY & HOLD (MESSA A REDDITO)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoPurpleOnContainer)
            }

            Text(
                text = "Simulazione di locazione a lungo termine o a canone concordato/studenti post ristrutturazione completa.",
                fontSize = 12.sp,
                color = TextSecondaryDark
            )

            HorizontalDivider(color = SurfaceCardBorder, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Canone Mensile Stimato:", fontSize = 11.sp, color = TextSecondaryDark)
                    Text(
                        text = "${euroFormat.format(result.estimatedMonthlyRentPostReno)} / mese",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Canone Annuo Totale:", fontSize = 11.sp, color = TextSecondaryDark)
                    Text(
                        text = euroFormat.format(result.annualGrossRentPostReno),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Gross Yield Lordo", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(
                            text = "${String.format(Locale.US, "%.2f", result.grossRentalYieldPostRenoPercent)}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldGreen
                        )
                        Text("su totale investito", fontSize = 10.sp, color = TextMutedDark)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Net Yield (Cedolare)", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(
                            text = "${String.format(Locale.US, "%.2f", result.netRentalYieldPostRenoPercent)}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent
                        )
                        Text("netto imposte 21%", fontSize = 10.sp, color = TextMutedDark)
                    }
                }
            }
        }
    }
}
