package com.example.worktimetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.ui.viewmodel.DashboardUiState
import com.example.worktimetracker.ui.viewmodel.DashboardViewModel
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.delay

/**
 * Dashboard screen showing current tracking status and manual controls.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = uiState) {
            is DashboardUiState.Idle -> IdleContent(
                onStartManual = viewModel::startManualTracking
            )
            is DashboardUiState.Tracking -> TrackingContent(
                type = state.type,
                startTime = state.startTime,
                onPause = viewModel::pauseTracking,
                onStop = viewModel::stopTracking
            )
            is DashboardUiState.Paused -> PausedContent(
                type = state.type,
                onResume = viewModel::resumeTracking,
                onStop = viewModel::stopTracking
            )
        }
    }
}

@Composable
private fun IdleContent(
    onStartManual: (TrackingType) -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }

    Text(
        text = "Keine aktive Arbeitszeit",
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(bottom = 24.dp)
    )

    Button(
        onClick = { showDropdown = true },
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Text("Tracking starten")
    }

    DropdownMenu(
        expanded = showDropdown,
        onDismissRequest = { showDropdown = false }
    ) {
        DropdownMenuItem(
            text = { Text("Home Office") },
            onClick = {
                showDropdown = false
                onStartManual(TrackingType.HOME_OFFICE)
            }
        )
        DropdownMenuItem(
            text = { Text("Büro") },
            onClick = {
                showDropdown = false
                onStartManual(TrackingType.COMMUTE_OFFICE)
            }
        )
        DropdownMenuItem(
            text = { Text("Sonstiges") },
            onClick = {
                showDropdown = false
                onStartManual(TrackingType.MANUAL)
            }
        )
    }
}

@Composable
private fun TrackingContent(
    type: TrackingType,
    startTime: LocalDateTime,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = LocalDateTime.now()
        }
    }

    val elapsed = Duration.between(startTime, currentTime)
    val hours = elapsed.toHours()
    val minutes = elapsed.toMinutes() % 60
    val seconds = elapsed.seconds % 60

    val typeText = when (type) {
        TrackingType.HOME_OFFICE -> "Home Office"
        TrackingType.COMMUTE_OFFICE -> "Büro"
        TrackingType.MANUAL -> "Manuell"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Arbeitszeit läuft",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = typeText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onPause,
                modifier = Modifier.weight(1f)
            ) {
                Text("Pause")
            }
            Button(
                onClick = onStop,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop")
            }
        }
    }
}

@Composable
private fun PausedContent(
    type: TrackingType,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val typeText = when (type) {
        TrackingType.HOME_OFFICE -> "Home Office"
        TrackingType.COMMUTE_OFFICE -> "Büro"
        TrackingType.MANUAL -> "Manuell"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pause",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = typeText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onResume,
                modifier = Modifier.weight(1f)
            ) {
                Text("Weiter")
            }
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop")
            }
        }
    }
}
