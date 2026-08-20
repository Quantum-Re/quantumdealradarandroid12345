package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_history")
data class PriceHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dealId: Long,
    val price: Double,
    val dateRecorded: String,
    val eventLabel: String // e.g. "1st Base Price", "Ribasso -20%", "Current Offer"
)
