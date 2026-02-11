package com.example.worktimetracker.data.local

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Type converters for Room database.
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
}
