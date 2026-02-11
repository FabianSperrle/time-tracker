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

- [ ] Tracking startet nur an konfigurierten Pendeltagen und im richtigen Zeitfenster
- [ ] Hinfahrt → Büro → Rückfahrt wird korrekt als Phasen erkannt
- [ ] Endzeit wird korrekt bei Bahnhofs-Geofence-Enter gesetzt
- [ ] Events außerhalb des Zeitfensters oder an Nicht-Pendeltagen werden ignoriert
- [ ] Reminder-Notification erscheint, wenn um 10:00 kein Tracking aktiv ist
- [ ] "Tracking vergessen zu stoppen"-Notification nach 21:00 wenn noch aktiv
- [ ] Unit-Tests für alle Edge Cases
