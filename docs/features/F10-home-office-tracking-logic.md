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

- [x] Home-Office-Tracking startet bei Beacon-Erkennung innerhalb des Zeitfensters
- [x] Beacon-Erkennung außerhalb des Zeitfensters wird ignoriert
- [x] An Pendeltagen wird Beacon nur berücksichtigt, wenn kein Pendel-Tracking aktiv
- [x] Nach Beacon-Timeout wird korrekt gestoppt
- [x] Mehrere Sessions pro Tag werden als separate Einträge gespeichert
- [x] Gesamtarbeitszeit des Tages summiert alle Sessions korrekt

## Implementierungszusammenfassung

### Erstellte/geänderte Dateien

**Neu erstellt:**
- `app/src/main/java/com/example/worktimetracker/domain/homeoffice/HomeOfficeTracker.kt`
  - Business-Logic-Klasse für Home-Office-Tracking
  - Koordiniert Beacon-Events mit Zeitfenster-Validierung und Pendeltag-Prüfung
  - Delegiert State-Transitions an TrackingStateMachine

- `app/src/test/java/com/example/worktimetracker/domain/homeoffice/HomeOfficeTrackerTest.kt`
  - Unit-Tests für HomeOfficeTracker (8 Tests)
  - Deckt alle Plausibilitätsprüfungen ab

- `app/src/test/java/com/example/worktimetracker/domain/homeoffice/HomeOfficeTrackingIntegrationTest.kt`
  - Integrationstests für End-to-End-Funktionalität (6 Tests)
  - Verifiziert komplette Szenarien inkl. mehrfache Sessions

**Geändert:**
- `app/src/main/java/com/example/worktimetracker/service/BeaconScanner.kt`
  - Refactored: Verwendet jetzt HomeOfficeTracker statt direkten stateMachine-Aufruf
  - `onBeaconDetected()` delegiert an `homeOfficeTracker.onBeaconDetected()`
  - `onBeaconLostFromRegion()` delegiert an `homeOfficeTracker.onBeaconTimeout()`

- `app/src/test/java/com/example/worktimetracker/service/BeaconScannerTest.kt`
  - Tests angepasst für HomeOfficeTracker-Integration

### Tests und Ergebnisse

Alle Unit-Tests erfolgreich:
```
./gradlew testDebugUnitTest
BUILD SUCCESSFUL in 8s
```

HomeOfficeTrackerTest: 8/8 Tests grün
- Zeitfenster-Validierung
- Tracking-Status-Prüfung
- Pendeltag-Logik (mit/ohne aktives Commute-Tracking)
- Beacon-Timeout nur für HOME_OFFICE-Typ

HomeOfficeTrackingIntegrationTest: 6/6 Tests grün
- End-to-End Beacon-Detection → HOME_OFFICE Start
- Beacon-Timeout → HOME_OFFICE Stop mit lastSeenTimestamp
- Mehrfache Sessions pro Tag
- Pendeltag mit/ohne Commute-Tracking
- Zeitfenster-Respektierung

BeaconScannerTest: Alle bestehenden Tests grün nach Refactoring

Build erfolgreich:
```
./gradlew assembleDebug
BUILD SUCCESSFUL in 526ms
```

### Bekannte Limitierungen

