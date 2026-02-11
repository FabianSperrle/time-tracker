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

- [ ] Manueller Start erstellt korrekten Eintrag mit Typ MANUAL
- [ ] Manueller Stop schließt den Eintrag mit korrekter Endzeit ab
- [ ] Pausen können gestartet und gestoppt werden
- [ ] Netto-Arbeitszeit wird korrekt berechnet (Brutto minus Pausen)
- [ ] Manuelles Tracking und automatisches Tracking schließen sich gegenseitig aus
- [ ] Start/Stop funktioniert sowohl über die App als auch über die Notification
