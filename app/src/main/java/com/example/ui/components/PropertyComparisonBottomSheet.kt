package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.DistressedProperty
import com.example.data.Property
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import com.example.util.ImageUtils
import java.text.NumberFormat
import java.util.Locale

/**
 * Unified model for side-by-side property comparisons.
 */
data class ComparableProperty(
    val id: String,
    val title: String,
    val location: String,
    val price: Double,
    val estimatedMarketValue: Double,
    val surfaceSqm: Int?,
    val discountPercent: Int = if (estimatedMarketValue > 0 && price > 0) (((estimatedMarketValue - price) / estimatedMarketValue) * 100).toInt().coerceAtLeast(0) else 0,
    val estimatedRoiCapRate: Double = 0.0,
    val propertyType: String = "Residenziale",
    val statusOrDistress: String = "Active",
    val imageUrl: String = "",
    val sourceName: String = "",
    val notes: String = "",
    val rawDeal: PropertyDeal? = null,
    val rawDistressed: DistressedProperty? = null,
    val rawProperty: Property? = null
) {
    // Nessuna superficie di ripiego: senza mq verificati il prezzo/m² resta non disponibile.
    val pricePerSqm: Double? get() = surfaceSqm?.takeIf { it > 0 }?.let { price / it }
    val totalProfitPotential: Double get() = (estimatedMarketValue - price).coerceAtLeast(0.0)
}

fun Property.toComparableProperty(): ComparableProperty {
    val totalCost = totalCostBasis
    val exitVal = effectiveExitValue
    val profit = projectedProfit
    val roi = projectedRoiPercent
    val discount = if (exitVal > price && exitVal > 0) (((exitVal - price) / exitVal) * 100).toInt().coerceAtLeast(0) else 0
    return ComparableProperty(
        id = "portfolio_$id",
        title = title.ifBlank { address },
        location = address,
        price = price,
        estimatedMarketValue = exitVal,
        surfaceSqm = surfaceSqm.takeIf { it > 0 },
        discountPercent = discount,
        estimatedRoiCapRate = roi,
        propertyType = propertyType,
        statusOrDistress = currentStatus.labelIt,
        imageUrl = photoUri ?: "",
        sourceName = "Mio Portafoglio ($strategyTags)",
        notes = "Costo Lavori: €${(actualRenovationCost.takeIf { it > 0 } ?: estimatedRenovationCost).toInt()} | Rent: €${projectedRentalIncome.toInt()}/m | $notes",
        rawDeal = null,
        rawDistressed = null,
        rawProperty = this
    )
}

fun PropertyDeal.toComparableProperty(): ComparableProperty {
    return ComparableProperty(
        id = "deal_$id",
        title = title,
        location = location,
        price = askingPrice,
        estimatedMarketValue = estimatedMarketValue,
        surfaceSqm = surfaceSqm,
        discountPercent = discountPercent,
        estimatedRoiCapRate = estimatedCapRate,
        propertyType = propertyType,
        statusOrDistress = status,
        imageUrl = imageUrl,
        sourceName = sourceName,
        notes = notes,
        rawDeal = this
    )
}

fun DistressedProperty.toComparableProperty(): ComparableProperty {
    val arvVal = if (estimatedArv != null && estimatedArv > 0) estimatedArv else estimatedValue
    val profit = if (arvVal > price && price > 0) arvVal - price else 0.0
    val roi = if (price > 0) (profit / price) * 100 else 0.0
    return ComparableProperty(
        id = "distressed_$id",
        title = address,
        location = address,
        price = price,
        estimatedMarketValue = arvVal,
        surfaceSqm = 110, // Default estimated surface sqm for distressed entity if unlisted
        discountPercent = if (arvVal > price && price > 0) (((arvVal - price) / arvVal) * 100).toInt().coerceAtLeast(0) else 0,
        estimatedRoiCapRate = roi,
        propertyType = category,
        statusOrDistress = distressLevel,
        imageUrl = imageUrl ?: "",
        sourceName = "Aste / Distressed DB",
        notes = notes,
        rawDistressed = this
    )
}

