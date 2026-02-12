# F11 — Manuelles Tracking

## Übersicht

Fallback-Modus: Der Nutzer kann Arbeitszeit manuell per Button in der App oder über die Persistent Notification starten und stoppen. Dient als Sicherheitsnetz, wenn automatische Erkennung nicht greift.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F02** (Local Database) — Einträge speichern
- **F03** (State Machine) — ManualStart/ManualStop Events
- **F04** (Foreground Service) — Notification-Actions

## Requirements-Referenz

FR-M1, FR-M3

## Umsetzung

### UI-Elemente

#### Dashboard-Button

Prominenter Start/Stop-Button auf dem Dashboard-Screen:

- **IDLE-Zustand:** Großer "Start"-Button mit Dropdown für Tracking-Typ (Home Office / Büro / Sonstiges)
- **TRACKING-Zustand:** "Stop"-Button + "Pause"-Button + laufender Timer
- **PAUSED-Zustand:** "Weiter"-Button + "Stop"-Button + pausierter Timer

#### Notification-Actions

Wie in F04 definiert: Pause- und Stop-Buttons direkt in der Persistent Notification.

### Pausen

```kotlin
// Pause starten
stateMachine.processEvent(TrackingEvent.PauseStart)
// → Erstellt Pause-Eintrag in DB, aktualisiert Notification

// Pause beenden
stateMachine.processEvent(TrackingEvent.PauseEnd)
// → Schließt Pause-Eintrag ab, Timer läuft weiter
```

Während einer Pause zeigt die Notification:
```
⏸ Pause seit 12:15 (Arbeitszeit: 4h 00min)
  [Weiter] [Stopp]
```

### Quick-Actions

Für häufige Szenarien:
- Notification-Reminder an Pendeltagen (F08) enthält "Manuell starten"-Button
- Widget (Phase 2): Home-Screen-Widget mit Ein-Tap-Start

### Akzeptanzkriterien

- [x] Manueller Start erstellt korrekten Eintrag mit Typ MANUAL
- [x] Manueller Stop schließt den Eintrag mit korrekter Endzeit ab
- [x] Pausen können gestartet und gestoppt werden
- [x] Netto-Arbeitszeit wird korrekt berechnet (Brutto minus Pausen)
- [x] Manuelles Tracking und automatisches Tracking schließen sich gegenseitig aus
- [x] Start/Stop funktioniert sowohl über die App als auch über die Notification

## Implementierungszusammenfassung

### Erstellte/Geänderte Dateien

#### Produktionscode

1. **DashboardViewModel.kt** (neu)
   - ViewModel für den Dashboard-Screen
   - Orchestriert die State Machine für manuelles Tracking
   - Bietet Methoden: `startManualTracking()`, `stopTracking()`, `pauseTracking()`, `resumeTracking()`
   - UI State Flow basierend auf TrackingStateMachine State

2. **DashboardScreen.kt** (erweitert)
   - Vollständige UI-Implementierung mit drei Zuständen:
     - **Idle**: Start-Button mit Dropdown für Tracking-Typ (Home Office / Büro / Sonstiges)
     - **Tracking**: Laufender Timer + Pause/Stop Buttons
     - **Paused**: Statischer Zustand + Weiter/Stop Buttons
   - Live-Timer mit LaunchedEffect für Tracking-Zustand
   - Integration mit DashboardViewModel über Hilt

#### Tests

3. **DashboardViewModelTest.kt** (neu)
   - 6 Unit-Tests für alle ViewModel-Funktionen
   - Tests für State-Flow-Transformation (Idle, Tracking, Paused)
   - Tests für Event-Dispatching (ManualStart, ManualStop, PauseStart, PauseEnd)
   - Verwendet Turbine für Flow-Testing, Mockk für Mocks

4. **ManualTrackingIntegrationTest.kt** (neu)
   - 3 Integration-Tests für End-to-End-Szenarien
   - Test: Vollständiger Flow (Start → Pause → Resume → Stop)
   - Test: Start mit HOME_OFFICE Typ
   - Test: Stop während Pause (schließt beide Einträge)

### Tests und Ergebnisse

Alle Tests erfolgreich:
```
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.ui.viewmodel.DashboardViewModelTest"
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.integration.ManualTrackingIntegrationTest"
BUILD SUCCESSFUL
```

- **DashboardViewModelTest**: 6/6 Tests bestanden
- **ManualTrackingIntegrationTest**: 3/3 Tests bestanden

Build erfolgreich:
```
./gradlew assembleDebug
BUILD SUCCESSFUL
```

### Bestehende Integrationspunkte

Das Feature nutzt vollständig die bestehende Infrastruktur:

