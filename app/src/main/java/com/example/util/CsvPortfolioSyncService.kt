package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.DataProvenance
import com.example.data.PipelineStatus
import com.example.data.Property
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import java.util.Locale

enum class CsvSyncMode(val labelIt: String, val descriptionIt: String) {
    MERGE_UPDATE(
        labelIt = "Unisci e Aggiorna",
        descriptionIt = "Aggiorna gli immobili esistenti corrispondenti per indirizzo/titolo e aggiunge i nuovi record."
    ),
    REPLACE_ALL(
        labelIt = "Sostituisci Portafoglio",
        descriptionIt = "Rimuove tutti gli immobili attuali e importa unicamente il contenuto del file CSV."
    ),
    APPEND_ONLY(
        labelIt = "Aggiungi come Nuovi",
        descriptionIt = "Crea nuovi immobili per ogni riga valida senza modificare i dati preesistenti."
    )
}

data class CsvSyncSummary(
    val totalRowsParsed: Int = 0,
    val validProperties: List<Property> = emptyList(),
    val insertedCount: Int = 0,
    val updatedCount: Int = 0,
    val errors: List<String> = emptyList(),
    val detectedDelimiter: String = ",",
    val matchedColumns: Map<String, String> = emptyMap(),
    val unrecognizedColumns: List<String> = emptyList()
)

object CsvPortfolioSyncService {
    private const val TAG = "CsvPortfolioSyncService"

    /**
     * Known Italian cities and approximate coordinates for automatic fallback geocoding
     */
    private val CITY_COORDINATES = mapOf(
        "milano" to Pair(45.4642, 9.1900),
        "milan" to Pair(45.4642, 9.1900),
        "roma" to Pair(41.9028, 12.4964),
        "rome" to Pair(41.9028, 12.4964),
        "torino" to Pair(45.0703, 7.6869),
        "turin" to Pair(45.0703, 7.6869),
        "napoli" to Pair(40.8518, 14.2681),
        "naples" to Pair(40.8518, 14.2681),
        "bologna" to Pair(44.4949, 11.3426),
        "firenze" to Pair(43.7696, 11.2558),
        "florence" to Pair(43.7696, 11.2558),
        "genova" to Pair(44.4056, 8.9463),
        "palermo" to Pair(38.1157, 13.3615),
        "venezia" to Pair(45.4408, 12.3155),
        "venice" to Pair(45.4408, 12.3155),
        "verona" to Pair(45.4384, 10.9916),
        "padova" to Pair(45.4064, 11.8768),
        "brescia" to Pair(45.5416, 10.2118),
        "bergamo" to Pair(45.6983, 9.6773),
        "trieste" to Pair(45.6495, 13.7768),
        "bari" to Pair(41.1171, 16.8719),
        "catania" to Pair(37.5079, 15.0873),
        "monza" to Pair(45.5845, 9.2744),
        "reggio emilia" to Pair(44.6983, 10.6312),
        "modena" to Pair(44.6471, 10.9252),
        "parma" to Pair(44.8015, 10.3279),
        "rimini" to Pair(44.0678, 12.5695),
        "salerno" to Pair(40.6824, 14.7681),
        "perugia" to Pair(43.1107, 12.3908),
        "livorno" to Pair(43.5485, 10.3106),
        "cagliari" to Pair(39.2238, 9.1217)
    )

