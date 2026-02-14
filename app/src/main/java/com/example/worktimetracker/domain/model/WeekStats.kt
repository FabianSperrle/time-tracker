package com.example.worktimetracker.domain.model

import java.time.Duration

/**
 * Weekly work time statistics.
 *
 * @property totalDuration Total work duration for the week
 * @property targetDuration Target work duration for the week
 * @property percentage Percentage of target achieved
 * @property overtime Overtime (positive) or undertime (negative)
 * @property averagePerDay Average work duration per worked day
 */
data class WeekStats(
    val totalDuration: Duration,
    val targetDuration: Duration,
    val percentage: Double,
    val overtime: Duration,
    val averagePerDay: Duration
) {
    companion object {
        /**
         * Calculates week statistics from daily summaries.
         *
         * @param summaries List of daily summaries for the week
         * @param targetHours Target work hours for the week
         * @return Calculated statistics
         */
        fun from(summaries: List<DaySummary>, targetHours: Float): WeekStats {
            // Calculate total duration
            val totalMinutes = summaries.sumOf { it.netDuration.toMinutes() }
            val totalDuration = Duration.ofMinutes(totalMinutes)

            // Calculate target
            val targetDuration = Duration.ofMinutes((targetHours * 60).toLong())

            // Calculate percentage
            val percentage = if (targetDuration.toMinutes() > 0) {
                (totalDuration.toMinutes().toDouble() / targetDuration.toMinutes().toDouble()) * 100.0
            } else {
                0.0
            }

            // Calculate overtime
            val overtime = totalDuration.minus(targetDuration)

            // Calculate average per worked day (exclude days with no work)
            val workedDays = summaries.count { it.netDuration > Duration.ZERO }
            val averagePerDay = if (workedDays > 0) {
                Duration.ofMinutes(totalMinutes / workedDays)
            } else {
                Duration.ZERO
            }

            return WeekStats(
                totalDuration = totalDuration,
                targetDuration = targetDuration,
                percentage = percentage,
                overtime = overtime,
                averagePerDay = averagePerDay
            )
        }

        /**
         * Empty statistics.
         */
        val EMPTY = WeekStats(
            totalDuration = Duration.ZERO,
            targetDuration = Duration.ZERO,
            percentage = 0.0,
            overtime = Duration.ZERO,
            averagePerDay = Duration.ZERO
        )
    }
}
