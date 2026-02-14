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

## F12 — Einträge bearbeiten & korrigieren

### Review erfolgreich abgeschlossen (Iteration 2 - APPROVED)

**Status:** APPROVED - Alle 6 Findings aus Iteration 1 behoben, 26 Tests (davon 5 neu), Code syntaktisch korrekt.

### Kritische Patterns (Iteration 2)
1. **Suspend Function für Race Condition**: saveEntry() ist suspend fun, alle Repository-Calls sequenziell (keine viewModelScope.launch)
2. **State Tracking für Deletions**: originalPauses Field trackt geladene Pausen, saveEntry() vergleicht und löscht nicht-existierende
3. **DatePicker State Management**: DatePickerState in lokalem Block erstellt, Update nur im confirmButton Callback
4. **Pause Validierung Complete**: Prüft pause.startTime < pause.endTime UND zeitlichen Overlap UND Entry-Range
5. **AssistedInject mit Factory**: Hilt 2.52+ required, Factory Pattern für optionale entryId

### Findings Iteration 1 → Iteration 2 (Alle behoben)
1. **CRITICAL DatePicker Race** → Suspend function + local state
2. **MAJOR Pause Deletion** → originalPauses tracking + delete loop
3. **MAJOR saveEntry() Race** → suspend fun pattern
4. **MINOR AssistedInject Docs** → KDoc added
5. **MINOR Validation Naming** → buildValidationMessages() with comments
6. **MINOR Pause Validation** → pause.startTime >= pause.endTime check added

## F16 — Einstellungen (Settings)

### Review erfolgreich abgeschlossen (Iteration 1 - APPROVED)

**Status:** APPROVED - Feature erfüllt alle ACs, 16 Tests grün, Build erfolgreich.

### Wichtige Erkenntnisse
1. DataStore Preferences vs Room: F16 nutzt DataStore (Key-Value), nicht Room - das ist korrekt für Settings (siehe SettingsProvider)
2. Dialog-State-Management: Nutzt MutableStateFlow<DialogState> kombiniert mit combine() - elegante Lösung für Show/Hide
3. SettingsUiState: Nutzt PartialSettings1/2 Helper-Klassen für bessere Übersichtlichkeit bei vielen Flows
4. Validierung: TimeWindow init-Block validiert Start < End, SettingsProvider validiert Ranges
5. Multi-Select Dialog: Struktur vorhanden, UI folgt nächste Iteration (explizit dokumentiert - kein versteckter Fehler)

## F15 — CSV-Export

### Review erfolgreich abgeschlossen (Iteration 2 - APPROVED)

**Status:** APPROVED - Alle 4 Findings aus Iteration 1 behoben, 15 Tests, Code RFC 4180 konform.

### Wichtige Patterns erkannt
1. **RFC 4180 CSV-Escaping**: escapeCsvField() checkt Semikolon/Quotes/Newlines, escaped Quotes durch Verdopplung
2. **CSV-Parser in Tests**: parseCsvRow() implementiert echtes RFC 4180 Parsing für Validierung (nicht String-Matching)
3. **UTF-8 BOM für Excel**: writer.write("\uFEFF") am Anfang für DACH-Region Excel-Kompatibilität
4. **Suspend-fun Export**: export() ist suspend fun, repository.getEntriesInRange().first() für Coroutine-Integration
5. **File Filtering**: filter { it.entry.endTime != null } entfernt incomplete entries vor Export

### Findings Iteration 1 → Iteration 2 (Alle behoben)
1. **CRITICAL LocalDate** → LocalDate.of(2026, 2, 10) korrekt
2. **MAJOR RFC 4180** → escapeCsvField() implementiert mit Quote-Doubling
3. **MAJOR CSV Parsing** → parseCsvRow() RFC 4180 konform, 3 neue Tests (Semicolons, Quotes, Newlines)
4. **MINOR Spec Consistency** → CSV-Beispiel von Kommas auf Semikolons aktualisiert

### Test-Verifikation
- 15 Tests: 10 Original + 3 Escaping-Tests + 2 weitere
- Alle Tests nutzen runTest{}, mockk(), CSV-Parser für echte Validierung
- Coverage: Struktur, Formatierung, Dezimalstunden, Pausen, Escaping, Sortierung, Incomplete Entries

### AC-Verifikation (7/7 erfüllt)
- CSV mit 9 Spalten, Zeitraum wählbar, Share Intent, Excel-Kompatibilität (UTF-8 BOM, Semikolon)
- Dezimalstunden korrekt (8h 22min → 8.37h), leere Tage gefiltert, Dateiname mit Zeitraum

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