    /**
     * Parses raw CSV content and constructs clean Property entities.
     */
    fun parseCsvToProperties(rawCsv: String): CsvSyncSummary {
        val trimmed = rawCsv.trim()
        if (trimmed.isBlank()) {
            return CsvSyncSummary(errors = listOf("Il file o testo CSV fornito è vuoto."))
        }

        val delimiter = detectDelimiter(trimmed)
        val rows = parseCsvIntoRows(trimmed, delimiter)
        if (rows.isEmpty()) {
            return CsvSyncSummary(errors = listOf("Nessuna riga valida individuata nel documento."))
        }

        val headerTokens = rows[0]
        if (headerTokens.isEmpty()) {
            return CsvSyncSummary(errors = listOf("Intestazione CSV mancante o non valida."))
        }

        val columnMapping = mutableMapOf<String, Int>()
        val matchedColumnsDisplay = mutableMapOf<String, String>()
        val unrecognizedHeaders = mutableListOf<String>()

        headerTokens.forEachIndexed { index, header ->
            val normalizedHeader = normalizeString(header)
            val matchedField = matchFieldToHeader(normalizedHeader)
            if (matchedField != null) {
                columnMapping[matchedField] = index
                matchedColumnsDisplay[header.trim()] = matchedField
            } else {
                unrecognizedHeaders.add(header.trim())
            }
        }

        val validProperties = mutableListOf<Property>()
        val errors = mutableListOf<String>()

        for (rowIndex in 1 until rows.size) {
            val row = rows[rowIndex]
            if (row.all { it.isBlank() }) continue

            try {
                fun getVal(field: String): String {
                    val idx = columnMapping[field] ?: return ""
                    return if (idx < row.size) row[idx].trim() else ""
                }

                val address = getVal("address").ifBlank { getVal("title") }
                val title = getVal("title").ifBlank {
                    if (address.isNotBlank()) address else "Immobile #${rowIndex}"
                }

                if (address.isBlank() && title.isBlank()) {
                    errors.add("Riga $rowIndex ignorata: indirizzo o titolo assente.")
                    continue
                }

                val price = parseFlexibleNumber(getVal("price"))
                val estimatedValue = parseFlexibleNumber(getVal("estimatedMarketValue")).let {
                    if (it > 0) it else if (price > 0) price else 100000.0
                }
                val finalPrice = if (price > 0) price else estimatedValue

                val surfaceSqm = parseFlexibleNumber(getVal("surfaceSqm")).toInt().coerceAtLeast(0)
                val distressStatus = normalizeDistressStatus(getVal("distressStatus"))
                val propertyType = normalizePropertyType(getVal("propertyType"))
                val strategyTags = normalizeStrategy(getVal("strategyTags"))
                val pipelineStatus = normalizePipelineStatus(getVal("pipelineStatus"))

                val estReno = parseFlexibleNumber(getVal("estimatedRenovationCost"))
                val actReno = parseFlexibleNumber(getVal("actualRenovationCost"))
                val targetResale = parseFlexibleNumber(getVal("targetResalePrice")).let {
                    if (it > 0) it else if (estimatedValue > finalPrice) estimatedValue else (finalPrice * 1.30)
                }
                val actualSale = parseFlexibleNumber(getVal("actualSalePrice"))
                val rentalIncome = parseFlexibleNumber(getVal("projectedRentalIncome"))
                val escrowDate = getVal("escrowClosingDate")
                val progress = parseFlexibleNumber(getVal("renovationProgressPercent")).toInt().coerceIn(0, 100)
                val contractorNotes = getVal("contractorNotes")
                val notes = getVal("notes")
                val photoUri = getVal("photoUri").ifBlank { null }

                var lat = parseFlexibleNumber(getVal("latitude"))
                var lng = parseFlexibleNumber(getVal("longitude"))
                var approxLocationRef: String? = null

                if (lat == 0.0 || lng == 0.0) {
                    val fallback = findCityCoordinates("$title $address $notes")
                    if (fallback != null) {
                        lat = fallback.first
                        lng = fallback.second
                        approxLocationRef = "posizione approssimata al comune"
                    } else {
                        lat = 0.0
                        lng = 0.0
                    }
                }

                val now = System.currentTimeMillis()
                val property = Property(
                    id = 0L,
                    title = title,
                    address = address.ifBlank { title },
                    price = finalPrice,
                    estimatedMarketValue = estimatedValue,
                    surfaceSqm = surfaceSqm,
                    distressStatus = distressStatus,
                    propertyType = propertyType,
                    strategyTags = strategyTags,
                    pipelineStatus = pipelineStatus.key,
                    estimatedRenovationCost = estReno,
                    actualRenovationCost = actReno,
                    targetResalePrice = targetResale,
                    actualSalePrice = actualSale,
                    projectedRentalIncome = rentalIncome,
                    escrowClosingDate = escrowDate,
                    renovationProgressPercent = progress,
                    contractorNotes = contractorNotes,
                    notes = notes,
                    photoUri = photoUri,
                    latitude = lat,
                    longitude = lng,
                    provenance = DataProvenance.USER_ENTERED.name,
                    retrievedAt = now,
                    evidenceRef = approxLocationRef,
                    createdAt = now - ((rows.size - rowIndex) * 60000L)
                )

                validProperties.add(property)
            } catch (e: Exception) {
                errors.add("Errore alla riga $rowIndex: ${e.localizedMessage ?: e.message}")
            }
        }

        return CsvSyncSummary(
            totalRowsParsed = rows.size - 1,
            validProperties = validProperties,
            errors = errors,
            detectedDelimiter = delimiter.toString(),
            matchedColumns = matchedColumnsDisplay,
            unrecognizedColumns = unrecognizedHeaders
        )
    }