fun ComparableProperty.toProperty(): Property {
    return rawProperty ?: Property(
        id = (id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL),
        title = title,
        address = location,
        price = price,
        // Property.surfaceSqm non è nullable: 0 è la convenzione già usata in tutto il
        // progetto per "superficie non disponibile" (vedi i takeIf { it > 0 } altrove).
        surfaceSqm = surfaceSqm ?: 0,
        propertyType = propertyType,
        distressStatus = statusOrDistress,
        estimatedRenovationCost = 0.0,
        targetResalePrice = estimatedMarketValue,
        estimatedMarketValue = estimatedMarketValue,
        projectedRentalIncome = if (estimatedRoiCapRate > 0) (price * (estimatedRoiCapRate / 100.0) / 12.0) else 0.0,
        notes = notes
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyComparisonBottomSheet(
    properties: List<ComparableProperty>,
    onDismissRequest: () -> Unit,
    onRemoveProperty: (String) -> Unit,
    onSimulateInRoiCalculator: (ComparableProperty) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.GERMANY).apply { maximumFractionDigits = 0 } }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Metric winner calculation helpers
    val minPriceId = remember(properties) { properties.minByOrNull { if (it.price > 0) it.price else Double.MAX_VALUE }?.id }
    val maxRoiId = remember(properties) { properties.maxByOrNull { it.estimatedRoiCapRate }?.id }
    val minPricePerSqmId = remember(properties) {
        properties.mapNotNull { p -> p.pricePerSqm?.let { p.id to it } }
            .minByOrNull { it.second }
            ?.first
    }
    val maxDiscountId = remember(properties) { properties.maxByOrNull { it.discountPercent }?.id }
    val maxProfitId = remember(properties) { properties.maxByOrNull { it.totalProfitPotential }?.id }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = DarkSlateBg,
        contentColor = TextPrimaryDark,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = TextMutedDark)
        },
        modifier = modifier.testTag("property_comparison_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
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
                            .background(BentoPurpleHeader, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Compare",
                            tint = BentoPurpleOnContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Confronto Immobili Side-by-Side",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "${properties.size} immobili selezionati per l'analisi comparativa",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Export PDF button (if at least 2 properties)
                    if (properties.size >= 2) {
                        IconButton(
                            onClick = {
                                val propA = properties[0].toProperty()
                                val propB = properties[1].toProperty()
                                com.example.util.PropertyPdfGenerator.generateAndShareComparisonPdf(
                                    context = context,
                                    propertyA = propA,
                                    propertyB = propB,
                                    emailOnly = false
                                )
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(BentoPurpleHeader, CircleShape)
                                .testTag("export_comparison_pdf_sheet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Esporta Dossier PDF Side-by-Side",
                                tint = BentoPurpleOnContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Export CSV button
                    IconButton(
                        onClick = {
                            exportComparisonCsv(context, properties)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceCardDark, CircleShape)
                            .testTag("export_comparison_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Esporta Confronto CSV",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceCardDark, CircleShape)
                            .testTag("close_comparison_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = TextMutedDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Divider(color = SurfaceCardBorder, thickness = 1.dp)

            if (properties.size < 2) {
                // Empty or insufficient items state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Seleziona almeno 2 immobili per iniziare il confronto",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tocca l'icona 'Confronta' sulle schede immobile per aggiungerli a questa tabella side-by-side.",
                            fontSize = 13.sp,
                            color = TextSecondaryDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val horizontalScrollState = rememberScrollState()
                val verticalScrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(verticalScrollState)
                        .padding(vertical = 12.dp)
                ) {
                    // Quick Recommendation Highlight Cards Banner
                    ComparisonTopInsightsBanner(
                        properties = properties,
                        minPriceId = minPriceId,
                        maxRoiId = maxRoiId,
                        minPricePerSqmId = minPricePerSqmId
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Side-by-side Comparison Matrix Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        // Metric Labels Fixed Left Column
                        Column(
                            modifier = Modifier
                                .width(130.dp)
                                .padding(end = 8.dp)
                        ) {
                            // Empty space matching property card headers
                            Spacer(modifier = Modifier.height(180.dp))

                            ComparisonMetricLabelRow("Prezzo Chiesto")
                            ComparisonMetricLabelRow("Valore di Mercato")
                            ComparisonMetricLabelRow("Sconto Mercato")
                            ComparisonMetricLabelRow("Superficie (m²)")
                            ComparisonMetricLabelRow("Prezzo al m²")
                            ComparisonMetricLabelRow("Cap Rate / ROI %")
                            ComparisonMetricLabelRow("Margine Potenziale")
                            ComparisonMetricLabelRow("Tipologia")
                            ComparisonMetricLabelRow("Stato / Asta")
                            ComparisonMetricLabelRow("Fonte Data")
                        }

                        // Property Item Columns
                        properties.forEach { property ->
                            Column(
                                modifier = Modifier
                                    .width(220.dp)
                                    .padding(horizontal = 4.dp)
                                    .background(SurfaceCardDark, RoundedCornerShape(16.dp))
                                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                                    .padding(10.dp)
                            ) {
                                // Property Header Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    val context = LocalContext.current
                                    AsyncImage(
                                        model = ImageUtils.buildOptimizedImageRequest(
                                            context = context,
                                            data = property.imageUrl.ifBlank { "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=800" },
                                            targetWidthPx = 360,
                                            targetHeightPx = 220
                                        ),
                                        contentDescription = property.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                                )
                                            )
                                    )
                                    IconButton(
                                        onClick = { onRemoveProperty(property.id) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(26.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Rimuovi",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = property.title,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = property.location,
                                            color = TextSecondaryDark,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { onSimulateInRoiCalculator(property) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BentoPurpleHeader,
                                        contentColor = BentoPurpleOnContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simula ROI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Metric Values
                                ComparisonMetricValueCell(
                                    valueText = currencyFormatter.format(property.price),
                                    isWinner = property.id == minPriceId,
                                    winnerText = "Prezzo Minimo"
                                )

                                ComparisonMetricValueCell(
                                    valueText = currencyFormatter.format(property.estimatedMarketValue),
                                    isWinner = false
                                )

                                ComparisonMetricValueCell(
                                    valueText = "-${property.discountPercent}%",
                                    isWinner = property.id == maxDiscountId,
                                    winnerText = "Sconto Max"
                                )

                                ComparisonMetricValueCell(
                                    valueText = property.surfaceSqm?.let { "$it m²" } ?: "N/D",
                                    isWinner = false
                                )

                                ComparisonMetricValueCell(
                                    valueText = if (property.pricePerSqm != null) "${currencyFormatter.format(property.pricePerSqm)}/m²" else "N/D",
                                    isWinner = property.id == minPricePerSqmId,
                                    winnerText = "Best €/m²"
                                )

                                ComparisonMetricValueCell(
                                    valueText = String.format(Locale.getDefault(), "%.1f%%", property.estimatedRoiCapRate),
                                    isWinner = property.id == maxRoiId,
                                    winnerText = "Max ROI"
                                )

                                ComparisonMetricValueCell(
                                    valueText = currencyFormatter.format(property.totalProfitPotential),
                                    isWinner = property.id == maxProfitId,
                                    winnerText = "Max Margine"
                                )

                                ComparisonMetricValueCell(
                                    valueText = property.propertyType,
                                    isWinner = false
                                )

                                ComparisonMetricValueCell(
                                    valueText = property.statusOrDistress,
                                    isWinner = false
                                )

                                ComparisonMetricValueCell(
                                    valueText = property.sourceName,
                                    isWinner = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ComparisonMetricLabelRow(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondaryDark
        )
    }
}

@Composable
private fun ComparisonMetricValueCell(
    valueText: String,
    isWinner: Boolean,
    winnerText: String = "Migliore"
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(vertical = 2.dp)
            .background(
                color = if (isWinner) EmeraldGreen.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
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
                fontSize = 12.sp,
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
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonTopInsightsBanner(
    properties: List<ComparableProperty>,
    minPriceId: String?,
    maxRoiId: String?,
    minPricePerSqmId: String?
) {
    val bestPriceProp = remember(minPriceId) { properties.find { it.id == minPriceId } }
    val bestRoiProp = remember(maxRoiId) { properties.find { it.id == maxRoiId } }
    val bestPriceSqmProp = remember(minPricePerSqmId) { properties.find { it.id == minPricePerSqmId } }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.GERMANY).apply { maximumFractionDigits = 0 } }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoPurpleHeader),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                    text = "Sintesi Analitica Highlights",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (bestRoiProp != null) {
                    InsightChip(
                        title = "Max Cap Rate / ROI",
                        value = "${String.format(Locale.getDefault(), "%.1f%%", bestRoiProp.estimatedRoiCapRate)} (${bestRoiProp.title})",
                        badgeColor = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (bestPriceSqmProp != null) {
                    InsightChip(
                        title = "Best Prezzo / m²",
                        value = bestPriceSqmProp.pricePerSqm?.let { "${currencyFormatter.format(it)}/m²" } ?: "N/D",
                        badgeColor = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (bestPriceProp != null) {
                    InsightChip(
                        title = "Prezzo d'Ingresso",
                        value = currencyFormatter.format(bestPriceProp.price),
                        badgeColor = AmberGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightChip(
    title: String,
    value: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = badgeColor
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun exportComparisonCsv(context: android.content.Context, properties: List<ComparableProperty>) {
    try {
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.GERMANY)
        val sb = StringBuilder()
        sb.append("Titolo Immobile,Location,Prezzo Chiesto (€),Valore Mercato (€),Sconto %,Superficie (m²),Prezzo/m²,Cap Rate / ROI %,Tipologia,Stato / Asta\n")

        properties.forEach { p ->
            sb.append("\"${p.title.replace("\"", "\"\"")}\",")
            sb.append("\"${p.location.replace("\"", "\"\"")}\",")
            sb.append("${p.price},")
            sb.append("${p.estimatedMarketValue},")
            sb.append("${p.discountPercent}%,")
            sb.append("${p.surfaceSqm ?: "N/D"},")
            sb.append("${p.pricePerSqm ?: "N/D"},")
            sb.append("${p.estimatedRoiCapRate}%,")
            sb.append("\"${p.propertyType}\",")
            sb.append("\"${p.statusOrDistress}\"\n")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Report Confronto Immobili DealRadar")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }

        val chooser = Intent.createChooser(intent, "Condividi Report Confronto CSV")
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Errore durante l'esportazione: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
