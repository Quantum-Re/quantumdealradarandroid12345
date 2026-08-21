package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.DealStage
import com.example.data.PriceHistory
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import com.example.util.ImageUtils
import java.text.NumberFormat
import java.util.Locale

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import com.example.util.AuctionDateStatus
import com.example.util.AuctionDateUtils
import com.example.util.PriceAlertNotificationManager
import com.example.util.PropertyPdfGenerator
import com.example.util.ImmobiliareObservatoryService
import com.example.ui.components.ImmobiliareAutoValuationCard
import com.example.ui.components.ImmobiliareObservatoryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealDetailBottomSheet(
    deal: PropertyDeal,
    priceHistory: List<PriceHistory>,
    onDismiss: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onCalculateRoiClick: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onRecordPriceDrop: (dealId: Long, newPrice: Double, eventLabel: String, dateRecorded: String) -> Unit = { _, _, _, _ -> },
    onSetPriceAlertThreshold: (dealId: Long, threshold: Double?) -> Unit = { _, _ -> },
    onTriggerTestNotification: (deal: PropertyDeal, threshold: Double) -> Unit = { _, _ -> },
    onDownloadOfflineMap: ((PropertyDeal) -> Unit)? = null,
    onUpdateStage: ((dealId: Long, newStage: String) -> Unit)? = null,
    onOpenGranularNotifications: (() -> Unit)? = null,
    investorProfile: com.example.data.InvestorProfile? = null,
    onUnlockSingleDeal: ((dealId: Long) -> Unit)? = null,
    onActivateProMembership: (() -> Unit)? = null,
    onUseTokenToUnlock: ((dealId: Long) -> Unit)? = null,
    onSubmitOffer: ((dealId: Long, amount: Double, newStage: String, notes: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val isBlind = com.example.util.BlindModeUtils.isDealBlind(deal.id, investorProfile)
    val displayTitle = com.example.util.BlindModeUtils.getMaskedTitle(deal, isBlind)
    val displayLocation = com.example.util.BlindModeUtils.getMaskedLocation(deal, isBlind)
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
    var notesText by remember { mutableStateOf(deal.notes) }

    // Dialog state for Blind Deal Unlock
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showUpgradePremiumModal by remember { mutableStateOf(false) }

    // Dialog state for Property Offer Formulation
    var showOfferDialog by remember { mutableStateOf(false) }

    // Price Alert threshold input state
    var thresholdInput by remember(deal.id, deal.priceAlertThreshold) {
        mutableStateOf(deal.priceAlertThreshold?.toInt()?.toString() ?: (deal.askingPrice * 0.9).toInt().toString())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.widget.Toast.makeText(context, "Permesso notifiche accordato! Riceverai allarmi sul calo prezzo.", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Permesso notifiche non accordato.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Dialog state for Due Diligence Documents
    var selectedDocForPreview by remember { mutableStateOf<Pair<String, String>?>(null) } // (title, type)

    // Dialog state for recording price drop
    var showPriceDropDialog by remember { mutableStateOf(false) }
    var newPriceInput by remember { mutableStateOf("") }
    var priceDropLabelInput by remember { mutableStateOf("Asta Deserta - Nuovo Prezzo") }

    // Dialog state for Bank & Partner PDF Dossier
    var showBankDossierDialog by remember { mutableStateOf(false) }
    var showObservatoryFullDialog by remember { mutableStateOf(false) }

    val autoValuation = remember(deal.id, deal.askingPrice, deal.location, deal.surfaceSqm) {
        ImmobiliareObservatoryService.evaluateProperty(
            location = deal.location,
            surfaceSqM = deal.surfaceSqm.toDouble(),
            askingPrice = deal.askingPrice
        )
    }

    var activeRoiCalcData by remember(deal.id) {
        val rent = (deal.surfaceSqm * 13.5).coerceAtLeast(650.0)
        // Il costo di ristrutturazione non si deduce dallo sconto sul prezzo: non c'è
        // alcuna relazione reale tra i due. Resta a zero finché l'utente non lo inserisce.
        val renov = 0.0
        val legal = (deal.askingPrice * 0.04).coerceAtLeast(4000.0)
        mutableStateOf(
            RoiCalculationData(
                purchasePrice = deal.askingPrice,
                renovationCost = renov,
                estimatedMonthlyRent = rent,
                legalFees = legal,
                monthlyExpenses = 150.0,
                downPaymentPercent = 20.0,
                mortgageRatePercent = 3.2,
                loanTermYears = 25,
                expectedResalePrice = deal.estimatedMarketValue
            )
        )
    }

    if (showUpgradePremiumModal) {
        UpgradeToPremiumModal(
            deal = deal,
            investorProfile = investorProfile,
            onDismiss = { showUpgradePremiumModal = false },
            onActivateProMembership = {
                onActivateProMembership?.invoke()
                showUpgradePremiumModal = false
            },
            onUnlockSingleDeal = { dealId ->
                onUnlockSingleDeal?.invoke(dealId)
                showUpgradePremiumModal = false
            },
            onUseTokenToUnlock = { dealId ->
                onUseTokenToUnlock?.invoke(dealId)
                showUpgradePremiumModal = false
            }
        )
    }

    if (showUnlockDialog) {
        BlindDealUnlockDialog(
            deal = deal,
            investorProfile = investorProfile,
            onDismiss = { showUnlockDialog = false },
            onUnlockSingleDeal = { dealId ->
                onUnlockSingleDeal?.invoke(dealId)
                showUnlockDialog = false
            },
            onActivateProMembership = {
                onActivateProMembership?.invoke()
                showUnlockDialog = false
            },
            onUseTokenToUnlock = { dealId ->
                onUseTokenToUnlock?.invoke(dealId)
                showUnlockDialog = false
            }
        )
    }

    if (showOfferDialog) {
        PropertyOfferDialog(
            deal = deal,
            investorProfile = investorProfile,
            onDismiss = { showOfferDialog = false },
            onSubmitOfferToPipeline = { amount, newStage, offerNotes ->
                onSubmitOffer?.invoke(deal.id, amount, newStage, offerNotes)
                showOfferDialog = false
            }
        )
    }

    if (selectedDocForPreview != null) {
        DueDiligenceDocumentDialog(
            deal = deal,
            documentTitle = selectedDocForPreview!!.first,
            documentType = selectedDocForPreview!!.second,
            onDismiss = { selectedDocForPreview = null }
        )
    }

    if (showPriceDropDialog) {
        AlertDialog(
            onDismissRequest = { showPriceDropDialog = false },
            title = { Text("Registra Ribasso d'Asta", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Inserisci il nuovo prezzo o l'offerta minima aggiornata dopo un'asta deserta per tracciare lo storico dei ribassi.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = newPriceInput,
                        onValueChange = { newPriceInput = it },
                        label = { Text("Nuovo Prezzo (€)", color = CyanAccent) },
                        placeholder = { Text("es. 320000", color = TextMutedDark) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder
                        )
                    )
                    OutlinedTextField(
                        value = priceDropLabelInput,
                        onValueChange = { priceDropLabelInput = it },
                        label = { Text("Causale / Evento", color = CyanAccent) },
                        placeholder = { Text("es. 2° Asta Deserta -25%", color = TextMutedDark) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedPrice = newPriceInput.toDoubleOrNull()
                        if (parsedPrice != null && parsedPrice > 0) {
                            onRecordPriceDrop(deal.id, parsedPrice, priceDropLabelInput, "Oggi")
                            showPriceDropDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Salva Ribasso", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriceDropDialog = false }) {
                    Text("Annulla", color = TextSecondaryDark)
                }
            },
            containerColor = SurfaceCardDark
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCardDark,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        modifier = Modifier.testTag("deal_detail_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Image Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageUtils.buildOptimizedImageRequest(
                            context = context,
                            data = deal.imageUrl.ifBlank { "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=800" },
                            targetWidthPx = 640,
                            targetHeightPx = 400
                        ),
                        contentDescription = deal.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Source Badge & Demo Badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val provenanceEnum = com.example.data.DataProvenance.fromString(deal.provenance)
                        if (!provenanceEnum.isTrustworthy) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RoseRed,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = provenanceEnum.label.uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.75f)
                        ) {
                            Text(
                                text = "Fonte: ${deal.sourceName}",
                                color = CyanAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Watchlist Button Overlay
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = if (deal.isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (deal.isBookmarked) "Rimuovi dai preferiti" else "Aggiungi ai preferiti",
                            tint = if (deal.isBookmarked) AmberGold else TextMutedDark
                        )
                    }
                }
            }

            item {
                // Blind Mode Banner if active for this deal
                if (isBlind) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AmberGold.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, AmberGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                                Text("OPERAZIONE IN MODALITÀ BLIND", color = AmberGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "I dati sensibili (indirizzo esatto, dati catastali, perizia CTU integrale e contatti del custode) sono protetti. Sblocca questo dossier per analizzare la perizia completa e fare un'offerta per l'immobile.",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = { showUnlockDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("sheet_unlock_deal_top_btn")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sblocca Dati & Fai Offerta (€29 o Pro)", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Title & Location & Watchlist Indicator
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayTitle,
                            color = if (isBlind) AmberGold else TextPrimaryDark,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isBlind) Icons.Default.Lock else Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isBlind) AmberGold else CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(text = displayLocation, color = if (isBlind) TextMutedDark else TextSecondaryDark, fontSize = 14.sp)
                        Text(text = "•", color = TextMutedDark)
                        Text(text = "${deal.surfaceSqm} m²", color = TextSecondaryDark, fontSize = 14.sp)
                    }

                    // Pipeline Stage Selector Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSlateBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.ViewColumn, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Stadio Pipeline Kanban:",
                                    color = TextPrimaryDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DealStage.values().forEach { stage ->
                                    val isSelected = DealStage.fromKey(deal.dealStage) == stage
                                    val stageColor = when (stage) {
                                        DealStage.PROSPECTING -> CyanAccent
                                        DealStage.UNDER_CONTRACT -> AmberGold
                                        DealStage.CLOSING -> Color(0xFFBA68C8)
                                        DealStage.CLOSED -> EmeraldGreen
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) stageColor else DarkSlateBg,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) stageColor else SurfaceCardBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onUpdateStage?.invoke(deal.id, stage.key) }
                                    ) {
                                        Text(
                                            text = stage.labelIt,
                                            color = if (isSelected) Color.Black else TextSecondaryDark,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Auction Expiration Banner
                    val auctionStatus = AuctionDateUtils.getStatus(deal.auctionDate)
                    if (auctionStatus !is AuctionDateStatus.NoDate) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (auctionStatus) {
                                is AuctionDateStatus.Expired -> RoseRed.copy(alpha = 0.2f)
                                is AuctionDateStatus.Today -> RoseRed.copy(alpha = 0.8f)
                                else -> PurpleIndigo.copy(alpha = 0.2f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (auctionStatus) {
                                    is AuctionDateStatus.Expired -> RoseRed
                                    is AuctionDateStatus.Today -> RoseRed
                                    else -> PurpleIndigo
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (auctionStatus is AuctionDateStatus.Expired) Icons.Default.Warning else Icons.Default.Gavel,
                                    contentDescription = null,
                                    tint = if (auctionStatus is AuctionDateStatus.Expired) RoseRed else CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = when (auctionStatus) {
                                        is AuctionDateStatus.Expired -> "⛔ ASTA SCADUTA IL ${auctionStatus.dateFormatted} - Non Partecipabile"
                                        is AuctionDateStatus.Today -> "🚨 ASTA IN SCADENZA OGGI (${auctionStatus.dateFormatted})"
                                        is AuctionDateStatus.Upcoming -> "⏳ Termine Presentazione Offerta: ${auctionStatus.dateFormatted} (${auctionStatus.daysLeft} giorni rimanenti)"
                                        else -> ""
                                    },
                                    color = if (auctionStatus is AuctionDateStatus.Expired) RoseRed else TextPrimaryDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(color = SurfaceCardBorder)
            }

            item {
                // Price Row & ROI CTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Offerta Base / Richiesta", color = TextMutedDark, fontSize = 11.sp)
                        Text(currencyFormat.format(deal.askingPrice), color = EmeraldGreen, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Valore Stima CTU: ${currencyFormat.format(deal.estimatedMarketValue)} (-${deal.discountPercent}%)", color = TextSecondaryDark, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onCalculateRoiClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("detail_calc_roi_btn")
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simula ROI", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                // Automated Valuation & Benchmark Card
                val unlockedPreview = if (autoValuation.isValid) {
                    listOf(
                        "📊 Prezzo medio micro-zona al m² (€${autoValuation.zoneAvgPricePerSqM.toInt()}/m²)",
                        "📈 Profitto stimato Fix & Flip (+€${autoValuation.estimatedFlipGrossProfit.toInt()})",
                        "🔑 Rendita stimata da locazione (€${autoValuation.estimatedMonthlyRent.toInt()}/mese - ${String.format(Locale.ITALY, "%.1f", autoValuation.grossRentalYieldPercent)}%)",
                        "⏱️ Rating liquidità (${autoValuation.liquidityRating})"
                    )
                } else {
                    listOf(
                        "⚠️ Dati insufficienti per la stima automatica: mancano ${autoValuation.missingFields.joinToString(", ")}"
                    )
                }

                LockedPremiumOverlay(
                    isLocked = isBlind,
                    title = "Valutazione e Benchmark di Mercato",
                    subtitle = "Accedi al benchmark micro-zona al m², stima rivendita Fix & Flip e canone locativo Buy & Hold",
                    unlockedItems = unlockedPreview,
                    ctaText = "SBLOCCA VALUTAZIONE & KPI MERCATO",
                    onUpgradeClick = { showUpgradePremiumModal = true }
                ) {
                    ImmobiliareAutoValuationCard(
                        valuation = autoValuation,
                        onApplyRoi = {
                            if (isBlind) {
                                showUpgradePremiumModal = true
                            } else if (autoValuation.isValid) {
                                activeRoiCalcData = activeRoiCalcData.copy(
                                    purchasePrice = deal.askingPrice,
                                    renovationCost = (deal.surfaceSqm * 400.0).coerceAtLeast(15000.0),
                                    estimatedMonthlyRent = autoValuation.estimatedMonthlyRent,
                                    expectedResalePrice = autoValuation.estimatedMarketValueRenovated
                                )
                                onCalculateRoiClick()
                            }
                        },
                        onOpenObservatory = {
                            if (isBlind) {
                                showUpgradePremiumModal = true
                            } else {
                                showObservatoryFullDialog = true
                            }
                        }
                    )
                }
            }

            item {
                var showInteractiveRoiCalc by remember(deal.id) { mutableStateOf(false) }

                Surface(
                    color = DarkSlateBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (showInteractiveRoiCalc) CyanAccent else SurfaceCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInteractiveRoiCalc = !showInteractiveRoiCalc }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (showInteractiveRoiCalc) "Nascondi Calcolatore ROI Rapido" else "Apri Calcolatore ROI & Rendimenti",
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = if (showInteractiveRoiCalc) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (showInteractiveRoiCalc) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val estimatedMonthlyRent = (deal.surfaceSqm * 13.5).coerceAtLeast(650.0)
                    // Il costo di ristrutturazione non si deduce dallo sconto sul prezzo:
                    // resta a zero finché l'utente non lo inserisce nel calcolatore.
                    val estimatedRenov = 0.0
                    LockedPremiumOverlay(
                        isLocked = isBlind,
                        title = "Simulatore Finanziario ROI & Rendimenti",
                        subtitle = "Sblocca il modello di calcolo con imposte di registro, perizia di stima e cash flow",
                        unlockedItems = listOf(
                            "🧮 Calcolo automatico Cap Rate e Net Yield asseverato",
                            "🏛️ Simulazione imposte di registro (2% prima casa / 9% seconda)",
                            "📑 Esportazione Dossier Finanziario PDF per banche e investitori"
                        ),
                        ctaText = "SBLOCCA CALCOLATORE ROI AVANZATO",
                        onUpgradeClick = { showUpgradePremiumModal = true }
                    ) {
                        InteractiveRoiCalculatorCard(
                            initialPurchasePrice = deal.askingPrice,
                            initialRenovationCost = estimatedRenov,
                            initialEstimatedRent = estimatedMonthlyRent,
                            initialLegalFees = (deal.askingPrice * 0.04).coerceAtLeast(4000.0),
                            initialMonthlyExpenses = 150.0,
                            propertyTitle = deal.title,
                            propertyLocation = deal.location,
                            surfaceSqm = deal.surfaceSqm,
                            onCalculateCompleted = { activeRoiCalcData = it },
                            onExportPdfClick = { calcData ->
                                if (isBlind) {
                                    showUpgradePremiumModal = true
                                } else {
                                    activeRoiCalcData = calcData
                                    showBankDossierDialog = true
                                }
                            }
                        )
                    }
                }
            }

            item {
                // Due Diligence Interactive Risk Checklist
                var checkUrban by remember(deal.id) { mutableStateOf(true) }
                var checkOccupancy by remember(deal.id) { mutableStateOf(false) }
                var checkCondoArrears by remember(deal.id) { mutableStateOf(true) }
                var checkLiens by remember(deal.id) { mutableStateOf(true) }
                var checkInspection by remember(deal.id) { mutableStateOf(false) }

                val completedCount = listOf(checkUrban, checkOccupancy, checkCondoArrears, checkLiens, checkInspection).count { it }
                val riskLevel = when {
                    completedCount >= 4 -> "RISCHIO BASSO 🟢"
                    completedCount >= 2 -> "RISCHIO MEDIO 🟡"
                    else -> "RISCHIO DA VERIFICARE 🔴"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                    shape = RoundedCornerShape(12.dp)
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                                Text("Checklist Due Diligence Rischi", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    completedCount >= 4 -> EmeraldGreen.copy(alpha = 0.2f)
                                    completedCount >= 2 -> AmberGold.copy(alpha = 0.2f)
                                    else -> RoseRed.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = "$completedCount/5 - $riskLevel",
                                    color = when {
                                        completedCount >= 4 -> EmeraldGreen
                                        completedCount >= 2 -> AmberGold
                                        else -> RoseRed
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text("Verifica i 5 requisiti fondamentali dell'immobile prima di fare un'offerta in asta:", color = TextMutedDark, fontSize = 11.sp)

                        DueDiligenceCheckItem("Conformità Edilizia & Catastale (CILA/SCIA sanabile)", checkUrban) { checkUrban = it }
                        DueDiligenceCheckItem("Stato Occupativo (Immobile Libero / Custode nominato)", checkOccupancy) { checkOccupancy = it }
                        DueDiligenceCheckItem("Assenza Spese Condominiali Arretrate > 2 anni", checkCondoArrears) { checkCondoArrears = it }
                        DueDiligenceCheckItem("Verifica Cancellazione Pignoramenti e Ipoteca", checkLiens) { checkLiens = it }
                        DueDiligenceCheckItem("Sopralluogo Fisico Effettuato con Custode", checkInspection) { checkInspection = it }
                    }
                }
            }

            item {
                // Watchlist & Price Drop Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                    shape = RoundedCornerShape(12.dp)
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                                Text("Storico Ribassi & Prezzi Rilevati", color = AmberGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            TextButton(
                                onClick = {
                                    newPriceInput = (deal.askingPrice * 0.8).toInt().toString()
                                    showPriceDropDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                Text("+ Registra Ribasso", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        val savingsAmount = (deal.estimatedMarketValue - deal.askingPrice).coerceAtLeast(0.0)
                        if (savingsAmount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldGreen.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Risparmio Totale rispetto a CTU:", color = TextSecondaryDark, fontSize = 11.sp)
                                    Text("${currencyFormat.format(savingsAmount)} (-${deal.discountPercent}%)", color = EmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (priceHistory.isEmpty()) {
                            Text("Nessun ribasso registrato precedentemente. Clicca '+ Registra Ribasso' per tracciare un'asta deserta o sconto.", color = TextMutedDark, fontSize = 12.sp)
                        } else {
                            priceHistory.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(item.eventLabel, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text(item.dateRecorded, color = TextMutedDark, fontSize = 10.sp)
                                    }
                                    Text(currencyFormat.format(item.price), color = EmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            item {
                // Local Price Drop Notification Alert Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("price_alert_card"),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                                Text("Allerta Prezzo Locale (Saved Deals)", color = CyanAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            if (deal.priceAlertThreshold != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldGreen.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, EmeraldGreen)
                                ) {
                                    Text(
                                        text = "ATTIVO ≤ ${currencyFormat.format(deal.priceAlertThreshold)}",
                                        color = EmeraldGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Ricevi un avviso istantaneo sul dispositivo quando il prezzo dell'immobile scende al di sotto della tua soglia target.",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )

                        // Presets
                        Text("Soglia Prezzo Target (€):", color = TextMutedDark, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val p5 = (deal.askingPrice * 0.95).toInt()
                            val p10 = (deal.askingPrice * 0.90).toInt()
                            val p15 = (deal.askingPrice * 0.85).toInt()

                            FilterChip(
                                selected = thresholdInput == p5.toString(),
                                onClick = { thresholdInput = p5.toString() },
                                label = { Text("-5% (${currencyFormat.format(p5)})", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = thresholdInput == p10.toString(),
                                onClick = { thresholdInput = p10.toString() },
                                label = { Text("-10% (${currencyFormat.format(p10)})", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = thresholdInput == p15.toString(),
                                onClick = { thresholdInput = p15.toString() },
                                label = { Text("-15% (${currencyFormat.format(p15)})", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = thresholdInput,
                                onValueChange = { thresholdInput = it },
                                label = { Text("Soglia Personalizzata (€)", color = CyanAccent) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("price_alert_threshold_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark,
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = SurfaceCardBorder
                                )
                            )

                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PriceAlertNotificationManager.hasNotificationPermission(context)) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    val thresholdVal = thresholdInput.toDoubleOrNull()
                                    if (thresholdVal != null && thresholdVal > 0) {
                                        onSetPriceAlertThreshold(deal.id, thresholdVal)
                                        android.widget.Toast.makeText(context, "Soglia allerta salvata: ${currencyFormat.format(thresholdVal)}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("save_price_alert_btn")
                            ) {
                                Text("Salva", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PriceAlertNotificationManager.hasNotificationPermission(context)) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    val thresholdVal = thresholdInput.toDoubleOrNull() ?: (deal.askingPrice * 0.9)
                                    onTriggerTestNotification(deal, thresholdVal)
                                    android.widget.Toast.makeText(context, "Inviata notifica di prova!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Notifica", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (deal.priceAlertThreshold != null) {
                                TextButton(
                                    onClick = {
                                        onSetPriceAlertThreshold(deal.id, null)
                                        android.widget.Toast.makeText(context, "Allerta prezzo disattivata.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Rimuovi Allerta", color = RoseRed, fontSize = 11.sp)
                                }
                            }
                        }

                        if (onOpenGranularNotifications != null) {
                            Surface(
                                color = BentoPurpleHeader,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenGranularNotifications() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = BentoPurpleOnContainer, modifier = Modifier.size(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Configura Allarmi Granulari Avanzati", color = BentoPurpleOnContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("Imposta soglie % dinamiche, rendimento target e monitoraggio aste", color = TextSecondaryDark, fontSize = 9.sp)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BentoPurpleOnContainer, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Due Diligence Documents Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBlind) "🔒 Documenti Due Diligence CTU (Protetti)" else "📁 Documenti Due Diligence CTU",
                            color = if (isBlind) AmberGold else TextPrimaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = {
                                if (isBlind) {
                                    showUnlockDialog = true
                                } else {
                                    selectedDocForPreview = "Dossier Integrale Asta (Pack ZIP)" to "CTU"
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(if (isBlind) Icons.Default.Lock else Icons.Default.DownloadForOffline, contentDescription = null, tint = if (isBlind) AmberGold else CyanAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBlind) "Sblocca Tutti" else "Scarica Tutti (ZIP)", color = if (isBlind) AmberGold else CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DocCardItem(
                            title = if (isBlind) "Perizia CTU (🔒 Blind)" else "Perizia CTU (PDF)",
                            subtext = if (isBlind) "Dati oscurati" else "Valutazione peritale",
                            icon = if (isBlind) Icons.Default.Lock else Icons.Default.Description,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (isBlind) showUnlockDialog = true
                                else selectedDocForPreview = "Perizia di Stima C.T.U." to "CTU"
                            }
                        )

                        DocCardItem(
                            title = if (isBlind) "Avviso Asta (🔒 Blind)" else "Avviso Asta (PDF)",
                            subtext = if (isBlind) "Bando protetto" else "Bando & R.G.E.",
                            icon = if (isBlind) Icons.Default.Lock else Icons.Default.Gavel,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (isBlind) showUnlockDialog = true
                                else selectedDocForPreview = "Avviso di Vendita e Bando" to "AVVISO"
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DocCardItem(
                            title = if (isBlind) "Planimetria (🔒 Blind)" else "Planimetria (PDF)",
                            subtext = if (isBlind) "Mappe oscurate" else "Mappe & Scheda",
                            icon = if (isBlind) Icons.Default.Lock else Icons.Default.Map,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (isBlind) showUnlockDialog = true
                                else selectedDocForPreview = "Planimetria Catastale e Mappe" to "PLANIMETRIA"
                            }
                        )

                        DocCardItem(
                            title = if (isBlind) "Ordinanza (🔒 Blind)" else "Ordinanza (PDF)",
                            subtext = if (isBlind) "Atto protetto" else "Atto del Giudice",
                            icon = if (isBlind) Icons.Default.Lock else Icons.Default.Verified,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (isBlind) showUnlockDialog = true
                                else selectedDocForPreview = "Ordinanza di Vendita Giudiziaria" to "ORDINANZA"
                            }
                        )
                    }
                }
            }

            item {
                // Rich Text Investor Notes Field
                RichTextNotesEditor(
                    initialNotes = deal.notes,
                    onSaveNotes = { newNotes ->
                        notesText = newNotes
                        onSaveNotes(newNotes)
                    }
                )
            }

            item {
                // PDF Dossier & Share via Email Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_summary_card"),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                                Text("Dossier Finanziario PDF & Email", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BentoPurpleHeader
                            ) {
                                Text(
                                    text = "PDF REPORT",
                                    color = CyanAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Genera un report PDF completo con il riepilogo finanziario, le proiezioni di rendimento, la stima costi e l'analisi dei trend di mercato per questo immobile.",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    showBankDossierDialog = true
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("open_bank_dossier_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Dossier Banche & Partner", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    PropertyPdfGenerator.generateAndSharePdf(context, deal, emailOnly = false)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_pdf_btn"),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Condividi", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (onDownloadOfflineMap != null) {
                item {
                    // Offline Site Visit Map Cache Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("offline_map_cache_card"),
                        colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AmberGold.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Map, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                                    }
                                }

                                Column {
                                    Text(
                                        text = "Mappa Offline per Sopralluogo",
                                        color = TextPrimaryDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Memorizza in Room DB le tessere della mappa nel raggio di 1.5 km per l'ispezione senza internet.",
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    onDownloadOfflineMap(deal)
                                    Toast.makeText(context, "Scaricamento mappa offline avviato!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("download_map_offline_btn")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scarica", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                // Primary Action: Submit Offer or Unlock Deal
                if (isBlind) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, AmberGold)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                                Text("Sblocca per Valutare & Fare un'Offerta", color = AmberGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "Per tutelare le operazioni e gli accordi di riservatezza, lo sblocco consente l'accesso ai dati catastali, alla perizia completa, ai contatti e al modulo di formulazione dell'offerta vincolante.",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = { showUnlockDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sheet_unlock_primary_btn")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SBLOCCA OPERAZIONE ORA", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, EmeraldGreen)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Gavel, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                                Text("Dossier Completo Sbloccato", color = EmeraldGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "Hai accesso a tutti i dati dell'operazione. Puoi formulare direttamente la tua proposta d'acquisto o offerta d'asta per avviare la trattativa nel CRM.",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = { showOfferDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sheet_submit_offer_primary_btn")
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("FAI UN'OFFERTA PER L'IMMOBILE 💼", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            item {
                // Fonte & Contatto Venditore Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isBlind) AmberGold.copy(alpha = 0.4f) else EmeraldGreen.copy(alpha = 0.4f))
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBlind) Icons.Default.Lock else Icons.Default.Language,
                                    contentDescription = null,
                                    tint = if (isBlind) AmberGold else EmeraldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isBlind) "Fonte Origine & Custode (Riservati)" else "Fonte Origine & Venditore",
                                    color = if (isBlind) AmberGold else EmeraldGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SurfaceCardDark
                            ) {
                                Text(
                                    text = deal.sourceName,
                                    color = TextPrimaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isBlind)
                                "I riferimenti diretti della procedura e i recapiti del custode giudiziario o del referente sono visibili dopo lo sblocco:"
                            else
                                "Link diretto all'annuncio ufficiale o portale di vendita per verificare i dettagli, scaricare la perizia o contattare direttamente il referente:",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )

                        val contactInfo = com.example.util.BlindModeUtils.getMaskedContact(isBlind)
                        val displayUrl = if (isBlind) "${contactInfo.first} (${contactInfo.second})" else deal.sourceUrl
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceCardDark,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, SurfaceCardBorder)
                        ) {
                            Text(
                                text = displayUrl,
                                color = if (isBlind) AmberGold else CyanAccent,
                                fontSize = 11.sp,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isBlind) {
                                        showUnlockDialog = true
                                    } else {
                                        try {
                                            val uri = if (deal.sourceUrl.startsWith("http://") || deal.sourceUrl.startsWith("https://")) {
                                                Uri.parse(deal.sourceUrl)
                                            } else {
                                                Uri.parse("https://${deal.sourceUrl}")
                                            }
                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Impossibile aprire il link della fonte", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isBlind) AmberGold else EmeraldGreen)
                            ) {
                                Icon(
                                    imageVector = if (isBlind) Icons.Default.LockOpen else Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBlind) "Sblocca per Contattare" else "Contatta Venditore / Apri Fonte",
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier
                                    .background(RoseRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .size(48.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Elimina immobile", tint = RoseRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBankDossierDialog) {
        PdfReportExportDialog(
            calcData = activeRoiCalcData,
            initialTitle = deal.title,
            initialLocation = deal.location,
            initialSurfaceSqm = deal.surfaceSqm,
            onDismissRequest = { showBankDossierDialog = false }
        )
    }

    if (showObservatoryFullDialog) {
        ImmobiliareObservatoryDialog(
            initialMunicipality = deal.location,
            onDismissRequest = { showObservatoryFullDialog = false },
            onApplyPricePerSqM = { pricePerSqM, _ ->
                val newResale = deal.surfaceSqm * pricePerSqM
                val newRent = deal.surfaceSqm * (pricePerSqM * 0.005)
                activeRoiCalcData = activeRoiCalcData.copy(
                    expectedResalePrice = newResale,
                    estimatedMonthlyRent = newRent
                )
                showObservatoryFullDialog = false
            }
        )
    }
}

@Composable
private fun DueDiligenceCheckItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClickLabel = "Attiva o disattiva $label") { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = EmeraldGreen,
                uncheckedColor = TextMutedDark,
                checkmarkColor = Color.Black
            )
        )
        Text(
            text = label,
            color = if (checked) TextPrimaryDark else TextSecondaryDark,
            fontSize = 12.sp,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun DocCardItem(
    title: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "Visualizza $title") { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSlateBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = title, tint = CyanAccent, modifier = Modifier.size(20.dp))
            Column {
                Text(title, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(subtext, color = TextMutedDark, fontSize = 10.sp)
            }
        }
    }
}

