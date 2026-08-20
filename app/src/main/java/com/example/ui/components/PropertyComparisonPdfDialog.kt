package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Property
import com.example.ui.theme.*
import com.example.util.PropertyComparisonReportCalculator
import com.example.util.PropertyOpportunityEvaluation
import com.example.util.PropertyPdfGenerator
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyComparisonPdfDialog(
    allProperties: List<Property>,
    initialPropertyA: Property? = null,
    initialPropertyB: Property? = null,
    evaluations: Map<Long, PropertyOpportunityEvaluation> = emptyMap(),
    onDismiss: () -> Unit,
    euroFormat: NumberFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 } }
) {
    val context = LocalContext.current

    var selectedPropAId by rememberSaveable {
        mutableStateOf(initialPropertyA?.id ?: allProperties.firstOrNull()?.id ?: -1L)
    }
    var selectedPropBId by rememberSaveable {
        mutableStateOf(
            initialPropertyB?.id ?: allProperties.getOrNull(1)?.id ?: allProperties.firstOrNull()?.id ?: -1L
        )
    }

    var investorNotes by rememberSaveable { mutableStateOf("") }
    var isPropertyADropdownOpen by remember { mutableStateOf(false) }
    var isPropertyBDropdownOpen by remember { mutableStateOf(false) }

    val propA = allProperties.find { it.id == selectedPropAId } ?: allProperties.firstOrNull()
    val propB = allProperties.find { it.id == selectedPropBId } ?: allProperties.getOrNull(1) ?: propA

    val isValidComparison = propA != null && propB != null && propA.id != propB.id

    val comparisonData = remember(propA, propB, evaluations) {
        if (propA != null && propB != null) {
            PropertyComparisonReportCalculator.calculate(
                propertyA = propA,
                propertyB = propB,
                evalA = evaluations[propA.id],
                evalB = evaluations[propB.id]
            )
        } else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceCardDark,
            border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("property_comparison_pdf_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BentoPurpleHeader,
                            border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF Comparison",
                                    tint = BentoPurpleOnContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Dossier di Confronto PDF",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Genera foglio di analisi side-by-side per 2 immobili",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = TextMutedDark
                        )
                    }
                }

                HorizontalDivider(color = SurfaceCardBorder)

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Property Selection Pickers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Selector A
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "IMMOBILE A",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )

                            ExposedDropdownMenuBox(
                                expanded = isPropertyADropdownOpen,
                                onExpandedChange = { isPropertyADropdownOpen = !isPropertyADropdownOpen },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DarkSlateBg,
                                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("select_property_a_dropdown")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = propA?.title?.ifBlank { propA.address } ?: "Seleziona",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = CyanAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                ExposedDropdownMenu(
                                    expanded = isPropertyADropdownOpen,
                                    onDismissRequest = { isPropertyADropdownOpen = false }
                                ) {
                                    allProperties.forEach { property ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = property.title.ifBlank { property.address },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = "${euroFormat.format(property.price)} • ${property.surfaceSqm} m²",
                                                        fontSize = 10.sp,
                                                        color = TextMutedDark
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedPropAId = property.id
                                                isPropertyADropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Selector B
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "IMMOBILE B",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoPurpleOnContainer
                            )

                            ExposedDropdownMenuBox(
                                expanded = isPropertyBDropdownOpen,
                                onExpandedChange = { isPropertyBDropdownOpen = !isPropertyBDropdownOpen },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DarkSlateBg,
                                    border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("select_property_b_dropdown")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = propB?.title?.ifBlank { propB.address } ?: "Seleziona",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = BentoPurpleOnContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                ExposedDropdownMenu(
                                    expanded = isPropertyBDropdownOpen,
                                    onDismissRequest = { isPropertyBDropdownOpen = false }
                                ) {
                                    allProperties.forEach { property ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = property.title.ifBlank { property.address },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = "${euroFormat.format(property.price)} • ${property.surfaceSqm} m²",
                                                        fontSize = 10.sp,
                                                        color = TextMutedDark
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedPropBId = property.id
                                                isPropertyBDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!isValidComparison) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RoseRed.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, RoseRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RoseRed, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Seleziona due immobili distinti per generare il dossier comparativo.",
                                    fontSize = 11.sp,
                                    color = RoseRed
                                )
                            }
                        }
                    }

                    // Live Side-by-Side Preview Summary Card
                    if (comparisonData != null && isValidComparison) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
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
                                    Text(
                                        text = "Anteprima Metriche Chiave nel PDF",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = EmeraldGreen.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "A vs B PRONTO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.5f))

                                // Quick Metric Comparison Table
                                PreviewMetricRow(
                                    label = "Prezzo d'Ingresso",
                                    valA = "${euroFormat.format(comparisonData.purchasePriceA)} (${comparisonData.pricePerSqmA.toInt()}€/m²)",
                                    valB = "${euroFormat.format(comparisonData.purchasePriceB)} (${comparisonData.pricePerSqmB.toInt()}€/m²)",
                                    winner = comparisonData.winnerEntryPricePerSqm
                                )
                                PreviewMetricRow(
                                    label = "Ristrutturazione (CapEx)",
                                    valA = euroFormat.format(comparisonData.renovationCostA),
                                    valB = euroFormat.format(comparisonData.renovationCostB),
                                    winner = comparisonData.winnerLowerCapExRisk
                                )
                                PreviewMetricRow(
                                    label = "Costo Totale Investito",
                                    valA = euroFormat.format(comparisonData.totalInvestedBasisA),
                                    valB = euroFormat.format(comparisonData.totalInvestedBasisB),
                                    winner = comparisonData.winnerLowestTotalCapital
                                )
                                PreviewMetricRow(
                                    label = "Plusvalenza Stimata",
                                    valA = "+${euroFormat.format(comparisonData.projectedGrossProfitA)}",
                                    valB = "+${euroFormat.format(comparisonData.projectedGrossProfitB)}",
                                    winner = comparisonData.winnerMaxProfit
                                )
                                PreviewMetricRow(
                                    label = "Ritorno sul Capitale (ROI)",
                                    valA = String.format(Locale.ITALY, "%.1f%%", comparisonData.projectedRoiPercentA),
                                    valB = String.format(Locale.ITALY, "%.1f%%", comparisonData.projectedRoiPercentB),
                                    winner = comparisonData.winnerMaxRoi
                                )
                                PreviewMetricRow(
                                    label = "Rendimento Locativo (Yield)",
                                    valA = String.format(Locale.ITALY, "%.1f%%", comparisonData.grossRentalYieldA),
                                    valB = String.format(Locale.ITALY, "%.1f%%", comparisonData.grossRentalYieldB),
                                    winner = comparisonData.winnerMaxRentalYield
                                )

                                HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.5f))

                                // Verdict Preview Box
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceCardDark,
                                    border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Verdetto Strategico Algoritmico:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BentoPurpleOnContainer
                                        )
                                        Text(
                                            text = comparisonData.verdictTitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = comparisonData.verdictSummary,
                                            fontSize = 10.sp,
                                            color = TextSecondaryDark,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Optional Notes Field
                    OutlinedTextField(
                        value = investorNotes,
                        onValueChange = { investorNotes = it },
                        label = { Text("Note & Annotazioni Investitore per il PDF (Opzionale)") },
                        placeholder = { Text("es. Valutazione offerta congiunta o strategia di uscita primaria...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comparison_pdf_notes_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoPurpleOnContainer,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedContainerColor = DarkSlateBg,
                            unfocusedContainerColor = DarkSlateBg
                        ),
                        maxLines = 3
                    )
                }

                HorizontalDivider(color = SurfaceCardBorder)

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("comparison_pdf_cancel_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Text("Annulla", color = TextSecondaryDark)
                    }

                    Button(
                        onClick = {
                            if (propA != null && propB != null && isValidComparison) {
                                val pdf = PropertyPdfGenerator.generateComparisonReportPdf(
                                    context = context,
                                    propertyA = propA,
                                    propertyB = propB,
                                    evalA = evaluations[propA.id],
                                    evalB = evaluations[propB.id],
                                    investorNotes = investorNotes
                                )
                                if (pdf != null && pdf.exists()) {
                                    PropertyPdfGenerator.shareComparisonPdf(
                                        context = context,
                                        pdfFile = pdf,
                                        propertyA = propA,
                                        propertyB = propB,
                                        emailOnly = false
                                    )
                                    onDismiss()
                                }
                            }
                        },
                        enabled = isValidComparison,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("generate_comparison_pdf_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoPurpleOnContainer,
                            disabledContainerColor = BentoPurpleOnContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Genera & Condividi PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewMetricRow(
    label: String,
    valA: String,
    valB: String,
    winner: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondaryDark,
            modifier = Modifier.weight(1.2f)
        )

        Row(
            modifier = Modifier.weight(2f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "A: $valA",
                fontSize = 10.sp,
                fontWeight = if (winner == "A") FontWeight.Bold else FontWeight.Normal,
                color = if (winner == "A") EmeraldGreen else TextPrimaryDark,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "B: $valB",
                fontSize = 10.sp,
                fontWeight = if (winner == "B") FontWeight.Bold else FontWeight.Normal,
                color = if (winner == "B") EmeraldGreen else TextPrimaryDark,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
