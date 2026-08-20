package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.DistressedProperty
import com.example.data.Property
import com.example.data.PropertyDeal
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private fun String.escapeCsv(): String {
        val clean = this.replace("\"", "\"\"")
        return if (clean.contains(",") || clean.contains("\n") || clean.contains("\r") || clean.contains("\"")) {
            "\"$clean\""
        } else {
            clean
        }
    }

    /**
     * Generates a detailed CSV file from a list of DistressedProperty items including ARV, Renovation Cost, Profit and ROI metrics.
     */
    fun exportDistressedPropertiesToCsv(context: Context, distressedProperties: List<DistressedProperty>): Boolean {
        if (distressedProperties.isEmpty()) {
            Toast.makeText(context, "No distressed properties available to export.", Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALY)
            val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALY)
            val fileName = "distressed_deals_arv_roi_${fileDateFormat.format(Date())}.csv"

            val csvHeader = "ID,Address,Acquisition Price (€),As-Is Market Value (€),Estimated ARV (€),Est Renovation (€),Net Potential Profit (€),Projected ROI (%),Distress Level,Status,Latitude,Longitude,Notes,Last Updated\n"

            val csvRows = distressedProperties.joinToString("\n") { prop ->
                val price = prop.price
                val arv = prop.estimatedArv ?: if (prop.estimatedValue > price) prop.estimatedValue else (price * 1.35)
                val renoCost = price * 0.15
                val profit = arv - price - renoCost
                val roi = if (price + renoCost > 0) (profit / (price + renoCost)) * 100.0 else 0.0

                listOf(
                    prop.id.toString(),
                    prop.address.escapeCsv(),
                    String.format(Locale.US, "%.2f", price),
                    String.format(Locale.US, "%.2f", prop.estimatedValue),
                    String.format(Locale.US, "%.2f", arv),
                    String.format(Locale.US, "%.2f", renoCost),
                    String.format(Locale.US, "%.2f", profit),
                    String.format(Locale.US, "%.1f", roi),
                    prop.distressLevel.escapeCsv(),
                    prop.status.escapeCsv(),
                    prop.latitude.toString(),
                    prop.longitude.toString(),
                    prop.notes.escapeCsv(),
                    dateFormat.format(Date(prop.lastUpdated)).escapeCsv()
                ).joinToString(",")
            }

            val csvContent = csvHeader + csvRows

            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val csvFile = File(exportDir, fileName)
            FileOutputStream(csvFile).use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                csvFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Property Deals Export (ARV & ROI) - $fileName")
                putExtra(Intent.EXTRA_TEXT, "Attached CSV export containing ${distressedProperties.size} property deal details including ARV and ROI metrics.")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Export Deals CSV...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Toast.makeText(context, "CSV exported successfully: $fileName", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error exporting CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Generates a CSV file from a list of Property items and opens the system Share sheet.
     */
    fun exportPropertiesToCsv(context: Context, properties: List<Property>): Boolean {
        if (properties.isEmpty()) {
            Toast.makeText(context, "Nessuna proprietà da esportare.", Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALY)
            val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALY)
            val fileName = "immobili_export_${fileDateFormat.format(Date())}.csv"

            val csvHeader = "ID,Titolo,Indirizzo,Prezzo (€),Valore Stimato (€),Superficie (mq),Stato Distress,Tipologia,Strategia,Note,Foto URI,Data Creazione\n"
            
            val csvRows = properties.joinToString("\n") { prop ->
                listOf(
                    prop.id.toString(),
                    prop.title.escapeCsv(),
                    prop.address.escapeCsv(),
                    prop.price.toString(),
                    prop.estimatedMarketValue.toString(),
                    prop.surfaceSqm.toString(),
                    prop.distressStatus.escapeCsv(),
                    prop.propertyType.escapeCsv(),
                    prop.strategyTags.escapeCsv(),
                    prop.notes.escapeCsv(),
                    (prop.photoUri ?: "").escapeCsv(),
                    dateFormat.format(Date(prop.createdAt)).escapeCsv()
                ).joinToString(",")
            }

            val csvContent = csvHeader + csvRows

            // Save to app cache directory
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val csvFile = File(exportDir, fileName)
            FileOutputStream(csvFile).use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            // Get FileProvider Uri
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                csvFile
            )

            // Share Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Export Immobili - $fileName")
                putExtra(Intent.EXTRA_TEXT, "In allegato il file CSV con ${properties.size} proprietà esportate dal sistema.")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Esporta CSV con...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Toast.makeText(context, "File CSV generato: $fileName", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Errore durante l'esportazione CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Generates a CSV file from a list of PropertyDeal items.
     */
    fun exportDealsToCsv(context: Context, deals: List<PropertyDeal>): Boolean {
        if (deals.isEmpty()) {
            Toast.makeText(context, "Nessun deal da esportare.", Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALY)
            val fileName = "deals_radar_export_${fileDateFormat.format(Date())}.csv"

            val csvHeader = "ID,Titolo,Località,Fonte,Prezzo Richiesto (€),Valore Stimato (€),Sconto (%),Superficie (mq),Stato,Tipologia,URL Dettaglio\n"

            val csvRows = deals.joinToString("\n") { deal ->
                listOf(
                    deal.id.toString(),
                    deal.title.escapeCsv(),
                    deal.location.escapeCsv(),
                    deal.sourceName.escapeCsv(),
                    deal.askingPrice.toString(),
                    deal.estimatedMarketValue.toString(),
                    deal.discountPercent.toString(),
                    deal.surfaceSqm.toString(),
                    deal.status.escapeCsv(),
                    deal.propertyType.escapeCsv(),
                    deal.sourceUrl.escapeCsv()
                ).joinToString(",")
            }

            val csvContent = csvHeader + csvRows

            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val csvFile = File(exportDir, fileName)
            FileOutputStream(csvFile).use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                csvFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Export Deals Radar - $fileName")
                putExtra(Intent.EXTRA_TEXT, "In allegato il file CSV con ${deals.size} affari esportati.")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Esporta CSV con...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Toast.makeText(context, "File CSV generato: $fileName", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Errore durante l'esportazione CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            false
        }
    }
}
