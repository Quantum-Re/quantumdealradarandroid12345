package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.PortfolioAlertsSheet
import com.example.ui.components.PortfolioAppreciationTrendChart
import com.example.ui.components.PropertySortingBar
import com.example.ui.components.PropertySortBottomSheet
import com.example.ui.components.PropertyBatchFloatingActionBar
import com.example.ui.components.PropertyPipelineComparisonView
import com.example.ui.components.BatchStatusBottomSheet
import com.example.ui.components.IllustrativeEmptyPortfolioState
import com.example.ui.components.CsvPortfolioSyncDialog
import com.example.ui.components.BatchArchiveConfirmDialog
import com.example.ui.components.BatchDeleteConfirmDialog
import com.example.ui.components.formatPropertyRelativeDate
import com.example.ui.components.formatPropertyDate
import com.example.ui.components.PropertyOpportunityBanner
import com.example.ui.components.PropertyOpportunityDetailSheet
import com.example.ui.components.PortfolioOverviewWidget
import com.example.ui.components.PropertyComparisonPdfDialog
import com.example.util.PropertyOpportunityEvaluation
import com.example.util.OpportunityTier
import com.example.data.PipelineStatus
import com.example.data.Property
import com.example.data.PropertySortOption
import com.example.ui.PropertyViewModel
import com.example.ui.theme.*
import com.example.util.CsvExporter
import com.example.util.PropertyPdfGenerator
import com.example.util.ImmobiliareObservatoryService
import java.text.NumberFormat
import java.util.Locale

// -------------------------------------------------------------
// Shared Element Transition Modifier Helpers
// -------------------------------------------------------------
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedElementIfAvailable(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    key: String
): Modifier {
    return if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            this@sharedElementIfAvailable.sharedElement(
                rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ ->
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                }
            )
        }
    } else {
        this
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedBoundsIfAvailable(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    key: String
): Modifier {
    return if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            this@sharedBoundsIfAvailable.sharedBounds(
                rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ ->
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                }
            )
        }
    } else {
        this
    }
}

enum class DashboardViewMode {
    CARDS,
    KANBAN,
    LEDGER,
    COMPARE
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MyPropertiesDashboardScreen(
    viewModel: PropertyViewModel,
    onNavigateToRoiCalculator: ((Property) -> Unit)? = null,
    onNavigateToAddProperty: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allProperties by viewModel.properties.collectAsStateWithLifecycle()
    val metrics = uiState.metrics

    var isTrendChartVisible by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(DashboardViewMode.CARDS) }
    var selectedPropertyForDetail by remember { mutableStateOf<Property?>(null) }
    var propertyToEditStatus by remember { mutableStateOf<Property?>(null) }
    var propertyToEditProgress by remember { mutableStateOf<Property?>(null) }
    var propertyToEditFinancials by remember { mutableStateOf<Property?>(null) }
    var propertyToSimulateDrop by remember { mutableStateOf<Property?>(null) }
    var propertyForRenovationSimulator by remember { mutableStateOf<Property?>(null) }
    var propertyToDelete by remember { mutableStateOf<Property?>(null) }
    var isAddPropertyDialogOpen by remember { mutableStateOf(false) }
    var addPropertyInitialStrategy by remember { mutableStateOf("Fix & Flip") }
    var isSortSheetOpen by remember { mutableStateOf(false) }
    var propertyForOpportunityDetail by remember { mutableStateOf<Pair<Property, PropertyOpportunityEvaluation>?>(null) }

    // Multi-selection state & dialogs
    var isBatchStatusSheetOpen by remember { mutableStateOf(false) }
    var isBatchArchiveConfirmOpen by remember { mutableStateOf(false) }
    var isBatchDeleteConfirmOpen by remember { mutableStateOf(false) }
    var isCsvSyncDialogOpen by remember { mutableStateOf(false) }
    var isComparisonPdfDialogOpen by rememberSaveable { mutableStateOf(false) }
    var comparisonPropA by remember { mutableStateOf<Property?>(null) }
    var comparisonPropB by remember { mutableStateOf<Property?>(null) }
    val isSelectionMode = uiState.isSelectionModeActive || uiState.selectedPropertyIds.isNotEmpty()

