package com.example.worktimetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.repository.TrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    private val _deleteConfirmationState = kotlinx.coroutines.flow.MutableStateFlow(DeleteConfirmationState())
    val deleteConfirmationState: StateFlow<DeleteConfirmationState> = _deleteConfirmationState

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