1. **TrackingStateMachine** (F03)
   - ManualStart/ManualStop Events bereits vorhanden
   - PauseStart/PauseEnd Events bereits vorhanden
   - State-Übergänge werden korrekt verarbeitet

2. **TrackingRepository** (F02)
   - `startTracking()` mit `autoDetected=false` Flag für manuelle Einträge
   - `stopTracking()`, `startPause()`, `stopPause()` bereits implementiert
   - Pause-Logik mit Netto-Berechnung in `TrackingEntryWithPauses.netDuration()`

3. **TrackingForegroundService** (F04)
   - Notification-Actions (Pause/Stop/Resume) bereits implementiert
   - Service beobachtet State Machine und aktualisiert Notification automatisch
   - Periodische Timer-Updates in Notification

### Bekannte Limitierungen

1. **Kein Tracking-Typ-Wechsel während laufendem Tracking**
   - User muss stoppen und neu starten, um Typ zu ändern
   - Design-Entscheidung für MVP: Verhindert Inkonsistenzen

2. **Timer-Granularität**
   - UI-Timer aktualisiert jede Sekunde (LaunchedEffect mit delay(1000))
   - Notification-Timer aktualisiert jede Minute (TrackingForegroundService)
   - Unterschied ist Batterie-Optimierung

3. **Keine Pause-Historie in UI**
   - Dashboard zeigt nur aktuellen Zustand
   - Pause-Details sind in EntriesScreen sichtbar (späteres Feature)

4. **Notification-Actions abhängig von Foreground Service**
   - Service wird automatisch bei Tracking-Start gestartet
   - Stop-Action in Notification beendet sowohl Tracking als auch Service

### TDD-Dokumentation

**RED Phase**: Tests geschrieben, Kompilierung fehlgeschlagen (DashboardViewModel existierte nicht)

**GREEN Phase**: Minimale Implementierung von DashboardViewModel + DashboardScreen UI

**REFACTOR Phase**: Integration-Tests hinzugefügt, Mock-Verhalten angepasst (UUID-Handling)

Alle Tests blieben während der Refactoring-Phase grün.

## Review Findings – Iteration 1

**Status: APPROVED**

Keine kritischen oder Major-Findings. Feature erfüllt alle 6 Akzeptanzkriterien.

### Verifikation durchgeführt

**Build & Tests:**
- ✓ Build: `./gradlew assembleDebug` SUCCESS – APK 31MB erstellt
- ✓ APK vorhanden: `/app/build/outputs/apk/debug/app-debug.apk`
- ✓ Unit Tests:
  - DashboardViewModelTest: 7/7 grün
  - ManualTrackingIntegrationTest: 3/3 grün
  - Insgesamt: 10 Tests erfolgreich
- ✓ Keine Kompilierungsfehler

**Akzeptanzkriterien (alle erfüllt):**

1. **AC1 – Manueller Start erstellt korrekten Eintrag mit Typ MANUAL**
   - ✓ `DashboardViewModel.startManualTracking(type)` ruft `stateMachine.processEvent(TrackingEvent.ManualStart(type))` auf
   - ✓ `TrackingStateMachine.handleManualStart()` ruft `repository.startTracking(event.type, autoDetected=false)` auf
   - ✓ Integration-Test `manual start with HOME_OFFICE type` verifiziert: Entry mit richtigem Typ und `autoDetected=false`
   - ✓ Alle drei Tracking-Typen werden in UI unterstützt (Home Office, Büro, Sonstiges)

2. **AC2 – Manueller Stop schließt den Eintrag mit korrekter Endzeit ab**
   - ✓ `DashboardViewModel.stopTracking()` sendet `TrackingEvent.ManualStop`
   - ✓ `TrackingStateMachine.handleManualStop(entryId)` ruft `repository.stopTracking(entryId)`
   - ✓ Repository setzt `entry.endTime = LocalDateTime.now()` und updated Entry
   - ✓ Integration-Test verifiziert: `trackingDao.update(match { it.endTime != null })`

3. **AC3 – Pausen können gestartet und gestoppt werden**
   - ✓ `pauseTracking()` → `TrackingEvent.PauseStart` → `repository.startPause(entryId)`
   - ✓ `resumeTracking()` → `TrackingEvent.PauseEnd` → `repository.stopPause(entryId)`
   - ✓ State Machine: `handlePauseStart()` wechselt zu `Paused`, `handlePauseEnd()` zu `Tracking`
   - ✓ Integration-Test `full manual tracking flow` testet: Start → Pause → Resume → Stop Sequenz

4. **AC4 – Netto-Arbeitszeit wird korrekt berechnet (Brutto minus Pausen)**
   - ✓ Berechnung erfolgt in `TrackingEntryWithPauses.netDuration()` (siehe F02)
   - ✓ Pause-Logik ist korrekt implementiert: Pause mit Start- und Endzeit wird abgezogen
   - ✓ Integration-Test bestätigt korrekte Pause-Speicherung mit `match { it.endTime != null }`

