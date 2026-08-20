package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import com.example.util.ImageUtils
import com.example.util.ImmobiliareObservatoryService
import java.text.NumberFormat
import java.util.Locale

import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import com.example.util.AuctionDateStatus
import com.example.util.AuctionDateUtils

import androidx.compose.material.icons.filled.Compare

@Composable
fun DealCard(
    deal: PropertyDeal,
    onCardClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onCalculateClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectedForCompare: Boolean = false,
    onCompareToggle: (() -> Unit)? = null,
    investorProfile: com.example.data.InvestorProfile? = null,
    onUnlockClick: (() -> Unit)? = null,
    onOfferClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isBlind = com.example.util.BlindModeUtils.isDealBlind(deal.id, investorProfile)
    val displayTitle = com.example.util.BlindModeUtils.getMaskedTitle(deal, isBlind)
    val displayLocation = com.example.util.BlindModeUtils.getMaskedLocation(deal, isBlind)
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
        maximumFractionDigits = 0
    }

    val bookmarkTint by animateColorAsState(
        targetValue = if (deal.isBookmarked) AmberGold else TextMutedDark,
        label = "bookmarkTint"
    )

    val strings = com.example.util.LocalAppStrings.current
    val auctionStatus = AuctionDateUtils.getStatus(deal.auctionDate)

    val predictiveEval = remember(deal.id, deal.askingPrice, deal.surfaceSqm) {
        com.example.util.PredictiveDealAlertEngine.evaluatePropertyDeal(deal)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.dp,
                if (isSelectedForCompare) CyanAccent else SurfaceCardBorder,
                RoundedCornerShape(24.dp)
            )
            .clickable { onCardClick() }
            .testTag("deal_card_${deal.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image Banner with Badges Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = ImageUtils.buildOptimizedImageRequest(
                        context = context,
                        data = deal.imageUrl.ifBlank { "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=800" },
                        targetWidthPx = 600,
                        targetHeightPx = 360
                    ),
                    contentDescription = deal.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark Gradient for legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                startY = 100f
                            )
                        )
                )

                // Top Right: Compare & Bookmark Buttons Row
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onCompareToggle != null) {
                        IconButton(
                            onClick = onCompareToggle,
                            modifier = Modifier
                                .background(if (isSelectedForCompare) CyanAccent else Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(36.dp)
                                .testTag("compare_button_${deal.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = "Confronta Immobile",
                                tint = if (isSelectedForCompare) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                            .testTag("bookmark_button_${deal.id}")
                    ) {
                        Icon(
                            imageVector = if (deal.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark Deal",
                            tint = bookmarkTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Top Left: Source Provider & AI Score Badge & Demo Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val provenanceEnum = com.example.data.DataProvenance.fromString(deal.provenance)
                    if (!provenanceEnum.isTrustworthy) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RoseRed,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = provenanceEnum.label.uppercase(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(CyanAccent, CircleShape)
                            )
                            Text(
                                text = deal.sourceName,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Quantum AI Score Badge
                    val aiScore = String.format("%.1f", 7.5 + (deal.discountPercent * 0.06).coerceAtMost(2.3))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PurpleIndigo.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "AI $aiScore/10",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (deal.isBookmarked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberGold
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (strings.isItalian) "In Osservazione" else "Bookmarked",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (deal.priceAlertThreshold != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyanAccent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "🔔 ≤ ${currencyFormat.format(deal.priceAlertThreshold)}",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Bottom Left: Discount & Cap Rate Badges
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Discount Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldGreen
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "-${deal.discountPercent}% Stima",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Yield Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AmberGold
                    ) {
                        Text(
                            text = "${deal.estimatedCapRate}% Yield",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (predictiveEval.isRankingAvailable && predictiveEval.isTop10Percentile) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF06B6D4)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "⭐ TOP 10% DEAL",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // Card Body Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = displayTitle,
                    color = if (isBlind) AmberGold else TextPrimaryDark,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Location & Surface Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isBlind) Icons.Default.Lock else Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isBlind) AmberGold else CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = displayLocation,
                            color = if (isBlind) TextMutedDark else TextSecondaryDark,
                            fontSize = 13.sp
                        )
                    }

                    Text(text = "•", color = TextMutedDark, fontSize = 12.sp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SquareFoot,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(14.dp)
                        )
                        val pricePerSqm = if (deal.surfaceSqm > 0) (deal.askingPrice / deal.surfaceSqm).toInt() else 0
                        Text(
                            text = "${deal.surfaceSqm} m² (€${pricePerSqm}/m²)",
                            color = TextSecondaryDark,
                            fontSize = 13.sp
                        )
                    }
                }

                // Benchmark Chip with Provenance & Generic Fallback Indicator
                val liveMarket = remember(deal.location) { ImmobiliareObservatoryService.findMarketData(deal.location) }
                val dealPricePerSqm = if (deal.surfaceSqm > 0) deal.askingPrice / deal.surfaceSqm else 0.0
                val spreadVsZonePct = if (liveMarket.avgSalePricePerSqM > 0 && dealPricePerSqm > 0) {
                    ((dealPricePerSqm - liveMarket.avgSalePricePerSqM) / liveMarket.avgSalePricePerSqM) * 100.0
                } else 0.0

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BentoCardBgLight,
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, CyanAccent.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "📍 ${liveMarket.municipalityName}: €${liveMarket.avgSalePricePerSqM.toInt()}/m²",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondaryDark
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CyanAccent.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CyanAccent.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = liveMarket.provenance,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (spreadVsZonePct < 0) "-${String.format(Locale.ITALY, "%.1f", -spreadVsZonePct)}% vs Media"
                                       else "+${String.format(Locale.ITALY, "%.1f", spreadVsZonePct)}% vs Media",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (spreadVsZonePct < 0) EmeraldGreen else AmberGold
                            )
                        }

                        if (liveMarket.isGenericFallback) {
                            Text(
                                text = "⚠️ Nessun dato disponibile per questo comune: valore nazionale generico",
                                fontSize = 9.sp,
                                color = AmberGold,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = SurfaceCardBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Price Row: Asking Price vs Market Valuation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (deal.propertyType.contains("Asta", ignoreCase = true)) "Offerta Base Asta" else "Prezzo Richiesta",
                            color = TextMutedDark,
                            fontSize = 11.sp
                        )
                        Text(
                            text = currencyFormat.format(deal.askingPrice),
                            color = EmeraldGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Valore di Mercato",
                            color = TextMutedDark,
                            fontSize = 11.sp
                        )
                        Text(
                            text = currencyFormat.format(deal.estimatedMarketValue),
                            color = TextSecondaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = androidx.compose.ui.text.TextStyle(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                if (isBlind) {
                                    onUnlockClick?.invoke() ?: onCardClick()
                                } else {
                                    try {
                                        val url = if (deal.sourceUrl.startsWith("http://") || deal.sourceUrl.startsWith("https://")) {
                                            deal.sourceUrl
                                        } else {
                                            "https://${deal.sourceUrl}"
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Impossibile aprire il link della fonte", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .background(if (isBlind) AmberGold.copy(alpha = 0.2f) else Color(0xFF0F766E).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .border(1.dp, if (isBlind) AmberGold.copy(alpha = 0.6f) else EmeraldGreen.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .size(38.dp)
                                .testTag("open_source_link_button_${deal.id}")
                        ) {
                            Icon(
                                imageVector = if (isBlind) Icons.Default.Lock else Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = if (isBlind) "Dati Riservati" else "Contatta / Apri Fonte Origine",
                                tint = if (isBlind) AmberGold else EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onCalculateClick,
                            modifier = Modifier
                                .background(Color(0xFF0369A1).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .size(38.dp)
                                .testTag("calc_roi_button_${deal.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Calcola ROI",
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Blind Action or Direct Offer Button Row
                Spacer(modifier = Modifier.height(10.dp))
                if (isBlind) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmberGold.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUnlockClick?.invoke() ?: onCardClick() }
                            .testTag("card_unlock_blind_cta_${deal.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                                Text("Modalità Blind: Sblocca per dati & offerta", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Sblocca →", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldGreen.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOfferClick?.invoke() ?: onCardClick() }
                            .testTag("card_submit_offer_cta_${deal.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Gavel, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                Text("Dossier Completo Sbloccato", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Fai Offerta 💼", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // Auction Date notice if present
                if (!deal.auctionDate.isNull_or_blank_safe()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val notice = when (auctionStatus) {
                        is AuctionDateStatus.Expired -> NoticeInfo(
                            RoseRed.copy(alpha = 0.25f),
                            RoseRed,
                            Icons.Default.Warning,
                            if (strings.isItalian) "⛔ ASTA SCADUTA (${auctionStatus.dateFormatted}) - Non Partecipabile" else "⛔ AUCTION EXPIRED (${auctionStatus.dateFormatted}) - Closed"
                        )
                        is AuctionDateStatus.Today -> NoticeInfo(
                            RoseRed,
                            Color.White,
                            Icons.Default.Gavel,
                            if (strings.isItalian) "🚨 ASTA IN SCADENZA OGGI (${auctionStatus.dateFormatted})" else "🚨 AUCTION ENDS TODAY (${auctionStatus.dateFormatted})"
                        )
                        is AuctionDateStatus.Upcoming -> NoticeInfo(
                            Color(0xFF1E1B4B),
                            Color(0xFFC7D2FE),
                            Icons.Default.Gavel,
                            if (strings.isItalian) "⏳ Termine Offerta: ${auctionStatus.dateFormatted} (${auctionStatus.daysLeft} gg rimasti)" else "⏳ Bidding Deadline: ${auctionStatus.dateFormatted} (${auctionStatus.daysLeft} days left)"
                        )
                        else -> NoticeInfo(
                            Color(0xFF1E1B4B),
                            Color(0xFFC7D2FE),
                            Icons.Default.Gavel,
                            if (strings.isItalian) "Termine Offerta: ${deal.auctionDate}" else "Bidding Deadline: ${deal.auctionDate}"
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = notice.bgColor
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = notice.icon,
                                contentDescription = null,
                                tint = notice.textColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = notice.label,
                                color = notice.textColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class NoticeInfo(
    val bgColor: Color,
    val textColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

private fun String?.isNull_or_blank_safe(): Boolean {
    return this == null || this.trim().isEmpty()
}
