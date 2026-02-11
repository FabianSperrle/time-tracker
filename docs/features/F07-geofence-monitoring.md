# F07 — Geofence Monitoring

## Übersicht

Registrierung und Überwachung von Geofences über die Google Play Services Geofencing API. Empfängt Enter/Exit-Events und leitet sie an die State Machine weiter.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F01** (Project Setup) — Google Play Services Dependency
- **F02** (Local Database) — Geofence-Zonen aus DB laden
- **F03** (State Machine) — Events an State Machine weiterleiten
- **F05** (Permissions) — Background Location Permission
- **F06** (Map Config) — Zonen müssen konfiguriert sein, bevor Monitoring startet

## Requirements-Referenz

FR-P1 bis FR-P7, NFR-B1

## Umsetzung

### Geofence-Registrierung

```kotlin
class GeofenceRegistrar @Inject constructor(
    private val geofencingClient: GeofencingClient,
    private val geofenceDao: GeofenceDao
) {
    suspend fun registerAllZones() {
        val zones = geofenceDao.getAllZonesOnce()
        val geofences = zones.map { zone ->
            Geofence.Builder()
                .setRequestId(zone.id)
                .setCircularRegion(zone.latitude, zone.longitude, zone.radiusMeters)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
    }

    suspend fun unregisterAll() {
        geofencingClient.removeGeofences(geofencePendingIntent)
    }

    // Wird aufgerufen wenn eine Zone in F06 geändert wird
    suspend fun refreshRegistrations() {
        unregisterAll()
        registerAllZones()
    }
}
```

### Geofence BroadcastReceiver

```kotlin
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent)
        if (event.hasError()) {
            Log.e("Geofence", "Error: ${event.errorCode}")
            return
        }

        val transition = event.geofenceTransition
        val triggeringGeofences = event.triggeringGeofences

        // Zone-ID → ZoneType-Lookup via DB
        // Event an State Machine weiterleiten:
        // ENTER + HOME_STATION → TrackingEvent.GeofenceEntered(HOME_STATION)
        // EXIT  + OFFICE       → TrackingEvent.GeofenceExited(OFFICE)
    }
}
```

### Event-Mapping

| Geofence Transition | Zone Type | Tracking Event |
|---|---|---|
| ENTER | HOME_STATION | `GeofenceEntered(HOME_STATION)` — Tracking Start (morgens) oder Tracking Stop (abends) |
| ENTER | OFFICE | `GeofenceEntered(OFFICE)` — Übergang Pendel → Büro |
| EXIT | OFFICE | `GeofenceExited(OFFICE)` — Rückfahrt-Beginn |
| ENTER | OFFICE_STATION | Informational (Logging) |

Die State Machine (F03) entscheidet anhand des aktuellen Zustands und des Zeitfensters, welche Aktion ausgeführt wird.

### Geofence-Limits & Refresh

- Google Play Services erlaubt max. 100 Geofences pro App (weit mehr als benötigt)
- **Geofences gehen bei Reboot verloren** → BootCompletedReceiver (F04) muss `registerAllZones()` aufrufen
- Bei Zone-Änderungen in der Karte (F06) → `refreshRegistrations()` aufrufen

### Fehlerbehandlung

- `GEOFENCE_NOT_AVAILABLE`: GPS deaktiviert → Notification an Nutzer
- `GEOFENCE_TOO_MANY_GEOFENCES`: Sollte nicht auftreten (max. 3–4 Zonen)
- `GEOFENCE_TOO_MANY_PENDING_INTENTS`: Max. 5 pro App, nur 1 wird benötigt
- Netzwerk-/GPS-Delays: Geofence-Events können bis zu 2–3 Minuten verzögert eintreffen → Bestätigungs-Notification ist daher wichtig

### Akzeptanzkriterien

- [ ] Geofences werden bei App-Start und nach Zonen-Änderungen registriert
- [ ] Enter-Events am Heimat-Bahnhof werden korrekt erkannt und an State Machine weitergeleitet
- [ ] Enter/Exit-Events am Büro werden korrekt erkannt
- [ ] Geofences werden nach Reboot neu registriert
- [ ] Fehlerzustände (GPS aus, Permission entzogen) werden dem Nutzer kommuniziert
- [ ] Geofence-Events werden mit Timestamp geloggt (für Debugging)
