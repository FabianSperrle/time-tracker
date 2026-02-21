package com.example.worktimetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worktimetracker.domain.export.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for the CSV export dialog.
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val csvExporter: CsvExporter
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(ExportRange.THIS_WEEK)
    val selectedRange: StateFlow<ExportRange> = _selectedRange

    private val _customStartDate = MutableStateFlow(LocalDate.now())
    private val _customEndDate = MutableStateFlow(LocalDate.now())

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    /**
     * The currently selected date range for export.
     */
    val dateRange: StateFlow<Pair<LocalDate, LocalDate>> = combine(
        _selectedRange,
        _customStartDate,
        _customEndDate
    ) { range, customStart, customEnd ->
        when (range) {
            ExportRange.THIS_WEEK -> getCurrentWeekRange()
            ExportRange.LAST_MONTH -> getLastMonthRange()
            ExportRange.CUSTOM -> customStart to customEnd
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = getCurrentWeekRange()
    )

    /**
     * Select the export range type.
     */
    fun selectRange(range: ExportRange) {
        _selectedRange.value = range
    }

    /**
     * Set custom start date.
     */
    fun setCustomStartDate(date: LocalDate) {
        _customStartDate.value = date
    }

    /**
     * Set custom end date.
     */
    fun setCustomEndDate(date: LocalDate) {
        _customEndDate.value = date
    }

    /**
     * Export the selected date range to CSV.
     */
    fun export() {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val (start, end) = when (_selectedRange.value) {
                    ExportRange.THIS_WEEK  -> getCurrentWeekRange()
                    ExportRange.LAST_MONTH -> getLastMonthRange()
                    ExportRange.CUSTOM     -> _customStartDate.value to _customEndDate.value
                }
                val file = csvExporter.export(start, end)
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    /**
     * Dismiss the export dialog and reset state.
     */
    fun dismissExport() {
        _exportState.value = ExportState.Idle
    }

    private fun getCurrentWeekRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        val friday = today.with(DayOfWeek.FRIDAY)
        return monday to friday
    }

    private fun getLastMonthRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val lastMonth = today.minusMonths(1)
        val firstDay = lastMonth.withDayOfMonth(1)
        val lastDay = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
        return firstDay to lastDay
    }
}

/**
 * Export range options.
 */
enum class ExportRange {
    THIS_WEEK,
    LAST_MONTH,
    CUSTOM
}

/**
 * State of the CSV export operation.
 */
sealed class ExportState {
    data object Idle : ExportState()
    data object Loading : ExportState()
    data class Success(val file: File) : ExportState()
    data class Error(val message: String) : ExportState()
}
