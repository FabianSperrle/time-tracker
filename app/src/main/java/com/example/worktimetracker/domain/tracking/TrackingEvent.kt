package com.example.worktimetracker.domain.tracking

import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.local.entity.ZoneType
import java.time.LocalDateTime

/**
 * Events that can trigger state transitions in the tracking state machine.
 */
sealed class TrackingEvent {
    // ========== Automatic Triggers ==========

    /**
     * User entered a geofence zone.
     *
     * @property zoneType The type of zone entered
     * @property timestamp When the event occurred
     */
    data class GeofenceEntered(
        val zoneType: ZoneType,
        val timestamp: LocalDateTime = LocalDateTime.now()
    ) : TrackingEvent()

    /**
     * User exited a geofence zone.
     *
     * @property zoneType The type of zone exited
     * @property timestamp When the event occurred
     */
    data class GeofenceExited(
        val zoneType: ZoneType,
        val timestamp: LocalDateTime = LocalDateTime.now()
    ) : TrackingEvent()

    /**
     * BLE beacon was detected.
     *
     * @property uuid The UUID of the detected beacon
     * @property timestamp When the event occurred
     */
    data class BeaconDetected(
        val uuid: String,
        val timestamp: LocalDateTime = LocalDateTime.now()
    ) : TrackingEvent()

    /**
     * BLE beacon was lost (not detected for timeout period).
     *
     * @property timestamp When the event occurred
     * @property lastSeenTimestamp When the beacon was last seen (for end time correction)
     */
    data class BeaconLost(
        val timestamp: LocalDateTime = LocalDateTime.now(),
        val lastSeenTimestamp: LocalDateTime? = null
    ) : TrackingEvent()

    // ========== Manual Triggers ==========

    /**
     * User manually started tracking.
     *
     * @property type The type of tracking to start
     * @property timestamp When the event occurred
     */
    data class ManualStart(
        val type: TrackingType = TrackingType.MANUAL,
        val timestamp: LocalDateTime = LocalDateTime.now()
    ) : TrackingEvent()

    /**
     * User manually stopped tracking.
     */
    object ManualStop : TrackingEvent()

    /**
     * User started a pause.
     */
    object PauseStart : TrackingEvent()

    /**
     * User ended a pause.
     */
    object PauseEnd : TrackingEvent()

    // ========== System Events ==========

    /**
     * App was restarted and needs to restore state.
     */
    object AppRestarted : TrackingEvent()
}
