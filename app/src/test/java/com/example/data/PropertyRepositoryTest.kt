package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.PropertyViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fake implementation of [PropertyDao] for isolated unit testing
 * of [PropertyRepository] and [PropertyViewModel].
 */
class FakePropertyDao(
    initialList: List<Property> = emptyList()
) : PropertyDao() {

    private val _propertiesFlow = MutableStateFlow(initialList)

    override fun getAllPropertiesRaw(): Flow<List<Property>> = _propertiesFlow

    override suspend fun getAllPropertiesList(): List<Property> = _propertiesFlow.value

    override fun getPropertiesPagedRaw(limit: Int, offset: Int): Flow<List<Property>> {
        return _propertiesFlow.map { list ->
            list.drop(offset).take(limit)
        }
    }

    override suspend fun getPropertiesPagedList(limit: Int, offset: Int): List<Property> {
        return _propertiesFlow.value.drop(offset).take(limit)
    }

    override fun getPropertiesCountRaw(): Flow<Int> {
        return _propertiesFlow.map { it.size }
    }

    override suspend fun getPropertyByIdRaw(id: Long): Property? {
        return _propertiesFlow.value.find { it.id == id }
    }

    override fun getPropertiesByDistressStatusRaw(status: String): Flow<List<Property>> {
        return _propertiesFlow.map { list ->
            list.filter { it.distressStatus.equals(status, ignoreCase = true) }
        }
    }

    override fun getPropertiesByDistressStatusPagedRaw(status: String, limit: Int, offset: Int): Flow<List<Property>> {
        return _propertiesFlow.map { list ->
            list.filter { it.distressStatus.equals(status, ignoreCase = true) }.drop(offset).take(limit)
        }
    }

    override fun getPropertiesByPipelineStatusRaw(pipelineStatus: String): Flow<List<Property>> {
        return _propertiesFlow.map { list ->
            list.filter { it.pipelineStatus.equals(pipelineStatus, ignoreCase = true) }
        }
    }

    override fun getPropertiesByPipelineStatusPagedRaw(pipelineStatus: String, limit: Int, offset: Int): Flow<List<Property>> {
        return _propertiesFlow.map { list ->
            list.filter { it.pipelineStatus.equals(pipelineStatus, ignoreCase = true) }.drop(offset).take(limit)
        }
    }

    override suspend fun updatePipelineStatusRaw(id: Long, newStatus: String) {
        val currentList = _propertiesFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(pipelineStatus = newStatus)
            _propertiesFlow.value = currentList
        }
    }

    override suspend fun updateRenovationProgressRaw(id: Long, progress: Int, actualCost: Double) {
        val currentList = _propertiesFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(
                renovationProgressPercent = progress,
                actualRenovationCost = actualCost
            )
            _propertiesFlow.value = currentList
        }
    }

    override suspend fun insertPropertyRaw(property: Property): Long {
        val currentList = _propertiesFlow.value.toMutableList()
        val assignedId = if (property.id == 0L) (_propertiesFlow.value.maxOfOrNull { it.id } ?: 0L) + 1L else property.id
        val newProperty = property.copy(id = assignedId)
        currentList.removeAll { it.id == assignedId }
        currentList.add(0, newProperty)
        _propertiesFlow.value = currentList
        return assignedId
    }

    override suspend fun insertPropertiesRaw(properties: List<Property>) {
        val currentList = _propertiesFlow.value.toMutableList()
        properties.forEach { prop ->
            val assignedId = if (prop.id == 0L) (currentList.maxOfOrNull { it.id } ?: 0L) + 1L else prop.id
            val newProp = prop.copy(id = assignedId)
            currentList.removeAll { it.id == assignedId }
            currentList.add(0, newProp)
        }
        _propertiesFlow.value = currentList
    }

    override suspend fun updatePropertyRaw(property: Property) {
        val currentList = _propertiesFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == property.id }
        if (index != -1) {
            currentList[index] = property
            _propertiesFlow.value = currentList
        }
    }

    override suspend fun deletePropertyRaw(property: Property) {
        val currentList = _propertiesFlow.value.toMutableList()
        currentList.removeAll { it.id == property.id }
        _propertiesFlow.value = currentList
    }

    override suspend fun deletePropertiesByIdsRaw(ids: List<Long>): Int {
        val currentList = _propertiesFlow.value.toMutableList()
        val before = currentList.size
        currentList.removeAll { it.id in ids }
        _propertiesFlow.value = currentList
        return before - currentList.size
    }

    override suspend fun updatePipelineStatusForMultipleRaw(ids: List<Long>, newStatus: String): Int {
        val currentList = _propertiesFlow.value.toMutableList()
        var updated = 0
        ids.forEach { id ->
            val index = currentList.indexOfFirst { it.id == id }
            if (index != -1) {
                currentList[index] = currentList[index].copy(pipelineStatus = newStatus)
                updated++
            }
        }
        _propertiesFlow.value = currentList
        return updated
    }

    override suspend fun clearAllRaw() {
        _propertiesFlow.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PropertyRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakePropertyDao
    private lateinit var repository: PropertyRepository
    private lateinit var application: Application

    private val sampleProperty1 = Property(
        id = 101L,
        title = "Appartamento Quadrilocale Via Roma",
        address = "Via Roma 45, Milano",
        price = 250000.0,
        estimatedMarketValue = 320000.0,
        surfaceSqm = 110,
        propertyType = "Appartamento",
        distressStatus = "Asta Imminente",
        strategyTags = "Trading / Stralcio",
        notes = "Opportunità investimento centro"
    )

    private val sampleProperty2 = Property(
        id = 102L,
        title = "Attico Panoramico Corso Garibaldi",
        address = "Corso Garibaldi 12, Torino",
        price = 180000.0,
        estimatedMarketValue = 240000.0,
        surfaceSqm = 85,
        propertyType = "Attico",
        distressStatus = "Pre-Asta",
        strategyTags = "Messa a Rendita",
        notes = "Da ristrutturare completamente"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        fakeDao = FakePropertyDao(listOf(sampleProperty1, sampleProperty2))
        repository = PropertyRepository(fakeDao, CoroutineScope(SupervisorJob() + testDispatcher), testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `allProperties flow emits initial items from Room database`() = runTest {
        val list = repository.allProperties.first()
        assertEquals(2, list.size)
        assertEquals("Appartamento Quadrilocale Via Roma", list[0].title)
        assertEquals("Attico Panoramico Corso Garibaldi", list[1].title)
    }

    @Test
    fun `getPropertiesByDistressStatus returns matching properties from database`() = runTest {
        val preAstaList = repository.getPropertiesByDistressStatus("Pre-Asta").first()
        assertEquals(1, preAstaList.size)
        assertEquals("Attico Panoramico Corso Garibaldi", preAstaList[0].title)

        val astaList = repository.getPropertiesByDistressStatus("Asta Imminente").first()
        assertEquals(1, astaList.size)
        assertEquals("Appartamento Quadrilocale Via Roma", astaList[0].title)
    }

    @Test
    fun `getPropertyById returns expected property or null if not found`() = runTest {
        val property = repository.getPropertyById(101L)
        assertNotNull(property)
        assertEquals("Via Roma 45, Milano", property?.address)

        val missing = repository.getPropertyById(999L)
        assertNull(missing)
    }

    @Test
    fun `insertProperty adds new item to database and triggers flow update`() = runTest {
        val newProp = Property(
            id = 103L,
            title = "Villetta A Schiera",
            address = "Via Dante 8, Bologna",
            price = 310000.0,
            distressStatus = "Asta Imminente"
        )

        val insertedId = repository.insertProperty(newProp)
        assertEquals(103L, insertedId)

        val updatedList = repository.allProperties.first()
        assertEquals(3, updatedList.size)
        assertTrue(updatedList.any { it.title == "Villetta A Schiera" })
    }

    @Test
    fun `insertProperties performs bulk insert into database`() = runTest {
        val bulkList = listOf(
            Property(id = 201L, title = "Locale Commerciale", address = "Via Firenze 2", price = 150000.0, distressStatus = "Asta"),
            Property(id = 202L, title = "Terratetto Unifamiliare", address = "Via Napoli 5", price = 220000.0, distressStatus = "Pre-Asta")
        )

        repository.insertProperties(bulkList)

        val updatedList = repository.allProperties.first()
        assertEquals(4, updatedList.size)
        assertTrue(updatedList.any { it.id == 201L })
        assertTrue(updatedList.any { it.id == 202L })
    }

    @Test
    fun `updateProperty modifies existing database entry`() = runTest {
        val updatedProperty = sampleProperty1.copy(price = 230000.0, notes = "Offerta rivista al ribasso")
        repository.updateProperty(updatedProperty)

        val fetched = repository.getPropertyById(101L)
        assertNotNull(fetched)
        assertEquals(230000.0, fetched?.price ?: 0.0, 0.01)
        assertEquals("Offerta rivista al ribasso", fetched?.notes)
    }

    @Test
    fun `deleteProperty removes property from database flow`() = runTest {
        repository.deleteProperty(sampleProperty1)

        val remaining = repository.allProperties.first()
        assertEquals(1, remaining.size)
        assertFalse(remaining.any { it.id == 101L })
    }

    @Test
    fun `clearAll empties property database table`() = runTest {
        repository.clearAll()

        val emptyList = repository.allProperties.first()
        assertTrue(emptyList.isEmpty())
    }

    // =========================================================================
    // Data Flow Verification: Database -> Repository -> ViewModel
    // =========================================================================

    @Test
    fun `data flow from Repository to ViewModel updates properties and uiState`() = runTest {
        val viewModel = PropertyViewModel(application, repository)
        val job1 = backgroundScope.launch { viewModel.properties.collect {} }
        val job2 = backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        val properties = viewModel.properties.value
        assertEquals(2, properties.size)

        val uiState = viewModel.uiState.value
        assertEquals(2, uiState.properties.size)
        job1.cancel()
        job2.cancel()
    }

    @Test
    fun `ViewModel search query filters properties emitted from Repository`() = runTest {
        val viewModel = PropertyViewModel(application, repository)
        val job = backgroundScope.launch { viewModel.filteredProperties.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.updateSearchQuery("Torino")
        testScheduler.advanceUntilIdle()

        val filtered = viewModel.filteredProperties.value
        assertEquals(1, filtered.size)
        assertEquals("Attico Panoramico Corso Garibaldi", filtered[0].title)
        job.cancel()
    }

    @Test
    fun `ViewModel addProperty updates database via Repository and propagates to ViewModel state`() = runTest {
        val viewModel = PropertyViewModel(application, repository)
        val job = backgroundScope.launch { viewModel.properties.collect {} }
        testScheduler.advanceUntilIdle()

        val newProp = Property(
            id = 104L,
            title = "Trilocale Solferino",
            address = "Via Solferino 10, Milano",
            price = 290000.0,
            distressStatus = "Asta Imminente"
        )

        viewModel.addProperty(newProp)
        testScheduler.advanceUntilIdle()

        val currentProperties = viewModel.properties.value
        assertEquals(3, currentProperties.size)
        assertTrue(currentProperties.any { it.title == "Trilocale Solferino" })

        // Check that repository underlying database reflects change
        val dbProperty = repository.getPropertyById(104L)
        assertNotNull(dbProperty)
        assertEquals("Via Solferino 10, Milano", dbProperty?.address)
        job.cancel()
    }

    @Test
    fun `ViewModel status filter interacts with database flow emissions`() = runTest {
        val viewModel = PropertyViewModel(application, repository)
        val job = backgroundScope.launch { viewModel.filteredProperties.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.updateStatusFilter("Pre-Asta")
        testScheduler.advanceUntilIdle()

        val filtered = viewModel.filteredProperties.value
        assertEquals(1, filtered.size)
        assertEquals("Pre-Asta", filtered[0].distressStatus)
        job.cancel()
    }

    @Test
    fun `ViewModel deleteProperty removes property from database and updates UI state`() = runTest {
        val viewModel = PropertyViewModel(application, repository)
        val job = backgroundScope.launch { viewModel.properties.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.deleteProperty(sampleProperty1)
        testScheduler.advanceUntilIdle()

        val remaining = viewModel.properties.value
        assertEquals(1, remaining.size)
        assertFalse(remaining.any { it.id == 101L })
        job.cancel()
    }

    @Test
    fun `ViewModel updatePropertyPipelineStatus updates status and recomputes pipeline metrics`() = runTest {
        val viewModel = PropertyViewModel(application, repository)
        val propJob = backgroundScope.launch { viewModel.properties.collect {} }
        val metricJob = backgroundScope.launch { viewModel.pipelineMetrics.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.updatePropertyPipelineStatus(sampleProperty1.id, PipelineStatus.RENOVATING)
        testScheduler.advanceUntilIdle()

        val updated = repository.getPropertyById(sampleProperty1.id)
        assertNotNull(updated)
        assertEquals(PipelineStatus.RENOVATING.key, updated?.pipelineStatus)

        val metrics = viewModel.pipelineMetrics.value
        assertEquals(1, metrics.activeRenovatingCount)
        propJob.cancel()
        metricJob.cancel()
    }

    @Test
    fun `ViewModel updateRenovationProgress updates progress and actual costs`() = runTest {
        val viewModel = PropertyViewModel(application, repository)
        val propJob = backgroundScope.launch { viewModel.properties.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.updateRenovationProgress(sampleProperty1.id, 75, 42000.0)
        testScheduler.advanceUntilIdle()

        val updated = repository.getPropertyById(sampleProperty1.id)
        assertNotNull(updated)
        assertEquals(75, updated?.renovationProgressPercent)
        assertEquals(42000.0, updated?.actualRenovationCost ?: 0.0, 0.001)
        propJob.cancel()
    }

    @Test
    fun `repository getPropertiesPaged and count work properly for large datasets`() = runTest {
        val extraProps = (1..10).map { i ->
            Property(
                id = 200L + i,
                title = "Prop $i",
                address = "Address $i",
                price = 100000.0 * i,
                distressStatus = if (i % 2 == 0) "Asta Imminente" else "Pre-Asta"
            )
        }
        repository.insertProperties(extraProps)

        val totalCount = repository.getPropertiesCount().first()
        assertEquals(12, totalCount)

        val page1 = repository.getPropertiesPaged(limit = 5, offset = 0).first()
        assertEquals(5, page1.size)

        val page2 = repository.getPropertiesPaged(limit = 5, offset = 5).first()
        assertEquals(5, page2.size)

        val page3 = repository.getPropertiesPaged(limit = 5, offset = 10).first()
        assertEquals(2, page3.size)

        val pagedList = repository.getPropertiesPagedList(limit = 3, offset = 0)
        assertEquals(3, pagedList.size)
    }
}
