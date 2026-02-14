package com.example.worktimetracker.domain.model

import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import java.time.Duration
import java.time.LocalDateTime

/**
 * Daily work time statistics.
 *
 * @property grossWorkTime Total work time including pauses
 * @property pauseTime Total pause time
 * @property netWorkTime Net work time (gross - pauses)
 * @property targetWorkTime Target work time for the day (weekly target / work days)
 * @property remainingTime Remaining work time to reach target (target - net)
 */
data class DayStats(
    val grossWorkTime: Duration,
    val pauseTime: Duration,
    val netWorkTime: Duration,
    val targetWorkTime: Duration,
    val remainingTime: Duration
) {
    companion object {
        /**
         * Calculates daily statistics from a list of entries.
         *
         * @param entries List of tracking entries with pauses for the day
         * @param dailyTargetHours Target work hours for the day
         * @return Calculated statistics
         */
        fun from(entries: List<TrackingEntryWithPauses>, dailyTargetHours: Float): DayStats {
            // Calculate gross work time
            val grossMinutes = entries.sumOf { entryWithPauses ->
                val entry = entryWithPauses.entry
                val endTime = entry.endTime ?: LocalDateTime.now()
                Duration.between(entry.startTime, endTime).toMinutes()
            }

            // Calculate pause time (only completed pauses)
            val pauseMinutes = entries.sumOf { entryWithPauses ->
                entryWithPauses.pauses
                    .filter { it.endTime != null }
                    .sumOf { pause ->
                        Duration.between(pause.startTime, pause.endTime!!).toMinutes()
                    }
            }

            val gross = Duration.ofMinutes(grossMinutes)
            val pause = Duration.ofMinutes(pauseMinutes)
            val net = gross.minus(pause)
            val target = Duration.ofMinutes((dailyTargetHours * 60).toLong())
            val remaining = target.minus(net).let { if (it.isNegative) Duration.ZERO else it }

            return DayStats(
                grossWorkTime = gross,
                pauseTime = pause,
                netWorkTime = net,
                targetWorkTime = target,
                remainingTime = remaining
            )
        }

        /**
         * Empty stats (all zeros).
         */
        val EMPTY = DayStats(
            grossWorkTime = Duration.ZERO,
            pauseTime = Duration.ZERO,
            netWorkTime = Duration.ZERO,
            targetWorkTime = Duration.ZERO,
            remainingTime = Duration.ZERO
        )
    }
}
