package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DistressedProperty
import com.example.ui.theme.*
import com.example.util.AppCurrency
import com.example.util.AreaUnit
import com.example.util.CurrencyUnitConverter
import java.util.Locale

@Composable
fun CurrencyAndUnitConverterCard(
    distressedProperty: DistressedProperty,
    modifier: Modifier = Modifier
) {
    var selectedCurrency by remember { mutableStateOf(AppCurrency.EUR) }
    var selectedUnit by remember { mutableStateOf(AreaUnit.SQ_METERS) }
    var surfaceSqmInput by remember { mutableStateOf(100.0) }
    var isExpanded by remember { mutableStateOf(true) }

    // Derive initial area from price if not custom adjusted (e.g. 100m² typical estimate)
    val price = distressedProperty.price
    val arv = distressedProperty.estimatedArv ?: distressedProperty.estimatedValue

    val metrics = remember(price, arv, surfaceSqmInput, selectedCurrency, selectedUnit) {
        CurrencyUnitConverter.computeMetrics(
            priceEur = price,
            arvEur = arv,
            surfaceSqm = surfaceSqmInput,
            currency = selectedCurrency,
            unit = selectedUnit
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_currency_unit_converter")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title Header with Expand/Collapse toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
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
                            .background(AmberGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = "Currency & Unit Metrics Converter",
                            tint = AmberGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Currency & Price/m² Metrics",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Convert FX rates & square-unit price metrics",
                            fontSize = 11.sp,
                            color = TextMutedDark
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Converter View",
                        tint = TextMutedDark
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Currency Selection Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "TARGET CURRENCY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMutedDark,
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppCurrency.entries.forEach { curr ->
                                val isSelected = curr == selectedCurrency
                                Surface(
                                    onClick = { selectedCurrency = curr },
                                    color = if (isSelected) CyanAccent else DarkSlateBg,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) CyanAccent else SurfaceCardBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chip_currency_${curr.code}")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = curr.symbol,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) Color.White else TextPrimaryDark
                                        )
                                        Text(
                                            text = curr.code,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else TextMutedDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Unit System & Surface Area Controls
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AREA METRIC UNIT & SIZE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedDark,
                                letterSpacing = 0.8.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                AreaUnit.entries.forEach { unitItem ->
                                    val isSelected = unitItem == selectedUnit
                                    Surface(
                                        onClick = { selectedUnit = unitItem },
                                        color = if (isSelected) BentoPurpleContainer else DarkSlateBg,
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) CyanAccent else SurfaceCardBorder
                                        )
                                    ) {
                                        Text(
                                            text = unitItem.symbol,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) BentoPurpleOnContainer else TextMutedDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Surface Area Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Slider(
                                value = surfaceSqmInput.toFloat(),
                                onValueChange = { surfaceSqmInput = it.toDouble() },
                                valueRange = 30f..350f,
                                steps = 32,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanAccent,
                                    activeTrackColor = CyanAccent,
                                    inactiveTrackColor = SurfaceCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("slider_surface_area")
                            )

                            Surface(
                                color = DarkSlateBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                            ) {
                                Text(
                                    text = "${metrics.surfaceInUnit.toInt()} ${metrics.unit.symbol}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Comparative Metrics Grid Display
                    Surface(
                        color = DarkSlateBg,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder.copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "CONVERTED FINANCIAL & UNIT COMPARISON",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedDark,
                                letterSpacing = 0.8.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ConversionTile(
                                    title = "CONVERTED PRICE",
                                    mainValue = metrics.formatCurrencyValue(metrics.convertedPrice),
                                    subValue = metrics.formatUnitPrice(metrics.pricePerUnit),
                                    accentColor = AmberGold,
                                    modifier = Modifier.weight(1f)
                                )

                                ConversionTile(
                                    title = "CONVERTED ARV",
                                    mainValue = metrics.formatCurrencyValue(metrics.convertedArv),
                                    subValue = metrics.formatUnitPrice(metrics.arvPerUnit),
                                    accentColor = CyanAccent,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ConversionTile(
                                    title = "POTENTIAL PROFIT",
                                    mainValue = metrics.formatCurrencyValue(metrics.convertedNetProfit),
                                    subValue = metrics.formatUnitPrice(metrics.profitPerUnit),
                                    accentColor = EmeraldGreen,
                                    modifier = Modifier.weight(1f)
                                )

                                ConversionTile(
                                    title = "EXCHANGE RATE",
                                    mainValue = "1 EUR = ${String.format(Locale.US, "%.2f", metrics.currency.rateFromEur)} ${metrics.currency.code}",
                                    subValue = "1 m² = 10.76 sq ft",
                                    accentColor = TextSecondaryDark,
                                    modifier = Modifier.weight(1f)
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
private fun ConversionTile(
    title: String,
    mainValue: String,
    subValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCardDark,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMutedDark,
                maxLines = 1
            )
            Text(
                text = mainValue,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                maxLines = 1
            )
            Text(
                text = subValue,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondaryDark,
                maxLines = 1
            )
        }
    }
}
