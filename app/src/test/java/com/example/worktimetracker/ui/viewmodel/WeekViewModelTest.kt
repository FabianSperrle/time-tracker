package com.example.worktimetracker.ui.viewmodel

import app.cash.turbine.test
import com.example.worktimetracker.data.local.entity.Pause
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.repository.TrackingRepository
import com.example.worktimetracker.data.settings.SettingsProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class WeekViewModelTest {

    private lateinit var repository: TrackingRepository
    private lateinit var settingsProvider: SettingsProvider
    private lateinit var viewModel: WeekViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        settingsProvider = mockk()
        every { repository.getAllEntriesWithPauses() } returns flowOf(emptyList())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `weekSummaries emits daily summaries for current week`() = runTest {
        val monday = LocalDate.of(2026, 2, 16) // KW 08 – actual Monday of current week
        val entry1 = TrackingEntryWithPauses(
            entry = TrackingEntry(
                id = "1",
                date = monday,
                type = TrackingType.HOME_OFFICE,
                startTime = LocalDateTime.of(2026, 2, 16, 8, 0),
                endTime = LocalDateTime.of(2026, 2, 16, 16, 0),
                autoDetected = true,
                confirmed = true
            ),
            pauses = emptyList()
        )

        every { repository.getEntriesInRange(any(), any()) } returns flowOf(listOf(entry1))
        every { settingsProvider.weeklyTargetHours } returns flowOf(40f)

        viewModel = WeekViewModel(repository, settingsProvider)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.weekSummaries.test {
            val summaries = awaitItem()
            assertEquals(5, summaries.size) // Mo-Fr
            assertEquals(TrackingType.HOME_OFFICE, summaries[0].type)
            assertEquals(Duration.ofHours(8), summaries[0].netDuration)
            assertNull(summaries[1].type) // No entry for Tuesday
        }
    }

    @Test
    fun `weekStats calculates correct statistics`() = runTest {
        val monday = LocalDate.of(2026, 2, 16) // KW 08
        val entries = listOf(
            TrackingEntryWithPauses(
                entry = TrackingEntry(
                    id = "1",
                    date = monday,
                    type = TrackingType.HOME_OFFICE,
                    startTime = LocalDateTime.of(2026, 2, 16, 8, 0),
                    endTime = LocalDateTime.of(2026, 2, 16, 16, 0),
                    autoDetected = true,
                    confirmed = true
                ),
                pauses = emptyList()
            ),
            TrackingEntryWithPauses(
                entry = TrackingEntry(
                    id = "2",
                    date = monday.plusDays(1),
                    type = TrackingType.COMMUTE_OFFICE,
                    startTime = LocalDateTime.of(2026, 2, 17, 7, 0),
                    endTime = LocalDateTime.of(2026, 2, 17, 16, 0),
                    autoDetected = true,
                    confirmed = true
                ),
                pauses = emptyList()
            )
        )

        every { repository.getEntriesInRange(any(), any()) } returns flowOf(entries)
        every { settingsProvider.weeklyTargetHours } returns flowOf(40f)

        viewModel = WeekViewModel(repository, settingsProvider)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.weekStats.test {
            val stats = awaitItem()
            assertEquals(Duration.ofHours(17), stats.totalDuration)
            assertEquals(Duration.ofHours(40), stats.targetDuration)
            assertEquals(42.5, stats.percentage, 0.1)
        }
    }

    @Test
    fun `previousWeek navigates to previous week`() = runTest {
        val currentMonday = LocalDate.of(2026, 2, 10)

        every { repository.getEntriesInRange(any(), any()) } returns flowOf(emptyList())
        every { settingsProvider.weeklyTargetHours } returns flowOf(40f)

        viewModel = WeekViewModel(repository, settingsProvider)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectedWeekStart.test {
            val initialWeek = awaitItem()
            viewModel.previousWeek()
            testDispatcher.scheduler.advanceUntilIdle()
            val previousWeek = awaitItem()
            assertEquals(7, java.time.temporal.ChronoUnit.DAYS.between(previousWeek, initialWeek))
        }
    }

    @Test
    fun `nextWeek navigates to next week`() = runTest {
        every { repository.getEntriesInRange(any(), any()) } returns flowOf(emptyList())
        every { settingsProvider.weeklyTargetHours } returns flowOf(40f)

        viewModel = WeekViewModel(repository, settingsProvider)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectedWeekStart.test {
            val initialWeek = awaitItem()
            viewModel.nextWeek()
            testDispatcher.scheduler.advanceUntilIdle()
            val nextWeek = awaitItem()
            assertEquals(7, java.time.temporal.ChronoUnit.DAYS.between(initialWeek, nextWeek))
        }
    }

    @Test
    fun `weekSummaries handles pauses correctly`() = runTest {
        val monday = LocalDate.of(2026, 2, 16) // KW 08
        val entry = TrackingEntryWithPauses(
            entry = TrackingEntry(
                id = "1",
                date = monday,
                type = TrackingType.HOME_OFFICE,
                startTime = LocalDateTime.of(2026, 2, 16, 8, 0),
                endTime = LocalDateTime.of(2026, 2, 16, 17, 0),
                autoDetected = false,
                confirmed = false
            ),
            pauses = listOf(
                Pause(
                    id = "p1",
                    entryId = "1",
                    startTime = LocalDateTime.of(2026, 2, 16, 12, 0),
                    endTime = LocalDateTime.of(2026, 2, 16, 13, 0)
                )
            )
        )

        every { repository.getEntriesInRange(any(), any()) } returns flowOf(listOf(entry))
        every { settingsProvider.weeklyTargetHours } returns flowOf(40f)

        viewModel = WeekViewModel(repository, settingsProvider)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.weekSummaries.test {
            val summaries = awaitItem()
            // 9 hours gross - 1 hour pause = 8 hours net
            assertEquals(Duration.ofHours(8), summaries[0].netDuration)
            assertFalse(summaries[0].confirmed)
        }
    }

    @Test
    fun `weekNumber returns correct calendar week`() = runTest {
        every { repository.getEntriesInRange(any(), any()) } returns flowOf(emptyList())
        every { settingsProvider.weeklyTargetHours } returns flowOf(40f)

        viewModel = WeekViewModel(repository, settingsProvider)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.weekNumber.test {
            val weekNum = awaitItem()
            assertTrue(weekNum > 0 && weekNum <= 53)
        }
    }

    @Test
    fun `hasUnconfirmedEntries is true when any entry is unconfirmed`() = runTest {
        val monday = LocalDate.of(2026, 2, 16) // KW 08
        val entries = listOf(
            TrackingEntryWithPauses(
                entry = TrackingEntry(
                    id = "1",
                    date = monday,
                    type = TrackingType.HOME_OFFICE,
                    startTime = LocalDateTime.of(2026, 2, 16, 8, 0),
                    endTime = LocalDateTime.of(2026, 2, 16, 16, 0),
                    autoDetected = true,
                    confirmed = false // Unconfirmed
                ),
                pauses = emptyList()
            )
        )

        every { repository.getEntriesInRange(any(), any()) } returns flowOf(entries)
        every { settingsProvider.weeklyTargetHours } returns flowOf(40f)

        viewModel = WeekViewModel(repository, settingsProvider)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.hasUnconfirmedEntries.test {
            assertTrue(awaitItem())
        }
    }
}
