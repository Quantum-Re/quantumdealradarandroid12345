package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

data class CoachMarkStep(
    val stepIndex: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val guideHowTo: String,
    val proValueProposition: String,
    val proBadgeText: String,
    val targetAreaLabel: String
)

@Composable
fun PropertySearchCoachMarkOverlay(
    isVisible: Boolean,
    onDismiss: (dontShowAgain: Boolean) -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    if (!isVisible) return

    var currentStepIndex by remember { mutableIntStateOf(0) }
    var dontShowAgain by remember { mutableStateOf(false) }

    val steps = remember {
        listOf(
            CoachMarkStep(
                stepIndex = 0,
                title = "Barra di Ricerca Intelligente",
                subtitle = "Trova opportunità per Città, CAP o Parola Chiave",
                icon = Icons.Default.Search,
                iconTint = CyanAccent,
                iconBg = Color(0xFF0F2E3D),
                guideHowTo = "Digita il comune (es. Milano, Roma, Monza) o termini specifici come 'Trilocale', 'All'asta' o 'NPL' per filtrare istantaneamente centinaia di portali sorgente.",
                proValueProposition = "Con Investor PRO sblocchi la densità di micro-zona in tempo reale, i trend storici OMI al m² e la stima automatica del valore intrinseco senza ritardi.",
                proBadgeText = "PRO: OMI Benchmark & Micro-Zone Live",
                targetAreaLabel = "ZONA: BARRA DI RICERCA & RECENTI"
            ),
            CoachMarkStep(
                stepIndex = 1,
                title = "Filtri Strategici Rapidi",
                subtitle = "Isola gli affari per Rendimento e Sconto",
                icon = Icons.Default.Tune,
                iconTint = AmberGold,
                iconBg = Color(0xFF3B270C),
                guideHowTo = "Usa i pulsanti rapidi in alto per visualizzare solo le 'Aste Giudiziarie', gli immobili con 'Sconto >30%' o le operazioni 'Fix & Flip' ad alto margine.",
                proValueProposition = "Il motore algoritmico PRO calcola il Top 10° Percentile di convenienza, evidenziando le asimmetrie di prezzo prima che vengano scoperte dal mercato.",
                proBadgeText = "PRO: Algoritmo Top 10% Percentile",
                targetAreaLabel = "ZONA: FILTRI RAPIDI & CATEGORIE"
            ),
            CoachMarkStep(
                stepIndex = 2,
                title = "Radar Feed & Schede Immobile",
                subtitle = "Blind Mode vs Accesso Completo PRO",
                icon = Icons.Default.LockOpen,
                iconTint = Color(0xFF10B981),
                iconBg = Color(0xFF063726),
                guideHowTo = "Nel feed vedi gli annunci scansionati dai tribunali e portali. Come utente Free puoi visualizzare le metriche chiave e hai 1 Token omaggio per sbloccare un immobile.",
                proValueProposition = "Gli utenti Free vedono i dati sensibili oscurati in Blind Mode. Con PRO elimini ogni limite: visualizzi indirizzo esatto, perizia CTU originale PDF e contatti diretti del delegato.",
                proBadgeText = "PRO: Nessun Blind Mode + Perizie CTU",
                targetAreaLabel = "ZONA: LISTA IMMOBILI & SCHEDE DETTAGLIO"
            ),
            CoachMarkStep(
                stepIndex = 3,
                title = "Simulatore Finanziario ROI & Fisco",
                subtitle = "Calcola Rendimento Netto e Imposte Italiane",
                icon = Icons.Default.Calculate,
                iconTint = Color(0xFF8B5CF6),
                iconBg = Color(0xFF2E1065),
                guideHowTo = "Tocca 'Calcola ROI' su qualsiasi scheda per stimare il Cash-on-Cash Return, simulare il mutuo e calcolare le imposte (Cedolare Secca 21%/10% o IRPEF).",
                proValueProposition = "L'abbonamento PRO abilita simulazioni finanziarie illimitate, calcolo della Plusvalenza ex Art. 67 TUIR per i Flip ed esportazione del Dossier PDF per banche e investitori.",
                proBadgeText = "PRO: Export Dossier PDF Bancario",
                targetAreaLabel = "ZONA: CALCOLATORE ROI & FISCALE"
            ),
            CoachMarkStep(
                stepIndex = 4,
                title = "Perché Scegliere Investor PRO",
                subtitle = "Il vantaggio competitivo per investitori immobiliari",
                icon = Icons.Default.WorkspacePremium,
                iconTint = AmberGold,
                iconBg = Color(0xFF451A03),
                guideHowTo = "Quantum Deal Radar è progettato per darti un vantaggio informativo asimmetrico: anticipa le aste, ricevi allarmi su ribassi di prezzo e chiudi operazioni a sconto.",
                proValueProposition = "Scegli il piano Annuale con il 20% di sconto (€79/mese anziché €99). Attivazione istantanea con Firebase Auth JWT Custom Claims e garanzia 14 giorni.",
                proBadgeText = "PROMO: Piano Annuale -20% Risparmio",
                targetAreaLabel = "VALORE PRO: PANORAMICA VANTAGGI"
            )
        )
    }

    val currentStep = steps[currentStepIndex]

    // Subtle pulsing animation for spotlight effect
    val infiniteTransition = rememberInfiniteTransition(label = "coach_mark_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Dialog(
        onDismissRequest = { onDismiss(dontShowAgain) },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(16.dp)
                .testTag("coach_mark_overlay")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar: Step Counter & Skip Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Guida alla Ricerca • Passo ${currentStepIndex + 1} di ${steps.size}",
                                color = TextPrimaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    TextButton(
                        onClick = { onDismiss(dontShowAgain) },
                        modifier = Modifier.testTag("coach_mark_skip_btn")
                    ) {
                        Text(
                            text = "Salta Guida",
                            color = TextSecondaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Center Highlight Area / Target Spotlight Cue
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, currentStep.iconTint.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .scale(pulseScale)
                            .testTag("coach_mark_spotlight_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(currentStep.iconTint)
                            )
                            Text(
                                text = currentStep.targetAreaLabel,
                                color = currentStep.iconTint,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }

                // Main Coach Mark Card
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    },
                    label = "coach_mark_step_transition"
                ) { step ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                1.5.dp,
                                Brush.verticalGradient(
                                    listOf(step.iconTint.copy(alpha = 0.7f), SurfaceCardBorder)
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .testTag("coach_mark_card"),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Header with Icon & Title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(step.iconBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = step.icon,
                                        contentDescription = null,
                                        tint = step.iconTint,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = step.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = step.subtitle,
                                        fontSize = 12.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }

                            // 1. How to use (Standard Search Guidance)
                            Surface(
                                color = DarkSlateBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SurfaceCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                        Text("Come utilizzarlo:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                    }
                                    Text(
                                        text = step.guideHowTo,
                                        fontSize = 12.sp,
                                        color = TextSecondaryDark,
                                        lineHeight = 17.sp
                                    )
                                }
                            }

                            // 2. Premium Value Proposition Box (High Contrast Gold/Amber)
                            Surface(
                                color = Color(0xFF1C1917),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
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
                                            Icon(Icons.Default.Star, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "Valore Aggiunto PRO",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AmberGold
                                            )
                                        }

                                        Surface(
                                            color = AmberGold.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = step.proBadgeText,
                                                color = AmberGold,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = step.proValueProposition,
                                        fontSize = 12.sp,
                                        color = Color(0xFFFDE68A),
                                        lineHeight = 17.sp
                                    )
                                }
                            }

                            // Step Dots Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                steps.indices.forEach { index ->
                                    val isSelected = index == currentStepIndex
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(if (isSelected) 10.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) AmberGold else Color(0xFF475569))
                                            .testTag("coach_mark_step_${index + 1}_indicator")
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Area & Navigation Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button (if not on step 0)
                        if (currentStepIndex > 0) {
                            OutlinedButton(
                                onClick = { currentStepIndex -= 1 },
                                modifier = Modifier
                                    .weight(0.7f)
                                    .height(48.dp)
                                    .testTag("coach_mark_prev_btn"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SurfaceCardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Indietro", fontSize = 13.sp)
                            }
                        }

                        // Next / Final CTA Button
                        if (currentStepIndex < steps.size - 1) {
                            Button(
                                onClick = { currentStepIndex += 1 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("coach_mark_next_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Prossimo Passo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            // Last Step CTA
                            Button(
                                onClick = {
                                    onDismiss(dontShowAgain)
                                    onNavigateToSubscription()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("coach_mark_upgrade_cta"),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sblocca Investor PRO (-20%)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Bottom Row: "Non mostrare più" checkbox & Dismiss button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { dontShowAgain = !dontShowAgain }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = dontShowAgain,
                                onCheckedChange = { dontShowAgain = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AmberGold,
                                    uncheckedColor = TextSecondaryDark,
                                    checkmarkColor = Color.Black
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Non mostrare più all'avvio",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }

                        if (currentStepIndex == steps.size - 1) {
                            TextButton(
                                onClick = { onDismiss(dontShowAgain) },
                                modifier = Modifier.testTag("coach_mark_close_btn")
                            ) {
                                Text("Esplora la Piattaforma", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
