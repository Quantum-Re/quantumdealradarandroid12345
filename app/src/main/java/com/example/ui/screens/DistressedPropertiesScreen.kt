package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DistressedProperty
import kotlinx.coroutines.launch
import com.example.ui.DistressedPropertyViewModel
import com.example.ui.components.AiFeaturesHubDialog
import com.example.ui.components.DistressedAlertCriteriaDialog
import com.example.ui.components.IllustrativeEmptySearchState
import com.example.auth.FirebaseAuthManager
import com.example.ui.components.DistressedPropertyDetailBottomSheet
import com.example.ui.components.DistressedPropertyMapView
import com.example.ui.components.PropertyRoiTrendChart
import com.example.ui.components.RecentSearchesBar
import com.example.ui.theme.*
import com.example.util.CsvExporter
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistressedPropertiesScreen(
    viewModel: DistressedPropertyViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val savedCriteria by viewModel.savedAlertCriteria.collectAsStateWithLifecycle()
    val isAnalyzingArv by viewModel.isAnalyzingArv.collectAsStateWithLifecycle()
    val arvResult by viewModel.arvAnalysisResult.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val handleDeleteWithUndo: (DistressedProperty) -> Unit = { property ->
        viewModel.deleteDistressedProperty(property)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Deleted: ${property.address}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreDistressedProperty(property)
            }
        }
    }

    LaunchedEffect(Unit) {
        FirebaseAuthManager.initialize(context)
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showAiHubDialog by remember { mutableStateOf(false) }
    var selectedPropertyForDetail by remember { mutableStateOf<DistressedProperty?>(null) }
    var viewMode by remember { mutableStateOf("LIST") } // "LIST" or "MAP"

    val filterLevels = remember {
        listOf(
            "ALL" to "All Levels",
            "High" to "High Distress",
            "Medium" to "Medium",
            "Low" to "Low",
            "Foreclosure" to "Foreclosure",
            "Auction" to "Auction",
            "REO" to "Bank Owned (REO)"
        )
    }

    val pricePresets = remember {
        listOf(
            "All Prices" to (null to null),
            "< €250k" to (null to 250000.0),
            "€250k–€500k" to (250000.0 to 500000.0),
            "< €1M" to (null to 1000000.0),
            "> €1M" to (1000000.0 to null)
        )
    }

    val proximityPresets = remember {
        listOf(
            "Any Dist." to null,
            "< 5 km" to 5.0,
            "< 15 km" to 15.0,
            "< 30 km" to 30.0,
            "< 50 km" to 50.0
        )
    }

    val hasActiveFilters = uiState.selectedDistressLevel != "ALL" ||
            uiState.minPrice != null ||
            uiState.maxPrice != null ||
            uiState.maxDistanceKm != null

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("snackbar_host_distressed")
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceCardDark,
                    contentColor = TextPrimaryDark,
                    actionColor = CyanAccent,
                    actionContentColor = CyanAccent,
                    dismissActionContentColor = TextMutedDark,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Distressed Properties",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "${uiState.distressedProperties.size} Opportunities Tracked",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_back_distressed_screen")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimaryDark
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiHubDialog = true },
                        modifier = Modifier.testTag("btn_open_ai_hub")
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = CyanAccent,
                                    contentColor = Color.Black
                                ) {
                                    Text("AI", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI & Cloud Hub",
                                tint = CyanAccent
                            )
                        }
                    }

                    IconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.testTag("btn_open_filter_dialog")
                    ) {
                        BadgedBox(
                            badge = {
                                if (hasActiveFilters) {
                                    Badge(
                                        containerColor = CyanAccent,
                                        contentColor = Color.White
                                    ) {
                                        Text("!", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter Properties",
                                tint = if (hasActiveFilters) CyanAccent else TextSecondaryDark
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewMode = if (viewMode == "LIST") "MAP" else "LIST" },
                        modifier = Modifier.testTag("btn_toggle_map_view_mode")
                    ) {
                        Icon(
                            imageVector = if (viewMode == "LIST") Icons.Default.Map else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = if (viewMode == "LIST") "Switch to Map View" else "Switch to List View",
                            tint = CyanAccent
                        )
                    }

                    IconButton(
                        onClick = {
                            CsvExporter.exportDistressedPropertiesToCsv(context, uiState.distressedProperties)
                        },
                        modifier = Modifier.testTag("btn_export_distressed_csv_top_bar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            tint = CyanAccent
                        )
                    }

                    IconButton(
                        onClick = { showAlertDialog = true },
                        modifier = Modifier.testTag("btn_open_alert_criteria")
                    ) {
                        BadgedBox(
                            badge = {
                                if (savedCriteria.alertsEnabled) {
                                    Badge(
                                        containerColor = EmeraldGreen,
                                        contentColor = Color.White
                                    ) {
                                        Text("ON", fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Alert Criteria",
                                tint = BentoPurpleOnContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceCardDark,
                    titleContentColor = TextPrimaryDark
                ),
                modifier = Modifier.testTag("distressed_properties_top_bar")
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CyanAccent,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_distressed_property")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Distressed Property"
                    )
                    Text(
                        text = "Add Property",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        },
        containerColor = DarkSlateBg,
        modifier = modifier.testTag("distressed_properties_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Permanent Unverified Data Warning Banner
                if (uiState.distressedProperties.any { !com.example.data.DataProvenance.fromString(it.provenance).isTrustworthy }) {
                    com.example.ui.components.UnverifiedDataWarningBanner()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search address, distress level, or notes...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextMutedDark
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search",
                                    tint = TextMutedDark
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { viewModel.submitSearchQuery(uiState.searchQuery) }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCardDark,
                        unfocusedContainerColor = SurfaceCardDark,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_distressed_properties")
                )

                // Recent Searches Bar from Room Storage
                RecentSearchesBar(
                    recentSearches = recentSearches,
                    onSelectQuery = { query ->
                        viewModel.submitSearchQuery(query)
                    },
                    onRemoveQuery = { query ->
                        viewModel.removeRecentSearch(query)
                    },
                    onClearAll = {
                        viewModel.clearRecentSearches()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Distress Level Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("row_distress_filter_chips")
                ) {
                    items(filterLevels) { (key, label) ->
                        val isSelected = uiState.selectedDistressLevel == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onDistressLevelSelected(key) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BentoPurpleContainer,
                                selectedLabelColor = BentoPurpleOnContainer,
                                containerColor = SurfaceCardDark,
                                labelColor = TextSecondaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = SurfaceCardBorder,
                                selectedBorderColor = CyanAccent
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("chip_distress_level_$key")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Price Range & Proximity Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("row_price_proximity_filter_chips")
                ) {
                    item {
                        // Filter Tune button
                        AssistChip(
                            onClick = { showFilterDialog = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filters",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (hasActiveFilters) "Filters On" else "All Filters",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasActiveFilters) CyanAccent else TextSecondaryDark
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceCardDark),
                            border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = if (hasActiveFilters) CyanAccent else SurfaceCardBorder),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("chip_open_filter_tune")
                        )
                    }

                    // Price Range Presets
                    items(pricePresets) { (label, range) ->
                        val (minP, maxP) = range
                        val isSelected = uiState.minPrice == minP && uiState.maxPrice == maxP
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setPriceRange(minP, maxP) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BentoPurpleContainer,
                                selectedLabelColor = BentoPurpleOnContainer,
                                containerColor = SurfaceCardDark,
                                labelColor = TextSecondaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = SurfaceCardBorder,
                                selectedBorderColor = CyanAccent
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("chip_price_preset_$label")
                        )
                    }

                    // Proximity Presets
                    items(proximityPresets) { (label, distKm) ->
                        val isSelected = uiState.maxDistanceKm == distKm
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setMaxDistanceKm(distKm) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = if (isSelected) CyanAccent else TextMutedDark,
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BentoPurpleContainer,
                                selectedLabelColor = BentoPurpleOnContainer,
                                containerColor = SurfaceCardDark,
                                labelColor = TextSecondaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = SurfaceCardBorder,
                                selectedBorderColor = CyanAccent
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("chip_proximity_preset_$label")
                        )
                    }
                }

                if (hasActiveFilters) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BentoPurpleContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("banner_active_filters")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                val activeFilterParts = mutableListOf<String>()
                                if (uiState.selectedDistressLevel != "ALL") activeFilterParts.add(uiState.selectedDistressLevel)
                                if (uiState.minPrice != null || uiState.maxPrice != null) {
                                    val priceStr = when {
                                        uiState.minPrice != null && uiState.maxPrice != null -> "€${uiState.minPrice!!.toInt()/1000}k–€${uiState.maxPrice!!.toInt()/1000}k"
                                        uiState.maxPrice != null -> "< €${uiState.maxPrice!!.toInt()/1000}k"
                                        else -> "> €${uiState.minPrice!!.toInt()/1000}k"
                                    }
                                    activeFilterParts.add(priceStr)
                                }
                                if (uiState.maxDistanceKm != null) {
                                    activeFilterParts.add("< ${uiState.maxDistanceKm!!.toInt()} km")
                                }
                                Text(
                                    text = "Active Filters: ${activeFilterParts.joinToString(" • ")}",
                                    fontSize = 11.sp,
                                    color = TextPrimaryDark,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                            TextButton(
                                onClick = { viewModel.clearAllFilters() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.testTag("btn_clear_all_filters")
                            ) {
                                Text("Clear All", fontSize = 11.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Error Banner
                uiState.errorMessage?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RoseRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("card_error_message")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = RoseRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = err,
                                fontSize = 13.sp,
                                color = RoseRed,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearErrorMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = RoseRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // WorkManager Background Service Status Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("card_workmanager_service_status")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen)
                            )
                            Column {
                                Text(
                                    text = "WorkManager Background Scanner Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "Checks Room DB for high-distress deals & triggers notifications",
                                    fontSize = 10.sp,
                                    color = TextMutedDark
                                )
                            }
                        }

                        TextButton(
                            onClick = { viewModel.triggerWorkManagerBackgroundCheck() },
                            modifier = Modifier.testTag("btn_trigger_workmanager_check")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Run Worker",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                    }
                }

                // Stats Header Card
                DistressedPropertiesStatsSummary(properties = uiState.distressedProperties)

                Spacer(modifier = Modifier.height(12.dp))

                // View Mode Toggle Selector (List vs Map Pins vs Alongside Split vs ROI Trends Chart)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = viewMode == "LIST",
                        onClick = { viewMode = "LIST" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "List (${uiState.distressedProperties.size})",
                                fontSize = 11.sp,
                                fontWeight = if (viewMode == "LIST") FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BentoPurpleContainer,
                            selectedLabelColor = BentoPurpleOnContainer,
                            containerColor = SurfaceCardDark,
                            labelColor = TextSecondaryDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = viewMode == "LIST",
                            borderColor = SurfaceCardBorder,
                            selectedBorderColor = CyanAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_view_mode_list")
                    )

                    FilterChip(
                        selected = viewMode == "ROI_TRENDS",
                        onClick = { viewMode = "ROI_TRENDS" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "ROI Chart",
                                fontSize = 11.sp,
                                fontWeight = if (viewMode == "ROI_TRENDS") FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BentoPurpleContainer,
                            selectedLabelColor = BentoPurpleOnContainer,
                            containerColor = SurfaceCardDark,
                            labelColor = TextSecondaryDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = viewMode == "ROI_TRENDS",
                            borderColor = SurfaceCardBorder,
                            selectedBorderColor = CyanAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_view_mode_roi_trends")
                    )

                    FilterChip(
                        selected = viewMode == "SPLIT",
                        onClick = { viewMode = "SPLIT" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VerticalSplit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Alongside",
                                fontSize = 11.sp,
                                fontWeight = if (viewMode == "SPLIT") FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BentoPurpleContainer,
                            selectedLabelColor = BentoPurpleOnContainer,
                            containerColor = SurfaceCardDark,
                            labelColor = TextSecondaryDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = viewMode == "SPLIT",
                            borderColor = SurfaceCardBorder,
                            selectedBorderColor = CyanAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_view_mode_split")
                    )

                    FilterChip(
                        selected = viewMode == "MAP",
                        onClick = { viewMode = "MAP" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Map",
                                fontSize = 11.sp,
                                fontWeight = if (viewMode == "MAP") FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BentoPurpleContainer,
                            selectedLabelColor = BentoPurpleOnContainer,
                            containerColor = SurfaceCardDark,
                            labelColor = TextSecondaryDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = viewMode == "MAP",
                            borderColor = SurfaceCardBorder,
                            selectedBorderColor = CyanAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_view_mode_map")
                    )
                }

                if (viewMode == "SPLIT") {
                    // Split View: Google Maps on Top, Room DB Property List Below
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                                .testTag("container_google_map_split")
                        ) {
                            DistressedPropertyMapView(
                                distressedProperties = uiState.distressedProperties,
                                savedAlertCriteria = savedCriteria,
                                onSaveAlertCriteria = { query, level, maxPrice, enabled ->
                                    viewModel.saveAlertCriteria(query, level, maxPrice, enabled)
                                },
                                onTriggerTestNotification = { address, price, level ->
                                    viewModel.triggerTestNotification(address, price, level)
                                },
                                onAddDistressedProperty = { address, price, level ->
                                    viewModel.addDistressedProperty(address, price, level)
                                },
                                onDeleteProperty = { property ->
                                    handleDeleteWithUndo(property)
                                },
                                onSaveNotes = { property, notes ->
                                    viewModel.updatePropertyNotes(property, notes)
                                },
                                onPropertySelected = { property ->
                                    selectedPropertyForDetail = property
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DISTRESSED PROPERTIES (${uiState.distressedProperties.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedDark,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Room Database",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = CyanAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (uiState.distressedProperties.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No properties match current filters.",
                                    fontSize = 13.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("list_distressed_properties_split")
                            ) {
                                items(
                                    items = uiState.distressedProperties,
                                    key = { it.id }
                                ) { property ->
                                    DistressedPropertyCard(
                                        property = property,
                                        onClick = { selectedPropertyForDetail = property },
                                        onDelete = { handleDeleteWithUndo(property) },
                                        modifier = Modifier.testTag("card_distressed_property_${property.id}")
                                    )
                                }
                            }
                        }
                    }
                } else if (viewMode == "ROI_TRENDS") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        PropertyRoiTrendChart(
                            distressedProperties = uiState.distressedProperties,
                            onPropertySelected = { selectedPropertyForDetail = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "PROPERTIES RANKED BY POTENTIAL YIELD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMutedDark,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("list_distressed_properties_roi")
                        ) {
                            items(
                                items = uiState.distressedProperties.sortedByDescending { prop ->
                                    val price = prop.price
                                    val arv = prop.estimatedArv ?: if (prop.estimatedValue > price) prop.estimatedValue else (price * 1.35)
                                    val reno = price * 0.15
                                    if (price + reno > 0) ((arv - price - reno) / (price + reno)) * 100.0 else 0.0
                                },
                                key = { it.id }
                            ) { property ->
                                DistressedPropertyCard(
                                    property = property,
                                    onClick = { selectedPropertyForDetail = property },
                                    onDelete = { handleDeleteWithUndo(property) },
                                    modifier = Modifier.testTag("card_distressed_property_${property.id}")
                                )
                            }
                        }
                    }
                } else if (viewMode == "MAP") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                            .testTag("container_google_map_pins")
                    ) {
                        DistressedPropertyMapView(
                            distressedProperties = uiState.distressedProperties,
                            savedAlertCriteria = savedCriteria,
                            onSaveAlertCriteria = { query, level, maxPrice, enabled ->
                                viewModel.saveAlertCriteria(query, level, maxPrice, enabled)
                            },
                            onTriggerTestNotification = { address, price, level ->
                                viewModel.triggerTestNotification(address, price, level)
                            },
                            onAddDistressedProperty = { address, price, level ->
                                viewModel.addDistressedProperty(address, price, level)
                            },
                            onDeleteProperty = { property ->
                                handleDeleteWithUndo(property)
                            },
                            onSaveNotes = { property, notes ->
                                viewModel.updatePropertyNotes(property, notes)
                            },
                            onPropertySelected = { property ->
                                selectedPropertyForDetail = property
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Property List or Empty State
                    if (uiState.distressedProperties.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IllustrativeEmptySearchState(
                                searchQuery = uiState.searchQuery,
                                selectedSource = uiState.selectedDistressLevel,
                                onResetFilters = { viewModel.clearAllFilters() },
                                onAddDealClick = { showAddDialog = true },
                                onSuggestionClick = { suggestion ->
                                    viewModel.onSearchQueryChange(suggestion)
                                }
                            )
                        }
                    } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 88.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("list_distressed_properties")
                    ) {
                        item {
                            PropertyRoiTrendChart(
                                distressedProperties = uiState.distressedProperties,
                                onPropertySelected = { selectedPropertyForDetail = it },
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(
                            items = uiState.distressedProperties,
                            key = { it.id }
                        ) { property ->
                            DistressedPropertyCard(
                                property = property,
                                onClick = { selectedPropertyForDetail = property },
                                onDelete = { handleDeleteWithUndo(property) },
                                modifier = Modifier.testTag("card_distressed_property_${property.id}")
                            )
                        }
                    }
                }
            }
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("loading_distressed_properties"),
                    color = CyanAccent
                )
            }
        }
    }

    // AI Features & Firebase Auth Hub Dialog
    if (showAiHubDialog) {
        AiFeaturesHubDialog(
            selectedProperty = selectedPropertyForDetail,
            allProperties = uiState.distressedProperties,
            onDismissRequest = { showAiHubDialog = false }
        )
    }

    // Modal Bottom Sheet for Details
    selectedPropertyForDetail?.let { property ->
        DistressedPropertyDetailBottomSheet(
            distressedProperty = property,
            onDismissRequest = { selectedPropertyForDetail = null },
            onDeleteProperty = {
                handleDeleteWithUndo(it)
                selectedPropertyForDetail = null
            },
            onSaveNotes = { prop, notes ->
                viewModel.updatePropertyNotes(prop, notes)
            },
            onUpdatePhoto = { prop, imagePath ->
                viewModel.updatePropertyPhoto(prop, imagePath)
            },
            onAnalyzeArvClick = { prop ->
                viewModel.analyzePropertyArv(prop)
            },
            isAnalyzingArv = isAnalyzingArv,
            arvResult = arvResult
        )
    }

    // Add Property Dialog
    if (showAddDialog) {
        AddDistressedPropertyDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { address, price, distressLevel, estimatedValue, status, lat, lng, imageUrl, notes ->
                viewModel.addDistressedProperty(
                    address = address,
                    price = price,
                    distressLevel = distressLevel,
                    estimatedValue = estimatedValue,
                    status = status,
                    latitude = lat,
                    longitude = lng,
                    imageUrl = imageUrl.ifBlank { null },
                    notes = notes
                )
                showAddDialog = false
            }
        )
    }

    // Alert Criteria Configuration Dialog
    if (showAlertDialog) {
        DistressedAlertCriteriaDialog(
            savedCriteria = savedCriteria,
            onDismissRequest = { showAlertDialog = false },
            onSaveCriteria = { query, level, maxPrice, enabled ->
                viewModel.saveAlertCriteria(query, level, maxPrice, enabled)
            },
            onTriggerTestNotification = { addr, pr, lvl ->
                viewModel.triggerTestNotification(addr, pr, lvl)
            },
            onAddSampleMatchingProperty = { addr, pr, lvl ->
                viewModel.addDistressedProperty(
                    address = addr,
                    price = pr,
                    distressLevel = lvl
                )
            }
        )
    }

    // Filter Dialog for Distress Level, Price Range, and Proximity
    if (showFilterDialog) {
        DistressedFilterDialog(
            currentDistressLevel = uiState.selectedDistressLevel,
            currentMinPrice = uiState.minPrice,
            currentMaxPrice = uiState.maxPrice,
            currentMaxDistanceKm = uiState.maxDistanceKm,
            onDismissRequest = { showFilterDialog = false },
            onApplyFilters = { level, minP, maxP, distKm ->
                viewModel.onDistressLevelSelected(level)
                viewModel.setPriceRange(minP, maxP)
                viewModel.setMaxDistanceKm(distKm)
                showFilterDialog = false
            },
            onResetFilters = {
                viewModel.clearAllFilters()
                showFilterDialog = false
            }
        )
    }
}

@Composable
private fun DistressedPropertiesStatsSummary(
    properties: List<DistressedProperty>
) {
    val totalCount = properties.size
    val totalPrice = properties.sumOf { it.price }
    val avgPrice = if (totalCount > 0) totalPrice / totalCount else 0.0
    val highDistressCount = properties.count {
        it.distressLevel.equals("High", ignoreCase = true) ||
                it.distressLevel.equals("Foreclosure", ignoreCase = true)
    }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.GERMANY) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceCardBorder)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_distressed_stats_summary")
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "TOTAL TRACKED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedDark
                )
                Text(
                    text = "$totalCount Deals",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp),
                color = SurfaceCardBorder
            )

            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "AVG PRICE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedDark
                )
                Text(
                    text = if (totalCount > 0) currencyFormatter.format(avgPrice) else "€0",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp),
                color = SurfaceCardBorder
            )

            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "HIGH DISTRESS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedDark
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(RoseRed)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$highDistressCount Deals",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseRed
                    )
                }
            }
        }
    }
}

