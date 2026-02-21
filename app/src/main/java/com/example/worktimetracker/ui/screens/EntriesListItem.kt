package com.example.worktimetracker.ui.screens

import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import java.time.LocalDate

sealed class EntriesListItem {
    data class WeekHeader(
        val weekNumber: Int,
        val weekStart: LocalDate,
        val weekEnd: LocalDate
    ) : EntriesListItem()

    data class EntryItem(
        val entryWithPauses: TrackingEntryWithPauses
    ) : EntriesListItem()
}
