# F12 - Entry Editing Architecture

## Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                            │
├─────────────────────────────────────────────────────────────┤
│  EntriesScreen                                              │
│  ├─ LazyColumn (Entry List)                                │
│  │  └─ EntryCard (for each entry)                          │
│  ├─ FloatingActionButton (+)                               │
│  ├─ EntryEditorSheet (ModalBottomSheet)                    │
│  │  ├─ DatePicker                                          │
│  │  ├─ TypeDropdown                                        │
│  │  ├─ TimePickers (Start/End)                             │
│  │  ├─ Pause List + AddPauseDialog                        │
│  │  ├─ Notes TextField                                     │
│  │  ├─ Confirmed Checkbox                                  │
│  │  └─ Save/Delete Buttons                                 │
│  └─ DeleteConfirmationDialog                               │
└─────────────────────────────────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      ViewModel Layer                        │
├─────────────────────────────────────────────────────────────┤
│  EntriesViewModel                                           │
│  ├─ entries: StateFlow<List<TrackingEntryWithPauses>>      │
│  ├─ deleteConfirmationState: StateFlow                     │
│  └─ Methods: showDelete, confirmDelete, cancelDelete       │
│                                                             │
│  EntryEditorViewModel (AssistedInject)                     │
│  ├─ editorState: StateFlow<EntryEditorState>              │
│  │  ├─ date, type, startTime, endTime                     │
│  │  ├─ notes, pauses, confirmed                            │
│  │  └─ netDuration (computed)                              │
│  ├─ validationErrors: StateFlow<List<String>>             │
│  └─ Methods:                                                │
│     ├─ updateDate, updateType, updateTimes, updateNotes   │
│     ├─ toggleConfirmed                                      │
│     ├─ addPause, removePause                               │
│     └─ saveEntry                                            │
└─────────────────────────────────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer                         │
├─────────────────────────────────────────────────────────────┤
│  TrackingRepository                                         │
│  ├─ getAllEntriesWithPauses(): Flow                        │
│  ├─ getEntryWithPausesById(id): Flow                       │
│  ├─ createEntry(...): String                               │
│  ├─ updateEntry(entry)                                     │
│  ├─ deleteEntry(entry)                                     │
│  ├─ addPause(...): String                                  │
│  ├─ updatePause(pause)                                     │
│  └─ deletePause(pause)                                     │
└─────────────────────────────────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                       DAO Layer                             │
├─────────────────────────────────────────────────────────────┤
│  TrackingDao                                                │
│  ├─ getAllEntriesWithPauses(): Flow<List<...>>            │
│  ├─ getEntryById(id): TrackingEntry?                       │
│  ├─ insert(entry)                                          │
│  ├─ update(entry)                                          │
│  └─ delete(entry)                                          │
│                                                             │
│  PauseDao                                                   │
│  ├─ getPausesForEntry(entryId): Flow<List<Pause>>         │
│  ├─ insert(pause)                                          │
│  ├─ update(pause)                                          │
│  └─ delete(pause)                                          │
└─────────────────────────────────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      Room Database                          │
├─────────────────────────────────────────────────────────────┤
│  tracking_entries                                           │
│  ├─ id (PK)                                                │
│  ├─ date, type, startTime, endTime                         │
│  ├─ autoDetected, confirmed, notes                         │
│  └─ @Transaction → TrackingEntryWithPauses                 │
│                                                             │
│  pauses                                                     │
│  ├─ id (PK)                                                │
│  ├─ entryId (FK → tracking_entries.id)                    │
│  └─ startTime, endTime                                     │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow Examples

### Creating New Entry

```
User Action (FAB +)
    ↓
EntriesScreen sets selectedEntryId = "new"
    ↓
EntryEditorSheet with entryId = null
    ↓
EntryEditorViewModel init (AssistedInject)
    ├─ No entryId → New Entry Mode
    └─ Default state: date=today, type=MANUAL, times=null
    ↓
User fills form & clicks Save
    ↓
EntryEditorViewModel.saveEntry()
    ├─ Validates editorState
    ├─ validationErrors.isEmpty() → proceed
    └─ repository.createEntry(...) → returns newEntryId
    ↓
For each pause in editorState.pauses:
    └─ repository.addPause(newEntryId, ...)
    ↓
UI closes, EntriesScreen refreshes (via entries Flow)
```

### Editing Existing Entry

