package com.example.worktimetracker.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * Test for Room type converters.
 */
class ConvertersTest {

    private val converters = Converters()

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
}
