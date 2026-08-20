package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * An overlay container that blurs / obscures underlying detailed financial data and KPI charts
 * for non-premium / non-subscribed users, displaying a high-converting upgrade prompt.
 */
@Composable
fun LockedPremiumOverlay(
    isLocked: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Analisi ROI & KPI di Mercato Riservati",
    subtitle: String = "Accedi a valutazioni asseverate, margini di rivendita e rendite nette",
    unlockedItems: List<String> = listOf(
        "📊 Benchmark OMI Micro-Zona & Prezzo al m²",
        "📈 ROI Fix & Flip & Profitto Netto da Ristrutturazione",
        "🔑 Gross Yield, Cap Rate & Stima Canone di Locazione",
        "⚡ Stress Test Fiscale (Cedolare Secca vs Ordinaria)"
    ),
    ctaText: String = "SBLOCCA DATI PRO & ROI COMPLETO",
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .testTag("locked_premium_container")
    ) {
        // Underlying content (blurred & obscured when locked)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isLocked) {
                        Modifier
                            .blur(radius = 10.dp)
                            .alpha(0.35f)
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }

        // Locked UI Overlay Layer
        if (isLocked) {
            // Invisible interceptor to capture all clicks on the container
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DarkSlateBg.copy(alpha = 0.70f),
                                DarkSlateBg.copy(alpha = 0.92f),
                                DarkSlateBg.copy(alpha = 0.98f)
                            )
                        )
                    )
                    .border(
                        BorderStroke(
                            1.2.dp,
                            Brush.linearGradient(
                                listOf(
                                    AmberGold.copy(alpha = 0.7f),
                                    PurpleIndigo.copy(alpha = 0.6f),
                                    CyanAccent.copy(alpha = 0.5f)
                                )
                            )
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onUpgradeClick
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Glowing Padlock Icon Badge
                    Surface(
                        shape = CircleShape,
                        color = AmberGold.copy(alpha = 0.2f),
                        border = BorderStroke(1.5.dp, AmberGold),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Contenuto Riservato",
                                tint = AmberGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Title & Subtitle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                color = TextPrimaryDark,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AmberGold
                            ) {
                                Text(
                                    text = "PRO",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = subtitle,
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    // Features checklist
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceCardDark.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, SurfaceCardBorder.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth(0.96f)
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            unlockedItems.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = item,
                                        color = TextPrimaryDark,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Main Action CTA Button
                    Button(
                        onClick = onUpgradeClick,
                        modifier = Modifier
                            .fillMaxWidth(0.96f)
                            .height(44.dp)
                            .testTag("overlay_unlock_kpi_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = ctaText,
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }

                    Text(
                        text = "Disponibile con Token di Benvenuto o Abbonamento PRO",
                        color = TextMutedDark,
                        fontSize = 9.5.sp
                    )
                }
            }
        }
    }
}
