package com.example.worktimetracker.domain.model

import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import java.time.Duration
import java.time.LocalDate

/**
 * Summary of work for a single day.
 *
 * @property date The date of this summary
 * @property type The tracking type (null if no work)
 * @property netDuration Net work duration (excluding pauses)
 * @property confirmed True if all entries are confirmed
 */
data class DaySummary(
    val date: LocalDate,
    val type: TrackingType?,
    val netDuration: Duration,
    val confirmed: Boolean
) {
    companion object {
        /**
         * Creates a day summary from a list of entries.
         *
         * @param date The date for this summary
         * @param entries List of tracking entries for the day
         * @return Day summary with aggregated data
         */
        fun from(date: LocalDate, entries: List<TrackingEntryWithPauses>): DaySummary {
            if (entries.isEmpty()) {
                return DaySummary(
                    date = date,
                    type = null,
                    netDuration = Duration.ZERO,
                    confirmed = true
                )
            }

            // Use the type of the first entry
            val type = entries.firstOrNull()?.entry?.type

            // Sum net durations
            val totalMinutes = entries.sumOf { it.netDuration().toMinutes() }
            val netDuration = Duration.ofMinutes(totalMinutes)

            // All entries must be confirmed
            val confirmed = entries.all { it.entry.confirmed }

            return DaySummary(
                date = date,
                type = type,
                netDuration = netDuration,
                confirmed = confirmed
            )
        }
    }
}
