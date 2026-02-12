package com.example.worktimetracker.service

import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.di.BeaconScannerScope
import com.example.worktimetracker.domain.model.BeaconConfig
import com.example.worktimetracker.domain.model.TimeWindow
import com.example.worktimetracker.domain.tracking.TrackingEvent
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages BLE beacon scanning for presence detection.
 *
 * Scans for configured iBeacon and triggers tracking events based on
 * beacon presence/absence with a configurable timeout mechanism.
 */
@Singleton
class BeaconScanner @Inject constructor(
    private val beaconManager: BeaconManager,
    private val settingsProvider: SettingsProvider,
    private val stateMachine: TrackingStateMachine,
    @BeaconScannerScope private val scope: CoroutineScope
) {

    private var lastSeenTimestamp: Instant? = null
    var timeoutJob: Job? = null
        private set

    private var currentRegion: Region? = null
    private var currentConfig: BeaconConfig? = null

    /**
     * Retrieves the current beacon configuration from settings.
     */
    suspend fun getBeaconConfig(): BeaconConfig {
        val uuid = settingsProvider.beaconUuid.first() ?: throw IllegalStateException("Beacon UUID not configured")
        val scanInterval = settingsProvider.bleScanInterval.first()
        val timeout = settingsProvider.beaconTimeout.first()
        val timeWindow = settingsProvider.workTimeWindow.first()

        return BeaconConfig(
            uuid = uuid,
            scanIntervalMs = scanInterval,
            timeoutMinutes = timeout,
            validTimeWindow = timeWindow
        )
    }

    /**
     * Starts monitoring for the configured beacon.
     */
    suspend fun startMonitoring() {
        val config = getBeaconConfig()
        currentConfig = config

        val region = Region(
            "desk-beacon",
            Identifier.parse(config.uuid),
            null,
            null
        )
        currentRegion = region

        // Configure beacon manager
        beaconManager.apply {
            // Add iBeacon parser if not already present
            if (beaconParsers.isEmpty()) {
                beaconParsers.add(
                    BeaconParser()
                        .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
                )
            }

            foregroundScanPeriod = config.scanIntervalMs
            foregroundBetweenScanPeriod = config.scanIntervalMs
            backgroundScanPeriod = config.scanIntervalMs
            backgroundBetweenScanPeriod = config.scanIntervalMs
        }

        // Set up monitoring callbacks
        beaconManager.addMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region) {
                scope.launch {
                    onBeaconDetected()
                }
            }

            override fun didExitRegion(region: Region) {
                scope.launch {
                    onBeaconLostFromRegion()
                }
            }

            override fun didDetermineStateForRegion(state: Int, region: Region) {
                // Not used in this implementation
            }
        })

        // Start monitoring
        beaconManager.startMonitoring(region)
        beaconManager.startRangingBeacons(region)
    }

    /**
     * Stops monitoring for beacons.
     */
    fun stopMonitoring() {
        currentRegion?.let { region ->
            beaconManager.stopMonitoring(region)
            beaconManager.stopRangingBeacons(region)
        }
        timeoutJob?.cancel()
        timeoutJob = null
    }

    /**
     * Called when a beacon is detected.
     * Updates last seen timestamp and cancels timeout.
     */
    suspend fun onBeaconDetected() {
        val config = currentConfig ?: getBeaconConfig()

        lastSeenTimestamp = Instant.now()
        timeoutJob?.cancel()
        timeoutJob = null

        // Only trigger event if currently idle and within valid time window
        if (stateMachine.state.value is TrackingState.Idle && isInValidTimeWindow()) {
            stateMachine.processEvent(TrackingEvent.BeaconDetected(uuid = config.uuid))
        }
    }

    /**
     * Called when beacon exits region.
     * Starts timeout countdown before triggering BeaconLost event.
     */
    fun onBeaconLostFromRegion() {
        val config = currentConfig ?: return

        // Cancel any existing timeout job
        timeoutJob?.cancel()

        // Start new timeout countdown
        timeoutJob = scope.launch {
            delay(config.timeoutMinutes * 60_000L)
            stateMachine.processEvent(TrackingEvent.BeaconLost())
        }
    }

    /**
     * Checks if current time is within the valid scanning window.
     */
    private fun isInValidTimeWindow(): Boolean {
        val config = currentConfig ?: return false
        val now = LocalTime.now()
        return !now.isBefore(config.validTimeWindow.start) &&
                !now.isAfter(config.validTimeWindow.end)
    }

    /**
     * Returns the last timestamp when beacon was seen.
     * Used for end time correction in state machine.
     */
    fun getLastSeenTimestamp(): Instant? = lastSeenTimestamp
}