    /**
     * Delimiter auto-detection: checks frequency of comma, semicolon, or tab.
     */
    private fun detectDelimiter(csv: String): Char {
        val firstLine = csv.lines().firstOrNull { it.isNotBlank() } ?: return ','
        val commas = firstLine.count { it == ',' }
        val semicolons = firstLine.count { it == ';' }
        val tabs = firstLine.count { it == '\t' }

        return when {
            semicolons > commas && semicolons >= tabs -> ';'
            tabs > commas && tabs > semicolons -> '\t'
            else -> ','
        }
    }

    /**
     * RFC 4180 CSV Tokenizer supporting multiline quoted strings and escaped quotes.
     */
    private fun parseCsvIntoRows(csv: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var insideQuotes = false
        var i = 0

        while (i < csv.length) {
            val c = csv[i]

            if (insideQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length && csv[i + 1] == '"') {
                        currentField.append('"')
                        i++ // Skip escaped quote
                    } else {
                        insideQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        insideQuotes = true
                    }
                    delimiter -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                    }
                    '\r' -> {
                        // ignore carriage return
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                    else -> {
                        currentField.append(c)
                    }
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow.toList())
            }
        }

        return rows
    }

    /**
     * Normalizes text by removing accents, punctuation, and converting to lowercase.
     */
    private fun normalizeString(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]".toRegex(), "")
    }

    /**
     * Fuzzy matching of known real estate headers in Italian and English.
     */
    private fun matchFieldToHeader(normalized: String): String? {
        return when {
            // Title / Name
            normalized.contains("titolo") || normalized == "title" || normalized == "name" ||
                    normalized == "nome" || normalized.contains("immobile") || normalized == "property" ||
                    normalized == "denominazione" || normalized == "oggetto" -> "title"

            // Address / Location
            normalized.contains("indirizzo") || normalized.contains("address") || normalized.contains("via") ||
                    normalized.contains("location") || normalized.contains("ubicazione") || normalized == "citta" ||
                    normalized == "city" || normalized == "luogo" || normalized == "comune" -> "address"

            // Acquisition Price
            normalized.contains("prezzoacquisto") || normalized.contains("acquisitionprice") ||
                    normalized.contains("purchaseprice") || normalized.contains("costoacquisto") ||
                    normalized == "prezzo" || normalized == "price" || normalized == "cost" ||
                    normalized == "costo" || normalized == "acquisto" -> "price"

            // Estimated Market Value
            normalized.contains("valorestimato") || normalized.contains("estimatedmarketvalue") ||
                    normalized.contains("marketvalue") || normalized.contains("valoremercato") ||
                    normalized.contains("stima") || normalized.contains("asisvalue") ||
                    normalized.contains("valoreimmobile") -> "estimatedMarketValue"

            // Surface (Sqm)
            normalized.contains("superficie") || normalized.contains("surface") || normalized.contains("sqm") ||
                    normalized.contains("mq") || normalized.contains("metriquadri") || normalized.contains("size") ||
                    normalized.contains("dimensione") -> "surfaceSqm"

            // Distress Status
            normalized.contains("distress") || normalized.contains("statodistress") ||
                    normalized.contains("asta") || normalized.contains("procedura") ||
                    normalized.contains("tribunale") || normalized.contains("saldoestralcio") -> "distressStatus"

            // Property Type
            normalized.contains("tipologia") || normalized.contains("propertytype") ||
                    normalized.contains("categoria") || normalized == "tipo" || normalized == "type" ||
                    normalized == "destinazione" -> "propertyType"

            // Strategy Tags
            normalized.contains("strategia") || normalized.contains("strategy") ||
                    normalized.contains("tag") || normalized.contains("piano") ||
                    normalized.contains("businessmodel") || normalized.contains("modello") -> "strategyTags"

            // Pipeline Status
            normalized.contains("pipeline") || normalized.contains("fase") || normalized.contains("stage") ||
                    normalized.contains("statoprogetto") || normalized == "stato" || normalized == "status" -> "pipelineStatus"

            // Renovation Cost
            normalized.contains("ristrutturazione") || normalized.contains("renovation") ||
                    normalized.contains("lavori") || normalized.contains("preventivo") ||
                    normalized.contains("estrenovation") || normalized.contains("capex") -> "estimatedRenovationCost"

            // Actual Renovation Cost
            normalized.contains("actualrenovation") || normalized.contains("lavorieffettivi") ||
                    normalized.contains("spesaeffettiva") || normalized.contains("costocantiere") -> "actualRenovationCost"

            // Target Resale Price / ARV
            normalized.contains("targetresale") || normalized.contains("prezzovendita") ||
                    normalized.contains("targetuscita") || normalized.contains("rivendita") ||
                    normalized.contains("exitprice") || normalized.contains("arv") ||
                    normalized.contains("prezzorivendita") -> "targetResalePrice"

            // Actual Sale Price
            normalized.contains("actualsale") || normalized.contains("prezzorealizzato") ||
                    normalized.contains("vendutoa") || normalized.contains("incasso") -> "actualSalePrice"

            // Rental Income
            normalized.contains("affitto") || normalized.contains("rentalincome") ||
                    normalized.contains("rent") || normalized.contains("rendita") ||
                    normalized.contains("canone") || normalized.contains("locazione") -> "projectedRentalIncome"

            // Escrow Closing Date
            normalized.contains("rogito") || normalized.contains("closingdate") ||
                    normalized.contains("dataescrow") || normalized.contains("datarogito") ||
                    normalized.contains("scadenza") -> "escrowClosingDate"

            // Renovation Progress
            normalized.contains("avanzamento") || normalized.contains("progress") ||
                    normalized.contains("completamento") || normalized.contains("lavoripercentuale") -> "renovationProgressPercent"

            // Contractor Notes
            normalized.contains("cantiere") || normalized.contains("contractor") ||
                    normalized.contains("ditta") || normalized.contains("impresa") -> "contractorNotes"

            // Notes
            normalized.contains("note") || normalized.contains("notes") ||
                    normalized.contains("commenti") || normalized.contains("osservazioni") ||
                    normalized.contains("descrizione") || normalized.contains("dettagli") -> "notes"

            // Coordinates
            normalized.contains("lat") || normalized.contains("latitudine") -> "latitude"
            normalized.contains("lon") || normalized.contains("lng") || normalized.contains("longitudine") -> "longitude"

            // Photo
            normalized.contains("foto") || normalized.contains("photo") || normalized.contains("immagine") ||
                    normalized.contains("image") || normalized.contains("url") -> "photoUri"

            else -> null
        }
    }

    /**
     * Flexible number parser supporting EU format (120.000,50) and US format (120,000.50).
     */
    private fun parseFlexibleNumber(raw: String): Double {
        if (raw.isBlank()) return 0.0
        var clean = raw.trim()
            .replace("€", "")
            .replace("$", "")
            .replace("£", "")
            .replace("mq", "", ignoreCase = true)
            .replace("sqm", "", ignoreCase = true)
            .replace("%", "")
            .replace(" ", "")
            .trim()

        if (clean.isBlank()) return 0.0

        // Handle European format with '.' as thousands separator and ',' as decimal separator
        if (clean.contains(",") && clean.contains(".")) {
            val lastComma = clean.lastIndexOf(',')
            val lastDot = clean.lastIndexOf('.')
            clean = if (lastComma > lastDot) {
                clean.replace(".", "").replace(",", ".")
            } else {
                clean.replace(",", "")
            }
        } else if (clean.contains(",")) {
            clean = clean.replace(",", ".")
        }

        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun normalizePipelineStatus(raw: String): PipelineStatus {
        val norm = normalizeString(raw)
        return when {
            norm.contains("trattativa") || norm.contains("escrow") || norm.contains("offerta") || norm.contains("compromesso") -> PipelineStatus.IN_ESCROW
            norm.contains("ristrutturaz") || norm.contains("renovat") || norm.contains("cantiere") || norm.contains("lavori") -> PipelineStatus.RENOVATING
            norm.contains("vendita") || norm.contains("listed") || norm.contains("mercato") || norm.contains("annuncio") -> PipelineStatus.LISTED
            norm.contains("locat") || norm.contains("rent") || norm.contains("affitt") || norm.contains("reddito") -> PipelineStatus.RENTED
            norm.contains("venduto") || norm.contains("sold") || norm.contains("chiuso") || norm.contains("concluso") -> PipelineStatus.SOLD
            norm.contains("archiv") -> PipelineStatus.ARCHIVED
            else -> PipelineStatus.ANALYZED
        }
    }

    private fun normalizeDistressStatus(raw: String): String {
        val norm = normalizeString(raw)
        return when {
            norm.contains("asta") || norm.contains("tribunale") || norm.contains("fallimento") -> "ASTA"
            norm.contains("saldo") || norm.contains("stralcio") || norm.contains("npl") -> "SALDO E STRALCIO"
            norm.contains("ribasso") || norm.contains("sconto") || norm.contains("motivato") -> "FORTE RIBASSO"
            norm.contains("eredit") || norm.contains("successione") -> "SUCCESSIONE"
            norm.contains("normale") || norm.contains("nessun") || norm.contains("libero") -> "Normale"
            raw.isNotBlank() -> raw.trim()
            else -> "Normale"
        }
    }

    private fun normalizePropertyType(raw: String): String {
        val norm = normalizeString(raw)
        return when {
            norm.contains("residenz") || norm.contains("appartament") || norm.contains("trilocale") || norm.contains("bilocale") || norm.contains("attico") -> "Residenziale"
            norm.contains("commercia") || norm.contains("negozio") || norm.contains("locale") -> "Commerciale"
            norm.contains("vill") || norm.contains("indipendent") -> "Villa Indipendente"
            norm.contains("uffici") || norm.contains("studio") -> "Ufficio"
            norm.contains("frazion") -> "Frazionamento"
            raw.isNotBlank() -> raw.trim()
            else -> "Residenziale"
        }
    }

    private fun normalizeStrategy(raw: String): String {
        val norm = normalizeString(raw)
        return when {
            norm.contains("flip") || norm.contains("rivendita") || norm.contains("trading") -> "Fix & Flip"
            norm.contains("hold") || norm.contains("affitto") || norm.contains("rendita") || norm.contains("locazione") -> "Buy & Hold"
            norm.contains("frazion") || norm.contains("sviluppo") -> "Frazionamento"
            norm.contains("asta") -> "Asta Immobiliare"
            norm.contains("stralcio") -> "Saldo e Stralcio"
            raw.isNotBlank() -> raw.trim()
            else -> "Fix & Flip"
        }
    }

    private fun findCityCoordinates(text: String): Pair<Double, Double>? {
        val lower = text.lowercase(Locale.ROOT)
        for ((city, coords) in CITY_COORDINATES) {
            if (lower.contains(city)) {
                return coords
            }
        }
        return null
    }

    /**
     * Generates a sample CSV template with diverse realistic properties for easy portfolio migration.
     */
    fun generateSampleCsvTemplate(): String {
        return """
            Titolo,Indirizzo,Prezzo Acquisto (€),Valore Stimato (€),Superficie (mq),Stato Distress,Tipologia,Strategia,Stato Pipeline,Costo Ristrutturazione (€),Target Rivendita (€),Affitto Mensile Previsto (€),Note
            Trilocale Porta Venezia,Via Tadino 24 Milano,240000,340000,88,Normale,Residenziale,Fix & Flip,RENOVATING,45000,370000,0,Finiture parquet e doppi servizi. Consegna cantiere prevista fine mese.
            Bilocale Zona Universitaria,Via Zamboni 62 Bologna,135000,185000,55,Normale,Residenziale,Buy & Hold,RENTED,15000,195000,850,Completamente arredato e locato a 2 studentesse con cedolare secca.
            Attico Panoramico San Salvario,Via Nizza 40 Torino,160000,260000,110,ASTA,Residenziale,Fix & Flip,IN_ESCROW,50000,290000,0,Asta giudiziaria aggiudicata con ribasso del 38%. In attesa del decreto di trasferimento.
            Palazzina Frazionabile,Via Cavour 18 Firenze,380000,560000,210,SALDO E STRALCIO,Frazionamento,Frazionamento,ANALYZED,80000,640000,0,Studio di fattibilità per ricavare 3 bilocali indipendenti.
            Quadrilocale Eur Fermi,Viale America 110 Roma,310000,430000,125,Normale,Residenziale,Fix & Flip,LISTED,35000,450000,0,Ristrutturazione ultimata in classe A. Pubblicato sui portali immobiliari.
        """.trimIndent()
    }

    /**
     * Exports and shares the sample CSV template via Android Intent.
     */
    fun exportSampleTemplate(context: Context): Boolean {
        return try {
            val fileName = "quantum_deal_radar_portfolio_template.csv"
            val content = generateSampleCsvTemplate()

            val exportDir = File(context.cacheDir, "templates").apply { if (!exists()) mkdirs() }
            val file = File(exportDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Quantum Deal Radar - Modello CSV Portafoglio")
                putExtra(Intent.EXTRA_TEXT, "In allegato il modello CSV pre-formattato per importare e sincronizzare il tuo portafoglio immobiliare su Quantum Deal Radar.")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Condividi Modello CSV...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Toast.makeText(context, "Modello CSV generato: $fileName", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Errore esportazione modello: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