```
User taps EntryCard
    ↓
EntriesScreen sets selectedEntryId = entry.id
    ↓
EntryEditorSheet with entryId = "actual-id"
    ↓
EntryEditorViewModel init
    └─ loadEntry(entryId)
        ├─ repository.getEntryWithPausesById(id) → Flow
        └─ Updates _date, _type, _startTime, _endTime, _pauses, etc.
        ↓
User modifies fields (e.g., startTime)
    ↓
EntryEditorViewModel.updateStartTime(newTime)
    └─ _startTime.value = newTime
        ↓
        editorState recomputes (combine)
        ├─ netDuration recalculated
        └─ validationErrors recalculated
        ↓
User clicks Save
    ↓
EntryEditorViewModel.saveEntry()
    ├─ originalEntry.copy(...) with new values
    └─ repository.updateEntry(updatedEntry)
    ↓
For new pauses: repository.addPause(...)
For modified pauses: repository.updatePause(...)
    ↓
UI closes, changes reflected in list
```

### Deleting Entry

```
User opens entry → clicks Delete
    ↓
EntryEditorSheet shows showDeleteConfirmation = true
    ↓
User confirms in AlertDialog
    ↓
Calls onDelete callback (from EntriesScreen)
    ↓
EntriesViewModel.showDeleteConfirmation(entry)
    └─ _deleteConfirmationState.value = DeleteConfirmationState(
        showDialog = true,
        entryToDelete = entry
    )
    ↓
EntriesScreen shows AlertDialog
    ↓
User confirms
    ↓
EntriesViewModel.confirmDelete()
    └─ repository.deleteEntry(entry)
        └─ TrackingDao.delete(entry)
            └─ Room CASCADE deletes all pauses (FK constraint)
            ↓
entries Flow emits new list (without deleted entry)
    ↓
UI updates automatically
```

## State Management

### EntryEditorState
```kotlin
data class EntryEditorState(
    val date: LocalDate,              // From DatePicker
    val type: TrackingType,           // From Dropdown
    val startTime: LocalTime?,        // From TimePicker
    val endTime: LocalTime?,          // From TimePicker
    val notes: String,                // From TextField
    val pauses: List<PauseEdit>,      // From AddPauseDialog
    val confirmed: Boolean,           // From Checkbox
    val netDuration: Duration?        // COMPUTED
)
```

**Computed netDuration**:
```kotlin
combine(_startTime, _endTime, _pauses) { start, end, pauses ->
    if (start != null && end != null) {
        val gross = Duration.between(start, end)
        val totalPause = pauses
            .filter { it.startTime != null && it.endTime != null }
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        gross.minusMinutes(totalPause)
    } else null
}
```

### Validation Flow
```kotlin
validationErrors: StateFlow<List<String>> = editorState.map { state ->
    val errors = mutableListOf<String>()

    // Required fields
    if (state.startTime == null) errors.add("Startzeit ist erforderlich")
    if (state.endTime == null) errors.add("Endzeit ist erforderlich")

    // Time validation
    if (state.startTime != null && state.endTime != null) {
        if (state.startTime >= state.endTime) {
            errors.add("Startzeit muss vor Endzeit liegen")
        }

        // Warning for long days
        if (state.netDuration?.toMinutes() ?: 0 > 12 * 60) {
            errors.add("Ungewöhnlich langer Tag (>12h)")
        }

        // Pause validations
        state.pauses.forEach { pause ->
            if (pause.startTime != null && pause.endTime != null) {
                if (pause.startTime < state.startTime ||
                    pause.endTime > state.endTime) {
                    errors.add("Pause außerhalb des Zeitraums")
                }
            }
        }

        // Overlapping pauses
        val sorted = state.pauses
            .filter { it.startTime != null && it.endTime != null }
            .sortedBy { it.startTime }
        for (i in 0 until sorted.size - 1) {
            if (sorted[i].endTime!! > sorted[i+1].startTime!!) {
                errors.add("Pausen überlappen sich")
                break
            }
        }
    }

    errors
}
```

## AssistedInject Pattern

```kotlin
@HiltViewModel(assistedFactory = EntryEditorViewModel.Factory::class)
class EntryEditorViewModel @AssistedInject constructor(
    private val repository: TrackingRepository,
    @Assisted private val entryId: String?  // Runtime parameter
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(entryId: String?): EntryEditorViewModel
    }

    init {
        if (entryId != null) {
            loadEntry(entryId)
        }
    }
}
```

**Usage in Composable**:
```kotlin
val viewModel: EntryEditorViewModel = hiltViewModel(
    creationCallback = { factory: EntryEditorViewModel.Factory ->
        factory.create(entryId)  // Pass runtime parameter
    }
)
```

This allows the ViewModel to have both injected dependencies (repository) and runtime parameters (entryId).

## Testing Strategy

### Repository Tests (Unit)
- Mock TrackingDao and PauseDao
- Verify correct delegation
- Test Flow transformations

### ViewModel Tests (Unit)
- Mock TrackingRepository
- Test state updates via StateFlow
- Verify validation logic
- Test save/delete operations
- Use Turbine for Flow testing

### UI Tests (Integration - Manual)
- Visual verification on device
- User interaction flows
- Validation error display
- Navigation and dialog behavior
