package com.example.util

import java.text.NumberFormat
import java.util.Locale

enum class AppCurrency(
    val code: String,
    val symbol: String,
    val rateFromEur: Double,
    val displayName: String,
    val flagEmoji: String = "🌐",
    val isSymbolPrefix: Boolean = true
) {
    EUR("EUR", "€", 1.0, "Euro (€)", "🇪🇺", isSymbolPrefix = false),
    USD("USD", "$", 1.09, "US Dollar ($)", "🇺🇸", isSymbolPrefix = true),
    GBP("GBP", "£", 0.86, "British Pound (£)", "🇬🇧", isSymbolPrefix = true),
    CHF("CHF", "CHF", 0.96, "Swiss Franc (CHF)", "🇨🇭", isSymbolPrefix = false),
    CAD("CAD", "CA$", 1.48, "Canadian Dollar (CA$)", "🇨🇦", isSymbolPrefix = true),
    AUD("AUD", "AU$", 1.65, "Australian Dollar (AU$)", "🇦🇺", isSymbolPrefix = true),
    AED("AED", "AED", 4.01, "UAE Dirham (AED)", "🇦🇪", isSymbolPrefix = false),
    JPY("JPY", "¥", 164.0, "Japanese Yen (¥)", "🇯🇵", isSymbolPrefix = true)
}

enum class AreaUnit(
    val code: String,
    val symbol: String,
    val factorFromSqm: Double, // 1 m² = 1.0 m², 1 m² = 10.7639 sq ft
    val displayName: String
) {
    SQ_METERS("SQM", "m²", 1.0, "Square Meters (m²)"),
    SQ_FEET("SQFT", "sq ft", 10.7639, "Square Feet (sq ft)")
}

data class ConvertedPropertyMetrics(
    val currency: AppCurrency,
    val unit: AreaUnit,
    val surfaceInSqm: Double,
    val surfaceInUnit: Double,
    val basePriceEur: Double,
    val baseArvEur: Double,
    val convertedPrice: Double,
    val convertedArv: Double,
    val convertedRenovationCost: Double,
    val convertedNetProfit: Double,
    val pricePerUnit: Double,
    val arvPerUnit: Double,
    val profitPerUnit: Double,
    val roiPercent: Double
) {
    fun formatCurrencyValue(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = 0
        return if (currency.isSymbolPrefix) {
            "${currency.symbol}${formatter.format(amount.toInt())}"
        } else {
            "${formatter.format(amount.toInt())} ${currency.symbol}"
        }
    }

    fun formatUnitPrice(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = 1
        return if (currency.isSymbolPrefix) {
            "${currency.symbol}${formatter.format(amount.toInt())}/${unit.symbol}"
        } else {
            "${formatter.format(amount.toInt())} ${currency.symbol}/${unit.symbol}"
        }
    }
}

object CurrencyUnitConverter {

    /**
     * Converts property values given base EUR price, estimated ARV, surface area in SQM, target currency and area unit.
     */
    fun computeMetrics(
        priceEur: Double,
        arvEur: Double?,
        surfaceSqm: Double = 100.0,
        currency: AppCurrency = AppCurrency.EUR,
        unit: AreaUnit = AreaUnit.SQ_METERS,
        customRate: Double? = null
    ): ConvertedPropertyMetrics {
        val safePrice = priceEur.coerceAtLeast(0.0)
        val calculatedArv = arvEur ?: if (safePrice > 0) safePrice * 1.35 else 0.0
        val safeSurfaceSqm = surfaceSqm.coerceAtLeast(10.0)

        val renoCostEur = safePrice * 0.15
        val netProfitEur = calculatedArv - safePrice - renoCostEur
        val roi = if (safePrice + renoCostEur > 0) (netProfitEur / (safePrice + renoCostEur)) * 100.0 else 0.0

        val rate = customRate ?: currency.rateFromEur
        val convertedPrice = safePrice * rate
        val convertedArv = calculatedArv * rate
        val convertedReno = renoCostEur * rate
        val convertedProfit = netProfitEur * rate

        val surfaceInUnit = safeSurfaceSqm * unit.factorFromSqm

        val pricePerUnit = convertedPrice / surfaceInUnit
        val arvPerUnit = convertedArv / surfaceInUnit
        val profitPerUnit = convertedProfit / surfaceInUnit

        return ConvertedPropertyMetrics(
            currency = currency,
            unit = unit,
            surfaceInSqm = safeSurfaceSqm,
            surfaceInUnit = surfaceInUnit,
            basePriceEur = safePrice,
            baseArvEur = calculatedArv,
            convertedPrice = convertedPrice,
            convertedArv = convertedArv,
            convertedRenovationCost = convertedReno,
            convertedNetProfit = convertedProfit,
            pricePerUnit = pricePerUnit,
            arvPerUnit = arvPerUnit,
            profitPerUnit = profitPerUnit,
            roiPercent = roi
        )
    }
}
