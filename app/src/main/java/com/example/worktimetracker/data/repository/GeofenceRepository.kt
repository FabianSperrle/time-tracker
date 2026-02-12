package com.example.worktimetracker.data.repository

import com.example.worktimetracker.data.local.dao.GeofenceDao
import com.example.worktimetracker.data.local.entity.GeofenceZone
import com.example.worktimetracker.data.local.entity.ZoneType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceRepository @Inject constructor(
    private val geofenceDao: GeofenceDao
) {
    fun getAllZones(): Flow<List<GeofenceZone>> = geofenceDao.getAllZones()

    suspend fun getZonesByType(type: ZoneType): List<GeofenceZone> =
        geofenceDao.getZonesByType(type)

    suspend fun insertZone(zone: GeofenceZone) {
        geofenceDao.insert(zone)
    }

    suspend fun updateZone(zone: GeofenceZone) {
        geofenceDao.update(zone)
    }

    suspend fun deleteZone(zone: GeofenceZone) {
        geofenceDao.delete(zone)
    }

    suspend fun hasRequiredZones(): Boolean {
        val homeStations = geofenceDao.getZonesByType(ZoneType.HOME_STATION)
        val offices = geofenceDao.getZonesByType(ZoneType.OFFICE)
        return homeStations.isNotEmpty() && offices.isNotEmpty()
    }
}
