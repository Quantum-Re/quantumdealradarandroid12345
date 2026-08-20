package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentSearchQuery
import com.example.ui.theme.*

@Composable
fun RecentSearchesBar(
    recentSearches: List<RecentSearchQuery>,
    onSelectQuery: (String) -> Unit,
    onRemoveQuery: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (recentSearches.isEmpty()) return

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("recent_searches_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Storico Ricerche",
                    tint = CyanAccent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Ricerche Recenti (Room DB)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryDark
                )
            }

            Text(
                text = "Cancella",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMutedDark,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onClearAll() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("clear_recent_searches_button")
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            recentSearches.take(5).forEach { item ->
                RecentSearchChip(
                    queryText = item.query,
                    onClick = { onSelectQuery(item.query) },
                    onRemove = { onRemoveQuery(item.query) }
                )
            }
        }
    }
}

@Composable
private fun RecentSearchChip(
    queryText: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = BentoPurpleHeader.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier
            .testTag("recent_search_chip_${queryText.lowercase().replace(" ", "_")}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = BentoPurpleOnContainer,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = queryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimaryDark
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Rimuovi ricerca",
                    tint = TextMutedDark,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
