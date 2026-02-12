package com.example.worktimetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.worktimetracker.data.local.entity.Pause
import kotlinx.coroutines.flow.Flow

@Dao
interface PauseDao {
    @Query("SELECT * FROM pauses WHERE entryId = :entryId")
    fun getPausesForEntry(entryId: String): Flow<List<Pause>>

    @Query("SELECT * FROM pauses WHERE entryId = :entryId AND endTime IS NULL LIMIT 1")
    suspend fun getActivePause(entryId: String): Pause?

    @Insert
    suspend fun insert(pause: Pause)

    @Update
    suspend fun update(pause: Pause)

    @Delete
    suspend fun delete(pause: Pause)
}
