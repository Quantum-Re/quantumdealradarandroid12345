package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertyDeal
import com.example.ui.screens.HeatmapRegion
import com.example.ui.screens.HotspotCluster
import com.example.ui.theme.*
import com.example.util.D3ColorScale
import com.example.util.D3OverlayMetric
import com.example.util.D3Palette
import com.google.android.gms.maps.model.LatLng
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * High-performance, rich, interactive vector map canvas.
 * Renders topographic grid lines, regional heatmap polygons with D3 color interpolation,
 * concentric density rings, and clickable property deal markers with smooth pan/zoom gestures.
 */
@Composable
fun InteractiveVectorMap(
    deals: List<PropertyDeal>,
    heatmapRegions: List<HeatmapRegion> = emptyList(),
    hotspotClusters: List<HotspotCluster> = emptyList(),
    activeMetric: D3OverlayMetric = D3OverlayMetric.DISTRESS_SEVERITY,
    activePalette: D3Palette = D3Palette.TURBO,
    overlayOpacity: Float = 0.40f,
    selectedDealId: Long? = null,
    selectedRegionId: String? = null,
    showHeatmapPolygons: Boolean = true,
    showDensityOverlay: Boolean = true,
    showDensityGrid: Boolean = false,
    showPinsHeatmap: Boolean = true,
    onDealSelected: (PropertyDeal) -> Unit = {},
    onRegionSelected: (HeatmapRegion) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Canvas Pan and Zoom State
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Selected Deal State
    var localSelectedDealId by remember(selectedDealId) { mutableStateOf(selectedDealId) }

    LaunchedEffect(deals.size, activePalette, activeMetric) {
        android.util.Log.d("InteractiveVectorMap", "Vector map refreshed: deals=${deals.size}, metric=$activeMetric, palette=$activePalette")
    }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY) }
    val textMeasurer = rememberTextMeasurer()

    // Bounding Box for Italy (lat 36.0 to 47.2, lng 6.6 to 18.5)
    val minLat = 36.0
    val maxLat = 47.5
    val minLng = 6.5
    val maxLng = 18.8

    // Pulsing Animation for Selected Markers
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .testTag("interactive_vector_map_canvas")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.6f, 5.0f)
                        offset += pan
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Helper to project LatLng to Screen Coordinates
            fun projectLatLng(latLng: LatLng): Offset {
                val xNorm = (latLng.longitude - minLng) / (maxLng - minLng)
                val yNorm = 1.0 - ((latLng.latitude - minLat) / (maxLat - minLat))

                val baseX = (xNorm * canvasWidth).toFloat()
                val baseY = (yNorm * canvasHeight).toFloat()

                val screenX = (baseX * scale) + offset.x
                val screenY = (baseY * scale) + offset.y

                return Offset(screenX, screenY)
            }

            // 1. Background Stylized Grid & Water Topography
            val gridColor = SurfaceCardBorder.copy(alpha = 0.35f)
            val gridStep = 80f * scale

            var x = (offset.x % gridStep)
            while (x < canvasWidth) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, canvasHeight),
                    strokeWidth = 1f
                )
                x += gridStep
            }

            var y = (offset.y % gridStep)
            while (y < canvasHeight) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }

            // 1.5 Density Grid Cells if active
            if (showDensityGrid) {
                val cellStepLng = 0.85
                val cellStepLat = 0.70
                var gridLat = minLat
                while (gridLat < maxLat) {
                    var gridLng = minLng
                    while (gridLng < maxLng) {
                        val cellCenter = LatLng(gridLat + cellStepLat / 2, gridLng + cellStepLng / 2)
                        val dealsInCell = deals.count {
                            val lat = it.effectiveLatitude
                            val lng = it.effectiveLongitude
                            lat != null && lng != null &&
                                    lat in gridLat..(gridLat + cellStepLat) &&
                                    lng in gridLng..(gridLng + cellStepLng)
                        }
                        if (dealsInCell > 0) {
                            val normT = (dealsInCell / 6f).coerceIn(0.15f, 1.0f)
                            val cellColor = D3ColorScale.interpolate(normT, activePalette, overlayOpacity * 0.75f)

                            val p1 = projectLatLng(LatLng(gridLat + cellStepLat, gridLng))
                            val p2 = projectLatLng(LatLng(gridLat, gridLng + cellStepLng))
                            val rectW = p2.x - p1.x
                            val rectH = p2.y - p1.y

                            if (rectW > 0 && rectH > 0) {
                                drawRect(
                                    color = cellColor,
                                    topLeft = p1,
                                    size = Size(rectW, rectH)
                                )
                                drawRect(
                                    color = cellColor.copy(alpha = 0.9f),
                                    topLeft = p1,
                                    size = Size(rectW, rectH),
                                    style = Stroke(width = 1f)
                                )
                            }
                        }
                        gridLng += cellStepLng
                    }
                    gridLat += cellStepLat
                }
            }

            // 2. Main Italian City / Region Geographic Nodes
            val majorNodes = listOf(
                "MILANO" to LatLng(45.4642, 9.1900),
                "ROMA" to LatLng(41.9028, 12.4964),
                "TORINO" to LatLng(45.0703, 7.6869),
                "BOLOGNA" to LatLng(44.4949, 11.3426),
                "FIRENZE" to LatLng(43.7696, 11.2558),
                "NAPOLI" to LatLng(40.8518, 14.2681),
                "VENEZIA" to LatLng(45.4408, 12.3155),
                "PALERMO" to LatLng(38.1157, 13.3615),
                "BARI" to LatLng(41.1171, 16.8719),
                "GENOVA" to LatLng(44.4056, 8.9463)
            )

            majorNodes.forEach { (name, latLng) ->
                val pos = projectLatLng(latLng)
                if (pos.x in -100f..(canvasWidth + 100f) && pos.y in -100f..(canvasHeight + 100f)) {
                    drawCircle(
                        color = CyanAccent.copy(alpha = 0.25f),
                        radius = 12f * scale,
                        center = pos
                    )
                    drawCircle(
                        color = CyanAccent,
                        radius = 4f * scale,
                        center = pos
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = name,
                        topLeft = Offset(pos.x + 8f, pos.y - 10f),
                        style = TextStyle(
                            color = TextSecondaryDark.copy(alpha = 0.7f),
                            fontSize = (10 * scale).coerceIn(8f, 14f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // 3. Regional Heatmap Polygons with D3-Driven Colors
            if (showHeatmapPolygons) {
                heatmapRegions.forEach { region ->
                    if (region.polygon.isNotEmpty()) {
                        val path = Path()
                        val firstPt = projectLatLng(region.polygon.first())
                        path.moveTo(firstPt.x, firstPt.y)

                        for (i in 1 until region.polygon.size) {
                            val pt = projectLatLng(region.polygon[i])
                            path.lineTo(pt.x, pt.y)
                        }
                        path.close()

                        val isSelected = region.id == selectedRegionId
                        val regionFill = region.getFillColor(activeMetric, activePalette, if (isSelected) (overlayOpacity + 0.2f).coerceAtMost(0.9f) else overlayOpacity)
                        val regionStroke = region.getStrokeColor(activeMetric, activePalette)

                        drawPath(
                            path = path,
                            color = regionFill
                        )
                        drawPath(
                            path = path,
                            color = if (isSelected) AmberGold else regionStroke,
                            style = Stroke(width = if (isSelected) 4f else 2f)
                        )
                    }
                }
            }

            // 4. Concentric Hotspot Density Circles with D3 Color Ramps
            if (showDensityOverlay) {
                hotspotClusters.forEach { cluster ->
                    val centerPt = projectLatLng(cluster.center)
                    val radiusPx = (cluster.radiusMeters / 1200.0).toFloat() * 15f * scale

                    val clusterFill = cluster.getFillColor(activeMetric, activePalette, overlayOpacity * 0.7f)
                    val clusterStroke = cluster.getStrokeColor(activeMetric, activePalette)

                    drawCircle(
                        color = clusterFill,
                        radius = radiusPx.coerceAtLeast(20f),
                        center = centerPt
                    )
                    drawCircle(
                        color = clusterStroke,
                        radius = radiusPx.coerceAtLeast(20f),
                        center = centerPt,
                        style = Stroke(width = 2f)
                    )
                }
            }

            // 5. Property Deal Pins with D3 Color Mapping
            deals.forEach { deal ->
                val lat = deal.effectiveLatitude
                val lng = deal.effectiveLongitude
                if (lat != null && lng != null) {
                    val pos = projectLatLng(LatLng(lat, lng))
                    val isSelected = deal.id == localSelectedDealId

                if (pos.x in -50f..(canvasWidth + 50f) && pos.y in -50f..(canvasHeight + 50f)) {
                    val basePinRadius = if (isSelected) 18f * pulseScale else 12f

                    // Outer Glow
                    if (isSelected) {
                        drawCircle(
                            color = AmberGold.copy(alpha = 0.4f),
                            radius = basePinRadius * 1.8f,
                            center = pos
                        )
                    }

                    // Pin Head color derived via active D3 Scale
                    val pinColor = if (showPinsHeatmap) {
                        when (activeMetric) {
                            D3OverlayMetric.DISCOUNT_PERCENT -> {
                                val norm = ((deal.discountPercent - 10f) / 45f).coerceIn(0f, 1f)
                                D3ColorScale.interpolate(norm, activePalette)
                            }
                            D3OverlayMetric.DISTRESS_SEVERITY -> {
                                val isAuction = deal.propertyType.contains("Asta", ignoreCase = true) || deal.sourceName.contains("Asta", ignoreCase = true)
                                val norm = if (isAuction) 0.85f else 0.4f
                                D3ColorScale.interpolate(norm, activePalette)
                            }
                            D3OverlayMetric.OPPORTUNITY_ALPHA -> {
                                val norm = ((deal.discountPercent / 50f) * 0.6f + (deal.estimatedCapRate.toFloat() / 12f) * 0.4f).coerceIn(0f, 1f)
                                D3ColorScale.interpolate(norm, activePalette)
                            }
                            D3OverlayMetric.AVERAGE_YIELD -> {
                                val norm = ((deal.estimatedCapRate.toFloat() - 3.5f) / 8.0f).coerceIn(0f, 1f)
                                D3ColorScale.interpolate(norm, activePalette)
                            }
                            D3OverlayMetric.PROPERTY_DENSITY -> {
                                CyanAccent
                            }
                        }
                    } else {
                        if (isSelected) AmberGold else CyanAccent
                    }

                    drawCircle(
                        color = pinColor,
                        radius = basePinRadius,
                        center = pos
                    )

                    drawCircle(
                        color = Color.White,
                        radius = basePinRadius * 0.4f,
                        center = pos
                    )

                    // Price Tag Badge on Canvas
                    val tagText = "€${(deal.askingPrice / 1000).toInt()}k (-${deal.discountPercent}%)"
                    val textLayout = textMeasurer.measure(
                        text = tagText,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    val bgWidth = textLayout.size.width + 16f
                    val bgHeight = textLayout.size.height + 8f
                    val badgeTopLeft = Offset(pos.x - (bgWidth / 2f), pos.y + basePinRadius + 4f)

                    drawRoundRect(
                        color = SurfaceCardDark.copy(alpha = 0.9f),
                        topLeft = badgeTopLeft,
                        size = Size(bgWidth, bgHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    drawRoundRect(
                        color = pinColor,
                        topLeft = badgeTopLeft,
                        size = Size(bgWidth, bgHeight),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 1.5f)
                    )

                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(badgeTopLeft.x + 8f, badgeTopLeft.y + 4f)
                    )
                }
            }
        }
        }

        // Overlay Interactive Pin Hit Detector (Click Handler)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(deals, scale, offset) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.6f, 5.0f)
                        offset += pan
                    }
                }
        )

        // Map Control Floating Buttons (Zoom In, Zoom Out, Recenter)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { scale = (scale * 1.3f).coerceAtMost(5.0f) },
                containerColor = SurfaceCardDark,
                contentColor = CyanAccent,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("vector_map_zoom_in")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            FloatingActionButton(
                onClick = { scale = (scale / 1.3f).coerceAtLeast(0.6f) },
                containerColor = SurfaceCardDark,
                contentColor = CyanAccent,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("vector_map_zoom_out")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }

            FloatingActionButton(
                onClick = {
                    scale = 1.0f
                    offset = Offset.Zero
                },
                containerColor = SurfaceCardDark,
                contentColor = AmberGold,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("vector_map_recenter")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Ricentra Italia")
            }
        }

        // Selected Property Preview Card at Bottom
        val activeSelectedDeal = deals.find { it.id == localSelectedDealId } ?: deals.firstOrNull()
        if (activeSelectedDeal != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.82f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CyanAccent, RoundedCornerShape(16.dp))
                    .testTag("vector_map_deal_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeSelectedDeal.title,
                            color = TextPrimaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${activeSelectedDeal.location} • ${activeSelectedDeal.propertyType}",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = currencyFormat.format(activeSelectedDeal.askingPrice),
                                color = CyanAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                color = RoseRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "-${activeSelectedDeal.discountPercent}% Sconto",
                                    color = RoseRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onDealSelected(activeSelectedDeal) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("vector_map_details_btn")
                    ) {
                        Text("Dettagli", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

