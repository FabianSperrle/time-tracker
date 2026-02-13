package com.example.worktimetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.worktimetracker.data.local.entity.GeofenceZone
import com.example.worktimetracker.data.local.entity.ZoneType
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceDao {
    @Query("SELECT * FROM geofence_zones")
    fun getAllZones(): Flow<List<GeofenceZone>>

    @Query("SELECT * FROM geofence_zones")
    suspend fun getAllZonesOnce(): List<GeofenceZone>

    @Query("SELECT * FROM geofence_zones WHERE id = :id")
    suspend fun getZoneById(id: String): GeofenceZone?

    @Query("SELECT * FROM geofence_zones WHERE zoneType = :type")
    suspend fun getZonesByType(type: ZoneType): List<GeofenceZone>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(zone: GeofenceZone)

    @Update
    suspend fun update(zone: GeofenceZone)

    @Delete
    suspend fun delete(zone: GeofenceZone)
}
