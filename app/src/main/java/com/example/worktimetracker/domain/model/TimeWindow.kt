package com.example.worktimetracker.domain.model

import java.time.LocalTime

/**
 * Represents a time window with start and end times.
 *
 * @property start The start time of the window
 * @property end The end time of the window
 * @throws IllegalArgumentException if start is after end
 */
data class TimeWindow(
    val start: LocalTime,
    val end: LocalTime
) {
    init {
        require(start.isBefore(end)) {
            "Start time must be before end time"
        }
    }

    /**
     * Formats the time window as "HH:mm–HH:mm"
     */
    fun format(): String = "${start}–${end}"

    companion object {
        /**
         * Default work time window (06:00–22:00)
         */
        val DEFAULT_WORK_TIME = TimeWindow(
            start = LocalTime.of(6, 0),
            end = LocalTime.of(22, 0)
        )

        /**
         * Default outbound commute window (06:00–09:30)
         */
        val DEFAULT_OUTBOUND = TimeWindow(
            start = LocalTime.of(6, 0),
            end = LocalTime.of(9, 30)
        )

        /**
         * Default return commute window (16:00–20:00)
         */
        val DEFAULT_RETURN = TimeWindow(
            start = LocalTime.of(16, 0),
            end = LocalTime.of(20, 0)
        )
    }
}
