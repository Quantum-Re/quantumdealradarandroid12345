package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.service.SupplyDemandMonitoringEngine
import com.example.service.SupplyDemandWorkManagerScheduler
import com.example.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplyDemandMonitorScreen(
    onBackClick: () -> Unit,
    onNavigateToProperty: (Long) -> Unit = {},
    onNavigateToDeal: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val alerts by SupplyDemandMonitoringEngine.alertsFlow.collectAsStateWithLifecycle()
    val isScanning by SupplyDemandMonitoringEngine.isScanningFlow.collectAsStateWithLifecycle()
    val settings by SupplyDemandMonitoringEngine.settingsFlow.collectAsStateWithLifecycle()
    val monitoredZones by SupplyDemandMonitoringEngine.monitoredZonesFlow.collectAsStateWithLifecycle()

    var isSettingsDialogOpen by remember { mutableStateOf(false) }
    var selectedFilterType by remember { mutableStateOf<SupplyDemandShiftType?>(null) }
    var simulationMessage by remember { mutableStateOf<String?>(null) }

    val filteredAlerts = remember(alerts, selectedFilterType) {
        if (selectedFilterType == null) alerts else alerts.filter { it.shiftType == selectedFilterType }
    }

    val primaryDarkBg = Color(0xFF0F172A)
    val cardDarkBg = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)
    val accentGold = Color(0xFFF59E0B)
    val accentGreen = Color(0xFF10B981)
    val accentRed = Color(0xFFEF4444)

    Scaffold(
        topBar = {
            Surface(
                color = primaryDarkBg,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                                    .testTag("supply_demand_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Indietro",
                                    tint = Color.White
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "PROACTIVE SUPPLY MONITOR",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = accentCyan,
                                        letterSpacing = 0.5.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (settings.isEnabled) accentGreen.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, if (settings.isEnabled) accentGreen else Color.Gray)
                                    ) {
                                        Text(
                                            text = if (settings.isEnabled) "ACTIVE DAEMON" else "PAUSED",
                                            color = if (settings.isEnabled) accentGreen else Color.LightGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "IMMOBILIARE.IT SCRAPER • SHIFT & TENSION RADAR",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        IconButton(
                            onClick = { isSettingsDialogOpen = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                                .testTag("supply_demand_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Impostazioni Sensibilità",
                                tint = accentCyan
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0B0F19)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("supply_demand_monitor_content"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Proactive Status Daemon & Live Scan Bar
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = cardDarkBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
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
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (settings.isEnabled) accentGreen else Color.Gray, CircleShape)
                                )
                                Text(
                                    text = "MONITORING ENGINE BACKGROUND DAEMON",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = "Ogni ${settings.checkIntervalMinutes} min",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = accentCyan
                            )
                        }

                        val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ITALY)
                        val lastScanStr = if (settings.lastScanTimestamp > 0) {
                            timeFormat.format(Date(settings.lastScanTimestamp))
                        } else "Nessuna scansione recente"

                        Text(
                            text = "Monitora costantemente le micro-zone dei tuoi immobili tramite scraping proattivo di Immobiliare.it. In caso di crollo dei giorni sul mercato (DOM) o picchi improvvisi di saturazione annunci, invia un alert push immediato con la contromisura strategica.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ultima Scansione: $lastScanStr",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8)
                            )

                            Text(
                                text = "Soglia: ±${settings.sensitivityThresholdPercent.toInt()}%",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = accentGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        simulationMessage = null
                                        val res = SupplyDemandMonitoringEngine.performProactiveMonitoringScan(context)
                                        simulationMessage = "✓ Scansione completata: ${res.size} shift rilevati!"
                                    }
                                },
                                enabled = !isScanning,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("trigger_live_scraper_scan_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "SCRAPING ZONE...", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                } else {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "SCANSIONE IMMEDIATA", fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        if (simulationMessage != null) {
                            Text(
                                text = simulationMessage!!,
                                fontSize = 10.sp,
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // 2. Instant Shift Simulator (Test Alert Delivery)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF131D31),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF25334D))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = accentGold, modifier = Modifier.size(16.dp))
                            Text(
                                text = "SIMULA SHOCK DI ZONA (TEST NOTIFICHE PUSH):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = accentGold
                            )
                        }

                        Text(
                            text = "Premi un pulsante per simulare una variazione improvvisa e verificare la generazione dell'alert push e la relativa analisi:",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        SupplyDemandMonitoringEngine.simulateImmediateAreaShift(
                                            context,
                                            "Paderno Dugnano",
                                            SupplyDemandShiftType.SUPPLY_SQUEEZE
                                        )
                                        simulationMessage = "🚀 Alert Supply Squeeze inviato a Paderno Dugnano!"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("simulate_squeeze_button"),
                                border = BorderStroke(1.dp, accentGreen),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentGreen),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(text = "🔥 Squeeze", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        SupplyDemandMonitoringEngine.simulateImmediateAreaShift(
                                            context,
                                            "Milano",
                                            SupplyDemandShiftType.SUPPLY_GLUT
                                        )
                                        simulationMessage = "⚠️ Alert Supply Glut inviato a Milano!"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("simulate_glut_button"),
                                border = BorderStroke(1.dp, accentRed),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentRed),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(text = "⚠️ Glut", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        SupplyDemandMonitoringEngine.simulateImmediateAreaShift(
                                            context,
                                            "Bologna",
                                            SupplyDemandShiftType.RENTAL_YIELD_DIVERGENCE
                                        )
                                        simulationMessage = "📈 Alert Boom Canoni inviato a Bologna!"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("simulate_rental_button"),
                                border = BorderStroke(1.dp, accentCyan),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentCyan),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(text = "📈 Affitti", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Monitored Micro-Zones & Tension Index Radar
            if (monitoredZones.isNotEmpty()) {
                item {
                    Text(
                        text = "MICRO-ZONE SOTTO MONITORAGGIO ATTIVO [${monitoredZones.size}]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = accentCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(monitoredZones) { zone ->
                            ZoneTensionCard(
                                zone = zone,
                                cardBg = cardDarkBg,
                                cyan = accentCyan,
                                green = accentGreen,
                                gold = accentGold,
                                red = accentRed
                            )
                        }
                    }
                }
            }

            // 4. Alerts Filter Bar & Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = accentGold, modifier = Modifier.size(18.dp))
                        Text(
                            text = "ALERT STREAM PROATTIVO [${filteredAlerts.size}]",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }

                    if (alerts.isNotEmpty()) {
                        TextButton(
                            onClick = { SupplyDemandMonitoringEngine.clearAlertHistory(context) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(text = "Svuota Storico", fontSize = 9.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilterType == null,
                        onClick = { selectedFilterType = null },
                        label = { Text("TUTTI (${alerts.size})", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentCyan,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedFilterType == SupplyDemandShiftType.SUPPLY_SQUEEZE,
                        onClick = { selectedFilterType = if (selectedFilterType == SupplyDemandShiftType.SUPPLY_SQUEEZE) null else SupplyDemandShiftType.SUPPLY_SQUEEZE },
                        label = { Text("🔥 SQUEEZE", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentGreen,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedFilterType == SupplyDemandShiftType.SUPPLY_GLUT,
                        onClick = { selectedFilterType = if (selectedFilterType == SupplyDemandShiftType.SUPPLY_GLUT) null else SupplyDemandShiftType.SUPPLY_GLUT },
                        label = { Text("⚠️ GLUT", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentRed,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedFilterType == SupplyDemandShiftType.RENTAL_YIELD_DIVERGENCE,
                        onClick = { selectedFilterType = if (selectedFilterType == SupplyDemandShiftType.RENTAL_YIELD_DIVERGENCE) null else SupplyDemandShiftType.RENTAL_YIELD_DIVERGENCE },
                        label = { Text("📈 CANONI", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentGold,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // 5. Alert Items List
            if (filteredAlerts.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF131D31),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF25334D))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = accentGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Nessun shift anomalo rilevato al momento",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Le micro-zone monitorate rientrano nei parametri di stabilità ordinaria. Il demone continuerà a scansionare periodicamente i dati di Immobiliare.it.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredAlerts, key = { it.id }) { alert ->
                    AlertItemCard(
                        alert = alert,
                        onMarkRead = { SupplyDemandMonitoringEngine.markAlertAsRead(context, alert.id) },
                        onNavigateToProperty = onNavigateToProperty,
                        onNavigateToDeal = onNavigateToDeal,
                        cardBg = cardDarkBg,
                        cyan = accentCyan,
                        green = accentGreen,
                        gold = accentGold,
                        red = accentRed
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Settings / Sensitivity Dialog
    if (isSettingsDialogOpen) {
        AlertDialog(
            onDismissRequest = { isSettingsDialogOpen = false },
            title = {
                Text(
                    text = "⚙️ Configurazione Proactive Monitoring",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = accentCyan
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Daemon Monitor Attivo:", fontSize = 12.sp, color = Color.White)
                        Switch(
                            checked = settings.isEnabled,
                            onCheckedChange = {
                                val updated = settings.copy(isEnabled = it)
                                SupplyDemandMonitoringEngine.saveSettings(context, updated)
                                if (it) {
                                    SupplyDemandWorkManagerScheduler.schedulePeriodicCheck(context, updated.checkIntervalMinutes)
                                } else {
                                    SupplyDemandWorkManagerScheduler.cancelMonitoring(context)
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Notifiche Push Immediati:", fontSize = 12.sp, color = Color.White)
                        Switch(
                            checked = settings.pushNotificationsEnabled,
                            onCheckedChange = {
                                val updated = settings.copy(pushNotificationsEnabled = it)
                                SupplyDemandMonitoringEngine.saveSettings(context, updated)
                            }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Soglia Minima Trigger Shift:", fontSize = 11.sp, color = Color.LightGray)
                            Text(text = "±${settings.sensitivityThresholdPercent.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentGold, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = settings.sensitivityThresholdPercent.toFloat(),
                            onValueChange = {
                                val updated = settings.copy(sensitivityThresholdPercent = it.toDouble())
                                SupplyDemandMonitoringEngine.saveSettings(context, updated)
                            },
                            valueRange = 5f..30f,
                            steps = 4,
                            colors = SliderDefaults.colors(thumbColor = accentCyan, activeTrackColor = accentCyan)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Frequenza Scansione Scraper:", fontSize = 11.sp, color = Color.LightGray)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(15L, 30L, 60L).forEach { mins ->
                                val isSelected = settings.checkIntervalMinutes == mins
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val updated = settings.copy(checkIntervalMinutes = mins)
                                            SupplyDemandMonitoringEngine.saveSettings(context, updated)
                                            SupplyDemandWorkManagerScheduler.schedulePeriodicCheck(context, mins)
                                        },
                                    color = if (isSelected) accentCyan else Color(0xFF334155),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isSettingsDialogOpen = false }) {
                    Text(text = "CHIUDI", color = accentCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun ZoneTensionCard(
    zone: SupplyDemandSnapshot,
    cardBg: Color,
    cyan: Color,
    green: Color,
    gold: Color,
    red: Color
) {
    val sdr = zone.supplyDemandRatioIndex
    val tensionColor = when {
        sdr >= 70.0 -> green
        sdr >= 50.0 -> cyan
        sdr >= 35.0 -> gold
        else -> red
    }

    Surface(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = zone.location.take(16),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = tensionColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, tensionColor)
                ) {
                    Text(
                        text = "SDR ${sdr.toInt()}/100",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = tensionColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Tension meter bar
            LinearProgressIndicator(
                progress = { (sdr / 100.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = tensionColor,
                trackColor = Color(0xFF0F172A)
            )

            Text(
                text = zone.tensionLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = tensionColor,
                maxLines = 1
            )

            HorizontalDivider(color = Color(0xFF334155))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "DOM", fontSize = 8.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                    Text(text = "${zone.daysOnMarket} gg", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column {
                    Text(text = "SATURAZIONE", fontSize = 8.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                    Text(text = "${zone.marketSaturation}/100", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column {
                    Text(text = "ASSORBIMENTO", fontSize = 8.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                    Text(text = "${zone.absorptionRatePercent.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cyan)
                }
            }
        }
    }
}

@Composable
private fun AlertItemCard(
    alert: SupplyDemandAlertRecord,
    onMarkRead: () -> Unit,
    onNavigateToProperty: (Long) -> Unit,
    onNavigateToDeal: (Long) -> Unit,
    cardBg: Color,
    cyan: Color,
    green: Color,
    gold: Color,
    red: Color
) {
    val context = LocalContext.current
    val accentColor = when (alert.shiftType) {
        SupplyDemandShiftType.SUPPLY_SQUEEZE -> green
        SupplyDemandShiftType.SUPPLY_GLUT -> red
        SupplyDemandShiftType.RENTAL_YIELD_DIVERGENCE -> gold
        SupplyDemandShiftType.MICRO_ZONE_HEATWAVE -> cyan
    }

    val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
    val formattedDate = timeFormat.format(Date(alert.timestamp))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alert_card_${alert.id}"),
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (!alert.isRead) accentColor.copy(alpha = 0.8f) else Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Badge header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, accentColor)
                    ) {
                        Text(
                            text = alert.shiftType.shortLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    if (alert.severity == ShiftSeverity.CRITICAL) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = red.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, red)
                        ) {
                            Text(
                                text = "CRITICO",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = red,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = formattedDate,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
            }

            // Headline & Description
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = alert.headline,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = alert.description,
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 16.sp
                )
            }

            // Metrics Comparison Strip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "RAPPORTO D/O", fontSize = 8.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", alert.previousRatio)} ➔ ${String.format(Locale.US, "%.1f", alert.currentRatio)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text(text = "GIORNI SUL MERCATO", fontSize = 8.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${alert.previousDom}gg ➔ ${alert.currentDom}gg",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text(text = "SATURAZIONE", fontSize = 8.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${alert.previousSaturation} ➔ ${alert.currentSaturation}/100",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Strategic Recommendation Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = accentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "AZIONE STRATEGICA CONSIGLIATA:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = accentColor
                        )
                        Text(
                            text = alert.strategicRecommendation,
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Affected Properties
            if (alert.affectedPropertyTitles.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🏢 Tuoi Immobili in quest'area:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    alert.affectedPropertyTitles.forEachIndexed { idx, title ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val propId = alert.affectedPropertyIds.getOrNull(idx)
                                    if (propId != null) onNavigateToProperty(propId)
                                },
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = title, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = cyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(alert.sourceUrl))
                        context.startActivity(intent)
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp), tint = cyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Fonte Immobiliare.it", fontSize = 10.sp, color = cyan)
                }

                if (!alert.isRead) {
                    OutlinedButton(
                        onClick = onMarkRead,
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Segna come Letto", fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}
