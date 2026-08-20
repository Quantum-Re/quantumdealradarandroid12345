package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertySortCategory
import com.example.data.PropertySortOption
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern Bento-styled sorting toolbar for My Properties dashboard.
 * Provides quick-action sorting chips (Date, Status, ROI, Price) with active indicators,
 * direct order toggling, and full modal sheet picker.
 */
@Composable
fun PropertySortingBar(
    currentSort: PropertySortOption,
    onSortSelected: (PropertySortOption) -> Unit,
    onOpenFullSortSheet: () -> Unit,
    totalCount: Int,
    isSelectionModeActive: Boolean = false,
    onToggleSelectionMode: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Active Sort Pill Button + Direction Flip + Selection Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive active sort button
                Surface(
                    onClick = onOpenFullSortSheet,
                    shape = RoundedCornerShape(10.dp),
                    color = BentoPurpleContainer.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.3f)),
                    modifier = Modifier.testTag("open_sort_sheet_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = getSortIcon(currentSort),
                            contentDescription = null,
                            tint = BentoPurpleOnContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "ORDINA PER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoPurpleOnContainer.copy(alpha = 0.7f),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = currentSort.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BentoPurpleOnContainer
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Apri opzioni ordinamento",
                            tint = BentoPurpleOnContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Quick toggle direction or open sheet action & Selection mode toggle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onToggleSelectionMode != null) {
                        Surface(
                            onClick = onToggleSelectionMode,
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelectionModeActive) BentoPurpleOnContainer else BentoPurpleHeader.copy(alpha = 0.5f),
                            modifier = Modifier.testTag("toggle_selection_mode_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelectionModeActive) Icons.Default.ChecklistRtl else Icons.Default.Checklist,
                                    contentDescription = "Attiva selezione multipla",
                                    tint = if (isSelectionModeActive) Color.White else BentoPurpleOnContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isSelectionModeActive) "Chiudi" else "Seleziona",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelectionModeActive) Color.White else BentoPurpleOnContainer
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            val flipped = when (currentSort) {
                                PropertySortOption.DATE_ADDED_DESC -> PropertySortOption.DATE_ADDED_ASC
                                PropertySortOption.DATE_ADDED_ASC -> PropertySortOption.DATE_ADDED_DESC
                                PropertySortOption.ROI_DESC -> PropertySortOption.ROI_ASC
                                PropertySortOption.ROI_ASC -> PropertySortOption.ROI_DESC
                                PropertySortOption.PRICE_DESC -> PropertySortOption.PRICE_ASC
                                PropertySortOption.PRICE_ASC -> PropertySortOption.PRICE_DESC
                                PropertySortOption.STATUS_WORKFLOW -> PropertySortOption.STATUS_NAME
                                PropertySortOption.STATUS_NAME -> PropertySortOption.STATUS_WORKFLOW
                                PropertySortOption.PROFIT_DESC -> PropertySortOption.ROI_DESC
                                PropertySortOption.OPPORTUNITY_SCORE_DESC -> PropertySortOption.OPPORTUNITY_SCORE_DESC
                            }
                            onSortSelected(flipped)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BentoPurpleHeader.copy(alpha = 0.5f))
                            .testTag("toggle_sort_direction_button")
                    ) {
                        val isAscending = currentSort in listOf(
                            PropertySortOption.DATE_ADDED_ASC,
                            PropertySortOption.ROI_ASC,
                            PropertySortOption.PRICE_ASC,
                            PropertySortOption.STATUS_NAME
                        )
                        Icon(
                            imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = if (isAscending) "Ordinamento crescente attivo. Tocca per invertire" else "Ordinamento decrescente attivo. Tocca per invertire",
                            tint = BentoPurpleOnContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenFullSortSheet,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Opzioni ordinamento",
                            modifier = Modifier.size(16.dp),
                            tint = BentoPurpleOnContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Opzioni",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPurpleOnContainer
                        )
                    }
                }
            }

            // Quick Category Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Date Sort Chip
                QuickSortFilterChip(
                    label = "Data Aggiunta",
                    icon = Icons.Default.CalendarToday,
                    isSelected = currentSort.category == PropertySortCategory.DATE,
                    sortOption = if (currentSort == PropertySortOption.DATE_ADDED_DESC) {
                        PropertySortOption.DATE_ADDED_ASC
                    } else {
                        PropertySortOption.DATE_ADDED_DESC
                    },
                    currentSort = currentSort,
                    onSelect = onSortSelected,
                    tag = "sort_chip_date"
                )

                // 2. Status Sort Chip
                QuickSortFilterChip(
                    label = "Stato Pipeline",
                    icon = Icons.Default.ViewKanban,
                    isSelected = currentSort.category == PropertySortCategory.STATUS,
                    sortOption = if (currentSort == PropertySortOption.STATUS_WORKFLOW) {
                        PropertySortOption.STATUS_NAME
                    } else {
                        PropertySortOption.STATUS_WORKFLOW
                    },
                    currentSort = currentSort,
                    onSelect = onSortSelected,
                    tag = "sort_chip_status"
                )

                // 3. ROI Sort Chip
                QuickSortFilterChip(
                    label = "ROI Stimato",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    isSelected = currentSort.category == PropertySortCategory.ROI,
                    sortOption = if (currentSort == PropertySortOption.ROI_DESC) {
                        PropertySortOption.ROI_ASC
                    } else {
                        PropertySortOption.ROI_DESC
                    },
                    currentSort = currentSort,
                    onSelect = onSortSelected,
                    tag = "sort_chip_roi"
                )

                // 4. Price Sort Chip
                QuickSortFilterChip(
                    label = "Prezzo Acquisto",
                    icon = Icons.Default.Payments,
                    isSelected = currentSort.category == PropertySortCategory.PRICE,
                    sortOption = if (currentSort == PropertySortOption.PRICE_ASC) {
                        PropertySortOption.PRICE_DESC
                    } else {
                        PropertySortOption.PRICE_ASC
                    },
                    currentSort = currentSort,
                    onSelect = onSortSelected,
                    tag = "sort_chip_price"
                )

                // 5. Opportunity Score Sort Chip
                QuickSortFilterChip(
                    label = "Opportunity Score",
                    icon = Icons.Default.AutoAwesome,
                    isSelected = currentSort.category == PropertySortCategory.OPPORTUNITY,
                    sortOption = PropertySortOption.OPPORTUNITY_SCORE_DESC,
                    currentSort = currentSort,
                    onSelect = onSortSelected,
                    tag = "sort_chip_opportunity"
                )
            }
        }
    }
}

