package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.DealStage
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import com.example.util.ImageUtils
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealKanbanBoard(
    deals: List<PropertyDeal>,
    savedDealsOnly: Boolean = false,
    onStageChange: (dealId: Long, newStage: String) -> Unit,
    onDealClick: (PropertyDeal) -> Unit,
    onBookmarkToggle: (PropertyDeal) -> Unit,
    onCalculateClick: (PropertyDeal) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 } }
    
    var filterSavedOnly by remember { mutableStateOf(savedDealsOnly) }
    var searchQuery by remember { mutableStateOf("") }

    val displayDeals = remember(deals, filterSavedOnly, searchQuery) {
        val searchTokens = searchQuery.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        deals.filter { deal ->
            val matchesSaved = !filterSavedOnly || deal.isBookmarked
            val matchesSearch = if (searchTokens.isEmpty()) {
                true
            } else {
                val searchableText = listOf(
                    deal.title,
                    deal.location,
                    deal.sourceName,
                    deal.propertyType,
                    deal.notes,
                    deal.status,
                    deal.dealStage,
                    "€${deal.askingPrice.toInt()}",
                    "${deal.askingPrice.toInt()}"
                ).joinToString(" ").lowercase()

                searchTokens.all { token -> searchableText.contains(token) }
            }
            matchesSaved && matchesSearch
        }
    }

    // Pipeline Volume Metrics
    val totalPipelineValue = displayDeals.sumOf { it.askingPrice }
    val prospectingDeals = displayDeals.filter { DealStage.fromKey(it.dealStage) == DealStage.PROSPECTING }
    val underContractDeals = displayDeals.filter { DealStage.fromKey(it.dealStage) == DealStage.UNDER_CONTRACT }
    val closingDeals = displayDeals.filter { DealStage.fromKey(it.dealStage) == DealStage.CLOSING }
    val closedDeals = displayDeals.filter { DealStage.fromKey(it.dealStage) == DealStage.CLOSED }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("deal_kanban_board"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Pipeline Summary Header Card ---
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceCardDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
            modifier = Modifier.fillMaxWidth()
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
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewColumn,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Pipeline Kanban Immobili",
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Gestione delle trattative per fase di avanzamento",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }

                    // Saved vs All Toggle Chip Row
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSlateBg)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (filterSavedOnly) CyanAccent.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { filterSavedOnly = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("kanban_toggle_saved"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Salvati (${deals.count { it.isBookmarked }})",
                                fontSize = 11.sp,
                                fontWeight = if (filterSavedOnly) FontWeight.Bold else FontWeight.Normal,
                                color = if (filterSavedOnly) CyanAccent else TextMutedDark
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!filterSavedOnly) CyanAccent.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { filterSavedOnly = false }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("kanban_toggle_all"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tutti (${deals.size})",
                                fontSize = 11.sp,
                                fontWeight = if (!filterSavedOnly) FontWeight.Bold else FontWeight.Normal,
                                color = if (!filterSavedOnly) CyanAccent else TextMutedDark
                            )
                        }
                    }
                }

                // Volume Breakdown Metric Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KanbanSummaryBadge(
                        label = "Totale Pipeline",
                        value = currencyFormat.format(totalPipelineValue),
                        accentColor = CyanAccent,
                        icon = Icons.AutoMirrored.Filled.TrendingUp
                    )
                    KanbanSummaryBadge(
                        label = "Prospecting",
                        value = "${prospectingDeals.size} (${currencyFormat.format(prospectingDeals.sumOf { it.askingPrice })})",
                        accentColor = CyanAccent,
                        icon = Icons.Default.Search
                    )
                    KanbanSummaryBadge(
                        label = "In Trattativa",
                        value = "${underContractDeals.size} (${currencyFormat.format(underContractDeals.sumOf { it.askingPrice })})",
                        accentColor = AmberGold,
                        icon = Icons.Default.Handshake
                    )
                    KanbanSummaryBadge(
                        label = "In Chiusura",
                        value = "${closingDeals.size} (${currencyFormat.format(closingDeals.sumOf { it.askingPrice })})",
                        accentColor = Color(0xFFBA68C8),
                        icon = Icons.Default.HourglassTop
                    )
                    KanbanSummaryBadge(
                        label = "Acquisito",
                        value = "${closedDeals.size} (${currencyFormat.format(closedDeals.sumOf { it.askingPrice })})",
                        accentColor = EmeraldGreen,
                        icon = Icons.Default.CheckCircle
                    )
                }

                // Search Filter TextField inside Kanban
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filtra per titolo, luogo o tipologia...", color = TextMutedDark, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMutedDark) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Pulisci", tint = TextMutedDark)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("kanban_search_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSlateBg,
                        unfocusedContainerColor = DarkSlateBg,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    singleLine = true
                )
            }
        }

        // --- Kanban Columns Horizontal Board ---
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item {
                KanbanColumn(
                    stage = DealStage.PROSPECTING,
                    deals = prospectingDeals,
                    accentColor = CyanAccent,
                    icon = Icons.Default.Radar,
                    onStageChange = onStageChange,
                    onDealClick = onDealClick,
                    onBookmarkToggle = onBookmarkToggle,
                    onCalculateClick = onCalculateClick
                )
            }

            item {
                KanbanColumn(
                    stage = DealStage.UNDER_CONTRACT,
                    deals = underContractDeals,
                    accentColor = AmberGold,
                    icon = Icons.Default.Handshake,
                    onStageChange = onStageChange,
                    onDealClick = onDealClick,
                    onBookmarkToggle = onBookmarkToggle,
                    onCalculateClick = onCalculateClick
                )
            }

            item {
                KanbanColumn(
                    stage = DealStage.CLOSING,
                    deals = closingDeals,
                    accentColor = Color(0xFFBA68C8),
                    icon = Icons.Default.HourglassTop,
                    onStageChange = onStageChange,
                    onDealClick = onDealClick,
                    onBookmarkToggle = onBookmarkToggle,
                    onCalculateClick = onCalculateClick
                )
            }

            item {
                KanbanColumn(
                    stage = DealStage.CLOSED,
                    deals = closedDeals,
                    accentColor = EmeraldGreen,
                    icon = Icons.Default.CheckCircle,
                    onStageChange = onStageChange,
                    onDealClick = onDealClick,
                    onBookmarkToggle = onBookmarkToggle,
                    onCalculateClick = onCalculateClick
                )
            }
        }
    }
}

