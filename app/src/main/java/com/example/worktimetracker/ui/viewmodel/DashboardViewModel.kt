package com.example.worktimetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.domain.tracking.TrackingEvent
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * UI state for the Dashboard screen.
 */
sealed class DashboardUiState {
    object Idle : DashboardUiState()

    data class Tracking(
        val entryId: String,
        val type: TrackingType,
        val startTime: LocalDateTime
    ) : DashboardUiState()

    data class Paused(
        val entryId: String,
        val type: TrackingType,
        val pauseId: String
    ) : DashboardUiState()
}

/**
 * ViewModel for the Dashboard screen.
 * Handles manual tracking start/stop/pause/resume.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val stateMachine: TrackingStateMachine
) : ViewModel() {

    /**
     * UI state derived from the state machine.
     */
    val uiState: StateFlow<DashboardUiState> = stateMachine.state.map { state ->
        when (state) {
            is TrackingState.Idle -> DashboardUiState.Idle
            is TrackingState.Tracking -> DashboardUiState.Tracking(
                entryId = state.entryId,
                type = state.type,
                startTime = state.startTime
            )
            is TrackingState.Paused -> DashboardUiState.Paused(
                entryId = state.entryId,
                type = state.type,
                pauseId = state.pauseId
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DashboardUiState.Idle
    )

    /**
     * Starts manual tracking with the given type.
     */
    fun startManualTracking(type: TrackingType) {
        viewModelScope.launch {
            stateMachine.processEvent(TrackingEvent.ManualStart(type))
        }
    }

    /**
     * Stops the current tracking session.
     */
    fun stopTracking() {
        viewModelScope.launch {
            stateMachine.processEvent(TrackingEvent.ManualStop)
        }
    }

    /**
     * Pauses the current tracking session.
     */
    fun pauseTracking() {
        viewModelScope.launch {
            stateMachine.processEvent(TrackingEvent.PauseStart)
        }
    }

    /**
     * Resumes a paused tracking session.
     */
    fun resumeTracking() {
        viewModelScope.launch {
            stateMachine.processEvent(TrackingEvent.PauseEnd)
        }
    }
}
