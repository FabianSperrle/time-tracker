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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.ui.viewmodel.EntriesViewModel
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntriesScreen(
    viewModel: EntriesViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val deleteConfirmationState by viewModel.deleteConfirmationState.collectAsState()
    var selectedEntryId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einträge") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { selectedEntryId = "new" }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Neuer Eintrag")
            }
        }
    ) { paddingValues ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Keine Einträge vorhanden.\nTippe auf + um einen neuen Eintrag zu erstellen.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.entry.id }) { entryWithPauses ->
                    EntryCard(
                        entryWithPauses = entryWithPauses,
                        onClick = { selectedEntryId = entryWithPauses.entry.id }
                    )
                }
            }
        }
    }

    if (selectedEntryId != null) {
        val entryToEdit = if (selectedEntryId != "new") {
            entries.find { it.entry.id == selectedEntryId }?.entry
        } else null

        EntryEditorSheet(
            entryId = if (selectedEntryId == "new") null else selectedEntryId,
            onDismiss = { selectedEntryId = null },
            onDelete = if (entryToEdit != null) {
                { viewModel.showDeleteConfirmation(entryToEdit) }
            } else null
        )
    }

    if (deleteConfirmationState.showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Eintrag löschen") },
            text = { Text("Möchten Sie diesen Eintrag unwiderruflich löschen?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmDelete()
                    selectedEntryId = null
                }) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

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

    val typeLabel = when (entry.type) {
        TrackingType.COMMUTE_OFFICE -> "Büro (Pendel)"
        TrackingType.HOME_OFFICE -> "Home Office"
        TrackingType.MANUAL -> "Manuell"
    }

    val statusIcon = if (entry.confirmed) "✅" else "⚠️"
    val statusLabel = if (entry.confirmed) "Bestätigt" else "Nicht bestätigt"

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)
    val dayOfWeek = entry.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN)

    val totalPauseMinutes = entryWithPauses.pauses
        .filter { it.endTime != null }
        .sumOf { java.time.Duration.between(it.startTime, it.endTime).toMinutes() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$dayOfWeek ${entry.date.format(dateFormatter)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$statusIcon $statusLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entry.confirmed)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$typeIcon $typeLabel",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${netDuration.toHours()}h ${netDuration.toMinutes() % 60}min",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${entry.startTime.format(timeFormatter)} – ${entry.endTime?.format(timeFormatter) ?: "..."}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (totalPauseMinutes > 0) {
                    Text(
                        text = "Pause: ${totalPauseMinutes}min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!entry.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}
