package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PropertyDeal
import com.example.ui.FilterCategory
import com.example.ui.UiState
import com.example.ui.components.DealCard
import com.example.ui.components.DealKanbanBoard
import com.example.ui.components.RecentSearchesBar
import com.example.ui.components.QuickAccessDealsBar
import com.example.ui.components.IllustrativeEmptySearchState
import com.example.ui.components.ImmobiliareObservatoryDialog
import com.example.ui.components.BlindDealUnlockDialog
import com.example.ui.components.UpgradeToPremiumModal
import com.example.ui.components.PropertyOfferDialog
import com.example.ui.components.BlindModeBanner
import com.example.ui.components.PropertySearchCoachMarkOverlay
import com.example.ui.theme.*
import com.example.util.LocalAppStrings

@Composable
fun FilterCategoryChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) CyanAccent else SurfaceCardDark,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (isSelected) CyanAccent else SurfaceCardBorder,
                RoundedCornerShape(20.dp)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.Black else TextSecondaryDark,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = if (isSelected) Color.Black else TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarFeedScreen(
    uiState: UiState,
    deals: List<PropertyDeal>,
    sources: List<com.example.data.ScraperSource>,
    recentlyViewedDeals: List<PropertyDeal> = emptyList(),
    savedDeals: List<PropertyDeal> = emptyList(),
    onSearchQueryChange: (String) -> Unit,
    onFilterCategorySelect: (FilterCategory) -> Unit,
    onSourceFilterSelect: (String) -> Unit,
    onPropertyTypeSelect: (String) -> Unit,
    onDealClick: (PropertyDeal) -> Unit,
    onBookmarkToggle: (PropertyDeal) -> Unit,
    onCalculateClick: (PropertyDeal) -> Unit,
    onAddDealClick: () -> Unit,
    onHideExpiredAuctionsToggle: () -> Unit = {},
    onLanguageToggle: () -> Unit = {},
    onThemeToggle: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onOpenMarketTrendDrawer: (String) -> Unit = {},
    onStageChange: (dealId: Long, newStage: String) -> Unit = { _, _ -> },
    onCompareToggle: ((PropertyDeal) -> Unit)? = null,
    onOpenComparisonClick: (() -> Unit)? = null,
    onOpenCyberTerminal: (() -> Unit)? = null,
    onOpenGraveDancer: (() -> Unit)? = null,
    onOpenSupplyDemandMonitor: (() -> Unit)? = null,
    recentSearches: List<com.example.data.RecentSearchQuery> = emptyList(),
    onSubmitSearchQuery: (String) -> Unit = {},
    onRemoveRecentSearch: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    investorProfile: com.example.data.InvestorProfile? = null,
    onUnlockSingleDeal: ((Long) -> Unit)? = null,
    onActivateProMembership: (() -> Unit)? = null,
    onUseTokenToUnlock: ((Long) -> Unit)? = null,
    onToggleBlindMode: ((Boolean) -> Unit)? = null,
    onSubmitOffer: ((Long, Double, String, String) -> Unit)? = null,
    onOpenSubscriptionManagement: (() -> Unit)? = null,
    onDismissSearchCoachMark: (() -> Unit)? = null,
    onOpenOnboardingCarousel: (() -> Unit)? = null,
    onOpenFcmPushCenter: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    var isSourceMenuExpanded by remember { mutableStateOf(false) }
    var isKanbanView by remember { mutableStateOf(false) }
    var showImmobiliareDialog by remember { mutableStateOf(false) }

    // Coach Mark Overlay state for new / non-premium users
    var showSearchCoachMark by remember { mutableStateOf(false) }
    var hasCheckedInitialCoachMark by remember { mutableStateOf(false) }

    LaunchedEffect(investorProfile) {
        if (!hasCheckedInitialCoachMark && investorProfile != null) {
            hasCheckedInitialCoachMark = true
            val isPro = investorProfile.isProSubscriber
            val hasSeen = investorProfile.hasSeenSearchCoachMark
            if (!isPro && !hasSeen) {
                showSearchCoachMark = true
            }
        }
    }

    if (showSearchCoachMark) {
        PropertySearchCoachMarkOverlay(
            isVisible = true,
            onDismiss = { dontShowAgain ->
                showSearchCoachMark = false
                if (dontShowAgain) {
                    onDismissSearchCoachMark?.invoke()
                }
            },
            onNavigateToSubscription = {
                showSearchCoachMark = false
                onOpenSubscriptionManagement?.invoke() ?: onActivateProMembership?.invoke()
            }
        )
    }

    // Dialog state for Deal Unlock & Offer Submission
    var dealToUnlock by remember { mutableStateOf<PropertyDeal?>(null) }
    var dealForOffer by remember { mutableStateOf<PropertyDeal?>(null) }
    var showUpgradePremiumModal by remember { mutableStateOf(false) }

    if (showUpgradePremiumModal) {
        UpgradeToPremiumModal(
            deal = null,
            investorProfile = investorProfile,
            onDismiss = { showUpgradePremiumModal = false },
            onActivateProMembership = {
                onActivateProMembership?.invoke()
                showUpgradePremiumModal = false
            }
        )
    }

    if (dealToUnlock != null) {
        BlindDealUnlockDialog(
            deal = dealToUnlock!!,
            investorProfile = investorProfile,
            onDismiss = { dealToUnlock = null },
            onUnlockSingleDeal = { dealId ->
                onUnlockSingleDeal?.invoke(dealId)
                dealToUnlock = null
            },
            onActivateProMembership = {
                onActivateProMembership?.invoke()
                dealToUnlock = null
            },
            onUseTokenToUnlock = { dealId ->
                onUseTokenToUnlock?.invoke(dealId)
                dealToUnlock = null
            }
        )
    }

    if (dealForOffer != null) {
        PropertyOfferDialog(
            deal = dealForOffer!!,
            investorProfile = investorProfile,
            onDismiss = { dealForOffer = null },
            onSubmitOfferToPipeline = { amount, newStage, offerNotes ->
                onSubmitOffer?.invoke(dealForOffer!!.id, amount, newStage, offerNotes)
                dealForOffer = null
            }
        )
    }

    val totalDealsCount = deals.size
    val avgDiscount = if (deals.isNotEmpty()) deals.map { it.discountPercent }.average().toInt() else 0
    val avgCapRate = if (deals.isNotEmpty()) String.format("%.1f", deals.map { it.estimatedCapRate }.average()) else "0.0"

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddDealClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black) },
                text = { Text("Aggiungi Deal", color = Color.Black, fontWeight = FontWeight.Bold) },
                containerColor = EmeraldGreen,
                modifier = Modifier.testTag("add_deal_fab")
            )
        },
        containerColor = DarkSlateBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Permanent Unverified Data Warning Banner
            if (deals.any { !com.example.data.DataProvenance.fromString(it.provenance).isTrustworthy }) {
                com.example.ui.components.UnverifiedDataWarningBanner()
            }

            // Header Bar
            Surface(
                color = SurfaceCardDark,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // App Title Bar
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
                                    .size(36.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(CyanAccent, PurpleIndigo)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = strings.appTitle,
                                    color = TextPrimaryDark,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.appSubtitle,
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Proactive Supply-Demand Monitor Button
                            Surface(
                                color = Color(0xFF082F49),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .clickable { onOpenSupplyDemandMonitor?.invoke() }
                                    .testTag("open_supply_demand_monitor_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Sensors, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(15.dp))
                                    Text(text = "📡 D/O", color = Color(0xFF06B6D4), fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            // Sam Zell "The Grave Dancer" Contrarian Mode Button
                            Surface(
                                color = Color(0xFF2E0911),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .clickable { onOpenGraveDancer?.invoke() }
                                    .testTag("open_grave_dancer_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(15.dp))
                                    Text(text = "💀 Zell", color = Color(0xFFD4AF37), fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            // Cyber Terminal First-Principles Engine Button (Elon Musk Mode)
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .clickable { onOpenCyberTerminal?.invoke() }
                                    .testTag("open_cyber_terminal_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFF9100), modifier = Modifier.size(15.dp))
                                    Text(text = "⚡ Cyber", color = Color(0xFFFF9100), fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            // Immobiliare.it Market Observatory Button
                            Surface(
                                color = BentoBlueContainer,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BentoBlueOnContainer.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clickable { showImmobiliareDialog = true }
                                    .testTag("open_immobiliare_observatory_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Assessment, contentDescription = null, tint = BentoBlueOnContainer, modifier = Modifier.size(15.dp))
                                    Text(text = "Osservatorio", color = BentoBlueOnContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Gemini Market Trend AI Drawer Button
                            Surface(
                                color = CyanAccent,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .clickable { onOpenMarketTrendDrawer("Milano") }
                                    .testTag("open_market_trend_drawer_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                                    Text(text = "Trend AI", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Language Toggle Button
                            Surface(
                                color = BentoPurpleHeader,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                                modifier = Modifier
                                    .clickable { onLanguageToggle() }
                                    .testTag("language_toggle_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(text = strings.language.flag, fontSize = 14.sp)
                                    Text(text = strings.language.name, color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Global Theme Toggle Button (Light / Dark Mode)
                            val isDarkMode = uiState.themeMode == ThemeMode.DARK
                            Surface(
                                color = if (isDarkMode) Color(0xFF1E1B4B) else Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDarkMode) Color(0xFF818CF8).copy(alpha = 0.6f) else Color(0xFFF59E0B).copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .clickable { onThemeToggle() }
                                    .testTag("theme_toggle_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = if (isDarkMode) "Passa a Tema Chiaro" else "Passa a Tema Scuro",
                                        tint = if (isDarkMode) Color(0xFFD0BCFF) else Color(0xFFD97706),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isDarkMode) "🌙 Scuro" else "☀️ Chiaro",
                                        color = if (isDarkMode) Color(0xFFEADDFF) else Color(0xFF92400E),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Firebase Cloud Messaging Real-Time Push Center Button
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.8f)),
                                modifier = Modifier
                                    .clickable { onOpenFcmPushCenter?.invoke() }
                                    .testTag("trigger_fcm_push_center_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = "FCM Push Real-Time", tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                                    Text(text = "⚡ Push FCM", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Quantum Radar Value Onboarding Carousel Button
                            Surface(
                                color = Color(0xFF0F2E3A),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.8f)),
                                modifier = Modifier
                                    .clickable { onOpenOnboardingCarousel?.invoke() }
                                    .testTag("trigger_onboarding_carousel_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.RocketLaunch, contentDescription = "Guida Valore Onboarding", tint = CyanAccent, modifier = Modifier.size(15.dp))
                                    Text(text = "🚀 Tour", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Interactive Property Search Coach Mark Guide Button
                            Surface(
                                color = Color(0xFF1E1B4B),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .clickable { showSearchCoachMark = true }
                                    .testTag("trigger_search_coach_mark_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Guida Ricerca", tint = Color(0xFFC084FC), modifier = Modifier.size(15.dp))
                                    Text(text = "💡 Guida PRO", color = Color(0xFFE9D5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Source Provider Filter Dropdown Button
                            Box {
                                OutlinedButton(
                                    onClick = { isSourceMenuExpanded = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("source_filter_dropdown")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (uiState.selectedSourceFilter == "ALL") strings.sourceAll else uiState.selectedSourceFilter,
                                        fontSize = 11.sp
                                    )
                                }

                                DropdownMenu(
                                    expanded = isSourceMenuExpanded,
                                    onDismissRequest = { isSourceMenuExpanded = false },
                                    modifier = Modifier.background(SurfaceCardDark)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(strings.sourceAll, color = TextPrimaryDark) },
                                        onClick = {
                                            onSourceFilterSelect("ALL")
                                            isSourceMenuExpanded = false
                                        }
                                    )
                                HorizontalDivider(color = SurfaceCardBorder)
                                sources.forEach { source ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(source.name, color = TextPrimaryDark, fontSize = 13.sp)
                                                Text(
                                                    "[${source.robotsStatus}]",
                                                    color = if (source.robotsStatus == "CONSENTITO") EmeraldGreen else AmberGold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSourceFilterSelect(source.id)
                                            isSourceMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    }

                    // Search Field
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(strings.searchPlaceholder, color = TextMutedDark) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondaryDark)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { onSubmitSearchQuery(uiState.searchQuery) }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSlateBg,
                            unfocusedContainerColor = DarkSlateBg,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input")
                    )

                    // Recent Searches Bar from Room Storage
                    RecentSearchesBar(
                        recentSearches = recentSearches,
                        onSelectQuery = { query ->
                            onSubmitSearchQuery(query)
                        },
                        onRemoveQuery = { query ->
                            onRemoveRecentSearch(query)
                        },
                        onClearAll = {
                            onClearRecentSearches()
                        }
                    )

                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterCategoryChip(
                            label = strings.filterAll,
                            icon = Icons.Default.GridView,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.ALL,
                            onClick = { onFilterCategorySelect(FilterCategory.ALL) },
                            testTag = "filter_all"
                        )
                        FilterCategoryChip(
                            label = strings.filterTargetBrief,
                            icon = Icons.Default.Verified,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.IN_TARGET_BRIEF,
                            onClick = { onFilterCategorySelect(FilterCategory.IN_TARGET_BRIEF) },
                            testTag = "filter_target_brief"
                        )
                        FilterCategoryChip(
                            label = strings.filterHighDiscount,
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.HIGH_DISCOUNT,
                            onClick = { onFilterCategorySelect(FilterCategory.HIGH_DISCOUNT) },
                            testTag = "filter_high_discount"
                        )
                        FilterCategoryChip(
                            label = strings.filterAuctionNpl,
                            icon = Icons.Default.Gavel,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.AUCTION_NPL,
                            onClick = { onFilterCategorySelect(FilterCategory.AUCTION_NPL) },
                            testTag = "filter_auctions"
                        )
                        FilterCategoryChip(
                            label = strings.filterBookmarked,
                            icon = Icons.Default.Bookmark,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.BOOKMARKED,
                            onClick = { onFilterCategorySelect(FilterCategory.BOOKMARKED) },
                            testTag = "filter_bookmarked"
                        )
                        FilterCategoryChip(
                            label = "Visti di Recente (${recentlyViewedDeals.size})",
                            icon = Icons.Default.History,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.RECENTLY_VIEWED,
                            onClick = { onFilterCategorySelect(FilterCategory.RECENTLY_VIEWED) },
                            testTag = "filter_recently_viewed"
                        )
                        FilterCategoryChip(
                            label = if (uiState.hideExpiredAuctions) "✓ ${strings.hideExpiredAuctions}" else strings.hideExpiredAuctions,
                            icon = Icons.Default.Event,
                            isSelected = uiState.hideExpiredAuctions,
                            onClick = { onHideExpiredAuctionsToggle() },
                            testTag = "filter_hide_expired"
                        )
                    }

                    // Property Type Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to strings.typeAll,
                            "Residenziale" to (if (strings.isItalian) "Residenziale" else "Residential"),
                            "Commerciale" to (if (strings.isItalian) "Commerciale" else "Commercial"),
                            "Industriale" to (if (strings.isItalian) "Industriale" else "Industrial"),
                            "Asta" to (if (strings.isItalian) "Aste Giudiziarie" else "Court Auctions")
                        ).forEach { (key, label) ->
                            val isSel = uiState.selectedPropertyTypeFilter == key
                            SuggestionChip(
                                onClick = { onPropertyTypeSelect(key) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSel) Color(0xFF0284C7).copy(alpha = 0.3f) else DarkSlateBg,
                                    labelColor = if (isSel) CyanAccent else TextSecondaryDark
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = if (isSel) CyanAccent else SurfaceCardBorder
                                )
                            )
                        }
                    }
                }
            }

            // Summary Stats & View Mode Switcher Bar
            Surface(
                color = Color(0xFF1E1B4B).copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$totalDealsCount ${strings.dealsFound}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${strings.avgDiscount}: -$avgDiscount%",
                                color = EmeraldGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${strings.avgCapRate}: $avgCapRate%",
                                color = AmberGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Feed vs Kanban View Mode Switcher Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSlateBg)
                            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
                            .padding(2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isKanbanView) CyanAccent else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isKanbanView = false }
                                .testTag("view_mode_list_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                    tint = if (!isKanbanView) Color.Black else TextSecondaryDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Elenco",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isKanbanView) Color.Black else TextSecondaryDark
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isKanbanView) CyanAccent else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isKanbanView = true }
                                .testTag("view_mode_kanban_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.ViewColumn,
                                    contentDescription = null,
                                    tint = if (isKanbanView) Color.Black else TextSecondaryDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Kanban",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isKanbanView) Color.Black else TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }

            if (isKanbanView) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    DealKanbanBoard(
                        deals = deals,
                        onStageChange = onStageChange,
                        onDealClick = onDealClick,
                        onBookmarkToggle = onBookmarkToggle,
                        onCalculateClick = onCalculateClick
                    )
                }
            } else if (deals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IllustrativeEmptySearchState(
                        searchQuery = uiState.searchQuery,
                        selectedSource = uiState.selectedSourceFilter,
                        onResetFilters = {
                            onSearchQueryChange("")
                            onFilterCategorySelect(FilterCategory.ALL)
                            onSourceFilterSelect("ALL")
                            onPropertyTypeSelect("ALL")
                        },
                        onAddDealClick = onAddDealClick,
                        onSuggestionClick = { suggestion ->
                            onSearchQueryChange(suggestion)
                        }
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("deals_list")
                ) {
                    item {
                        BlindModeBanner(
                            investorProfile = investorProfile,
                            onToggleBlindMode = { onToggleBlindMode?.invoke(it) },
                            onOpenUpgradeDialog = { showUpgradePremiumModal = true }
                        )
                    }

                    item {
                        QuickAccessDealsBar(
                            recentlyViewedDeals = recentlyViewedDeals,
                            savedDeals = savedDeals,
                            onDealClick = onDealClick,
                            onBookmarkToggle = onBookmarkToggle,
                            onClearHistory = onClearHistory
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_radar_hero_1786384712474),
                                    contentDescription = "Quantum Deal Radar Hero",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    DarkSlateBg.copy(alpha = 0.95f),
                                                    DarkSlateBg.copy(alpha = 0.5f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = EmeraldGreen.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = "⚡ LIVE RADAR ATTIVO",
                                            color = EmeraldGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Scansione Aste & Immobili Distressed",
                                        color = TextPrimaryDark,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Algoritmo AI di stima sconto di perizia e calcolo yield",
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    items(deals, key = { it.id }) { deal ->
                        DealCard(
                            deal = deal,
                            onCardClick = { onDealClick(deal) },
                            onBookmarkToggle = { onBookmarkToggle(deal) },
                            onCalculateClick = { onCalculateClick(deal) },
                            isSelectedForCompare = uiState.selectedComparisonDealIds.contains(deal.id),
                            onCompareToggle = if (onCompareToggle != null) { { onCompareToggle(deal) } } else null,
                            investorProfile = investorProfile,
                            onUnlockClick = { dealToUnlock = deal },
                            onOfferClick = { dealForOffer = deal }
                        )
                    }
                }
            }
        }

        // Floating Compare Bar
        if (uiState.selectedComparisonDealIds.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanAccent),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .testTag("floating_compare_bar")
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(CyanAccent.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "${uiState.selectedComparisonDealIds.size} immobili selezionati",
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tocca per confrontare la tabella side-by-side",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { onOpenComparisonClick?.invoke() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("open_comparison_button")
                    ) {
                        Text("Confronta Ora", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        if (showImmobiliareDialog) {
            ImmobiliareObservatoryDialog(
                initialMunicipality = "Paderno Dugnano",
                onDismissRequest = { showImmobiliareDialog = false }
            )
        }
    }
}
}

