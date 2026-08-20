package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.ImmobiliareObservatoryService
import com.example.util.MunicipalityMarketData
import com.example.util.SubZoneBenchmark
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmobiliareObservatoryDialog(
    initialMunicipality: String = "Paderno Dugnano",
    onDismissRequest: () -> Unit,
    onApplyPricePerSqM: ((Double, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedData by remember {
        mutableStateOf(ImmobiliareObservatoryService.findMarketData(initialMunicipality))
    }
    var customSearchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(26.dp))
                .border(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(CyanAccent.copy(alpha = 0.6f), SurfaceCardBorder, BentoBlueOnContainer.copy(alpha = 0.4f))
                    ),
                    RoundedCornerShape(26.dp)
                ),
            color = SurfaceCardDark,
            tonalElevation = 8.dp
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Osservatorio Mercato Immobiliare",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Benchmark & Quotazioni di riferimento al m²",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("btn_close_observatory")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Chiudi", tint = TextSecondaryDark)
                    }
                }

                HorizontalDivider(
                    color = SurfaceCardBorder,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Body content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Presets Row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Seleziona Comune o Mercato di Riferimento:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondaryDark
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(ImmobiliareObservatoryService.ALL_PRESETS) { preset ->
                                val isSelected = selectedData.municipalityName.equals(preset.municipalityName, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedData = preset
                                    },
                                    label = {
                                        Text(
                                            text = if (preset.municipalityName == "Paderno Dugnano") "📍 Paderno Dugnano" else preset.municipalityName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanAccent,
                                        selectedLabelColor = Color.Black,
                                        containerColor = BentoCardBgLight,
                                        labelColor = TextPrimaryDark
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) CyanAccent else SurfaceCardBorder
                                    )
                                )
                            }
                        }
                    }

                    // Search by URL or Custom City
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customSearchText,
                            onValueChange = { customSearchText = it },
                            placeholder = { Text("Incolla link Immobiliare.it o cerca comune...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = SurfaceCardBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(18.dp))
                            }
                        )

                        Button(
                            onClick = {
                                if (customSearchText.isNotBlank()) {
                                    selectedData = ImmobiliareObservatoryService.findMarketData(customSearchText)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                        ) {
                            Text("Cerca", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Main KPI Hero Card for the Selected Municipality
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoCardBgLight),
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = "${selectedData.municipalityName} (${selectedData.province})",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = CyanAccent.copy(alpha = 0.15f),
                                            border = BorderStroke(0.5.dp, CyanAccent.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = selectedData.provenance,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyanAccent,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${selectedData.region} • Aggiornato all'Osservatorio Mensile",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldGreen.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "+${selectedData.trendSaleYoY}% YoY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (selectedData.isGenericFallback) {
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
                                            fontSize = 11.sp,
                                            color = AmberGold,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.5f))

                            // 4 Key KPI Tiles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MarketKpiTile(
                                    title = "Prezzo Medio Vendita",
                                    value = "€${selectedData.avgSalePricePerSqM.toInt()}/m²",
                                    subtitle = "Forchetta: €${selectedData.minSalePricePerSqM.toInt()} - €${selectedData.maxSalePricePerSqM.toInt()}",
                                    accentColor = CyanAccent,
                                    modifier = Modifier.weight(1f)
                                )

                                MarketKpiTile(
                                    title = "Canone Medio Affitto",
                                    value = "€${String.format(Locale.ITALY, "%.2f", selectedData.avgRentPricePerSqMMonth)}/m²",
                                    subtitle = "Trend Affitti: +${selectedData.trendRentYoY}%",
                                    accentColor = EmeraldGreen,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MarketKpiTile(
                                    title = "Gross Rental Yield",
                                    value = "${String.format(Locale.ITALY, "%.2f", selectedData.grossRentalYield)}%",
                                    subtitle = "Rendimento Lordo Annuo",
                                    accentColor = AmberGold,
                                    modifier = Modifier.weight(1f)
                                )

                                MarketKpiTile(
                                    title = "Giorni Medi a Mercato",
                                    value = "${selectedData.averageDaysOnMarket} gg",
                                    subtitle = "Liquidità & Velocità Assorbimento",
                                    accentColor = BentoPurpleOnContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (selectedData.notes.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = BentoBlueContainer.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, BentoBlueOnContainer.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = CyanAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = selectedData.notes,
                                            fontSize = 12.sp,
                                            color = TextPrimaryDark,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sub-zones / Frazioni Breakdown (e.g. Centro, Calderara, Palazzolo Milanese, etc.)
                    if (selectedData.subZones.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Quotazioni Dettagliate per Frazione / Zona:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )

                            selectedData.subZones.forEach { subZone ->
                                SubZoneRowItem(
                                    subZone = subZone,
                                    onSelect = {
                                        onApplyPricePerSqM?.invoke(subZone.avgSalePricePerSqM, "${selectedData.municipalityName} (${subZone.name})")
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = SurfaceCardBorder,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedData.officialUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                ImmobiliareObservatoryService.openOfficialObservatory(context, selectedData.officialUrl)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_open_immobiliare_url")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apri Fonte", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (onApplyPricePerSqM != null) {
                        Button(
                            onClick = {
                                onApplyPricePerSqM(selectedData.avgSalePricePerSqM, selectedData.municipalityName)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                            modifier = Modifier
                                .weight(if (selectedData.officialUrl.isNotBlank()) 1.2f else 1f)
                                .testTag("btn_apply_zone_price")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Applica a Perizia (€${selectedData.avgSalePricePerSqM.toInt()}/m²)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketKpiTile(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCardDark,
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondaryDark,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextMutedDark,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun SubZoneRowItem(
    subZone: SubZoneBenchmark,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BentoCardBgLight,
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subZone.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Affitto: €${String.format(Locale.ITALY, "%.2f", subZone.avgRentPricePerSqMMonth)}/m²/mese • Rendita: ${String.format(Locale.ITALY, "%.1f", subZone.grossYield)}%",
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyanAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "€${subZone.avgSalePricePerSqM.toInt()}/m²",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMutedDark,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
