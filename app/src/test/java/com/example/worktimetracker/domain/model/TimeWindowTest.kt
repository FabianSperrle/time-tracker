package com.example.worktimetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalTime

class TimeWindowTest {

    @Test
    fun `create valid time window`() {
        val window = TimeWindow(
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0)
        )

        assertEquals(LocalTime.of(9, 0), window.start)
        assertEquals(LocalTime.of(17, 0), window.end)
    }

    @Test
    fun `throws exception when start is after end`() {
        assertThrows(IllegalArgumentException::class.java) {
            TimeWindow(
                start = LocalTime.of(17, 0),
                end = LocalTime.of(9, 0)
            )
        }
    }

    @Test
    fun `throws exception when start equals end`() {
        assertThrows(IllegalArgumentException::class.java) {
            TimeWindow(
                start = LocalTime.of(9, 0),
                end = LocalTime.of(9, 0)
            )
        }
    }

    @Test
    fun `format returns correct string`() {
        val window = TimeWindow(
            start = LocalTime.of(6, 0),
            end = LocalTime.of(9, 30)
        )

        assertEquals("06:00–09:30", window.format())
    }

    @Test
    fun `default work time window is correct`() {
        val window = TimeWindow.DEFAULT_WORK_TIME

        assertEquals(LocalTime.of(6, 0), window.start)
        assertEquals(LocalTime.of(22, 0), window.end)
    }

    @Test
    fun `default outbound window is correct`() {
        val window = TimeWindow.DEFAULT_OUTBOUND

        assertEquals(LocalTime.of(6, 0), window.start)
        assertEquals(LocalTime.of(9, 30), window.end)
    }

    @Test
    fun `default return window is correct`() {
        val window = TimeWindow.DEFAULT_RETURN

        assertEquals(LocalTime.of(16, 0), window.start)
        assertEquals(LocalTime.of(20, 0), window.end)
    }
}
