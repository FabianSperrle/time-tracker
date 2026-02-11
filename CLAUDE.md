# Work Time Tracker

Android-App zur automatischen Arbeitszeiterfassung.
Kotlin, Jetpack Compose, Room, BLE (AltBeacon), Geofencing (Google Play Services).

## Architektur
- MVVM + Repository Pattern
- minSdk 31 (Android 12+)
- Hilt (DI), Room (DB), Jetpack Compose (UI)
- Package: `com.example.worktimetracker.{ui,domain,data,service,di,util}`

## Konventionen
- Kotlin Coding Conventions
- Tests: JUnit 5 + Mockk + Turbine (Flow-Testing)
- Commits: Conventional Commits (feat:, fix:, test:, refactor:)
- Fuege dich nicht selbst als co-author hinzu.

## Feature-Specs
Jedes Feature ist in `docs/features/F{XX}-*.md` vollständig spezifiziert
inkl. Akzeptanzkriterien. Review-Findings und Developer-Responses werden
direkt in derselben Datei dokumentiert.

## Befehle
- Build: `./gradlew assembleDebug`
- Tests: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- APK: `app/build/outputs/apk/debug/app-debug.apk`
