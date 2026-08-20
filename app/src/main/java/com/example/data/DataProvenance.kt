package com.example.data

enum class DataProvenance(val label: String, val isTrustworthy: Boolean) {
    REAL_SOURCE("Fonte verificata", true),
    USER_ENTERED("Inserito dall'utente", true),
    AI_INFERRED("Stima AI non verificata", false),
    CURATED_FALLBACK("Valore interno di riferimento", false),
    SYNTHETIC_DEMO("Dato dimostrativo", false),
    LEGACY_UNKNOWN("Provenienza sconosciuta", false);

    companion object {
        fun fromString(value: String?): DataProvenance {
            if (value.isNullOrBlank()) return LEGACY_UNKNOWN
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                LEGACY_UNKNOWN
            }
        }
    }
}