    val context = LocalContext.current
    val euroFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
            maximumFractionDigits = 0
        }
    }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedPropertyForDetail,
            transitionSpec = {
                (fadeIn(animationSpec = tween(320, easing = LinearOutSlowInEasing)) +
                 scaleIn(initialScale = 0.96f, animationSpec = tween(320, easing = FastOutSlowInEasing)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(260, easing = FastOutLinearInEasing)) +
                        scaleOut(targetScale = 0.96f, animationSpec = tween(260, easing = FastOutLinearInEasing))
                    )
            },
            label = "property_dashboard_to_detail_transition"
        ) { targetProperty ->
            if (targetProperty == null) {
                Scaffold(
                    containerColor = DarkSlateBg,
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { isAddPropertyDialogOpen = true },
                    containerColor = BentoPurpleOnContainer,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_property_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Aggiungi Immobile", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                PropertyBatchFloatingActionBar(
                    selectedCount = uiState.selectedPropertyIds.size,
                    totalCount = uiState.properties.size,
                    onSelectAllToggle = {
                        if (uiState.selectedPropertyIds.size == uiState.properties.size && uiState.properties.isNotEmpty()) {
                            viewModel.clearSelection()
                        } else {
                            viewModel.selectAllProperties(uiState.properties)
                        }
                    },
                    onOpenChangeStatus = { isBatchStatusSheetOpen = true },
                    onOpenArchiveConfirm = { isBatchArchiveConfirmOpen = true },
                    onOpenDeleteConfirm = { isBatchDeleteConfirmOpen = true },
                    onCancelSelection = { viewModel.clearSelection() },
                    onOpenComparison = {
                        viewMode = DashboardViewMode.COMPARE
                    }
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("my_properties_dashboard_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permanent Unverified Data Warning Banner
            if (allProperties.any { !com.example.data.DataProvenance.fromString(it.provenance).isTrustworthy }) {
                com.example.ui.components.UnverifiedDataWarningBanner()
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
            ) {
                // 1. Dashboard Header Banner & KPI Summary
            item {
                PortfolioKpiHeader(
                    metrics = metrics,
                    totalCount = allProperties.size,
                    unreadAlertCount = uiState.unreadAlertCount,
                    euroFormat = euroFormat,
                    isTrendChartVisible = isTrendChartVisible,
                    onToggleTrendChart = { isTrendChartVisible = !isTrendChartVisible },
                    onOpenAlerts = { viewModel.setAlertsSheetOpen(true) },
                    onExportCsv = {
                        CsvExporter.exportPropertiesToCsv(context, allProperties)
                    },
                    onSyncCsv = { isCsvSyncDialogOpen = true },
                    onOpenComparisonPdf = {
                        comparisonPropA = allProperties.firstOrNull()
                        comparisonPropB = allProperties.getOrNull(1) ?: allProperties.firstOrNull()
                        isComparisonPdfDialogOpen = true
                    }
                )
            }

            // 1a. High-Level Portfolio Overview Widget (Total Equity & Average Yield via Scraped Market Comps)
            item {
                PortfolioOverviewWidget(
                    properties = allProperties,
                    evaluations = uiState.opportunityEvaluations,
                    isRefreshing = uiState.isRefreshingMarketData,
                    onRefreshMarketData = { viewModel.refreshLiveMarketCompsForProperties() },
                    euroFormat = euroFormat,
                    onOpenPropertyDetail = { prop ->
                        viewModel.selectProperty(prop)
                    }
                )
            }

            // 1b. Real-Time Opportunity Radar Banner (Immobiliare.it Comps)
            item {
                PropertyOpportunityBanner(
                    evaluations = uiState.opportunityEvaluations,
                    isOnlyUndervaluedActive = uiState.showOnlyUndervalued,
                    isRefreshingMarket = uiState.isRefreshingMarketData,
                    onToggleUndervaluedFilter = { viewModel.toggleOnlyUndervaluedFilter() },
                    onRefreshMarketComps = { viewModel.refreshLiveMarketCompsForProperties() }
                )
            }

            // 1b. Collapsible Portfolio Value Appreciation Trend Line & Area Chart
            if (isTrendChartVisible) {
                item {
                    PortfolioAppreciationTrendChart(
                        properties = allProperties,
                        euroFormat = euroFormat,
                        onPropertySelected = { prop ->
                            propertyToEditFinancials = prop
                        }
                    )
                }
            }


            // 2. Search Bar & View Mode Switcher
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Search Field
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Cerca per via, titolo, note cantiere, strategia...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = BentoPurpleOnContainer)
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Cancella ricerca")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pipeline_search_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoPurpleOnContainer,
                                unfocusedBorderColor = SurfaceCardBorder,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        // View Mode Switcher & Counter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.properties.size} immobili trovati",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondaryDark
                            )

                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.testTag("view_mode_toggle")
                            ) {
                                SegmentedButton(
                                    selected = viewMode == DashboardViewMode.CARDS,
                                    onClick = { viewMode = DashboardViewMode.CARDS },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = BentoPurpleContainer,
                                        activeContentColor = BentoPurpleOnContainer
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Schede", fontSize = 10.sp)
                                }
                                SegmentedButton(
                                    selected = viewMode == DashboardViewMode.KANBAN,
                                    onClick = { viewMode = DashboardViewMode.KANBAN },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = BentoPurpleContainer,
                                        activeContentColor = BentoPurpleOnContainer
                                    )
                                ) {
                                    Icon(Icons.Default.ViewKanban, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Kanban", fontSize = 10.sp)
                                }
                                SegmentedButton(
                                    selected = viewMode == DashboardViewMode.LEDGER,
                                    onClick = { viewMode = DashboardViewMode.LEDGER },
                                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = BentoPurpleContainer,
                                        activeContentColor = BentoPurpleOnContainer
                                    )
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Ledger", fontSize = 10.sp)
                                }
                                SegmentedButton(
                                    selected = viewMode == DashboardViewMode.COMPARE,
                                    onClick = { viewMode = DashboardViewMode.COMPARE },
                                    shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = BentoPurpleContainer,
                                        activeContentColor = BentoPurpleOnContainer
                                    )
                                ) {
                                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Confronto", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Horizontal Pipeline Status Filter Chips
            item {
                PipelineStatusFilterChipsRow(
                    selectedFilter = uiState.selectedPipelineFilter,
                    allProperties = allProperties,
                    onSelectFilter = { viewModel.updatePipelineFilter(it) }
                )
            }

            // 3b. Sorting Toolbar (Date Added, Status, Estimated ROI, Price) & Multi-Selection Toggle
            item {
                PropertySortingBar(
                    currentSort = uiState.selectedSortOption,
                    onSortSelected = { viewModel.updateSortOption(it) },
                    onOpenFullSortSheet = { isSortSheetOpen = true },
                    totalCount = uiState.properties.size,
                    isSelectionModeActive = isSelectionMode,
                    onToggleSelectionMode = {
                        if (isSelectionMode) {
                            viewModel.clearSelection()
                        } else {
                            viewModel.setSelectionMode(true)
                        }
                    }
                )
            }

            // 4. Main Body Content (Cards / Kanban / Ledger)
            if (uiState.properties.isEmpty()) {
                item {
                    EmptyPipelinePlaceholder(
                        searchQuery = uiState.searchQuery,
                        selectedFilter = uiState.selectedPipelineFilter,
                        onResetFilters = {
                            viewModel.updateSearchQuery("")
                            viewModel.updatePipelineFilter("ALL")
                        },
                        onAddProperty = {
                            addPropertyInitialStrategy = "Fix & Flip"
                            isAddPropertyDialogOpen = true
                        },
                        onStrategySelected = { strategy ->
                            addPropertyInitialStrategy = strategy
                            isAddPropertyDialogOpen = true
                        },
                        onLoadDemoTemplate = { strategy ->
                            viewModel.loadStrategyTemplate(strategy)
                        },
                        onSyncCsv = { isCsvSyncDialogOpen = true }
                    )
                }
            } else {
                when (viewMode) {
                    DashboardViewMode.CARDS -> {
                        items(
                            items = uiState.properties,
                            key = { it.id }
                        ) { property ->
                            val isSelected = uiState.selectedPropertyIds.contains(property.id)
                            val evaluation = uiState.opportunityEvaluations[property.id]
                            PipelinePropertyCard(
                                property = property,
                                euroFormat = euroFormat,
                                evaluation = evaluation,
                                onOpenOpportunityDetail = { eval ->
                                    propertyForOpportunityDetail = property to eval
                                },
                                isSelected = isSelected,
                                isSelectionModeActive = isSelectionMode,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedContent,
                                onToggleSelection = { viewModel.togglePropertySelection(property.id) },
                                onCardClick = {
                                    if (isSelectionMode) {
                                        viewModel.togglePropertySelection(property.id)
                                    } else {
                                        selectedPropertyForDetail = property
                                    }
                                },
                                onOpenDetailClick = {
                                    selectedPropertyForDetail = property
                                },
                                onUpdateStatusClick = { propertyToEditStatus = property },
                                onUpdateProgressClick = { propertyToEditProgress = property },
                                onEditFinancialsClick = { propertyToEditFinancials = property },
                                onSimulatePriceDropClick = { propertyToSimulateDrop = property },
                                onCalculateRoiClick = {
                                    if (onNavigateToRoiCalculator != null) {
                                        onNavigateToRoiCalculator(property)
                                    }
                                },
                                onExportPdfClick = {
                                    PropertyPdfGenerator.generateAndSharePdf(context, property, emailOnly = false)
                                },
                                onDeleteClick = { propertyToDelete = property }
                            )
                        }
                    }

                    DashboardViewMode.KANBAN -> {
                        item {
                            KanbanPipelineBoard(
                                properties = uiState.properties,
                                euroFormat = euroFormat,
                                selectedPropertyIds = uiState.selectedPropertyIds,
                                isSelectionModeActive = isSelectionMode,
                                onToggleSelection = { propId ->
                                    viewModel.togglePropertySelection(propId)
                                },
                                onMoveStatus = { prop, newStatus ->
                                    viewModel.updatePropertyPipelineStatus(prop.id, newStatus)
                                },
                                onPropertyClick = { prop ->
                                    if (isSelectionMode) {
                                        viewModel.togglePropertySelection(prop.id)
                                    } else {
                                        selectedPropertyForDetail = prop
                                    }
                                }
                            )
                        }
                    }

                    DashboardViewMode.LEDGER -> {
                        item {
                            PipelineLedgerTable(
                                properties = uiState.properties,
                                euroFormat = euroFormat,
                                selectedPropertyIds = uiState.selectedPropertyIds,
                                isSelectionModeActive = isSelectionMode,
                                onToggleSelection = { propId ->
                                    viewModel.togglePropertySelection(propId)
                                },
                                onRowClick = { prop ->
                                    if (isSelectionMode) {
                                        viewModel.togglePropertySelection(prop.id)
                                    } else {
                                        selectedPropertyForDetail = prop
                                    }
                                },
                                onStatusClick = { prop ->
                                    propertyToEditStatus = prop
                                }
                            )
                        }
                    }

                    DashboardViewMode.COMPARE -> {
                        item {
                            PropertyPipelineComparisonView(
                                allProperties = uiState.properties,
                                selectedPropertyIds = uiState.selectedPropertyIds,
                                onTogglePropertySelection = { propId ->
                                    viewModel.togglePropertySelection(propId)
                                },
                                onSelectAllProperties = {
                                    viewModel.selectAllProperties(uiState.properties)
                                },
                                onClearSelection = {
                                    viewModel.clearSelection()
                                },
                                euroFormat = euroFormat,
                                onCalculateRoiClick = { prop ->
                                    onNavigateToRoiCalculator?.invoke(prop)
                                },
                                onEditFinancialsClick = { prop ->
                                    selectedPropertyForDetail = prop
                                },
                                onUpdateStatusClick = { prop ->
                                    propertyToEditStatus = prop
                                },
                                onUpdateProgressClick = { prop ->
                                    propertyToEditProgress = prop
                                },
                                onSimulatePriceDropClick = { prop ->
                                    propertyToSimulateDrop = prop
                                },
                                onExportPdfClick = { prop ->
                                    PropertyPdfGenerator.generateAndSharePdf(context, prop, emailOnly = false)
                                },
                                onDeleteClick = { prop ->
                                    propertyToDelete = prop
                                },
                                onOpenComparisonPdfDialog = { propA, propB ->
                                    comparisonPropA = propA ?: allProperties.firstOrNull()
                                    comparisonPropB = propB ?: allProperties.getOrNull(1) ?: allProperties.firstOrNull()
                                    isComparisonPdfDialogOpen = true
                                }
                            )
                        }
                    }
                }
                }
            }
        }
    }
} else {
    // Full Screen Property Detail with Shared Element Transitions
    val currentTarget = targetProperty
    if (currentTarget != null) {
        val liveProperty = allProperties.find { it.id == currentTarget.id } ?: currentTarget
        PropertyDetailScreen(
            property = liveProperty,
            animatedVisibilityScope = this@AnimatedContent,
            sharedTransitionScope = this@SharedTransitionLayout,
            onBackClick = { selectedPropertyForDetail = null },
            onEditFinancialsClick = { propertyToEditFinancials = liveProperty },
            onUpdateStatusClick = { propertyToEditStatus = liveProperty },
            onUpdateProgressClick = { propertyToEditProgress = liveProperty },
            onSimulatePriceDropClick = { propertyToSimulateDrop = liveProperty },
            onCalculateRoiClick = {
                onNavigateToRoiCalculator?.invoke(liveProperty)
            },
            onOpenRenovationSimulator = {
                propertyForRenovationSimulator = liveProperty
            },
            onDeleteClick = {
                propertyToDelete = liveProperty
                selectedPropertyForDetail = null
            },
            onSaveNotes = { newNotes ->
                viewModel.updateProperty(liveProperty.copy(notes = newNotes))
            }
        )
    }
}
}
}

    // Dialog: Change Status
    propertyToEditStatus?.let { prop ->
        ChangePipelineStatusDialog(
            property = prop,
            onDismiss = { propertyToEditStatus = null },
            onStatusSelected = { newStatus, extraData ->
                var updated = prop.copy(pipelineStatus = newStatus.key)
                if (extraData.actualSalePrice > 0) {
                    updated = updated.copy(actualSalePrice = extraData.actualSalePrice)
                }
                if (extraData.escrowClosingDate.isNotBlank()) {
                    updated = updated.copy(escrowClosingDate = extraData.escrowClosingDate)
                }
                viewModel.updateProperty(updated)
                propertyToEditStatus = null
            }
        )
    }

    // Dialog: Update Renovation Progress
    propertyToEditProgress?.let { prop ->
        UpdateRenovationProgressDialog(
            property = prop,
            onDismiss = { propertyToEditProgress = null },
            onSave = { progress, actualCost, notes ->
                val updated = prop.copy(
                    renovationProgressPercent = progress,
                    actualRenovationCost = actualCost,
                    contractorNotes = notes
                )
                viewModel.updateProperty(updated)
                propertyToEditProgress = null
            }
        )
    }

    // Dialog: Edit Full Financials & Details
    propertyToEditFinancials?.let { prop ->
        EditPropertyFinancialsDialog(
            property = prop,
            onDismiss = { propertyToEditFinancials = null },
            onSave = { updatedProp ->
                viewModel.updateProperty(updatedProp)
                propertyToEditFinancials = null
            }
        )
    }

    // Dialog: Add New Analyzed Property
    if (isAddPropertyDialogOpen) {
        AddNewAnalyzedPropertyDialog(
            initialStrategy = addPropertyInitialStrategy,
            onDismiss = { isAddPropertyDialogOpen = false },
            onSave = { newProp ->
                viewModel.addProperty(newProp)
                isAddPropertyDialogOpen = false
            }
        )
    }

    // Dialog: Confirm Deletion
    propertyToDelete?.let { prop ->
        AlertDialog(
            onDismissRequest = { propertyToDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = RoseRed) },
            title = { Text("Rimuovere dalla Pipeline?") },
            text = { Text("Sei sicuro di voler rimuovere '${prop.title}' da My Properties? L'operazione eliminerà il record locale dal database Room.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProperty(prop)
                        propertyToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { propertyToDelete = null }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Dialog: Simulate Price Drop & Push Notification
    propertyToSimulateDrop?.let { prop ->
        SimulatePriceDropDialog(
            property = prop,
            euroFormat = euroFormat,
            onDismiss = { propertyToSimulateDrop = null },
            onApplyDrop = { dropPercent ->
                viewModel.simulatePriceDropForProperty(prop.id, dropPercent)
                propertyToSimulateDrop = null
            }
        )
    }

    // Bottom Sheet: Portfolio Alerts Feed & Preferences
    if (uiState.isAlertsSheetOpen) {
        PortfolioAlertsSheet(
            viewModel = viewModel,
            uiState = uiState,
            onDismiss = { viewModel.setAlertsSheetOpen(false) },
            onSelectProperty = { propId ->
                val prop = allProperties.find { it.id == propId }
                if (prop != null) {
                    propertyToEditFinancials = prop
                }
            }
        )
    }

    // Bottom Sheet: Full Property Sorting Options
    if (isSortSheetOpen) {
        PropertySortBottomSheet(
            currentSort = uiState.selectedSortOption,
            onSortSelected = { viewModel.updateSortOption(it) },
            onDismiss = { isSortSheetOpen = false }
        )
    }

    // Modal Bottom Sheet: Batch Status Change
    if (isBatchStatusSheetOpen) {
        BatchStatusBottomSheet(
            selectedCount = uiState.selectedPropertyIds.size,
            onStatusSelected = { newStatus ->
                viewModel.batchUpdatePipelineStatus(newStatus)
                isBatchStatusSheetOpen = false
            },
            onDismiss = { isBatchStatusSheetOpen = false }
        )
    }

    // Dialog: Batch Archive Confirmation
    if (isBatchArchiveConfirmOpen) {
        BatchArchiveConfirmDialog(
            selectedCount = uiState.selectedPropertyIds.size,
            onConfirm = {
                viewModel.batchArchiveSelectedProperties()
                isBatchArchiveConfirmOpen = false
            },
            onDismiss = { isBatchArchiveConfirmOpen = false }
        )
    }

    // Dialog: Batch Delete Confirmation
    if (isBatchDeleteConfirmOpen) {
        BatchDeleteConfirmDialog(
            selectedCount = uiState.selectedPropertyIds.size,
            onConfirm = {
                viewModel.batchDeleteSelectedProperties()
                isBatchDeleteConfirmOpen = false
            },
            onDismiss = { isBatchDeleteConfirmOpen = false }
        )
    }

    // Bottom Sheet: Deep Opportunity Analysis
    propertyForOpportunityDetail?.let { (prop, eval) ->
        PropertyOpportunityDetailSheet(
            property = prop,
            evaluation = eval,
            isRefreshing = uiState.isRefreshingMarketData,
            onRefreshMarketComps = { viewModel.refreshLiveMarketCompsForProperties() },
            onDismiss = { propertyForOpportunityDetail = null }
        )
    }

    // Dialog: CSV Portfolio Manual Sync & Import
    if (isCsvSyncDialogOpen) {
        CsvPortfolioSyncDialog(
            onDismiss = { isCsvSyncDialogOpen = false },
            onConfirmSync = { propertiesToSync, mode, onDone ->
                viewModel.syncPortfolioFromProperties(propertiesToSync, mode) { summary ->
                    onDone(summary)
                }
            }
        )
    }

    // Dialog: Property Comparison Report PDF
    if (isComparisonPdfDialogOpen) {
        PropertyComparisonPdfDialog(
            allProperties = allProperties,
            initialPropertyA = comparisonPropA,
            initialPropertyB = comparisonPropB,
            evaluations = uiState.opportunityEvaluations,
            onDismiss = {
                isComparisonPdfDialogOpen = false
                comparisonPropA = null
                comparisonPropB = null
            },
            euroFormat = euroFormat
        )
    }

    // Modal Sheet / Dialog: Renovation Simulator & Computo Metrico
    propertyForRenovationSimulator?.let { prop ->
        val zonePrice = if (prop.surfaceSqm > 0) prop.price / prop.surfaceSqm else 2800.0
        val renoCost = if (prop.estimatedRenovationCost > 0) prop.estimatedRenovationCost else (prop.surfaceSqm * 380.0)
        Dialog(
            onDismissRequest = { propertyForRenovationSimulator = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkSlateBg)
            ) {
                RenovationSimulatorScreen(
                    initialPrice = prop.price,
                    initialSqm = if (prop.surfaceSqm > 0) prop.surfaceSqm.toDouble() else 75.0,
                    initialZonePricePerSqm = zonePrice,
                    propertyTitle = prop.title,
                    propertyAddress = prop.address,
                    onNavigateBack = { propertyForRenovationSimulator = null },
                    onApplyToProperty = { calculatedRenoCost ->
                        val updated = prop.copy(estimatedRenovationCost = calculatedRenoCost)
                        viewModel.updateProperty(updated)
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// KPI Header Banner (Simplified & Clean)
// -------------------------------------------------------------
@Composable
fun PortfolioKpiHeader(
    metrics: com.example.ui.PipelineMetrics,
    totalCount: Int,
    unreadAlertCount: Int = 0,
    euroFormat: NumberFormat,
    isTrendChartVisible: Boolean = false,
    onToggleTrendChart: () -> Unit = {},
    onOpenAlerts: () -> Unit,
    onExportCsv: () -> Unit,
    onSyncCsv: () -> Unit = {},
    onOpenComparisonPdf: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Clean Title + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "I Miei Immobili",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BentoPurpleHeader,
                        border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "$totalCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPurpleOnContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Trend Chart Toggle Button
                    IconButton(
                        onClick = onToggleTrendChart,
                        modifier = Modifier
                            .size(34.dp)
                            .border(
                                1.dp,
                                if (isTrendChartVisible) BentoPurpleOnContainer else SurfaceCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .background(
                                if (isTrendChartVisible) BentoPurpleHeader else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .testTag("toggle_trend_chart_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Grafico Trend",
                            modifier = Modifier.size(17.dp),
                            tint = BentoPurpleOnContainer
                        )
                    }

                    // Alerts Button
                    IconButton(
                        onClick = onOpenAlerts,
                        modifier = Modifier
                            .size(34.dp)
                            .border(
                                1.dp,
                                if (unreadAlertCount > 0) RoseRed.copy(alpha = 0.8f) else SurfaceCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .background(
                                if (unreadAlertCount > 0) RoseRed.copy(alpha = 0.12f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .testTag("open_portfolio_alerts_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadAlertCount > 0) {
                                    Badge(
                                        containerColor = RoseRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("$unreadAlertCount", fontSize = 8.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Allarmi Portafoglio",
                                modifier = Modifier.size(17.dp),
                                tint = if (unreadAlertCount > 0) RoseRed else BentoPurpleOnContainer
                            )
                        }
                    }

                    // CSV Import & Sync Button
                    IconButton(
                        onClick = onSyncCsv,
                        modifier = Modifier
                            .size(34.dp)
                            .border(1.dp, BentoPurpleOnContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .background(BentoPurpleHeader, RoundedCornerShape(8.dp))
                            .testTag("sync_portfolio_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SyncAlt,
                            contentDescription = "Sincronizza / Importa CSV",
                            modifier = Modifier.size(17.dp),
                            tint = BentoPurpleOnContainer
                        )
                    }

                    // Comparison PDF Report Button
                    IconButton(
                        onClick = onOpenComparisonPdf,
                        modifier = Modifier
                            .size(34.dp)
                            .border(1.dp, BentoPurpleOnContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .background(BentoPurpleHeader, RoundedCornerShape(8.dp))
                            .testTag("open_comparison_pdf_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Dossier Confronto PDF",
                            modifier = Modifier.size(17.dp),
                            tint = BentoPurpleOnContainer
                        )
                    }

                    // CSV Export Button
                    IconButton(
                        onClick = onExportCsv,
                        modifier = Modifier
                            .size(34.dp)
                            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
                            .testTag("export_pipeline_csv_button")
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Esporta CSV",
                            modifier = Modifier.size(17.dp),
                            tint = BentoPurpleOnContainer
                        )
                    }
                }
            }

            // Clean 2x2 Metric Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Valore Totale
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF9F8FB),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Valore Portafoglio", fontSize = 11.sp, color = TextMutedDark)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = euroFormat.format(metrics.totalEstimatedExitValue),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                }

                // Margine Netto Previsto
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldGainBg,
                    border = BorderStroke(1.dp, EmeraldGainBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Margine Netto", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "+${euroFormat.format(metrics.totalProjectedProfit)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen
                        )
                    }
                }
            }

            // Quick Stage Counter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF4F0F8))
                    .padding(vertical = 5.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StageCounterPill("Analizzati", metrics.analyzedCount, CyanAccent)
                StageCounterPill("In Escrow", metrics.activeEscrowDealCount, AmberGold)
                StageCounterPill("Cantiere", metrics.activeRenovatingCount, BentoPurpleOnContainer)
                StageCounterPill("In Vendita", metrics.listedCount, PurpleIndigo)
                StageCounterPill("Venduti", metrics.soldCompletedCount, EmeraldGreen)
            }
        }
    }
}

@Composable
fun KpiMetricTile(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondaryDark)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
            Text(subtitle, fontSize = 10.sp, color = TextMutedDark)
        }
    }
}

@Composable
fun StageCounterPill(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text("$label: ", fontSize = 11.sp, color = TextSecondaryDark)
        Text("$count", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
    }
}

// -------------------------------------------------------------
// Pipeline Status Filter Chips Row
// -------------------------------------------------------------
@Composable
fun PipelineStatusFilterChipsRow(
    selectedFilter: String,
    allProperties: List<Property>,
    onSelectFilter: (String) -> Unit
) {
    val filters = listOf(
        Triple("ALL", "Tutti (${allProperties.size})", Icons.Default.AllInclusive),
        Triple(PipelineStatus.ANALYZED.key, "Analizzati (${allProperties.count { it.pipelineStatus == PipelineStatus.ANALYZED.key }})", Icons.Default.Search),
        Triple(PipelineStatus.IN_ESCROW.key, "In Escrow (${allProperties.count { it.pipelineStatus == PipelineStatus.IN_ESCROW.key }})", Icons.Default.Gavel),
        Triple(PipelineStatus.RENOVATING.key, "In Ristrutturazione (${allProperties.count { it.pipelineStatus == PipelineStatus.RENOVATING.key }})", Icons.Default.Build),
        Triple(PipelineStatus.LISTED.key, "In Vendita (${allProperties.count { it.pipelineStatus == PipelineStatus.LISTED.key }})", Icons.Default.Storefront),
        Triple(PipelineStatus.RENTED.key, "A Reddito (${allProperties.count { it.pipelineStatus == PipelineStatus.RENTED.key }})", Icons.Default.Key),
        Triple(PipelineStatus.SOLD.key, "Venduti (${allProperties.count { it.pipelineStatus == PipelineStatus.SOLD.key }})", Icons.Default.CheckCircle),
        Triple(PipelineStatus.ARCHIVED.key, "Archiviati (${allProperties.count { it.pipelineStatus == PipelineStatus.ARCHIVED.key }})", Icons.Default.Archive)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(filters) { (key, label, icon) ->
            val isSelected = selectedFilter == key
            FilterChip(
                selected = isSelected,
                onClick = { onSelectFilter(key) },
                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) BentoPurpleOnContainer else TextSecondaryDark
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BentoPurpleHeader,
                    selectedLabelColor = BentoPurpleOnContainer,
                    containerColor = SurfaceCardDark,
                    labelColor = TextPrimaryDark
                ),
                border = BorderStroke(1.dp, if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder),
                modifier = Modifier.testTag("pipeline_filter_chip_$key")
            )
        }
    }
}

// -------------------------------------------------------------
// Pipeline Property Card
// -------------------------------------------------------------
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PipelinePropertyCard(
    property: Property,
    euroFormat: NumberFormat,
    evaluation: PropertyOpportunityEvaluation? = null,
    onOpenOpportunityDetail: ((PropertyOpportunityEvaluation) -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionModeActive: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onToggleSelection: () -> Unit = {},
    onCardClick: () -> Unit = {},
    onOpenDetailClick: () -> Unit = {},
    onUpdateStatusClick: () -> Unit,
    onUpdateProgressClick: () -> Unit,
    onEditFinancialsClick: () -> Unit,
    onSimulatePriceDropClick: () -> Unit = {},
    onCalculateRoiClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrowRotation"
    )

    val status = property.currentStatus
    val statusColor = when (status) {
        PipelineStatus.ANALYZED -> CyanAccent
        PipelineStatus.IN_ESCROW -> AmberGold
        PipelineStatus.RENOVATING -> BentoPurpleOnContainer
        PipelineStatus.LISTED -> PurpleIndigo
        PipelineStatus.RENTED -> Color(0xFF00796B)
        PipelineStatus.SOLD -> EmeraldGreen
        PipelineStatus.ARCHIVED -> Color(0xFF64748B)
    }

    val statusBg = when (status) {
        PipelineStatus.ANALYZED -> BentoPurpleHeader
        PipelineStatus.IN_ESCROW -> Color(0xFFFFF3E0)
        PipelineStatus.RENOVATING -> BentoPurpleContainer
        PipelineStatus.LISTED -> Color(0xFFEDE7F6)
        PipelineStatus.RENTED -> Color(0xFFE0F2F1)
        PipelineStatus.SOLD -> Color(0xFFE8F5E9)
        PipelineStatus.ARCHIVED -> Color(0xFFF1F5F9)
    }

    val typeIcon = when {
        property.propertyType.contains("Villa", ignoreCase = true) -> Icons.Default.Villa
        property.propertyType.contains("Commerciale", ignoreCase = true) || property.propertyType.contains("Ufficio", ignoreCase = true) -> Icons.Default.Business
        else -> Icons.Default.Home
    }

    val cardBorderColor = if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder
    val cardBorderWidth = if (isSelected) 2.dp else 1.dp
    val cardBg = if (isSelected) BentoPurpleContainer.copy(alpha = 0.25f) else SurfaceCardDark

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(cardBorderWidth, cardBorderColor, RoundedCornerShape(16.dp))
            .sharedBoundsIfAvailable(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                key = "property_card_${property.id}"
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .clickable {
                if (isSelectionModeActive) {
                    onToggleSelection()
                } else {
                    onCardClick()
                }
            }
            .testTag("property_pipeline_card_${property.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Top Row: Type Icon / Checkbox + Title & Address + Interactive Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isSelectionModeActive) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelection() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BentoPurpleOnContainer,
                                uncheckedColor = TextSecondaryDark,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.testTag("select_property_checkbox_${property.id}")
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = statusBg,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val provenanceEnum = com.example.data.DataProvenance.fromString(property.provenance)
                            if (!provenanceEnum.isTrustworthy) {
                                Surface(
                                    color = RoseRed,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = provenanceEnum.label.uppercase(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = property.title.ifBlank { property.address },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (property.latitude == 0.0 && property.longitude == 0.0) AmberGold else TextMutedDark,
                                modifier = Modifier.size(12.dp)
                            )
                            val locationSubtitle = when {
                                property.latitude == 0.0 && property.longitude == 0.0 ->
                                    "${property.address} (Posizione non disponibile)"
                                property.evidenceRef?.contains("approssimata") == true ->
                                    "${property.address} (Posizione approssimata al comune)"
                                else -> property.address
                            }
                            Text(
                                text = locationSubtitle,
                                fontSize = 12.sp,
                                color = TextSecondaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Interactive Status Badge (Click to change status)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable { onUpdateStatusClick() }
                        .testTag("status_badge_btn_${property.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = status.labelIt,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // 2. Clean Minimal Summary Row (Essential Numbers)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF9F7FA),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prezzo Acquisto
                    Column {
                        Text("Acquisto", fontSize = 10.sp, color = TextMutedDark)
                        Text(
                            text = euroFormat.format(property.price),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }

                    // Target Uscita
                    Column {
                        Text("Target Uscita", fontSize = 10.sp, color = TextMutedDark)
                        Text(
                            text = euroFormat.format(property.effectiveExitValue),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }

                    // Margine Previsto
                    Column {
                        Text("Margine", fontSize = 10.sp, color = TextMutedDark)
                        Text(
                            text = "+${euroFormat.format(property.projectedProfit)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen
                        )
                    }

                    // ROI Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldGainBg,
                        border = BorderStroke(1.dp, EmeraldGainBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = EmeraldGainText,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = String.format(Locale.ITALY, "ROI +%.1f%%", property.projectedRoiPercent),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGainText
                            )
                        }
                    }
                }
            }

            // Real-Time Opportunity Score Badge (Immobiliare.it Comps)
            evaluation?.let { eval ->
                val tierColor = Color(eval.tier.colorHex)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = tierColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, tierColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenOpportunityDetail?.invoke(eval) }
                        .testTag("opportunity_score_pill_${property.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = tierColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Score ${eval.opportunityScore}/100 • ${eval.tier.label}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = tierColor
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (eval.undervaluedPercent >= 0)
                                    "-${String.format(Locale.US, "%.0f", eval.undervaluedPercent)}% vs Mercato"
                                else
                                    "+${String.format(Locale.US, "%.0f", -eval.undervaluedPercent)}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tierColor
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Dettagli Opportunità",
                                tint = tierColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Optional: Renovation Progress Quick Strip in Collapsed mode
            if (status == PipelineStatus.RENOVATING && !isExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { property.renovationProgressPercent / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = BentoPurpleOnContainer,
                        trackColor = Color(0xFFEDE7F6)
                    )
                    Text(
                        text = "Lavori ${property.renovationProgressPercent}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoPurpleOnContainer
                    )
                }
            }

            // 3. Compact Meta & Expand Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Meta Tags: Type, Sqm, Renovation pill, Date
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (property.surfaceSqm > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF3EDF7)
                        ) {
                            Text(
                                text = "${property.surfaceSqm} mq",
                                fontSize = 10.sp,
                                color = TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = formatPropertyRelativeDate(property.createdAt),
                        fontSize = 10.sp,
                        color = TextMutedDark,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Expand / Collapse Chevron with Label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag("expand_card_btn_${property.id}")
                ) {
                    Text(
                        text = if (isExpanded) "Meno dettagli" else "Dettagli",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BentoPurpleOnContainer
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Comprimi scheda" else "Espandi scheda",
                        tint = BentoPurpleOnContainer,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(arrowRotation)
                    )
                }
            }

            // 4. Expanded Detailed Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.5f))

                    // Detailed Financial Breakdown
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF7F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Quadro Economico Dettagliato",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondaryDark
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FinancialItem(label = "Prezzo Acquisto", value = euroFormat.format(property.price))
                                FinancialItem(
                                    label = "Lavori (${if (property.actualRenovationCost > 0) "Spesi" else "Budget"})",
                                    value = euroFormat.format(if (property.actualRenovationCost > 0) property.actualRenovationCost else property.estimatedRenovationCost)
                                )
                                FinancialItem(
                                    label = "Target Uscita",
                                    value = euroFormat.format(property.effectiveExitValue)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FinancialItem(
                                    label = "Margine Netto",
                                    value = "+${euroFormat.format(property.projectedProfit)}",
                                    highlightColor = EmeraldGreen
                                )
                                FinancialItem(
                                    label = "ROI Previsto",
                                    value = String.format(Locale.ITALY, "+%.1f%%", property.projectedRoiPercent),
                                    highlightColor = EmeraldGreen
                                )
                                if (property.projectedRentalIncome > 0) {
                                    FinancialItem(
                                        label = "Rendita Locativa",
                                        value = "€${property.projectedRentalIncome.toInt()}/mese",
                                        highlightColor = Color(0xFF00796B)
                                    )
                                } else {
                                    FinancialItem(
                                        label = "Tipologia",
                                        value = property.propertyType
                                    )
                                }
                            }
                        }
                    }

                    // Status-Specific Detail Block
                    when (status) {
                        PipelineStatus.RENOVATING -> {
                            RenovationProgressBlock(
                                progress = property.renovationProgressPercent,
                                notes = property.contractorNotes,
                                onEditProgress = onUpdateProgressClick
                            )
                        }

                        PipelineStatus.IN_ESCROW -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFF8E1),
                                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("Trattativa & Rogito in Corso", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                                        Text(
                                            text = if (property.escrowClosingDate.isNotBlank()) "Data prevista closing: ${property.escrowClosingDate}" else "Data closing da definire con notaio",
                                            fontSize = 11.sp,
                                            color = TextPrimaryDark
                                        )
                                    }
                                }
                            }
                        }

                        PipelineStatus.SOLD -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text("Operazione Conclusa con Successo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                            Text("Prezzo incassato: ${euroFormat.format(property.effectiveExitValue)}", fontSize = 11.sp, color = TextPrimaryDark)
                                        }
                                    }
                                    Text(
                                        text = String.format(Locale.ITALY, "ROI +%.1f%%", property.projectedRoiPercent),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldGreen
                                    )
                                }
                            }
                        }

                        PipelineStatus.RENTED -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE0F2F1),
                                border = BorderStroke(1.dp, Color(0xFF00796B).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF00796B), modifier = Modifier.size(20.dp))
                                        Column {
                                            Text("Immobile Messo a Reddito", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00796B))
                                            Text("Flusso di cassa locativo attivo", fontSize = 11.sp, color = TextPrimaryDark)
                                        }
                                    }
                                    Text(
                                        text = if (property.projectedRentalIncome > 0) "€${property.projectedRentalIncome.toInt()}/mese" else "A rendita",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00796B)
                                    )
                                }
                            }
                        }

                        PipelineStatus.ARCHIVED -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("Immobile Archiviato", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                        Text("Puoi ripristinarlo o aggiornarne lo stato in qualsiasi momento.", fontSize = 11.sp, color = TextSecondaryDark)
                                    }
                                }
                            }
                        }

                        PipelineStatus.ANALYZED, PipelineStatus.LISTED -> {
                            if (property.notes.isNotBlank()) {
                                Text(
                                    text = property.notes,
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Action Buttons Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = onEditFinancialsClick,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = BentoPurpleHeader,
                                    contentColor = BentoPurpleOnContainer
                                ),
                                modifier = Modifier.testTag("edit_financials_btn_${property.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Modifica", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onCalculateRoiClick,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, SurfaceCardBorder),
                                modifier = Modifier.testTag("calc_roi_btn_${property.id}")
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(15.dp), tint = BentoPurpleOnContainer)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ROI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoPurpleOnContainer)
                            }

                            IconButton(
                                onClick = onExportPdfClick,
                                modifier = Modifier
                                    .size(34.dp)
                                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
                                    .testTag("export_pdf_btn_${property.id}")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Esporta Dossier PDF", tint = BentoPurpleOnContainer, modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = onSimulatePriceDropClick,
                                modifier = Modifier
                                    .size(34.dp)
                                    .border(1.dp, EmeraldGainBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .background(EmeraldGainBg.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .testTag("price_alert_btn_${property.id}")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = "Simula Ribasso Prezzo & Notifica",
                                    tint = EmeraldGainText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Elimina", tint = RoseRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialItem(
    label: String,
    value: String,
    highlightColor: Color = TextPrimaryDark
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(label, fontSize = 10.sp, color = TextMutedDark)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = highlightColor)
    }
}

