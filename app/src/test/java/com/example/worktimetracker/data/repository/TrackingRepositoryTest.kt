package com.example.worktimetracker.data.repository

import app.cash.turbine.test
import com.example.worktimetracker.data.local.dao.PauseDao
import com.example.worktimetracker.data.local.dao.TrackingDao
import com.example.worktimetracker.data.local.entity.Pause
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TrackingRepositoryTest {

    private lateinit var trackingDao: TrackingDao
    private lateinit var pauseDao: PauseDao
    private lateinit var repository: TrackingRepository

    @BeforeEach
    fun setup() {
        trackingDao = mockk(relaxed = true)
        pauseDao = mockk(relaxed = true)
        repository = TrackingRepository(trackingDao, pauseDao)
    }

    @Test
    fun `getTodayEntries returns entries with pauses for today`() = runTest {
        val today = LocalDate.now()
        val entry = TrackingEntry(
            id = "1",
            date = today,
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.now().minusHours(2),
            endTime = LocalDateTime.now(),
            autoDetected = true
        )
        val pause = Pause(
            id = "p1",
            entryId = "1",
            startTime = LocalDateTime.now().minusHours(1),
            endTime = LocalDateTime.now().minusMinutes(30)
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, listOf(pause))

        every { trackingDao.getEntriesByDateWithPauses(today) } returns flowOf(listOf(entryWithPauses))

        repository.getTodayEntries().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("1", result[0].entry.id)
            assertEquals(1, result[0].pauses.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getWeekEntries returns entries for 7 days starting from weekStart`() = runTest {
        val weekStart = LocalDate.now().minusDays(3)
        val entry = TrackingEntry(
            id = "1",
            date = weekStart,
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.now().minusDays(3),
            endTime = LocalDateTime.now().minusDays(3).plusHours(8),
            autoDetected = false
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, emptyList())

        every {
            trackingDao.getEntriesInRangeWithPauses(weekStart, weekStart.plusDays(6))
        } returns flowOf(listOf(entryWithPauses))

        repository.getWeekEntries(weekStart).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("1", result[0].entry.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getActiveEntry returns null when no active entry exists`() = runTest {
        coEvery { trackingDao.getActiveEntry() } returns null

        val result = repository.getActiveEntry()

        assertNull(result)
    }

    @Test
    fun `getActiveEntry returns active entry when exists`() = runTest {
        val activeEntry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.now(),
            endTime = null,
            autoDetected = true
        )
        coEvery { trackingDao.getActiveEntry() } returns activeEntry

        val result = repository.getActiveEntry()

        assertNotNull(result)
        assertEquals("1", result?.id)
        assertNull(result?.endTime)
    }

    @Test
    fun `startTracking creates new entry with current time`() = runTest {
        val entrySlot = slot<TrackingEntry>()
        coEvery { trackingDao.insert(capture(entrySlot)) } returns Unit

        val result = repository.startTracking(TrackingType.COMMUTE_OFFICE, autoDetected = true)

        assertNotNull(result.id)
        assertEquals(LocalDate.now(), result.date)
        assertEquals(TrackingType.COMMUTE_OFFICE, result.type)
        assertNull(result.endTime)
        assertTrue(result.autoDetected)
        assertFalse(result.confirmed)
        coVerify { trackingDao.insert(any()) }
    }

    @Test
    fun `stopTracking updates entry with endTime using default now`() = runTest {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.now().minusHours(2),
            endTime = null,
            autoDetected = false
        )
        coEvery { trackingDao.getEntryById("1") } returns entry
        coEvery { trackingDao.update(any()) } returns Unit

        repository.stopTracking("1")

        coVerify {
            trackingDao.update(match {
                it.id == "1" && it.endTime != null
            })
        }
    }

    @Test
    fun `stopTracking uses explicit endTime when provided`() = runTest {
        val explicitEndTime = LocalDateTime.of(2026, 2, 9, 17, 23)
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.now().minusHours(8),
            endTime = null,
            autoDetected = true
        )
        coEvery { trackingDao.getEntryById("1") } returns entry
        coEvery { trackingDao.update(any()) } returns Unit

        repository.stopTracking("1", explicitEndTime)

        coVerify {
            trackingDao.update(match {
                it.id == "1" && it.endTime == explicitEndTime
            })
        }
    }

    @Test
    fun `startPause creates new pause for entry and returns pause ID`() = runTest {
        val pauseSlot = slot<Pause>()
        coEvery { pauseDao.insert(capture(pauseSlot)) } returns Unit

        val pauseId = repository.startPause("entry-1")

        val captured = pauseSlot.captured
        assertEquals("entry-1", captured.entryId)
        assertNull(captured.endTime)
        assertEquals(captured.id, pauseId)
        coVerify { pauseDao.insert(any()) }
    }

    @Test
    fun `stopPause updates active pause with endTime`() = runTest {
        val activePause = Pause(
            id = "p1",
            entryId = "entry-1",
            startTime = LocalDateTime.now().minusMinutes(15),
            endTime = null
        )
        coEvery { pauseDao.getActivePause("entry-1") } returns activePause
        coEvery { pauseDao.update(any()) } returns Unit

        repository.stopPause("entry-1")

        coVerify {
            pauseDao.update(match {
                it.id == "p1" && it.endTime != null
            })
        }
    }

    @Test
    fun `updateEntry delegates to dao`() = runTest {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.now(),
            endTime = LocalDateTime.now().plusHours(8),
            autoDetected = false,
            confirmed = true
        )
        coEvery { trackingDao.update(entry) } returns Unit

        repository.updateEntry(entry)

        coVerify { trackingDao.update(entry) }
    }

    @Test
    fun `deleteEntry delegates to dao`() = runTest {
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.now(),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.now(),
            endTime = LocalDateTime.now().plusHours(8),
            autoDetected = true
        )
        coEvery { trackingDao.delete(entry) } returns Unit

        repository.deleteEntry(entry)

        coVerify { trackingDao.delete(entry) }
    }

    @Test
    fun `hasCompletedOfficeCommuteToday returns true when completed commute exists`() = runTest {
        coEvery { trackingDao.hasCompletedOfficeCommute(LocalDate.now()) } returns true

        val result = repository.hasCompletedOfficeCommuteToday()

        assertTrue(result)
        coVerify { trackingDao.hasCompletedOfficeCommute(LocalDate.now()) }
    }

    @Test
    fun `hasCompletedOfficeCommuteToday returns false when no completed commute exists`() = runTest {
        coEvery { trackingDao.hasCompletedOfficeCommute(LocalDate.now()) } returns false

        val result = repository.hasCompletedOfficeCommuteToday()

        assertFalse(result)
        coVerify { trackingDao.hasCompletedOfficeCommute(LocalDate.now()) }
    }
}
