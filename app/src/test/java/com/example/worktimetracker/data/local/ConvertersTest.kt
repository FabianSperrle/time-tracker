package com.example.worktimetracker.data.local

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Test for Room type converters.
 */
class ConvertersTest {

    private lateinit var converters: Converters

    @BeforeEach
    fun setup() {
        converters = Converters()
    }

    @Test
    fun `fromTimestamp converts valid timestamp to Instant`() {
        val timestamp = 1640000000000L
        val expected = Instant.ofEpochMilli(timestamp)

        val result = converters.fromTimestamp(timestamp)

        assertEquals(expected, result)
    }

    @Test
    fun `fromTimestamp returns null for null input`() {
        val result = converters.fromTimestamp(null)
        assertNull(result)
    }

    @Test
    fun `instantToTimestamp converts Instant to timestamp`() {
        val instant = Instant.ofEpochMilli(1640000000000L)
        val expected = 1640000000000L

        val result = converters.instantToTimestamp(instant)

        assertEquals(expected, result)
    }

    @Test
    fun `instantToTimestamp returns null for null input`() {
        val result = converters.instantToTimestamp(null)
        assertNull(result)
    }

    @Test
    fun `timestamp conversion is reversible`() {
        // Use epoch milli to avoid precision loss from nanoseconds
        val original = Instant.ofEpochMilli(System.currentTimeMillis())
        val timestamp = converters.instantToTimestamp(original)
        val converted = converters.fromTimestamp(timestamp)

        assertEquals(original, converted)
    }

    @Test
    fun `LocalDate to String conversion`() {
        val date = LocalDate.of(2026, 2, 11)
        val result = converters.localDateToString(date)
        assertEquals("2026-02-11", result)
    }

    @Test
    fun `null LocalDate to String conversion`() {
        val result = converters.localDateToString(null)
        assertNull(result)
    }

    @Test
    fun `String to LocalDate conversion`() {
        val dateString = "2026-02-11"
        val result = converters.stringToLocalDate(dateString)
        assertEquals(LocalDate.of(2026, 2, 11), result)
    }

    @Test
    fun `null String to LocalDate conversion`() {
        val result = converters.stringToLocalDate(null)
        assertNull(result)
    }

    @Test
    fun `LocalDateTime to String conversion`() {
        val dateTime = LocalDateTime.of(2026, 2, 11, 14, 30, 0)
        val result = converters.localDateTimeToString(dateTime)
        assertEquals("2026-02-11T14:30:00", result)
    }

    @Test
    fun `null LocalDateTime to String conversion`() {
        val result = converters.localDateTimeToString(null)
        assertNull(result)
    }

    @Test
    fun `String to LocalDateTime conversion`() {
        val dateTimeString = "2026-02-11T14:30:00"
        val result = converters.stringToLocalDateTime(dateTimeString)
        assertEquals(LocalDateTime.of(2026, 2, 11, 14, 30, 0), result)
    }

    @Test
    fun `null String to LocalDateTime conversion`() {
        val result = converters.stringToLocalDateTime(null)
        assertNull(result)
    }

    @Test
    fun `LocalDate roundtrip conversion`() {
        val original = LocalDate.of(2026, 2, 11)
        val string = converters.localDateToString(original)
        val result = converters.stringToLocalDate(string)
        assertEquals(original, result)
    }

    @Test
    fun `LocalDateTime roundtrip conversion`() {
        val original = LocalDateTime.of(2026, 2, 11, 14, 30, 45)
        val string = converters.localDateTimeToString(original)
        val result = converters.stringToLocalDateTime(string)
        assertEquals(original, result)
    }
}