## F03 — Tracking State Machine

### Review erfolgreich abgeschlossen (Iteration 2 - APPROVED)

**Status:** APPROVED - Alle 3 Findings aus Iteration 1 behoben, 40+ Tests grün, Build SUCCESS.

### Findings Iteration 1 → Iteration 2 (Behoben)
1. **CRITICAL - TrackingNotificationManager entfernt**: Constructor reduziert auf 3 Parameter (Repository, SettingsProvider, StateStorage). Keine Imports mehr für CoroutineScope/Dispatchers/SupervisorJob.
2. **MAJOR - "Bereits im Büro gewesen" Check implementiert**: DAO-Query `hasCompletedOfficeCommute(date)` mit EXISTS. Repository-Methode `hasCompletedOfficeCommuteToday()`. State Machine prüft in `handleGeofenceEnteredWhileTracking()` vor stopTracking(). Zwei neue Tests: mit/ohne Prior Office Visit.
3. **MINOR - CoroutineScope Lifecycle optimiert**: Init-Block entfernt. `restoreState()` jetzt public suspend fun mit KDoc. Aufrufer muss Lifecycle-Management übernehmen (sauberer design).

### Verifiziert Iteration 2
- ✓ Build: clean build SUCCESS (11s)
- ✓ Tests: 40+ Unit Tests grün (17 TrackingStateMachine + 11 TrackingStateStorage + 12+ Repository)
- ✓ APK: erfolgreich erstellt
- ✓ Alle 6 Akzeptanzkriterien erfüllt
- ✓ Keine Speicherlecks mehr (CoroutineScope entfernt)
- ✓ Repository Pattern und Hilt DI korrekt

### Patterns erkannt
1. **Sealed Classes für State Machine**: Elegant und type-safe
2. **Nested Handler Methods**: handleGeofenceEnteredWhileIdle, handleTracking, etc. - gute Separation
3. **Turbine für Flow Testing**: Test nutzt Turbine richtig (awaitItem, expectNoEvents)
4. **Validation in init**: TimeWindow init-Block validiert start < end (siehe TimeWindow.kt)
5. **Query mit EXISTS**: Effizient für Existence Checks statt count() oder first()

## F04 — Foreground Service & Notifications

### Review Iteration 2 - APPROVED ✓

**Status:** APPROVED - Alle 5 Findings aus Iteration 1 erfolgreich behoben. 26/26 Service-Tests grün, Build SUCCESS.

### Wichtige Patterns implementiert
1. **TrackingServiceManager als Application-Level Singleton:**
   - Observiert StateFlow mit `distinctUntilChanged()` → keine Duplikate
   - Startet Service automatisch bei State-Wechsel (Idle → Tracking/Paused)
   - Wird in App.onCreate() initialisiert via `startObserving()`
   - Hilt-Injection: `@ServiceDispatcher` Qualifier für TestDispatcher-Kompatibilität

2. **Job-Management für Memory Safety:**
   - Private `stateJob` und `updateJob` für explizite Lifecycle-Bindung
   - Schutz vor Mehrfach-Initialisierung: `if (stateJob?.isActive == true) return` in onCreate()
   - Cleanup in onDestroy(): beide Jobs cancelt, serviceScope cancelt
   - **Pattern:** Private nullable Job? mit expliziter Verwaltung

3. **BroadcastReceiver mit goAsync():**
   - Ersetzt unverwaltete `CoroutineScope(Dispatchers.Default).launch`
   - `goAsync()` + PendingResult + try/finally
   - **Benefit:** Verhindert ANR, Best Practice für async BroadcastReceiver

4. **Test-Pattern für Services:**
   - Robolectric hat Limits: `startForeground()` nicht vollständig simuliert
   - Lösung: `spyk<NotificationManager>()` für verfügbare Verifikation
   - `runTest { }` für async-Szenarien
   - **Wichtig:** Kommentare dokumentieren Limitierungen (Transparenz statt Fake-Tests)

### Verifiziert Iteration 2
- ✓ TrackingServiceManager: 5/5 Tests grün
- ✓ TrackingForegroundService: 7/7 Tests grün (vorher 3)
- ✓ NotificationActionReceiver: 4/4 Tests + goAsync() implementiert
- ✓ BootCompletedReceiver: 3/3 Tests
- ✓ NotificationChannelManager: 7/7 Tests
- ✓ Build: SUCCESS, APK erstellt
- ✓ AC #1, #2, #3, #7 erfüllt

