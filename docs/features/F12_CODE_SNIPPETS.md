# F12 - Key Code Snippets

## 1. Repository Extensions

### getAllEntriesWithPauses
```kotlin
// TrackingDao.kt
@Transaction
@Query("SELECT * FROM tracking_entries ORDER BY date DESC, startTime DESC")
fun getAllEntriesWithPauses(): Flow<List<TrackingEntryWithPauses>>

// TrackingRepository.kt
fun getAllEntriesWithPauses(): Flow<List<TrackingEntryWithPauses>> {
    return trackingDao.getAllEntriesWithPauses()
}
```

### getEntryWithPausesById
```kotlin
fun getEntryWithPausesById(entryId: String): Flow<TrackingEntryWithPauses?> {
    return combine(
        flow {
            val entry = trackingDao.getEntryById(entryId)
            emit(entry)
        },
        pauseDao.getPausesForEntry(entryId)
    ) { entry, pauses ->
        entry?.let { TrackingEntryWithPauses(it, pauses) }
    }
}
```

### createEntry
```kotlin
suspend fun createEntry(
    date: LocalDate,
    type: TrackingType,
    startTime: LocalDateTime,
    endTime: LocalDateTime?,
    notes: String? = null
): String {
    val entry = TrackingEntry(
        date = date,
        type = type,
        startTime = startTime,
        endTime = endTime,
        autoDetected = false,
        confirmed = false,
        notes = notes
    )
    trackingDao.insert(entry)
    return entry.id
}
```

## 2. EntriesViewModel

```kotlin
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

    fun confirmDelete() {
        val entry = _deleteConfirmationState.value.entryToDelete
        if (entry != null) {
            viewModelScope.launch {
                repository.deleteEntry(entry)
                _deleteConfirmationState.value = DeleteConfirmationState()
            }
        }
    }
}
```

## 3. EntryEditorViewModel - State Computation

```kotlin
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
    initialValue = EntryEditorState(/* defaults */)
)
```

### Validation Logic
```kotlin
val validationErrors: StateFlow<List<String>> = editorState.map { state ->
    val errors = mutableListOf<String>()

    if (state.startTime == null) {
        errors.add("Startzeit ist erforderlich")
    }
    if (state.endTime == null) {
        errors.add("Endzeit ist erforderlich")
    }

    if (state.startTime != null && state.endTime != null) {
        if (state.startTime >= state.endTime) {
            errors.add("Startzeit muss vor Endzeit liegen")
        }

        val netMinutes = state.netDuration?.toMinutes() ?: 0
        if (netMinutes > 12 * 60) {
            errors.add("Ungewöhnlich langer Tag (>12h)")
        }

        // Pause validations
        state.pauses.forEach { pause ->
            if (pause.startTime != null && pause.endTime != null) {
                if (pause.startTime < state.startTime || pause.endTime > state.endTime) {
                    errors.add("Pause ${pause.startTime}-${pause.endTime} liegt außerhalb des Zeitraums")
                }
            }
        }

        // Overlapping pauses
        val sortedPauses = state.pauses.filter { it.startTime != null && it.endTime != null }
            .sortedBy { it.startTime }
        for (i in 0 until sortedPauses.size - 1) {
            val current = sortedPauses[i]
            val next = sortedPauses[i + 1]
            if (current.endTime!! > next.startTime!!) {
                errors.add("Pausen überlappen sich")
                break
            }
        }
    }

    errors
}.stateIn(/* ... */)
```

### saveEntry Logic
```kotlin
fun saveEntry(): Boolean {
    if (validationErrors.value.isNotEmpty()) {
        return false
    }

    val state = editorState.value
    val startDateTime = LocalDateTime.of(state.date, state.startTime!!)
    val endDateTime = state.endTime?.let { LocalDateTime.of(state.date, it) }

    viewModelScope.launch {
        if (entryId == null) {
            // Create new entry
            val newEntryId = repository.createEntry(
                date = state.date,
                type = state.type,
                startTime = startDateTime,
                endTime = endDateTime,
                notes = state.notes.ifBlank { null }
            )

            // Add pauses
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
            // Update existing entry
            val updatedEntry = originalEntry!!.copy(
                date = state.date,
                type = state.type,
                startTime = startDateTime,
                endTime = endDateTime,
                notes = state.notes.ifBlank { null },
                confirmed = state.confirmed
            )
            repository.updateEntry(updatedEntry)

            // Handle pauses (update existing, add new)
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
                    // Add new pause
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
    }

    return true
}
```

## 4. UI Components

### EntryCard
```kotlin
@Composable
fun EntryCard(
    entryWithPauses: TrackingEntryWithPauses,
    onClick: () -> Unit
) {
    val entry = entryWithPauses.entry
    val netDuration = entryWithPauses.netDuration()

    val typeIcon = when (entry.type) {
        TrackingType.COMMUTE_OFFICE -> "🏢"
        TrackingType.HOME_OFFICE -> "🏠"
        TrackingType.MANUAL -> "✏️"
    }

    val statusIcon = if (entry.confirmed) "✅" else "⚠️"
    val statusLabel = if (entry.confirmed) "Bestätigt" else "Nicht bestätigt"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Date + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$dayOfWeek ${entry.date.format(dateFormatter)}")
                Text("$statusIcon $statusLabel")
            }

            // Type + Net Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$typeIcon $typeLabel")
                Text("${netDuration.toHours()}h ${netDuration.toMinutes() % 60}min")
            }

            // Time + Pause
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${entry.startTime.format(timeFormatter)} – ${entry.endTime?.format(timeFormatter) ?: "..."}")
                if (totalPauseMinutes > 0) {
                    Text("Pause: ${totalPauseMinutes}min")
                }
            }

            // Notes
            if (!entry.notes.isNullOrBlank()) {
                Text(entry.notes, fontStyle = Italic)
            }
        }
    }
}
```

