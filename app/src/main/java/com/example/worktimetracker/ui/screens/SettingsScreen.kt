package com.example.worktimetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.worktimetracker.domain.model.BeaconScanResult
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
    val scanResults by viewModel.beaconScanResults.collectAsStateWithLifecycle()
    val liveRssi by viewModel.liveRssi.collectAsStateWithLifecycle()

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
            SettingsItem(
                label = "Beacon testen",
                value = when {
                    uiState.beaconUuid == null -> "Scannt alle Beacons in Reichweite"
                    uiState.beaconRssiThreshold != null -> "Kalibriert (${uiState.beaconRssiThreshold} dBm)"
                    else -> "Nicht kalibriert"
                },
                onClick = { viewModel.showBeaconTestDialog() }
            )
            if (uiState.beaconRssiThreshold != null) {
                SettingsItem(
                    label = "Reichweiten-Schwellenwert",
                    value = "${uiState.beaconRssiThreshold} dBm",
                    onClick = { viewModel.startCalibration() }
                )
            }

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

            // Backup Section
            SettingsSectionHeader(title = "DATENSICHERUNG")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automatische Sicherung",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ihre Daten werden automatisch von Android auf Google Drive gesichert. Beim Einrichten eines neuen Geräts werden sie wiederhergestellt, wenn Sie mit demselben Google-Konto angemeldet sind.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "ℹ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
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

    // Beacon Test Dialog
    if (uiState.showBeaconTestDialog) {
        BeaconTestDialog(
            results = scanResults,
            onSelect = { uuid -> viewModel.selectBeaconFromTest(uuid) },
            onCalibrate = { viewModel.startCalibration() },
            onDismiss = { viewModel.dismissBeaconTestDialog() }
        )
    }

    // Calibration Wizard Dialog
    if (uiState.showCalibrationWizard) {
        CalibrationWizardDialog(
            step = uiState.calibrationStep,
            atDeskRssi = uiState.atDeskRssi,
            awayRssi = uiState.awayRssi,
            liveRssi = liveRssi,
            onMeasureDesk = { viewModel.recordAtDeskRssi() },
            onMeasureAway = { viewModel.recordAwayRssi() },
            onConfirm = { viewModel.confirmCalibration() },
            onBack = { viewModel.backToCalibrationStep1() },
            onDismiss = { viewModel.dismissCalibrationWizard() }
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
 * Returns signal strength bar count (1-4) based on RSSI.
 */
private fun rssiToSignalBars(rssi: Int): Int = when {
    rssi >= -60 -> 4
    rssi >= -70 -> 3
    rssi >= -80 -> 2
    else -> 1
}

/**
 * Returns a visual representation of signal strength using Unicode blocks.
 */
private fun signalBarsText(rssi: Int): String {
    val bars = rssiToSignalBars(rssi)
    return "▮".repeat(bars) + "▯".repeat(4 - bars)
}

/**
 * Dialog showing all nearby beacons in real time for testing/discovery.
 */
@Composable
private fun BeaconTestDialog(
    results: List<BeaconScanResult>,
    onSelect: (String) -> Unit,
    onCalibrate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Beacon testen") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (results.isEmpty()) "Suche läuft..." else "${results.size} Beacon(s) gefunden",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (results.isEmpty()) {
                    Text(
                        text = "Noch keine Beacons in Reichweite",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(results) { result ->
                            BeaconResultRow(
                                result = result,
                                onSelect = onSelect,
                                onCalibrate = onCalibrate
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

/**
 * A single row in the beacon test dialog showing beacon info and action buttons.
 */
@Composable
private fun BeaconResultRow(
    result: BeaconScanResult,
    onSelect: (String) -> Unit,
    onCalibrate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.uuid,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (result.isConfigured) FontWeight.Bold else FontWeight.Normal
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${result.rssi} dBm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = signalBarsText(result.rssi),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "~${"%.1f".format(result.distance)} m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (result.isConfigured) {
                Badge { Text("✓") }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!result.isConfigured) {
                OutlinedButton(
                    onClick = { onSelect(result.uuid) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Verwenden", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (result.isConfigured) {
                OutlinedButton(
                    onClick = onCalibrate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Kalibrieren", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * 3-step calibration wizard dialog for setting the RSSI proximity threshold.
 */
@Composable
private fun CalibrationWizardDialog(
    step: Int,
    atDeskRssi: Int?,
    awayRssi: Int?,
    liveRssi: Int?,
    onMeasureDesk: () -> Unit,
    onMeasureAway: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reichweite kalibrieren – Schritt $step/3") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (step) {
                    1 -> {
                        Text("Setz dich an deinen Schreibtisch in normalem Abstand zum Beacon. Tippe dann 'Jetzt messen'.")
                        liveRssi?.let {
                            Text(
                                text = "Aktuelles Signal: $it dBm  ${signalBarsText(it)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    2 -> {
                        Text("Geh jetzt an den Ort, ab dem das Tracking aufhören soll (z. B. Küche). Wenn du dort bist, tippe 'Hier bin ich weg'.")
                        atDeskRssi?.let {
                            Text(
                                text = "Signal am Schreibtisch: $it dBm",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        liveRssi?.let {
                            Text(
                                text = "Aktuelles Signal: $it dBm  ${signalBarsText(it)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    3 -> {
                        val threshold = if (atDeskRssi != null && awayRssi != null) {
                            (atDeskRssi + awayRssi) / 2
                        } else null

                        atDeskRssi?.let { Text("Signal am Schreibtisch: $it dBm") }
                        awayRssi?.let { Text("Signal weg vom Tisch: $it dBm") }
                        threshold?.let {
                            Text(
                                text = "Schwellenwert: $it dBm — Tracking startet ab diesem Signal, stoppt darunter.",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (step) {
                1 -> Button(onClick = onMeasureDesk) { Text("Jetzt messen") }
                2 -> Button(onClick = onMeasureAway) { Text("Hier bin ich weg") }
                3 -> Button(onClick = onConfirm) { Text("Speichern") }
            }
        },
        dismissButton = {
            Row {
                if (step == 3) {
                    TextButton(onClick = onBack) { Text("Zurück") }
                }
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
            }
        }
    )
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
