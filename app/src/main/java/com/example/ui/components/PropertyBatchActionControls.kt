package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PipelineStatus
import com.example.ui.theme.*

/**
 * Floating bottom action bar displayed when multi-selection mode is active.
 * Allows users to bulk change pipeline status, bulk archive, or bulk delete selected properties.
 */
@Composable
fun PropertyBatchFloatingActionBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAllToggle: () -> Unit,
    onOpenChangeStatus: () -> Unit,
    onOpenArchiveConfirm: () -> Unit,
    onOpenDeleteConfirm: () -> Unit,
    onCancelSelection: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenComparison: (() -> Unit)? = null
) {
    val allSelected = selectedCount > 0 && selectedCount == totalCount

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E1B2E), // Rich dark bento surface
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("batch_floating_action_bar")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top row: Counter, select-all shortcut, and dismiss close button
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(BentoPurpleHeader)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$selectedCount / $totalCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoPurpleOnContainer
                        )
                    }

                    Text(
                        text = if (selectedCount == 1) "1 immobile selezionato" else "$selectedCount immobili selezionati",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Select All / Deselect button
                    TextButton(
                        onClick = onSelectAllToggle,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("batch_select_all_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                            contentDescription = if (allSelected) "Deseleziona tutti gli immobili" else "Seleziona tutti gli immobili",
                            tint = BentoPurpleContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (allSelected) "Deseleziona" else "Tutti ($totalCount)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPurpleContainer
                        )
                    }

                    IconButton(
                        onClick = onCancelSelection,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("batch_cancel_selection_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi selezione multipla",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Bottom Action buttons row: Change Status, Compare, Archive, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Cambia Stato
                Button(
                    onClick = onOpenChangeStatus,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BentoPurpleOnContainer,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .heightIn(min = 48.dp)
                        .testTag("batch_action_change_status_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Cambia Stato",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Stato",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 2. Confronta (Se abilitato / 2+ selezionati)
                if (onOpenComparison != null) {
                    FilledTonalButton(
                        onClick = onOpenComparison,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CyanAccent.copy(alpha = 0.2f),
                            contentColor = CyanAccent
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp)
                            .testTag("batch_action_compare_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Confronta",
                            tint = CyanAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Confronta",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }
                }

                // 3. Archivia
                FilledTonalButton(
                    onClick = onOpenArchiveConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF322E47),
                        contentColor = Color(0xFFD0BCFF)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .heightIn(min = 48.dp)
                        .testTag("batch_action_archive_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Archivia",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Archivia",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 4. Elimina
                OutlinedButton(
                    onClick = onOpenDeleteConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RoseRed
                    ),
                    border = BorderStroke(1.dp, RoseRed.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1.0f)
                        .heightIn(min = 48.dp)
                        .testTag("batch_action_delete_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Elimina",
                        tint = RoseRed,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Elimina",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseRed
                    )
                }
            }
        }
    }
}

/**
 * Bottom Sheet for changing the pipeline status of multiple selected properties.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchStatusBottomSheet(
    selectedCount: Int,
    onStatusSelected: (PipelineStatus) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceCardDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("batch_status_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
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
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = BentoPurpleOnContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Cambia Stato di Gruppo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Applica un nuovo stato a $selectedCount immobili selezionati",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextSecondaryDark)
                }
            }

            HorizontalDivider(color = SurfaceCardBorder.copy(alpha = 0.5f))

            // List of all statuses
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PipelineStatus.values().forEach { status ->
                    val (icon, color) = getStatusPresentation(status)
                    Surface(
                        onClick = {
                            onStatusSelected(status)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = BentoCardBgLight,
                        border = BorderStroke(1.dp, SurfaceCardBorder.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("batch_select_status_${status.key.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = status.labelIt,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = status.description,
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark,
                                    maxLines = 1
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Confirmation dialog for bulk archiving properties.
 */
@Composable
fun BatchArchiveConfirmDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("batch_archive_confirm_dialog"),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BentoPurpleHeader),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    tint = BentoPurpleOnContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Archivia $selectedCount Immobili",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Gli immobili selezionati verranno contrassegnati come 'Archiviati'. Potrai sempre consultarli filtrando per stato Archiviato o ripristinarli in qualsiasi momento.",
                fontSize = 13.sp,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOnContainer),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_batch_archive_btn")
            ) {
                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Archivia Tutti", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Annulla")
            }
        },
        containerColor = SurfaceCardDark,
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Confirmation dialog for bulk deleting properties permanently.
 */
@Composable
fun BatchDeleteConfirmDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("batch_delete_confirm_dialog"),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(RoseRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RoseRed,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "Elimina $selectedCount Immobili",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = RoseRed,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Sei sicuro di voler eliminare definitivamente gli immobili selezionati e tutti i dati finanziari associati? Questa azione non può essere annullata.",
                fontSize = 13.sp,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_batch_delete_btn")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Elimina Definitivamente", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Annulla")
            }
        },
        containerColor = SurfaceCardDark,
        shape = RoundedCornerShape(20.dp)
    )
}

private fun getStatusPresentation(status: PipelineStatus): Pair<ImageVector, Color> {
    return when (status) {
        PipelineStatus.ANALYZED -> Icons.Default.Search to CyanAccent
        PipelineStatus.IN_ESCROW -> Icons.Default.Handshake to AmberGold
        PipelineStatus.RENOVATING -> Icons.Default.Build to BentoPurpleOnContainer
        PipelineStatus.LISTED -> Icons.Default.Storefront to PurpleIndigo
        PipelineStatus.RENTED -> Icons.Default.Key to Color(0xFF00796B)
        PipelineStatus.SOLD -> Icons.Default.CheckCircle to EmeraldGreen
        PipelineStatus.ARCHIVED -> Icons.Default.Archive to Color(0xFF757575)
    }
}
