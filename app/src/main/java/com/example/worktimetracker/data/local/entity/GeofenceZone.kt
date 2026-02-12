package com.example.worktimetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "geofence_zones")
data class GeofenceZone(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 150f,
    val zoneType: ZoneType,
    val color: Int
)

enum class ZoneType {
    HOME_STATION,
    OFFICE,
    OFFICE_STATION
}
