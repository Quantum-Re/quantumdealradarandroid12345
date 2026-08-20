package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FcmPushAlert
import com.example.data.FcmPushType
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import com.example.util.FcmPushManager
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FcmPushCenterDialog(
    isVisible: Boolean,
    fcmToken: String?,
    isMasterEnabled: Boolean,
    subscribedTopics: Set<String>,
    pushHistory: List<FcmPushAlert>,
    deals: List<PropertyDeal>,
    onDismiss: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onToggleTopic: (String, Boolean) -> Unit,
    onSimulatePush: (FcmPushType, Long?) -> Unit,
    onMarkRead: (String) -> Unit,
    onClearHistory: () -> Unit,
    onSelectDeal: (Long) -> Unit
) {
    if (!isVisible) return

    val context = LocalContext.current
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ITALY) }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Panoramica & Canali, 1 = Simulatore Push, 2 = Storico Notifiche
    var selectedSimType by remember { mutableStateOf(FcmPushType.PRICE_DROP) }
    var selectedDealId by remember { mutableStateOf<Long?>(deals.firstOrNull()?.id ?: 101L) }
    var showPayloadSnippet by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkSlateBg)
                .testTag("fcm_push_center_dialog"),
            color = DarkSlateBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCardDark)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(CyanAccent, PurpleIndigo)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "FIREBASE CLOUD MESSAGING",
                                        color = TextPrimaryDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                    Surface(
                                        color = if (fcmToken != null) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (fcmToken != null) "FCM ATTIVO" else "OFFLINE",
                                            color = if (fcmToken != null) EmeraldGreen else RoseRed,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Notifiche Push in Tempo Reale per Ribassi & Procedure",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("fcm_dialog_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextPrimaryDark)
                        }
                    }

                    // Navigation Tab Row
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF0F172A),
                        contentColor = CyanAccent,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Canali & Token", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("fcm_tab_channels")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Simulatore Push", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("fcm_tab_simulator")
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Storico Push", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    if (pushHistory.isNotEmpty()) {
                                        Surface(
                                            color = CyanAccent,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "${pushHistory.size}",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            icon = { Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("fcm_tab_history")
                        )
                    }

                    // Content based on selected Tab
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> ChannelsAndTokenTab(
                                context = context,
                                fcmToken = fcmToken,
                                isMasterEnabled = isMasterEnabled,
                                subscribedTopics = subscribedTopics,
                                showPayloadSnippet = showPayloadSnippet,
                                onToggleShowPayload = { showPayloadSnippet = !showPayloadSnippet },
                                onToggleMaster = onToggleMaster,
                                onToggleTopic = onToggleTopic
                            )
                            1 -> SimulatorTab(
                                deals = deals,
                                selectedDealId = selectedDealId,
                                selectedSimType = selectedSimType,
                                onSelectDealId = { selectedDealId = it },
                                onSelectSimType = { selectedSimType = it },
                                onSendPush = {
                                    onSimulatePush(selectedSimType, selectedDealId)
                                    Toast.makeText(context, "🚀 Notifica FCM inviata con successo!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            2 -> PushHistoryTab(
                                pushHistory = pushHistory,
                                dateFormat = dateFormat,
                                currencyFormat = currencyFormat,
                                onMarkRead = onMarkRead,
                                onClearHistory = onClearHistory,
                                onSelectDeal = { dealId ->
                                    onDismiss()
                                    onSelectDeal(dealId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelsAndTokenTab(
    context: Context,
    fcmToken: String?,
    isMasterEnabled: Boolean,
    subscribedTopics: Set<String>,
    showPayloadSnippet: Boolean,
    onToggleShowPayload: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onToggleTopic: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Master Switch Card
        item {
            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isMasterEnabled) CyanAccent.copy(alpha = 0.5f) else SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isMasterEnabled) CyanAccent.copy(alpha = 0.2f) else Color(0xFF334155)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMasterEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (isMasterEnabled) CyanAccent else TextSecondaryDark
                            )
                        }
                        Column {
                            Text(
                                text = "Ricezione Notifiche Push FCM",
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isMasterEnabled) "Attivo: riceverai alert istantanei per cambi prezzo e bandi" else "Disattivato: le notifiche push sono sospese",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = isMasterEnabled,
                        onCheckedChange = onToggleMaster,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyanAccent
                        ),
                        modifier = Modifier.testTag("fcm_master_switch")
                    )
                }
            }
        }

        // FCM Device Registration Token Card
        item {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Device Registration Token",
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                val tokenToCopy = fcmToken ?: "N/A"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("FCM Token", tokenToCopy)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Token FCM copiato negli appunti!", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.testTag("fcm_copy_token_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copia", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Questo token identifica univocamente questa istanza per l'invio di push dirette dai server cloud o Firebase Admin SDK:",
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )

                    Surface(
                        color = Color(0xFF020617),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = fcmToken ?: "Acquisizione token Firebase in corso...",
                            color = if (fcmToken != null) Color(0xFF38BDF8) else TextSecondaryDark,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    // REST API Payload Guide Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleShowPayload() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Visualizza Payload JSON per Backend / cURL",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (showPayloadSnippet) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = showPayloadSnippet) {
                        Surface(
                            color = Color(0xFF020617),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = """
// Esempio FCM HTTP v1 / Legacy Payload
{
  "to": "${fcmToken?.take(15) ?: "YOUR_TOKEN"}...",
  "priority": "high",
  "data": {
    "deal_id": "101",
    "type": "PRICE_DROP",
    "property_title": "Quadrilocale Porta Romana",
    "old_price": "320000",
    "new_price": "240000",
    "discount_percent": "25.0",
    "city": "Milano"
  }
}
                                    """.trimIndent(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subscribed Topics List
        item {
            Text(
                text = "CANALI & TOPIC PUSH SOTTOSCRITTI",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        items(FcmPushManager.DEFAULT_TOPICS) { topic ->
            val isSubscribed = subscribedTopics.contains(topic.topicId)

            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSubscribed) CyanAccent.copy(alpha = 0.3f) else SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = topic.title,
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = topic.description,
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Topic: /topics/${topic.topicId}",
                                color = Color(0xFF38BDF8),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Switch(
                        checked = isSubscribed,
                        onCheckedChange = { onToggleTopic(topic.topicId, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyanAccent
                        ),
                        modifier = Modifier.testTag("fcm_topic_switch_${topic.topicId}")
                    )
                }
            }
        }
    }
}

@Composable
private fun SimulatorTab(
    deals: List<PropertyDeal>,
    selectedDealId: Long?,
    selectedSimType: FcmPushType,
    onSelectDealId: (Long) -> Unit,
    onSelectSimType: (FcmPushType) -> Unit,
    onSendPush: () -> Unit
) {
    val selectedDeal = deals.find { it.id == selectedDealId } ?: deals.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Simulatore di Notifiche Push in Tempo Reale",
                            color = TextPrimaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Genera un payload FCM istantaneo per testare l'apparizione delle notifiche nella barra di sistema Android, il testo espandibile BigTextStyle e i deep-link.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Alert Type Selection
        item {
            Text(
                text = "1. SELEZIONA IL TIPO DI EVENTO REAL-TIME",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    FcmPushType.PRICE_DROP to Pair("⚡ Ribasso di Prezzo Immediato", "Notifica riduzione prezzo o sconto asta del 25%"),
                    FcmPushType.STATUS_CHANGE to Pair("⚖️ Cambio Stato Procedura", "Notifica 2° Incanto Deserto e nuovo bando"),
                    FcmPushType.GRAVE_DANCER_DISTRESS to Pair("🔥 Grave Dancer™ Distress", "Notifica asimmetria estrema e venditori motivati"),
                    FcmPushType.BRIEF_MATCH to Pair("🎯 Match Investor Brief", "Deal perfettamente in linea con i criteri di ricerca"),
                    FcmPushType.NEW_AUCTION to Pair("🏛️ Nuovo Bando Asta PVP", "Nuova esecuzione immobiliare pubblicata")
                ).forEach { (type, info) ->
                    val isSelected = selectedSimType == type
                    Surface(
                        color = if (isSelected) Color(0xFF0C4A6E) else SurfaceCardDark,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isSelected) CyanAccent else SurfaceCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSimType(type) }
                            .testTag("fcm_sim_type_${type.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectSimType(type) },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                            )
                            Column {
                                Text(text = info.first, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(text = info.second, color = TextSecondaryDark, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Deal Target Selection
        item {
            Text(
                text = "2. IMMOBILE TARGET ASSOCIATO",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (selectedDeal != null) {
            item {
                Surface(
                    color = SurfaceCardDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = selectedDeal.title, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "ID #${selectedDeal.id}", color = CyanAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text(text = "📍 ${selectedDeal.location}", color = TextSecondaryDark, fontSize = 11.sp)
                        Text(text = "Prezzo Attuale: € ${selectedDeal.askingPrice.toInt()}", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Action Button: Send Real Push
        item {
            Button(
                onClick = onSendPush,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("fcm_send_test_push_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Invia Notifica Push FCM in Tempo Reale",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun PushHistoryTab(
    pushHistory: List<FcmPushAlert>,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat,
    onMarkRead: (String) -> Unit,
    onClearHistory: () -> Unit,
    onSelectDeal: (Long) -> Unit
) {
    if (pushHistory.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(48.dp))
                Text("Nessuna notifica push ricevuta finora", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Usa il tab 'Simulatore Push' per inviare una notifica test istantanea.", color = TextSecondaryDark, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOG ALERT REAL-TIME (${pushHistory.size})",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                TextButton(
                    onClick = onClearHistory,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.testTag("fcm_clear_history_button")
                ) {
                    Text("Cancella Tutto", color = RoseRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(pushHistory, key = { it.id }) { alert ->
            val badgeColor = when (alert.type) {
                FcmPushType.PRICE_DROP -> CyanAccent
                FcmPushType.STATUS_CHANGE -> AmberGold
                FcmPushType.GRAVE_DANCER_DISTRESS -> RoseRed
                FcmPushType.BRIEF_MATCH -> EmeraldGreen
                else -> Color(0xFF38BDF8)
            }

            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (!alert.isRead) badgeColor.copy(alpha = 0.5f) else SurfaceCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onMarkRead(alert.id)
                        alert.dealId?.let { onSelectDeal(it) }
                    }
                    .testTag("fcm_alert_item_${alert.id}")
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
                        Surface(
                            color = badgeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = alert.type.name,
                                color = badgeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = dateFormat.format(Date(alert.receivedTimestamp)),
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = alert.title,
                        color = TextPrimaryDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = alert.body,
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    if (alert.oldPrice != null && alert.newPrice != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Prezzo: ${currencyFormat.format(alert.oldPrice)} ➔ ${currencyFormat.format(alert.newPrice)}",
                                color = Color(0xFFA7F3D0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (alert.discountPercent != null) {
                                Surface(
                                    color = RoseRed.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "-${String.format(Locale.US, "%.1f", alert.discountPercent)}%",
                                        color = Color(0xFFF87171),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (alert.dealId != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Tocca per aprire scheda deal ➔",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
