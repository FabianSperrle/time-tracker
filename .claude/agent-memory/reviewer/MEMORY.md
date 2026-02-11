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
- Testing: JUnit 4 + Mockk + Turbine + Robolectric
