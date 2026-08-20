package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.*
import com.example.ui.PropertyViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioAlertsSheet(
    viewModel: PropertyViewModel,
    uiState: com.example.ui.PropertyUiState,
    onDismiss: () -> Unit,
    onSelectProperty: (Long) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedFilterType by remember { mutableStateOf<PropertyAlertType?>(null) }

    var hasPostNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPostNotificationPermission = isGranted
    }

    val prefs = uiState.alertPreferences
    val alerts = uiState.alertHistory

    val filteredAlerts = remember(alerts, selectedFilterType) {
        if (selectedFilterType == null) alerts
        else alerts.filter { it.alertType == selectedFilterType }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCardDark,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = SurfaceCardBorder
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("portfolio_alerts_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(BentoPurpleHeader),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = BentoPurpleOnContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Allarmi & Notifiche Portafoglio",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Avvisi automatici per ribassi di prezzo e cambi stato",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi",
                        tint = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notification permission banner if missing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberWarningContainer.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AmberWarningBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = AmberWarningText,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permesso Notifiche Richiesto",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarningText
                            )
                            Text(
                                text = "Consenti le notifiche per ricevere allarmi istantanei sui ribassi di prezzo.",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarningText)
                        ) {
                            Text("Consenti", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }

            // Tab Navigation
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = { HorizontalDivider(color = SurfaceCardBorder) },
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTab),
                        color = BentoPurpleOnContainer
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Feed Allarmi", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (uiState.unreadAlertCount > 0) {
                                Badge(containerColor = RoseRed, contentColor = Color.White) {
                                    Text("${uiState.unreadAlertCount}", fontSize = 10.sp)
                                }
                            }
                        }
                    },
                    selectedContentColor = BentoPurpleOnContainer,
                    unselectedContentColor = TextSecondaryDark
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Configurazione", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    selectedContentColor = BentoPurpleOnContainer,
                    unselectedContentColor = TextSecondaryDark
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: ALERTS FEED & HISTORY
                    AlertsFeedContent(
                        alerts = filteredAlerts,
                        selectedFilter = selectedFilterType,
                        onSelectFilter = { selectedFilterType = it },
                        onMarkAllAsRead = {
                            alerts.forEach { viewModel.markAlertAsRead(it.id) }
                        },
                        onClearAll = { viewModel.clearAllAlerts() },
                        onSelectProperty = { propId ->
                            onSelectProperty(propId)
                            onDismiss()
                        },
                        onSendTestAlert = {
                            viewModel.sendTestPriceDropAlert()
                        }
                    )
                }
                1 -> {
                    // TAB 1: PREFERENCES & LIVE TESTS
                    AlertPreferencesContent(
                        prefs = prefs,
                        onUpdatePrefs = { viewModel.updateAlertPreferences(it) },
                        onTestPriceDrop = { viewModel.sendTestPriceDropAlert() },
                        onTestStatusChange = { viewModel.sendTestStatusChangeAlert() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertsFeedContent(
    alerts: List<PropertyAlertRecord>,
    selectedFilter: PropertyAlertType?,
    onSelectFilter: (PropertyAlertType?) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearAll: () -> Unit,
    onSelectProperty: (Long) -> Unit,
    onSendTestAlert: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Filter Chips Row + Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { onSelectFilter(null) },
                    label = { Text("Tutti (${alerts.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BentoPurpleContainer,
                        selectedLabelColor = BentoPurpleOnContainer
                    )
                )
                FilterChip(
                    selected = selectedFilter == PropertyAlertType.PRICE_DROP,
                    onClick = {
                        onSelectFilter(if (selectedFilter == PropertyAlertType.PRICE_DROP) null else PropertyAlertType.PRICE_DROP)
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, modifier = Modifier.size(12.dp), tint = EmeraldGainText)
                    },
                    label = { Text("Ribassi", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGainBg,
                        selectedLabelColor = EmeraldGainText
                    )
                )
                FilterChip(
                    selected = selectedFilter == PropertyAlertType.STATUS_CHANGE,
                    onClick = {
                        onSelectFilter(if (selectedFilter == PropertyAlertType.STATUS_CHANGE) null else PropertyAlertType.STATUS_CHANGE)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(12.dp), tint = BentoPurpleOnContainer)
                    },
                    label = { Text("Stati", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BentoPurpleContainer,
                        selectedLabelColor = BentoPurpleOnContainer
                    )
                )
            }

            if (alerts.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Svuota", fontSize = 11.sp, color = RoseRed)
                }
            }
        }

        if (alerts.isEmpty()) {
            // Empty State
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BentoCardBgLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(BentoPurpleHeader),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = BentoPurpleOnContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "Nessun allarme recente",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Il sistema monitora costantemente i tuoi immobili salvati. Quando un prezzo subisce un ribasso o lo stato della pipeline avanza, l'allarme comparirà qui e riceverai una notifica push.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = onSendTestAlert,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOnContainer),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Invia Allarme di Prova", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alerts, key = { it.id }) { alert ->
                    AlertRecordItemCard(
                        alert = alert,
                        onItemClick = { onSelectProperty(alert.propertyId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertRecordItemCard(
    alert: PropertyAlertRecord,
    onItemClick: () -> Unit
) {
    val (icon, iconBg, iconTint, badgeText) = when (alert.alertType) {
        PropertyAlertType.PRICE_DROP -> Quadruple(
            Icons.AutoMirrored.Filled.TrendingDown,
            EmeraldGainBg,
            EmeraldGainText,
            "Ribasso Prezzo"
        )
        PropertyAlertType.STATUS_CHANGE -> Quadruple(
            Icons.Default.SwapHoriz,
            BentoPurpleHeader,
            BentoPurpleOnContainer,
            "Cambio Pipeline"
        )
        PropertyAlertType.DISTRESS_STATUS_CHANGE -> Quadruple(
            Icons.Default.Gavel,
            AmberWarningContainer.copy(alpha = 0.3f),
            AmberWarningText,
            "Procedura Asta"
        )
        PropertyAlertType.RENOVATION_MILESTONE -> Quadruple(
            Icons.Default.Construction,
            BentoBlueContainer,
            BentoBlueOnContainer,
            "Cantiere"
        )
    }

    val timeFormatted = remember(alert.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.ITALY)
        sdf.format(Date(alert.timestamp))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardBgLight),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (!alert.isRead) BentoPurpleOnContainer.copy(alpha = 0.4f) else SurfaceCardBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onItemClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.propertyTitle.ifBlank { alert.propertyAddress },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = TextSecondaryDark
                    )
                }

                Text(
                    text = alert.changeSummary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (alert.alertType) {
                        PropertyAlertType.PRICE_DROP -> EmeraldGainText
                        PropertyAlertType.STATUS_CHANGE -> BentoPurpleOnContainer
                        else -> TextPrimaryDark
                    }
                )

                if (alert.propertyAddress.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = alert.propertyAddress,
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Dettagli",
                tint = TextSecondaryDark,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AlertPreferencesContent(
    prefs: MyPropertiesAlertPreferences,
    onUpdatePrefs: (MyPropertiesAlertPreferences) -> Unit,
    onTestPriceDrop: () -> Unit,
    onTestStatusChange: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Master Toggle
        item {
            PreferenceToggleCard(
                title = "Notifiche Portafoglio 'I Miei Immobili'",
                subtitle = "Ricevi avvisi push prioritari sui tuoi affari salvati",
                checked = prefs.notificationsEnabled,
                onCheckedChange = { onUpdatePrefs(prefs.copy(notificationsEnabled = it)) },
                icon = Icons.Default.NotificationsActive,
                iconTint = BentoPurpleOnContainer
            )
        }

        // Price Drop Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BentoCardBgLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = EmeraldGainText,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text("Allarme Ribasso Prezzo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                Text("Avvisa quando un prezzo scende", fontSize = 11.sp, color = TextSecondaryDark)
                            }
                        }
                        Switch(
                            checked = prefs.priceDropAlertsEnabled && prefs.notificationsEnabled,
                            onCheckedChange = { onUpdatePrefs(prefs.copy(priceDropAlertsEnabled = it)) },
                            enabled = prefs.notificationsEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldGainText
                            )
                        )
                    }

                    if (prefs.priceDropAlertsEnabled && prefs.notificationsEnabled) {
                        HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.5f))
                        Text(
                            text = "Soglia Minima di Ribasso:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )

                        // Threshold Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(3.0, 5.0, 8.0, 10.0, 15.0).forEach { pct ->
                                val isSelected = prefs.priceDropThresholdPercent == pct
                                OutlinedButton(
                                    onClick = { onUpdatePrefs(prefs.copy(priceDropThresholdPercent = pct)) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) EmeraldGainBg else Color.Transparent,
                                        contentColor = if (isSelected) EmeraldGainText else TextSecondaryDark
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) EmeraldGainText else SurfaceCardBorder
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("≥ ${pct.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = "L'allarme scatterà se il prezzo scende di almeno il ${prefs.priceDropThresholdPercent.toInt()}% o di oltre €${prefs.priceDropMinAbsoluteEuros.toInt()}.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // Pipeline Status Section
        item {
            PreferenceToggleCard(
                title = "Allarme Cambio Fase Pipeline",
                subtitle = "Notifica quando un immobile passa a In Trattativa, Cantiere, In Vendita o Venduto",
                checked = prefs.statusChangeAlertsEnabled && prefs.notificationsEnabled,
                onCheckedChange = { onUpdatePrefs(prefs.copy(statusChangeAlertsEnabled = it)) },
                icon = Icons.Default.SwapHoriz,
                iconTint = BentoPurpleOnContainer,
                enabled = prefs.notificationsEnabled
            )
        }

        // Renovation Milestone Section
        item {
            PreferenceToggleCard(
                title = "Notifiche SAL Cantiere",
                subtitle = "Avviso al raggiungimento del 50%, 75% e 100% dei lavori di ristrutturazione",
                checked = prefs.renovationMilestoneAlertsEnabled && prefs.notificationsEnabled,
                onCheckedChange = { onUpdatePrefs(prefs.copy(renovationMilestoneAlertsEnabled = it)) },
                icon = Icons.Default.Construction,
                iconTint = BentoBlueOnContainer,
                enabled = prefs.notificationsEnabled
            )
        }

        // Action Testing Buttons
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BentoCardBgLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Collaudo Allarmi in Tempo Reale",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Invia notifiche simulate per verificare l'integrazione sul tuo dispositivo:",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTestPriceDrop,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EmeraldGainText.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = EmeraldGainBg.copy(alpha = 0.3f),
                                contentColor = EmeraldGainText
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Ribasso", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onTestStatusChange,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = BentoPurpleContainer.copy(alpha = 0.5f),
                                contentColor = BentoPurpleOnContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Stato", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    iconTint: Color,
    enabled: Boolean = true
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardBgLight),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Text(text = subtitle, fontSize = 11.sp, color = TextSecondaryDark, lineHeight = 14.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = iconTint
                )
            )
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
