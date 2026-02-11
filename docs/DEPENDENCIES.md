# Feature-Abhängigkeiten & Implementierungsreihenfolge

## Feature-Übersicht

| ID | Feature | Phase |
|----|---------|-------|
| F01 | Project Setup & Architektur | MVP |
| F02 | Lokale Datenbank (Room) | MVP |
| F03 | Tracking State Machine | MVP |
| F04 | Foreground Service & Notifications | MVP |
| F05 | Permission Management & Onboarding | MVP |
| F06 | Geofence-Konfiguration via Karte | MVP |
| F07 | Geofence Monitoring | MVP |
| F08 | Pendel-Tracking Logik | MVP |
| F09 | BLE Beacon Scanning | MVP |
| F10 | Home-Office-Tracking Logik | MVP |
| F11 | Manuelles Tracking | MVP |
| F12 | Einträge bearbeiten & korrigieren | MVP |
| F13 | Dashboard: Tagesansicht | MVP |
| F14 | Dashboard: Wochenansicht | MVP |
| F15 | CSV-Export | MVP |
| F16 | Einstellungen | MVP |

---

## Abhängigkeitsgraph

```
F01 ─────┬──────────────────────────────────────────────────────┐
         │                                                      │
         ├──► F02 ──┬──► F03 ──┬──► F04                        │
         │          │          │     │                          │
         │          │          │     ├──► F08 ◄── F07 ◄── F06 ◄┤
         │          │          │     │                     │    │
         │          │          │     ├──► F10 ◄── F09      │    │
         │          │          │     │                     │    │
         │          │          ├──► F11                    │    │
         │          │          │                           │    │
         │          ├──► F12   │                           │    │
         │          │          │                           │    │
         │          ├──► F13 ◄─┘                           │    │
         │          │                                      │    │
         │          ├──► F14                               │    │
         │          │     │                                │    │
         │          │     └──► F15                         │    │
         │          │                                      │    │
         ├──► F05 ─┴──────────────────────────────────────┘    │
         │                                                      │
         └──► F16 ◄────────────────────────────────────────────┘
```

---

## Abhängigkeitsmatrix

| Feature | Hängt ab von |
|---------|-------------|
| **F01** | — |
| **F02** | F01 |
| **F03** | F01, F02 |
| **F04** | F01, F03 |
| **F05** | F01 |
| **F06** | F01, F02, F05 |
| **F07** | F01, F02, F03, F05, F06 |
| **F08** | F03, F07, F16 |
| **F09** | F01, F03, F04, F05, F16 |
| **F10** | F03, F09 |
| **F11** | F02, F03, F04 |
| **F12** | F01, F02 |
| **F13** | F01, F02, F03, F11 |
| **F14** | F01, F02 |
| **F15** | F02, F14 |
| **F16** | F01 |

---

## Implementierungsplan nach Sprints

### Sprint 1: Fundament

**Parallel implementierbar:**

```
┌─────────┐     ┌─────────┐     ┌─────────┐
│   F01   │     │   F05   │     │   F16   │
│ Project │────►│ Permis- │     │Settings │
│  Setup  │     │  sions  │     │ (Basis) │
└─────────┘     └─────────┘     └─────────┘
     │
     ▼
┌─────────┐
│   F02   │
│Database │
└─────────┘
```

- **F01** zuerst (Voraussetzung für alles)
- **F02** direkt nach F01 (wird von fast allem gebraucht)
- **F05** kann parallel zu F02 starten (braucht nur F01)
- **F16** (Basis-Settings-Screen + DataStore) kann parallel starten (braucht nur F01)

**Sprint-Ergebnis:** App startet, DB ist initialisiert, Permissions werden angefragt, Settings können konfiguriert werden.

---

### Sprint 2: Kern-Tracking-Engine

**Sequentiell:**

```
┌─────────┐     ┌─────────┐     ┌─────────┐
│   F03   │────►│   F04   │────►│   F11   │
│  State  │     │Foregrnd │     │ Manual  │
│ Machine │     │ Service │     │Tracking │
└─────────┘     └─────────┘     └─────────┘
```

- **F03** (State Machine) → Kernstück der gesamten Tracking-Logik
- **F04** (Foreground Service) → braucht State Machine als Grundlage
- **F11** (Manuelles Tracking) → braucht Service + State Machine, aber kein BLE/Geofence

**Sprint-Ergebnis:** Manuelles Tracking funktioniert Ende-zu-Ende: Start per Button → Timer läuft im Foreground Service → Stop → Eintrag in DB.

---

