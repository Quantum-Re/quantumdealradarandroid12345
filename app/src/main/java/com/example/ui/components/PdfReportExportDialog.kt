package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.PropertyPdfGenerator
import java.io.File
import java.text.NumberFormat
import java.util.Locale

/**
 * Professional Dialog for configuring and exporting Bank & Partner Investment PDF reports.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReportExportDialog(
    calcData: RoiCalculationData,
    initialTitle: String = "Immobile Target Investimento",
    initialLocation: String = "Milano (MI)",
    initialSurfaceSqm: Int = 80,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
    }

    var reportType by remember { mutableStateOf("Banca / Richiesta Finanziamento") }
    var recipientName by remember { mutableStateOf("Ufficio Fidi & Crediti Immobiliari") }
    var propertyTitle by remember { mutableStateOf(initialTitle) }
    var propertyLocation by remember { mutableStateOf(initialLocation) }
    var strategy by remember { mutableStateOf("Buy & Hold (Messa a Reddito)") }
    var investorNotes by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<File?>(null) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("pdf_report_export_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = DarkSlateBg,
            border = BorderStroke(1.dp, SurfaceCardBorder)
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
                                .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Esporta Dossier PDF",
                                color = TextPrimaryDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Prospetto Finanziario per Banche & Partner",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("pdf_dialog_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = SurfaceCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form & Preview
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Target Template Preset Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Profilo Destinatario del Report:",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = reportType.startsWith("Banca"),
                                onClick = {
                                    reportType = "Banca / Richiesta Finanziamento"
                                    recipientName = "Ufficio Crediti & Mutui"
                                },
                                label = { Text("🏦 Banca / Fidi") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanAccent,
                                    selectedLabelColor = Color.Black
                                )
                            )
                            FilterChip(
                                selected = reportType.startsWith("Partner"),
                                onClick = {
                                    reportType = "Partner di Co-Investimento (Equity)"
                                    recipientName = "Partner Privato di Capitale"
                                },
                                label = { Text("🤝 Partner / Investitori") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                            FilterChip(
                                selected = reportType.startsWith("Business"),
                                onClick = {
                                    reportType = "Business Plan & Due Diligence"
                                    recipientName = "Comitato di Valutazione"
                                },
                                label = { Text("📋 Business Plan Interno") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberGold,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    // Input 1: Recipient Name / Institution
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Nome Banca / Partner Destinatario", fontSize = 12.sp, color = TextSecondaryDark)
                        OutlinedTextField(
                            value = recipientName,
                            onValueChange = { recipientName = it },
                            placeholder = { Text("es. Intesa Sanpaolo / Dott. Rossi") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_recipient_name_input")
                        )
                    }

                    // Input 2: Title and Location
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Titolo Progetto / Immobile", fontSize = 12.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = propertyTitle,
                                onValueChange = { propertyTitle = it },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pdf_property_title_input")
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Località", fontSize = 12.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = propertyLocation,
                                onValueChange = { propertyLocation = it },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pdf_location_input")
                            )
                        }
                    }

                    // Strategy Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Strategia Principale nel Report:", fontSize = 12.sp, color = TextSecondaryDark)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestionChip(
                                onClick = { strategy = "Buy & Hold (Messa a Reddito)" },
                                label = { Text("🔑 Buy & Hold (Rendita)", fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (strategy.startsWith("Buy")) CyanAccent.copy(alpha = 0.2f) else SurfaceCardDark,
                                    labelColor = if (strategy.startsWith("Buy")) CyanAccent else TextPrimaryDark
                                ),
                                border = BorderStroke(1.dp, if (strategy.startsWith("Buy")) CyanAccent else SurfaceCardBorder),
                                modifier = Modifier.weight(1f)
                            )
                            SuggestionChip(
                                onClick = { strategy = "Fix & Flip (Riqualificazione & Rivendita)" },
                                label = { Text("🔨 Fix & Flip (Rivendita)", fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (strategy.startsWith("Fix")) AmberGold.copy(alpha = 0.2f) else SurfaceCardDark,
                                    labelColor = if (strategy.startsWith("Fix")) AmberGold else TextPrimaryDark
                                ),
                                border = BorderStroke(1.dp, if (strategy.startsWith("Fix")) AmberGold else SurfaceCardBorder),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Custom Notes for Bank/Partner
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Note & Assunzioni per la Banca/Partner (Opzionale)", fontSize = 12.sp, color = TextSecondaryDark)
                        OutlinedTextField(
                            value = investorNotes,
                            onValueChange = { investorNotes = it },
                            placeholder = { Text("es. Richiesta preammortamento 12 mesi per ristrutturazione...") },
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_investor_notes_input")
                        )
                    }

                    // Live Metrics Preview Card (Institutional Summary)
                    Surface(
                        color = SurfaceCardDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📊 Indicatori Inclusi nel Report",
                                    color = TextPrimaryDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "2 Pagine A4",
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            HorizontalDivider(color = SurfaceCardBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Costo Totale Progetto:", fontSize = 11.sp, color = TextSecondaryDark)
                                    Text(currencyFormat.format(calcData.totalProjectCost), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Capitale Proprio (Equity):", fontSize = 11.sp, color = TextSecondaryDark)
                                    Text(currencyFormat.format(calcData.initialCashRequired), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Mutuo Richiesto (LTV):", fontSize = 11.sp, color = TextSecondaryDark)
                                    Text("${currencyFormat.format(calcData.loanAmount)} (${100 - calcData.downPaymentPercent.toInt()}%)", fontSize = 12.sp, color = AmberGold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Cash Flow Netto:", fontSize = 11.sp, color = TextSecondaryDark)
                                    Text("${currencyFormat.format(calcData.monthlyNetCashFlow)}/mese", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                }
                            }

                            val dscr = if (calcData.annualDebtService > 0) calcData.netOperatingIncome / calcData.annualDebtService else 9.99
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("DSCR (Copertura Debito):", fontSize = 11.sp, color = TextSecondaryDark)
                                    Text(if (calcData.annualDebtService > 0) String.format(Locale.ITALY, "%.2fx", dscr) else "100% Cash", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Cash-on-Cash Return:", fontSize = 11.sp, color = TextSecondaryDark)
                                    Text(String.format(Locale.ITALY, "%.2f%%", calcData.cashOnCashReturnPercent), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = SurfaceCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // View / Preview PDF Button
                        OutlinedButton(
                            onClick = {
                                val file = PropertyPdfGenerator.generateBankAndPartnerReportPdf(
                                    context = context,
                                    calcData = calcData,
                                    propertyTitle = propertyTitle,
                                    location = propertyLocation,
                                    surfaceSqm = initialSurfaceSqm,
                                    recipientName = recipientName,
                                    recipientType = reportType,
                                    strategy = strategy,
                                    investorNotes = investorNotes
                                )
                                if (file != null && file.exists()) {
                                    generatedFile = file
                                    PropertyPdfGenerator.openPdfFile(context, file)
                                } else {
                                    Toast.makeText(context, "Errore generazione PDF", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CyanAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pdf_open_preview_btn")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Anteprima PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Share PDF Button
                        Button(
                            onClick = {
                                isGenerating = true
                                val file = PropertyPdfGenerator.generateBankAndPartnerReportPdf(
                                    context = context,
                                    calcData = calcData,
                                    propertyTitle = propertyTitle,
                                    location = propertyLocation,
                                    surfaceSqm = initialSurfaceSqm,
                                    recipientName = recipientName,
                                    recipientType = reportType,
                                    strategy = strategy,
                                    investorNotes = investorNotes
                                )
                                isGenerating = false
                                if (file != null && file.exists()) {
                                    generatedFile = file
                                    PropertyPdfGenerator.shareBankAndPartnerPdf(
                                        context = context,
                                        pdfFile = file,
                                        propertyTitle = propertyTitle,
                                        recipientName = recipientName,
                                        emailOnly = false
                                    )
                                } else {
                                    Toast.makeText(context, "Errore nella creazione del file PDF.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("pdf_generate_and_share_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Condividi con Partner", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Copy textual summary button
                    TextButton(
                        onClick = {
                            val summary = buildString {
                                appendLine("📄 DOSSIER FINANZIARIO & BUSINESS PLAN")
                                appendLine("🏢 Progetto: $propertyTitle ($propertyLocation)")
                                appendLine("🏦 Destinatario: $recipientName ($reportType)")
                                appendLine("💶 Prezzo Acquisto: ${currencyFormat.format(calcData.purchasePrice)}")
                                appendLine("🔨 Lavori & Ristrutturazione: ${currencyFormat.format(calcData.renovationCost)}")
                                appendLine("🏢 Costo Totale: ${currencyFormat.format(calcData.totalProjectCost)}")
                                appendLine("💵 Capitale Proprio (Equity): ${currencyFormat.format(calcData.initialCashRequired)}")
                                appendLine("🏦 Mutuo Richiesto: ${currencyFormat.format(calcData.loanAmount)} (${100 - calcData.downPaymentPercent.toInt()}% LTV)")
                                appendLine("💰 Canone Affitto: ${currencyFormat.format(calcData.estimatedMonthlyRent)}/m (${currencyFormat.format(calcData.annualGrossRent)}/anno)")
                                appendLine("📈 DSCR (Copertura Debito): ${String.format(Locale.ITALY, "%.2fx", if (calcData.annualDebtService > 0) calcData.netOperatingIncome / calcData.annualDebtService else 9.99)}")
                                appendLine("⚡ Cash-on-Cash Return: ${String.format(Locale.ITALY, "%.2f%%", calcData.cashOnCashReturnPercent)}")
                                appendLine("💵 Cash Flow Netto: ${currencyFormat.format(calcData.monthlyNetCashFlow)}/mese (${currencyFormat.format(calcData.annualNetCashFlow)}/anno)")
                                if (investorNotes.isNotBlank()) appendLine("📝 Note: $investorNotes")
                            }
                            clipboardManager.setText(AnnotatedString(summary))
                            Toast.makeText(context, "Sintesi per banca copiata negli appunti!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_copy_bank_summary_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copia Sintesi Testuale per Email / WhatsApp", color = TextSecondaryDark, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
