package com.example.worktimetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.worktimetracker.data.local.entity.PlaceholderEntity

/**
 * Main Room database for Work Time Tracker.
 * PlaceholderEntity will be replaced by real entities in future features.
 */
@Database(
    entities = [PlaceholderEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase()
