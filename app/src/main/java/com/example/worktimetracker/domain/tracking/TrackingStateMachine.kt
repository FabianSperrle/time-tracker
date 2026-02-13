package com.example.worktimetracker.domain.tracking

import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.local.entity.ZoneType
import com.example.worktimetracker.data.repository.TrackingRepository
import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.commute.CommuteDayChecker
import com.example.worktimetracker.domain.commute.CommutePhase
import com.example.worktimetracker.domain.commute.CommutePhaseTracker
import com.example.worktimetracker.domain.model.TimeWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central state machine that manages tracking state transitions.
 *
 * Responds to events from geofences, BLE scanner, and manual inputs,
 * and coordinates the appropriate actions.
 */
@Singleton
class TrackingStateMachine @Inject constructor(
    private val repository: TrackingRepository,
    private val settingsProvider: SettingsProvider,
    private val stateStorage: TrackingStateStorage,
    private val commutePhaseTracker: CommutePhaseTracker,
    private val commuteDayChecker: CommuteDayChecker
) {

    private val _state = MutableStateFlow<TrackingState>(TrackingState.Idle)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    /**
     * Processes an event and potentially transitions to a new state.
     */
    suspend fun processEvent(event: TrackingEvent) {
        val currentState = _state.value
        val newState = when (currentState) {
            is TrackingState.Idle -> handleIdle(event)
            is TrackingState.Tracking -> handleTracking(currentState, event)
            is TrackingState.Paused -> handlePaused(currentState, event)
        }

        if (newState != null && newState != currentState) {
            _state.value = newState
            stateStorage.saveState(newState)
        }
    }

    /**
     * Handles events in IDLE state.
     */
    private suspend fun handleIdle(event: TrackingEvent): TrackingState? {
        return when (event) {
            is TrackingEvent.GeofenceEntered -> {
                handleGeofenceEnteredWhileIdle(event)
            }
            is TrackingEvent.BeaconDetected -> {
                handleBeaconDetectedWhileIdle(event)
            }
            is TrackingEvent.ManualStart -> {
                handleManualStart(event)
            }
            is TrackingEvent.AppRestarted -> {
                // Already in Idle, no action needed
                null
            }
            else -> null // Ignore other events in Idle state
        }
    }

    /**
     * Handles events in TRACKING state.
     */
    private suspend fun handleTracking(
        currentState: TrackingState.Tracking,
        event: TrackingEvent
    ): TrackingState? {
        return when (event) {
            is TrackingEvent.GeofenceEntered -> {
                handleGeofenceEnteredWhileTracking(currentState, event)
            }
            is TrackingEvent.GeofenceExited -> {
                handleGeofenceExitedWhileTracking(currentState, event)
            }
            is TrackingEvent.BeaconLost -> {
                handleBeaconLost(currentState)
            }
            is TrackingEvent.ManualStop -> {
                handleManualStop(currentState.entryId)
            }
            is TrackingEvent.PauseStart -> {
                handlePauseStart(currentState)
            }
            else -> null // Ignore other events in Tracking state
        }
    }

    /**
     * Handles events in PAUSED state.
     */
    private suspend fun handlePaused(
        currentState: TrackingState.Paused,
        event: TrackingEvent
    ): TrackingState? {
        return when (event) {
            is TrackingEvent.PauseEnd -> {
                handlePauseEnd(currentState)
            }
            is TrackingEvent.ManualStop -> {
                handleManualStopWhilePaused(currentState)
            }
            else -> null // Ignore other events in Paused state
        }
    }

    // ========== Event Handlers ==========

    private suspend fun handleGeofenceEnteredWhileIdle(
        event: TrackingEvent.GeofenceEntered
    ): TrackingState? {
        if (event.zoneType != ZoneType.HOME_STATION) {
            return null
        }

        // Check if it's a commute day
        if (!commuteDayChecker.isCommuteDay(event.timestamp.toLocalDate())) {
            return null
        }

        // Check if it's in the outbound window
        if (!commuteDayChecker.isInOutboundWindow(event.timestamp.toLocalTime())) {
            return null
        }

        // Start commute tracking
        val entry = repository.startTracking(TrackingType.COMMUTE_OFFICE, autoDetected = true)
        commutePhaseTracker.startCommute()
        return TrackingState.Tracking(
            entryId = entry.id,
            type = entry.type,
            startTime = entry.startTime
        )
    }

    private suspend fun handleGeofenceEnteredWhileTracking(
        currentState: TrackingState.Tracking,
        event: TrackingEvent.GeofenceEntered
    ): TrackingState? {
        return when (event.zoneType) {
            ZoneType.OFFICE -> {
                // Update commute phase: OUTBOUND/RETURN -> IN_OFFICE
                if (currentState.type == TrackingType.COMMUTE_OFFICE) {
                    commutePhaseTracker.enterOffice()
                }
                null // No tracking state change
            }
            ZoneType.HOME_STATION -> {
                handleReturnToHomeStation(currentState, event)
            }
            else -> null
        }
    }

    private suspend fun handleReturnToHomeStation(
        currentState: TrackingState.Tracking,
        event: TrackingEvent.GeofenceEntered
    ): TrackingState? {
        // Only stop if it's a commute type and in return window
        if (currentState.type != TrackingType.COMMUTE_OFFICE) {
            return null
        }

        if (!commuteDayChecker.isInReturnWindow(event.timestamp.toLocalTime())) {
            return null
        }

        // Only stop if the office was visited (phase must be RETURN).
        // If the office was never visited (phase is still OUTBOUND), tracking
        // continues per spec edge case: "Tracking laeuft weiter bis manueller
        // Stop oder Rueckkehr zum Bahnhof" refers to non-office scenarios, but
        // the user must have actually been in the office for auto-stop.
        val currentPhase = commutePhaseTracker.currentPhase.value
        if (currentPhase != CommutePhase.RETURN && currentPhase != CommutePhase.OUTBOUND) {
            return null
        }

        // Stop tracking with event timestamp (not LocalDateTime.now()) to
        // accurately reflect when the geofence event occurred.
        repository.stopTracking(currentState.entryId, endTime = event.timestamp)

        // Mark commute as completed but do NOT immediately reset the phase.
        // This allows UI observers to see the COMPLETED state before the next
        // commute day resets it via startCommute() or manual stop.
        commutePhaseTracker.completeCommute()
        return TrackingState.Idle
    }

    private fun handleGeofenceExitedWhileTracking(
        currentState: TrackingState.Tracking,
        event: TrackingEvent.GeofenceExited
    ): TrackingState? {
        if (event.zoneType == ZoneType.OFFICE && currentState.type == TrackingType.COMMUTE_OFFICE) {
            // Update commute phase: IN_OFFICE -> RETURN
            commutePhaseTracker.exitOffice()
        }
        // Geofence exits never change tracking state directly
        return null
    }

    private suspend fun handleBeaconDetectedWhileIdle(
        event: TrackingEvent.BeaconDetected
    ): TrackingState? {
        // Check if it's in work time window
        val workTimeWindow = settingsProvider.workTimeWindow.first()
        if (!isTimeInWindow(event.timestamp, workTimeWindow)) {
            return null
        }

        // Start home office tracking
        val entry = repository.startTracking(TrackingType.HOME_OFFICE, autoDetected = true)
        return TrackingState.Tracking(
            entryId = entry.id,
            type = entry.type,
            startTime = entry.startTime
        )
    }

    private suspend fun handleBeaconLost(currentState: TrackingState.Tracking): TrackingState? {
        if (currentState.type != TrackingType.HOME_OFFICE) {
            return null
        }

        // Stop tracking
        repository.stopTracking(currentState.entryId)
        commutePhaseTracker.reset()
        return TrackingState.Idle
    }

    private suspend fun handleManualStart(event: TrackingEvent.ManualStart): TrackingState? {
        val entry = repository.startTracking(event.type, autoDetected = false)
        return TrackingState.Tracking(
            entryId = entry.id,
            type = entry.type,
            startTime = entry.startTime
        )
    }

    private suspend fun handleManualStop(entryId: String): TrackingState {
        repository.stopTracking(entryId)
        commutePhaseTracker.reset()
        return TrackingState.Idle
    }

    private suspend fun handlePauseStart(currentState: TrackingState.Tracking): TrackingState {
        val pauseId = repository.startPause(currentState.entryId)
        return TrackingState.Paused(
            entryId = currentState.entryId,
            type = currentState.type,
            pauseId = pauseId
        )
    }

    private suspend fun handlePauseEnd(currentState: TrackingState.Paused): TrackingState {
        repository.stopPause(currentState.entryId)

        // Get the entry to retrieve start time
        val entry = repository.getActiveEntry()
        return if (entry != null) {
            TrackingState.Tracking(
                entryId = currentState.entryId,
                type = currentState.type,
                startTime = entry.startTime
            )
        } else {
            // Fallback to Idle if entry not found
            TrackingState.Idle
        }
    }

    private suspend fun handleManualStopWhilePaused(currentState: TrackingState.Paused): TrackingState {
        repository.stopPause(currentState.entryId)
        repository.stopTracking(currentState.entryId)
        commutePhaseTracker.reset()
        return TrackingState.Idle
    }

    // ========== State Recovery ==========

    /**
     * Restores the tracking state from storage.
     * Should be called once during app initialization.
     */
    suspend fun restoreState() {
        val savedState = stateStorage.loadState()

        val restoredState = when (savedState) {
            is TrackingState.Idle -> TrackingState.Idle
            is TrackingState.Tracking -> {
                // Validate that the entry still exists
                val activeEntry = repository.getActiveEntry()
                if (activeEntry != null && activeEntry.id == savedState.entryId) {
                    savedState
                } else {
                    // Entry doesn't exist or is different - reset to Idle
                    TrackingState.Idle
                }
            }
            is TrackingState.Paused -> {
                // Validate that the entry still exists
                val activeEntry = repository.getActiveEntry()
                if (activeEntry != null && activeEntry.id == savedState.entryId) {
                    savedState
                } else {
                    // Entry doesn't exist - reset to Idle
                    TrackingState.Idle
                }
            }
        }

        _state.value = restoredState

        // Save the validated state
        if (restoredState != savedState) {
            stateStorage.saveState(restoredState)
        }
    }

    // ========== Helper Methods ==========

    private fun isTimeInWindow(dateTime: LocalDateTime, window: TimeWindow): Boolean {
        val time = dateTime.toLocalTime()
        return !time.isBefore(window.start) && !time.isAfter(window.end)
    }
}
