package com.example.worktimetracker.domain.model

import com.example.worktimetracker.data.local.entity.TrackingType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

class WeekStatsTest {

    @Test
    fun `from calculates correct statistics for full week`() {
        val summaries = listOf(
            DaySummary(
                date = LocalDate.of(2026, 2, 10),
                type = TrackingType.HOME_OFFICE,
                netDuration = Duration.ofHours(8),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 11),
                type = TrackingType.COMMUTE_OFFICE,
                netDuration = Duration.ofHours(9),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 12),
                type = TrackingType.HOME_OFFICE,
                netDuration = Duration.ofHours(7).plusMinutes(30),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 13),
                type = TrackingType.COMMUTE_OFFICE,
                netDuration = Duration.ofHours(8).plusMinutes(30),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 14),
                type = TrackingType.HOME_OFFICE,
                netDuration = Duration.ofHours(7),
                confirmed = true
            )
        )

        val stats = WeekStats.from(summaries, targetHours = 40f)

        // Total: 8 + 9 + 7.5 + 8.5 + 7 = 40 hours
        assertEquals(Duration.ofHours(40), stats.totalDuration)
        assertEquals(Duration.ofHours(40), stats.targetDuration)
        assertEquals(100.0, stats.percentage, 0.1)
        assertEquals(Duration.ZERO, stats.overtime)
        assertEquals(Duration.ofHours(8), stats.averagePerDay)
    }

    @Test
    fun `from calculates overtime correctly`() {
        val summaries = listOf(
            DaySummary(
                date = LocalDate.of(2026, 2, 10),
                type = TrackingType.HOME_OFFICE,
                netDuration = Duration.ofHours(9),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 11),
                type = TrackingType.COMMUTE_OFFICE,
                netDuration = Duration.ofHours(10),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 12),
                type = TrackingType.HOME_OFFICE,
                netDuration = Duration.ofHours(9),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 13),
                type = TrackingType.COMMUTE_OFFICE,
                netDuration = Duration.ofHours(9),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 14),
                type = TrackingType.HOME_OFFICE,
                netDuration = Duration.ofHours(8),
                confirmed = true
            )
        )

        val stats = WeekStats.from(summaries, targetHours = 40f)

        // Total: 45 hours
        assertEquals(Duration.ofHours(45), stats.totalDuration)
        assertEquals(Duration.ofHours(5), stats.overtime) // +5 hours
        assertEquals(112.5, stats.percentage, 0.1)
    }

    @Test
    fun `from calculates negative overtime for under target`() {
        val summaries = listOf(
            DaySummary(
                date = LocalDate.of(2026, 2, 10),
                type = TrackingType.HOME_OFFICE,
                netDuration = Duration.ofHours(7),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 11),
                type = TrackingType.COMMUTE_OFFICE,
                netDuration = Duration.ofHours(7),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 12),
                type = TrackingType.HOME_OFFICE,
                netDuration = Duration.ofHours(6),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 13),
                type = TrackingType.COMMUTE_OFFICE,
                netDuration = Duration.ofHours(7),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 14),
                type = TrackingType.HOME_OFFICE,
                netDuration = Duration.ofHours(7),
                confirmed = true
            )
        )

        val stats = WeekStats.from(summaries, targetHours = 40f)

        // Total: 34 hours
        assertEquals(Duration.ofHours(34), stats.totalDuration)
        assertEquals(Duration.ofHours(-6), stats.overtime) // -6 hours
        assertEquals(85.0, stats.percentage, 0.1)
    }

    @Test
    fun `from handles empty week`() {
        val stats = WeekStats.from(emptyList(), targetHours = 40f)

        assertEquals(Duration.ZERO, stats.totalDuration)
        assertEquals(Duration.ofHours(40), stats.targetDuration)
        assertEquals(0.0, stats.percentage, 0.1)
        assertEquals(Duration.ofHours(-40), stats.overtime)
        assertEquals(Duration.ZERO, stats.averagePerDay)
    }

    @Test
    fun `from calculates average only for worked days`() {
        val summaries = listOf(
            DaySummary(
                date = LocalDate.of(2026, 2, 10),
                type = TrackingType.HOME_OFFICE,
                netDuration = Duration.ofHours(8),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 11),
                type = null, // No work
                netDuration = Duration.ZERO,
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 12),
                type = TrackingType.HOME_OFFICE,
                netDuration = Duration.ofHours(8),
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 13),
                type = null, // No work
                netDuration = Duration.ZERO,
                confirmed = true
            ),
            DaySummary(
                date = LocalDate.of(2026, 2, 14),
                type = TrackingType.HOME_OFFICE,
                netDuration = Duration.ofHours(9),
                confirmed = true
            )
        )

        val stats = WeekStats.from(summaries, targetHours = 40f)

        // Total: 25 hours over 3 worked days
        assertEquals(Duration.ofHours(25), stats.totalDuration)
        // Average: 25 / 3 = 8.33 hours
        assertEquals(Duration.ofMinutes(500), stats.averagePerDay) // 8h 20min
    }

    @Test
    fun `EMPTY has all zeros`() {
        assertEquals(Duration.ZERO, WeekStats.EMPTY.totalDuration)
        assertEquals(Duration.ZERO, WeekStats.EMPTY.targetDuration)
        assertEquals(0.0, WeekStats.EMPTY.percentage, 0.1)
        assertEquals(Duration.ZERO, WeekStats.EMPTY.overtime)
        assertEquals(Duration.ZERO, WeekStats.EMPTY.averagePerDay)
    }
}
