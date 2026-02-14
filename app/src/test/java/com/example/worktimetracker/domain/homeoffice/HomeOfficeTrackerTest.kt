package com.example.worktimetracker.domain.homeoffice

import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.commute.CommuteDayChecker
import com.example.worktimetracker.domain.model.TimeWindow
import com.example.worktimetracker.domain.tracking.TrackingEvent
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class HomeOfficeTrackerTest {

    private lateinit var stateMachine: TrackingStateMachine
    private lateinit var commuteDayChecker: CommuteDayChecker
    private lateinit var settingsProvider: SettingsProvider
    private lateinit var tracker: HomeOfficeTracker

    private val stateFlow = MutableStateFlow<TrackingState>(TrackingState.Idle)

    @BeforeEach
    fun setup() {
        stateMachine = mockk(relaxed = true)
        commuteDayChecker = mockk()
        settingsProvider = mockk()

        every { stateMachine.state } returns stateFlow

        tracker = HomeOfficeTracker(
            stateMachine = stateMachine,
            commuteDayChecker = commuteDayChecker,
            settingsProvider = settingsProvider
        )
    }

    @Test
    fun `onBeaconDetected starts tracking when inside work time window and idle`() = runTest {
        // GIVEN: Current time is 09:00, within work window (06:00-22:00)
        val now = LocalDateTime.of(2026, 2, 13, 9, 0)
        val workWindow = TimeWindow(LocalTime.of(6, 0), LocalTime.of(22, 0))

        coEvery { settingsProvider.workTimeWindow } returns flowOf(workWindow)
        coEvery { commuteDayChecker.isCommuteDay(any()) } returns false
        stateFlow.value = TrackingState.Idle

        // WHEN: Beacon is detected
        tracker.onBeaconDetected("test-uuid", now)

        // THEN: BeaconDetected event is processed
        coVerify(exactly = 1) {
            stateMachine.processEvent(
                match { event ->
                    event is TrackingEvent.BeaconDetected &&
                    event.uuid == "test-uuid" &&
                    event.timestamp == now
                }
            )
        }
    }

    @Test
    fun `onBeaconDetected ignores beacon outside work time window`() = runTest {
        // GIVEN: Current time is 23:00, outside work window (06:00-22:00)
        val now = LocalDateTime.of(2026, 2, 13, 23, 0)
        val workWindow = TimeWindow(LocalTime.of(6, 0), LocalTime.of(22, 0))

        coEvery { settingsProvider.workTimeWindow } returns flowOf(workWindow)

        // WHEN: Beacon is detected
        tracker.onBeaconDetected("test-uuid", now)

        // THEN: No event is processed
        coVerify(exactly = 0) {
            stateMachine.processEvent(any())
        }
    }

    @Test
    fun `onBeaconDetected ignores beacon when already tracking`() = runTest {
        // GIVEN: Already tracking HOME_OFFICE (not a commute day)
        val now = LocalDateTime.of(2026, 2, 13, 9, 0)
        val workWindow = TimeWindow(LocalTime.of(6, 0), LocalTime.of(22, 0))

        coEvery { settingsProvider.workTimeWindow } returns flowOf(workWindow)
        coEvery { commuteDayChecker.isCommuteDay(any()) } returns false
        stateFlow.value = TrackingState.Tracking(
            entryId = "123",
            type = TrackingType.HOME_OFFICE,
            startTime = now.minusHours(1)
        )

        // WHEN: Beacon is detected
        tracker.onBeaconDetected("test-uuid", now)

        // THEN: No event is processed
        coVerify(exactly = 0) {
            stateMachine.processEvent(any())
        }
    }

    @Test
    fun `onBeaconDetected starts tracking on commute day when no commute tracking active`() = runTest {
        // GIVEN: It's a commute day but no tracking active
        val now = LocalDateTime.of(2026, 2, 13, 9, 0)
        val workWindow = TimeWindow(LocalTime.of(6, 0), LocalTime.of(22, 0))

        coEvery { settingsProvider.workTimeWindow } returns flowOf(workWindow)
        coEvery { commuteDayChecker.isCommuteDay(any()) } returns true
        stateFlow.value = TrackingState.Idle

        // WHEN: Beacon is detected
        tracker.onBeaconDetected("test-uuid", now)

        // THEN: BeaconDetected event is processed (user works from home today)
        coVerify(exactly = 1) {
            stateMachine.processEvent(
                match { event ->
                    event is TrackingEvent.BeaconDetected
                }
            )
        }
    }

    @Test
    fun `onBeaconDetected ignores beacon on commute day when commute tracking active`() = runTest {
        // GIVEN: It's a commute day and commute tracking is active
        val now = LocalDateTime.of(2026, 2, 13, 9, 0)
        val workWindow = TimeWindow(LocalTime.of(6, 0), LocalTime.of(22, 0))

        coEvery { settingsProvider.workTimeWindow } returns flowOf(workWindow)
        coEvery { commuteDayChecker.isCommuteDay(any()) } returns true
        stateFlow.value = TrackingState.Tracking(
            entryId = "123",
            type = TrackingType.COMMUTE_OFFICE,
            startTime = now.minusHours(1)
        )

        // WHEN: Beacon is detected (user returned home from office)
        tracker.onBeaconDetected("test-uuid", now)

        // THEN: No event is processed (user is not working from home)
        coVerify(exactly = 0) {
            stateMachine.processEvent(any())
        }
    }

    @Test
    fun `onBeaconTimeout stops tracking only if HOME_OFFICE tracking active`() = runTest {
        // GIVEN: HOME_OFFICE tracking is active
        val now = LocalDateTime.of(2026, 2, 13, 17, 30)
        stateFlow.value = TrackingState.Tracking(
            entryId = "123",
            type = TrackingType.HOME_OFFICE,
            startTime = now.minusHours(8)
        )

        // WHEN: Beacon timeout occurs
        tracker.onBeaconTimeout(now, lastSeenTimestamp = now.minusMinutes(10))

        // THEN: BeaconLost event is processed
        coVerify(exactly = 1) {
            stateMachine.processEvent(
                match { event ->
                    event is TrackingEvent.BeaconLost &&
                    event.timestamp == now &&
                    event.lastSeenTimestamp == now.minusMinutes(10)
                }
            )
        }
    }

    @Test
    fun `onBeaconTimeout ignores timeout when COMMUTE tracking active`() = runTest {
        // GIVEN: COMMUTE tracking is active
        val now = LocalDateTime.of(2026, 2, 13, 17, 30)
        stateFlow.value = TrackingState.Tracking(
            entryId = "123",
            type = TrackingType.COMMUTE_OFFICE,
            startTime = now.minusHours(8)
        )

        // WHEN: Beacon timeout occurs
        tracker.onBeaconTimeout(now, lastSeenTimestamp = now.minusMinutes(10))

        // THEN: No event is processed
        coVerify(exactly = 0) {
            stateMachine.processEvent(any())
        }
    }

    @Test
    fun `onBeaconTimeout ignores timeout when idle`() = runTest {
        // GIVEN: No tracking active
        val now = LocalDateTime.of(2026, 2, 13, 17, 30)
        stateFlow.value = TrackingState.Idle

        // WHEN: Beacon timeout occurs
        tracker.onBeaconTimeout(now, lastSeenTimestamp = now.minusMinutes(10))

        // THEN: No event is processed
        coVerify(exactly = 0) {
            stateMachine.processEvent(any())
        }
    }
}