@Composable
fun DistressedPropertyCard(
    property: DistressedProperty,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.GERMANY) }
    val distressAccentColor = getDistressAccentColor(property.distressLevel)
    val (badgeBg, badgeText) = getDistressBadgeColors(property.distressLevel)

    val hasDiscount = property.estimatedValue > property.price && property.price > 0
    val discountPercent = if (hasDiscount) {
        (((property.estimatedValue - property.price) / property.estimatedValue) * 100).toInt()
    } else 0

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(property.lastUpdated) {
        dateFormat.format(Date(property.lastUpdated))
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, distressAccentColor.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Material3 Distress Level Indicator Bar (Red = High, Yellow = Medium, Green = Low)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        color = distressAccentColor,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Distress Badge & Status & Demo Badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val provenanceEnum = com.example.data.DataProvenance.fromString(property.provenance)
                    if (!provenanceEnum.isTrustworthy) {
                        Surface(
                            color = RoseRed,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = provenanceEnum.label.uppercase(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = property.distressLevel.uppercase(),
                            color = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = BentoPurpleHeader,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = property.status,
                            color = BentoPurpleOnContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (property.estimatedArv != null && property.estimatedArv > 0) {
                        Surface(
                            color = CyanAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "ARV: €${NumberFormat.getNumberInstance(Locale.ITALY).format(property.estimatedArv.toInt())}",
                                    color = CyanAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Property",
                        tint = TextMutedDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Address
            Text(
                text = property.address,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Price & Valuation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Asking Price",
                        fontSize = 10.sp,
                        color = TextMutedDark,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = currencyFormatter.format(property.price),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyanAccent
                    )
                }

                if (property.estimatedValue > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Est. Market Value",
                            fontSize = 10.sp,
                            color = TextMutedDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currencyFormatter.format(property.estimatedValue),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondaryDark
                            )
                            if (hasDiscount && discountPercent > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = EmeraldGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "-$discountPercent%",
                                            color = EmeraldGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!property.notes.isNullWithBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = DarkSlateBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = property.notes!!,
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer coordinates & update time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextMutedDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${String.format("%.4f", property.latitude)}, ${String.format("%.4f", property.longitude)}",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                }

                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = TextMutedDark
                )
            }
        }
    }
}
}

