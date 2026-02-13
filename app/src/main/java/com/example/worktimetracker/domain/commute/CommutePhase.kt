package com.example.worktimetracker.domain.commute

/**
 * Phases of a commute day.
 *
 * A typical commute day progresses:
 * OUTBOUND -> IN_OFFICE -> RETURN -> COMPLETED
 */
enum class CommutePhase {
    /** On the way to the office (from home station geofence). */
    OUTBOUND,

    /** At the office (entered office geofence). */
    IN_OFFICE,

    /** On the way back (exited office geofence during return window). */
    RETURN,

    /** Arrived back at home station. Commute day is complete. */
    COMPLETED
}
