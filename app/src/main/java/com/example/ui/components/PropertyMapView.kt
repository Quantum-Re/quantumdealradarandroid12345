package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Property
import com.example.ui.theme.*
import com.example.util.ImageUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * PropertyMapView is a Compose component that integrates with the Google Maps SDK
 * to display markers for each Property entity saved in the Room database,
 * using its latitude and longitude coordinate fields.
 */
@Composable
fun PropertyMapView(
    properties: List<Property>,
    modifier: Modifier = Modifier,
    onPropertySelected: ((Property) -> Unit)? = null,
    onDeleteProperty: ((Property) -> Unit)? = null
) {
    val mappableProperties = remember(properties) {
        properties.filter {
            val lat = it.latitude
            val lng = it.longitude
            lat != null && lng != null && (lat != 0.0 || lng != 0.0)
        }
    }
    var selectedPropertyId by remember { mutableStateOf<Long?>(mappableProperties.firstOrNull()?.id) }
    
    // Maintain selection when list changes
    val selectedProperty = remember(selectedPropertyId, mappableProperties) {
        mappableProperties.find { it.id == selectedPropertyId } ?: mappableProperties.firstOrNull()
    }

    val coroutineScope = rememberCoroutineScope()

    // Default center for Italy
    val defaultCenter = LatLng(42.5, 12.5)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 5.8f)
    }

    var useVectorEngine by remember { mutableStateOf(true) }
    var selectedMapType by remember { mutableStateOf(MapType.NORMAL) }
    val mapProperties by remember(selectedMapType) {
        derivedStateOf {
            MapProperties(
                mapType = selectedMapType,
                isBuildingEnabled = true,
                isIndoorEnabled = true
            )
        }
    }

    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            )
        )
    }

    // Pulse animation for selected marker
    val infiniteTransition = rememberInfiniteTransition(label = "property_marker_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("property_map_view")
    ) {
        if (useVectorEngine) {
            val convertedDeals = remember(mappableProperties) {
                mappableProperties.mapNotNull { prop ->
                    val coords = resolvePropertyCoordinates(prop) ?: return@mapNotNull null
                    com.example.data.PropertyDeal(
                        id = prop.id,
                        title = prop.title.ifBlank { prop.address },
                        sourceKey = "saved",
                        sourceName = "Portfolio Properties",
                        sourceUrl = "",
                        location = prop.address,
                        propertyType = prop.propertyType.ifBlank { "Residenziale" },
                        askingPrice = prop.price,
                        estimatedMarketValue = prop.price * 1.25,
                        surfaceSqm = prop.surfaceSqm,
                        discountPercent = 20,
                        estimatedCapRate = 7.5,
                        latitude = coords.latitude,
                        longitude = coords.longitude
                    )
                }
            }
            InteractiveVectorMap(
                deals = convertedDeals,
                selectedDealId = selectedPropertyId,
                onDealSelected = { deal ->
                    selectedPropertyId = deal.id
                    mappableProperties.find { it.id == deal.id }?.let {
                        onPropertySelected?.invoke(it)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
        // Google Maps View Canvas
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        ) {
            mappableProperties.forEach { property ->
                val pos = resolvePropertyCoordinates(property) ?: return@forEach
                val isSelected = (property.id == selectedPropertyId)

                MarkerComposable(
                    keys = arrayOf<Any>(property.id, isSelected),
                    state = rememberMarkerState(key = property.id.toString(), position = pos),
                    title = property.title.ifBlank { property.address },
                    snippet = "€${property.price.toInt()} - ${property.distressStatus}",
                    onClick = {
                        selectedPropertyId = property.id
                        onPropertySelected?.invoke(property)
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(pos, 14f)
                            )
                        }
                        true
                    }
                ) {
                    PropertyMapMarker(
                        property = property,
                        isSelected = isSelected,
                        pulseScale = pulseScale
                    )
                }
            }
        }
        }

        // Top Controls: Map Type Switcher & Properties Counter Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Map Type Selector Surface
            Surface(
                color = SurfaceCardDark.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.testTag("map_type_selector")
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    PropertyMapTypeChip(
                        label = "🌐 Vettoriale",
                        isSelected = useVectorEngine,
                        onClick = { useVectorEngine = true }
                    )
                    PropertyMapTypeChip(
                        label = "🛰️ Google Maps",
                        isSelected = !useVectorEngine,
                        onClick = { useVectorEngine = false }
                    )
                }
            }

            // Room Properties Badge Counter
            Surface(
                color = SurfaceCardDark.copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Room,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${mappableProperties.size}/${properties.size} Geolocalizzati",
                        color = TextPrimaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Floating Camera Zoom & Recenter Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 64.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                    }
                },
                containerColor = SurfaceCardDark,
                contentColor = TextPrimaryDark,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ingrandisci Zoom")
            }

            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                    }
                },
                containerColor = SurfaceCardDark,
                contentColor = TextPrimaryDark,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("zoom_out_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Riduci Zoom")
            }

            FloatingActionButton(
                onClick = {
                    if (mappableProperties.isNotEmpty()) {
                        val builder = LatLngBounds.builder()
                        mappableProperties.forEach { resolvePropertyCoordinates(it)?.let { coords -> builder.include(coords) } }
                        coroutineScope.launch {
                            try {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
                            } catch (_: Exception) {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(defaultCenter, 5.8f))
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(defaultCenter, 5.8f))
                        }
                    }
                },
                containerColor = BentoPurpleHeader,
                contentColor = AmberGold,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("fit_all_properties_button")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Inquadra Tutti gli Immobili")
            }
        }

        // Bottom Selected Property Details Banner
        selectedProperty?.let { prop ->
            val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY) }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Surface(
                    color = SurfaceCardDark,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!prop.photoUri.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
                            ) {
                                val context = LocalContext.current
                                AsyncImage(
                                    model = ImageUtils.buildOptimizedImageRequest(
                                        context = context,
                                        data = prop.photoUri,
                                        targetWidthPx = 500,
                                        targetHeightPx = 300
                                    ),
                                    contentDescription = "Foto della proprietà",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (prop.distressStatus.uppercase()) {
                                    "ASTA" -> RoseRed.copy(alpha = 0.2f)
                                    "NPL" -> PurpleIndigo.copy(alpha = 0.2f)
                                    "STRALCIO" -> AmberGold.copy(alpha = 0.2f)
                                    else -> CyanAccent.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = prop.distressStatus,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (prop.distressStatus.uppercase()) {
                                        "ASTA" -> RoseRed
                                        "NPL" -> PurpleIndigo
                                        "STRALCIO" -> AmberGold
                                        else -> CyanAccent
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (onDeleteProperty != null) {
                                IconButton(
                                    onClick = { onDeleteProperty(prop) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Elimina Proprietà",
                                        tint = RoseRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = prop.title.ifBlank { prop.address },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                            Text(
                                text = prop.address,
                                fontSize = 13.sp,
                                color = TextSecondaryDark
                            )
                        }

                        if (prop.strategyTags.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                prop.strategyTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                                    val badgeColor = when {
                                        tag.contains("flip", ignoreCase = true) -> AmberGold
                                        tag.contains("rental", ignoreCase = true) -> EmeraldGreen
                                        tag.contains("brrrr", ignoreCase = true) -> CyanAccent
                                        tag.contains("reddito", ignoreCase = true) -> PurpleIndigo
                                        else -> TextSecondaryDark
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = badgeColor.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "🏷️ $tag",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (prop.notes.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceCardDark.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.StickyNote2,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = prop.notes,
                                        fontSize = 12.sp,
                                        color = TextSecondaryDark,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Divider(color = SurfaceCardBorder, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Prezzo Richiesto", fontSize = 10.sp, color = TextSecondaryDark)
                                Text(
                                    text = numberFormat.format(prop.price),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberGold
                                )
                            }

                            if (prop.surfaceSqm > 0) {
                                Column {
                                    Text("Superficie", fontSize = 10.sp, color = TextSecondaryDark)
                                    Text(
                                        text = "${prop.surfaceSqm} mq",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark
                                    )
                                }
                            }

                            Column {
                                Text("Valore Stimato", fontSize = 10.sp, color = TextSecondaryDark)
                                Text(
                                    text = numberFormat.format(prop.estimatedMarketValue),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Resolves coordinate field for a Property. Returns null when the property
 * has no verified coordinates: such a property must not be drawn on the map.
 */
private fun resolvePropertyCoordinates(property: Property): LatLng? {
    val lat = property.latitude
    val lng = property.longitude
    if (lat == null || lng == null) return null
    return LatLng(lat, lng)
}

@Composable
fun PropertyMapTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) AmberGold else Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PropertyMapMarker(
    property: Property,
    isSelected: Boolean,
    pulseScale: Float
) {
    val statusColor = when (property.distressStatus.uppercase()) {
        "ASTA" -> RoseRed
        "NPL" -> PurpleIndigo
        "STRALCIO" -> AmberGold
        else -> CyanAccent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Price Badge Card
        Surface(
            color = if (isSelected) AmberGold else SurfaceCardDark,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.White else statusColor
            ),
            shadowElevation = if (isSelected) 10.dp else 4.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = "€${(property.price / 1000).toInt()}k",
                    color = if (isSelected) Color.Black else TextPrimaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = if (isSelected) Color.Black else statusColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = property.distressStatus,
                        color = if (isSelected) AmberGold else Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Pin Pointer Icon with pulsing halo on selection
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size((20 * pulseScale).dp)
                        .clip(CircleShape)
                        .background(AmberGold.copy(alpha = 0.35f))
                )
            }

            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = if (isSelected) AmberGold else statusColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
