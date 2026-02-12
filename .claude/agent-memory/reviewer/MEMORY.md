# Reviewer Agent Memory

## Review-Prozess

1. Immer Feature-Dokument vollständig lesen (Akzeptanzkriterien!)
2. Build und Tests TATSÄCHLICH ausführen, nicht nur annehmen
3. Alle Akzeptanzkriterien einzeln durchgehen und abhaken
4. Code auf Konsistenz prüfen (Namensgebung, Package-Struktur)

## Häufige Probleme bei Android-Projekten

### Setup-Phase (F01)
- Android Studio generiert oft Template-Namen ("My Application", "Theme.MyApplication")
- Alte Package-Namen können nach Refactoring übrig bleiben
- Immer prüfen: AndroidManifest, strings.xml, themes.xml, alte Test-Dateien

### Schweregrad-Einordnung
- CRITICAL: Blockiert Funktionalität oder bricht Build
- MAJOR: Funktioniert, aber signifikante Qualitätsprobleme
- MINOR: Kosmetik, Inkonsistenzen, nicht-kritische Verbesserungen

## Gelernte Lessons - F01

### Iteration 1 (Initial Review)
- Tests mit `./gradlew testDebugUnitTest` ausführen, nicht nur Status prüfen
- Test-Anzahl verifizieren: `find app/build/test-results -name "*.xml" -exec grep -h "testcase" {} \;`
- APK-Existenz prüfen: `ls -la app/build/outputs/apk/debug/`
- Package-Struktur mit `find app/src/main/java -type d` auflisten

### Iteration 2 (Re-Review nach Fixes)
- Bei Iteration 2: Verifizieren dass alte Packages wirklich komplett entfernt sind mit `find app/src -path "*old/package/*"`
- Theme-Konsistenz prüfen: Sowohl `values/themes.xml` als auch `values-night/themes.xml` checken
- Clean Build nach Änderungen durchführen, um Cache-Probleme auszuschließen
- Feature Status: "READY FOR INTEGRATION" dokumentieren nach erfolgreicher Approval

## Work Time Tracker spezifisch

- Package: `com.example.worktimetracker`
- Min SDK: 31 (Android 12+)
- Architektur: MVVM + Repository Pattern
- DI: Hilt
- Testing: JUnit 5 (Jupiter) + Mockk + Turbine + Robolectric

## F16 — Einstellungen (Settings)

### Review erfolgreich abgeschlossen (Iteration 1 - APPROVED)

**Status:** APPROVED - Feature erfüllt alle ACs, 16 Tests grün, Build erfolgreich.

### Wichtige Erkenntnisse
1. DataStore Preferences vs Room: F16 nutzt DataStore (Key-Value), nicht Room - das ist korrekt für Settings (siehe SettingsProvider)
2. Dialog-State-Management: Nutzt MutableStateFlow<DialogState> kombiniert mit combine() - elegante Lösung für Show/Hide
3. SettingsUiState: Nutzt PartialSettings1/2 Helper-Klassen für bessere Übersichtlichkeit bei vielen Flows
4. Validierung: TimeWindow init-Block validiert Start < End, SettingsProvider validiert Ranges
5. Multi-Select Dialog: Struktur vorhanden, UI folgt nächste Iteration (explizit dokumentiert - kein versteckter Fehler)

## F05 — Permission Management & Onboarding

### Review erfolgreich abgeschlossen (Iteration 1 - APPROVED)

**Status:** APPROVED - Alle 9 ACs erfüllt, 19 Tests grün, Build SUCCESS, App crasht nicht bei Perm-Denial.

### Wichtige Patterns
1. **PermissionChecker als Singleton Domain Service**: API-Level-aware (Build.VERSION.SDK_INT Checks für API 31, 33)
2. **StateFlow-ViewModel ohne Android-Leaks**: viewModelScope.launch korrekt, @ApplicationContext für Context-Injection
3. **Sequential Permission Requests**: LocationPermissionStep zeigt Background-Button nur wenn Fine=granted
4. **Graceful Degradation**: Elvis-Operator `powerManager?.isIgnoringBatteryOptimizations() ?: false`, conditional UI rendering
5. **Test Strategy**: MockK + mockkStatic(ContextCompat::class) für Android APIs, runTest() mit TestDispatcher
6. **Navigation**: AppNavHost mit popUpTo(Screen.Onboarding.route) { inclusive = true } für Skip-Flow

### Manifest-Setup
- Alle 7 Permissions deklariert: ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION, BLUETOOTH_SCAN, BLUETOOTH_CONNECT, POST_NOTIFICATIONS, FOREGROUND_SERVICE_LOCATION, RECEIVE_BOOT_COMPLETED, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
- BLUETOOTH_SCAN hat `neverForLocation` Flag

## F02 — Lokale Datenbank (Room)

### Review erfolgreich abgeschlossen (Iteration 2 - APPROVED)

**Status:** APPROVED - Alle 4 Findings behoben, Tests grün (35+ Tests), Build SUCCESS.

### Häufige Probleme und deren Lösung
1. **netDuration() null-safety**: Nach `.filter { it.endTime != null }` ist `it.endTime` immer noch `LocalDateTime?` - SmartCast funktioniert hier nicht mit Properties. Lösung: `it.endTime!!` verwenden (Assertion ist sicher weil Filter null ausschließt)
2. **Flow.first() in Repository**: Sollte nicht in `.map()` aufgerufen werden - blockiert Coroutine und ist nicht lazy. Lösung: `@Transaction` Queries im DAO verwenden, die direkt `TrackingEntryWithPauses` zurückgeben
3. **PlaceholderEntity**: Sollte nach F01 aus AppDatabase.entities entfernt sein - nicht mehr importiert oder verwendet
4. **stopTracking() Logic**: Sollte `getEntryById(entryId)` verwenden statt `getActiveEntry()` um die richtige Entry zu garantieren
5. **Database Version**: Wenn Version hochgezählt wird, muss migration strategy passen - Fallback: `fallbackToDestructiveMigration()` nur in Entwicklung

### Wichtige Patterns erkannt
1. **@Transaction Queries für Relationen**: `@Transaction` + `@Relation` in DAO ist effizient und lazy, besser als `.first()` in Repository
2. **TrackingEntryWithPauses als Aggregat**: @Embedded + @Relation Pattern korrekt umgesetzt
3. **TypeConverters**: ISO-8601 Strings für LocalDate/LocalDateTime (standardkonform)
