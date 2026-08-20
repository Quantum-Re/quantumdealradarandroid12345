package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import kotlinx.coroutines.launch

data class OnboardingStep(
    val stepNumber: Int,
    val badgeTitle: String,
    val badgeColor: Color,
    val badgeIcon: ImageVector,
    val title: String,
    val tagline: String,
    val description: String,
    val highlights: List<Pair<ImageVector, String>>,
    val accentColors: List<Color>,
    val previewType: OnboardingPreviewType
)

enum class OnboardingPreviewType {
    RADAR_SOURCING,
    GRAVE_DANCER_DISTRESS,
    ROI_FINANCIAL_ENGINE,
    INVESTOR_BRIEF_ALERTS,
    ACCREDITED_REGISTRATION_CTA
}

@Composable
fun OnboardingCarouselDialog(
    isVisible: Boolean,
    onDismiss: (dontShowAgain: Boolean) -> Unit,
    onStartRegistration: () -> Unit
) {
    if (!isVisible) return

    val coroutineScope = rememberCoroutineScope()
    var dontShowAgain by remember { mutableStateOf(false) }

    val steps = remember {
        listOf(
            OnboardingStep(
                stepNumber = 1,
                badgeTitle = "DEAL SOURCING 24/7",
                badgeColor = CyanAccent,
                badgeIcon = Icons.Default.Radar,
                title = "Algoritmi di Sourcing & Quantum Score™",
                tagline = "Scansione continua del mercato immobiliare e delle aste giudiziarie",
                description = "Quantum Deal Radar aggrega in tempo reale annunci da aste telematiche (PVP), fallimenti, portali NPL e mercato libero. Ogni opportunità viene valutata istantaneamente rispetto ai benchmark OMI con il nostro algoritmo di scoring proprietario.",
                highlights = listOf(
                    Icons.Default.TrendingDown to "Calcolo automatico sconto vs quotazioni reali OMI",
                    Icons.Default.Speed to "Quantum Score (0-100) basato su liquidità e rendimento",
                    Icons.Default.Gavel to "Integrazione diretta bandi d'asta ed esecuzioni immobiliari"
                ),
                accentColors = listOf(CyanAccent, PurpleIndigo),
                previewType = OnboardingPreviewType.RADAR_SOURCING
            ),
            OnboardingStep(
                stepNumber = 2,
                badgeTitle = "PREDICTIVE DISTRESS",
                badgeColor = RoseRed,
                badgeIcon = Icons.Default.WarningAmber,
                title = "Grave Dancer™ & Venditori Motivati",
                tagline = "Individua le asimmetrie di prezzo prima della concorrenza",
                description = "Ispirato alla strategia contrarian di Sam Zell, il modulo di distress intelligence monitora aste deserte successive (-25% a round), segnali di urgenza finanziaria dei venditori e scadenze procedurali critiche.",
                highlights = listOf(
                    Icons.Default.Autorenew to "Previsione ribassi automatici al 2° e 3° incanto d'asta",
                    Icons.Default.Psychology to "Rilevamento venditori motivati e urgenza di chiusura",
                    Icons.Default.Shield to "Filtro anti-trappola per evitare immobili con abusi insanabili"
                ),
                accentColors = listOf(RoseRed, Color(0xFF991B1B)),
                previewType = OnboardingPreviewType.GRAVE_DANCER_DISTRESS
            ),
            OnboardingStep(
                stepNumber = 3,
                badgeTitle = "FINANCIAL MODELING",
                badgeColor = EmeraldGreen,
                badgeIcon = Icons.Default.Calculate,
                title = "Simulatore Finanziario & Pro-Forma ROI",
                tagline = "Business plan bancabili in pochi secondi",
                description = "Trasforma ogni immobile in un modello finanziario completo: Cash-on-Cash Return, Net Cap Rate, Regola del 70% Fix & Flip, leva finanziaria con LTV personalizzato e stima automatica delle imposte e spese d'asta.",
                highlights = listOf(
                    Icons.Default.AccountBalance to "Simulazione mutuo, ammortamento e cash-flow mensile",
                    Icons.Default.Handyman to "Computo metrico estimativo ristrutturazione integrato",
                    Icons.Default.ShowChart to "Confronto scenari: Compravendita veloce vs Rendita da affitto"
                ),
                accentColors = listOf(EmeraldGreen, Color(0xFF047857)),
                previewType = OnboardingPreviewType.ROI_FINANCIAL_ENGINE
            ),
            OnboardingStep(
                stepNumber = 4,
                badgeTitle = "PERSONALIZED RADAR",
                badgeColor = Color(0xFF38BDF8),
                badgeIcon = Icons.Default.Tune,
                title = "Investor Brief & Notifiche in Background",
                tagline = "Ricevi notifiche push solo per deal rigorosamente in target",
                description = "Imposta i tuoi parametri operativi: budget, zone target, tipologia d'immobile e sconto minimo. Il motore di background WorkManager scansiona il mercato e ti avvisa immediatamente non appena appare un'opportunità idonea.",
                highlights = listOf(
                    Icons.Default.NotificationsActive to "Alert proattivi in background senza consumare batteria",
                    Icons.Default.FilterAlt to "Matching personalizzato basato sulla tua tesi d'investimento",
                    Icons.Default.NearMe to "Mappa geospaziale interattiva con visualizzazione offline"
                ),
                accentColors = listOf(Color(0xFF38BDF8), Color(0xFF0369A1)),
                previewType = OnboardingPreviewType.INVESTOR_BRIEF_ALERTS
            ),
            OnboardingStep(
                stepNumber = 5,
                badgeTitle = "ACCREDITED ACCESS",
                badgeColor = AmberGold,
                badgeIcon = Icons.Default.WorkspacePremium,
                title = "Accesso Riservato Investitori Accreditati",
                tagline = "Sblocca l'ecosistema completo per investitori istituzionali e privati",
                description = "Registra il tuo profilo per accedere a perizie tecniche CTU, dati catastali in chiaro, token mensili per lo sblocco di deal ciechi esclusivi e sincronizzazione cloud multi-dispositivo sicura con Firebase Auth.",
                highlights = listOf(
                    Icons.Default.Description to "Accesso istantaneo a perizie e documenti d'asta",
                    Icons.Default.CloudSync to "Sincronizzazione cloud e salvataggio preferiti",
                    Icons.Default.VpnKey to "Token mensili per sblocco opportunità blindate"
                ),
                accentColors = listOf(AmberGold, Color(0xFFB45309)),
                previewType = OnboardingPreviewType.ACCREDITED_REGISTRATION_CTA
            )
        )
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { steps.size })

    Dialog(
        onDismissRequest = { onDismiss(dontShowAgain) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkSlateBg)
                .testTag("onboarding_carousel_dialog"),
            color = DarkSlateBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top Navigation Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(CyanAccent, PurpleIndigo)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "QUANTUM RADAR",
                                    color = TextPrimaryDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Guida al Valore per Investitori",
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Skip / Close Button
                        TextButton(
                            onClick = { onDismiss(dontShowAgain) },
                            modifier = Modifier.testTag("onboarding_skip_button")
                        ) {
                            Text(
                                text = "Salta",
                                color = TextSecondaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Horizontal Pager Content
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("onboarding_pager")
                    ) { pageIndex ->
                        val step = steps[pageIndex]
                        OnboardingSlideContent(
                            step = step,
                            isLastSlide = pageIndex == steps.lastIndex,
                            onStartRegistration = onStartRegistration
                        )
                    }

                    // Bottom Navigation Bar & Progress Dots
                    Surface(
                        color = SurfaceCardDark,
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Step Indicators & Page Count
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Progress Dots
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    steps.indices.forEach { index ->
                                        val isSelected = pagerState.currentPage == index
                                        val targetWidth by animateDpAsState(
                                            targetValue = if (isSelected) 24.dp else 8.dp,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .height(8.dp)
                                                .width(targetWidth)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (isSelected) steps[pagerState.currentPage].badgeColor
                                                    else SurfaceCardBorder
                                                )
                                                .clickable {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(index)
                                                    }
                                                }
                                                .testTag("onboarding_dot_$index")
                                        )
                                    }
                                }

                                Text(
                                    text = "${pagerState.currentPage + 1} di ${steps.size}",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Don't show again toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dontShowAgain = !dontShowAgain },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = dontShowAgain,
                                    onCheckedChange = { dontShowAgain = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = CyanAccent,
                                        uncheckedColor = TextSecondaryDark,
                                        checkmarkColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .testTag("onboarding_dont_show_again_checkbox")
                                )
                                Text(
                                    text = "Non mostrare più all'avvio dell'applicazione",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (pagerState.currentPage > 0) {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, SurfaceCardBorder),
                                        modifier = Modifier
                                            .weight(0.35f)
                                            .height(48.dp)
                                            .testTag("onboarding_prev_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null,
                                            tint = TextPrimaryDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Indietro", color = TextPrimaryDark, fontSize = 13.sp)
                                    }
                                }

                                if (pagerState.currentPage < steps.lastIndex) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = steps[pagerState.currentPage].badgeColor
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(if (pagerState.currentPage > 0) 0.65f else 1f)
                                            .height(48.dp)
                                            .testTag("onboarding_next_button")
                                    ) {
                                        Text(
                                            text = "Continua",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    // Final Slide CTA
                                    Button(
                                        onClick = {
                                            onDismiss(dontShowAgain)
                                            onStartRegistration()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AmberGold
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(if (pagerState.currentPage > 0) 0.65f else 1f)
                                            .height(48.dp)
                                            .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = AmberGold)
                                            .testTag("onboarding_register_cta")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RocketLaunch,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Registrati Ora & Crea Profilo",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingSlideContent(
    step: OnboardingStep,
    isLastSlide: Boolean,
    onStartRegistration: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Badge
        Surface(
            color = step.badgeColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, step.badgeColor.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = step.badgeIcon,
                    contentDescription = null,
                    tint = step.badgeColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = step.badgeTitle,
                    color = step.badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Title & Tagline
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = step.title,
                color = TextPrimaryDark,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
            Text(
                text = step.tagline,
                color = CyanAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        // Visual Interactive / Illustrative Mock Preview Card
        OnboardingVisualPreview(step.previewType)

        // Description Box
        Surface(
            color = SurfaceCardDark,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SurfaceCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = step.description,
                color = TextSecondaryDark,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(14.dp)
            )
        }

        // Key Value Highlights List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            step.highlights.forEach { (icon, highlightText) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(step.badgeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = step.badgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = highlightText,
                        color = TextPrimaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (isLastSlide) {
            // Extra CTA Section on the last slide
            Surface(
                color = AmberGold.copy(alpha = 0.1f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Accesso Immediato Senza Carta di Credito",
                            color = AmberGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Crea un profilo con email aziendale o personale per iniziare a monitorare gli immobili con il massimo vantaggio competitivo.",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun OnboardingVisualPreview(previewType: OnboardingPreviewType) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            when (previewType) {
                OnboardingPreviewType.RADAR_SOURCING -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = CyanAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ASTA TELEMATICA PVP #4920",
                                    color = CyanAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Quantum Score Badge
                            Surface(
                                color = Color(0xFF064E3B),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, EmeraldGreen)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(12.dp))
                                    Text("SCORE 94/100", color = EmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Text(
                            text = "Trilocale Ristrutturato • Porta Romana, Milano",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text("Prezzo d'Asta Base", color = TextSecondaryDark, fontSize = 10.sp)
                                Text("€ 142.000", color = TextPrimaryDark, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Valore Mercato OMI: € 250.000", color = TextSecondaryDark, fontSize = 9.sp)
                                Surface(
                                    color = RoseRed.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("-43.2% SOTTOCOSTO", color = Color(0xFFF87171), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }

                OnboardingPreviewType.GRAVE_DANCER_DISTRESS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = RoseRed, modifier = Modifier.size(16.dp))
                                Text("ALLARME PROCEDURALE", color = RoseRed, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            Text("Tribunale di Milano", color = TextSecondaryDark, fontSize = 10.sp)
                        }

                        Surface(
                            color = Color(0xFF1E1B4B),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Stato Procedura:", color = TextSecondaryDark, fontSize = 9.sp)
                                    Text("2° Incanto Deserto", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Ribasso Previsto Prossimo Incanto:", color = TextSecondaryDark, fontSize = 9.sp)
                                    Text("-25% (€ 106.500)", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = Color(0xFF312E81),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Urgenza: Massima", color = Color(0xFFA5B4FC), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(4.dp))
                            }
                            Surface(
                                color = Color(0xFF064E3B),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Margine: 48%", color = EmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                }

                OnboardingPreviewType.ROI_FINANCIAL_ENGINE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Simulazione Finanziaria Pro-Forma", color = TextSecondaryDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("LTV 80% • Fix & Flip", color = EmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xFF064E3B),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("CASH-ON-CASH", color = Color(0xFFA7F3D0), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("+28.4%", color = EmeraldGreen, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("UTILE NETTO", color = TextSecondaryDark, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("+€ 64.500", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("CAP RATE", color = TextSecondaryDark, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("9.8%", color = CyanAccent, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        // Mini Leverage Bar
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Leva Bancaria (€120k)", color = TextSecondaryDark, fontSize = 9.sp)
                                Text("Capitale Proprio (€35k)", color = TextSecondaryDark, fontSize = 9.sp)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            ) {
                                Box(modifier = Modifier.weight(0.8f).fillMaxHeight().background(CyanAccent))
                                Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(AmberGold))
                            }
                        }
                    }
                }

                OnboardingPreviewType.INVESTOR_BRIEF_ALERTS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        .background(EmeraldGreen)
                                )
                                Text("RADAR ATTIVO IN BACKGROUND", color = EmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("WorkManager 15m", color = TextSecondaryDark, fontSize = 9.sp)
                        }

                        Surface(
                            color = Color(0xFF082F49),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF0284C7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                Column {
                                    Text("3 Nuove Opportunità in Target", color = Color(0xFFE0F2FE), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Brief: Milano / Budget < €300k / Sconto > 35%", color = Color(0xFFBAE6FD), fontSize = 9.sp)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("📍 Milano & Hinterland", color = TextSecondaryDark, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                            }
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("🏢 Residenziale / Aste", color = TextSecondaryDark, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                            }
                        }
                    }
                }

                OnboardingPreviewType.ACCREDITED_REGISTRATION_CTA -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                                Text("PROFILO ACCREDITATO", color = AmberGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            Surface(
                                color = AmberGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("FIREBASE AUTH", color = AmberGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Perizie CTU", color = TextSecondaryDark, fontSize = 9.sp)
                                    Text("Sbloccate", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Token Mensili", color = TextSecondaryDark, fontSize = 9.sp)
                                    Text("5 Inclusi", color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Cloud Sync", color = TextSecondaryDark, fontSize = 9.sp)
                                    Text("Attivo", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = "✓ Registrazione gratuita per singoli investitori e società immobiliari",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
