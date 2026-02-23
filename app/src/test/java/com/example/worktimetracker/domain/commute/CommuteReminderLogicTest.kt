package com.example.worktimetracker.domain.commute

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalTime

class CommuteReminderLogicTest {

    // ========== No Tracking Reminder ==========

    @Nested
    inner class NoTrackingReminder {

        @Test
        fun `returns true when past reminder time on commute day without active tracking`() {
            val result = CommuteReminderLogic.shouldShowNoTrackingReminder(
                currentTime = LocalTime.of(10, 30),
                reminderTime = LocalTime.of(10, 0),
                isCommuteDay = true,
                hasTrackingToday = false
            )
            assertTrue(result)
        }

        @Test
        fun `returns true at exactly the reminder time`() {
            val result = CommuteReminderLogic.shouldShowNoTrackingReminder(
                currentTime = LocalTime.of(10, 0),
                reminderTime = LocalTime.of(10, 0),
                isCommuteDay = true,
                hasTrackingToday = false
            )
            assertTrue(result)
        }

        @Test
        fun `returns false when not a commute day`() {
            val result = CommuteReminderLogic.shouldShowNoTrackingReminder(
                currentTime = LocalTime.of(10, 30),
                reminderTime = LocalTime.of(10, 0),
                isCommuteDay = false,
                hasTrackingToday = false
            )
            assertFalse(result)
        }

        @Test
        fun `returns false when tracking is already active`() {
            val result = CommuteReminderLogic.shouldShowNoTrackingReminder(
                currentTime = LocalTime.of(10, 30),
                reminderTime = LocalTime.of(10, 0),
                isCommuteDay = true,
                hasTrackingToday = true
            )
            assertFalse(result)
        }

        @Test
        fun `returns false before reminder time`() {
            val result = CommuteReminderLogic.shouldShowNoTrackingReminder(
                currentTime = LocalTime.of(9, 30),
                reminderTime = LocalTime.of(10, 0),
                isCommuteDay = true,
                hasTrackingToday = false
            )
            assertFalse(result)
        }

        @Test
        fun `returns false one minute before reminder time`() {
            val result = CommuteReminderLogic.shouldShowNoTrackingReminder(
                currentTime = LocalTime.of(9, 59),
                reminderTime = LocalTime.of(10, 0),
                isCommuteDay = true,
                hasTrackingToday = false
            )
            assertFalse(result)
        }

        @Test
        fun `returns false when not commute day even if past time and no tracking`() {
            val result = CommuteReminderLogic.shouldShowNoTrackingReminder(
                currentTime = LocalTime.of(12, 0),
                reminderTime = LocalTime.of(10, 0),
                isCommuteDay = false,
                hasTrackingToday = false
            )
            assertFalse(result)
        }
    }

    // ========== Late Tracking Reminder ==========

    @Nested
    inner class LateTrackingReminder {

        @Test
        fun `returns true when past cutoff time with active tracking`() {
            val result = CommuteReminderLogic.shouldShowLateTrackingReminder(
                currentTime = LocalTime.of(21, 30),
                cutoffTime = LocalTime.of(21, 0),
                hasTrackingToday = true
            )
            assertTrue(result)
        }

        @Test
        fun `returns true at exactly the cutoff time`() {
            val result = CommuteReminderLogic.shouldShowLateTrackingReminder(
                currentTime = LocalTime.of(21, 0),
                cutoffTime = LocalTime.of(21, 0),
                hasTrackingToday = true
            )
            assertTrue(result)
        }

        @Test
        fun `returns false when no active tracking`() {
            val result = CommuteReminderLogic.shouldShowLateTrackingReminder(
                currentTime = LocalTime.of(21, 30),
                cutoffTime = LocalTime.of(21, 0),
                hasTrackingToday = false
            )
            assertFalse(result)
        }

        @Test
        fun `returns false before cutoff time`() {
            val result = CommuteReminderLogic.shouldShowLateTrackingReminder(
                currentTime = LocalTime.of(20, 30),
                cutoffTime = LocalTime.of(21, 0),
                hasTrackingToday = true
            )
            assertFalse(result)
        }

        @Test
        fun `returns false one minute before cutoff time`() {
            val result = CommuteReminderLogic.shouldShowLateTrackingReminder(
                currentTime = LocalTime.of(20, 59),
                cutoffTime = LocalTime.of(21, 0),
                hasTrackingToday = true
            )
            assertFalse(result)
        }

        @Test
        fun `returns false when before cutoff and no active tracking`() {
            val result = CommuteReminderLogic.shouldShowLateTrackingReminder(
                currentTime = LocalTime.of(20, 0),
                cutoffTime = LocalTime.of(21, 0),
                hasTrackingToday = false
            )
            assertFalse(result)
        }
    }

    // ========== Default Constants ==========

    @Test
    fun `default no tracking reminder time is 10 00`() {
        val expected = LocalTime.of(10, 0)
        assertTrue(CommuteReminderLogic.DEFAULT_NO_TRACKING_REMINDER_TIME == expected)
    }

    @Test
    fun `default late tracking cutoff time is 21 00`() {
        val expected = LocalTime.of(21, 0)
        assertTrue(CommuteReminderLogic.DEFAULT_LATE_TRACKING_CUTOFF == expected)
    }
}
