package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScraperSource
import com.example.data.ScraperTestResult
import com.example.ui.UiState
import com.example.ui.theme.*

import com.example.data.PropertyDeal

@Composable
fun ParserSandboxScreen(
    uiState: UiState,
    sources: List<ScraperSource>,
    onSourceSelectForTest: (ScraperSource) -> Unit,
    onRunTestClick: (ScraperSource, String, String) -> Unit,
    onUpdateStatusClick: (String, String) -> Unit,
    onEditRulesClick: (ScraperSource) -> Unit,
    onValidateSourcesClick: () -> Unit = {},
    onImportDealsClick: (List<PropertyDeal>) -> Unit = {},
    onExecuteBatchScrapeClick: () -> Unit = {}
) {
    var selectedSource by remember { mutableStateOf<ScraperSource?>(sources.firstOrNull()) }
    var sampleHtmlPayload by remember {
        mutableStateOf(
            """
            <div class="property-card">
              <h3 class="card-title">Attico Corso Buenos Aires - Asta Dismissione</h3>
              <span class="price-value">€ 215.000</span>
              <span class="valuation-badge">€ 340.000</span>
              <span class="surface-m2">105 mq</span>
              <span class="city-label">Milano (MI)</span>
            </div>
            """.trimIndent()
        )
    }
    var rulesJsonInput by remember { mutableStateOf("") }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingDealsToImport by remember { mutableStateOf<List<PropertyDeal>>(emptyList()) }

    if (showImportConfirmDialog && pendingDealsToImport.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AmberGold)
                    Text("Conferma Importazione Test", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Stai per importare ${pendingDealsToImport.size} record nel Radar Feed.",
                        fontSize = 13.sp,
                        color = TextPrimaryDark
                    )
                    Text(
                        "Questi dati provengono da uno snippet incollato a mano nella sandbox di collaudo e saranno etichettati con provenienza Dati Sintetici (SYNTHETIC_DEMO). Non provengono da una connessione live con portali reali.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportDealsClick(pendingDealsToImport)
                        showImportConfirmDialog = false
                        pendingDealsToImport = emptyList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black)
                ) {
                    Text("Importa come Dati di Test", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = false }) {
                    Text("Annulla", color = TextSecondaryDark)
                }
            },
            containerColor = SurfaceCardDark,
            titleContentColor = TextPrimaryDark,
            textContentColor = TextSecondaryDark
        )
    }

    LaunchedEffect(selectedSource) {
        selectedSource?.let {
            rulesJsonInput = it.activeParserRulesJson
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Banner Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gestione Fonti & Sandbox Parser",
                    color = TextPrimaryDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Verifica Robots.txt e Collaudo Selettori Scraper",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BentoPurpleContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Text(
                    text = "${sources.size} Fonti Configurate",
                    color = BentoPurpleOnContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Main 2-Column or Scrollable Layout: Left Sources List, Right Simulator
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Section 1: Active Sources Overview Grid/List
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rete Fonti Scraper & Stato Robots.txt",
                            color = CyanAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { /* Disabled in this build */ },
                                enabled = false,
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = SurfaceCardBorder,
                                    disabledContentColor = TextMutedDark
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("batch_scrape_button")
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Batch Scrape (Disabilitato)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onValidateSourcesClick,
                                colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleHeader, contentColor = BentoPurpleOnContainer),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("validate_sources_button")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Valida Configurazioni", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Ingestion unavailable disclaimer banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceCardDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Ingestione dati non disponibile: questa build non è collegata ad alcuna fonte reale. Nessun immobile è stato importato.",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            items(sources, key = { it.id }) { source ->
                val isSelected = selectedSource?.id == source.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            1.dp,
                            if (isSelected) CyanAccent else SurfaceCardBorder,
                            RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            selectedSource = source
                            onSourceSelectForTest(source)
                        }
                        .testTag("source_item_${source.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) BentoPurpleContainer.copy(alpha = 0.3f) else SurfaceCardDark
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.name,
                                    color = TextPrimaryDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = source.url,
                                    color = TextMutedDark,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }

                            // Status Badges
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusBadge(
                                    label = "Robots: ${source.robotsStatus}",
                                    isOk = source.robotsStatus == "CONSENTITO" || source.robotsStatus == "NESSUN_ROBOTS"
                                )
                                StatusBadge(
                                    label = source.configStatus,
                                    isOk = source.configStatus == "CONSENTITO"
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rilevati ${source.totalDealsFound} Immobili",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Toggle status button
                                TextButton(
                                    onClick = {
                                        val newStatus = if (source.configStatus == "CONSENTITO") "DA_VERIFICARE" else "CONSENTITO"
                                        onUpdateStatusClick(source.id, newStatus)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (source.configStatus == "CONSENTITO") "Imposta 'da_verificare'" else "Approva 'consentito'",
                                        fontSize = 11.sp,
                                        color = if (source.configStatus == "CONSENTITO") AmberGold else EmeraldGreen
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedSource = source
                                        onEditRulesClick(source)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("test_source_btn_${source.id}")
                                ) {
                                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyanAccent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Selettori", fontSize = 11.sp, color = CyanAccent)
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Live Parser Testing Console & Simulator
            item {
                Spacer(modifier = Modifier.height(8.dp))
                selectedSource?.let { src ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(28.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                            .size(36.dp)
                                            .background(BentoPurpleHeader, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.BugReport, contentDescription = null, tint = BentoPurpleOnContainer)
                                    }
                                    Text(
                                        text = "Simulator Parser: ${src.name}",
                                        color = TextPrimaryDark,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        onRunTestClick(src, sampleHtmlPayload, rulesJsonInput)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.testTag("run_parser_test_btn")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Esegui Test Parser", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Rules Config JSON Input
                            Text(
                                text = "Regole Selettori JSON (CSS / XPath / Regex):",
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = rulesJsonInput,
                                onValueChange = { rulesJsonInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .testTag("rules_json_input"),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = CyanAccent
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = LightBg,
                                    unfocusedContainerColor = LightBg,
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = SurfaceCardBorder
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )

                            // Sample DOM Input
                            Text(
                                text = "Snippet Payload HTML / JSON di Prova:",
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = sampleHtmlPayload,
                                onValueChange = { sampleHtmlPayload = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .testTag("sample_html_input"),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = TextPrimaryDark
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = LightBg,
                                    unfocusedContainerColor = LightBg,
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = SurfaceCardBorder
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )

                            // Console Output Logs Terminal (Bento Grid dark box)
                            Text(
                                text = "Console Log di Esecuzione Parser:",
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(TerminalBg)
                                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(TerminalGreen, RoundedCornerShape(50))
                                        )
                                        Text(
                                            text = "TERMINAL OUTPUT",
                                            color = TerminalText.copy(alpha = 0.5f),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (uiState.isRunningParserTest) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = TerminalPurple,
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                text = "Simulazione scraping in corso...",
                                                color = TerminalAmber,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    } else if (uiState.parserTestLogs.isEmpty()) {
                                        Text(
                                            text = "> Premere 'Esegui Test Parser' per iniziare la verifica dei selettori e la simulazione.",
                                            color = TerminalText.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    } else {
                                        uiState.parserTestLogs.forEach { logLine ->
                                            Text(
                                                text = logLine,
                                                color = if (logLine.contains("SUCCESS") || logLine.contains("OK")) TerminalGreen
                                                else if (logLine.contains("Error") || logLine.contains("FAIL")) RoseRed
                                                else TerminalText,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        uiState.parserTestResult?.let { res ->
                                            if (res.isSuccess) {
                                                HorizontalDivider(color = SurfaceCardBorder, modifier = Modifier.padding(vertical = 4.dp))
                                                Text(
                                                    text = "PARSED PROPERLY: '${res.extractedTitle}'",
                                                    color = TerminalPurple,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = "Prezzo Estratto: € ${res.extractedPrice} | Valutazione: € ${res.extractedMarketValue} (Sconto: -${res.discountPercent}%)",
                                                    color = TerminalGreen,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )

                                                if (res.extractedDeals.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Button(
                                                        onClick = { onImportDealsClick(res.extractedDeals) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("import_scraped_deals_button")
                                                    ) {
                                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            "Importa ${res.extractedDeals.size} Immobili Estratti nel Radar Feed",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
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
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, isOk: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isOk) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOk) EmeraldGreen.copy(alpha = 0.5f) else RoseRed.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = label,
            color = if (isOk) EmeraldGreen else RoseRed,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
