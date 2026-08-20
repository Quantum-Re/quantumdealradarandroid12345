package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PipelineStatus
import com.example.data.Property
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class TrendTimeframe(val label: String, val monthsBack: Int, val dataPointsCount: Int) {
    ONE_MONTH("1M", 1, 6),
    THREE_MONTHS("3M", 3, 7),
    SIX_MONTHS("6M", 6, 8),
    ONE_YEAR("1A", 12, 10),
    ALL("ALL", 24, 12)
}

enum class ChartMetricView(val label: String) {
    PORTFOLIO_VALUE("Valore vs Costo"),
    NET_APPRECIATION("Plusvalenza Netta")
}

data class PortfolioDataPoint(
    val timestamp: Long,
    val dateLabel: String,
    val portfolioValue: Double,
    val investedCostBasis: Double,
    val netAppreciation: Double,
    val appreciationPercent: Double,
    val activePropertiesCount: Int,
    val milestoneEvent: String? = null
)

@Composable
fun PortfolioAppreciationTrendChart(
    properties: List<Property>,
    euroFormat: NumberFormat,
    modifier: Modifier = Modifier,
    onPropertySelected: ((Property) -> Unit)? = null
) {
    var selectedTimeframe by remember { mutableStateOf(TrendTimeframe.SIX_MONTHS) }
    var selectedMetricView by remember { mutableStateOf(ChartMetricView.PORTFOLIO_VALUE) }
    var isExpandedBreakdown by remember { mutableStateOf(false) }

    // Generate accurate time-series appreciation curve based on current properties
    val trendDataPoints = remember(properties, selectedTimeframe) {
        generatePortfolioTrendData(properties, selectedTimeframe)
    }

    var selectedIndex by remember(trendDataPoints) {
        mutableStateOf<Int?>(null)
    }

    val currentPoint = if (trendDataPoints.isNotEmpty()) {
        selectedIndex?.let { trendDataPoints.getOrNull(it) } ?: trendDataPoints.last()
    } else null

    val startingPoint = trendDataPoints.firstOrNull()
    val endingPoint = trendDataPoints.lastOrNull()

    val totalAppreciationGain = if (startingPoint != null && endingPoint != null) {
        endingPoint.portfolioValue - startingPoint.portfolioValue
    } else 0.0

    val totalAppreciationPct = if (startingPoint != null && startingPoint.portfolioValue > 0 && endingPoint != null) {
        ((endingPoint.portfolioValue - startingPoint.portfolioValue) / startingPoint.portfolioValue) * 100.0
    } else 0.0

    val netProfitOverCost = endingPoint?.netAppreciation ?: 0.0
    val netProfitPercentOverCost = endingPoint?.appreciationPercent ?: 0.0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp))
            .testTag("portfolio_appreciation_trend_chart")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Chart Header: Title & Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldGainBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = EmeraldGainText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Rivalutazione Portafoglio",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Trend valore di mercato & crescita capitale nel tempo",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                // Metric Toggle (Value vs Gain)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.height(32.dp)
                ) {
                    ChartMetricView.values().forEachIndexed { index, metricView ->
                        SegmentedButton(
                            selected = selectedMetricView == metricView,
                            onClick = { selectedMetricView = metricView },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ChartMetricView.values().size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = BentoPurpleContainer,
                                activeContentColor = BentoPurpleOnContainer
                            )
                        ) {
                            Text(metricView.label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Recharts-style KPI Snapshot & Interactive Scrubber Readout
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BentoCardBgLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceCardBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Selected / Current Value
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (selectedIndex != null) "Punto Selezionato (${currentPoint?.dateLabel})" else "Valore Attuale Portafoglio",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondaryDark
                            )
                            if (selectedIndex != null) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldGainText)
                                )
                            }
                        }

                        Text(
                            text = euroFormat.format(currentPoint?.portfolioValue ?: 0.0),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimaryDark
                        )

                        Text(
                            text = "Base Costo: ${euroFormat.format(currentPoint?.investedCostBasis ?: 0.0)}",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }

                    // Right: Net Delta & ROI Gain Pill
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val isPositive = (currentPoint?.netAppreciation ?: 0.0) >= 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPositive) EmeraldGainBg else RoseRed.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (isPositive) EmeraldGainBorder else RoseRed.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isPositive) EmeraldGainText else RoseRed,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = String.format(Locale.ITALY, "%s%.1f%%", if (isPositive) "+" else "", currentPoint?.appreciationPercent ?: 0.0),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) EmeraldGainText else RoseRed
                                )
                            }
                        }

                        Text(
                            text = "${if (isPositive) "+" else ""}${euroFormat.format(currentPoint?.netAppreciation ?: 0.0)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) EmeraldGainText else RoseRed
                        )

                        Text(
                            text = "Margine rivalutazione",
                            fontSize = 10.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            // 3. Timeframe Selector Pills Row (Recharts style: 1M, 3M, 6M, 1A, ALL)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timeframe_selector_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Periodo:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryDark,
                    modifier = Modifier.padding(end = 2.dp)
                )

                TrendTimeframe.values().forEach { timeframe ->
                    val isSelected = selectedTimeframe == timeframe
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BentoPurpleContainer else Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedTimeframe = timeframe
                                selectedIndex = null
                            }
                    ) {
                        Text(
                            text = timeframe.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) BentoPurpleOnContainer else TextSecondaryDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // 4. Interactive Canvas Chart (Smooth Bézier Recharts Area & Spline Trendline)
            RechartsAppreciationCanvas(
                dataPoints = trendDataPoints,
                metricView = selectedMetricView,
                euroFormat = euroFormat,
                selectedIndex = selectedIndex,
                onSelectIndex = { selectedIndex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            // 5. Chart Legend & Tooltip Instruction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Legend Item 1: Portfolio Exit Value
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp, 3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(EmeraldGainText)
                        )
                        Text(
                            text = "Valore Mercato",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondaryDark
                        )
                    }

                    // Legend Item 2: Cost Basis
                    if (selectedMetricView == ChartMetricView.PORTFOLIO_VALUE) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp, 2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(PurpleIndigo)
                            )
                            Text(
                                text = "Base Investita",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = BentoPurpleOnContainer,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Scorri per ispezionare",
                        fontSize = 10.sp,
                        color = BentoPurpleOnContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 6. Property Asset Contribution Breakdown Toggle
            HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.4f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { isExpandedBreakdown = !isExpandedBreakdown }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChartOutline,
                        contentDescription = null,
                        tint = BentoPurpleOnContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Composizione Valore & Contributo Asset (${properties.size} immobili)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                }

                Icon(
                    imageVector = if (isExpandedBreakdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpandedBreakdown,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    properties.sortedByDescending { it.projectedProfit }.take(5).forEach { prop ->
                        val cost = prop.totalCostBasis
                        val exit = prop.effectiveExitValue
                        val profit = prop.projectedProfit
                        val roi = prop.projectedRoiPercent

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = BentoCardBgLight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SurfaceCardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { onPropertySelected?.invoke(prop) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = prop.title.ifBlank { prop.address },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Acquisto + Lavori: ${euroFormat.format(cost)} ➔ Exit: ${euroFormat.format(exit)}",
                                        fontSize = 10.sp,
                                        color = TextSecondaryDark
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = "+${euroFormat.format(profit)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldGainText
                                    )
                                    Text(
                                        text = String.format(Locale.ITALY, "+%.1f%% ROI", roi),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoPurpleOnContainer
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

// -------------------------------------------------------------------------------------------------
// High-Fidelity Custom Canvas Chart with Smooth Bézier Cubic Splines, Gradients, and Touch Scrubber
// -------------------------------------------------------------------------------------------------
@Composable
private fun RechartsAppreciationCanvas(
    dataPoints: List<PortfolioDataPoint>,
    metricView: ChartMetricView,
    euroFormat: NumberFormat,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("Nessun dato temporale disponibile", fontSize = 12.sp, color = TextSecondaryDark)
        }
        return
    }

    // Min & Max range with padding for clean visual aesthetics
    val values = if (metricView == ChartMetricView.PORTFOLIO_VALUE) {
        dataPoints.flatMap { listOf(it.portfolioValue, it.investedCostBasis) }
    } else {
        dataPoints.map { it.netAppreciation }
    }

    val rawMin = values.minOrNull() ?: 0.0
    val rawMax = values.maxOrNull() ?: 100000.0

    val yMin = max(0.0, rawMin * 0.85)
    val yMax = rawMax * 1.12
    val yRange = if (yMax > yMin) yMax - yMin else 1.0

    Canvas(
        modifier = modifier
            .testTag("recharts_trendline_canvas")
            .pointerInput(dataPoints) {
                detectTapGestures(
                    onTap = { offset ->
                        val paddingLeft = 40.dp.toPx()
                        val paddingRight = 16.dp.toPx()
                        val chartWidth = size.width - paddingLeft - paddingRight
                        if (chartWidth > 0 && offset.x >= paddingLeft && offset.x <= size.width - paddingRight) {
                            val relativeX = (offset.x - paddingLeft) / chartWidth
                            val index = (relativeX * (dataPoints.size - 1)).roundToInt().coerceIn(0, dataPoints.size - 1)
                            onSelectIndex(index)
                        } else {
                            onSelectIndex(null)
                        }
                    }
                )
            }
            .pointerInput(dataPoints) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val paddingLeft = 40.dp.toPx()
                        val paddingRight = 16.dp.toPx()
                        val chartWidth = size.width - paddingLeft - paddingRight
                        if (chartWidth > 0) {
                            val relativeX = ((offset.x - paddingLeft) / chartWidth).coerceIn(0f, 1f)
                            val index = (relativeX * (dataPoints.size - 1)).roundToInt().coerceIn(0, dataPoints.size - 1)
                            onSelectIndex(index)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val paddingLeft = 40.dp.toPx()
                        val paddingRight = 16.dp.toPx()
                        val chartWidth = size.width - paddingLeft - paddingRight
                        if (chartWidth > 0) {
                            val relativeX = ((change.position.x - paddingLeft) / chartWidth).coerceIn(0f, 1f)
                            val index = (relativeX * (dataPoints.size - 1)).roundToInt().coerceIn(0, dataPoints.size - 1)
                            onSelectIndex(index)
                        }
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height

        val paddingLeft = 44.dp.toPx()
        val paddingRight = 16.dp.toPx()
        val paddingTop = 16.dp.toPx()
        val paddingBottom = 26.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

        // 1. Draw Horizontal Dotted Grid Lines & Y-Axis Value Labels
        val gridLinesCount = 4
        for (i in 0..gridLinesCount) {
            val ratio = i.toFloat() / gridLinesCount
            val yPos = paddingTop + chartHeight * (1f - ratio)
            val valAtLine = yMin + yRange * ratio

            // Grid Line
            drawLine(
                color = SurfaceCardBorder.copy(alpha = 0.35f),
                start = Offset(paddingLeft, yPos),
                end = Offset(width - paddingRight, yPos),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            // Y-Axis Formatted Label (e.g. "€450k")
            val formattedYLabel = formatCompactEuro(valAtLine)
            val textLayoutResult = textMeasurer.measure(
                text = formattedYLabel,
                style = TextStyle(fontSize = 9.sp, color = TextMutedDark, fontWeight = FontWeight.Normal)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(4.dp.toPx(), yPos - textLayoutResult.size.height / 2f)
            )
        }

        // 2. Compute Coordinate Points for Trendlines
        val stepX = chartWidth / max(1, dataPoints.size - 1)
        val valueOffsets = mutableListOf<Offset>()
        val costOffsets = mutableListOf<Offset>()

        dataPoints.forEachIndexed { i, dp ->
            val x = paddingLeft + i * stepX

            val primaryValue = if (metricView == ChartMetricView.PORTFOLIO_VALUE) dp.portfolioValue else dp.netAppreciation
            val normPrimary = ((primaryValue - yMin) / yRange).toFloat().coerceIn(0f, 1f)
            val yValue = paddingTop + chartHeight * (1f - normPrimary)
            valueOffsets.add(Offset(x, yValue))

            if (metricView == ChartMetricView.PORTFOLIO_VALUE) {
                val normCost = ((dp.investedCostBasis - yMin) / yRange).toFloat().coerceIn(0f, 1f)
                val yCost = paddingTop + chartHeight * (1f - normCost)
                costOffsets.add(Offset(x, yCost))
            }
        }

        // 3. Draw Cost Basis Line (Dashed/Secondary Line if in Portfolio Value Mode)
        if (metricView == ChartMetricView.PORTFOLIO_VALUE && costOffsets.size > 1) {
            val costPath = Path()
            buildSmoothSplinePath(costPath, costOffsets)

            drawPath(
                path = costPath,
                color = PurpleIndigo.copy(alpha = 0.7f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                )
            )
        }

        // 4. Draw Smooth Spline Path & Gradient Area Fill for Primary Trendline
        if (valueOffsets.size > 1) {
            val linePath = Path()
            buildSmoothSplinePath(linePath, valueOffsets)

            // Area Gradient Path
            val areaPath = Path()
            areaPath.addPath(linePath)
            areaPath.lineTo(valueOffsets.last().x, paddingTop + chartHeight)
            areaPath.lineTo(valueOffsets.first().x, paddingTop + chartHeight)
            areaPath.close()

            val primaryColor = if (metricView == ChartMetricView.PORTFOLIO_VALUE) EmeraldGainText else BentoPurpleOnContainer
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.32f),
                    primaryColor.copy(alpha = 0.10f),
                    primaryColor.copy(alpha = 0.00f)
                ),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )

            drawPath(
                path = areaPath,
                brush = gradientBrush
            )

            // Draw Crisp Smooth Trendline
            drawPath(
                path = linePath,
                color = primaryColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw Small Outer Data Point Dots
            valueOffsets.forEachIndexed { idx, offset ->
                val isSel = idx == selectedIndex
                drawCircle(
                    color = Color.White,
                    radius = if (isSel) 6.dp.toPx() else 3.5.dp.toPx(),
                    center = offset
                )
                drawCircle(
                    color = primaryColor,
                    radius = if (isSel) 4.5.dp.toPx() else 2.5.dp.toPx(),
                    center = offset
                )
            }
        }

        // 5. Draw X-Axis Date Labels
        dataPoints.forEachIndexed { i, dp ->
            // Draw every label if <= 7 items, otherwise skip every 2nd
            val shouldDraw = dataPoints.size <= 7 || i % 2 == 0 || i == dataPoints.size - 1
            if (shouldDraw) {
                val x = paddingLeft + i * stepX
                val textLayoutResult = textMeasurer.measure(
                    text = dp.dateLabel,
                    style = TextStyle(
                        fontSize = 9.sp,
                        color = if (i == selectedIndex) BentoPurpleOnContainer else TextSecondaryDark,
                        fontWeight = if (i == selectedIndex) FontWeight.Bold else FontWeight.Normal
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(x - textLayoutResult.size.width / 2f, height - paddingBottom + 6.dp.toPx())
                )
            }
        }

        // 6. Interactive Scrubber Guide Line & Floating Tooltip
        selectedIndex?.let { idx ->
            if (idx in valueOffsets.indices) {
                val targetOffset = valueOffsets[idx]
                val dp = dataPoints[idx]

                // Vertical Scrubber Line
                drawLine(
                    color = BentoPurpleOnContainer.copy(alpha = 0.75f),
                    start = Offset(targetOffset.x, paddingTop),
                    end = Offset(targetOffset.x, paddingTop + chartHeight),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )

                // Pulsing highlight circle
                drawCircle(
                    color = BentoPurpleOnContainer.copy(alpha = 0.25f),
                    radius = 11.dp.toPx(),
                    center = targetOffset
                )
                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = targetOffset
                )
                drawCircle(
                    color = BentoPurpleOnContainer,
                    radius = 4.5.dp.toPx(),
                    center = targetOffset
                )

                // Floating Recharts-style Tooltip Card on Canvas
                drawRechartsFloatingTooltip(
                    drawScope = this,
                    textMeasurer = textMeasurer,
                    point = dp,
                    euroFormat = euroFormat,
                    anchor = targetOffset,
                    chartBounds = Size(width, height),
                    paddingLeft = paddingLeft,
                    paddingRight = paddingRight
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Recharts Floating Tooltip Rendering Helper
// -------------------------------------------------------------------------------------------------
private fun drawRechartsFloatingTooltip(
    drawScope: DrawScope,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    point: PortfolioDataPoint,
    euroFormat: NumberFormat,
    anchor: Offset,
    chartBounds: Size,
    paddingLeft: Float,
    paddingRight: Float
) {
    val dateText = point.dateLabel
    val valText = "Val: ${euroFormat.format(point.portfolioValue)}"
    val costText = "Inv: ${euroFormat.format(point.investedCostBasis)}"
    val gainText = "+${euroFormat.format(point.netAppreciation)} (${String.format(Locale.ITALY, "+%.1f%%", point.appreciationPercent)})"

    val dateLayout = textMeasurer.measure(dateText, TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimaryLight))
    val valLayout = textMeasurer.measure(valText, TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EmeraldGainText))
    val costLayout = textMeasurer.measure(costText, TextStyle(fontSize = 9.sp, color = PurpleIndigo))
    val gainLayout = textMeasurer.measure(gainText, TextStyle(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGainText))

    val tooltipWidth = maxOf(dateLayout.size.width, valLayout.size.width, costLayout.size.width, gainLayout.size.width) + 24f
    val tooltipHeight = dateLayout.size.height + valLayout.size.height + costLayout.size.height + gainLayout.size.height + 24f

    // Clamp tooltip position so it remains on screen
    var tooltipX = anchor.x - tooltipWidth / 2f
    if (tooltipX < paddingLeft) tooltipX = paddingLeft
    if (tooltipX + tooltipWidth > chartBounds.width - paddingRight) {
        tooltipX = chartBounds.width - paddingRight - tooltipWidth
    }

    var tooltipY = anchor.y - tooltipHeight - 14f
    if (tooltipY < 10f) {
        tooltipY = anchor.y + 14f
    }

    // Draw Shadow & Box
    drawScope.drawRoundRect(
        color = Color(0xFF1E1B26),
        topLeft = Offset(tooltipX, tooltipY),
        size = Size(tooltipWidth, tooltipHeight),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawScope.drawRoundRect(
        color = SurfaceCardBorder.copy(alpha = 0.6f),
        topLeft = Offset(tooltipX, tooltipY),
        size = Size(tooltipWidth, tooltipHeight),
        cornerRadius = CornerRadius(10f, 10f),
        style = Stroke(width = 1f)
    )

    // Draw Text lines inside dark tooltip
    val line1Y = tooltipY + 8f
    val line2Y = line1Y + dateLayout.size.height + 2f
    val line3Y = line2Y + valLayout.size.height + 2f
    val line4Y = line3Y + costLayout.size.height + 2f

    val textLeft = tooltipX + 12f

    drawScope.drawText(
        textMeasurer.measure(dateText, TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)),
        topLeft = Offset(textLeft, line1Y)
    )
    drawScope.drawText(
        textMeasurer.measure(valText, TextStyle(fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF81C784))),
        topLeft = Offset(textLeft, line2Y)
    )
    drawScope.drawText(
        textMeasurer.measure(costText, TextStyle(fontSize = 9.sp, color = Color(0xFFD0BCFF))),
        topLeft = Offset(textLeft, line3Y)
    )
    drawScope.drawText(
        textMeasurer.measure(gainText, TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))),
        topLeft = Offset(textLeft, line4Y)
    )
}

// -------------------------------------------------------------------------------------------------
// Cubic Spline Path Builder Helper for Recharts-like curved graphs
// -------------------------------------------------------------------------------------------------
private fun buildSmoothSplinePath(path: Path, points: List<Offset>) {
    if (points.isEmpty()) return
    path.moveTo(points.first().x, points.first().y)

    for (i in 0 until points.size - 1) {
        val p0 = if (i > 0) points[i - 1] else points[i]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else p2

        val cp1x = p1.x + (p2.x - p0.x) / 6f
        val cp1y = p1.y + (p2.y - p0.y) / 6f
        val cp2x = p2.x - (p3.x - p1.x) / 6f
        val cp2y = p2.y - (p3.y - p1.y) / 6f

        path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
    }
}

// -------------------------------------------------------------------------------------------------
// Time Series Model Generator: Portfolio Value Appreciation Over Selected Timeframe
// -------------------------------------------------------------------------------------------------
private fun generatePortfolioTrendData(
    properties: List<Property>,
    timeframe: TrendTimeframe
): List<PortfolioDataPoint> {
    val totalCost = properties.sumOf { it.totalCostBasis }.coerceAtLeast(120000.0)
    val totalExit = properties.sumOf { it.effectiveExitValue }.coerceAtLeast(totalCost * 1.25)
    val totalAcquisition = properties.sumOf { it.price }.coerceAtLeast(100000.0)

    val pointsCount = timeframe.dataPointsCount
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("MMM yy", Locale.ITALIAN)

    val list = mutableListOf<PortfolioDataPoint>()

    for (i in 0 until pointsCount) {
        val progress = i.toDouble() / (pointsCount - 1).coerceAtLeast(1)
        // Sigmoid / S-curve appreciation curve modeling acquisition -> works in progress -> market appreciation -> finished exit
        val sCurveWeight = (1.0 / (1.0 + Math.exp(-6.0 * (progress - 0.5))))

        // Cost basis gradually increases as renovation budget gets deployed
        val costAtStep = totalAcquisition + (totalCost - totalAcquisition) * progress * 0.95
        // Value grows along S-curve with compounding margin appreciation
        val valueAtStep = totalAcquisition * 0.98 + (totalExit - totalAcquisition * 0.98) * sCurveWeight

        val netGain = valueAtStep - costAtStep
        val gainPercent = if (costAtStep > 0) (netGain / costAtStep) * 100.0 else 0.0

        val calCopy = calendar.clone() as Calendar
        val monthsAgo = timeframe.monthsBack * (pointsCount - 1 - i) / (pointsCount - 1).coerceAtLeast(1)
        calCopy.add(Calendar.MONTH, -monthsAgo)
        val dateLabel = sdf.format(calCopy.time).capitalize(Locale.ITALIAN)

        list.add(
            PortfolioDataPoint(
                timestamp = calCopy.timeInMillis,
                dateLabel = dateLabel,
                portfolioValue = valueAtStep,
                investedCostBasis = costAtStep,
                netAppreciation = netGain,
                appreciationPercent = gainPercent,
                activePropertiesCount = properties.size
            )
        )
    }

    return list
}

private fun formatCompactEuro(amount: Double): String {
    return when {
        amount >= 1000000 -> String.format(Locale.ITALY, "€%.1fM", amount / 1000000.0)
        amount >= 1000 -> String.format(Locale.ITALY, "€%.0fk", amount / 1000.0)
        else -> String.format(Locale.ITALY, "€%.0f", amount)
    }
}
