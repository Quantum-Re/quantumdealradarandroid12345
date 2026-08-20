package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import com.example.data.DistressedProperty
import com.example.data.PropertyDeal
import com.example.ui.SavedAlertCriteria
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
import kotlin.math.abs

/**
 * DistressedPropertyMapView is a Google Maps UI component that renders pins for each
 * DistressedProperty entity fetched from the Room database.
 */
@Composable
fun DistressedPropertyMapView(
    distressedProperties: List<DistressedProperty>,
    modifier: Modifier = Modifier,
    selectedPropertyId: Long? = null,
    savedAlertCriteria: SavedAlertCriteria = SavedAlertCriteria(),
    onPropertySelected: ((DistressedProperty) -> Unit)? = null,
    onDeleteProperty: ((DistressedProperty) -> Unit)? = null,
    onSaveNotes: ((DistressedProperty, String) -> Unit)? = null,
    onAddPropertyClicked: (() -> Unit)? = null,
    onSaveAlertCriteria: ((query: String, level: String, maxPrice: Double?, enabled: Boolean) -> Unit)? = null,
    onTriggerTestNotification: ((address: String, price: Double, level: String) -> Unit)? = null,
    onAddDistressedProperty: ((address: String, price: Double, level: String) -> Unit)? = null
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var showAlertCriteriaDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDistressLevel by remember { mutableStateOf("ALL") }
    var showFilterDropdown by remember { mutableStateOf(false) }

    val distressFilterOptions = remember {
        listOf(
            "ALL" to "All Levels",
            "Foreclosure" to "Foreclosure",
            "Auction" to "Auction",
            "Pre-Foreclosure" to "Pre-Foreclosure",
            "Tax Lien" to "Tax Lien"
        )
    }

    val searchTokens = searchQuery.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
    val filteredProperties = remember(distressedProperties, searchQuery, selectedDistressLevel) {
        android.util.Log.d("DistressedPropertyMapView", "Rendering map view with ${distressedProperties.size} total properties passed from ViewModel (LevelFilter='$selectedDistressLevel', SearchQuery='$searchQuery')")
        val filtered = distressedProperties.filter { item ->
            val matchesSearch = if (searchTokens.isEmpty()) {
                true
            } else {
                val searchableText = listOf(
                    item.address,
                    item.distressLevel,
                    item.notes,
                    "€${item.price.toInt()}",
                    "${item.price.toInt()}"
                ).joinToString(" ").lowercase()

                searchTokens.all { token -> searchableText.contains(token) }
            }

            val matchesLevel = when (selectedDistressLevel.uppercase()) {
                "ALL" -> true
                "FORECLOSURE" -> item.distressLevel.contains("FORECLOSURE", ignoreCase = true) ||
                        item.distressLevel.contains("CRITICAL", ignoreCase = true) ||
                        item.distressLevel.contains("HIGH", ignoreCase = true)
                "AUCTION" -> item.distressLevel.contains("AUCTION", ignoreCase = true) ||
                        item.distressLevel.contains("ASTA", ignoreCase = true)
                "PRE-FORECLOSURE" -> item.distressLevel.contains("PRE", ignoreCase = true) ||
                        item.distressLevel.contains("LOW", ignoreCase = true)
                "TAX LIEN" -> item.distressLevel.contains("TAX", ignoreCase = true) ||
                        item.distressLevel.contains("LIEN", ignoreCase = true) ||
                        item.distressLevel.contains("NPL", ignoreCase = true) ||
                        item.distressLevel.contains("MEDIUM", ignoreCase = true)
                else -> item.distressLevel.equals(selectedDistressLevel, ignoreCase = true) ||
                        item.distressLevel.contains(selectedDistressLevel, ignoreCase = true)
            }

            if (searchTokens.isNotEmpty()) {
                matchesSearch
            } else {
                matchesSearch && matchesLevel
            }
        }
        android.util.Log.d("DistressedPropertyMapView", "DistressedPropertyMapView filtered to ${filtered.size} pins to display on map")
        filtered
    }

    var activePropertyId by remember(selectedPropertyId, filteredProperties) {
        mutableStateOf(selectedPropertyId ?: filteredProperties.firstOrNull()?.id)
    }

    val selectedProperty = remember(activePropertyId, filteredProperties) {
        filteredProperties.find { it.id == activePropertyId }
    }

    val coroutineScope = rememberCoroutineScope()

    // Default map center (e.g., Rome / Central Italy coordinates)
    val defaultCenter = LatLng(41.9028, 12.4964)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 6f)
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

    // Pulse animation for the selected pin marker
    val infiniteTransition = rememberInfiniteTransition(label = "distressed_pin_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Fit bounds on search results or initial load
    LaunchedEffect(filteredProperties) {
        if (filteredProperties.isNotEmpty()) {
            val validPositions = filteredProperties.map { resolveDistressedCoordinates(it) }
            val builder = LatLngBounds.Builder()
            validPositions.forEach { builder.include(it) }
            try {
                val bounds = builder.build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120))
            } catch (_: Exception) {
                // Fallback to first property position if bounds calculation fails
                validPositions.firstOrNull()?.let {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 10f)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("distressed_property_map_view")
    ) {
        if (useVectorEngine) {
            val convertedDeals = remember(filteredProperties) {
                filteredProperties.map { item ->
                    val coords = resolveDistressedCoordinates(item)
                    PropertyDeal(
                        id = item.id,
                        title = item.address,
                        sourceKey = "distressed",
                        sourceName = "Distressed Database",
                        sourceUrl = "",
                        location = item.address,
                        propertyType = item.distressLevel,
                        askingPrice = item.price,
                        estimatedMarketValue = item.price * 1.3,
                        surfaceSqm = 100,
                        discountPercent = 25,
                        estimatedCapRate = 8.0,
                        latitude = coords.latitude,
                        longitude = coords.longitude
                    )
                }
            }
            InteractiveVectorMap(
                deals = convertedDeals,
                selectedDealId = activePropertyId,
                onDealSelected = { deal ->
                    activePropertyId = deal.id
                    showBottomSheet = true
                    filteredProperties.find { it.id == deal.id }?.let {
                        onPropertySelected?.invoke(it)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
        // Google Map displaying filtered pins from Room database
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        ) {
            filteredProperties.forEach { item ->
                val pos = resolveDistressedCoordinates(item)
                val isSelected = (item.id == activePropertyId)

                MarkerComposable(
                    keys = arrayOf<Any>(item.id, isSelected, item.distressLevel),
                    state = rememberMarkerState(key = "distressed_${item.id}", position = pos),
                    title = item.address,
                    snippet = "€${NumberFormat.getNumberInstance(Locale.ITALY).format(item.price)} • ${item.distressLevel}",
                    onClick = {
                        activePropertyId = item.id
                        showBottomSheet = true
                        onPropertySelected?.invoke(item)
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(pos, 14f)
                            )
                        }
                        true
                    }
                ) {
                    DistressedPropertyMarkerPin(
                        distressedProperty = item,
                        isSelected = isSelected,
                        pulseScale = pulseScale
                    )
                }
            }
        }
        }

        // Top Control Overlay: Search Bar & Map Type Selector & Pin Count
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Bar Component
            Surface(
                color = SurfaceCardDark.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("distressed_map_search_bar")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search address or city...",
                                color = TextMutedDark,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            cursorColor = CyanAccent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("distressed_map_search_input")
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = TextMutedDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Distress Level Filter Chips & Dropdown Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dropdown Button
                Box {
                    Surface(
                        onClick = { showFilterDropdown = true },
                        color = SurfaceCardDark.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedDistressLevel != "ALL") CyanAccent else SurfaceCardBorder
                        ),
                        modifier = Modifier.testTag("distress_filter_dropdown_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter Dropdown",
                                tint = if (selectedDistressLevel != "ALL") CyanAccent else TextMutedDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showFilterDropdown,
                        onDismissRequest = { showFilterDropdown = false },
                        modifier = Modifier
                            .background(SurfaceCardDark)
                            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
                            .testTag("distress_filter_dropdown_menu")
                    ) {
                        distressFilterOptions.forEach { (key, label) ->
                            val count = if (key == "ALL") distressedProperties.size else distressedProperties.count { item ->
                                when (key.uppercase()) {
                                    "FORECLOSURE" -> item.distressLevel.contains("FORECLOSURE", ignoreCase = true) || item.distressLevel.contains("CRITICAL", ignoreCase = true) || item.distressLevel.contains("HIGH", ignoreCase = true)
                                    "AUCTION" -> item.distressLevel.contains("AUCTION", ignoreCase = true) || item.distressLevel.contains("ASTA", ignoreCase = true)
                                    "PRE-FORECLOSURE" -> item.distressLevel.contains("PRE", ignoreCase = true) || item.distressLevel.contains("LOW", ignoreCase = true)
                                    "TAX LIEN" -> item.distressLevel.contains("TAX", ignoreCase = true) || item.distressLevel.contains("NPL", ignoreCase = true) || item.distressLevel.contains("MEDIUM", ignoreCase = true)
                                    else -> item.distressLevel.equals(key, ignoreCase = true)
                                }
                            }

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (selectedDistressLevel == key) CyanAccent else TextPrimaryDark,
                                            fontWeight = if (selectedDistressLevel == key) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Surface(
                                            color = if (selectedDistressLevel == key) CyanAccent.copy(alpha = 0.2f) else SurfaceCardDark,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "$count",
                                                color = if (selectedDistressLevel == key) CyanAccent else TextMutedDark,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedDistressLevel = key
                                    showFilterDropdown = false
                                },
                                leadingIcon = if (selectedDistressLevel == key) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = CyanAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier.testTag("distress_filter_option_$key")
                            )
                        }
                    }
                }

                // Scrollable Chip Group Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("distress_level_chip_group")
                ) {
                    items(distressFilterOptions) { (key, label) ->
                        val isSelected = selectedDistressLevel == key
                        val count = if (key == "ALL") distressedProperties.size else distressedProperties.count { item ->
                            when (key.uppercase()) {
                                "FORECLOSURE" -> item.distressLevel.contains("FORECLOSURE", ignoreCase = true) || item.distressLevel.contains("CRITICAL", ignoreCase = true) || item.distressLevel.contains("HIGH", ignoreCase = true)
                                "AUCTION" -> item.distressLevel.contains("AUCTION", ignoreCase = true) || item.distressLevel.contains("ASTA", ignoreCase = true)
                                "PRE-FORECLOSURE" -> item.distressLevel.contains("PRE", ignoreCase = true) || item.distressLevel.contains("LOW", ignoreCase = true)
                                "TAX LIEN" -> item.distressLevel.contains("TAX", ignoreCase = true) || item.distressLevel.contains("NPL", ignoreCase = true) || item.distressLevel.contains("MEDIUM", ignoreCase = true)
                                else -> item.distressLevel.equals(key, ignoreCase = true)
                            }
                        }

                        Surface(
                            onClick = { selectedDistressLevel = key },
                            color = if (isSelected) CyanAccent else SurfaceCardDark.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyanAccent else SurfaceCardBorder
                            ),
                            shadowElevation = if (isSelected) 4.dp else 0.dp,
                            modifier = Modifier.testTag("distress_chip_$key")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TextPrimaryDark,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Surface(
                                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else SurfaceCardBorder.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "$count",
                                        color = if (isSelected) Color.White else TextMutedDark,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Map Controls & Pin Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Map Type Selector Surface
                Surface(
                    color = SurfaceCardDark.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier.testTag("distressed_map_type_selector")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        MapTypeChip(
                            label = "🌐 Vettoriale",
                            isSelected = useVectorEngine,
                            onClick = { useVectorEngine = true }
                        )
                        MapTypeChip(
                            label = "🛰️ Google Maps",
                            isSelected = !useVectorEngine,
                            onClick = { useVectorEngine = false }
                        )
                    }
                }

                // Room DB Pin Count Counter Badge
                Surface(
                    color = SurfaceCardDark.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (filteredProperties.size != distressedProperties.size) CyanAccent.copy(alpha = 0.5f) else RoseRed.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("distressed_room_count_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (filteredProperties.size != distressedProperties.size) Icons.Default.FilterList else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (filteredProperties.size != distressedProperties.size) CyanAccent else RoseRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (filteredProperties.size != distressedProperties.size)
                                "${filteredProperties.size}/${distressedProperties.size} Pins"
                            else
                                "${distressedProperties.size} Distressed Pins",
                            color = TextPrimaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Floating Action Controls: Recenter, Alert Criteria & Zoom Buttons
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Alert Criteria & Notification Configuration Button
            SmallFloatingActionButton(
                onClick = { showAlertCriteriaDialog = true },
                containerColor = if (savedAlertCriteria.alertsEnabled) CyanAccent.copy(alpha = 0.2f) else SurfaceCardDark,
                contentColor = if (savedAlertCriteria.alertsEnabled) CyanAccent else TextMutedDark,
                modifier = Modifier
                    .border(1.dp, if (savedAlertCriteria.alertsEnabled) CyanAccent else SurfaceCardBorder, CircleShape)
                    .testTag("open_alert_criteria_dialog_fab")
            ) {
                Icon(
                    imageVector = if (savedAlertCriteria.alertsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                    contentDescription = "Configura Alert Immobili Distressed"
                )
            }

            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        if (filteredProperties.isNotEmpty()) {
                            val builder = LatLngBounds.Builder()
                            filteredProperties.forEach { builder.include(resolveDistressedCoordinates(it)) }
                            try {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
                            } catch (_: Exception) {}
                        }
                    }
                },
                containerColor = SurfaceCardDark,
                contentColor = CyanAccent,
                modifier = Modifier.testTag("recenter_distressed_map_button")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Fit All Pins")
            }

            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                    }
                },
                containerColor = SurfaceCardDark,
                contentColor = TextPrimaryDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                    }
                },
                containerColor = SurfaceCardDark,
                contentColor = TextPrimaryDark
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
        }

        // Bottom Card Info Banner for Currently Selected Pin
        selectedProperty?.let { property ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                SelectedDistressedPropertyCard(
                    distressedProperty = property,
                    onOpenDetails = { showBottomSheet = true },
                    onDelete = onDeleteProperty,
                    onClose = { activePropertyId = null }
                )
            }

            if (showBottomSheet) {
                DistressedPropertyDetailBottomSheet(
                    distressedProperty = property,
                    onDismissRequest = { showBottomSheet = false },
                    onDeleteProperty = onDeleteProperty,
                    onSaveNotes = onSaveNotes
                )
            }
        }

        if (showAlertCriteriaDialog) {
            DistressedAlertCriteriaDialog(
                savedCriteria = savedAlertCriteria,
                onDismissRequest = { showAlertCriteriaDialog = false },
                onSaveCriteria = { query, level, maxPrice, enabled ->
                    onSaveAlertCriteria?.invoke(query, level, maxPrice, enabled)
                },
                onTriggerTestNotification = { address, price, level ->
                    onTriggerTestNotification?.invoke(address, price, level)
                },
                onAddSampleMatchingProperty = { address, price, level ->
                    onAddDistressedProperty?.invoke(address, price, level)
                }
            )
        }
    }
}

