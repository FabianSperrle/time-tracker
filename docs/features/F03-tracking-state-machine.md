# F03 — Tracking State Machine

## Übersicht

Zentrale Zustandsmaschine, die den Tracking-Status verwaltet (IDLE → TRACKING → PAUSED → IDLE). Reagiert auf Events von Geofences, BLE-Scanner und manuellen Inputs und koordiniert die entsprechenden Aktionen.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F01** (Project Setup) — Architektur-Grundlage
- **F02** (Local Database) — Persistierung von Zustandsänderungen

## Requirements-Referenz

Zustandsmodell (2.2), NFR-R2 (Crash Recovery)

## Umsetzung

### Zustände & Events

```kotlin
sealed class TrackingState {
    object Idle : TrackingState()
    data class Tracking(
        val entryId: String,
        val type: TrackingType,
        val startTime: LocalDateTime
    ) : TrackingState()
    data class Paused(
        val entryId: String,
        val type: TrackingType,
        val pauseId: String
    ) : TrackingState()
}

sealed class TrackingEvent {
    // Automatische Trigger
    data class GeofenceEntered(val zoneType: ZoneType) : TrackingEvent()
    data class GeofenceExited(val zoneType: ZoneType) : TrackingEvent()
    data class BeaconDetected(val uuid: String) : TrackingEvent()
    object BeaconLost : TrackingEvent()

    // Manuelle Trigger
    data class ManualStart(val type: TrackingType = TrackingType.MANUAL) : TrackingEvent()
    object ManualStop : TrackingEvent()
    object PauseStart : TrackingEvent()
    object PauseEnd : TrackingEvent()

    // System
    object AppRestarted : TrackingEvent()
}
```

### State Machine Logik

```kotlin
class TrackingStateMachine @Inject constructor(
    private val repository: TrackingRepository,
    private val settingsProvider: SettingsProvider,
    private val notificationManager: TrackingNotificationManager
) {
    private val _state = MutableStateFlow<TrackingState>(TrackingState.Idle)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    suspend fun processEvent(event: TrackingEvent) {
        val currentState = _state.value
        val newState = when (currentState) {
            is TrackingState.Idle -> handleIdle(event)
            is TrackingState.Tracking -> handleTracking(currentState, event)
            is TrackingState.Paused -> handlePaused(currentState, event)
        }
        if (newState != null) {
            _state.value = newState
            persistState(newState)
        }
    }
}
```

### Zustandsübergänge

| Aktueller Zustand | Event | Bedingung | Neuer Zustand | Aktion |
|---|---|---|---|---|
| IDLE | GeofenceEntered(HOME_STATION) | Pendeltag + Hin-Zeitfenster | TRACKING (COMMUTE) | Entry anlegen, Notification |
| IDLE | BeaconDetected | Zeitfenster gültig | TRACKING (HOME_OFFICE) | Entry anlegen, Notification |
| IDLE | ManualStart | — | TRACKING (MANUAL) | Entry anlegen |
| TRACKING | GeofenceEntered(HOME_STATION) | Rück-Zeitfenster + bereits im Büro gewesen | IDLE | Entry abschließen, Notification |
| TRACKING | BeaconLost | Timeout abgelaufen | IDLE | Entry abschließen (Endzeit = letzte Beacon-Erkennung) |
| TRACKING | ManualStop | — | IDLE | Entry abschließen |
| TRACKING | PauseStart | — | PAUSED | Pause anlegen |
| PAUSED | PauseEnd | — | TRACKING | Pause abschließen |
| PAUSED | ManualStop | — | IDLE | Pause + Entry abschließen |

### Validierungen

Die State Machine prüft vor jedem Zustandsübergang:

- **Pendel-Modus:** Ist heute ein konfigurierter Pendeltag? Liegt die aktuelle Uhrzeit im gültigen Zeitfenster (Hin/Rück)?
- **Home-Office-Modus:** Liegt die aktuelle Uhrzeit im gültigen Arbeitszeit-Zeitfenster?
- **Doppeltes Tracking:** Es kann immer nur ein aktiver Eintrag existieren. Events, die ein zweites Tracking starten würden, werden ignoriert.

### Persistierung & Recovery

Der aktuelle Zustand wird zusätzlich zur DB in `SharedPreferences` gespeichert (schneller Zugriff beim App-Start):

```kotlin
data class PersistedTrackingState(
    val state: String,          // "IDLE", "TRACKING", "PAUSED"
    val activeEntryId: String?,
    val activePauseId: String?,
    val trackingType: String?
)
```

Bei `AppRestarted`-Event:
1. Persisted State aus SharedPreferences lesen
2. Aktiven Eintrag aus DB laden und validieren
3. State Machine in den korrekten Zustand versetzen
4. Falls Foreground Service nicht läuft → neu starten

### Akzeptanzkriterien

- [ ] Alle Zustandsübergänge gemäß Tabelle funktionieren korrekt
- [ ] Ungültige Events werden ignoriert (z.B. GeofenceEntered im falschen Zeitfenster)
- [ ] Nur ein aktiver Eintrag gleichzeitig möglich
- [ ] State wird bei jedem Übergang in SharedPreferences persistiert
- [ ] Nach simuliertem App-Restart wird der korrekte Zustand wiederhergestellt
- [ ] Unit-Tests für alle Zustandsübergänge und Edge Cases