@Composable
fun RenovationProgressBlock(
    progress: Int,
    notes: String,
    onEditProgress: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BentoPurpleHeader.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditProgress() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = BentoPurpleOnContainer, modifier = Modifier.size(16.dp))
                    Text("Avanzamento Cantiere", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoPurpleOnContainer)
                }
                Text("$progress%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = BentoPurpleOnContainer)
            }

            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = BentoPurpleOnContainer,
                trackColor = Color.White
            )

            if (notes.isNotBlank()) {
                Text(
                    text = notes,
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Tocca per aggiornare percentuale e spese cantiere...",
                    fontSize = 10.sp,
                    color = TextMutedDark
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Kanban Pipeline Board
// -------------------------------------------------------------
@Composable
fun KanbanPipelineBoard(
    properties: List<Property>,
    euroFormat: NumberFormat,
    selectedPropertyIds: Set<Long> = emptySet(),
    isSelectionModeActive: Boolean = false,
    onToggleSelection: (Long) -> Unit = {},
    onMoveStatus: (Property, PipelineStatus) -> Unit,
    onPropertyClick: (Property) -> Unit
) {
    val stages = listOf(
        PipelineStatus.ANALYZED,
        PipelineStatus.IN_ESCROW,
        PipelineStatus.RENOVATING,
        PipelineStatus.LISTED,
        PipelineStatus.RENTED,
        PipelineStatus.SOLD
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(540.dp)
            .testTag("kanban_board_container")
    ) {
        items(stages) { stage ->
            val stageProperties = properties.filter { it.pipelineStatus == stage.key }
            KanbanStageColumn(
                stage = stage,
                properties = stageProperties,
                euroFormat = euroFormat,
                selectedPropertyIds = selectedPropertyIds,
                isSelectionModeActive = isSelectionModeActive,
                onToggleSelection = onToggleSelection,
                onMoveStatus = onMoveStatus,
                onPropertyClick = onPropertyClick
            )
        }
    }
}

@Composable
fun KanbanStageColumn(
    stage: PipelineStatus,
    properties: List<Property>,
    euroFormat: NumberFormat,
    selectedPropertyIds: Set<Long> = emptySet(),
    isSelectionModeActive: Boolean = false,
    onToggleSelection: (Long) -> Unit = {},
    onMoveStatus: (Property, PipelineStatus) -> Unit,
    onPropertyClick: (Property) -> Unit
) {
    val stageColor = when (stage) {
        PipelineStatus.ANALYZED -> CyanAccent
        PipelineStatus.IN_ESCROW -> AmberGold
        PipelineStatus.RENOVATING -> BentoPurpleOnContainer
        PipelineStatus.LISTED -> PurpleIndigo
        PipelineStatus.RENTED -> Color(0xFF00796B)
        PipelineStatus.SOLD -> EmeraldGreen
        PipelineStatus.ARCHIVED -> Color(0xFF64748B)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F2F7)),
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Column Header
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
                            .background(stageColor)
                    )
                    Text(
                        text = stage.labelIt,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Text(
                        text = "${properties.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = stageColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.4f))

            if (properties.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nessun immobile", fontSize = 11.sp, color = TextMutedDark)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(properties, key = { it.id }) { prop ->
                        val isSelected = selectedPropertyIds.contains(prop.id)
                        KanbanItemCard(
                            property = prop,
                            euroFormat = euroFormat,
                            currentStage = stage,
                            isSelected = isSelected,
                            isSelectionModeActive = isSelectionModeActive,
                            onToggleSelection = { onToggleSelection(prop.id) },
                            onMovePrev = {
                                val prevIndex = stage.ordinal - 1
                                if (prevIndex >= 0) {
                                    onMoveStatus(prop, PipelineStatus.values()[prevIndex])
                                }
                            },
                            onMoveNext = {
                                val nextIndex = stage.ordinal + 1
                                if (nextIndex < PipelineStatus.values().size) {
                                    onMoveStatus(prop, PipelineStatus.values()[nextIndex])
                                }
                            },
                            onClick = { onPropertyClick(prop) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KanbanItemCard(
    property: Property,
    euroFormat: NumberFormat,
    currentStage: PipelineStatus,
    isSelected: Boolean = false,
    isSelectionModeActive: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onMovePrev: () -> Unit,
    onMoveNext: () -> Unit,
    onClick: () -> Unit
) {
    val cardBorderColor = if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder.copy(alpha = 0.6f)
    val cardBorderWidth = if (isSelected) 2.dp else 1.dp
    val cardBg = if (isSelected) BentoPurpleContainer.copy(alpha = 0.35f) else Color.White

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(cardBorderWidth, cardBorderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isSelectionModeActive) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = BentoPurpleOnContainer,
                            uncheckedColor = TextSecondaryDark,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = property.title.ifBlank { property.address },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = property.address,
                fontSize = 11.sp,
                color = TextSecondaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Prezzo: ${euroFormat.format(property.price)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                Text(
                    text = String.format(Locale.ITALY, "+%.1f%%", property.projectedRoiPercent),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }

            if (currentStage == PipelineStatus.RENOVATING) {
                LinearProgressIndicator(
                    progress = { property.renovationProgressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = BentoPurpleOnContainer,
                    trackColor = Color(0xFFE8DEF8)
                )
            }

            // Move Prev / Next Stepper Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStage.ordinal > 0) {
                    IconButton(
                        onClick = onMovePrev,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fase precedente", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.size(24.dp))
                }

                Text(
                    text = "${property.surfaceSqm} mq",
                    fontSize = 10.sp,
                    color = TextMutedDark
                )

                if (currentStage.ordinal < PipelineStatus.values().size - 1) {
                    IconButton(
                        onClick = onMoveNext,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Fase successiva", tint = BentoPurpleOnContainer, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Pipeline Ledger Table
// -------------------------------------------------------------
@Composable
fun PipelineLedgerTable(
    properties: List<Property>,
    euroFormat: NumberFormat,
    selectedPropertyIds: Set<Long> = emptySet(),
    isSelectionModeActive: Boolean = false,
    onToggleSelection: (Long) -> Unit = {},
    onRowClick: (Property) -> Unit,
    onStatusClick: (Property) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            .testTag("ledger_table_container")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ledger Dettagliato Immobili", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                if (isSelectionModeActive) {
                    Text(
                        "${selectedPropertyIds.size}/${properties.size} Selezionati",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoPurpleOnContainer
                    )
                }
            }

            HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.5f))

            properties.forEachIndexed { index, prop ->
                val isSelected = selectedPropertyIds.contains(prop.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) BentoPurpleContainer.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { onRowClick(prop) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionModeActive) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelection(prop.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BentoPurpleOnContainer,
                                uncheckedColor = TextSecondaryDark,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = prop.title.ifBlank { prop.address },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = prop.address,
                            fontSize = 10.sp,
                            color = TextMutedDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(euroFormat.format(prop.price), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        Text("+${euroFormat.format(prop.projectedProfit)}", fontSize = 10.sp, color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BentoPurpleHeader,
                        modifier = Modifier.clickable { onStatusClick(prop) }
                    ) {
                        Text(
                            text = prop.currentStatus.labelIt,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPurpleOnContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (index < properties.size - 1) {
                    HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.3f))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Empty Pipeline Placeholder
// -------------------------------------------------------------
@Composable
fun EmptyPipelinePlaceholder(
    searchQuery: String,
    selectedFilter: String,
    onResetFilters: () -> Unit,
    onAddProperty: () -> Unit,
    onStrategySelected: ((String) -> Unit)? = null,
    onLoadDemoTemplate: ((String) -> Unit)? = null,
    onSyncCsv: (() -> Unit)? = null
) {
    IllustrativeEmptyPortfolioState(
        searchQuery = searchQuery,
        selectedFilter = selectedFilter,
        onResetFilters = onResetFilters,
        onAddProperty = onAddProperty,
        onStrategySelected = onStrategySelected,
        onLoadDemoTemplate = onLoadDemoTemplate,
        onSyncCsv = onSyncCsv
    )
}

// -------------------------------------------------------------
// DIALOG: Change Pipeline Status
// -------------------------------------------------------------
data class ExtraStatusData(
    val actualSalePrice: Double = 0.0,
    val escrowClosingDate: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePipelineStatusDialog(
    property: Property,
    onDismiss: () -> Unit,
    onStatusSelected: (PipelineStatus, ExtraStatusData) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(property.currentStatus) }
    var salePriceText by remember { mutableStateOf(if (property.actualSalePrice > 0) property.actualSalePrice.toInt().toString() else if (property.targetResalePrice > 0) property.targetResalePrice.toInt().toString() else "") }
    var escrowDateText by remember { mutableStateOf(property.escrowClosingDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = BentoPurpleOnContainer)
                Text("Aggiorna Stato Pipeline", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Seleziona la fase di avanzamento per '${property.title.ifBlank { property.address }}':",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )

                PipelineStatus.values().forEach { stage ->
                    val isCurrent = selectedStatus == stage
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrent) BentoPurpleHeader else Color(0xFFF9F7FA),
                        border = BorderStroke(1.dp, if (isCurrent) BentoPurpleOnContainer else SurfaceCardBorder.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = stage }
                            .testTag("status_option_${stage.key}")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = isCurrent,
                                onClick = { selectedStatus = stage }
                            )
                            Column {
                                Text(stage.labelIt, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                Text(stage.description, fontSize = 11.sp, color = TextSecondaryDark)
                            }
                        }
                    }
                }

                if (selectedStatus == PipelineStatus.SOLD) {
                    OutlinedTextField(
                        value = salePriceText,
                        onValueChange = { salePriceText = it },
                        label = { Text("Prezzo Effettivo di Vendita (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedStatus == PipelineStatus.IN_ESCROW) {
                    OutlinedTextField(
                        value = escrowDateText,
                        onValueChange = { escrowDateText = it },
                        label = { Text("Data Prevista Rogito (gg/mm/aaaa)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onStatusSelected(
                        selectedStatus,
                        ExtraStatusData(
                            actualSalePrice = salePriceText.toDoubleOrNull() ?: 0.0,
                            escrowClosingDate = escrowDateText.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOnContainer),
                modifier = Modifier.testTag("confirm_status_update_btn")
            ) {
                Text("Conferma Aggiornamento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

// -------------------------------------------------------------
// DIALOG: Update Renovation Progress
// -------------------------------------------------------------
@Composable
fun UpdateRenovationProgressDialog(
    property: Property,
    onDismiss: () -> Unit,
    onSave: (progressPercent: Int, actualCost: Double, notes: String) -> Unit
) {
    var progressSlider by remember { mutableFloatStateOf(property.renovationProgressPercent.toFloat()) }
    var costText by remember { mutableStateOf(if (property.actualRenovationCost > 0) property.actualRenovationCost.toInt().toString() else if (property.estimatedRenovationCost > 0) property.estimatedRenovationCost.toInt().toString() else "") }
    var notesText by remember { mutableStateOf(property.contractorNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = BentoPurpleOnContainer)
                Text("Avanzamento Cantiere", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Aggiorna stato avanzamento e costi per '${property.title}':",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )

                // Progress Percentage Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Percentuale Completamento:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${progressSlider.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = BentoPurpleOnContainer)
                    }

                    Slider(
                        value = progressSlider,
                        onValueChange = { progressSlider = it },
                        valueRange = 0f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = BentoPurpleOnContainer,
                            activeTrackColor = BentoPurpleOnContainer
                        ),
                        modifier = Modifier.testTag("renovation_progress_slider")
                    )

                    // Quick Step Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(0, 25, 50, 75, 100).forEach { pct ->
                            OutlinedButton(
                                onClick = { progressSlider = pct.toFloat() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("$pct%", fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Actual Cost Spent Field
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Costi di Ristrutturazione Effettivi (€)") },
                    placeholder = { Text("es. 45000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Contractor / Site Notes Field
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Note di Cantiere / Ditta") },
                    placeholder = { Text("es. Impianti completati, posa pavimenti in corso...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        progressSlider.toInt(),
                        costText.toDoubleOrNull() ?: 0.0,
                        notesText.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOnContainer),
                modifier = Modifier.testTag("save_renovation_progress_btn")
            ) {
                Text("Salva Avanzamento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

// -------------------------------------------------------------
// DIALOG: Edit Full Property Details & Financials
// -------------------------------------------------------------
@Composable
fun EditPropertyFinancialsDialog(
    property: Property,
    onDismiss: () -> Unit,
    onSave: (Property) -> Unit
) {
    var title by remember { mutableStateOf(property.title) }
    var address by remember { mutableStateOf(property.address) }
    var priceText by remember { mutableStateOf(property.price.toInt().toString()) }
    var renoCostText by remember { mutableStateOf(property.estimatedRenovationCost.toInt().toString()) }
    var targetResaleText by remember { mutableStateOf(property.targetResalePrice.toInt().toString()) }
    var rentalIncomeText by remember { mutableStateOf(if (property.projectedRentalIncome > 0) property.projectedRentalIncome.toInt().toString() else "") }
    var surfaceText by remember { mutableStateOf(if (property.surfaceSqm > 0) property.surfaceSqm.toString() else "") }
    var notes by remember { mutableStateOf(property.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica Dati Immobile", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titolo / Identificativo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Indirizzo Immobile") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Prezzo Acquisto (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = renoCostText,
                            onValueChange = { renoCostText = it },
                            label = { Text("Budget Lavori (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetResaleText,
                            onValueChange = { targetResaleText = it },
                            label = { Text("Target Rivendita (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = surfaceText,
                            onValueChange = { surfaceText = it },
                            label = { Text("Superficie (mq)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = rentalIncomeText,
                        onValueChange = { rentalIncomeText = it },
                        label = { Text("Affitto Mensile Previsto (€/mese)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Note Analisi / Strategia") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = property.copy(
                        title = title.trim(),
                        address = address.trim(),
                        price = priceText.toDoubleOrNull() ?: property.price,
                        estimatedRenovationCost = renoCostText.toDoubleOrNull() ?: property.estimatedRenovationCost,
                        targetResalePrice = targetResaleText.toDoubleOrNull() ?: property.targetResalePrice,
                        projectedRentalIncome = rentalIncomeText.toDoubleOrNull() ?: 0.0,
                        surfaceSqm = surfaceText.toIntOrNull() ?: property.surfaceSqm,
                        notes = notes.trim()
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOnContainer)
            ) {
                Text("Salva Modifiche")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

// -------------------------------------------------------------
// DIALOG: Add New Analyzed Property
// -------------------------------------------------------------
@Composable
fun AddNewAnalyzedPropertyDialog(
    initialStrategy: String = "Fix & Flip",
    onDismiss: () -> Unit,
    onSave: (Property) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var renoCostText by remember { mutableStateOf("") }
    var targetResaleText by remember { mutableStateOf("") }
    var surfaceText by remember { mutableStateOf("") }
    var propertyType by remember { mutableStateOf("Appartamento") }
    var strategyTags by remember { mutableStateOf(initialStrategy) }
    var selectedStatus by remember { mutableStateOf(PipelineStatus.ANALYZED) }
    var notes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AddHomeWork, contentDescription = null, tint = BentoPurpleOnContainer)
                Text("Aggiungi a My Properties", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titolo / Nome Operazione *") },
                        placeholder = { Text("es. Trilocale Navigli da Ristrutturare") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            errorMessage = null
                        },
                        label = { Text("Indirizzo Completo *") },
                        placeholder = { Text("es. Via Savona 18, Milano o Paderno Dugnano") },
                        isError = errorMessage != null && address.isBlank(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (address.length >= 3) {
                    item {
                        val liveData = remember(address) { ImmobiliareObservatoryService.findMarketData(address) }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BentoCardBgLight,
                            border = BorderStroke(1.dp, if (liveData.isGenericFallback) RoseRed.copy(alpha = 0.4f) else CyanAccent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "📍 ${liveData.municipalityName} (${liveData.province})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (liveData.isGenericFallback) RoseRed.copy(alpha = 0.15f) else AmberGold.copy(alpha = 0.15f),
                                            border = BorderStroke(0.6.dp, if (liveData.isGenericFallback) RoseRed else AmberGold)
                                        ) {
                                            Text(
                                                text = if (liveData.isGenericFallback) "FALLBACK GENERICO" else "FALLBACK CURATO",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (liveData.isGenericFallback) RoseRed else AmberGold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            val sqm = surfaceText.toDoubleOrNull() ?: 80.0
                                            if (surfaceText.isBlank()) surfaceText = "80"
                                            targetResaleText = (sqm * liveData.maxSalePricePerSqM * 0.95).toInt().toString()
                                            renoCostText = (sqm * 400.0).toInt().toString()
                                            if (priceText.isBlank()) {
                                                priceText = (sqm * liveData.avgSalePricePerSqM * 0.70).toInt().toString()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("✨ Auto-Stima", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                                    }
                                }

                                Text(
                                    text = "Media: €${liveData.avgSalePricePerSqM.toInt()}/m² • Affitto: €${liveData.avgRentPricePerSqMMonth}/m² • Yield: ${String.format(Locale.ITALY, "%.1f", liveData.grossRentalYield)}%",
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark
                                )

                                if (liveData.isGenericFallback) {
                                    Text(
                                        text = "⚠️ Comune non presente nei preset: parametri stimati su media generica non verificata.",
                                        fontSize = 9.sp,
                                        color = RoseRed
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Prezzo Acquisto (€) *") },
                            placeholder = { Text("240000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = renoCostText,
                            onValueChange = { renoCostText = it },
                            label = { Text("Budget Lavori (€)") },
                            placeholder = { Text("35000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetResaleText,
                            onValueChange = { targetResaleText = it },
                            label = { Text("Target Rivendita (€)") },
                            placeholder = { Text("340000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = surfaceText,
                            onValueChange = { surfaceText = it },
                            label = { Text("Mq") },
                            placeholder = { Text("75") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("Fase Iniziale Pipeline:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryDark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PipelineStatus.values().forEach { st ->
                            val isSel = selectedStatus == st
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedStatus = st },
                                label = { Text(st.labelIt, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Note Strategia / Cantiere") },
                        placeholder = { Text("es. Potenziale frazionamento in due unità...") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                errorMessage?.let { err ->
                    item {
                        Text(err, fontSize = 12.sp, color = RoseRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (address.isBlank() || priceText.isBlank()) {
                        errorMessage = "Inserisci almeno indirizzo e prezzo d'acquisto."
                        return@Button
                    }
                    val now = System.currentTimeMillis()
                    val prop = Property(
                        title = title.trim().ifBlank { address.trim() },
                        address = address.trim(),
                        price = priceText.toDoubleOrNull() ?: 0.0,
                        distressStatus = "Analizzato",
                        propertyType = propertyType,
                        estimatedMarketValue = targetResaleText.toDoubleOrNull() ?: (priceText.toDoubleOrNull() ?: 0.0),
                        surfaceSqm = surfaceText.toIntOrNull() ?: 0,
                        notes = notes.trim(),
                        strategyTags = strategyTags,
                        pipelineStatus = selectedStatus.key,
                        estimatedRenovationCost = renoCostText.toDoubleOrNull() ?: 0.0,
                        targetResalePrice = targetResaleText.toDoubleOrNull() ?: 0.0,
                        provenance = com.example.data.DataProvenance.USER_ENTERED.name,
                        retrievedAt = now,
                        createdAt = now
                    )
                    onSave(prop)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOnContainer),
                modifier = Modifier.testTag("save_new_analyzed_property_btn")
            ) {
                Text("Salva in Pipeline")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun SimulatePriceDropDialog(
    property: Property,
    euroFormat: NumberFormat,
    onDismiss: () -> Unit,
    onApplyDrop: (Double) -> Unit
) {
    var selectedPercent by remember { mutableDoubleStateOf(8.0) }
    var isCustom by remember { mutableStateOf(false) }
    var customPercentText by remember { mutableStateOf("10") }

    val effectivePercent = if (isCustom) {
        customPercentText.toDoubleOrNull() ?: 8.0
    } else {
        selectedPercent
    }

    val dropAmount = property.price * (effectivePercent / 100.0)
    val newPrice = (property.price - dropAmount).coerceAtLeast(1000.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCardDark,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(EmeraldGainBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = EmeraldGainText,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Simula Ribasso di Prezzo",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Simula un ribasso di mercato su '${property.title.ifBlank { property.address }}' per verificare l'allarme push e il ricalcolo istantaneo del ROI:",
                    fontSize = 12.sp,
                    color = TextSecondaryDark,
                    lineHeight = 16.sp
                )

                // Price Comparison Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoCardBgLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Prezzo Attuale", fontSize = 11.sp, color = TextSecondaryDark)
                            Text(euroFormat.format(property.price), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = EmeraldGainText, modifier = Modifier.size(18.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Nuovo Prezzo", fontSize = 11.sp, color = EmeraldGainText, fontWeight = FontWeight.SemiBold)
                            Text(euroFormat.format(newPrice), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGainText)
                        }
                    }
                }

                // Percentage Chips
                Text("Seleziona Percentuale Ribasso:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5.0, 8.0, 10.0, 15.0).forEach { pct ->
                        val isSelected = !isCustom && selectedPercent == pct
                        OutlinedButton(
                            onClick = {
                                isCustom = false
                                selectedPercent = pct
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) EmeraldGainBg else Color.Transparent,
                                contentColor = if (isSelected) EmeraldGainText else TextSecondaryDark
                            ),
                            border = BorderStroke(1.dp, if (isSelected) EmeraldGainText else SurfaceCardBorder),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-${pct.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Delta Highlight
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldGainBg.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, EmeraldGainBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Risparmio sul Prezzo:", fontSize = 11.sp, color = EmeraldGainText)
                        Text("-${euroFormat.format(dropAmount)} (-${effectivePercent.toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGainText)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyDrop(effectivePercent) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGainText),
                modifier = Modifier.testTag("confirm_simulate_drop_btn")
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Applica & Invia Allarme", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = TextSecondaryDark)
            }
        }
    )
}

