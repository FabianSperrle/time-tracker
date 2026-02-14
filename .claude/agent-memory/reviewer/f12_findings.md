# F12 — Einträge bearbeiten & korrigieren — Review Findings

## Critical Issues Found (Iteration 1)

### 1. DatePicker Race Condition (CRITICAL)
- **Location:** EntryEditorSheet.kt:321-326
- **Issue:** `datePickerState.selectedDateMillis?.let { viewModel.updateDate() }` executes on every recompose, not just on confirm
- **Impact:** Multiple state updates, UI inconsistency
- **Fix:** Store selection in local state, update ViewModel only in confirmButton callback

### 2. Incomplete Pause Deletion on Update (MAJOR)
- **Location:** EntryEditorViewModel.kt:271-292
- **Issue:** When updating existing entry, deleted pauses are removed from UI list but not from database
- **Symptom:** `existingPauseIds` is calculated but never used
- **Fix:** Delete pauses not in `currentPauseIds` using `repository.deletePause()`

### 3. Race Condition in saveEntry() (MAJOR)
- **Location:** EntryEditorViewModel.kt:232-297
- **Issue:** Function returns Boolean immediately but Repository operations are async via viewModelScope.launch
- **Impact:** UI could close dialog before save completes
- **Fix:** Make saveEntry() suspend or add Save Status StateFlow (Loading/Success/Error)

### 4. AssistedInject Configuration (MINOR)
- **Location:** EntryEditorSheet.kt:65-69
- **Issue:** Requires Hilt 2.52+ but no documentation or fallback
- **Fix:** Add version requirement documentation

### 5. Validation Naming Confusion (MINOR)
- **Location:** EntryEditorViewModel.kt:99-144
- **Issue:** `validationErrors` includes both errors (blocking) and warnings (informative like >12h)
- **Confusion:** Shows 12h warning in error card, looks like blocker but isn't
- **Fix:** Split into `validationErrors` and `validationWarnings` OR rename to `validationMessages`

### 6. Missing Pause Internal Validation (MINOR)
- **Location:** EntryEditorViewModel.kt:119-125
- **Issue:** Validates pause.start/end within entry range, but doesn't validate pause.start < pause.end
- **Scenario:** Pause with 13:00-12:00 could pass validation in extreme edge case
- **Fix:** Add: `pause.startTime < pause.endTime` check in validation loop

## Test Coverage Notes

- 49 total tests across 3 test classes ✓
- Good coverage of basic CRUD and validation ✓
- **Missing tests:**
  1. Pause deletion on entry update
  2. saveEntry() race condition scenario
  3. DatePicker recompose behavior
  4. Pause.start < Pause.end validation

## Architecture Assessment

**Positive:**
- MVVM pattern correctly applied
- Repository abstraction layer working
- AssistedInject for optional parameters is advanced pattern
- Hilt DI properly configured
- Flow-based reactive updates with StateFlow

**Concerns:**
- Async operations not properly handled with loading state
- State management for DatePicker not idiomatic (side effect in let block)
- Pause update logic has gaps

## Integration Verified

- Navigation properly integrated (Screen.Entries, BottomNavigationBar)
- Repository methods exist (getAllEntriesWithPauses, getEntryWithPausesById, etc.)
- PauseDao and TrackingDao ready
- Data entities properly structured with Room annotations

## Build Status

Cannot verify in current CI environment (AAPT2 Daemon failures). Local testing required.
