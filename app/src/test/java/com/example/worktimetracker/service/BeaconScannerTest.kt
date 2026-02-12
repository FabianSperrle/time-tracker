package com.example.worktimetracker.service

import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.model.BeaconConfig
import com.example.worktimetracker.domain.model.TimeWindow
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.altbeacon.beacon.BeaconManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalTime

/**
 * Unit tests for BeaconScanner.
 *
 * Note: Full integration testing of BLE scanning requires real hardware.
 * These tests verify the configuration logic and basic structure.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BeaconScannerTest {

    private lateinit var beaconManager: BeaconManager
    private lateinit var settingsProvider: SettingsProvider
    private lateinit var stateMachine: TrackingStateMachine
    private lateinit var beaconScanner: BeaconScanner
    private lateinit var testScope: TestScope
    private val testDispatcher = StandardTestDispatcher()

    private val testUuid = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
    private val testConfig = BeaconConfig(
        uuid = testUuid,
        scanIntervalMs = 60_000,
        timeoutMinutes = 10,
        validTimeWindow = TimeWindow(
            start = LocalTime.of(6, 0),
            end = LocalTime.of(22, 0)
        )
    )

    @BeforeEach
    fun setUp() {
        testScope = TestScope(testDispatcher)
        beaconManager = mockk(relaxed = true)
        settingsProvider = mockk()
        stateMachine = mockk(relaxed = true)

        every { settingsProvider.beaconUuid } returns flowOf(testConfig.uuid)
        every { settingsProvider.bleScanInterval } returns flowOf(testConfig.scanIntervalMs)
        every { settingsProvider.beaconTimeout } returns flowOf(testConfig.timeoutMinutes)
        every { settingsProvider.workTimeWindow } returns flowOf(testConfig.validTimeWindow)

        beaconScanner = BeaconScanner(
            beaconManager = beaconManager,
            settingsProvider = settingsProvider,
            stateMachine = stateMachine,
            scope = testScope
        )
    }

    @Test
    fun `getBeaconConfig should return correct config from settings`() = runTest {
        // When
        val config = beaconScanner.getBeaconConfig()

        // Then
        assertEquals(testUuid, config.uuid)
        assertEquals(60_000L, config.scanIntervalMs)
        assertEquals(10, config.timeoutMinutes)
    }

    @Test
    fun `BeaconConfig has correct default values`() {
        // Given
        val uuid = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"

        // When
        val config = BeaconConfig(uuid = uuid)

        // Then
        assertEquals(uuid, config.uuid)
        assertEquals(60_000L, config.scanIntervalMs)
        assertEquals(10, config.timeoutMinutes)
        assertEquals(LocalTime.of(6, 0), config.validTimeWindow.start)
        assertEquals(LocalTime.of(22, 0), config.validTimeWindow.end)
    }

    @Test
    fun `BeaconScanner is created with correct dependencies`() {
        // When/Then - no exception should be thrown
        assertEquals(true, beaconScanner is BeaconScanner)
    }

    @Test
    fun `timeoutJob is null initially`() {
        // Then
        assertNull(beaconScanner.timeoutJob)
    }

    @Test
    fun `getLastSeenTimestamp returns null initially`() {
        // Then
        assertNull(beaconScanner.getLastSeenTimestamp())
    }
}
