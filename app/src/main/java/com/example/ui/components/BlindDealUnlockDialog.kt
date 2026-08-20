package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.InvestorProfile
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

enum class UnlockOptionType {
    SINGLE_DEAL,
    PRO_MEMBERSHIP,
    USE_TOKEN
}

@Composable
fun BlindDealUnlockDialog(
    deal: PropertyDeal,
    investorProfile: InvestorProfile?,
    onDismiss: () -> Unit,
    onUnlockSingleDeal: (dealId: Long) -> Unit,
    onActivateProMembership: () -> Unit,
    onUseTokenToUnlock: (dealId: Long) -> Unit
) {
    val context = LocalContext.current
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
    
    val availableTokens = investorProfile?.availableUnlockTokens ?: 1
    var selectedOption by remember {
        mutableStateOf(
            if (availableTokens > 0) UnlockOptionType.USE_TOKEN else UnlockOptionType.SINGLE_DEAL
        )
    }
    var selectedPaymentMethod by remember { mutableStateOf("CREDIT_CARD") } // "CREDIT_CARD", "APPLE_GOOGLE_PAY", "BANK_TRANSFER"
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("blind_deal_unlock_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceCardDark,
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
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
                            color = CyanAccent.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Sblocca Operazione & Dati Completi",
                                color = TextPrimaryDark,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Passa da Blind a Dossier Asseverato",
                                color = TextMutedDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextMutedDark)
                    }
                }

                // Deal Teaser Info Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSlateBg,
                    border = BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "-${deal.discountPercent}%",
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = deal.propertyType.ifBlank { "Immobile a Reddito" } + " • " + deal.location.split("(").firstOrNull()?.trim(),
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Prezzo: ${currencyFormat.format(deal.askingPrice)} | Valore Stimato: ${currencyFormat.format(deal.estimatedMarketValue)}",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // What is Unlocked Checklist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSlateBg.copy(alpha = 0.6f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "COSA VIENE SBLOCCATO ALL'ISTANTE:",
                        color = CyanAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    val unlockedPerks = listOf(
                        "📍 Indirizzo Esatto, Civico & Mappa Catastale Georeferenziata",
                        "📑 Perizia CTU del Tribunale & Conformità Urbanistica in PDF",
                        "🏛️ Dati Catastali (Foglio, Particella, Sub) & Numero Procedura R.G.E.",
                        "📞 Contatti Diretti Custode Giudiziario / Delegato alla Vendita",
                        "💼 Modulo di Calcolo & Generazione Lettera d'Offerta / Proposta"
                    )

                    unlockedPerks.forEach { perk ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                            Text(perk, color = TextPrimaryDark, fontSize = 11.sp)
                        }
                    }
                }

                // Unlock Options Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "SCEGLI LA MODALITÀ DI ACCESSO:",
                        color = TextMutedDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    // Option 1: Welcome Token (if available)
                    if (availableTokens > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedOption == UnlockOptionType.USE_TOKEN) CyanAccent.copy(alpha = 0.15f) else DarkSlateBg,
                            border = BorderStroke(1.dp, if (selectedOption == UnlockOptionType.USE_TOKEN) CyanAccent else SurfaceCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = UnlockOptionType.USE_TOKEN }
                                .testTag("unlock_option_token")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedOption == UnlockOptionType.USE_TOKEN,
                                        onClick = { selectedOption = UnlockOptionType.USE_TOKEN },
                                        colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                                    )
                                    Column {
                                        Text("Usa Token Gratuito di Benvenuto", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Disponibili: $availableTokens token nel tuo profilo", color = EmeraldGreen, fontSize = 10.sp)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldGreen.copy(alpha = 0.2f)
                                ) {
                                    Text("GRATIS", color = EmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }

                    // Option 2: Single Deal Unlock
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedOption == UnlockOptionType.SINGLE_DEAL) CyanAccent.copy(alpha = 0.15f) else DarkSlateBg,
                        border = BorderStroke(1.dp, if (selectedOption == UnlockOptionType.SINGLE_DEAL) CyanAccent else SurfaceCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = UnlockOptionType.SINGLE_DEAL }
                            .testTag("unlock_option_single")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedOption == UnlockOptionType.SINGLE_DEAL,
                                    onClick = { selectedOption = UnlockOptionType.SINGLE_DEAL },
                                    colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                                )
                                Column {
                                    Text("Sblocca Singola Operazione", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Accesso a vita al dossier di questo immobile", color = TextMutedDark, fontSize = 10.sp)
                                }
                            }
                            Text("€29", color = AmberGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Option 3: Pro Membership
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedOption == UnlockOptionType.PRO_MEMBERSHIP) PurpleIndigo.copy(alpha = 0.25f) else DarkSlateBg,
                        border = BorderStroke(1.dp, if (selectedOption == UnlockOptionType.PRO_MEMBERSHIP) PurpleIndigo else SurfaceCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = UnlockOptionType.PRO_MEMBERSHIP }
                            .testTag("unlock_option_pro")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedOption == UnlockOptionType.PRO_MEMBERSHIP,
                                    onClick = { selectedOption = UnlockOptionType.PRO_MEMBERSHIP },
                                    colors = RadioButtonDefaults.colors(selectedColor = PurpleIndigo)
                                )
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Pass Investor PRO Illimitato", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AmberGold.copy(alpha = 0.2f)
                                        ) {
                                            Text("CONSIGLIATO", color = AmberGold, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                    Text("Tutti i deal radar sbloccati + allarmi istantanei", color = TextSecondaryDark, fontSize = 10.sp)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("€99", color = PurpleIndigo, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("/mese", color = TextMutedDark, fontSize = 9.sp)
                            }
                        }
                    }
                }

                // Payment Method Selector (only if paid)
                if (selectedOption != UnlockOptionType.USE_TOKEN) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "METODO DI PAGAMENTO:",
                            color = TextMutedDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val methods = listOf(
                                Triple("CREDIT_CARD", "Carta / Debito", Icons.Default.CreditCard),
                                Triple("APPLE_GOOGLE_PAY", "Google Pay", Icons.Default.Payment),
                                Triple("BANK_TRANSFER", "Bonifico Istantaneo", Icons.Default.AccountBalance)
                            )
                            methods.forEach { (id, label, icon) ->
                                val isSel = selectedPaymentMethod == id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) CyanAccent.copy(alpha = 0.2f) else DarkSlateBg,
                                    border = BorderStroke(1.dp, if (isSel) CyanAccent else SurfaceCardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedPaymentMethod = id }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = if (isSel) CyanAccent else TextMutedDark, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(label, color = if (isSel) CyanAccent else TextSecondaryDark, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                // Confirmation Button
                Button(
                    onClick = {
                        isProcessing = true
                        when (selectedOption) {
                            UnlockOptionType.USE_TOKEN -> {
                                onUseTokenToUnlock(deal.id)
                                Toast.makeText(context, "🎉 Token utilizzato! Dossier sbloccato con successo.", Toast.LENGTH_LONG).show()
                            }
                            UnlockOptionType.SINGLE_DEAL -> {
                                onUnlockSingleDeal(deal.id)
                                Toast.makeText(context, "💳 Pagamento €29 completato! Dossier operazione sbloccato.", Toast.LENGTH_LONG).show()
                            }
                            UnlockOptionType.PRO_MEMBERSHIP -> {
                                onActivateProMembership()
                                Toast.makeText(context, "👑 Benvenuto in Investor PRO! Tutti i deal sono ora sbloccati.", Toast.LENGTH_LONG).show()
                            }
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_unlock_deal_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedOption == UnlockOptionType.PRO_MEMBERSHIP) PurpleIndigo else CyanAccent
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = if (selectedOption == UnlockOptionType.PRO_MEMBERSHIP) Color.White else Color.Black, modifier = Modifier.size(16.dp))
                            Text(
                                text = when (selectedOption) {
                                    UnlockOptionType.USE_TOKEN -> "Usa 1 Token e Sblocca Subito"
                                    UnlockOptionType.SINGLE_DEAL -> "Paga €29 & Sblocca Dossier Completo"
                                    UnlockOptionType.PRO_MEMBERSHIP -> "Attiva Abbonamento PRO (€99/mese)"
                                },
                                color = if (selectedOption == UnlockOptionType.PRO_MEMBERSHIP) Color.White else Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
