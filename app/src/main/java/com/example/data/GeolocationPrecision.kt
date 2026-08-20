package com.example.data

enum class GeolocationPrecision(val label: String, val description: String) {
    SOURCE_COORDINATES("Coordinate fonte", "Coordinate precise fornite dalla fonte dell'annuncio"),
    ADDRESS_GEOCODED("Indirizzo geocodificato", "Coordinate ricavate dalla geocodifica dell'indirizzo"),
    CITY_CENTROID("Centroide comune", "Centroide del comune: utile per la mappa, NON è la posizione dell'immobile"),
    UNKNOWN("Sconosciuta", "Nessuna coordinata o posizione geografica disponibile");

    companion object {
        fun fromString(value: String?): GeolocationPrecision {
            if (value.isNullOrBlank()) return UNKNOWN
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                UNKNOWN
            }
        }
    }
}
