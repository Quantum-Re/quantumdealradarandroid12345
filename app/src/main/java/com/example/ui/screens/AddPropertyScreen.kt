package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.DataProvenance
import com.example.data.Property
import com.example.ui.PropertyViewModel
import com.example.ui.theme.*
import com.example.util.CsvExporter
import com.example.util.ImageUtils
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPropertyScreen(
    viewModel: PropertyViewModel,
    modifier: Modifier = Modifier,
    onPropertySaved: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val propertiesList by viewModel.properties.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var estimatedValueText by remember { mutableStateOf("") }
    var surfaceSqmText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var capturedPhotoUri by remember { mutableStateOf<String?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            capturedPhotoUri = tempCameraUri.toString()
        }
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val saved = ImageUtils.saveBitmapToStorage(context, bitmap)
            if (saved != null) {
                capturedPhotoUri = saved
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = ImageUtils.copyGalleryUriToStorage(context, uri)
            capturedPhotoUri = saved ?: uri.toString()
        }
    }

    var selectedDistressStatus by remember { mutableStateOf("ASTA") }
    var selectedPropertyType by remember { mutableStateOf("Residenziale") }
    
    val strategyOptions = listOf("Fix & Flip", "Rental", "BRRRR", "Messa a Reddito", "Riqualificazione")
    var selectedStrategiesForNewProp by remember { mutableStateOf(setOf("Fix & Flip")) }

    var isDistressMenuExpanded by remember { mutableStateOf(false) }
    var isTypeMenuExpanded by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessSnackbar by remember { mutableStateOf(false) }

    val distressStatusOptions = listOf("ASTA", "NPL", "STRALCIO", "Pre-Asta", "Sconto Elevato", "Nessuno")
    val propertyTypeOptions = listOf("Residenziale", "Commerciale", "Industriale", "Terreno", "Altro")

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showSuccessSnackbar) {
        if (showSuccessSnackbar) {
            snackbarHostState.showSnackbar("Proprietà salvata con successo nel database Room!")
            showSuccessSnackbar = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkSlateBg,
        modifier = modifier
            .fillMaxSize()
            .testTag("add_property_screen")
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                // Header Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CyanAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddBusiness,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Inserisci Nuova Proprietà",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "Aggiungi un immobile o opportunità al database Room",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }

            item {
                // Property Input Form Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Dettagli Immobile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanAccent
                        )

                        // Title field (Optional/Descriptive)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Titolo / Descrizione (opzionale)") },
                            placeholder = { Text("es. Trilocale Centro Storico") },
                            leadingIcon = {
                                Icon(Icons.Default.HomeWork, contentDescription = null, tint = TextSecondaryDark)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("title_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = SurfaceCardBorder,
                                focusedLabelColor = CyanAccent,
                                unfocusedLabelColor = TextSecondaryDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            )
                        )

                        // Address field (REQUIRED)
                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it
                                errorMessage = null
                            },
                            label = { Text("Indirizzo *") },
                            placeholder = { Text("es. Via Roma 12, Milano") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = CyanAccent)
                            },
                            isError = errorMessage != null && address.isBlank(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("address_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = SurfaceCardBorder,
                                focusedLabelColor = CyanAccent,
                                unfocusedLabelColor = TextSecondaryDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            )
                        )

                        // Price field (REQUIRED)
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = {
                                priceText = it
                                errorMessage = null
                            },
                            label = { Text("Prezzo Richiesto (€) *") },
                            placeholder = { Text("es. 120000") },
                            leadingIcon = {
                                Icon(Icons.Default.Euro, contentDescription = null, tint = AmberGold)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = errorMessage != null && priceText.toDoubleOrNull() == null,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("price_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberGold,
                                unfocusedBorderColor = SurfaceCardBorder,
                                focusedLabelColor = AmberGold,
                                unfocusedLabelColor = TextSecondaryDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            )
                        )

                        // Distress Status Dropdown/Selector (REQUIRED)
                        ExposedDropdownMenuBox(
                            expanded = isDistressMenuExpanded,
                            onExpandedChange = { isDistressMenuExpanded = !isDistressMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedDistressStatus,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Stato Opportunità / Distress *") },
                                leadingIcon = {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = RoseRed)
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDistressMenuExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("distress_status_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoseRed,
                                    unfocusedBorderColor = SurfaceCardBorder,
                                    focusedLabelColor = RoseRed,
                                    unfocusedLabelColor = TextSecondaryDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = isDistressMenuExpanded,
                                onDismissRequest = { isDistressMenuExpanded = false }
                            ) {
                                distressStatusOptions.forEach { statusOption ->
                                    DropdownMenuItem(
                                        text = { Text(statusOption) },
                                        onClick = {
                                            selectedDistressStatus = statusOption
                                            isDistressMenuExpanded = false
                                        },
                                        modifier = Modifier.testTag("distress_option_$statusOption")
                                    )
                                }
                            }
                        }

                        // Property Type Dropdown
                        ExposedDropdownMenuBox(
                            expanded = isTypeMenuExpanded,
                            onExpandedChange = { isTypeMenuExpanded = !isTypeMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedPropertyType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipologia Immobile") },
                                leadingIcon = {
                                    Icon(Icons.Default.Category, contentDescription = null, tint = TextSecondaryDark)
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeMenuExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("property_type_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = SurfaceCardBorder,
                                    focusedLabelColor = CyanAccent,
                                    unfocusedLabelColor = TextSecondaryDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = isTypeMenuExpanded,
                                onDismissRequest = { isTypeMenuExpanded = false }
                            ) {
                                propertyTypeOptions.forEach { typeOption ->
                                    DropdownMenuItem(
                                        text = { Text(typeOption) },
                                        onClick = {
                                            selectedPropertyType = typeOption
                                            isTypeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Row with Surface Sqm & Estimated Value
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = surfaceSqmText,
                                onValueChange = { surfaceSqmText = it },
                                label = { Text("Superficie (mq)") },
                                placeholder = { Text("85") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("surface_sqm_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = SurfaceCardBorder,
                                    focusedLabelColor = CyanAccent,
                                    unfocusedLabelColor = TextSecondaryDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )

                            OutlinedTextField(
                                value = estimatedValueText,
                                onValueChange = { estimatedValueText = it },
                                label = { Text("Valore Mkt Est. (€)") },
                                placeholder = { Text("180000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("estimated_value_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = SurfaceCardBorder,
                                    focusedLabelColor = CyanAccent,
                                    unfocusedLabelColor = TextSecondaryDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )
                        }

                        // Investment Strategy Tags Selection
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Tag Strategia di Investimento (Seleziona una o più)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondaryDark
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                strategyOptions.forEach { strat ->
                                    val isSelected = selectedStrategiesForNewProp.contains(strat)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedStrategiesForNewProp = if (isSelected) {
                                                if (selectedStrategiesForNewProp.size > 1) selectedStrategiesForNewProp - strat else selectedStrategiesForNewProp
                                            } else {
                                                selectedStrategiesForNewProp + strat
                                            }
                                        },
                                        label = { Text(strat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyanAccent,
                                            selectedLabelColor = Color.Black,
                                            selectedLeadingIconColor = Color.Black,
                                            containerColor = SurfaceCardDark,
                                            labelColor = TextSecondaryDark
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = SurfaceCardBorder,
                                            selectedBorderColor = CyanAccent
                                        ),
                                        modifier = Modifier.testTag("strategy_chip_select_$strat")
                                    )
                                }
                            }
                        }

                        // Notes & Research field
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("Note & Ricerche sull'Opportunità") },
                            placeholder = { Text("es. Perizia visionata, udienza fissata il 15/10, costo ristrutturazione stimato 25.000€...") },
                            leadingIcon = {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = CyanAccent)
                            },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notes_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = SurfaceCardBorder,
                                focusedLabelColor = CyanAccent,
                                unfocusedLabelColor = TextSecondaryDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            )
                        )

                        // Camera / Photo Capture Section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Foto Immobile / Perizia",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondaryDark
                            )

                            if (capturedPhotoUri != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, AmberGold, RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = ImageUtils.buildOptimizedImageRequest(
                                            context = context,
                                            data = capturedPhotoUri,
                                            targetWidthPx = 500,
                                            targetHeightPx = 300
                                        ),
                                        contentDescription = "Foto dell'immobile",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    IconButton(
                                        onClick = { capturedPhotoUri = null },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(32.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Rimuovi foto", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val uri = ImageUtils.createCameraImageUri(context)
                                            if (uri != null) {
                                                tempCameraUri = uri
                                                try {
                                                    cameraLauncher.launch(uri)
                                                } catch (e: Exception) {
                                                    cameraPreviewLauncher.launch(null)
                                                }
                                            } else {
                                                cameraPreviewLauncher.launch(null)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("take_photo_button")
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Scatta Foto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("pick_gallery_button")
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Galleria", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Error Message
                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            errorMessage?.let { msg ->
                                Surface(
                                    color = RoseRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RoseRed)
                                        Text(text = msg, color = RoseRed, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // Submit Save Button
                        Button(
                            onClick = {
                                val parsedPrice = priceText.toDoubleOrNull()
                                if (address.isBlank()) {
                                    errorMessage = "Inserisci un indirizzo valido per la proprietà."
                                    return@Button
                                }
                                if (parsedPrice == null || parsedPrice <= 0.0) {
                                    errorMessage = "Inserisci un prezzo valido maggiore di zero."
                                    return@Button
                                }

                                val parsedEstimatedValue = estimatedValueText.toDoubleOrNull() ?: parsedPrice
                                val parsedSurfaceSqm = surfaceSqmText.toIntOrNull() ?: 0

                                val strategyTagsString = selectedStrategiesForNewProp.joinToString(", ").ifBlank { "Fix & Flip" }
                                val now = System.currentTimeMillis()

                                val newProperty = Property(
                                    title = title.ifBlank { "Immobile $selectedDistressStatus - $address" },
                                    address = address.trim(),
                                    price = parsedPrice,
                                    distressStatus = selectedDistressStatus,
                                    propertyType = selectedPropertyType,
                                    estimatedMarketValue = parsedEstimatedValue,
                                    surfaceSqm = parsedSurfaceSqm,
                                    notes = notesText.trim(),
                                    photoUri = capturedPhotoUri,
                                    strategyTags = strategyTagsString,
                                    provenance = DataProvenance.USER_ENTERED.name,
                                    retrievedAt = now,
                                    createdAt = now
                                )

                                viewModel.addProperty(newProperty)

                                // Reset form inputs
                                title = ""
                                address = ""
                                priceText = ""
                                estimatedValueText = ""
                                surfaceSqmText = ""
                                notesText = ""
                                capturedPhotoUri = null
                                selectedStrategiesForNewProp = setOf("Fix & Flip")
                                errorMessage = null
                                showSuccessSnackbar = true

                                onPropertySaved?.invoke()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_property_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Salva Proprietà nel Database",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Saved Properties Section Header & Filters
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Proprietà nel Database (${uiState.properties.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            if (propertiesList.size != uiState.properties.size) {
                                Text(
                                    text = "Filtro attivo: ${uiState.properties.size} di ${propertiesList.size} mostrati",
                                    fontSize = 11.sp,
                                    color = AmberGold
                                )
                            }
                        }

                        if (propertiesList.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { CsvExporter.exportPropertiesToCsv(context, propertiesList) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("export_csv_button")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Esporta CSV", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Esporta CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                TextButton(
                                    onClick = { viewModel.clearAllProperties() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = RoseRed)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pulisci tutto", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Search Input Field
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Cerca per indirizzo o titolo...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = CyanAccent)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Cancella ricerca", tint = TextSecondaryDark)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_property_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )

                    // Distressed Only Toggle & Status Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Distressed Toggle Button / Switch
                        Surface(
                            onClick = { viewModel.toggleShowOnlyDistressed() },
                            shape = RoundedCornerShape(20.dp),
                            color = if (uiState.showOnlyDistressed) RoseRed.copy(alpha = 0.25f) else SurfaceCardDark,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (uiState.showOnlyDistressed) RoseRed else SurfaceCardBorder
                            ),
                            modifier = Modifier.testTag("toggle_distressed_filter")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.showOnlyDistressed) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                                    contentDescription = null,
                                    tint = if (uiState.showOnlyDistressed) RoseRed else TextSecondaryDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Solo Distressed",
                                    fontSize = 12.sp,
                                    fontWeight = if (uiState.showOnlyDistressed) FontWeight.Bold else FontWeight.Medium,
                                    color = if (uiState.showOnlyDistressed) RoseRed else TextSecondaryDark
                                )
                                Switch(
                                    checked = uiState.showOnlyDistressed,
                                    onCheckedChange = { viewModel.setShowOnlyDistressed(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = RoseRed,
                                        checkedTrackColor = RoseRed.copy(alpha = 0.3f),
                                        uncheckedThumbColor = TextSecondaryDark,
                                        uncheckedTrackColor = SurfaceCardDark
                                    ),
                                    modifier = Modifier.height(20.dp)
                                )
                            }
                        }

                        // Filter reset if active
                        if (uiState.selectedStatusFilter != "ALL" || uiState.selectedStrategies.isNotEmpty() || uiState.showOnlyDistressed || uiState.searchQuery.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    viewModel.updateSearchQuery("")
                                    viewModel.updateStatusFilter("ALL")
                                    viewModel.clearStrategyFilters()
                                    viewModel.setShowOnlyDistressed(false)
                                }
                            ) {
                                Text("Azzera filtri", fontSize = 11.sp, color = CyanAccent)
                            }
                        }
                    }

                    // Multi-Select Investment Strategy Filter Chips Row
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filtra per Strategia (Multi-selezione):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondaryDark
                            )
                            if (uiState.selectedStrategies.isNotEmpty()) {
                                Text(
                                    text = "${uiState.selectedStrategies.size} selezionate",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            strategyOptions.forEach { stratKey ->
                                val isSelected = uiState.selectedStrategies.contains(stratKey)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.toggleStrategyFilter(stratKey) },
                                    label = { Text(stratKey, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldGreen,
                                        selectedLabelColor = Color.Black,
                                        selectedLeadingIconColor = Color.Black,
                                        containerColor = SurfaceCardDark,
                                        labelColor = TextSecondaryDark
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = SurfaceCardBorder,
                                        selectedBorderColor = EmeraldGreen
                                    ),
                                    modifier = Modifier.testTag("filter_chip_strategy_$stratKey")
                                )
                            }
                        }
                    }

                    // Status Filter Chips Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filterOptions = listOf(
                            "ALL" to "Tutti",
                            "ASTA" to "Asta",
                            "NPL" to "NPL",
                            "STRALCIO" to "Stralcio",
                            "Pre-Asta" to "Pre-Asta"
                        )

                        filterOptions.forEach { (statusKey, label) ->
                            val isSelected = (uiState.selectedStatusFilter == statusKey)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateStatusFilter(statusKey) },
                                label = { Text(label, fontSize = 11.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberGold,
                                    selectedLabelColor = Color.Black,
                                    selectedLeadingIconColor = Color.Black,
                                    containerColor = SurfaceCardDark,
                                    labelColor = TextSecondaryDark
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = SurfaceCardBorder,
                                    selectedBorderColor = AmberGold
                                ),
                                modifier = Modifier.testTag("filter_chip_$statusKey")
                            )
                        }
                    }
                }
            }

            // Saved Properties List Items
            if (uiState.properties.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = TextSecondaryDark,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = if (propertiesList.isEmpty()) "Nessuna proprietà nel database" else "Nessuna proprietà trovata con i filtri correnti",
                                    fontSize = 14.sp,
                                    color = TextSecondaryDark,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (propertiesList.isEmpty()) "Compila il modulo in alto per aggiungere il primo immobile." else "Prova a modificare la ricerca o disattivare il filtro 'Solo Distressed'.",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            } else {
                items(uiState.properties, key = { it.id }) { prop ->
                    SavedPropertyCard(
                        property = prop,
                        onDeleteClick = { viewModel.deleteProperty(prop) },
                        onPhotoUpdated = { newUri ->
                            viewModel.updateProperty(prop.copy(photoUri = newUri))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedPropertyCard(
    property: Property,
    onDeleteClick: () -> Unit,
    onPhotoUpdated: ((String) -> Unit)? = null
) {
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY) }
    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            onPhotoUpdated?.invoke(tempCameraUri.toString())
        }
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val saved = ImageUtils.saveBitmapToStorage(context, bitmap)
            if (saved != null) {
                onPhotoUpdated?.invoke(saved)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = ImageUtils.copyGalleryUriToStorage(context, uri)
            onPhotoUpdated?.invoke(saved ?: uri.toString())
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
            .testTag("saved_property_card_${property.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Display Saved Property Photo if present
            if (!property.photoUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = ImageUtils.buildOptimizedImageRequest(
                            context = context,
                            data = property.photoUri,
                            targetWidthPx = 500,
                            targetHeightPx = 300
                        ),
                        contentDescription = "Foto della proprietà",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clickable {
                                galleryLauncher.launch("image/*")
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("Cambia Foto", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (property.distressStatus.uppercase()) {
                        "ASTA" -> RoseRed.copy(alpha = 0.2f)
                        "NPL" -> PurpleIndigo.copy(alpha = 0.2f)
                        "STRALCIO" -> AmberGold.copy(alpha = 0.2f)
                        else -> CyanAccent.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = property.distressStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (property.distressStatus.uppercase()) {
                            "ASTA" -> RoseRed
                            "NPL" -> PurpleIndigo
                            "STRALCIO" -> AmberGold
                            else -> CyanAccent
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (property.photoUri.isNullOrBlank() && onPhotoUpdated != null) {
                        IconButton(
                            onClick = {
                                val uri = ImageUtils.createCameraImageUri(context)
                                if (uri != null) {
                                    tempCameraUri = uri
                                    try {
                                        cameraLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        cameraPreviewLauncher.launch(null)
                                    }
                                } else {
                                    cameraPreviewLauncher.launch(null)
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = "Scatta Foto",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Elimina",
                            tint = RoseRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = property.title.ifBlank { property.address },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
                Text(
                    text = property.address,
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }

            if (property.strategyTags.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    property.strategyTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
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

            if (property.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceCardDark.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
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
                            text = property.notes,
                            fontSize = 12.sp,
                            color = TextSecondaryDark,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Divider(color = SurfaceCardBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Prezzo", fontSize = 10.sp, color = TextSecondaryDark)
                    Text(
                        text = numberFormat.format(property.price),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold
                    )
                }

                if (property.surfaceSqm > 0) {
                    Column {
                        Text("Superficie", fontSize = 10.sp, color = TextSecondaryDark)
                        Text(
                            text = "${property.surfaceSqm} mq",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                    }
                }

                Column {
                    Text("Valore Stimato", fontSize = 10.sp, color = TextSecondaryDark)
                    Text(
                        text = numberFormat.format(property.estimatedMarketValue),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldGreen
                    )
                }
            }
        }
    }
}
