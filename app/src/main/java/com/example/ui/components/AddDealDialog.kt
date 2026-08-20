package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AddDealDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        sourceKey: String,
        sourceName: String,
        location: String,
        propertyType: String,
        askingPrice: Double,
        marketValue: Double,
        sqm: Int,
        auctionDate: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var sourceName by remember { mutableStateOf("Quimmo") }
    var location by remember { mutableStateOf("Milano (MI)") }
    var askingPriceStr by remember { mutableStateOf("180000") }
    var marketValueStr by remember { mutableStateOf("290000") }
    var sqmStr by remember { mutableStateOf("95") }
    var propertyType by remember { mutableStateOf("Residenziale") }
    var auctionDate by remember { mutableStateOf("15/10/2026") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCardDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = null, tint = CyanAccent)
                Text("Aggiungi Immobili in Radar", color = TextPrimaryDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo Annuncio / Immobile", color = TextSecondaryDark) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_deal_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceCardBorder
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sourceName,
                        onValueChange = { sourceName = it },
                        label = { Text("Portale / Fonte", color = TextSecondaryDark) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("add_deal_source"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder
                        )
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Città / Ubicazione", color = TextSecondaryDark) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("add_deal_location"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = askingPriceStr,
                        onValueChange = { askingPriceStr = it },
                        label = { Text("Offerta Base (€)", color = TextSecondaryDark) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("add_deal_asking_price"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder
                        )
                    )
                    OutlinedTextField(
                        value = marketValueStr,
                        onValueChange = { marketValueStr = it },
                        label = { Text("Stima Mercato (€)", color = TextSecondaryDark) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("add_deal_market_val"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sqmStr,
                        onValueChange = { sqmStr = it },
                        label = { Text("Superficie (m²)", color = TextSecondaryDark) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("add_deal_sqm"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder
                        )
                    )
                    OutlinedTextField(
                        value = auctionDate,
                        onValueChange = { auctionDate = it },
                        label = { Text("Data Asta / Scadenza", color = TextSecondaryDark) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("add_deal_auction_date"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title,
                        sourceName.lowercase().replace(" ", "_"),
                        sourceName,
                        location,
                        propertyType,
                        askingPriceStr.toDoubleOrNull() ?: 180000.0,
                        marketValueStr.toDoubleOrNull() ?: 290000.0,
                        sqmStr.toIntOrNull() ?: 90,
                        auctionDate.ifBlank { null }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("confirm_add_deal_btn")
            ) {
                Text("Salva in Radar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = TextSecondaryDark)
            }
        }
    )
}
