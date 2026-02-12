package com.example.worktimetracker.ui.viewmodel

import app.cash.turbine.test
import com.example.worktimetracker.data.local.entity.GeofenceZone
import com.example.worktimetracker.data.local.entity.ZoneType
import com.example.worktimetracker.data.repository.GeofenceRepository
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
    private lateinit var viewModel: MapViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
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
        viewModel = MapViewModel(repository)

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
        viewModel = MapViewModel(repository)
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
        viewModel = MapViewModel(repository)

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
        viewModel = MapViewModel(repository)

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
        viewModel = MapViewModel(repository)
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
        viewModel = MapViewModel(repository)
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
        viewModel = MapViewModel(repository)
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
        viewModel = MapViewModel(repository)
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
        viewModel = MapViewModel(repository)
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
        viewModel = MapViewModel(repository)
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
        viewModel = MapViewModel(repository)
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
        viewModel = MapViewModel(repository)
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
        viewModel = MapViewModel(repository)

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
        viewModel = MapViewModel(repository)

        // When
        viewModel.searchAddress("München Hauptbahnhof")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("München Hauptbahnhof", state.searchQuery)
        }
    }
}