private fun String?.isNullWithBlank(): Boolean {
    return this == null || this.isBlank()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDistressedPropertyDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        address: String,
        price: Double,
        distressLevel: String,
        estimatedValue: Double,
        status: String,
        lat: Double,
        lng: Double,
        imageUrl: String,
        notes: String
    ) -> Unit
) {
    var address by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var estimatedValueText by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("High") }
    var selectedStatus by remember { mutableStateOf("Active") }
    var latText by remember { mutableStateOf("45.4642") }
    var lngText by remember { mutableStateOf("9.1900") }
    var imageUrl by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    var addressError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    imageUrl = file.absolutePath
                    Toast.makeText(context, "Property photo captured!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.cacheDir
            val file = File(storageDir, "new_property_photo_${System.currentTimeMillis()}.jpg")
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                currentPhotoFile = file
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission required to capture property photos", Toast.LENGTH_SHORT).show()
        }
    }

    fun takeNewPropertyPhoto() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.cacheDir
            val file = File(storageDir, "new_property_photo_${System.currentTimeMillis()}.jpg")
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                currentPhotoFile = file
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val distressLevels = listOf("High", "Medium", "Low", "Foreclosure", "Auction", "REO")
    val statuses = listOf("Active", "Pending", "Resolved", "Under Review")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AddBusiness,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Distressed Property",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Address Field
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        addressError = false
                    },
                    label = { Text("Property Address *") },
                    isError = addressError,
                    supportingText = if (addressError) { { Text("Address is required") } } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_add_property_address")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Price Field
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = {
                            priceText = it
                            priceError = false
                        },
                        label = { Text("Asking Price (€) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = priceError,
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_add_property_price")
                    )

                    // Estimated Value
                    OutlinedTextField(
                        value = estimatedValueText,
                        onValueChange = { estimatedValueText = it },
                        label = { Text("Est. Value (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_add_property_est_value")
                    )
                }

                // Distress Level Choice Chips
                Text(
                    text = "Distress Level",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryDark
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(distressLevels) { level ->
                        FilterChip(
                            selected = selectedLevel == level,
                            onClick = { selectedLevel = level },
                            label = { Text(level, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_add_distress_$level")
                        )
                    }
                }

                // Status Choice Chips
                Text(
                    text = "Status",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryDark
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(statuses) { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BentoPurpleContainer,
                                selectedLabelColor = BentoPurpleOnContainer
                            )
                        )
                    }
                }

                // Lat/Lng Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Image URL or Camera Photo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Image URL or Local Path") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = { takeNewPropertyPhoto() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("btn_add_dialog_take_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Snap Photo",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Initial Notes & Observations Field
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes & Observations") },
                    placeholder = { Text("Initial observations, property state, contact details...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_add_property_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceVal = priceText.toDoubleOrNull()
                    if (address.isBlank()) {
                        addressError = true
                        return@Button
                    }
                    if (priceVal == null || priceVal <= 0) {
                        priceError = true
                        return@Button
                    }

                    val estVal = estimatedValueText.toDoubleOrNull() ?: 0.0
                    val latVal = latText.toDoubleOrNull() ?: 0.0
                    val lngVal = lngText.toDoubleOrNull() ?: 0.0

                    onConfirm(
                        address,
                        priceVal,
                        selectedLevel,
                        estVal,
                        selectedStatus,
                        latVal,
                        lngVal,
                        imageUrl,
                        notesText
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                modifier = Modifier.testTag("btn_confirm_add_distressed_property")
            ) {
                Text("Save Property")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_add_distressed_property")
            ) {
                Text("Cancel", color = TextSecondaryDark)
            }
        },
        containerColor = SurfaceCardDark,
        modifier = Modifier.testTag("dialog_add_distressed_property")
    )
}

