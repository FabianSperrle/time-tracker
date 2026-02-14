package com.example.worktimetracker.domain.model

import com.example.worktimetracker.data.local.entity.Pause
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class DaySummaryTest {

    @Test
    fun `from creates summary from single entry with home office`() {
        val date = LocalDate.of(2026, 2, 10)
        val entry = TrackingEntry(
            id = "1",
            date = date,
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 16, 30),
            autoDetected = true,
            confirmed = true
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, emptyList())

        val summary = DaySummary.from(date, listOf(entryWithPauses))

        assertEquals(date, summary.date)
        assertEquals(TrackingType.HOME_OFFICE, summary.type)
        assertEquals(Duration.ofHours(8).plusMinutes(30), summary.netDuration)
        assertTrue(summary.confirmed)
    }

    @Test
    fun `from creates summary from multiple entries uses first type`() {
        val date = LocalDate.of(2026, 2, 11)
        val entry1 = TrackingEntry(
            id = "1",
            date = date,
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 11, 7, 0),
            endTime = LocalDateTime.of(2026, 2, 11, 12, 0),
            autoDetected = true,
            confirmed = true
        )
        val entry2 = TrackingEntry(
            id = "2",
            date = date,
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 11, 13, 0),
            endTime = LocalDateTime.of(2026, 2, 11, 17, 0),
            autoDetected = true,
            confirmed = true
        )

        val summary = DaySummary.from(
            date,
            listOf(
                TrackingEntryWithPauses(entry1, emptyList()),
                TrackingEntryWithPauses(entry2, emptyList())
            )
        )

        assertEquals(TrackingType.COMMUTE_OFFICE, summary.type)
        assertEquals(Duration.ofHours(9), summary.netDuration)
        assertTrue(summary.confirmed)
    }

    @Test
    fun `from calculates net duration with pauses`() {
        val date = LocalDate.of(2026, 2, 12)
        val entry = TrackingEntry(
            id = "1",
            date = date,
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 12, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 12, 17, 0),
            autoDetected = false,
            confirmed = false
        )
        val pause = Pause(
            id = "p1",
            entryId = "1",
            startTime = LocalDateTime.of(2026, 2, 12, 12, 0),
            endTime = LocalDateTime.of(2026, 2, 12, 13, 0)
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, listOf(pause))

        val summary = DaySummary.from(date, listOf(entryWithPauses))

        // 9 hours gross - 1 hour pause = 8 hours net
        assertEquals(Duration.ofHours(8), summary.netDuration)
        assertFalse(summary.confirmed)
    }

    @Test
    fun `from returns empty summary for no entries`() {
        val date = LocalDate.of(2026, 2, 13)

        val summary = DaySummary.from(date, emptyList())

        assertEquals(date, summary.date)
        assertNull(summary.type)
        assertEquals(Duration.ZERO, summary.netDuration)
        assertTrue(summary.confirmed) // No unconfirmed entries
    }

    @Test
    fun `from marks as unconfirmed if any entry is unconfirmed`() {
        val date = LocalDate.of(2026, 2, 14)
        val entry1 = TrackingEntry(
            id = "1",
            date = date,
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.of(2026, 2, 14, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 14, 12, 0),
            autoDetected = false,
            confirmed = true
        )
        val entry2 = TrackingEntry(
            id = "2",
            date = date,
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.of(2026, 2, 14, 13, 0),
            endTime = LocalDateTime.of(2026, 2, 14, 17, 0),
            autoDetected = false,
            confirmed = false // One unconfirmed
        )

        val summary = DaySummary.from(
            date,
            listOf(
                TrackingEntryWithPauses(entry1, emptyList()),
                TrackingEntryWithPauses(entry2, emptyList())
            )
        )

        assertFalse(summary.confirmed)
    }
}
