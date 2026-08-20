package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.AuthResult
import com.example.data.InvestorProfile
import com.example.data.PropertyDeal
import com.example.ui.components.DealKanbanBoard
import com.example.ui.components.EmailVerificationDialog
import com.example.ui.components.ForgotPasswordDialog
import com.example.ui.theme.*
import com.example.util.BriefMatcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorBriefScreen(
    profile: InvestorProfile?,
    allDeals: List<PropertyDeal>,
    isFirebaseConfigured: Boolean,
    onSaveProfile: (InvestorProfile) -> Unit,
    onSignUp: (email: String, pass: String, fullName: String, company: String, tier: String, capital: Double, onResult: (AuthResult) -> Unit) -> Unit,
    onSignIn: (email: String, pass: String, onResult: (AuthResult) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    onDealClick: (PropertyDeal) -> Unit,
    onCalculateRoiClick: (PropertyDeal) -> Unit,
    onLanguageToggle: () -> Unit = {},
    onStageChange: (dealId: Long, newStage: String) -> Unit = { _, _ -> },
    onBookmarkToggle: (PropertyDeal) -> Unit = {},
    onTriggerScan: () -> Unit = {},
    onResetNotifiedCache: () -> Unit = {},
    onSendPasswordResetEmail: ((email: String, onResult: (AuthResult) -> Unit) -> Unit)? = null,
    onCheckEmailVerification: ((onResult: (Boolean) -> Unit) -> Unit)? = null,
    onResendEmailVerification: ((onResult: (AuthResult) -> Unit) -> Unit)? = null,
    onSimulateEmailVerification: (() -> Unit)? = null
) {
    val strings = com.example.util.LocalAppStrings.current
    val context = LocalContext.current
    var isAuthDialogOpen by remember { mutableStateOf(false) }
    var pendingVerificationEmail by remember { mutableStateOf<String?>(null) }

    val activeProfile = profile ?: InvestorProfile()

    // Brief Form State initialized with activeProfile
    var targetLocations by remember(activeProfile) { mutableStateOf(activeProfile.briefTargetLocations) }
    var targetTypes by remember(activeProfile) { mutableStateOf(activeProfile.briefPropertyTypes) }
    var maxBudgetStr by remember(activeProfile) { mutableStateOf(activeProfile.briefMaxBudget.toInt().toString()) }
    var minDiscountStr by remember(activeProfile) { mutableStateOf(activeProfile.briefMinDiscountPercent.toString()) }
    var minRoiStr by remember(activeProfile) { mutableStateOf(activeProfile.briefMinTargetRoiPercent.toString()) }
    var selectedStrategy by remember(activeProfile) { mutableStateOf(activeProfile.briefStrategy) }
    var maxRenovationStr by remember(activeProfile) { mutableStateOf(activeProfile.briefMaxRenovationCost.toInt().toString()) }
    var isBriefActive by remember(activeProfile) { mutableStateOf(activeProfile.briefActive) }
    var alertsEnabled by remember(activeProfile) { mutableStateOf(activeProfile.briefAlertsEnabled) }

    // Evaluated matched deals
    val matchedDeals = remember(allDeals, activeProfile, targetLocations, targetTypes, maxBudgetStr, minDiscountStr) {
        val currentBudget = maxBudgetStr.toDoubleOrNull() ?: activeProfile.briefMaxBudget
        val currentDiscount = minDiscountStr.toIntOrNull() ?: activeProfile.briefMinDiscountPercent

        val tempProfile = activeProfile.copy(
            briefTargetLocations = targetLocations,
            briefPropertyTypes = targetTypes,
            briefMaxBudget = currentBudget,
            briefMinDiscountPercent = currentDiscount,
            briefActive = isBriefActive
        )

        allDeals.map { deal ->
            val match = BriefMatcher.evaluate(deal, tempProfile)
            Pair(deal, match)
        }.filter { it.second.isTargetMatch }
            .sortedByDescending { it.second.score }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(16.dp)
            .testTag("investor_brief_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = strings.briefAreaTitle,
                            color = TextPrimaryDark,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = strings.briefAreaSubtitle,
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = SurfaceCardDark,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier
                            .clickable { onLanguageToggle() }
                            .testTag("language_toggle_button_brief")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(text = strings.language.flag, fontSize = 14.sp)
                            Text(text = strings.language.name, color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        color = SurfaceCardDark,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (activeProfile.isRegistered) EmeraldGreen else AmberGold)
                            )
                            Text(
                                text = if (activeProfile.isRegistered) strings.verifiedInvestor else strings.guestUser,
                                color = TextPrimaryDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Account Profile Overview Card with Firebase Auth details
        item {
            Surface(
                color = BentoPurpleHeader,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(AmberGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeProfile.fullName.take(2).uppercase(),
                                color = Color.Black,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeProfile.fullName,
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeProfile.companyName,
                                color = CyanAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${activeProfile.investorTier} • ${activeProfile.email}",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = { isAuthDialogOpen = true },
                            modifier = Modifier.testTag("edit_investor_account_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = "Accedi / Registrati Firebase",
                                tint = AmberGold,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Firebase Auth Status Strip
                    Surface(
                        color = SurfaceCardDark.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isFirebaseConfigured) CyanAccent else AmberGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isFirebaseConfigured) "Firebase Auth: Online" else "Firebase Auth: Modalità Locale",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }

                            if (activeProfile.isRegistered) {
                                TextButton(
                                    onClick = {
                                        onSignOut()
                                        Toast.makeText(context, "Disconnesso con successo", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Disconnetti", color = RoseRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = SurfaceCardBorder)

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
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Capitale Allocabile:",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "€${String.format("%,.0f", activeProfile.availableCapital)}",
                                color = EmeraldGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { isAuthDialogOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (activeProfile.isRegistered) "Gestisci Account" else "Accedi / Registrati",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Brief di Ricerca Configuration Form
        item {
            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Brief di Ricerca Investitore",
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isBriefActive) "Attivo" else "Pausa",
                                color = if (isBriefActive) EmeraldGreen else TextSecondaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = isBriefActive,
                                onCheckedChange = { isBriefActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldGreen,
                                    checkedTrackColor = BentoPurpleHeader
                                ),
                                modifier = Modifier.testTag("brief_active_switch")
                            )
                        }
                    }

                    Text(
                        text = "Imposta le tue regole di investimento per filtrare ed evidenziare automaticamente gli immobili nello stream Radar e nella Mappa Mappa.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )

                    HorizontalDivider(color = SurfaceCardBorder)

                    // Target Locations
                    OutlinedTextField(
                        value = targetLocations,
                        onValueChange = { targetLocations = it },
                        label = { Text("Città e Zone Target (separate da virgola)") },
                        placeholder = { Text("es. Milano, Roma, Bologna, Torino") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = CyanAccent) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedLabelColor = CyanAccent,
                            unfocusedLabelColor = TextSecondaryDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("brief_locations_input")
                    )

                    // Quick Location Suggestions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Esempi rapidi: ", color = TextSecondaryDark, fontSize = 11.sp)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val suggestions = listOf("Milano", "Roma", "Bologna", "Torino", "Firenze", "Napoli")
                            items(suggestions) { city ->
                                Surface(
                                    color = BentoPurpleHeader,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable {
                                        if (!targetLocations.contains(city, ignoreCase = true)) {
                                            targetLocations = if (targetLocations.isBlank()) city else "$targetLocations, $city"
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "+ $city",
                                        color = TextPrimaryDark,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Property Types Target
                    OutlinedTextField(
                        value = targetTypes,
                        onValueChange = { targetTypes = it },
                        label = { Text("Tipologie Immobile / Operazione Target") },
                        placeholder = { Text("es. Residenziale, Commerciale, Asta, NPL") },
                        leadingIcon = { Icon(Icons.Default.HomeWork, contentDescription = null, tint = AmberGold) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGold,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedLabelColor = AmberGold,
                            unfocusedLabelColor = TextSecondaryDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("brief_types_input")
                    )

                    // Budget & Discount Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = maxBudgetStr,
                            onValueChange = { maxBudgetStr = it.filter { char -> char.isDigit() } },
                            label = { Text("Budget Max (€)") },
                            leadingIcon = { Icon(Icons.Default.Euro, contentDescription = null, tint = EmeraldGreen) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = SurfaceCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("brief_budget_input")
                        )

                        OutlinedTextField(
                            value = minDiscountStr,
                            onValueChange = { minDiscountStr = it.filter { char -> char.isDigit() } },
                            label = { Text("Sconto Min (%)") },
                            leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null, tint = AmberGold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberGold,
                                unfocusedBorderColor = SurfaceCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("brief_discount_input")
                        )
                    }

                    // ROI & Renovation Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = minRoiStr,
                            onValueChange = { minRoiStr = it },
                            label = { Text("Target ROI (%)") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = EmeraldGreen) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = SurfaceCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("brief_roi_input")
                        )

                        OutlinedTextField(
                            value = maxRenovationStr,
                            onValueChange = { maxRenovationStr = it.filter { char -> char.isDigit() } },
                            label = { Text("Ristruttur. Max (€)") },
                            leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = CyanAccent) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = SurfaceCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("brief_renovation_input")
                        )
                    }

                    // Strategy Radio Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Strategia di Investimento Prevalente:", color = TextSecondaryDark, fontSize = 12.sp)
                        val strategies = listOf(
                            "Trading / Flip Rapido",
                            "Messa a Rendita / Cash Flow",
                            "Short-Rent B&B",
                            "Sviluppo & Riqualificazione"
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(strategies) { strat ->
                                Surface(
                                    color = if (selectedStrategy == strat) AmberGold else SurfaceCardDark,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (selectedStrategy == strat) AmberGold else SurfaceCardBorder
                                    ),
                                    modifier = Modifier.clickable { selectedStrategy = strat }
                                ) {
                                    Text(
                                        text = strat,
                                        color = if (selectedStrategy == strat) Color.Black else TextPrimaryDark,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedStrategy == strat) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Notification Alerts Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AmberGold)
                            Column {
                                Text("Notifiche Immediati Match", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Avviso quando lo scraper trova nuovi immobili in target", color = TextSecondaryDark, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = alertsEnabled,
                            onCheckedChange = { alertsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = AmberGold)
                        )
                    }

                    // Save Brief Button
                    Button(
                        onClick = {
                            val updatedProfile = activeProfile.copy(
                                briefTargetLocations = targetLocations,
                                briefPropertyTypes = targetTypes,
                                briefMaxBudget = maxBudgetStr.toDoubleOrNull() ?: activeProfile.briefMaxBudget,
                                briefMinDiscountPercent = minDiscountStr.toIntOrNull() ?: activeProfile.briefMinDiscountPercent,
                                briefMinTargetRoiPercent = minRoiStr.toDoubleOrNull() ?: activeProfile.briefMinTargetRoiPercent,
                                briefStrategy = selectedStrategy,
                                briefMaxRenovationCost = maxRenovationStr.toDoubleOrNull() ?: activeProfile.briefMaxRenovationCost,
                                briefActive = isBriefActive,
                                briefAlertsEnabled = alertsEnabled
                            )
                            onSaveProfile(updatedProfile)
                            Toast.makeText(context, "Brief di Ricerca Salvato con Successo!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_brief_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aggiorna & Salva Brief Investitore", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    // WorkManager Background Scanner Status & Trigger
                    Surface(
                        color = BentoPurpleHeader.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (alertsEnabled) EmeraldGreen else AmberGold)
                                    )
                                    Text(
                                        text = "WorkManager Background Scanner",
                                        color = TextPrimaryDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    color = if (alertsEnabled) EmeraldGreen.copy(alpha = 0.2f) else SurfaceCardDark,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (alertsEnabled) EmeraldGreen.copy(alpha = 0.4f) else SurfaceCardBorder)
                                ) {
                                    Text(
                                        text = if (alertsEnabled) "Attivo (Ogni 15 min)" else "Notifiche Inattive",
                                        color = if (alertsEnabled) EmeraldGreen else TextSecondaryDark,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Il servizio WorkManager monitora periodicamente il database Room locale per individuare nuovi immobili e aste conformi ai tuoi criteri e inviare notifiche push locali.",
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onTriggerScan()
                                        Toast.makeText(context, "Avvio scansione immediata WorkManager...", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .testTag("trigger_workmanager_scan_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scansiona Ora", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onResetNotifiedCache()
                                        Toast.makeText(context, "Cache notifiche azzerata.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .testTag("reset_workmanager_cache_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Azzera Notificati", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Live Matched Deals Header
        item {
            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = null, tint = EmeraldGreen)
                            Text(
                                text = "Opportunità In Target Brief",
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = EmeraldGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen)
                        ) {
                            Text(
                                text = "${matchedDeals.size} Immobili Trovati",
                                color = EmeraldGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (matchedDeals.isNotEmpty()) {
                        val totalVol = matchedDeals.sumOf { it.first.askingPrice }
                        val avgDiscount = matchedDeals.map { it.first.discountPercent }.average().toInt()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Volume Totale Target", color = TextSecondaryDark, fontSize = 11.sp)
                                Text("€${String.format("%,.0f", totalVol)}", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Sconto Medio", color = TextSecondaryDark, fontSize = 11.sp)
                                Text("-$avgDiscount%", color = AmberGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Strategia Preferita", color = TextSecondaryDark, fontSize = 11.sp)
                                Text(selectedStrategy, color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Nessun immobile nel database rispetta tutti i criteri stringenti del brief. Prova ad ampliare il budget massimo o le zone target.",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Kanban Pipeline Board Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .padding(vertical = 6.dp)
            ) {
                DealKanbanBoard(
                    deals = allDeals,
                    savedDealsOnly = false,
                    onStageChange = onStageChange,
                    onDealClick = onDealClick,
                    onBookmarkToggle = onBookmarkToggle,
                    onCalculateClick = onCalculateRoiClick
                )
            }
        }

        // Matched Deals Cards List
        items(matchedDeals) { (deal, match) ->
            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (match.score >= 80) AmberGold else SurfaceCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDealClick(deal) }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = if (match.score >= 80) AmberGold else EmeraldGreen,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                Text(
                                    text = "${match.score}% MATCH BRIEF",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = deal.location,
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = deal.title,
                        color = TextPrimaryDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "€${String.format("%,.0f", deal.askingPrice)}",
                            color = EmeraldGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stima: €${String.format("%,.0f", deal.estimatedMarketValue)}",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                        Surface(
                            color = AmberGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "-${deal.discountPercent}%",
                                color = AmberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Reasons Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(match.reasons) { reason ->
                            Surface(
                                color = BentoPurpleHeader,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = reason,
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onCalculateRoiClick(deal) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simula ROI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onDealClick(deal) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleHeader),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Apri Scheda", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Firebase Auth & Account Registration Dialog
    if (isAuthDialogOpen) {
        FirebaseAuthInvestorDialog(
            currentProfile = activeProfile,
            isFirebaseConfigured = isFirebaseConfigured,
            onDismiss = { isAuthDialogOpen = false },
            onSignUp = { email, pass, name, company, tier, capital ->
                onSignUp(email, pass, name, company, tier, capital) { result ->
                    when (result) {
                        is AuthResult.Success -> {
                            if (result.isEmailVerified) {
                                Toast.makeText(context, "Registrazione completata per ${result.email}!", Toast.LENGTH_SHORT).show()
                                isAuthDialogOpen = false
                            } else {
                                isAuthDialogOpen = false
                                pendingVerificationEmail = result.email
                            }
                        }
                        is AuthResult.RequiresVerification -> {
                            isAuthDialogOpen = false
                            pendingVerificationEmail = result.email
                        }
                        is AuthResult.Error -> {
                            Toast.makeText(context, "Errore: ${result.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onSignIn = { email, pass ->
                onSignIn(email, pass) { result ->
                    when (result) {
                        is AuthResult.Success -> {
                            if (result.isEmailVerified) {
                                Toast.makeText(context, "Bentornato ${result.email}!", Toast.LENGTH_SHORT).show()
                                isAuthDialogOpen = false
                            } else {
                                isAuthDialogOpen = false
                                pendingVerificationEmail = result.email
                            }
                        }
                        is AuthResult.RequiresVerification -> {
                            isAuthDialogOpen = false
                            pendingVerificationEmail = result.email
                        }
                        is AuthResult.Error -> {
                            Toast.makeText(context, "Errore di accesso: ${result.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onSendPasswordResetEmail = onSendPasswordResetEmail
        )
    }

    // Mandatory Email Verification Dialog
    if (pendingVerificationEmail != null) {
        EmailVerificationDialog(
            email = pendingVerificationEmail ?: "",
            isFirebaseConfigured = isFirebaseConfigured,
            onDismiss = { pendingVerificationEmail = null },
            onCheckVerification = { callback ->
                if (onCheckEmailVerification != null) {
                    onCheckEmailVerification(callback)
                } else {
                    callback(true)
                }
            },
            onResendVerification = { callback ->
                if (onResendEmailVerification != null) {
                    onResendEmailVerification(callback)
                } else {
                    callback(AuthResult.Success("sim_uid", pendingVerificationEmail ?: "", isEmailVerified = false))
                }
            },
            onSimulateVerification = onSimulateEmailVerification,
            onVerificationSuccess = {
                Toast.makeText(context, "Email verificata con successo!", Toast.LENGTH_SHORT).show()
                pendingVerificationEmail = null
            }
        )
    }
}

@Composable
fun FirebaseAuthInvestorDialog(
    currentProfile: InvestorProfile,
    isFirebaseConfigured: Boolean,
    onDismiss: () -> Unit,
    onSignUp: (email: String, pass: String, fullName: String, companyName: String, investorTier: String, availableCapital: Double) -> Unit,
    onSignIn: (email: String, pass: String) -> Unit,
    onSendPasswordResetEmail: ((email: String, onResult: (AuthResult) -> Unit) -> Unit)? = null
) {
    var isSignUpTab by remember { mutableStateOf(true) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf(currentProfile.fullName) }
    var companyName by remember { mutableStateOf(currentProfile.companyName) }
    var email by remember { mutableStateOf(currentProfile.email) }
    var password by remember { mutableStateOf("") }
    var investorTier by remember { mutableStateOf(currentProfile.investorTier) }
    var capitalStr by remember { mutableStateOf(currentProfile.availableCapital.toInt().toString()) }

    var isLoading by remember { mutableStateOf(false) }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            isFirebaseConfigured = isFirebaseConfigured,
            onDismiss = { showForgotPasswordDialog = false },
            onSendResetEmail = { targetEmail, callback ->
                if (onSendPasswordResetEmail != null) {
                    onSendPasswordResetEmail(targetEmail, callback)
                } else {
                    callback(AuthResult.Success("sim_reset_fallback", targetEmail))
                }
            }
        )
    }

    val tiers = listOf(
        "Family Office / Fix & Flip",
        "Investitore Privato",
        "Società d'Investimento RE",
        "Operatore NPL / Aste"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCardDark,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = AmberGold)
                    Text(
                        text = if (isSignUpTab) "Registrazione Firebase Investitore" else "Accedi all'Account Investitore",
                        color = TextPrimaryDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Tab Selector (Registrati vs Accedi)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BentoPurpleHeader)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isSignUpTab = true }
                            .background(if (isSignUpTab) AmberGold else Color.Transparent)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nuova Registrazione",
                            color = if (isSignUpTab) Color.Black else TextPrimaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isSignUpTab = false }
                            .background(if (!isSignUpTab) AmberGold else Color.Transparent)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Accedi (Login)",
                            color = if (!isSignUpTab) Color.Black else TextPrimaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isFirebaseConfigured) {
                    Surface(
                        color = AmberGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Firebase Auth attivo in modalità di simulazione locale (pronto per google-services.json)",
                                color = TextPrimaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (isSignUpTab) {
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(Icons.Default.MarkEmailUnread, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Verifica Obbligatoria: Dopo la registrazione invieremo un'email di attivazione per verificare la casella di posta.",
                                color = TextPrimaryDark,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Aziendale / PEC") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password Firebase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input")
                )

                if (!isSignUpTab) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showForgotPasswordDialog = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("forgot_password_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Password dimenticata? Recupera accesso",
                                color = AmberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (isSignUpTab) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nome e Cognome Referente") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_fullname_input")
                    )

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Ragione Sociale / Fondo / Società") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_company_input")
                    )

                    OutlinedTextField(
                        value = capitalStr,
                        onValueChange = { capitalStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Capitale Disponibile per Operazioni (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_capital_input")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Profilo Operativo:", color = TextSecondaryDark, fontSize = 11.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(tiers) { t ->
                                Surface(
                                    color = if (investorTier == t) AmberGold else BentoPurpleHeader,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { investorTier = t }
                                ) {
                                    Text(
                                        text = t,
                                        color = if (investorTier == t) Color.Black else TextPrimaryDark,
                                        fontSize = 10.sp,
                                        fontWeight = if (investorTier == t) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        if (isSignUpTab) {
                            val cap = capitalStr.toDoubleOrNull() ?: 500000.0
                            onSignUp(email, password, fullName, companyName, investorTier, cap)
                        } else {
                            onSignIn(email, password)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("auth_confirm_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = if (isSignUpTab) "Crea Account Firebase" else "Accedi",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = TextSecondaryDark)
            }
        }
    )
}
