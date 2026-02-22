package com.example.worktimetracker.service

import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.di.BeaconScannerScope
import com.example.worktimetracker.domain.homeoffice.HomeOfficeTracker
import com.example.worktimetracker.domain.model.BeaconConfig
import com.example.worktimetracker.domain.model.BeaconScanResult
import com.example.worktimetracker.domain.model.TimeWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages BLE beacon scanning for presence detection.
 *
 * Scans for configured iBeacon and triggers tracking events based on
 * beacon presence/absence with a configurable timeout mechanism.
 *
 * Scanning is only active within the configured time window (default 06:00-22:00).
 * Outside the window, the scanner is stopped to save battery.
 */
@Singleton
class BeaconScanner @Inject constructor(
    private val beaconManager: BeaconManager,
    private val settingsProvider: SettingsProvider,
    private val homeOfficeTracker: HomeOfficeTracker,
    @BeaconScannerScope private val scope: CoroutineScope
) {

    private var lastSeenTimestamp: Instant? = null
    var timeoutJob: Job? = null
        private set

    private var currentRegion: Region? = null
    private var currentConfig: BeaconConfig? = null
    private var scheduleJob: Job? = null
    var isMonitoringActive: Boolean = false
        private set

    // --- Test scanning (Feature 1) ---

    private val _scanResults = MutableStateFlow<List<BeaconScanResult>>(emptyList())
    val scanResults: StateFlow<List<BeaconScanResult>> = _scanResults.asStateFlow()

    private var testRegion: Region? = null
    private var testRangeNotifier: RangeNotifier? = null

    // --- RSSI threshold detection (Feature 2 runtime) ---

    internal var isBeaconInRssiRange: Boolean = false
    private var rssiRangeNotifier: RangeNotifier? = null

    /**
     * Retrieves the current beacon configuration from settings.
     */
    suspend fun getBeaconConfig(): BeaconConfig {
        val uuid = settingsProvider.beaconUuid.first() ?: throw IllegalStateException("Beacon UUID not configured")
        val scanInterval = settingsProvider.bleScanInterval.first()
        val timeout = settingsProvider.beaconTimeout.first()
        val timeWindow = settingsProvider.workTimeWindow.first()
        val rssiThreshold = settingsProvider.beaconRssiThreshold.first()

        return BeaconConfig(
            uuid = uuid,
            scanIntervalMs = scanInterval,
            timeoutMinutes = timeout,
            validTimeWindow = timeWindow,
            rssiThreshold = rssiThreshold
        )
    }

    /**
     * Starts monitoring for the configured beacon.
     * Configures BeaconManager, sets up callbacks, and begins region monitoring.
     */
    suspend fun startMonitoring() {
        if (isMonitoringActive) return

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

        if (config.rssiThreshold != null) {
            // RSSI-threshold mode: use RangeNotifier with edge-triggered logic
            val threshold = config.rssiThreshold
            val notifier = object : RangeNotifier {
                override fun didRangeBeaconsInRegion(beacons: Collection<Beacon>, region: Region) {
                    val nowInRange = beacons.any { b ->
                        b.id1.toString().equals(config.uuid, ignoreCase = true) && b.rssi >= threshold
                    }
                    applyRssiRanging(nowInRange)
                }
            }
            rssiRangeNotifier = notifier
            beaconManager.addRangeNotifier(notifier)
            beaconManager.startRangingBeacons(region)
        } else {
            // Standard mode: use MonitorNotifier
            beaconManager.addMonitorNotifier(object : MonitorNotifier {
                override fun didEnterRegion(region: Region) {
                    scope.launch {
                        try {
                            onBeaconDetected()
                        } catch (e: Exception) {
                            // Gracefully handle errors (e.g. config not available)
                        }
                    }
                }

                override fun didExitRegion(region: Region) {
                    scope.launch {
                        try {
                            onBeaconLostFromRegion()
                        } catch (e: Exception) {
                            // Gracefully handle errors
                        }
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

        isMonitoringActive = true
    }

    /**
     * Applies edge-triggered RSSI ranging logic.
     * Calls onBeaconDetected on false→true transition, onBeaconLostFromRegion on true→false.
     * Internal visibility for unit testing.
     */
    internal fun applyRssiRanging(nowInRange: Boolean) {
        if (nowInRange && !isBeaconInRssiRange) {
            isBeaconInRssiRange = true
            scope.launch {
                try {
                    onBeaconDetected()
                } catch (e: Exception) {
                    // Gracefully handle errors
                }
            }
        } else if (!nowInRange && isBeaconInRssiRange) {
            isBeaconInRssiRange = false
            scope.launch {
                try {
                    onBeaconLostFromRegion()
                } catch (e: Exception) {
                    // Gracefully handle errors
                }
            }
        }
    }

    /**
     * Stops monitoring for beacons and resets all state.
     */
    fun stopMonitoring() {
        currentRegion?.let { region ->
            beaconManager.stopMonitoring(region)
            beaconManager.stopRangingBeacons(region)
        }
        rssiRangeNotifier?.let { beaconManager.removeRangeNotifier(it) }
        rssiRangeNotifier = null
        isBeaconInRssiRange = false
        currentRegion = null
        currentConfig = null
        timeoutJob?.cancel()
        timeoutJob = null
        lastSeenTimestamp = null
        isMonitoringActive = false
    }

    /**
     * Starts scanning all nearby beacons for test/discovery purposes.
     * Results are emitted via [scanResults].
     */
    fun startBeaconTest() {
        val wildcardRegion = Region("beacon-test-wildcard", null, null, null)
        testRegion = wildcardRegion

        if (beaconManager.beaconParsers.isEmpty()) {
            beaconManager.beaconParsers.add(
                BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
            )
        }

        val notifier = object : RangeNotifier {
            override fun didRangeBeaconsInRegion(beacons: Collection<Beacon>, region: Region) {
                scope.launch {
                    val currentConfiguredUuid = settingsProvider.beaconUuid.first()
                    val results = beacons.map { beacon ->
                        BeaconScanResult(
                            uuid = beacon.id1.toString(),
                            rssi = beacon.rssi,
                            distance = beacon.distance,
                            isConfigured = currentConfiguredUuid != null &&
                                beacon.id1.toString().equals(currentConfiguredUuid, ignoreCase = true)
                        )
                    }.sortedByDescending { it.rssi }
                    _scanResults.value = results
                }
            }
        }
        testRangeNotifier = notifier
        beaconManager.addRangeNotifier(notifier)
        beaconManager.startRangingBeacons(wildcardRegion)
    }

    /**
     * Stops beacon test scanning and clears results.
     */
    fun stopBeaconTest() {
        testRegion?.let { region ->
            beaconManager.stopRangingBeacons(region)
        }
        testRangeNotifier?.let { beaconManager.removeRangeNotifier(it) }
        testRegion = null
        testRangeNotifier = null
        _scanResults.value = emptyList()
    }

    /**
     * Sets fast scan mode for calibration (1100ms period for responsiveness).
     */
    fun enableFastScanMode() {
        beaconManager.foregroundScanPeriod = 1100L
        beaconManager.foregroundBetweenScanPeriod = 0L
        beaconManager.updateScanPeriods()
    }

    /**
     * Restores normal scan mode after calibration.
     */
    fun disableFastScanMode() {
        val config = currentConfig
        val interval = config?.scanIntervalMs ?: 60_000L
        beaconManager.foregroundScanPeriod = interval
        beaconManager.foregroundBetweenScanPeriod = interval
        beaconManager.updateScanPeriods()
    }

    /**
     * Starts scheduled monitoring that respects the configured time window.
     *
     * If the current time is within the window, monitoring starts immediately.
     * If outside the window, scheduling waits until the window opens.
     * When the window closes, monitoring is stopped automatically.
     */
    fun startScheduledMonitoring() {
        scheduleJob?.cancel()
        scheduleJob = scope.launch {
            while (true) {
                val config = try {
                    getBeaconConfig()
                } catch (e: Exception) {
                    // Beacon not configured, retry in 5 minutes
                    delay(5 * 60_000L)
                    continue
                }

                val now = LocalTime.now()
                val window = config.validTimeWindow

                if (isTimeInWindow(now, window)) {
                    // Inside time window - start monitoring if not already active
                    if (!isMonitoringActive) {
                        try {
                            startMonitoring()
                        } catch (e: Exception) {
                            // Failed to start, retry in 1 minute
                            delay(60_000L)
                            continue
                        }
                    }
                    // Wait until end of window, then stop
                    val delayUntilEnd = millisUntilTime(now, window.end)
                    delay(delayUntilEnd)
                    stopMonitoring()
                } else {
                    // Outside time window - ensure monitoring is stopped
                    if (isMonitoringActive) {
                        stopMonitoring()
                    }
                    // Wait until start of window
                    val delayUntilStart = millisUntilTime(now, window.start)
                    delay(delayUntilStart)
                }
            }
        }
    }

    /**
     * Stops the scheduled monitoring loop and any active monitoring.
     */
    fun stopScheduledMonitoring() {
        scheduleJob?.cancel()
        scheduleJob = null
        if (isMonitoringActive) {
            stopMonitoring()
        }
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

        // Delegate to HomeOfficeTracker for business logic
        val now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        homeOfficeTracker.onBeaconDetected(uuid = config.uuid, timestamp = now)
    }

    /**
     * Called when beacon exits region.
     * Starts timeout countdown before triggering BeaconLost event.
     */
    fun onBeaconLostFromRegion() {
        val config = currentConfig ?: return

        // Cancel any existing timeout job
        timeoutJob?.cancel()

        // Capture lastSeenTimestamp before the delay so the end time reflects
        // when the beacon was actually last seen, not when the timeout fires.
        val lastSeen = lastSeenTimestamp

        // Start new timeout countdown
        timeoutJob = scope.launch {
            delay(config.timeoutMinutes * 60_000L)
            val now = LocalDateTime.now()
            val lastSeenLocal = lastSeen?.let {
                LocalDateTime.ofInstant(it, ZoneId.systemDefault())
            }
            // Delegate to HomeOfficeTracker for business logic
            homeOfficeTracker.onBeaconTimeout(timestamp = now, lastSeenTimestamp = lastSeenLocal)
        }
    }

    /**
     * Returns the last timestamp when beacon was seen.
     * Used for end time correction in state machine.
     */
    fun getLastSeenTimestamp(): Instant? = lastSeenTimestamp

    companion object {
        /**
         * Checks if the given time is within the specified window.
         */
        fun isTimeInWindow(time: LocalTime, window: TimeWindow): Boolean {
            return !time.isBefore(window.start) && !time.isAfter(window.end)
        }

        /**
         * Calculates the milliseconds from the current time until the target time.
         * If the target time is before or equal to the current time, assumes it is the next day.
         */
        fun millisUntilTime(now: LocalTime, target: LocalTime): Long {
            val nowNanos = now.toNanoOfDay()
            val targetNanos = target.toNanoOfDay()
            val diffNanos = if (targetNanos > nowNanos) {
                targetNanos - nowNanos
            } else {
                // Target is tomorrow
                (24L * 60 * 60 * 1_000_000_000) - nowNanos + targetNanos
            }
            return diffNanos / 1_000_000 // Convert to millis
        }
    }
}
