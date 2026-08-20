package com.example.ui

import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.FakePropertyDao
import com.example.data.PipelineStatus
import com.example.data.Property
import com.example.data.PropertyRepository
import com.example.ui.components.BatchArchiveConfirmDialog
import com.example.ui.components.BatchDeleteConfirmDialog
import com.example.ui.components.PropertyBatchFloatingActionBar
import com.example.ui.theme.QuantumDealRadarTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PropertyBatchSelectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val sampleProperties = listOf(
        Property(
            id = 1L,
            title = "Bilocale Milano Isola",
            address = "Via Borsieri 12, Milano",
            price = 180000.0,
            estimatedRenovationCost = 25000.0,
            targetResalePrice = 270000.0,
            pipelineStatus = PipelineStatus.ANALYZED.key,
            createdAt = System.currentTimeMillis()
        ),
        Property(
            id = 2L,
            title = "Trilocale Torino Centro",
            address = "Via Roma 45, Torino",
            price = 140000.0,
            estimatedRenovationCost = 35000.0,
            targetResalePrice = 230000.0,
            pipelineStatus = PipelineStatus.RENOVATING.key,
            createdAt = System.currentTimeMillis()
        ),
        Property(
            id = 3L,
            title = "Villa Bologna Colli",
            address = "Via Panoramica 8, Bologna",
            price = 320000.0,
            estimatedRenovationCost = 50000.0,
            targetResalePrice = 460000.0,
            pipelineStatus = PipelineStatus.IN_ESCROW.key,
            createdAt = System.currentTimeMillis()
        )
    )

    @Test
    fun testViewModel_MultiSelectionAndSelectAll() = runTest {
        val fakeDao = FakePropertyDao(sampleProperties)
        val repository = PropertyRepository(fakeDao, CoroutineScope(SupervisorJob() + testDispatcher), testDispatcher)
        val viewModel = PropertyViewModel(ApplicationProvider.getApplicationContext(), repository)
        testScheduler.advanceUntilIdle()

        // Initial state: not active, empty selection
        assertFalse(viewModel.isSelectionModeActive.value)
        assertTrue(viewModel.selectedPropertyIds.value.isEmpty())

        // Toggle selection mode
        viewModel.setSelectionMode(true)
        assertTrue(viewModel.isSelectionModeActive.value)

        // Toggle property 1 and 2
        viewModel.togglePropertySelection(1L)
        viewModel.togglePropertySelection(2L)
        assertEquals(setOf(1L, 2L), viewModel.selectedPropertyIds.value)

        // Toggle property 1 again (unselect)
        viewModel.togglePropertySelection(1L)
        assertEquals(setOf(2L), viewModel.selectedPropertyIds.value)

        // Select All
        viewModel.selectAllProperties(sampleProperties)
        assertEquals(setOf(1L, 2L, 3L), viewModel.selectedPropertyIds.value)

        // Clear selection
        viewModel.clearSelection()
        assertTrue(viewModel.selectedPropertyIds.value.isEmpty())
        assertFalse(viewModel.isSelectionModeActive.value)
    }

    @Test
    fun testViewModel_BatchOperations() = runTest {
        val fakeDao = FakePropertyDao(sampleProperties)
        val repository = PropertyRepository(fakeDao, CoroutineScope(SupervisorJob() + testDispatcher), testDispatcher)
        val viewModel = PropertyViewModel(ApplicationProvider.getApplicationContext(), repository)
        testScheduler.advanceUntilIdle()

        // Select properties 1 and 2
        viewModel.togglePropertySelection(1L)
        viewModel.togglePropertySelection(2L)

        // 1. Batch status update
        viewModel.batchUpdatePipelineStatus(PipelineStatus.RENTED)
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.selectedPropertyIds.value.isEmpty())
        val updatedProps = repository.allProperties.first()
        assertEquals(PipelineStatus.RENTED.key, updatedProps.find { it.id == 1L }?.pipelineStatus)
        assertEquals(PipelineStatus.RENTED.key, updatedProps.find { it.id == 2L }?.pipelineStatus)

        // Select property 3 for archiving
        viewModel.togglePropertySelection(3L)
        viewModel.batchArchiveSelectedProperties()
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.selectedPropertyIds.value.isEmpty())
        val archivedProps = repository.allProperties.first()
        assertEquals(PipelineStatus.ARCHIVED.key, archivedProps.find { it.id == 3L }?.pipelineStatus)

        // Select property 2 for deletion
        viewModel.togglePropertySelection(2L)
        viewModel.batchDeleteSelectedProperties()
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.selectedPropertyIds.value.isEmpty())
        val remainingProps = repository.allProperties.first()
        assertNull(remainingProps.find { it.id == 2L })
    }

    @Test
    fun testBatchFloatingActionBar_RendersAndCapturesScreenshot() {
        var statusClicked = false
        var archiveClicked = false
        var deleteClicked = false
        var cancelClicked = false

        composeTestRule.setContent {
            QuantumDealRadarTheme {
                PropertyBatchFloatingActionBar(
                    selectedCount = 2,
                    totalCount = 3,
                    onSelectAllToggle = {},
                    onOpenChangeStatus = { statusClicked = true },
                    onOpenArchiveConfirm = { archiveClicked = true },
                    onOpenDeleteConfirm = { deleteClicked = true },
                    onCancelSelection = { cancelClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("batch_floating_action_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("immobili selezionati", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("batch_action_change_status_btn").assertIsDisplayed().performClick()
        assertTrue(statusClicked)

        composeTestRule.onNodeWithTag("batch_action_archive_btn").assertIsDisplayed().performClick()
        assertTrue(archiveClicked)

        composeTestRule.onNodeWithTag("batch_action_delete_btn").assertIsDisplayed().performClick()
        assertTrue(deleteClicked)

        composeTestRule.onNodeWithTag("batch_cancel_selection_btn").assertIsDisplayed().performClick()
        assertTrue(cancelClicked)

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/property_batch_action_bar.png")
    }

    @Test
    fun testBatchArchiveConfirmDialog_RenderCorrectly() {
        var archiveConfirmed = false

        composeTestRule.setContent {
            QuantumDealRadarTheme {
                BatchArchiveConfirmDialog(
                    selectedCount = 3,
                    onConfirm = { archiveConfirmed = true },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("batch_archive_confirm_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_batch_archive_btn").assertIsDisplayed().performClick()
        assertTrue(archiveConfirmed)
    }

    @Test
    fun testBatchDeleteConfirmDialog_RenderCorrectly() {
        var deleteConfirmed = false

        composeTestRule.setContent {
            QuantumDealRadarTheme {
                BatchDeleteConfirmDialog(
                    selectedCount = 2,
                    onConfirm = { deleteConfirmed = true },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("batch_delete_confirm_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_batch_delete_btn").assertIsDisplayed().performClick()
        assertTrue(deleteConfirmed)
    }
}