### Review Iteration 1 - CHANGES_REQUESTED

**Status:** CHANGES_REQUESTED - 5 Findings (1 CRITICAL, 3 MAJOR, 1 MINOR)

### Kritische Findings
1. **CRITICAL - AC #1 nicht erfüllt**: Service wird nicht beim normalen Tracking-Start gestartet
   - AC besagt "Foreground Service startet bei Tracking-Beginn" aber nur BootCompletedReceiver hat `startForegroundService()` Code
   - Service wird beobachtet (StateFlow.collectLatest), aber never started außer nach Boot
   - Fehlt: Application-Context Component (TrackingServiceManager) die auf StateMachine.state horcht

2. **MAJOR - Memory Leak Risk in CoroutineScope**:
   - Service erzeugt `CoroutineScope(Dispatchers.Default + Job())` in onCreate()
   - `collectLatest {}` läuft endlos bis onDestroy()
   - Sollte ggf. Job-Initialisierung checken gegen Mehrfach-onCreate()
   - Oder: Scope in onStartCommand() statt onCreate() initialisieren

3. **MAJOR - Schwache Tests (qualitativ)**:
   - NotificationChannelManagerTest: 7 Tests grün aber testen nur Konstanten
   - TrackingForegroundServiceTest: 3 Tests, checken nicht ob `startForeground()` aufgerufen wurde
   - NotificationActionReceiverTest: 4 Tests, only constants
   - BootCompletedReceiverTest: 3 Tests, testen nicht echtes Verhalten
   - Robolectric limitation: Würde `spyk<NotificationManager>` + `verify {}` brauchen

4. **MAJOR - Fehlende Test-Abdeckung AC #2**: 60-Sekunden Notification Update wird nicht getestet
   - Kein Test für `startPeriodicUpdates()` oder `updateNotification()`
   - Würde `runTest { advanceTimeBy(60_000) }` brauchen
   - Aktuell erkannt: Tests als Dokumentation, echte Validierung auf realem Gerät

5. **MINOR - NotificationActionReceiver CoroutineScope nicht idiomatisch**:
   - `CoroutineScope(Dispatchers.Default).launch { }` für jeden Broadcast
   - Sollte `goAsync()` mit PendingResult verwenden (Android Q+) oder WorkManager
   - Funktioniert aber für kurze Fire-and-Forget Operation

### Verifiziert Iteration 1
- ✓ Build: SUCCESS, APK erstellt (28MB)
- ✓ Tests: 17 Service-Tests grün (aber qualitativ schwach)
- ✓ Manifest: Alle Receiver + Service registered
- ✓ Integration: NotificationChannelManager in WorkTimeTrackerApp.onCreate()

### Lessons für F04
1. **AC-Markierung vs Implementation**: AC als [x] markiert aber Code fehlt völlig - wurde übersehen?
2. **Service-Lifecycle**: Service wird via `startForegroundService()` gestartet, müsste zentral in App-Context komponente erfolgen
3. **Test-Anforderungen für Services**: Robolectric hat Limits - aktuelle Tests sind placeholder
4. **Dokumentiert aber nicht implementiert**: "Nächste Schritte" erwähnt "UI-Integration", aber AC #1 sollte bereits done sein

## F08 — Pendel-Tracking Logik

### Review erfolgreich abgeschlossen (Iteration 2 - APPROVED) ✓

**Status:** APPROVED - Alle 3 Findings aus Iteration 1 behoben, 82+ Tests grün, Build SUCCESS.

### Findings Iteration 1 → Iteration 2 (ALLE BEHOBEN)

1. **CRITICAL - stopTracking() mit Event-Timestamp**
   - FIXED: `TrackingRepository.stopTracking(entryId: String, endTime: LocalDateTime = LocalDateTime.now())`
   - `handleReturnToHomeStation()` übergibt `event.timestamp` statt `LocalDateTime.now()`
   - Test verifiziert Timestamp-Übergabe korrekt

2. **MINOR - hasCompletedOfficeCommuteToday() Check**
   - CLARIFIED & FIXED: Edge Case richtig interpretiert
   - Tracking SOLL bei HOME_STATION-Return enden auch ohne Office-Visit
   - Code validiert Phase statt Office-Visit Check (sauberer)
   - Test `return to home station without office visit` validates korrektes Verhalten

3. **MINOR - Phase-Transition zu schnell (completeCommute + reset)**
   - FIXED: `reset()` wird NICHT mehr nach `completeCommute()` aufgerufen
   - COMPLETED-Phase bleibt persistent bis nächster `startCommute()` oder `handleManualStop()`
   - UI kann COMPLETED-Phase zuverlässig beobachten
   - Test `COMPLETED phase persists` validates Persistenz

