# F12 - Entry Editing - Manuelle Verifikation

## Build & Test Commands

Da die DevContainer-Umgebung kein Java 17 hat, müssen Tests und Build manuell in einer geeigneten Umgebung ausgeführt werden.

### Voraussetzungen
- Java 17 (OpenJDK oder Oracle)
- Android SDK (über Android Studio oder Kommandozeile)

### Befehle

```bash
# Tests ausführen
./gradlew testDebugUnitTest

# Spezifische Test-Klassen
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.data.repository.TrackingRepositoryTest"
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.ui.viewmodel.EntriesViewModelTest"
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.ui.viewmodel.EntryEditorViewModelTest"

# Build
./gradlew assembleDebug

# APK Pfad nach erfolgreichem Build
# app/build/outputs/apk/debug/app-debug.apk
```

## Manuelle UI-Tests

Nach Installation der APK auf Gerät/Emulator:

### Test 1: Eintrags-Liste anzeigen
1. App öffnen
2. Zu "Einträge"-Tab navigieren
3. Erwartung: Liste aller Einträge, sortiert nach Datum (neueste zuerst)
4. Prüfen: Datum, Typ-Icon, Netto-Dauer, Bestätigungsstatus korrekt angezeigt

### Test 2: Neuen Eintrag erstellen
1. Auf FAB (+) tippen
2. Bottom Sheet öffnet sich mit "Neuer Eintrag"
3. Datum: Standardwert = heute
4. Typ: "Manuell" auswählen
5. Start: 09:00 setzen
6. Ende: 17:00 setzen
7. Pause hinzufügen: 12:00 - 12:30
8. Netto-Dauer prüfen: 7h 30min
9. Notiz: "Test-Eintrag" eingeben
10. Bestätigt: Checkbox aktivieren
11. Speichern
12. Erwartung: Bottom Sheet schließt, Eintrag erscheint in Liste

### Test 3: Bestehenden Eintrag bearbeiten
1. Eintrag aus Liste antippen
2. Bottom Sheet mit Eintragsdaten öffnet sich
3. Startzeit ändern: z.B. 08:30
4. Netto-Dauer aktualisiert sich live
5. Speichern
6. Erwartung: Änderungen in Liste sichtbar

### Test 4: Pause hinzufügen/entfernen
1. Eintrag öffnen
2. "Pause hinzufügen" tippen
3. Dialog öffnet sich
4. Start: 14:00, Ende: 14:15 setzen
5. Hinzufügen
6. Pause erscheint in Liste
7. Netto-Dauer reduziert sich um 15min
8. Auf Löschen-Icon (Mülleimer) tippen
9. Pause verschwindet
10. Netto-Dauer steigt wieder

### Test 5: Validierungen
1. Neuen Eintrag erstellen
2. Start: 17:00, Ende: 09:00 setzen
3. Erwartung: Fehlermeldung "Startzeit muss vor Endzeit liegen"
4. Speichern-Button deaktiviert
5. Zeiten korrigieren (Start: 07:00, Ende: 22:00)
6. Erwartung: Warnung "Ungewöhnlich langer Tag (>12h)"
7. Speichern trotzdem möglich

### Test 6: Pause außerhalb Zeitraum
1. Eintrag mit Start: 09:00, Ende: 17:00
2. Pause hinzufügen: 08:00 - 08:30
3. Erwartung: Fehlermeldung "Pause liegt außerhalb des Zeitraums"
4. Speichern-Button deaktiviert

### Test 7: Überlappende Pausen
1. Eintrag mit Start: 09:00, Ende: 17:00
2. Pause 1: 12:00 - 12:45
3. Pause 2: 12:30 - 13:00
4. Erwartung: Fehlermeldung "Pausen überlappen sich"
5. Speichern-Button deaktiviert

### Test 8: Eintrag löschen
1. Eintrag öffnen
2. "Löschen"-Button tippen
3. Bestätigungsdialog erscheint
4. "Löschen" bestätigen
5. Erwartung: Bottom Sheet schließt, Eintrag aus Liste entfernt

### Test 9: Bestätigungsstatus ändern
1. Unbestätigten Eintrag öffnen (⚠️ Icon)
2. "Bestätigt"-Checkbox aktivieren
3. Speichern
4. Erwartung: Icon wechselt zu ✅

## Bekannte Limitierungen

1. **Pause-Inline-Editing**: Bestehende Pausen können nur gelöscht und neu erstellt werden, nicht direkt bearbeitet
2. **Material3 Experimental APIs**: DatePicker, TimePicker, ModalBottomSheet verwenden experimentelle APIs
3. **AssistedInject**: Erfordert Hilt 2.52+ (im Projekt vorhanden)

## Integration mit anderen Features

- **F02 (Database)**: Nutzt TrackingEntry, Pause, TrackingDao, PauseDao
- **F04 (Tracking Service)**: Einträge können bestätigt werden
- Navigation aus Bottom Bar funktioniert

## Code-Qualität

- TDD: Tests vor Implementierung geschrieben
- MVVM: Clean Separation (Repository → ViewModel → UI)
- StateFlow: Reaktive UI-Updates
- Validierungen: Im ViewModel, nicht in UI
- Material3 Design: Konsistent mit Rest der App
