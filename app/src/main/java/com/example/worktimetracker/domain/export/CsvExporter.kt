package com.example.worktimetracker.domain.export

import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.repository.TrackingRepository
import com.example.worktimetracker.di.CacheDirectory
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Exports tracking data to CSV format.
 *
 * Format:
 * - Separator: Semicolon (;)
 * - Encoding: UTF-8 with BOM
 * - Decimal separator: Dot (.)
 * - Decimal hours: 2 decimal places
 */
class CsvExporter @Inject constructor(
    private val repository: TrackingRepository,
    @CacheDirectory private val cacheDir: File
) {
    companion object {
        private const val SEPARATOR = ";"
        private const val UTF8_BOM = "\uFEFF"
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }

    /**
     * Exports tracking entries for the given date range to a CSV file.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return File containing the CSV data
     */
    suspend fun export(startDate: LocalDate, endDate: LocalDate): File {
        val entries = repository.getEntriesInRange(startDate, endDate).first()
        val filename = "arbeitszeit_${startDate}_${endDate}.csv"
        val file = File(cacheDir, filename)

        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            // Write UTF-8 BOM for Excel compatibility
            writer.write(UTF8_BOM)

            // Write header
            writer.write(buildHeader())
            writer.newLine()

            // Write data rows (sorted by date, skip incomplete entries)
            entries
                .filter { it.entry.endTime != null }
                .sortedBy { it.entry.date }
                .forEach { entry ->
                    writer.write(formatRow(entry))
                    writer.newLine()
                }
        }

        return file
    }

    private fun buildHeader(): String {
        return listOf(
            "Datum",
            "Wochentag",
            "Typ",
            "Startzeit",
            "Endzeit",
            "Brutto (h)",
            "Pausen (h)",
            "Netto (h)",
            "Notiz"
        ).joinToString(SEPARATOR)
    }

    private fun formatRow(entry: TrackingEntryWithPauses): String {
        val trackingEntry = entry.entry
        val date = trackingEntry.date
        val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
        val type = formatType(trackingEntry.type)
        val startTime = trackingEntry.startTime.format(TIME_FORMATTER)
        val endTime = trackingEntry.endTime?.format(TIME_FORMATTER) ?: ""

        // Calculate durations
        val grossDuration = if (trackingEntry.endTime != null) {
            Duration.between(trackingEntry.startTime, trackingEntry.endTime)
        } else {
            Duration.ZERO
        }

        val pauseDuration = entry.pauses
            .filter { it.endTime != null }
            .map { Duration.between(it.startTime, it.endTime!!) }
            .fold(Duration.ZERO) { acc, duration -> acc.plus(duration) }

        val netDuration = grossDuration.minus(pauseDuration)

        val grossHours = formatDecimalHours(grossDuration)
        val pauseHours = formatDecimalHours(pauseDuration)
        val netHours = formatDecimalHours(netDuration)

        val notes = trackingEntry.notes ?: ""

        return listOf(
            date.toString(),
            dayOfWeek,
            type,
            startTime,
            endTime,
            grossHours,
            pauseHours,
            netHours,
            notes
        ).joinToString(SEPARATOR) { escapeCsvField(it) }
    }

    private fun formatType(type: TrackingType): String {
        return when (type) {
            TrackingType.HOME_OFFICE -> "Home Office"
            TrackingType.COMMUTE_OFFICE -> "Büro (Pendel)"
            TrackingType.MANUAL -> "Manuell"
        }
    }

    private fun formatDecimalHours(duration: Duration): String {
        val totalMinutes = duration.toMinutes()
        val hours = totalMinutes / 60.0
        return String.format(Locale.US, "%.2f", hours)
    }

    /**
     * Escapes a CSV field according to RFC 4180.
     *
     * Fields containing semicolons, quotes, or newlines are enclosed in quotes.
     * Quotes within fields are escaped by doubling them.
     *
     * @param field The field value to escape
     * @return The escaped field value
     */
    private fun escapeCsvField(field: String): String {
        // Check if field needs quoting
        val needsQuoting = field.contains(SEPARATOR) ||
                          field.contains('"') ||
                          field.contains('\n') ||
                          field.contains('\r')

        if (!needsQuoting) {
            return field
        }

        // Escape quotes by doubling them
        val escaped = field.replace("\"", "\"\"")

        // Wrap in quotes
        return "\"$escaped\""
    }
}
