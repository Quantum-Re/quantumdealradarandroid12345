package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

/**
 * Custom illustrative empty state for 'My Properties' dashboard.
 * Features glowing ambient breathing animation, interactive strategy shortcuts,
 * 1-tap template loader, and quick 'Add Property' action.
 */
@Composable
fun IllustrativeEmptyPortfolioState(
    searchQuery: String,
    selectedFilter: String,
    onResetFilters: () -> Unit,
    onAddProperty: () -> Unit,
    onStrategySelected: ((String) -> Unit)? = null,
    onLoadDemoTemplate: ((String) -> Unit)? = null,
    onSyncCsv: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isFilterActive = searchQuery.isNotBlank() || (selectedFilter != "ALL" && selectedFilter.isNotBlank())

    // Infinite breathing glow animation for the illustrative art
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val floatScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatScale"
    )

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        BentoPurpleOnContainer.copy(alpha = glowAlpha * 0.7f),
                        SurfaceCardBorder,
                        CyanAccent.copy(alpha = glowAlpha * 0.3f)
                    )
                ),
                RoundedCornerShape(26.dp)
            )
            .testTag("empty_portfolio_state_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Illustrative Art Container with animated glowing aura
            Box(
                modifier = Modifier
                    .scale(floatScale)
                    .size(165.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                BentoPurpleContainer.copy(alpha = glowAlpha),
                                SurfaceCardDark
                            )
                        )
                    )
                    .border(
                        2.dp,
                        Brush.linearGradient(
                            listOf(
                                BentoPurpleOnContainer.copy(alpha = glowAlpha),
                                CyanAccent.copy(alpha = glowAlpha * 0.8f)
                            )
                        ),
                        RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_empty_portfolio),
                    contentDescription = "Portfolio Immobiliare",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Status Pill Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isFilterActive) AmberWarningContainer else BentoPurpleHeader,
                border = BorderStroke(1.dp, if (isFilterActive) AmberWarningBorder else BentoPurpleOnContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isFilterActive) Icons.Default.FilterAltOff else Icons.Default.AddHomeWork,
                        contentDescription = null,
                        tint = if (isFilterActive) AmberWarningText else BentoPurpleOnContainer,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isFilterActive) "FILTRI ATTIVI (0 RISULTATI)" else "PORTFOLIO OPERAZIONI VUOTO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFilterActive) AmberWarningText else BentoPurpleOnContainer,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            // Headings & Context
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isFilterActive) "Nessun immobile corrisponde ai criteri" else "Inizia a costruire il tuo Portfolio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isFilterActive)
                        "Nessuna proprietà corrisponde alla ricerca \"$searchQuery\" o allo stato selezionato. Azzera i filtri o inserisci una nuova operazione con questi parametri."
                    else
                        "Registra e gestisci le tue opportunità immobiliari: calcola ROI netto e Cap Rate, monitora avanzamento cantieri e genera report di investimento pronti per la banca.",
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }

            // Interactive Bento Highlight Features
            if (!isFilterActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EmptyFeatureBentoTile(
                        icon = Icons.Default.Analytics,
                        label = "ROI & Cap Rate",
                        desc = "Calcolo netto automatico",
                        accentColor = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                    EmptyFeatureBentoTile(
                        icon = Icons.Default.Construction,
                        label = "Stato Cantieri",
                        desc = "Monitoraggio SAL & Spese",
                        accentColor = AmberGold,
                        modifier = Modifier.weight(1f)
                    )
                    EmptyFeatureBentoTile(
                        icon = Icons.Default.PictureAsPdf,
                        label = "Dossier PDF",
                        desc = "Schede per finanziatori",
                        accentColor = BentoPurpleOnContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Strategy Shortcuts with 1-Tap Load
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Strategie suggerite & Template:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = "Tocca per creare o precaricare",
                            fontSize = 11.sp,
                            color = TextMutedDark
                        )
                    }

                    StrategyShortcutCard(
                        icon = Icons.Default.Build,
                        title = "Fix & Flip (Ristruttura e Rivendi)",
                        subtitle = "Acquisto a forte sconto, computo metrico e stima ROI netto di rivendita.",
                        accentColor = CyanAccent,
                        onPrimaryClick = { onStrategySelected?.invoke("Fix & Flip") ?: onAddProperty() },
                        onLoadDemoClick = { onLoadDemoTemplate?.invoke("Fix & Flip") }
                    )

                    StrategyShortcutCard(
                        icon = Icons.Default.Key,
                        title = "Buy & Hold (Rendita da Locazione)",
                        subtitle = "Flusso di cassa mensile, canone concordato/studenti e rendimento netto.",
                        accentColor = EmeraldGreen,
                        onPrimaryClick = { onStrategySelected?.invoke("Buy & Hold") ?: onAddProperty() },
                        onLoadDemoClick = { onLoadDemoTemplate?.invoke("Buy & Hold") }
                    )

                    StrategyShortcutCard(
                        icon = Icons.Default.Gavel,
                        title = "Aste Giudiziarie & Stralci NPL",
                        subtitle = "Ribassi oltre il 30% sul valore di perizia, saldo e stralcio e saldo prezzo.",
                        accentColor = AmberGold,
                        onPrimaryClick = { onStrategySelected?.invoke("Aste & NPL") ?: onAddProperty() },
                        onLoadDemoClick = { onLoadDemoTemplate?.invoke("Aste & NPL") }
                    )
                }
            }

            // Primary Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFilterActive) {
                    OutlinedButton(
                        onClick = onResetFilters,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_reset_portfolio_filters")
                    ) {
                        Icon(Icons.Default.FilterListOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Azzera Filtri", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    if (onSyncCsv != null) {
                        OutlinedButton(
                            onClick = onSyncCsv,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = BentoPurpleHeader.copy(alpha = 0.6f),
                                contentColor = BentoPurpleOnContainer
                            ),
                            border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_empty_sync_csv")
                        ) {
                            Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importa CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (onLoadDemoTemplate != null) {
                        OutlinedButton(
                            onClick = { onLoadDemoTemplate("Fix & Flip") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = BentoPurpleHeader.copy(alpha = 0.5f),
                                contentColor = BentoPurpleOnContainer
                            ),
                            border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_load_sample_property")
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Carica Demo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = onAddProperty,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BentoPurpleOnContainer,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("btn_add_first_property")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isFilterActive) "Aggiungi Immobile" else "Nuova Operazione",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Bento Highlight Feature item for the empty state.
 */
@Composable
private fun EmptyFeatureBentoTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    desc: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BentoCardBgLight,
        border = BorderStroke(1.dp, SurfaceCardBorder.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = desc,
                fontSize = 9.sp,
                color = TextMutedDark,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp
            )
        }
    }
}

