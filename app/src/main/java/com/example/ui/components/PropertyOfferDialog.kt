package com.example.ui.components

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.DealStage
import com.example.data.InvestorProfile
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PropertyOfferDialog(
    deal: PropertyDeal,
    investorProfile: InvestorProfile?,
    onDismiss: () -> Unit,
    onSubmitOfferToPipeline: (offerAmount: Double, newStage: String, notes: String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }

    // Minimum permissible offer for Italian judicial auctions is typically basePrice * 0.75 (-25%)
    val isAuction = deal.sourceKey.contains("asta", ignoreCase = true) || deal.propertyType.contains("Asta", ignoreCase = true)
    val minOfferAllowed = if (isAuction) deal.askingPrice * 0.75 else deal.askingPrice * 0.70
    
    var offerAmount by remember(deal.id) { mutableStateOf(minOfferAllowed) }
    var offerAmountInput by remember(deal.id) { mutableStateOf(minOfferAllowed.toInt().toString()) }
    var depositPercent by remember { mutableStateOf("10") }
    var closingDays by remember { mutableStateOf(if (isAuction) "120" else "90") }
    var includeMortgageContingency by remember { mutableStateOf(false) }
    var bidderName by remember { mutableStateOf(investorProfile?.fullName ?: "Marco Rossi") }
    var bidderEntity by remember { mutableStateOf(investorProfile?.companyName ?: "Quantum Capital RE Srl") }
    var bidderTaxCode by remember { mutableStateOf("IT09876543210") }
    var bidderPec by remember { mutableStateOf(investorProfile?.email ?: "m.rossi@pec.it") }

    val calculatedDiscountOnMarket = remember(offerAmount, deal.estimatedMarketValue) {
        if (deal.estimatedMarketValue > 0) {
            (((deal.estimatedMarketValue - offerAmount) / deal.estimatedMarketValue) * 100).toInt()
        } else {
            0
        }
    }

    val calculatedDepositAmount = remember(offerAmount, depositPercent) {
        val pct = depositPercent.toDoubleOrNull() ?: 10.0
        offerAmount * (pct / 100.0)
    }

    val simulatedGrossMargin = remember(offerAmount, deal.estimatedMarketValue) {
        (deal.estimatedMarketValue - offerAmount).coerceAtLeast(0.0)
    }

    val generatedOfferLetterText = remember(
        offerAmount, depositPercent, closingDays, includeMortgageContingency,
        bidderName, bidderEntity, bidderTaxCode, bidderPec
    ) {
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date())
        """
        =======================================================
        PROPOSTA D'ACQUISTO / MANIFESTAZIONE DI INTERESSE
        Quantum Real Estate Deal Radar • Piattaforma Investitori
        Data: $dateStr
        =======================================================
        
        OGGETTO: Proposta irrevocabile per immobile identificato al Fascicolo:
        • Immobile: ${deal.title}
        • Ubicazione: ${deal.location}
        • Procedura / Riferimento: ${deal.sourceName} (ID: #${deal.id})
        • Valore di Perizia Stimato: ${currencyFormat.format(deal.estimatedMarketValue)}
        
        1. SOGGETTO PROPONENTE:
        • Intestatario Proposta: $bidderName / $bidderEntity
        • C.F. / P.IVA: $bidderTaxCode
        • PEC / Email Referente: $bidderPec
        
        2. CONDIZIONI ECONOMICHE OFFERTA:
        • Prezzo Offerto: ${currencyFormat.format(offerAmount)} (${if (isAuction) "Offerta Telematica Asta" else "Proposta Saldo & Stralcio / Libero Mercato"})
        • Sconto su Valore Stimato: $calculatedDiscountOnMarket%
        • Cauzione / Caparra Confirmatoria ($depositPercent%): ${currencyFormat.format(calculatedDepositAmount)} (mediante bonifico bancario irrevocabile / assegno circolare)
        
        3. MODALITÀ E TERMINI DI SALDO PREZZO:
        • Termine massimo stipula rogito / versamento saldo: $closingDays giorni dall'accettazione / aggiudicazione.
        • Condizione Sospensiva Mutuo: ${if (includeMortgageContingency) "SI (subordinata a delibera reddituale entro 30gg)" else "NO (Liquidità Pronta / Senza clausola mutuo)"}.
        • Garanzie richieste: Immobile trasferito libero da persone e cose, con cancellazione di ogni gravame/ipoteca a cura della procedura.
        
        Firmato digitalmente per accettazione,
        $bidderEntity • $bidderName
        =======================================================
        """.trimIndent()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(8.dp)
                .testTag("property_offer_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceCardDark,
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldGreen.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Gavel,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Formula Offerta per l'Immobile",
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAuction) "Modulo Offerta Telematica Asta" else "Proposta d'Acquisto Privata",
                                color = TextMutedDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextMutedDark)
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Deal Summary Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSlateBg,
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(deal.title, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Prezzo Base: ${currencyFormat.format(deal.askingPrice)}", color = TextSecondaryDark, fontSize = 11.sp)
                                Text("Valore Mercato: ${currencyFormat.format(deal.estimatedMarketValue)}", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Section 1: Economic Offer Configuration
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "1. DEFINIZIONE PREZZO OFFERTA:",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )

                            // Quick Preset Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val presets = listOf(
                                    Triple("-25% Min Asta", minOfferAllowed, "min"),
                                    Triple("-20% Target", deal.askingPrice * 0.80, "target"),
                                    Triple("-10% Sicura", deal.askingPrice * 0.90, "safe")
                                )
                                presets.forEach { (label, amt, tag) ->
                                    val isCurrent = offerAmount.toInt() == amt.toInt()
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) CyanAccent.copy(alpha = 0.2f) else SurfaceCardDark,
                                        border = BorderStroke(1.dp, if (isCurrent) CyanAccent else SurfaceCardBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                offerAmount = amt
                                                offerAmountInput = amt.toInt().toString()
                                            }
                                            .testTag("offer_preset_$tag")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(label, color = if (isCurrent) CyanAccent else TextPrimaryDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Text(currencyFormat.format(amt), color = if (isCurrent) CyanAccent else TextMutedDark, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }

                            // Manual Amount Input
                            OutlinedTextField(
                                value = offerAmountInput,
                                onValueChange = { input ->
                                    offerAmountInput = input
                                    input.toDoubleOrNull()?.let { offerAmount = it }
                                },
                                label = { Text("Importo Offerta Proposta (€)", color = CyanAccent) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("offer_amount_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = SurfaceCardBorder,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Projected Gain & Discount Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldGreen.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Sconto su Valore OMI", color = TextMutedDark, fontSize = 9.sp)
                                        Text("-$calculatedDiscountOnMarket%", color = EmeraldGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AmberGold.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Plusvalenza Potenziale", color = TextMutedDark, fontSize = 9.sp)
                                        Text("+${currencyFormat.format(simulatedGrossMargin)}", color = AmberGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Terms & Contingencies
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "2. CONDIZIONI & CLAUSOLE CONTRATTUALI:",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = depositPercent,
                                    onValueChange = { depositPercent = it },
                                    label = { Text("Cauzione/Caparra (%)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark)
                                )

                                OutlinedTextField(
                                    value = closingDays,
                                    onValueChange = { closingDays = it },
                                    label = { Text("Giorni Saldo Rogito") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Clausola Sospensiva Delibera Mutuo", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Rende l'offerta vincolata all'approvazione della banca", color = TextMutedDark, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = includeMortgageContingency,
                                    onCheckedChange = { includeMortgageContingency = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = CyanAccent.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }

                    // Section 3: Bidder Entity Info
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "3. DATI OFFERENTE & INTESTATARIO:",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )

                            OutlinedTextField(
                                value = bidderEntity,
                                onValueChange = { bidderEntity = it },
                                label = { Text("Ragione Sociale SRL / Intestatario") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = bidderName,
                                    onValueChange = { bidderName = it },
                                    label = { Text("Legale Rappresentante") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark)
                                )

                                OutlinedTextField(
                                    value = bidderTaxCode,
                                    onValueChange = { bidderTaxCode = it },
                                    label = { Text("C.F. / P.IVA") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark)
                                )
                            }
                        }
                    }

                    // Section 4: Live Proposal Preview Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceCardDark,
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ANTEPRIMA LETTERA PROPOSTA:", color = TextMutedDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(generatedOfferLetterText))
                                        Toast.makeText(context, "Testo Proposta copiato negli appunti!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copia", tint = CyanAccent, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = generatedOfferLetterText,
                                color = TextSecondaryDark,
                                fontSize = 9.sp,
                                lineHeight = 13.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }

                // Action Bottom Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy / Share
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, generatedOfferLetterText)
                                putExtra(Intent.EXTRA_SUBJECT, "Proposta d'Acquisto - ${deal.title}")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Condividi Proposta d'Acquisto")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyanAccent)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Condividi", color = CyanAccent, fontSize = 12.sp)
                    }

                    // Save to Pipeline & Submit
                    Button(
                        onClick = {
                            onSubmitOfferToPipeline(
                                offerAmount,
                                DealStage.UNDER_CONTRACT.key,
                                "Offerta formulata di €${offerAmount.toInt()} (${if (includeMortgageContingency) "Con clausola mutuo" else "Senza vincolo mutuo"}) depositata per ${deal.title}."
                            )
                            Toast.makeText(context, "🎉 Offerta registrata nel CRM! Deal spostato in 'In Trattativa'.", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("submit_offer_to_pipeline_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invia & Registra nel CRM", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