### Test Coverage F08
- CommuteDayCheckerTest: 15 Tests
- CommutePhaseTrackerTest: 14 Tests
- CommuteStateMachineIntegrationTest: 8 Tests (+2 neue für Findings)
- CommuteReminderLogicTest: 15 Tests
- TrackingRepositoryTest: 13 Tests (+1 neu für explicit endTime)
- TrackingStateMachineTest: 17 Tests
- **Alle GRÜN, keine Failures**

### Patterns erkannt für zukünftige Features

1. **Event Timestamp Consistency**: Geofence-Events sollten immer mit `event.timestamp` persistiert werden, nicht `LocalDateTime.now()`
2. **Phase Persistence Design**: Enums wie COMPLETED sollten persistent sein bis aktiv überschrieben, nicht sofort resettet
3. **Edge Case Validation über State**: Statt separate Datenbank-Queries (hasCompletedOfficeCommuteToday), einfach State Machine Phase checken
4. **StateFlow Observability**: Achten dass UI-kritische States observable bleiben (nicht zu schnell resettet)

## F06 — Geofence-Konfiguration via Karte

### Review erfolgreich abgeschlossen (Iteration 1 - APPROVED)

**Status:** APPROVED - Alle 8 Akzeptanzkriterien erfüllt, 23 Tests grün, Build SUCCESS.

### Wichtige Patterns erkannt
1. **Google Maps Compose:** maps-compose 4.4.1 + play-services-maps 18.2.0 in libs.versions.toml
2. **StateFlow mit Temporary State:** MapUiState enthält temporäre Felder während Editing
3. **Live Circle-Vorschau:** Radius-Slider aktualisiert Circle in Echtzeit (Compose re-render)
4. **Marker + Circle:** Existierende Zonen + temporäre Zone beide gerendert
5. **Bottom Sheet Edit Pattern:** ModalBottomSheet mit skipPartiallyExpanded = true
6. **Validierte Save:** `enabled = name.isNotBlank() && position != null` (Smart UI!)
7. **hasRequiredZones():** Prüft beide HOME_STATION und OFFICE nicht leer

### Verifiziert Iteration 1
- ✓ Build: assembleDebug SUCCESS, APK erstellt
- ✓ Tests: 23 Unit Tests (9 Repo + 14 ViewModel), alle grün
- ✓ MVVM: MapViewModel @HiltViewModel, GeofenceRepository @Singleton
- ✓ Room: GeofenceZone in AppDatabase, DAO mit CRUD
- ✓ Hilt: DatabaseModule.provideGeofenceDao() korrekt
- ✓ F02 Integration: Entity + DAO vorhanden
- ✓ F05 Integration: isMyLocationEnabled, Manifest permissions
- ✓ F01 Integration: MapScreen in AppNavHost
- ✓ Nullsafety: temporaryPosition?.let (keine !! nötig)
- ✓ Coroutines: viewModelScope, Flow lazy, keine Leaks

### Bekannte Limitierungen (dokumentiert)
1. AC#4 Geocoding = [ ] nicht [x], wird F07
2. API Key Placeholder (User setzt selbst)
3. Kein Marker Drag (Tap-to-Place reicht)
4. Geofence-Registrierung erst F07

## F11 — Manuelles Tracking

### Review erfolgreich abgeschlossen (Iteration 1 - APPROVED)

**Status:** APPROVED - Alle 6 ACs erfüllt, 10 Tests grün (7 ViewModel + 3 Integration), Build SUCCESS.

### Wichtige Erkenntnisse
1. **UI State Machine**: DashboardViewModel nutzt `map()` auf TrackingStateMachine.state + `stateIn()` für UI-State - elegant und effizient
2. **Composable Struktur**: Drei Sub-Composables (IdleContent, TrackingContent, PausedContent) für saubere Separation
3. **Timer-Implementierung**: LaunchedEffect mit `while(true) + delay(1000)` ist einfach und batteriesparsam
4. **Dropdown für Typ-Auswahl**: Drei Typen (Home Office, Büro, Sonstiges) alle mapped zu TrackingType Enum
5. **Integration mit F04**: NotificationActionReceiver nutzt `goAsync()` + `PendingResult` für ANR-sichere Broadcasts
6. **Pause-Logik**: Vollständig implementiert in State Machine und Repository (startPause/stopPause mit Pause-ID-Rückgabe)
7. **Test-Pattern**: Integration-Tests mit MockK (coEvery/coVerify) + Turbine für State-Flows - genau richtig

