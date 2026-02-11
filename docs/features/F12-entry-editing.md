# F12 — Einträge bearbeiten & korrigieren

## Übersicht

Nachträgliches Bearbeiten, Löschen und manuelles Anlegen von Tracking-Einträgen. Ermöglicht Korrekturen bei fehlerhafter automatischer Erkennung.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F01** (Project Setup) — Navigation, Compose
- **F02** (Local Database) — CRUD-Operationen auf TrackingEntry/Pause

## Requirements-Referenz

FR-M2

## Umsetzung

### Eintrags-Liste

Screen mit allen Einträgen, sortiert nach Datum (neueste zuerst). Jeder Eintrag zeigt:

```
┌──────────────────────────────────────┐
│  Di 11.02.2026                       │
│  🏢 Büro (Pendel)     8h 47min      │
│  07:45 – 16:32    Pause: 30min       │
│  ✅ Bestätigt                        │
├──────────────────────────────────────┤
│  Mo 10.02.2026                       │
│  🏠 Home Office        7h 52min      │
│  08:15 – 16:37    Pause: 30min       │
│  ⚠️ Nicht bestätigt                  │
└──────────────────────────────────────┘
```

### Entry-Editor (Bottom Sheet oder eigener Screen)

```
┌──────────────────────────────────────┐
│  Eintrag bearbeiten                  │
│                                      │
│  Datum:    [11.02.2026          ]    │
│  Typ:      [🏢 Büro (Pendel)   ▼]   │
│  Start:    [07:45              ]     │
│  Ende:     [16:32              ]     │
│                                      │
│  Pausen:                             │
│    12:00 – 12:30  [✏️] [🗑️]         │
│    + Pause hinzufügen                │
│                                      │
│  Netto: 8h 17min                     │
│                                      │
│  Notiz:   [Teammeeting am Nachmitt] │
│                                      │
│  [Löschen]           [Speichern]     │
└──────────────────────────────────────┘
```

### Funktionen

- **Startzeit ändern:** TimePicker, Validierung: Start < Ende
- **Endzeit ändern:** TimePicker, Validierung: Ende > Start
- **Typ ändern:** Dropdown (Büro/Pendel, Home Office, Manuell)
- **Datum ändern:** DatePicker
- **Pausen bearbeiten:** Einzelne Pausen hinzufügen, entfernen, Start/Ende ändern
- **Eintrag löschen:** Bestätigungsdialog ("Eintrag unwiderruflich löschen?")
- **Neuen Eintrag anlegen:** Leerer Editor mit aktuellem Datum, Start/Ende manuell setzen
- **Notiz hinzufügen:** Freitextfeld

### Validierungen

- Startzeit muss vor Endzeit liegen
- Pausen müssen innerhalb des Eintrags-Zeitraums liegen
- Pausen dürfen sich nicht überlappen
- Netto-Dauer wird live beim Bearbeiten aktualisiert
- Warnung wenn Netto-Dauer > 12h (ungewöhnlich langer Tag)

### Bestätigungs-Workflow

Automatisch erkannte Einträge haben `confirmed = false`. Der Nutzer kann sie bestätigen über:
1. Bestätigungs-Notification (F04)
2. Tap auf den Eintrag in der Liste → "Bestätigen"-Button
3. Wöchentliche Bestätigung (alle unbestätigten Einträge der Woche durchgehen)

### Akzeptanzkriterien

- [ ] Eintrags-Liste zeigt alle Einträge sortiert nach Datum
- [ ] Einträge können geöffnet und bearbeitet werden (Start, Ende, Typ, Pausen)
- [ ] Neue Einträge können manuell angelegt werden
- [ ] Einträge können gelöscht werden (mit Bestätigung)
- [ ] Pausen können hinzugefügt, bearbeitet und gelöscht werden
- [ ] Validierungen verhindern ungültige Daten (Start > Ende, überlappende Pausen)
- [ ] Netto-Dauer aktualisiert sich live beim Bearbeiten
- [ ] Bestätigungsstatus wird korrekt angezeigt und kann geändert werden
