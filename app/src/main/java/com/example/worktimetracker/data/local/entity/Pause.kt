package com.example.worktimetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "pauses",
    foreignKeys = [ForeignKey(
        entity = TrackingEntry::class,
        parentColumns = ["id"],
        childColumns = ["entryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("entryId")]
)
data class Pause(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entryId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null
)