### Verifiziert Iteration 1
- ✓ Build: assembleDebug SUCCESS (APK 31MB)
- ✓ Tests: 10 Tests grün (DashboardViewModelTest 7, ManualTrackingIntegrationTest 3)
- ✓ APK: erfolgreich erstellt
- ✓ Alle 6 Akzeptanzkriterien erfüllt
- ✓ Keine Speicherlecks (viewModelScope.launch korrekt genutzt)
- ✓ MVVM + Repository + State Machine Patterns korrekt umgesetzt

### Patterns erkannt
1. **Sealed Class für UI State**: DashboardUiState mit Idle, Tracking, Paused - typsicher
2. **StateFlow Transformation**: `state.map { ... }.stateIn()` für View-Model-Derived State
3. **Dropdown mit State**: `remember { mutableStateOf(false) }` für show/hide, korrekt
4. **When-Expression für Typen**: Vollständige Pattern-Matching ohne else - sicher
5. **Tuple-Capture in Mocks**: Integration-Tests capturen Entry-IDs mit answers-Blöcken, gut für State-Testing

### Known Limitations (alle dokumentiert als MVP):
1. Kein Typ-Wechsel während Tracking → Design-Entscheidung, OK für Phase 1
2. Timer-Granularität UI vs Notification → Batterie-Optimierung, OK
3. Keine Pause-Historie in UI → Planmäßig für Phase 2
4. Abhängigkeit von F04 Service → Dokumentiert, Dependency korrekt gehandhabt

## F08 — Pendel-Tracking Logik

### Review Iteration 1 - CHANGES_REQUESTED

**Status:** CHANGES_REQUESTED - 3 Findings (1 CRITICAL, 2 MINOR)

### Kritische Findings

1. **CRITICAL - AC #3 nicht erfüllt: Endzeit wird mit LocalDateTime.now() statt Event-Timestamp gesetzt**
   - `TrackingRepository.stopTracking()` nutzt `LocalDateTime.now()` statt Event-Zeitstempel
   - Sollte: `stopTracking(entryId, endTime: LocalDateTime)` mit Event-Zeitstempel übergeben
   - Impact: Zeitversatz wenn Verarbeitung verzögert ist

2. **MINOR - hasCompletedOfficeCommuteToday() Check entfernt, Spec Edge Case unklar**
   - Alte Logik prüfte "Büro mindestens einmal besucht", neue Logik prüft nur TrackingType
   - Spec sagt: "Büro-Geofence nie betreten → Tracking läuft weiter" aber Code stoppt trotzdem
   - Klären: Ist das beabsichtigte Verhalten oder wurde Validierung versehentlich entfernt?

3. **MINOR - Phase-Transition COMPLETED → null ist zu schnell**
   - `completeCommute()` + sofort `reset()` in `handleReturnToHomeStation()`
   - Phase COMPLETED existiert effektiv nur einen Event-Zyklus lang
   - Könnte UI-Observer stören die auf Phase horchen

### Positiv identifiziert
- ✓ Build: SUCCESS, Tests: 43 grün (10 CommuteDayChecker + 12 CommutePhaseTracker + 15 CommuteReminderLogic + 6 CommuteStateMachineIntegration)
- ✓ APK: erstellt
- ✓ Hilt: WorkTimeTrackerApp implementiert Configuration.Provider + HiltWorkerFactory
- ✓ WorkManager: CommuteReminderScheduler.scheduleReminders() in App.onCreate() aufgerufen
- ✓ Code-Qualität: Kotlin-idiomatisch, @Singleton, Suspend-Funktionen für Flow.first()
- ✓ AC #1, #2, #4, #5, #6, #7: alle erfüllt
- ✓ AC #3: Logik vorhanden aber mit falscher Zeitstempel-Quelle

### Patterns erkannt
1. **CommuteDayChecker als Singleton Domain Service**: Suspend-Funktionen, Flow.first() aus Settings
2. **CommutePhaseTracker mit StateFlow**: Saubere State-Machine mit validierten Übergängen
3. **Pure Logic in CommuteReminderLogic**: Object statt Klasse, keine Dependencies
4. **HiltWorker**: @HiltWorker + @AssistedInject für WorkManager-Integration
5. **PeriodicWorkRequest mit KEEP Policy**: Verhindert Duplikate bei App-Restarts

## F07 — Geofence Monitoring

### Review Iteration 1 - CHANGES_REQUESTED

