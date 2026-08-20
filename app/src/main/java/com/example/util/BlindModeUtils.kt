package com.example.util

import com.example.data.InvestorProfile
import com.example.data.PropertyDeal

object BlindModeUtils {

    fun isDealBlind(dealId: Long, profile: InvestorProfile?): Boolean {
        if (profile == null) return true
        return !profile.isDealUnlocked(dealId)
    }

    fun getMaskedTitle(deal: PropertyDeal, isBlind: Boolean): String {
        if (!isBlind) return deal.title
        val type = deal.propertyType.ifBlank { "Immobile a Reddito / Opportunità" }
        val city = deal.location.split("(").firstOrNull()?.trim() ?: "Milano"
        return "🔒 [OPERAZIONE BLIND] $type in Zona Strategica • $city"
    }

    fun getMaskedLocation(deal: PropertyDeal, isBlind: Boolean): String {
        if (!isBlind) return deal.location
        val parts = deal.location.split("-", ",")
        val city = parts.firstOrNull()?.trim() ?: deal.location
        return "$city • Macro-Area Riservata (Dati Protetti)"
    }

    fun getMaskedAddress(deal: PropertyDeal, isBlind: Boolean): String {
        if (!isBlind) return "Via/Piazza specifica registrata nel fascicolo peritale"
        return "Via *******, Civico ** • 🔒 Indirizzo Riservato agli Investitori Autorizzati"
    }

    fun getMaskedCadastralInfo(isBlind: Boolean): String {
        if (!isBlind) return "Foglio 142, Particella 3819, Subalterno 12 - Categoria A/3 - Rendita €840,00"
        return "Foglio ***, Particella ****, Sub. ** - Categoria A/* (🔒 Dati Catastali Protetti)"
    }

    fun getMaskedProcedureNumber(deal: PropertyDeal, isBlind: Boolean): String {
        if (!isBlind) return "Procedura Esecutiva R.G.E. n. 482/2025 - Tribunale di Competenza"
        return "Procedura Esecutiva R.G.E. n. ***/*** - 🔒 Tribunale Riservato"
    }

    fun getMaskedContact(isBlind: Boolean): Pair<String, String> {
        if (!isBlind) return Pair("Avv. Dott. M. Valenti (Delegato alla Vendita)", "+39 02 8945 1100")
        return Pair("🔒 Custode Giudiziario / Referente Riservato", "+39 *** *** ****")
    }

    fun getTeaserRoiMetrics(deal: PropertyDeal): String {
        val estimatedRoiMin = (deal.estimatedCapRate * 1.8).coerceAtLeast(14.0).toInt()
        val estimatedRoiMax = (deal.estimatedCapRate * 2.5).coerceAtLeast(22.0).toInt()
        return "ROI Atteso: $estimatedRoiMin% - $estimatedRoiMax% • Sconto stimato: ~${deal.discountPercent}%"
    }
}
