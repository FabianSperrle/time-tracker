package com.example.worktimetracker.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalTime

class BeaconConfigTest {

    @Test
    fun `BeaconConfig should have correct default values`() {
        // Given
        val uuid = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"

        // When
        val config = BeaconConfig(uuid = uuid)

        // Then
        assertEquals(uuid, config.uuid)
        assertNull(config.major)
        assertNull(config.minor)
        assertEquals(60_000L, config.scanIntervalMs)
        assertEquals(10, config.timeoutMinutes)
        assertEquals(LocalTime.of(6, 0), config.validTimeWindow.start)
        assertEquals(LocalTime.of(22, 0), config.validTimeWindow.end)
    }

    @Test
    fun `BeaconConfig should allow setting major and minor`() {
        // Given
        val uuid = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
        val major = 100
        val minor = 200

        // When
        val config = BeaconConfig(
            uuid = uuid,
            major = major,
            minor = minor
        )

        // Then
        assertEquals(major, config.major)
        assertEquals(minor, config.minor)
    }

    @Test
    fun `BeaconConfig should allow custom scan interval`() {
        // Given
        val uuid = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
        val scanInterval = 30_000L

        // When
        val config = BeaconConfig(
            uuid = uuid,
            scanIntervalMs = scanInterval
        )

        // Then
        assertEquals(scanInterval, config.scanIntervalMs)
    }

    @Test
    fun `BeaconConfig should allow custom timeout`() {
        // Given
        val uuid = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
        val timeout = 15

        // When
        val config = BeaconConfig(
            uuid = uuid,
            timeoutMinutes = timeout
        )

        // Then
        assertEquals(timeout, config.timeoutMinutes)
    }

    @Test
    fun `BeaconConfig should allow custom time window`() {
        // Given
        val uuid = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
        val timeWindow = TimeWindow(
            start = LocalTime.of(7, 0),
            end = LocalTime.of(20, 0)
        )

        // When
        val config = BeaconConfig(
            uuid = uuid,
            validTimeWindow = timeWindow
        )

        // Then
        assertEquals(timeWindow, config.validTimeWindow)
    }
}
