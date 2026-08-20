package com.example.util

import com.example.data.PropertyDeal
import kotlin.math.roundToInt

sealed class DealValidationResult {
    data class Valid(val sanitizedDeal: PropertyDeal, val warnings: List<String> = emptyList()) : DealValidationResult()
    data class Invalid(val reasons: List<String>) : DealValidationResult()
}

object DealDataValidator {
    private const val MIN_PRICE = 1_000.0
    private const val MAX_PRICE = 50_000_000.0
    private const val MIN_SURFACE_SQM = 10
    private const val MAX_SURFACE_SQM = 25_000
    private const val MIN_PRICE_PER_SQM = 100.0
    private const val MAX_PRICE_PER_SQM = 30_000.0

    fun validateAndSanitize(deal: PropertyDeal): DealValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 1. Title validation
        val sanitizedTitle = deal.title.trim().replace(Regex("[<>\"'\n\r]"), "")
        if (sanitizedTitle.length < 3) {
            errors.add("Titolo troppo breve o non valido")
        }

        // 2. Price validation
        if (deal.askingPrice < MIN_PRICE) {
            errors.add("Prezzo richiesto non plausibile (< 1.000 €)")
        } else if (deal.askingPrice > MAX_PRICE) {
            errors.add("Prezzo richiesto fuori scala (> 50.000.000 €)")
        }

        // 3. Surface area validation
        val sanitizedSurface = deal.surfaceSqm.coerceIn(1, 100_000)
        if (deal.surfaceSqm < MIN_SURFACE_SQM) {
            warnings.add("Superficie insolitamente ridotta (< 10 mq)")
        } else if (deal.surfaceSqm > MAX_SURFACE_SQM) {
            warnings.add("Superficie eccezionale (> 25.000 mq)")
        }

        // 4. Price per Sqm plausibility check
        if (sanitizedSurface > 0 && deal.askingPrice >= MIN_PRICE) {
            val pricePerSqm = deal.askingPrice / sanitizedSurface
            if (pricePerSqm < MIN_PRICE_PER_SQM) {
                warnings.add("Prezzo al mq insolitamente basso (${pricePerSqm.toInt()} €/mq)")
            } else if (pricePerSqm > MAX_PRICE_PER_SQM) {
                warnings.add("Prezzo al mq elevato (${pricePerSqm.toInt()} €/mq)")
            }
        }

        // 5. Market value & discount sanity calculation
        val marketValue = if (deal.estimatedMarketValue <= 0.0) {
            deal.askingPrice * 1.25 // Default fallback valuation if uncalculated
        } else {
            deal.estimatedMarketValue
        }

        val calculatedDiscount = if (marketValue > deal.askingPrice) {
            (((marketValue - deal.askingPrice) / marketValue) * 100.0).roundToInt().coerceIn(0, 95)
        } else {
            0
        }

        // 6. Cap Rate sanity
        val estimatedCapRate = if (deal.estimatedCapRate <= 0.0 || deal.estimatedCapRate > 50.0) {
            // Recalculate realistic cap rate based on Italian standard yields (4% to 12%)
            val simulatedAnnualRent = marketValue * 0.06
            ((simulatedAnnualRent / deal.askingPrice) * 100.0).coerceIn(2.0, 25.0)
        } else {
            deal.estimatedCapRate
        }

        if (errors.isNotEmpty()) {
            return DealValidationResult.Invalid(errors)
        }

        val sanitizedDeal = deal.copy(
            title = sanitizedTitle,
            location = deal.location.trim(),
            surfaceSqm = sanitizedSurface,
            estimatedMarketValue = marketValue,
            discountPercent = calculatedDiscount,
            estimatedCapRate = Math.round(estimatedCapRate * 10.0) / 10.0
        )

        return DealValidationResult.Valid(sanitizedDeal, warnings)
    }
}