@Composable
private fun MapTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) CyanAccent.copy(alpha = 0.25f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) CyanAccent else TextMutedDark,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DistressedPropertyMarkerPin(
    distressedProperty: DistressedProperty,
    isSelected: Boolean,
    pulseScale: Float
) {
    val levelColor = getDistressColor(distressedProperty.distressLevel)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.testTag("distressed_pin_marker_${distressedProperty.id}")
    ) {
        // Outer pulsing ring for selected pin
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size((38 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(levelColor.copy(alpha = 0.35f))
            )
        }

        // Marker Pin Container
        Surface(
            color = if (isSelected) levelColor else SurfaceCardDark,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = if (isSelected) 2.dp else 1.5.dp,
                color = if (isSelected) Color.White else levelColor
            ),
            shadowElevation = 6.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else levelColor)
                )

                Text(
                    text = "€${NumberFormat.getNumberInstance(Locale.ITALY).format(distressedProperty.price.toInt())}",
                    color = if (isSelected) Color.White else TextPrimaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = (if (isSelected) Color.White else levelColor).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = distressedProperty.distressLevel.take(3).uppercase(),
                        color = if (isSelected) Color.White else levelColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedDistressedPropertyCard(
    distressedProperty: DistressedProperty,
    onOpenDetails: () -> Unit = {},
    onDelete: ((DistressedProperty) -> Unit)?,
    onClose: () -> Unit
) {
    val levelColor = getDistressColor(distressedProperty.distressLevel)

    Surface(
        color = SurfaceCardDark.copy(alpha = 0.96f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        shadowElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_distressed_property_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = levelColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, levelColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = levelColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "DISTRESS: ${distressedProperty.distressLevel.uppercase()}",
                            color = levelColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Card",
                        tint = TextMutedDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Property Thumbnail using Coil
                val imageUrl = remember(distressedProperty.imageUrl, distressedProperty.id) {
                    if (!distressedProperty.imageUrl.isNullOrBlank()) {
                        distressedProperty.imageUrl
                    } else {
                        val fallbackPhotos = listOf(
                            "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?auto=format&fit=crop&w=400&q=80",
                            "https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=400&q=80",
                            "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=400&q=80",
                            "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=400&q=80",
                            "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=400&q=80"
                        )
                        val index = (distressedProperty.id.coerceAtLeast(0) % fallbackPhotos.size).toInt()
                        fallbackPhotos[index]
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("selected_card_property_thumbnail")
                ) {
                    val context = LocalContext.current
                    SubcomposeAsyncImage(
                        model = ImageUtils.buildOptimizedImageRequest(
                            context = context,
                            data = imageUrl,
                            targetWidthPx = 160,
                            targetHeightPx = 160
                        ),
                        contentDescription = "Property Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SurfaceCardDark),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = CyanAccent,
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SurfaceCardDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HomeWork,
                                    contentDescription = null,
                                    tint = TextMutedDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = distressedProperty.address,
                        color = TextPrimaryDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Price from DB",
                        color = TextMutedDark,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "€${NumberFormat.getNumberInstance(Locale.ITALY).format(distressedProperty.price)}",
                        color = AmberGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                val coords = resolveDistressedCoordinates(distressedProperty)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Coordinates",
                        color = TextMutedDark,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "%.4f, %.4f".format(coords.latitude, coords.longitude),
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenDetails,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent.copy(alpha = 0.2f),
                        contentColor = CyanAccent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Full Details Sheet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                onDelete?.let { deleteAction ->
                    TextButton(
                        onClick = { deleteAction(distressedProperty) },
                        colors = ButtonDefaults.textButtonColors(contentColor = RoseRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Delete", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun getDistressColor(level: String): Color {
    return when (level.uppercase()) {
        "CRITICAL", "HIGH", "AUCTION_SEVERE" -> RoseRed
        "MEDIUM", "MODERATE", "TAX_LIEN" -> AmberGold
        "LOW", "PRE_FORECLOSURE" -> EmeraldGreen
        else -> CyanAccent
    }
}

/**
 * Resolves coordinate lat/lng for a DistressedProperty.
 * Uses stored lat/lng if valid non-zero values exist, or falls back to standard regional coordinates.
 */
private fun resolveDistressedCoordinates(property: DistressedProperty): LatLng {
    val lat = property.latitude
    val lng = property.longitude
    if (lat != null && lng != null && abs(lat) > 0.001 && abs(lng) > 0.001) {
        return LatLng(lat, lng)
    }

    // Deterministic fallback based on property ID or address hash for demonstration/testing
    val baseLat = 41.9028
    val baseLng = 12.4964
    val hash = property.address.hashCode()
    val latOffset = ((hash % 100) / 500.0)
    val lngOffset = (((hash / 100) % 100) / 500.0)

    return LatLng(baseLat + latOffset, baseLng + lngOffset)
}
