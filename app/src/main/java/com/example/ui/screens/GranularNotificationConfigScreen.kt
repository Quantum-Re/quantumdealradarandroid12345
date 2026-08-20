package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.DealRadarViewModel
import com.example.ui.theme.*
import com.example.util.GranularNotificationManager
import com.example.util.ImageUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GranularNotificationConfigScreen(
    viewModel: DealRadarViewModel,
    onNavigateBack: () -> Unit,
    onOpenDealDetail: ((PropertyDeal) -> Unit)? = null
) {
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 } }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Observe flows from ViewModel
    val allDeals by viewModel.allDeals.collectAsStateWithLifecycle()
    val savedDeals by viewModel.savedDeals.collectAsStateWithLifecycle()
    val globalSettings by viewModel.globalNotificationSettings.collectAsStateWithLifecycle()
    val propertyAlertsMap by viewModel.granularPropertyAlerts.collectAsStateWithLifecycle()
    val alertHistory by viewModel.granularAlertHistory.collectAsStateWithLifecycle()

    // Permission state
    var hasPermission by remember {
        mutableStateOf(GranularNotificationManager.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Permesso notifiche accordato! Riceverai allarmi granulari.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permesso notifiche negato.", Toast.LENGTH_SHORT).show()
        }
    }

    // Screen UI State
    var selectedScreenTab by remember { mutableStateOf(0) } // 0: Immobili, 1: Storico Eventi, 2: Impostazioni Globali
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE_ONLY", "AUCTION", "RESIDENTIAL"
    var showPresetDialog by remember { mutableStateOf(false) }
    var showSimulateDropDialogForDeal by remember { mutableStateOf<PropertyDeal?>(null) }
    var simulatedNewPriceStr by remember { mutableStateOf("") }

    // Aggregate statistics
    val totalTrackedDeals = allDeals.size
    val activeAlertsCount = allDeals.count { deal ->
        propertyAlertsMap[deal.id]?.isAlertEnabled ?: true
    }
    val totalMonitoredValue = allDeals.sumOf { it.askingPrice }

    // Filter deals list
    val displayedDeals = remember(allDeals, propertyAlertsMap, searchQuery, selectedFilterCategory) {
        allDeals.filter { deal ->
            val alert = propertyAlertsMap[deal.id]
            val isEnabled = alert?.isAlertEnabled ?: true

            val matchesFilter = when (selectedFilterCategory) {
                "ACTIVE_ONLY" -> isEnabled
                "AUCTION" -> deal.propertyType.contains("Asta", ignoreCase = true) || deal.sourceKey.contains("asta", ignoreCase = true)
                "RESIDENTIAL" -> deal.propertyType.contains("Residenziale", ignoreCase = true)
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                deal.title.contains(searchQuery, ignoreCase = true) ||
                        deal.location.contains(searchQuery, ignoreCase = true) ||
                        deal.propertyType.contains(searchQuery, ignoreCase = true)
            }

            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Notifiche & Soglie Prezzo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Allarmi granulari e ribassi personalizzati",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("granular_notifications_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    // Preset Strategies Action
                    IconButton(
                        onClick = { showPresetDialog = true },
                        modifier = Modifier.testTag("open_alert_presets_btn")
                    ) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = "Strategie Preset",
                            tint = AmberGold
                        )
                    }

                    // Global Test Notification
                    IconButton(
                        onClick = {
                            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                val sampleDeal = allDeals.firstOrNull() ?: PropertyDeal(
                                    id = 1,
                                    title = "Attico Panoramico Duomo",
                                    location = "Milano (MI)",
                                    propertyType = "Residenziale",
                                    askingPrice = 280000.0,
                                    estimatedMarketValue = 360000.0
                                )
                                val sampleAlert = propertyAlertsMap[sampleDeal.id] ?: GranularPropertyAlert(
                                    dealId = sampleDeal.id,
                                    dealTitle = sampleDeal.title,
                                    dealLocation = sampleDeal.location,
                                    currentAskingPrice = sampleDeal.askingPrice,
                                    targetPriceThreshold = sampleDeal.askingPrice * 0.85
                                )
                                viewModel.sendTestNotificationForProperty(sampleAlert)
                                Toast.makeText(context, "Notifica di test inviata!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("send_global_test_notification_btn")
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = "Test Notifica",
                            tint = CyanAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSlateBg
                )
            )
        },
        containerColor = DarkSlateBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission Alert Banner if missing
            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Surface(
                    color = AmberGold.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Permesso notifiche disattivato", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Abilita le notifiche per ricevere allarmi istantanei sui ribassi di prezzo", color = TextSecondaryDark, fontSize = 10.sp)
                        }
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Abilita", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Top Tab Navigation: Immobili Tracciati | Storico Allarmi | Impostazioni Globali
            TabRow(
                selectedTabIndex = selectedScreenTab,
                containerColor = DarkSlateBg,
                contentColor = TextPrimaryDark,
                divider = { Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SurfaceCardBorder)) }
            ) {
                Tab(
                    selected = selectedScreenTab == 0,
                    onClick = { selectedScreenTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.HomeWork, contentDescription = null, modifier = Modifier.size(15.dp))
                            Text("Immobili ($totalTrackedDeals)", fontSize = 12.sp, fontWeight = if (selectedScreenTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("tab_tracked_properties")
                )
                Tab(
                    selected = selectedScreenTab == 1,
                    onClick = { selectedScreenTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp))
                            Text("Storico (${alertHistory.size})", fontSize = 12.sp, fontWeight = if (selectedScreenTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("tab_alert_history")
                )
                Tab(
                    selected = selectedScreenTab == 2,
                    onClick = { selectedScreenTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                            Text("Preferenze", fontSize = 12.sp, fontWeight = if (selectedScreenTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("tab_global_settings")
                )
            }

            when (selectedScreenTab) {
                0 -> {
                    // ==========================================
                    // TAB 0: TRACKED PROPERTIES GRANULAR ALERTS
                    // ==========================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            // Metric KPI Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GranularKpiPill(
                                    title = "Allarmi Attivi",
                                    value = "$activeAlertsCount / $totalTrackedDeals",
                                    subtitle = "Tracciati attivi",
                                    accentColor = EmeraldGreen,
                                    icon = Icons.Default.NotificationsActive,
                                    modifier = Modifier.weight(1f)
                                )
                                GranularKpiPill(
                                    title = "Soglia Predefinita",
                                    value = "-${globalSettings.defaultPriceDropPercent.toInt()}%",
                                    subtitle = "Ribasso target",
                                    accentColor = RoseRed,
                                    icon = Icons.Default.TrendingDown,
                                    modifier = Modifier.weight(1f)
                                )
                                GranularKpiPill(
                                    title = "Valore Monitorato",
                                    value = currencyFormat.format(totalMonitoredValue),
                                    subtitle = "Massa totale",
                                    accentColor = AmberGold,
                                    icon = Icons.Default.AccountBalance,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Search and Filter Bar
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Cerca per titolo, città o zona...", color = TextMutedDark, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancella", tint = TextSecondaryDark)
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BentoPurpleOnContainer,
                                    unfocusedBorderColor = SurfaceCardBorder,
                                    focusedContainerColor = SurfaceCardDark,
                                    unfocusedContainerColor = SurfaceCardDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("property_alert_search_input")
                            )
                        }

                        // Filter Chips
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedFilterCategory == "ALL",
                                    onClick = { selectedFilterCategory = "ALL" },
                                    label = { Text("Tutti (${allDeals.size})", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BentoPurpleHeader,
                                        selectedLabelColor = BentoPurpleOnContainer
                                    )
                                )
                                FilterChip(
                                    selected = selectedFilterCategory == "ACTIVE_ONLY",
                                    onClick = { selectedFilterCategory = "ACTIVE_ONLY" },
                                    label = { Text("🔔 Attivi ($activeAlertsCount)", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f),
                                        selectedLabelColor = EmeraldGreen
                                    )
                                )
                                FilterChip(
                                    selected = selectedFilterCategory == "AUCTION",
                                    onClick = { selectedFilterCategory = "AUCTION" },
                                    label = { Text("⚖️ Aste", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberGold.copy(alpha = 0.2f),
                                        selectedLabelColor = AmberGold
                                    )
                                )
                            }
                        }

                        if (displayedDeals.isEmpty()) {
                            item {
                                Surface(
                                    color = SurfaceCardDark,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, SurfaceCardBorder),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(36.dp))
                                        Text("Nessun immobile trovato", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Modifica i filtri o la ricerca per visualizzare gli immobili", color = TextSecondaryDark, fontSize = 12.sp, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(displayedDeals, key = { it.id }) { deal ->
                                val alertConfig = propertyAlertsMap[deal.id] ?: GranularPropertyAlert(
                                    dealId = deal.id,
                                    dealTitle = deal.title,
                                    dealLocation = deal.location,
                                    currentAskingPrice = deal.askingPrice,
                                    originalAskingPrice = deal.askingPrice,
                                    estimatedMarketValue = deal.estimatedMarketValue,
                                    propertyType = deal.propertyType,
                                    imageUrl = deal.imageUrl,
                                    isAlertEnabled = true,
                                    dropPercentThreshold = globalSettings.defaultPriceDropPercent,
                                    targetPriceThreshold = deal.askingPrice * (1.0 - (globalSettings.defaultPriceDropPercent / 100.0))
                                )

                                GranularPropertyAlertCard(
                                    deal = deal,
                                    alertConfig = alertConfig,
                                    onSaveAlert = { updatedAlert ->
                                        viewModel.saveGranularPropertyAlert(updatedAlert)
                                        Toast.makeText(context, "Soglia salvata per '${deal.title.take(20)}...'", Toast.LENGTH_SHORT).show()
                                    },
                                    onSendTestAlert = { alert ->
                                        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.sendTestNotificationForProperty(alert)
                                            Toast.makeText(context, "Notifica di test inviata con successo!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onSimulatePriceDrop = {
                                        showSimulateDropDialogForDeal = deal
                                        simulatedNewPriceStr = (deal.askingPrice * 0.85).toInt().toString()
                                    },
                                    onOpenDealDetail = { onOpenDealDetail?.invoke(deal) }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }

                1 -> {
                    // ==========================================
                    // TAB 1: ALERT HISTORY LOG
                    // ==========================================
                    GranularAlertHistoryTab(
                        alertHistory = alertHistory,
                        onClearHistory = { viewModel.clearGranularAlertHistory() },
                        onMarkAsRead = { viewModel.markGranularAlertAsRead(it) },
                        onOpenDeal = { dealId ->
                            val deal = allDeals.find { it.id == dealId }
                            if (deal != null) {
                                onOpenDealDetail?.invoke(deal)
                            }
                        }
                    )
                }

                2 -> {
                    // ==========================================
                    // TAB 2: GLOBAL PREFERENCES
                    // ==========================================
                    GlobalNotificationPreferencesTab(
                        globalSettings = globalSettings,
                        onSaveSettings = { updated ->
                            viewModel.saveGlobalNotificationSettings(updated)
                            Toast.makeText(context, "Preferenze globali salvate!", Toast.LENGTH_SHORT).show()
                        },
                        onApplyPresetToAll = { percent ->
                            viewModel.applyAlertPresetToAll(percent)
                            Toast.makeText(context, "Soglia -$percent% applicata a tutti i ${allDeals.size} immobili!", Toast.LENGTH_SHORT).show()
                        },
                        onToggleAllAlerts = { enable ->
                            viewModel.toggleAllAlerts(enable)
                            Toast.makeText(context, if (enable) "Tutti gli allarmi attivati" else "Tutti gli allarmi disattivati", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Dialog for Quick Preset Strategies
    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = AmberGold)
                    Text("Strategie di Allarme Preset", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Seleziona una strategia da applicare rapidamente a tutti gli immobili in monitoraggio:",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )

                    PresetOptionCard(
                        title = "🦅 Aggressive Bargain Hunter (-20%)",
                        subtitle = "Notifica solo sui crolli di prezzo significativi e aste deserte al secondo tentativo",
                        accentColor = RoseRed,
                        onClick = {
                            viewModel.applyAlertPresetToAll(20.0)
                            showPresetDialog = false
                            Toast.makeText(context, "Strategia Aggressive (-20%) applicata a tutti!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    PresetOptionCard(
                        title = "⚖️ Investitore Bilanciato (-10%)",
                        subtitle = "Soglia standard per cogliere ribassi periodici e trattative in discesa",
                        accentColor = CyanAccent,
                        onClick = {
                            viewModel.applyAlertPresetToAll(10.0)
                            showPresetDialog = false
                            Toast.makeText(context, "Strategia Bilanciata (-10%) applicata a tutti!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    PresetOptionCard(
                        title = "⚡ Fast Scalper (-5%)",
                        subtitle = "Notifica tempestiva al primo accenno di sconto o ribasso listino",
                        accentColor = EmeraldGreen,
                        onClick = {
                            viewModel.applyAlertPresetToAll(5.0)
                            showPresetDialog = false
                            Toast.makeText(context, "Strategia Fast Scalper (-5%) applicata a tutti!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    PresetOptionCard(
                        title = "🏛️ Solo Aste Ribasso Legale (-25%)",
                        subtitle = "Imposta allarme -25% solo per le aste giudiziarie ed esecutive",
                        accentColor = AmberGold,
                        onClick = {
                            viewModel.applyAlertPresetToAll(25.0, isAuctionOnly = true)
                            showPresetDialog = false
                            Toast.makeText(context, "Soglia -25% impostata su tutte le Aste!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text("Chiudi", color = BentoPurpleOnContainer)
                }
            },
            containerColor = SurfaceCardDark
        )
    }

    // Dialog for Simulating a Price Drop on a specific deal
    showSimulateDropDialogForDeal?.let { deal ->
        AlertDialog(
            onDismissRequest = { showSimulateDropDialogForDeal = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = RoseRed)
                    Text("Simula Ribasso di Prezzo", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Inserisci un nuovo prezzo per '${deal.title.take(24)}...' per testare l'innesco reale della notifica push e del tracciamento:",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )

                    Text(
                        "Prezzo attuale: ${currencyFormat.format(deal.askingPrice)}",
                        color = AmberGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = simulatedNewPriceStr,
                        onValueChange = { simulatedNewPriceStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Nuovo Prezzo Ribassato (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoseRed,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("simulate_price_input")
                    )

                    val newPrice = simulatedNewPriceStr.toDoubleOrNull() ?: 0.0
                    if (newPrice > 0 && newPrice < deal.askingPrice) {
                        val drop = deal.askingPrice - newPrice
                        val dropPct = (drop / deal.askingPrice) * 100.0
                        Text(
                            "📉 Ribasso calcolato: -${currencyFormat.format(drop)} (-${String.format(Locale.ITALY, "%.1f%%", dropPct)})",
                            color = EmeraldGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPrice = simulatedNewPriceStr.toDoubleOrNull()
                        if (newPrice != null && newPrice > 0) {
                            viewModel.simulatePriceDropForDeal(deal.id, newPrice)
                            showSimulateDropDialogForDeal = null
                            Toast.makeText(context, "Ribasso registrato! Notifica inviata se sotto soglia.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                    modifier = Modifier.testTag("confirm_simulate_drop_btn")
                ) {
                    Text("Applica Ribasso & Invia", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSimulateDropDialogForDeal = null }) {
                    Text("Annulla", color = TextSecondaryDark)
                }
            },
            containerColor = SurfaceCardDark
        )
    }
}

/**
 * Granular Card for an individual property allowing deep customization of price thresholds.
 */
@Composable
fun GranularPropertyAlertCard(
    deal: PropertyDeal,
    alertConfig: GranularPropertyAlert,
    onSaveAlert: (GranularPropertyAlert) -> Unit,
    onSendTestAlert: (GranularPropertyAlert) -> Unit,
    onSimulatePriceDrop: () -> Unit,
    onOpenDealDetail: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 } }
    var isExpanded by remember { mutableStateOf(false) }

    // Local edit state
    var isAlertEnabled by remember(alertConfig.isAlertEnabled) { mutableStateOf(alertConfig.isAlertEnabled) }
    var triggerMode by remember(alertConfig.triggerMode) { mutableStateOf(alertConfig.triggerMode) }
    var dropPercent by remember(alertConfig.dropPercentThreshold) { mutableStateOf(alertConfig.dropPercentThreshold.toFloat()) }
    var targetPriceStr by remember(alertConfig.targetPriceThreshold) {
        mutableStateOf(alertConfig.targetPriceThreshold.toInt().toString())
    }
    var minCapRate by remember(alertConfig.minCapRateThreshold) { mutableStateOf(alertConfig.minCapRateThreshold.toFloat()) }
    var targetDiscountOmi by remember(alertConfig.targetDiscountOmiThreshold) { mutableStateOf(alertConfig.targetDiscountOmiThreshold.toFloat()) }
    var isHighPriority by remember(alertConfig.isHighPriority) { mutableStateOf(alertConfig.isHighPriority) }
    var notifyAuctionDeserta by remember(alertConfig.notifyOnAuctionDeserta) { mutableStateOf(alertConfig.notifyOnAuctionDeserta) }

    // Real-time computed effective trigger price based on local edits
    val computedEffectivePrice = remember(triggerMode, dropPercent, targetPriceStr, minCapRate, targetDiscountOmi, deal.askingPrice, deal.estimatedMarketValue) {
        when (triggerMode) {
            AlertTriggerMode.PERCENTAGE_DROP -> deal.askingPrice * (1.0 - (dropPercent / 100.0))
            AlertTriggerMode.TARGET_PRICE -> targetPriceStr.toDoubleOrNull() ?: (deal.askingPrice * 0.9)
            AlertTriggerMode.TARGET_DISCOUNT_OMI -> {
                if (deal.estimatedMarketValue > 0) {
                    deal.estimatedMarketValue * (1.0 - (targetDiscountOmi / 100.0))
                } else {
                    deal.askingPrice * 0.85
                }
            }
            AlertTriggerMode.MIN_CAP_RATE -> {
                val estRent = (deal.surfaceSqm * 13.5 * 12.0).coerceAtLeast(7000.0)
                if (minCapRate > 0) (estRent / (minCapRate / 100.0)) else (deal.askingPrice * 0.9)
            }
        }
    }

    val computedSavings = (deal.askingPrice - computedEffectivePrice).coerceAtLeast(0.0)
    val computedEffectiveDropPct = if (deal.askingPrice > 0) ((computedSavings / deal.askingPrice) * 100.0) else 0.0

    Surface(
        color = SurfaceCardDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isAlertEnabled) {
                if (isExpanded) BentoPurpleOnContainer else SurfaceCardBorder
            } else {
                SurfaceCardBorder.copy(alpha = 0.5f)
            }
        ),
        shadowElevation = if (isExpanded) 6.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("granular_property_card_${deal.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Deal Thumbnail
                    if (deal.imageUrl.isNotBlank()) {
                        val context = LocalContext.current
                        AsyncImage(
                            model = ImageUtils.buildOptimizedImageRequest(
                                context = context,
                                data = deal.imageUrl,
                                targetWidthPx = 140,
                                targetHeightPx = 140
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenDealDetail() }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BentoPurpleHeader),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Apartment, contentDescription = null, tint = BentoPurpleOnContainer, modifier = Modifier.size(24.dp))
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = deal.title,
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${deal.location} • ${deal.propertyType}",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currencyFormat.format(deal.askingPrice),
                                color = AmberGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (deal.discountPercent > 0) {
                                Surface(
                                    color = RoseRed.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "-${deal.discountPercent}%",
                                        color = RoseRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Master Toggle Switch for this property
                Switch(
                    checked = isAlertEnabled,
                    onCheckedChange = {
                        isAlertEnabled = it
                        val updated = alertConfig.copy(
                            isAlertEnabled = it,
                            triggerMode = triggerMode,
                            dropPercentThreshold = dropPercent.toDouble(),
                            targetPriceThreshold = targetPriceStr.toDoubleOrNull() ?: alertConfig.targetPriceThreshold,
                            minCapRateThreshold = minCapRate.toDouble(),
                            targetDiscountOmiThreshold = targetDiscountOmi.toInt(),
                            isHighPriority = isHighPriority,
                            notifyOnAuctionDeserta = notifyAuctionDeserta
                        )
                        onSaveAlert(updated)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldGreen,
                        uncheckedTrackColor = DarkSlateBg
                    ),
                    modifier = Modifier.testTag("property_alert_switch_${deal.id}")
                )
            }

            // Summary Indicator Pill
            Surface(
                color = if (isAlertEnabled) DarkSlateBg else SurfaceCardDark,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isAlertEnabled) BentoPurpleOnContainer.copy(alpha = 0.3f) else SurfaceCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = triggerMode.icon, fontSize = 14.sp)
                        Column {
                            Text(
                                text = if (isAlertEnabled) {
                                    "Soglia attiva: ≤ ${currencyFormat.format(computedEffectivePrice)} (-${String.format(Locale.ITALY, "%.1f%%", computedEffectiveDropPct)})"
                                } else {
                                    "Allarme disattivato per questo immobile"
                                },
                                color = if (isAlertEnabled) TextPrimaryDark else TextMutedDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAlertEnabled) "Modalità: ${triggerMode.titleIt}" else "Tocca per configurare e riattivare",
                                color = TextSecondaryDark,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = BentoPurpleOnContainer
                    )
                }
            }

            // Expandable Deep Configuration Form
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider(color = SurfaceCardBorder, thickness = 1.dp)

                    Text(
                        text = "1. Seleziona Modalità di Innesco Allarme",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Trigger Mode Selector Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AlertTriggerMode.values().forEach { mode ->
                            val isSelected = (triggerMode == mode)
                            Surface(
                                color = if (isSelected) BentoPurpleHeader else DarkSlateBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { triggerMode = mode }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = mode.icon, fontSize = 14.sp)
                                    Text(
                                        text = when (mode) {
                                            AlertTriggerMode.PERCENTAGE_DROP -> "Ribasso %"
                                            AlertTriggerMode.TARGET_PRICE -> "Prezzo €"
                                            AlertTriggerMode.MIN_CAP_RATE -> "Yield %"
                                            AlertTriggerMode.TARGET_DISCOUNT_OMI -> "Sconto OMI"
                                        },
                                        color = if (isSelected) BentoPurpleOnContainer else TextSecondaryDark,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Mode-Specific Sliders / Controls
                    when (triggerMode) {
                        AlertTriggerMode.PERCENTAGE_DROP -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Percentuale di Ribasso Richiesta:", color = TextSecondaryDark, fontSize = 11.sp)
                                    Text("-${dropPercent.toInt()}%", color = RoseRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                Slider(
                                    value = dropPercent,
                                    onValueChange = { dropPercent = it },
                                    valueRange = 1f..50f,
                                    steps = 49,
                                    colors = SliderDefaults.colors(
                                        thumbColor = RoseRed,
                                        activeTrackColor = RoseRed,
                                        inactiveTrackColor = DarkSlateBg
                                    )
                                )

                                // Preset pills
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(5f, 10f, 15f, 20f, 25f, 30f).forEach { p ->
                                        Surface(
                                            color = if (dropPercent.toInt() == p.toInt()) RoseRed.copy(alpha = 0.25f) else DarkSlateBg,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, if (dropPercent.toInt() == p.toInt()) RoseRed else SurfaceCardBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { dropPercent = p }
                                        ) {
                                            Text(
                                                text = "-${p.toInt()}%",
                                                color = if (dropPercent.toInt() == p.toInt()) RoseRed else TextSecondaryDark,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AlertTriggerMode.TARGET_PRICE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Inserisci Prezzo Bersaglio Esatto (€):", color = TextSecondaryDark, fontSize = 11.sp)
                                OutlinedTextField(
                                    value = targetPriceStr,
                                    onValueChange = { targetPriceStr = it.filter { c -> c.isDigit() } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = { Text("€", color = AmberGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberGold,
                                        unfocusedBorderColor = SurfaceCardBorder,
                                        focusedContainerColor = DarkSlateBg,
                                        unfocusedContainerColor = DarkSlateBg,
                                        focusedTextColor = TextPrimaryDark,
                                        unfocusedTextColor = TextPrimaryDark
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Quick step buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(-5000, -10000, -25000, -50000).forEach { delta ->
                                        Surface(
                                            color = DarkSlateBg,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, SurfaceCardBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    val current = targetPriceStr.toDoubleOrNull() ?: deal.askingPrice
                                                    val newT = (current + delta).coerceAtLeast(10000.0)
                                                    targetPriceStr = newT.toInt().toString()
                                                }
                                        ) {
                                            Text(
                                                text = if (delta > 0) "+${delta / 1000}k" else "${delta / 1000}k",
                                                color = AmberGold,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AlertTriggerMode.MIN_CAP_RATE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Rendimento Lordo Minimo Desiderato:", color = TextSecondaryDark, fontSize = 11.sp)
                                    Text("${String.format(Locale.ITALY, "%.1f%%", minCapRate)}", color = EmeraldGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                Slider(
                                    value = minCapRate,
                                    onValueChange = { minCapRate = it },
                                    valueRange = 4.0f..15.0f,
                                    steps = 22,
                                    colors = SliderDefaults.colors(
                                        thumbColor = EmeraldGreen,
                                        activeTrackColor = EmeraldGreen,
                                        inactiveTrackColor = DarkSlateBg
                                    )
                                )
                            }
                        }

                        AlertTriggerMode.TARGET_DISCOUNT_OMI -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Sconto Minimo rispetto al Valore OMI:", color = TextSecondaryDark, fontSize = 11.sp)
                                    Text("${targetDiscountOmi.toInt()}%", color = CyanAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                Slider(
                                    value = targetDiscountOmi,
                                    onValueChange = { targetDiscountOmi = it },
                                    valueRange = 10f..60f,
                                    steps = 25,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CyanAccent,
                                        activeTrackColor = CyanAccent,
                                        inactiveTrackColor = DarkSlateBg
                                    )
                                )
                            }
                        }
                    }

                    // Live Simulation Box
                    Surface(
                        color = BentoPurpleHeader.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = BentoPurpleOnContainer, modifier = Modifier.size(13.dp))
                                Text("Innesco Notifica in Tempo Reale", color = BentoPurpleOnContainer, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "L'allarme scatterà se il prezzo scende da ${currencyFormat.format(deal.askingPrice)} a ≤ ${currencyFormat.format(computedEffectivePrice)}.",
                                color = TextPrimaryDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "📉 Risparmio potenziale per l'investitore: ${currencyFormat.format(computedSavings)} (-${String.format(Locale.ITALY, "%.1f%%", computedEffectiveDropPct)})",
                                color = EmeraldGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Secondary Toggles: Auction Deserta & Priority
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Allerta Asta Deserta (-25%)", color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Innesco automatico al ribasso di legge per asta andata deserta", color = TextSecondaryDark, fontSize = 9.sp)
                            }
                            Checkbox(
                                checked = notifyAuctionDeserta,
                                onCheckedChange = { notifyAuctionDeserta = it },
                                colors = CheckboxDefaults.colors(checkedColor = AmberGold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Notifica Prioritaria Heads-Up", color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Mostra banner a schermo con vibrazione istantanea", color = TextSecondaryDark, fontSize = 9.sp)
                            }
                            Checkbox(
                                checked = isHighPriority,
                                onCheckedChange = { isHighPriority = it },
                                colors = CheckboxDefaults.colors(checkedColor = RoseRed)
                            )
                        }
                    }

                    // Action Buttons: Save, Test Notification, Simulate Drop
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val updated = alertConfig.copy(
                                    isAlertEnabled = isAlertEnabled,
                                    triggerMode = triggerMode,
                                    dropPercentThreshold = dropPercent.toDouble(),
                                    targetPriceThreshold = targetPriceStr.toDoubleOrNull() ?: computedEffectivePrice,
                                    minCapRateThreshold = minCapRate.toDouble(),
                                    targetDiscountOmiThreshold = targetDiscountOmi.toInt(),
                                    isHighPriority = isHighPriority,
                                    notifyOnAuctionDeserta = notifyAuctionDeserta
                                )
                                onSaveAlert(updated)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOnContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("save_property_alert_btn_${deal.id}")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salva", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val currentConfig = alertConfig.copy(
                                    isAlertEnabled = true,
                                    triggerMode = triggerMode,
                                    dropPercentThreshold = dropPercent.toDouble(),
                                    targetPriceThreshold = targetPriceStr.toDoubleOrNull() ?: computedEffectivePrice,
                                    minCapRateThreshold = minCapRate.toDouble(),
                                    targetDiscountOmiThreshold = targetDiscountOmi.toInt(),
                                    isHighPriority = isHighPriority,
                                    notifyOnAuctionDeserta = notifyAuctionDeserta
                                )
                                onSendTestAlert(currentConfig)
                            },
                            border = BorderStroke(1.dp, CyanAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("test_property_alert_btn_${deal.id}")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Push", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onSimulatePriceDrop,
                            border = BorderStroke(1.dp, RoseRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(38.dp)
                                .testTag("simulate_drop_btn_${deal.id}")
                        ) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = RoseRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simula Calo", color = RoseRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab displaying triggered price alerts and notification log history.
 */
@Composable
fun GranularAlertHistoryTab(
    alertHistory: List<GranularAlertHistoryEvent>,
    onClearHistory: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onOpenDeal: (Long) -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 } }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Registro Notifiche & Ribassi", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${alertHistory.size} eventi di prezzo registrati", color = TextSecondaryDark, fontSize = 11.sp)
            }

            if (alertHistory.isNotEmpty()) {
                TextButton(
                    onClick = onClearHistory,
                    modifier = Modifier.testTag("clear_alert_history_btn")
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = RoseRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancella Storico", color = RoseRed, fontSize = 11.sp)
                }
            }
        }

        if (alertHistory.isEmpty()) {
            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(40.dp))
                    Text("Nessuna notifica nello storico", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Quando un immobile monitorato scende sotto la soglia configurata, troverai qui il report dettagliato del ribasso.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alertHistory, key = { it.id }) { event ->
                    Surface(
                        color = SurfaceCardDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (!event.isRead) RoseRed.copy(alpha = 0.5f) else SurfaceCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMarkAsRead(event.id)
                                onOpenDeal(event.dealId)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (!event.isRead) RoseRed else TextMutedDark)
                                    )
                                    Text(
                                        text = event.alertTypeTitle,
                                        color = RoseRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = dateFormat.format(Date(event.timestamp)),
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp
                                )
                            }

                            Text(
                                text = event.dealTitle,
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Prezzo Precedente", color = TextSecondaryDark, fontSize = 9.sp)
                                    Text(currencyFormat.format(event.oldPrice), color = TextMutedDark, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = RoseRed, modifier = Modifier.size(14.dp))
                                Column {
                                    Text("Nuovo Prezzo", color = TextSecondaryDark, fontSize = 9.sp)
                                    Text(currencyFormat.format(event.newPrice), color = EmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Column {
                                    Text("Risparmio", color = TextSecondaryDark, fontSize = 9.sp)
                                    Text("-${currencyFormat.format(event.dropAmount)}", color = RoseRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Global Preferences Tab for notification delivery, sound, and batch preset applications.
 */
@Composable
fun GlobalNotificationPreferencesTab(
    globalSettings: GlobalNotificationSettings,
    onSaveSettings: (GlobalNotificationSettings) -> Unit,
    onApplyPresetToAll: (Double) -> Unit,
    onToggleAllAlerts: (Boolean) -> Unit
) {
    var masterPush by remember(globalSettings.masterPushEnabled) { mutableStateOf(globalSettings.masterPushEnabled) }
    var soundVibration by remember(globalSettings.soundAndVibrationEnabled) { mutableStateOf(globalSettings.soundAndVibrationEnabled) }
    var headsUpHighPriority by remember(globalSettings.headsUpHighPriorityEnabled) { mutableStateOf(globalSettings.headsUpHighPriorityEnabled) }
    var autoAuctionDeserta by remember(globalSettings.autoAuctionDesertaAlerts) { mutableStateOf(globalSettings.autoAuctionDesertaAlerts) }
    var defaultDropPct by remember(globalSettings.defaultPriceDropPercent) { mutableStateOf(globalSettings.defaultPriceDropPercent.toFloat()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Impostazioni Generali Notifiche", color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Configura il comportamento globale del motore di monitoraggio", color = TextSecondaryDark, fontSize = 11.sp)
        }

        item {
            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Master Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notifiche Push di Sistema", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Attiva o disattiva globalmente tutte le notifiche del radar", color = TextSecondaryDark, fontSize = 10.sp)
                        }
                        Switch(
                            checked = masterPush,
                            onCheckedChange = {
                                masterPush = it
                                onSaveSettings(globalSettings.copy(masterPushEnabled = it))
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = EmeraldGreen)
                        )
                    }

                    Divider(color = SurfaceCardBorder, thickness = 1.dp)

                    // Sound & Vibration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Suono e Vibrazione", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Emetti suono all'arrivo di una segnalazione ribasso", color = TextSecondaryDark, fontSize = 10.sp)
                        }
                        Switch(
                            checked = soundVibration,
                            onCheckedChange = {
                                soundVibration = it
                                onSaveSettings(globalSettings.copy(soundAndVibrationEnabled = it))
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = CyanAccent)
                        )
                    }

                    Divider(color = SurfaceCardBorder, thickness = 1.dp)

                    // Heads-up high priority
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notifica Prioritaria a Schermo Intero", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Mostra banner a comparsa heads-up per ribassi > 15%", color = TextSecondaryDark, fontSize = 10.sp)
                        }
                        Switch(
                            checked = headsUpHighPriority,
                            onCheckedChange = {
                                headsUpHighPriority = it
                                onSaveSettings(globalSettings.copy(headsUpHighPriorityEnabled = it))
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = RoseRed)
                        )
                    }

                    Divider(color = SurfaceCardBorder, thickness = 1.dp)

                    // Auto Auction Deserta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allarme Automatico Aste Deserte", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Calcola automaticamente il ribasso legale del -25% al cambio asta", color = TextSecondaryDark, fontSize = 10.sp)
                        }
                        Switch(
                            checked = autoAuctionDeserta,
                            onCheckedChange = {
                                autoAuctionDeserta = it
                                onSaveSettings(globalSettings.copy(autoAuctionDesertaAlerts = it))
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = AmberGold)
                        )
                    }
                }
            }
        }

        item {
            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Soglia di Ribasso Predefinita per Nuovi Immobili", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Quando un nuovo immobile viene tracciato, verrà applicata questa soglia:", color = TextSecondaryDark, fontSize = 11.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ribasso Minimo:", color = TextSecondaryDark, fontSize = 11.sp)
                        Text("-${defaultDropPct.toInt()}%", color = RoseRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = defaultDropPct,
                        onValueChange = { defaultDropPct = it },
                        valueRange = 3f..35f,
                        steps = 32,
                        colors = SliderDefaults.colors(
                            thumbColor = RoseRed,
                            activeTrackColor = RoseRed,
                            inactiveTrackColor = DarkSlateBg
                        )
                    )

                    Button(
                        onClick = {
                            onSaveSettings(globalSettings.copy(defaultPriceDropPercent = defaultDropPct.toDouble()))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOnContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salva Soglia Predefinita", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Azioni di Massa su Tutti gli Immobili", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onToggleAllAlerts(true) },
                            border = BorderStroke(1.dp, EmeraldGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Attiva Tutti", color = EmeraldGreen, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { onToggleAllAlerts(false) },
                            border = BorderStroke(1.dp, RoseRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Disattiva Tutti", color = RoseRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GranularKpiPill(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCardDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(13.dp))
                Text(title, color = TextSecondaryDark, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Text(value, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(subtitle, color = TextMutedDark, fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PresetOptionCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = DarkSlateBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondaryDark, fontSize = 10.sp)
        }
    }
}
