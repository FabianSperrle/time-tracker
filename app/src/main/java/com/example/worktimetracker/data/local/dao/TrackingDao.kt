package com.example.worktimetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TrackingDao {
    @Query("SELECT * FROM tracking_entries ORDER BY date DESC, startTime DESC")
    fun getAllEntries(): Flow<List<TrackingEntry>>

    @Transaction
    @Query("SELECT * FROM tracking_entries ORDER BY date DESC, startTime DESC")
    fun getAllEntriesWithPauses(): Flow<List<TrackingEntryWithPauses>>

    @Transaction
    @Query("SELECT * FROM tracking_entries WHERE date = :date")
    fun getEntriesByDateWithPauses(date: LocalDate): Flow<List<TrackingEntryWithPauses>>

    @Transaction
    @Query("SELECT * FROM tracking_entries WHERE date BETWEEN :start AND :end ORDER BY date")
    fun getEntriesInRangeWithPauses(start: LocalDate, end: LocalDate): Flow<List<TrackingEntryWithPauses>>

    @Query("SELECT * FROM tracking_entries WHERE date = :date")
    fun getEntriesByDate(date: LocalDate): Flow<List<TrackingEntry>>

    @Query("SELECT * FROM tracking_entries WHERE date BETWEEN :start AND :end ORDER BY date")
    fun getEntriesInRange(start: LocalDate, end: LocalDate): Flow<List<TrackingEntry>>

    @Query("SELECT * FROM tracking_entries WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveEntry(): TrackingEntry?

    @Query("SELECT * FROM tracking_entries WHERE id = :id")
    suspend fun getEntryById(id: String): TrackingEntry?

    @Query("SELECT EXISTS(SELECT 1 FROM tracking_entries WHERE date = :date AND type = 'COMMUTE_OFFICE' AND endTime IS NOT NULL)")
    suspend fun hasCompletedOfficeCommute(date: LocalDate): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TrackingEntry)

    @Update
    suspend fun update(entry: TrackingEntry)

    @Delete
    suspend fun delete(entry: TrackingEntry)
}
