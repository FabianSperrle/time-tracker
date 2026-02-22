package com.example.worktimetracker.service

import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.homeoffice.HomeOfficeTracker
import com.example.worktimetracker.domain.model.BeaconConfig
import com.example.worktimetracker.domain.model.TimeWindow
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.altbeacon.beacon.BeaconManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalTime

/**
 * Unit tests for BeaconScanner.
 *
 * Tests cover:
 * - Configuration retrieval
 * - Beacon detection and lastSeenTimestamp tracking
 * - Timeout mechanism (start, cancel, completion)
 * - State reset on stopMonitoring
 * - Time window helper methods
 *
 * Note: startMonitoring() cannot be called in pure unit tests because it
 * requires AltBeacon's Identifier.parse() which depends on Android framework.
 * We use reflection to set currentConfig directly for testing the logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BeaconScannerTest {

    private lateinit var beaconManager: BeaconManager
    private lateinit var settingsProvider: SettingsProvider
    private lateinit var homeOfficeTracker: HomeOfficeTracker
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

    private val testConfigWithThreshold = testConfig.copy(rssiThreshold = -70)

    @BeforeEach
    fun setUp() {
        testScope = TestScope(testDispatcher)
        beaconManager = mockk(relaxed = true)
        settingsProvider = mockk()
        homeOfficeTracker = mockk(relaxed = true)

        every { settingsProvider.beaconUuid } returns flowOf(testConfig.uuid)
        every { settingsProvider.bleScanInterval } returns flowOf(testConfig.scanIntervalMs)
        every { settingsProvider.beaconTimeout } returns flowOf(testConfig.timeoutMinutes)
        every { settingsProvider.workTimeWindow } returns flowOf(testConfig.validTimeWindow)
        every { settingsProvider.beaconRssiThreshold } returns flowOf(null)

        beaconScanner = BeaconScanner(
            beaconManager = beaconManager,
            settingsProvider = settingsProvider,
            homeOfficeTracker = homeOfficeTracker,
            scope = testScope
        )
    }

    /**
     * Sets the currentConfig field via reflection to simulate startMonitoring()
     * without requiring the Android-dependent AltBeacon Identifier.parse().
     */
    private fun setCurrentConfig(config: BeaconConfig = testConfig) {
        val field = BeaconScanner::class.java.getDeclaredField("currentConfig")
        field.isAccessible = true
        field.set(beaconScanner, config)
    }

    // ========== Configuration Tests ==========

    @Nested
    inner class ConfigurationTests {
        @Test
        fun `getBeaconConfig returns correct config from settings`() = runTest {
            val config = beaconScanner.getBeaconConfig()

            assertEquals(testUuid, config.uuid)
            assertEquals(60_000L, config.scanIntervalMs)
            assertEquals(10, config.timeoutMinutes)
        }

        @Test
        fun `BeaconConfig has correct default values`() {
            val config = BeaconConfig(uuid = testUuid)

            assertEquals(testUuid, config.uuid)
            assertEquals(60_000L, config.scanIntervalMs)
            assertEquals(10, config.timeoutMinutes)
            assertEquals(LocalTime.of(6, 0), config.validTimeWindow.start)
            assertEquals(LocalTime.of(22, 0), config.validTimeWindow.end)
            assertNull(config.rssiThreshold)
        }

        @Test
        fun `getBeaconConfig includes rssiThreshold from settings when configured`() = runTest {
            every { settingsProvider.beaconRssiThreshold } returns flowOf(-70)

            val config = beaconScanner.getBeaconConfig()

            assertEquals(-70, config.rssiThreshold)
        }

        @Test
        fun `getBeaconConfig has null rssiThreshold when not calibrated`() = runTest {
            every { settingsProvider.beaconRssiThreshold } returns flowOf(null)

            val config = beaconScanner.getBeaconConfig()

            assertNull(config.rssiThreshold)
        }
    }

    // ========== Initial State Tests ==========

    @Nested
    inner class InitialStateTests {
        @Test
        fun `timeoutJob is null initially`() {
            assertNull(beaconScanner.timeoutJob)
        }

        @Test
        fun `getLastSeenTimestamp returns null initially`() {
            assertNull(beaconScanner.getLastSeenTimestamp())
        }

        @Test
        fun `isMonitoringActive is false initially`() {
            assertFalse(beaconScanner.isMonitoringActive)
        }
    }

    // ========== Beacon Detection Tests ==========

    @Nested
    inner class BeaconDetectionTests {
        @Test
        fun `onBeaconDetected sets lastSeenTimestamp`() = testScope.runTest {
            // Arrange
            setCurrentConfig()

            // Act
            beaconScanner.onBeaconDetected()

            // Assert
            assertNotNull(beaconScanner.getLastSeenTimestamp())
        }

        @Test
        fun `onBeaconDetected cancels existing timeout job`() = testScope.runTest {
            // Arrange
            setCurrentConfig()

            // Start a timeout first
            beaconScanner.onBeaconLostFromRegion()
            assertNotNull(beaconScanner.timeoutJob)
            assertTrue(beaconScanner.timeoutJob!!.isActive)

            // Act: beacon detected again cancels timeout
            beaconScanner.onBeaconDetected()

            // Assert
            assertNull(beaconScanner.timeoutJob)
        }

        @Test
        fun `onBeaconDetected delegates to HomeOfficeTracker`() = testScope.runTest {
            // Arrange
            setCurrentConfig()

            // Act
            beaconScanner.onBeaconDetected()

            // Assert
            coVerify {
                homeOfficeTracker.onBeaconDetected(uuid = testUuid, timestamp = any())
            }
        }
    }

    // ========== Timeout Mechanism Tests ==========

    @Nested
    inner class TimeoutTests {
        @Test
        fun `onBeaconLostFromRegion starts timeout job`() = testScope.runTest {
            // Arrange
            setCurrentConfig()

            // Act
            beaconScanner.onBeaconLostFromRegion()

            // Assert
            assertNotNull(beaconScanner.timeoutJob)
            assertTrue(beaconScanner.timeoutJob!!.isActive)
        }

        @Test
        fun `timeout triggers HomeOfficeTracker after configured minutes`() = testScope.runTest {
            // Arrange
            setCurrentConfig()
            // Simulate a beacon detection first to set lastSeenTimestamp
            beaconScanner.onBeaconDetected()

            // Act
            beaconScanner.onBeaconLostFromRegion()
            advanceTimeBy(10 * 60_000L + 1) // 10 minutes + 1ms

            // Assert
            coVerify {
                homeOfficeTracker.onBeaconTimeout(timestamp = any(), lastSeenTimestamp = any())
            }
        }

        @Test
        fun `timeout does not trigger before configured minutes`() = testScope.runTest {
            // Arrange
            setCurrentConfig()

            // Act
            beaconScanner.onBeaconLostFromRegion()
            advanceTimeBy(9 * 60_000L) // Only 9 minutes

            // Assert: event should NOT have been sent yet
            coVerify(exactly = 0) { homeOfficeTracker.onBeaconTimeout(any(), any()) }
        }

        @Test
        fun `short absence does not trigger BeaconLost - beacon re-detected before timeout`() = testScope.runTest {
            // Arrange: AC #3 - kurze Abwesenheiten unterbrechen Tracking nicht
            setCurrentConfig()
            beaconScanner.onBeaconDetected()

            // Act: beacon lost, then re-detected after 5 minutes (before 10 min timeout)
            beaconScanner.onBeaconLostFromRegion()
            advanceTimeBy(5 * 60_000L) // 5 minutes
            beaconScanner.onBeaconDetected() // Re-detected, cancels timeout

            // Verify timeout was cancelled
            assertNull(beaconScanner.timeoutJob)

            // Wait remaining time plus some extra
            advanceTimeBy(6 * 60_000L)

            // Assert: BeaconTimeout should NOT have been triggered (only 2 calls from onBeaconDetected)
            coVerify(exactly = 0) {
                homeOfficeTracker.onBeaconTimeout(any(), any())
            }
        }

        @Test
        fun `repeated beacon exits reset the timeout`() = testScope.runTest {
            // Arrange
            setCurrentConfig()

            // Act: first exit
            beaconScanner.onBeaconLostFromRegion()

            advanceTimeBy(5 * 60_000L) // 5 minutes

            // Second exit resets the timer
            beaconScanner.onBeaconLostFromRegion()
            val secondJob = beaconScanner.timeoutJob

            // Assert: new job was created
            assertNotNull(secondJob)
            assertTrue(secondJob!!.isActive)

            // Original 10 min mark (from first exit) - should not trigger
            advanceTimeBy(5 * 60_000L + 1)
            coVerify(exactly = 0) { homeOfficeTracker.onBeaconTimeout(any(), any()) }

            // 10 min from second exit
            advanceTimeBy(5 * 60_000L)
            coVerify(exactly = 1) { homeOfficeTracker.onBeaconTimeout(any(), any()) }
        }

        @Test
        fun `onBeaconLostFromRegion does nothing without config`() = testScope.runTest {
            // Arrange: no setCurrentConfig called, so currentConfig is null

            // Act
            beaconScanner.onBeaconLostFromRegion()

            // Assert: no timeout job should be created
            assertNull(beaconScanner.timeoutJob)
        }

        @Test
        fun `BeaconTimeout includes lastSeenTimestamp for end time correction`() = testScope.runTest {
            // Arrange: AC #5 - Endzeit auf letzten Beacon-Kontakt gesetzt
            setCurrentConfig()
            beaconScanner.onBeaconDetected()
            val lastSeen = beaconScanner.getLastSeenTimestamp()
            assertNotNull(lastSeen)

            // Act: beacon lost, wait for timeout
            beaconScanner.onBeaconLostFromRegion()
            advanceTimeBy(10 * 60_000L + 1)

            // Assert: onBeaconTimeout should be called with lastSeenTimestamp
            coVerify {
                homeOfficeTracker.onBeaconTimeout(timestamp = any(), lastSeenTimestamp = match { it != null })
            }
        }
    }

    // ========== Stop Monitoring Tests ==========

    @Nested
    inner class StopMonitoringTests {
        @Test
        fun `stopMonitoring resets all state including currentRegion and currentConfig`() = testScope.runTest {
            // Arrange: set up some state
            setCurrentConfig()
            beaconScanner.onBeaconDetected()
            beaconScanner.onBeaconLostFromRegion()
            assertNotNull(beaconScanner.getLastSeenTimestamp())
            assertNotNull(beaconScanner.timeoutJob)

            // Act
            beaconScanner.stopMonitoring()

            // Assert: everything is reset (Finding 5)
            assertNull(beaconScanner.getLastSeenTimestamp())
            assertNull(beaconScanner.timeoutJob)
            assertFalse(beaconScanner.isMonitoringActive)

            // Verify currentConfig is null by checking onBeaconLostFromRegion does nothing
            beaconScanner.onBeaconLostFromRegion()
            assertNull(beaconScanner.timeoutJob) // Would be set if config existed
        }

        @Test
        fun `stopMonitoring is safe to call when not monitoring`() {
            // Act: should not throw
            beaconScanner.stopMonitoring()

            // Assert
            assertFalse(beaconScanner.isMonitoringActive)
        }

        @Test
        fun `stopScheduledMonitoring stops monitoring and cancels schedule`() = testScope.runTest {
            // Arrange
            setCurrentConfig()

            // Start scheduled monitoring by setting up schedule job
            beaconScanner.startScheduledMonitoring()

            // Act
            beaconScanner.stopScheduledMonitoring()

            // Assert
            assertFalse(beaconScanner.isMonitoringActive)
        }
    }

    // ========== RSSI Threshold Edge-Trigger Tests ==========

    @Nested
    inner class RssiThresholdTests {
        @Test
        fun `applyRssiRanging calls onBeaconDetected on false-to-true transition`() = testScope.runTest {
            // Arrange: isBeaconInRssiRange starts false, config set for delegation
            setCurrentConfig(testConfigWithThreshold)

            // Act: transition false -> true
            beaconScanner.applyRssiRanging(nowInRange = true)
            testScope.testScheduler.runCurrent()

            // Assert: onBeaconDetected was called
            coVerify { homeOfficeTracker.onBeaconDetected(uuid = testUuid, timestamp = any()) }
            assertTrue(beaconScanner.isBeaconInRssiRange)
        }

        @Test
        fun `applyRssiRanging does not call onBeaconDetected if already in range`() = testScope.runTest {
            // Arrange: pre-set isBeaconInRssiRange to true
            setCurrentConfig(testConfigWithThreshold)
            beaconScanner.isBeaconInRssiRange = true

            // Act: stays true (no edge)
            beaconScanner.applyRssiRanging(nowInRange = true)
            testScope.testScheduler.runCurrent()

            // Assert: no call
            coVerify(exactly = 0) { homeOfficeTracker.onBeaconDetected(any(), any()) }
        }

        @Test
        fun `applyRssiRanging calls onBeaconLostFromRegion on true-to-false transition`() = testScope.runTest {
            // Arrange: pre-set isBeaconInRssiRange to true, config must be set for timeout
            setCurrentConfig(testConfigWithThreshold)
            beaconScanner.isBeaconInRssiRange = true

            // Act: transition true -> false
            beaconScanner.applyRssiRanging(nowInRange = false)
            testScope.testScheduler.runCurrent()

            // Assert: timeout job started (onBeaconLostFromRegion starts timeout)
            assertNotNull(beaconScanner.timeoutJob)
            assertFalse(beaconScanner.isBeaconInRssiRange)
        }

        @Test
        fun `applyRssiRanging does not call onBeaconLostFromRegion if already out of range`() = testScope.runTest {
            // Arrange: isBeaconInRssiRange is already false
            setCurrentConfig(testConfigWithThreshold)

            // Act: stays false (no edge)
            beaconScanner.applyRssiRanging(nowInRange = false)
            testScope.testScheduler.runCurrent()

            // Assert: no timeout job
            assertNull(beaconScanner.timeoutJob)
        }

        @Test
        fun `stopMonitoring resets isBeaconInRssiRange to false`() = testScope.runTest {
            // Arrange: set in-range
            setCurrentConfig(testConfigWithThreshold)
            beaconScanner.isBeaconInRssiRange = true

            // Act
            beaconScanner.stopMonitoring()

            // Assert
            assertFalse(beaconScanner.isBeaconInRssiRange)
        }
    }

    // ========== Time Window Helper Tests ==========

    @Nested
    inner class TimeWindowHelperTests {
        @Test
        fun `isTimeInWindow returns true for time within window`() {
            val window = TimeWindow(start = LocalTime.of(6, 0), end = LocalTime.of(22, 0))

            assertTrue(BeaconScanner.isTimeInWindow(LocalTime.of(12, 0), window))
            assertTrue(BeaconScanner.isTimeInWindow(LocalTime.of(6, 0), window))
            assertTrue(BeaconScanner.isTimeInWindow(LocalTime.of(22, 0), window))
        }

        @Test
        fun `isTimeInWindow returns false for time outside window`() {
            val window = TimeWindow(start = LocalTime.of(6, 0), end = LocalTime.of(22, 0))

            assertFalse(BeaconScanner.isTimeInWindow(LocalTime.of(5, 59), window))
            assertFalse(BeaconScanner.isTimeInWindow(LocalTime.of(22, 1), window))
            assertFalse(BeaconScanner.isTimeInWindow(LocalTime.of(0, 0), window))
        }

        @Test
        fun `millisUntilTime calculates correct delay for future time`() {
            val now = LocalTime.of(6, 0)
            val target = LocalTime.of(22, 0)

            val result = BeaconScanner.millisUntilTime(now, target)

            // 16 hours = 16 * 60 * 60 * 1000 ms
            assertEquals(16L * 60 * 60 * 1000, result)
        }

        @Test
        fun `millisUntilTime wraps to next day for past time`() {
            val now = LocalTime.of(22, 0)
            val target = LocalTime.of(6, 0)

            val result = BeaconScanner.millisUntilTime(now, target)

            // 8 hours = 8 * 60 * 60 * 1000 ms
            assertEquals(8L * 60 * 60 * 1000, result)
        }

        @Test
        fun `millisUntilTime returns full day for same time`() {
            val now = LocalTime.of(10, 0)
            val target = LocalTime.of(10, 0)

            val result = BeaconScanner.millisUntilTime(now, target)

            // Same time means next day = 24 hours
            assertEquals(24L * 60 * 60 * 1000, result)
        }
    }
}
