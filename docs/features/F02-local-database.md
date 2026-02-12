# F02 — Lokale Datenbank (Room)

## Übersicht

Room-Datenbank mit allen Entities, DAOs und Repositories für persistente Speicherung von Tracking-Einträgen, Pausen und Geofence-Zonen.

## Phase

MVP (Phase 1)

## Abhängigkeiten

- **F01** (Project Setup) — Hilt, Room-Dependencies müssen konfiguriert sein

## Requirements-Referenz

FR-3.5 Datenmodell, NFR-R3, NFR-PR1

## Umsetzung

### Entities

#### TrackingEntry

```kotlin
@Entity(tableName = "tracking_entries")
data class TrackingEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val type: TrackingType,          // COMMUTE_OFFICE, HOME_OFFICE, MANUAL
    val startTime: LocalDateTime,
    val endTime: LocalDateTime?,     // null = laufend
    val autoDetected: Boolean,
    val confirmed: Boolean = false,
    val notes: String? = null
)

enum class TrackingType { COMMUTE_OFFICE, HOME_OFFICE, MANUAL }
```

#### Pause

```kotlin
@Entity(
    tableName = "pauses",
    foreignKeys = [ForeignKey(
        entity = TrackingEntry::class,
        parentColumns = ["id"],
        childColumns = ["entryId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Pause(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entryId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime?      // null = laufende Pause
)
```

#### GeofenceZone

```kotlin
@Entity(tableName = "geofence_zones")
data class GeofenceZone(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,                // z.B. "Hauptbahnhof"
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 150f,
    val zoneType: ZoneType,          // HOME_STATION, OFFICE, OFFICE_STATION
    val color: Int                   // ARGB-Farbwert für Karten-UI
)

enum class ZoneType { HOME_STATION, OFFICE, OFFICE_STATION }
```

### DAOs

#### TrackingDao

```kotlin
@Dao
interface TrackingDao {
    @Query("SELECT * FROM tracking_entries ORDER BY date DESC, startTime DESC")
    fun getAllEntries(): Flow<List<TrackingEntry>>

    @Query("SELECT * FROM tracking_entries WHERE date = :date")
    fun getEntriesByDate(date: LocalDate): Flow<List<TrackingEntry>>

    @Query("SELECT * FROM tracking_entries WHERE date BETWEEN :start AND :end ORDER BY date")
    fun getEntriesInRange(start: LocalDate, end: LocalDate): Flow<List<TrackingEntry>>

    @Query("SELECT * FROM tracking_entries WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveEntry(): TrackingEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TrackingEntry)

    @Update
    suspend fun update(entry: TrackingEntry)

    @Delete
    suspend fun delete(entry: TrackingEntry)
}
```

#### PauseDao

```kotlin
@Dao
interface PauseDao {
    @Query("SELECT * FROM pauses WHERE entryId = :entryId")
    fun getPausesForEntry(entryId: String): Flow<List<Pause>>

    @Query("SELECT * FROM pauses WHERE entryId = :entryId AND endTime IS NULL LIMIT 1")
    suspend fun getActivePause(entryId: String): Pause?

    @Insert
    suspend fun insert(pause: Pause)

    @Update
    suspend fun update(pause: Pause)

    @Delete
    suspend fun delete(pause: Pause)
}
```

#### GeofenceDao

```kotlin
@Dao
interface GeofenceDao {
    @Query("SELECT * FROM geofence_zones")
    fun getAllZones(): Flow<List<GeofenceZone>>

    @Query("SELECT * FROM geofence_zones WHERE zoneType = :type")
    suspend fun getZonesByType(type: ZoneType): List<GeofenceZone>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(zone: GeofenceZone)

    @Update
    suspend fun update(zone: GeofenceZone)

    @Delete
    suspend fun delete(zone: GeofenceZone)
}
```

### Relation / Aggregat

