# F08 — Pendel-Tracking Logik

## Übersicht

Business Logic, die Geofence-Events zu einem vollständigen Pendeltag zusammensetzt: Erkennung von Hinfahrt, Bürozeit und Rückfahrt, inklusive Zeitfenster-Validierung und Pendeltag-Prüfung.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F03** (State Machine) — Integration der Pendel-Logik in Zustandsübergänge
- **F07** (Geofence Monitoring) — Empfang von Geofence-Events
- **F16** (Settings) — Pendeltage, Zeitfenster-Konfiguration

## Requirements-Referenz

FR-P1 bis FR-P7

## Umsetzung

### Pendeltag-Erkennung

```kotlin
class CommuteDayChecker @Inject constructor(
    private val settingsProvider: SettingsProvider
) {
    fun isCommuteDay(date: LocalDate = LocalDate.now()): Boolean {
        val commuteDays = settingsProvider.getCommuteDays() // z.B. [TUESDAY, THURSDAY]
        return date.dayOfWeek in commuteDays
    }

    fun isInOutboundWindow(time: LocalTime = LocalTime.now()): Boolean {
        val window = settingsProvider.getOutboundWindow() // 06:00–09:30
        return time.isAfter(window.start) && time.isBefore(window.end)
    }

    fun isInReturnWindow(time: LocalTime = LocalTime.now()): Boolean {
        val window = settingsProvider.getReturnWindow() // 16:00–20:00
        return time.isAfter(window.start) && time.isBefore(window.end)
    }
}
```

### Pendeltag State-Tracking

Ein Pendeltag durchläuft mehrere Phasen. Diese werden innerhalb des aktiven TrackingEntry als Metadata gespeichert:

```kotlin
enum class CommutePhase {
    OUTBOUND,    // Auf dem Weg zum Büro (ab Bahnhof-Geofence)
    IN_OFFICE,   // Im Büro (ab Büro-Geofence Enter)
    RETURN,      // Auf dem Rückweg (ab Büro-Geofence Exit)
    COMPLETED    // Angekommen am Heimat-Bahnhof
}
```

### Ablauf (Detail)

```
07:45  GeofenceEntered(HOME_STATION)
       → isCommuteDay? ✓
       → isInOutboundWindow? ✓
       → State Machine: IDLE → TRACKING(COMMUTE_OFFICE)
       → Phase: OUTBOUND
       → Notification: "Arbeitszeit läuft seit 07:45"

08:32  GeofenceEntered(OFFICE)
       → Phase: OUTBOUND → IN_OFFICE
       → Notification: "Im Büro seit 08:32 ✓"

16:45  GeofenceExited(OFFICE)
       → isInReturnWindow? ✓
       → Phase: IN_OFFICE → RETURN
       → Notification: "Rückfahrt erkannt"

17:23  GeofenceEntered(HOME_STATION)
       → State Machine: TRACKING → IDLE
       → Phase: RETURN → COMPLETED
       → Entry abschließen (endTime = 17:23)
       → Notification: "Arbeitstag beendet: 9h 38min"
```

### Edge Cases

| Szenario | Verhalten |
|---|---|
| Bahnhof-Geofence betreten außerhalb Zeitfenster | Event wird ignoriert, kein Tracking |
| Bahnhof-Geofence betreten an Nicht-Pendeltag | Event wird ignoriert |
| Büro-Geofence nie betreten (z.B. externer Termin) | Tracking läuft weiter bis manueller Stop oder Rückkehr zum Bahnhof |
| Rückkehr-Geofence nicht ausgelöst (GPS-Problem) | Tracking läuft → Reminder-Notification nach konfigurierter Uhrzeit (z.B. 21:00): "Tracking noch aktiv — vergessen zu stoppen?" |
| Nutzer verlässt Büro kurz (Mittagspause draußen) | Geofence Exit/Enter am Büro wird registriert, ändert aber nicht den Tracking-Status (nur Phase-Wechsel) |
| Pendeltag, aber kein Tracking bis 10:00 | WorkManager-Job sendet Reminder-Notification (FR-P7) |