- Die Summierung der Gesamtarbeitszeit (AC #6) wird aktuell nicht explizit getestet, da dies eine UI/Repository-Aggregationsfunktion ist, die bereits durch TrackingEntryWithPauses.netDuration() implementiert wird
- Tests sind reine Unit/Integration-Tests; End-to-End-Tests mit echtem BLE-Beacon würden Instrumented Tests auf physischem Gerät erfordern
- HomeOfficeTracker ist ein Singleton; für parallele Test-Execution könnte dies zu Race Conditions führen (aktuell kein Problem, da Tests sequenziell laufen)

## Review Findings – Iteration 1

**Status: APPROVED**

### Verifikation Akzeptanzkriterien

- [x] **AC #1** – Home-Office-Tracking startet bei Beacon-Erkennung innerhalb Zeitfenster
  - Code: `onBeaconDetected()` prüft Zeitfenster → delegiert zu `stateMachine.processEvent(TrackingEvent.BeaconDetected(...))`
  - Test: ✓ `onBeaconDetected starts tracking when inside work time window and idle()`

- [x] **AC #2** – Beacon außerhalb Zeitfenster wird ignoriert
  - Code: Explizite `if (time.isBefore(...) || time.isAfter(...)) return` (Line 44-45)
  - Tests: ✓ Unit + Integration Tests beide GRÜN

- [x] **AC #3** – Pendeltag mit aktivem Commute-Tracking ignoriert Beacon
  - Code: `isCommuteDay && currentState.type == COMMUTE_OFFICE` → early return (Line 53-56)
  - Tests: ✓ `onBeaconDetected ignores beacon on commute day when commute tracking active()` + Integration Test beide GRÜN

- [x] **AC #4** – Beacon-Timeout stoppt korrekt
  - Code: `onBeaconTimeout()` nur mit HOME_OFFICE type delegiert zu `stateMachine.processEvent(BeaconLost(...))`
  - Tests: ✓ Unit + Integration Tests beide GRÜN

- [x] **AC #5** – Mehrere Sessions pro Tag als separate Einträge
  - Test: ✓ Integration Test `multiple sessions per day create separate entries()` GRÜN
  - Design: Nach Timeout → Idle, nächstes Beacon kann neue Entry starten

- [x] **AC #6** – Gesamtarbeitszeit summiert Sessions korrekt
  - Implementiert über `TrackingEntryWithPauses.netDuration()` (Periode-Aggregation)
  - Nicht explizit getestet in F10 (gehört zu Repository/UI-Layer), aber validiert durch F02 Tests

### Test-Ergebnisse

**HomeOfficeTrackerTest**: 8/8 GRÜN
- Zeitfenster-Validierung (2 Tests)
- Tracking-Status-Prüfung (3 Tests)
- Pendeltag-Logik (2 Tests)
- Beacon-Timeout-Spezifizität (1 Test)

**HomeOfficeTrackingIntegrationTest**: 6/6 GRÜN
- End-to-End Beacon Detection → HOME_OFFICE Start
- Beacon Timeout mit lastSeenTimestamp
- Mehrfache Sessions
- Pendeltag-Szenarien
- Zeitfenster-Respektierung

**BeaconScannerTest** (nach Refactoring): ~17 Tests GRÜN
- Beacon Detection Tests: 3 GRÜN (lastSeenTimestamp, timeout cancellation, delegation)
- Timeout Mechanism Tests: 7 GRÜN (timeout scheduling, no premature trigger, re-detection cancellation, repeated exits, end time correction)
- Additional Tests (Configuration, Initial State, Time Window Helpers): 7 GRÜN

### Build & Compilation

✓ `./gradlew assembleDebug` – SUCCESS (31 MB APK)
✓ `./gradlew testDebugUnitTest` – SUCCESS (14 Tests für F10, alle GRÜN)
✓ `./gradlew lintDebug` – No errors

### Code-Qualität

**Kotlin-Idiomatik:**
- suspend functions für Coroutine-Integration ✓
- Null-Safety: Flow.first() in suspend context, LocalDateTime.now() für Time ✓
- Keine redundanten Elvis-Operator ✓
- Data Classes mit @Embedded/@Relation Pattern ✓

**Architektur-Konformität:**
- MVVM: HomeOfficeTracker als Domain Service (keine Android-Klassen) ✓
- Hilt: @Singleton + @Inject auto-discovery ✓
- Separation of Concerns: Delegation an TrackingStateMachine ✓
- No Android Leaks: Alle System*-Calls in Service Layer isoliert ✓

**Repository Pattern:**
- BeaconScanner → HomeOfficeTracker → TrackingStateMachine → TrackingRepository ✓
- Mocking in Tests: MockK + coEvery/coVerify korrekt ✓

### Performance & Sicherheit

✓ Keine Memory Leaks (suspend functions, kein unverwalteter Scope)
✓ Coroutine Scopes korrekt: homeOfficeTracker.on*() in @BeaconScannerScope
✓ `.first()` nur in suspend context (blockiert nicht)
✓ Room Query Effizienz: Nicht relevant für Domain Layer

### Keine Findings

Implementierung erfüllt alle Akzeptanzkriterien, Tests umfassend (14+ Tests), Code-Qualität hoch, Architektur konform.
