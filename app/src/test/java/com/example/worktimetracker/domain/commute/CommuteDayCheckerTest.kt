package com.example.worktimetracker.domain.commute

import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.model.TimeWindow
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class CommuteDayCheckerTest {

    private lateinit var settingsProvider: SettingsProvider
    private lateinit var checker: CommuteDayChecker

    @BeforeEach
    fun setup() {
        settingsProvider = mockk()
        every { settingsProvider.commuteDays } returns flowOf(
            setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
        )
        every { settingsProvider.outboundWindow } returns flowOf(
            TimeWindow(LocalTime.of(6, 0), LocalTime.of(9, 30))
        )
        every { settingsProvider.returnWindow } returns flowOf(
            TimeWindow(LocalTime.of(16, 0), LocalTime.of(20, 0))
        )

        checker = CommuteDayChecker(settingsProvider)
    }

    // ========== isCommuteDay Tests ==========

    @Test
    fun `isCommuteDay returns true for configured commute day`() = runTest {
        // Tuesday 2026-02-10
        val tuesday = LocalDate.of(2026, 2, 10)
        assertTrue(checker.isCommuteDay(tuesday))
    }

    @Test
    fun `isCommuteDay returns true for second configured commute day`() = runTest {
        // Thursday 2026-02-12
        val thursday = LocalDate.of(2026, 2, 12)
        assertTrue(checker.isCommuteDay(thursday))
    }

    @Test
    fun `isCommuteDay returns false for non-commute weekday`() = runTest {
        // Monday 2026-02-09
        val monday = LocalDate.of(2026, 2, 9)
        assertFalse(checker.isCommuteDay(monday))
    }

    @Test
    fun `isCommuteDay returns false for weekend`() = runTest {
        // Saturday 2026-02-14
        val saturday = LocalDate.of(2026, 2, 14)
        assertFalse(checker.isCommuteDay(saturday))
    }

    @Test
    fun `isCommuteDay returns false when no commute days configured`() = runTest {
        every { settingsProvider.commuteDays } returns flowOf(emptySet())
        val newChecker = CommuteDayChecker(settingsProvider)

        val tuesday = LocalDate.of(2026, 2, 10)
        assertFalse(newChecker.isCommuteDay(tuesday))
    }

    // ========== isInOutboundWindow Tests ==========

    @Test
    fun `isInOutboundWindow returns true for time within window`() = runTest {
        val time = LocalTime.of(7, 45)
        assertTrue(checker.isInOutboundWindow(time))
    }

    @Test
    fun `isInOutboundWindow returns true at window start`() = runTest {
        val time = LocalTime.of(6, 0)
        assertTrue(checker.isInOutboundWindow(time))
    }

    @Test
    fun `isInOutboundWindow returns true at window end`() = runTest {
        val time = LocalTime.of(9, 30)
        assertTrue(checker.isInOutboundWindow(time))
    }

    @Test
    fun `isInOutboundWindow returns false before window`() = runTest {
        val time = LocalTime.of(5, 59)
        assertFalse(checker.isInOutboundWindow(time))
    }

    @Test
    fun `isInOutboundWindow returns false after window`() = runTest {
        val time = LocalTime.of(9, 31)
        assertFalse(checker.isInOutboundWindow(time))
    }

    // ========== isInReturnWindow Tests ==========

    @Test
    fun `isInReturnWindow returns true for time within window`() = runTest {
        val time = LocalTime.of(17, 30)
        assertTrue(checker.isInReturnWindow(time))
    }

    @Test
    fun `isInReturnWindow returns true at window start`() = runTest {
        val time = LocalTime.of(16, 0)
        assertTrue(checker.isInReturnWindow(time))
    }

    @Test
    fun `isInReturnWindow returns true at window end`() = runTest {
        val time = LocalTime.of(20, 0)
        assertTrue(checker.isInReturnWindow(time))
    }

    @Test
    fun `isInReturnWindow returns false before window`() = runTest {
        val time = LocalTime.of(15, 59)
        assertFalse(checker.isInReturnWindow(time))
    }

    @Test
    fun `isInReturnWindow returns false after window`() = runTest {
        val time = LocalTime.of(20, 1)
        assertFalse(checker.isInReturnWindow(time))
    }
}
