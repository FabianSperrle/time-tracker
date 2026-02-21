package com.example.worktimetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.repository.TrackingRepository
import com.example.worktimetracker.ui.screens.EntriesListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.temporal.WeekFields
import javax.inject.Inject

data class DeleteConfirmationState(
    val showDialog: Boolean = false,
    val entryToDelete: TrackingEntry? = null
)

@HiltViewModel
class EntriesViewModel @Inject constructor(
    private val repository: TrackingRepository
) : ViewModel() {

    val entries: StateFlow<List<TrackingEntryWithPauses>> =
        repository.getAllEntriesWithPauses()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _deleteConfirmationState = MutableStateFlow(DeleteConfirmationState())
    val deleteConfirmationState: StateFlow<DeleteConfirmationState> = _deleteConfirmationState

    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    val selectedYearMonth: StateFlow<YearMonth> = _selectedYearMonth

    private val _pendingDeleteIds = MutableStateFlow<Set<String>>(emptySet())
    private val _pendingDeleteJobs = mutableMapOf<String, Job>()

    val listItems: StateFlow<List<EntriesListItem>> =
        combine(repository.getAllEntriesWithPauses(), _selectedYearMonth, _pendingDeleteIds) { all, ym, pending ->
            all.filter { YearMonth.from(it.entry.date) == ym && it.entry.id !in pending }
               .toListItemsGroupedByWeek()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun previousMonth() { _selectedYearMonth.value = _selectedYearMonth.value.minusMonths(1) }
    fun nextMonth()     { _selectedYearMonth.value = _selectedYearMonth.value.plusMonths(1) }

    fun swipeDelete(entry: TrackingEntry, onShowSnackbar: (TrackingEntry) -> Unit) {
        _pendingDeleteIds.value = _pendingDeleteIds.value + entry.id
        onShowSnackbar(entry)
        val job = viewModelScope.launch {
            delay(5_000)
            repository.deleteEntry(entry)
            _pendingDeleteIds.value = _pendingDeleteIds.value - entry.id
            _pendingDeleteJobs.remove(entry.id)
        }
        _pendingDeleteJobs[entry.id] = job
    }

    fun undoDelete(entry: TrackingEntry) {
        _pendingDeleteJobs[entry.id]?.cancel()
        _pendingDeleteJobs.remove(entry.id)
        _pendingDeleteIds.value = _pendingDeleteIds.value - entry.id
    }

    fun confirmEntry(entry: TrackingEntry) {
        viewModelScope.launch { repository.updateEntry(entry.copy(confirmed = true)) }
    }

    fun showDeleteConfirmation(entry: TrackingEntry) {
        _deleteConfirmationState.value = DeleteConfirmationState(
            showDialog = true,
            entryToDelete = entry
        )
    }

    fun confirmDelete() {
        val entry = _deleteConfirmationState.value.entryToDelete
        if (entry != null) {
            viewModelScope.launch {
                repository.deleteEntry(entry)
                _deleteConfirmationState.value = DeleteConfirmationState()
            }
        }
    }

    fun cancelDelete() {
        _deleteConfirmationState.value = DeleteConfirmationState()
    }
}

private fun List<TrackingEntryWithPauses>.toListItemsGroupedByWeek(): List<EntriesListItem> {
    val wf = WeekFields.ISO
    val result = mutableListOf<EntriesListItem>()
    groupBy { Pair(it.entry.date.get(wf.weekBasedYear()), it.entry.date.get(wf.weekOfWeekBasedYear())) }
        .forEach { (_, weekEntries) ->
            val weekStart = weekEntries.minOf { it.entry.date }.with(DayOfWeek.MONDAY)
            result += EntriesListItem.WeekHeader(
                weekNumber = weekStart.get(wf.weekOfWeekBasedYear()),
                weekStart  = weekStart,
                weekEnd    = weekStart.plusDays(6)
            )
            weekEntries.forEach { result += EntriesListItem.EntryItem(it) }
        }
    return result
}
