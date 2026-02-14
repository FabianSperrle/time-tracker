package com.example.worktimetracker.ui.viewmodel

import app.cash.turbine.test
import com.example.worktimetracker.domain.export.CsvExporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {

    private lateinit var csvExporter: CsvExporter
    private lateinit var viewModel: ExportViewModel

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        csvExporter = mockk()
        viewModel = ExportViewModel(csvExporter)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has this week selected`() = runTest {
        viewModel.selectedRange.test {
            val range = awaitItem()
            assertEquals(ExportRange.THIS_WEEK, range)
        }
    }

    @Test
    fun `this week range calculates Monday to Friday`() = runTest {
        // Arrange
        viewModel.selectRange(ExportRange.THIS_WEEK)

        // Act & Assert
        viewModel.dateRange.test {
            val (start, end) = awaitItem()
            assertEquals(DayOfWeek.MONDAY, start.dayOfWeek)
            assertEquals(DayOfWeek.FRIDAY, end.dayOfWeek)
            assertTrue(end.isAfter(start) || end.isEqual(start))
        }
    }

    @Test
    fun `last month range calculates first and last day of previous month`() = runTest {
        // Arrange
        viewModel.selectRange(ExportRange.LAST_MONTH)

        // Act & Assert
        viewModel.dateRange.test {
            val (start, end) = awaitItem()
            val now = LocalDate.now()
            val lastMonth = now.minusMonths(1)
            assertEquals(1, start.dayOfMonth)
            assertEquals(lastMonth.month, start.month)
            assertEquals(lastMonth.lengthOfMonth(), end.dayOfMonth)
            assertEquals(lastMonth.month, end.month)
        }
    }

    @Test
    fun `custom range uses provided dates`() = runTest {
        // Arrange
        val customStart = LocalDate.of(2026, 1, 10)
        val customEnd = LocalDate.of(2026, 1, 20)
        viewModel.selectRange(ExportRange.CUSTOM)
        viewModel.setCustomStartDate(customStart)
        viewModel.setCustomEndDate(customEnd)

        // Act & Assert
        viewModel.dateRange.test {
            val (start, end) = awaitItem()
            assertEquals(customStart, start)
            assertEquals(customEnd, end)
        }
    }

    @Test
    fun `export success triggers file ready state`() = runTest {
        // Arrange
        val mockFile = mockk<File>()
        coEvery { csvExporter.export(any(), any()) } returns mockFile

        // Act
        viewModel.export()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        viewModel.exportState.test {
            val state = awaitItem()
            assertTrue(state is ExportState.Success)
            assertEquals(mockFile, (state as ExportState.Success).file)
        }
    }

    @Test
    fun `export error triggers error state`() = runTest {
        // Arrange
        val errorMessage = "Export failed"
        coEvery { csvExporter.export(any(), any()) } throws Exception(errorMessage)

        // Act
        viewModel.export()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        viewModel.exportState.test {
            val state = awaitItem()
            assertTrue(state is ExportState.Error)
            assertEquals(errorMessage, (state as ExportState.Error).message)
        }
    }

    @Test
    fun `export loading state is shown during export`() = runTest {
        // Arrange
        val mockFile = mockk<File>()
        coEvery { csvExporter.export(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            mockFile
        }

        // Act
        viewModel.exportState.test {
            // Initial state
            assertEquals(ExportState.Idle, awaitItem())

            viewModel.export()

            // Should show loading
            assertEquals(ExportState.Loading, awaitItem())
        }
    }

    @Test
    fun `dismiss clears export state`() = runTest {
        // Arrange
        val mockFile = mockk<File>()
        coEvery { csvExporter.export(any(), any()) } returns mockFile
        viewModel.export()
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.dismissExport()

        // Assert
        viewModel.exportState.test {
            assertEquals(ExportState.Idle, awaitItem())
        }
    }

    @Test
    fun `export is called with correct date range`() = runTest {
        // Arrange
        val customStart = LocalDate.of(2026, 2, 10)
        val customEnd = LocalDate.of(2026, 2, 14)
        viewModel.selectRange(ExportRange.CUSTOM)
        viewModel.setCustomStartDate(customStart)
        viewModel.setCustomEndDate(customEnd)

        val mockFile = mockk<File>()
        coEvery { csvExporter.export(customStart, customEnd) } returns mockFile

        // Act
        viewModel.export()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify { csvExporter.export(customStart, customEnd) }
    }
}
