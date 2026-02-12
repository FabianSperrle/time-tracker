package com.example.worktimetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "tracking_entries")
data class TrackingEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val type: TrackingType,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val autoDetected: Boolean,
    val confirmed: Boolean = false,
    val notes: String? = null
)

enum class TrackingType {
    COMMUTE_OFFICE,
    HOME_OFFICE,
    MANUAL
}
