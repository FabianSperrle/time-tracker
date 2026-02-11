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

- [ ] Alle drei Entities (TrackingEntry, Pause, GeofenceZone) sind als Room-Entities angelegt
- [ ] DAOs bieten CRUD-Operationen + spezifische Queries (aktiver Eintrag, Datumsbereich)
- [ ] Repository-Layer abstrahiert DAOs mit sauberen Domain-Methoden
- [ ] `netDuration()` berechnet korrekt: Bruttozeit minus Pausenzeit
- [ ] TypeConverters für LocalDate/LocalDateTime funktionieren korrekt
- [ ] Unit-Tests für Repository und Netto-Dauer-Berechnung
