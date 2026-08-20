package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.PropertyOpportunityEvaluation
import java.util.Locale

@Composable
fun PropertyOpportunityBanner(
    evaluations: Map<Long, PropertyOpportunityEvaluation>,
    isOnlyUndervaluedActive: Boolean,
    isRefreshingMarket: Boolean,
    onToggleUndervaluedFilter: () -> Unit,
    onRefreshMarketComps: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (evaluations.isEmpty()) return

    val evalList = evaluations.values
    val undervaluedDeals = evalList.filter { it.opportunityScore >= 70 || it.undervaluedPercent >= 15.0 }
    val maxScore = evalList.maxOfOrNull { it.opportunityScore } ?: 0
    val totalAlpha = evalList.filter { it.alphaEquityGain > 0 }.sumOf { it.alphaEquityGain }

    val gradientColors = listOf(
        Color(0xFF064E3B), // Deep Emerald
        Color(0xFF0F172A)  // Dark Slate
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(
            1.dp,
            if (isOnlyUndervaluedActive) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("opportunity_summary_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "REAL-TIME OPPORTUNITY RADAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFF10B981)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onRefreshMarketComps,
                        enabled = !isRefreshingMarket,
                        modifier = Modifier.size(28.dp).testTag("refresh_market_kpis_btn")
                    ) {
                        if (isRefreshingMarket) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Ricarica Quotazioni Live",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Metric Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Metric 1: Sottoquotati count
                Column {
                    Text(
                        text = "Sottoquotati (>15%)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${undervaluedDeals.size} su ${evalList.size}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (undervaluedDeals.isNotEmpty()) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Metric 2: Max Opportunity Score
                Column {
                    Text(
                        text = "Top Opportunity Score",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$maxScore",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = if (maxScore >= 80) Color(0xFF10B981) else Color(0xFF06B6D4)
                        )
                        Text(
                            text = "/100",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Metric 3: Extra Equity Potential
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Alpha Equity Margins",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+€${String.format(Locale.ITALY, "%,.0f", totalAlpha)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }

            // Quick Filter & Toggle Action
            FilterChip(
                selected = isOnlyUndervaluedActive,
                onClick = onToggleUndervaluedFilter,
                label = {
                    Text(
                        text = if (isOnlyUndervaluedActive)
                            "Mostrando solo Sottoquotati (${undervaluedDeals.size})"
                        else
                            "Evidenzia / Filtra solo Sottoquotati",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    if (isOnlyUndervaluedActive) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                    selectedLabelColor = Color(0xFF10B981),
                    selectedLeadingIconColor = Color(0xFF10B981)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isOnlyUndervaluedActive,
                    borderColor = if (isOnlyUndervaluedActive) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("filter_undervalued_chip")
            )
        }
    }
}