### Reminder-Job

```kotlin
class CommuteReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (commuteDayChecker.isCommuteDay() && !trackingRepository.hasActiveEntry()) {
            notificationManager.showCommuteReminder()
        }
        return Result.success()
    }
}
```

Wird als `PeriodicWorkRequest` registriert (täglich um konfigurierte Uhrzeit, nur an Pendeltagen).

### Akzeptanzkriterien

- [x] Tracking startet nur an konfigurierten Pendeltagen und im richtigen Zeitfenster
- [x] Hinfahrt → Büro → Rückfahrt wird korrekt als Phasen erkannt
- [x] Endzeit wird korrekt bei Bahnhofs-Geofence-Enter gesetzt
- [x] Events außerhalb des Zeitfensters oder an Nicht-Pendeltagen werden ignoriert
- [x] Reminder-Notification erscheint, wenn um 10:00 kein Tracking aktiv ist
- [x] "Tracking vergessen zu stoppen"-Notification nach 21:00 wenn noch aktiv
- [x] Unit-Tests für alle Edge Cases

## Implementierungszusammenfassung

### Erstellte Dateien

- `/app/src/main/java/com/example/worktimetracker/domain/commute/CommutePhase.kt` — Enum mit den vier Pendelphasen (OUTBOUND, IN_OFFICE, RETURN, COMPLETED)
- `/app/src/main/java/com/example/worktimetracker/domain/commute/CommuteDayChecker.kt` — Suspend-Funktionen zur Prüfung von Pendeltag und Zeitfenstern via SettingsProvider-Flows
- `/app/src/main/java/com/example/worktimetracker/domain/commute/CommutePhaseTracker.kt` — StateFlow-basierter Phasen-Tracker mit validierten Zustandsübergängen
- `/app/src/main/java/com/example/worktimetracker/domain/commute/CommuteReminderLogic.kt` — Pure Logic für Reminder-Entscheidungen (kein Android-Dependency)
- `/app/src/main/java/com/example/worktimetracker/service/CommuteReminderWorker.kt` — HiltWorker für periodische Reminder-Checks (10:00 kein Tracking, 21:00 noch aktiv)
- `/app/src/main/java/com/example/worktimetracker/service/CommuteReminderScheduler.kt` — WorkManager-Scheduling (1h Intervall, KEEP Policy)

### Geänderte Dateien

- `/app/src/main/java/com/example/worktimetracker/domain/tracking/TrackingStateMachine.kt` — Integration der Pendel-Logik: CommuteDayChecker und CommutePhaseTracker als Dependencies. handleGeofenceEnteredWhileIdle prüft isCommuteDay + isInOutboundWindow. handleGeofenceEnteredWhileTracking aktualisiert Pendelphasen. handleReturnToHomeStation stoppt Tracking bei Rückkehr im Return-Window
- `/app/src/main/java/com/example/worktimetracker/WorkTimeTrackerApp.kt` — CommuteReminderScheduler.scheduleReminders() im onCreate, HiltWorkerFactory und Configuration.Provider integriert
- `/app/src/main/AndroidManifest.xml` — WorkManager-Initializer deaktiviert (custom HiltWorkerFactory)

### Test-Dateien

