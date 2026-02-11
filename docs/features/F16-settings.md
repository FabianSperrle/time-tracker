# F16 — Einstellungen

## Übersicht

Zentraler Settings-Screen für alle konfigurierbaren Parameter: Pendeltage, Zeitfenster, Beacon-Konfiguration, Soll-Arbeitszeit und mehr.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F01** (Project Setup) — Navigation, Compose
- **F05** (Permissions) — Permission-Status anzeigen, Onboarding erneut starten

## Requirements-Referenz

Konfigurationsparameter (Kapitel 6)

## Umsetzung

### Speicherung

`DataStore<Preferences>` (Jetpack DataStore) für alle Settings. Kein Room nötig — Settings sind Key-Value-Paare.

```kotlin
class SettingsProvider @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val commuteDays: Flow<Set<DayOfWeek>>
    val outboundWindow: Flow<TimeWindow>
    val returnWindow: Flow<TimeWindow>
    val beaconUuid: Flow<String?>
    val beaconTimeout: Flow<Int>     // Minuten
    val bleScanInterval: Flow<Long>  // Millisekunden
    val workTimeWindow: Flow<TimeWindow>
    val weeklyTargetHours: Flow<Float>
    // ... setter-Methoden
}
```

### Settings-Screen Struktur

```
┌──────────────────────────────────────┐
│  ⚙️ Einstellungen                    │
│                                      │
│  PENDELN                             │
│  ──────────────────────────────────  │
│  Pendeltage          Di, Do     ▶   │
│  Zeitfenster Hin     06:00–09:30 ▶  │
│  Zeitfenster Rück    16:00–20:00 ▶  │
│  Geofence-Zonen      3 konfiguriert▶│ → Navigiert zu F06 (Karte)
│                                      │
│  HOME OFFICE                         │
│  ──────────────────────────────────  │
│  Beacon              FDA506... ▶    │ → Beacon-Setup-Screen
│  Beacon Timeout      10 min    ▶    │
│  Scan-Intervall      60 sek    ▶    │
│                                      │
│  ARBEITSZEIT                         │
│  ──────────────────────────────────  │
│  Arbeitszeitfenster  06:00–22:00 ▶  │
│  Wochensoll          40h        ▶   │
│                                      │
│  SYSTEM                              │
│  ──────────────────────────────────  │
│  Berechtigungen      Alle OK   ▶   │ → Permission-Status / Onboarding
│  Akku-Optimierung    Deaktiviert ▶  │
│  Über die App                    ▶  │
│  Daten zurücksetzen              ▶  │
└──────────────────────────────────────┘
```

### Pendeltage-Auswahl

Multi-Select-Dialog mit Wochentagen (Mo–Fr). Samstag/Sonntag ausgegraut aber wählbar für Sonderfälle.

### Zeitfenster-Editor

Wiederverwendbare Composable mit zwei TimePickern (Von/Bis):
```
┌─────────────────────────┐
│  Zeitfenster Hinfahrt   │
│  Von: [06:00]           │
│  Bis: [09:30]           │
│  [Abbrechen] [OK]       │
└─────────────────────────┘
```

### Beacon-Setup

Eigener Sub-Screen (detailliert in F09):
- UUID manuell eingeben
- "Beacon suchen" → BLE-Scan zeigt alle Beacons in der Nähe
- Signalstärke-Indikator

### Validierungen

- Zeitfenster: Start muss vor Ende liegen
- Beacon UUID: Muss gültiges UUID-Format haben
- Wochensoll: 0–80h (Dezimalstunden)
- Scan-Intervall: 10–300 Sekunden
- Beacon Timeout: 1–60 Minuten

### Akzeptanzkriterien

- [ ] Alle Parameter aus Kapitel 6 des Requirements-Docs sind konfigurierbar
- [ ] Pendeltage können ausgewählt werden (Multi-Select)
- [ ] Zeitfenster können per TimePicker angepasst werden
- [ ] Beacon-UUID kann eingegeben oder per Scan ausgewählt werden
- [ ] Änderungen werden sofort in DataStore persistiert
- [ ] Geofence-Zonen-Link navigiert zur Karten-Konfiguration
- [ ] Permission-Status wird korrekt angezeigt
- [ ] "Daten zurücksetzen" löscht alle Einträge (mit Bestätigungsdialog)
