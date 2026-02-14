package com.example.worktimetracker.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.worktimetracker.ui.viewmodel.ExportRange
import com.example.worktimetracker.ui.viewmodel.ExportState
import com.example.worktimetracker.ui.viewmodel.ExportViewModel
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Dialog for exporting tracking data to CSV.
 */
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedRange by viewModel.selectedRange.collectAsStateWithLifecycle()
    val dateRange by viewModel.dateRange.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    // Handle export success - trigger share intent
    LaunchedEffect(exportState) {
        if (exportState is ExportState.Success) {
            val file = (exportState as ExportState.Success).file
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Arbeitszeit Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "CSV exportieren"))
            viewModel.dismissExport()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.dismissExport()
            onDismiss()
        },
        title = {
            Text("Daten exportieren")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Zeitraum:",
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Range selection
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RangeOption(
                        label = buildWeekLabel(dateRange.first),
                        selected = selectedRange == ExportRange.THIS_WEEK,
                        onClick = { viewModel.selectRange(ExportRange.THIS_WEEK) }
                    )

                    RangeOption(
                        label = "Letzter Monat",
                        selected = selectedRange == ExportRange.LAST_MONTH,
                        onClick = { viewModel.selectRange(ExportRange.LAST_MONTH) }
                    )

                    RangeOption(
                        label = "Benutzerdefiniert",
                        selected = selectedRange == ExportRange.CUSTOM,
                        onClick = { viewModel.selectRange(ExportRange.CUSTOM) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preview
                Text(
                    "Zeitraum: ${formatDateRange(dateRange)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Error message
                if (exportState is ExportState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Fehler: ${(exportState as ExportState.Error).message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Loading indicator
                if (exportState is ExportState.Loading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.export() },
                enabled = exportState !is ExportState.Loading
            ) {
                Text("Exportieren")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.dismissExport()
                    onDismiss()
                },
                enabled = exportState !is ExportState.Loading
            ) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun RangeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private fun buildWeekLabel(weekStart: java.time.LocalDate): String {
    val weekFields = WeekFields.of(Locale.getDefault())
    val weekNumber = weekStart.get(weekFields.weekOfWeekBasedYear())
    return "Diese Woche (KW $weekNumber)"
}

private fun formatDateRange(range: Pair<java.time.LocalDate, java.time.LocalDate>): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    return "${range.first.format(formatter)} - ${range.second.format(formatter)}"
}
