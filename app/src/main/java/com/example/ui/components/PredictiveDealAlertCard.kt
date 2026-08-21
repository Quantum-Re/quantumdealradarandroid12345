package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Property
import com.example.ui.theme.*
import com.example.util.PredictiveDealAlertEngine
import com.example.util.PredictiveDealEvaluation
import com.example.util.PredictiveDealNotificationManager
import com.example.util.ProvinceScrapedKpi
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Material 3 Card showcasing predictive deal evaluation, percentile benchmarking,
 * and 12M/24M forecast models.
 */
@Composable
fun PredictiveDealAlertCard(
    property: Property,
    kpi: ProvinceScrapedKpi? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val euroFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
    }

    var simulatedPrice by remember(property.id, property.price) {
        mutableDoubleStateOf(property.price)
    }
    var isAlertsSubscribed by remember { mutableStateOf(true) }

    val evalProperty = remember(property, simulatedPrice) {
        property.copy(price = simulatedPrice)
    }

    val evaluation: PredictiveDealEvaluation = remember(evalProperty, kpi) {
        PredictiveDealAlertEngine.evaluateProperty(evalProperty, kpi)
    }

    val isRankingAvailable = evaluation.isRankingAvailable
    val isTop10 = evaluation.isTop10Percentile
    val percentile = evaluation.dealPercentile ?: 50.0

    // Colors based on percentile tier
    val accentColor = if (!isRankingAvailable) {
        Color(0xFF64748B) // Slate
    } else when {
        percentile >= 95.0 -> Color(0xFF10B981) // Emerald
        percentile >= 90.0 -> Color(0xFF06B6D4) // Cyan
        percentile >= 75.0 -> Color(0xFF3B82F6) // Blue
        percentile >= 50.0 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Red
    }

    val cardBorder = if (isTop10 && isRankingAvailable) {
        BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF06B6D4))))
    } else {
        BorderStroke(1.dp, SurfaceCardBorder)
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        border = cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .testTag("predictive_deal_alert_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar: Badge & Alert Toggle
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
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isTop10 && isRankingAvailable) Icons.Default.ElectricBolt else Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "PREDICTIVE DEAL RADAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Provincia di ${evaluation.provinceHistoricalStats.locationName} (${evaluation.provinceCode})",
                            fontSize = 12.sp,
                            color = TextSecondaryDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Notification Bell Action
                if (isRankingAvailable) {
                    IconButton(
                        onClick = {
                            PredictiveDealNotificationManager.sendTop10DealAlertNotification(
                                context = context,
                                evaluation = evaluation
                            )
                            Toast.makeText(
                                context,
                                "Notifica inviata: Allerta Top 10% Deal attivata per ${evaluation.location}!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceElevatedDark, CircleShape)
                            .testTag("predictive_alert_notification_btn")
                    ) {
                        Icon(
                            imageVector = if (isAlertsSubscribed) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = "Testa Notifica",
                            tint = if (isTop10) Color(0xFF10B981) else CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // If ranking not available, show clear notice
            if (!isRankingAvailable) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Ranking predittivo non disponibile: storico verificato insufficiente.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Per questa provincia non è presente una serie storica certificata (minimo 12 rilevazioni). Percentili e benchmark P90/P95 sono disattivati a tutela dell'integrità dei dati.",
                                fontSize = 11.sp,
                                color = TextSecondaryDark,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                // Central Percentile Gauge & Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceElevatedDark, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        evaluation.topTierRankLabel?.let { rankLabel ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = rankLabel,
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.0f°", percentile),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = accentColor
                            )
                            Text(
                                text = "Percentile Deal",
                                fontSize = 13.sp,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Text(
                            text = if (isTop10) {
                                "🔥 Supera il 90% degli annunci storici della provincia!"
                            } else {
                                "Posizionato al di sotto della soglia Top 10% (P90)"
                            },
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }

                    // Percentile Visual Gauge
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = (percentile / 100f).toFloat().coerceIn(0f, 1f),
                            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                            label = "percentile_arc"
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 7.dp.toPx()
                            // Background track
                            drawArc(
                                color = Color(0xFF334155),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            // Progress arc
                            drawArc(
                                brush = Brush.sweepGradient(listOf(Color(0xFF3B82F6), Color(0xFF06B6D4), Color(0xFF10B981))),
                                startAngle = 135f,
                                sweepAngle = 270f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isTop10) "TOP" else "P",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedDark
                            )
                            Text(
                                text = if (isTop10) "${(100 - percentile).roundToInt()}%" else "${percentile.roundToInt()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor
                            )
                        }
                    }
                }

                // Historical Yield Trajectory vs Property Yield Benchmark (only when stats available)
                if (evaluation.provinceHistoricalStats.isAvailable && evaluation.provinceHistoricalStats.historicalPoints.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONFRONTO YIELD STORICO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondaryDark,
                                letterSpacing = 0.5.sp
                            )

                            evaluation.provinceHistoricalStats.p90HistoricalYield?.let { p90 ->
                                Text(
                                    text = "Benchmark P90: ${String.format(Locale.ITALY, "%.2f%%", p90)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            }
                        }

                        HistoricalYieldComparisonChart(
                            stats = evaluation.provinceHistoricalStats,
                            propertyYield = evaluation.propertyImpliedGrossYield
                        )

                        // Key metrics row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Rendimento Immobile", fontSize = 11.sp, color = TextMutedDark)
                                Text(
                                    text = String.format(Locale.ITALY, "%.2f%% Lordo", evaluation.propertyImpliedGrossYield),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isTop10) EmeraldGreen else TextPrimaryDark
                                )
                            }

                            evaluation.provinceHistoricalStats.p50HistoricalYield?.let { p50 ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Media Storica (P50)", fontSize = 11.sp, color = TextMutedDark)
                                    Text(
                                        text = String.format(Locale.ITALY, "%.2f%%", p50),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondaryDark
                                    )
                                }
                            }

                            evaluation.yieldSpreadVsP90?.let { spread ->
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Alpha Spread vs P90", fontSize = 11.sp, color = TextMutedDark)
                                    val spreadSign = if (spread >= 0) "+" else ""
                                    Text(
                                        text = String.format(Locale.ITALY, "%s%.2f%%", spreadSign, spread),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (spread >= 0) EmeraldGreen else AmberGold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Predictive 12M / 24M Forecast Cards (Available based on trend rates)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 12M Forecast Box
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevatedDark),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Previsione 12M",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                        if (evaluation.predicted12mMarketValue != null) {
                            Text(
                                text = euroFormat.format(evaluation.predicted12mMarketValue),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Yield: ${String.format(Locale.ITALY, "%.1f%%", evaluation.predicted12mYield ?: 0.0)}",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        } else {
                            Text(
                                text = "Proiezione non disponibile: trend di mercato non verificati",
                                fontSize = 10.sp,
                                color = AmberGold,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                // 24M Forecast Box
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevatedDark),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Timeline,
                                contentDescription = null,
                                tint = PurpleIndigo,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Previsione 24M",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleIndigo
                            )
                        }
                        if (evaluation.predicted24mMarketValue != null) {
                            Text(
                                text = euroFormat.format(evaluation.predicted24mMarketValue),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimaryDark
                            )
                            val equitySign = if ((evaluation.predicted12mEquityGain ?: 0.0) >= 0) "+" else ""
                            Text(
                                text = "Equity: $equitySign${euroFormat.format(evaluation.predicted12mEquityGain ?: 0.0)}",
                                fontSize = 11.sp,
                                color = if ((evaluation.predicted12mEquityGain ?: 0.0) >= 0) EmeraldGreen else TextSecondaryDark
                            )
                        } else {
                            Text(
                                text = "Proiezione non disponibile: trend di mercato non verificati",
                                fontSize = 10.sp,
                                color = AmberGold,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }

            // Yield & Price Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevatedDark, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRankingAvailable) "🎯 SOGLIA PREZZO TOP 10%" else "📊 STIMA RENDIMENTO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryDark
                    )

                    if (isRankingAvailable && evaluation.targetPriceForTop10Percentile != null) {
                        Text(
                            text = "Target Max: ${euroFormat.format(evaluation.targetPriceForTop10Percentile)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTop10) EmeraldGreen else AmberGold
                        )
                    } else {
                        Text(
                            text = "Yield: ${String.format(Locale.ITALY, "%.2f%%", evaluation.propertyImpliedGrossYield)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanAccent
                        )
                    }
                }

                Text(
                    text = evaluation.actionableSummary,
                    fontSize = 12.sp,
                    color = TextPrimaryDark,
                    lineHeight = 16.sp
                )

                // Interactive Price Adjustment Slider
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Simula Prezzo Offerta:",
                            fontSize = 11.sp,
                            color = TextMutedDark
                        )
                        Text(
                            text = euroFormat.format(simulatedPrice),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }

                    val minRange = max(10000.0, property.price * 0.5)
                    val maxRange = property.price * 1.5

                    Slider(
                        value = simulatedPrice.toFloat(),
                        onValueChange = { simulatedPrice = it.toDouble() },
                        valueRange = minRange.toFloat()..maxRange.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAccent,
                            activeTrackColor = CyanAccent,
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoricalYieldComparisonChart(
    stats: com.example.util.ProvinceHistoricalYieldStats,
    propertyYield: Double
) {
    val points = stats.historicalPoints
    val p90 = stats.p90HistoricalYield ?: return

    val allYields = points.map { it.grossYieldPercent } + propertyYield + p90
    val minYield = max(1.0, (allYields.minOrNull() ?: 3.0) * 0.85)
    val maxYield = (allYields.maxOrNull() ?: 8.0) * 1.15
    val range = max(0.1, maxYield - minYield)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height - 20.dp.toPx()
            val bottomY = height

            // Draw P90 Benchmark dashed line
            val p90Y = bottomY - (((p90 - minYield) / range) * height).toFloat()
            drawLine(
                color = Color(0xFF06B6D4).copy(alpha = 0.7f),
                start = Offset(0f, p90Y),
                end = Offset(width, p90Y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Draw bars for historical years
            val totalBars = points.size + 1
            val barWidth = (width / totalBars) * 0.55f
            val spacing = width / totalBars

            points.forEachIndexed { index, point ->
                val x = index * spacing + (spacing - barWidth) / 2f
                val barHeight = (((point.grossYieldPercent - minYield) / range) * height).toFloat().coerceAtLeast(6f)
                val y = bottomY - barHeight

                drawRoundRect(
                    color = Color(0xFF475569),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }

            // Draw Property Implied Yield Bar (Glowing green/cyan)
            val propX = points.size * spacing + (spacing - barWidth) / 2f
            val propBarHeight = (((propertyYield - minYield) / range) * height).toFloat().coerceAtLeast(6f)
            val propY = bottomY - propBarHeight

            val propColor = if (propertyYield >= p90) Color(0xFF10B981) else Color(0xFFF59E0B)

            drawRoundRect(
                brush = Brush.verticalGradient(listOf(propColor, propColor.copy(alpha = 0.5f))),
                topLeft = Offset(propX, propY),
                size = Size(barWidth, propBarHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }

        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.yearLabel.take(4),
                    fontSize = 9.sp,
                    color = TextMutedDark,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = "IMMOBILE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (propertyYield >= p90) EmeraldGreen else AmberGold,
                textAlign = TextAlign.Center
            )
        }
    }
}
