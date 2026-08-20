package com.example.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.HeatmapRegion
import com.example.ui.theme.*
import com.example.util.D3ColorScale
import com.example.util.D3OverlayMetric
import com.example.util.D3Palette
import java.util.Locale

/**
 * Top/Bottom Floating HUD Control Panel for D3-Based Google Maps Overlays.
 * Enables real-time switching between D3 colormaps (Turbo, Inferno, Viridis, Plasma, etc.),
 * changing visualization metrics (Density, Distress, Discount, Opportunity Alpha),
 * and fine-tuning alpha opacity.
 */
@Composable
fun D3MapOverlayHUD(
    activeMetric: D3OverlayMetric,
    activePalette: D3Palette,
    overlayOpacity: Float,
    showPolygons: Boolean,
    showHotspots: Boolean,
    showPinsHeatmap: Boolean,
    showDensityGrid: Boolean,
    onMetricChange: (D3OverlayMetric) -> Unit,
    onPaletteChange: (D3Palette) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onTogglePolygons: () -> Unit,
    onToggleHotspots: () -> Unit,
    onTogglePinsHeatmap: () -> Unit,
    onToggleDensityGrid: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        color = SurfaceCardDark.copy(alpha = 0.94f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("d3_map_overlay_hud")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Active Metric & D3 Palette summary with toggle expand button
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    D3ColorScale.getGradientColors(activePalette, 8)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(activeMetric.icon, fontSize = 14.sp)
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "D3 Overlay: ${activeMetric.displayName}",
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = CyanAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = activePalette.displayName,
                                    color = CyanAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Opacità ${(overlayOpacity * 100).toInt()}% • ${activePalette.description}",
                            color = TextSecondaryDark,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Tune,
                        contentDescription = "Configura Overlay D3",
                        tint = AmberGold
                    )
                }
            }

            // Metric Selector Row (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                D3OverlayMetric.values().forEach { metric ->
                    val isSelected = metric == activeMetric
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMetricChange(metric) },
                        label = {
                            Text(
                                text = "${metric.icon} ${metric.displayName}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BentoPurpleHeader,
                            selectedLabelColor = TextPrimaryDark,
                            containerColor = DarkSlateBg
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = SurfaceCardBorder,
                            selectedBorderColor = CyanAccent,
                            enabled = true,
                            selected = isSelected
                        ),
                        modifier = Modifier.testTag("metric_chip_${metric.name}")
                    )
                }
            }

            // Expanded Controls (Palette selection, Layer Toggles, Opacity Slider)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider(color = SurfaceCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                    // Palette Selection Row
                    Text(
                        text = "Scala Cromatica D3",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        D3Palette.values().forEach { palette ->
                            val isSelected = palette == activePalette
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) BentoPurpleHeader else DarkSlateBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) AmberGold else SurfaceCardBorder
                                ),
                                modifier = Modifier
                                    .clickable { onPaletteChange(palette) }
                                    .testTag("palette_chip_${palette.name}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    // Mini Palette Gradient Strip
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    D3ColorScale.getGradientColors(palette, 6)
                                                )
                                            )
                                    )
                                    Text(
                                        text = palette.displayName,
                                        color = if (isSelected) TextPrimaryDark else TextSecondaryDark,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Opacity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Trasparenza:",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                        Slider(
                            value = overlayOpacity,
                            onValueChange = onOpacityChange,
                            valueRange = 0.15f..0.85f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanAccent,
                                activeTrackColor = CyanAccent,
                                inactiveTrackColor = DarkSlateBg
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .testTag("d3_opacity_slider")
                        )
                        Text(
                            text = "${(overlayOpacity * 100).toInt()}%",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp)
                        )
                    }

                    // Map Overlay Layers Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = showPolygons,
                            onClick = onTogglePolygons,
                            label = { Text("Poligoni D3", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent.copy(alpha = 0.25f),
                                selectedLabelColor = TextPrimaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = SurfaceCardBorder,
                                selectedBorderColor = CyanAccent,
                                enabled = true,
                                selected = showPolygons
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = showHotspots,
                            onClick = onToggleHotspots,
                            label = { Text("Hotspot Concentrici", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGold.copy(alpha = 0.25f),
                                selectedLabelColor = TextPrimaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = SurfaceCardBorder,
                                selectedBorderColor = AmberGold,
                                enabled = true,
                                selected = showHotspots
                            ),
                            modifier = Modifier.weight(1.2f)
                        )

                        FilterChip(
                            selected = showPinsHeatmap,
                            onClick = onTogglePinsHeatmap,
                            label = { Text("Colori Pin", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoseRed.copy(alpha = 0.25f),
                                selectedLabelColor = TextPrimaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = SurfaceCardBorder,
                                selectedBorderColor = RoseRed,
                                enabled = true,
                                selected = showPinsHeatmap
                            ),
                            modifier = Modifier.weight(0.9f)
                        )

                        FilterChip(
                            selected = showDensityGrid,
                            onClick = onToggleDensityGrid,
                            label = { Text("Griglia", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreen.copy(alpha = 0.25f),
                                selectedLabelColor = TextPrimaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = SurfaceCardBorder,
                                selectedBorderColor = EmeraldGreen,
                                enabled = true,
                                selected = showDensityGrid
                            ),
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Continuous D3 Gradient Legend bar displaying dynamic min, median, and max labels.
 */
@Composable
fun D3GradientLegend(
    metric: D3OverlayMetric,
    palette: D3Palette,
    minVal: Float,
    maxVal: Float,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCardDark.copy(alpha = 0.94f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        shadowElevation = 8.dp,
        modifier = modifier
            .widthIn(min = 210.dp, max = 260.dp)
            .testTag("d3_gradient_legend")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = AmberGold, modifier = Modifier.size(13.dp))
                    Text(
                        text = "${metric.displayName} (${palette.displayName})",
                        color = TextPrimaryDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi Legenda", tint = TextMutedDark, modifier = Modifier.size(11.dp))
                }
            }

            // Continuous D3 Gradient Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            D3ColorScale.getGradientColors(palette, 16)
                        )
                    )
            )

            // Step labels along the scale
            val midVal = (minVal + maxVal) / 2f
            val q1Val = (minVal + midVal) / 2f
            val q3Val = (midVal + maxVal) / 2f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatLegendTick(minVal, metric), color = TextSecondaryDark, fontSize = 9.sp)
                Text(formatLegendTick(q1Val, metric), color = TextMutedDark, fontSize = 8.sp)
                Text(formatLegendTick(midVal, metric), color = TextSecondaryDark, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                Text(formatLegendTick(q3Val, metric), color = TextMutedDark, fontSize = 8.sp)
                Text(formatLegendTick(maxVal, metric), color = AmberGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatLegendTick(value: Float, metric: D3OverlayMetric): String {
    return when (metric) {
        D3OverlayMetric.PROPERTY_DENSITY -> "${value.toInt()} ass"
        D3OverlayMetric.DISTRESS_SEVERITY -> "${(value * 100).toInt()}%"
        D3OverlayMetric.AVERAGE_YIELD -> String.format(Locale.ITALY, "%.1f%%", value)
        D3OverlayMetric.DISCOUNT_PERCENT -> "-${value.toInt()}%"
        D3OverlayMetric.OPPORTUNITY_ALPHA -> String.format(Locale.US, "%.1f", value)
    }
}