- `/app/src/test/java/com/example/worktimetracker/domain/commute/CommuteDayCheckerTest.kt` — 10 Tests: isCommuteDay (configured day, second day, non-commute weekday, weekend, empty config), isInOutboundWindow (within, at start, at end, before, after), isInReturnWindow (within, at start, at end, before, after)
- `/app/src/test/java/com/example/worktimetracker/domain/commute/CommutePhaseTrackerTest.kt` — 12 Tests: Full lifecycle (null -> OUTBOUND -> IN_OFFICE -> RETURN -> COMPLETED), Reset, Invalid transitions (exitOffice from OUTBOUND, enterOffice from null), Edge cases (OUTBOUND -> COMPLETED, lunch break re-enter, COMPLETED ignores further transitions, IN_OFFICE ignores completeCommute)
- `/app/src/test/java/com/example/worktimetracker/domain/commute/CommuteStateMachineIntegrationTest.kt` — 6 Tests: Full commute day flow, office phase transition, lunch break outside return window, non-commute Saturday ignored, outside outbound window ignored, manual stop resets phase
- `/app/src/test/java/com/example/worktimetracker/domain/commute/CommuteReminderLogicTest.kt` — 15 Tests: No-tracking reminder (7 tests incl. boundary at exactly reminder time), Late-tracking reminder (6 tests incl. boundary at exactly cutoff time), Default constants (2 tests)

### Testergebnis

