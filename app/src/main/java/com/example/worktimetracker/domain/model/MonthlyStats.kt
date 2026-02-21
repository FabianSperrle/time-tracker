package com.example.worktimetracker.domain.model

import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import java.time.DayOfWeek
import java.time.Duration
import java.time.YearMonth

data class MonthlyStats(
    val month: YearMonth,
    val saldo: Duration,
    val typeBreakdown: List<TypeHours>
) {
    data class TypeHours(
        val type: TrackingType,
        val duration: Duration,
        val percentage: Double
    )

    companion object {
        val EMPTY = MonthlyStats(YearMonth.now(), Duration.ZERO, emptyList())

        fun from(
            entries: List<TrackingEntryWithPauses>,
            month: YearMonth,
            weeklyTargetHours: Float
        ): MonthlyStats {
            val workingDays = countWorkingDays(month)
            val monthlyTarget = Duration.ofMinutes((workingDays * weeklyTargetHours / 5.0 * 60).toLong())
            val totalMinutes = entries.sumOf { it.netDuration().toMinutes() }
            val total = Duration.ofMinutes(totalMinutes)
            val saldo = total.minus(monthlyTarget)
            val breakdown = entries
                .groupBy { it.entry.type }
                .map { (type, es) ->
                    val dur = Duration.ofMinutes(es.sumOf { it.netDuration().toMinutes() })
                    TypeHours(
                        type = type,
                        duration = dur,
                        percentage = if (totalMinutes > 0) dur.toMinutes() * 100.0 / totalMinutes else 0.0
                    )
                }
                .sortedByDescending { it.duration }
            return MonthlyStats(month, saldo, breakdown)
        }

        private fun countWorkingDays(ym: YearMonth): Int =
            (1..ym.lengthOfMonth()).count { day ->
                val dow = ym.atDay(day).dayOfWeek
                dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY
            }
    }
}
