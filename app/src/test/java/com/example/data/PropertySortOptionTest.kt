package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PropertySortOptionTest {

    private val prop1 = Property(
        id = 1,
        title = "Via Garibaldi 10",
        address = "Milano",
        price = 150_000.0,
        targetResalePrice = 220_000.0,
        estimatedRenovationCost = 30_000.0,
        pipelineStatus = PipelineStatus.ANALYZED.key,
        createdAt = 1000L
    )

    private val prop2 = Property(
        id = 2,
        title = "Corso Buenos Aires 45",
        address = "Milano",
        price = 250_000.0,
        targetResalePrice = 350_000.0,
        estimatedRenovationCost = 40_000.0,
        pipelineStatus = PipelineStatus.RENOVATING.key,
        createdAt = 3000L
    )

    private val prop3 = Property(
        id = 3,
        title = "Via Torino 12",
        address = "Milano",
        price = 90_000.0,
        targetResalePrice = 160_000.0,
        estimatedRenovationCost = 20_000.0,
        pipelineStatus = PipelineStatus.IN_ESCROW.key,
        createdAt = 2000L
    )

    private val properties = listOf(prop1, prop2, prop3)

    @Test
    fun testSortByDateAddedDesc() {
        val sorted = properties.sortProperties(PropertySortOption.DATE_ADDED_DESC)
        assertEquals(listOf(prop2.id, prop3.id, prop1.id), sorted.map { it.id })
    }

    @Test
    fun testSortByDateAddedAsc() {
        val sorted = properties.sortProperties(PropertySortOption.DATE_ADDED_ASC)
        assertEquals(listOf(prop1.id, prop3.id, prop2.id), sorted.map { it.id })
    }

    @Test
    fun testSortByStatusWorkflow() {
        val sorted = properties.sortProperties(PropertySortOption.STATUS_WORKFLOW)
        // Workflow order: IN_ESCROW (1) -> RENOVATING (2) -> LISTED (3) -> RENTED (4) -> SOLD (5) -> ANALYZED (6) -> ARCHIVED (7)
        assertEquals(listOf(prop3.id, prop2.id, prop1.id), sorted.map { it.id })
    }

    @Test
    fun testSortByRoiDesc() {
        // ROI prop3: (160k - 110k)/110k = 45.45%
        // ROI prop2: (350k - 290k)/290k = 20.69%
        // ROI prop1: (220k - 180k)/180k = 22.22%
        val sorted = properties.sortProperties(PropertySortOption.ROI_DESC)
        assertEquals(prop3.id, sorted.first().id)
    }

    @Test
    fun testSortByPriceAsc() {
        val sorted = properties.sortProperties(PropertySortOption.PRICE_ASC)
        assertEquals(listOf(prop3.id, prop1.id, prop2.id), sorted.map { it.id })
    }
}
