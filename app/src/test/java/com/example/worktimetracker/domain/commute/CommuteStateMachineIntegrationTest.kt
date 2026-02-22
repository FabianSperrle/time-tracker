package com.example.worktimetracker.domain.commute

import app.cash.turbine.test
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.local.entity.ZoneType
import com.example.worktimetracker.data.repository.TrackingRepository
import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.model.TimeWindow
import com.example.worktimetracker.domain.tracking.TrackingEvent
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import com.example.worktimetracker.domain.tracking.TrackingStateStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class CommuteStateMachineIntegrationTest {

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
        commuteDayChecker = CommuteDayChecker(settingsProvider)

        // Default settings: Mon-Fri commute days
        every { settingsProvider.commuteDays } returns flowOf(
            setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        )
        every { settingsProvider.outboundWindow } returns flowOf(TimeWindow.DEFAULT_OUTBOUND)
        every { settingsProvider.returnWindow } returns flowOf(TimeWindow.DEFAULT_RETURN)
        every { settingsProvider.workTimeWindow } returns flowOf(TimeWindow.DEFAULT_WORK_TIME)
        every { settingsProvider.beaconTimeout } returns flowOf(10)

        coEvery { repository.getActiveEntry() } returns null

        stateMachine = TrackingStateMachine(
            repository, settingsProvider, stateStorage, commutePhaseTracker, commuteDayChecker
        )
    }

    // ========== Full Commute Day Flow ==========

    @Test
    fun `full commute day flow - outbound, office, return, completed`() = runTest {
        // 07:45 - Enter home station geofence
        val outboundTime = LocalDateTime.of(2026, 2, 9, 7, 45)
        val officeEnterTime = LocalDateTime.of(2026, 2, 9, 8, 32)
        val officeExitTime = LocalDateTime.of(2026, 2, 9, 16, 45)
        val returnTime = LocalDateTime.of(2026, 2, 9, 17, 23)

        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = outboundTime.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = outboundTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns trackingEntry

        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            // Step 1: Enter home station -> OUTBOUND
            stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, outboundTime))
            val trackingState = awaitItem()
            assertTrue(trackingState is TrackingState.Tracking)
            assertEquals(TrackingType.COMMUTE_OFFICE, (trackingState as TrackingState.Tracking).type)
        }

        // Verify commute phase is OUTBOUND
        commutePhaseTracker.currentPhase.test {
            assertEquals(CommutePhase.OUTBOUND, awaitItem())
        }

        // Step 2: Enter office -> IN_OFFICE
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.OFFICE, officeEnterTime))
        commutePhaseTracker.currentPhase.test {
            assertEquals(CommutePhase.IN_OFFICE, awaitItem())
        }

        // Step 3: Exit office in return window -> RETURN phase + auto-pause
        stateMachine.processEvent(TrackingEvent.GeofenceExited(ZoneType.OFFICE, officeExitTime))
        commutePhaseTracker.currentPhase.test {
            assertEquals(CommutePhase.RETURN, awaitItem())
        }

        // Step 4: Enter home station while paused -> COMPLETED, tracking stops
        stateMachine.state.test {
            val currentState = awaitItem()
            assertTrue(currentState is TrackingState.Paused) // paused after office exit

            stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, returnTime))
            val idleState = awaitItem()
            assertTrue(idleState is TrackingState.Idle)
        }

        // Verify stopTracking called with event timestamp, not now()
        coVerify { repository.stopTracking("entry-1", returnTime) }

        // Phase should be COMPLETED (not reset to null)
        commutePhaseTracker.currentPhase.test {
            assertEquals(CommutePhase.COMPLETED, awaitItem())
        }
    }

    // ========== Edge Cases ==========

    @Test
    fun `GeofenceEntered OFFICE during commute sets phase to IN_OFFICE`() = runTest {
        val outboundTime = LocalDateTime.of(2026, 2, 9, 7, 45)
        val officeTime = LocalDateTime.of(2026, 2, 9, 8, 30)

        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = outboundTime.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = outboundTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns trackingEntry

        // Start commute
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, outboundTime))

        // Enter office
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.OFFICE, officeTime))

        commutePhaseTracker.currentPhase.test {
            assertEquals(CommutePhase.IN_OFFICE, awaitItem())
        }

        // Tracking state should still be active (no state change from office enter)
        assertTrue(stateMachine.state.value is TrackingState.Tracking)
    }

    @Test
    fun `GeofenceExited OFFICE outside return window does not change tracking state`() = runTest {
        val outboundTime = LocalDateTime.of(2026, 2, 9, 7, 45)
        val officeTime = LocalDateTime.of(2026, 2, 9, 8, 30)
        // Lunch break exit at 12:00 - outside return window
        val lunchExitTime = LocalDateTime.of(2026, 2, 9, 12, 0)

        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = outboundTime.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = outboundTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns trackingEntry

        // Start commute and enter office
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, outboundTime))
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.OFFICE, officeTime))

        // Exit office at lunch - phase changes but tracking continues
        stateMachine.processEvent(TrackingEvent.GeofenceExited(ZoneType.OFFICE, lunchExitTime))

        // Tracking state should still be active
        assertTrue(stateMachine.state.value is TrackingState.Tracking)
    }

    @Test
    fun `GeofenceEntered HOME_STATION ignored on non-commute Saturday`() = runTest {
        // Saturday, 08:00
        every { settingsProvider.commuteDays } returns flowOf(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        )
        val newChecker = CommuteDayChecker(settingsProvider)
        val newStateMachine = TrackingStateMachine(
            repository, settingsProvider, stateStorage, commutePhaseTracker, newChecker
        )

        val saturdayTime = LocalDateTime.of(2026, 2, 14, 8, 0)

        newStateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            newStateMachine.processEvent(
                TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, saturdayTime)
            )

            expectNoEvents()
            coVerify(exactly = 0) { repository.startTracking(any(), any()) }
        }
    }

    @Test
    fun `GeofenceEntered HOME_STATION ignored outside outbound window`() = runTest {
        // Monday, 11:00 - outside outbound window
        val lateTime = LocalDateTime.of(2026, 2, 9, 11, 0)

        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            stateMachine.processEvent(
                TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, lateTime)
            )

            expectNoEvents()
            coVerify(exactly = 0) { repository.startTracking(any(), any()) }
        }
    }

    @Test
    fun `return to home station without office visit stops tracking per spec`() = runTest {
        // Spec edge case: "Buero-Geofence nie betreten -- Tracking laeuft weiter
        // bis manueller Stop oder Rueckkehr zum Bahnhof"
        val outboundTime = LocalDateTime.of(2026, 2, 9, 7, 45)
        val returnTime = LocalDateTime.of(2026, 2, 9, 17, 0)

        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = outboundTime.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = outboundTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns trackingEntry

        // Start commute (OUTBOUND phase)
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, outboundTime))
        assertTrue(stateMachine.state.value is TrackingState.Tracking)

        // Return to home station WITHOUT visiting office (phase still OUTBOUND)
        stateMachine.state.test {
            val currentState = awaitItem()
            assertTrue(currentState is TrackingState.Tracking)

            stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, returnTime))
            val idleState = awaitItem()
            assertTrue(idleState is TrackingState.Idle)
        }

        coVerify { repository.stopTracking("entry-1", returnTime) }

        // Phase should be COMPLETED
        commutePhaseTracker.currentPhase.test {
            assertEquals(CommutePhase.COMPLETED, awaitItem())
        }
    }

    @Test
    fun `COMPLETED phase persists after auto-stop and resets on next commute start`() = runTest {
        // Verifies Finding 3: COMPLETED phase is observable by UI before reset
        val outboundTime = LocalDateTime.of(2026, 2, 9, 7, 45)
        val officeEnterTime = LocalDateTime.of(2026, 2, 9, 8, 32)
        val officeExitTime = LocalDateTime.of(2026, 2, 9, 16, 45)
        val returnTime = LocalDateTime.of(2026, 2, 9, 17, 23)

        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = outboundTime.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = outboundTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns trackingEntry

        // Complete full commute
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, outboundTime))
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.OFFICE, officeEnterTime))
        stateMachine.processEvent(TrackingEvent.GeofenceExited(ZoneType.OFFICE, officeExitTime))
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, returnTime))

        // Phase should be COMPLETED (not null)
        assertEquals(CommutePhase.COMPLETED, commutePhaseTracker.currentPhase.value)

        // Next day: starting a new commute resets the completed phase
        val nextDayOutbound = LocalDateTime.of(2026, 2, 10, 7, 45)
        val nextDayEntry = TrackingEntry(
            id = "entry-2",
            date = nextDayOutbound.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = nextDayOutbound,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns nextDayEntry

        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, nextDayOutbound))

        // Phase should now be OUTBOUND (new commute started, old COMPLETED overwritten)
        assertEquals(CommutePhase.OUTBOUND, commutePhaseTracker.currentPhase.value)
    }

    // ========== OFFICE_STATION Auto-Pause/Resume Integration Tests ==========

    @Test
    fun `full day happy path with OFFICE_STATION pause and resume`() = runTest {
        // Full flow: HOME_STATION → OFFICE_STATION (pause) → OFFICE (resume) →
        //            EXIT OFFICE (pause) → EXIT OFFICE_STATION (resume) → HOME_STATION (stop)
        val outboundTime = LocalDateTime.of(2026, 2, 9, 7, 45)
        val officeStationEnterTime = LocalDateTime.of(2026, 2, 9, 8, 10)
        val officeEnterTime = LocalDateTime.of(2026, 2, 9, 8, 25)
        val officeExitTime = LocalDateTime.of(2026, 2, 9, 17, 0)
        val officeStationExitTime = LocalDateTime.of(2026, 2, 9, 17, 18)
        val returnTime = LocalDateTime.of(2026, 2, 9, 17, 30)

        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = outboundTime.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = outboundTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns trackingEntry
        coEvery { repository.startPause("entry-1") } returns "pause-1"
        coEvery { repository.getActiveEntry() } returns trackingEntry

        // Step 1: Start commute
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, outboundTime))
        assertTrue(stateMachine.state.value is TrackingState.Tracking)
        assertEquals(CommutePhase.OUTBOUND, commutePhaseTracker.currentPhase.value)

        // Step 2: Enter OFFICE_STATION → pause (morning walk starts)
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.OFFICE_STATION, officeStationEnterTime))
        assertTrue(stateMachine.state.value is TrackingState.Paused)

        // Step 3: Enter OFFICE → resume (arrived at work)
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.OFFICE, officeEnterTime))
        assertTrue(stateMachine.state.value is TrackingState.Tracking)
        assertEquals(CommutePhase.IN_OFFICE, commutePhaseTracker.currentPhase.value)

        // Step 4: Exit OFFICE in return window → pause (evening walk starts)
        stateMachine.processEvent(TrackingEvent.GeofenceExited(ZoneType.OFFICE, officeExitTime))
        assertTrue(stateMachine.state.value is TrackingState.Paused)
        assertEquals(CommutePhase.RETURN, commutePhaseTracker.currentPhase.value)

        // Step 5: Exit OFFICE_STATION → resume (on the train home)
        stateMachine.processEvent(TrackingEvent.GeofenceExited(ZoneType.OFFICE_STATION, officeStationExitTime))
        assertTrue(stateMachine.state.value is TrackingState.Tracking)

        // Step 6: Enter HOME_STATION → stop tracking
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, returnTime))
        assertTrue(stateMachine.state.value is TrackingState.Idle)
        assertEquals(CommutePhase.COMPLETED, commutePhaseTracker.currentPhase.value)

        // Verify: startPause ×2, stopPause ×2, stopTracking ×1 with correct timestamp
        coVerify(exactly = 2) { repository.startPause("entry-1") }
        coVerify(exactly = 2) { repository.stopPause("entry-1") }
        coVerify(exactly = 1) { repository.stopTracking("entry-1", endTime = returnTime) }
    }

    @Test
    fun `EXIT OFFICE_STATION never fires - stop tracking from PAUSED via HOME_STATION`() = runTest {
        // Edge case: EXIT OFFICE_STATION is never detected, user arrives at HOME_STATION
        // while still in PAUSED state (after EXIT OFFICE). Tracking should stop correctly.
        val outboundTime = LocalDateTime.of(2026, 2, 9, 7, 45)
        val officeEnterTime = LocalDateTime.of(2026, 2, 9, 8, 30)
        val officeExitTime = LocalDateTime.of(2026, 2, 9, 17, 0) // return window → auto-pause
        val returnTime = LocalDateTime.of(2026, 2, 9, 17, 35) // in return window

        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = outboundTime.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = outboundTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns trackingEntry
        coEvery { repository.startPause("entry-1") } returns "pause-1"

        // Start commute → tracking
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, outboundTime))
        assertTrue(stateMachine.state.value is TrackingState.Tracking)

        // Enter office (no OFFICE_STATION pause in this scenario)
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.OFFICE, officeEnterTime))
        assertEquals(CommutePhase.IN_OFFICE, commutePhaseTracker.currentPhase.value)

        // Exit office in return window → auto-pause
        stateMachine.processEvent(TrackingEvent.GeofenceExited(ZoneType.OFFICE, officeExitTime))
        assertTrue(stateMachine.state.value is TrackingState.Paused)
        assertEquals(CommutePhase.RETURN, commutePhaseTracker.currentPhase.value)

        // EXIT OFFICE_STATION never fires; directly enter HOME_STATION while PAUSED
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, returnTime))
        assertTrue(stateMachine.state.value is TrackingState.Idle)
        assertEquals(CommutePhase.COMPLETED, commutePhaseTracker.currentPhase.value)

        coVerify(exactly = 1) { repository.startPause("entry-1") }
        coVerify(exactly = 1) { repository.stopPause("entry-1") }
        coVerify(exactly = 1) { repository.stopTracking("entry-1", endTime = returnTime) }
    }

    @Test
    fun `commute phase resets when tracking stops manually`() = runTest {
        val outboundTime = LocalDateTime.of(2026, 2, 9, 7, 45)

        val trackingEntry = TrackingEntry(
            id = "entry-1",
            date = outboundTime.toLocalDate(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = outboundTime,
            endTime = null,
            autoDetected = true
        )
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, true) } returns trackingEntry

        // Start commute
        stateMachine.processEvent(TrackingEvent.GeofenceEntered(ZoneType.HOME_STATION, outboundTime))

        commutePhaseTracker.currentPhase.test {
            assertEquals(CommutePhase.OUTBOUND, awaitItem())
        }

        // Manual stop
        stateMachine.processEvent(TrackingEvent.ManualStop)

        commutePhaseTracker.currentPhase.test {
            assertEquals(null, awaitItem())
        }
    }
}
