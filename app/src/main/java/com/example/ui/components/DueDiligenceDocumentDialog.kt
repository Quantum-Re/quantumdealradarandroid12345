package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDiligenceDocumentDialog(
    deal: PropertyDeal,
    documentTitle: String,
    documentType: String, // "CTU", "AVVISO", "PLANIMETRIA", "ORDINANZA", "REPORT_AI"
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var isDownloaded by remember { mutableStateOf(false) }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
    val rgeNumber = "R.G.E. N. ${(deal.id * 17) % 900 + 100}/${2023 + (deal.id.toInt() % 3)}"

    // Simulate downloading
    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            downloadProgress = 0f
            while (downloadProgress < 1f) {
                delay(120)
                downloadProgress += 0.15f
            }
            downloadProgress = 1f
            isDownloading = false
            isDownloaded = true
            Toast.makeText(context, "📥 Documento '$documentTitle' scaricato in /Download/", Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCardDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            shape = RoundedCornerShape(10.dp),
                            color = CyanAccent.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = when (documentType) {
                                    "CTU" -> Icons.Default.Description
                                    "AVVISO" -> Icons.Default.Gavel
                                    "PLANIMETRIA" -> Icons.Default.Map
                                    "ORDINANZA" -> Icons.Default.Verified
                                    else -> Icons.Default.Assessment
                                },
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Documentazione Due Diligence",
                                color = TextMutedDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = documentTitle,
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextSecondaryDark)
                    }
                }

                HorizontalDivider(color = SurfaceCardBorder)

                // Document Metadata Card (Tribunale / Procedura)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Procedura Esecutiva:", color = TextSecondaryDark, fontSize = 12.sp)
                            Text(rgeNumber, color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Immobile Target:", color = TextSecondaryDark, fontSize = 12.sp)
                            Text(deal.location, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Valutazione Perizia CTU:", color = TextSecondaryDark, fontSize = 12.sp)
                            Text(currencyFormat.format(deal.estimatedMarketValue), color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Offerta Minima Asta:", color = TextSecondaryDark, fontSize = 12.sp)
                            Text(currencyFormat.format(deal.askingPrice), color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Simulated Document View Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131722))
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "TRIBUNALE DI ORDINARIO - SEZIONE ESECUZIONI IMMOBILIARI",
                                color = TextMutedDark,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "RELAZIONE DI STIMA DEL C.T.U. NOMINATO ($rgeNumber)",
                                color = CyanAccent,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Dati Catastali: Foglio ${(deal.id * 3) % 40 + 1}, Particella ${(deal.id * 11) % 500 + 10}, Sub. ${(deal.id % 12) + 1}\n" +
                                        "• Categoria Catastale: A/2 - Classe 3 - Vani ${(deal.surfaceSqm / 25).coerceAtLeast(2)}\n" +
                                        "• Superficie Commerciale Calpestabile: ${deal.surfaceSqm} m²\n" +
                                        "• Stato Occupazionale: Libero da persone / Disponibile all'aggiudicazione\n" +
                                        "• Conformità Urbanistica: Esaminata in perizia. Piccole difformità interne sanabili con CILA in sanatoria (stima costi sanatoria: € 2.500).\n" +
                                        "• Valore di Stima Peritale: ${currencyFormat.format(deal.estimatedMarketValue)}\n" +
                                        "• Prezzo Base Asta: ${currencyFormat.format(deal.askingPrice)} (-${deal.discountPercent}% dal valore di stima)",
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Download Progress
                if (isDownloading) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Download in corso...", color = CyanAccent, fontSize = 12.sp)
                            Text("${(downloadProgress * 100).toInt()}%", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = CyanAccent,
                            trackColor = SurfaceCardBorder
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Text("Chiudi Anteprima", color = TextSecondaryDark, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { isDownloading = true },
                        enabled = !isDownloading,
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDownloaded) EmeraldGreen else CyanAccent,
                            disabledContainerColor = DarkSlateBg
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDownloaded) "Scaricato (PDF)" else "Scarica File PDF",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
