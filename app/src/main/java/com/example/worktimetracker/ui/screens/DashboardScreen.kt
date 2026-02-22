package com.example.worktimetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.domain.model.DayStats
import com.example.worktimetracker.ui.viewmodel.DashboardUiState
import com.example.worktimetracker.ui.viewmodel.DashboardViewModel
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/**
 * Dashboard screen showing current tracking status and manual controls.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Date Header
        Text(
            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy")),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Status Card
        when (val state = uiState) {
            is DashboardUiState.Idle -> IdleCard(
                onStartManual = viewModel::startManualTracking
            )
            is DashboardUiState.Tracking -> TrackingCard(
                type = state.type,
                startTime = state.startTime,
                onPause = viewModel::pauseTracking,
                onStop = viewModel::stopTracking
            )
            is DashboardUiState.Paused -> PausedCard(
                type = state.type,
                onResume = viewModel::resumeTracking,
                onStop = viewModel::stopTracking
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Statistics
        DailyStatsCard(stats = todayStats)
    }
}

@Composable
private fun IdleCard(
    onStartManual: (TrackingType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Kein aktives Tracking",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onStartManual(TrackingType.HOME_OFFICE) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Home Office")
                }
                Button(
                    onClick = { onStartManual(TrackingType.COMMUTE_OFFICE) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Büro")
                }
            }
        }
    }
}

@Composable
private fun TrackingCard(
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TRACKING AKTIV",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$typeText seit ${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⏸ Pause")
                }
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⏹ Stopp")
                }
            }
        }
    }
}

@Composable
private fun PausedCard(
    type: TrackingType,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val typeText = when (type) {
        TrackingType.HOME_OFFICE -> "Home Office"
        TrackingType.COMMUTE_OFFICE -> "Büro"
        TrackingType.MANUAL -> "Manuell"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PAUSE",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = typeText,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onResume,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("▶ Weiter")
                }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⏹ Stopp")
                }
            }
        }
    }
}

@Composable
private fun DailyStatsCard(stats: DayStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Heute",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Divider(modifier = Modifier.padding(bottom = 12.dp))

            StatRow("Brutto:", formatDuration(stats.grossWorkTime))
            StatRow("Pausen:", formatDuration(stats.pauseTime))
            StatRow("Netto:", formatDuration(stats.netWorkTime), highlighted = true)
            StatRow("Soll:", formatDuration(stats.targetWorkTime))

            if (stats.remainingTime > Duration.ZERO) {
                StatRow("Verbleibend:", formatDuration(stats.remainingTime))
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, highlighted: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (highlighted) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = if (highlighted) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyLarge,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun formatDuration(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return "${hours}h ${minutes}min"
}
