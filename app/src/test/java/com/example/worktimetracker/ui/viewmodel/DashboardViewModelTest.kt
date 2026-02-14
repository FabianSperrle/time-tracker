package com.example.worktimetracker.ui.viewmodel

import app.cash.turbine.test
import com.example.worktimetracker.data.local.entity.Pause
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.repository.TrackingRepository
import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var stateMachine: TrackingStateMachine
    private lateinit var repository: TrackingRepository
    private lateinit var settingsProvider: SettingsProvider
    private lateinit var viewModel: DashboardViewModel
    private lateinit var stateFlow: MutableStateFlow<TrackingState>

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        stateFlow = MutableStateFlow<TrackingState>(TrackingState.Idle)
        stateMachine = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        settingsProvider = mockk(relaxed = true)

        every { stateMachine.state } returns stateFlow
        every { repository.getTodayEntries() } returns flowOf(emptyList())
        every { settingsProvider.weeklyTargetHours } returns flowOf(40f)

        viewModel = DashboardViewModel(stateMachine, repository, settingsProvider)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState should reflect Idle state initially`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(true, state is DashboardUiState.Idle)
        }
    }

    @Test
    fun `uiState should reflect Tracking state when state machine is tracking`() = runTest {
        viewModel.uiState.test {
            // Initial Idle
            assertEquals(DashboardUiState.Idle, awaitItem())

            // Emit Tracking state
            val trackingState = TrackingState.Tracking(
                entryId = "test-123",
                type = TrackingType.MANUAL,
                startTime = LocalDateTime.now()
            )
            stateFlow.value = trackingState
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(true, state is DashboardUiState.Tracking)
            assertEquals(trackingState.entryId, (state as DashboardUiState.Tracking).entryId)
            assertEquals(trackingState.type, state.type)
        }
    }

    @Test
    fun `uiState should reflect Paused state when state machine is paused`() = runTest {
        viewModel.uiState.test {
            // Initial Idle
            assertEquals(DashboardUiState.Idle, awaitItem())

            // Emit Paused state
            val pausedState = TrackingState.Paused(
                entryId = "test-123",
                type = TrackingType.MANUAL,
                pauseId = "pause-456"
            )
            stateFlow.value = pausedState
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(true, state is DashboardUiState.Paused)
            assertEquals(pausedState.entryId, (state as DashboardUiState.Paused).entryId)
            assertEquals(pausedState.type, state.type)
        }
    }

    @Test
    fun `startManualTracking should trigger ManualStart event with selected type`() = runTest {
        coEvery { stateMachine.processEvent(any()) } returns Unit

        viewModel.startManualTracking(TrackingType.HOME_OFFICE)
        advanceUntilIdle()

        coVerify {
            stateMachine.processEvent(
                withArg { event ->
                    assertEquals(true, event is com.example.worktimetracker.domain.tracking.TrackingEvent.ManualStart)
                    assertEquals(
                        TrackingType.HOME_OFFICE,
                        (event as com.example.worktimetracker.domain.tracking.TrackingEvent.ManualStart).type
                    )
                }
            )
        }
    }

    @Test
    fun `stopTracking should trigger ManualStop event`() = runTest {
        coEvery { stateMachine.processEvent(any()) } returns Unit

        viewModel.stopTracking()
        advanceUntilIdle()

        coVerify {
            stateMachine.processEvent(
                withArg { event ->
                    assertEquals(true, event is com.example.worktimetracker.domain.tracking.TrackingEvent.ManualStop)
                }
            )
        }
    }

    @Test
    fun `pauseTracking should trigger PauseStart event`() = runTest {
        coEvery { stateMachine.processEvent(any()) } returns Unit

        viewModel.pauseTracking()
        advanceUntilIdle()

        coVerify {
            stateMachine.processEvent(
                withArg { event ->
                    assertEquals(true, event is com.example.worktimetracker.domain.tracking.TrackingEvent.PauseStart)
                }
            )
        }
    }

    @Test
    fun `resumeTracking should trigger PauseEnd event`() = runTest {
        coEvery { stateMachine.processEvent(any()) } returns Unit

        viewModel.resumeTracking()
        advanceUntilIdle()

        coVerify {
            stateMachine.processEvent(
                withArg { event ->
                    assertEquals(true, event is com.example.worktimetracker.domain.tracking.TrackingEvent.PauseEnd)
                }
            )
        }
    }

    @Test
    fun `todayStats should calculate correct stats for single entry`() = runTest {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.now().minusHours(4),
            endTime = LocalDateTime.now(),
            autoDetected = false
        )
        every { repository.getTodayEntries() } returns flowOf(
            listOf(TrackingEntryWithPauses(entry, emptyList()))
        )
        every { settingsProvider.weeklyTargetHours } returns flowOf(40f)

        viewModel = DashboardViewModel(stateMachine, repository, settingsProvider)
        advanceUntilIdle()

        viewModel.todayStats.test {
            val stats = awaitItem()
            // 4 hours work, 8 hours target (40h / 5 days)
            assertEquals(Duration.ofHours(4).toMinutes(), stats.grossWorkTime.toMinutes())
            assertEquals(Duration.ofHours(4).toMinutes(), stats.netWorkTime.toMinutes())
            assertEquals(Duration.ofHours(8).toMinutes(), stats.targetWorkTime.toMinutes())
        }
    }

    @Test
    fun `todayStats should calculate correct stats with pauses`() = runTest {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.now().minusHours(5),
            endTime = LocalDateTime.now(),
            autoDetected = false
        )
        val pause = Pause(
            entryId = "1",
            startTime = LocalDateTime.now().minusHours(3),
            endTime = LocalDateTime.now().minusHours(2)
        )
        every { repository.getTodayEntries() } returns flowOf(
            listOf(TrackingEntryWithPauses(entry, listOf(pause)))
        )
        every { settingsProvider.weeklyTargetHours } returns flowOf(40f)

        viewModel = DashboardViewModel(stateMachine, repository, settingsProvider)
        advanceUntilIdle()

        viewModel.todayStats.test {
            val stats = awaitItem()
            // 5 hours gross, 1 hour pause, 4 hours net
            assertEquals(Duration.ofHours(5).toMinutes(), stats.grossWorkTime.toMinutes())
            assertEquals(Duration.ofHours(1).toMinutes(), stats.pauseTime.toMinutes())
            assertEquals(Duration.ofHours(4).toMinutes(), stats.netWorkTime.toMinutes())
        }
    }

    @Test
    fun `todayStats should handle multiple entries correctly`() = runTest {
        val entry1 = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.now().minusHours(8),
            endTime = LocalDateTime.now().minusHours(4),
            autoDetected = false
        )
        val entry2 = TrackingEntry(
            id = "2",
            date = LocalDate.now(),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.now().minusHours(3),
            endTime = LocalDateTime.now(),
            autoDetected = false
        )
        every { repository.getTodayEntries() } returns flowOf(
            listOf(
                TrackingEntryWithPauses(entry1, emptyList()),
                TrackingEntryWithPauses(entry2, emptyList())
            )
        )
        every { settingsProvider.weeklyTargetHours } returns flowOf(40f)

        viewModel = DashboardViewModel(stateMachine, repository, settingsProvider)
        advanceUntilIdle()

        viewModel.todayStats.test {
            val stats = awaitItem()
            // 4 + 3 = 7 hours
            assertEquals(Duration.ofHours(7).toMinutes(), stats.grossWorkTime.toMinutes())
            assertEquals(Duration.ofHours(7).toMinutes(), stats.netWorkTime.toMinutes())
        }
    }

    @Test
    fun `todayStats should return EMPTY for no entries`() = runTest {
        every { repository.getTodayEntries() } returns flowOf(emptyList())
        every { settingsProvider.weeklyTargetHours } returns flowOf(40f)

        viewModel = DashboardViewModel(stateMachine, repository, settingsProvider)
        advanceUntilIdle()

        viewModel.todayStats.test {
            val stats = awaitItem()
            assertEquals(Duration.ZERO, stats.grossWorkTime)
            assertEquals(Duration.ZERO, stats.pauseTime)
            assertEquals(Duration.ZERO, stats.netWorkTime)
            assertEquals(Duration.ofHours(8).toMinutes(), stats.targetWorkTime.toMinutes())
        }
    }
}
