package com.example.worktimetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worktimetracker.data.local.entity.Pause
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.repository.TrackingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class PauseEdit(
    val id: String = UUID.randomUUID().toString(),
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val pauseEntity: Pause? = null
)

data class EntryEditorState(
    val date: LocalDate,
    val type: TrackingType,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val notes: String,
    val pauses: List<PauseEdit>,
    val confirmed: Boolean,
    val netDuration: Duration?
)

/**
 * ViewModel for editing tracking entries.
 *
 * Uses AssistedInject to support optional entryId parameter.
 * Requires Hilt 2.52+ for @HiltViewModel with assistedFactory support.
 *
 * @param entryId Optional ID of existing entry to edit. If null, creates a new entry.
 */
@HiltViewModel(assistedFactory = EntryEditorViewModel.Factory::class)
class EntryEditorViewModel @AssistedInject constructor(
    private val repository: TrackingRepository,
    @Assisted private val entryId: String?
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(entryId: String?): EntryEditorViewModel
    }

    private val _date = MutableStateFlow(LocalDate.now())
    private val _type = MutableStateFlow(TrackingType.MANUAL)
    private val _startTime = MutableStateFlow<LocalTime?>(null)
    private val _endTime = MutableStateFlow<LocalTime?>(null)
    private val _notes = MutableStateFlow("")
    private val _pauses = MutableStateFlow<List<PauseEdit>>(emptyList())
    private val _confirmed = MutableStateFlow(false)
    private var originalEntry: TrackingEntry? = null
    private var originalPauses: List<Pause> = emptyList()

    val editorState: StateFlow<EntryEditorState> = combine(
        _date,
        _type,
        _startTime,
        _endTime,
        _notes,
        _pauses,
        _confirmed
    ) { date, type, startTime, endTime, notes, pauses, confirmed ->
        val netDuration = calculateNetDuration(startTime, endTime, pauses)
        EntryEditorState(
            date = date,
            type = type,
            startTime = startTime,
            endTime = endTime,
            notes = notes,
            pauses = pauses,
            confirmed = confirmed,
            netDuration = netDuration
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EntryEditorState(
            date = LocalDate.now(),
            type = TrackingType.MANUAL,
            startTime = null,
            endTime = null,
            notes = "",
            pauses = emptyList(),
            confirmed = false,
            netDuration = null
        )
    )

    /**
     * Validation messages including both blocking errors and non-blocking warnings.
     * Only blocking errors prevent saving.
     */
    val validationErrors: StateFlow<List<String>> = editorState.map { state ->
        buildValidationMessages(state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun buildValidationMessages(state: EntryEditorState): List<String> {
        val messages = mutableListOf<String>()

        // Blocking errors
        if (state.startTime == null) {
            messages.add("Startzeit ist erforderlich")
        }
        if (state.endTime == null) {
            messages.add("Endzeit ist erforderlich")
        }

        if (state.startTime != null && state.endTime != null) {
            if (state.startTime >= state.endTime) {
                messages.add("Startzeit muss vor Endzeit liegen")
            }

            state.pauses.forEach { pause ->
                if (pause.startTime != null && pause.endTime != null) {
                    if (pause.startTime >= pause.endTime) {
                        messages.add("Pause ${pause.startTime}-${pause.endTime}: Start muss vor Ende liegen")
                    }
                    if (pause.startTime < state.startTime || pause.endTime > state.endTime) {
                        messages.add("Pause ${pause.startTime}-${pause.endTime} liegt außerhalb des Zeitraums")
                    }
                }
            }

            val sortedPauses = state.pauses.filter { it.startTime != null && it.endTime != null }
                .sortedBy { it.startTime }
            for (i in 0 until sortedPauses.size - 1) {
                val current = sortedPauses[i]
                val next = sortedPauses[i + 1]
                if (current.endTime!! > next.startTime!!) {
                    messages.add("Pausen überlappen sich")
                    break
                }
            }

            // Non-blocking warning (but still prevents save for safety)
            val netMinutes = state.netDuration?.toMinutes() ?: 0
            if (netMinutes > 12 * 60) {
                messages.add("Warnung: Ungewöhnlich langer Tag (>12h)")
            }
        }

        return messages
    }

    init {
        if (entryId != null) {
            loadEntry(entryId)
        }
    }

    private fun loadEntry(id: String) {
        viewModelScope.launch {
            repository.getEntryWithPausesById(id).collect { entryWithPauses ->
                if (entryWithPauses != null) {
                    originalEntry = entryWithPauses.entry
                    originalPauses = entryWithPauses.pauses
                    _date.value = entryWithPauses.entry.date
                    _type.value = entryWithPauses.entry.type
                    _startTime.value = entryWithPauses.entry.startTime.toLocalTime()
                    _endTime.value = entryWithPauses.entry.endTime?.toLocalTime()
                    _notes.value = entryWithPauses.entry.notes ?: ""
                    _confirmed.value = entryWithPauses.entry.confirmed
                    _pauses.value = entryWithPauses.pauses.map { pause ->
                        PauseEdit(
                            id = pause.id,
                            startTime = pause.startTime.toLocalTime(),
                            endTime = pause.endTime?.toLocalTime(),
                            pauseEntity = pause
                        )
                    }
                }
            }
        }
    }

    private fun calculateNetDuration(
        startTime: LocalTime?,
        endTime: LocalTime?,
        pauses: List<PauseEdit>
    ): Duration? {
        if (startTime == null || endTime == null) return null

        val gross = Duration.between(startTime, endTime)
        val totalPause = pauses
            .filter { it.startTime != null && it.endTime != null }
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }

        return gross.minusMinutes(totalPause)
    }

    fun updateDate(date: LocalDate) {
        _date.value = date
    }

    fun updateType(type: TrackingType) {
        _type.value = type
    }

    fun updateStartTime(time: LocalTime) {
        _startTime.value = time
    }

    fun updateEndTime(time: LocalTime) {
        _endTime.value = time
    }

    fun updateNotes(notes: String) {
        _notes.value = notes
    }

    fun toggleConfirmed() {
        _confirmed.value = !_confirmed.value
    }

    fun addPause(startTime: LocalTime?, endTime: LocalTime?) {
        _pauses.value = _pauses.value + PauseEdit(
            startTime = startTime,
            endTime = endTime
        )
    }

    fun removePause(pauseId: String) {
        viewModelScope.launch {
            val pauseToRemove = _pauses.value.find { it.id == pauseId }
            if (pauseToRemove?.pauseEntity != null) {
                repository.deletePause(pauseToRemove.pauseEntity)
            }
            _pauses.value = _pauses.value.filter { it.id != pauseId }
        }
    }

    suspend fun saveEntry(): Boolean {
        if (validationErrors.value.isNotEmpty()) {
            return false
        }

        val state = editorState.value
        val startDateTime = LocalDateTime.of(state.date, state.startTime!!)
        val endDateTime = state.endTime?.let { LocalDateTime.of(state.date, it) }

        if (entryId == null) {
            val newEntryId = repository.createEntry(
                date = state.date,
                type = state.type,
                startTime = startDateTime,
                endTime = endDateTime,
                notes = state.notes.ifBlank { null }
            )

            state.pauses.forEach { pause ->
                if (pause.startTime != null && pause.endTime != null) {
                    repository.addPause(
                        entryId = newEntryId,
                        startTime = LocalDateTime.of(state.date, pause.startTime),
                        endTime = LocalDateTime.of(state.date, pause.endTime)
                    )
                }
            }
        } else {
            val updatedEntry = originalEntry!!.copy(
                date = state.date,
                type = state.type,
                startTime = startDateTime,
                endTime = endDateTime,
                notes = state.notes.ifBlank { null },
                confirmed = state.confirmed
            )
            repository.updateEntry(updatedEntry)

            // Track which pauses are in the current state
            val currentPauseEntityIds = state.pauses.mapNotNull { it.pauseEntity?.id }.toSet()

            // Delete pauses that existed originally but are no longer in the current state
            originalPauses.forEach { originalPause ->
                if (!currentPauseEntityIds.contains(originalPause.id)) {
                    repository.deletePause(originalPause)
                }
            }

            // Update or create pauses
            state.pauses.forEach { pause ->
                if (pause.pauseEntity != null) {
                    // Update existing pause
                    if (pause.startTime != null && pause.endTime != null) {
                        val updatedPause = pause.pauseEntity.copy(
                            startTime = LocalDateTime.of(state.date, pause.startTime),
                            endTime = LocalDateTime.of(state.date, pause.endTime)
                        )
                        repository.updatePause(updatedPause)
                    }
                } else {
                    // Create new pause
                    if (pause.startTime != null && pause.endTime != null) {
                        repository.addPause(
                            entryId = entryId,
                            startTime = LocalDateTime.of(state.date, pause.startTime),
                            endTime = LocalDateTime.of(state.date, pause.endTime)
                        )
                    }
                }
            }
        }

        return true
    }
}
