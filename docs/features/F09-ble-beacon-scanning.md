# F09 — BLE Beacon Scanning

## Übersicht

Periodischer Bluetooth Low Energy Scanner, der einen konfigurierten Beacon am Schreibtisch erkennt und Präsenz-/Abwesenheits-Events an die State Machine meldet.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F01** (Project Setup) — Dependencies
- **F03** (State Machine) — Events weiterleiten
- **F04** (Foreground Service) — BLE-Scan läuft innerhalb des Foreground Service
- **F05** (Permissions) — Bluetooth Permissions
- **F16** (Settings) — Beacon UUID, Scan-Intervall, Timeout, Zeitfenster

## Requirements-Referenz

FR-H1 bis FR-H6, NFR-B2

## Umsetzung

### Library

**AltBeacon Library** (empfohlen für iBeacon-Support auf Android):

```kotlin
implementation("org.altbeacon:android-beacon-library:2.20+")
```

Bietet: Background-Scanning, Region-Monitoring, Ranging, iBeacon/Eddystone-Support.

### Beacon-Konfiguration

```kotlin
data class BeaconConfig(
    val uuid: String,           // z.B. "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
    val major: Int? = null,     // Optional, für spezifischeres Matching
    val minor: Int? = null,     // Optional
    val scanIntervalMs: Long = 60_000,        // 60 Sekunden
    val timeoutMinutes: Int = 10,              // Beacon-Verlust-Timeout
    val validTimeWindow: TimeWindow = TimeWindow(
        start = LocalTime.of(6, 0),
        end = LocalTime.of(22, 0)
    )
)
```

### Scanner-Architektur

```kotlin
class BeaconScanner @Inject constructor(
    private val beaconManager: BeaconManager,
    private val settingsProvider: SettingsProvider,
    private val stateMachine: TrackingStateMachine
) {
    private var lastSeenTimestamp: Instant? = null
    private var timeoutJob: Job? = null

    fun startMonitoring() {
        val config = settingsProvider.getBeaconConfig()
        val region = Region("desk-beacon",
            Identifier.parse(config.uuid),
            config.major?.let { Identifier.fromInt(it) },
            config.minor?.let { Identifier.fromInt(it) }
        )

        beaconManager.apply {
            beaconParsers.add(BeaconParser()
                .setBeaconLayout("m:2-3=0215,...")) // iBeacon Layout
            foregroundScanPeriod = config.scanIntervalMs
            foregroundBetweenScanPeriod = config.scanIntervalMs
            backgroundScanPeriod = config.scanIntervalMs
            backgroundBetweenScanPeriod = config.scanIntervalMs
        }

        // Region Monitoring (grob: in/out of range)
        beaconManager.startMonitoring(region)

        // Ranging (fein: RSSI, Distanz — optional für Kalibrierung)
        beaconManager.startRangingBeacons(region)
    }

    fun stopMonitoring() {
        beaconManager.stopMonitoring(region)
        beaconManager.stopRangingBeacons(region)
        timeoutJob?.cancel()
    }
}
```

### Timeout-Mechanismus

Der Timeout-Mechanismus verhindert, dass kurze Abwesenheiten (Küche, Bad) das Tracking unterbrechen:

```
Beacon erkannt → lastSeenTimestamp aktualisieren, Timeout-Timer zurücksetzen
Beacon nicht erkannt → Timeout-Timer läuft weiter
Timeout erreicht → BeaconLost-Event an State Machine
Beacon wieder erkannt (vor Timeout) → Timer zurücksetzen, weiter tracken
```

```kotlin
private fun onBeaconDetected() {
    lastSeenTimestamp = Instant.now()
    timeoutJob?.cancel()

    if (stateMachine.state.value is TrackingState.Idle) {
        if (isInValidTimeWindow()) {
            stateMachine.processEvent(TrackingEvent.BeaconDetected(config.uuid))
        }
    }
}

private fun onBeaconLostFromRegion() {
    // Starte Timeout-Countdown
    timeoutJob = scope.launch {
        delay(config.timeoutMinutes * 60_000L)
        stateMachine.processEvent(TrackingEvent.BeaconLost)
    }
}
```

### Endzeit-Korrektur

Wenn das Tracking durch Beacon-Timeout endet, wird die Endzeit auf den `lastSeenTimestamp` gesetzt — nicht auf den Zeitpunkt des Timeouts. So wird die tatsächliche Anwesenheitszeit korrekt erfasst.

```
18:00  Letzter Beacon-Scan erkannt → lastSeenTimestamp = 18:00
18:10  Timeout erreicht (10 min ohne Signal)
       → Entry.endTime = 18:00 (nicht 18:10)
```

### Scan-Zeitfenster

BLE-Scanning wird nur innerhalb des konfigurierten Arbeitszeitfensters (Default: 06:00–22:00) aktiviert. Außerhalb wird der Scanner deaktiviert, um Akku zu sparen und versehentliches Tracking zu vermeiden.

```kotlin
class BeaconScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun scheduleDaily() {
        // WorkManager: BLE-Scan starten um 06:00, stoppen um 22:00
        // Alternativ: AlarmManager für präziseres Timing
    }
}
```

### Beacon-Setup im Settings-Screen

- UUID-Eingabefeld (manuell oder via QR-Code-Scan des Beacons)
- "Beacon suchen"-Modus: Scannt alle Beacons in der Nähe und zeigt sie in einer Liste an → Nutzer wählt seinen Beacon aus
- Signalstärke-Anzeige (RSSI) für Kalibrierung der Reichweite

### Akzeptanzkriterien

- [ ] Beacon wird innerhalb von 2 Scan-Intervallen nach Annäherung erkannt
- [ ] BeaconDetected-Event wird an State Machine gesendet
- [ ] Kurze Abwesenheiten (< Timeout) unterbrechen das Tracking nicht
- [ ] Tracking stoppt nach konfiguriertem Timeout ohne Beacon-Signal
- [ ] Endzeit wird auf letzten Beacon-Kontakt gesetzt (nicht Timeout-Zeitpunkt)
- [ ] Scanning läuft nur innerhalb des Zeitfensters
- [ ] Beacon-Erkennung funktioniert im Hintergrund (Foreground Service)
- [ ] "Beacon suchen"-Modus listet alle sichtbaren Beacons
- [ ] Akkuverbrauch durch BLE-Scan bleibt akzeptabel (< 2% täglich)
