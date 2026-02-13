package com.example.worktimetracker.domain.commute

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the current phase of a commute day.
 *
 * Phase transitions:
 * - null -> OUTBOUND: Commute starts (home station geofence entered)
 * - OUTBOUND -> IN_OFFICE: Office geofence entered
 * - IN_OFFICE -> RETURN: Office geofence exited
 * - RETURN -> IN_OFFICE: Office geofence re-entered (e.g., forgot something)
 * - RETURN -> COMPLETED: Home station geofence entered on return
 * - OUTBOUND -> COMPLETED: Home station re-entered without visiting office
 * - any -> null: Reset (tracking stopped or new day)
 */
@Singleton
class CommutePhaseTracker @Inject constructor() {

    private val _currentPhase = MutableStateFlow<CommutePhase?>(null)
    val currentPhase: StateFlow<CommutePhase?> = _currentPhase.asStateFlow()

    /**
     * Starts a new commute. Sets phase to OUTBOUND.
     */
    fun startCommute() {
        _currentPhase.value = CommutePhase.OUTBOUND
    }

    /**
     * Marks entry into the office.
     * Valid from OUTBOUND or RETURN phases.
     */
    fun enterOffice() {
        val current = _currentPhase.value
        if (current == CommutePhase.OUTBOUND || current == CommutePhase.RETURN) {
            _currentPhase.value = CommutePhase.IN_OFFICE
        }
    }

    /**
     * Marks exit from the office.
     * Valid from IN_OFFICE phase only.
     */
    fun exitOffice() {
        if (_currentPhase.value == CommutePhase.IN_OFFICE) {
            _currentPhase.value = CommutePhase.RETURN
        }
    }

    /**
     * Marks the commute as completed (arrived home).
     * Valid from OUTBOUND or RETURN phases.
     */
    fun completeCommute() {
        val current = _currentPhase.value
        if (current == CommutePhase.OUTBOUND || current == CommutePhase.RETURN) {
            _currentPhase.value = CommutePhase.COMPLETED
        }
    }

    /**
     * Resets the phase tracker. Called when tracking stops.
     */
    fun reset() {
        _currentPhase.value = null
    }
}