@Composable
private fun QuickSortFilterChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    sortOption: PropertySortOption,
    currentSort: PropertySortOption,
    onSelect: (PropertySortOption) -> Unit,
    tag: String
) {
    Surface(
        onClick = { onSelect(sortOption) },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) BentoPurpleContainer else Color.White,
        border = BorderStroke(
            1.dp,
            if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder
        ),
        modifier = Modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) BentoPurpleOnContainer else TextSecondaryDark,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) BentoPurpleOnContainer else TextPrimaryDark
            )
            if (isSelected) {
                val isDesc = currentSort in listOf(
                    PropertySortOption.DATE_ADDED_DESC,
                    PropertySortOption.ROI_DESC,
                    PropertySortOption.PRICE_DESC,
                    PropertySortOption.STATUS_WORKFLOW,
                    PropertySortOption.PROFIT_DESC
                )
                Icon(
                    imageVector = if (isDesc) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = BentoPurpleOnContainer,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Modal Bottom Sheet for complete property sorting configuration.
 * Categorized by Date Added, Pipeline Status, ROI & Profitability, and Capital/Price.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertySortBottomSheet(
    currentSort: PropertySortOption,
    onSortSelected: (PropertySortOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceCardDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BentoPurpleHeader),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = null,
                            tint = BentoPurpleOnContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Ordinamento Immobili",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Seleziona il criterio di organizzazione del portafoglio",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextSecondaryDark)
                }
            }

            HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.5f))

            // Sort Option Sections
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. DATA DI INSERIMENTO (Date Added)
                SortCategorySection(
                    categoryTitle = "DATA DI INSERIMENTO",
                    categoryIcon = Icons.Default.CalendarToday
                ) {
                    SortOptionRowItem(
                        option = PropertySortOption.DATE_ADDED_DESC,
                        isSelected = currentSort == PropertySortOption.DATE_ADDED_DESC,
                        icon = Icons.Default.History,
                        onSelect = {
                            onSortSelected(it)
                            onDismiss()
                        }
                    )
                    SortOptionRowItem(
                        option = PropertySortOption.DATE_ADDED_ASC,
                        isSelected = currentSort == PropertySortOption.DATE_ADDED_ASC,
                        icon = Icons.Default.CalendarMonth,
                        onSelect = {
                            onSortSelected(it)
                            onDismiss()
                        }
                    )
                }

                // 2. STATO PIPELINE (Status)
                SortCategorySection(
                    categoryTitle = "STATO PIPELINE & AVANZAMENTO",
                    categoryIcon = Icons.Default.ViewKanban
                ) {
                    SortOptionRowItem(
                        option = PropertySortOption.STATUS_WORKFLOW,
                        isSelected = currentSort == PropertySortOption.STATUS_WORKFLOW,
                        icon = Icons.Default.AccountTree,
                        onSelect = {
                            onSortSelected(it)
                            onDismiss()
                        }
                    )
                    SortOptionRowItem(
                        option = PropertySortOption.STATUS_NAME,
                        isSelected = currentSort == PropertySortOption.STATUS_NAME,
                        icon = Icons.Default.SortByAlpha,
                        onSelect = {
                            onSortSelected(it)
                            onDismiss()
                        }
                    )
                }

                // 3. ROI & REDDITIVITÀ (Estimated ROI)
                SortCategorySection(
                    categoryTitle = "ROI & REDDITIVITÀ OPERAZIONE",
                    categoryIcon = Icons.AutoMirrored.Filled.TrendingUp
                ) {
                    SortOptionRowItem(
                        option = PropertySortOption.ROI_DESC,
                        isSelected = currentSort == PropertySortOption.ROI_DESC,
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        onSelect = {
                            onSortSelected(it)
                            onDismiss()
                        }
                    )
                    SortOptionRowItem(
                        option = PropertySortOption.ROI_ASC,
                        isSelected = currentSort == PropertySortOption.ROI_ASC,
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        onSelect = {
                            onSortSelected(it)
                            onDismiss()
                        }
                    )
                    SortOptionRowItem(
                        option = PropertySortOption.PROFIT_DESC,
                        isSelected = currentSort == PropertySortOption.PROFIT_DESC,
                        icon = Icons.Default.Savings,
                        onSelect = {
                            onSortSelected(it)
                            onDismiss()
                        }
                    )
                }

                // 4. PREZZO & CAPITALE (Price & Capital)
                SortCategorySection(
                    categoryTitle = "PREZZO & CAPITALE IMPIEGATO",
                    categoryIcon = Icons.Default.Payments
                ) {
                    SortOptionRowItem(
                        option = PropertySortOption.PRICE_ASC,
                        isSelected = currentSort == PropertySortOption.PRICE_ASC,
                        icon = Icons.Default.ArrowUpward,
                        onSelect = {
                            onSortSelected(it)
                            onDismiss()
                        }
                    )
                    SortOptionRowItem(
                        option = PropertySortOption.PRICE_DESC,
                        isSelected = currentSort == PropertySortOption.PRICE_DESC,
                        icon = Icons.Default.ArrowDownward,
                        onSelect = {
                            onSortSelected(it)
                            onDismiss()
                        }
                    )
                }

                // 5. OPPORTUNITY SCORE & SOTTOVALUTAZIONE
                SortCategorySection(
                    categoryTitle = "OPPORTUNITY SCORE & SOTTOVALUTAZIONE",
                    categoryIcon = Icons.Default.AutoAwesome
                ) {
                    SortOptionRowItem(
                        option = PropertySortOption.OPPORTUNITY_SCORE_DESC,
                        isSelected = currentSort == PropertySortOption.OPPORTUNITY_SCORE_DESC,
                        icon = Icons.Default.AutoAwesome,
                        onSelect = {
                            onSortSelected(it)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortCategorySection(
    categoryTitle: String,
    categoryIcon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = null,
                tint = BentoPurpleOnContainer,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = categoryTitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = BentoPurpleOnContainer,
                letterSpacing = 0.5.sp
            )
        }
        content()
    }
}

