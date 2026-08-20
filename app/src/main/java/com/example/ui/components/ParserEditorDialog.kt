package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScraperSource
import com.example.ui.theme.*

@Composable
fun ParserEditorDialog(
    source: ScraperSource,
    onDismiss: () -> Unit,
    onSaveRules: (sourceId: String, rulesJson: String) -> Unit
) {
    var rulesJsonText by remember { mutableStateOf(source.activeParserRulesJson) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCardDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Code, contentDescription = null, tint = CyanAccent)
                Text("Selettori Scraper: ${source.name}", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Configura i selettori CSS/XPath per l'estrazione automatica di prezzi e titoli:",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = rulesJsonText,
                    onValueChange = { rulesJsonText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("dialog_rules_json_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF818CF8)
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSlateBg,
                        unfocusedContainerColor = DarkSlateBg,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceCardBorder
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveRules(source.id, rulesJsonText) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                modifier = Modifier.testTag("save_rules_btn")
            ) {
                Text("Salva Regole", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = TextSecondaryDark)
            }
        }
    )
}
