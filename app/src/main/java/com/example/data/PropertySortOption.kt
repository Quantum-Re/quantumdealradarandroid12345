package com.example.data

enum class PropertySortCategory(val labelIt: String, val labelEn: String) {
    OPPORTUNITY("Opportunity & Sconto Mercato", "Opportunity & Discount"),
    DATE("Data Inserimento", "Date Added"),
    STATUS("Stato Pipeline", "Pipeline Status"),
    ROI("ROI & Redditività", "Estimated ROI"),
    PRICE("Prezzo & Capitale", "Price & Capital")
}

enum class PropertySortOption(
    val key: String,
    val title: String,
    val subtitle: String,
    val category: PropertySortCategory
) {
    OPPORTUNITY_SCORE_DESC(
        key = "OPPORTUNITY_SCORE_DESC",
        title = "🔥 Opportunity Score: Più alto",
        subtitle = "Immobili più sottoquotati rispetto ai valori medi di mercato live",
        category = PropertySortCategory.OPPORTUNITY
    ),
    DATE_ADDED_DESC(
        key = "DATE_ADDED_DESC",
        title = "Data: Più recenti",
        subtitle = "Visualizza prima gli immobili aggiunti di recente",
        category = PropertySortCategory.DATE
    ),
    DATE_ADDED_ASC(
        key = "DATE_ADDED_ASC",
        title = "Data: Meno recenti",
        subtitle = "Visualizza prima gli immobili meno recenti",
        category = PropertySortCategory.DATE
    ),
    STATUS_WORKFLOW(
        key = "STATUS_WORKFLOW",
        title = "Stato: Avanzamento Pipeline",
        subtitle = "In trattativa ➔ In cantiere ➔ In vendita ➔ A reddito ➔ Venduto ➔ Analizzato",
        category = PropertySortCategory.STATUS
    ),
    STATUS_NAME(
        key = "STATUS_NAME",
        title = "Stato: Alfabetico (A - Z)",
        subtitle = "Ordinamento in base al nome dello stato pipeline",
        category = PropertySortCategory.STATUS
    ),
    ROI_DESC(
        key = "ROI_DESC",
        title = "ROI Stimato: Più alto",
        subtitle = "Dal rendimento percentuale più elevato al più basso",
        category = PropertySortCategory.ROI
    ),
    ROI_ASC(
        key = "ROI_ASC",
        title = "ROI Stimato: Più basso",
        subtitle = "Dal rendimento percentuale più contenuto al più alto",
        category = PropertySortCategory.ROI
    ),
    PROFIT_DESC(
        key = "PROFIT_DESC",
        title = "Profitto Netto Atteso (€)",
        subtitle = "Dal margine lordo/netto maggiore in Euro",
        category = PropertySortCategory.ROI
    ),
    PRICE_ASC(
        key = "PRICE_ASC",
        title = "Prezzo Acquisto: Crescente",
        subtitle = "Dall'immobile più economico a quello più costoso",
        category = PropertySortCategory.PRICE
    ),
    PRICE_DESC(
        key = "PRICE_DESC",
        title = "Prezzo Acquisto: Decrescente",
        subtitle = "Dall'investimento più elevato a quello minore",
        category = PropertySortCategory.PRICE
    );

    companion object {
        val DEFAULT = DATE_ADDED_DESC

        fun fromKey(key: String): PropertySortOption {
            return values().find { it.key.equals(key, ignoreCase = true) } ?: DEFAULT
        }
    }
}

fun List<Property>.sortProperties(
    sortOption: PropertySortOption,
    opportunityScoreMap: Map<Long, Int> = emptyMap()
): List<Property> {
    return when (sortOption) {
        PropertySortOption.OPPORTUNITY_SCORE_DESC -> {
            sortedWith(
                compareByDescending<Property> { opportunityScoreMap[it.id] ?: 50 }
                    .thenByDescending { it.projectedRoiPercent }
                    .thenByDescending { it.createdAt }
            )
        }
        PropertySortOption.DATE_ADDED_DESC -> {
            sortedWith(
                compareByDescending<Property> { it.createdAt }
                    .thenByDescending { it.id }
            )
        }
        PropertySortOption.DATE_ADDED_ASC -> {
            sortedWith(
                compareBy<Property> { it.createdAt }
                    .thenBy { it.id }
            )
        }
        PropertySortOption.STATUS_WORKFLOW -> {
            val workflowPriority = mapOf(
                PipelineStatus.IN_ESCROW to 1,
                PipelineStatus.RENOVATING to 2,
                PipelineStatus.LISTED to 3,
                PipelineStatus.RENTED to 4,
                PipelineStatus.SOLD to 5,
                PipelineStatus.ANALYZED to 6,
                PipelineStatus.ARCHIVED to 7
            )
            sortedWith(
                compareBy<Property> { workflowPriority[it.currentStatus] ?: 99 }
                    .thenByDescending { it.projectedRoiPercent }
                    .thenByDescending { it.createdAt }
            )
        }
        PropertySortOption.STATUS_NAME -> {
            sortedWith(
                compareBy<Property> { it.currentStatus.labelIt }
                    .thenByDescending { it.createdAt }
            )
        }
        PropertySortOption.ROI_DESC -> {
            sortedWith(
                compareByDescending<Property> { it.projectedRoiPercent }
                    .thenByDescending { it.projectedProfit }
                    .thenByDescending { it.createdAt }
            )
        }
        PropertySortOption.ROI_ASC -> {
            sortedWith(
                compareBy<Property> { it.projectedRoiPercent }
                    .thenBy { it.projectedProfit }
                    .thenByDescending { it.createdAt }
            )
        }
        PropertySortOption.PROFIT_DESC -> {
            sortedWith(
                compareByDescending<Property> { it.projectedProfit }
                    .thenByDescending { it.projectedRoiPercent }
            )
        }
        PropertySortOption.PRICE_ASC -> {
            sortedWith(
                compareBy<Property> { it.price }
                    .thenByDescending { it.projectedRoiPercent }
            )
        }
        PropertySortOption.PRICE_DESC -> {
            sortedWith(
                compareByDescending<Property> { it.price }
                    .thenByDescending { it.projectedRoiPercent }
            )
        }
    }
}
