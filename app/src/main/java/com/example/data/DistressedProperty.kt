package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "distressed_properties")
data class DistressedProperty(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val price: Double = 0.0,
    val estimatedValue: Double = 0.0,
    val distressLevel: String = "Medium",
    val status: String = "Active",
    val category: String = "Residential",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geolocationPrecision: String = GeolocationPrecision.UNKNOWN.name,
    val imageUrl: String? = null,
    val notes: String = "",
    val estimatedArv: Double? = null,
    val aiAnalysisReport: String? = null,
    val provenance: String = DataProvenance.LEGACY_UNKNOWN.name,
    val sourceRef: String? = null,       // id o URL della fonte, se esiste
    val retrievedAt: Long? = null,       // quando il dato è stato ottenuto
    val confidence: Double? = null,      // 0.0-1.0, null se non misurabile
    val evidenceRef: String? = null,     // riferimento verificabile (URL, RGE, doc)
    val lastUpdated: Long = System.currentTimeMillis()
)
