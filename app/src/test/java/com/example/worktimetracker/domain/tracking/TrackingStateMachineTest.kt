package com.example.worktimetracker.domain.tracking

import app.cash.turbine.test
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.local.entity.ZoneType
import com.example.worktimetracker.data.repository.TrackingRepository
import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.commute.CommuteDayChecker
import com.example.worktimetracker.domain.commute.CommutePhaseTracker
import com.example.worktimetracker.domain.model.TimeWindow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TrackingStateMachineTest {

    private lateinit var repository: TrackingRepository
    private lateinit var settingsProvider: SettingsProvider
    private lateinit var stateStorage: TrackingStateStorage
    private lateinit var commutePhaseTracker: CommutePhaseTracker
    private lateinit var commuteDayChecker: CommuteDayChecker
    private lateinit var stateMachine: TrackingStateMachine

    @BeforeEach
    fun setup() {
        repository = mockk(relaxed = true)
        settingsProvider = mockk()
        stateStorage = mockk(relaxed = true)
        commutePhaseTracker = CommutePhaseTracker()

        // Default settings
        every { settingsProvider.commuteDays } returns flowOf(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
        every { settingsProvider.outboundWindow } returns flowOf(TimeWindow.DEFAULT_OUTBOUND)
        every { settingsProvider.returnWindow } returns flowOf(TimeWindow.DEFAULT_RETURN)
        every { settingsProvider.workTimeWindow } returns flowOf(TimeWindow.DEFAULT_WORK_TIME)
        every { settingsProvider.beaconTimeout } returns flowOf(10)

        coEvery { repository.getActiveEntry() } returns null

        commuteDayChecker = CommuteDayChecker(settingsProvider)
        stateMachine = TrackingStateMachine(repository, settingsProvider, stateStorage, commutePhaseTracker, commuteDayChecker)
    }

    // ========== IDLE → TRACKING Tests ==========

    @Test
    fun `IDLE to TRACKING on GeofenceEntered HOME_STATION during outbound window on commute day`() = runTest {
        // Arrange: Monday, 08:00 (valid outbound window)
        val testTime = LocalDateTime.of(2026, 2, 9, 8, 0) // Monday
        val event = TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, testTime)

        val createdEntry = TrackingEntry(
            id = "entry-1",
            date = testTime.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = testTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns createdEntry

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            stateMachine.processEvent(event)

            // Assert
            val newState = awaitItem()
            assertTrue(newState is TrackingState.Tracking)
            assertEquals("entry-1", (newState as TrackingState.Tracking).entryId)
            assertEquals(TrackingType.COMMUTE_OFFICE, newState.type)
            assertEquals(testTime, newState.startTime)

            coVerify { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) }
            coVerify { stateStorage.saveState(newState) }
        }
    }

    @Test
    fun `IDLE ignores GeofenceEntered HOME_STATION outside outbound window`() = runTest {
        // Arrange: Monday, 11:00 (outside outbound window)
        val testTime = LocalDateTime.of(2026, 2, 9, 11, 0)
        val event = TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, testTime)

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            stateMachine.processEvent(event)

            // Assert: State should remain Idle
            expectNoEvents()
            coVerify(exactly = 0) { repository.startTracking(any(), any()) }
        }
    }

    @Test
    fun `IDLE ignores GeofenceEntered HOME_STATION on non-commute day`() = runTest {
        // Arrange: Saturday, 08:00
        val testTime = LocalDateTime.of(2026, 2, 14, 8, 0)
        val event = TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, testTime)

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            stateMachine.processEvent(event)

            // Assert
            expectNoEvents()
            coVerify(exactly = 0) { repository.startTracking(any(), any()) }
        }
    }

    @Test
    fun `IDLE to TRACKING on BeaconDetected during work time window`() = runTest {
        // Arrange: Monday, 09:00
        val testTime = LocalDateTime.of(2026, 2, 9, 9, 0)
        val event = TrackingEvent.BeaconDetected("beacon-uuid", testTime)

        val createdEntry = TrackingEntry(
            id = "entry-2",
            date = testTime.toLocalDate(),
            type = TrackingType.HOME_OFFICE,
            startTime = testTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.HOME_OFFICE, true) } returns createdEntry

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            stateMachine.processEvent(event)

            // Assert
            val newState = awaitItem()
            assertTrue(newState is TrackingState.Tracking)
            assertEquals("entry-2", (newState as TrackingState.Tracking).entryId)
            assertEquals(TrackingType.HOME_OFFICE, newState.type)

            coVerify { repository.startTracking(TrackingType.HOME_OFFICE, true) }
        }
    }

    @Test
    fun `IDLE ignores BeaconDetected outside work time window`() = runTest {
        // Arrange: Monday, 23:00 (outside work time window)
        val testTime = LocalDateTime.of(2026, 2, 9, 23, 0)
        val event = TrackingEvent.BeaconDetected("beacon-uuid", testTime)

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            stateMachine.processEvent(event)

            // Assert
            expectNoEvents()
            coVerify(exactly = 0) { repository.startTracking(any(), any()) }
        }
    }

    @Test
    fun `IDLE to TRACKING on ManualStart`() = runTest {
        // Arrange
        val testTime = LocalDateTime.now()
        val event = TrackingEvent.ManualStart(TrackingType.MANUAL, testTime)

        val createdEntry = TrackingEntry(
            id = "entry-3",
            date = testTime.toLocalDate(),
            type = TrackingType.MANUAL,
            startTime = testTime,
            endTime = null,
            autoDetected = false
        )
        coEvery { repository.startTracking(TrackingType.MANUAL, false) } returns createdEntry

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            stateMachine.processEvent(event)

            // Assert
            val newState = awaitItem()
            assertTrue(newState is TrackingState.Tracking)
            assertEquals("entry-3", (newState as TrackingState.Tracking).entryId)
            assertEquals(TrackingType.MANUAL, newState.type)

            coVerify { repository.startTracking(TrackingType.MANUAL, false) }
        }
    }

    // ========== TRACKING → IDLE Tests ==========

    @Test
    fun `TRACKING to IDLE on GeofenceEntered HOME_STATION during return window`() = runTest {
        // Arrange: Start tracking at 08:00, end at 17:00 (return window)
        val startTime = LocalDateTime.of(2026, 2, 9, 8, 0)
        val returnTime = LocalDateTime.of(2026, 2, 9, 17, 0)

        // First start tracking
        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = startTime.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = startTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns trackingEntry

        val startEvent = TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, startTime)
        val returnEvent = TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, returnTime)

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            // Start tracking
            stateMachine.processEvent(startEvent)
            val trackingState = awaitItem()
            assertTrue(trackingState is TrackingState.Tracking)

            // End tracking
            stateMachine.processEvent(returnEvent)

            // Assert
            val newState = awaitItem()
            assertTrue(newState is TrackingState.Idle)

            // Verify event timestamp is used, not LocalDateTime.now()
            coVerify { repository.stopTracking("entry-1", returnTime) }
            coVerify { stateStorage.saveState(TrackingState.Idle) }
        }
    }

    @Test
    fun `TRACKING ignores GeofenceEntered HOME_STATION outside return window`() = runTest {
        // Arrange: Start tracking at 08:00, try to end at 12:00 (outside return window)
        val startTime = LocalDateTime.of(2026, 2, 9, 8, 0)
        val earlyReturnTime = LocalDateTime.of(2026, 2, 9, 12, 0)

        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = startTime.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = startTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns trackingEntry

        val startEvent = TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, startTime)
        val returnEvent = TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, earlyReturnTime)

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            // Start tracking
            stateMachine.processEvent(startEvent)
            val trackingState = awaitItem()
            assertTrue(trackingState is TrackingState.Tracking)

            // Try to end tracking outside return window - should be ignored
            stateMachine.processEvent(returnEvent)

            // Assert: Should stay in Tracking state
            expectNoEvents()
            coVerify(exactly = 0) { repository.stopTracking(any(), any()) }
        }
    }

    @Test
    fun `TRACKING to IDLE on ManualStop`() = runTest {
        // Arrange: Start tracking
        val testTime = LocalDateTime.now()
        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = testTime.toLocalDate(),
            type = TrackingType.MANUAL,
            startTime = testTime,
            endTime = null,
            autoDetected = false
        )
        coEvery { repository.startTracking(TrackingType.MANUAL, false) } returns trackingEntry

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            // Start tracking
            stateMachine.processEvent(TrackingEvent.ManualStart(TrackingType.MANUAL, testTime))
            assertTrue(awaitItem() is TrackingState.Tracking)

            // Stop tracking
            stateMachine.processEvent(TrackingEvent.ManualStop)

            // Assert
            val newState = awaitItem()
            assertTrue(newState is TrackingState.Idle)

            coVerify { repository.stopTracking("entry-1", any()) }
        }
    }

    @Test
    fun `TRACKING to IDLE on BeaconLost after timeout`() = runTest {
        // Arrange: Start tracking via beacon
        val startTime = LocalDateTime.of(2026, 2, 9, 9, 0)
        val lostTime = LocalDateTime.of(2026, 2, 9, 18, 10) // Timeout fires at 18:10
        val lastSeenTime = LocalDateTime.of(2026, 2, 9, 18, 0) // Beacon last seen at 18:00

        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = startTime.toLocalDate(),
            type = TrackingType.HOME_OFFICE,
            startTime = startTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.HOME_OFFICE, true) } returns trackingEntry

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            // Start tracking
            stateMachine.processEvent(TrackingEvent.BeaconDetected("beacon-uuid", startTime))
            assertTrue(awaitItem() is TrackingState.Tracking)

            // Beacon lost with lastSeenTimestamp for end time correction
            stateMachine.processEvent(
                TrackingEvent.BeaconLost(
                    timestamp = lostTime,
                    lastSeenTimestamp = lastSeenTime
                )
            )

            // Assert
            val newState = awaitItem()
            assertTrue(newState is TrackingState.Idle)

            // Verify end time uses lastSeenTimestamp (18:00), not timeout time (18:10)
            coVerify { repository.stopTracking("entry-1", endTime = lastSeenTime) }
        }
    }

    @Test
    fun `TRACKING to IDLE on BeaconLost without lastSeenTimestamp uses event timestamp`() = runTest {
        // Arrange
        val startTime = LocalDateTime.of(2026, 2, 9, 9, 0)
        val lostTime = LocalDateTime.of(2026, 2, 9, 18, 10)

        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = startTime.toLocalDate(),
            type = TrackingType.HOME_OFFICE,
            startTime = startTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.HOME_OFFICE, true) } returns trackingEntry

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            stateMachine.processEvent(TrackingEvent.BeaconDetected("beacon-uuid", startTime))
            assertTrue(awaitItem() is TrackingState.Tracking)

            // BeaconLost without lastSeenTimestamp
            stateMachine.processEvent(TrackingEvent.BeaconLost(timestamp = lostTime))

            val newState = awaitItem()
            assertTrue(newState is TrackingState.Idle)

            // Falls back to event timestamp when lastSeenTimestamp is null
            coVerify { repository.stopTracking("entry-1", endTime = lostTime) }
        }
    }

    @Test
    fun `TRACKING ignores BeaconLost for non-HOME_OFFICE tracking`() = runTest {
        // Arrange: Start commute tracking (not home office)
        val testTime = LocalDateTime.of(2026, 2, 9, 8, 0) // Monday
        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = testTime.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = testTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns trackingEntry

        val startEvent = TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, testTime)

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            stateMachine.processEvent(startEvent)
            assertTrue(awaitItem() is TrackingState.Tracking)

            // BeaconLost should be ignored for COMMUTE_OFFICE tracking
            stateMachine.processEvent(TrackingEvent.BeaconLost())

            expectNoEvents()
            coVerify(exactly = 0) { repository.stopTracking(any(), any()) }
        }
    }

    // ========== TRACKING → PAUSED Tests ==========

    @Test
    fun `TRACKING to PAUSED on PauseStart`() = runTest {
        // Arrange: Start tracking
        val testTime = LocalDateTime.now()
        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = testTime.toLocalDate(),
            type = TrackingType.MANUAL,
            startTime = testTime,
            endTime = null,
            autoDetected = false
        )
        coEvery { repository.startTracking(TrackingType.MANUAL, false) } returns trackingEntry
        coEvery { repository.startPause("entry-1") } returns "pause-1"

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            // Start tracking
            stateMachine.processEvent(TrackingEvent.ManualStart(TrackingType.MANUAL, testTime))
            assertTrue(awaitItem() is TrackingState.Tracking)

            // Start pause
            stateMachine.processEvent(TrackingEvent.PauseStart)

            // Assert
            val newState = awaitItem()
            assertTrue(newState is TrackingState.Paused)
            assertEquals("entry-1", (newState as TrackingState.Paused).entryId)
            assertEquals("pause-1", newState.pauseId)
            assertEquals(TrackingType.MANUAL, newState.type)

            coVerify { repository.startPause("entry-1") }
            coVerify { stateStorage.saveState(newState) }
        }
    }

    // ========== PAUSED → TRACKING Tests ==========

    @Test
    fun `PAUSED to TRACKING on PauseEnd`() = runTest {
        // Arrange: Start tracking, then pause
        val testTime = LocalDateTime.now()
        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = testTime.toLocalDate(),
            type = TrackingType.MANUAL,
            startTime = testTime,
            endTime = null,
            autoDetected = false
        )
        coEvery { repository.startTracking(TrackingType.MANUAL, false) } returns trackingEntry
        coEvery { repository.startPause("entry-1") } returns "pause-1"
        coEvery { repository.getActiveEntry() } returns trackingEntry

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            // Start tracking
            stateMachine.processEvent(TrackingEvent.ManualStart(TrackingType.MANUAL, testTime))
            val trackingState = awaitItem()
            assertTrue(trackingState is TrackingState.Tracking)

            // Start pause
            stateMachine.processEvent(TrackingEvent.PauseStart)
            val pausedState = awaitItem()
            assertTrue(pausedState is TrackingState.Paused)

            // End pause
            stateMachine.processEvent(TrackingEvent.PauseEnd)

            // Assert
            val newState = awaitItem()
            assertTrue(newState is TrackingState.Tracking)
            assertEquals("entry-1", (newState as TrackingState.Tracking).entryId)

            coVerify { repository.stopPause("entry-1") }
            coVerify(exactly = 2) { stateStorage.saveState(match { it is TrackingState.Tracking }) }
        }
    }

    // ========== PAUSED → IDLE Tests ==========

    @Test
    fun `PAUSED to IDLE on ManualStop`() = runTest {
        // Arrange: Start tracking, then pause
        val testTime = LocalDateTime.now()
        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = testTime.toLocalDate(),
            type = TrackingType.MANUAL,
            startTime = testTime,
            endTime = null,
            autoDetected = false
        )
        coEvery { repository.startTracking(TrackingType.MANUAL, false) } returns trackingEntry
        coEvery { repository.startPause("entry-1") } returns "pause-1"

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            // Start tracking
            stateMachine.processEvent(TrackingEvent.ManualStart(TrackingType.MANUAL, testTime))
            assertTrue(awaitItem() is TrackingState.Tracking)

            // Start pause
            stateMachine.processEvent(TrackingEvent.PauseStart)
            assertTrue(awaitItem() is TrackingState.Paused)

            // Manual stop
            stateMachine.processEvent(TrackingEvent.ManualStop)

            // Assert
            val newState = awaitItem()
            assertTrue(newState is TrackingState.Idle)

            coVerify { repository.stopPause("entry-1") }
            coVerify { repository.stopTracking("entry-1", any()) }
            coVerify { stateStorage.saveState(TrackingState.Idle) }
        }
    }

    // ========== Edge Cases ==========

    @Test
    fun `ignores events that would start second active tracking`() = runTest {
        // Arrange: Already tracking
        val testTime = LocalDateTime.of(2026, 2, 9, 8, 0)
        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = testTime.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = testTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns trackingEntry

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            // Start first tracking
            stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, testTime))
            assertTrue(awaitItem() is TrackingState.Tracking)

            // Try to start second tracking
            stateMachine.processEvent(TrackingEvent.ManualStart(TrackingType.MANUAL, testTime))

            // Assert: Should ignore the second event
            expectNoEvents()
            coVerify(exactly = 1) { repository.startTracking(any(), any()) }
        }
    }

    // ========== Recovery Tests ==========

    @Test
    fun `AppRestarted restores IDLE state`() = runTest {
        // Arrange
        coEvery { stateStorage.loadState() } returns TrackingState.Idle

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            stateMachine.processEvent(TrackingEvent.AppRestarted)

            // Assert: State should remain Idle
            expectNoEvents()
        }
    }

    @Test
    fun `restoreState restores TRACKING state when valid`() = runTest {
        // Arrange
        val savedState = TrackingState.Tracking(
            entryId = "entry-1",
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.now()
        )
        coEvery { stateStorage.loadState() } returns savedState

        val activeEntry = TrackingEntry(
            id = "entry-1",
            date = LocalDate.now(),
            type = TrackingType.MANUAL,
            startTime = savedState.startTime,
            endTime = null,
            autoDetected = false
        )
        coEvery { repository.getActiveEntry() } returns activeEntry

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            stateMachine.restoreState()

            // Assert: Should restore tracking state
            val restoredState = awaitItem()
            assertTrue(restoredState is TrackingState.Tracking)
            assertEquals("entry-1", (restoredState as TrackingState.Tracking).entryId)
        }
    }

    @Test
    fun `restoreState handles corrupted state by resetting to IDLE`() = runTest {
        // Arrange: Saved state says tracking, but no active entry in DB
        val savedState = TrackingState.Tracking(
            entryId = "entry-1",
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.now()
        )
        coEvery { stateStorage.loadState() } returns savedState
        coEvery { repository.getActiveEntry() } returns null

        // Act
        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            stateMachine.restoreState()

            // Assert: Should reset to Idle and save
            expectNoEvents() // State was already Idle, so no change emitted

            coVerify { stateStorage.saveState(TrackingState.Idle) }
        }
    }
}