@Composable
private fun KanbanSummaryBadge(
    label: String,
    value: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSlateBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
            Column {
                Text(text = label, color = TextSecondaryDark, fontSize = 9.sp, fontWeight = FontWeight.Normal)
                Text(text = value, color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun KanbanColumn(
    stage: DealStage,
    deals: List<PropertyDeal>,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onStageChange: (dealId: Long, newStage: String) -> Unit,
    onDealClick: (PropertyDeal) -> Unit,
    onBookmarkToggle: (PropertyDeal) -> Unit,
    onCalculateClick: (PropertyDeal) -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 } }
    val columnSum = deals.sumOf { it.askingPrice }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .testTag("kanban_column_${stage.key.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Column Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                        }
                    }

                    Column {
                        Text(
                            text = stage.labelIt,
                            color = TextPrimaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stage.description,
                            color = TextMutedDark,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Deal Count Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${deals.size}",
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Subtotal row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Valore Totale:", color = TextSecondaryDark, fontSize = 11.sp)
                Text(currencyFormat.format(columnSum), color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Divider(color = SurfaceCardBorder, thickness = 1.dp)

            // Items List or Empty State
            if (deals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSlateBg)
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Nessun immobile in questa fase",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Utilizza i pulsanti di cambio fase sulle schede per sposta opportunita qui.",
                            color = TextMutedDark,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(deals, key = { it.id }) { deal ->
                        KanbanDealCard(
                            deal = deal,
                            currentStage = stage,
                            accentColor = accentColor,
                            onStageChange = onStageChange,
                            onDealClick = onDealClick,
                            onBookmarkToggle = onBookmarkToggle,
                            onCalculateClick = onCalculateClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KanbanDealCard(
    deal: PropertyDeal,
    currentStage: DealStage,
    accentColor: Color,
    onStageChange: (dealId: Long, newStage: String) -> Unit,
    onDealClick: (PropertyDeal) -> Unit,
    onBookmarkToggle: (PropertyDeal) -> Unit,
    onCalculateClick: (PropertyDeal) -> Unit
) {
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDealClick(deal) }
            .testTag("kanban_deal_card_${deal.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Optional Thumbnail Image
            if (deal.imageUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageUtils.buildOptimizedImageRequest(
                            context = context,
                            data = deal.imageUrl,
                            targetWidthPx = 320,
                            targetHeightPx = 180
                        ),
                        contentDescription = deal.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )

                    // Discount Pill Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RoseRed,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "-${deal.discountPercent}%",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Bookmark Star Action
                    IconButton(
                        onClick = { onBookmarkToggle(deal) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (deal.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Salva",
                            tint = if (deal.isBookmarked) AmberGold else Color.White
                        )
                    }
                }
            }

            // Title & Location
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = deal.title,
                        color = TextPrimaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (deal.imageUrl.isBlank()) {
                        IconButton(
                            onClick = { onBookmarkToggle(deal) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (deal.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Salva",
                                tint = if (deal.isBookmarked) AmberGold else TextMutedDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "📍 ${deal.location} • ${deal.surfaceSqm} m²",
                    color = TextSecondaryDark,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Price & Cap Rate Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = currencyFormat.format(deal.askingPrice),
                        color = CyanAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Stima: ${currencyFormat.format(deal.estimatedMarketValue)}",
                        color = TextMutedDark,
                        fontSize = 9.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${deal.estimatedCapRate}% Yield",
                        color = EmeraldGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Action: Open ROI Calculator button
            OutlinedButton(
                onClick = { onCalculateClick(deal) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Calcola ROI / Simula", color = TextSecondaryDark, fontSize = 10.sp)
            }

            Divider(color = SurfaceCardBorder, thickness = 1.dp)

            // --- Quick Move Pipeline Stage Chips ---
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Sposta Fase Pipeline:",
                    color = TextMutedDark,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DealStage.values().forEach { targetStage ->
                        if (targetStage != currentStage) {
                            val stageColor = when (targetStage) {
                                DealStage.PROSPECTING -> CyanAccent
                                DealStage.UNDER_CONTRACT -> AmberGold
                                DealStage.CLOSING -> Color(0xFFBA68C8)
                                DealStage.CLOSED -> EmeraldGreen
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = stageColor.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, stageColor.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onStageChange(deal.id, targetStage.key)
                                        Toast
                                            .makeText(
                                                context,
                                                "Spostato in '${targetStage.labelIt}'",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = stageColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = targetStage.labelIt,
                                        color = stageColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
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