### EntryEditorSheet - Key Parts
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorSheet(
    entryId: String?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val viewModel: EntryEditorViewModel = hiltViewModel(
        creationCallback = { factory: EntryEditorViewModel.Factory ->
            factory.create(entryId)
        }
    )
    val editorState by viewModel.editorState.collectAsState()
    val validationErrors by viewModel.validationErrors.collectAsState()

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss
    ) {
        LazyColumn {
            // Date field
            item {
                OutlinedTextField(
                    value = editorState.date.format(...),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        TextButton(onClick = { showDatePicker = true }) {
                            Text("Ändern")
                        }
                    }
                )
            }

            // Type dropdown
            item {
                TypeDropdown(
                    selectedType = editorState.type,
                    onTypeSelected = viewModel::updateType
                )
            }

            // Time pickers
            item {
                Row {
                    OutlinedTextField(/* Start */)
                    OutlinedTextField(/* End */)
                }
            }

            // Pauses
            items(editorState.pauses, key = { it.id }) { pause ->
                PauseItem(
                    startTime = pause.startTime,
                    endTime = pause.endTime,
                    onDelete = { viewModel.removePause(pause.id) }
                )
            }

            item {
                TextButton(onClick = { showAddPauseDialog = true }) {
                    Icon(Icons.Default.Add)
                    Text("Pause hinzufügen")
                }
            }

            // Net duration
            item {
                if (editorState.netDuration != null) {
                    Text("Netto: ${hours}h ${minutes}min")
                }
            }

            // Validation errors
            if (validationErrors.isNotEmpty()) {
                item {
                    Card(containerColor = errorContainer) {
                        validationErrors.forEach { error ->
                            Text("• $error")
                        }
                    }
                }
            }

            // Buttons
            item {
                Row {
                    if (entryId != null && onDelete != null) {
                        OutlinedButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete)
                            Text("Löschen")
                        }
                    }
                    Button(
                        onClick = {
                            if (viewModel.saveEntry()) {
                                onDismiss()
                            }
                        },
                        enabled = validationErrors.isEmpty()
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}
```

## 5. Test Examples

### Repository Test
```kotlin
@Test
fun `createEntry inserts new entry with provided data`() = runTest {
    val entrySlot = slot<TrackingEntry>()
    coEvery { trackingDao.insert(capture(entrySlot)) } returns Unit

    val startTime = LocalDateTime.of(2026, 2, 10, 8, 0)
    val endTime = LocalDateTime.of(2026, 2, 10, 16, 30)

    val result = repository.createEntry(
        date = LocalDate.of(2026, 2, 10),
        type = TrackingType.MANUAL,
        startTime = startTime,
        endTime = endTime,
        notes = "Manual entry"
    )

    val captured = entrySlot.captured
    assertEquals(LocalDate.of(2026, 2, 10), captured.date)
    assertEquals(TrackingType.MANUAL, captured.type)
    assertEquals(startTime, captured.startTime)
    assertEquals(endTime, captured.endTime)
    assertFalse(captured.autoDetected)
    assertFalse(captured.confirmed)
    assertEquals("Manual entry", captured.notes)
    assertEquals(captured.id, result)
}
```

### ViewModel Test - Validation
```kotlin
@Test
fun `validation fails when startTime is after endTime`() = runTest {
    viewModel = EntryEditorViewModel(repository, null)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateStartTime(LocalTime.of(17, 0))
    viewModel.updateEndTime(LocalTime.of(9, 0))

    viewModel.validationErrors.test {
        val errors = awaitItem()
        assertTrue(errors.contains("Startzeit muss vor Endzeit liegen"))
        cancelAndIgnoreRemainingEvents()
    }
}
```

### ViewModel Test - Save
```kotlin
@Test
fun `saveEntry creates new entry when entryId is null`() = runTest {
    coEvery { repository.createEntry(any(), any(), any(), any(), any()) } returns "new-id"

    viewModel = EntryEditorViewModel(repository, null)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateDate(LocalDate.of(2026, 2, 15))
    viewModel.updateType(TrackingType.MANUAL)
    viewModel.updateStartTime(LocalTime.of(9, 0))
    viewModel.updateEndTime(LocalTime.of(17, 0))
    viewModel.updateNotes("Manual entry")

    val result = viewModel.saveEntry()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(result)
    coVerify {
        repository.createEntry(
            date = LocalDate.of(2026, 2, 15),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.of(2026, 2, 15, 9, 0),
            endTime = LocalDateTime.of(2026, 2, 15, 17, 0),
            notes = "Manual entry"
        )
    }
}
```
