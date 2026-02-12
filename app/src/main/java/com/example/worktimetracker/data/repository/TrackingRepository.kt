package com.example.worktimetracker.data.repository

import com.example.worktimetracker.data.local.dao.PauseDao
import com.example.worktimetracker.data.local.dao.TrackingDao
import com.example.worktimetracker.data.local.entity.Pause
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingRepository @Inject constructor(
    private val trackingDao: TrackingDao,
    private val pauseDao: PauseDao
) {
    fun getTodayEntries(): Flow<List<TrackingEntryWithPauses>> {
        val today = LocalDate.now()
        return getEntriesByDate(today)
    }

    fun getWeekEntries(weekStart: LocalDate): Flow<List<TrackingEntryWithPauses>> {
        val weekEnd = weekStart.plusDays(6)
        return getEntriesInRange(weekStart, weekEnd)
    }

    fun getEntriesInRange(start: LocalDate, end: LocalDate): Flow<List<TrackingEntryWithPauses>> {
        return trackingDao.getEntriesInRangeWithPauses(start, end)
    }

    private fun getEntriesByDate(date: LocalDate): Flow<List<TrackingEntryWithPauses>> {
        return trackingDao.getEntriesByDateWithPauses(date)
    }

    suspend fun getActiveEntry(): TrackingEntry? {
        return trackingDao.getActiveEntry()
    }

    suspend fun hasCompletedOfficeCommuteToday(): Boolean {
        return trackingDao.hasCompletedOfficeCommute(LocalDate.now())
    }

    suspend fun startTracking(type: TrackingType, autoDetected: Boolean): TrackingEntry {
        val entry = TrackingEntry(
            date = LocalDate.now(),
            type = type,
            startTime = LocalDateTime.now(),
            endTime = null,
            autoDetected = autoDetected,
            confirmed = false
        )
        trackingDao.insert(entry)
        return entry
    }

    suspend fun stopTracking(entryId: String) {
        val entry = trackingDao.getEntryById(entryId)
        if (entry != null && entry.endTime == null) {
            val updatedEntry = entry.copy(endTime = LocalDateTime.now())
            trackingDao.update(updatedEntry)
        }
    }

    suspend fun startPause(entryId: String): String {
        val pause = Pause(
            entryId = entryId,
            startTime = LocalDateTime.now(),
            endTime = null
        )
        pauseDao.insert(pause)
        return pause.id
    }

    suspend fun stopPause(entryId: String) {
        val activePause = pauseDao.getActivePause(entryId)
        if (activePause != null) {
            val updatedPause = activePause.copy(endTime = LocalDateTime.now())
            pauseDao.update(updatedPause)
        }
    }

    suspend fun updateEntry(entry: TrackingEntry) {
        trackingDao.update(entry)
    }

    suspend fun deleteEntry(entry: TrackingEntry) {
        trackingDao.delete(entry)
    }
}
