package com.example.worktimetracker.ui.viewmodel

import app.cash.turbine.test
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var stateMachine: TrackingStateMachine
    private lateinit var viewModel: DashboardViewModel
    private lateinit var stateFlow: MutableStateFlow<TrackingState>

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        stateFlow = MutableStateFlow<TrackingState>(TrackingState.Idle)
        stateMachine = mockk(relaxed = true)
        every { stateMachine.state } returns stateFlow

        viewModel = DashboardViewModel(stateMachine)
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
}