@Composable
private fun SortOptionRowItem(
    option: PropertySortOption,
    isSelected: Boolean,
    icon: ImageVector,
    onSelect: (PropertySortOption) -> Unit
) {
    Surface(
        onClick = { onSelect(option) },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) BentoPurpleContainer.copy(alpha = 0.9f) else BentoCardBgLight,
        border = BorderStroke(
            1.dp,
            if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sort_option_${option.key.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) BentoPurpleHeader else Color.White
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) BentoPurpleOnContainer else TextSecondaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = option.title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) BentoPurpleOnContainer else TextPrimaryDark
                    )
                    Text(
                        text = option.subtitle,
                        fontSize = 11.sp,
                        color = if (isSelected) BentoPurpleOnContainer.copy(alpha = 0.8f) else TextSecondaryDark
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = { onSelect(option) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = BentoPurpleOnContainer,
                    unselectedColor = SurfaceCardBorder
                )
            )
        }
    }
}

private fun getSortIcon(option: PropertySortOption): ImageVector {
    return when (option) {
        PropertySortOption.DATE_ADDED_DESC, PropertySortOption.DATE_ADDED_ASC -> Icons.Default.CalendarToday
        PropertySortOption.STATUS_WORKFLOW, PropertySortOption.STATUS_NAME -> Icons.Default.ViewKanban
        PropertySortOption.ROI_DESC, PropertySortOption.ROI_ASC, PropertySortOption.PROFIT_DESC -> Icons.AutoMirrored.Filled.TrendingUp
        PropertySortOption.PRICE_ASC, PropertySortOption.PRICE_DESC -> Icons.Default.Payments
        PropertySortOption.OPPORTUNITY_SCORE_DESC -> Icons.Default.AutoAwesome
    }
}

/**
 * Formats a unix timestamp into an Italian human-readable date.
 */
fun formatPropertyDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN)
    return sdf.format(Date(timestamp))
}

/**
 * Formats a timestamp into a relative duration string (e.g., "5 gg fa", "Oggi").
 */
fun formatPropertyRelativeDate(timestamp: Long): String {
    val diffMillis = System.currentTimeMillis() - timestamp
    val diffDays = (diffMillis / (1000L * 60 * 60 * 24)).toInt()
    return when {
        diffDays <= 0 -> "Oggi"
        diffDays == 1 -> "Ieri"
        diffDays < 30 -> "$diffDays gg fa"
        diffDays < 365 -> "${diffDays / 30} mesi fa"
        else -> "${diffDays / 365} anni fa"
    }
}
