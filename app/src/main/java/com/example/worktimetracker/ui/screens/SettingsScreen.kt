package com.example.worktimetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.worktimetracker.domain.model.TimeWindow
import com.example.worktimetracker.ui.viewmodel.SettingsViewModel
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Settings screen for app configuration.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToMap: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = "Einstellungen",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            // Commute Section
            SettingsSectionHeader(title = "PENDELN")
            SettingsItem(
                label = "Pendeltage",
                value = formatCommuteDays(uiState.commuteDays),
                onClick = { viewModel.showCommuteDaysDialog() }
            )
            SettingsItem(
                label = "Zeitfenster Hin",
                value = uiState.outboundWindow.format(),
                onClick = { viewModel.showOutboundWindowDialog() }
            )
            SettingsItem(
                label = "Zeitfenster Rück",
                value = uiState.returnWindow.format(),
                onClick = { viewModel.showReturnWindowDialog() }
            )
            SettingsItem(
                label = "Geofence-Zonen",
                value = "${uiState.zoneCount} konfiguriert",
                onClick = onNavigateToMap
            )

            // Home Office Section
            SettingsSectionHeader(title = "HOME OFFICE")
            SettingsItem(
                label = "Beacon",
                value = uiState.beaconUuid?.take(10)?.plus("...") ?: "Nicht konfiguriert",
                onClick = { viewModel.showBeaconUuidDialog() }
            )
            SettingsItem(
                label = "Beacon Timeout",
                value = "${uiState.beaconTimeout} min",
                onClick = { viewModel.showBeaconTimeoutDialog() }
            )
            SettingsItem(
                label = "Scan-Intervall",
                value = "${uiState.bleScanInterval / 1000} sek",
                onClick = { viewModel.showBleScanIntervalDialog() }
            )

            // Work Time Section
            SettingsSectionHeader(title = "ARBEITSZEIT")
            SettingsItem(
                label = "Arbeitszeitfenster",
                value = uiState.workTimeWindow.format(),
                onClick = { viewModel.showWorkTimeWindowDialog() }
            )
            SettingsItem(
                label = "Wochensoll",
                value = "${uiState.weeklyTargetHours}h",
                onClick = { viewModel.showWeeklyTargetHoursDialog() }
            )

            // System Section
            SettingsSectionHeader(title = "SYSTEM")
            SettingsItem(
                label = "Berechtigungen",
                value = "Alle OK",
                onClick = onNavigateToPermissions
            )
            SettingsItem(
                label = "Akku-Optimierung",
                value = "Deaktiviert",
                onClick = { /* Battery optimization is managed via system settings */ }
            )
            SettingsItem(
                label = "Über die App",
                value = "",
                onClick = { /* About screen */ }
            )
            SettingsItem(
                label = "Daten zurücksetzen",
                value = "",
                onClick = { viewModel.showResetConfirmation() }
            )
        }
    }

    // Reset Confirmation Dialog
    if (uiState.showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetConfirmation() },
            title = { Text("Daten zurücksetzen") },
            text = { Text("Möchten Sie wirklich alle Einstellungen und Daten zurücksetzen? Diese Aktion kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.resetAllData() }
                ) {
                    Text("Zurücksetzen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissResetConfirmation() }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Commute Days Dialog
    if (uiState.showCommuteDaysDialog) {
        CommuteDaysDialog(
            selectedDays = uiState.commuteDays,
            onDismiss = { viewModel.dismissCommuteDaysDialog() },
            onConfirm = { days ->
                viewModel.updateCommuteDays(days)
                viewModel.dismissCommuteDaysDialog()
            }
        )
    }

    // Outbound Time Window Dialog
    if (uiState.showOutboundWindowDialog) {
        TimeWindowDialog(
            title = "Zeitfenster Hinfahrt",
            timeWindow = uiState.outboundWindow,
            onDismiss = { viewModel.dismissOutboundWindowDialog() },
            onConfirm = { window ->
                viewModel.updateOutboundWindow(window)
                viewModel.dismissOutboundWindowDialog()
            }
        )
    }

    // Return Time Window Dialog
    if (uiState.showReturnWindowDialog) {
        TimeWindowDialog(
            title = "Zeitfenster Rückfahrt",
            timeWindow = uiState.returnWindow,
            onDismiss = { viewModel.dismissReturnWindowDialog() },
            onConfirm = { window ->
                viewModel.updateReturnWindow(window)
                viewModel.dismissReturnWindowDialog()
            }
        )
    }

    // Work Time Window Dialog
    if (uiState.showWorkTimeWindowDialog) {
        TimeWindowDialog(
            title = "Arbeitszeitfenster",
            timeWindow = uiState.workTimeWindow,
            onDismiss = { viewModel.dismissWorkTimeWindowDialog() },
            onConfirm = { window ->
                viewModel.updateWorkTimeWindow(window)
                viewModel.dismissWorkTimeWindowDialog()
            }
        )
    }

    // Beacon Timeout Dialog
    if (uiState.showBeaconTimeoutDialog) {
        IntegerInputDialog(
            title = "Beacon Timeout",
            label = "Minuten (1–60)",
            initialValue = uiState.beaconTimeout,
            minValue = 1,
            maxValue = 60,
            unit = "min",
            onDismiss = { viewModel.dismissBeaconTimeoutDialog() },
            onConfirm = { minutes ->
                viewModel.updateBeaconTimeout(minutes)
                viewModel.dismissBeaconTimeoutDialog()
            }
        )
    }

    // BLE Scan Interval Dialog
    if (uiState.showBleScanIntervalDialog) {
        IntegerInputDialog(
            title = "Scan-Intervall",
            label = "Sekunden (10–300)",
            initialValue = (uiState.bleScanInterval / 1000).toInt(),
            minValue = 10,
            maxValue = 300,
            unit = "sek",
            onDismiss = { viewModel.dismissBleScanIntervalDialog() },
            onConfirm = { seconds ->
                viewModel.updateBleScanInterval(seconds)
                viewModel.dismissBleScanIntervalDialog()
            }
        )
    }

    // Weekly Target Hours Dialog
    if (uiState.showWeeklyTargetHoursDialog) {
        FloatInputDialog(
            title = "Wochensoll",
            label = "Stunden (0–80)",
            initialValue = uiState.weeklyTargetHours,
            minValue = 0f,
            maxValue = 80f,
            unit = "h",
            onDismiss = { viewModel.dismissWeeklyTargetHoursDialog() },
            onConfirm = { hours ->
                viewModel.updateWeeklyTargetHours(hours)
                viewModel.dismissWeeklyTargetHoursDialog()
            }
        )
    }

    // Beacon UUID Dialog
    if (uiState.showBeaconUuidDialog) {
        BeaconUuidDialog(
            currentUuid = uiState.beaconUuid,
            onDismiss = { viewModel.dismissBeaconUuidDialog() },
            onConfirm = { uuid ->
                viewModel.updateBeaconUuid(uuid)
                viewModel.dismissBeaconUuidDialog()
            }
        )
    }
}

/**
 * Section header for settings groups.
 */
@Composable
private fun SettingsSectionHeader(title: String) {
    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * Individual settings item.
 */
@Composable
private fun SettingsItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Formats commute days for display.
 */
private fun formatCommuteDays(days: Set<DayOfWeek>): String {
    if (days.isEmpty()) return "Keine"

    val dayNames = mapOf(
        DayOfWeek.MONDAY to "Mo",
        DayOfWeek.TUESDAY to "Di",
        DayOfWeek.WEDNESDAY to "Mi",
        DayOfWeek.THURSDAY to "Do",
        DayOfWeek.FRIDAY to "Fr",
        DayOfWeek.SATURDAY to "Sa",
        DayOfWeek.SUNDAY to "So"
    )

    return days
        .sortedBy { it.value }
        .mapNotNull { dayNames[it] }
        .joinToString(", ")
}

/**
 * Dialog for selecting commute days via multi-select checkboxes.
 */
@Composable
private fun CommuteDaysDialog(
    selectedDays: Set<DayOfWeek>,
    onDismiss: () -> Unit,
    onConfirm: (Set<DayOfWeek>) -> Unit
) {
    var currentSelection by remember { mutableStateOf(selectedDays) }

    val allDays = listOf(
        DayOfWeek.MONDAY to "Montag",
        DayOfWeek.TUESDAY to "Dienstag",
        DayOfWeek.WEDNESDAY to "Mittwoch",
        DayOfWeek.THURSDAY to "Donnerstag",
        DayOfWeek.FRIDAY to "Freitag",
        DayOfWeek.SATURDAY to "Samstag",
        DayOfWeek.SUNDAY to "Sonntag"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pendeltage auswählen") },
        text = {
            Column {
                allDays.forEach { (day, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentSelection = if (currentSelection.contains(day)) {
                                    currentSelection - day
                                } else {
                                    currentSelection + day
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = currentSelection.contains(day),
                            onCheckedChange = { checked ->
                                currentSelection = if (checked) {
                                    currentSelection + day
                                } else {
                                    currentSelection - day
                                }
                            }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentSelection) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Dialog for editing a time window with two time pickers (start and end).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeWindowDialog(
    title: String,
    timeWindow: TimeWindow,
    onDismiss: () -> Unit,
    onConfirm: (TimeWindow) -> Unit
) {
    val startPickerState = rememberTimePickerState(
        initialHour = timeWindow.start.hour,
        initialMinute = timeWindow.start.minute,
        is24Hour = true
    )
    val endPickerState = rememberTimePickerState(
        initialHour = timeWindow.end.hour,
        initialMinute = timeWindow.end.minute,
        is24Hour = true
    )

    var showingStartPicker by rememberSaveable { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(title)
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    TextButton(
                        onClick = { showingStartPicker = true }
                    ) {
                        Text(
                            text = "Von",
                            fontWeight = if (showingStartPicker) FontWeight.Bold else FontWeight.Normal,
                            color = if (showingStartPicker) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    TextButton(
                        onClick = { showingStartPicker = false }
                    ) {
                        Text(
                            text = "Bis",
                            fontWeight = if (!showingStartPicker) FontWeight.Bold else FontWeight.Normal,
                            color = if (!showingStartPicker) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            if (showingStartPicker) {
                TimePicker(
                    state = startPickerState,
                    colors = TimePickerDefaults.colors()
                )
            } else {
                TimePicker(
                    state = endPickerState,
                    colors = TimePickerDefaults.colors()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = LocalTime.of(startPickerState.hour, startPickerState.minute)
                    val end = LocalTime.of(endPickerState.hour, endPickerState.minute)
                    if (start.isBefore(end)) {
                        errorMessage = null
                        onConfirm(TimeWindow(start, end))
                    } else {
                        errorMessage = "Startzeit muss vor Endzeit liegen"
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Dialog for entering an integer value within a range.
 */
@Composable
private fun IntegerInputDialog(
    title: String,
    label: String,
    initialValue: Int,
    minValue: Int,
    maxValue: Int,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var inputText by rememberSaveable { mutableStateOf(initialValue.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { newValue ->
                        inputText = newValue
                        errorMessage = null
                    },
                    label = { Text(label) },
                    suffix = { Text(unit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorMessage != null
                )
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = inputText.toIntOrNull()
                    when {
                        value == null -> errorMessage = "Bitte eine gültige Zahl eingeben"
                        value < minValue || value > maxValue ->
                            errorMessage = "Wert muss zwischen $minValue und $maxValue liegen"
                        else -> {
                            errorMessage = null
                            onConfirm(value)
                        }
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Dialog for entering a float value within a range.
 */
@Composable
private fun FloatInputDialog(
    title: String,
    label: String,
    initialValue: Float,
    minValue: Float,
    maxValue: Float,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    // Format without trailing .0 if it's a whole number
    val formattedInitial = if (initialValue % 1f == 0f) {
        initialValue.toInt().toString()
    } else {
        initialValue.toString()
    }
    var inputText by rememberSaveable { mutableStateOf(formattedInitial) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { newValue ->
                        inputText = newValue
                        errorMessage = null
                    },
                    label = { Text(label) },
                    suffix = { Text(unit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null
                )
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = inputText.replace(',', '.').toFloatOrNull()
                    when {
                        value == null -> errorMessage = "Bitte eine gültige Zahl eingeben"
                        value < minValue || value > maxValue ->
                            errorMessage = "Wert muss zwischen $minValue und $maxValue liegen"
                        else -> {
                            errorMessage = null
                            onConfirm(value)
                        }
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Dialog for entering or clearing a Beacon UUID.
 */
@Composable
private fun BeaconUuidDialog(
    currentUuid: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var inputText by rememberSaveable { mutableStateOf(currentUuid ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val uuidPattern = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Beacon UUID") },
        text = {
            Column {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { newValue ->
                        inputText = newValue
                        errorMessage = null
                    },
                    label = { Text("UUID (z. B. xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    singleLine = true,
                    isError = errorMessage != null
                )
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (currentUuid != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { onConfirm(null) },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = "Beacon entfernen",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = inputText.trim()
                    when {
                        trimmed.isEmpty() -> onConfirm(null)
                        !uuidPattern.matches(trimmed) ->
                            errorMessage = "Ungültiges UUID-Format"
                        else -> {
                            errorMessage = null
                            onConfirm(trimmed)
                        }
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
