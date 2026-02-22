package com.example.worktimetracker.domain.model

import java.time.LocalTime

/**
 * Configuration for BLE beacon scanning.
 *
 * @property uuid The UUID of the beacon to monitor
 * @property major Optional major version for more specific matching
 * @property minor Optional minor version for more specific matching
 * @property scanIntervalMs Time between scans in milliseconds
 * @property timeoutMinutes Minutes without beacon signal before triggering BeaconLost
 * @property validTimeWindow Time window during which scanning is active
 */
data class BeaconConfig(
    val uuid: String,
    val major: Int? = null,
    val minor: Int? = null,
    val scanIntervalMs: Long = 60_000,
    val timeoutMinutes: Int = 10,
    val validTimeWindow: TimeWindow = TimeWindow(
        start = LocalTime.of(6, 0),
        end = LocalTime.of(22, 0)
    ),
    val rssiThreshold: Int? = null
)
