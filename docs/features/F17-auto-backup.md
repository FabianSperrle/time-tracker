# F17 — Android Auto Backup (Google Drive)

## Übersicht

Automatische tägliche Datensicherung auf Google Drive und nahtlose Wiederherstellung beim Gerätewechsel durch Android Auto Backup. Kein benutzerseitiger Eingriff erforderlich — das Betriebssystem übernimmt Backup und Restore vollständig.

## Phase

Post-MVP (Phase 3)

## Abhängigkeiten

- F02 (Local Database) — Room-DB muss existieren, bevor Backup-Regeln greifen
- F09 (DataStore Settings) — DataStore-Preferences werden mitgesichert

## Entscheidung: Android Auto Backup

Drei Ansätze wurden evaluiert:

| Ansatz | Aufwand | Nutzer-UX | Offline |
|---|---|---|---|
| Drive App Data Folder API | Hoch | UI nötig | Nein |
| **Android Auto Backup** | **Minimal** | **Transparent** | **Ja** |
| Drive Files API | Sehr hoch | UI nötig | Nein |

Android Auto Backup wurde gewählt, da kein OAuth, keine API-Schlüssel und keine eigene Backup/Restore-Logik erforderlich sind.

## Was wird gesichert

| Datei | Domain | Pfad | Aktion |
|---|---|---|---|
| Room DB (Hauptdatei) | `database` | `worktime_tracker_db` | ✅ Include |
| Room WAL | `database` | `worktime_tracker_db-wal` | ❌ Exclude |
| Room SHM | `database` | `worktime_tracker_db-shm` | ❌ Exclude |
| DataStore Preferences | `file` | `datastore/settings.preferences_pb` | ✅ Include |
| SharedPreferences `tracking_state` | `sharedpref` | `tracking_state` | ❌ Exclude (transient) |

WAL und SHM werden ausgeschlossen, weil vor dem Backup ein WAL-Checkpoint durchgeführt wird (`PRAGMA wal_checkpoint(TRUNCATE)`), sodass alle Daten in der Hauptdatei konsolidiert sind.

## Umsetzung

### AppBackupAgent

`com.example.worktimetracker.service.AppBackupAgent` erweitert `BackupAgent` und checkpointet die Room WAL vor dem Backup, bevor `super.onFullBackup()` die XML-Regeln anwendet.

### Backup-Regeln (Android 6–11)

`res/xml/backup_rules.xml` — `<full-backup-content>` mit Include/Exclude-Regeln.

### Datenextraktion (Android 12+)

`res/xml/data_extraction_rules.xml` — `<cloud-backup>` und `<device-transfer>` Sektionen mit identischen Regeln.

### AndroidManifest

```xml
android:backupAgent=".service.AppBackupAgent"
android:fullBackupOnly="true"
```

### UI — Settings

Informativer Abschnitt "DATENSICHERUNG" am Ende der Einstellungen-Seite.

### UI — Onboarding

WelcomeStep informiert Nutzer über automatische Wiederherstellung bei Gerätewechsel.

## Akzeptanzkriterien

- [x] `AppBackupAgent.kt` existiert und checkpointet WAL vor dem Backup
- [x] `backup_rules.xml` enthält korrekte Include/Exclude-Regeln
- [x] `data_extraction_rules.xml` enthält korrekte cloud-backup und device-transfer Regeln
- [x] AndroidManifest referenziert `backupAgent` und setzt `fullBackupOnly="true"`
- [x] SettingsScreen zeigt "DATENSICHERUNG"-Abschnitt
- [x] OnboardingScreen WelcomeStep erwähnt automatische Wiederherstellung
- [x] `./gradlew assembleDebug` baut ohne Fehler

## Implementierungszusammenfassung

### Erstellte Dateien

- `app/src/main/java/com/example/worktimetracker/service/AppBackupAgent.kt`

### Geänderte Dateien

- `app/src/main/res/xml/backup_rules.xml` — Backup-Regeln befüllt
- `app/src/main/res/xml/data_extraction_rules.xml` — Extraktions-Regeln befüllt
- `app/src/main/AndroidManifest.xml` — backupAgent + fullBackupOnly ergänzt
- `app/src/main/java/com/example/worktimetracker/ui/screens/SettingsScreen.kt` — DATENSICHERUNG-Abschnitt
- `app/src/main/java/com/example/worktimetracker/ui/screens/OnboardingScreen.kt` — WelcomeStep-Text ergänzt

### Keine neuen Abhängigkeiten

Alle verwendeten APIs (`android.app.backup.BackupAgent`, `android.database.sqlite.SQLiteDatabase`) sind Teil des Android SDK. Keine Gradle-Änderungen nötig.
