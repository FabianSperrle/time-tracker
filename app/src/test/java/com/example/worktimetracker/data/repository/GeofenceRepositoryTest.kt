package com.example.worktimetracker.data.repository

import app.cash.turbine.test
import com.example.worktimetracker.data.local.dao.GeofenceDao
import com.example.worktimetracker.data.local.entity.GeofenceZone
import com.example.worktimetracker.data.local.entity.ZoneType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GeofenceRepositoryTest {

    private lateinit var geofenceDao: GeofenceDao
    private lateinit var repository: GeofenceRepository

    @BeforeEach
    fun setup() {
        geofenceDao = mockk()
        repository = GeofenceRepository(geofenceDao)
    }

    @Test
    fun `getAllZones returns flow from dao`() = runTest {
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
        coEvery { geofenceDao.getAllZones() } returns flowOf(zones)

        // When
        repository.getAllZones().test {
            // Then
            assertEquals(zones, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getZonesByType returns zones from dao`() = runTest {
        // Given
        val zones = listOf(
            GeofenceZone(
                id = "1",
                name = "Office",
                latitude = 48.1351,
                longitude = 11.5820,
                radiusMeters = 150f,
                zoneType = ZoneType.OFFICE,
                color = 0xFF00FF00.toInt()
            )
        )
        coEvery { geofenceDao.getZonesByType(ZoneType.OFFICE) } returns zones

        // When
        val result = repository.getZonesByType(ZoneType.OFFICE)

        // Then
        assertEquals(zones, result)
        coVerify { geofenceDao.getZonesByType(ZoneType.OFFICE) }
    }

    @Test
    fun `insertZone calls dao insert`() = runTest {
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
        coEvery { geofenceDao.insert(zone) } returns Unit

        // When
        repository.insertZone(zone)

        // Then
        coVerify { geofenceDao.insert(zone) }
    }

    @Test
    fun `updateZone calls dao update`() = runTest {
        // Given
        val zone = GeofenceZone(
            id = "1",
            name = "Updated Office",
            latitude = 48.1351,
            longitude = 11.5820,
            radiusMeters = 200f,
            zoneType = ZoneType.OFFICE,
            color = 0xFF00FF00.toInt()
        )
        coEvery { geofenceDao.update(zone) } returns Unit

        // When
        repository.updateZone(zone)

        // Then
        coVerify { geofenceDao.update(zone) }
    }

    @Test
    fun `deleteZone calls dao delete`() = runTest {
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
        coEvery { geofenceDao.delete(zone) } returns Unit

        // When
        repository.deleteZone(zone)

        // Then
        coVerify { geofenceDao.delete(zone) }
    }

    @Test
    fun `hasRequiredZones returns true when home_station and office zones exist`() = runTest {
        // Given
        coEvery { geofenceDao.getZonesByType(ZoneType.HOME_STATION) } returns listOf(
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
        coEvery { geofenceDao.getZonesByType(ZoneType.OFFICE) } returns listOf(
            GeofenceZone(
                id = "2",
                name = "Office",
                latitude = 48.1351,
                longitude = 11.5820,
                radiusMeters = 150f,
                zoneType = ZoneType.OFFICE,
                color = 0xFF00FF00.toInt()
            )
        )

        // When
        val result = repository.hasRequiredZones()

        // Then
        assertEquals(true, result)
    }

    @Test
    fun `hasRequiredZones returns false when home_station is missing`() = runTest {
        // Given
        coEvery { geofenceDao.getZonesByType(ZoneType.HOME_STATION) } returns emptyList()
        coEvery { geofenceDao.getZonesByType(ZoneType.OFFICE) } returns listOf(
            GeofenceZone(
                id = "2",
                name = "Office",
                latitude = 48.1351,
                longitude = 11.5820,
                radiusMeters = 150f,
                zoneType = ZoneType.OFFICE,
                color = 0xFF00FF00.toInt()
            )
        )

        // When
        val result = repository.hasRequiredZones()

        // Then
        assertEquals(false, result)
    }

    @Test
    fun `hasRequiredZones returns false when office is missing`() = runTest {
        // Given
        coEvery { geofenceDao.getZonesByType(ZoneType.HOME_STATION) } returns listOf(
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
        coEvery { geofenceDao.getZonesByType(ZoneType.OFFICE) } returns emptyList()

        // When
        val result = repository.hasRequiredZones()

        // Then
        assertEquals(false, result)
    }

    @Test
    fun `hasRequiredZones returns false when both zones are missing`() = runTest {
        // Given
        coEvery { geofenceDao.getZonesByType(ZoneType.HOME_STATION) } returns emptyList()
        coEvery { geofenceDao.getZonesByType(ZoneType.OFFICE) } returns emptyList()

        // When
        val result = repository.hasRequiredZones()

        // Then
        assertEquals(false, result)
    }
}
