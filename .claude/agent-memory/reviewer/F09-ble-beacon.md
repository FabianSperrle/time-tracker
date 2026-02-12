# F09 — BLE Beacon Scanning Review Notes

## Review Iteration 1 - CHANGES_REQUESTED

**Status:** CHANGES_REQUESTED - 5 Findings (2 CRITICAL, 2 MAJOR, 1 MINOR)

### Critical Findings

1. **CRITICAL - AC #5 nicht erfüllt**: Endzeit wird auf NOW statt lastSeenTimestamp gesetzt
   - BeaconScanner.onBeaconDetected() speichert korrekt: `lastSeenTimestamp = Instant.now()`
   - Aber: TrackingRepository.stopTracking() setzt immer: `entry.copy(endTime = LocalDateTime.now())`
   - Problem: BeaconScanner.getLastSeenTimestamp() wird NIEMALS von State Machine aufgerufen
   - Folge: Wenn Beacon um 18:00 gelost wird (timeout 18:10), wird Entry mit 18:10 EndTime gespeichert statt 18:00

2. **CRITICAL - Test-Coverage für Timeout-Mechanismus fehlt komplett**
   - Spec (S. 215-217) verspricht: "Tests für Timeout-Mechanismus (mit TestScope und advanceTimeBy)"
   - Realität: BeaconScannerTest hat nur 5 triviale Tests:
     1. getBeaconConfig returns config
     2. BeaconConfig defaults
     3. BeaconScanner construction
     4. timeoutJob null initially
     5. getLastSeenTimestamp null initially
   - Fehlende Tests: onBeaconDetected, onBeaconLostFromRegion, Timeout-Trigger, Event-Weiterleitung
   - ACs nicht testbar: #1 (Recognition), #3 (Short absences), #4 (Timeout stops)

### Major Findings

3. **MAJOR - AC #6 nicht vollständig**: Scanner läuft außerhalb Zeitfenster
   - isInValidTimeWindow() prüft nur ob EVENT gesendet wird, stoppt aber Scanner nicht
   - BeaconScheduler (S. 141-148 Spec) komplett missing
   - Scanner sollte um 22:00 stopped und um 06:00 gestartet werden

4. **MAJOR - Crash bei fehlender Beacon-UUID-Config**
   - getBeaconConfig() throws: `throw IllegalStateException("Beacon UUID not configured")`
   - onBeaconDetected() kann dies abfangen: `val config = currentConfig ?: getBeaconConfig()`
   - Aber onBeaconDetected ist im AltBeacon-Callback (scope.launch in MonitorNotifier)
   - Exception ist nicht gecacht, kann ANR verursachen

### Minor Findings

5. **MINOR - stopMonitoring() setzt currentRegion nicht null**
   - Zweiter startMonitoring() Call entfernt alte Region nicht
   - currentConfig auch nicht zurückgesetzt
   - Kann zu doppelten Region Monitoring führen

## Verification Iteration 1

- Build: SUCCESS (APK 31MB)
- Tests: 10 tests passed (5 BeaconConfigTest + 5 BeaconScannerTest)
- Integration: BeaconScanner in TrackingForegroundService ✓
- Permissions: BLUETOOTH_SCAN + BLUETOOTH_CONNECT in Manifest ✓
- Dependencies: AltBeacon 2.20.6 in libs.versions.toml ✓

## Key Patterns Found

1. **AltBeacon MonitorNotifier**: Callbacks sind async via `scope.launch` – gut für nicht-blocking
2. **Timeout Job Pattern**: `cancel()` + `new launch` mit `delay()` – kann problematisch sein wenn Callback zweimal läuft
3. **CoroutineScope Isolation**: @BeaconScannerScope mit SupervisorJob für Lifecycle-Management
4. **State Machine Events**: BeaconDetected/BeaconLost als data classes mit timestamps

## AC Status Summary

- AC #1: ✗ Beacon recognition in 2 intervals – only testable with hardware
- AC #2: ✗ Event to State Machine – code present but untested
- AC #3: ✗ Short absences don't interrupt – logic present but not verified
- AC #4: ✗ Stops after timeout – implemented but not tested
- AC #5: ✗ Endtime on lastSeenTimestamp – **NOT IMPLEMENTED** (uses NOW)
- AC #6: ⚠ Only within time window – Events checked but scanner not stopped
- AC #7: ✓ Background in Foreground Service – integrated correctly

## Next Actions

1. Fix AC #5: Modify stopTracking() signature and integrate lastSeenTimestamp from BeaconScanner
2. Add comprehensive tests for onBeaconDetected, onBeaconLostFromRegion, and timeout mechanism
3. Implement BeaconScheduler for start/stop outside time window
4. Add exception handling in onBeaconDetected callback
5. Reset currentRegion/currentConfig in stopMonitoring()
