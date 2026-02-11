# F04 — Foreground Service & Notifications

## Übersicht

Android Foreground Service, der das Tracking im Hintergrund am Leben hält, sowie das Notification-System für Bestätigungen, Status-Updates und manuelle Steuerung.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F01** (Project Setup) — Hilt, Service-Registrierung
- **F03** (State Machine) — Service reagiert auf State-Änderungen

## Requirements-Referenz

NFR-R1, NFR-R2, FR-P6, FR-H6, FR-M1

## Umsetzung

### Foreground Service

```kotlin
@AndroidEntryPoint
class TrackingForegroundService : Service() {
    @Inject lateinit var stateMachine: TrackingStateMachine

    // Wird gestartet, sobald Tracking aktiv ist
    // Zeigt Persistent Notification mit aktuellem Status
    // Wird gestoppt, wenn Tracking auf IDLE wechselt
}
```

**Lifecycle:**
- Start: Sobald State Machine in TRACKING oder PAUSED wechselt
- Läuft: Aktualisiert Notification alle 60 Sekunden (Elapsed Time)
- Stop: Sobald State Machine in IDLE wechselt

**Foreground Service Type:** `foregroundServiceType="location"` (Android 14+ erfordert expliziten Typ)

### Notification Channels

| Channel ID | Name | Priorität | Beschreibung |
|---|---|---|---|
| `tracking_active` | Aktives Tracking | LOW | Persistent Notification während Tracking läuft |
| `tracking_events` | Tracking Events | HIGH | Bestätigungs-Notifications (Start/Stop) |
| `tracking_reminders` | Erinnerungen | DEFAULT | Pendeltag-Reminder, Export-Erinnerung |

### Notification-Typen

#### 1. Persistent Notification (während Tracking)

```
🔴 Arbeitszeit läuft — 3h 24min
   Home Office seit 08:15
   [Pause] [Stopp]
```

- Action-Buttons: **Pause** und **Stopp** direkt aus der Notification
- Update-Intervall: 60 Sekunden
- Nicht dismissbar (Foreground Service)

#### 2. Bestätigungs-Notification (bei Start/Stop)

```
✅ Tracking gestartet: 07:45 (Pendel)
   Automatisch erkannt am Hauptbahnhof
   [Korrekt ✓] [Korrigieren]
```

```
⏹ Arbeitstag beendet: 8h 47min
   Pendel (07:45 – 16:32)
   [Bestätigen ✓] [Korrigieren]
```

- Tappable: Öffnet Entry-Editor bei "Korrigieren"
- Auto-Dismiss nach Bestätigung

#### 3. Reminder-Notification (Pendeltag ohne Tracking)

```
📋 Pendeltag (Dienstag) — kein Tracking aktiv
   Bist du heute im Büro?
   [Manuell starten] [Heute Home Office]
```

- Wird ausgelöst per WorkManager-Job um konfigurierte Uhrzeit (Default: 10:00)
- Nur an konfigurierten Pendeltagen

### Notification Actions (BroadcastReceiver)

```kotlin
class NotificationActionReceiver : BroadcastReceiver() {
    // Empfängt Actions von Notification-Buttons:
    // ACTION_PAUSE, ACTION_STOP, ACTION_CONFIRM, ACTION_CORRECT
    // Leitet an State Machine weiter
}
```

### Boot Receiver

```kotlin
class BootCompletedReceiver : BroadcastReceiver() {
    // Bei Geräteneustart:
    // 1. Prüfe persisted State
    // 2. Wenn Tracking aktiv war → Foreground Service neu starten
    // 3. Geofences neu registrieren (gehen bei Reboot verloren!)
}
```

### Akzeptanzkriterien

- [ ] Foreground Service startet bei Tracking-Beginn und zeigt Persistent Notification
- [ ] Notification zeigt korrekte Elapsed Time, aktualisiert sich minütlich
- [ ] Pause- und Stopp-Buttons in der Notification funktionieren
- [ ] Bestätigungs-Notification erscheint bei automatischem Start/Stop
- [ ] "Korrigieren"-Action öffnet den Entry-Editor
- [ ] Pendeltag-Reminder wird korrekt am konfigurierten Tag/Uhrzeit ausgelöst
- [ ] Nach Geräteneustart wird aktives Tracking wiederhergestellt
- [ ] Service überlebt Doze Mode und App-Standby
