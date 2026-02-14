package com.example.worktimetracker.ui.viewmodel

import app.cash.turbine.test
import com.example.worktimetracker.data.local.entity.GeofenceZone
import com.example.worktimetracker.data.local.entity.ZoneType
import com.example.worktimetracker.data.repository.GeofenceRepository
import com.example.worktimetracker.domain.GeocodingService
import com.example.worktimetracker.domain.SearchResult
import com.google.android.gms.maps.model.LatLng
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: GeofenceRepository
    private lateinit var geocodingService: GeocodingService
    private lateinit var viewModel: MapViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        geocodingService = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty zones list`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllZones() } returns flowOf(emptyList())

        // When
        viewModel = MapViewModel(repository, geocodingService)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.zones.isEmpty())
            assertFalse(state.isEditingZone)
            assertNull(state.selectedZone)
        }
    }

    @Test
    fun `zones are loaded from repository`() = runTest(testDispatcher) {
        // Given
        val zones = listOf(
            GeofenceZone(
                id = "1",
                name = "Home Station",
                latitude = 48.1351,
                longitude = 11.5820,
                radiusMeters = 150f,
                zoneType = ZoneType.HOME_STATION,
                color = 0xFF0000FF.toInt()
            )
        )
        coEvery { repository.getAllZones() } returns flowOf(zones)

        // When
        viewModel = MapViewModel(repository, geocodingService)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.zones.size)
            assertEquals("Home Station", state.zones[0].name)
        }
    }

    @Test
    fun `startAddingZone opens bottom sheet`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        viewModel = MapViewModel(repository, geocodingService)

        // When
        viewModel.startAddingZone()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isEditingZone)
            assertNull(state.selectedZone)
        }
    }

    @Test
    fun `startEditingZone opens bottom sheet with selected zone`() = runTest(testDispatcher) {
        // Given
        val zone = GeofenceZone(
            id = "1",
            name = "Office",
            latitude = 48.1351,
            longitude = 11.5820,
            radiusMeters = 150f,
            zoneType = ZoneType.OFFICE,
            color = 0xFF00FF00.toInt()
        )
        coEvery { repository.getAllZones() } returns flowOf(listOf(zone))
        viewModel = MapViewModel(repository, geocodingService)

        // When
        viewModel.startEditingZone(zone)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isEditingZone)
            assertEquals(zone, state.selectedZone)
        }
    }

    @Test
    fun `cancelEditing closes bottom sheet`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        viewModel = MapViewModel(repository, geocodingService)
        viewModel.startAddingZone()

        // When
        viewModel.cancelEditing()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isEditingZone)
            assertNull(state.selectedZone)
        }
    }

    @Test
    fun `setZonePosition updates temporary position`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        viewModel = MapViewModel(repository, geocodingService)
        viewModel.startAddingZone()

        // When
        val position = LatLng(48.1351, 11.5820)
        viewModel.setZonePosition(position)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(position, state.temporaryPosition)
        }
    }

    @Test
    fun `setZoneName updates temporary name`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        viewModel = MapViewModel(repository, geocodingService)
        viewModel.startAddingZone()

        // When
        viewModel.setZoneName("Test Office")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Test Office", state.temporaryName)
        }
    }

    @Test
    fun `setZoneType updates temporary type`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        viewModel = MapViewModel(repository, geocodingService)
        viewModel.startAddingZone()

        // When
        viewModel.setZoneType(ZoneType.OFFICE)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(ZoneType.OFFICE, state.temporaryType)
        }
    }

    @Test
    fun `setZoneRadius updates temporary radius`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        viewModel = MapViewModel(repository, geocodingService)
        viewModel.startAddingZone()

        // When
        viewModel.setZoneRadius(200f)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(200f, state.temporaryRadius)
        }
    }

    @Test
    fun `setZoneColor updates temporary color`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        viewModel = MapViewModel(repository, geocodingService)
        viewModel.startAddingZone()

        // When
        viewModel.setZoneColor(0xFF00FF00.toInt())

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0xFF00FF00.toInt(), state.temporaryColor)
        }
    }

    @Test
    fun `saveZone creates new zone when adding`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        coEvery { repository.insertZone(any()) } returns Unit
        viewModel = MapViewModel(repository, geocodingService)
        viewModel.startAddingZone()
        viewModel.setZoneName("New Office")
        viewModel.setZonePosition(LatLng(48.1351, 11.5820))
        viewModel.setZoneType(ZoneType.OFFICE)
        viewModel.setZoneRadius(150f)
        viewModel.setZoneColor(0xFF00FF00.toInt())

        // When
        viewModel.saveZone()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify {
            repository.insertZone(
                match {
                    it.name == "New Office" &&
                            it.latitude == 48.1351 &&
                            it.longitude == 11.5820 &&
                            it.zoneType == ZoneType.OFFICE &&
                            it.radiusMeters == 150f &&
                            it.color == 0xFF00FF00.toInt()
                }
            )
        }
    }

    @Test
    fun `saveZone updates existing zone when editing`() = runTest(testDispatcher) {
        // Given
        val existingZone = GeofenceZone(
            id = "1",
            name = "Old Office",
            latitude = 48.1351,
            longitude = 11.5820,
            radiusMeters = 150f,
            zoneType = ZoneType.OFFICE,
            color = 0xFF00FF00.toInt()
        )
        coEvery { repository.getAllZones() } returns flowOf(listOf(existingZone))
        coEvery { repository.updateZone(any()) } returns Unit
        viewModel = MapViewModel(repository, geocodingService)
        viewModel.startEditingZone(existingZone)
        viewModel.setZoneName("Updated Office")
        viewModel.setZoneRadius(200f)

        // When
        viewModel.saveZone()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify {
            repository.updateZone(
                match {
                    it.id == "1" &&
                            it.name == "Updated Office" &&
                            it.radiusMeters == 200f
                }
            )
        }
    }

    @Test
    fun `deleteZone removes zone from repository`() = runTest(testDispatcher) {
        // Given
        val zone = GeofenceZone(
            id = "1",
            name = "Office",
            latitude = 48.1351,
            longitude = 11.5820,
            radiusMeters = 150f,
            zoneType = ZoneType.OFFICE,
            color = 0xFF00FF00.toInt()
        )
        coEvery { repository.getAllZones() } returns flowOf(listOf(zone))
        coEvery { repository.deleteZone(zone) } returns Unit
        viewModel = MapViewModel(repository, geocodingService)

        // When
        viewModel.deleteZone(zone)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.deleteZone(zone) }
    }

    @Test
    fun `searchAddress updates search query`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        viewModel = MapViewModel(repository, geocodingService)

        // When
        viewModel.updateSearchQuery("München Hauptbahnhof")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("München Hauptbahnhof", state.searchQuery)
        }
    }

    @Test
    fun `performSearch returns search results on success`() = runTest(testDispatcher) {
        // Given
        val searchResults = listOf(
            SearchResult(
                name = "München Hauptbahnhof",
                address = "Bayerstraße, München, Bayern, Germany",
                latLng = LatLng(48.1405, 11.5584)
            ),
            SearchResult(
                name = "München Hbf",
                address = "München, Germany",
                latLng = LatLng(48.1406, 11.5585)
            )
        )
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        coEvery { geocodingService.searchAddress("München Hauptbahnhof") } returns Result.success(searchResults)
        viewModel = MapViewModel(repository, geocodingService)

        // When
        viewModel.performSearch("München Hauptbahnhof")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSearching)
            assertEquals(2, state.searchResults.size)
            assertEquals("München Hauptbahnhof", state.searchResults[0].name)
            assertNull(state.searchError)
        }
    }

    @Test
    fun `performSearch sets loading state while searching`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        coEvery { geocodingService.searchAddress(any()) } returns Result.success(emptyList())
        viewModel = MapViewModel(repository, geocodingService)

        // When
        viewModel.performSearch("München")

        // Then - should be loading immediately
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isSearching)
        }
    }

    @Test
    fun `performSearch sets error on failure`() = runTest(testDispatcher) {
        // Given
        val errorMessage = "Network error"
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        coEvery { geocodingService.searchAddress("Invalid Address") } returns Result.failure(Exception(errorMessage))
        viewModel = MapViewModel(repository, geocodingService)

        // When
        viewModel.performSearch("Invalid Address")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSearching)
            assertTrue(state.searchResults.isEmpty())
            assertEquals(errorMessage, state.searchError)
        }
    }

    @Test
    fun `selectSearchResult updates camera target and clears search`() = runTest(testDispatcher) {
        // Given
        val searchResult = SearchResult(
            name = "München Hauptbahnhof",
            address = "Bayerstraße, München",
            latLng = LatLng(48.1405, 11.5584)
        )
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        viewModel = MapViewModel(repository, geocodingService)

        // When
        viewModel.selectSearchResult(searchResult)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(LatLng(48.1405, 11.5584), state.cameraTarget)
            assertTrue(state.searchResults.isEmpty())
            assertEquals("", state.searchQuery)
        }
    }

    @Test
    fun `clearSearch resets search state`() = runTest(testDispatcher) {
        // Given
        val searchResults = listOf(
            SearchResult("Test", "Address", LatLng(48.0, 11.0))
        )
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        coEvery { geocodingService.searchAddress(any()) } returns Result.success(searchResults)
        viewModel = MapViewModel(repository, geocodingService)
        viewModel.performSearch("Test")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearSearch()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.searchQuery)
            assertTrue(state.searchResults.isEmpty())
            assertNull(state.searchError)
            assertFalse(state.isSearching)
        }
    }

    @Test
    fun `performSearch with blank query clears results`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllZones() } returns flowOf(emptyList())
        viewModel = MapViewModel(repository, geocodingService)

        // When
        viewModel.performSearch("")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.searchResults.isEmpty())
            assertFalse(state.isSearching)
        }
    }
}
