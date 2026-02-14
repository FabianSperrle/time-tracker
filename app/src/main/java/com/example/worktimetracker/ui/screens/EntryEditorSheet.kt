package com.example.worktimetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.ui.viewmodel.EntryEditorViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showAddPauseDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            item {
                Text(
                    text = if (entryId == null) "Neuer Eintrag" else "Eintrag bearbeiten",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = editorState.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)),
                    onValueChange = {},
                    label = { Text("Datum") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { showDatePicker = true }) {
                            Text("Ändern")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                TypeDropdown(
                    selectedType = editorState.type,
                    onTypeSelected = viewModel::updateType
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editorState.startTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "",
                        onValueChange = {},
                        label = { Text("Start") },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            TextButton(onClick = { showStartTimePicker = true }) {
                                Text("Ändern")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = editorState.endTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "",
                        onValueChange = {},
                        label = { Text("Ende") },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            TextButton(onClick = { showEndTimePicker = true }) {
                                Text("Ändern")
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(
                    text = "Pausen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(editorState.pauses, key = { it.id }) { pause ->
                PauseItem(
                    startTime = pause.startTime,
                    endTime = pause.endTime,
                    onDelete = { viewModel.removePause(pause.id) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                TextButton(
                    onClick = { showAddPauseDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Pause hinzufügen", modifier = Modifier.padding(start = 4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                if (editorState.netDuration != null) {
                    val hours = editorState.netDuration.toHours()
                    val minutes = editorState.netDuration.toMinutes() % 60
                    Text(
                        text = "Netto: ${hours}h ${minutes}min",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                OutlinedTextField(
                    value = editorState.notes,
                    onValueChange = viewModel::updateNotes,
                    label = { Text("Notiz") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = editorState.confirmed,
                        onCheckedChange = { viewModel.toggleConfirmed() }
                    )
                    Text("Bestätigt", modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (validationErrors.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            validationErrors.forEach { error ->
                                Text(
                                    text = "• $error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (entryId != null && onDelete != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Text("Löschen", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val success = viewModel.saveEntry()
                                if (success) {
                                    sheetState.hide()
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = validationErrors.isEmpty()
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Eintrag löschen") },
            text = { Text("Möchten Sie diesen Eintrag unwiderruflich löschen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            onDelete?.invoke()
                            onDismiss()
                        }
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editorState.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.updateDate(newDate)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = { time ->
                viewModel.updateStartTime(time)
                showStartTimePicker = false
            },
            initialTime = editorState.startTime ?: LocalTime.now()
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = { time ->
                viewModel.updateEndTime(time)
                showEndTimePicker = false
            },
            initialTime = editorState.endTime ?: LocalTime.now()
        )
    }

    if (showAddPauseDialog) {
        AddPauseDialog(
            onDismiss = { showAddPauseDialog = false },
            onConfirm = { start, end ->
                viewModel.addPause(start, end)
                showAddPauseDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeDropdown(
    selectedType: TrackingType,
    onTypeSelected: (TrackingType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val typeLabels = mapOf(
        TrackingType.COMMUTE_OFFICE to "🏢 Büro (Pendel)",
        TrackingType.HOME_OFFICE to "🏠 Home Office",
        TrackingType.MANUAL to "✏️ Manuell"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = typeLabels[selectedType] ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Typ") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TrackingType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(typeLabels[type] ?: "") },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PauseItem(
    startTime: LocalTime?,
    endTime: LocalTime?,
    onDelete: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${startTime?.format(timeFormatter) ?: "..."} – ${endTime?.format(timeFormatter) ?: "..."}",
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Pause löschen")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    initialTime: LocalTime
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPauseDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalTime, LocalTime) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf<LocalTime?>(null) }
    var endTime by remember { mutableStateOf<LocalTime?>(null) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pause hinzufügen") },
        text = {
            Column {
                OutlinedTextField(
                    value = startTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "",
                    onValueChange = {},
                    label = { Text("Start") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { showStartPicker = true }) {
                            Text("Wählen")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = endTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "",
                    onValueChange = {},
                    label = { Text("Ende") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { showEndPicker = true }) {
                            Text("Wählen")
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (startTime != null && endTime != null) {
                        onConfirm(startTime!!, endTime!!)
                    }
                },
                enabled = startTime != null && endTime != null
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )

    if (showStartPicker) {
        TimePickerDialog(
            onDismiss = { showStartPicker = false },
            onConfirm = { time ->
                startTime = time
                showStartPicker = false
            },
            initialTime = startTime ?: LocalTime.of(12, 0)
        )
    }

    if (showEndPicker) {
        TimePickerDialog(
            onDismiss = { showEndPicker = false },
            onConfirm = { time ->
                endTime = time
                showEndPicker = false
            },
            initialTime = endTime ?: LocalTime.of(12, 30)
        )
    }
}
