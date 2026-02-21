package com.example.worktimetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worktimetracker.data.local.entity.GeofenceZone
import com.example.worktimetracker.data.local.entity.ZoneType
import com.example.worktimetracker.data.repository.GeofenceRepository
import com.example.worktimetracker.domain.GeocodingService
import com.example.worktimetracker.domain.SearchResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val zones: List<GeofenceZone> = emptyList(),
    val isEditingZone: Boolean = false,
    val selectedZone: GeofenceZone? = null,
    val temporaryPosition: LatLng? = null,
    val temporaryName: String = "",
    val temporaryType: ZoneType = ZoneType.HOME_STATION,
    val temporaryRadius: Float = 150f,
    val temporaryColor: Int = 0xFF0000FF.toInt(),
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val cameraTarget: LatLng? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val geofenceRepository: GeofenceRepository,
    private val geocodingService: GeocodingService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _pendingDeleteId = MutableStateFlow<String?>(null)
    private var _pendingDeleteJob: Job? = null

    init {
        loadZones()
    }

    private fun loadZones() {
        viewModelScope.launch {
            geofenceRepository.getAllZones()
                .combine(_pendingDeleteId) { zones, pendingId ->
                    if (pendingId != null) zones.filter { it.id != pendingId } else zones
                }
                .collect { zones ->
                    _uiState.update { it.copy(zones = zones) }
                }
        }
    }

    fun startAddingZone() {
        _uiState.update {
            it.copy(
                isEditingZone = true,
                selectedZone = null,
                temporaryPosition = null,
                temporaryName = "",
                temporaryType = ZoneType.HOME_STATION,
                temporaryRadius = 150f,
                temporaryColor = 0xFF0000FF.toInt()
            )
        }
    }

    fun startEditingZone(zone: GeofenceZone) {
        _uiState.update {
            it.copy(
                isEditingZone = true,
                selectedZone = zone,
                temporaryPosition = LatLng(zone.latitude, zone.longitude),
                temporaryName = zone.name,
                temporaryType = zone.zoneType,
                temporaryRadius = zone.radiusMeters,
                temporaryColor = zone.color
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                isEditingZone = false,
                selectedZone = null,
                temporaryPosition = null,
                temporaryName = "",
                temporaryType = ZoneType.HOME_STATION,
                temporaryRadius = 150f,
                temporaryColor = 0xFF0000FF.toInt()
            )
        }
    }

    fun setZonePosition(position: LatLng) {
        _uiState.update { it.copy(temporaryPosition = position) }
    }

    fun setZoneName(name: String) {
        _uiState.update { it.copy(temporaryName = name) }
    }

    fun setZoneType(type: ZoneType) {
        _uiState.update { it.copy(temporaryType = type) }
    }

    fun setZoneRadius(radius: Float) {
        _uiState.update { it.copy(temporaryRadius = radius) }
    }

    fun setZoneColor(color: Int) {
        _uiState.update { it.copy(temporaryColor = color) }
    }

    fun saveZone() {
        viewModelScope.launch {
            val state = _uiState.value
            val position = state.temporaryPosition ?: return@launch

            if (state.selectedZone != null) {
                // Update existing zone
                val updatedZone = state.selectedZone.copy(
                    name = state.temporaryName,
                    latitude = position.latitude,
                    longitude = position.longitude,
                    radiusMeters = state.temporaryRadius,
                    zoneType = state.temporaryType,
                    color = state.temporaryColor
                )
                geofenceRepository.updateZone(updatedZone)
            } else {
                // Create new zone
                val newZone = GeofenceZone(
                    name = state.temporaryName,
                    latitude = position.latitude,
                    longitude = position.longitude,
                    radiusMeters = state.temporaryRadius,
                    zoneType = state.temporaryType,
                    color = state.temporaryColor
                )
                geofenceRepository.insertZone(newZone)
            }

            cancelEditing()
        }
    }

    fun focusZone(zone: GeofenceZone) {
        _uiState.update { it.copy(cameraTarget = LatLng(zone.latitude, zone.longitude)) }
    }

    fun deleteZone(zone: GeofenceZone, onShowSnackbar: (GeofenceZone) -> Unit) {
        _pendingDeleteId.value = zone.id
        onShowSnackbar(zone)
        _pendingDeleteJob = viewModelScope.launch {
            delay(5_000)
            geofenceRepository.deleteZone(zone)
            _pendingDeleteId.value = null
        }
    }

    fun undoDeleteZone(zone: GeofenceZone) {
        _pendingDeleteJob?.cancel()
        _pendingDeleteId.value = null
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun performSearch(query: String) {
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    searchError = null,
                    isSearching = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }

            val result = geocodingService.searchAddress(query)

            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchResults = result.getOrElse { emptyList() },
                    searchError = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun selectSearchResult(result: SearchResult) {
        _uiState.update {
            it.copy(
                cameraTarget = result.latLng,
                searchResults = emptyList(),
                searchQuery = "",
                searchError = null
            )
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                searchError = null,
                isSearching = false
            )
        }
    }

    fun clearCameraTarget() {
        _uiState.update { it.copy(cameraTarget = null) }
    }
}