private fun getDistressAccentColor(distressLevel: String): Color {
    val level = distressLevel.lowercase()
    return when {
        level.contains("high") || level.contains("foreclosure") || level.contains("critical") -> RoseRed // Red
        level.contains("medium") || level.contains("auction") || level.contains("tax") || level.contains("short") -> AmberGold // Yellow
        level.contains("low") || level.contains("reo") || level.contains("pre") || level.contains("bank") -> EmeraldGreen // Green
        else -> CyanAccent
    }
}

private fun getDistressBadgeColors(distressLevel: String): Pair<Color, Color> {
    val accent = getDistressAccentColor(distressLevel)
    return accent.copy(alpha = 0.18f) to accent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistressedFilterDialog(
    currentDistressLevel: String,
    currentMinPrice: Double?,
    currentMaxPrice: Double?,
    currentMaxDistanceKm: Double?,
    onDismissRequest: () -> Unit,
    onApplyFilters: (distressLevel: String, minPrice: Double?, maxPrice: Double?, maxDistanceKm: Double?) -> Unit,
    onResetFilters: () -> Unit
) {
    var level by remember { mutableStateOf(currentDistressLevel) }
    var minPriceText by remember { mutableStateOf(currentMinPrice?.toInt()?.toString() ?: "") }
    var maxPriceText by remember { mutableStateOf(currentMaxPrice?.toInt()?.toString() ?: "") }
    var distanceKmText by remember { mutableStateOf(currentMaxDistanceKm?.toInt()?.toString() ?: "") }

    val levels = listOf(
        "ALL" to "All Levels",
        "High" to "High Distress",
        "Medium" to "Medium",
        "Low" to "Low",
        "Foreclosure" to "Foreclosure",
        "Auction" to "Auction",
        "REO" to "Bank Owned (REO)"
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Filter Distressed Properties",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Distress Level Selection
                Text("Distress Level", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(levels) { (key, label) ->
                        FilterChip(
                            selected = level == key,
                            onClick = { level = key },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("dialog_filter_level_$key")
                        )
                    }
                }

                // Price Range Inputs
                Text("Price Range (€)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = minPriceText,
                        onValueChange = { minPriceText = it },
                        label = { Text("Min (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_filter_min_price")
                    )
                    OutlinedTextField(
                        value = maxPriceText,
                        onValueChange = { maxPriceText = it },
                        label = { Text("Max (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_filter_max_price")
                    )
                }

                // Proximity Distance
                Text("Proximity Radius (KM)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark)
                OutlinedTextField(
                    value = distanceKmText,
                    onValueChange = { distanceKmText = it },
                    label = { Text("Max Radius (km) e.g. 15") },
                    placeholder = { Text("Leave blank for any distance") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_filter_max_distance")
                )
                Text(
                    text = "Proximity calculated relative to Milan city center (45.4642° N, 9.1900° E)",
                    fontSize = 10.sp,
                    color = TextMutedDark
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minP = minPriceText.toDoubleOrNull()
                    val maxP = maxPriceText.toDoubleOrNull()
                    val distKm = distanceKmText.toDoubleOrNull()
                    onApplyFilters(level, minP, maxP, distKm)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                modifier = Modifier.testTag("btn_apply_distressed_filters")
            ) {
                Text("Apply Filters")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onResetFilters,
                modifier = Modifier.testTag("btn_reset_distressed_filters")
            ) {
                Text("Reset All", color = RoseRed)
            }
        },
        containerColor = SurfaceCardDark,
        modifier = Modifier.testTag("dialog_distressed_filters")
    )
}
