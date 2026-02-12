package com.example.worktimetracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import java.time.Duration
import java.time.LocalDateTime

data class TrackingEntryWithPauses(
    @Embedded val entry: TrackingEntry,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val pauses: List<Pause>
) {
    fun netDuration(): Duration {
        val totalPause = pauses
            .filter { it.endTime != null }
            .sumOf { Duration.between(it.startTime, it.endTime!!).toMinutes() }
        val gross = Duration.between(entry.startTime, entry.endTime ?: LocalDateTime.now())
        return gross.minusMinutes(totalPause)
    }
}
