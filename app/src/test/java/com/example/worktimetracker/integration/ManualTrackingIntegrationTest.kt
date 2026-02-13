package com.example.worktimetracker.integration

import app.cash.turbine.test
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.domain.tracking.TrackingEvent
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import com.example.worktimetracker.data.local.dao.PauseDao
import com.example.worktimetracker.data.local.dao.TrackingDao
import com.example.worktimetracker.data.repository.TrackingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.Pause
import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.commute.CommuteDayChecker
import com.example.worktimetracker.domain.commute.CommutePhaseTracker
import com.example.worktimetracker.domain.tracking.TrackingStateStorage
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Integration test for manual tracking flow.
 * Tests the interaction between TrackingStateMachine and TrackingRepository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManualTrackingIntegrationTest {

    private lateinit var trackingDao: TrackingDao
    private lateinit var pauseDao: PauseDao
    private lateinit var repository: TrackingRepository
    private lateinit var settingsProvider: SettingsProvider
    private lateinit var stateStorage: TrackingStateStorage
    private lateinit var stateMachine: TrackingStateMachine

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
    fun `full manual tracking flow - start, pause, resume, stop`() = runTest {
        var capturedEntryId: String? = null
        var capturedEntry: TrackingEntry? = null

        // Capture the entry when it's inserted
        coEvery { trackingDao.insert(any()) } answers {
            capturedEntry = firstArg()
            capturedEntryId = capturedEntry!!.id
            Unit
        }

        // Return the captured entry when queried
        coEvery { trackingDao.getEntryById(any()) } answers {
            val requestedId = firstArg<String>()
            if (requestedId == capturedEntryId) capturedEntry else null
        }
        coEvery { trackingDao.getActiveEntry() } answers { capturedEntry }

        // Mock pause operations
        var capturedPause: Pause? = null
        coEvery { pauseDao.insert(any()) } answers {
            capturedPause = firstArg()
            Unit
        }
        coEvery { pauseDao.getActivePause(any()) } answers { capturedPause }
        coEvery { pauseDao.update(any()) } answers {
            capturedPause = firstArg()
            Unit
        }
        coEvery { trackingDao.update(any()) } answers {
            capturedEntry = firstArg()
            Unit
        }

        stateMachine.state.test {
            // Initial state should be Idle
            assertEquals(TrackingState.Idle, awaitItem())

            // 1. Start manual tracking
            stateMachine.processEvent(TrackingEvent.ManualStart(TrackingType.MANUAL))
            val trackingState = awaitItem()
            assertTrue(trackingState is TrackingState.Tracking)
            assertEquals(TrackingType.MANUAL, (trackingState as TrackingState.Tracking).type)
            assertNotNull(capturedEntry)
            assertEquals(false, capturedEntry!!.autoDetected)

            // Verify entry was created
            coVerify { trackingDao.insert(match { it.type == TrackingType.MANUAL && !it.autoDetected }) }

            val activeEntryId = trackingState.entryId

            // 2. Pause tracking
            stateMachine.processEvent(TrackingEvent.PauseStart)
            val pausedState = awaitItem()
            assertTrue(pausedState is TrackingState.Paused)
            assertEquals(activeEntryId, (pausedState as TrackingState.Paused).entryId)

            // Verify pause was created
            coVerify { pauseDao.insert(match { it.entryId == activeEntryId && it.endTime == null }) }

            // 3. Resume tracking
            stateMachine.processEvent(TrackingEvent.PauseEnd)
            val resumedState = awaitItem()
            assertTrue(resumedState is TrackingState.Tracking)

            // Verify pause was closed
            coVerify { pauseDao.update(match { it.entryId == activeEntryId && it.endTime != null }) }

            // 4. Stop tracking
            stateMachine.processEvent(TrackingEvent.ManualStop)
            val idleState = awaitItem()
            assertEquals(TrackingState.Idle, idleState)

            // Verify entry was closed
            coVerify { trackingDao.update(match { it.id == activeEntryId && it.endTime != null }) }
        }
    }

    @Test
    fun `manual start with HOME_OFFICE type creates correct entry`() = runTest {
        coEvery { trackingDao.insert(any()) } returns Unit

        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            stateMachine.processEvent(TrackingEvent.ManualStart(TrackingType.HOME_OFFICE))
            val trackingState = awaitItem() as TrackingState.Tracking

            assertEquals(TrackingType.HOME_OFFICE, trackingState.type)
            coVerify { trackingDao.insert(match { it.type == TrackingType.HOME_OFFICE && !it.autoDetected }) }
        }
    }

    @Test
    fun `stop while paused closes both pause and entry`() = runTest {
        var capturedEntry: TrackingEntry? = null
        var capturedPause: Pause? = null

        coEvery { trackingDao.insert(any()) } answers {
            capturedEntry = firstArg()
            Unit
        }
        coEvery { trackingDao.getEntryById(any()) } answers { capturedEntry }
        coEvery { trackingDao.getActiveEntry() } answers { capturedEntry }
        coEvery { trackingDao.update(any()) } answers {
            capturedEntry = firstArg()
            Unit
        }

        coEvery { pauseDao.insert(any()) } answers {
            capturedPause = firstArg()
            Unit
        }
        coEvery { pauseDao.getActivePause(any()) } answers { capturedPause }
        coEvery { pauseDao.update(any()) } answers {
            capturedPause = firstArg()
            Unit
        }

        stateMachine.state.test {
            assertEquals(TrackingState.Idle, awaitItem())

            // Start tracking
            stateMachine.processEvent(TrackingEvent.ManualStart(TrackingType.MANUAL))
            awaitItem() // Tracking state

            // Pause
            stateMachine.processEvent(TrackingEvent.PauseStart)
            awaitItem() // Paused state

            // Stop while paused
            stateMachine.processEvent(TrackingEvent.ManualStop)
            val finalState = awaitItem()
            assertEquals(TrackingState.Idle, finalState)

            // Verify both pause and entry were closed
            coVerify { pauseDao.update(match { it.endTime != null }) }
            coVerify { trackingDao.update(match { it.endTime != null }) }
        }
    }
}
