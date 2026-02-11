package com.example.worktimetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Placeholder entity for database initialization.
 * Will be removed when real entities are added in future features.
 */
@Entity(tableName = "placeholder")
data class PlaceholderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
)
