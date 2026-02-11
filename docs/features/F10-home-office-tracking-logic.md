# F10 — Home-Office-Tracking Logik

## Übersicht

Business Logic für Home-Office-Tage: Verbindet BLE-Beacon-Events mit Zeitfenster-Validierung und steuert den automatischen Start/Stop des Trackings am Heimarbeitsplatz.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F03** (State Machine) — Integration der Home-Office-Logik
- **F09** (BLE Beacon Scanning) — Empfang von Beacon-Events
- **F16** (Settings) — Zeitfenster, Beacon-Konfiguration

## Requirements-Referenz

FR-H1 bis FR-H6

## Umsetzung

### Logik-Fluss

```kotlin
class HomeOfficeTracker @Inject constructor(
    private val stateMachine: TrackingStateMachine,
    private val commuteDayChecker: CommuteDayChecker,
    private val settingsProvider: SettingsProvider
) {
    fun onBeaconDetected() {
        val now = LocalTime.now()
        val window = settingsProvider.getWorkTimeWindow()

        // Plausibilitätsprüfungen
        if (now !in window) return                    // Außerhalb Arbeitszeit
        if (stateMachine.isTracking()) return          // Bereits aktiv

        // An Pendeltagen: Beacon wird ignoriert wenn Pendel-Tracking aktiv
        // An Nicht-Pendeltagen: Beacon startet Home-Office-Tracking
        stateMachine.processEvent(TrackingEvent.BeaconDetected(...))
    }

    fun onBeaconTimeout() {
        // Nur stoppen wenn aktuell HOME_OFFICE-Tracking aktiv
        if (stateMachine.currentType() == TrackingType.HOME_OFFICE) {
            stateMachine.processEvent(TrackingEvent.BeaconLost)
        }
    }
}
```

### Interaktion mit Pendeltagen

An Pendeltagen kann es sein, dass der Nutzer sich doch gegen das Büro entscheidet und von zu Hause arbeitet. Die Logik:

- Pendeltag + Beacon erkannt + kein Pendel-Tracking aktiv → Home-Office-Tracking starten (Nutzer pendelt offenbar nicht)
- Pendeltag + Pendel-Tracking bereits aktiv → Beacon-Events ignorieren (Nutzer ist aus dem Büro zurück und sitzt zuhause am Schreibtisch)

### Mehrfache Sessions pro Tag

Falls der Nutzer den Schreibtisch für längere Zeit verlässt (> Timeout) und später zurückkehrt, wird ein neuer Tracking-Eintrag erstellt. Beide Einträge zählen für den Tag.

### Akzeptanzkriterien

- [ ] Home-Office-Tracking startet bei Beacon-Erkennung innerhalb des Zeitfensters
- [ ] Beacon-Erkennung außerhalb des Zeitfensters wird ignoriert
- [ ] An Pendeltagen wird Beacon nur berücksichtigt, wenn kein Pendel-Tracking aktiv
- [ ] Nach Beacon-Timeout wird korrekt gestoppt
- [ ] Mehrere Sessions pro Tag werden als separate Einträge gespeichert
- [ ] Gesamtarbeitszeit des Tages summiert alle Sessions korrekt
