package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.MarketInsightsUiState
import com.example.ui.MarketInsightsViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketInsightsScreen(
    viewModel: MarketInsightsViewModel,
    onOpenKpiDashboard: (() -> Unit)? = null,
    onOpenRegionalHeatmap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filteredItems = remember(uiState.report.headlineItems, uiState.selectedSentimentFilter, uiState.bookmarkedInsightIds) {
        uiState.report.headlineItems.filter { item ->
            val matchesSentiment = uiState.selectedSentimentFilter == null || item.sentiment == uiState.selectedSentimentFilter
            matchesSentiment
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = SurfaceCardLight,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Market Insights & News",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                )
                            }
                            Text(
                                text = "Real-time Real Estate Intelligence powered by Google Search Grounding",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Grounding info badge
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (uiState.report.isGroundedWithGoogleSearch) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                modifier = Modifier
                                    .clickable { viewModel.setGroundingSheetOpen(true) }
                                    .testTag("grounding_sources_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.report.isGroundedWithGoogleSearch) Icons.Default.Search else Icons.Default.CloudQueue,
                                        contentDescription = null,
                                        tint = if (uiState.report.isGroundedWithGoogleSearch) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (uiState.report.isGroundedWithGoogleSearch) "Google Search" else "Curated",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (uiState.report.isGroundedWithGoogleSearch) Color(0xFF2E7D32) else Color(0xFFE65100)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { viewModel.refresh() },
                                modifier = Modifier.testTag("refresh_insights_button")
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = CyanAccent
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Aggiorna Notizie",
                                        tint = CyanAccent
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom Search Query Input
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                text = "Cerca notizie, sentenze aste, tassi mutui, città...",
                                fontSize = 13.sp,
                                color = TextMutedDark
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = CyanAccent
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { viewModel.executeSearch("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Cancella",
                                            tint = TextMutedDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.executeSearch(uiState.searchQuery) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .padding(end = 6.dp)
                                            .testTag("submit_search_button")
                                    ) {
                                        Text("Cerca", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedContainerColor = Color(0xFFF9F7FC),
                            unfocusedContainerColor = Color(0xFFF9F7FC)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("market_search_text_field")
                    )
                }
            }
        },
        containerColor = DarkSlateBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("market_insights_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 0. Dedicated Recharts KPI Dashboard Quick Access Banner
            if (onOpenKpiDashboard != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = BentoCardBgLight,
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenKpiDashboard() }
                            .testTag("banner_open_kpi_dashboard")
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Osservatorio KPI Recharts", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = CyanAccent
                                        ) {
                                            Text("2026 LIVE", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                    Text("Curve spline interattive, trend regionali & analisi yield per provincia", fontSize = 10.sp, color = TextSecondaryDark)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 0.1 Dedicated D3 Regional Heatmap Banner
            if (onOpenRegionalHeatmap != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = BentoPurpleHeader,
                        border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenRegionalHeatmap() }
                            .testTag("banner_open_regional_heatmap")
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(BentoPurpleOnContainer, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Heatmap Regionale D3", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoPurpleOnContainer)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = EmeraldGreen.copy(alpha = 0.2f),
                                            border = BorderStroke(0.6.dp, EmeraldGreen)
                                        ) {
                                            Text("D3 SHADER", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                    Text("Densità prezzi al m², rendimenti medi da locazione e sentiment provinciale", fontSize = 10.sp, color = TextPrimaryDark)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = BentoPurpleOnContainer, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 1. Macro Indicators Carousel Strip
            item {
                Text(
                    text = "INDICATORI MACRO IMMOBILIARI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedDark,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                MacroIndicatorsRibbon(indicators = uiState.report.macroIndicators)
            }

            // 2. Topic Filter Chips
            item {
                Text(
                    text = "CANALI TEMATICI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedDark,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                TopicChipsRow(
                    selectedTopic = uiState.selectedTopic,
                    onSelectTopic = { viewModel.selectTopic(it) }
                )
            }

            // 3. Sentiment Filters & Active Search Badge
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SentimentFilterBar(
                        selectedSentiment = uiState.selectedSentimentFilter,
                        onSelectSentiment = { viewModel.setSentimentFilter(it) }
                    )
                }
            }

            // 4. Error banner if any
            if (uiState.errorMessage != null) {
                item {
                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                fontSize = 12.sp,
                                color = TextPrimaryDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Section Header with count and timestamp
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredItems.size} NOTIZIE & REPORT DI MERCATO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )

                    val timeFormatted = remember(uiState.report.lastUpdatedTimestamp) {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(uiState.report.lastUpdatedTimestamp))
                    }
                    Text(
                        text = "Aggiornato: $timeFormatted",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                }
            }

            // 6. Headline items list
            if (filteredItems.isEmpty()) {
                item {
                    Surface(
                        color = SurfaceCardLight,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nessuna notizia trovata con i filtri attuali",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Prova a selezionare 'Tutte le Notizie' o reimpostare i filtri sentiment.",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    val isBookmarked = uiState.bookmarkedInsightIds.contains(item.id)
                    MarketInsightCard(
                        item = item,
                        isBookmarked = isBookmarked,
                        onBookmarkToggle = { viewModel.toggleBookmark(item.id) },
                        onCardClick = { viewModel.setSelectedInsight(item) },
                        onOpenUrl = { url ->
                            if (url.isNotBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Impossibile aprire il link", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Nessun link esterno disponibile", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Detail Bottom Sheet
    if (uiState.selectedInsightForDetail != null) {
        val item = uiState.selectedInsightForDetail!!
        MarketInsightDetailSheet(
            item = item,
            isBookmarked = uiState.bookmarkedInsightIds.contains(item.id),
            onBookmarkToggle = { viewModel.toggleBookmark(item.id) },
            onDismiss = { viewModel.setSelectedInsight(null) },
            onOpenUrl = { url ->
                if (url.isNotBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Impossibile aprire il link", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Grounding Sources Sheet
    if (uiState.isGroundingSheetOpen) {
        GroundingSourcesSheet(
            report = uiState.report,
            onDismiss = { viewModel.setGroundingSheetOpen(false) },
            onOpenUrl = { url ->
                if (url.isNotBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Impossibile aprire il link", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun MacroIndicatorsRibbon(indicators: List<MacroEconomicIndicator>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("macro_indicators_row")
    ) {
        items(indicators) { ind ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceCardLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier
                    .width(180.dp)
                    .testTag("macro_card_${ind.label.take(8)}")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = ind.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ind.value,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanAccent
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (ind.isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = ind.trendDelta,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ind.isPositive) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ind.description,
                        fontSize = 10.sp,
                        color = TextMutedDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TopicChipsRow(
    selectedTopic: MarketInsightTopic,
    onSelectTopic: (MarketInsightTopic) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("topic_chips_row")
    ) {
        items(MarketInsightTopic.values()) { topic ->
            val isSelected = selectedTopic == topic
            FilterChip(
                selected = isSelected,
                onClick = { onSelectTopic(topic) },
                label = {
                    Text(
                        text = topic.titleIt,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    val icon = when (topic) {
                        MarketInsightTopic.ALL -> Icons.Default.Newspaper
                        MarketInsightTopic.MORTGAGE_RATES -> Icons.AutoMirrored.Filled.TrendingUp
                        MarketInsightTopic.AUCTIONS_NPL -> Icons.Default.Gavel
                        MarketInsightTopic.HOTSPOTS_CITIES -> Icons.Default.LocationCity
                        MarketInsightTopic.REGULATION_TAX -> Icons.Default.Eco
                        MarketInsightTopic.YIELDS_INVESTING -> Icons.Default.Paid
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BentoPurpleContainer,
                    selectedLabelColor = BentoPurpleOnContainer,
                    selectedLeadingIconColor = BentoPurpleOnContainer,
                    containerColor = SurfaceCardLight,
                    labelColor = TextPrimaryDark,
                    iconColor = TextSecondaryDark
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = SurfaceCardBorder,
                    selectedBorderColor = CyanAccent
                ),
                modifier = Modifier.testTag("topic_chip_${topic.key}")
            )
        }
    }
}

@Composable
fun SentimentFilterBar(
    selectedSentiment: MarketSentiment?,
    onSelectSentiment: (MarketSentiment?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Sentiment:",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMutedDark,
            modifier = Modifier.padding(end = 2.dp)
        )

        // All Sentiment chip
        val isAllSelected = selectedSentiment == null
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isAllSelected) BentoPurpleHeader else SurfaceCardLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isAllSelected) CyanAccent else SurfaceCardBorder),
            modifier = Modifier
                .clickable { onSelectSentiment(null) }
                .testTag("sentiment_all")
        ) {
            Text(
                text = "Tutti",
                fontSize = 11.sp,
                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isAllSelected) BentoPurpleOnContainer else TextSecondaryDark,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Specific Sentiments
        MarketSentiment.values().forEach { sent ->
            val isSelected = selectedSentiment == sent
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(sent.colorHex).copy(alpha = 0.15f) else SurfaceCardLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(sent.colorHex) else SurfaceCardBorder),
                modifier = Modifier
                    .clickable { onSelectSentiment(sent) }
                    .testTag("sentiment_${sent.key}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(sent.colorHex))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sent.labelIt,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(sent.colorHex) else TextSecondaryDark
                    )
                }
            }
        }
    }
}

@Composable
fun MarketInsightCard(
    item: MarketInsightItem,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onCardClick: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("market_insight_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top metadata bar: Source, Date, Sentiment, Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BentoPurpleHeader
                    ) {
                        Text(
                            text = item.sourcePublication,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPurpleOnContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "• ${item.publishedDateStr}",
                        fontSize = 11.sp,
                        color = TextMutedDark,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sentiment Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(item.sentiment.colorHex).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = item.sentiment.labelIt,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(item.sentiment.colorHex),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("bookmark_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Salva",
                            tint = if (isBookmarked) CyanAccent else TextMutedDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.testTag("insight_title_${item.id}")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Summary
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondaryDark,
                    lineHeight = 18.sp
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Key Takeaways preview if available
            if (item.keyTakeaways.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF7F5FA),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "💡 IMPLICAZIONI INVESTITORE:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        item.keyTakeaways.take(2).forEach { point ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "›",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = point,
                                    fontSize = 11.sp,
                                    color = TextPrimaryDark,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Action Row: Region tag, Impact rating, External link action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFECE6F0)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = item.regionTag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE8DEF8)
                    ) {
                        Text(
                            text = "Impatto ${item.impactRating}/10",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BentoPurpleOnContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.primaryUrl.isNotBlank()) {
                        TextButton(
                            onClick = { onOpenUrl(item.primaryUrl) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = if (item.webDomain.isNotBlank()) item.webDomain else "Fonte",
                                fontSize = 11.sp,
                                color = CyanAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    TextButton(
                        onClick = onCardClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "Dettagli",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketInsightDetailSheet(
    item: MarketInsightItem,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCardLight,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("insight_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: Source, Topic, Bookmark, Sentiment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BentoPurpleContainer
                ) {
                    Text(
                        text = "${item.sourcePublication} • ${item.topic.titleIt}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoPurpleOnContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(item.sentiment.colorHex).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.sentiment.labelIt,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(item.sentiment.colorHex),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onBookmarkToggle) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Salva",
                            tint = if (isBookmarked) CyanAccent else TextMutedDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    lineHeight = 24.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = TextMutedDark,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pubblicato: ${item.publishedDateStr} • Ambito: ${item.regionTag}",
                    fontSize = 12.sp,
                    color = TextMutedDark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Full Summary
            Text(
                text = "PANORAMICA ANALITICA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMutedDark,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimaryDark,
                    lineHeight = 20.sp
                )
            )

            // Key Takeaways Box
            if (item.keyTakeaways.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BentoPurpleHeader,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = BentoPurpleOnContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CONCLUSIONI STRATEGICHE INVESTITORE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoPurpleOnContainer,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        item.keyTakeaways.forEach { takeaway ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = takeaway,
                                    fontSize = 12.sp,
                                    color = TextPrimaryDark,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Chiudi", color = TextSecondaryDark)
                }

                if (item.primaryUrl.isNotBlank()) {
                    Button(
                        onClick = { onOpenUrl(item.primaryUrl) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apri Articolo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroundingSourcesSheet(
    report: MarketInsightsReport,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCardLight,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("grounding_sources_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Fonti Web & Google Search Grounding",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    )
                    Text(
                        text = "Trasparenza delle fonti e query eseguite dal modello Gemini",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search queries executed
            if (report.searchQueriesExecuted.isNotEmpty()) {
                Text(
                    text = "QUERY DI RICERCA ESEGUITE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedDark,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF5F2F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        report.searchQueriesExecuted.forEach { q ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🔍",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = q,
                                    fontSize = 12.sp,
                                    color = TextPrimaryDark
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Grounding sources
            Text(
                text = "FONTI WEB VERIFICATE (${report.groundingSources.size})",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMutedDark,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(report.groundingSources) { src ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceCardLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenUrl(src.uri) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = src.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (src.domain.isNotBlank()) {
                                    Text(
                                        text = src.domain,
                                        fontSize = 10.sp,
                                        color = CyanAccent
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Chiudi", fontWeight = FontWeight.Bold)
            }
        }
    }
}
