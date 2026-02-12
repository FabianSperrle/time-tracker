package com.example.worktimetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.worktimetracker.domain.model.TimeWindow
import com.example.worktimetracker.ui.viewmodel.SettingsViewModel
import java.time.DayOfWeek

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
                onClick = { /* TODO: Open time picker */ }
            )
            SettingsItem(
                label = "Zeitfenster Rück",
                value = uiState.returnWindow.format(),
                onClick = { /* TODO: Open time picker */ }
            )
            SettingsItem(
                label = "Geofence-Zonen",
                value = "3 konfiguriert",
                onClick = onNavigateToMap
            )

            // Home Office Section
            SettingsSectionHeader(title = "HOME OFFICE")
            SettingsItem(
                label = "Beacon",
                value = uiState.beaconUuid?.take(10)?.plus("...") ?: "Nicht konfiguriert",
                onClick = { /* TODO: Open beacon setup */ }
            )
            SettingsItem(
                label = "Beacon Timeout",
                value = "${uiState.beaconTimeout} min",
                onClick = { /* TODO: Open timeout picker */ }
            )
            SettingsItem(
                label = "Scan-Intervall",
                value = "${uiState.bleScanInterval / 1000} sek",
                onClick = { /* TODO: Open interval picker */ }
            )

            // Work Time Section
            SettingsSectionHeader(title = "ARBEITSZEIT")
            SettingsItem(
                label = "Arbeitszeitfenster",
                value = uiState.workTimeWindow.format(),
                onClick = { /* TODO: Open time picker */ }
            )
            SettingsItem(
                label = "Wochensoll",
                value = "${uiState.weeklyTargetHours}h",
                onClick = { /* TODO: Open hours picker */ }
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
                onClick = { /* TODO: Open battery settings */ }
            )
            SettingsItem(
                label = "Über die App",
                value = "",
                onClick = { /* TODO: Open about screen */ }
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
 * Dialog for selecting commute days.
 */
@Composable
private fun CommuteDaysDialog(
    selectedDays: Set<DayOfWeek>,
    onDismiss: () -> Unit,
    onConfirm: (Set<DayOfWeek>) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pendeltage auswählen") },
        text = {
            // TODO: Implement multi-select checkboxes
            Text("Multi-Select UI folgt in nächster Iteration")
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedDays) }) {
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
