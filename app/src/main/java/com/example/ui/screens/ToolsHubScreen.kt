package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainTab
import com.example.ui.theme.*
import com.example.util.LocalAppStrings

@Composable
fun ToolsHubScreen(
    onSelectTool: (MainTab) -> Unit,
    onLanguageToggle: () -> Unit,
    onThemeToggle: () -> Unit = {},
    onOpenOnboardingCarousel: (() -> Unit)? = null,
    onOpenFcmPushCenter: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val colors = AppTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Strumenti & Analisi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Moduli avanzati per investimenti immobiliari",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.secondaryContainer,
                    border = BorderStroke(1.dp, colors.onSecondaryContainer.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .clickable { onLanguageToggle() }
                        .testTag("language_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = strings.language.flag, fontSize = 13.sp)
                        Text(
                            text = strings.language.name,
                            color = colors.onSecondaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Global Theme Toggle
                val isDarkMode = colors.isDark
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkMode) Color(0xFF1E1B4B) else Color(0xFFFEF3C7),
                    border = BorderStroke(
                        1.dp,
                        if (isDarkMode) Color(0xFF818CF8).copy(alpha = 0.6f) else Color(0xFFF59E0B).copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .clickable { onThemeToggle() }
                        .testTag("theme_toggle_button_tools")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Toggle Theme",
                            tint = if (isDarkMode) Color(0xFFD0BCFF) else Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isDarkMode) "🌙 Scuro" else "☀️ Chiaro",
                            color = if (isDarkMode) Color(0xFFEADDFF) else Color(0xFF92400E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ToolItemCard(
                    title = "📊 Yield Benchmarking • Normalizzazione Macro ROI",
                    subtitle = "Tassi di riferimento, Euribor 12M, rendimento BTP 10Y e inflazione. Normalizza il ROI con l'equazione di Fisher, calcola gli spread e stress-testa il debito",
                    icon = Icons.Default.Assessment,
                    accentColor = CyanAccent,
                    iconBg = Color(0xFF0C4A6E),
                    testTag = "tool_card_yield_benchmarking",
                    onClick = { onSelectTool(MainTab.YIELD_BENCHMARKING) }
                )
            }

            item {
                ToolItemCard(
                    title = "📑 Dossier di Confronto PDF (Side-by-Side)",
                    subtitle = "Genera report comparativo istituzionale per due immobili con benchmarking costi/m², CapEx, ROI, Yield e verdetto strategico",
                    icon = Icons.Default.PictureAsPdf,
                    accentColor = Color(0xFF06B6D4),
                    iconBg = Color(0xFF082F49),
                    testTag = "tool_card_comparison_pdf",
                    onClick = { onSelectTool(MainTab.MY_PROPERTIES) }
                )
            }

            item {
                ToolItemCard(
                    title = "📂 Sincronizzazione Portafoglio CSV / Excel",
                    subtitle = "Importa da Excel, Stessa, DealCheck o software gestionale con auto-mapping delle colonne e unione dati",
                    icon = Icons.Default.SyncAlt,
                    accentColor = BentoPurpleOnContainer,
                    iconBg = BentoPurpleHeader,
                    testTag = "tool_card_csv_sync",
                    onClick = { onSelectTool(MainTab.MY_PROPERTIES) }
                )
            }

            item {
                ToolItemCard(
                    title = "⚡ Firebase Cloud Messaging (FCM) & Push Real-Time",
                    subtitle = "Allarmi istantanei su ribassi di prezzo, cambi stato asta, topic broadcast e simulatore push",
                    icon = Icons.Default.CloudSync,
                    accentColor = CyanAccent,
                    iconBg = Color(0xFF0C4A6E),
                    testTag = "tool_card_fcm_push_center",
                    onClick = { onOpenFcmPushCenter?.invoke() }
                )
            }

            item {
                ToolItemCard(
                    title = "🚀 Guida Onboarding & Valore Quantum Radar",
                    subtitle = "Rivedi la presentazione del valore: Deal Sourcing, Grave Dancer™, Calcoli ROI e Accesso Investitore",
                    icon = Icons.Default.RocketLaunch,
                    accentColor = Color(0xFF38BDF8),
                    iconBg = Color(0xFF0C4A6E),
                    testTag = "tool_card_onboarding_guide",
                    onClick = { onOpenOnboardingCarousel?.invoke() }
                )
            }

            item {
                ToolItemCard(
                    title = "⭐ Abbonamento & Firebase Custom Claims",
                    subtitle = "Gestisci i piani Mensile/Annuale (-20%), verifica i claims del token Auth JWT e sblocca i dati ROI completi",
                    icon = Icons.Default.WorkspacePremium,
                    accentColor = Color(0xFFF59E0B),
                    iconBg = Color(0xFF451A03),
                    testTag = "tool_card_subscription_management",
                    onClick = { onSelectTool(MainTab.SUBSCRIPTION) }
                )
            }

            item {
                ToolItemCard(
                    title = "📡 Proactive Supply-Demand Monitor // Scraper Live",
                    subtitle = "Allarmi push immediati per shock di offerta (Squeeze), accumulo stock (Glut) e boom canoni di zona",
                    icon = Icons.Default.Sensors,
                    accentColor = Color(0xFF06B6D4),
                    iconBg = Color(0xFF082F49),
                    testTag = "tool_card_supply_demand_monitor",
                    onClick = { onSelectTool(MainTab.SUPPLY_DEMAND_MONITOR) }
                )
            }

            item {
                ToolItemCard(
                    title = "💀 The Grave Dancer // Sam Zell Contrarian Mode",
                    subtitle = "Arbitraggio su Replacement Cost, Supply Moats, Downside Protection e acquisizioni distressed",
                    icon = Icons.Default.Gavel,
                    accentColor = Color(0xFFD4AF37),
                    iconBg = Color(0xFF4A0E17),
                    testTag = "tool_card_grave_dancer",
                    onClick = { onSelectTool(MainTab.GRAVE_DANCER) }
                )
            }

            item {
                ToolItemCard(
                    title = "⚡ Cyber Terminal & First-Principles Engine",
                    subtitle = "Scomposizione fisica atomica, stress-test Black Swan, energia solare e audit Optimus AI",
                    icon = Icons.Default.Bolt,
                    accentColor = Color(0xFFFF9100),
                    iconBg = Color(0xFFFFF3E0),
                    testTag = "tool_card_cyber_terminal",
                    onClick = { onSelectTool(MainTab.CYBER_TERMINAL) }
                )
            }

            item {
                ToolItemCard(
                    title = "🛠️ Simulatore Ristrutturazione & Computo Metrico",
                    subtitle = "Computo estimativo, detrazioni fiscali (Bonus 50%/65%), CapEx al m² e ROI Flip vs Buy&Hold",
                    icon = Icons.Default.Handyman,
                    accentColor = Color(0xFF10B981),
                    iconBg = Color(0xFF064E3B),
                    testTag = "tool_card_renovation_simulator",
                    onClick = { onSelectTool(MainTab.RENOVATION_SIMULATOR) }
                )
            }

            item {
                ToolItemCard(
                    title = "🔥 Heatmap Regionale D3 • Densità Prezzi & Rendimenti",
                    subtitle = "Mappa termica interattiva dell'Italia con shader D3, gradienti iso-densità e sentiment provinciale",
                    icon = Icons.Default.Map,
                    accentColor = Color(0xFFFF5252),
                    iconBg = Color(0xFF3E1418),
                    testTag = "tool_card_regional_heatmap",
                    onClick = { onSelectTool(MainTab.REGIONAL_HEATMAP) }
                )
            }

            item {
                ToolItemCard(
                    title = "Osservatorio KPI Mercato Immobiliare",
                    subtitle = "Dashboard interattiva con curve spline, trend regionali, yield e saturazione",
                    icon = Icons.Default.ShowChart,
                    accentColor = Color(0xFF00B0FF),
                    iconBg = Color(0xFFE1F5FE),
                    testTag = "tool_card_market_kpi_dashboard",
                    onClick = { onSelectTool(MainTab.MARKET_KPI_DASHBOARD) }
                )
            }

            item {
                ToolItemCard(
                    title = "Market Insights & Trend AI",
                    subtitle = "Analisi di mercato, andamento quotazioni e reportistica intelligente",
                    icon = Icons.Default.Public,
                    accentColor = BentoPurpleOnContainer,
                    iconBg = BentoPurpleHeader,
                    testTag = "tool_card_market_insights",
                    onClick = { onSelectTool(MainTab.MARKET_INSIGHTS) }
                )
            }

            item {
                ToolItemCard(
                    title = "Aste Giudiziarie & Distressed",
                    subtitle = "Immobili all'asta, NPL, procedure esecutive e stralci",
                    icon = Icons.Default.HouseSiding,
                    accentColor = Color(0xFFE65100),
                    iconBg = Color(0xFFFFE0B2),
                    testTag = "tool_card_distressed",
                    onClick = { onSelectTool(MainTab.DISTRESSED) }
                )
            }

            item {
                ToolItemCard(
                    title = "Profilo Investitore & Criteri Target",
                    subtitle = "Brief di acquisto, parametri di rendimento minimo e matching affari",
                    icon = Icons.Default.Badge,
                    accentColor = Color(0xFF00897B),
                    iconBg = Color(0xFFE0F2F1),
                    testTag = "tool_card_investor_brief",
                    onClick = { onSelectTool(MainTab.INVESTOR_BRIEF) }
                )
            }

            item {
                ToolItemCard(
                    title = "Analytics Portafoglio",
                    subtitle = "Scomposizione asset, plusvalenze e statistiche dettagliate",
                    icon = Icons.Default.Analytics,
                    accentColor = Color(0xFF1565C0),
                    iconBg = Color(0xFFE3F2FD),
                    testTag = "tool_card_analytics",
                    onClick = { onSelectTool(MainTab.ANALYTICS) }
                )
            }

            item {
                ToolItemCard(
                    title = "Allarmi Prezzo & Notifiche Granulari",
                    subtitle = "Soglie di ribasso personalizzate (% o €), notifiche istantanee e allarmi asta su singoli immobili",
                    icon = Icons.Default.NotificationsActive,
                    accentColor = Color(0xFFD81B60),
                    iconBg = Color(0xFFFCE4EC),
                    testTag = "tool_card_granular_notifications",
                    onClick = { onSelectTool(MainTab.NOTIFICATION_CONFIG) }
                )
            }

            item {
                ToolItemCard(
                    title = "Fonti Dati & Scraper Sandbox",
                    subtitle = "Gestione portali sorgente, regole di parsing e calibrazione dati",
                    icon = Icons.Default.Terminal,
                    accentColor = Color(0xFF6A1B9A),
                    iconBg = Color(0xFFF3E5F5),
                    testTag = "tool_card_parser_sandbox",
                    onClick = { onSelectTool(MainTab.PARSER_SANDBOX) }
                )
            }
        }
    }
}

@Composable
private fun ToolItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    iconBg: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCardDark,
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    lineHeight = 14.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMutedDark,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
