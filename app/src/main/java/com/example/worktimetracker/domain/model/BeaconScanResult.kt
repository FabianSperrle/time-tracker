package com.example.worktimetracker.domain.model

/**
 * Represents a scanned Bluetooth beacon with signal strength information.
 *
 * @property uuid The UUID of the beacon
 * @property rssi Signal strength in dBm (e.g. -65)
 * @property distance Estimated distance in meters
 * @property isConfigured Whether this beacon matches the configured UUID
 */
data class BeaconScanResult(
    val uuid: String,
    val rssi: Int,
    val distance: Double,
    val isConfigured: Boolean
)
