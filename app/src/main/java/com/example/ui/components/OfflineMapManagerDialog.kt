package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MapOfflineRegion
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OfflineMapManagerDialog(
    cachedTileCount: Int,
    totalCacheSizeBytes: Long,
    offlineRegions: List<MapOfflineRegion>,
    isDownloading: Boolean,
    downloadProgress: Pair<Int, Int>, // (downloaded, total)
    availableDeals: List<PropertyDeal>,
    onDownloadDealMap: (PropertyDeal) -> Unit,
    onDownloadCustomRegion: (String, Double, Double) -> Unit,
    onClearCache: () -> Unit,
    onDeleteRegion: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Statistiche & Region, 1: Scarica Mappa Sopralluogo

    val mbFormatter = remember { DecimalFormat("#,##0.0") }
    val cacheMb = totalCacheSizeBytes / (1024f * 1024f)

    var customCityName by remember { mutableStateOf("Milano Centro") }
    var customLatStr by remember { mutableStateOf("45.4642") }
    var customLngStr by remember { mutableStateOf("9.1900") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("offline_map_manager_dialog"),
        containerColor = DarkSlateBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Cache Mappe Offline Room",
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sopralluoghi & Visite senza internet",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextMutedDark)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Room Cache Overview Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCardDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Spazio Utilizzato in Room DB",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${mbFormatter.format(cacheMb)} MB",
                                color = CyanAccent,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "$cachedTileCount tessere salvate",
                                color = TextMutedDark,
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldGreen.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(12.dp))
                                Text(
                                    text = "Ready Offline",
                                    color = EmeraldGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Download Progress Indicator
                if (isDownloading) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BentoPurpleHeader,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Scaricamento tessere in Room DB...",
                                    color = TextPrimaryDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${downloadProgress.first} / ${downloadProgress.second}",
                                    color = CyanAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val progressFraction = if (downloadProgress.second > 0) {
                                downloadProgress.first.toFloat() / downloadProgress.second.toFloat()
                            } else 0f

                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CyanAccent,
                                trackColor = DarkSlateBg
                            )
                        }
                    }
                }

                // Tab Row (0: Regioni Cachable, 1: Pre-Scarica per Immobili)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCardDark)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) CyanAccent.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Mappe Salvate (${offlineRegions.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) CyanAccent else TextMutedDark
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) CyanAccent.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Scarica Sopralluogo",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) CyanAccent else TextMutedDark
                        )
                    }
                }

                if (selectedTab == 0) {
                    // List of Cached Regions
                    if (offlineRegions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nessuna regione mappa pre-scaricata.\nVai alla scheda '+ Scarica Sopralluogo' per memorizzare le mappe in Room DB.",
                                color = TextMutedDark,
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(offlineRegions, key = { it.id }) { region ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkSlateBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = region.regionName,
                                                color = TextPrimaryDark,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(Date(region.downloadedAt))
                                            Text(
                                                text = "$dateStr • ${region.tileCount} tessere • ${mbFormatter.format(region.totalSizeBytes / (1024f * 1024f))} MB",
                                                color = TextSecondaryDark,
                                                fontSize = 10.sp
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteRegion(region.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = RoseRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Tab 1: Select Property or City to Pre-download
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Seleziona un'opportunità da scaricare per la visita sul posto:",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }

                        items(availableDeals, key = { it.id }) { deal ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceCardDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = deal.title,
                                            color = TextPrimaryDark,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "📍 ${deal.location} • ${deal.propertyType}",
                                            color = TextSecondaryDark,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Button(
                                        onClick = { onDownloadDealMap(deal) },
                                        enabled = !isDownloading,
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Scarica", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (offlineRegions.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        onClearCache()
                        Toast.makeText(context, "Cache tessere mappa azzerata", Toast.LENGTH_SHORT).show()
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoseRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseRed)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pulisci Tutta la Cache", fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi", color = TextSecondaryDark)
            }
        }
    )
}
