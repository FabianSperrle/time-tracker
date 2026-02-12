package com.example.worktimetracker.data.local.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class TrackingEntryWithPausesTest {

    @Test
    fun `netDuration with no pauses equals gross duration`() {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 11, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 11, 17, 0),
            autoDetected = true
        )
        val entryWithPauses = TrackingEntryWithPauses(
            entry = entry,
            pauses = emptyList()
        )

        val netDuration = entryWithPauses.netDuration()

        assertEquals(9 * 60, netDuration.toMinutes())
    }

    @Test
    fun `netDuration subtracts completed pauses`() {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 11, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 11, 17, 0),
            autoDetected = true
        )
        val pause1 = Pause(
            id = "p1",
            entryId = "1",
            startTime = LocalDateTime.of(2026, 2, 11, 12, 0),
            endTime = LocalDateTime.of(2026, 2, 11, 12, 30)
        )
        val pause2 = Pause(
            id = "p2",
            entryId = "1",
            startTime = LocalDateTime.of(2026, 2, 11, 15, 0),
            endTime = LocalDateTime.of(2026, 2, 11, 15, 15)
        )
        val entryWithPauses = TrackingEntryWithPauses(
            entry = entry,
            pauses = listOf(pause1, pause2)
        )

        val netDuration = entryWithPauses.netDuration()

        // 9 hours minus 30 minutes minus 15 minutes = 8 hours 15 minutes = 495 minutes
        assertEquals(495, netDuration.toMinutes())
    }

    @Test
    fun `netDuration ignores running pauses without endTime`() {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 11, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 11, 17, 0),
            autoDetected = false
        )
        val completedPause = Pause(
            id = "p1",
            entryId = "1",
            startTime = LocalDateTime.of(2026, 2, 11, 12, 0),
            endTime = LocalDateTime.of(2026, 2, 11, 12, 30)
        )
        val runningPause = Pause(
            id = "p2",
            entryId = "1",
            startTime = LocalDateTime.of(2026, 2, 11, 15, 0),
            endTime = null
        )
        val entryWithPauses = TrackingEntryWithPauses(
            entry = entry,
            pauses = listOf(completedPause, runningPause)
        )

        val netDuration = entryWithPauses.netDuration()

        // 9 hours minus 30 minutes = 8 hours 30 minutes = 510 minutes
        assertEquals(510, netDuration.toMinutes())
    }

    @Test
    fun `netDuration uses current time when entry is still running`() {
        val startTime = LocalDateTime.now().minusHours(2)
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = startTime,
            endTime = null,
            autoDetected = true
        )
        val entryWithPauses = TrackingEntryWithPauses(
            entry = entry,
            pauses = emptyList()
        )

        val netDuration = entryWithPauses.netDuration()

        // Should be approximately 2 hours (120 minutes), allow 1 minute tolerance
        val expectedMinutes = Duration.between(startTime, LocalDateTime.now()).toMinutes()
        assertEquals(expectedMinutes.toDouble(), netDuration.toMinutes().toDouble(), 1.0)
    }

    @Test
    fun `netDuration with multiple pauses calculates correctly`() {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 11, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 11, 18, 0),
            autoDetected = true
        )
        val pauses = listOf(
            Pause("p1", "1", LocalDateTime.of(2026, 2, 11, 10, 0), LocalDateTime.of(2026, 2, 11, 10, 15)),
            Pause("p2", "1", LocalDateTime.of(2026, 2, 11, 12, 0), LocalDateTime.of(2026, 2, 11, 13, 0)),
            Pause("p3", "1", LocalDateTime.of(2026, 2, 11, 15, 30), LocalDateTime.of(2026, 2, 11, 15, 45))
        )
        val entryWithPauses = TrackingEntryWithPauses(
            entry = entry,
            pauses = pauses
        )

        val netDuration = entryWithPauses.netDuration()

        // 10 hours - 15 min - 60 min - 15 min = 8 hours 30 minutes = 510 minutes
        assertEquals(510, netDuration.toMinutes())
    }
}
