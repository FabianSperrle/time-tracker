# F13 — Dashboard: Tagesansicht

## Übersicht

Haupt-Screen der App mit Live-Timer, Tagesübersicht und schnellem Zugriff auf manuelle Steuerung. Zeigt den aktuellen Tracking-Status und die heutige Arbeitszeit.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F01** (Project Setup) — Compose, Navigation
- **F02** (Local Database) — Tageseinträge laden
- **F03** (State Machine) — Aktuellen Status anzeigen
- **F11** (Manual Tracking) — Start/Stop/Pause-Buttons

## Requirements-Referenz

FR-D1

## Umsetzung

### Layout

```
┌──────────────────────────────────────┐
│  Dienstag, 11. Februar 2026         │
│                                      │
│  ┌────────────────────────────────┐  │
│  │     🔴 TRACKING AKTIV         │  │
│  │                                │  │
│  │       04:23:17                 │  │  ← Live-Timer (aktualisiert sich)
│  │                                │  │
│  │   🏢 Büro seit 07:45          │  │
│  │   Phase: Im Büro               │  │
│  │                                │  │
│  │   [ ⏸ Pause ]  [ ⏹ Stopp ]   │  │
│  └────────────────────────────────┘  │
│                                      │
│  Heute                               │
│  ─────────────────────────────────   │
│  Brutto:     4h 23min               │
│  Pausen:     0min                    │
│  Netto:      4h 23min               │
│  Soll:       8h 00min               │
│  Verbleibend: 3h 37min              │
│                                      │
│  Timeline                            │
│  07:45 ████████████████░░░░░░ 16:00? │
│        ↑ Start          ↑ Prognose   │
│                                      │
│  ─────── ● ─────── ● ─────── ● ──── │
│  Dashboard  Einträge  Karte  Settings│
└──────────────────────────────────────┘
```

### Komponenten

#### Status-Card

- **IDLE:** "Kein aktives Tracking" + großer Start-Button
- **TRACKING:** Live-Timer + Tracking-Typ + Phase + Pause/Stop-Buttons
- **PAUSED:** Pausierter Timer + "Weiter"-Button

Live-Timer: `LaunchedEffect` mit `delay(1000)` oder `rememberCoroutineScope` für sekundenweise Aktualisierung.

#### Tagesstatistik

- Brutto-Arbeitszeit (alle Einträge des Tages)
- Pausenzeit (Summe aller Pausen)
- Netto-Arbeitszeit (Brutto − Pausen)
- Soll-Arbeitszeit (Wochensoll ÷ Arbeitstage)
- Verbleibend (Soll − Netto, nur wenn Tracking aktiv)

#### Timeline-Visualisierung

Horizontaler Balken, der den Arbeitstag visuell darstellt:
- Grüne Blöcke: Arbeitszeit
- Graue Lücken: Pausen
- Gestrichelt: Prognose bis Feierabend (basierend auf Soll)

### Datenfluss

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TrackingRepository,
    private val stateMachine: TrackingStateMachine
) : ViewModel() {

    val trackingState = stateMachine.state
    val todayEntries = repository.getTodayEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayStats: StateFlow<DayStats> // Berechnet aus todayEntries
}
```

### Akzeptanzkriterien

- [ ] Live-Timer zeigt korrekte laufende Arbeitszeit (sekundengenau)
- [ ] Status-Card zeigt korrekten Tracking-Status (Idle/Tracking/Paused)
- [ ] Tagesstatistik zeigt korrekte Brutto/Netto/Pausen-Werte
- [ ] Soll-/Ist-Vergleich wird korrekt berechnet
- [ ] Start/Stop/Pause-Buttons funktionieren
- [ ] Screen aktualisiert sich reaktiv bei State-Änderungen
- [ ] Mehrere Einträge pro Tag werden korrekt summiert
