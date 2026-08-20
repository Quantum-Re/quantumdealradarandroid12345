package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.Property
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import com.example.util.CsvExporter
import java.text.NumberFormat
import java.util.Locale

import androidx.compose.material.icons.filled.AutoAwesome

@Composable
fun AnalyticsScreen(
    deals: List<PropertyDeal>,
    properties: List<Property> = emptyList(),
    onOpenMarketTrendDrawer: (String) -> Unit = {}
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
        maximumFractionDigits = 0
    }

    val context = LocalContext.current

    val totalAskingPrice = deals.sumOf { it.askingPrice } + properties.sumOf { it.price }
    val totalMarketValue = deals.sumOf { it.estimatedMarketValue } + properties.sumOf { it.estimatedMarketValue }
    val totalPotentialEquity = totalMarketValue - totalAskingPrice

    val sourceGroup = deals.groupBy { it.sourceName }
    val typeGroup = (deals.map { it.propertyType } + properties.map { it.propertyType }).groupBy { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header with Export CSV Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PurpleIndigo, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Analytics Radar & Trend Prezzi",
                        color = TextPrimaryDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Statistiche e valore portafoglio",
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    if (properties.isNotEmpty()) {
                        CsvExporter.exportPropertiesToCsv(context, properties)
                    } else if (deals.isNotEmpty()) {
                        CsvExporter.exportDealsToCsv(context, deals)
                    } else {
                        CsvExporter.exportPropertiesToCsv(context, emptyList())
                    }
                },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("analytics_export_csv_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = "Esporta CSV", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Esporta CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Gemini Market Trend AI Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BentoPurpleHeader)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CyanAccent, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Report Trend Regionale (Gemini AI)", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Analisi prezzi al mq e previsione domanda per la tua regione", color = TextSecondaryDark, fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = { onOpenMarketTrendDrawer("Milano") },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("open_analytics_market_drawer_button")
                ) {
                    Text("Genera AI", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Section 1: Property Price Trend Chart Summary
        PropertyTrendSummaryScreen(
            properties = properties,
            deals = deals,
            modifier = Modifier.padding(0.dp)
        )

        // Hero Card: Equity Surplus / Savings Potential
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = BentoPurpleContainer.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Delta Valore Mercato vs Prezzo Iniziale",
                    color = BentoPurpleOnContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "+ ${currencyFormat.format(totalPotentialEquity)}",
                    color = EmeraldGreen,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Prezzo Asta / Richiesta", color = TextSecondaryDark, fontSize = 11.sp)
                        Text(currencyFormat.format(totalAskingPrice), color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Stima Perizia Totale", color = TextSecondaryDark, fontSize = 11.sp)
                        Text(currencyFormat.format(totalMarketValue), color = CyanAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Distribution by Source Portal
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PieChart, contentDescription = null, tint = CyanAccent)
                    Text("Distribuzione Opportunità per Portale", color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = SurfaceCardBorder)

                sourceGroup.forEach { (sourceName, list) ->
                    val percent = if (deals.isNotEmpty()) (list.size.toFloat() / deals.size) else 0f

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(sourceName, color = TextPrimaryDark, fontSize = 13.sp)
                            Text("${list.size} Deal (${(percent * 100).toInt()}%)", color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        LinearProgressIndicator(
                            progress = { percent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CyanAccent,
                            trackColor = BentoPurpleHeader
                        )
                    }
                }
            }
        }

        // Section 3: Property Type Breakdown
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = AmberGold)
                    Text("Tipologia Asset Immobiliari", color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = SurfaceCardBorder)

                typeGroup.forEach { (type, list) ->
                    val percent = if (deals.isNotEmpty()) (list.size.toFloat() / deals.size) else 0f

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(type, color = TextPrimaryDark, fontSize = 13.sp)
                            Text("${list.size} Immobili", color = AmberGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        LinearProgressIndicator(
                            progress = { percent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AmberGold,
                            trackColor = BentoPurpleHeader
                        )
                    }
                }
            }
        }
    }
}
