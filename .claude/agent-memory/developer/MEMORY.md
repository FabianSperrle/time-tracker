# Developer Agent Memory - Work Time Tracker

## TDD Patterns

### Room Entities & DAOs
- **Tests before Implementation**: Write unit tests for TypeConverters first (Roundtrip-Tests sind wichtig)
- **Foreign Keys**: Immer Index auf FK-Spalten anlegen (Room gibt Warning, kann Performance-Problem werden)
- **Relations**: `@Embedded` + `@Relation` für 1:n Beziehungen, netDuration()-Logik im Data-Class
- **DAO Testing**: Mockk für DAOs in Repository-Tests, Turbine für Flow-Testing

### Kotlin Type Inference Issues
- **combine() mit >8 Parametern**: Kotlin kann Typen nicht inferieren → explizite Typ-Annotationen nötig
- **Workaround**: Nested `combine()` calls oder explizite Lambda-Parameter-Typen

### JUnit 5 Migration
- `org.junit.Assert.*` → `org.junit.jupiter.api.Assertions.*`
- `@Before` → `@BeforeEach`
- `@Test` Import aus `org.junit.jupiter.api.Test`
- `testOptions { unitTests { all { it.useJUnitPlatform() } } }` im build.gradle.kts erforderlich

### Room Best Practices
- ISO-8601 Strings für LocalDate/LocalDateTime (via `toString()` und `parse()`)
- `OnConflictStrategy.REPLACE` für Insert nur bei echten Upsert-Szenarien
- `Flow<List<T>>` für Queries, `suspend fun` für Mutations
- Database Version erhöhen bei Schema-Änderungen

## DataStore Patterns (F16)
- **Mocking schwierig**: DataStore<Preferences> ist komplex zu mocken in Unit Tests
- **Bessere Strategien**:
  - Basis-Tests für Konstanten und Struktur
  - Instrumented Tests für vollständige Flow-Validierung
  - FakeDataStore für komplexere Unit-Test-Szenarien
- **Flow Combination**: `combine()` maximal 5 Flows → Nested calls mit Helper-Klassen bei mehr Flows
- **Hilt Module**: DataStore via Extension Property auf Context, dann als Singleton Provider

## Gradle Issues
- **KSP Caching**: Bei Build-Fehlern mit KSP → `./gradlew clean` hilft oft
- **Test Execution Error**: "Failed to load JUnit Platform" → JUnit 5 + Vintage Engine + Platform Launcher nötig
- **JUnit 4 + 5**: Vintage Engine erlaubt beide parallel → `testRuntimeOnly(libs.junit.vintage.engine)`

## Robolectric Testing (F04)
- **JUnit 4 required**: Projekt nutzt `@RunWith(RobolectricTestRunner::class)` statt JUnit 5 Extension
- **Android Framework Klassen**: NotificationChannel, NotificationManager müssen mit Robolectric getestet werden
- **Hilt + BroadcastReceiver**: Schwer zu testen in Unit Tests → Tests auf Constants und basic Instantiation beschränken
- **Service Lifecycle**: Vollständige Service-Tests limitiert in Robolectric → `Robolectric.buildService()` für basics
- **Async Testing**: BroadcastReceiver mit eigener CoroutineScope schwer zu verifizieren → Acceptance auf real device

## Foreground Service Patterns (F04)
- **State Observation**: Service observiert `StateFlow` in eigenem `CoroutineScope`
- **Lifecycle Management**: Service stoppt sich selbst bei `TrackingState.Idle`
- **Notification Updates**: Periodic Updates nur bei TRACKING, nicht bei PAUSED (Battery Saving)
- **PendingIntent Flags**: Immer `FLAG_IMMUTABLE` für Android 12+ (targetSdk 31+)
- **Manifest**: `foregroundServiceType="location"` erforderlich für Location-based Services (Android 14+)

## Google Maps Integration (F06)
- **maps-compose**: Verwendet `com.google.maps.android:maps-compose` für Jetpack Compose Integration
- **API Key**: In AndroidManifest.xml via `<meta-data android:name="com.google.android.geo.API_KEY" />`
- **LatLng im ViewModel**: Google Maps LatLng-Klasse kann direkt im ViewModel verwendet werden
- **Live Preview**: Temporäre Marker/Circles während Edit-Mode via StateFlow im ViewModel
- **MapProperties**: `isMyLocationEnabled = true` erfordert FINE_LOCATION Permission (aus F05)