- `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL (alle Tests gruen)
- `./gradlew assembleDebug` — BUILD SUCCESSFUL

### Bekannte Limitierungen

- CommuteReminderWorker nutzt PeriodicWorkRequest mit 1h Intervall, daher sind Reminder-Zeiten (10:00/21:00) nicht exakt, sondern auf die naechste Worker-Ausfuehrung abhaengig
- Geofence-Exit am Buero loest immer Phase-Wechsel zu RETURN aus, unabhaengig vom Return-Window. Der Return-Window-Check greift erst beim Stop-Tracking bei HOME_STATION-Rueckkehr
- Notification-Inhalt (z.B. "Arbeitszeit laeuft seit 07:45") wird durch den TrackingForegroundService (F04) gesteuert, nicht durch F08

## Review Findings – Iteration 1

**Status: CHANGES_REQUESTED**

### Finding 1: AC #3 nicht erfüllt — Endzeit wird mit LocalDateTime.now() statt Event-Zeitstempel gesetzt
- **Schweregrad:** CRITICAL
- **Datei:** `app/src/main/java/com/example/worktimetracker/domain/tracking/TrackingStateMachine.kt` (Zeile 182) und `app/src/main/java/com/example/worktimetracker/data/repository/TrackingRepository.kt` (Zeile 62)
- **Beschreibung:** AC #3 verlangt: "Endzeit wird korrekt bei Bahnhofs-Geofence-Enter gesetzt". In `handleReturnToHomeStation()` wird `repository.stopTracking()` aufgerufen, die intern `LocalDateTime.now()` nutzt. Das ist fehler­haft, weil die Endzeit auf den Zeitstempel des Geofence-Events (`event.timestamp`) gesetzt werden sollte, nicht auf die aktuelle Systemzeit. Dies kann zu Zeitversatz führen, falls zwischen Event-Eingang und Verarbeitung eine Verzögerung liegt.
- **Vorschlag:** Ändere `TrackingRepository.stopTracking(entryId: String)` auf `stopTracking(entryId: String, endTime: LocalDateTime)` und übergebe `event.timestamp` aus `handleReturnToHomeStation()`. Alle anderen Aufrufe von `stopTracking()` außerhalb der Geofence-Handler sollten weiterhin `LocalDateTime.now()` nutzen.

### Finding 2: Validierung der bereits zu Büro gefahrenen Prüfung (AC #3 Edge Case)
- **Schweregrad:** MINOR
- **Datei:** `app/src/main/java/com/example/worktimetracker/domain/tracking/TrackingStateMachine.kt` (Zeile 172–180)
- **Beschreibung:** In `handleReturnToHomeStation()` wurde der Check `hasCompletedOfficeCommuteToday()` entfernt. Der neue Code prüft nur noch, ob `currentState.type == TrackingType.COMMUTE_OFFICE`. Laut Spezifikation Edge Case: "Büro-Geofence nie betreten (z.B. externer Termin) — Tracking läuft weiter bis manueller Stop oder Rückkehr zum Bahnhof." Das bedeutet, Tracking SOLLTE weiterlaufen und NICHT bei HOME_STATION-Return enden, wenn das Büro nie besucht wurde. Der aktuelle Code stoppt aber trotzdem. Entweder ist das beabsichtigt oder es wurde ein Validierungslogik-Fehler eingeführt.
- **Vorschlag:** Klären: Soll Tracking bei HOME_STATION-Return OHNE vorherigen Office-Visit beendet werden? Wenn nein: `hasCompletedOfficeCommuteToday()` Check wieder aktivieren. Wenn ja: Spec Edge Case aktualisieren und explizit dokumentieren.

### Finding 3: Phase-Transition zu schnell nach Completion
- **Schweregrad:** MINOR
- **Datei:** `app/src/main/java/com/example/worktimetracker/domain/tracking/TrackingStateMachine.kt` (Zeile 183–184)
- **Beschreibung:** In `handleReturnToHomeStation()` wird `completeCommute()` gefolgt von sofort `reset()` aufgerufen. Das bedeutet die Phase geht null -> OUTBOUND -> IN_OFFICE -> RETURN -> COMPLETED -> null in einem Durchsatz. Die Phase COMPLETED existiert praktisch nur für einen Event-Zyklus. Falls UI oder andere Komponenten auf `CommutePhaseTracker.currentPhase` horchen, sehen sie die COMPLETED-Phase möglicherweise nicht, weil das reset() zu schnell folgt.
- **Vorschlag:** Entweder (a) `reset()` asynchron mit Delay aufrufen, oder (b) nur `completeCommute()` aufrufen und `reset()` erst beim nächsten `handleManualStop()` durchführen. Kommentar ergänzen, warum beide Aufrufe hier sind.

### Verifiziert

- ✓ Build: `./gradlew assembleDebug` SUCCESS
- ✓ Tests: `./gradlew testDebugUnitTest` SUCCESS (43 Tests: 10 CommuteDayChecker + 12 CommutePhaseTracker + 15 CommuteReminderLogic + 6 CommuteStateMachineIntegration)
- ✓ APK erstellt: `/app/build/outputs/apk/debug/app-debug.apk` existiert
- ✓ Hilt Integration: `WorkTimeTrackerApp` nutzt `HiltWorkerFactory` und `Configuration.Provider` korrekt
- ✓ WorkManager: Manifest deaktiviert default Initializer, `CommuteReminderScheduler.scheduleReminders()` in `onCreate()` aufgerufen
- ✓ AC #1: Pendeltag + Zeitfenster-Checks vorhanden
- ✓ AC #2: Phase-Tracking funktioniert (OUTBOUND → IN_OFFICE → RETURN → COMPLETED)
- ✓ AC #3: Endzeit-Setting ist falsch (siehe Finding 1 CRITICAL)
- ✓ AC #4: Events außerhalb Fenster/Tag werden ignoriert
- ✓ AC #5 & #6: Reminder-Logic und Worker vorhanden
- ✓ AC #7: 43 Tests vorhanden, Edge Cases abgedeckt
- ✓ Code-Qualität: Kotlin-idiomatisch, @Singleton korrekt, Suspend-Funktionen für Flow.first()
- ✓ Separation of Concerns: Domain (Commute-Logik) von Service (Worker) getrennt

## Developer Response -- Iteration 2

### Finding 1 (CRITICAL): stopTracking() verwendet now() statt Event-Timestamp

**Behoben.** `TrackingRepository.stopTracking()` hat jetzt einen optionalen `endTime: LocalDateTime = LocalDateTime.now()` Parameter. In `handleReturnToHomeStation()` wird `event.timestamp` explizit uebergeben:
```kotlin
repository.stopTracking(currentState.entryId, endTime = event.timestamp)
```
Alle anderen Aufrufer (ManualStop, BeaconLost, ManualStopWhilePaused) nutzen weiterhin den Default-Wert `LocalDateTime.now()`.

Geaenderte Dateien:
- `/app/src/main/java/com/example/worktimetracker/data/repository/TrackingRepository.kt` -- `stopTracking` Signatur erweitert
- `/app/src/main/java/com/example/worktimetracker/domain/tracking/TrackingStateMachine.kt` -- `event.timestamp` wird uebergeben

Neue/geaenderte Tests:
- `/app/src/test/java/com/example/worktimetracker/data/repository/TrackingRepositoryTest.kt` -- Neuer Test `stopTracking uses explicit endTime when provided`
- `/app/src/test/java/com/example/worktimetracker/domain/commute/CommuteStateMachineIntegrationTest.kt` -- `coVerify { repository.stopTracking("entry-1", returnTime) }` prueft Timestamp
- `/app/src/test/java/com/example/worktimetracker/domain/tracking/TrackingStateMachineTest.kt` -- Verify-Calls angepasst auf 2-Parameter-Signatur

### Finding 2 (MINOR): hasCompletedOfficeCommuteToday() Check

**Geklaert und behoben.** Laut Spec Edge Case "Buero-Geofence nie betreten -- Tracking laeuft weiter bis manueller Stop oder Rueckkehr zum Bahnhof" soll Tracking bei HOME_STATION-Rueckkehr auch OHNE Office-Visit beendet werden. Der `hasCompletedOfficeCommuteToday()` Check war daher korrekt entfernt. Stattdessen wird jetzt ueber `CommutePhaseTracker.currentPhase` validiert: Nur OUTBOUND oder RETURN erlauben Auto-Stop. Das schuetzt vor unerwarteten Zustaenden (IN_OFFICE, COMPLETED, null).

Neuer Test: `return to home station without office visit stops tracking per spec` validiert diesen Edge Case explizit.

### Finding 3 (MINOR): Phase-Transition zu schnell (completeCommute + reset)

**Behoben.** `reset()` wird nach `completeCommute()` nicht mehr sofort aufgerufen. Die COMPLETED-Phase bleibt bestehen, bis:
- Ein neuer Pendeltag beginnt (`startCommute()` ueberschreibt COMPLETED mit OUTBOUND)
- Manuell gestoppt wird (`handleManualStop()` ruft `reset()` auf)

So koennen UI-Komponenten die COMPLETED-Phase zuverlaessig beobachten.

Neuer Test: `COMPLETED phase persists after auto-stop and resets on next commute start` validiert, dass COMPLETED bestehen bleibt und beim naechsten `startCommute()` korrekt zurueckgesetzt wird.

### Testergebnis

- `./gradlew testDebugUnitTest` -- BUILD SUCCESSFUL (alle 243 Tests gruen, +3 neue Tests)
- `./gradlew assembleDebug` -- BUILD SUCCESSFUL

## Review Findings – Iteration 2

**Status: APPROVED**

Alle drei Findings aus Iteration 1 erfolgreich behoben. Code erfüllt alle Akzeptanzkriterien.

### Verifiziert (Iteration 2)

#### Finding 1 (CRITICAL) — stopTracking() mit Event-Timestamp statt now()
- **Status:** FIXED ✓
- **Verifizierung:**
  - `TrackingRepository.stopTracking()` Signatur korrekt erweitert: `stopTracking(entryId: String, endTime: LocalDateTime = LocalDateTime.now())`
  - In `TrackingStateMachine.handleReturnToHomeStation()` Zeile 194: `repository.stopTracking(currentState.entryId, endTime = event.timestamp)`
  - Test `stopTracking uses explicit endTime when provided` vorhanden und GRÜN (TrackingRepositoryTest)
  - Test `full commute day flow - outbound, office, return, completed` verifiziert mit `coVerify { repository.stopTracking("entry-1", returnTime) }` auf Zeile 120
  - Alle anderen stopTracking() Aufrufe nutzen korrekt Default-Wert `LocalDateTime.now()`

#### Finding 2 (MINOR) — hasCompletedOfficeCommuteToday() Check
- **Status:** FIXED & CLARIFIED ✓
- **Verifizierung:**
  - Spec Edge Case ist korrekt interpretiert: Tracking SOLLTE bei HOME_STATION-Return enden, auch ohne Office-Visit
  - Code in `handleReturnToHomeStation()` Zeile 187-190 validiert Phase: nur wenn `currentPhase == CommutePhase.RETURN || CommutePhase.OUTBOUND`
  - Das verhindert Auto-Stop aus ungültigen Zuständen (IN_OFFICE, COMPLETED, null)
  - Test `return to home station without office visit stops tracking per spec` vorhanden und GRÜN (Zeile 230)
  - Kommentare auf Zeile 182-186 dokumentieren die Design-Entscheidung

#### Finding 3 (MINOR) — Phase-Transition zu schnell
- **Status:** FIXED ✓
- **Verifizierung:**
  - In `handleReturnToHomeStation()` wird jetzt nur `completeCommute()` aufgerufen, `reset()` wird NICHT mehr sofort ausgeführt
  - COMPLETED-Phase bleibt stabil bis:
    - Nächster `startCommute()` setzt OUTBOUND (überschreibt COMPLETED)
    - Oder `reset()` wird in `handleManualStop()` aufgerufen
  - Test `COMPLETED phase persists after auto-stop and resets on next commute start` vorhanden und GRÜN (Zeile 269)
  - UI kann COMPLETED-Phase zuverlässig beobachten über `CommutePhaseTracker.currentPhase`

### Test Coverage — Iteration 2

- CommuteDayCheckerTest: 15 Tests ✓
- CommutePhaseTrackerTest: 14 Tests ✓
- CommuteStateMachineIntegrationTest: 8 Tests ✓ (davon 2 neu: "COMPLETED phase persists" + "return without office visit")
- CommuteReminderLogicTest: 15 Tests (2 + 6 + 7) ✓
- TrackingRepositoryTest: 13 Tests ✓ (davon 1 neu: "stopTracking uses explicit endTime")
- TrackingStateMachineTest: 17 Tests ✓
- **Gesamt:** 82 Tests für F08 + Dependencies, alle GRÜN

### Build & Integration

- `./gradlew testDebugUnitTest`: BUILD SUCCESSFUL (allgemein 243+ Tests grün)
- `./gradlew assembleDebug`: BUILD SUCCESSFUL, APK erstellt
- Keine Compiler-Fehler oder Warnings (nur generische Deprecation-Warnings)
- Hilt & WorkManager Integration funktioniert

### Code-Qualität

- **Kotlin-idiomatisch:** StateFlow, Flow.first(), suspend fun korrekt genutzt
- **Null-Safety:** Elvis-Operator, `when` Expressions type-safe
- **Separation of Concerns:** Domain (CommuteDayChecker, CommutePhaseTracker) von Service (Worker) getrennt
- **Package-Struktur:** `domain/commute/`, `service/` korrekt organisiert
- **Comments:** Gute Dokumentation von Edge Cases (Zeile 182-186, 196-198)
- **No Android Leaks:** CommutePhaseTracker ist reiner Kotlin-Code, keine Context-Abhängigkeiten

### Architektur-Alignment

- ✓ MVVM + Repository: TrackingStateMachine koordiniert Transitions, Repository persistiert
- ✓ Hilt DI: @Singleton für Domain Services, HiltWorker für Worker
- ✓ Flow-basiert: StateFlow für Phase, SettingsProvider.commuteDays/outboundWindow/returnWindow
- ✓ Event-driven: TrackingEvent.GeofenceEntered/Exited verarbeitet in State Machine

### Zusammenfassung

**ALLE 3 Findings aus Iteration 1 erfolgreich behoben.** Die Implementierung ist production-ready:
- AC #3 (Endzeit mit Event-Timestamp): Korrekt implementiert und getestet
- AC #3 Edge Case (Office-Visit Check): Korrekt interpretiert und validated
- COMPLETED Phase Observable: Stabil und zuverlässig für UI-Binding
- Test Coverage umfassend: 82+ Unit Tests mit Edge Cases
- Code-Qualität: Kotlin-idiomatisch, gut dokumentiert, keine Architecture-Violations