### Sprint 3a: Pendel-Tracking (Geofence-Pfad)

**Sequentiell:**

```
┌─────────┐     ┌─────────┐     ┌─────────┐
│   F06   │────►│   F07   │────►│   F08   │
│  Karte  │     │Geofence │     │ Pendel  │
│ Config  │     │Monitori.│     │  Logik  │
└─────────┘     └─────────┘     └─────────┘
```

- **F06** → F07 → F08 ist ein linearer Pfad (jeder Schritt braucht den vorherigen)

### Sprint 3b: Home-Office-Tracking (BLE-Pfad)

**Parallel zu Sprint 3a:**

```
┌─────────┐     ┌─────────┐
│   F09   │────►│   F10   │
│   BLE   │     │  Home   │
│ Scanner │     │ Office  │
└─────────┘     └─────────┘
```

- **F09** → F10 ist ebenfalls ein linearer Pfad
- **Kann komplett parallel zu Sprint 3a laufen** (keine Abhängigkeiten zwischen den Pfaden)

**Sprint-Ergebnis:** Beide automatischen Tracking-Modi funktionieren. App erkennt Pendeltage und Home-Office-Tage automatisch.

---

### Sprint 4: UI & Export

**Parallel implementierbar:**

```
┌─────────┐     ┌─────────┐     ┌─────────┐
│   F12   │     │   F13   │     │   F14   │
│ Entry   │     │  Day    │     │  Week   │
│ Editor  │     │  View   │     │  View   │
└─────────┘     └─────────┘     └─────────┘
                                     │
                                     ▼
                                ┌─────────┐
                                │   F15   │
                                │  CSV    │
                                │ Export  │
                                └─────────┘
```

- **F12**, **F13**, **F14** können alle parallel entwickelt werden
- **F15** braucht F14 (Export-Button in Wochenansicht), kann aber auch standalone mit eigenem Entry-Point gebaut werden

**Sprint-Ergebnis:** Vollständige MVP-App mit Dashboard, Eintrags-Editor und Export.

---

## Kritischer Pfad

Der längste sequentielle Pfad bestimmt die Mindest-Entwicklungszeit:

```
F01 → F02 → F03 → F04 → F07 → F08
  1      2     3     4     5     6   (Sprints/Iterations)
```

Dieser Pfad hat 6 sequentielle Stufen. Allerdings können ab Stufe 3 parallele Pfade die Gesamtzeit verkürzen:

```
Stufe:    1       2       3         4         5         6
          F01 ──► F02 ──► F03 ──┬─► F04 ──┬─► F07 ──► F08
                    │            │    │     │
                    │            │    │     └─► F09 ──► F10  (parallel)
                    │            │    │
                    │            │    └──► F11
                    │            │
                    │            └──► F06 (parallel, Karte braucht keinen Service)
                    │
                    ├──► F12 (parallel ab Stufe 3)
                    ├──► F14 ──► F15 (parallel ab Stufe 3)
                    │
          F05 (parallel ab Stufe 1)
          F16 (parallel ab Stufe 1)
          F13 (ab Stufe 5, braucht State Machine + DB)
```

---

## Zusammenfassung: Was kann parallel laufen?

| Parallelisierbar | Muss sequentiell sein |
|---|---|
| F05 + F16 (ab Sprint 1, parallel zu F02) | F01 → F02 → F03 → F04 (Kern-Pipeline) |
| F06 + F09 (ab Sprint 3, parallel zueinander) | F06 → F07 → F08 (Geofence-Pipeline) |
| F12 + F13 + F14 (ab Sprint 4, parallel) | F09 → F10 (BLE-Pipeline) |
| Sprint 3a + Sprint 3b (komplett parallel) | F14 → F15 (Export braucht Wochenansicht) |

---

## Empfohlene Reihenfolge für Solo-Entwickler

Wenn nur eine Person entwickelt (kein Parallelisieren möglich):

```
Woche 1:  F01 → F02 → F05
Woche 2:  F03 → F04 → F16
Woche 3:  F11 → F13 → F12
Woche 4:  F06 → F07 → F08
Woche 5:  F09 → F10
Woche 6:  F14 → F15
          ──────────────────
          Integrationstests, Bugfixes, Polish
```

**Begründung:** Nach Woche 3 hat man eine funktionsfähige App mit manuellem Tracking, Dashboard und Eintrags-Editor. Die automatische Erkennung (Wochen 4–5) baut darauf auf. Der Export kommt zuletzt, weil er erst nützlich wird, wenn Daten vorliegen.
