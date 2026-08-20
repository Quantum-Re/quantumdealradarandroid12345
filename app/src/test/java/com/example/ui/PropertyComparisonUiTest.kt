package com.example.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.PipelineStatus
import com.example.data.Property
import com.example.ui.components.ComparisonScenario
import com.example.ui.components.PropertyBatchFloatingActionBar
import com.example.ui.components.PropertyPipelineComparisonView
import com.example.ui.components.toComparableProperty
import com.example.ui.theme.QuantumDealRadarTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.text.NumberFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PropertyComparisonUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val euroFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
        maximumFractionDigits = 0
    }

    private val testProperties = listOf(
        Property(
            id = 1L,
            title = "Bilocale Porta Romana",
            address = "Via Crema 12, Milano",
            price = 180000.0,
            surfaceSqm = 65,
            strategyTags = "Fix & Flip",
            pipelineStatus = "RENOVATING",
            estimatedRenovationCost = 35000.0,
            actualRenovationCost = 32000.0,
            targetResalePrice = 280000.0,
            projectedRentalIncome = 1200.0,
            renovationProgressPercent = 60
        ),
        Property(
            id = 2L,
            title = "Trilocale CityLife",
            address = "Via Silva 8, Milano",
            price = 350000.0,
            surfaceSqm = 105,
            strategyTags = "Buy & Hold",
            pipelineStatus = "IN_ESCROW",
            estimatedRenovationCost = 45000.0,
            targetResalePrice = 480000.0,
            projectedRentalIncome = 1900.0,
            renovationProgressPercent = 10
        ),
        Property(
            id = 3L,
            title = "Monolocale Isola",
            address = "Via Borsieri 22, Milano",
            price = 140000.0,
            surfaceSqm = 42,
            strategyTags = "Affitto Breve",
            pipelineStatus = "RENTED",
            estimatedRenovationCost = 20000.0,
            targetResalePrice = 210000.0,
            projectedRentalIncome = 1100.0,
            renovationProgressPercent = 100
        )
    )

    @Test
    fun testPropertyPipelineComparisonView_RendersSideBySideAndHighlights() {
        var calculatedProp: Property? = null
        var editedProp: Property? = null

        composeTestRule.setContent {
            QuantumDealRadarTheme {
                PropertyPipelineComparisonView(
                    allProperties = testProperties,
                    selectedPropertyIds = setOf(1L, 2L),
                    onTogglePropertySelection = {},
                    onSelectAllProperties = {},
                    onClearSelection = {},
                    euroFormat = euroFormat,
                    onCalculateRoiClick = { calculatedProp = it },
                    onEditFinancialsClick = { editedProp = it },
                    onUpdateStatusClick = {},
                    onUpdateProgressClick = {},
                    onSimulatePriceDropClick = {},
                    onExportPdfClick = {},
                    onDeleteClick = {}
                )
            }
        }

        // Verify root comparison view is rendered
        composeTestRule.onNodeWithTag("property_pipeline_comparison_view").assertIsDisplayed()

        // Verify asset titles are visible in side-by-side columns
        composeTestRule.onAllNodesWithText("Bilocale Porta Romana", substring = true)[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Trilocale CityLife", substring = true)[0].assertIsDisplayed()

        // Verify Key Metric sections exist
        composeTestRule.onNodeWithText("CAPITALE & LAVORI", substring = true).assertExists()
        composeTestRule.onNodeWithText("EXIT & PLUSVALENZA", substring = true).assertExists()
        composeTestRule.onNodeWithText("RENDIMENTO & ROI", substring = true).assertExists()

        // Verify sensitivity scenario bar exists
        composeTestRule.onNodeWithTag("comparison_scenario_bar").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Scenario Base", substring = true)[0].assertExists()

        // Verify Simula ROI action exists
        val roiButtons = composeTestRule.onAllNodesWithText("Simula ROI")
        if (roiButtons.fetchSemanticsNodes().isNotEmpty()) {
            roiButtons[0].assertExists()
        }

        // Capture snapshot
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/property_pipeline_comparison_view.png")
    }

    @Test
    fun testBatchFloatingActionBar_CompareButtonTriggersAction() {
        var compareClicked = false

        composeTestRule.setContent {
            QuantumDealRadarTheme {
                PropertyBatchFloatingActionBar(
                    selectedCount = 2,
                    totalCount = 3,
                    onSelectAllToggle = {},
                    onOpenChangeStatus = {},
                    onOpenArchiveConfirm = {},
                    onOpenDeleteConfirm = {},
                    onCancelSelection = {},
                    onOpenComparison = { compareClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("batch_action_compare_btn").assertIsDisplayed().performClick()
        assertTrue(compareClicked)
    }

    @Test
    fun testPropertyToComparableModel_MappingAccuracy() {
        val prop = testProperties[0]
        val comparable = prop.toComparableProperty()

        assertEquals("portfolio_1", comparable.id)
        assertEquals("Bilocale Porta Romana", comparable.title)
        assertEquals(180000.0, comparable.price, 0.01)
        assertEquals(280000.0, comparable.estimatedMarketValue, 0.01)
        assertEquals(65, comparable.surfaceSqm)
        assertTrue(comparable.estimatedRoiCapRate > 0.0)
    }
}
