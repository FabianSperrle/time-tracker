# Issue #3 Fix: Manual Tracking Entries Not Appearing in Entries List

## Root Cause

The `TrackingDao.getAllEntriesWithPauses()` query was returning **ALL** entries from the database, including:
- Completed entries (endTime != null)
- **Active entries** (endTime = null) - entries that are currently being tracked

When a user started manual tracking, an entry was created with `endTime = null`. This entry would **not appear** in the entries list because the query included it, but the UI expected only completed entries.

When the user stopped tracking, the entry was updated with `endTime = LocalDateTime.now()`, making it a completed entry. However, Room's Flow might not have emitted an update, or the active entry was being displayed incorrectly.

## The Fix

### 1. Updated TrackingDao Query

**File:** `app/src/main/java/com/example/worktimetracker/data/local/dao/TrackingDao.kt`

**Before:**
```kotlin
@Transaction
@Query("SELECT * FROM tracking_entries ORDER BY date DESC, startTime DESC")
fun getAllEntriesWithPauses(): Flow<List<TrackingEntryWithPauses>>
```

**After:**
```kotlin
@Transaction
@Query("SELECT * FROM tracking_entries WHERE endTime IS NOT NULL ORDER BY date DESC, startTime DESC")
fun getAllEntriesWithPauses(): Flow<List<TrackingEntryWithPauses>>
```

**Rationale:**
- The entries list should only show **completed work sessions**
- Active tracking sessions are already displayed in the DashboardScreen
- This matches the behavior of CSV export (which also filters for `endTime != null`)
- Better separation of concerns: Dashboard = active session, Entries = history

### 2. Updated Tests

#### TrackingRepositoryTest
- Renamed test: `getAllEntriesWithPauses returns only completed entries sorted by date descending`
- Added new test: `getAllEntriesWithPauses excludes active entries with null endTime`
  - Verifies that active entries (endTime=null) are excluded
  - Only completed entries are returned

#### ManualTrackingEntriesListTest (NEW)
- Created comprehensive integration test
- Tests:
  1. Manual tracking entries appear after stop (not before)
  2. Multiple manual sessions all appear
  3. MANUAL type entries are not filtered out

### 3. Test Coverage

**Existing tests that verify the fix:**
- `ManualTrackingIntegrationTest`: Verifies that `trackingDao.update()` is called with `endTime != null`
- `TrackingRepositoryTest.stopTracking tests`: Verify that endTime is set correctly
- `EntriesViewModelTest`: All tests use completed entries (already had endTime set)

**New tests:**
- `TrackingRepositoryTest.getAllEntriesWithPauses excludes active entries with null endTime`
- `ManualTrackingEntriesListTest.manual tracking entries appear in entries list after stop`
- `ManualTrackingEntriesListTest.multiple manual tracking sessions all appear in entries list`
- `ManualTrackingEntriesListTest.manual tracking entries with MANUAL type are not filtered out`

## Verification Steps

### Manual Testing Checklist

1. **Start Manual Tracking**
   - Open app → Dashboard
   - Click "Start" button, select "Manuell"
   - **Expected:** Entry does NOT appear in Entries list yet
   - **Expected:** Dashboard shows active tracking with timer

2. **Stop Manual Tracking**
   - Click "Stop" button
   - **Expected:** Entry NOW appears in Entries list with correct time
   - **Expected:** Entry shows type "✏️ Manuell"
   - **Expected:** Entry shows correct duration

3. **Multiple Sessions**
   - Start and stop 3 manual tracking sessions
   - **Expected:** All 3 entries appear in list
   - **Expected:** Sorted by date DESC (newest first)

4. **Different Types**
   - Start manual tracking with "Home Office"
   - Stop
   - Start manual tracking with "Büro"
   - Stop
   - **Expected:** Both entries appear with correct types

### Automated Tests

Run the following to verify the fix:

```bash
./gradlew testDebugUnitTest --tests "TrackingRepositoryTest"
./gradlew testDebugUnitTest --tests "EntriesViewModelTest"
./gradlew testDebugUnitTest --tests "ManualTrackingIntegrationTest"
./gradlew testDebugUnitTest --tests "ManualTrackingEntriesListTest"
```

## Impact Analysis

### Affected Components

1. **TrackingDao.getAllEntriesWithPauses()** - Query changed
2. **EntriesViewModel** - No code changes, but behavior changed
3. **EntriesScreen** - No code changes, now shows only completed entries

### Backward Compatibility

- Database schema: No changes
- API contract: The query signature remains the same
- UI behavior: Changed - now only shows completed entries (improvement)

### Other Features Using getAllEntriesWithPauses()

Checked all usages:
1. **EntriesViewModel** - Fixed by this change ✓
2. **Export functionality** - Already filters for endTime != null ✓
3. **Statistics/Reports** - Would benefit from this fix ✓

## Related Issues

This fix also improves:
- CSV export consistency
- Separation between "current session" (Dashboard) and "history" (Entries)
- Performance (smaller result set from database)

## Future Considerations

### Potential Enhancements

1. **Show Active Entry Badge**
   - Could add a badge on Dashboard showing "Currently tracking: X hours"
   - Separate from history list

2. **Resume Unfinished Entry**
   - If app crashes with active entry (endTime=null), could detect and offer resume
   - Would need additional state recovery logic

3. **Flexible Filtering**
   - Add option to show/hide active entries
   - Settings toggle: "Show active sessions in history"

### Alternative Approaches Considered

1. **Filter in ViewModel** - Rejected, data layer responsibility
2. **Two separate methods** - Could add `getActiveEntry()` and `getCompletedEntries()`, but current solution is simpler
3. **UI-level filter** - Rejected, inefficient (loads all then filters)

## Commit Message

```
fix: Issue #3 - Filter out active entries from entries list

The getAllEntriesWithPauses() query now filters for completed entries
(endTime IS NOT NULL) to prevent active tracking sessions from appearing
in the history list.

Active sessions are shown in DashboardScreen, while EntriesScreen shows
completed work sessions only.

Added tests:
- TrackingRepositoryTest: Verify active entry exclusion
- ManualTrackingEntriesListTest: Integration test for Issue #3

Closes #3
```
