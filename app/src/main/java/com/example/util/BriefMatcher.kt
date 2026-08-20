package com.example.util

import com.example.data.InvestorProfile
import com.example.data.PropertyDeal

data class BriefMatchResult(
    val score: Int,             // 0 to 100
    val isTargetMatch: Boolean,  // score >= 60
    val reasons: List<String>
)

object BriefMatcher {
    fun evaluate(deal: PropertyDeal, profile: InvestorProfile?): BriefMatchResult {
        if (profile == null || !profile.briefActive) {
            return BriefMatchResult(score = 0, isTargetMatch = false, reasons = listOf("Brief non attivo"))
        }

        var score = 0
        val reasons = mutableListOf<String>()

        // 1. Budget check (Max 25 pts)
        if (deal.askingPrice <= profile.briefMaxBudget) {
            score += 25
            reasons.add("In Budget (€${String.format("%,.0f", deal.askingPrice)} ≤ €${String.format("%,.0f", profile.briefMaxBudget)})")
        } else {
            val overage = deal.askingPrice - profile.briefMaxBudget
            reasons.add("Fuori Budget (+€${String.format("%,.0f", overage)})")
        }

        // 2. Minimum Discount check (Max 25 pts)
        if (deal.discountPercent >= profile.briefMinDiscountPercent) {
            score += 25
            reasons.add("Sconto Rispetto Target (-${deal.discountPercent}% ≥ -${profile.briefMinDiscountPercent}%)")
        } else {
            reasons.add("Sconto Inferiore al Brief (-${deal.discountPercent}%)")
        }

        // 3. Location check (Max 30 pts)
        val targetCities = profile.briefTargetLocations.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val dealLocLower = deal.location.lowercase()
        val matchCity = targetCities.find { dealLocLower.contains(it) || it.contains(dealLocLower) }
        if (matchCity != null) {
            score += 30
            reasons.add("Città/Area Target (${matchCity.replaceFirstChar { it.uppercase() }})")
        } else if (targetCities.isEmpty()) {
            score += 15
            reasons.add("Nessuna limitazione geografica")
        } else {
            reasons.add("Area fuori target preferito")
        }

        // 4. Property/Deal Type check (Max 20 pts)
        val targetTypes = profile.briefPropertyTypes.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val dealTypeLower = deal.propertyType.lowercase() + " " + deal.title.lowercase()
        val matchType = targetTypes.find { dealTypeLower.contains(it) || it.contains(dealTypeLower) }
        if (matchType != null) {
            score += 20
            reasons.add("Tipologia Target (${matchType.replaceFirstChar { it.uppercase() }})")
        } else if (targetTypes.isEmpty()) {
            score += 10
        } else {
            reasons.add("Tipologia secondaria")
        }

        return BriefMatchResult(
            score = score,
            isTargetMatch = score >= 60,
            reasons = reasons
        )
    }
}
