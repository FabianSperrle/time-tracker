package com.example.worktimetracker.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.worktimetracker.domain.model.TimeWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to app settings stored in DataStore.
 */
@Singleton
class SettingsProvider @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    object Keys {
        val COMMUTE_DAYS = stringSetPreferencesKey("commute_days")
        val OUTBOUND_WINDOW_START = stringPreferencesKey("outbound_window_start")
        val OUTBOUND_WINDOW_END = stringPreferencesKey("outbound_window_end")
        val RETURN_WINDOW_START = stringPreferencesKey("return_window_start")
        val RETURN_WINDOW_END = stringPreferencesKey("return_window_end")
        val BEACON_UUID = stringPreferencesKey("beacon_uuid")
        val BEACON_TIMEOUT = intPreferencesKey("beacon_timeout")
        val BLE_SCAN_INTERVAL = longPreferencesKey("ble_scan_interval")
        val WORK_TIME_WINDOW_START = stringPreferencesKey("work_time_window_start")
        val WORK_TIME_WINDOW_END = stringPreferencesKey("work_time_window_end")
        val WEEKLY_TARGET_HOURS = floatPreferencesKey("weekly_target_hours")
        val BEACON_RSSI_THRESHOLD = intPreferencesKey("beacon_rssi_threshold")
    }

    /**
     * Flow of selected commute days.
     */
    val commuteDays: Flow<Set<DayOfWeek>> = dataStore.data.map { preferences ->
        preferences[Keys.COMMUTE_DAYS]
            ?.mapNotNull { dayName ->
                try {
                    DayOfWeek.valueOf(dayName)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            ?.toSet()
            ?: emptySet()
    }

    /**
     * Flow of outbound commute time window.
     */
    val outboundWindow: Flow<TimeWindow> = dataStore.data.map { preferences ->
        val start = preferences[Keys.OUTBOUND_WINDOW_START]?.let { LocalTime.parse(it) }
        val end = preferences[Keys.OUTBOUND_WINDOW_END]?.let { LocalTime.parse(it) }

        if (start != null && end != null) {
            TimeWindow(start, end)
        } else {
            TimeWindow.DEFAULT_OUTBOUND
        }
    }

    /**
     * Flow of return commute time window.
     */
    val returnWindow: Flow<TimeWindow> = dataStore.data.map { preferences ->
        val start = preferences[Keys.RETURN_WINDOW_START]?.let { LocalTime.parse(it) }
        val end = preferences[Keys.RETURN_WINDOW_END]?.let { LocalTime.parse(it) }

        if (start != null && end != null) {
            TimeWindow(start, end)
        } else {
            TimeWindow.DEFAULT_RETURN
        }
    }

    /**
     * Flow of beacon UUID. Null if not configured.
     */
    val beaconUuid: Flow<String?> = dataStore.data.map { preferences ->
        preferences[Keys.BEACON_UUID]
    }

    /**
     * Flow of beacon timeout in minutes.
     */
    val beaconTimeout: Flow<Int> = dataStore.data.map { preferences ->
        preferences[Keys.BEACON_TIMEOUT] ?: 10
    }

    /**
     * Flow of BLE scan interval in milliseconds.
     */
    val bleScanInterval: Flow<Long> = dataStore.data.map { preferences ->
        preferences[Keys.BLE_SCAN_INTERVAL] ?: 60000L
    }

    /**
     * Flow of work time window.
     */
    val workTimeWindow: Flow<TimeWindow> = dataStore.data.map { preferences ->
        val start = preferences[Keys.WORK_TIME_WINDOW_START]?.let { LocalTime.parse(it) }
        val end = preferences[Keys.WORK_TIME_WINDOW_END]?.let { LocalTime.parse(it) }

        if (start != null && end != null) {
            TimeWindow(start, end)
        } else {
            TimeWindow.DEFAULT_WORK_TIME
        }
    }

    /**
     * Flow of weekly target hours.
     */
    val weeklyTargetHours: Flow<Float> = dataStore.data.map { preferences ->
        preferences[Keys.WEEKLY_TARGET_HOURS] ?: 40f
    }

    /**
     * Flow of RSSI threshold for beacon proximity detection. Null if not calibrated.
     */
    val beaconRssiThreshold: Flow<Int?> = dataStore.data.map { preferences ->
        preferences[Keys.BEACON_RSSI_THRESHOLD]
    }

    /**
     * Sets the commute days.
     */
    suspend fun setCommuteDays(days: Set<DayOfWeek>) {
        dataStore.edit { preferences ->
            preferences[Keys.COMMUTE_DAYS] = days.map { it.name }.toSet()
        }
    }

    /**
     * Sets the outbound commute time window.
     */
    suspend fun setOutboundWindow(window: TimeWindow) {
        dataStore.edit { preferences ->
            preferences[Keys.OUTBOUND_WINDOW_START] = window.start.toString()
            preferences[Keys.OUTBOUND_WINDOW_END] = window.end.toString()
        }
    }

    /**
     * Sets the return commute time window.
     */
    suspend fun setReturnWindow(window: TimeWindow) {
        dataStore.edit { preferences ->
            preferences[Keys.RETURN_WINDOW_START] = window.start.toString()
            preferences[Keys.RETURN_WINDOW_END] = window.end.toString()
        }
    }

    /**
     * Sets the beacon UUID.
     */
    suspend fun setBeaconUuid(uuid: String?) {
        dataStore.edit { preferences ->
            if (uuid != null) {
                preferences[Keys.BEACON_UUID] = uuid
            } else {
                preferences.remove(Keys.BEACON_UUID)
            }
        }
    }

    /**
     * Sets the beacon timeout in minutes.
     */
    suspend fun setBeaconTimeout(minutes: Int) {
        require(minutes in 1..60) { "Beacon timeout must be between 1 and 60 minutes" }
        dataStore.edit { preferences ->
            preferences[Keys.BEACON_TIMEOUT] = minutes
        }
    }

    /**
     * Sets the BLE scan interval in seconds.
     */
    suspend fun setBleScanInterval(seconds: Int) {
        require(seconds in 10..300) { "Scan interval must be between 10 and 300 seconds" }
        dataStore.edit { preferences ->
            preferences[Keys.BLE_SCAN_INTERVAL] = seconds * 1000L
        }
    }

    /**
     * Sets the work time window.
     */
    suspend fun setWorkTimeWindow(window: TimeWindow) {
        dataStore.edit { preferences ->
            preferences[Keys.WORK_TIME_WINDOW_START] = window.start.toString()
            preferences[Keys.WORK_TIME_WINDOW_END] = window.end.toString()
        }
    }

    /**
     * Sets the weekly target hours.
     */
    suspend fun setWeeklyTargetHours(hours: Float) {
        require(hours in 0f..80f) { "Weekly target hours must be between 0 and 80" }
        dataStore.edit { preferences ->
            preferences[Keys.WEEKLY_TARGET_HOURS] = hours
        }
    }

    /**
     * Sets the RSSI threshold for beacon proximity detection. Null removes the threshold.
     */
    suspend fun setBeaconRssiThreshold(threshold: Int?) {
        dataStore.edit { preferences ->
            if (threshold != null) {
                preferences[Keys.BEACON_RSSI_THRESHOLD] = threshold
            } else {
                preferences.remove(Keys.BEACON_RSSI_THRESHOLD)
            }
        }
    }

    /**
     * Clears all settings.
     */
    suspend fun clearAllSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
