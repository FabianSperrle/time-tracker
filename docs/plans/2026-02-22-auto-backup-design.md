# Design Doc: Android Auto Backup — F17

**Date:** 2026-02-22
**Status:** Implemented

---

## Problem

Nutzer möchten ihre erfassten Arbeitszeiten und Einstellungen behalten, wenn sie das Gerät wechseln oder die App neu installieren. Ohne Backup gehen alle Room-Datenbankeinträge und DataStore-Einstellungen verloren.

---

## Evaluated Approaches

### Option A: Drive App Data Folder API
- Custom `DriveServiceHelper`, OAuth-Flow, Drive-API-Dependencies
- Manuelle Backup- und Restore-Logik in der App
- Restore-UI im Onboarding erforderlich
- **Abgelehnt:** Hoher Aufwand, Fehlerquellen, Google API Console Setup

### Option B: Android Auto Backup ✅ (gewählt)
- OS übernimmt Backup und Restore vollständig
- Keine eigene Logik, kein OAuth, keine Dependencies
- Restore passiert automatisch beim ersten App-Start nach Neuinstallation
- Einziger Trade-off: Kein in-app Restore-UI (Nutzer können Backup nicht manuell auslösen)

### Option C: Drive Files API
- Nutzer sieht Backup-Dateien in Google Drive
- Deutlich komplexer als Option A
- **Abgelehnt:** Überdimensioniert für den Use Case

---

## Architecture

```
AndroidManifest.xml
  └─ android:backupAgent=".service.AppBackupAgent"
  └─ android:fullBackupOnly="true"
  └─ android:dataExtractionRules="@xml/data_extraction_rules"   (API 31+)
  └─ android:fullBackupContent="@xml/backup_rules"               (API 23–30)

AppBackupAgent.onFullBackup()
  1. PRAGMA wal_checkpoint(TRUNCATE)   ← consolidate WAL into main DB file
  2. super.onFullBackup()              ← Android reads backup_rules.xml / data_extraction_rules.xml

Google Drive (Auto Backup)
  ├─ worktime_tracker_db              ← all work time entries
  └─ datastore/settings.preferences_pb  ← all user settings
```

---

## WAL Checkpoint Strategy

Room verwendet standardmäßig WAL-Modus (Write-Ahead Logging). Ohne Checkpoint können Daten im WAL-File stehen, das vom Backup ausgeschlossen ist. Der `TRUNCATE`-Checkpoint:
1. Schreibt alle WAL-Einträge in die Hauptdatei
2. Setzt das WAL auf 0 zurück
3. Garantiert, dass die Hauptdatei beim Backup vollständig ist

---

## Backup Scope Decision

| Datei | Entscheidung | Begründung |
|---|---|---|
| `worktime_tracker_db` | Include | Kern-Daten, müssen persistent sein |
| `worktime_tracker_db-wal` | Exclude | Nach Checkpoint leer; würde Konflikte bei Restore verursachen |
| `worktime_tracker_db-shm` | Exclude | Shared memory index, nach Checkpoint irrelevant |
| `datastore/settings.preferences_pb` | Include | Alle Nutzereinstellungen (Zeitzonen, Beacon-UUID, etc.) |
| `tracking_state` SharedPreferences | Exclude | Transient session state — kein Wiederherstellungswert |

---

## Restore Flow

1. Nutzer kauft neues Gerät
2. Meldet sich mit gleichem Google-Konto an
3. Installiert APK (gleicher Package Name + Signing Key)
4. Beim ersten Start: Android ruft automatisch letzten Backup-Stand ab
5. App startet mit vollständigen historischen Einträgen und Einstellungen

**Voraussetzungen:**
- Gleicher Google-Account
- Gleicher Package Name: `com.example.worktimetracker`
- Gleicher Signing Key (Debug Key oder Release Key)

---

## Known Limitations

- **Backup-Frequenz:** Android Auto Backup läuft typischerweise einmal täglich (wenn Gerät geladen, WLAN verbunden, idle)
- **Kein manueller Trigger:** Nutzer können Backup nicht aus der App heraus erzwingen (nur via ADB für Entwickler)
- **Keine Backup-Anzeige:** Es gibt keine in-app Anzeige wann das letzte Backup lief
- **Size Cap:** Kein Limit für minSdk ≥ 31 (kein 25 MB Cap)

---

## Files Changed

| Datei | Art |
|---|---|
| `service/AppBackupAgent.kt` | Neu |
| `res/xml/backup_rules.xml` | Geändert |
| `res/xml/data_extraction_rules.xml` | Geändert |
| `AndroidManifest.xml` | Geändert |
| `ui/screens/SettingsScreen.kt` | Geändert |
| `ui/screens/OnboardingScreen.kt` | Geändert |
