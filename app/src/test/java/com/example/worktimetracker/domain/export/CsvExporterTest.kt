package com.example.worktimetracker.domain.export

import com.example.worktimetracker.data.local.entity.Pause
import com.example.worktimetracker.data.local.entity.TrackingEntry
import com.example.worktimetracker.data.local.entity.TrackingEntryWithPauses
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.repository.TrackingRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class CsvExporterTest {

    private lateinit var repository: TrackingRepository
    private lateinit var exporter: CsvExporter

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        repository = mockk()
        exporter = CsvExporter(repository, tempDir)
    }

    @Test
    fun `export creates CSV file with correct name`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 10)
        val endDate = LocalDate.of(2026, 2, 14)
        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(emptyList())

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        assertTrue(file.exists())
        assertEquals("arbeitszeit_2026-02-10_2026-02-14.csv", file.name)
    }

    @Test
    fun `export generates CSV with UTF-8 BOM`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 10)
        val endDate = LocalDate.of(2026, 2, 10)
        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(emptyList())

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        val content = file.readText(Charsets.UTF_8)
        assertTrue(content.startsWith("\uFEFF"), "CSV should start with UTF-8 BOM")
    }

    @Test
    fun `export generates correct header row`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 10)
        val endDate = LocalDate.of(2026, 2, 10)
        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(emptyList())

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        val lines = file.readLines(Charsets.UTF_8)
        val header = lines[0].removePrefix("\uFEFF")
        assertEquals("Datum;Wochentag;Typ;Startzeit;Endzeit;Brutto (h);Pausen (h);Netto (h);Notiz", header)
    }

    @Test
    fun `export formats home office entry correctly`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 10)
        val endDate = LocalDate.of(2026, 2, 10)
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 10),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 15),
            endTime = LocalDateTime.of(2026, 2, 10, 16, 37),
            autoDetected = true,
            confirmed = true,
            notes = null
        )
        val pause = Pause(
            id = "p1",
            entryId = "1",
            startTime = LocalDateTime.of(2026, 2, 10, 12, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 12, 30)
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, listOf(pause))

        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(listOf(entryWithPauses))

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        val lines = file.readLines(Charsets.UTF_8)
        assertEquals(2, lines.size) // Header + 1 entry
        val row = lines[1]
        assertEquals("2026-02-10;Montag;Home Office;08:15;16:37;8.37;0.50;7.87;", row)
    }

    @Test
    fun `export formats commute office entry correctly`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 11)
        val endDate = LocalDate.of(2026, 2, 11)
        val entry = TrackingEntry(
            id = "2",
            date = LocalDate.of(2026, 2, 11),
            type = TrackingType.COMMUTE_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 11, 7, 45),
            endTime = LocalDateTime.of(2026, 2, 11, 16, 32),
            autoDetected = true,
            confirmed = true,
            notes = "Teammeeting"
        )
        val pause = Pause(
            id = "p2",
            entryId = "2",
            startTime = LocalDateTime.of(2026, 2, 11, 12, 0),
            endTime = LocalDateTime.of(2026, 2, 11, 12, 30)
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, listOf(pause))

        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(listOf(entryWithPauses))

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        val lines = file.readLines(Charsets.UTF_8)
        val row = lines[1]
        assertEquals("2026-02-11;Dienstag;Büro (Pendel);07:45;16:32;8.78;0.50;8.28;Teammeeting", row)
    }

    @Test
    fun `export formats manual entry correctly`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 12)
        val endDate = LocalDate.of(2026, 2, 12)
        val entry = TrackingEntry(
            id = "3",
            date = LocalDate.of(2026, 2, 12),
            type = TrackingType.MANUAL,
            startTime = LocalDateTime.of(2026, 2, 12, 9, 0),
            endTime = LocalDateTime.of(2026, 2, 12, 17, 0),
            autoDetected = false,
            confirmed = true,
            notes = null
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, emptyList())

        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(listOf(entryWithPauses))

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        val lines = file.readLines(Charsets.UTF_8)
        val row = lines[1]
        assertEquals("2026-02-12;Mittwoch;Manuell;09:00;17:00;8.00;0.00;8.00;", row)
    }

    @Test
    fun `export calculates decimal hours correctly`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 10)
        val endDate = LocalDate.of(2026, 2, 10)
        // 8h 22min gross = 8.37h
        // 30min pause = 0.50h
        // 7h 52min net = 7.87h
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 10),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 15),
            endTime = LocalDateTime.of(2026, 2, 10, 16, 37),
            autoDetected = true,
            confirmed = true
        )
        val pause = Pause(
            id = "p1",
            entryId = "1",
            startTime = LocalDateTime.of(2026, 2, 10, 12, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 12, 30)
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, listOf(pause))

        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(listOf(entryWithPauses))

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        val lines = file.readLines(Charsets.UTF_8)
        val row = lines[1]
        val columns = row.split(";")
        assertEquals("8.37", columns[5]) // Brutto
        assertEquals("0.50", columns[6]) // Pausen
        assertEquals("7.87", columns[7]) // Netto
    }

    @Test
    fun `export handles multiple pauses correctly`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 10)
        val endDate = LocalDate.of(2026, 2, 10)
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 10),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 17, 0),
            autoDetected = true,
            confirmed = true
        )
        // 30min + 15min = 45min = 0.75h total pauses
        val pause1 = Pause(
            id = "p1",
            entryId = "1",
            startTime = LocalDateTime.of(2026, 2, 10, 12, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 12, 30)
        )
        val pause2 = Pause(
            id = "p2",
            entryId = "1",
            startTime = LocalDateTime.of(2026, 2, 10, 15, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 15, 15)
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, listOf(pause1, pause2))

        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(listOf(entryWithPauses))

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        val lines = file.readLines(Charsets.UTF_8)
        val row = lines[1]
        val columns = row.split(";")
        assertEquals("9.00", columns[5]) // Brutto: 9h
        assertEquals("0.75", columns[6]) // Pausen: 45min
        assertEquals("8.25", columns[7]) // Netto: 8h 15min
    }

    @Test
    fun `export handles empty notes field correctly`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 10)
        val endDate = LocalDate.of(2026, 2, 10)
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 10),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 17, 0),
            autoDetected = true,
            confirmed = true,
            notes = null
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, emptyList())

        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(listOf(entryWithPauses))

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        val lines = file.readLines(Charsets.UTF_8)
        val row = lines[1]
        assertTrue(row.endsWith(";"), "Empty notes should result in trailing semicolon")
    }

    @Test
    fun `export handles notes with special characters correctly`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 10)
        val endDate = LocalDate.of(2026, 2, 10)
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 10),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 17, 0),
            autoDetected = true,
            confirmed = true,
            notes = "Meeting; Präsentation, \"wichtig\""
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, emptyList())

        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(listOf(entryWithPauses))

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        val lines = file.readLines(Charsets.UTF_8)
        val row = lines[1]
        // Parse CSV properly - should have exactly 9 fields
        val columns = parseCsvRow(row)
        assertEquals(9, columns.size, "CSV row should have exactly 9 fields")
        // Notes field should contain the original text (after unescaping)
        assertEquals("Meeting; Präsentation, \"wichtig\"", columns[8])
    }

    @Test
    fun `export escapes semicolons in notes`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 10)
        val endDate = LocalDate.of(2026, 2, 10)
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 10),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 17, 0),
            autoDetected = true,
            confirmed = true,
            notes = "Meeting; Konferenz"
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, emptyList())

        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(listOf(entryWithPauses))

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        val lines = file.readLines(Charsets.UTF_8)
        val row = lines[1]
        val columns = parseCsvRow(row)
        assertEquals(9, columns.size, "Row with semicolon in notes should still have 9 fields")
        assertEquals("Meeting; Konferenz", columns[8])
    }

    @Test
    fun `export escapes quotes in notes`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 10)
        val endDate = LocalDate.of(2026, 2, 10)
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 10),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 17, 0),
            autoDetected = true,
            confirmed = true,
            notes = "Er sagte \"Hallo\""
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, emptyList())

        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(listOf(entryWithPauses))

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        val lines = file.readLines(Charsets.UTF_8)
        val row = lines[1]
        val columns = parseCsvRow(row)
        assertEquals(9, columns.size)
        assertEquals("Er sagte \"Hallo\"", columns[8])
    }

    @Test
    fun `export escapes newlines in notes`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 10)
        val endDate = LocalDate.of(2026, 2, 10)
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 10),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 17, 0),
            autoDetected = true,
            confirmed = true,
            notes = "Zeile 1\nZeile 2"
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, emptyList())

        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(listOf(entryWithPauses))

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        val content = file.readText(Charsets.UTF_8)
        val lines = content.split("\n")
        // Should still be 2 lines: header + 1 entry (newline in notes should be escaped)
        assertEquals(2, lines.size)
        val row = lines[1]
        val columns = parseCsvRow(row)
        assertEquals(9, columns.size)
        assertEquals("Zeile 1\nZeile 2", columns[8])
    }

    /**
     * Simple RFC 4180 compliant CSV parser for testing.
     * Handles quoted fields with semicolons, quotes, and newlines.
     */
    private fun parseCsvRow(row: String): List<String> {
        val fields = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < row.length) {
            val char = row[i]

            when {
                // Handle quote characters
                char == '"' -> {
                    if (inQuotes) {
                        // Check if it's an escaped quote (double quote)
                        if (i + 1 < row.length && row[i + 1] == '"') {
                            currentField.append('"')
                            i++ // Skip next quote
                        } else {
                            // End of quoted field
                            inQuotes = false
                        }
                    } else {
                        // Start of quoted field
                        inQuotes = true
                    }
                }
                // Handle field separator
                char == ';' && !inQuotes -> {
                    fields.add(currentField.toString())
                    currentField.clear()
                }
                // Regular character
                else -> {
                    currentField.append(char)
                }
            }
            i++
        }

        // Add the last field
        fields.add(currentField.toString())

        return fields
    }

    @Test
    fun `export sorts entries by date ascending`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 10)
        val endDate = LocalDate.of(2026, 2, 12)
        val entry1 = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 12),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 12, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 12, 17, 0),
            autoDetected = true,
            confirmed = true
        )
        val entry2 = TrackingEntry(
            id = "2",
            date = LocalDate.of(2026, 2, 10),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 10, 17, 0),
            autoDetected = true,
            confirmed = true
        )
        val entry3 = TrackingEntry(
            id = "3",
            date = LocalDate.of(2026, 2, 11),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 11, 8, 0),
            endTime = LocalDateTime.of(2026, 2, 11, 17, 0),
            autoDetected = true,
            confirmed = true
        )

        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(
            listOf(
                TrackingEntryWithPauses(entry1, emptyList()),
                TrackingEntryWithPauses(entry2, emptyList()),
                TrackingEntryWithPauses(entry3, emptyList())
            )
        )

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert
        val lines = file.readLines(Charsets.UTF_8)
        assertEquals(4, lines.size) // Header + 3 entries
        assertTrue(lines[1].startsWith("2026-02-10"))
        assertTrue(lines[2].startsWith("2026-02-11"))
        assertTrue(lines[3].startsWith("2026-02-12"))
    }

    @Test
    fun `export handles incomplete entry without endTime`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2026, 2, 10)
        val endDate = LocalDate.of(2026, 2, 10)
        val entry = TrackingEntry(
            id = "1",
            date = LocalDate.of(2026, 2, 10),
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.of(2026, 2, 10, 8, 0),
            endTime = null, // Still tracking
            autoDetected = true,
            confirmed = false
        )
        val entryWithPauses = TrackingEntryWithPauses(entry, emptyList())

        coEvery { repository.getEntriesInRange(startDate, endDate) } returns kotlinx.coroutines.flow.flowOf(listOf(entryWithPauses))

        // Act
        val file = exporter.export(startDate, endDate)

        // Assert - Should be skipped or handled gracefully
        val lines = file.readLines(Charsets.UTF_8)
        // Incomplete entries should be skipped
        assertEquals(1, lines.size) // Only header
    }
}
