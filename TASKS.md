# Feature Tasks mit Abhängigkeiten

Erstelle folgende Tasks mit den angegebenen Abhängigkeiten:

## Task-Definitionen

```
Task "F01: Project Setup & Architektur"
  Status: READY
  BlockedBy: []

Task "F02: Lokale Datenbank"
  Status: READY (wird verfügbar nach F01)
  BlockedBy: [F01]

Task "F03: Tracking State Machine"
  Status: READY (wird verfügbar nach F01, F02)
  BlockedBy: [F01, F02]

Task "F04: Foreground Service"
  Status: READY (wird verfügbar nach F01, F03)
  BlockedBy: [F01, F03]

Task "F05: Permission Management"
  Status: READY (wird verfügbar nach F01)
  BlockedBy: [F01]

Task "F06: Geofence Map Config"
  Status: READY (wird verfügbar nach F01, F02, F05)
  BlockedBy: [F01, F02, F05]

Task "F07: Geofence Monitoring"
  Status: READY (wird verfügbar nach F01, F02, F03, F05, F06)
  BlockedBy: [F01, F02, F03, F05, F06]

Task "F08: Pendel-Tracking Logik"
  Status: READY (wird verfügbar nach F03, F07, F16)
  BlockedBy: [F03, F07, F16]

Task "F09: BLE Beacon Scanning"
  Status: READY (wird verfügbar nach F01, F03, F04, F05, F16)
  BlockedBy: [F01, F03, F04, F05, F16]

Task "F10: Home-Office-Tracking Logik"
  Status: READY (wird verfügbar nach F03, F09)
  BlockedBy: [F03, F09]

Task "F11: Manuelles Tracking"
  Status: READY (wird verfügbar nach F02, F03, F04)
  BlockedBy: [F02, F03, F04]

Task "F12: Einträge bearbeiten"
  Status: READY (wird verfügbar nach F01, F02)
  BlockedBy: [F01, F02]

Task "F13: Dashboard Tagesansicht"
  Status: READY (wird verfügbar nach F01, F02, F03, F11)
  BlockedBy: [F01, F02, F03, F11]

Task "F14: Dashboard Wochenansicht"
  Status: READY (wird verfügbar nach F01, F02)
  BlockedBy: [F01, F02]

Task "F15: CSV-Export"
  Status: READY (wird verfügbar nach F02, F14)
  BlockedBy: [F02, F14]

Task "F16: Einstellungen"
  Status: READY (wird verfügbar nach F01)
  BlockedBy: [F01]
```

## Parallelisierungswellen

```
Welle 1:  F01
  └─ Startbar: 1 Task

Welle 2:  F02, F05, F16
  └─ Parallel: 3 Tasks (nach F01 fertig)

Welle 3:  F03, F04
  └─ Sequentiell mit F02 (F03 → F04)

Welle 4:  F06→F07→F08 | F09→F10 | F11
  └─ 3 parallele Pfade

Welle 5:  F12, F13, F14
  └─ Parallel: 3 Tasks

Welle 6:  F15
  └─ Nach F14 fertig
```

## Orchestrierungsvorgaben

- Nach jedem abgeschlossenen Feature: APK bauen und versionieren
- Sobald eine Task als DONE markiert: Abhängige Tasks automatisch freischalten
- Max. 5 Review-Iterationen pro Feature vor manuellem Eingriff
- Bei parallelen Features: Nach jedem Abschluss Git-Commit vor nächstem Start
