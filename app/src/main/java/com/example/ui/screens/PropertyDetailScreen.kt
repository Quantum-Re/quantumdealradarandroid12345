package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PipelineStatus
import com.example.data.Property
import com.example.ui.theme.*
import com.example.util.ImageUtils
import com.example.ui.components.PredictiveDealAlertCard
import com.example.ui.components.RichTextNotesEditor
import com.example.util.PropertyPdfGenerator
import com.example.util.PropertyOpportunityEngine
import com.example.util.PropertyOpportunityEvaluation
import com.example.util.OpportunityTier
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getPropertyFallbackImageUrl(property: Property): String {
    if (!property.photoUri.isNullOrBlank()) {
        return property.photoUri
    }
    return when ((property.id % 4).toInt()) {
        0 -> "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=800"
        1 -> "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=800"
        2 -> "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800"
        else -> "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800"
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    property: Property,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    onBackClick: () -> Unit,
    onEditFinancialsClick: () -> Unit,
    onUpdateStatusClick: () -> Unit,
    onUpdateProgressClick: () -> Unit,
    onSimulatePriceDropClick: () -> Unit,
    onCalculateRoiClick: () -> Unit,
    onOpenRenovationSimulator: (() -> Unit)? = null,
    onDeleteClick: () -> Unit,
    onSaveNotes: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    with(sharedTransitionScope) {
        val context = LocalContext.current
    val euroFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
            maximumFractionDigits = 0
        }
    }

    val opportunityEvaluation = remember(property) {
        PropertyOpportunityEngine.evaluateProperty(property)
    }

    var activeTab by remember { mutableIntStateOf(0) }

    // Intercept hardware / gesture back navigation to smoothly transition back
    BackHandler(onBack = onBackClick)

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

    val imageUrl = remember(property.id, property.photoUri) { getPropertyFallbackImageUrl(property) }

    // Surface bounds shared element container for the whole screen
    Surface(
        modifier = modifier
            .fillMaxSize()
            .sharedBounds(
                rememberSharedContentState(key = "property_card_${property.id}"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ ->
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                }
            )
            .testTag("property_detail_screen_${property.id}"),
        color = DarkSlateBg
    ) {
        Scaffold(
            topBar = {
                // Top App Bar with back button, share and action icons
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = property.title.ifBlank { property.address },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = property.address,
                                fontSize = 12.sp,
                                color = TextMutedDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("property_detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Indietro",
                                tint = TextPrimaryDark
                            )
                        }
                    },
                    actions = {
                        // Export PDF Dossier
                        IconButton(
                            onClick = {
                                PropertyPdfGenerator.generateAndSharePdf(context, property, emailOnly = false)
                            },
                            modifier = Modifier.testTag("property_detail_export_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Esporta Dossier PDF",
                                tint = BentoPurpleOnContainer
                            )
                        }

                        // Edit Financials
                        IconButton(
                            onClick = onEditFinancialsClick,
                            modifier = Modifier.testTag("property_detail_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modifica Parametri",
                                tint = TextPrimaryDark
                            )
                        }

                        // Delete Property
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.testTag("property_detail_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Elimina Immobile",
                                tint = RoseRed
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SurfaceCardDark.copy(alpha = 0.95f),
                        titleContentColor = TextPrimaryDark
                    )
                )
            },
            containerColor = DarkSlateBg
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero Image with Shared Element Morph
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .sharedElement(
                                rememberSharedContentState(key = "property_image_${property.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                }
                            )
                    ) {
                        val context = LocalContext.current
                        AsyncImage(
                            model = ImageUtils.buildOptimizedImageRequest(
                                context = context,
                                data = imageUrl,
                                targetWidthPx = 800,
                                targetHeightPx = 560
                            ),
                            contentDescription = property.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Gradient Scrim for readable badges
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.2f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                        )

                        // Top Badges Overlay: Strategy + Distress Status + Demo Badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val provenanceEnum = com.example.data.DataProvenance.fromString(property.provenance)
                                if (!provenanceEnum.isTrustworthy) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = RoseRed,
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = provenanceEnum.label.uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Shared Strategy Tag
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BentoPurpleContainer,
                                    border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.5f)),
                                    modifier = Modifier.sharedBounds(
                                        rememberSharedContentState(key = "property_strategy_${property.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                ) {
                                    Text(
                                        text = property.strategyTags.ifBlank { "Fix & Flip" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoPurpleOnContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (property.distressStatus.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Gavel,
                                            contentDescription = null,
                                            tint = AmberGold,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = property.distressStatus,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Hero Content: Title, Sqm & Price with Shared Bounds
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = property.title.ifBlank { property.address },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.sharedBounds(
                                    rememberSharedContentState(key = "property_title_${property.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            )

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
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = property.address,
                                        fontSize = 13.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                    if (property.surfaceSqm > 0) {
                                        Text(
                                            text = "• ${property.surfaceSqm} m²",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CyanAccent
                                        )
                                    }
                                    if (property.latitude == 0.0 && property.longitude == 0.0) {
                                        Surface(
                                            color = AmberGold.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "Posizione non disponibile",
                                                color = AmberGold,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (property.evidenceRef?.contains("approssimata") == true) {
                                        Surface(
                                            color = CyanAccent.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "Posizione approssimata al comune",
                                                color = CyanAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Interactive Pipeline Status Badge in Hero
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = statusBg,
                                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clickable { onUpdateStatusClick() }
                                        .sharedBounds(
                                            rememberSharedContentState(key = "property_status_${property.id}"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                        .testTag("property_detail_status_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(statusColor)
                                        )
                                        Text(
                                            text = status.labelIt,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Financial Highlight Bar (Shared Price & Key Returns)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("property_detail_financial_hero")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shared Price Block
                            Column(
                                modifier = Modifier.sharedBounds(
                                    rememberSharedContentState(key = "property_price_${property.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            ) {
                                Text("Prezzo Acquisto", fontSize = 11.sp, color = TextMutedDark)
                                Text(
                                    text = euroFormat.format(property.price),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimaryDark
                                )
                                if (property.surfaceSqm > 0) {
                                    val pricePerSqm = property.price / property.surfaceSqm
                                    Text(
                                        text = "${euroFormat.format(pricePerSqm)}/m²",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }

                            // Cost Basis (Acquisto + Lavori)
                            Column {
                                Text("Costo Totale", fontSize = 11.sp, color = TextMutedDark)
                                Text(
                                    text = euroFormat.format(property.totalCostBasis),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                val reno = if (property.actualRenovationCost > 0) property.actualRenovationCost else property.estimatedRenovationCost
                                Text(
                                    text = "Lavori: ${euroFormat.format(reno)}",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }

                            // Margine Previsto & ROI
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Plusvalenza", fontSize = 11.sp, color = TextMutedDark)
                                Text(
                                    text = "+${euroFormat.format(property.projectedProfit)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldGreen
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldGainBg,
                                    border = BorderStroke(1.dp, EmeraldGainBorder),
                                    modifier = Modifier.padding(top = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = EmeraldGainText,
                                            modifier = Modifier.size(11.dp)
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
                    }
                }

                // 2b. Real-Time Opportunity Score & Sottovalutazione Mercato (Live Immobiliare.it Comps)
                item {
                    val eval = opportunityEvaluation
                    val tierColor = Color(eval.tier.colorHex)

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                        border = BorderStroke(1.dp, tierColor.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("property_detail_opportunity_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header
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
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = tierColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "OPPORTUNITY RADAR",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tierColor,
                                        letterSpacing = 0.8.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = tierColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, tierColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = eval.tier.label,
                                        color = tierColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Score and Undervaluation Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "${eval.opportunityScore}",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black,
                                            color = tierColor
                                        )
                                        Text(
                                            text = "/100",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextSecondaryDark,
                                            modifier = Modifier.padding(bottom = 5.dp)
                                        )
                                    }
                                    Text(
                                        text = eval.headline,
                                        fontSize = 12.sp,
                                        color = TextPrimaryDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (eval.undervaluedPercent >= 0) "Sconto su Mercato" else "Sovrapprezzo",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                    Text(
                                        text = "${if (eval.undervaluedPercent >= 0) "+" else ""}${String.format(Locale.US, "%.1f", eval.undervaluedPercent)}%",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = tierColor
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { eval.opportunityScore / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = tierColor,
                                trackColor = Color(0xFF1E293B)
                            )

                            // Comps breakdown
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Prezzo/m² Nostro", fontSize = 10.sp, color = TextMutedDark)
                                        Text("€${String.format(Locale.ITALY, "%,.0f", eval.propertyPricePerSqm)}/m²", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                    }
                                    Column {
                                        Text("Media Zona Live", fontSize = 10.sp, color = TextMutedDark)
                                        Text("€${String.format(Locale.ITALY, "%,.0f", eval.liveMarketPricePerSqm)}/m²", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tierColor)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Alpha Equity Gain", fontSize = 10.sp, color = TextMutedDark)
                                        Text("${if (eval.alphaEquityGain >= 0) "+€" else "-€"}${String.format(Locale.ITALY, "%,.0f", kotlin.math.abs(eval.alphaEquityGain))}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (eval.alphaEquityGain >= 0) Color(0xFF10B981) else Color(0xFFEF4444))
                                    }
                                }
                            }

                            // Strategic Insight Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = eval.actionableInsight,
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // 2c. Predictive Deal Alert & Top 10% Historical Yield Potential
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        PredictiveDealAlertCard(property = property)
                    }
                }

                // 3. Pipeline Lifecycle Stepper
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Fasi Operazione Immobiliare",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                TextButton(
                                    onClick = onUpdateStatusClick,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Cambia Fase", fontSize = 12.sp, color = BentoPurpleOnContainer, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Stepper Row
                            val stages = listOf(
                                PipelineStatus.ANALYZED,
                                PipelineStatus.IN_ESCROW,
                                PipelineStatus.RENOVATING,
                                PipelineStatus.LISTED,
                                PipelineStatus.SOLD
                            )
                            val currentStageIndex = stages.indexOf(status).let { if (it == -1) 0 else it }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                stages.forEachIndexed { index, stage ->
                                    val isPassedOrCurrent = index <= currentStageIndex
                                    val isCurrent = index == currentStageIndex
                                    val stageColor = if (isCurrent) BentoPurpleOnContainer else if (isPassedOrCurrent) EmeraldGreen else Color(0xFF64748B)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isCurrent) BentoPurpleContainer
                                                    else if (isPassedOrCurrent) EmeraldGainBg
                                                    else Color(0xFF1E293B)
                                                )
                                                .border(
                                                    1.5.dp,
                                                    stageColor,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isPassedOrCurrent && !isCurrent) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = EmeraldGreen,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = stageColor
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stage.labelIt,
                                            fontSize = 9.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) BentoPurpleOnContainer else TextSecondaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Renovation SAL & Progress (If Renovating or Analyzed)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
                                        Icons.Default.Construction,
                                        contentDescription = null,
                                        tint = AmberGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Avanzamento Cantiere & SAL",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }

                                TextButton(
                                    onClick = onUpdateProgressClick,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Aggiorna SAL", fontSize = 12.sp, color = BentoPurpleOnContainer, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Progress bar
                            LinearProgressIndicator(
                                progress = { property.renovationProgressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = BentoPurpleOnContainer,
                                trackColor = Color(0xFF1E293B)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "SAL Completato: ${property.renovationProgressPercent}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPurpleOnContainer
                                )

                                val estReno = property.estimatedRenovationCost
                                val actReno = property.actualRenovationCost
                                val variance = actReno - estReno
                                val varianceText = when {
                                    actReno <= 0 -> "In linea col budget"
                                    variance > 0 -> "+${euroFormat.format(variance)} Fuori Budget"
                                    variance < 0 -> "${euroFormat.format(variance)} Risparmio"
                                    else -> "In budget"
                                }
                                val varianceColor = if (variance > 0) RoseRed else EmeraldGreen

                                Text(
                                    text = varianceText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = varianceColor
                                )
                            }

                            if (property.contractorNotes.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Engineering,
                                            contentDescription = null,
                                            tint = TextMutedDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = property.contractorNotes,
                                            fontSize = 12.sp,
                                            color = TextSecondaryDark,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Interactive Action Quick Hub (ROI Simulator, Price Drop, PDF Dossier, Google Maps)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Strumenti & Simulazioni Deal",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. ROI Calculator Launch
                                Button(
                                    onClick = onCalculateRoiClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("property_detail_simulate_roi_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOnContainer),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Calculate, contentDescription = "Simula ROI", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Simula ROI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // 2. Price Drop Simulation
                                OutlinedButton(
                                    onClick = onSimulatePriceDropClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("property_detail_price_drop_btn"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                                    border = BorderStroke(1.dp, AmberGold),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = "Simula Ribasso", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Simula Ribasso", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 3. Renovation Simulator / Computo Metrico
                                OutlinedButton(
                                    onClick = { onOpenRenovationSimulator?.invoke() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("property_detail_renovation_calc_btn"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                                    border = BorderStroke(1.dp, EmeraldGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Handyman, contentDescription = "Computo Metrico", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Computo CapEx", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // 4. Bank PDF Dossier
                                OutlinedButton(
                                    onClick = {
                                        PropertyPdfGenerator.generateAndSharePdf(context, property, emailOnly = false)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("property_detail_pdf_dossier_btn"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                                    border = BorderStroke(1.dp, SurfaceCardBorder),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Esporta Dossier PDF", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Dossier PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 5. Open in Google Maps
                            val hasCoordinates = property.latitude != null && property.longitude != null
                            val mapsButtonLabel = when {
                                !hasCoordinates -> "Posizione non disponibile"
                                property.evidenceRef?.contains("approssimata") == true ->
                                    "Apri su Mappe (Posizione approssimata al comune)"
                                else -> "Apri Posizione su Google Maps"
                            }
                            OutlinedButton(
                                enabled = hasCoordinates,
                                onClick = {
                                    if (hasCoordinates) {
                                        try {
                                            val query = "geo:${property.latitude},${property.longitude}?q=${property.latitude},${property.longitude}(${Uri.encode(property.title)})"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(query))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Impossibile aprire l'app Mappe", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .testTag("property_detail_open_maps_btn"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = "Apri in Google Maps", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(mapsButtonLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 6. Due Diligence Checklist & Operational Notes
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                        border = BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Due Diligence & Note Operative",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )

                            // Document Checklist
                            val checkItems = listOf(
                                "Visura Catastale e Planimetria depositata",
                                "Provenienza / Titolo di acquisto o Rogito precedente",
                                "Certificato di Agibilità e Conformità Urbanistica",
                                "Attestato Prestazione Energetica (APE)",
                                "Preventivo Ristrutturazione Imprese Asseverato"
                            )

                            checkItems.forEachIndexed { idx, docName ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (idx < 3) EmeraldGreen else TextMutedDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = docName,
                                        fontSize = 12.sp,
                                        color = if (idx < 3) TextPrimaryDark else TextSecondaryDark
                                    )
                                }
                            }

                            HorizontalDivider(color = SurfaceCardBorder, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }

                // 7. Rich Text Investor Notes & Inspection Observations
                item {
                    RichTextNotesEditor(
                        initialNotes = property.notes,
                        onSaveNotes = { newNotes ->
                            onSaveNotes(newNotes)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
}