**Status:** CHANGES_REQUESTED - 3 Findings (2 MAJOR, 1 MINOR)

### Kritische Findings
1. **MAJOR - BroadcastReceiver.onReceive() nutzt unsichere CoroutineScope** (Line 98)
   - `CoroutineScope(Dispatchers.Default).launch {}` ohne Binding an Receiver-Lifecycle
   - Kann zu Memory Leaks führen, wenn Coroutine länger läuft als Receiver lebt
   - Sollte: `goAsync()` API nutzen oder `PendingResult` Pattern. Best Practice seit Android Q+

2. **MAJOR - Fehlende Exception-Handling in onReceive()** (Lines 99-117)
   - Loop über `triggeringGeofences` ruft `geofenceDao.getZoneById(zoneId)` auf
   - Falls DAO Exception wirft, wird sie nicht abgefangen → potentiell ANR
   - Sollte: try-catch Block um Zone-Lookup mit Log auf Error

3. **MINOR - Test-Abdeckung für onReceive() unvollständig**
   - GeofenceBroadcastReceiverTest testet nur `mapTransitionToEvent()` (statische Methode)
   - onReceive() komplexe Logik (Zone-Lookup, State Machine Call, Logging) nicht getestet
   - Grund: GeofencingEvent.fromIntent() nicht mockbar - würde Instrumented Tests brauchen
   - Nicht blockierend: E2E Testing ist akzeptabel, dokumentieren aber Limitation

### Positiv identifiziert
- ✓ GeofenceRegistrar: Korrekt implementiert mit Task-Listeners + Exception-Handling
- ✓ GeofenceModule: FLAG_MUTABLE korrekt für GeofencingClient (Geofencing API modifiziert Intent)
- ✓ BootCompletedReceiver: Ruft registerAllZones() nach Boot auf ✓
- ✓ WorkTimeTrackerApp: Ruft registerAllZones() bei App-Start auf ✓
- ✓ refreshRegistrations() Placeholder für F06 (OK, noch nicht implementiert)
- ✓ Tests: 14 Tests grün (6 Registrar + 8 Receiver Event-Mapping), alle bestanden
- ✓ Build: assembleDebug SUCCESS, APK erstellt
- ✓ AC #1-6 Struktur vorhanden, nur onReceive() Lifecycle-Issue

### Wichtige Pattern erkannt
1. **PendingIntent.FLAG_MUTABLE notwendig**: Geofencing API modifiziert Intent mit Event-Daten
2. **DAO im BroadcastReceiver**: Hilt-Injection via @AndroidEntryPoint funktioniert
3. **Geofence ID = Zone ID**: request ID mapped direkt zu Zone PK für Lookup
4. **handleGeofenceError()**: Switch über ErrorCodes mit LOG → Notification kommt F09

## F09 — BLE Beacon Scanning

### Review erfolgreich abgeschlossen (Iteration 2 - APPROVED) ✓

**Status:** APPROVED - Alle 5 Findings aus Iteration 1 behoben, 29+ Tests grün, Build SUCCESS, alle ACs erfüllt.

### Findings Iteration 1 → Iteration 2 (ALLE BEHOBEN)

1. **CRITICAL - stopTracking() soll lastSeenTimestamp statt now() verwenden**
   - FIXED: `TrackingEvent.BeaconLost` um `lastSeenTimestamp: LocalDateTime?` Parameter erweitert
   - `TrackingStateMachine.handleBeaconLost()` nutzt `event.lastSeenTimestamp ?: event.timestamp` als endTime
   - `BeaconScanner.onBeaconLostFromRegion()` captured lastSeenTimestamp vor delay() und konvertiert zu LocalDateTime (Zeile 247-254)
   - 3 neue Tests in TrackingStateMachineTest (19 Tests gesamt, +3 für BeaconLost)

2. **MAJOR - Mehr Tests für Timeout-Mechanismus**
   - BeaconScannerTest: 5 → 29 Tests erweitert (Nested Classes Struktur)
   - 8 TimeoutTests: AC #3 (Kurze Abwesenheiten), AC #4 (Timeout-Trigger), AC #5 (lastSeenTimestamp)
   - `advanceTimeBy()` für Timeout-Simulation + `verify()` für Event-Propagierung
   - 5 TimeWindowHelperTests: isTimeInWindow() Edge Cases (in/out, future/past/same)

