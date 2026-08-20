package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PipelineStatus
import com.example.data.Property
import com.example.ui.theme.*
import com.example.util.CsvPortfolioSyncService
import com.example.util.CsvSyncMode
import com.example.util.CsvSyncSummary
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CsvPortfolioSyncDialog(
    onDismiss: () -> Unit,
    onConfirmSync: (List<Property>, CsvSyncMode, (CsvSyncSummary) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val euroFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
            maximumFractionDigits = 0
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var rawCsvText by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var syncMode by remember { mutableStateOf(CsvSyncMode.MERGE_UPDATE) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncResultSummary by remember { mutableStateOf<CsvSyncSummary?>(null) }

    // Parse summary dynamically when raw CSV text changes
    val parsedSummary by remember(rawCsvText) {
        derivedStateOf {
            if (rawCsvText.isNotBlank()) {
                CsvPortfolioSyncService.parseCsvToProperties(rawCsvText)
            } else {
                null
            }
        }
    }

    // Android System File Picker for CSV / TXT files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                    rawCsvText = content
                    selectedFileName = uri.lastPathSegment ?: "file_portafoglio.csv"
                    selectedTabIndex = 1 // Switch to preview tab
                    Toast.makeText(context, "File CSV caricato correttamente!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Errore lettura file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isSyncing) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = !isSyncing
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(24.dp))
                .testTag("csv_portfolio_sync_dialog"),
            colors = CardDefaults.cardColors(containerColor = DarkSlateBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BentoPurpleHeader,
                            border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SyncAlt,
                                    contentDescription = "Sincronizza CSV",
                                    tint = BentoPurpleOnContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Sincronizzazione Portafoglio CSV",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Importa da Excel, Stessa, DealCheck o software gestionale",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSyncing,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_csv_sync_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi finestra sincronizzazione",
                            tint = TextMutedDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = BentoPurpleOnContainer,
                    divider = { HorizontalDivider(color = SurfaceCardBorder) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        modifier = Modifier.heightIn(min = 48.dp),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Origine Dati", modifier = Modifier.size(16.dp))
                                Text("1. Origine Dati", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        modifier = Modifier.heightIn(min = 48.dp),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Modalità", modifier = Modifier.size(16.dp))
                                Text("2. Modalità & Opzioni", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        modifier = Modifier.heightIn(min = 48.dp),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Preview, contentDescription = "Anteprima", modifier = Modifier.size(16.dp))
                                Text(
                                    text = "3. Anteprima (${parsedSummary?.validProperties?.size ?: 0})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTabIndex) {
                        0 -> DataOriginTabContent(
                            rawCsvText = rawCsvText,
                            selectedFileName = selectedFileName,
                            onPickFileClick = { filePickerLauncher.launch("*/*") },
                            onCsvTextChanged = { rawCsvText = it },
                            onLoadSampleData = {
                                rawCsvText = CsvPortfolioSyncService.generateSampleCsvTemplate()
                                selectedFileName = "modello_portfolio_esempio.csv"
                                Toast.makeText(context, "Dati di esempio caricati!", Toast.LENGTH_SHORT).show()
                            },
                            onDownloadTemplate = {
                                CsvPortfolioSyncService.exportSampleTemplate(context)
                            },
                            onClearData = {
                                rawCsvText = ""
                                selectedFileName = null
                            }
                        )

                        1 -> SyncModeTabContent(
                            currentMode = syncMode,
                            onModeSelected = { syncMode = it },
                            parsedSummary = parsedSummary
                        )

                        2 -> PreviewTabContent(
                            parsedSummary = parsedSummary,
                            euroFormat = euroFormat
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = SurfaceCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Status / Count indicator
                    if (parsedSummary != null && parsedSummary!!.validProperties.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldGainBg,
                            border = BorderStroke(1.dp, EmeraldGainBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Pronto",
                                    tint = EmeraldGainText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${parsedSummary!!.validProperties.size} immobili pronti da sincronizzare",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGainText
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (rawCsvText.isBlank()) "Carica un file o incolla testo CSV per procedere" else "Nessuna riga valida riconosciuta",
                            fontSize = 12.sp,
                            color = TextMutedDark
                        )
                    }

                    // Right Side Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isSyncing,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("cancel_csv_sync_button")
                        ) {
                            Text("Annulla", color = TextSecondaryDark)
                        }

                        Button(
                            onClick = {
                                val propertiesToSync = parsedSummary?.validProperties.orEmpty()
                                if (propertiesToSync.isEmpty()) {
                                    Toast.makeText(context, "Nessun immobile valido da sincronizzare.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isSyncing = true
                                onConfirmSync(propertiesToSync, syncMode) { summary ->
                                    isSyncing = false
                                    syncResultSummary = summary
                                    Toast.makeText(
                                        context,
                                        "Sincronizzazione completata: ${summary.insertedCount} aggiunti, ${summary.updatedCount} aggiornati.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onDismiss()
                                }
                            },
                            enabled = !isSyncing && parsedSummary != null && parsedSummary!!.validProperties.isNotEmpty(),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("confirm_csv_sync_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOnContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sincronizzazione in corso...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = "Conferma Sincronizzazione", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sincronizza Portafoglio", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: Data Origin (File Picker, Paste Text & Sample Data)
// -------------------------------------------------------------
@Composable
private fun DataOriginTabContent(
    rawCsvText: String,
    selectedFileName: String?,
    onPickFileClick: () -> Unit,
    onCsvTextChanged: (String) -> Unit,
    onLoadSampleData: () -> Unit,
    onDownloadTemplate: () -> Unit,
    onClearData: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section A: File Picker Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                            Text("Carica File CSV / TXT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        if (selectedFileName != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BentoPurpleHeader,
                                border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = selectedFileName,
                                    fontSize = 11.sp,
                                    color = BentoPurpleOnContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Seleziona un file esportato da Excel, Google Fogli, Stessa o dal tuo CRM immobiliare. Supporta formati con separatore virgola (,), punto e virgola (;) e tabulazione.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPickFileClick,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("pick_csv_file_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Sfoglia file", tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sfoglia File sul Dispositivo", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onDownloadTemplate,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("download_csv_template_btn"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BentoPurpleOnContainer)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Scarica Modello", tint = BentoPurpleOnContainer, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scarica Modello CSV", color = BentoPurpleOnContainer, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section B: Paste / Direct Text Editor
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                            Text("Oppure Incolla Dati CSV Direttamente", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = onLoadSampleData,
                                modifier = Modifier.heightIn(min = 44.dp).testTag("load_sample_csv_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Esempio", tint = AmberGold, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Carica Esempio", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (rawCsvText.isNotBlank()) {
                                IconButton(
                                    onClick = onClearData,
                                    modifier = Modifier.size(44.dp).testTag("clear_csv_text_btn")
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Cancella testo", tint = RoseRed)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = rawCsvText,
                        onValueChange = onCsvTextChanged,
                        placeholder = {
                            Text(
                                text = "Incolla qui le righe CSV con intestazione...\nEs: Titolo,Indirizzo,Prezzo,Superficie,Stato,Ristrutturazione,Target Rivendita\nTrilocale Porta Venezia,Via Tadino 24 Milano,240000,88,In Ristrutturazione,45000,370000",
                                fontSize = 11.sp,
                                color = TextMutedDark
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("raw_csv_text_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSlateBg,
                            unfocusedContainerColor = DarkSlateBg,
                            focusedBorderColor = BentoPurpleOnContainer,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: Sync Mode & Strategy Options
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SyncModeTabContent(
    currentMode: CsvSyncMode,
    onModeSelected: (CsvSyncMode) -> Unit,
    parsedSummary: CsvSyncSummary?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Seleziona la strategia di sincronizzazione:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        items(CsvSyncMode.values()) { mode ->
            val isSelected = currentMode == mode
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) BentoPurpleContainer.copy(alpha = 0.35f) else SurfaceCardDark
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) BentoPurpleOnContainer else SurfaceCardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        onClickLabel = "Seleziona modalità ${mode.labelIt}",
                        onClick = { onModeSelected(mode) }
                    )
                    .testTag("sync_mode_option_${mode.name}")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onModeSelected(mode) },
                        colors = RadioButtonDefaults.colors(selectedColor = BentoPurpleOnContainer)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = mode.labelIt,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BentoPurpleOnContainer else TextPrimaryDark
                            )
                            if (mode == CsvSyncMode.MERGE_UPDATE) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EmeraldGainBg,
                                    border = BorderStroke(1.dp, EmeraldGainBorder)
                                ) {
                                    Text(
                                        text = "CONSIGLIATO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldGainText,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mode.descriptionIt,
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        }

        // Mapping Detection Summary Card
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Text("Auto-Riconoscimento Intestazioni & Colonne", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }

                    if (parsedSummary != null && parsedSummary.matchedColumns.isNotEmpty()) {
                        Text(
                            text = "${parsedSummary.matchedColumns.size} colonne riconosciute automaticamente (Separatore: '${parsedSummary.detectedDelimiter}'):",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            parsedSummary.matchedColumns.forEach { (original, mapped) ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BentoPurpleHeader,
                                    border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "$original → $mapped",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BentoPurpleOnContainer,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Nessun dato CSV valido ancora caricato. Inserisci i dati nel primo passaggio.",
                            fontSize = 11.sp,
                            color = TextMutedDark
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: Preview of Parsed Properties
// -------------------------------------------------------------
@Composable
private fun PreviewTabContent(
    parsedSummary: CsvSyncSummary?,
    euroFormat: NumberFormat
) {
    if (parsedSummary == null || parsedSummary.validProperties.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = TextMutedDark,
                    modifier = Modifier.size(48.dp)
                )
                Text("Nessun dato pronto per l'anteprima", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark)
                Text("Carica o incolla un file CSV nel primo tab.", fontSize = 12.sp, color = TextMutedDark)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Warning / Error notification if any
        if (parsedSummary.errors.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberGold.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = "Attenzione", tint = AmberGold, modifier = Modifier.size(18.dp))
                        Text(
                            text = "${parsedSummary.errors.size} note durante l'analisi delle righe: ${parsedSummary.errors.firstOrNull() ?: ""}",
                            fontSize = 11.sp,
                            color = TextPrimaryDark
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Anteprima immobili estratti (${parsedSummary.validProperties.size}):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        items(parsedSummary.validProperties) { prop ->
            val status = PipelineStatus.fromKey(prop.pipelineStatus)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prop.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BentoPurpleHeader,
                            border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = status.labelIt,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoPurpleOnContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = prop.address,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    HorizontalDivider(color = SurfaceCardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Prezzo Acquisto", fontSize = 9.sp, color = TextMutedDark)
                            Text(euroFormat.format(prop.price), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        if (prop.estimatedRenovationCost > 0 || prop.actualRenovationCost > 0) {
                            Column {
                                Text("Ristrutturazione", fontSize = 9.sp, color = TextMutedDark)
                                Text(
                                    euroFormat.format(if (prop.actualRenovationCost > 0) prop.actualRenovationCost else prop.estimatedRenovationCost),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }
                        }

                        Column {
                            Text("Target Rivendita", fontSize = 9.sp, color = TextMutedDark)
                            Text(euroFormat.format(prop.effectiveExitValue), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldGainBg,
                            border = BorderStroke(1.dp, EmeraldGainBorder)
                        ) {
                            Text(
                                text = String.format(Locale.ITALY, "ROI +%.1f%%", prop.projectedRoiPercent),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGainText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (prop.notes.isNotBlank()) {
                        Text(
                            text = "Note: ${prop.notes}",
                            fontSize = 10.sp,
                            color = TextMutedDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
