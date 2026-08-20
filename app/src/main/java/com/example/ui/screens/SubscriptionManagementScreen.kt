package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.AuthResult
import com.example.auth.FirebaseCustomClaims
import com.example.data.InvestorProfile
import com.example.ui.components.EmailVerificationDialog
import com.example.ui.components.ForgotPasswordDialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionManagementScreen(
    investorProfile: InvestorProfile?,
    customClaims: FirebaseCustomClaims,
    isFirebaseConfigured: Boolean,
    currentUserEmail: String?,
    currentUserId: String?,
    onNavigateBack: () -> Unit,
    onSelectPlan: (plan: String, isAnnual: Boolean) -> Unit,
    onRefreshClaims: () -> Unit,
    onSimulateServerClaimUpdate: (plan: String, isPremium: Boolean, role: String) -> Unit,
    onCancelSubscription: () -> Unit,
    onSendPasswordResetEmail: ((email: String, onResult: (AuthResult) -> Unit) -> Unit)? = null,
    onCheckEmailVerification: ((onResult: (Boolean) -> Unit) -> Unit)? = null,
    onResendEmailVerification: ((onResult: (AuthResult) -> Unit) -> Unit)? = null,
    onSimulateEmailVerification: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isAnnualBillingSelected by remember { mutableStateOf(investorProfile?.subscriptionBillingCycle != "MONTHLY") }
    var isRefreshingClaims by remember { mutableStateOf(false) }
    var showDevClaimsDialog by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showEmailVerificationDialog by remember { mutableStateOf(false) }

    if (showEmailVerificationDialog) {
        val userEmail = currentUserEmail ?: investorProfile?.email ?: "investitore@quantum.it"
        EmailVerificationDialog(
            email = userEmail,
            isFirebaseConfigured = isFirebaseConfigured,
            onDismiss = { showEmailVerificationDialog = false },
            onCheckVerification = { cb ->
                if (onCheckEmailVerification != null) {
                    onCheckEmailVerification(cb)
                } else {
                    cb(true)
                }
            },
            onResendVerification = { cb ->
                if (onResendEmailVerification != null) {
                    onResendEmailVerification(cb)
                } else {
                    cb(AuthResult.Success("sim_uid", userEmail, isEmailVerified = false))
                }
            },
            onSimulateVerification = onSimulateEmailVerification,
            onVerificationSuccess = {
                Toast.makeText(context, "Email verificata con successo!", Toast.LENGTH_SHORT).show()
                showEmailVerificationDialog = false
            }
        )
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = currentUserEmail ?: investorProfile?.email ?: "",
            isFirebaseConfigured = isFirebaseConfigured,
            onDismiss = { showForgotPasswordDialog = false },
            onSendResetEmail = { email, cb ->
                if (onSendPasswordResetEmail != null) {
                    onSendPasswordResetEmail(email, cb)
                } else {
                    cb(AuthResult.Success("sim_reset", email))
                }
            }
        )
    }

    val isCurrentPro = investorProfile?.isProSubscriber == true || customClaims.isPremium
    val currentPlanName = if (isCurrentPro) {
        if (investorProfile?.subscriptionBillingCycle == "MONTHLY" || customClaims.plan == "MONTHLY") "PRO Mensile" else "PRO Annuale"
    } else {
        "Free Explorer (Blind Mode)"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Gestione Abbonamento",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Firebase Auth Custom Claims & Piani PRO",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("sub_screen_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isRefreshingClaims = true
                                onRefreshClaims()
                                delay(600)
                                isRefreshingClaims = false
                                Toast.makeText(context, "Claims Firebase aggiornati!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("refresh_claims_appbar_btn")
                    ) {
                        if (isRefreshingClaims) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = CyanAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Aggiorna Claims Firebase",
                                tint = CyanAccent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSlateBg
                )
            )
        },
        containerColor = DarkSlateBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Current Membership Status Hero
            item {
                CurrentStatusHeroCard(
                    isPro = isCurrentPro,
                    planName = currentPlanName,
                    claims = customClaims,
                    profile = investorProfile,
                    userEmail = currentUserEmail ?: investorProfile?.email ?: "investor@quantum.it",
                    userId = currentUserId ?: "uid_987",
                    onOpenClaimsDetails = { showDevClaimsDialog = true }
                )
            }

            // 2. Billing Cycle Toggle (Monthly vs Annual with -20% Discount Badge)
            item {
                BillingCycleSelector(
                    isAnnual = isAnnualBillingSelected,
                    onToggle = { isAnnualBillingSelected = it }
                )
            }

            // 3. Plan Cards Comparison
            item {
                Text(
                    text = "Piani Disponibili",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            item {
                PlanPricingCard(
                    title = "Investor PRO",
                    badge = if (isAnnualBillingSelected) "CONSIGLIATO (-20%)" else "SENZA VINCOLI",
                    price = if (isAnnualBillingSelected) "€79" else "€99",
                    period = "/ mese",
                    subtext = if (isAnnualBillingSelected) "Fatturato annualmente (€948/anno + IVA)" else "Fatturato mensilmente, disdici quando vuoi",
                    isCurrentPlan = isCurrentPro && ((isAnnualBillingSelected && (investorProfile?.subscriptionBillingCycle == "ANNUAL" || customClaims.plan == "ANNUAL")) || (!isAnnualBillingSelected && (investorProfile?.subscriptionBillingCycle == "MONTHLY" || customClaims.plan == "MONTHLY"))),
                    isHighlighted = true,
                    features = listOf(
                        "Accesso Illimitato a tutti gli immobili (Nessun Blind Mode)",
                        "Benchmark OMI e micro-zona al m² in tempo reale",
                        "Simulatore ROI Avanzato con imposte e cash flow",
                        "Scaricamento Perizie CTU e Planimetrie originali PDF",
                        "Allarmi Push su ribassi d'asta e shock di mercato",
                        "Esportazione Dossier Finanziario asseverato per Banche"
                    ),
                    ctaText = if (isCurrentPro) {
                        if (isAnnualBillingSelected && investorProfile?.subscriptionBillingCycle == "MONTHLY") "Passa al Piano Annuale (Risparmia)"
                        else if (!isAnnualBillingSelected && investorProfile?.subscriptionBillingCycle == "ANNUAL") "Passa al Piano Mensile"
                        else "Piano Attualmente Attivo"
                    } else {
                        "SOTTOSCRIVI INVESTOR PRO"
                    },
                    onCtaClick = {
                        val selectedPlan = if (isAnnualBillingSelected) "ANNUAL" else "MONTHLY"
                        onSelectPlan(selectedPlan, isAnnualBillingSelected)
                        Toast.makeText(context, "Piano $selectedPlan attivato con Custom Claims Firebase!", Toast.LENGTH_LONG).show()
                    }
                )
            }

            item {
                PlanPricingCard(
                    title = "Free Explorer",
                    badge = "PIANO BASE",
                    price = "€0",
                    period = " per sempre",
                    subtext = "Ideale per esplorare la piattaforma con Blind Mode",
                    isCurrentPlan = !isCurrentPro,
                    isHighlighted = false,
                    features = listOf(
                        "Visualizzazione radar con dati sensibili oscurati (Blind)",
                        "1 Token sblocco omaggio per un immobile",
                        "Filtri di ricerca base per regione e categoria",
                        "Notifiche generiche di nuovi annunci"
                    ),
                    ctaText = if (!isCurrentPro) "Piano Attualmente Attivo" else "Esegui Downgrade a Free",
                    onCtaClick = {
                        if (isCurrentPro) {
                            showCancelConfirmDialog = true
                        }
                    }
                )
            }

            // 4. Firebase Auth Custom Claims Diagnostic Section
            item {
                FirebaseClaimsDiagnosticCard(
                    claims = customClaims,
                    isFirebaseConfigured = isFirebaseConfigured,
                    userEmail = currentUserEmail ?: investorProfile?.email ?: "investor@quantum.it",
                    userId = currentUserId ?: "uid_987",
                    isRefreshing = isRefreshingClaims,
                    onRefreshClick = {
                        coroutineScope.launch {
                            isRefreshingClaims = true
                            onRefreshClaims()
                            delay(500)
                            isRefreshingClaims = false
                            Toast.makeText(context, "Token claims aggiornati da Firebase Auth!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSimulateClaimsClick = { showDevClaimsDialog = true },
                    onForgotPasswordClick = { showForgotPasswordDialog = true },
                    onVerifyEmailClick = { showEmailVerificationDialog = true }
                )
            }

            // 5. Billing & Invoices Section
            item {
                BillingInvoicesCard(
                    isPro = isCurrentPro,
                    renewalDate = investorProfile?.subscriptionRenewalDate ?: customClaims.formattedValidUntil,
                    billingCycle = if (isAnnualBillingSelected) "Annuale" else "Mensile"
                )
            }

            // 6. Security & Guarantee Badges
            item {
                SecurityGuaranteeCard()
            }
        }
    }

    // Modal: Developer & Server Claims Simulator Dialog
    if (showDevClaimsDialog) {
        AlertDialog(
            onDismissRequest = { showDevClaimsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AmberGold)
                    Text("Firebase Auth Claims Console", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimaryDark)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "I Custom Claims sono attributi di sicurezza crittografati nel token JWT di Firebase Auth emessi dal backend (Cloud Functions). Puoi testare l'aggiornamento istantaneo dei claims:",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )

                    Surface(
                        color = SurfaceCardDark,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("ID Token JWT (Snippet):", fontSize = 11.sp, color = TextMutedDark)
                            Text(customClaims.idTokenSnippet ?: "eyJhbGciOiJSUzI1Ni...dHJ1ZQ", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyanAccent)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Raw Claims Map:", fontSize = 11.sp, color = TextMutedDark)
                            Text(
                                "{\n  \"premium\": ${customClaims.isPremium},\n  \"plan\": \"${customClaims.plan}\",\n  \"role\": \"${customClaims.role}\",\n  \"unlockedDeals\": ${customClaims.maxUnlockedDeals}\n}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = AmberGold
                            )
                        }
                    }

                    Text("Imposta rapidamente stato Claim:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSimulateServerClaimUpdate("ANNUAL", true, "pro_investor")
                                showDevClaimsDialog = false
                                Toast.makeText(context, "Claims impostati: PRO ANNUAL", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("PRO Annual", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onSimulateServerClaimUpdate("MONTHLY", true, "pro_investor")
                                showDevClaimsDialog = false
                                Toast.makeText(context, "Claims impostati: PRO MONTHLY", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("PRO Monthly", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onSimulateServerClaimUpdate("FREE", false, "investor")
                            showDevClaimsDialog = false
                            Toast.makeText(context, "Claims reimpostati: FREE", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset a FREE Tier", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDevClaimsDialog = false }) {
                    Text("Chiudi", color = CyanAccent)
                }
            },
            containerColor = DarkSlateBg
        )
    }

    // Modal: Confirm Cancel / Downgrade Dialog
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = {
                Text("Conferma Downgrade a Free", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            },
            text = {
                Text(
                    "Se confermi il passaggio al piano Free, i dati finanziari ROI, i benchmark OMI riservati e le perizie CTU verranno nuovamente protetti con la modalità Blind. Sei sicuro di voler procedere?",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelSubscription()
                        showCancelConfirmDialog = false
                        Toast.makeText(context, "Abbonamento disattivato. Sei passato al piano Free.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Conferma Downgrade", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Mantieni PRO", color = CyanAccent)
                }
            },
            containerColor = DarkSlateBg
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Sub-Components
// -------------------------------------------------------------------------------------------------

@Composable
private fun CurrentStatusHeroCard(
    isPro: Boolean,
    planName: String,
    claims: FirebaseCustomClaims,
    profile: InvestorProfile?,
    userEmail: String,
    userId: String,
    onOpenClaimsDetails: () -> Unit
) {
    val bgBrush = if (isPro) {
        Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF1A1C29)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (isPro) AmberGold.copy(alpha = 0.5f) else SurfaceCardBorder,
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(bgBrush)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isPro) AmberGold.copy(alpha = 0.15f) else Color(0xFF334155),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isPro) AmberGold else Color(0xFF64748B))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isPro) Icons.Default.WorkspacePremium else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isPro) AmberGold else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isPro) "PRO ATTIVO" else "FREE TIER",
                                color = if (isPro) AmberGold else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onOpenClaimsDetails() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(12.dp))
                            Text("Claims Auth", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = planName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Account: $userEmail",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }

                HorizontalDivider(color = SurfaceCardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Prossimo Rinnovo", fontSize = 11.sp, color = TextMutedDark)
                        Text(
                            text = if (isPro) (profile?.subscriptionRenewalDate ?: claims.formattedValidUntil) else "Nessuno (Free)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPro) CyanAccent else TextSecondaryDark
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Stato Sicurezza Token", fontSize = 11.sp, color = TextMutedDark)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isPro) Color(0xFF10B981) else AmberGold)
                            )
                            Text(
                                text = if (isPro) "JWT Asseverato" else "Base Token",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BillingCycleSelector(
    isAnnual: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        color = SurfaceCardDark,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Monthly Button
            Surface(
                color = if (!isAnnual) DarkSlateBg else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = if (!isAnnual) BorderStroke(1.dp, CyanAccent) else null,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggle(false) }
                    .testTag("sub_toggle_monthly")
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Fatturazione Mensile",
                        fontSize = 13.sp,
                        fontWeight = if (!isAnnual) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isAnnual) CyanAccent else TextSecondaryDark
                    )
                    Text(
                        text = "€99 / mese",
                        fontSize = 11.sp,
                        color = if (!isAnnual) TextPrimaryDark else TextMutedDark
                    )
                }
            }

            // Annual Button with Discount Badge
            Surface(
                color = if (isAnnual) DarkSlateBg else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = if (isAnnual) BorderStroke(1.dp, AmberGold) else null,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggle(true) }
                    .testTag("sub_toggle_annual")
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Fatturazione Annuale",
                            fontSize = 13.sp,
                            fontWeight = if (isAnnual) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAnnual) AmberGold else TextSecondaryDark
                        )
                        Surface(
                            color = AmberGold,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "-20%",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "€79 / mese (€948/anno)",
                        fontSize = 11.sp,
                        color = if (isAnnual) TextPrimaryDark else TextMutedDark
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanPricingCard(
    title: String,
    badge: String,
    price: String,
    period: String,
    subtext: String,
    isCurrentPlan: Boolean,
    isHighlighted: Boolean,
    features: List<String>,
    ctaText: String,
    onCtaClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.5.dp,
                if (isCurrentPlan) AmberGold else if (isHighlighted) CyanAccent.copy(alpha = 0.6f) else SurfaceCardBorder,
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Surface(
                    color = if (isHighlighted) AmberGold.copy(alpha = 0.2f) else Color(0xFF334155),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (isHighlighted) AmberGold else Color(0xFF64748B))
                ) {
                    Text(
                        text = badge,
                        color = if (isHighlighted) AmberGold else TextSecondaryDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = price,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isHighlighted) AmberGold else TextPrimaryDark
                )
                Text(
                    text = period,
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                )
            }

            Text(
                text = subtext,
                fontSize = 12.sp,
                color = TextSecondaryDark
            )

            HorizontalDivider(color = SurfaceCardBorder)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isHighlighted) CyanAccent else TextMutedDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = feature,
                            fontSize = 12.sp,
                            color = TextPrimaryDark
                        )
                    }
                }
            }

            Button(
                onClick = onCtaClick,
                enabled = !isCurrentPlan || isHighlighted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("plan_action_btn_${title.lowercase().replace(" ", "_")}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrentPlan) Color(0xFF334155) else if (isHighlighted) AmberGold else CyanAccent,
                    contentColor = if (isCurrentPlan) TextSecondaryDark else Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = ctaText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FirebaseClaimsDiagnosticCard(
    claims: FirebaseCustomClaims,
    isFirebaseConfigured: Boolean,
    userEmail: String,
    userId: String,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    onSimulateClaimsClick: () -> Unit,
    onForgotPasswordClick: () -> Unit = {},
    onVerifyEmailClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Firebase Auth Custom Claims",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Surface(
                    color = if (isFirebaseConfigured) Color(0xFF065F46) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isFirebaseConfigured) "FIREBASE LIVE" else "DEV SIMULATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFirebaseConfigured) Color(0xFF34D399) else CyanAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (!isFirebaseConfigured) {
                Surface(
                    color = Color(0xFF334155).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF64748B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Funzione non disponibile: Firebase non è configurato in questa build. Operazioni in modalità emulata/offline.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            Text(
                text = "Lo stato dell'abbonamento viene validato crittograficamente tramite i Custom Claims iniettati nel token ID di Firebase Authentication.",
                fontSize = 12.sp,
                color = TextSecondaryDark
            )

            // Claims Grid
            Surface(
                color = DarkSlateBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ClaimRowItem("claims.premium", claims.isPremium.toString(), if (claims.isPremium) Color(0xFF10B981) else Color(0xFFEF4444))
                    ClaimRowItem("claims.plan", claims.plan, AmberGold)
                    ClaimRowItem("claims.role", claims.role, CyanAccent)
                    ClaimRowItem("claims.validUntil", claims.formattedValidUntil, TextPrimaryDark)
                    ClaimRowItem("claims.lastSynced", claims.lastSyncedAt, TextMutedDark)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRefreshClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("refresh_claims_btn"),
                    border = BorderStroke(1.dp, CyanAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CyanAccent)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Forza Sync Token", color = CyanAccent, fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onSimulateClaimsClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_claims_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Claims", color = TextPrimaryDark, fontSize = 12.sp)
                }
            }

            // Password Reset / Account Recovery Button
            OutlinedButton(
                onClick = onForgotPasswordClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sub_reset_password_btn"),
                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold)
            ) {
                Icon(Icons.Default.LockReset, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Invia Email di Reset Password / Recupero", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            // Email Verification Status / Action Button
            OutlinedButton(
                onClick = onVerifyEmailClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sub_verify_email_btn"),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
            ) {
                Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gestisci / Verifica Stato Email Firebase", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ClaimRowItem(key: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(key, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextMutedDark)
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun BillingInvoicesCard(
    isPro: Boolean,
    renewalDate: String,
    billingCycle: String
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Dati di Fatturazione & Ricevute",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            Surface(
                color = DarkSlateBg,
                shape = RoundedCornerShape(10.dp),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(22.dp))
                        Column {
                            Text("Fattura Elettronica SDI #INV-2026-08", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text("Importo: €${if (billingCycle == "Annuale") "948,00" else "99,00"} + IVA (Pagato)", fontSize = 11.sp, color = TextSecondaryDark)
                        }
                    }

                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Scaricamento PDF Fattura #INV-2026-08 in corso...", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Scarica Fattura", tint = CyanAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityGuaranteeCard() {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
            Column {
                Text(
                    text = "Garanzia Soddisfatti o Rimborsati 14 Giorni",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Deducibilità fiscale integrale al 100% come costo di ricerca & sviluppo per società e P.IVA.",
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
            }
        }
    }
}
