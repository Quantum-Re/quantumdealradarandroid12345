package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.HeatmapRegion
import com.example.ui.screens.MapOverlayMetricType
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * Interactive Google Maps Overlay Toggle and Regional Analytics Controller.
 * Provides seamless, high-contrast switching between:
 * 1. 'Distress Level' Intensity (Auction/Foreclosure pressure, NPL volume, severe discount depth)
 * 2. 'Average Yield' Density (Gross rental yields, Cap Rate concentration, Cash-flow potential)
 * for the selected Italian region.
 */
@Composable
fun GoogleMapsOverlayToggle(
    activeOverlayMode: MapOverlayMetricType,
    selectedRegion: HeatmapRegion?,
    allRegions: List<HeatmapRegion>,
    onOverlayModeChange: (MapOverlayMetricType) -> Unit,
    onRegionSelect: (HeatmapRegion) -> Unit,
    onQuickFilterRegionDeals: ((HeatmapRegion) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.ITALY) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Primary Interactive Toggle Switch
        Surface(
            color = BentoPurpleHeader.copy(alpha = 0.96f),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = when (activeOverlayMode) {
                    MapOverlayMetricType.DISTRESS_INTENSITY -> RoseRed.copy(alpha = 0.8f)
                    MapOverlayMetricType.AVERAGE_YIELD -> EmeraldGreen.copy(alpha = 0.8f)
                }
            ),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("google_maps_overlay_toggle_container")
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Bar with Active Mode Badge & Quick Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) RoseRed else EmeraldGreen
                                )
                        )
                        Text(
                            text = "LAYER OVERLAY GOOGLE MAPS",
                            color = TextSecondaryDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Surface(
                        color = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) {
                            RoseRed.copy(alpha = 0.2f)
                        } else {
                            EmeraldGreen.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) RoseRed.copy(alpha = 0.5f) else EmeraldGreen.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) {
                                "🔥 Modalità Distress Aste"
                            } else {
                                "📈 Modalità Rendimento Lordo"
                            },
                            color = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) RoseRed else EmeraldGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Segmented Toggle Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSlateBg)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Option 1: Distress Level Intensity
                    Surface(
                        color = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) {
                            RoseRed
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOverlayModeChange(MapOverlayMetricType.DISTRESS_INTENSITY) }
                            .testTag("toggle_distress_intensity_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) Color.White else RoseRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Intensità Distress",
                                    color = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) Color.White else TextPrimaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Aste, NPL & Sofferenze",
                                    color = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) Color.White.copy(alpha = 0.85f) else TextSecondaryDark,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    // Option 2: Average Yield Density
                    Surface(
                        color = if (activeOverlayMode == MapOverlayMetricType.AVERAGE_YIELD) {
                            EmeraldGreen
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOverlayModeChange(MapOverlayMetricType.AVERAGE_YIELD) }
                            .testTag("toggle_average_yield_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (activeOverlayMode == MapOverlayMetricType.AVERAGE_YIELD) Color.White else EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Densità Rendimento",
                                    color = if (activeOverlayMode == MapOverlayMetricType.AVERAGE_YIELD) Color.White else TextPrimaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Yield % & Cap Rate",
                                    color = if (activeOverlayMode == MapOverlayMetricType.AVERAGE_YIELD) Color.White.copy(alpha = 0.85f) else TextSecondaryDark,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                // Region Quick Switcher Scrollable Reel
                if (allRegions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        allRegions.forEach { region ->
                            val isSelected = (region.id == selectedRegion?.id)
                            val activeColor = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) {
                                region.getDistressColor()
                            } else {
                                region.getYieldColor()
                            }

                            Surface(
                                color = if (isSelected) activeColor.copy(alpha = 0.25f) else DarkSlateBg,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) activeColor else SurfaceCardBorder
                                ),
                                modifier = Modifier
                                    .clickable { onRegionSelect(region) }
                                    .testTag("region_selector_chip_${region.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(activeColor)
                                    )
                                    Text(
                                        text = region.code,
                                        color = if (isSelected) activeColor else TextPrimaryDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) {
                                            "-${region.avgDiscountPercent}%"
                                        } else {
                                            String.format(Locale.ITALY, "%.1f%%", region.avgGrossYieldPercent)
                                        },
                                        color = if (isSelected) TextPrimaryDark else TextSecondaryDark,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
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

/**
 * Detailed Interactive Regional Inspection Card for the active Google Maps selection.
 * Visualizes the deep comparative metrics between Distress Level and Yield Density for the selected Italian region.
 */
@Composable
fun SelectedRegionMapInspectorCard(
    region: HeatmapRegion,
    activeOverlayMode: MapOverlayMetricType,
    onOverlayModeChange: (MapOverlayMetricType) -> Unit,
    onClose: () -> Unit,
    onFilterDeals: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.ITALY) }

    Surface(
        color = SurfaceCardDark.copy(alpha = 0.96f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) RoseRed else EmeraldGreen
        ),
        shadowElevation = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("selected_region_map_inspector")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title Header & Close Button
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
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) region.getDistressColor() else region.getYieldColor()
                            )
                    )
                    Column {
                        Text(
                            text = region.name,
                            color = TextPrimaryDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hub Regionale • Codice ${region.code}",
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) RoseRed.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) {
                                region.distressTier
                            } else {
                                region.yieldTier
                            },
                            color = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) RoseRed else EmeraldGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Metric Comparison Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Distress Metric Card
                Surface(
                    color = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) {
                        RoseRed.copy(alpha = 0.15f)
                    } else {
                        DarkSlateBg
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) RoseRed else SurfaceCardBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOverlayModeChange(MapOverlayMetricType.DISTRESS_INTENSITY) }
                        .testTag("region_inspector_distress_box")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RoseRed, modifier = Modifier.size(13.dp))
                            Text("Distress Index", color = RoseRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "${(region.densityScore * 100).toInt()} / 100",
                            color = TextPrimaryDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${region.totalDistressedAssets} Aste attive (-${region.avgDiscountPercent}%)",
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }
                }

                // Yield Metric Card
                Surface(
                    color = if (activeOverlayMode == MapOverlayMetricType.AVERAGE_YIELD) {
                        EmeraldGreen.copy(alpha = 0.15f)
                    } else {
                        DarkSlateBg
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (activeOverlayMode == MapOverlayMetricType.AVERAGE_YIELD) EmeraldGreen else SurfaceCardBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOverlayModeChange(MapOverlayMetricType.AVERAGE_YIELD) }
                        .testTag("region_inspector_yield_box")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(13.dp))
                            Text("Rendimento Lordo", color = EmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = String.format(Locale.ITALY, "%.1f%% Lordo", region.avgGrossYieldPercent),
                            color = TextPrimaryDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Cap Rate: ${String.format(Locale.ITALY, "%.1f%%", region.avgCapRatePercent)} • €${region.avgMonthlyRentPerSqMeter.toInt()}/m²",
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Real Estate Market Spread Details
            Surface(
                color = DarkSlateBg,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Prezzo Medio OMI", color = TextSecondaryDark, fontSize = 9.sp)
                        Text("€${region.avgPricePerSqMeter} / m²", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(SurfaceCardBorder))
                    Column {
                        Text("Canone Affitto", color = TextSecondaryDark, fontSize = 9.sp)
                        Text("€${String.format(Locale.ITALY, "%.2f", region.avgMonthlyRentPerSqMeter)}/m² mese", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(SurfaceCardBorder))
                    Column {
                        Text("Top Procedura", color = TextSecondaryDark, fontSize = 9.sp)
                        Text(region.topOpportunityType.take(18) + "...", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Action Button
            if (onFilterDeals != null) {
                Button(
                    onClick = onFilterDeals,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) RoseRed else EmeraldGreen
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("filter_region_deals_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mostra Immobili in ${region.name}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Dynamic Floating Overlay Legend reflecting the active mode on Google Maps.
 */
@Composable
fun GoogleMapsDynamicLegend(
    activeOverlayMode: MapOverlayMetricType,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCardDark.copy(alpha = 0.94f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) RoseRed.copy(alpha = 0.5f) else EmeraldGreen.copy(alpha = 0.5f)
        ),
        shadowElevation = 8.dp,
        modifier = modifier
            .widthIn(max = 240.dp)
            .testTag("google_maps_dynamic_legend")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    Icon(
                        imageVector = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) Icons.Default.Warning else Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) RoseRed else EmeraldGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) "Legenda Intensità Distress" else "Legenda Densità Rendimento",
                        color = TextPrimaryDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(12.dp))
                }
            }

            // Gradient Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) {
                                listOf(
                                    Color(0xFF2E7D32), // Low distress (green)
                                    AmberGold,         // Moderate
                                    Color(0xFFFF6D00), // High
                                    RoseRed            // Critical
                                )
                            } else {
                                listOf(
                                    Color(0xFF1E3A8A), // Core yield (<5.5%)
                                    CyanAccent,        // Moderate (6-7.5%)
                                    Color(0xFF00E676), // High yield (7.5-9%)
                                    Color(0xFF76FF03)  // Super Alpha (>9%)
                                )
                            }
                        )
                    )
            )

            // Ticks Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (activeOverlayMode == MapOverlayMetricType.DISTRESS_INTENSITY) {
                    Text("Basso (<40)", color = TextSecondaryDark, fontSize = 8.sp)
                    Text("Medio (60)", color = TextSecondaryDark, fontSize = 8.sp)
                    Text("Alto (80)", color = AmberGold, fontSize = 8.sp)
                    Text("Critico (>90)", color = RoseRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("< 5.5%", color = TextSecondaryDark, fontSize = 8.sp)
                    Text("6.5% - 7.5%", color = TextSecondaryDark, fontSize = 8.sp)
                    Text("8.0% - 9.0%", color = CyanAccent, fontSize = 8.sp)
                    Text("> 9.5%+", color = EmeraldGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
