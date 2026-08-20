package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RichTextNotesEditor(
    initialNotes: String,
    onSaveNotes: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var textFieldValue by remember(initialNotes) {
        mutableStateOf(TextFieldValue(initialNotes))
    }
    var isPreviewMode by remember { mutableStateOf(false) }
    var lastSavedTime by remember { mutableStateOf<String?>(null) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    // Auto-save on note changes
    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text != initialNotes) {
            hasUnsavedChanges = true
            onSaveNotes(textFieldValue.text)
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            lastSavedTime = timeFormat.format(Date())
            hasUnsavedChanges = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rich_text_notes_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
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
                        Icons.AutoMirrored.Filled.StickyNote2,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Note & Appunti Due Diligence",
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // DB Saved Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, EmeraldGreen.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (lastSavedTime != null) "Room DB ($lastSavedTime)" else "Room DB Ok",
                            color = EmeraldGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Mode Selector & Preview Toggle Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented control Edit vs Preview
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCardDark)
                        .padding(2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (!isPreviewMode) CyanAccent.copy(alpha = 0.2f) else Color.Transparent,
                        modifier = Modifier
                            .clickable { isPreviewMode = false }
                            .testTag("rich_note_edit_tab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (!isPreviewMode) CyanAccent else TextMutedDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Editor Rich Text",
                                fontSize = 11.sp,
                                fontWeight = if (!isPreviewMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isPreviewMode) CyanAccent else TextMutedDark
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPreviewMode) CyanAccent.copy(alpha = 0.2f) else Color.Transparent,
                        modifier = Modifier
                            .clickable { isPreviewMode = true }
                            .testTag("rich_note_preview_tab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (isPreviewMode) CyanAccent else TextMutedDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Anteprima",
                                fontSize = 11.sp,
                                fontWeight = if (isPreviewMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (isPreviewMode) CyanAccent else TextMutedDark
                            )
                        }
                    }
                }

                // Explicit Save Button
                Button(
                    onClick = {
                        onSaveNotes(textFieldValue.text)
                        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        lastSavedTime = timeFormat.format(Date())
                        hasUnsavedChanges = false
                        Toast.makeText(context, "Note salvate nel Database Room!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("rich_note_save_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Salva note", tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salva", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (!isPreviewMode) {
                // Formatting Toolbar & Quick Tag Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Action Buttons Row (Bold, Italic, Bullet, Checklist, Highlight, Clear)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCardDark)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            // Bold
                            IconButton(
                                onClick = { textFieldValue = applyFormatting(textFieldValue, "**") },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("rich_note_bold_btn")
                            ) {
                                Text("B", color = TextPrimaryDark, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            }

                            // Italic
                            IconButton(
                                onClick = { textFieldValue = applyFormatting(textFieldValue, "*") },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("rich_note_italic_btn")
                            ) {
                                Text("I", color = TextPrimaryDark, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            // Bullet
                            IconButton(
                                onClick = { textFieldValue = toggleLinePrefix(textFieldValue, "• ") },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("rich_note_bullet_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Elenco puntato", tint = CyanAccent, modifier = Modifier.size(18.dp))
                            }

                            // Checklist
                            IconButton(
                                onClick = { textFieldValue = toggleLinePrefix(textFieldValue, "[ ] ") },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("rich_note_checklist_btn")
                            ) {
                                Icon(Icons.Default.CheckBox, contentDescription = "Casella di controllo", tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                            }

                            // Highlight
                            IconButton(
                                onClick = { textFieldValue = applyFormatting(textFieldValue, "==") },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("rich_note_highlight_btn")
                            ) {
                                Icon(Icons.Default.Highlight, contentDescription = "Evidenzia testo", tint = AmberGold, modifier = Modifier.size(18.dp))
                            }

                            // Heading
                            IconButton(
                                onClick = { textFieldValue = toggleLinePrefix(textFieldValue, "## ") },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("rich_note_heading_btn")
                            ) {
                                Text("H2", color = PurpleIndigo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Clear note
                        if (textFieldValue.text.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    textFieldValue = TextFieldValue("")
                                    onSaveNotes("")
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Cancella tutto il testo", tint = RoseRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Quick Tag Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Text("Tag rapidi:", color = TextMutedDark, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }

                        val tags = listOf(
                            "#Ispezione",
                            "#Sopralluogo",
                            "#Trattativa",
                            "#Offerta",
                            "#Ristrutturazione",
                            "#SpeseCondominiali",
                            "#Inquilino",
                            "#PeriziaCTU",
                            "#Rischi",
                            "#ExitStrategy"
                        )

                        items(tags) { tag ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceCardDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clickable {
                                        textFieldValue = insertTag(textFieldValue, tag)
                                    }
                                    .testTag("rich_note_tag_${tag.removePrefix("#")}")
                            ) {
                                Text(
                                    text = tag,
                                    color = CyanAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Quick Template Snippets Row (Horizontal scrollable)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Text("Template:", color = TextMutedDark, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }

                        item {
                            AssistChip(
                                onClick = {
                                    val template = "## 🔍 Sopralluogo & Ispezione Immobile\n• [ ] Impianto elettrico a norma / certificato\n• [ ] Impianto idraulico e scarichi funzionanti\n• [ ] Stato infissi, doppi vetri e coibentazione\n• [ ] Presenza tracce di umidità o muffe\n• [ ] Tetto, facciata e parti comuni stabili\n• [ ] Luminosità, esposizione e silenziosità"
                                    textFieldValue = insertTemplate(textFieldValue, template)
                                },
                                label = { Text("🔍 Ispezione & Sopralluogo", fontSize = 10.sp) },
                                modifier = Modifier.testTag("template_inspection_btn"),
                                colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceCardDark, labelColor = TextPrimaryDark)
                            )
                        }

                        item {
                            AssistChip(
                                onClick = {
                                    val template = "## 💼 Punti Trattativa & Negoziazione\n• **Prezzo Richiesto**: €\n• **Offerta Massima Target**: €\n• **Margine Sconto Obiettivo**: -%\n• **Motivazione Venditore**: Urgenza di realizzo / Nessuna fretta\n• **Clausola Sospensiva Mutuo**: Si / No\n• **Caparra Confirmatoria al Preliminare**: €\n• **Data Limite Accettazione**: "
                                    textFieldValue = insertTemplate(textFieldValue, template)
                                },
                                label = { Text("💼 Punti Trattativa", fontSize = 10.sp) },
                                modifier = Modifier.testTag("template_negotiation_btn"),
                                colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceCardDark, labelColor = TextPrimaryDark)
                            )
                        }

                        item {
                            AssistChip(
                                onClick = {
                                    val template = "## 🛠️ Stima Opere Ristrutturazione\n• [ ] Demolizioni & Nuovi Tramezzi: €\n• [ ] Rifacimento Bagno & Cucina: €\n• [ ] Pavimenti & Rivestimenti: €\n• [ ] Impianto Elettrico + Domotica: €\n• [ ] Infissi & Porta Blindata: €\n• ==Budget Ristrutturazione Stimato==: €"
                                    textFieldValue = insertTemplate(textFieldValue, template)
                                },
                                label = { Text("🛠️ Lavori Ristrutturazione", fontSize = 10.sp) },
                                modifier = Modifier.testTag("template_renovation_btn"),
                                colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceCardDark, labelColor = TextPrimaryDark)
                            )
                        }

                        item {
                            AssistChip(
                                onClick = {
                                    val template = "## 📝 Osservazioni Generali Deal\n• **Punti di Forza**: Zona ben servita, ottima rendita locativa stimata\n• **Criticità**: Spese condominiali straordinarie in delibera\n• **Strategia Exit**: Buy & Hold (Affitto canone concordato) o Fast Flip"
                                    textFieldValue = insertTemplate(textFieldValue, template)
                                },
                                label = { Text("📝 Osservazioni Generali", fontSize = 10.sp) },
                                modifier = Modifier.testTag("template_observations_btn"),
                                colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceCardDark, labelColor = TextPrimaryDark)
                            )
                        }
                    }

                    // Rich Text Input Field
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                        },
                        placeholder = {
                            Text(
                                "Scrivi osservazioni, rilievi del sopralluogo o punti di trattativa...\nEsempi:\n• **Grassetto** per dati cruciali\n• ==Evidenzia== con due '=='\n• [ ] per creare elementi checklist\n• #Tag per categorizzare",
                                color = TextMutedDark,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 250.dp)
                            .testTag("rich_note_input")
                            .testTag("property_detail_notes_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSlateBg,
                            unfocusedContainerColor = DarkSlateBg,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            } else {
                // Preview Mode Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSlateBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        if (textFieldValue.text.isBlank()) {
                            Text(
                                "Nessuna nota o appunto inserito.",
                                color = TextMutedDark,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic
                            )
                        } else {
                            Text(
                                text = parseRichTextToAnnotatedString(textFieldValue.text),
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Formatting Helper Functions

private fun applyFormatting(
    currentValue: TextFieldValue,
    prefix: String,
    suffix: String = prefix
): TextFieldValue {
    val text = currentValue.text
    val selection = currentValue.selection

    return if (selection.collapsed) {
        val newText = text.substring(0, selection.start) + prefix + suffix + text.substring(selection.start)
        val newCursor = selection.start + prefix.length
        TextFieldValue(text = newText, selection = TextRange(newCursor))
    } else {
        val start = selection.min
        val end = selection.max
        val selectedText = text.substring(start, end)
        val replacement = "$prefix$selectedText$suffix"
        val newText = text.substring(0, start) + replacement + text.substring(end)
        val newCursor = start + replacement.length
        TextFieldValue(text = newText, selection = TextRange(newCursor))
    }
}

private fun toggleLinePrefix(currentValue: TextFieldValue, prefix: String): TextFieldValue {
    val text = currentValue.text
    val selection = currentValue.selection
    val cursor = selection.start

    val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
    val lineText = text.substring(lineStart)

    return if (lineText.startsWith(prefix)) {
        val newText = text.substring(0, lineStart) + lineText.substring(prefix.length)
        TextFieldValue(text = newText, selection = TextRange((cursor - prefix.length).coerceAtLeast(lineStart)))
    } else {
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        TextFieldValue(text = newText, selection = TextRange(cursor + prefix.length))
    }
}

private fun insertTag(currentValue: TextFieldValue, tag: String): TextFieldValue {
    val text = currentValue.text
    val selection = currentValue.selection
    val cursor = selection.start
    val tagWithSpace = if (cursor > 0 && !text[cursor - 1].isWhitespace()) " $tag " else "$tag "
    val newText = text.substring(0, cursor) + tagWithSpace + text.substring(cursor)
    val newCursor = cursor + tagWithSpace.length
    return TextFieldValue(text = newText, selection = TextRange(newCursor))
}

private fun insertTemplate(currentValue: TextFieldValue, templateText: String): TextFieldValue {
    val text = currentValue.text
    val prefixNewline = if (text.isNotBlank() && !text.endsWith("\n")) "\n\n" else ""
    val newText = text + prefixNewline + templateText
    return TextFieldValue(text = newText, selection = TextRange(newText.length))
}

private fun parseRichTextToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { lineIndex, line ->
            var currentLine = line
            if (currentLine.startsWith("[x]") || currentLine.startsWith("[X]")) {
                withStyle(SpanStyle(color = EmeraldGreen, fontWeight = FontWeight.Bold)) {
                    append("☑ ")
                }
                currentLine = currentLine.substring(3).trimStart()
            } else if (currentLine.startsWith("[ ]")) {
                withStyle(SpanStyle(color = TextMutedDark, fontWeight = FontWeight.Bold)) {
                    append("☐ ")
                }
                currentLine = currentLine.substring(3).trimStart()
            } else if (currentLine.startsWith("• ") || currentLine.startsWith("- ")) {
                withStyle(SpanStyle(color = CyanAccent, fontWeight = FontWeight.Bold)) {
                    append("• ")
                }
                currentLine = currentLine.substring(2).trimStart()
            }

            parseInlineFormatting(currentLine)

            if (lineIndex < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.parseInlineFormatting(line: String) {
    val regex = Regex("""(\*\*.*?\*\*|\*.*?\*|==.*?==|#[A-Za-z0-9_]+)""")
    val matches = regex.findAll(line)

    var lastIndex = 0
    for (match in matches) {
        if (match.range.first > lastIndex) {
            append(line.substring(lastIndex, match.range.first))
        }

        val value = match.value
        when {
            value.startsWith("**") && value.endsWith("**") && value.length >= 4 -> {
                val inner = value.substring(2, value.length - 2)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimaryDark)) {
                    append(inner)
                }
            }
            value.startsWith("*") && value.endsWith("*") && value.length >= 2 -> {
                val inner = value.substring(1, value.length - 1)
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = TextPrimaryDark)) {
                    append(inner)
                }
            }
            value.startsWith("==") && value.endsWith("==") && value.length >= 4 -> {
                val inner = value.substring(2, value.length - 2)
                withStyle(SpanStyle(background = AmberGold.copy(alpha = 0.35f), color = AmberGold, fontWeight = FontWeight.SemiBold)) {
                    append(inner)
                }
            }
            value.startsWith("#") -> {
                withStyle(SpanStyle(color = CyanAccent, fontWeight = FontWeight.Bold)) {
                    append(value)
                }
            }
            else -> {
                append(value)
            }
        }
        lastIndex = match.range.last + 1
    }

    if (lastIndex < line.length) {
        append(line.substring(lastIndex))
    }
}