3. **MAJOR - BeaconScheduler für Zeitfenster**
   - `startScheduledMonitoring()`: Endlosschleife mit Zeitfenster-Management (Zeile 164-205)
   - `stopScheduledMonitoring()`: Cleanup beide Jobs + Monitoring (Zeile 210-216)
   - `isTimeInWindow()` und `millisUntilTime()` als Companion Objects (testbar ohne Android-Deps)
   - TrackingForegroundService nutzt `startScheduledMonitoring()` statt `startMonitoring()` (Zeile 62)

4. **MAJOR - Exception-Handling in AltBeacon callback**
   - `didEnterRegion()` mit try/catch (Zeile 110-117)
   - `didExitRegion()` mit try/catch (Zeile 120-127)
   - Graceful handling wenn Beacon nicht konfiguriert
   - `onBeaconDetected()` nutzt `currentConfig ?: getBeaconConfig()` mit Exception-Handling

5. **MINOR - stopMonitoring() State reset**
   - `currentRegion = null` (Zeile 149)
   - `currentConfig = null` (Zeile 150)
   - `lastSeenTimestamp = null` (Zeile 153)
   - `isMonitoringActive = false` (Zeile 154)
   - Test "stopMonitoring resets all state" verifiziert totalen Reset (Zeile 346-365)

### Test Coverage F09 Iteration 2

