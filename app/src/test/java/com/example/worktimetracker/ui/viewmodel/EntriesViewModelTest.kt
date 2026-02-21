package com.example.worktimetracker.ui.viewmodel

import app.cash.turbine.test
import com.example.worktimetracker.data.local.entity.Pause
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.repository.TrackingRepository
import com.example.worktimetracker.ui.screens.EntriesListItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class EntriesViewModelTest {

    private lateinit var repository: TrackingRepository
    private lateinit var viewModel: EntriesViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = EntriesViewModel(repository)
    }

    @Test
    fun `initial state has empty entries list`() = runTest {
        every { repository.getAllEntriesWithPauses() } returns flowOf(emptyList())

        viewModel.entries.test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `entries are loaded from repository`() = runTest {
        val entry1 = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.now().minusHours(8),
            endTime = LocalDateTime.now(),
            autoDetected = true,
            confirmed = false
        )
        val entry2 = TrackingEntry(
            id = "2",
            date = LocalDate.now().minusDays(1),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.now().minusDays(1).minusHours(8),
            endTime = LocalDateTime.now().minusDays(1),
            autoDetected = false,
            confirmed = true
        )
        val pause = Pause(
            id = "p1",
            entryId = "1",
            startTime = LocalDateTime.now().minusHours(4),
            endTime = LocalDateTime.now().minusHours(3).minusMinutes(30)
        )

        // Set up mock BEFORE creating the viewModel so the stateIn collects the correct data
        every { repository.getAllEntriesWithPauses() } returns flowOf(
            listOf(
                TrackingEntryWithPauses(entry1, listOf(pause)),
                TrackingEntryWithPauses(entry2, emptyList())
            )
        )
        viewModel = EntriesViewModel(repository)

        viewModel.entries.test {
            var entries = awaitItem()
            if (entries.isEmpty()) entries = awaitItem()
            assertEquals(2, entries.size)
            assertEquals("1", entries[0].entry.id)
            assertEquals("2", entries[1].entry.id)
            assertEquals(1, entries[0].pauses.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteEntry calls repository and shows confirmation dialog`() = runTest {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.now().minusHours(8),
            endTime = LocalDateTime.now(),
            autoDetected = false,
            confirmed = true
        )
        coEvery { repository.deleteEntry(entry) } returns Unit

        viewModel.showDeleteConfirmation(entry)

        viewModel.deleteConfirmationState.test {
            val state = awaitItem()
            assertTrue(state.showDialog)
            assertEquals(entry, state.entryToDelete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirmDelete executes deletion and hides dialog`() = runTest {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.now().minusHours(8),
            endTime = LocalDateTime.now(),
            autoDetected = false,
            confirmed = true
        )
        coEvery { repository.deleteEntry(entry) } returns Unit

        viewModel.showDeleteConfirmation(entry)
        viewModel.confirmDelete()

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteEntry(entry) }

        viewModel.deleteConfirmationState.test {
            val state = awaitItem()
            assertFalse(state.showDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancelDelete hides dialog without deletion`() = runTest {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.now().minusHours(8),
            endTime = LocalDateTime.now(),
            autoDetected = false,
            confirmed = true
        )

        viewModel.showDeleteConfirmation(entry)
        viewModel.cancelDelete()

        viewModel.deleteConfirmationState.test {
            val state = awaitItem()
            assertFalse(state.showDialog)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { repository.deleteEntry(any()) }
    }

    // ── Month navigation ────────────────────────────────────────────────────

    @Test
    fun `previousMonth decrements selectedYearMonth by one month`() = runTest {
        val initial = viewModel.selectedYearMonth.value
        viewModel.previousMonth()
        assertEquals(initial.minusMonths(1), viewModel.selectedYearMonth.value)
    }

    @Test
    fun `nextMonth increments selectedYearMonth by one month`() = runTest {
        val initial = viewModel.selectedYearMonth.value
        viewModel.nextMonth()
        assertEquals(initial.plusMonths(1), viewModel.selectedYearMonth.value)
    }

    // ── listItems grouping and filtering ────────────────────────────────────

    @Test
    fun `listItems groups entries by calendar week with headers`() = runTest {
        // KW 6 (Feb 2026): Feb 4 and Feb 5
        // KW 7 (Feb 2026): Feb 10
        val entryKw6a = makeEntry("kw6a", LocalDate.of(2026, 2, 4))
        val entryKw6b = makeEntry("kw6b", LocalDate.of(2026, 2, 5))
        val entryKw7  = makeEntry("kw7",  LocalDate.of(2026, 2, 10))

        every { repository.getAllEntriesWithPauses() } returns flowOf(
            listOf(
                TrackingEntryWithPauses(entryKw6a, emptyList()),
                TrackingEntryWithPauses(entryKw6b, emptyList()),
                TrackingEntryWithPauses(entryKw7,  emptyList())
            )
        )
        viewModel = EntriesViewModel(repository)
        // Navigate to Feb 2026
        while (viewModel.selectedYearMonth.value != YearMonth.of(2026, 2)) {
            if (viewModel.selectedYearMonth.value.isBefore(YearMonth.of(2026, 2)))
                viewModel.nextMonth()
            else
                viewModel.previousMonth()
        }

        viewModel.listItems.test {
            var items = awaitItem()
            if (items.isEmpty()) items = awaitItem()

            val headers = items.filterIsInstance<EntriesListItem.WeekHeader>()
            val entries = items.filterIsInstance<EntriesListItem.EntryItem>()

            assertEquals(2, headers.size)
            assertEquals(3, entries.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `listItems filters by selected month`() = runTest {
        val entryJan = makeEntry("jan", LocalDate.of(2026, 1, 15))
        val entryFeb = makeEntry("feb", LocalDate.of(2026, 2, 10))

        every { repository.getAllEntriesWithPauses() } returns flowOf(
            listOf(
                TrackingEntryWithPauses(entryJan, emptyList()),
                TrackingEntryWithPauses(entryFeb, emptyList())
            )
        )
        viewModel = EntriesViewModel(repository)
        // Navigate to Jan 2026
        while (viewModel.selectedYearMonth.value != YearMonth.of(2026, 1)) {
            if (viewModel.selectedYearMonth.value.isBefore(YearMonth.of(2026, 1)))
                viewModel.nextMonth()
            else
                viewModel.previousMonth()
        }

        viewModel.listItems.test {
            var items = awaitItem()
            if (items.isEmpty()) items = awaitItem()

            val entryItems = items.filterIsInstance<EntriesListItem.EntryItem>()
            assertEquals(1, entryItems.size)
            assertEquals("jan", entryItems.first().entryWithPauses.entry.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `listItems excludes pending-delete entries`() = runTest {
        val entry1 = makeEntry("e1", LocalDate.of(2026, 2, 4))
        val entry2 = makeEntry("e2", LocalDate.of(2026, 2, 5))

        every { repository.getAllEntriesWithPauses() } returns flowOf(
            listOf(
                TrackingEntryWithPauses(entry1, emptyList()),
                TrackingEntryWithPauses(entry2, emptyList())
            )
        )
        viewModel = EntriesViewModel(repository)
        while (viewModel.selectedYearMonth.value != YearMonth.of(2026, 2)) {
            if (viewModel.selectedYearMonth.value.isBefore(YearMonth.of(2026, 2)))
                viewModel.nextMonth()
            else
                viewModel.previousMonth()
        }

        viewModel.swipeDelete(entry1) { /* no-op snackbar callback */ }

        viewModel.listItems.test {
            var items = awaitItem()
            if (items.isEmpty()) items = awaitItem()

            val entryItems = items.filterIsInstance<EntriesListItem.EntryItem>()
            assertEquals(1, entryItems.size)
            assertEquals("e2", entryItems.first().entryWithPauses.entry.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── confirmEntry ─────────────────────────────────────────────────────────

    @Test
    fun `confirmEntry calls repository updateEntry with confirmed = true`() = runTest {
        val entry = makeEntry("e1", LocalDate.now(), confirmed = false)
        coEvery { repository.updateEntry(any()) } returns Unit

        viewModel.confirmEntry(entry)
        testDispatcher.scheduler.advanceUntilIdle()

        val slot = slot<TrackingEntry>()
        coVerify { repository.updateEntry(capture(slot)) }
        assertTrue(slot.captured.confirmed)
        assertEquals("e1", slot.captured.id)
    }

    // ── swipeDelete ──────────────────────────────────────────────────────────

    @Test
    fun `swipeDelete triggers onShowSnackbar callback`() = runTest {
        val entry = makeEntry("e1", LocalDate.now())
        var callbackCalled = false

        viewModel.swipeDelete(entry) { callbackCalled = true }

        assertTrue(callbackCalled)
    }

    @Test
    fun `swipeDelete adds entry to pendingDeleteIds immediately`() = runTest {
        val entry = makeEntry("e1", LocalDate.now())
        every { repository.getAllEntriesWithPauses() } returns flowOf(
            listOf(TrackingEntryWithPauses(entry, emptyList()))
        )
        viewModel = EntriesViewModel(repository)
        while (viewModel.selectedYearMonth.value != YearMonth.from(entry.date)) {
            if (viewModel.selectedYearMonth.value.isBefore(YearMonth.from(entry.date)))
                viewModel.nextMonth()
            else
                viewModel.previousMonth()
        }

        viewModel.swipeDelete(entry) { }

        viewModel.listItems.test {
            val items = awaitItem()
            val entryItems = items.filterIsInstance<EntriesListItem.EntryItem>()
            assertTrue(entryItems.none { it.entryWithPauses.entry.id == "e1" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── undoDelete ───────────────────────────────────────────────────────────

    @Test
    fun `undoDelete removes entry from pendingDeleteIds before actual delete`() = runTest {
        val entry = makeEntry("e1", LocalDate.now())
        coEvery { repository.deleteEntry(any()) } returns Unit

        viewModel.swipeDelete(entry) { }
        viewModel.undoDelete(entry)

        // Advance past the 5-second delay
        advanceTimeBy(6_000)

        coVerify(exactly = 0) { repository.deleteEntry(any()) }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun makeEntry(
        id: String,
        date: LocalDate,
        confirmed: Boolean = false
    ) = TrackingEntry(
        id = id,
        date = date,
        type = TrackingType.MANUAL,
        startTime = date.atTime(8, 0),
        endTime = date.atTime(16, 0),
        autoDetected = false,
        confirmed = confirmed
    )
}