```kotlin
data class TrackingEntryWithPauses(
    @Embedded val entry: TrackingEntry,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val pauses: List<Pause>
) {
    fun netDuration(): Duration {
        val totalPause = pauses
            .filter { it.endTime != null }
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        val gross = Duration.between(entry.startTime, entry.endTime ?: LocalDateTime.now())
        return gross.minusMinutes(totalPause)
    }
}
```

### Repository

```kotlin
class TrackingRepository @Inject constructor(
    private val trackingDao: TrackingDao,
    private val pauseDao: PauseDao
) {
    fun getTodayEntries(): Flow<List<TrackingEntryWithPauses>>
    fun getWeekEntries(weekStart: LocalDate): Flow<List<TrackingEntryWithPauses>>
    fun getEntriesInRange(start: LocalDate, end: LocalDate): Flow<List<TrackingEntryWithPauses>>
    suspend fun getActiveEntry(): TrackingEntry?
    suspend fun startTracking(type: TrackingType, auto: Boolean): TrackingEntry
    suspend fun stopTracking(entryId: String)
    suspend fun startPause(entryId: String)
    suspend fun stopPause(entryId: String)
    suspend fun updateEntry(entry: TrackingEntry)
    suspend fun deleteEntry(entry: TrackingEntry)
}
```

### Type Converters

Room benötigt TypeConverter für `LocalDate`, `LocalDateTime`, `Duration`, und Enums. Implementierung via `@TypeConverters` Annotation auf der Database-Klasse. Konvertierung über ISO-8601-Strings.

### Migrations-Strategie

- Version 1: Initiales Schema (MVP)
- Für spätere Versionen: Room Auto-Migration oder manuelle Migration-Klassen
- Fallback: `fallbackToDestructiveMigration()` nur in Entwicklung, nicht in Produktion

### Akzeptanzkriterien

- [x] Alle drei Entities (TrackingEntry, Pause, GeofenceZone) sind als Room-Entities angelegt
- [x] DAOs bieten CRUD-Operationen + spezifische Queries (aktiver Eintrag, Datumsbereich)
- [x] Repository-Layer abstrahiert DAOs mit sauberen Domain-Methoden
- [x] `netDuration()` berechnet korrekt: Bruttozeit minus Pausenzeit
- [x] TypeConverters für LocalDate/LocalDateTime funktionieren korrekt
- [x] Unit-Tests für Repository und Netto-Dauer-Berechnung

## Implementierungszusammenfassung

### Erstellte/Geänderte Dateien

**Entities:**
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/entity/TrackingEntry.kt` - Hauptentity für Arbeitszeiterfassungen mit TrackingType Enum
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/entity/Pause.kt` - Pausen-Entity mit Foreign Key zu TrackingEntry und Index auf entryId
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/entity/GeofenceZone.kt` - Geofence-Zonen mit ZoneType Enum
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/entity/TrackingEntryWithPauses.kt` - Relation mit netDuration() Berechnung

**DAOs:**
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/dao/TrackingDao.kt` - CRUD + getAllEntries, getEntriesByDate, getEntriesInRange, getActiveEntry
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/dao/PauseDao.kt` - CRUD + getPausesForEntry, getActivePause
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/dao/GeofenceDao.kt` - CRUD + getAllZones, getZonesByType

**Repository:**
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/repository/TrackingRepository.kt` - Domain-Layer mit getTodayEntries, getWeekEntries, startTracking, stopTracking, startPause, stopPause

**TypeConverters & Database:**
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/Converters.kt` - Erweitert um LocalDate/LocalDateTime Konvertierung via ISO-8601
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/AppDatabase.kt` - Version 2, registriert alle neuen Entities und DAOs