5. **AC5 – Manuelles Tracking und automatisches Tracking schließen sich gegenseitig aus**
   - ✓ State Machine erlaubt nur ein aktives Tracking: "Nur ein aktiver Eintrag gleichzeitig" (siehe F03)
   - ✓ `getActiveEntry()` gibt max. ein Entry zurück (`LIMIT 1`)
   - ✓ Neue Events während Tracking werden von State Machine korrekt gefiltert

6. **AC6 – Start/Stop funktioniert sowohl über die App als auch über die Notification**
   - ✓ App-Level: `DashboardViewModel.startManualTracking()` / `.stopTracking()`
   - ✓ Notification-Level: `NotificationActionReceiver` empfängt `ACTION_PAUSE`, `ACTION_RESUME`, `ACTION_STOP` und konvertiert zu `TrackingEvent`
   - ✓ Beide Pfade nutzen dieselbe `TrackingStateMachine.processEvent()` → Konsistenz garantiert
   - ✓ Receiver nutzt `goAsync() + PendingResult.finish()` (Best Practice für ANR-Vermeidung)

**Code-Qualität:**

- ✓ **MVVM + DI:** DashboardViewModel mit `@HiltViewModel`, korrekte Injection von TrackingStateMachine
- ✓ **Null-Safety:** Kotlin-idiomatisch, keine unsicheren Casts
- ✓ **State Management:** Proper use von `StateFlow` mit `stateIn()` für UI-State
- ✓ **Coroutines:** `viewModelScope.launch` für asynchrone Events, kein leaky scope
- ✓ **Compose UI:** Stateful Composition, LaunchedEffect mit Lifecycle Management
- ✓ **Keine Code-Duplikation:** Drei Sub-Composables (IdleContent, TrackingContent, PausedContent) mit sauberer Separation

**Architektur & Integration:**

- ✓ Abhängigkeiten (F02, F03, F04) vollständig genutzt und korrekt integriert
- ✓ TrackingRepository-Methoden alle vorhanden: `startTracking()`, `stopTracking()`, `startPause()`, `stopPause()`
- ✓ TrackingStateMachine Events alle vorhanden: `ManualStart`, `ManualStop`, `PauseStart`, `PauseEnd`
- ✓ NotificationActionReceiver korrekt mit State Machine verbunden
- ✓ Keine Android-Klassen-Leaks in ViewModel (nur Context via DI, wo nötig nicht der Fall)

**Tests:**

- ✓ Test-Strategie: Unit-Tests mit Mocks (Turbine für Flows) + Integration-Tests
- ✓ DashboardViewModelTest (7 Tests):
  - Idle state initial ✓
  - Tracking state emission ✓
  - Paused state emission ✓
  - ManualStart event trigger ✓
  - ManualStop event trigger ✓
  - PauseStart event trigger ✓
  - PauseEnd event trigger ✓
- ✓ ManualTrackingIntegrationTest (3 Tests):
  - Full flow (Start → Pause → Resume → Stop) ✓
  - HOME_OFFICE type creation ✓
  - Stop while paused closes both pause and entry ✓
- ✓ Tests nutzen Best Practices: Turbine für Flow testing, runTest() für Coroutines, coVerify für async verification

**Performance & Batterieeffizenz:**

- ✓ Timer in TrackingContent nutzt `delay(1000)` nur während aktiven Tracking
- ✓ LaunchedEffect mit Unit-Key startet nur einmal
- ✓ ViewModels nutzen `SharingStarted.Eagerly` für effiziente State-Distribution
- ✓ Notification-Updates alle 60s (F04) ist batterieschonend

**Bekannte Limitierungen (dokumentiert):**

Die vier dokumentierten Limitierungen sind angemessen für MVP:
1. Kein Typ-Wechsel während Tracking → Design-Entscheidung, logisch
2. Timer-Granularität-Unterschiede (UI: 1s, Notification: 60s) → Batterie-Optimierung
3. Keine Pause-Historie in UI → Planmäßig für späteres Feature
4. Abhängigkeit vom Foreground Service → Korrekt, Service wird von F04 verwaltet

### Fazit

**Status: APPROVED**

Feature F11 ist produktionsreif für Integration. Alle Akzeptanzkriterien sind erfüllt, Tests sind grün, Code folgt Architektur-Konventionen und Best Practices. Die Implementierung zeigt gutes Verständnis für State Management, Coroutines und Jetpack Compose.

**Nächste Schritte:**
- Merge to main
- APK bauen: `builds/v1.0.0.apk`
- Task als DONE markieren
