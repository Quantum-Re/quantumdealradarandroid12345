package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DistressedProperty
import com.example.ui.theme.*
import com.example.util.CsvExporter
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class PropertyRoiItem(
    val property: DistressedProperty,
    val price: Double,
    val estimatedArv: Double,
    val estimatedRenovationCost: Double,
    val netProfit: Double,
    val roiPercent: Double,
    val label: String
)

@Composable
fun PropertyRoiTrendChart(
    distressedProperties: List<DistressedProperty>,
    modifier: Modifier = Modifier,
    onPropertySelected: ((DistressedProperty) -> Unit)? = null
) {
    var sortBy by remember { mutableStateOf("ROI_DESC") } // "ROI_DESC", "PRICE_ASC", "DISTRESS"
    var selectedPropertyId by remember { mutableStateOf<Long?>(null) }

    // Calculate ROI metrics for all properties
    val roiItems = remember(distressedProperties, sortBy) {
        val items = distressedProperties.mapIndexed { index, prop ->
            val price = prop.price
            val arv = prop.estimatedArv ?: if (prop.estimatedValue > price) prop.estimatedValue else (price * 1.35)
            val renoCost = price * 0.15
            val profit = arv - price - renoCost
            val roi = if (price + renoCost > 0) (profit / (price + renoCost)) * 100.0 else 0.0

            val shortAddress = prop.address.split(",").firstOrNull() ?: "Prop #${prop.id}"
            PropertyRoiItem(
                property = prop,
                price = price,
                estimatedArv = arv,
                estimatedRenovationCost = renoCost,
                netProfit = profit,
                roiPercent = (roi * 10.0).roundToInt() / 10.0,
                label = shortAddress
            )
        }

        when (sortBy) {
            "ROI_DESC" -> items.sortedByDescending { it.roiPercent }
            "PRICE_ASC" -> items.sortedBy { it.price }
            "DISTRESS" -> items.sortedBy { it.property.distressLevel }
            else -> items
        }
    }

    if (roiItems.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
            modifier = modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No property data available for ROI trend analysis.",
                    fontSize = 13.sp,
                    color = TextMutedDark
                )
            }
        }
        return
    }

    // Portfolio Summary Stats
    val avgRoi = roiItems.map { it.roiPercent }.average()
    val maxRoiItem = roiItems.maxByOrNull { it.roiPercent }
    val highYieldCount = roiItems.count { it.roiPercent >= 25.0 }
    val totalPotentialProfit = roiItems.sumOf { it.netProfit }

    val selectedItem = roiItems.find { it.property.id == selectedPropertyId } ?: roiItems.firstOrNull()

    val textMeasurer = rememberTextMeasurer()

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_property_roi_trend_chart")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
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
                            .background(CyanAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "ROI Trends Chart",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Potential ROI Trends",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Canvas Real Estate Yield & Flip Analytics",
                            fontSize = 11.sp,
                            color = TextMutedDark
                        )
                    }
                }

                // Sort Chip Toggle
                Surface(
                    color = DarkSlateBg,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SortPill(
                            label = "ROI ↓",
                            selected = sortBy == "ROI_DESC",
                            onClick = { sortBy = "ROI_DESC" }
                        )
                        SortPill(
                            label = "Price",
                            selected = sortBy == "PRICE_ASC",
                            onClick = { sortBy = "PRICE_ASC" }
                        )
                        SortPill(
                            label = "Distress",
                            selected = sortBy == "DISTRESS",
                            onClick = { sortBy = "DISTRESS" }
                        )
                    }
                }
            }

            // Summary Stat Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(
                    title = "AVERAGE ROI",
                    value = "+${String.format(Locale.US, "%.1f", avgRoi)}%",
                    valueColor = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = "TOP YIELD",
                    value = "+${String.format(Locale.US, "%.1f", maxRoiItem?.roiPercent ?: 0.0)}%",
                    valueColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = "TOTAL PROFIT",
                    value = "€${NumberFormat.getNumberInstance(Locale.ITALY).format((totalPotentialProfit / 1000).toInt())}k",
                    valueColor = AmberGold,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = "DEALS > 25%",
                    value = "$highYieldCount / ${roiItems.size}",
                    valueColor = PurpleIndigo,
                    modifier = Modifier.weight(1f)
                )
            }

            // Canvas Bar Chart
            val maxRoi = max(100.0, (roiItems.maxOfOrNull { it.roiPercent } ?: 50.0) * 1.15)
            val chartHeightDp = 220.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeightDp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSlateBg)
                    .padding(8.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(roiItems) {
                            detectTapGestures { offset ->
                                val width = size.width
                                val height = size.height
                                val paddingLeft = 40f
                                val paddingRight = 10f
                                val paddingTop = 25f
                                val paddingBottom = 35f

                                val chartWidth = width - paddingLeft - paddingRight
                                val barWidth = chartWidth / roiItems.size

                                val clickedIndex = ((offset.x - paddingLeft) / barWidth).toInt()
                                if (clickedIndex in roiItems.indices) {
                                    val item = roiItems[clickedIndex]
                                    selectedPropertyId = item.property.id
                                    onPropertySelected?.invoke(item.property)
                                }
                            }
                        }
                        .testTag("canvas_roi_bar_chart")
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingLeft = 45f
                    val paddingRight = 15f
                    val paddingTop = 25f
                    val paddingBottom = 40f

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    // Gridlines (0%, 25%, 50%, 75%, 100% of maxRoi)
                    val gridSteps = 4
                    val stepValue = maxRoi / gridSteps
                    for (i in 0..gridSteps) {
                        val yVal = maxRoi - (stepValue * i)
                        val yPos = paddingTop + (chartHeight * (1f - (yVal / maxRoi).toFloat()))

                        // Dashed Grid Line
                        drawLine(
                            color = SurfaceCardBorder.copy(alpha = 0.5f),
                            start = Offset(paddingLeft, yPos),
                            end = Offset(width - paddingRight, yPos),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )

                        // Y-Axis Labels
                        val labelText = "${yVal.toInt()}%"
                        drawText(
                            textMeasurer = textMeasurer,
                            text = labelText,
                            style = TextStyle(
                                color = TextMutedDark,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            topLeft = Offset(4f, yPos - 12f)
                        )
                    }

                    // Baseline (0% line)
                    drawLine(
                        color = SurfaceCardBorder,
                        start = Offset(paddingLeft, height - paddingBottom),
                        end = Offset(width - paddingRight, height - paddingBottom),
                        strokeWidth = 2f
                    )

                    // Target 25% ROI Benchmark Line
                    val targetRoiY = paddingTop + (chartHeight * (1f - (25.0 / maxRoi).toFloat()))
                    if (25.0 <= maxRoi) {
                        drawLine(
                            color = EmeraldGreen.copy(alpha = 0.6f),
                            start = Offset(paddingLeft, targetRoiY),
                            end = Offset(width - paddingRight, targetRoiY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "25% Target",
                            style = TextStyle(
                                color = EmeraldGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            topLeft = Offset(width - paddingRight - 55f, targetRoiY - 14f)
                        )
                    }

                    // Render Bars
                    val numBars = roiItems.size
                    val totalBarSpace = chartWidth / numBars
                    val barWidth = max(12f, totalBarSpace * 0.65f)
                    val barGap = totalBarSpace - barWidth

                    roiItems.forEachIndexed { index, item ->
                        val barLeft = paddingLeft + (index * totalBarSpace) + (barGap / 2f)
                        val barHeightFraction = (item.roiPercent / maxRoi).coerceIn(0.02, 1.0).toFloat()
                        val barHeightPx = chartHeight * barHeightFraction
                        val barTop = (height - paddingBottom) - barHeightPx

                        val isSelected = item.property.id == selectedPropertyId

                        // Bar Gradient Colors
                        val topColor = when {
                            item.roiPercent >= 40.0 -> EmeraldGreen
                            item.roiPercent >= 20.0 -> CyanAccent
                            else -> AmberGold
                        }
                        val bottomColor = topColor.copy(alpha = if (isSelected) 0.95f else 0.6f)

                        // Draw Bar
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(topColor, bottomColor),
                                startY = barTop,
                                endY = height - paddingBottom
                            ),
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeightPx),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        // Selection Glow Ring around bar
                        if (isSelected) {
                            drawRoundRect(
                                color = CyanAccent,
                                topLeft = Offset(barLeft - 2f, barTop - 2f),
                                size = Size(barWidth + 4f, barHeightPx + 4f),
                                cornerRadius = CornerRadius(8f, 8f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                            )
                        }

                        // Text Above Bar (ROI %)
                        if (barWidth > 18f || isSelected) {
                            val roiLabel = "${item.roiPercent.toInt()}%"
                            val measured = textMeasurer.measure(roiLabel, TextStyle(fontSize = 9.sp))
                            val labelX = barLeft + (barWidth - measured.size.width) / 2f
                            val labelY = max(2f, barTop - measured.size.height - 2f)

                            drawText(
                                textMeasurer = textMeasurer,
                                text = roiLabel,
                                style = TextStyle(
                                    color = if (isSelected) CyanAccent else TextPrimaryDark,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                ),
                                topLeft = Offset(labelX, labelY)
                            )
                        }

                        // X-Axis Short Label below bar
                        val shortName = "P${index + 1}"
                        val xLabelMeasured = textMeasurer.measure(shortName, TextStyle(fontSize = 9.sp))
                        val xLabelX = barLeft + (barWidth - xLabelMeasured.size.width) / 2f

                        drawText(
                            textMeasurer = textMeasurer,
                            text = shortName,
                            style = TextStyle(
                                color = if (isSelected) CyanAccent else TextMutedDark,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            topLeft = Offset(xLabelX, height - paddingBottom + 6f)
                        )
                    }
                }
            }

            // Interactive Selected Property Card Tooltip Below Chart
            selectedItem?.let { item ->
                Surface(
                    color = SurfaceCardDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPropertySelected?.invoke(item.property) }
                        .testTag("card_selected_roi_property_detail")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = BentoPurpleContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.property.distressLevel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoPurpleOnContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = item.property.address,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "Acquisition: €${NumberFormat.getNumberInstance(Locale.ITALY).format(item.price.toInt())} • ARV: €${NumberFormat.getNumberInstance(Locale.ITALY).format(item.estimatedArv.toInt())}",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "+${item.roiPercent}% ROI",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldGreen
                            )
                            Text(
                                text = "+€${NumberFormat.getNumberInstance(Locale.ITALY).format(item.netProfit.toInt())} profit",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                    }
                }
            }

            val context = LocalContext.current

            // Export CSV Action Button
            OutlinedButton(
                onClick = {
                    CsvExporter.exportDistressedPropertiesToCsv(context, distressedProperties)
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = DarkSlateBg,
                    contentColor = CyanAccent
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_export_roi_deals_csv")
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export Property Deals to CSV (with ARV & ROI)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSlateBg,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextMutedDark,
                maxLines = 1
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SortPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) CyanAccent else Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else TextMutedDark,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
