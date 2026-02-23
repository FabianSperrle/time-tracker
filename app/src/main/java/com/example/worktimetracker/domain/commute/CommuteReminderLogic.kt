package com.example.worktimetracker.domain.commute

import java.time.LocalTime

/**
 * Pure logic for commute reminder decisions.
 * Separated from Worker for easy unit testing.
 */
object CommuteReminderLogic {

    /** Default time to remind if no tracking started on a commute day. */
    val DEFAULT_NO_TRACKING_REMINDER_TIME: LocalTime = LocalTime.of(10, 0)

    /** Default time to warn about still-active tracking. */
    val DEFAULT_LATE_TRACKING_CUTOFF: LocalTime = LocalTime.of(21, 0)

    /**
     * Determines whether to show a "no tracking started" reminder.
     *
     * Shows reminder when:
     * - It is a commute day
     * - Current time is past the reminder time
     * - No tracking session has started today (active or completed)
     */
    fun shouldShowNoTrackingReminder(
        currentTime: LocalTime,
        reminderTime: LocalTime = DEFAULT_NO_TRACKING_REMINDER_TIME,
        isCommuteDay: Boolean,
        hasTrackingToday: Boolean
    ): Boolean {
        return isCommuteDay && !currentTime.isBefore(reminderTime) && !hasTrackingToday
    }

    /**
     * Determines whether to show a "forgot to stop tracking" reminder.
     *
     * Shows reminder when:
     * - Current time is past the cutoff time
     * - Tracking is still active
     */
    fun shouldShowLateTrackingReminder(
        currentTime: LocalTime,
        cutoffTime: LocalTime = DEFAULT_LATE_TRACKING_CUTOFF,
        hasTrackingToday: Boolean
    ): Boolean {
        return hasTrackingToday && !currentTime.isBefore(cutoffTime)
    }
}
