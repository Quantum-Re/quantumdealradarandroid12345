package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import com.example.util.ImageUtils
import java.text.NumberFormat
import java.util.Locale

@Composable
fun QuickAccessDealsBar(
    recentlyViewedDeals: List<PropertyDeal>,
    savedDeals: List<PropertyDeal>,
    onDealClick: (PropertyDeal) -> Unit,
    onBookmarkToggle: (PropertyDeal) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Recently Viewed, 1 = Saved Deals
    var isExpanded by remember { mutableStateOf(true) }

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
            maximumFractionDigits = 0
        }
    }

    val displayedDeals = if (selectedTab == 0) recentlyViewedDeals else savedDeals

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row with Toggle & Collapse
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
                            .size(32.dp)
                            .background(
                                color = if (selectedTab == 0) CyanAccent.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.History else Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = if (selectedTab == 0) CyanAccent else EmeraldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Accesso Rapido Immobili",
                            color = TextPrimaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedTab == 0) "${recentlyViewedDeals.size} visti di recente" else "${savedDeals.size} salvati nei preferiti",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectedTab == 0 && recentlyViewedDeals.isNotEmpty()) {
                        IconButton(
                            onClick = onClearHistory,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Pulisci Cronologia",
                                tint = TextMutedDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("toggle_quick_access_expanded")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Segmented Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSlateBg, RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 0) CyanAccent else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = 0 }
                                .testTag("tab_recently_viewed")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) Color.Black else TextSecondaryDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Visti di Recente (${recentlyViewedDeals.size})",
                                    color = if (selectedTab == 0) Color.Black else TextSecondaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 1) EmeraldGreen else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = 1 }
                                .testTag("tab_saved_deals")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) Color.Black else TextSecondaryDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Preferiti (${savedDeals.size})",
                                    color = if (selectedTab == 1) Color.Black else TextSecondaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Content Horizontal Scroll or Empty State
                    if (displayedDeals.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSlateBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Default.HistoryToggleOff else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = TextMutedDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedTab == 0)
                                        "Nessun immobile visualizzato di recente. Clicca su un deal per tracciarlo."
                                    else
                                        "Nessun immobile salvato tra i preferiti. Tocca l'icona segnalibro per salvarlo.",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            displayedDeals.forEach { deal ->
                                CompactDealCard(
                                    deal = deal,
                                    currencyFormat = currencyFormat,
                                    onClick = { onDealClick(deal) },
                                    onBookmarkToggle = { onBookmarkToggle(deal) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDealCard(
    deal: PropertyDeal,
    currencyFormat: NumberFormat,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    val timeAgoText = remember(deal.lastViewedAt) {
        if (deal.lastViewedAt <= 0L) "Nuovo" else formatTimeAgo(deal.lastViewedAt)
    }

    val defaultImg = "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=800"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSlateBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("compact_deal_card_${deal.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCardDark)
            ) {
                val context = LocalContext.current
                AsyncImage(
                    model = ImageUtils.buildOptimizedImageRequest(
                        context = context,
                        data = deal.imageUrl.ifBlank { defaultImg },
                        targetWidthPx = 260,
                        targetHeightPx = 160
                    ),
                    contentDescription = deal.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Discount Chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldGreen,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "-${deal.discountPercent}%",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Bookmark Toggle
                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (deal.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = if (deal.isBookmarked) EmeraldGreen else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = deal.title,
                color = TextPrimaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = deal.location,
                color = TextSecondaryDark,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currencyFormat.format(deal.askingPrice),
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SurfaceCardDark
                ) {
                    Text(
                        text = timeAgoText,
                        color = TextMutedDark,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diffMs = System.currentTimeMillis() - timestamp
    val seconds = diffMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Ora"
        minutes < 60 -> "${minutes}m fa"
        hours < 24 -> "${hours}h fa"
        days < 7 -> "${days}g fa"
        else -> "${days}g fa"
    }
}
