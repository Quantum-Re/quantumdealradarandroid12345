package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DistressedProperty
import com.example.data.DistressedPropertyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SavedAlertCriteria(
    val query: String = "",
    val distressLevel: String = "ALL",
    val maxPrice: Double? = null,
    val alertsEnabled: Boolean = true
)

data class DistressedPropertyUiState(
    val distressedProperties: List<DistressedProperty> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedDistressLevel: String = "ALL",
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val maxDistanceKm: Double? = null,
    val userLat: Double = 45.4642,
    val userLng: Double = 9.1900,
    val selectedProperty: DistressedProperty? = null,
    val errorMessage: String? = null
)

class DistressedPropertyViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: DistressedPropertyRepository = DistressedPropertyRepository(
        AppDatabase.getDatabase(application).distressedPropertyDao()
    )
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("distressed_alert_prefs", Context.MODE_PRIVATE)

    private val _savedAlertCriteria = MutableStateFlow(
        SavedAlertCriteria(
            query = prefs.getString("saved_query", "") ?: "",
            distressLevel = prefs.getString("saved_distress_level", "ALL") ?: "ALL",
            maxPrice = if (prefs.contains("saved_max_price")) prefs.getFloat("saved_max_price", 0f).toDouble() else null,
            alertsEnabled = prefs.getBoolean("alerts_enabled", true)
        )
    )
    val savedAlertCriteria: StateFlow<SavedAlertCriteria> = _savedAlertCriteria.asStateFlow()

    fun saveAlertCriteria(query: String, distressLevel: String, maxPrice: Double?, enabled: Boolean) {
        prefs.edit().apply {
            putString("saved_query", query)
            putString("saved_distress_level", distressLevel)
            if (maxPrice != null) {
                putFloat("saved_max_price", maxPrice.toFloat())
            } else {
                remove("saved_max_price")
            }
            putBoolean("alerts_enabled", enabled)
            apply()
        }
        _savedAlertCriteria.value = SavedAlertCriteria(query, distressLevel, maxPrice, enabled)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDistressLevel = MutableStateFlow("ALL")
    val selectedDistressLevel: StateFlow<String> = _selectedDistressLevel.asStateFlow()

    private val _minPrice = MutableStateFlow<Double?>(null)
    val minPrice: StateFlow<Double?> = _minPrice.asStateFlow()

    private val _maxPrice = MutableStateFlow<Double?>(null)
    val maxPrice: StateFlow<Double?> = _maxPrice.asStateFlow()

    private val _maxDistanceKm = MutableStateFlow<Double?>(null)
    val maxDistanceKm: StateFlow<Double?> = _maxDistanceKm.asStateFlow()

    private val _userLat = MutableStateFlow(45.4642) // Milan default reference center
    val userLat: StateFlow<Double> = _userLat.asStateFlow()

    private val _userLng = MutableStateFlow(9.1900)
    val userLng: StateFlow<Double> = _userLng.asStateFlow()

    private val _selectedProperty = MutableStateFlow<DistressedProperty?>(null)
    val selectedProperty: StateFlow<DistressedProperty?> = _selectedProperty.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        android.util.Log.d("DistressedPropertyViewModel", "DistressedPropertyViewModel init, checking database contents...")
        viewModelScope.launch {
            val result = com.example.data.AppDatabase.seedDatabaseIfEmpty(getApplication())
            android.util.Log.d("DistressedPropertyViewModel", "Database seed check completed on startup: $result")
        }
        com.example.service.DistressedWorkManagerScheduler.schedulePeriodicCheck(getApplication())
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val allDistressedProperties: StateFlow<List<DistressedProperty>> = repository.allDistressedProperties

    val filteredDistressedProperties: StateFlow<List<DistressedProperty>> = combine(
        repository.allDistressedProperties,
        _searchQuery,
        _selectedDistressLevel,
        _minPrice,
        _maxPrice,
        _maxDistanceKm
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val properties = array[0] as List<DistressedProperty>
        val query = array[1] as String
        val levelFilter = array[2] as String
        val minP = array[3] as? Double
        val maxP = array[4] as? Double
        val maxDist = array[5] as? Double

        android.util.Log.d("DistressedPropertyViewModel", "filteredDistressedProperties combine triggered with ${properties.size} items from DB (Filter: '$levelFilter', Query: '$query', MinPrice: $minP, MaxPrice: $maxP, MaxDist: $maxDist km)")
        val searchTokens = query.trim().let { q ->
            java.text.Normalizer.normalize(q, java.text.Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase()
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
        }

        val refLat = _userLat.value
        val refLng = _userLng.value

        val result = properties.filter { property ->
            val matchesQuery = if (searchTokens.isEmpty()) {
                true
            } else {
                val rawText = listOf(
                    property.address,
                    property.distressLevel,
                    property.notes ?: "",
                    "€${property.price.toInt()}",
                    "${property.price.toInt()}",
                    property.id.toString()
                ).joinToString(" ")

                val searchableText = java.text.Normalizer.normalize(rawText, java.text.Normalizer.Form.NFD)
                    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                    .lowercase()

                searchTokens.all { token -> searchableText.contains(token) }
            }

            val matchesLevel = levelFilter == "ALL" || 
                property.distressLevel.equals(levelFilter, ignoreCase = true) ||
                property.distressLevel.contains(levelFilter, ignoreCase = true)

            val matchesMinPrice = minP == null || property.price >= minP
            val matchesMaxPrice = maxP == null || property.price <= maxP

            val matchesProximity = if (maxDist == null || property.latitude == null || property.longitude == null || (property.latitude == 0.0 && property.longitude == 0.0)) {
                true
            } else {
                val dist = calculateDistanceKm(refLat, refLng, property.latitude, property.longitude)
                dist <= maxDist
            }

            matchesQuery && matchesLevel && matchesMinPrice && matchesMaxPrice && matchesProximity
        }
        android.util.Log.d("DistressedPropertyViewModel", "filteredDistressedProperties produced ${result.size} items")
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<DistressedPropertyUiState> = combine(
        filteredDistressedProperties,
        _isLoading,
        _searchQuery,
        _selectedDistressLevel,
        _minPrice,
        _maxPrice,
        _maxDistanceKm,
        _selectedProperty,
        _errorMessage
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        DistressedPropertyUiState(
            distressedProperties = array[0] as List<DistressedProperty>,
            isLoading = array[1] as Boolean,
            searchQuery = array[2] as String,
            selectedDistressLevel = array[3] as String,
            minPrice = array[4] as? Double,
            maxPrice = array[5] as? Double,
            maxDistanceKm = array[6] as? Double,
            userLat = _userLat.value,
            userLng = _userLng.value,
            selectedProperty = array[7] as? DistressedProperty,
            errorMessage = array[8] as? String
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DistressedPropertyUiState()
    )

    private val recentSearchRepository = com.example.data.RecentSearchRepository(
        AppDatabase.getDatabase(application).recentSearchDao()
    )

    val recentSearches: StateFlow<List<com.example.data.RecentSearchQuery>> = recentSearchRepository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun submitSearchQuery(query: String) {
        onSearchQueryChange(query)
        viewModelScope.launch {
            recentSearchRepository.saveSearchQuery(query)
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            recentSearchRepository.removeSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            recentSearchRepository.clearAll()
        }
    }

    fun onDistressLevelSelected(level: String) {
        _selectedDistressLevel.value = level
    }

    fun setPriceRange(min: Double?, max: Double?) {
        _minPrice.value = min
        _maxPrice.value = max
    }

    fun setMaxDistanceKm(distanceKm: Double?) {
        _maxDistanceKm.value = distanceKm
    }

    fun setUserLocation(lat: Double, lng: Double) {
        _userLat.value = lat
        _userLng.value = lng
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedDistressLevel.value = "ALL"
        _minPrice.value = null
        _maxPrice.value = null
        _maxDistanceKm.value = null
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    fun onSelectProperty(property: DistressedProperty?) {
        _selectedProperty.value = property
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun addDistressedProperty(
        address: String,
        price: Double,
        distressLevel: String,
        estimatedValue: Double = 0.0,
        status: String = "Active",
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        imageUrl: String? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val newProperty = DistressedProperty(
                    address = address,
                    price = price,
                    estimatedValue = estimatedValue,
                    distressLevel = distressLevel,
                    status = status,
                    latitude = latitude,
                    longitude = longitude,
                    imageUrl = imageUrl,
                    notes = notes,
                    lastUpdated = System.currentTimeMillis()
                )
                val insertedId = repository.insertDistressedProperty(newProperty)
                val savedProperty = newProperty.copy(id = insertedId)

                checkAndTriggerAlertNotification(savedProperty)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add distressed property: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun checkAndTriggerAlertNotification(property: DistressedProperty) {
        val criteria = _savedAlertCriteria.value
        if (!criteria.alertsEnabled) return

        val matchesQuery = criteria.query.isBlank() ||
                property.address.contains(criteria.query, ignoreCase = true) ||
                property.distressLevel.contains(criteria.query, ignoreCase = true)

        val matchesLevel = criteria.distressLevel.equals("ALL", ignoreCase = true) ||
                property.distressLevel.equals(criteria.distressLevel, ignoreCase = true) ||
                property.distressLevel.contains(criteria.distressLevel, ignoreCase = true)

        val matchesPrice = criteria.maxPrice == null || property.price <= criteria.maxPrice

        if (matchesQuery && matchesLevel && matchesPrice) {
            val matchedReasonList = mutableListOf<String>()
            if (criteria.query.isNotBlank()) matchedReasonList.add("Filtro: '${criteria.query}'")
            if (!criteria.distressLevel.equals("ALL", ignoreCase = true)) matchedReasonList.add("Distress: ${criteria.distressLevel}")
            if (criteria.maxPrice != null) matchedReasonList.add("Max €${criteria.maxPrice.toInt()}")

            val matchDesc = if (matchedReasonList.isNotEmpty()) {
                matchedReasonList.joinToString(" • ")
            } else {
                "Notifica generale immobili distressed"
            }

            com.example.util.PriceAlertNotificationManager.sendNewDistressedPropertyAlertNotification(
                context = getApplication(),
                property = property,
                matchedCriteria = matchDesc
            )
        }
    }

    private val _isAnalyzingArv = MutableStateFlow(false)
    val isAnalyzingArv: StateFlow<Boolean> = _isAnalyzingArv.asStateFlow()

    private val _arvAnalysisResult = MutableStateFlow<com.example.util.ArvAnalysisResult?>(null)
    val arvAnalysisResult: StateFlow<com.example.util.ArvAnalysisResult?> = _arvAnalysisResult.asStateFlow()

    fun analyzePropertyArv(property: DistressedProperty) {
        viewModelScope.launch {
            _isAnalyzingArv.value = true
            _arvAnalysisResult.value = null
            try {
                val result = com.example.util.GeminiArvAnalyzerService.analyzePropertyArv(property)
                if (result.isSuccess) {
                    val arvResult = result.getOrNull()
                    _arvAnalysisResult.value = arvResult
                    if (arvResult != null) {
                        val updated = property.copy(
                            estimatedArv = arvResult.estimatedArv,
                            aiAnalysisReport = arvResult.detailedReportMarkdown,
                            lastUpdated = System.currentTimeMillis()
                        )
                        repository.updateDistressedProperty(updated)
                        if (_selectedProperty.value?.id == property.id) {
                            _selectedProperty.value = updated
                        }
                    }
                } else {
                    _errorMessage.value = "ARV Analysis failed: ${result.exceptionOrNull()?.localizedMessage}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error estimating ARV: ${e.localizedMessage}"
            } finally {
                _isAnalyzingArv.value = false
            }
        }
    }

    fun clearArvAnalysisResult() {
        _arvAnalysisResult.value = null
    }

    fun triggerWorkManagerBackgroundCheck() {
        com.example.service.DistressedWorkManagerScheduler.triggerImmediateCheck(getApplication())
    }

    fun triggerTestNotification(sampleAddress: String, samplePrice: Double, sampleLevel: String) {
        val testProperty = DistressedProperty(
            id = (System.currentTimeMillis() % 10000),
            address = sampleAddress,
            price = samplePrice,
            distressLevel = sampleLevel,
            latitude = 45.4642,
            longitude = 9.1900
        )
        com.example.util.PriceAlertNotificationManager.sendNewDistressedPropertyAlertNotification(
            context = getApplication(),
            property = testProperty,
            matchedCriteria = "Notifica di Prova"
        )
    }

    fun updateDistressedProperty(property: DistressedProperty) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val updated = property.copy(lastUpdated = System.currentTimeMillis())
                repository.updateDistressedProperty(updated)
                if (_selectedProperty.value?.id == property.id) {
                    _selectedProperty.value = updated
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update distressed property: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePropertyNotes(property: DistressedProperty, newNotes: String) {
        val updatedProperty = property.copy(notes = newNotes)
        updateDistressedProperty(updatedProperty)
    }

    fun updatePropertyPhoto(property: DistressedProperty, imagePath: String) {
        val updatedProperty = property.copy(imageUrl = imagePath)
        updateDistressedProperty(updatedProperty)
    }

    fun deleteDistressedProperty(property: DistressedProperty) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteDistressedProperty(property)
                if (_selectedProperty.value?.id == property.id) {
                    _selectedProperty.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete distressed property: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun restoreDistressedProperty(property: DistressedProperty) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.insertDistressedProperty(property)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to restore distressed property: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearAllDistressedProperties() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.clearAll()
                _selectedProperty.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to clear distressed properties: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
