package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scraper_sources")
data class ScraperSource(
    @PrimaryKey val id: String, // e.g. "quimmo", "iqera", "re_impresa", "bper_leasing", "demanio", "astalegale", "mps_bandi"
    val name: String,
    val url: String,
    val robotsStatus: String, // "CONSENTITO", "NESSUN_ROBOTS", "DA_VERIFICARE", "IRRAGGIUNGIBILE"
    val configStatus: String, // "CONSENTITO", "DA_VERIFICARE", "SOLO_CONTATTO"
    val activeParserRulesJson: String, // CSS / JSON / XPath selectors config
    val totalDealsFound: Int = 0,
    val lastVerifiedTimestamp: Long = System.currentTimeMillis()
)
