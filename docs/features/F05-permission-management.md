# F05 — Permission Management & Onboarding

## Übersicht

Verwaltung aller benötigten Runtime-Permissions (Location, Bluetooth, Notifications) inklusive eines geführten Onboarding-Flows, der den Nutzer durch alle nötigen Berechtigungen und Battery-Optimization-Einstellungen leitet.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F01** (Project Setup) — Navigation, Compose-UI

## Requirements-Referenz

NFR-PL1 (Android 12+), Offene Fragen Punkt 4 (Battery Restrictions), Punkt 6 (Datenschutz)

## Umsetzung

### Benötigte Permissions

| Permission | Zweck | Ab API Level |
|---|---|---|
| `ACCESS_FINE_LOCATION` | Geofencing | Immer |
| `ACCESS_BACKGROUND_LOCATION` | Geofence-Monitoring im Hintergrund | Immer (separater Dialog ab API 30) |
| `BLUETOOTH_SCAN` | BLE-Beacon-Erkennung | 31+ |
| `BLUETOOTH_CONNECT` | BLE-Verbindung | 31+ |
| `POST_NOTIFICATIONS` | Notifications anzeigen | 33+ |
| `FOREGROUND_SERVICE_LOCATION` | Foreground Service mit Location | 34+ |
| `RECEIVE_BOOT_COMPLETED` | Boot Receiver (Normal Permission) | Immer |

### Onboarding Flow

Geführter Wizard beim ersten App-Start (3–4 Screens):

**Screen 1: Willkommen**
- Kurze Erklärung, was die App tut
- "Die App braucht einige Berechtigungen, um automatisch zu funktionieren"

**Screen 2: Standort-Berechtigung**
- Erklärung: "Standort wird nur genutzt, um zu erkennen, wann du am Bahnhof oder im Büro bist"
- Erst `ACCESS_FINE_LOCATION` anfragen
- Dann separat `ACCESS_BACKGROUND_LOCATION` anfragen (Android zeigt eigenen System-Dialog mit "Immer erlauben")
- Hinweis: Ohne Background Location funktioniert Geofencing nicht

**Screen 3: Bluetooth-Berechtigung**
- Erklärung: "Bluetooth wird genutzt, um den Beacon an deinem Schreibtisch zu erkennen"
- `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` anfragen

**Screen 4: Battery Optimization**
- Erklärung: "Damit das Tracking zuverlässig im Hintergrund läuft, muss die Akku-Optimierung für diese App deaktiviert werden"
- Button: "Einstellungen öffnen" → `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- Herstellerspezifische Hinweise einblenden (Samsung: "Akku" → "App-Energiesparmodus", Xiaomi: "Autostart", etc.)
- Link zu dontkillmyapp.com für das spezifische Gerät

**Screen 5: Notification-Berechtigung** (nur Android 13+)
- `POST_NOTIFICATIONS` anfragen

### Permission-Status-Check

```kotlin
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun checkAllPermissions(): PermissionStatus
    fun isLocationGranted(): Boolean
    fun isBackgroundLocationGranted(): Boolean
    fun isBluetoothGranted(): Boolean
    fun isNotificationGranted(): Boolean
    fun isBatteryOptimizationDisabled(): Boolean
}

data class PermissionStatus(
    val location: Boolean,
    val backgroundLocation: Boolean,
    val bluetooth: Boolean,
    val notification: Boolean,
    val batteryOptimization: Boolean
) {
    val allGranted: Boolean get() = location && backgroundLocation
        && bluetooth && notification && batteryOptimization
}
```

### Fortlaufende Prüfung

- Bei jedem App-Start: `PermissionChecker.checkAllPermissions()` ausführen
- Bei fehlenden Permissions: Banner im Dashboard anzeigen ("Berechtigungen fehlen — Tracking eingeschränkt")
- Tap auf Banner → Onboarding-Screen für die fehlende Permission

### Akzeptanzkriterien

- [ ] Onboarding-Flow wird beim ersten Start angezeigt
- [ ] Jede Permission wird einzeln mit Erklärung angefragt
- [ ] Background Location wird separat nach Fine Location angefragt
- [ ] Battery-Optimization-Einstellungen können direkt geöffnet werden
- [ ] Permission-Status wird bei App-Start geprüft
- [ ] Bei fehlenden Permissions wird ein Hinweis-Banner angezeigt
- [ ] App crasht nicht bei verweigerten Permissions (graceful degradation)
- [ ] Onboarding kann übersprungen und später nachgeholt werden
