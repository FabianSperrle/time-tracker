package com.example.worktimetracker.integration

import app.cash.turbine.test
import com.example.worktimetracker.data.local.dao.PauseDao
import com.example.worktimetracker.data.local.dao.TrackingDao
import com.example.worktimetracker.data.local.entity.Pause
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.repository.TrackingRepository
import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.commute.CommuteDayChecker
import com.example.worktimetracker.domain.commute.CommutePhaseTracker
import com.example.worktimetracker.domain.tracking.TrackingEvent
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import com.example.worktimetracker.domain.tracking.TrackingStateStorage
import com.example.worktimetracker.ui.viewmodel.EntriesViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Integration test verifying that manually tracked entries appear in the entries list.
 * This test reproduces Issue #3.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManualTrackingEntriesListTest {

    private lateinit var trackingDao: TrackingDao
    private lateinit var pauseDao: PauseDao
    private lateinit var repository: TrackingRepository
    private lateinit var settingsProvider: SettingsProvider
    private lateinit var stateStorage: TrackingStateStorage
    private lateinit var stateMachine: TrackingStateMachine
    private lateinit var entriesViewModel: EntriesViewModel

    @BeforeEach
    fun setUp() {
        trackingDao = mockk(relaxed = true)
        pauseDao = mockk(relaxed = true)
        repository = TrackingRepository(trackingDao, pauseDao)
        settingsProvider = mockk(relaxed = true)
        stateStorage = mockk(relaxed = true)

        coEvery { stateStorage.loadState() } returns TrackingState.Idle
        coEvery { stateStorage.saveState(any()) } returns Unit

        stateMachine = TrackingStateMachine(
            repository, settingsProvider, stateStorage,
            CommutePhaseTracker(), CommuteDayChecker(settingsProvider)
        )
    }

    @Test
    fun `manual tracking entries appear in entries list after stop`() = runTest {
        val completedEntries = mutableListOf<TrackingEntry>()
        var activeEntry: TrackingEntry? = null

        // Mock: capture inserted entries
        coEvery { trackingDao.insert(any()) } answers {
            activeEntry = firstArg()
            Unit
        }

        // Mock: capture updated entries
        coEvery { trackingDao.update(any()) } answers {
            val updated = firstArg<TrackingEntry>()
            activeEntry = updated
            if (updated.endTime != null) {
                completedEntries.add(updated)
            }
            Unit
        }

        // Mock: getEntryById returns active entry
        coEvery { trackingDao.getEntryById(any()) } answers { activeEntry }

        // Mock: getActiveEntry returns entry without endTime
        coEvery { trackingDao.getActiveEntry() } answers {
            activeEntry?.takeIf { it.endTime == null }
        }

        // Mock: getAllEntriesWithPauses returns only completed entries (endTime != null)
        every { trackingDao.getAllEntriesWithPauses() } answers {
            flowOf(completedEntries
                .filter { it.endTime != null }
                .map { TrackingEntryWithPauses(it, emptyList()) })
        }

        // Create EntriesViewModel
        entriesViewModel = EntriesViewModel(repository)

        // Step 1: Verify entries list is empty initially
        entriesViewModel.entries.test {
            val initial = awaitItem()
            assertEquals(0, initial.size, "Entries list should be empty initially")

            // Step 2: Start manual tracking
            stateMachine.processEvent(TrackingEvent.ManualStart(TrackingType.MANUAL))

            // Step 3: Verify entry was created but not yet in completed list (endTime = null)
            assertNotNull(activeEntry, "Active entry should be created")
            assertEquals(TrackingType.MANUAL, activeEntry?.type)
            assertEquals(null, activeEntry?.endTime, "Active entry should not have endTime")
            assertEquals(0, completedEntries.size, "No completed entries yet")

            // Step 4: Stop manual tracking
            stateMachine.processEvent(TrackingEvent.ManualStop)

            // Step 5: Verify entry was updated with endTime
            assertNotNull(activeEntry?.endTime, "Entry should have endTime after stop")
            assertEquals(1, completedEntries.size, "One completed entry should exist")

            // Step 6: Verify entry appears in entries list
            val entriesAfterStop = awaitItem()
            assertEquals(1, entriesAfterStop.size, "One entry should appear in entries list")
            assertEquals(TrackingType.MANUAL, entriesAfterStop[0].entry.type)
            assertNotNull(entriesAfterStop[0].entry.endTime, "Displayed entry should have endTime")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple manual tracking sessions all appear in entries list`() = runTest {
        val allEntries = mutableListOf<TrackingEntry>()

        coEvery { trackingDao.insert(any()) } answers {
            val entry = firstArg<TrackingEntry>()
            Unit
        }

        coEvery { trackingDao.update(any()) } answers {
            val entry = firstArg<TrackingEntry>()
            if (entry.endTime != null) {
                allEntries.add(entry)
            }
            Unit
        }

        coEvery { trackingDao.getEntryById(any()) } answers {
            val id = firstArg<String>()
            allEntries.find { it.id == id }
        }

        coEvery { trackingDao.getActiveEntry() } returns null

        every { trackingDao.getAllEntriesWithPauses() } answers {
            flowOf(allEntries
                .filter { it.endTime != null }
                .map { TrackingEntryWithPauses(it, emptyList()) })
        }

        entriesViewModel = EntriesViewModel(repository)

        entriesViewModel.entries.test {
            assertEquals(0, awaitItem().size, "Initially empty")

            // Create 3 manual tracking sessions
            for (i in 1..3) {
                val entry = TrackingEntry(
                    id = "entry-$i",
                    date = LocalDate.now(),
                    type = TrackingType.MANUAL,
                    startTime = LocalDateTime.now().minusHours(i.toLong()),
                    endTime = LocalDateTime.now().minusHours(i.toLong()).plusHours(1),
                    autoDetected = false,
                    confirmed = false
                )
                repository.updateEntry(entry)
            }

            // Verify all 3 entries appear
            val entries = awaitItem()
            assertEquals(3, entries.size, "All 3 manual entries should appear")
            entries.forEach {
                assertEquals(TrackingType.MANUAL, it.entry.type)
                assertEquals(false, it.entry.autoDetected)
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `manual tracking entries with MANUAL type are not filtered out`() = runTest {
        val manualEntry = TrackingEntry(
            id = "manual-1",
            date = LocalDate.now(),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.now().minusHours(2),
            endTime = LocalDateTime.now(),
            autoDetected = false,
            confirmed = false
        )

        val homeOfficeEntry = TrackingEntry(
            id = "home-1",
            date = LocalDate.now().minusDays(1),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.now().minusDays(1).minusHours(8),
            endTime = LocalDateTime.now().minusDays(1),
            autoDetected = true,
            confirmed = true
        )

        val commuteEntry = TrackingEntry(
            id = "commute-1",
            date = LocalDate.now().minusDays(2),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.now().minusDays(2).minusHours(8),
            endTime = LocalDateTime.now().minusDays(2),
            autoDetected = true,
            confirmed = true
        )

        every { trackingDao.getAllEntriesWithPauses() } returns flowOf(
            listOf(
                TrackingEntryWithPauses(manualEntry, emptyList()),
                TrackingEntryWithPauses(homeOfficeEntry, emptyList()),
                TrackingEntryWithPauses(commuteEntry, emptyList())
            )
        )

        entriesViewModel = EntriesViewModel(repository)

        entriesViewModel.entries.test {
            val entries = awaitItem()
            assertEquals(3, entries.size, "All three entry types should appear")

            val manual = entries.find { it.entry.type == TrackingType.MANUAL }
            val home = entries.find { it.entry.type == TrackingType.HOME_OFFICE }
            val commute = entries.find { it.entry.type == TrackingType.COMMUTE_OFFICE }

            assertNotNull(manual, "MANUAL entry should be present")
            assertNotNull(home, "HOME_OFFICE entry should be present")
            assertNotNull(commute, "COMMUTE_OFFICE entry should be present")

            cancelAndIgnoreRemainingEvents()
        }
    }
}
