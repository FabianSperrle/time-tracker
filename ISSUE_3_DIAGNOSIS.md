# Issue #3 Diagnosis: Manual Tracking Entries Not Appearing

## Problem Statement
When user manually starts and stops tracking, the tracking entries do not appear in the entries list.

## Investigation Steps

### 1. Code Flow Analysis

**Start Manual Tracking:**
1. User clicks "Start" button in DashboardScreen
2. DashboardViewModel.startManualTracking(type) called
3. stateMachine.processEvent(TrackingEvent.ManualStart(type))
4. TrackingStateMachine.handleManualStart() → repository.startTracking(type, autoDetected=false)
5. TrackingEntry created with endTime=null, inserted into database

**Stop Manual Tracking:**
1. User clicks "Stop" button
2. DashboardViewModel.stopTracking() called
3. stateMachine.processEvent(TrackingEvent.ManualStop)
4. TrackingStateMachine.handleManualStop(entryId) → repository.stopTracking(entryId)
5. TrackingEntry updated with endTime=LocalDateTime.now()

**Entries List Display:**
1. EntriesViewModel loads entries via repository.getAllEntriesWithPauses()
2. Repository calls trackingDao.getAllEntriesWithPauses()
3. DAO query: `SELECT * FROM tracking_entries ORDER BY date DESC, startTime DESC`
4. Room observes database and emits Flow updates
5. EntriesScreen displays entries

### 2. Potential Issues

#### A) Query Returns ALL Entries (Including Active)
The current query returns **all entries**, including those with endTime=null (active entries).
This should not be a problem since:
- TrackingEntryWithPauses.netDuration() handles null endTime (uses LocalDateTime.now())
- EntryCard displays "..." for null endTime

However, the spec (F12) shows examples with only completed entries. Should we filter?

#### B) Flow Not Emitting Updates
Room should automatically emit new values when tracking_entries table changes.
Possible issues:
- Database transaction not committed?
- Flow collector not active?
- StateFlow caching issue?

#### C) Entry Not Being Created/Updated
Tests verify that entries are created and updated correctly.
But in production:
- Is the database actually persisting data?
- Is fallbackToDestructiveMigration() causing issues?

### 3. Root Cause Hypothesis

**HYPOTHESIS: The query should filter out active entries (endTime IS NOT NULL)**

Looking at F12 spec examples and CSV export code:
```kotlin
// CsvExporter.kt line 57
.filter { it.entry.endTime != null }
```

The export code explicitly filters for completed entries. The entries list should probably do the same.

## Proposed Fix

### Option 1: Filter in DAO Query (Recommended)
Modify the query to only return completed entries:

```kotlin
@Transaction
@Query("SELECT * FROM tracking_entries WHERE endTime IS NOT NULL ORDER BY date DESC, startTime DESC")
fun getAllEntriesWithPauses(): Flow<List<TrackingEntryWithPauses>>
```

**Pros:**
- Clearer intent: entries list shows completed work sessions
- Consistent with export behavior
- Prevents displaying active sessions in history

**Cons:**
- Active sessions won't appear in list (but that's probably desired)

### Option 2: Filter in Repository
Add a new repository method:

```kotlin
fun getCompletedEntriesWithPauses(): Flow<List<TrackingEntryWithPauses>> {
    return trackingDao.getAllEntriesWithPauses()
        .map { entries -> entries.filter { it.entry.endTime != null } }
}
```

**Pros:**
- Keeps DAO query flexible
- Can have both completed and all entries methods

**Cons:**
- Filtering happens in-memory instead of database

### Option 3: Filter in ViewModel
Filter in EntriesViewModel:

```kotlin
val entries: StateFlow<List<TrackingEntryWithPauses>> =
    repository.getAllEntriesWithPauses()
        .map { it.filter { entry -> entry.entry.endTime != null } }
        .stateIn(...)
```

**Pros:**
- No database/repository changes
- Easy to toggle filter

**Cons:**
- UI layer doing data filtering

## Recommendation

**Implement Option 1** - Filter in DAO query with `WHERE endTime IS NOT NULL`.

This is the cleanest solution because:
1. It matches the semantic intent: "Entries" = completed work sessions
2. Active sessions are already shown in DashboardScreen
3. Consistent with export functionality
4. Better performance (database-level filtering)

## Implementation Plan

1. Update TrackingDao.getAllEntriesWithPauses() query
2. Update tests to verify filtering
3. Add integration test reproducing Issue #3
4. Verify fix manually on device
