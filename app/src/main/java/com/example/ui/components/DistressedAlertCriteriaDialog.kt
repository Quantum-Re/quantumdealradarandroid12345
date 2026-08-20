package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SavedAlertCriteria
import com.example.ui.theme.*
import com.example.util.PriceAlertNotificationManager

@Composable
fun DistressedAlertCriteriaDialog(
    savedCriteria: SavedAlertCriteria,
    onDismissRequest: () -> Unit,
    onSaveCriteria: (query: String, level: String, maxPrice: Double?, enabled: Boolean) -> Unit,
    onTriggerTestNotification: (address: String, price: Double, level: String) -> Unit,
    onAddSampleMatchingProperty: (address: String, price: Double, level: String) -> Unit
) {
    val context = LocalContext.current

    var searchQuery by remember(savedCriteria) { mutableStateOf(savedCriteria.query) }
    var selectedLevel by remember(savedCriteria) { mutableStateOf(savedCriteria.distressLevel) }
    var maxPriceText by remember(savedCriteria) {
        mutableStateOf(savedCriteria.maxPrice?.toInt()?.toString() ?: "")
    }
    var alertsEnabled by remember(savedCriteria) { mutableStateOf(savedCriteria.alertsEnabled) }
    var hasPermission by remember {
        mutableStateOf(PriceAlertNotificationManager.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    val distressLevels = listOf(
        "ALL" to "Tutti i Livelli",
        "Foreclosure" to "Foreclosure",
        "Auction" to "Asta / Execution",
        "Pre-Foreclosure" to "Pre-Foreclosure",
        "Tax Lien" to "Tax Lien / NPL"
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("distressed_alert_criteria_dialog"),
        containerColor = DarkSlateBg,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = CyanAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Alert Nuovi Immobili",
                        color = TextPrimaryDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Notifiche locali al nuovo inserimento in Room DB",
                        color = TextMutedDark,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Permission Status Warning Banner (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                    Surface(
                        color = RoseRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = RoseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Permesso notifiche Android richiesto per ricevere gli allarmi.",
                                    color = TextPrimaryDark,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseRed, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("request_notification_permission_button")
                            ) {
                                Text("Consenti", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Global Alert Notification Switch
                Surface(
                    color = SurfaceCardDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
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
                                text = "Attiva Allarmi Automatici",
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Invia notifica push locale quando viene aggiunto un nuovo immobile in target",
                                color = TextMutedDark,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = alertsEnabled,
                            onCheckedChange = { alertsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyanAccent,
                                uncheckedBorderColor = SurfaceCardBorder
                            ),
                            modifier = Modifier.testTag("distressed_alert_switch")
                        )
                    }
                }

                // Criteria Input: Search Filter / Keyword
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "FILTRO PAROLA CHIAVE / INDIRIZZO",
                        color = TextMutedDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("es. Milano, Centro, Asta...", color = TextMutedDark.copy(alpha = 0.6f), fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMutedDark, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedContainerColor = SurfaceCardDark,
                            unfocusedContainerColor = SurfaceCardDark,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            cursorColor = CyanAccent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("alert_criteria_keyword_input")
                    )
                }

                // Criteria Input: Target Distress Level Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "LIVELLO DISTRESS TARGET",
                        color = TextMutedDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    var expandedLevelDropdown by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { expandedLevelDropdown = true },
                            colors = CardDefaults.outlinedCardColors(containerColor = SurfaceCardDark),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("alert_criteria_level_dropdown")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = RoseRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = distressLevels.find { it.first.equals(selectedLevel, ignoreCase = true) }?.second ?: selectedLevel,
                                        color = TextPrimaryDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMutedDark)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedLevelDropdown,
                            onDismissRequest = { expandedLevelDropdown = false },
                            modifier = Modifier
                                .background(SurfaceCardDark)
                                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
                        ) {
                            distressLevels.forEach { (levelKey, levelLabel) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = levelLabel,
                                            color = if (levelKey.equals(selectedLevel, ignoreCase = true)) CyanAccent else TextPrimaryDark,
                                            fontWeight = if (levelKey.equals(selectedLevel, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedLevel = levelKey
                                        expandedLevelDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Criteria Input: Max Base Price Limit
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "PREZZO MASSIMO SOGLIA (€)",
                        color = TextMutedDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    OutlinedTextField(
                        value = maxPriceText,
                        onValueChange = { maxPriceText = it.filter { char -> char.isDigit() } },
                        placeholder = { Text("es. 200000 (Lascia vuoto per nessun limite)", color = TextMutedDark.copy(alpha = 0.6f), fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Euro, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGold,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedContainerColor = SurfaceCardDark,
                            unfocusedContainerColor = SurfaceCardDark,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            cursorColor = AmberGold
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("alert_criteria_max_price_input")
                    )
                }

                // Quick Action Testing Buttons
                Divider(color = SurfaceCardBorder, thickness = 1.dp)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "COLLAUDO E PROVA NOTIFICHE",
                        color = TextMutedDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Instant Notification Test Button
                        OutlinedButton(
                            onClick = {
                                val maxP = maxPriceText.toDoubleOrNull() ?: 185000.0
                                val addr = if (searchQuery.isNotBlank()) "Via $searchQuery 12, Milano" else "Corso Buenos Aires 45, Milano"
                                val lvl = if (selectedLevel != "ALL") selectedLevel else "Foreclosure"
                                onTriggerTestNotification(addr, maxP, lvl)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("trigger_test_notification_btn")
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Invia Prova", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Add Sample Property to Room DB to test trigger
                        Button(
                            onClick = {
                                val maxP = maxPriceText.toDoubleOrNull() ?: 145000.0
                                val addr = if (searchQuery.isNotBlank()) "Esempio: Immobile di test ($searchQuery)" else "Esempio: Immobile di test per criterio"
                                val lvl = if (selectedLevel != "ALL") selectedLevel else "Auction"
                                onAddSampleMatchingProperty(addr, maxP, lvl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("add_matching_property_trigger_btn")
                        ) {
                            Icon(Icons.Default.AddHome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Aggiungi e Notifica", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val maxP = maxPriceText.toDoubleOrNull()
                    onSaveCriteria(searchQuery, selectedLevel, maxP, alertsEnabled)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_alert_criteria_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salva Criteri Alert", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("cancel_alert_criteria_button")
            ) {
                Text("Annulla", color = TextMutedDark, fontSize = 12.sp)
            }
        }
    )
}
