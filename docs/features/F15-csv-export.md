# F15 — CSV-Export

## Übersicht

Export der Tracking-Daten als CSV-Datei für einen wählbaren Zeitraum. Die exportierten Daten dienen zum manuellen Übertrag ins offizielle Zeiterfassungstool der Firma.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F02** (Local Database) — Einträge aus DB laden
- **F14** (Week View) — Export-Button in der Wochenansicht

## Requirements-Referenz

FR-E1, FR-E2

## Umsetzung

### Export-Dialog

```
┌──────────────────────────────────────┐
│  Daten exportieren                   │
│                                      │
│  Zeitraum:                           │
│  ● Diese Woche (KW 07)              │
│  ○ Letzter Monat                     │
│  ○ Benutzerdefiniert                 │
│    Von: [10.02.2026]                 │
│    Bis: [14.02.2026]                 │
│                                      │
│  Vorschau:                           │
│  5 Einträge, 34h 12min gesamt       │
│                                      │
│  [Abbrechen]         [Exportieren]   │
└──────────────────────────────────────┘
```

### CSV-Format

```csv
Datum,Wochentag,Typ,Startzeit,Endzeit,Brutto (h),Pausen (h),Netto (h),Notiz
2026-02-10,Montag,Home Office,08:15,16:37,8.37,0.50,7.87,
2026-02-11,Dienstag,Büro (Pendel),07:45,16:32,8.78,0.50,8.28,Teammeeting
2026-02-12,Mittwoch,Home Office,08:03,16:33,8.50,0.50,8.00,
2026-02-13,Donnerstag,Büro (Pendel),07:30,17:00,9.50,0.50,9.00,
```

**Spalten:**
- Datum (ISO 8601)
- Wochentag (lokalisiert)
- Typ (Home Office / Büro (Pendel) / Manuell)
- Startzeit (HH:mm)
- Endzeit (HH:mm)
- Brutto-Dauer (Dezimalstunden, 2 Nachkommastellen)
- Pausen (Dezimalstunden)
- Netto-Dauer (Dezimalstunden)
- Notiz (optional)

**Encoding:** UTF-8 mit BOM (für Excel-Kompatibilität)
**Dezimaltrennzeichen:** Punkt (international) — konfigurierbar in Phase 2 (Komma für DE)
**CSV-Separator:** Semikolon (`;`) als Default für bessere Excel-Kompatibilität im DACH-Raum

### Export-Generierung

```kotlin
class CsvExporter @Inject constructor(
    private val repository: TrackingRepository
) {
    suspend fun export(
        startDate: LocalDate,
        endDate: LocalDate
    ): File {
        val entries = repository.getEntriesWithPausesInRange(startDate, endDate)
        val file = File(context.cacheDir, "arbeitszeit_${startDate}_${endDate}.csv")

        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            // BOM für Excel
            writer.write("\uFEFF")
            // Header
            writer.writeLine("Datum;Wochentag;Typ;Startzeit;Endzeit;Brutto (h);Pausen (h);Netto (h);Notiz")
            // Rows
            entries.forEach { entry ->
                writer.writeLine(formatRow(entry))
            }
        }
        return file
    }
}
```

### Teilen (Share Intent)

Nach dem Export:

```kotlin
val uri = FileProvider.getUriForFile(context, "${packageName}.fileprovider", file)
val shareIntent = Intent(Intent.ACTION_SEND).apply {
    type = "text/csv"
    putExtra(Intent.EXTRA_STREAM, uri)
    putExtra(Intent.EXTRA_SUBJECT, "Arbeitszeit KW07 2026")
}
context.startActivity(Intent.createChooser(shareIntent, "Export teilen"))
```

Ermöglicht Teilen via E-Mail, Google Drive, Messenger, etc.

### Akzeptanzkriterien

- [ ] CSV wird korrekt generiert mit allen Spalten
- [ ] Zeitraum ist wählbar (Woche, Monat, benutzerdefiniert)
- [ ] Share Intent öffnet sich mit der CSV-Datei
- [ ] Datei kann in Excel/Google Sheets korrekt geöffnet werden (Semikolon-Separator, UTF-8 BOM)
- [ ] Dezimalstunden sind korrekt berechnet (z.B. 8h 30min = 8.50)
- [ ] Leere Tage werden nicht in der CSV aufgeführt
- [ ] Dateiname enthält den Zeitraum
