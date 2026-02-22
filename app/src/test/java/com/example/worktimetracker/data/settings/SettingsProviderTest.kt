package com.example.worktimetracker.data.settings

import com.example.worktimetracker.domain.model.TimeWindow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/**
 * Unit tests for SettingsProvider.
 * Note: Full DataStore tests would require instrumented tests or FakeDataStore.
 * These tests verify the basic structure and constants.
 */
class SettingsProviderTest {

    @Test
    fun `TimeWindow DEFAULT_OUTBOUND is correct`() {
        assertEquals(LocalTime.of(6, 0), TimeWindow.DEFAULT_OUTBOUND.start)
        assertEquals(LocalTime.of(9, 30), TimeWindow.DEFAULT_OUTBOUND.end)
    }

    @Test
    fun `TimeWindow DEFAULT_RETURN is correct`() {
        assertEquals(LocalTime.of(16, 0), TimeWindow.DEFAULT_RETURN.start)
        assertEquals(LocalTime.of(20, 0), TimeWindow.DEFAULT_RETURN.end)
    }

    @Test
    fun `TimeWindow DEFAULT_WORK_TIME is correct`() {
        assertEquals(LocalTime.of(6, 0), TimeWindow.DEFAULT_WORK_TIME.start)
        assertEquals(LocalTime.of(22, 0), TimeWindow.DEFAULT_WORK_TIME.end)
    }

    @Test
    fun `SettingsProvider Keys are defined correctly`() {
        assertEquals("commute_days", SettingsProvider.Keys.COMMUTE_DAYS.name)
        assertEquals("outbound_window_start", SettingsProvider.Keys.OUTBOUND_WINDOW_START.name)
        assertEquals("beacon_uuid", SettingsProvider.Keys.BEACON_UUID.name)
        assertEquals("weekly_target_hours", SettingsProvider.Keys.WEEKLY_TARGET_HOURS.name)
    }

    @Test
    fun `BEACON_RSSI_THRESHOLD key has correct name`() {
        assertEquals("beacon_rssi_threshold", SettingsProvider.Keys.BEACON_RSSI_THRESHOLD.name)
    }
}
