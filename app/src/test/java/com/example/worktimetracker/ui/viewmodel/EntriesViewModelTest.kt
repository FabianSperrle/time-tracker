package com.example.worktimetracker.ui.viewmodel

import app.cash.turbine.test
import com.example.worktimetracker.data.local.entity.Pause
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.repository.TrackingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class EntriesViewModelTest {

    private lateinit var repository: TrackingRepository
    private lateinit var viewModel: EntriesViewModel
    private val testDispatcher = StandardTestDispatcher()

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

        every { repository.getAllEntriesWithPauses() } returns flowOf(
            listOf(
                TrackingEntryWithPauses(entry1, listOf(pause)),
                TrackingEntryWithPauses(entry2, emptyList())
            )
        )

        viewModel.entries.test {
            val entries = awaitItem()
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
}
