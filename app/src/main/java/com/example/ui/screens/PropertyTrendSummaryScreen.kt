package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Property
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class PriceTrendPoint(
    val label: String,
    val averagePrice: Double,
    val averageMarketValue: Double,
    val averagePricePerSqm: Double,
    val dealCount: Int,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyTrendSummaryScreen(
    properties: List<Property>,
    deals: List<PropertyDeal> = emptyList(),
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
            maximumFractionDigits = 0
        }
    }

    var selectedTimePeriod by remember { mutableStateOf("Mese") } // "Mese", "Trimestre", "Tutti"
    var selectedPropertyTypeFilter by remember { mutableStateOf("Tutti") }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    // Combine Room properties and System deals into unified price points
    val combinedItems = remember(properties, deals, selectedPropertyTypeFilter) {
        val filteredProps = if (selectedPropertyTypeFilter == "Tutti") {
            properties
        } else {
            properties.filter { it.propertyType.equals(selectedPropertyTypeFilter, ignoreCase = true) }
        }

        val filteredDeals = if (selectedPropertyTypeFilter == "Tutti") {
            deals
        } else {
            deals.filter { it.propertyType.equals(selectedPropertyTypeFilter, ignoreCase = true) }
        }

        // Map Room Property items
        val propData = filteredProps.map {
            PriceTrendPoint(
                label = "",
                averagePrice = it.price,
                averageMarketValue = it.estimatedMarketValue,
                averagePricePerSqm = if (it.surfaceSqm > 0) it.price / it.surfaceSqm else 0.0,
                dealCount = 1,
                timestamp = it.createdAt
            )
        }

        // Map PropertyDeal items
        val dealData = filteredDeals.map {
            PriceTrendPoint(
                label = "",
                averagePrice = it.askingPrice,
                averageMarketValue = it.estimatedMarketValue,
                averagePricePerSqm = if (it.surfaceSqm > 0) it.askingPrice / it.surfaceSqm else 0.0,
                dealCount = 1,
                timestamp = System.currentTimeMillis() - ((it.id.hashCode() % 180).coerceAtLeast(0) * 86400000L)
            )
        }

        (propData + dealData).sortedBy { it.timestamp }
    }

    // Group items by time interval for trend calculation
    val trendPoints = remember(combinedItems, selectedTimePeriod) {
        if (combinedItems.isEmpty()) {
            // Mock trend baseline data if database is empty to visualize charts
            generateMockTrendData()
        } else {
            val sdf = when (selectedTimePeriod) {
                "Mese" -> SimpleDateFormat("MMM yy", Locale.ITALIAN)
                "Trimestre" -> SimpleDateFormat("'Q'M yy", Locale.ITALIAN)
                else -> SimpleDateFormat("dd/MM", Locale.ITALIAN)
            }

            combinedItems.groupBy { sdf.format(Date(it.timestamp)) }
                .map { (label, list) ->
                    val avgPrice = list.map { it.averagePrice }.average()
                    val avgMkt = list.map { it.averageMarketValue }.average()
                    val avgSqm = list.map { it.averagePricePerSqm }.filter { it > 0 }.let {
                        if (it.isNotEmpty()) it.average() else 0.0
                    }
                    PriceTrendPoint(
                        label = label,
                        averagePrice = avgPrice,
                        averageMarketValue = avgMkt,
                        averagePricePerSqm = avgSqm,
                        dealCount = list.size,
                        timestamp = list.first().timestamp
                    )
                }.takeLast(8)
        }
    }

    // Compute market shift indicators
    val overallAvgPrice = remember(trendPoints) {
        if (trendPoints.isNotEmpty()) trendPoints.map { it.averagePrice }.average() else 0.0
    }

    val priceTrendChangePercent = remember(trendPoints) {
        if (trendPoints.size >= 2) {
            val first = trendPoints.first().averagePrice
            val last = trendPoints.last().averagePrice
            if (first > 0) ((last - first) / first) * 100 else 0.0
        } else 0.0
    }

    val isPriceDropping = priceTrendChangePercent < 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Header
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
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmberGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Andamento Prezzi Immobili",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Analisi trend di mercato e valore medio nel tempo",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }
            }
        }

        // Market Shift Summary Banner Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Market Shift & Prezzo Medio",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanAccent
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPriceDropping) EmeraldGreen.copy(alpha = 0.2f) else RoseRed.copy(alpha = 0.2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPriceDropping) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = if (isPriceDropping) EmeraldGreen else RoseRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${if (priceTrendChangePercent >= 0) "+" else ""}${String.format(Locale.US, "%.1f", priceTrendChangePercent)}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPriceDropping) EmeraldGreen else RoseRed
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Prezzo Medio Registrato",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = currencyFormat.format(overallAvgPrice),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberGold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isPriceDropping) "Opportunità Acquisto Sotto-Prezzo" else "Fase Aumento Valori",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isPriceDropping) EmeraldGreen else AmberGold
                        )
                        Text(
                            text = "Campione: ${trendPoints.sumOf { it.dealCount }} Immobili",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        }

        // Time Period & Property Type Filters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Period Selector
            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(2.dp)
                ) {
                    listOf("Mese", "Trimestre", "Tutti").forEach { period ->
                        val isSelected = (selectedTimePeriod == period)
                        Surface(
                            color = if (isSelected) CyanAccent else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable { selectedTimePeriod = period }
                        ) {
                            Text(
                                text = period,
                                color = if (isSelected) Color.Black else TextSecondaryDark,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Property Type Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Tutti", "Residenziale", "Commerciale").forEach { type ->
                    val isSelected = (selectedPropertyTypeFilter == type)
                    Surface(
                        color = if (isSelected) AmberGold else SurfaceCardDark,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AmberGold else SurfaceCardBorder),
                        modifier = Modifier.clickable { selectedPropertyTypeFilter = type }
                    ) {
                        Text(
                            text = type,
                            color = if (isSelected) Color.Black else TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Main Price Trend Chart Canvas Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                .testTag("property_price_trend_chart_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grafico Prezzo Medio Nel Tempo (€)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AmberGold))
                            Text("Prezzo Asta/Richiesta", fontSize = 10.sp, color = TextSecondaryDark)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyanAccent))
                            Text("Stima Perizia", fontSize = 10.sp, color = TextSecondaryDark)
                        }
                    }
                }

                // Custom Jetpack Compose Canvas Trendline Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 8.dp)
                ) {
                    PropertyTrendCanvasChart(
                        trendPoints = trendPoints,
                        selectedIndex = selectedPointIndex,
                        onPointSelected = { selectedPointIndex = it }
                    )
                }

                // Interactive Tooltip Card for Selected Data Point
                selectedPointIndex?.let { idx ->
                    if (idx in trendPoints.indices) {
                        val point = trendPoints[idx]
                        Surface(
                            color = BentoPurpleHeader.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Periodo: ${point.label}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberGold
                                    )
                                    Text(
                                        text = "Prezzo Medio: ${currencyFormat.format(point.averagePrice)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    if (point.averagePricePerSqm > 0) {
                                        Text(
                                            text = "Prezzo/mq: ${currencyFormat.format(point.averagePricePerSqm)}/mq",
                                            fontSize = 11.sp,
                                            color = CyanAccent
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Stima Perizia: ${currencyFormat.format(point.averageMarketValue)}",
                                        fontSize = 12.sp,
                                        color = TextSecondaryDark
                                    )
                                    Text(
                                        text = "${point.dealCount} Immobili salvati",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = EmeraldGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Distress Status Average Price Comparison Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Confronto Prezzo Medio Per Stato Distress",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                val distressGroups = remember(properties, deals) {
                    val propPairs: List<Pair<String, Double>> = properties.map { Pair(it.distressStatus, it.price) }
                    val dealPairs: List<Pair<String, Double>> = deals.map { Pair(it.propertyType, it.askingPrice) }
                    val allCombined: List<Pair<String, Double>> = propPairs + dealPairs

                    allCombined.groupBy { it.first.uppercase() }
                        .mapValues { entry -> entry.value.map { it.second }.average() }
                }

                listOf(
                    "ASTA" to RoseRed,
                    "NPL" to PurpleIndigo,
                    "STRALCIO" to AmberGold,
                    "PRE-ASTA" to CyanAccent
                ).forEach { (status, color) ->
                    val avgPriceForStatus = distressGroups[status] ?: (overallAvgPrice * 0.85)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                            Text(currencyFormat.format(avgPriceForStatus), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        val maxPrice = (distressGroups.values.maxOrNull() ?: overallAvgPrice * 1.2).coerceAtLeast(1.0)
                        val fraction = (avgPriceForStatus / maxPrice).toFloat().coerceIn(0.1f, 1f)

                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = color,
                            trackColor = SurfaceCardBorder
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PropertyTrendCanvasChart(
    trendPoints: List<PriceTrendPoint>,
    selectedIndex: Int?,
    onPointSelected: (Int) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(trendPoints) {
                detectTapGestures { tapOffset ->
                    if (trendPoints.isNotEmpty()) {
                        val stepX = size.width / (trendPoints.size - 1).coerceAtLeast(1)
                        val closestIndex = (tapOffset.x / stepX).toInt().coerceIn(0, trendPoints.size - 1)
                        onPointSelected(closestIndex)
                    }
                }
            }
    ) {
        if (trendPoints.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val paddingBottom = 30f
        val paddingTop = 20f

        val availableHeight = height - paddingBottom - paddingTop

        val maxPrice = (trendPoints.maxOfOrNull { maxOf(it.averagePrice, it.averageMarketValue) } ?: 200000.0) * 1.15
        val minPrice = (trendPoints.minOfOrNull { minOf(it.averagePrice, it.averageMarketValue) } ?: 50000.0) * 0.85
        val priceRange = (maxPrice - minPrice).coerceAtLeast(1.0)

        val stepX = if (trendPoints.size > 1) width / (trendPoints.size - 1) else width / 2

        // Draw horizontal grid lines
        for (i in 0..3) {
            val y = paddingTop + availableHeight * (i / 3f)
            drawLine(
                color = SurfaceCardBorder,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Generate coordinates for asking price trendline
        val askingPoints = trendPoints.mapIndexed { index, point ->
            val x = index * stepX
            val normalizedY = ((point.averagePrice - minPrice) / priceRange).toFloat()
            val y = height - paddingBottom - (normalizedY * availableHeight)
            Offset(x, y)
        }

        // Generate coordinates for estimated market value trendline
        val marketPoints = trendPoints.mapIndexed { index, point ->
            val x = index * stepX
            val normalizedY = ((point.averageMarketValue - minPrice) / priceRange).toFloat()
            val y = height - paddingBottom - (normalizedY * availableHeight)
            Offset(x, y)
        }

        // Build Gradient Fill Path for Asking Price
        if (askingPoints.size > 1) {
            val fillPath = Path().apply {
                moveTo(askingPoints.first().x, height - paddingBottom)
                askingPoints.forEach { lineTo(it.x, it.y) }
                lineTo(askingPoints.last().x, height - paddingBottom)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AmberGold.copy(alpha = 0.35f),
                        AmberGold.copy(alpha = 0.02f)
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw Asking Price Trend Line
            val askingLinePath = Path().apply {
                moveTo(askingPoints.first().x, askingPoints.first().y)
                for (i in 0 until askingPoints.size - 1) {
                    val p1 = askingPoints[i]
                    val p2 = askingPoints[i + 1]
                    val controlX1 = p1.x + (p2.x - p1.x) / 2f
                    cubicTo(controlX1, p1.y, controlX1, p2.y, p2.x, p2.y)
                }
            }

            drawPath(
                path = askingLinePath,
                color = AmberGold,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
        }

        // Draw Market Value Trend Line
        if (marketPoints.size > 1) {
            val marketLinePath = Path().apply {
                moveTo(marketPoints.first().x, marketPoints.first().y)
                for (i in 0 until marketPoints.size - 1) {
                    val p1 = marketPoints[i]
                    val p2 = marketPoints[i + 1]
                    val controlX1 = p1.x + (p2.x - p1.x) / 2f
                    cubicTo(controlX1, p1.y, controlX1, p2.y, p2.x, p2.y)
                }
            }

            drawPath(
                path = marketLinePath,
                color = CyanAccent,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
            )
        }

        // Draw Data Point Circles & Selection Highlight
        askingPoints.forEachIndexed { index, pointOffset ->
            val isSelected = (index == selectedIndex)

            if (isSelected) {
                drawCircle(
                    color = AmberGold.copy(alpha = 0.25f),
                    radius = 16.dp.toPx(),
                    center = pointOffset
                )
                drawLine(
                    color = AmberGold,
                    start = Offset(pointOffset.x, 0f),
                    end = Offset(pointOffset.x, height - paddingBottom),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )
            }

            drawCircle(
                color = if (isSelected) Color.White else AmberGold,
                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                center = pointOffset
            )

            drawCircle(
                color = Color.Black,
                radius = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
                center = pointOffset
            )
        }
    }
}

private fun generateMockTrendData(): List<PriceTrendPoint> {
    val months = listOf("Gen", "Feb", "Mar", "Apr", "Mag", "Giug", "Lug", "Ago")
    val baseAsking = 135000.0
    val baseMkt = 185000.0

    return months.mapIndexed { i, month ->
        val asking = baseAsking - (i * 2400) + (i % 2 * 1500)
        val mkt = baseMkt + (i * 1200)
        PriceTrendPoint(
            label = month,
            averagePrice = asking,
            averageMarketValue = mkt,
            averagePricePerSqm = asking / 85,
            dealCount = 3 + i,
            timestamp = System.currentTimeMillis() - (8 - i) * 30L * 86400000L
        )
    }
}
