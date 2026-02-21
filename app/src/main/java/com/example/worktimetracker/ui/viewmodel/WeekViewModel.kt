package com.example.worktimetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worktimetracker.data.repository.TrackingRepository
import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.model.DaySummary
import com.example.worktimetracker.domain.model.MonthlyStats
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
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
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
     * Daily summaries for the selected week (Mon–Sun). Empty Sat/Sun are omitted.
     */
    val weekSummaries: StateFlow<List<DaySummary>> = _selectedWeekStart
        .flatMapLatest { weekStart ->
            val weekEnd = weekStart.plusDays(6) // Sunday
            repository.getEntriesInRange(weekStart, weekEnd)
                .map { entries ->
                    val byDate = entries.groupBy { it.entry.date }
                    (0..6).mapNotNull { offset ->
                        val date = weekStart.plusDays(offset.toLong())
                        val dayEntries = byDate[date] ?: emptyList()
                        // Hide empty weekend days; always show Mon–Fri
                        if (offset >= 5 && dayEntries.isEmpty()) null
                        else DaySummary.from(date, dayEntries)
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
     * Monthly statistics for the month containing the selected week.
     */
    val monthlyStats: StateFlow<MonthlyStats> = _selectedWeekStart
        .flatMapLatest { weekStart ->
            val ym = YearMonth.from(weekStart)
            combine(
                repository.getEntriesInRange(ym.atDay(1), ym.atEndOfMonth()),
                settingsProvider.weeklyTargetHours
            ) { entries, targetHours ->
                MonthlyStats.from(entries, ym, targetHours)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonthlyStats.EMPTY
        )

    /**
     * All-time saldo (actual hours worked minus expected hours since first entry).
     */
    val allTimeSaldo: StateFlow<Duration> = combine(
        repository.getAllEntriesWithPauses(),
        settingsProvider.weeklyTargetHours
    ) { entries, targetHours ->
        if (entries.isEmpty()) {
            Duration.ZERO
        } else {
            val firstDate = entries.minOf { it.entry.date }
            val workingDays = countWorkingDays(firstDate, LocalDate.now())
            val target = Duration.ofMinutes((workingDays * targetHours / 5.0 * 60).toLong())
            val actual = Duration.ofMinutes(entries.sumOf { it.netDuration().toMinutes() })
            actual.minus(target)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Duration.ZERO
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

    /**
     * Counts Mon–Fri working days between start and end (inclusive) in O(1).
     */
    private fun countWorkingDays(start: LocalDate, end: LocalDate): Long {
        val total = ChronoUnit.DAYS.between(start, end) + 1
        val fullWeeks = total / 7
        val remainder = (total % 7).toInt()
        var count = fullWeeks * 5
        val startDow = start.dayOfWeek.value // 1=Mon … 7=Sun
        repeat(remainder) { i -> if ((startDow - 1 + i) % 7 < 5) count++ }
        return count
    }
}
