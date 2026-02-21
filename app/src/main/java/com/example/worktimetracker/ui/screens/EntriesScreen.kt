package com.example.worktimetracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.ui.viewmodel.EntriesViewModel
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EntriesScreen(
    viewModel: EntriesViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val listItems by viewModel.listItems.collectAsState()
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsState()
    val deleteConfirmationState by viewModel.deleteConfirmationState.collectAsState()
    var selectedEntryId by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun onSwipeDelete(entry: TrackingEntry) {
        viewModel.swipeDelete(entry) { deleted ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Eintrag gelöscht",
                    actionLabel = "Rückgängig",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(deleted)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MonthNavigationBar(
                selectedYearMonth = selectedYearMonth,
                onPrev = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )
            if (listItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keine Einträge in diesem Monat.\nTippe auf + um einen neuen Eintrag zu erstellen.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listItems.forEach { listItem ->
                        when (listItem) {
                            is EntriesListItem.WeekHeader ->
                                stickyHeader(key = "week_${listItem.weekNumber}_${listItem.weekStart}") {
                                    WeekSeparatorHeader(listItem)
                                }
                            is EntriesListItem.EntryItem ->
                                item(key = listItem.entryWithPauses.entry.id) {
                                    SwipeableEntryCard(
                                        entryWithPauses = listItem.entryWithPauses,
                                        onSwipeDelete   = { onSwipeDelete(listItem.entryWithPauses.entry) },
                                        onSwipeConfirm  = { viewModel.confirmEntry(listItem.entryWithPauses.entry) },
                                        onClick         = { selectedEntryId = listItem.entryWithPauses.entry.id }
                                    )
                                }
                        }
                    }
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
private fun MonthNavigationBar(
    selectedYearMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
    val label = selectedYearMonth.format(formatter)
        .replaceFirstChar { it.uppercaseChar() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Vorheriger Monat")
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Nächster Monat")
        }
    }
}

@Composable
private fun WeekSeparatorHeader(header: EntriesListItem.WeekHeader) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "KW ${header.weekNumber}  ·  ${header.weekStart.format(dateFormatter)} – ${header.weekEnd.format(dateFormatter)}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableEntryCard(
    entryWithPauses: TrackingEntryWithPauses,
    onSwipeDelete: () -> Unit,
    onSwipeConfirm: () -> Unit,
    onClick: () -> Unit
) {
    val entry = entryWithPauses.entry
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipeDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!entry.confirmed) onSwipeConfirm()
                    false
                }
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !entry.confirmed,
        enableDismissFromEndToStart = true,
        backgroundContent = { SwipeBackground(dismissState) },
        content = { CompactEntryCard(entryWithPauses, onClick) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(state: androidx.compose.material3.SwipeToDismissBoxState) {
    val direction = state.targetValue
    val color by animateColorAsState(
        targetValue = when (direction) {
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
            SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50).copy(alpha = 0.2f)
            SwipeToDismissBoxValue.Settled -> Color.Transparent
        },
        label = "swipeBackgroundColor"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = when (direction) {
            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        when (direction) {
            SwipeToDismissBoxValue.EndToStart ->
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Löschen",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            SwipeToDismissBoxValue.StartToEnd ->
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Bestätigen",
                    tint = Color(0xFF4CAF50)
                )
            SwipeToDismissBoxValue.Settled -> {}
        }
    }
}

@Composable
private fun CompactEntryCard(
    entryWithPauses: TrackingEntryWithPauses,
    onClick: () -> Unit
) {
    val entry = entryWithPauses.entry
    val netDuration = entryWithPauses.netDuration()

    val dayOfWeek = entry.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN)
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val typeLabel = when (entry.type) {
        TrackingType.COMMUTE_OFFICE -> "🏢 Büro"
        TrackingType.HOME_OFFICE -> "🏠 HO"
        TrackingType.MANUAL -> "✏️ Manuell"
    }

    val totalPauseMinutes = entryWithPauses.pauses
        .filter { it.endTime != null }
        .sumOf { java.time.Duration.between(it.startTime, it.endTime).toMinutes() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$dayOfWeek ${entry.date.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${netDuration.toHours()}h ${netDuration.toMinutes() % 60}min",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entry.startTime.format(timeFormatter)} – ${entry.endTime?.format(timeFormatter) ?: "..."}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (totalPauseMinutes > 0) {
                        Text(
                            text = "⏸ ${totalPauseMinutes}min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (entry.confirmed) Icons.Default.Check else Icons.Default.Delete,
                        contentDescription = if (entry.confirmed) "Bestätigt" else "Nicht bestätigt",
                        modifier = Modifier.size(16.dp),
                        tint = if (entry.confirmed)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            if (!entry.notes.isNullOrBlank()) {
                Text(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
