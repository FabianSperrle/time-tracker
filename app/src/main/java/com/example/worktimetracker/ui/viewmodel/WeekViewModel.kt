package com.example.worktimetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worktimetracker.data.repository.TrackingRepository
import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.model.DaySummary
import com.example.worktimetracker.domain.model.WeekStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the week view dashboard.
 */
@HiltViewModel
class WeekViewModel @Inject constructor(
    private val repository: TrackingRepository,
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    private val _selectedWeekStart = MutableStateFlow(currentWeekStart())

    /**
     * Start of the selected week (Monday).
     */
    val selectedWeekStart: StateFlow<LocalDate> = _selectedWeekStart

    /**
     * Calendar week number.
     */
    val weekNumber: StateFlow<Int> = _selectedWeekStart.map { weekStart ->
        val weekFields = WeekFields.of(Locale.getDefault())
        weekStart.get(weekFields.weekOfWeekBasedYear())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1
    )

    /**
     * Daily summaries for the selected week (Monday to Friday).
     */
    val weekSummaries: StateFlow<List<DaySummary>> = _selectedWeekStart
        .flatMapLatest { weekStart ->
            val weekEnd = weekStart.plusDays(4) // Friday
            repository.getEntriesInRange(weekStart, weekEnd)
                .map { entries ->
                    // Group entries by date
                    val entriesByDate = entries.groupBy { it.entry.date }

                    // Generate summaries for Monday through Friday
                    (0..4).map { dayOffset ->
                        val date = weekStart.plusDays(dayOffset.toLong())
                        val entriesForDay = entriesByDate[date] ?: emptyList()
                        DaySummary.from(date, entriesForDay)
                    }
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Weekly statistics.
     */
    val weekStats: StateFlow<WeekStats> = combine(
        weekSummaries,
        settingsProvider.weeklyTargetHours
    ) { summaries, targetHours ->
        WeekStats.from(summaries, targetHours)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeekStats.EMPTY
    )

    /**
     * True if any entry in the week is unconfirmed.
     */
    val hasUnconfirmedEntries: StateFlow<Boolean> = weekSummaries
        .map { summaries ->
            summaries.any { !it.confirmed }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Navigate to the previous week.
     */
    fun previousWeek() {
        _selectedWeekStart.value = _selectedWeekStart.value.minusWeeks(1)
    }

    /**
     * Navigate to the next week.
     */
    fun nextWeek() {
        _selectedWeekStart.value = _selectedWeekStart.value.plusWeeks(1)
    }

    /**
     * Select a specific week by date.
     */
    fun selectWeek(date: LocalDate) {
        _selectedWeekStart.value = date.with(DayOfWeek.MONDAY)
    }

    /**
     * Returns the Monday of the current week.
     */
    private fun currentWeekStart(): LocalDate {
        return LocalDate.now().with(DayOfWeek.MONDAY)
    }
}
