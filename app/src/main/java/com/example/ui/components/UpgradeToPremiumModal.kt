package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.InvestorProfile
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

enum class PremiumSubscriptionTier {
    ANNUAL_PRO,
    MONTHLY_PRO,
    SINGLE_DEAL_UNLOCK
}

/**
 * Modal overlay explaining in rich detail all the investor benefits
 * of upgrading to Premium/PRO to access detailed ROI, OMI market benchmarks,
 * yield stress-testing, and unlocked perital dossiers for property records.
 */
@Composable
fun UpgradeToPremiumModal(
    deal: PropertyDeal? = null,
    investorProfile: InvestorProfile? = null,
    onDismiss: () -> Unit,
    onActivateProMembership: () -> Unit,
    onUnlockSingleDeal: ((dealId: Long) -> Unit)? = null,
    onUseTokenToUnlock: ((dealId: Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 } }
    val availableTokens = investorProfile?.availableUnlockTokens ?: 0
    
    var selectedTier by remember { mutableStateOf(PremiumSubscriptionTier.ANNUAL_PRO) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Benefici ROI & KPI, 1: Confronto Piani
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 16.dp)
                .testTag("upgrade_to_premium_modal"),
            shape = RoundedCornerShape(24.dp),
            color = DarkSlateBg,
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        AmberGold,
                        PurpleIndigo,
                        CyanAccent
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Bar with Close Button
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
                            shape = CircleShape,
                            color = AmberGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, AmberGold),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "Investor PRO",
                                    tint = AmberGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Investor PRO Intelligence",
                                    color = TextPrimaryDark,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AmberGold
                                ) {
                                    Text(
                                        text = "UNLIMITED",
                                        color = Color.Black,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Sblocca analisi ROI e indicatori di mercato asseverati",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (!isProcessing) onDismiss() },
                        modifier = Modifier.testTag("modal_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextMutedDark)
                    }
                }

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceCardDark,
                    contentColor = CyanAccent,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AmberGold
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Vantaggi ROI & KPI",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) AmberGold else TextSecondaryDark
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Scegli Abbonamento",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) AmberGold else TextSecondaryDark
                            )
                        }
                    )
                }

                // Scrollable Body Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedTab == 0) {
                        item {
                            // Banner Deal Context (if opened from a specific deal)
                            if (deal != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceCardDark,
                                    border = BorderStroke(1.dp, SurfaceCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Immobile in Analisi:",
                                                fontSize = 10.sp,
                                                color = TextMutedDark
                                            )
                                            Text(
                                                text = deal.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${deal.location} • ${deal.surfaceSqm} m²",
                                                fontSize = 11.sp,
                                                color = CyanAccent
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Richiesta Base",
                                                fontSize = 9.sp,
                                                color = TextMutedDark
                                            )
                                            Text(
                                                text = currencyFormat.format(deal.askingPrice),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldGreen
                                            )
                                            Text(
                                                text = "Stima CTU: ${currencyFormat.format(deal.estimatedMarketValue)}",
                                                fontSize = 9.sp,
                                                color = TextSecondaryDark
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Perché sbloccare i Dati Finanziari Avanzati?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        // 4 Detailed Benefit Cards
                        item {
                            BenefitCardItem(
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                iconColor = EmeraldGreen,
                                title = "1. Analisi Finanziaria & Margini Fix & Flip",
                                subtitle = "Simulazione completa del conto economico",
                                bulletPoints = listOf(
                                    "Stima plusvalenza netta e margine al m² post-ristrutturazione.",
                                    "Calcolo automatico di imposte di registro (2% prima casa o 9% seconda casa), onorario notarile e spese di perizia.",
                                    "Computo metrico parametrico delle opere edili necessarie."
                                )
                            )
                        }

                        item {
                            BenefitCardItem(
                                icon = Icons.Default.Assessment,
                                iconColor = CyanAccent,
                                title = "2. Benchmark OMI & Intelligenza di Mercato",
                                subtitle = "Dati certi certificati Agenzia delle Entrate",
                                bulletPoints = listOf(
                                    "Prezzo medio al m² registrato nella micro-zona di riferimento.",
                                    "Rilevamento storico dei trend di rivalutazione YoY (ultimi 24 mesi).",
                                    "Indice di liquidità della zona e stima dei giorni medi di assorbimento sul mercato."
                                )
                            )
                        }

                        item {
                            BenefitCardItem(
                                icon = Icons.Default.Key,
                                iconColor = AmberGold,
                                title = "3. Rendimento da Locazione (Buy & Hold)",
                                subtitle = "Gross Yield, Net Yield e Cash Flow Mensile",
                                bulletPoints = listOf(
                                    "Canone di locazione mensile asseverato su annunci comparabili.",
                                    "Confronto fiscale immediato: Cedolare Secca 10% (Canone Concordato), 21% ordinaria o regime societario IRES/IRAP.",
                                    "Stress test rate mutuo e simulazione di leva finanziaria."
                                )
                            )
                        }

                        item {
                            BenefitCardItem(
                                icon = Icons.Default.FolderZip,
                                iconColor = BentoPurpleOnContainer,
                                title = "4. Dossier Peritale Integrale & Accesso Diretto",
                                subtitle = "Rimozione totale del Blind Mode",
                                bulletPoints = listOf(
                                    "Indirizzo esatto dell'immobile, foglio e particella catastale.",
                                    "Download istantaneo delle perizie CTU originali, ordinanze del giudice e planimetrie quotate.",
                                    "Contatti diretti del Custode Giudiziario e del Delegato alla Vendita per visite immediate."
                                )
                            )
                        }

                        item {
                            BenefitCardItem(
                                icon = Icons.Default.PictureAsPdf,
                                iconColor = CyanAccent,
                                title = "5. Export Dossier PDF Bancario & Offerte",
                                subtitle = "Documentazione pronta per istituti di credito e soci",
                                bulletPoints = listOf(
                                    "Esportazione PDF con grafici di rendimento e analisi di rischio per la richiesta di mutuo/finanziamento.",
                                    "Generatore automatico di offerte d'acquisto e lettere formali di partecipazione."
                                )
                            )
                        }
                    } else {
                        // TAB 1: Pricing & Subscription Selection
                        item {
                            Text(
                                text = "Scegli il piano più adatto al tuo volume di operazioni:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        item {
                            // Plan 1: Annual PRO (Best Value)
                            PlanOptionCard(
                                title = "Investor PRO Annuale",
                                badge = "RISPARMIA 20% - PIÙ POPOLARE",
                                badgeColor = EmeraldGreen,
                                price = "€79",
                                period = "/mese",
                                subprice = "Fatturati annualmente (€948/anno) • IVA compresa",
                                features = listOf(
                                    "Accesso ILLIMITATO a tutti gli immobili in Italia",
                                    "Visualizzazione completa di ROI, OMI e KPI di mercato",
                                    "Download illimitato di perizie CTU e planimetrie",
                                    "Allarmi istantanei sui ribassi d'asta via Push",
                                    "Esportazione illimitata Dossier PDF per Banche",
                                    "Supporto peritale e legale prioritario"
                                ),
                                isSelected = selectedTier == PremiumSubscriptionTier.ANNUAL_PRO,
                                onClick = { selectedTier = PremiumSubscriptionTier.ANNUAL_PRO }
                            )
                        }

                        item {
                            // Plan 2: Monthly PRO
                            PlanOptionCard(
                                title = "Investor PRO Mensile",
                                badge = "FLESSIBILE",
                                badgeColor = CyanAccent,
                                price = "€99",
                                period = "/mese",
                                subprice = "Rinnovo mensile • Disdici in qualsiasi momento con un click",
                                features = listOf(
                                    "Tutti i dati e KPI di mercato sbloccati",
                                    "Calcolatore ROI e Stress Test inclusi",
                                    "Download perizie CTU e contatti custodi",
                                    "Nessun vincolo di durata contrattuale"
                                ),
                                isSelected = selectedTier == PremiumSubscriptionTier.MONTHLY_PRO,
                                onClick = { selectedTier = PremiumSubscriptionTier.MONTHLY_PRO }
                            )
                        }

                        if (deal != null) {
                            item {
                                // Plan 3: Single Deal Unlock / Token
                                PlanOptionCard(
                                    title = if (availableTokens > 0) "Sblocca con 1 Token Gratuito" else "Sblocca Singola Operazione",
                                    badge = if (availableTokens > 0) "1 TOKEN DISPONIBILE" else "PAY-PER-DEAL",
                                    badgeColor = if (availableTokens > 0) AmberGold else TextSecondaryDark,
                                    price = if (availableTokens > 0) "GRATIS" else "€29",
                                    period = " una tantum",
                                    subprice = if (availableTokens > 0) "Usa il token di benvenuto per questo immobile" else "Accesso permanente a questo singolo immobile",
                                    features = listOf(
                                        "Sblocco completo di indirizzo, CTU e perizia di questo immobile",
                                        "Calcolatore ROI & Valutazione OMI sbloccati per questo record",
                                        "Generazione Dossier PDF per questo affare"
                                    ),
                                    isSelected = selectedTier == PremiumSubscriptionTier.SINGLE_DEAL_UNLOCK,
                                    onClick = { selectedTier = PremiumSubscriptionTier.SINGLE_DEAL_UNLOCK }
                                )
                            }
                        }

                        item {
                            // Trust & Security Note
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceCardDark,
                                border = BorderStroke(1.dp, SurfaceCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Transazione Sicura & Garanzia 100%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = "Pagamenti crittografati a 256-bit. Se non trovi valore entro 14 giorni, rimborso garantito senza domande.",
                                            fontSize = 10.sp,
                                            color = TextSecondaryDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Button
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            if (isProcessing) return@Button
                            isProcessing = true

                            when (selectedTier) {
                                PremiumSubscriptionTier.ANNUAL_PRO, PremiumSubscriptionTier.MONTHLY_PRO -> {
                                    onActivateProMembership()
                                    Toast.makeText(
                                        context,
                                        "Abbonamento Investor PRO attivato con successo! Tutti i dati e KPI sono ora sbloccati.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onDismiss()
                                }
                                PremiumSubscriptionTier.SINGLE_DEAL_UNLOCK -> {
                                    if (deal != null) {
                                        if (availableTokens > 0 && onUseTokenToUnlock != null) {
                                            onUseTokenToUnlock(deal.id)
                                            Toast.makeText(
                                                context,
                                                "Dossier e KPI sbloccati con successo tramite Token di benvenuto!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else if (onUnlockSingleDeal != null) {
                                            onUnlockSingleDeal(deal.id)
                                            Toast.makeText(
                                                context,
                                                "Acquisto completato! Dati e KPI sbloccati per ${deal.title}.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        onDismiss()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("modal_upgrade_pro_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (selectedTier == PremiumSubscriptionTier.SINGLE_DEAL_UNLOCK) Icons.Default.LockOpen else Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = when (selectedTier) {
                                        PremiumSubscriptionTier.ANNUAL_PRO -> "ATTIVA INVESTOR PRO ANNUALE (€79/m)"
                                        PremiumSubscriptionTier.MONTHLY_PRO -> "ATTIVA INVESTOR PRO MENSILE (€99/m)"
                                        PremiumSubscriptionTier.SINGLE_DEAL_UNLOCK -> if (availableTokens > 0) "SBLOCCA SUBITO CON 1 TOKEN" else "SBLOCCA QUESTO IMMOBILE (€29)"
                                    },
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }

                    if (selectedTab == 0) {
                        TextButton(
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Vedi dettagli e prezzi degli abbonamenti →",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitCardItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    bulletPoints: List<String>
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCardDark,
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                bulletPoints.forEach { point ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("•", color = iconColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = point,
                            fontSize = 11.sp,
                            color = TextPrimaryDark.copy(alpha = 0.9f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    badge: String,
    badgeColor: Color,
    price: String,
    period: String,
    subprice: String,
    features: List<String>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) SurfaceCardDark else SurfaceCardDark.copy(alpha = 0.6f),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) AmberGold else SurfaceCardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("plan_option_${title.replace(" ", "_").lowercase()}")
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
                    RadioButton(
                        selected = isSelected,
                        onClick = onClick,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = AmberGold,
                            unselectedColor = TextMutedDark
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.2f),
                    border = BorderStroke(0.5.dp, badgeColor)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = price,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) AmberGold else TextPrimaryDark
                )
                Text(
                    text = period,
                    fontSize = 12.sp,
                    color = TextSecondaryDark,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Text(
                text = subprice,
                fontSize = 10.sp,
                color = TextMutedDark
            )

            HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.5f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isSelected) AmberGold else CyanAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = feature,
                            fontSize = 11.sp,
                            color = TextPrimaryDark
                        )
                    }
                }
            }
        }
    }
}
