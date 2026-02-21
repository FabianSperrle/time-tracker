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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.domain.model.DaySummary
import com.example.worktimetracker.domain.model.MonthlyStats
import com.example.worktimetracker.domain.model.WeekStats
import com.example.worktimetracker.ui.viewmodel.WeekViewModel
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Week view dashboard screen.
 */
@Composable
fun WeekScreen(
    viewModel: WeekViewModel = hiltViewModel(),
    onDayClick: (LocalDate) -> Unit = {},
    onExportClick: () -> Unit = {}
) {
    val weekStart by viewModel.selectedWeekStart.collectAsState()
    val weekNumber by viewModel.weekNumber.collectAsState()
    val summaries by viewModel.weekSummaries.collectAsState()
    val stats by viewModel.weekStats.collectAsState()
    val hasUnconfirmed by viewModel.hasUnconfirmedEntries.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val allTimeSaldo by viewModel.allTimeSaldo.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        WeekNavigationHeader(
            weekNumber = weekNumber,
            weekStart = weekStart,
            onPreviousWeek = viewModel::previousWeek,
            onNextWeek = viewModel::nextWeek
        )

        Spacer(modifier = Modifier.height(16.dp))

        WeekStatsCard(stats = stats)

        Spacer(modifier = Modifier.height(16.dp))

        DailySummariesCard(
            summaries = summaries,
            onDayClick = onDayClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        AdditionalStatsCard(
            averagePerDay = stats.averagePerDay,
            overtime = stats.overtime
        )

        Spacer(modifier = Modifier.height(16.dp))

        MonthlyAndAllTimeCard(
            monthlyStats = monthlyStats,
            allTimeSaldo = allTimeSaldo
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (monthlyStats.typeBreakdown.isNotEmpty()) {
            TypeBreakdownCard(monthlyStats = monthlyStats)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onExportClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CSV exportieren")
        }

        if (hasUnconfirmed) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚠️ Einige Einträge sind noch nicht bestätigt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WeekNavigationHeader(
    weekNumber: Int,
    weekStart: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    val weekEnd = weekStart.plusDays(4)
    val dateFormatter = DateTimeFormatter.ofPattern("dd. MMM yyyy", Locale.GERMAN)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousWeek) {
            Text("◀", style = MaterialTheme.typography.titleLarge)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "KW $weekNumber",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${weekStart.format(dateFormatter)} – ${weekEnd.format(dateFormatter)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onNextWeek) {
            Text("▶", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun WeekStatsCard(stats: WeekStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Gesamt:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${formatDuration(stats.totalDuration)} / ${formatDuration(stats.targetDuration)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (stats.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${String.format("%.1f", stats.percentage)}%",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun DailySummariesCard(
    summaries: List<DaySummary>,
    onDayClick: (LocalDate) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            summaries.forEach { summary ->
                DayRow(
                    summary = summary,
                    onClick = { onDayClick(summary.date) }
                )
                if (summary != summaries.last()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun DayRow(
    summary: DaySummary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Column {
                Text(
                    text = summary.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = summary.date.format(DateTimeFormatter.ofPattern("dd.MM.")),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            summary.types.forEach { type ->
                val icon = when (type) {
                    TrackingType.COMMUTE_OFFICE -> "🏢"
                    TrackingType.HOME_OFFICE -> "🏠"
                    TrackingType.MANUAL -> "✋"
                }
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (summary.types.isEmpty()) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!summary.confirmed && summary.type != null) {
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Text(
                text = if (summary.netDuration > Duration.ZERO) {
                    formatDuration(summary.netDuration)
                } else {
                    "—"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AdditionalStatsCard(
    averagePerDay: Duration,
    overtime: Duration
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ø pro Tag:",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formatDuration(averagePerDay),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Überstunden:",
                    style = MaterialTheme.typography.bodyLarge
                )
                val overtimeText = if (overtime.isNegative) {
                    "−${formatDuration(overtime.abs())}"
                } else {
                    "+${formatDuration(overtime)}"
                }
                Text(
                    text = overtimeText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        overtime.isNegative -> MaterialTheme.colorScheme.error
                        overtime > Duration.ZERO -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun MonthlyAndAllTimeCard(
    monthlyStats: MonthlyStats,
    allTimeSaldo: Duration
) {
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)

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
                text = monthlyStats.month.atDay(1).format(monthFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Monatssaldo:", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = formatSaldo(monthlyStats.saldo),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = saldoColor(monthlyStats.saldo)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Gesamtsaldo:", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = formatSaldo(allTimeSaldo),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = saldoColor(allTimeSaldo)
                )
            }
        }
    }
}

@Composable
private fun TypeBreakdownCard(monthlyStats: MonthlyStats) {
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)

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
                text = "Arbeitstypen – ${monthlyStats.month.atDay(1).format(monthFormatter)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            monthlyStats.typeBreakdown.forEach { typeHours ->
                val icon = when (typeHours.type) {
                    TrackingType.COMMUTE_OFFICE -> "🏢"
                    TrackingType.HOME_OFFICE -> "🏠"
                    TrackingType.MANUAL -> "✋"
                }
                val label = when (typeHours.type) {
                    TrackingType.COMMUTE_OFFICE -> "Büro"
                    TrackingType.HOME_OFFICE -> "Home Office"
                    TrackingType.MANUAL -> "Manuell"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$icon $label",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${formatDuration(typeHours.duration)}  ${typeHours.percentage.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun saldoColor(saldo: Duration) = when {
    saldo.isNegative -> MaterialTheme.colorScheme.error
    saldo > Duration.ZERO -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurface
}

private fun formatSaldo(duration: Duration): String {
    val abs = duration.abs()
    val hours = abs.toHours()
    val minutes = abs.toMinutes() % 60
    val sign = if (duration.isNegative) "−" else "+"
    return "$sign${hours}h ${minutes.toString().padStart(2, '0')}min"
}

private fun formatDuration(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return "${hours}h ${minutes.toString().padStart(2, '0')}min"
}
