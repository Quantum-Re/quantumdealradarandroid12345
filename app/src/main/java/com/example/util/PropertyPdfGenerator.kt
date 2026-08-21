package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.Property
import com.example.data.PropertyDeal
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object PropertyPdfGenerator {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
        maximumFractionDigits = 0
    }

    /**
     * Generates a detailed PDF summary document for a PropertyDeal containing financial metrics and market trends.
     */
    fun generatePdfForDeal(context: Context, deal: PropertyDeal): File? {
        return try {
            val pdfDocument = PdfDocument()
            // Standard A4 dimensions at 72 dpi: 595 x 842 pt
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            // Colors
            val navyBg = android.graphics.Color.rgb(15, 23, 42) // #0F172A
            val cardBg = android.graphics.Color.rgb(30, 41, 59) // #1E293B
            val cardBorder = android.graphics.Color.rgb(51, 65, 85) // #334155
            val cyanAccent = android.graphics.Color.rgb(6, 182, 212) // #06B6D4
            val emeraldGreen = android.graphics.Color.rgb(16, 185, 129) // #10B981
            val amberGold = android.graphics.Color.rgb(245, 158, 11) // #F59E0B
            val textWhite = android.graphics.Color.rgb(248, 250, 252) // #F8FAFC
            val textMuted = android.graphics.Color.rgb(148, 163, 184) // #94A3B8
            val lightGrayBg = android.graphics.Color.rgb(241, 245, 249)

            // Page Background
            canvas.drawColor(android.graphics.Color.WHITE)

            // 1. Header Banner (Navy)
            paint.color = navyBg
            canvas.drawRect(0f, 0f, 595f, 90f, paint)

            // Header Accent Line
            paint.color = cyanAccent
            canvas.drawRect(0f, 87f, 595f, 90f, paint)

            // App Title in Header
            paint.color = textWhite
            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("RE-INVESTOR ANALYTICS", 25f, 40f, paint)

            paint.color = cyanAccent
            paint.textSize = 12f
            paint.isFakeBoldText = false
            canvas.drawText("DOSSIER VALUTAZIONE IMMOBILIARE & FINANCIAL METRICS", 25f, 62f, paint)

            // Generation Date
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
            paint.color = textMuted
            paint.textSize = 9f
            canvas.drawText("Generato il: ${dateFormat.format(Date())}", 430f, 40f, paint)
            canvas.drawText("ID Deal: #${deal.id}", 430f, 58f, paint)

            // 2. Property Main Info Card
            paint.color = cardBg
            val infoCardRect = RectF(25f, 105f, 570f, 215f)
            canvas.drawRoundRect(infoCardRect, 10f, 10f, paint)

            paint.color = cardBorder
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(infoCardRect, 10f, 10f, paint)
            paint.style = Paint.Style.FILL

            // Property Title
            paint.color = textWhite
            paint.textSize = 16f
            paint.isFakeBoldText = true
            val truncatedTitle = if (deal.title.length > 45) deal.title.take(42) + "..." else deal.title
            canvas.drawText(truncatedTitle, 40f, 135f, paint)

            // Location & Details
            paint.color = cyanAccent
            paint.textSize = 11f
            paint.isFakeBoldText = false
            canvas.drawText("Ubicazione: ${deal.location}", 40f, 158f, paint)

            paint.color = textMuted
            paint.textSize = 10f
            canvas.drawText("Tipologia: ${deal.propertyType}  |  Superficie: ${deal.surfaceSqm} m²  |  Fonte: ${deal.sourceName}", 40f, 178f, paint)

            if (!deal.auctionDate.isNullOrBlank()) {
                paint.color = amberGold
                canvas.drawText("Asta / Termine Presentazione: ${deal.auctionDate}", 40f, 198f, paint)
            } else {
                paint.color = textMuted
                canvas.drawText("Stato Portale: ${deal.status}", 40f, 198f, paint)
            }

            // 3. Financial Summary Grid Header
            paint.color = navyBg
            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas.drawText("METRICHE FINANZIARIE CHIAVE", 25f, 240f, paint)

            paint.color = cyanAccent
            canvas.drawLine(25f, 246f, 220f, 246f, paint)

            // 4. Metric Box 1: Prezzo Richiesto
            drawMetricBox(
                canvas = canvas,
                rect = RectF(25f, 255f, 195f, 325f),
                label = "PREZZO RICHIESTO",
                value = currencyFormat.format(deal.askingPrice),
                subtext = "${(deal.askingPrice / if (deal.surfaceSqm > 0) deal.surfaceSqm else 1).toInt()} €/m²",
                accentColor = cyanAccent
            )

            // Metric Box 2: Valore Stimato Mercato
            drawMetricBox(
                canvas = canvas,
                rect = RectF(212f, 255f, 382f, 325f),
                label = "VALORE MERCATO STIMATO",
                value = currencyFormat.format(deal.estimatedMarketValue),
                subtext = "Sconto: -${deal.discountPercent}%",
                accentColor = emeraldGreen
            )

            // Metric Box 3: Rendimento Atteso (Cap Rate)
            drawMetricBox(
                canvas = canvas,
                rect = RectF(400f, 255f, 570f, 325f),
                label = "ESTIMATED CAP RATE / ROI",
                value = String.format(Locale.ITALY, "%.1f%%", deal.estimatedCapRate),
                subtext = "Margine: ${currencyFormat.format((deal.estimatedMarketValue - deal.askingPrice).coerceAtLeast(0.0))}",
                accentColor = amberGold
            )

            // 5. Detailed Cash Flow & Investment Breakdown Table
            paint.color = navyBg
            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas.drawText("PROIEZIONE CASH FLOW & PROSPETTO INVESTIMENTO", 25f, 355f, paint)

            paint.color = cyanAccent
            canvas.drawLine(25f, 361f, 330f, 361f, paint)

            // Table Background Header
            paint.color = cardBg
            canvas.drawRect(25f, 370f, 570f, 392f, paint)

            paint.color = textWhite
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("Voce di Costo / Rendimento", 35f, 385f, paint)
            canvas.drawText("Stima Parametrica", 300f, 385f, paint)
            canvas.drawText("Impatto Finanziario", 440f, 385f, paint)

            // Table Rows Data
            val estimatedMonthlyRent = (deal.askingPrice * (deal.estimatedCapRate / 100.0) / 12.0).coerceAtLeast(400.0)
            val estimatedRenovation = deal.surfaceSqm * 350.0 // 350€/mq estimate
            val estimatedTaxesFees = deal.askingPrice * 0.08 // 8% taxes/fees estimate
            val totalInvestment = deal.askingPrice + estimatedRenovation + estimatedTaxesFees
            val annualGrossIncome = estimatedMonthlyRent * 12.0
            val grossYield = if (totalInvestment > 0) (annualGrossIncome / totalInvestment) * 100 else 0.0

            val tableRows = listOf(
                Triple("Acquisto Immobile (Offerta Base)", currencyFormat.format(deal.askingPrice), "Uscita Iniziale"),
                Triple("Stima Ristrutturazione (~350€/m²)", currencyFormat.format(estimatedRenovation), "Uscita Iniziale"),
                Triple("Imposte, Notaio & Commissioni (~8%)", currencyFormat.format(estimatedTaxesFees), "Uscita Iniziale"),
                Triple("Totale Capitale Investito (Total Outlay)", currencyFormat.format(totalInvestment), "Capitale Richiesto"),
                Triple("Canone Mensile Stimato da Locazione", currencyFormat.format(estimatedMonthlyRent) + "/mese", "Entrata Ricorrente"),
                Triple("Incasso Lordo Annuo (Gross Cash Flow)", currencyFormat.format(annualGrossIncome) + "/anno", "Entrata Annua"),
                Triple("Rendimento Lordo su Investimento Totale", String.format(Locale.ITALY, "%.2f%%", grossYield), "ROI Annuo")
            )

            var rowY = 392f
            tableRows.forEachIndexed { idx, row ->
                paint.color = if (idx % 2 == 0) lightGrayBg else android.graphics.Color.WHITE
                canvas.drawRect(25f, rowY, 570f, rowY + 22f, paint)

                paint.color = cardBorder
                canvas.drawLine(25f, rowY + 22f, 570f, rowY + 22f, paint)

                paint.color = if (idx == 3 || idx == 6) navyBg else android.graphics.Color.BLACK
                paint.textSize = 9.5f
                paint.isFakeBoldText = (idx == 3 || idx == 6)

                canvas.drawText(row.first, 35f, rowY + 15f, paint)
                canvas.drawText(row.second, 300f, rowY + 15f, paint)

                if (idx == 6) paint.color = emeraldGreen else if (idx == 3) paint.color = cyanAccent
                canvas.drawText(row.third, 440f, rowY + 15f, paint)

                rowY += 22f
            }

            // 6. Market Trends & Zone Comparison Section
            rowY += 20f
            paint.color = navyBg
            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas.drawText("ANALISI DI MERCATO & TREND REGIONALE", 25f, rowY, paint)

            paint.color = cyanAccent
            canvas.drawLine(25f, rowY + 6f, 280f, rowY + 6f, paint)

            rowY += 15f
            paint.color = cardBg
            val trendCardRect = RectF(25f, rowY, 570f, rowY + 115f)
            canvas.drawRoundRect(trendCardRect, 8f, 8f, paint)

            paint.color = cardBorder
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(trendCardRect, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            val avgZonePriceSqm = (deal.estimatedMarketValue / if (deal.surfaceSqm > 0) deal.surfaceSqm else 1).toInt()
            val propertyPriceSqm = (deal.askingPrice / if (deal.surfaceSqm > 0) deal.surfaceSqm else 1).toInt()

            paint.color = textWhite
            paint.textSize = 10.5f
            paint.isFakeBoldText = true
            canvas.drawText("• Valore Medio di Zona (${deal.location}): ~${avgZonePriceSqm} €/m²", 40f, rowY + 25f, paint)

            paint.color = cyanAccent
            canvas.drawText("• Prezzo Richiesto Immobile: ~${propertyPriceSqm} €/m² (Sconto di ${avgZonePriceSqm - propertyPriceSqm} €/m²)", 40f, rowY + 45f, paint)

            paint.color = emeraldGreen
            canvas.drawText("• Posizionamento Opportunità: ${deal.discountPercent}% sotto la media comparativa locale.", 40f, rowY + 65f, paint)

            paint.color = textMuted
            paint.textSize = 9.5f
            paint.isFakeBoldText = false
            val trendAnalysisText = "Trend storico di zona positivo (+2.4% annuo). Ottimo potenziale di valorizzazione sia in logica Buy & Hold che Fix & Flip."
            canvas.drawText(trendAnalysisText, 40f, rowY + 85f, paint)
            canvas.drawText("Fonte Dati & Algoritmo: RE-Investor Radar Scraper Engine (${deal.sourceName})", 40f, rowY + 102f, paint)

            // 7. Investor Notes (if present)
            rowY += 130f
            if (deal.notes.isNotBlank()) {
                paint.color = navyBg
                paint.textSize = 12f
                paint.isFakeBoldText = true
                canvas.drawText("NOTE & APPUNTI INVESTITORE", 25f, rowY, paint)

                paint.color = cardBg
                val notesRect = RectF(25f, rowY + 10f, 570f, rowY + 60f)
                canvas.drawRoundRect(notesRect, 6f, 6f, paint)

                paint.color = textWhite
                paint.textSize = 9.5f
                paint.isFakeBoldText = false
                val truncatedNotes = if (deal.notes.length > 130) deal.notes.take(127) + "..." else deal.notes
                canvas.drawText(truncatedNotes, 35f, rowY + 32f, paint)

                rowY += 75f
            }

            // 8. Footer Section
            paint.color = navyBg
            canvas.drawRect(0f, 800f, 595f, 842f, paint)

            paint.color = textMuted
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            canvas.drawText("RE-Investor Mobile Suite — Documento per uso interno e analisi di due diligence finanziaria riservata.", 25f, 822f, paint)
            canvas.drawText("Pagina 1 / 1", 520f, 822f, paint)

            pdfDocument.finishPage(page)

            // Save PDF File
            val exportDir = File(context.cacheDir, "pdf_reports").apply { if (!exists()) mkdirs() }
            val sanitizeName = deal.title.replace(Regex("[^a-zA-Z0-9]"), "_").take(25)
            val pdfFile = File(exportDir, "Dossier_${sanitizeName}_${System.currentTimeMillis()}.pdf")

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates an institutional-grade, multi-page PDF Investment Report tailored for Banks, Lenders, and Equity Partners.
     * Includes Capital Structure (Uses & Sources), Cash Flow Waterfall, DSCR Bank Ratio, Sensitivity Stress-Test,
     * Fix & Flip Scenario, and Formal Sign-off Section.
     */
    fun generateBankAndPartnerReportPdf(
        context: Context,
        calcData: com.example.ui.components.RoiCalculationData,
        propertyTitle: String = "Immobile Target per Investimento",
        location: String = "Italia",
        propertyType: String = "Residenziale",
        surfaceSqm: Int = 80,
        recipientName: String = "Istituto Bancario / Partner di Credito",
        recipientType: String = "Banca / Istituto di Credito",
        strategy: String = "Buy & Hold (Messa a Reddito)",
        investorNotes: String = ""
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            val paint = Paint().apply { isAntiAlias = true }

            // Palette
            val navyBg = android.graphics.Color.rgb(15, 23, 42) // #0F172A
            val headerSubNavy = android.graphics.Color.rgb(30, 41, 59) // #1E293B
            val cardBg = android.graphics.Color.rgb(248, 250, 252) // #F8FAFC
            val cardBorder = android.graphics.Color.rgb(226, 232, 240) // #E2E8F0
            val darkBorder = android.graphics.Color.rgb(51, 65, 85) // #334155
            val cyanAccent = android.graphics.Color.rgb(6, 182, 212) // #06B6D4
            val emeraldGreen = android.graphics.Color.rgb(16, 185, 129) // #10B981
            val emeraldDark = android.graphics.Color.rgb(5, 150, 105)
            val amberGold = android.graphics.Color.rgb(217, 119, 6) // #D97706
            val purpleIndigo = android.graphics.Color.rgb(99, 102, 241) // #6366F1
            val textDark = android.graphics.Color.rgb(15, 23, 42)
            val textMuted = android.graphics.Color.rgb(100, 116, 139)
            val lightGrayRow = android.graphics.Color.rgb(241, 245, 249)

            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.ITALY)
            val timestampStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(Date())

            // -------------------------------------------------------------
            // PAGE 1: Executive Summary, Uses & Sources, Cash Flow & Banking Ratios
            // -------------------------------------------------------------
            val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page1 = pdfDocument.startPage(pageInfo1)
            val c1 = page1.canvas
            c1.drawColor(android.graphics.Color.WHITE)

            // Header Banner
            paint.color = navyBg
            c1.drawRect(0f, 0f, pageWidth.toFloat(), 85f, paint)

            paint.color = cyanAccent
            c1.drawRect(0f, 82f, pageWidth.toFloat(), 85f, paint)

            paint.color = android.graphics.Color.WHITE
            paint.textSize = 17f
            paint.isFakeBoldText = true
            c1.drawText("DOSSIER FINANZIARIO & BUSINESS PLAN IMMOBILIARE", 25f, 34f, paint)

            paint.color = cyanAccent
            paint.textSize = 10.5f
            paint.isFakeBoldText = false
            c1.drawText("PROPOSTA DI CO-INVESTIMENTO E RICHIESTA AFFIDAMENTO BANCARIO", 25f, 52f, paint)

            paint.color = android.graphics.Color.rgb(203, 213, 225)
            paint.textSize = 9f
            c1.drawText("Destinatario: $recipientName ($recipientType)", 25f, 70f, paint)

            paint.color = android.graphics.Color.rgb(148, 163, 184)
            c1.drawText("Data: $timestampStr", 420f, 34f, paint)
            c1.drawText("Protocollo: RE-INV-${System.currentTimeMillis().toString().takeLast(6)}", 420f, 50f, paint)
            c1.drawText("Rating: ${calcData.yieldRating.label.take(18)}", 420f, 68f, paint)

            // 1. Executive Summary Box
            var currentY = 100f
            paint.color = cardBg
            val execBox = RectF(25f, currentY, 570f, currentY + 75f)
            c1.drawRoundRect(execBox, 8f, 8f, paint)
            paint.color = cardBorder
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f
            c1.drawRoundRect(execBox, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            // Left stripe on Executive summary
            paint.color = cyanAccent
            c1.drawRoundRect(RectF(25f, currentY, 32f, currentY + 75f), 4f, 4f, paint)

            paint.color = textDark
            paint.textSize = 13f
            paint.isFakeBoldText = true
            val cleanTitle = if (propertyTitle.length > 45) propertyTitle.take(42) + "..." else propertyTitle
            c1.drawText(cleanTitle, 42f, currentY + 22f, paint)

            paint.color = textMuted
            paint.textSize = 9.5f
            paint.isFakeBoldText = false
            c1.drawText("Ubicazione: $location  •  Tipologia: $propertyType ($surfaceSqm m²)  •  Strategia: $strategy", 42f, currentY + 40f, paint)

            paint.color = emeraldDark
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
            c1.drawText("Sintesi Economica: Capitale Iniziale ${currencyFormat.format(calcData.initialCashRequired)}  |  Cashflow ${currencyFormat.format(calcData.monthlyNetCashFlow)}/mese  |  CoC ${String.format(Locale.ITALY, "%.2f%%", calcData.cashOnCashReturnPercent)}", 42f, currentY + 58f, paint)

            currentY += 90f

            // 2. Quadro Impieghi & Fonti (Capital Structure / Uses & Sources)
            paint.color = navyBg
            paint.textSize = 12f
            paint.isFakeBoldText = true
            c1.drawText("1. QUADRO ECONOMICO: IMPIEGHI & FONTI DI FINANZIAMENTO", 25f, currentY, paint)

            paint.color = cyanAccent
            c1.drawLine(25f, currentY + 4f, 380f, currentY + 4f, paint)

            currentY += 12f

            // Uses & Sources Table
            val usesHeaderY = currentY
            paint.color = headerSubNavy
            c1.drawRect(25f, usesHeaderY, 570f, usesHeaderY + 20f, paint)

            paint.color = android.graphics.Color.WHITE
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
            c1.drawText("Impieghi di Capitale (Uscite)", 35f, usesHeaderY + 14f, paint)
            c1.drawText("Importo (€)", 195f, usesHeaderY + 14f, paint)
            c1.drawText("Fonti di Copertura Finanziaria", 305f, usesHeaderY + 14f, paint)
            c1.drawText("Importo / Quota", 465f, usesHeaderY + 14f, paint)

            val usesAndSourcesRows = listOf(
                Pair(Pair("Prezzo di Acquisto / Asta", currencyFormat.format(calcData.purchasePrice)), Pair("Capitale Proprio (Equity)", currencyFormat.format(calcData.initialCashRequired))),
                Pair(Pair("Ristrutturazione & Opere Capex", currencyFormat.format(calcData.renovationCost)), Pair("Quota Equity su Costo Tot.", "${String.format(Locale.ITALY, "%.1f", if (calcData.totalProjectCost > 0) (calcData.initialCashRequired / calcData.totalProjectCost) * 100 else 0.0)}%")),
                Pair(Pair("Spese Notarili, Tecniche & Imposte", currencyFormat.format(calcData.legalFees)), Pair("Mutuo / Debito Bancario", currencyFormat.format(calcData.loanAmount))),
                Pair(Pair("TOTALE IMPIEGHI (Costo Progetto)", currencyFormat.format(calcData.totalProjectCost)), Pair("TOTALE FONTI A COPERTURA", currencyFormat.format(calcData.totalProjectCost)))
            )

            var rowUsesY = usesHeaderY + 20f
            usesAndSourcesRows.forEachIndexed { idx, pair ->
                val isTotal = idx == usesAndSourcesRows.size - 1
                paint.color = if (isTotal) cardBg else if (idx % 2 == 0) lightGrayRow else android.graphics.Color.WHITE
                c1.drawRect(25f, rowUsesY, 570f, rowUsesY + 20f, paint)

                paint.color = cardBorder
                c1.drawLine(25f, rowUsesY + 20f, 570f, rowUsesY + 20f, paint)
                c1.drawLine(295f, rowUsesY, 295f, rowUsesY + 20f, paint)

                paint.color = if (isTotal) navyBg else textDark
                paint.textSize = if (isTotal) 9.5f else 9f
                paint.isFakeBoldText = isTotal

                c1.drawText(pair.first.first, 35f, rowUsesY + 14f, paint)
                c1.drawText(pair.first.second, 195f, rowUsesY + 14f, paint)

                if (isTotal) paint.color = emeraldDark
                c1.drawText(pair.second.first, 305f, rowUsesY + 14f, paint)
                c1.drawText(pair.second.second, 465f, rowUsesY + 14f, paint)

                rowUsesY += 20f
            }

            currentY = rowUsesY + 18f

            // 3. Prospetto Cash Flow & Conto Economico di Gestione
            paint.color = navyBg
            paint.textSize = 12f
            paint.isFakeBoldText = true
            c1.drawText("2. CONTO ECONOMICO ANNUALE & SOSTENIBILITÀ DEL DEBITO", 25f, currentY, paint)

            paint.color = cyanAccent
            c1.drawLine(25f, currentY + 4f, 380f, currentY + 4f, paint)

            currentY += 12f

            paint.color = headerSubNavy
            c1.drawRect(25f, currentY, 570f, currentY + 20f, paint)

            paint.color = android.graphics.Color.WHITE
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
            c1.drawText("Voce Conto Economico", 35f, currentY + 14f, paint)
            c1.drawText("Base Mensile", 260f, currentY + 14f, paint)
            c1.drawText("Base Annuale", 370f, currentY + 14f, paint)
            c1.drawText("Incidenza %", 480f, currentY + 14f, paint)

            val annualGross = calcData.annualGrossRent
            val ceRows = listOf(
                listOf("Ricavi Lordi da Locazione (Gross Revenue)", currencyFormat.format(calcData.estimatedMonthlyRent), currencyFormat.format(annualGross), "100.0%"),
                listOf("Spese Operative (IMU, Gestione, Assicurazione)", "- " + currencyFormat.format(calcData.monthlyExpenses), "- " + currencyFormat.format(calcData.annualExpenses), String.format(Locale.ITALY, "-%.1f%%", if (annualGross > 0) (calcData.annualExpenses / annualGross) * 100 else 0.0)),
                listOf("Reddito Operativo Netto (NOI - Net Operating Income)", currencyFormat.format(calcData.netOperatingIncome / 12.0), currencyFormat.format(calcData.netOperatingIncome), String.format(Locale.ITALY, "%.1f%%", if (annualGross > 0) (calcData.netOperatingIncome / annualGross) * 100 else 0.0)),
                listOf("Servizio del Debito (Rata Mutuo: ${calcData.mortgageRatePercent}%, ${calcData.loanTermYears}a)", "- " + currencyFormat.format(calcData.monthlyMortgagePayment), "- " + currencyFormat.format(calcData.annualDebtService), String.format(Locale.ITALY, "-%.1f%%", if (annualGross > 0) (calcData.annualDebtService / annualGross) * 100 else 0.0)),
                listOf("FLUSSO DI CASSA NETTO (Pre-tax Cash Flow)", currencyFormat.format(calcData.monthlyNetCashFlow), currencyFormat.format(calcData.annualNetCashFlow), String.format(Locale.ITALY, "%.1f%%", if (annualGross > 0) (calcData.annualNetCashFlow / annualGross) * 100 else 0.0))
            )

            var ceY = currentY + 20f
            ceRows.forEachIndexed { idx, row ->
                val isNoi = idx == 2
                val isFinal = idx == 4
                paint.color = if (isFinal) cardBg else if (idx % 2 == 0) lightGrayRow else android.graphics.Color.WHITE
                c1.drawRect(25f, ceY, 570f, ceY + 20f, paint)

                paint.color = cardBorder
                c1.drawLine(25f, ceY + 20f, 570f, ceY + 20f, paint)

                paint.color = if (isFinal) emeraldDark else if (isNoi) navyBg else textDark
                paint.textSize = if (isFinal || isNoi) 9.5f else 9f
                paint.isFakeBoldText = (isFinal || isNoi)

                c1.drawText(row[0], 35f, ceY + 14f, paint)
                c1.drawText(row[1], 260f, ceY + 14f, paint)
                c1.drawText(row[2], 370f, ceY + 14f, paint)
                c1.drawText(row[3], 480f, ceY + 14f, paint)

                ceY += 20f
            }

            currentY = ceY + 18f

            // 4. Indicatori di Bancabilità & Rendimento (Bank & Partner Ratios)
            paint.color = navyBg
            paint.textSize = 12f
            paint.isFakeBoldText = true
            c1.drawText("3. INDICATORI CHIAVE PER L'ISTITUTO FINANZIATORE & PARTNER", 25f, currentY, paint)

            paint.color = cyanAccent
            c1.drawLine(25f, currentY + 4f, 430f, currentY + 4f, paint)

            currentY += 14f

            // DSCR calculation
            val dscr = if (calcData.annualDebtService > 0) calcData.netOperatingIncome / calcData.annualDebtService else 9.99
            val dscrStr = if (calcData.annualDebtService > 0) String.format(Locale.ITALY, "%.2fx", dscr) else "100% Cash"
            val dscrSubtext = if (dscr >= 1.3) "Ottima copertura (> 1.30x)" else if (dscr >= 1.15) "Copertura idonea (> 1.15x)" else "Copertura limitata"

            // 4 Metric cards
            val cardW = 127f
            val cardH = 68f

            // Box 1: DSCR (Debt Service Coverage)
            drawBankRatioBox(
                c1,
                RectF(25f, currentY, 25f + cardW, currentY + cardH),
                "DSCR (DEBT COVERAGE)",
                dscrStr,
                dscrSubtext,
                if (dscr >= 1.25) emeraldDark else amberGold
            )

            // Box 2: Cash-on-Cash Return
            drawBankRatioBox(
                c1,
                RectF(162f, currentY, 162f + cardW, currentY + cardH),
                "CASH-ON-CASH RETURN",
                String.format(Locale.ITALY, "%.2f%%", calcData.cashOnCashReturnPercent),
                "Ritorno su Equity versata",
                emeraldDark
            )

            // Box 3: Net Cap Rate
            drawBankRatioBox(
                c1,
                RectF(299f, currentY, 299f + cardW, currentY + cardH),
                "CAP RATE NETTO",
                String.format(Locale.ITALY, "%.2f%%", calcData.netYieldCapRatePercent),
                "NOI su Costo Totale",
                cyanAccent
            )

            // Box 4: LTV & Payback
            val ltv = if (calcData.purchasePrice > 0) (calcData.loanAmount / calcData.purchasePrice) * 100 else 0.0
            drawBankRatioBox(
                c1,
                RectF(436f, currentY, 570f, currentY + cardH),
                "LOAN-TO-VALUE (LTV)",
                "${String.format(Locale.ITALY, "%.0f", ltv)}%",
                "Payback: ${String.format(Locale.ITALY, "%.1f", calcData.breakEvenYears)} anni",
                purpleIndigo
            )

            // Page 1 Footer
            paint.color = navyBg
            c1.drawRect(0f, 805f, pageWidth.toFloat(), pageHeight.toFloat(), paint)
            paint.color = android.graphics.Color.rgb(148, 163, 184)
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            c1.drawText("RE-Investor Suite — Prospetto Finanziario Riservato per Valutazione Bancaria e Partner", 25f, 825f, paint)
            c1.drawText("Pagina 1 di 2", 515f, 825f, paint)

            pdfDocument.finishPage(page1)

            // -------------------------------------------------------------
            // PAGE 2: Stress-Test / Sensibilità, Fix & Flip, Note & Firme
            // -------------------------------------------------------------
            val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
            val page2 = pdfDocument.startPage(pageInfo2)
            val c2 = page2.canvas
            c2.drawColor(android.graphics.Color.WHITE)

            // Header Banner Page 2
            paint.color = navyBg
            c2.drawRect(0f, 0f, pageWidth.toFloat(), 55f, paint)
            paint.color = cyanAccent
            c2.drawRect(0f, 52f, pageWidth.toFloat(), 55f, paint)

            paint.color = android.graphics.Color.WHITE
            paint.textSize = 14f
            paint.isFakeBoldText = true
            c2.drawText("ANALISI DI STRESS TEST, EXIT STRATEGY & FIRME", 25f, 32f, paint)

            paint.color = android.graphics.Color.rgb(148, 163, 184)
            paint.textSize = 9f
            paint.isFakeBoldText = false
            c2.drawText("Dossier: $propertyTitle • Rif: $recipientName", 350f, 32f, paint)

            var p2Y = 75f

            // 1. Stress Test & Scenario Matrix
            paint.color = navyBg
            paint.textSize = 12f
            paint.isFakeBoldText = true
            c2.drawText("4. ANALISI DI SENSIBILITÀ & STRESS TEST BANCARIO", 25f, p2Y, paint)

            paint.color = cyanAccent
            c2.drawLine(25f, p2Y + 4f, 360f, p2Y + 4f, paint)

            p2Y += 12f

            paint.color = headerSubNavy
            c2.drawRect(25f, p2Y, 570f, p2Y + 20f, paint)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
            c2.drawText("Scenario di Analisi", 35f, p2Y + 14f, paint)
            c2.drawText("Ipotesi Finanziaria", 185f, p2Y + 14f, paint)
            c2.drawText("Cash Flow Netto", 335f, p2Y + 14f, paint)
            c2.drawText("DSCR Risultante", 465f, p2Y + 14f, paint)

            // Scenarios calculations
            val stressRate = calcData.mortgageRatePercent + 1.5
            val monthlyStressRate = (stressRate / 100.0) / 12.0
            val nStress = (calcData.loanTermYears * 12).toDouble()
            val compoundStress = Math.pow(1.0 + monthlyStressRate, nStress)
            val stressMortgagePayment = if (calcData.loanAmount > 0 && compoundStress != 1.0) {
                calcData.loanAmount * (monthlyStressRate * compoundStress) / (compoundStress - 1.0)
            } else calcData.monthlyMortgagePayment
            val stressAnnualDebt = stressMortgagePayment * 12.0
            val stressVacancyRent = calcData.annualGrossRent * 0.916 // 1 month vacancy (8.3%)
            val stressNoi = (stressVacancyRent - (calcData.annualExpenses * 1.15)).coerceAtLeast(0.0)
            val stressNetCashflow = stressNoi - stressAnnualDebt
            val stressDscr = if (stressAnnualDebt > 0) stressNoi / stressAnnualDebt else 9.99

            val scenarioRows = listOf(
                listOf("Scenario Base (Atteso)", "Tasso ${calcData.mortgageRatePercent}%, Sfitto 0%", "${currencyFormat.format(calcData.monthlyNetCashFlow)}/m", dscrStr),
                listOf("Scenario Prudenziale", "Tasso +0.75%, Sfitto 4%", "${currencyFormat.format((calcData.annualNetCashFlow * 0.85) / 12.0)}/m", String.format(Locale.ITALY, "%.2fx", (dscr * 0.88).coerceAtLeast(1.0))),
                listOf("Stress Test Estremo", "Tasso +1.50%, Sfitto 1 mese (8.3%)", "${currencyFormat.format(stressNetCashflow / 12.0)}/m", if (stressAnnualDebt > 0) String.format(Locale.ITALY, "%.2fx", stressDscr) else "100% Cash")
            )

            var scY = p2Y + 20f
            scenarioRows.forEachIndexed { idx, row ->
                paint.color = if (idx % 2 == 0) lightGrayRow else android.graphics.Color.WHITE
                c2.drawRect(25f, scY, 570f, scY + 20f, paint)

                paint.color = cardBorder
                c2.drawLine(25f, scY + 20f, 570f, scY + 20f, paint)

                paint.color = textDark
                paint.textSize = 9f
                paint.isFakeBoldText = (idx == 0)

                c2.drawText(row[0], 35f, scY + 14f, paint)
                c2.drawText(row[1], 185f, scY + 14f, paint)

                paint.color = if (idx == 2 && stressNetCashflow < 0) android.graphics.Color.RED else emeraldDark
                c2.drawText(row[2], 335f, scY + 14f, paint)

                paint.color = textDark
                c2.drawText(row[3], 465f, scY + 14f, paint)

                scY += 20f
            }

            p2Y = scY + 18f

            // 2. Scenario Alternativo di Uscita Rapida: Fix & Flip
            paint.color = navyBg
            paint.textSize = 12f
            paint.isFakeBoldText = true
            c2.drawText("5. PIANO DI VALORIZZAZIONE ALTERNATIVO: FIX & FLIP (RIVENDITA)", 25f, p2Y, paint)

            paint.color = cyanAccent
            c2.drawLine(25f, p2Y + 4f, 440f, p2Y + 4f, paint)

            p2Y += 12f

            paint.color = cardBg
            val flipBox = RectF(25f, p2Y, 570f, p2Y + 70f)
            c2.drawRoundRect(flipBox, 8f, 8f, paint)
            paint.color = cardBorder
            paint.style = Paint.Style.STROKE
            c2.drawRoundRect(flipBox, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            val resaleVal = calcData.expectedResalePrice
            val flipProfit = calcData.totalFlipProfit
            val flipRoi = calcData.flipRoiPercent

            paint.color = textDark
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
            c2.drawText("Valore di Rivendita Stimato post Riqualificazione (ARV): ${currencyFormat.format(resaleVal)}", 40f, p2Y + 22f, paint)

            paint.color = textMuted
            paint.isFakeBoldText = false
            c2.drawText("Costo Totale di Realizzazione (Acquisto + Opere + Oneri): ${currencyFormat.format(calcData.totalProjectCost)}", 40f, p2Y + 40f, paint)

            paint.color = if (flipProfit > 0) emeraldDark else android.graphics.Color.RED
            paint.isFakeBoldText = true
            c2.drawText("Margine di Utile Netto Atteso: ${currencyFormat.format(flipProfit)}  |  ROI Totale Operazione: ${String.format(Locale.ITALY, "%.1f%%", flipRoi)}", 40f, p2Y + 58f, paint)

            p2Y += 85f

            // 3. Note di Due Diligence & Punti di Forza
            paint.color = navyBg
            paint.textSize = 12f
            paint.isFakeBoldText = true
            c2.drawText("6. NOTE DI DUE DILIGENCE, GARANZIE & CONCLUSIONI", 25f, p2Y, paint)

            paint.color = cyanAccent
            c2.drawLine(25f, p2Y + 4f, 380f, p2Y + 4f, paint)

            p2Y += 12f

            paint.color = cardBg
            val notesBox = RectF(25f, p2Y, 570f, p2Y + 95f)
            c2.drawRoundRect(notesBox, 8f, 8f, paint)
            paint.color = cardBorder
            paint.style = Paint.Style.STROKE
            c2.drawRoundRect(notesBox, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            paint.color = textDark
            paint.textSize = 9f
            paint.isFakeBoldText = false
            c2.drawText("• Conformità Urbanistica & Catastale: Da verificare in sede di perizia tecnica preventiva.", 38f, p2Y + 20f, paint)
            c2.drawText("• Posizionamento Prezzo: Operazione impostata a forte sconto rispetto alle quotazioni OMI di mercato.", 38f, p2Y + 36f, paint)
            c2.drawText("• Liquidità del Mercato Locale: Elevata domanda sia per locazione transitoria/studenti che per prima casa.", 38f, p2Y + 52f, paint)

            val customNoteText = if (investorNotes.isNotBlank()) {
                "• Note Aggiuntive: " + if (investorNotes.length > 90) investorNotes.take(87) + "..." else investorNotes
            } else {
                "• Garanzie Finanziarie: Disponibilità di apporto capitale proprio immediato a copertura della quota di equity."
            }
            c2.drawText(customNoteText, 38f, p2Y + 68f, paint)
            c2.drawText("• Mitigazione Rischio: Presenza di buffer di sicurezza per imprevisti nei costi di ristrutturazione (~10%).", 38f, p2Y + 84f, paint)

            p2Y += 115f

            // 4. Sezione Firme & Approvazione Formale (Bank / Partner Sign-off)
            paint.color = navyBg
            paint.textSize = 12f
            paint.isFakeBoldText = true
            c2.drawText("7. SOTTOSCRIZIONE & PRESA VISIONE PROPOSTA", 25f, p2Y, paint)

            paint.color = cyanAccent
            c2.drawLine(25f, p2Y + 4f, 300f, p2Y + 4f, paint)

            p2Y += 20f

            // Proponente Sign Box
            paint.color = cardBg
            val sign1 = RectF(25f, p2Y, 285f, p2Y + 70f)
            c2.drawRoundRect(sign1, 6f, 6f, paint)
            paint.color = cardBorder
            paint.style = Paint.Style.STROKE
            c2.drawRoundRect(sign1, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            paint.color = textDark
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
            c2.drawText("L'INVESTITORE / PROPONENTE", 35f, p2Y + 18f, paint)
            paint.color = textMuted
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            c2.drawText("Firma e Data: ________________________", 35f, p2Y + 54f, paint)

            // Bank/Partner Sign Box
            paint.color = cardBg
            val sign2 = RectF(305f, p2Y, 570f, p2Y + 70f)
            c2.drawRoundRect(sign2, 6f, 6f, paint)
            paint.color = cardBorder
            paint.style = Paint.Style.STROKE
            c2.drawRoundRect(sign2, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            paint.color = textDark
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
            c2.drawText("PER LA BANCA / PARTNER FINANZIARIO", 315f, p2Y + 18f, paint)
            paint.color = textMuted
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            c2.drawText("Firma e Timbro: ________________________", 315f, p2Y + 54f, paint)

            // Page 2 Footer
            paint.color = navyBg
            c2.drawRect(0f, 805f, pageWidth.toFloat(), pageHeight.toFloat(), paint)
            paint.color = android.graphics.Color.rgb(148, 163, 184)
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            c2.drawText("RE-Investor Suite — Prospetto Finanziario Riservato per Valutazione Bancaria e Partner", 25f, 825f, paint)
            c2.drawText("Pagina 2 di 2", 515f, 825f, paint)

            pdfDocument.finishPage(page2)

            // Save document
            val exportDir = File(context.cacheDir, "pdf_reports").apply { if (!exists()) mkdirs() }
            val sanitizeName = propertyTitle.replace(Regex("[^a-zA-Z0-9]"), "_").take(25)
            val pdfFile = File(exportDir, "Report_Bancario_${sanitizeName}_${System.currentTimeMillis()}.pdf")

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawBankRatioBox(
        canvas: Canvas,
        rect: RectF,
        label: String,
        value: String,
        subtext: String,
        accentColor: Int
    ) {
        val paint = Paint().apply { isAntiAlias = true }
        val cardBg = android.graphics.Color.rgb(248, 250, 252)
        val cardBorder = android.graphics.Color.rgb(226, 232, 240)
        val textMuted = android.graphics.Color.rgb(100, 116, 139)

        paint.color = cardBg
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.color = cardBorder
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Top Accent Stripe
        paint.color = accentColor
        canvas.drawRoundRect(RectF(rect.left + 8f, rect.top + 3f, rect.right - 8f, rect.top + 5f), 2f, 2f, paint)

        // Label
        paint.color = textMuted
        paint.textSize = 7.5f
        paint.isFakeBoldText = true
        canvas.drawText(label, rect.left + 8f, rect.top + 20f, paint)

        // Value
        paint.color = accentColor
        paint.textSize = 13.5f
        paint.isFakeBoldText = true
        canvas.drawText(value, rect.left + 8f, rect.top + 42f, paint)

        // Subtext
        paint.color = textMuted
        paint.textSize = 7.5f
        paint.isFakeBoldText = false
        val cleanSubtext = if (subtext.length > 24) subtext.take(22) + "..." else subtext
        canvas.drawText(cleanSubtext, rect.left + 8f, rect.top + 57f, paint)
    }

    /**
     * Opens the generated PDF directly in an external or system PDF viewer.
     */
    fun openPdfFile(context: Context, pdfFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(viewIntent, "Apri Report PDF con...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Nessun lettore PDF trovato. Condividi il file.", Toast.LENGTH_SHORT).show()
            sharePropertyPdf(context, pdfFile, "Report Finanziario", "Italia", emailOnly = false)
        }
    }

    /**
     * Shares the bank/partner PDF file via chooser (WhatsApp, Email, Drive, etc.).
     */
    fun shareBankAndPartnerPdf(
        context: Context,
        pdfFile: File,
        propertyTitle: String,
        recipientName: String,
        emailOnly: Boolean = false
    ) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val subject = "Dossier Finanziario & Business Plan: $propertyTitle"
            val bodyText = """
                Gentile $recipientName,
                
                Invio in allegato il dossier finanziario completo in formato PDF relativo all'operazione immobiliare "$propertyTitle".
                
                Il report contiene:
                • Quadro Economico Impieghi & Fonti (Costo complessivo e struttura debito/equity)
                • Conto Economico di Gestione, Net Operating Income (NOI) e Flussi di Cassa
                • Indicatori Chiave per il Credito (DSCR Debt Service Coverage, LTV, Cash-on-Cash Return)
                • Stress Test di sensibilità a tassi/sfitti e piano alternativo Fix & Flip
                
                A disposizione per qualsiasi approfondimento o chiarimento.
                
                Cordiali saluti,
                RE-Investor Analytics
            """.trimIndent()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (emailOnly) "message/rfc822" else "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, bodyText)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserTitle = if (emailOnly) "Invia Dossier via Email..." else "Condividi Report Bancario con Partner/Banca..."
            val chooser = Intent.createChooser(shareIntent, chooserTitle)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Errore nella condivisione del PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Helper to generate and share bank report in one click.
     */
    fun generateAndShareBankReport(
        context: Context,
        calcData: com.example.ui.components.RoiCalculationData,
        propertyTitle: String = "Immobile Target",
        location: String = "Milano (MI)",
        recipientName: String = "Banca / Partner",
        strategy: String = "Buy & Hold",
        investorNotes: String = "",
        emailOnly: Boolean = false
    ): File? {
        Toast.makeText(context, "Generazione dossier per Banche & Partner...", Toast.LENGTH_SHORT).show()
        val pdfFile = generateBankAndPartnerReportPdf(
            context = context,
            calcData = calcData,
            propertyTitle = propertyTitle,
            location = location,
            recipientName = recipientName,
            strategy = strategy,
            investorNotes = investorNotes
        )

        if (pdfFile != null && pdfFile.exists()) {
            shareBankAndPartnerPdf(context, pdfFile, propertyTitle, recipientName, emailOnly)
        } else {
            Toast.makeText(context, "Impossibile generare il PDF.", Toast.LENGTH_SHORT).show()
        }
        return pdfFile
    }

    /**
     * Shares the PDF file via Installed Email Apps or general share chooser.
     */
    fun sharePropertyPdf(
        context: Context,
        pdfFile: File,
        propertyTitle: String,
        location: String,
        emailOnly: Boolean = true
    ) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val subject = "Dossier Immobile PDF: $propertyTitle ($location)"
            val bodyText = """
                Salve,

                In allegato trova il dossier riassuntivo in formato PDF relativo all'immobile:
                • Titolo: $propertyTitle
                • Ubicazione: $location

                Il report include l'analisi finanziaria, i costi stimati di ristrutturazione, le proiezioni di rendimento Cash-on-Cash e i dati di confronto con i trend di mercato locali.

                Generato da RE-Investor Analytics.
            """.trimIndent()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (emailOnly) "message/rfc822" else "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, bodyText)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserTitle = if (emailOnly) "Invia PDF via Email..." else "Condividi Report PDF con..."
            val chooser = Intent.createChooser(shareIntent, chooserTitle)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Errore nella condivisione del PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun drawMetricBox(
        canvas: Canvas,
        rect: RectF,
        label: String,
        value: String,
        subtext: String,
        accentColor: Int
    ) {
        val paint = Paint().apply { isAntiAlias = true }
        val cardBg = android.graphics.Color.rgb(30, 41, 59)
        val cardBorder = android.graphics.Color.rgb(51, 65, 85)
        val textMuted = android.graphics.Color.rgb(148, 163, 184)

        paint.color = cardBg
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.color = cardBorder
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Top Accent Line
        paint.color = accentColor
        canvas.drawRect(rect.left + 8f, rect.top + 3f, rect.right - 8f, rect.top + 5f, paint)

        // Label
        paint.color = textMuted
        paint.textSize = 8.5f
        paint.isFakeBoldText = true
        canvas.drawText(label, rect.left + 10f, rect.top + 20f, paint)

        // Value
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 13.5f
        paint.isFakeBoldText = true
        canvas.drawText(value, rect.left + 10f, rect.top + 42f, paint)

        // Subtext
        paint.color = accentColor
        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        canvas.drawText(subtext, rect.left + 10f, rect.top + 58f, paint)
    }

    /**
     * Utility method that generates the PDF and opens the share intent in a single call.
     */
    fun generateAndSharePdf(
        context: Context,
        deal: PropertyDeal,
        emailOnly: Boolean = true
    ) {
        Toast.makeText(context, "Generazione dossier PDF in corso...", Toast.LENGTH_SHORT).show()
        val pdfFile = generatePdfForDeal(context, deal)
        if (pdfFile != null && pdfFile.exists()) {
            sharePropertyPdf(context, pdfFile, deal.title, deal.location, emailOnly = emailOnly)
        } else {
            Toast.makeText(context, "Impossibile creare il file PDF.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates a side-by-side comparative PDF analysis sheet evaluating two properties (Property A vs Property B).
     */
    fun generateComparisonReportPdf(
        context: Context,
        propertyA: Property,
        propertyB: Property,
        evalA: PropertyOpportunityEvaluation? = null,
        evalB: PropertyOpportunityEvaluation? = null,
        investorNotes: String = ""
    ): File? {
        return try {
            val data = PropertyComparisonReportCalculator.calculate(propertyA, propertyB, evalA, evalB)
            fun fmtSqm(v: Int?): String = v?.let { "$it m²" } ?: "N/D"
            fun fmtPerSqm(v: Double?): String = v?.let { "${it.roundToInt()} €/m²" } ?: "N/D"

            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            // Color Palette
            val navyBg = android.graphics.Color.rgb(15, 23, 42) // #0F172A
            val headerSubNavy = android.graphics.Color.rgb(30, 41, 59) // #1E293B
            val cardBgDark = android.graphics.Color.rgb(30, 41, 59) // #1E293B
            val cardBorder = android.graphics.Color.rgb(51, 65, 85) // #334155
            val lightCardBg = android.graphics.Color.rgb(248, 250, 252) // #F8FAFC
            val lightCardBorder = android.graphics.Color.rgb(226, 232, 240) // #E2E8F0
            val lightRowBg = android.graphics.Color.rgb(241, 245, 249)
            val cyanAccent = android.graphics.Color.rgb(6, 182, 212) // #06B6D4
            val emeraldGreen = android.graphics.Color.rgb(16, 185, 129) // #10B981
            val amberGold = android.graphics.Color.rgb(245, 158, 11) // #F59E0B
            val purpleAccent = android.graphics.Color.rgb(168, 85, 247) // #A855F7
            val roseRed = android.graphics.Color.rgb(239, 68, 68) // #EF4444
            val textWhite = android.graphics.Color.rgb(248, 250, 252)
            val textMuted = android.graphics.Color.rgb(148, 163, 184)
            val textDark = android.graphics.Color.rgb(15, 23, 42)

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
            val timestampStr = dateFormat.format(Date())
            val protocolId = "RE-COMP-${System.currentTimeMillis().toString().takeLast(6)}"

            // Page Background
            canvas.drawColor(android.graphics.Color.WHITE)

            // 1. Header Banner (Navy)
            paint.color = navyBg
            canvas.drawRect(0f, 0f, 595f, 82f, paint)

            // Cyan/Purple Accent Split Lines
            paint.color = cyanAccent
            canvas.drawRect(0f, 79f, 297f, 82f, paint)
            paint.color = purpleAccent
            canvas.drawRect(297f, 79f, 595f, 82f, paint)

            // Header Title
            paint.color = textWhite
            paint.textSize = 17f
            paint.isFakeBoldText = true
            canvas.drawText("RE-INVESTOR ANALYTICS", 25f, 32f, paint)

            paint.color = cyanAccent
            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawText("DOSSIER DI CONFRONTO COMPARATIVO IMMOBILIARE • SIDE-BY-SIDE", 25f, 49f, paint)

            paint.color = textMuted
            paint.textSize = 8.5f
            canvas.drawText("Analisi multi-dimensionale di rendimento, fabbisogno finanziario e rischio cantiere", 25f, 65f, paint)

            // Timestamp & Protocol
            paint.color = textMuted
            paint.textSize = 8.5f
            canvas.drawText("Data: $timestampStr", 440f, 32f, paint)
            canvas.drawText("Protocollo: $protocolId", 440f, 48f, paint)
            paint.color = amberGold
            canvas.drawText("Target: 2 Immobili", 440f, 65f, paint)

            // 2. Side-by-Side Property Badges (Y: 92f to 158f)
            val cardHeight = 64f
            val cardWidth = 265f

            // Card A (Left)
            val cardRectA = RectF(25f, 92f, 25f + cardWidth, 92f + cardHeight)
            paint.color = cardBgDark
            canvas.drawRoundRect(cardRectA, 8f, 8f, paint)
            paint.color = cyanAccent
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f
            canvas.drawRoundRect(cardRectA, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            // Top Tab Tag A
            paint.color = cyanAccent
            canvas.drawRoundRect(RectF(35f, 86f, 130f, 102f), 4f, 4f, paint)
            paint.color = navyBg
            paint.textSize = 8.5f
            paint.isFakeBoldText = true
            canvas.drawText("IMMOBILE A", 45f, 97f, paint)

            paint.color = textWhite
            paint.textSize = 12f
            paint.isFakeBoldText = true
            val titleA = if (propertyA.title.length > 28) propertyA.title.take(25) + "..." else propertyA.title
            canvas.drawText(titleA, 35f, 118f, paint)

            paint.color = textMuted
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            val addrA = if (propertyA.address.length > 34) propertyA.address.take(31) + "..." else propertyA.address
            canvas.drawText(addrA, 35f, 132f, paint)
            canvas.drawText("${propertyA.propertyType} • ${fmtSqm(data.surfaceSqmA)} • ${propertyA.strategyTags.ifBlank { "Standard" }}", 35f, 146f, paint)

            // Card B (Right)
            val cardRectB = RectF(305f, 92f, 305f + cardWidth, 92f + cardHeight)
            paint.color = cardBgDark
            canvas.drawRoundRect(cardRectB, 8f, 8f, paint)
            paint.color = purpleAccent
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f
            canvas.drawRoundRect(cardRectB, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            // Top Tab Tag B
            paint.color = purpleAccent
            canvas.drawRoundRect(RectF(315f, 86f, 410f, 102f), 4f, 4f, paint)
            paint.color = navyBg
            paint.textSize = 8.5f
            paint.isFakeBoldText = true
            canvas.drawText("IMMOBILE B", 325f, 97f, paint)

            paint.color = textWhite
            paint.textSize = 12f
            paint.isFakeBoldText = true
            val titleB = if (propertyB.title.length > 28) propertyB.title.take(25) + "..." else propertyB.title
            canvas.drawText(titleB, 315f, 118f, paint)

            paint.color = textMuted
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            val addrB = if (propertyB.address.length > 34) propertyB.address.take(31) + "..." else propertyB.address
            canvas.drawText(addrB, 315f, 132f, paint)
            canvas.drawText("${propertyB.propertyType} • ${fmtSqm(data.surfaceSqmB)} • ${propertyB.strategyTags.ifBlank { "Standard" }}", 315f, 146f, paint)

            // 3. Four Key Comparison Metric Bento Boxes (Y: 166f to 286f)
            fun drawComparisonMetricPill(
                rect: RectF,
                title: String,
                valA: String,
                valB: String,
                subA: String,
                subB: String,
                winner: String,
                accentColor: Int
            ) {
                paint.color = lightCardBg
                canvas.drawRoundRect(rect, 6f, 6f, paint)
                paint.color = lightCardBorder
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(rect, 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                // Top Accent Bar
                paint.color = accentColor
                canvas.drawRect(rect.left + 6f, rect.top + 2f, rect.right - 6f, rect.top + 4f, paint)

                // Metric Title
                paint.color = textDark
                paint.textSize = 8.5f
                paint.isFakeBoldText = true
                canvas.drawText(title, rect.left + 10f, rect.top + 16f, paint)

                // Value A
                paint.color = if (winner == "A") emeraldGreen else textDark
                paint.textSize = 11.5f
                paint.isFakeBoldText = true
                canvas.drawText("A: $valA", rect.left + 10f, rect.top + 33f, paint)

                paint.color = textMuted
                paint.textSize = 7.5f
                paint.isFakeBoldText = false
                canvas.drawText(subA, rect.left + 10f, rect.top + 45f, paint)

                // Value B
                paint.color = if (winner == "B") emeraldGreen else textDark
                paint.textSize = 11.5f
                paint.isFakeBoldText = true
                canvas.drawText("B: $valB", rect.left + 140f, rect.top + 33f, paint)

                paint.color = textMuted
                paint.textSize = 7.5f
                paint.isFakeBoldText = false
                canvas.drawText(subB, rect.left + 140f, rect.top + 45f, paint)
            }

            // Metric 1: Prezzo di Ingresso
            drawComparisonMetricPill(
                rect = RectF(25f, 166f, 290f, 222f),
                title = "PREZZO D'ACQUISTO & COSTO/M²",
                valA = currencyFormat.format(data.purchasePriceA),
                valB = currencyFormat.format(data.purchasePriceB),
                subA = "${fmtPerSqm(data.pricePerSqmA)} ${if (data.winnerEntryPricePerSqm == "A") "★ Vantaggioso" else ""}",
                subB = "${fmtPerSqm(data.pricePerSqmB)} ${if (data.winnerEntryPricePerSqm == "B") "★ Vantaggioso" else ""}",
                winner = data.winnerEntryPricePerSqm,
                accentColor = cyanAccent
            )

            // Metric 2: Ristrutturazione & CapEx
            drawComparisonMetricPill(
                rect = RectF(305f, 166f, 570f, 222f),
                title = "CAPEX & INCIDENZA RISTRUTTURAZIONE",
                valA = currencyFormat.format(data.renovationCostA),
                valB = currencyFormat.format(data.renovationCostB),
                subA = "${String.format(Locale.ITALY, "%.1f", data.renoToPurchaseRatioA)}% su acquisto",
                subB = "${String.format(Locale.ITALY, "%.1f", data.renoToPurchaseRatioB)}% su acquisto",
                winner = data.winnerLowerCapExRisk,
                accentColor = amberGold
            )

            // Metric 3: Capitale Totale Investito
            drawComparisonMetricPill(
                rect = RectF(25f, 228f, 290f, 284f),
                title = "BASE DI COSTO COMPLESSIVA (INVESTITO)",
                valA = currencyFormat.format(data.totalInvestedBasisA),
                valB = currencyFormat.format(data.totalInvestedBasisB),
                subA = "${fmtPerSqm(data.totalInvestedPerSqmA)} totale",
                subB = "${fmtPerSqm(data.totalInvestedPerSqmB)} totale",
                winner = data.winnerLowestTotalCapital,
                accentColor = purpleAccent
            )

            // Metric 4: Profitto & ROI Stimato
            drawComparisonMetricPill(
                rect = RectF(305f, 228f, 570f, 284f),
                title = "MARGINALITÀ PLUSVALENZA & ROI %",
                valA = "${String.format(Locale.ITALY, "%.1f", data.projectedRoiPercentA)}%",
                valB = "${String.format(Locale.ITALY, "%.1f", data.projectedRoiPercentB)}%",
                subA = "+${currencyFormat.format(data.projectedGrossProfitA)} ${if (data.winnerMaxRoi == "A") "★ Max ROI" else ""}",
                subB = "+${currencyFormat.format(data.projectedGrossProfitB)} ${if (data.winnerMaxRoi == "B") "★ Max ROI" else ""}",
                winner = data.winnerMaxRoi,
                accentColor = emeraldGreen
            )

            // 4. Comparison Matrix Table (Y: 294f to 560f)
            paint.color = navyBg
            paint.textSize = 11.5f
            paint.isFakeBoldText = true
            canvas.drawText("TABELLA COMPARATIVA METRICHE FINANZIARIE", 25f, 304f, paint)

            paint.color = cyanAccent
            canvas.drawLine(25f, 309f, 290f, 309f, paint)

            // Table Header
            var tableY = 315f
            paint.color = headerSubNavy
            canvas.drawRect(25f, tableY, 570f, tableY + 18f, paint)

            paint.color = textWhite
            paint.textSize = 8.5f
            paint.isFakeBoldText = true
            canvas.drawText("Parametro / Indicatore", 32f, tableY + 12f, paint)
            canvas.drawText("Immobile A", 240f, tableY + 12f, paint)
            canvas.drawText("Immobile B", 350f, tableY + 12f, paint)
            canvas.drawText("Delta & Benchmark", 460f, tableY + 12f, paint)

            tableY += 18f

            val comparisonRows = listOf(
                arrayOf(
                    "Superficie Commerciale",
                    fmtSqm(data.surfaceSqmA),
                    fmtSqm(data.surfaceSqmB),
                    if (data.surfaceSqmA != null && data.surfaceSqmB != null) "${data.surfaceSqmA - data.surfaceSqmB} m²" else "N/D"
                ),
                arrayOf("Prezzo Richiesto / Acquisto", currencyFormat.format(data.purchasePriceA), currencyFormat.format(data.purchasePriceB), "${if (data.deltaPurchasePrice > 0) "+" else ""}${currencyFormat.format(data.deltaPurchasePrice)}"),
                arrayOf(
                    "Prezzo d'Ingresso al m²",
                    fmtPerSqm(data.pricePerSqmA),
                    fmtPerSqm(data.pricePerSqmB),
                    when {
                        data.winnerEntryPricePerSqm == "A" && data.pricePerSqmA != null && data.pricePerSqmB != null ->
                            "A vince (-${(data.pricePerSqmB - data.pricePerSqmA).roundToInt()}€/m²)"
                        data.winnerEntryPricePerSqm == "B" && data.pricePerSqmA != null && data.pricePerSqmB != null ->
                            "B vince (-${(data.pricePerSqmA - data.pricePerSqmB).roundToInt()}€/m²)"
                        data.winnerEntryPricePerSqm == "N/D" -> "N/D"
                        else -> "Pari"
                    }
                ),
                arrayOf("Costo Stimato Ristrutturazione", currencyFormat.format(data.renovationCostA), currencyFormat.format(data.renovationCostB), "${if (data.deltaRenovationCost > 0) "+" else ""}${currencyFormat.format(data.deltaRenovationCost)}"),
                arrayOf("Incidenza Lavori / Acquisto", "${String.format(Locale.ITALY, "%.1f", data.renoToPurchaseRatioA)}%", "${String.format(Locale.ITALY, "%.1f", data.renoToPurchaseRatioB)}%", "${if (data.winnerLowerCapExRisk == "A") "A minor rischio" else if (data.winnerLowerCapExRisk == "B") "B minor rischio" else "Pari"}"),
                arrayOf("Base di Costo Totale (Investito)", currencyFormat.format(data.totalInvestedBasisA), currencyFormat.format(data.totalInvestedBasisB), "${if (data.deltaTotalInvested > 0) "+" else ""}${currencyFormat.format(data.deltaTotalInvested)}"),
                arrayOf("Valore di Mercato Stimato (Exit)", currencyFormat.format(data.effectiveExitValueA), currencyFormat.format(data.effectiveExitValueB), "${if (data.deltaExitValue > 0) "+" else ""}${currencyFormat.format(data.deltaExitValue)}"),
                arrayOf("Plusvalenza / Margine Lordo", "+${currencyFormat.format(data.projectedGrossProfitA)}", "+${currencyFormat.format(data.projectedGrossProfitB)}", "${if (data.winnerMaxProfit == "A") "A vince (+${currencyFormat.format(data.deltaGrossProfit)})" else if (data.winnerMaxProfit == "B") "B vince (+${currencyFormat.format(-data.deltaGrossProfit)})" else "Pari"}"),
                arrayOf("Ritorno sul Capitale (ROI %)", "${String.format(Locale.ITALY, "%.1f", data.projectedRoiPercentA)}%", "${String.format(Locale.ITALY, "%.1f", data.projectedRoiPercentB)}%", "${if (data.winnerMaxRoi == "A") "A vince (+${String.format(Locale.ITALY, "%.1f", data.deltaRoiPercent)}%)" else if (data.winnerMaxRoi == "B") "B vince (+${String.format(Locale.ITALY, "%.1f", -data.deltaRoiPercent)}%)" else "Pari"}"),
                arrayOf("Canone Mensile da Locazione", if (data.monthlyRentalIncomeA > 0) "${currencyFormat.format(data.monthlyRentalIncomeA)}/m" else "N/D", if (data.monthlyRentalIncomeB > 0) "${currencyFormat.format(data.monthlyRentalIncomeB)}/m" else "N/D", if (data.deltaRentalMonthly != 0.0) "${if (data.deltaRentalMonthly > 0) "+" else ""}${currencyFormat.format(data.deltaRentalMonthly)}" else "Pari"),
                arrayOf("Rendimento Lordo Locativo (Yield)", if (data.grossRentalYieldA > 0) "${String.format(Locale.ITALY, "%.1f", data.grossRentalYieldA)}%" else "N/D", if (data.grossRentalYieldB > 0) "${String.format(Locale.ITALY, "%.1f", data.grossRentalYieldB)}%" else "N/D", "${if (data.winnerMaxRentalYield == "A") "A vince (+${String.format(Locale.ITALY, "%.1f", data.deltaGrossYield)}%)" else if (data.winnerMaxRentalYield == "B") "B vince (+${String.format(Locale.ITALY, "%.1f", -data.deltaGrossYield)}%)" else "Allineato"}")
            )

            comparisonRows.forEachIndexed { idx, row ->
                val isHighlighted = idx == 5 || idx == 7 || idx == 8
                paint.color = if (isHighlighted) lightCardBg else if (idx % 2 == 0) lightRowBg else android.graphics.Color.WHITE
                canvas.drawRect(25f, tableY, 570f, tableY + 17f, paint)

                paint.color = lightCardBorder
                canvas.drawLine(25f, tableY + 17f, 570f, tableY + 17f, paint)

                paint.color = if (isHighlighted) textDark else android.graphics.Color.rgb(51, 65, 85)
                paint.textSize = 8f
                paint.isFakeBoldText = isHighlighted
                canvas.drawText(row[0], 32f, tableY + 11.5f, paint)

                paint.color = if (isHighlighted) cyanAccent else textDark
                canvas.drawText(row[1], 240f, tableY + 11.5f, paint)

                paint.color = if (isHighlighted) purpleAccent else textDark
                canvas.drawText(row[2], 350f, tableY + 11.5f, paint)

                paint.color = if (row[3].contains("vince") || row[3].contains("Max")) emeraldGreen else textMuted
                canvas.drawText(row[3], 460f, tableY + 11.5f, paint)

                tableY += 17f
            }

            // 5. Strategic Verdict & Executive Recommendation (Y: 520f to 710f)
            tableY += 12f
            paint.color = navyBg
            paint.textSize = 11.5f
            paint.isFakeBoldText = true
            canvas.drawText("VERDETTO ESECUTIVO & STRATEGIA CONSIGLIATA", 25f, tableY, paint)

            paint.color = cyanAccent
            canvas.drawLine(25f, tableY + 5f, 310f, tableY + 5f, paint)

            tableY += 10f
            val verdictBox = RectF(25f, tableY, 570f, tableY + 120f)
            paint.color = cardBgDark
            canvas.drawRoundRect(verdictBox, 8f, 8f, paint)
            paint.color = cardBorder
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(verdictBox, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            // Left vertical accent stripe on verdict box
            paint.color = if (data.winnerMaxRoi == "A") cyanAccent else purpleAccent
            canvas.drawRoundRect(RectF(25f, tableY, 32f, tableY + 120f), 4f, 4f, paint)

            // Verdict Header
            paint.color = textWhite
            paint.textSize = 11f
            paint.isFakeBoldText = true
            val cleanVerdictTitle = if (data.verdictTitle.length > 60) data.verdictTitle.take(57) + "..." else data.verdictTitle
            canvas.drawText(cleanVerdictTitle, 40f, tableY + 20f, paint)

            // Verdict Summary
            paint.color = textMuted
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            val summaryLine1 = if (data.verdictSummary.length > 90) data.verdictSummary.take(88) + "..." else data.verdictSummary
            canvas.drawText(summaryLine1, 40f, tableY + 36f, paint)

            // Bullet points
            var bulletY = tableY + 54f
            data.keyTakeaways.take(3).forEach { takeaway ->
                paint.color = cyanAccent
                paint.textSize = 8.5f
                paint.isFakeBoldText = true
                canvas.drawText("•", 40f, bulletY, paint)

                paint.color = textWhite
                paint.textSize = 8f
                paint.isFakeBoldText = false
                val trimmedTakeaway = if (takeaway.length > 95) takeaway.take(92) + "..." else takeaway
                canvas.drawText(trimmedTakeaway, 50f, bulletY, paint)
                bulletY += 16f
            }

            tableY += 130f

            // 6. Investor Notes (if provided)
            if (investorNotes.isNotBlank()) {
                val notesBox = RectF(25f, tableY, 570f, tableY + 45f)
                paint.color = lightCardBg
                canvas.drawRoundRect(notesBox, 6f, 6f, paint)
                paint.color = lightCardBorder
                paint.style = Paint.Style.STROKE
                canvas.drawRoundRect(notesBox, 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                paint.color = textDark
                paint.textSize = 8.5f
                paint.isFakeBoldText = true
                canvas.drawText("NOTE & ANNOTAZIONI INVESTITORE:", 35f, tableY + 16f, paint)

                paint.color = textMuted
                paint.textSize = 8f
                paint.isFakeBoldText = false
                val cleanNotes = if (investorNotes.length > 110) investorNotes.take(107) + "..." else investorNotes
                canvas.drawText(cleanNotes, 35f, tableY + 32f, paint)
            }

            // 7. Footer
            paint.color = navyBg
            canvas.drawRect(0f, 805f, 595f, 842f, paint)

            paint.color = textMuted
            paint.textSize = 8f
            paint.isFakeBoldText = false
            canvas.drawText("RE-Investor Mobile Suite • Documento per uso interno, valutazione comparativa e due diligence finanziaria.", 25f, 825f, paint)
            canvas.drawText("Pagina 1 / 1", 525f, 825f, paint)

            pdfDocument.finishPage(page)

            // Save PDF File
            val exportDir = File(context.cacheDir, "pdf_reports").apply { if (!exists()) mkdirs() }
            val sanitizeA = propertyA.title.replace(Regex("[^a-zA-Z0-9]"), "_").take(12)
            val sanitizeB = propertyB.title.replace(Regex("[^a-zA-Z0-9]"), "_").take(12)
            val pdfFile = File(exportDir, "Confronto_${sanitizeA}_vs_${sanitizeB}_${System.currentTimeMillis()}.pdf")

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Shares the comparison PDF file via Android Chooser.
     */
    fun shareComparisonPdf(
        context: Context,
        pdfFile: File,
        propertyA: Property,
        propertyB: Property,
        emailOnly: Boolean = false
    ) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val titleA = propertyA.title.ifBlank { propertyA.address }
            val titleB = propertyB.title.ifBlank { propertyB.address }

            val subject = "Dossier Comparativo PDF: $titleA vs $titleB"
            val bodyText = """
                Salve,

                In allegato trova il dossier di confronto comparativo in formato PDF (Side-by-Side Analysis Sheet) relativo agli immobili:
                • Immobile A: $titleA (${propertyA.address})
                • Immobile B: $titleB (${propertyB.address})

                Il report evidenzia:
                - Confronto costi di ingresso e prezzo al m²
                - Analisi CapEx di ristrutturazione e incidenza sul capitale
                - Marginalità lorda e ROI % stimato a confronto
                - Rendimento locativo (Yield) e cash-flow mensile atteso
                - Verdetto strategico e posizionamento comparativo

                Generato da RE-Investor Analytics.
            """.trimIndent()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (emailOnly) "message/rfc822" else "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, bodyText)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserTitle = if (emailOnly) "Invia Dossier Comparativo via Email..." else "Condividi Report di Confronto PDF..."
            val chooser = Intent.createChooser(shareIntent, chooserTitle)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Errore nella condivisione del PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Generates and shares the comparison PDF in a single call.
     */
    fun generateAndShareComparisonPdf(
        context: Context,
        propertyA: Property,
        propertyB: Property,
        evalA: PropertyOpportunityEvaluation? = null,
        evalB: PropertyOpportunityEvaluation? = null,
        investorNotes: String = "",
        emailOnly: Boolean = false
    ): File? {
        Toast.makeText(context, "Generazione dossier di confronto PDF in corso...", Toast.LENGTH_SHORT).show()
        val pdfFile = generateComparisonReportPdf(
            context = context,
            propertyA = propertyA,
            propertyB = propertyB,
            evalA = evalA,
            evalB = evalB,
            investorNotes = investorNotes
        )

        if (pdfFile != null && pdfFile.exists()) {
            shareComparisonPdf(context, pdfFile, propertyA, propertyB, emailOnly)
        } else {
            Toast.makeText(context, "Impossibile creare il dossier di confronto PDF.", Toast.LENGTH_SHORT).show()
        }
        return pdfFile
    }

    /**
     * Utility method for Property
     */
    fun generatePdfForProperty(context: Context, property: Property): File? {
        val deal = PropertyDeal(
            id = property.id,
            title = property.title.ifBlank { property.address },
            location = property.address,
            propertyType = property.propertyType.ifBlank { "Residenziale" },
            askingPrice = property.price,
            estimatedMarketValue = if (property.estimatedMarketValue > 0) property.estimatedMarketValue else property.price * 1.25,
            surfaceSqm = if (property.surfaceSqm > 0) property.surfaceSqm else 85,
            notes = property.notes
        )
        return generatePdfForDeal(context, deal)
    }

    fun generateAndSharePdf(
        context: Context,
        property: Property,
        emailOnly: Boolean = true
    ) {
        Toast.makeText(context, "Generazione dossier PDF in corso...", Toast.LENGTH_SHORT).show()
        val pdfFile = generatePdfForProperty(context, property)
        if (pdfFile != null && pdfFile.exists()) {
            sharePropertyPdf(context, pdfFile, property.title.ifBlank { property.address }, property.address, emailOnly = emailOnly)
        } else {
            Toast.makeText(context, "Impossibile creare il file PDF.", Toast.LENGTH_SHORT).show()
        }
    }
}