/**
 * Strategy shortcut mini-card with optional 1-tap template demo loader.
 */
@Composable
private fun StrategyShortcutCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onPrimaryClick: () -> Unit,
    onLoadDemoClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BentoCardBgLight,
        border = BorderStroke(1.dp, SurfaceCardBorder.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPrimaryClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    lineHeight = 14.sp
                )
            }

            if (onLoadDemoClick != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLoadDemoClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                        Text("Demo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextMutedDark,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Custom illustrative empty state for search results in Deal Radar and Distressed Feeds.
 * Features radar wave glow animation, contextual tips, and 1-tap query chips.
 */
@Composable
fun IllustrativeEmptySearchState(
    searchQuery: String,
    selectedSource: String = "ALL",
    onResetFilters: () -> Unit,
    onAddDealClick: () -> Unit,
    onSuggestionClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "search_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        CyanAccent.copy(alpha = pulseAlpha * 0.7f),
                        SurfaceCardBorder,
                        BentoBlueOnContainer.copy(alpha = 0.3f)
                    )
                ),
                RoundedCornerShape(26.dp)
            )
            .testTag("empty_search_state_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Illustrative Art Container with radar cyan glow
            Box(
                modifier = Modifier
                    .size(165.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CyanAccent.copy(alpha = pulseAlpha * 0.35f),
                                SurfaceCardDark
                            )
                        )
                    )
                    .border(
                        2.dp,
                        Brush.linearGradient(
                            listOf(
                                CyanAccent.copy(alpha = pulseAlpha),
                                BentoBlueOnContainer.copy(alpha = pulseAlpha * 0.7f)
                            )
                        ),
                        RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_empty_search),
                    contentDescription = "Nessun risultato di ricerca",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Status Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BentoBlueContainer,
                border = BorderStroke(1.dp, BentoBlueOnContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = BentoBlueOnContainer,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "RADAR SCAN (0 CORRISPONDENZE)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoBlueOnContainer,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            // Headings & Prompt
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "Nessuna opportunità per \"$searchQuery\"" else "Nessuna opportunità con i filtri correnti",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "I criteri di ricerca impostati non hanno prodotto risultati tra le aste, le dismissioni e gli annunci monitorati. Prova a selezionare una ricerca rapida o inserisci manualmente un'opportunità.",
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }

            // Quick suggestion search chips
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Ricerche rapide consigliate:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMutedDark
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    listOf("Milano", "Roma Aste", "Sconto > 30%", "Fix & Flip").forEach { querySuggestion ->
                        SuggestionChip(
                            onClick = {
                                if (onSuggestionClick != null) {
                                    onSuggestionClick(querySuggestion)
                                } else {
                                    onResetFilters()
                                }
                            },
                            label = { Text(querySuggestion, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = BentoCardBgLight,
                                labelColor = CyanAccent
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = CyanAccent.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }

            // Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onResetFilters,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                    border = BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_reset_search_empty_state")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mostra Tutto", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onAddDealClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_add_manual_deal_empty_state")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aggiungi Deal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
