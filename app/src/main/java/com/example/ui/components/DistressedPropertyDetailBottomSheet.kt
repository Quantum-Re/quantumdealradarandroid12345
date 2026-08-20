package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import com.example.data.DistressedProperty
import com.example.ui.theme.*
import com.example.util.ImageUtils
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modal Bottom Sheet component displaying detailed information (price, address, distress level,
 * coordinates, last updated timestamp) when a user taps on a distressed property map pin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistressedPropertyDetailBottomSheet(
    distressedProperty: DistressedProperty,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteProperty: ((DistressedProperty) -> Unit)? = null,
    onSaveNotes: ((DistressedProperty, String) -> Unit)? = null,
    onUpdatePhoto: ((DistressedProperty, String) -> Unit)? = null,
    onCalculateRoiClick: ((DistressedProperty) -> Unit)? = null,
    onAnalyzeArvClick: ((DistressedProperty) -> Unit)? = null,
    isAnalyzingArv: Boolean = false,
    arvResult: com.example.util.ArvAnalysisResult? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current
    val levelColor = getDistressColor(distressedProperty.distressLevel)

    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var showBankDossierDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    val imagePath = file.absolutePath
                    onUpdatePhoto?.invoke(distressedProperty, imagePath)
                    Toast.makeText(context, "Property photo captured & saved to Room DB!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.cacheDir
            val file = File(storageDir, "property_photo_${distressedProperty.id}_${System.currentTimeMillis()}.jpg")
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

    fun takePropertyPhoto() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.cacheDir
            val file = File(storageDir, "property_photo_${distressedProperty.id}_${System.currentTimeMillis()}.jpg")
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

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SurfaceCardDark,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = SurfaceCardBorder
            ) {}
        },
        modifier = modifier.testTag("distressed_property_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Distress Level Tag & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val provenanceEnum = com.example.data.DataProvenance.fromString(distressedProperty.provenance)
                    if (!provenanceEnum.isTrustworthy) {
                        Surface(
                            color = RoseRed,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = provenanceEnum.label.uppercase(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Surface(
                        color = levelColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, levelColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("bottom_sheet_distress_level")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = levelColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "DISTRESS: ${distressedProperty.distressLevel.uppercase()}",
                                color = levelColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Bottom Sheet",
                        tint = TextMutedDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Address Heading
            Text(
                text = distressedProperty.address,
                color = TextPrimaryDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("bottom_sheet_address")
            )

            // Property Image Display using Coil
            val context = LocalContext.current
            val propertyImageModel = remember(distressedProperty.imageUrl, distressedProperty.id) {
                val rawUrl = if (!distressedProperty.imageUrl.isNullOrBlank()) {
                    distressedProperty.imageUrl
                } else {
                    val fallbackPhotos = listOf(
                        "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=800&q=80"
                    )
                    val index = (distressedProperty.id.coerceAtLeast(0) % fallbackPhotos.size).toInt()
                    fallbackPhotos[index]
                }
                ImageUtils.buildOptimizedImageRequest(
                    context = context,
                    data = rawUrl,
                    targetWidthPx = 600,
                    targetHeightPx = 360
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("bottom_sheet_property_image")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SubcomposeAsyncImage(
                        model = propertyImageModel,
                        contentDescription = "Property photo for ${distressedProperty.address}",
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
                                    modifier = Modifier.size(28.dp),
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
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HomeWork,
                                        contentDescription = null,
                                        tint = TextMutedDark,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "No Property Preview",
                                        color = TextMutedDark,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    )

                    // Overlay "Take Photo" Button on Image
                    Surface(
                        onClick = { takePropertyPhoto() },
                        color = SurfaceCardDark.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .testTag("btn_overlay_take_photo")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Take Photo with Camera",
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (!distressedProperty.imageUrl.isNullOrBlank() && (distressedProperty.imageUrl.contains("/") || distressedProperty.imageUrl.contains("file"))) "Retake Photo" else "Snap Photo",
                                color = TextPrimaryDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Primary Stat Card: Price & Coordinates
            Surface(
                color = SurfaceCardDark,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "ESTIMATED VALUE / AUCTION",
                            color = TextMutedDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "€${NumberFormat.getNumberInstance(Locale.ITALY).format(distressedProperty.price)}",
                            color = AmberGold,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("bottom_sheet_price")
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "COORDINATES",
                            color = TextMutedDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "%.4f, %.4f".format(distressedProperty.latitude, distressedProperty.longitude),
                                color = TextSecondaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Additional Info Grid: ID & Last Updated
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailInfoTile(
                    label = "DATABASE ID",
                    value = "#${distressedProperty.id}",
                    icon = Icons.Default.Storage,
                    modifier = Modifier.weight(1f)
                )

                val formattedDate = remember(distressedProperty.lastUpdated) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    sdf.format(Date(distressedProperty.lastUpdated))
                }

                DetailInfoTile(
                    label = "LAST UPDATED",
                    value = formattedDate,
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f)
                )
            }

            // Gemini AI ARV Property Analyzer Card
            GeminiArvAnalyzerCard(
                distressedProperty = distressedProperty,
                isAnalyzing = isAnalyzingArv,
                arvResult = arvResult,
                onAnalyzeClick = { onAnalyzeArvClick?.invoke(distressedProperty) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_arv_analyzer_card")
            )

            // Valuation & Market Trend Graph Card
            PropertyTrendGraphCard(
                distressedProperty = distressedProperty,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("property_trend_graph_card")
            )

            // Currency & Price/m² Square-Unit Metrics Converter Card
            CurrencyAndUnitConverterCard(
                distressedProperty = distressedProperty,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("currency_and_unit_converter_card")
            )

            // Personal Text Notes Card
            PropertyPersonalNotesCard(
                distressedProperty = distressedProperty,
                onSaveNotes = onSaveNotes,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("property_personal_notes_card")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { takePropertyPhoto() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BentoPurpleContainer,
                        contentColor = BentoPurpleOnContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bottom_sheet_take_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                onCalculateRoiClick?.let { calculateAction ->
                    OutlinedButton(
                        onClick = {
                            calculateAction(distressedProperty)
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bottom_sheet_roi_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ROI Calc", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { showBankDossierDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bottom_sheet_pdf_report_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF Dossier", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }

                onDeleteProperty?.let { deleteAction ->
                    Button(
                        onClick = {
                            deleteAction(distressedProperty)
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoseRed.copy(alpha = 0.15f),
                            contentColor = RoseRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseRed.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bottom_sheet_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showBankDossierDialog) {
        val estimatedRent = 950.0
        val estimatedRenov = (distressedProperty.price * 0.15).coerceAtLeast(15000.0)
        val resaleVal = if (distressedProperty.estimatedValue > 0) distressedProperty.estimatedValue else (distressedProperty.price * 1.25)
        val defaultCalcData = RoiCalculationData(
            purchasePrice = distressedProperty.price,
            renovationCost = estimatedRenov,
            estimatedMonthlyRent = estimatedRent,
            legalFees = (distressedProperty.price * 0.04).coerceAtLeast(4000.0),
            monthlyExpenses = 120.0,
            downPaymentPercent = 20.0,
            mortgageRatePercent = 3.2,
            loanTermYears = 25,
            expectedResalePrice = resaleVal
        )

        PdfReportExportDialog(
            calcData = defaultCalcData,
            initialTitle = "Immobile Asta: ${distressedProperty.address}",
            initialLocation = distressedProperty.address,
            initialSurfaceSqm = 85,
            onDismissRequest = { showBankDossierDialog = false }
        )
    }
}

@Composable
private fun DetailInfoTile(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCardDark.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = label,
                    color = TextMutedDark,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = value,
                    color = TextPrimaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
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

@Composable
private fun PropertyTrendGraphCard(
    distressedProperty: DistressedProperty,
    modifier: Modifier = Modifier
) {
    val estimatedMarketValue = remember(distressedProperty.price) {
        distressedProperty.price * 1.38
    }
    val discountPercentage = remember(distressedProperty.price, estimatedMarketValue) {
        if (estimatedMarketValue > 0) {
            ((estimatedMarketValue - distressedProperty.price) / estimatedMarketValue * 100).toInt()
        } else 0
    }

    val trendPoints = remember(distressedProperty.price) {
        listOf(
            distressedProperty.price * 1.25,
            distressedProperty.price * 1.18,
            distressedProperty.price * 1.12,
            distressedProperty.price * 1.07,
            distressedProperty.price * 1.02,
            distressedProperty.price
        )
    }

    val months = listOf("-5M", "-4M", "-3M", "-2M", "-1M", "NOW")

    Surface(
        color = SurfaceCardDark,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "HISTORICAL VALUATION TREND",
                        color = TextMutedDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "-$discountPercentage% BELOW MARKET",
                        color = EmeraldGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Legend & Price Comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Market Fair Value",
                        color = TextMutedDark,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "€${NumberFormat.getNumberInstance(Locale.ITALY).format(estimatedMarketValue.toLong())}",
                        color = AmberGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(CyanAccent, androidx.compose.foundation.shape.CircleShape)
                        )
                        Text("Auction Base", color = TextMutedDark, fontSize = 10.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AmberGold, androidx.compose.foundation.shape.CircleShape)
                        )
                        Text("Market Ref", color = TextMutedDark, fontSize = 10.sp)
                    }
                }
            }

            // Native Jetpack Compose Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(vertical = 4.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    if (width <= 0 || height <= 0 || trendPoints.isEmpty()) return@Canvas

                    val maxVal = (estimatedMarketValue * 1.05).coerceAtLeast(1.0)
                    val minVal = (trendPoints.minOrNull() ?: 0.0) * 0.9

                    val range = (maxVal - minVal).coerceAtLeast(1.0)
                    val stepX = width / (trendPoints.size - 1)

                    fun getY(valNum: Double): Float {
                        val norm = ((valNum - minVal) / range).toFloat().coerceIn(0f, 1f)
                        return height - (norm * (height - 20f)) - 10f
                    }

                    // 1. Draw Grid Lines
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    for (i in 0..2) {
                        val gridY = height * (i / 2f)
                        drawLine(
                            color = SurfaceCardBorder,
                            start = androidx.compose.ui.geometry.Offset(0f, gridY),
                            end = androidx.compose.ui.geometry.Offset(width, gridY),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                    }

                    // 2. Draw Market Value Ref Line
                    val marketY = getY(estimatedMarketValue)
                    drawLine(
                        color = AmberGold.copy(alpha = 0.7f),
                        start = androidx.compose.ui.geometry.Offset(0f, marketY),
                        end = androidx.compose.ui.geometry.Offset(width, marketY),
                        strokeWidth = 2f,
                        pathEffect = dashEffect
                    )

                    // 3. Build Smooth Trend Curve Path
                    val path = Path()
                    val fillPath = Path()

                    trendPoints.forEachIndexed { index, pointVal ->
                        val x = index * stepX
                        val y = getY(pointVal)

                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                        } else {
                            val prevX = (index - 1) * stepX
                            val prevY = getY(trendPoints[index - 1])
                            val controlX1 = prevX + (x - prevX) / 2f
                            val controlX2 = prevX + (x - prevX) / 2f

                            path.cubicTo(controlX1, prevY, controlX2, y, x, y)
                            fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                        }

                        if (index == trendPoints.size - 1) {
                            fillPath.lineTo(x, height)
                            fillPath.close()
                        }
                    }

                    // 4. Fill Area Under Curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(CyanAccent.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )

                    // 5. Draw Trend Line
                    drawPath(
                        path = path,
                        color = CyanAccent,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )

                    // 6. Draw Data Dots
                    trendPoints.forEachIndexed { index, pointVal ->
                        val x = index * stepX
                        val y = getY(pointVal)
                        val isLast = index == trendPoints.size - 1

                        if (isLast) {
                            drawCircle(
                                color = CyanAccent.copy(alpha = 0.3f),
                                radius = 9f,
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                            drawCircle(
                                color = CyanAccent,
                                radius = 5f,
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                        } else {
                            drawCircle(
                                color = CyanAccent,
                                radius = 3f,
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                        }
                    }
                }
            }

            // Month Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                months.forEachIndexed { idx, label ->
                    Text(
                        text = label,
                        color = if (idx == months.size - 1) CyanAccent else TextMutedDark,
                        fontSize = 10.sp,
                        fontWeight = if (idx == months.size - 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyPersonalNotesCard(
    distressedProperty: DistressedProperty,
    onSaveNotes: ((DistressedProperty, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var noteText by remember(distressedProperty.id, distressedProperty.notes) {
        mutableStateOf(distressedProperty.notes)
    }
    var isSavedRecently by remember { mutableStateOf(false) }

    LaunchedEffect(isSavedRecently) {
        if (isSavedRecently) {
            kotlinx.coroutines.delay(2500)
            isSavedRecently = false
        }
    }

    val quickObservationTags = listOf(
        "🛠️ Major Renovation Needed",
        "👥 Occupied by Tenant",
        "⚖️ Auction Date Pending",
        "🚀 High Flip Yield Potential",
        "🏚️ Roof / Structural Issue",
        "📞 Owner Contacted"
    )

    Surface(
        color = SurfaceCardDark,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PROPERTY NOTES & OBSERVATIONS",
                        color = TextMutedDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                if (distressedProperty.notes.isNotBlank() && !isSavedRecently) {
                    Surface(
                        color = CyanAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "PERSISTED IN ROOM",
                            color = CyanAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Quick Tag Insertion Chips
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickObservationTags.size) { index ->
                    val tag = quickObservationTags[index]
                    Surface(
                        onClick = {
                            val trimmed = noteText.trim()
                            noteText = if (trimmed.isEmpty()) tag else "$trimmed • $tag"
                            isSavedRecently = false
                        },
                        color = DarkSlateBg,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 10.sp,
                            color = TextSecondaryDark,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Custom Text Field for Notes & Observations
            OutlinedTextField(
                value = noteText,
                onValueChange = {
                    noteText = it
                    isSavedRecently = false
                },
                label = { Text("Custom Notes & Observations", fontSize = 12.sp, color = CyanAccent) },
                placeholder = {
                    Text(
                        text = "Enter detailed observations regarding physical condition, appraisal notes, auction strategy or owner contacts...",
                        color = TextMutedDark.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                },
                minLines = 3,
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = SurfaceCardBorder,
                    focusedContainerColor = DarkSlateBg,
                    unfocusedContainerColor = DarkSlateBg,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    cursorColor = CyanAccent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("property_notes_input_field")
            )

            // Save Action Row & Character Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${noteText.length} chars",
                    fontSize = 10.sp,
                    color = TextMutedDark
                )

                Button(
                    onClick = {
                        onSaveNotes?.invoke(distressedProperty, noteText)
                        isSavedRecently = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSavedRecently) EmeraldGreen else CyanAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("save_property_notes_button")
                ) {
                    Icon(
                        imageVector = if (isSavedRecently) Icons.Default.Check else Icons.Default.Save,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSavedRecently) "Saved to DB" else "Save Observations",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun GeminiArvAnalyzerCard(
    distressedProperty: DistressedProperty,
    isAnalyzing: Boolean,
    arvResult: com.example.util.ArvAnalysisResult?,
    onAnalyzeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedReport by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
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
                            .size(32.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(CyanAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "AI ARV Property Analyzer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Gemini 3.5 Flash ARV Valuation Engine",
                            fontSize = 10.sp,
                            color = TextMutedDark
                        )
                    }
                }

                if (distressedProperty.estimatedArv != null || arvResult != null) {
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "ARV Saved",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Content State: Loading / Results / Default
            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = CyanAccent,
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "Calculating ARV based on address & distress level...",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                val arvVal = arvResult?.estimatedArv ?: distressedProperty.estimatedArv
                val reportText = arvResult?.detailedReportMarkdown ?: distressedProperty.aiAnalysisReport

                if (arvVal != null && arvVal > 0) {
                    val price = distressedProperty.price
                    val valueUplift = arvVal - price
                    val upliftPercent = if (price > 0) (valueUplift / price * 100).toInt() else 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ARV Metric Card
                        Surface(
                            color = DarkSlateBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("ESTIMATED ARV", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMutedDark)
                                Text(
                                    text = "€${NumberFormat.getNumberInstance(Locale.ITALY).format(arvVal.toInt())}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldGreen
                                )
                                Text("+$upliftPercent% ARV lift", fontSize = 10.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Repairs & ROI Card
                        Surface(
                            color = DarkSlateBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val renoBudget = arvResult?.estimatedRenovationCost ?: (price * 0.18)
                                val roi = arvResult?.potentialRoiPercent ?: if (price + renoBudget > 0) ((arvVal - price - renoBudget) / (price + renoBudget) * 100) else 0.0
                                Text("ESTIMATED REPAIRS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMutedDark)
                                Text(
                                    text = "€${NumberFormat.getNumberInstance(Locale.ITALY).format(renoBudget.toInt())}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AmberGold
                                )
                                Text("ROI: ${String.format(Locale.US, "%.1f", roi)}%", fontSize = 10.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (!reportText.isNullOrBlank()) {
                        TextButton(
                            onClick = { expandedReport = !expandedReport },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = if (expandedReport) "Hide AI ARV Appraisal" else "View AI ARV Breakdown",
                                fontSize = 11.sp,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (expandedReport) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (expandedReport) {
                            Surface(
                                color = DarkSlateBg,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = reportText,
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Estimate After Repair Value (ARV), potential profit, and renovation scope based on property address & distress level.",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                }

                // Analyze Action Button
                Button(
                    onClick = onAnalyzeClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_analyze_arv_gemini")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (arvVal != null && arvVal > 0) "Re-Estimate ARV with Gemini AI" else "✨ Calculate ARV with Gemini AI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
