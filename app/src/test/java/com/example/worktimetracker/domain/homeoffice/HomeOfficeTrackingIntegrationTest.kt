package com.example.worktimetracker.domain.homeoffice

import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.repository.TrackingRepository
import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.commute.CommuteDayChecker
import com.example.worktimetracker.domain.commute.CommutePhaseTracker
import com.example.worktimetracker.domain.model.TimeWindow
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Integration tests for Home Office tracking logic.
 *
 * Tests the full flow from beacon detection through HomeOfficeTracker
 * to TrackingStateMachine to TrackingRepository.
 */
class HomeOfficeTrackingIntegrationTest {

    private lateinit var repository: TrackingRepository
    private lateinit var settingsProvider: SettingsProvider
    private lateinit var stateStorage: TrackingStateStorage
    private lateinit var commutePhaseTracker: CommutePhaseTracker
    private lateinit var commuteDayChecker: CommuteDayChecker
    private lateinit var stateMachine: TrackingStateMachine
    private lateinit var homeOfficeTracker: HomeOfficeTracker

    private val testUuid = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"

    @BeforeEach
    fun setup() {
        repository = mockk(relaxed = true)
        settingsProvider = mockk()
        stateStorage = mockk(relaxed = true)
        commutePhaseTracker = mockk(relaxed = true)
        commuteDayChecker = mockk()

        // Default settings
        every { settingsProvider.workTimeWindow } returns flowOf(
            TimeWindow(LocalTime.of(6, 0), LocalTime.of(22, 0))
        )
        every { settingsProvider.commuteDays } returns flowOf(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
        every { settingsProvider.outboundWindow } returns flowOf(
            TimeWindow(LocalTime.of(6, 0), LocalTime.of(10, 0))
        )
        every { settingsProvider.returnWindow } returns flowOf(
            TimeWindow(LocalTime.of(16, 0), LocalTime.of(20, 0))
        )

        stateMachine = TrackingStateMachine(
            repository = repository,
            settingsProvider = settingsProvider,
            stateStorage = stateStorage,
            commutePhaseTracker = commutePhaseTracker,
            commuteDayChecker = commuteDayChecker
        )

        homeOfficeTracker = HomeOfficeTracker(
            stateMachine = stateMachine,
            commuteDayChecker = commuteDayChecker,
            settingsProvider = settingsProvider
        )
    }

    @Test
    fun `beacon detection starts HOME_OFFICE tracking within work window`() = runTest {
        // GIVEN: Thursday (non-commute day), 9:00 AM
        val timestamp = LocalDateTime.of(2026, 2, 12, 9, 0) // Thursday
        coEvery { commuteDayChecker.isCommuteDay(any()) } returns false

        val mockEntry = mockk<com.example.worktimetracker.data.local.entity.TrackingEntry>(relaxed = true)
        every { mockEntry.id } returns "entry-123"
        every { mockEntry.type } returns TrackingType.HOME_OFFICE
        every { mockEntry.startTime } returns timestamp
        coEvery { repository.startTracking(TrackingType.HOME_OFFICE, true) } returns mockEntry

        // WHEN: Beacon detected
        homeOfficeTracker.onBeaconDetected(testUuid, timestamp)

        // THEN: HOME_OFFICE tracking started
        coVerify(exactly = 1) {
            repository.startTracking(TrackingType.HOME_OFFICE, autoDetected = true)
        }
        val state = stateMachine.state.first()
        assertTrue(state is TrackingState.Tracking)
        assertEquals(TrackingType.HOME_OFFICE, (state as TrackingState.Tracking).type)
    }

    @Test
    fun `beacon timeout stops HOME_OFFICE tracking`() = runTest {
        // GIVEN: HOME_OFFICE tracking active
        val startTime = LocalDateTime.of(2026, 2, 12, 9, 0)
        val timeoutTime = LocalDateTime.of(2026, 2, 12, 17, 30)
        val lastSeen = LocalDateTime.of(2026, 2, 12, 17, 20)

        coEvery { commuteDayChecker.isCommuteDay(any()) } returns false

        val mockEntry = mockk<com.example.worktimetracker.data.local.entity.TrackingEntry>(relaxed = true)
        every { mockEntry.id } returns "entry-123"
        every { mockEntry.type } returns TrackingType.HOME_OFFICE
        every { mockEntry.startTime } returns startTime
        coEvery { repository.startTracking(TrackingType.HOME_OFFICE, true) } returns mockEntry

        // Start tracking
        homeOfficeTracker.onBeaconDetected(testUuid, startTime)

        // WHEN: Beacon timeout
        homeOfficeTracker.onBeaconTimeout(timeoutTime, lastSeen)

        // THEN: Tracking stopped with lastSeen as end time
        coVerify(exactly = 1) {
            repository.stopTracking("entry-123", lastSeen)
        }
        val state = stateMachine.state.first()
        assertTrue(state is TrackingState.Idle)
    }

    @Test
    fun `multiple sessions per day create separate entries`() = runTest {
        // GIVEN: Non-commute day
        val session1Start = LocalDateTime.of(2026, 2, 12, 9, 0)
        val session1End = LocalDateTime.of(2026, 2, 12, 11, 30)
        val session2Start = LocalDateTime.of(2026, 2, 12, 13, 0)

        coEvery { commuteDayChecker.isCommuteDay(any()) } returns false

        // Mock entries
        val mockEntry1 = mockk<com.example.worktimetracker.data.local.entity.TrackingEntry>(relaxed = true)
        every { mockEntry1.id } returns "entry-1"
        every { mockEntry1.type } returns TrackingType.HOME_OFFICE
        every { mockEntry1.startTime } returns session1Start

        val mockEntry2 = mockk<com.example.worktimetracker.data.local.entity.TrackingEntry>(relaxed = true)
        every { mockEntry2.id } returns "entry-2"
        every { mockEntry2.type } returns TrackingType.HOME_OFFICE
        every { mockEntry2.startTime } returns session2Start
        coEvery { repository.startTracking(TrackingType.HOME_OFFICE, true) } returnsMany listOf(
            mockEntry1,
            mockEntry2
        )

        // WHEN: First session
        homeOfficeTracker.onBeaconDetected(testUuid, session1Start)
        homeOfficeTracker.onBeaconTimeout(session1End, session1End.minusMinutes(10))

        // WHEN: Second session
        homeOfficeTracker.onBeaconDetected(testUuid, session2Start)

        // THEN: Two separate entries created
        coVerify(exactly = 2) {
            repository.startTracking(TrackingType.HOME_OFFICE, autoDetected = true)
        }
        coVerify(exactly = 1) {
            repository.stopTracking("entry-1", any())
        }
    }

    @Test
    fun `beacon ignored on commute day with active COMMUTE tracking`() = runTest {
        // GIVEN: Monday (commute day), COMMUTE tracking active
        val timestamp = LocalDateTime.of(2026, 2, 10, 18, 0) // Monday
        coEvery { commuteDayChecker.isCommuteDay(LocalDate.of(2026, 2, 10)) } returns true

        val mockCommuteEntry = mockk<com.example.worktimetracker.data.local.entity.TrackingEntry>(relaxed = true)
        every { mockCommuteEntry.id } returns "commute-entry"
        every { mockCommuteEntry.type } returns TrackingType.COMMUTE_OFFICE
        every { mockCommuteEntry.startTime } returns timestamp.minusHours(8)
        coEvery { repository.startTracking(TrackingType.COMMUTE_OFFICE, false) } returns mockCommuteEntry

        // Start commute tracking manually
        stateMachine.processEvent(
            com.example.worktimetracker.domain.tracking.TrackingEvent.ManualStart(
                type = TrackingType.COMMUTE_OFFICE,
                timestamp = timestamp.minusHours(8)
            )
        )

        // WHEN: Beacon detected at home (user returned from office)
        homeOfficeTracker.onBeaconDetected(testUuid, timestamp)

        // THEN: No new tracking started (beacon ignored)
        coVerify(exactly = 0) {
            repository.startTracking(TrackingType.HOME_OFFICE, any())
        }
        val state = stateMachine.state.first()
        assertTrue(state is TrackingState.Tracking)
        assertEquals(TrackingType.COMMUTE_OFFICE, (state as TrackingState.Tracking).type)
    }

    @Test
    fun `beacon starts HOME_OFFICE on commute day when no tracking active`() = runTest {
        // GIVEN: Monday (commute day), but no tracking active (user stays home)
        val timestamp = LocalDateTime.of(2026, 2, 10, 9, 0) // Monday
        coEvery { commuteDayChecker.isCommuteDay(LocalDate.of(2026, 2, 10)) } returns true

        val mockEntry = mockk<com.example.worktimetracker.data.local.entity.TrackingEntry> {
            every { id } returns "home-entry"
            every { type } returns TrackingType.HOME_OFFICE
            every { startTime } returns timestamp
        }
        coEvery { repository.startTracking(TrackingType.HOME_OFFICE, true) } returns mockEntry

        // WHEN: Beacon detected
        homeOfficeTracker.onBeaconDetected(testUuid, timestamp)

        // THEN: HOME_OFFICE tracking started (user works from home today)
        coVerify(exactly = 1) {
            repository.startTracking(TrackingType.HOME_OFFICE, autoDetected = true)
        }
        val state = stateMachine.state.first()
        assertTrue(state is TrackingState.Tracking)
        assertEquals(TrackingType.HOME_OFFICE, (state as TrackingState.Tracking).type)
    }

    @Test
    fun `beacon ignored outside work time window`() = runTest {
        // GIVEN: 23:00 (outside work window 06:00-22:00)
        val timestamp = LocalDateTime.of(2026, 2, 12, 23, 0)
        coEvery { commuteDayChecker.isCommuteDay(any()) } returns false

        // WHEN: Beacon detected
        homeOfficeTracker.onBeaconDetected(testUuid, timestamp)

        // THEN: No tracking started
        coVerify(exactly = 0) {
            repository.startTracking(any(), any())
        }
    }
}
