package com.example.worktimetracker.domain.model

import com.example.worktimetracker.data.local.entity.Pause
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class DayStatsTest {

    @Test
    fun `from should calculate correct stats for single completed entry`() {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 14),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.of(2026, 2, 14, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 14, 12, 0),
            autoDetected = false
        )
        val stats = DayStats.from(listOf(TrackingEntryWithPauses(entry, emptyList())), 8.0f)

        assertEquals(Duration.ofHours(4), stats.grossWorkTime)
        assertEquals(Duration.ZERO, stats.pauseTime)
        assertEquals(Duration.ofHours(4), stats.netWorkTime)
        assertEquals(Duration.ofHours(8), stats.targetWorkTime)
        assertEquals(Duration.ofHours(4), stats.remainingTime)
    }

    @Test
    fun `from should calculate correct stats with pauses`() {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 14),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.of(2026, 2, 14, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 14, 17, 0),
            autoDetected = false
        )
        val pause = Pause(
            entryId = "1",
            startTime = LocalDateTime.of(2026, 2, 14, 12, 0),
            endTime = LocalDateTime.of(2026, 2, 14, 13, 0)
        )
        val stats = DayStats.from(
            listOf(TrackingEntryWithPauses(entry, listOf(pause))),
            8.0f
        )

        assertEquals(Duration.ofHours(9), stats.grossWorkTime)
        assertEquals(Duration.ofHours(1), stats.pauseTime)
        assertEquals(Duration.ofHours(8), stats.netWorkTime)
        assertEquals(Duration.ofHours(8), stats.targetWorkTime)
        assertEquals(Duration.ZERO, stats.remainingTime)
    }

    @Test
    fun `from should calculate correct stats for multiple entries`() {
        val entry1 = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 14),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.of(2026, 2, 14, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 14, 12, 0),
            autoDetected = false
        )
        val entry2 = TrackingEntry(
            id = "2",
            date = LocalDate.of(2026, 2, 14),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 14, 13, 0),
            endTime = LocalDateTime.of(2026, 2, 14, 17, 0),
            autoDetected = false
        )
        val stats = DayStats.from(
            listOf(
                TrackingEntryWithPauses(entry1, emptyList()),
                TrackingEntryWithPauses(entry2, emptyList())
            ),
            8.0f
        )

        assertEquals(Duration.ofHours(8), stats.grossWorkTime)
        assertEquals(Duration.ZERO, stats.pauseTime)
        assertEquals(Duration.ofHours(8), stats.netWorkTime)
        assertEquals(Duration.ofHours(8), stats.targetWorkTime)
        assertEquals(Duration.ZERO, stats.remainingTime)
    }

    @Test
    fun `from should handle active tracking entry correctly`() {
        val now = LocalDateTime.now()
        val startTime = now.minusHours(2)
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.MANUAL,
            startTime = startTime,
            endTime = null, // Still active
            autoDetected = false
        )
        val stats = DayStats.from(listOf(TrackingEntryWithPauses(entry, emptyList())), 8.0f)

        // Gross time should be approximately 2 hours (within 1 minute tolerance)
        val expectedGross = Duration.ofHours(2)
        val tolerance = Duration.ofMinutes(1)
        val diff = stats.grossWorkTime.minus(expectedGross).abs()
        assert(diff < tolerance) {
            "Expected gross time ~2h, got ${stats.grossWorkTime}"
        }
    }

    @Test
    fun `from should return zero stats for empty entries`() {
        val stats = DayStats.from(emptyList(), 8.0f)

        assertEquals(Duration.ZERO, stats.grossWorkTime)
        assertEquals(Duration.ZERO, stats.pauseTime)
        assertEquals(Duration.ZERO, stats.netWorkTime)
        assertEquals(Duration.ofHours(8), stats.targetWorkTime)
        assertEquals(Duration.ofHours(8), stats.remainingTime)
    }

    @Test
    fun `from should handle active pause correctly`() {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.now().minusHours(3),
            endTime = null,
            autoDetected = false
        )
        val completedPause = Pause(
            entryId = "1",
            startTime = LocalDateTime.now().minusHours(2),
            endTime = LocalDateTime.now().minusHours(1)
        )
        val activePause = Pause(
            entryId = "1",
            startTime = LocalDateTime.now().minusMinutes(30),
            endTime = null
        )
        val stats = DayStats.from(
            listOf(TrackingEntryWithPauses(entry, listOf(completedPause, activePause))),
            8.0f
        )

        // Should only count completed pause (1 hour)
        val tolerance = Duration.ofMinutes(1)
        val diff = stats.pauseTime.minus(Duration.ofHours(1)).abs()
        assert(diff < tolerance) {
            "Expected pause time ~1h, got ${stats.pauseTime}"
        }
    }

    @Test
    fun `from should handle overtime correctly`() {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 14),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.of(2026, 2, 14, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 14, 19, 0), // 11 hours
            autoDetected = false
        )
        val stats = DayStats.from(listOf(TrackingEntryWithPauses(entry, emptyList())), 8.0f)

        assertEquals(Duration.ofHours(11), stats.grossWorkTime)
        assertEquals(Duration.ofHours(11), stats.netWorkTime)
        assertEquals(Duration.ofHours(8), stats.targetWorkTime)
        // Overtime - remaining should be negative (or zero)
        assert(stats.remainingTime <= Duration.ZERO) {
            "Expected remaining time to be <= 0 for overtime, got ${stats.remainingTime}"
        }
    }
}
