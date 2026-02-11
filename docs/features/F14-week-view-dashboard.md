# F14 — Dashboard: Wochenansicht

## Übersicht

Wöchentliche Übersicht aller Arbeitstage mit Gesamtstunden, Soll-/Ist-Vergleich und Tagesdetails. Dient als Basis für den wöchentlichen Export ins Firmen-Tool.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F01** (Project Setup) — Compose
- **F02** (Local Database) — Wocheneinträge abfragen

## Requirements-Referenz

FR-D2

## Umsetzung

### Layout

```
┌──────────────────────────────────────┐
│  ◄  KW 07 (10.–14. Feb 2026)  ►    │
│                                      │
│  Gesamt: 34h 12min / 40h 00min      │
│  ████████████████████░░░░░  85.5%    │
│                                      │
│  Mo 10.  🏠 Home Office   7h 52min  │
│  Di 11.  🏢 Büro          8h 47min  │
│  Mi 12.  🏠 Home Office   8h 03min  │
│  Do 13.  🏢 Büro          9h 30min  │
│  Fr 14.  🏠 Home Office   —         │  ← Noch kein Eintrag
│                                      │
│  Ø pro Tag:  8h 33min               │
│  Überstunden: −5h 48min             │
│                                      │
│  [CSV exportieren]                   │
└──────────────────────────────────────┘
```

### Komponenten

#### Wochennavigation

- Pfeile links/rechts zum Wechseln zwischen Wochen
- Aktuelle Woche als Default
- Tap auf KW-Header → DatePicker für beliebige Woche

#### Tageszeilen

Pro Tag:
- Datum + Wochentag
- Icon: 🏢 (Büro), 🏠 (Home Office), ✋ (Manuell)
- Netto-Arbeitszeit
- Tap → Navigation zur Tagesansicht (F13) für diesen Tag
- Unbestätigte Einträge markiert (⚠️)

#### Wochenstatistik

- Gesamtarbeitszeit (Netto, Summe aller Tage)
- Soll-Arbeitszeit (konfiguriert, z.B. 40h)
- Fortschrittsbalken (Prozent)
- Differenz (Über-/Unterstunden)
- Durchschnitt pro Tag

### Datenfluss

```kotlin
@HiltViewModel
class WeekViewModel @Inject constructor(
    private val repository: TrackingRepository,
    private val settingsProvider: SettingsProvider
) : ViewModel() {
    private val _selectedWeekStart = MutableStateFlow(currentWeekStart())

    val weekEntries: StateFlow<List<DaySummary>> =
        _selectedWeekStart.flatMapLatest { start ->
            repository.getEntriesInRange(start, start.plusDays(4))
        }.map { entries -> groupByDay(entries) }
        .stateIn(...)

    val weekStats: StateFlow<WeekStats> // Berechnet aus weekEntries
}

data class DaySummary(
    val date: LocalDate,
    val type: TrackingType?,
    val netDuration: Duration,
    val confirmed: Boolean
)
```

### Akzeptanzkriterien

- [ ] Wochenansicht zeigt Mo–Fr mit jeweiliger Arbeitszeit
- [ ] Navigation zwischen Wochen funktioniert
- [ ] Gesamtstunden werden korrekt summiert
- [ ] Soll-/Ist-Vergleich mit Fortschrittsbalken
- [ ] Tap auf Tag navigiert zur Tagesansicht
- [ ] Unbestätigte Einträge sind visuell markiert
- [ ] Export-Button ist sichtbar und führt zu F15