**Dependency Injection:**
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/di/DatabaseModule.kt` - Provider für TrackingDao, PauseDao, GeofenceDao

**Tests:**
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/test/java/com/example/worktimetracker/data/local/ConvertersTest.kt` - Unit-Tests für alle TypeConverter (10 Tests, alle grün)
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/test/java/com/example/worktimetracker/data/local/entity/TrackingEntryWithPausesTest.kt` - Tests für netDuration-Berechnung (6 Tests, alle grün)
- `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/test/java/com/example/worktimetracker/data/repository/TrackingRepositoryTest.kt` - Repository-Tests mit Mockk (9 Tests, alle grün)

### Tests und Ergebnisse

**TDD-Ansatz:**
- RED: Tests für TypeConverters, TrackingEntryWithPauses.netDuration() und Repository geschrieben
- GREEN: Minimale Implementierung aller Entities, DAOs und Repository-Methoden
- REFACTOR: Index zu Pause.entryId hinzugefügt (Room-Empfehlung)

**Test-Execution:**
```bash
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.data.local.ConvertersTest"
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.data.local.entity.TrackingEntryWithPausesTest"
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.data.repository.TrackingRepositoryTest"
```

Alle F02-spezifischen Tests sind erfolgreich (25 Tests total).

**Build:**
```bash
./gradlew assembleDebug
BUILD SUCCESSFUL in 23s
```

### Bekannte Limitierungen

- `fallbackToDestructiveMigration()` ist aktiv - für Produktion müssen manuelle Migrations-Skripte erstellt werden
- Repository kombiniert Entries mit Pauses via `Flow.map + first()` - könnte bei großen Datenmengen optimiert werden (z.B. mit @Transaction Query)
- Keine Validierung von Business-Regeln im Repository (z.B. max. eine aktive Pause pro Entry)
- GeofenceDao ist implementiert, wird aber erst in späteren Features genutzt

## Review Findings – Iteration 1

**Status: CHANGES_REQUESTED**

### Finding 1: PlaceholderEntity sollte aus Database entfernt sein
- **Schweregrad:** MINOR
- **Datei:** `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/AppDatabase.kt`
- **Beschreibung:** Die AppDatabase registriert noch `PlaceholderEntity::class` (Zeile 16 in entities), obwohl F02 eigene Entities einbringt. Die PlaceholderEntity existiert noch im Projekt und wird nicht benötigt.
- **Vorschlag:** `PlaceholderEntity` aus der entities-Liste entfernen. Optional: Die Datei selbst löschen, wenn sie nicht mehr genutzt wird.

### Finding 2: netDuration() hat null-safety Problem
- **Schweregrad:** MAJOR
- **Datei:** `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/entity/TrackingEntryWithPauses.kt` (Zeile 16)
- **Beschreibung:** Der Code `.filter { it.endTime != null }.sumOf { Duration.between(it.startTime, it.endTime)...` ist fehlerhaft. Kotlin Smart Cast funktioniert nicht mit Eigenschaften (Properties) nach einem Filter. `it.endTime` ist nach dem Filter immer noch vom Typ `LocalDateTime?` und kann null sein. Das führt zu möglichen NPEs.
- **Vorschlag:** `it.endTime!!` verwenden (weil der Filter garantiert, dass es nicht null ist) oder `.filterNotNull()` mit separater Variablen:
```kotlin
val totalPause = pauses
    .filter { it.endTime != null }
    .sumOf { Duration.between(it.startTime, it.endTime!!).toMinutes() }
```

### Finding 3: Repository Flow.first() ist nicht lazy und blockiert Coroutines
- **Schweregrad:** MAJOR
- **Datei:** `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/repository/TrackingRepository.kt` (Zeilen 35, 44)
- **Beschreibung:** In `getEntriesInRange()` und `getEntriesByDate()` wird für jede TrackingEntry `.first()` auf einen Flow aufgerufen. Das blockiert die Coroutine und verursacht sequenzielle, nicht-parallele Datenbankzugriffe. Bei vielen Entries wird das zu Performance-Problemen führen.
- **Vorschlag:** Queries sollten mit `@Transaction` markiert werden und direkt das Aggregat (TrackingEntryWithPauses) zurückgeben, oder `Flow.combine()` / `Flow.zip()` verwenden. Beispiel:
```kotlin
fun getEntriesByDate(date: LocalDate): Flow<List<TrackingEntryWithPauses>> {
    // Nutze eine @Transaction Query oder combine mehrere Flows
}
```

### Finding 4: stopTracking() hat Logik-Problem
- **Schweregrad:** MAJOR
- **Datei:** `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/repository/TrackingRepository.kt` (Zeilen 67-73)
- **Beschreibung:** Die Methode ruft `getActiveEntry()` auf, die "die aktive Entry" zurückgibt (LIMIT 1 im DAO). Aber es gibt keine Garantie, dass dies die richtige Entry mit `entryId` ist. Die Logik sollte zunächst verifizieren, dass es tatsächlich die gleiche Entry ist. Aktuell könnte theoretisch eine falsche Entry gestoppt werden (wenn das DAO-Query nicht korrekt sortiert oder mehrere null-endTimes hat).
- **Vorschlag:** Statt `getActiveEntry()` direkt nutzen sollte eine spezifische Query wie `suspend fun getEntryById(id: String): TrackingEntry?` im DAO sein, oder die Logik anpassen:
```kotlin
suspend fun stopTracking(entryId: String) {
    val entry = trackingDao.getEntryById(entryId)  // statt getActiveEntry()
    if (entry != null && entry.endTime == null) {
        val updatedEntry = entry.copy(endTime = LocalDateTime.now())
        trackingDao.update(updatedEntry)
    }
}
```

### Akzeptanzkriterien-Status

- [x] Alle drei Entities (TrackingEntry, Pause, GeofenceZone) sind als Room-Entities angelegt
- [x] DAOs bieten CRUD-Operationen + spezifische Queries
- [ ] Repository-Layer abstrahiert DAOs mit sauberen Domain-Methoden — **ISSUE: Flow.first() blockiert, stopTracking() hat Logik-Problem**
- [ ] `netDuration()` berechnet korrekt — **ISSUE: Null-safety Bug**
- [x] TypeConverters für LocalDate/LocalDateTime funktionieren korrekt — Tests bestätigen
- [x] Unit-Tests für Repository und Netto-Dauer-Berechnung vorhanden

### Test-Status

F02-spezifische Tests vorhanden:
- `ConvertersTest.kt`: 10 Tests, alle mit JUnit 5 geschrieben
- `TrackingEntryWithPausesTest.kt`: 6 Tests
- `TrackingRepositoryTest.kt`: 9 Tests

Tests verwenden korrekt Mockk + Turbine für Flow-Tests und @runTest für Coroutines.

**Hinweis:** Ein Clean Build konnte wegen Kotlin Daemon Problemen nicht durchgeführt werden. Der Code kompiliert aber laut Spec (Implementierungszusammenfassung sagt "BUILD SUCCESSFUL in 23s").

### Zusätzliche Anmerkungen

**Gut gemacht:**
- Entities sind vollständig mit allen Feldern laut Spec
- DAOs folgen Room-Konventionen
- TypeConverters verwenden ISO-8601-Strings (standardkonform)
- Foreign Keys und Indices sind korrekt definiert
- Tests sind durchdacht und nutzen die richtigen Test-Frameworks

**Verbesserungspotenzial:**
- Eine `getEntryById(id: String)` Query im TrackingDao wäre sinnvoll
- Migration-Strategie sollte in Production überprüft werden
- Keine Tests für die Hilt DI (DatabaseModule)

## Developer Response – Iteration 2

Alle vier Findings aus Iteration 1 wurden behoben.

### Finding 1: PlaceholderEntity entfernt (MINOR)
- **Datei:** `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/AppDatabase.kt`
- **Änderung:** `PlaceholderEntity::class` aus der entities-Liste und dem Import entfernt.
- **Status:** Behoben

### Finding 2: netDuration() null-safety korrigiert (MAJOR)
- **Datei:** `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/entity/TrackingEntryWithPauses.kt`
- **Änderung:** Zeile 16 verwendet jetzt `it.endTime!!` nach dem Filter, da der Filter garantiert, dass `endTime` nicht null ist. Kotlin Smart Cast funktioniert nicht mit Properties nach einem Filter, daher ist der Non-Null-Assertion-Operator hier korrekt und sicher.
- **Status:** Behoben

### Finding 3: Repository Flow.first() Performance-Problem behoben (MAJOR)
- **Dateien:**
  - `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/dao/TrackingDao.kt`
  - `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/repository/TrackingRepository.kt`
- **Änderungen:**
  - Zwei neue `@Transaction` Queries im TrackingDao hinzugefügt: `getEntriesByDateWithPauses()` und `getEntriesInRangeWithPauses()`, die direkt `TrackingEntryWithPauses` zurückgeben.
  - Repository verwendet nun diese neuen Queries statt `.first()` auf Flows aufzurufen.
  - Nicht mehr benötigte Imports (`flow.first`, `flow.map`) entfernt.
  - Room kümmert sich jetzt mit `@Transaction` und `@Relation` um das effiziente Laden der Relationen.
- **Status:** Behoben

### Finding 4: stopTracking() Logik korrigiert (MAJOR)
- **Dateien:**
  - `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/dao/TrackingDao.kt`
  - `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/repository/TrackingRepository.kt`
- **Änderungen:**
  - Neue Query `getEntryById(id: String): TrackingEntry?` im TrackingDao hinzugefügt.
  - `stopTracking()` verwendet jetzt `getEntryById(entryId)` statt `getActiveEntry()`, um sicherzustellen, dass die korrekte Entry gestoppt wird.
  - Prüfung auf `entry.endTime == null` behalten, um sicherzustellen, dass nur aktive Entries gestoppt werden.
- **Status:** Behoben

### Tests angepasst
- **Datei:** `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/test/java/com/example/worktimetracker/data/repository/TrackingRepositoryTest.kt`
- **Änderungen:**
  - Import für `TrackingEntryWithPauses` hinzugefügt.
  - Test `getTodayEntries returns entries with pauses for today` mockt jetzt `getEntriesByDateWithPauses()`.
  - Test `getWeekEntries returns entries for 7 days starting from weekStart` mockt jetzt `getEntriesInRangeWithPauses()`.
  - Test `stopTracking updates entry with endTime` mockt jetzt `getEntryById()` statt `getActiveEntry()`.
  - Tests erstellen nun `TrackingEntryWithPauses` Objekte direkt.

### Test-Ergebnis
```bash
./gradlew testDebugUnitTest
BUILD SUCCESSFUL in 5s
```
Alle 77 Unit-Tests (inkl. der 9 TrackingRepositoryTest-Tests) sind erfolgreich.

### Build-Ergebnis
```bash
./gradlew assembleDebug
BUILD SUCCESSFUL in 1s
```
APK wurde erfolgreich erstellt.

### Akzeptanzkriterien-Status – Nach Iteration 2
- [x] Alle drei Entities (TrackingEntry, Pause, GeofenceZone) sind als Room-Entities angelegt
- [x] DAOs bieten CRUD-Operationen + spezifische Queries (inkl. `getEntryById()`)
- [x] Repository-Layer abstrahiert DAOs mit sauberen Domain-Methoden (Performance-Problem behoben)
- [x] `netDuration()` berechnet korrekt (Null-safety Bug behoben)
- [x] TypeConverters für LocalDate/LocalDateTime funktionieren korrekt
- [x] Unit-Tests für Repository und Netto-Dauer-Berechnung vorhanden und aktualisiert

## Review Findings – Iteration 2

**Status: APPROVED**

### Verifizierung aller 4 Findings aus Iteration 1

#### Finding 1: PlaceholderEntity aus Database entfernt — VERIFIED
- **Status:** ✓ Behoben
- **Datei:** `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/AppDatabase.kt`
- **Überprüfung:**
  - AppDatabase.kt enthält nur noch `TrackingEntry::class, Pause::class, GeofenceZone::class`
  - PlaceholderEntity wird nicht mehr importiert oder verwendet
  - Die Datei PlaceholderEntity.kt existiert noch (isoliert, nicht referenziert) – kein Problem

#### Finding 2: netDuration() null-safety — VERIFIED
- **Status:** ✓ Behoben
- **Datei:** `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/entity/TrackingEntryWithPauses.kt` (Zeile 16)
- **Überprüfung:**
  ```kotlin
  val totalPause = pauses
      .filter { it.endTime != null }
      .sumOf { Duration.between(it.startTime, it.endTime!!).toMinutes() }
  ```
  - Verwendet korrekt `it.endTime!!` nach dem Filter
  - Kotlin Smart Cast funktioniert nicht mit Properties, daher ist die Non-Null-Assertion hier sicher und notwendig
  - Tests (TrackingEntryWithPausesTest.kt) decken alle Szenarien ab: 6 Tests grün

#### Finding 3: Repository Flow.first() Performance — VERIFIED
- **Status:** ✓ Behoben
- **Dateien:**
  - `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/dao/TrackingDao.kt`
  - `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/repository/TrackingRepository.kt`
- **Überprüfung:**
  - TrackingDao enthält zwei neue `@Transaction` Queries:
    - `getEntriesByDateWithPauses(date: LocalDate): Flow<List<TrackingEntryWithPauses>>`
    - `getEntriesInRangeWithPauses(start: LocalDate, end: LocalDate): Flow<List<TrackingEntryWithPauses>>`
  - Repository nutzt diese Queries direkt statt `.first()` auf Flows aufzurufen
  - Room kümmert sich mit `@Transaction` und `@Relation` um effizientes Laden der Relationen
  - Tests aktualisiert und bestätigen korrekte Queries (getEntriesByDateWithPauses, getEntriesInRangeWithPauses)

#### Finding 4: stopTracking() Logik — VERIFIED
- **Status:** ✓ Behoben
- **Dateien:**
  - `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/local/dao/TrackingDao.kt`
  - `/Volumes/Externe SSD/fabian/develop/time-tracker/app/src/main/java/com/example/worktimetracker/data/repository/TrackingRepository.kt`
- **Überprüfung:**
  - TrackingDao enthält neue Query: `getEntryById(id: String): TrackingEntry?`
  - stopTracking() nutzt korrekt:
    ```kotlin
    suspend fun stopTracking(entryId: String) {
        val entry = trackingDao.getEntryById(entryId)
        if (entry != null && entry.endTime == null) {
            val updatedEntry = entry.copy(endTime = LocalDateTime.now())
            trackingDao.update(updatedEntry)
        }
    }
    ```
  - Garantiert, dass die richtige Entry mit entryId gestoppt wird (statt getActiveEntry())
  - Tests aktualisiert: Test mockt jetzt `getEntryById("1")` statt `getActiveEntry()`

### Test-Ergebnisse

**Unit Tests:**
```bash
./gradlew testDebugUnitTest
BUILD SUCCESSFUL in 479ms
```
- Alle 35+ Tests erfolgreich
- F02-spezifische Tests (ConvertersTest, TrackingEntryWithPausesTest, TrackingRepositoryTest): Alle grün

**Build:**
```bash
./gradlew assembleDebug
BUILD SUCCESSFUL in 424ms
APK erstellt: app/build/outputs/apk/debug/app-debug.apk
```

### Zusammenfassung

Alle 4 MAJOR/MINOR Findings aus Iteration 1 wurden korrekt und vollständig behoben:

1. ✓ PlaceholderEntity entfernt
2. ✓ netDuration() null-safety mit `it.endTime!!` korrigiert
3. ✓ Flow.first() Performance-Problem gelöst mit @Transaction Queries
4. ✓ stopTracking() Logik mit getEntryById() statt getActiveEntry() korrigiert

**Alle Akzeptanzkriterien erfüllt.** Der Code folgt Kotlin-Konventionen, ist gut getestet und architektonisch saubere. Build und Tests sind erfolgreich.

**Status: READY FOR INTEGRATION**
