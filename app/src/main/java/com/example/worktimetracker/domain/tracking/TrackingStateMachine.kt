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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
                handleBeaconLost(currentState, event)
            }
            is TrackingEvent.ManualStop -> {
                handleManualStop(currentState.entryId)
            }
            is TrackingEvent.PauseStart -> {
                handlePauseStart(currentState)
            }
            is TrackingEvent.MidnightRollover -> {
                handleMidnightRolloverWhileTracking(currentState)
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
            is TrackingEvent.GeofenceEntered -> handleGeofenceEnteredWhilePaused(currentState, event)
            is TrackingEvent.GeofenceExited -> handleGeofenceExitedWhilePaused(currentState, event)
            is TrackingEvent.PauseEnd -> {
                handlePauseEnd(currentState)
            }
            is TrackingEvent.ManualStop -> {
                handleManualStopWhilePaused(currentState)
            }
            is TrackingEvent.MidnightRollover -> {
                handleMidnightRolloverWhilePaused(currentState)
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
            ZoneType.OFFICE_STATION -> handleOfficeStationEnteredWhileTracking(currentState, event)
            ZoneType.HOME_STATION -> {
                handleReturnToHomeStation(currentState, event)
            }
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

    private suspend fun handleGeofenceExitedWhileTracking(
        currentState: TrackingState.Tracking,
        event: TrackingEvent.GeofenceExited
    ): TrackingState? {
        return if (event.zoneType == ZoneType.OFFICE && currentState.type == TrackingType.COMMUTE_OFFICE) {
            // Update commute phase: IN_OFFICE -> RETURN (always)
            commutePhaseTracker.exitOffice()
            if (commuteDayChecker.isInReturnWindow(event.timestamp.toLocalTime())) {
                // Evening: auto-pause for the walk from office to station
                handlePauseStart(currentState)
            } else {
                // Lunch break or other exit: phase updated but no pause
                null
            }
        } else {
            null
        }
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

    private suspend fun handleBeaconLost(
        currentState: TrackingState.Tracking,
        event: TrackingEvent.BeaconLost
    ): TrackingState? {
        if (currentState.type != TrackingType.HOME_OFFICE) {
            return null
        }

        // Use lastSeenTimestamp for end time correction (AC #5):
        // When beacon is lost due to timeout, the actual end of work is when
        // the beacon was last seen, not when the timeout expired.
        val endTime = event.lastSeenTimestamp ?: event.timestamp
        repository.stopTracking(currentState.entryId, endTime = endTime)
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

    private suspend fun handleOfficeStationEnteredWhileTracking(
        currentState: TrackingState.Tracking,
        @Suppress("UNUSED_PARAMETER") event: TrackingEvent.GeofenceEntered
    ): TrackingState? {
        if (currentState.type != TrackingType.COMMUTE_OFFICE) return null
        if (commutePhaseTracker.currentPhase.value != CommutePhase.OUTBOUND) return null
        return handlePauseStart(currentState)
    }

    private suspend fun handleGeofenceEnteredWhilePaused(
        currentState: TrackingState.Paused,
        event: TrackingEvent.GeofenceEntered
    ): TrackingState? {
        return when (event.zoneType) {
            ZoneType.OFFICE -> {
                // Morning: resume when arriving at office
                if (currentState.type == TrackingType.COMMUTE_OFFICE &&
                    commutePhaseTracker.currentPhase.value == CommutePhase.OUTBOUND) {
                    commutePhaseTracker.enterOffice()
                    handlePauseEnd(currentState)
                } else null
            }
            ZoneType.HOME_STATION -> handleReturnToHomeStationWhilePaused(currentState, event)
            else -> null
        }
    }

    private suspend fun handleGeofenceExitedWhilePaused(
        currentState: TrackingState.Paused,
        event: TrackingEvent.GeofenceExited
    ): TrackingState? {
        // Evening: EXIT OFFICE_STATION resumes tracking (on the train home)
        if (event.zoneType != ZoneType.OFFICE_STATION) return null
        if (currentState.type != TrackingType.COMMUTE_OFFICE) return null
        if (commutePhaseTracker.currentPhase.value != CommutePhase.RETURN) return null
        return handlePauseEnd(currentState)
    }

    private suspend fun handleReturnToHomeStationWhilePaused(
        currentState: TrackingState.Paused,
        event: TrackingEvent.GeofenceEntered
    ): TrackingState? {
        // Stop tracking from PAUSED state (EXIT OFFICE_STATION may never have fired)
        if (currentState.type != TrackingType.COMMUTE_OFFICE) return null
        if (!commuteDayChecker.isInReturnWindow(event.timestamp.toLocalTime())) return null
        val phase = commutePhaseTracker.currentPhase.value
        if (phase != CommutePhase.RETURN && phase != CommutePhase.OUTBOUND) return null

        repository.stopPause(currentState.entryId)
        repository.stopTracking(currentState.entryId, endTime = event.timestamp)
        commutePhaseTracker.completeCommute()
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

    // ========== Midnight Rollover ==========

    private suspend fun handleMidnightRolloverWhileTracking(
        currentState: TrackingState.Tracking
    ): TrackingState {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val endOfYesterday = LocalDateTime.of(yesterday, LocalTime.of(23, 59, 59))
        val startOfToday = LocalDateTime.of(today, LocalTime.MIDNIGHT)

        // Stop old entry at end of yesterday
        repository.stopTracking(currentState.entryId, endTime = endOfYesterday)

        // Start new entry at start of today
        val entry = repository.startTrackingAt(
            type = currentState.type,
            autoDetected = true,
            startTime = startOfToday,
            date = today
        )

        return TrackingState.Tracking(
            entryId = entry.id,
            type = entry.type,
            startTime = entry.startTime
        )
    }

    private suspend fun handleMidnightRolloverWhilePaused(
        currentState: TrackingState.Paused
    ): TrackingState {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val endOfYesterday = LocalDateTime.of(yesterday, LocalTime.of(23, 59, 59))
        val startOfToday = LocalDateTime.of(today, LocalTime.MIDNIGHT)

        // Close active pause and stop old entry
        repository.stopPause(currentState.entryId)
        repository.stopTracking(currentState.entryId, endTime = endOfYesterday)

        // Start new entry at start of today
        val entry = repository.startTrackingAt(
            type = currentState.type,
            autoDetected = true,
            startTime = startOfToday,
            date = today
        )

        // Start a new pause on the new entry
        val pauseId = repository.startPause(entry.id)

        return TrackingState.Paused(
            entryId = entry.id,
            type = entry.type,
            pauseId = pauseId
        )
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

        // Handle midnight rollover(s) for entries from previous days
        performMidnightRolloverIfNeeded()
    }

    private suspend fun performMidnightRolloverIfNeeded() {
        val today = LocalDate.now()
        var currentState = _state.value

        // Loop to handle multiple days (e.g., app not opened for several days)
        while (currentState is TrackingState.Tracking || currentState is TrackingState.Paused) {
            val entryDate = when (currentState) {
                is TrackingState.Tracking -> currentState.startTime.toLocalDate()
                is TrackingState.Paused -> {
                    val entry = repository.getActiveEntry()
                    entry?.startTime?.toLocalDate() ?: break
                }
                else -> break
            }

            if (!entryDate.isBefore(today)) break

            processEvent(TrackingEvent.MidnightRollover)
            currentState = _state.value
        }
    }

    // ========== Helper Methods ==========

    private fun isTimeInWindow(dateTime: LocalDateTime, window: TimeWindow): Boolean {
        val time = dateTime.toLocalTime()
        return !time.isBefore(window.start) && !time.isAfter(window.end)
    }
}
