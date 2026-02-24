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

    suspend fun hasAnyTrackingToday(): Boolean {
        return trackingDao.hasAnyEntryForDate(LocalDate.now())
    }

    suspend fun startTracking(
        type: TrackingType,
        autoDetected: Boolean,
        notes: String? = null
    ): TrackingEntry {
        val entry = TrackingEntry(
            date = LocalDate.now(),
            type = type,
            startTime = LocalDateTime.now(),
            endTime = null,
            autoDetected = autoDetected,
            confirmed = false,
            notes = notes
        )
        trackingDao.insert(entry)
        return entry
    }

    suspend fun startTrackingAt(
        type: TrackingType,
        autoDetected: Boolean,
        startTime: LocalDateTime,
        date: LocalDate
    ): TrackingEntry {
        val entry = TrackingEntry(
            date = date,
            type = type,
            startTime = startTime,
            endTime = null,
            autoDetected = autoDetected,
            confirmed = false
        )
        trackingDao.insert(entry)
        return entry
    }

    suspend fun stopTracking(entryId: String, endTime: LocalDateTime = LocalDateTime.now()) {
        val entry = trackingDao.getEntryById(entryId)
        if (entry != null && entry.endTime == null) {
            val updatedEntry = entry.copy(endTime = endTime)
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

    fun getAllEntriesWithPauses(): Flow<List<TrackingEntryWithPauses>> {
        return trackingDao.getAllEntriesWithPauses()
    }

    fun getEntryWithPausesById(entryId: String): Flow<TrackingEntryWithPauses?> {
        return kotlinx.coroutines.flow.combine(
            kotlinx.coroutines.flow.flow {
                val entry = trackingDao.getEntryById(entryId)
                emit(entry)
            },
            pauseDao.getPausesForEntry(entryId)
        ) { entry, pauses ->
            entry?.let { TrackingEntryWithPauses(it, pauses) }
        }
    }

    suspend fun createEntry(
        date: LocalDate,
        type: TrackingType,
        startTime: LocalDateTime,
        endTime: LocalDateTime?,
        notes: String? = null
    ): String {
        val entry = TrackingEntry(
            date = date,
            type = type,
            startTime = startTime,
            endTime = endTime,
            autoDetected = false,
            confirmed = false,
            notes = notes
        )
        trackingDao.insert(entry)
        return entry.id
    }

    suspend fun addPause(
        entryId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime?
    ): String {
        val pause = Pause(
            entryId = entryId,
            startTime = startTime,
            endTime = endTime
        )
        pauseDao.insert(pause)
        return pause.id
    }

    suspend fun updatePause(pause: Pause) {
        pauseDao.update(pause)
    }

    suspend fun deletePause(pause: Pause) {
        pauseDao.delete(pause)
    }
}