BeaconScannerTest Nested Classes:
- ConfigurationTests: 2 Tests PASSED
- InitialStateTests: 3 Tests PASSED
- BeaconDetectionTests: 4 Tests PASSED (processEvent korrekt called)
- TimeoutTests: 8 Tests PASSED (AC #3/4/5 verifiziert)
- StopMonitoringTests: 3 Tests PASSED (State Reset komplett)
- TimeWindowHelperTests: 5 Tests PASSED (millisUntilTime Edge Cases)

BeaconConfigTest: 5 Tests PASSED (Defaults korrekt)
TrackingStateMachineTest: 19 Tests PASSED (+3 für BeaconLost Event)

**Gesamt: 264 Unit Tests grün, 0 Failures, Build SUCCESS**

### AC-Erfüllung Status F09

- AC #1 "Beacon innerhalb 2 Intervalle erkannt" ✓ (Callbacks vorhanden, nur Hardware testbar)
- AC #2 "BeaconDetected-Event an State Machine" ✓ (processEvent Zeile 231)
- AC #3 "Kurze Abwesenheiten unterbrechen nicht" ✓ (Test Zeile 260-280)
- AC #4 "Stoppt nach Timeout" ✓ (Test Zeile 228-243)
- AC #5 "Endzeit auf letzten Beacon-Kontakt" ✓ (lastSeenTimestamp propagiert)
- AC #6 "Scanning nur im Zeitfenster" ✓ (startScheduledMonitoring)
- AC #7 "Background-Funktion im Service" ✓ (in TrackingForegroundService)

### Wichtige Patterns erkannt F09

1. **Instant ↔ LocalDateTime Konvertierung**: `LocalDateTime.ofInstant(it, ZoneId.systemDefault())` korrekt (Zeile 252-253)
2. **Companion Objects für Helper-Methoden**: `isTimeInWindow()` und `millisUntilTime()` statisch, vollständig testbar
3. **Reflection-Workaround für Unit Tests**: `setCurrentConfig()` umgeht AltBeacon Android-Deps (Zeile 93-97)
4. **Nested Classes für Test-Struktur**: Bessere Lesbarkeit + VSCode Outline Navigation
5. **Graceful Degradation mit Elvis-Operator**: `currentConfig ?: return` in onBeaconLostFromRegion() (Zeile 240)
6. **Scheduling Loop mit Retry**: `catch (e: Exception) delay(5 min) continue` bei fehlender Config (Zeile 168-174)

### Verifiziert Iteration 2

- ✓ Build: assembleDebug SUCCESS (31MB APK)
- ✓ Tests: 264 Unit Tests grün, keine Failures
- ✓ APK: erfolgreich erstellt
- ✓ Alle 7 ACs vollständig erfüllt
- ✓ Keine Speicherlecks: scheduleJob wird cancelt
- ✓ Coroutines: CoroutineScope Lifecycle correct, @BeaconScannerScope
- ✓ BLE Battery-Efficient: Window-basiertes Scheduling, kein durchgehendes Scanning
- ✓ Exception-Safe: try/catch in alle Callbacks
- ✓ Kotlin-idiomatisch: Elvis, Null-Safety, suspend fun, Flow

## F14 — Dashboard: Wochenansicht

### Review erfolgreich abgeschlossen (Iteration 1 - APPROVED)

**Status:** APPROVED - Alle 7 ACs erfüllt, 19 Tests grün, Code syntaktisch korrekt.

### Findings Iteration 1
- **MINOR selectWeek() Untested**: Public method exists but never tested or called (DatePicker not implemented yet)
- **MINOR Locale Inconsistency**: WeekViewModel uses `Locale.getDefault()` for week number, but WeekScreen uses hardcoded `Locale.GERMAN` for display

### Wichtige Erkenntnisse F14
1. **Flow mit flatMapLatest**: `_selectedWeekStart.flatMapLatest { weekStart → repository.getEntriesInRange() }` für reactive week updates
2. **Stateless UI**: WeekScreen nimmt 5 Flows (weekStart, weekNumber, summaries, stats, hasUnconfirmed), keine eigene State nötig
3. **DaySummary.from()**: Companion function aggregiert TrackingEntryWithPauses je Tag
4. **WeekStats.from()**: Berechnet total, target, percentage (clamped 0..100), overtime (kann negativ sein), average (nur über gearbeitete Tage)
5. **Mo-Fr Generation**: `(0..4).map { dayOffset → weekStart.plusDays(dayOffset.toLong()) }` generiert immer 5 Einträge
6. **Unconfirmed Warning Logic**: Zeigt ⚠️ nur wenn `!confirmed && type != null` (nicht für leere Tage)

### Test Coverage F14
- DaySummaryTest: 6 Tests (single, multiple, pauses, empty, unconfirmed)
- WeekStatsTest: 6 Tests (full week, overtime/undertime, empty, average, EMPTY constant)
- WeekViewModelTest: 7 Tests (summaries, stats, navigation prev/next, weekNumber, unconfirmed)
- **Total: 19 Tests, alle Szenarien abgedeckt**

### AC-Erfüllung F14
- [x] AC #1: Mo-Fr mit Arbeitszeit ✓
- [x] AC #2: Navigation zwischen Wochen ✓
- [x] AC #3: Gesamtstunden korrekt summiert ✓
- [x] AC #4: Soll-/Ist-Vergleich mit Fortschrittsbalken ✓
- [x] AC #5: Tap auf Tag navigiert ✓
- [x] AC #6: Unbestätigte Einträge markiert ✓
- [x] AC #7: Export-Button sichtbar ✓

### Bekannte Limitierungen (dokumentiert)
1. DatePicker nicht implementiert (Spec-Komponente, kein AC)
2. DayView navigiert zu Entries-Screen (F13 nicht parametrisierbar)
3. Hardcoded German strings (Lokalisierung für Phase 2)
4. Locale.GERMAN statt Locale.getDefault() in UI (consistency issue aber dokumentiert)

## Issue #1 — Google Maps API Key Configuration

### Review erfolgreich abgeschlossen (Iteration 1 - APPROVED)

**Status:** APPROVED - Sichere, produktionsreife Implementierung für API-Key-Management.

### Wichtige Patterns implementiert
1. **Gradle manifestPlaceholders**: `manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey` - Android Standard seit ~2010, safe
2. **Multi-Source Fallback Chain**: local.properties → env var → placeholder, jeweils mit Elvis-Operator
3. **Resource-Safe File Reading**: `inputStream().use { properties.load(it) }` - kein Leak
4. **Gitignore + Template Separation**: `local.properties` gitignored, `local.properties.template` committed (safe)
5. **Verification Script mit Farbcodes**: bash script mit exit codes, `set -e` für error-propagation
6. **Comprehensive Documentation**: 40KB guides (Gradle, Manifest, Setup, Troubleshooting, CI/CD)

### Security Best Practices verifiziert
- ✅ Keine hardcodierten Keys in Code (grep "AIza" = 0 matches)
- ✅ local.properties bereits in .gitignore (Zeile 6)
- ✅ Template enthält nur Placeholder
- ✅ Environment-Variable Support für CI/CD
- ✅ Dokumentation empfiehlt API-Key-Restrictions (Package + SHA-1)

### Backward Compatibility
- ✅ Build läuft ohne local.properties (fallback zu placeholder)
- ✅ Keine neuen Dependencies
- ✅ manifestPlaceholders ist Standard-API (seit Android Gradle Plugin 1.0)
- ✅ Manifest-Syntax `${MAPS_API_KEY}` = Standard (Google nutzt das selbst)

### Test + Build Verifikation
- ✅ Verification Script läuft und gibt correct feedback (placeholder detection)
- ✅ Manifest-XML valide (checked with grep)
- ✅ Gradle-Syntax korrekt (properties.getProperty chain)
- ✅ Keine Unit-Tests needed (config-only change)
