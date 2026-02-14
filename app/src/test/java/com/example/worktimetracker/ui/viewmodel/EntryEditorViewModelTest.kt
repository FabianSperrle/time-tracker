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
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class EntryEditorViewModelTest {

    private lateinit var repository: TrackingRepository
    private lateinit var viewModel: EntryEditorViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @Test
    fun `loadEntry loads existing entry with pauses`() = runTest {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 10),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 16, 30),
            autoDetected = true,
            confirmed = false,
            notes = "Test note"
        )
        val pause = Pause(
            id = "p1",
            entryId = "1",
            startTime = LocalDateTime.of(2026, 2, 10, 12, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 12, 30)
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, listOf(pause))

        every { repository.getEntryWithPausesById("1") } returns flowOf(entryWithPauses)

        viewModel = EntryEditorViewModel(repository, "1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.editorState.test {
            val state = awaitItem()
            assertEquals(LocalDate.of(2026, 2, 10), state.date)
            assertEquals(TrackingType.COMMUTE_OFFICE, state.type)
            assertEquals(LocalTime.of(8, 0), state.startTime)
            assertEquals(LocalTime.of(16, 30), state.endTime)
            assertEquals("Test note", state.notes)
            assertEquals(1, state.pauses.size)
            assertFalse(state.confirmed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `new entry initializes with current date and empty fields`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.editorState.test {
            val state = awaitItem()
            assertEquals(LocalDate.now(), state.date)
            assertEquals(TrackingType.MANUAL, state.type)
            assertNull(state.startTime)
            assertNull(state.endTime)
            assertEquals("", state.notes)
            assertEquals(0, state.pauses.size)
            assertFalse(state.confirmed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateDate changes date in state`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        val newDate = LocalDate.of(2026, 2, 15)
        viewModel.updateDate(newDate)

        viewModel.editorState.test {
            val state = awaitItem()
            assertEquals(newDate, state.date)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateType changes type in state`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateType(TrackingType.HOME_OFFICE)

        viewModel.editorState.test {
            val state = awaitItem()
            assertEquals(TrackingType.HOME_OFFICE, state.type)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateStartTime changes start time in state`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        val newTime = LocalTime.of(9, 30)
        viewModel.updateStartTime(newTime)

        viewModel.editorState.test {
            val state = awaitItem()
            assertEquals(newTime, state.startTime)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateEndTime changes end time in state`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        val newTime = LocalTime.of(17, 0)
        viewModel.updateEndTime(newTime)

        viewModel.editorState.test {
            val state = awaitItem()
            assertEquals(newTime, state.endTime)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateNotes changes notes in state`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateNotes("New note")

        viewModel.editorState.test {
            val state = awaitItem()
            assertEquals("New note", state.notes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleConfirmed toggles confirmed status`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleConfirmed()

        viewModel.editorState.test {
            val state = awaitItem()
            assertTrue(state.confirmed)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.toggleConfirmed()

        viewModel.editorState.test {
            val state = awaitItem()
            assertFalse(state.confirmed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `validation fails when startTime is after endTime`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateStartTime(LocalTime.of(17, 0))
        viewModel.updateEndTime(LocalTime.of(9, 0))

        viewModel.validationErrors.test {
            val errors = awaitItem()
            assertTrue(errors.contains("Startzeit muss vor Endzeit liegen"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `validation fails when startTime or endTime is missing`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.validationErrors.test {
            val errors = awaitItem()
            assertTrue(errors.contains("Startzeit ist erforderlich"))
            assertTrue(errors.contains("Endzeit ist erforderlich"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `validation warns when net duration exceeds 12 hours`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateStartTime(LocalTime.of(7, 0))
        viewModel.updateEndTime(LocalTime.of(22, 0))

        viewModel.validationErrors.test {
            val errors = awaitItem()
            assertTrue(errors.any { it.contains("Ungewöhnlich langer Tag (>12h)") })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `validation passes with valid times`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateStartTime(LocalTime.of(9, 0))
        viewModel.updateEndTime(LocalTime.of(17, 0))

        viewModel.validationErrors.test {
            val errors = awaitItem()
            assertEquals(0, errors.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addPause adds new pause to state`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        val pauseStart = LocalTime.of(12, 0)
        val pauseEnd = LocalTime.of(12, 30)
        viewModel.addPause(pauseStart, pauseEnd)

        viewModel.editorState.test {
            val state = awaitItem()
            assertEquals(1, state.pauses.size)
            assertEquals(pauseStart, state.pauses[0].startTime)
            assertEquals(pauseEnd, state.pauses[0].endTime)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removePause removes pause from state`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addPause(LocalTime.of(12, 0), LocalTime.of(12, 30))

        viewModel.editorState.test {
            val state = awaitItem()
            val pauseId = state.pauses[0].id
            viewModel.removePause(pauseId)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.editorState.test {
            val state = awaitItem()
            assertEquals(0, state.pauses.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `validation fails when pause is outside entry time range`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateStartTime(LocalTime.of(9, 0))
        viewModel.updateEndTime(LocalTime.of(17, 0))
        viewModel.addPause(LocalTime.of(8, 0), LocalTime.of(8, 30))

        viewModel.validationErrors.test {
            val errors = awaitItem()
            assertTrue(errors.any { it.contains("außerhalb des Zeitraums") })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `validation fails when pauses overlap`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateStartTime(LocalTime.of(9, 0))
        viewModel.updateEndTime(LocalTime.of(17, 0))
        viewModel.addPause(LocalTime.of(12, 0), LocalTime.of(12, 45))
        viewModel.addPause(LocalTime.of(12, 30), LocalTime.of(13, 0))

        viewModel.validationErrors.test {
            val errors = awaitItem()
            assertTrue(errors.any { it.contains("überlappen") })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveEntry creates new entry when entryId is null`() = runTest {
        coEvery { repository.createEntry(any(), any(), any(), any(), any()) } returns "new-id"

        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDate(LocalDate.of(2026, 2, 15))
        viewModel.updateType(TrackingType.MANUAL)
        viewModel.updateStartTime(LocalTime.of(9, 0))
        viewModel.updateEndTime(LocalTime.of(17, 0))
        viewModel.updateNotes("Manual entry")

        val result = viewModel.saveEntry()

        assertTrue(result)
        coVerify {
            repository.createEntry(
                date = LocalDate.of(2026, 2, 15),
                type = TrackingType.MANUAL,
                startTime = LocalDateTime.of(2026, 2, 15, 9, 0),
                endTime = LocalDateTime.of(2026, 2, 15, 17, 0),
                notes = "Manual entry"
            )
        }
    }

    @Test
    fun `saveEntry updates existing entry when entryId is not null`() = runTest {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 10),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 16, 30),
            autoDetected = true,
            confirmed = false
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, emptyList())

        every { repository.getEntryWithPausesById("1") } returns flowOf(entryWithPauses)
        coEvery { repository.updateEntry(any()) } returns Unit

        viewModel = EntryEditorViewModel(repository, "1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateStartTime(LocalTime.of(8, 30))
        viewModel.updateNotes("Updated note")
        viewModel.toggleConfirmed()

        val result = viewModel.saveEntry()

        assertTrue(result)
        coVerify {
            repository.updateEntry(match {
                it.id == "1" &&
                it.startTime == LocalDateTime.of(2026, 2, 10, 8, 30) &&
                it.notes == "Updated note" &&
                it.confirmed
            })
        }
    }

    @Test
    fun `saveEntry fails when validation errors exist`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.saveEntry()

        assertFalse(result)
        coVerify(exactly = 0) { repository.createEntry(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.updateEntry(any()) }
    }

    @Test
    fun `saveEntry saves pauses for new entry`() = runTest {
        coEvery { repository.createEntry(any(), any(), any(), any(), any()) } returns "new-id"
        coEvery { repository.addPause(any(), any(), any()) } returns "pause-id"

        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDate(LocalDate.of(2026, 2, 15))
        viewModel.updateStartTime(LocalTime.of(9, 0))
        viewModel.updateEndTime(LocalTime.of(17, 0))
        viewModel.addPause(LocalTime.of(12, 0), LocalTime.of(12, 30))

        viewModel.saveEntry()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.addPause(
                entryId = "new-id",
                startTime = LocalDateTime.of(2026, 2, 15, 12, 0),
                endTime = LocalDateTime.of(2026, 2, 15, 12, 30)
            )
        }
    }

    @Test
    fun `netDuration calculates correctly without pauses`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateStartTime(LocalTime.of(9, 0))
        viewModel.updateEndTime(LocalTime.of(17, 0))

        viewModel.editorState.test {
            val state = awaitItem()
            assertNotNull(state.netDuration)
            assertEquals(480, state.netDuration?.toMinutes())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `netDuration calculates correctly with pauses`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateStartTime(LocalTime.of(9, 0))
        viewModel.updateEndTime(LocalTime.of(17, 0))
        viewModel.addPause(LocalTime.of(12, 0), LocalTime.of(12, 30))

        viewModel.editorState.test {
            val state = awaitItem()
            assertNotNull(state.netDuration)
            assertEquals(450, state.netDuration?.toMinutes())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `validation fails when pause start is after or equal to pause end`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateStartTime(LocalTime.of(9, 0))
        viewModel.updateEndTime(LocalTime.of(17, 0))
        viewModel.addPause(LocalTime.of(13, 0), LocalTime.of(12, 0)) // Invalid: end before start

        viewModel.validationErrors.test {
            val errors = awaitItem()
            assertTrue(errors.any { it.contains("Start muss vor Ende liegen") })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveEntry deletes removed pauses from database`() = runTest {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 10),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 16, 30),
            autoDetected = true,
            confirmed = false
        )
        val pause1 = Pause(
            id = "p1",
            entryId = "1",
            startTime = LocalDateTime.of(2026, 2, 10, 12, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 12, 30)
        )
        val pause2 = Pause(
            id = "p2",
            entryId = "1",
            startTime = LocalDateTime.of(2026, 2, 10, 14, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 14, 15)
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, listOf(pause1, pause2))

        every { repository.getEntryWithPausesById("1") } returns flowOf(entryWithPauses)
        coEvery { repository.updateEntry(any()) } returns Unit
        coEvery { repository.deletePause(any()) } returns Unit

        viewModel = EntryEditorViewModel(repository, "1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Get the pause IDs from state
        val pauseToRemove = viewModel.editorState.value.pauses.first { it.pauseEntity?.id == "p1" }
        viewModel.removePause(pauseToRemove.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.saveEntry()

        assertTrue(result)
        coVerify { repository.deletePause(pause1) }
        coVerify(exactly = 0) { repository.deletePause(pause2) }
    }

    @Test
    fun `saveEntry completes before returning true`() = runTest {
        coEvery { repository.createEntry(any(), any(), any(), any(), any()) } returns "new-id"

        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateStartTime(LocalTime.of(9, 0))
        viewModel.updateEndTime(LocalTime.of(17, 0))

        val result = viewModel.saveEntry()

        // If saveEntry is truly synchronous (suspend), repository call should be complete
        assertTrue(result)
        coVerify { repository.createEntry(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `validation fails when pause has equal start and end times`() = runTest {
        viewModel = EntryEditorViewModel(repository, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateStartTime(LocalTime.of(9, 0))
        viewModel.updateEndTime(LocalTime.of(17, 0))
        viewModel.addPause(LocalTime.of(12, 0), LocalTime.of(12, 0)) // Invalid: same time

        viewModel.validationErrors.test {
            val errors = awaitItem()
            assertTrue(errors.any { it.contains("Start muss vor Ende liegen") })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
