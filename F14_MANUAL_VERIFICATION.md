# F14 - Week View Dashboard - Manual Verification Checklist

## Build Environment Issue
Due to ARM64/x86_64 AAPT2 incompatibility, automated tests cannot be run.
This document tracks manual verification steps for when tests can be executed.

## Test Execution Commands

```bash
# Unit Tests
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.domain.model.DaySummaryTest"
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.domain.model.WeekStatsTest"
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.ui.viewmodel.WeekViewModelTest"

# Build APK
./gradlew assembleDebug
```

## Acceptance Criteria Verification

### 1. Wochenansicht zeigt Mo-Fr mit jeweiliger Arbeitszeit
- [ ] WeekScreen displays 5 days (Monday to Friday)
- [ ] Each day shows date and day of week
- [ ] Net work duration is shown for each day
- [ ] Days without entries show "—"

**Code Location:**
- `WeekScreen.kt:214-222` - DayRow loop through summaries
- `WeekViewModel.kt:54-74` - weekSummaries generation (0..4 days)

### 2. Navigation zwischen Wochen funktioniert
- [ ] Previous week arrow (◄) navigates backward
- [ ] Next week arrow (►) navigates forward
- [ ] Week number (KW) updates correctly
- [ ] Date range updates correctly

**Code Location:**
- `WeekViewModel.kt:106-115` - previousWeek() / nextWeek()
- `WeekScreen.kt:130-149` - WeekNavigationHeader with IconButtons

### 3. Gesamtstunden werden korrekt summiert
- [ ] Total duration sums all daily net durations
- [ ] Multiple entries per day are summed correctly
- [ ] Pauses are subtracted from total

**Code Location:**
- `WeekStats.kt:29-32` - summaries.sumOf { it.netDuration.toMinutes() }
- `DaySummary.kt:44` - entries.sumOf { it.netDuration().toMinutes() }

### 4. Soll-/Ist-Vergleich mit Fortschrittsbalken
- [ ] Target hours displayed (from settings)
- [ ] Percentage calculated correctly
- [ ] LinearProgressIndicator shows correct progress (0-100%)
- [ ] Progress bar color follows Material3 theme

**Code Location:**
- `WeekScreen.kt:183-186` - LinearProgressIndicator
- `WeekStats.kt:34-42` - percentage calculation

### 5. Tap auf Tag navigiert zur Tagesansicht
- [ ] Clicking a day row triggers onDayClick callback
- [ ] Navigation passes correct LocalDate
- [ ] Navigation to day view or entries screen works

**Code Location:**
- `WeekScreen.kt:217` - onClick callback in DayRow
- `AppNavHost.kt:39-43` - onDayClick navigation handler

### 6. Unbestätigte Einträge sind visuell markiert
- [ ] ⚠️ icon shown in day row when !confirmed
- [ ] Warning text below export button when hasUnconfirmedEntries
- [ ] Warning text is red colored

**Code Location:**
- `WeekScreen.kt:290-295` - ⚠️ in DayRow
- `WeekScreen.kt:103-111` - Warning text
- `WeekViewModel.kt:93-101` - hasUnconfirmedEntries calculation

### 7. Export-Button ist sichtbar und führt zu F15
- [ ] "CSV exportieren" button visible
- [ ] Button triggers onExportClick callback
- [ ] Placeholder ready for F15 integration

**Code Location:**
- `WeekScreen.kt:95-100` - Export button
- `AppNavHost.kt:41-43` - onExportClick placeholder

## Additional Verification

### Domain Models
- [ ] DaySummary.from() handles empty entries
- [ ] DaySummary.from() uses first entry's type
- [ ] DaySummary.from() marks as unconfirmed if any entry unconfirmed
- [ ] WeekStats.from() calculates overtime (positive/negative)
- [ ] WeekStats.from() calculates average only over worked days

### ViewModel
- [ ] WeekViewModel initializes with current week Monday
- [ ] weekNumber uses correct Locale (German KW)
- [ ] weekSummaries always has 5 entries (Mo-Fr)
- [ ] weekStats combines summaries with target hours
- [ ] Navigation methods update _selectedWeekStart correctly

### UI
- [ ] Week number formatted correctly (e.g., "KW 07")
- [ ] Date range formatted correctly (German locale)
- [ ] Type icons displayed: 🏢 (Office), 🏠 (Home), ✋ (Manual)
- [ ] Duration formatted as "Xh XXmin"
- [ ] Overtime shows + or − sign with color coding
- [ ] Cards have proper elevation and spacing

### Navigation Integration
- [ ] "Woche" tab in bottom navigation
- [ ] WeekScreen is start destination
- [ ] Bottom nav has 4 items (Woche, Heute, Einträge, Settings)
- [ ] Navigation state preserved on back/forward

## Expected Test Results

### DaySummaryTest (6 tests)
1. from_creates_summary_from_single_entry_with_home_office
2. from_creates_summary_from_multiple_entries_uses_first_type
3. from_calculates_net_duration_with_pauses
4. from_returns_empty_summary_for_no_entries
5. from_marks_as_unconfirmed_if_any_entry_is_unconfirmed

### WeekStatsTest (6 tests)
1. from_calculates_correct_statistics_for_full_week
2. from_calculates_overtime_correctly
3. from_calculates_negative_overtime_for_under_target
4. from_handles_empty_week
5. from_calculates_average_only_for_worked_days
6. EMPTY_has_all_zeros

### WeekViewModelTest (7 tests)
1. weekSummaries_emits_daily_summaries_for_current_week
2. weekStats_calculates_correct_statistics
3. previousWeek_navigates_to_previous_week
4. nextWeek_navigates_to_next_week
5. weekSummaries_handles_pauses_correctly
6. weekNumber_returns_correct_calendar_week
7. hasUnconfirmedEntries_is_true_when_any_entry_is_unconfirmed

## Build Verification

Once build environment is fixed:

```bash
# 1. Run all tests
./gradlew testDebugUnitTest

# 2. Build debug APK
./gradlew assembleDebug

# 3. Verify APK location
ls -lh app/build/outputs/apk/debug/app-debug.apk

# 4. Install on device/emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Manual Testing on Device

1. Launch app
2. Should land on "Woche" screen (WeekScreen)
3. Verify current week is displayed
4. Tap previous/next week arrows
5. Tap on a day row → should navigate to entries
6. Check bottom navigation tabs work
7. Verify statistics calculations with known data
8. Test with unconfirmed entries → warning should appear

## Known Issues to Accept

1. DatePicker on KW header not implemented (future enhancement)
2. Day view screen not dedicated (navigates to entries)
3. CSV export button is placeholder (F15)
4. German strings hard-coded (localization later)

## Files Modified

- Domain: DaySummary.kt, WeekStats.kt (new)
- ViewModel: WeekViewModel.kt (new)
- UI: WeekScreen.kt (new)
- Navigation: Screen.kt, AppNavHost.kt, BottomNavigationBar.kt (modified)
- Tests: DaySummaryTest.kt, WeekStatsTest.kt, WeekViewModelTest.kt (new)

## Success Criteria

✅ All 19 unit tests pass
✅ APK builds successfully
✅ All 7 acceptance criteria verified on device
✅ No regression in existing features (F01-F13)
