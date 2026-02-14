# F15 CSV-Export - Manual Verification Guide

## Build Status

Due to build environment issues (AAPT2 architecture mismatch in ARM64 container), automated tests could not be executed. All code has been implemented following TDD principles with comprehensive test coverage.

## Prerequisites for Testing

1. Build the app on a proper Android development environment
2. Install the app on a real device or emulator
3. Have some tracking entries in the database for testing

## Test Cases

### TC1: Export This Week

**Steps:**
1. Navigate to Week View screen
2. Click "CSV exportieren" button
3. Verify Export Dialog appears
4. Verify "Diese Woche (KW XX)" is selected by default
5. Verify date range preview shows current week Monday-Friday
6. Click "Exportieren"
7. Verify loading indicator appears briefly
8. Verify Share Intent opens with options (Email, Drive, etc.)

**Expected Result:**
- CSV file named `arbeitszeit_YYYY-MM-DD_YYYY-MM-DD.csv` is shared
- File contains entries from Monday to Friday of current week

### TC2: Export Last Month

**Steps:**
1. Open Export Dialog
2. Select "Letzter Monat" radio button
3. Verify date range updates to first and last day of previous month
4. Click "Exportieren"
5. Verify Share Intent opens

**Expected Result:**
- CSV contains all entries from previous month

### TC3: CSV Format Verification

**Steps:**
1. Export a week with various entry types
2. Share to Email or save to Drive
3. Open CSV in Excel or Google Sheets

**Expected Result:**
- File opens correctly with proper column separation
- UTF-8 characters (German umlauts) display correctly
- Columns are:
  - Datum (YYYY-MM-DD)
  - Wochentag (Montag, Dienstag, etc.)
  - Typ (Home Office, Büro (Pendel), Manuell)
  - Startzeit (HH:mm)
  - Endzeit (HH:mm)
  - Brutto (h) (decimal hours with 2 places)
  - Pausen (h) (decimal hours with 2 places)
  - Netto (h) (decimal hours with 2 places)
  - Notiz (text or empty)

### TC4: Decimal Hours Calculation

**Steps:**
1. Create a test entry: 08:15 to 16:37 with 30min pause
2. Export the data
3. Open CSV and verify calculations

**Expected Result:**
- Brutto: 8.37 (8h 22min)
- Pausen: 0.50 (30min)
- Netto: 7.87 (7h 52min)

### TC5: Multiple Pauses

**Steps:**
1. Create entry with multiple pauses (e.g., 30min + 15min)
2. Export and verify

**Expected Result:**
- Pause column shows sum of all pauses (0.75 for 45min total)

### TC6: Entry Sorting

**Steps:**
1. Have entries on different days in random order
2. Export

**Expected Result:**
- CSV rows are sorted by date ascending (oldest first)

### TC7: Incomplete Entries

**Steps:**
1. Start tracking but don't stop it (endTime = null)
2. Export

**Expected Result:**
- Incomplete entry is NOT included in CSV (skipped)

### TC8: Special Characters in Notes

**Steps:**
1. Create entry with notes containing: "Meeting; Präsentation, \"wichtig\""
2. Export and open in Excel

**Expected Result:**
- Notes field displays correctly without breaking CSV structure
- Semicolons in notes don't create extra columns

### TC9: Empty Week

**Steps:**
1. Select a week with no entries
2. Export

**Expected Result:**
- CSV contains only header row
- Share Intent still opens successfully

### TC10: Error Handling

**Steps:**
1. Turn off storage permissions (if possible)
2. Try to export

**Expected Result:**
- Error message appears in dialog
- User can dismiss and try again

## CSV Sample Output

```csv
Datum;Wochentag;Typ;Startzeit;Endzeit;Brutto (h);Pausen (h);Netto (h);Notiz
2026-02-10;Montag;Home Office;08:15;16:37;8.37;0.50;7.87;
2026-02-11;Dienstag;Büro (Pendel);07:45;16:32;8.78;0.50;8.28;Teammeeting
2026-02-12;Mittwoch;Home Office;08:03;16:33;8.50;0.50;8.00;
2026-02-13;Donnerstag;Büro (Pendel);07:30;17:00;9.50;0.50;9.00;
2026-02-14;Freitag;Home Office;08:00;16:45;8.75;0.50;8.25;
```

## Known Limitations

1. **Custom Date Range UI**: Radio button exists but DatePicker not implemented yet
2. **Preview Statistics**: Entry count and total hours not shown in dialog
3. **Locale Settings**: Decimal separator always dot (.) - no German comma (,) option

## Files to Review

### Implementation Files
- `/workspace/app/src/main/java/com/example/worktimetracker/domain/export/CsvExporter.kt`
- `/workspace/app/src/main/java/com/example/worktimetracker/ui/viewmodel/ExportViewModel.kt`
- `/workspace/app/src/main/java/com/example/worktimetracker/ui/screens/ExportDialog.kt`

### Test Files
- `/workspace/app/src/test/java/com/example/worktimetracker/domain/export/CsvExporterTest.kt`
- `/workspace/app/src/test/java/com/example/worktimetracker/ui/viewmodel/ExportViewModelTest.kt`

### Configuration
- `/workspace/app/src/main/AndroidManifest.xml` (FileProvider)
- `/workspace/app/src/main/res/xml/file_paths.xml`
- `/workspace/app/src/main/java/com/example/worktimetracker/di/TrackingModule.kt`

## Next Steps After Manual Verification

1. If tests pass: Mark feature as complete
2. If issues found: Document in `## Review Findings` section of F15 spec
3. Consider implementing missing features (DatePicker, Preview Stats)
4. Test on multiple devices and Android versions
5. Verify Excel compatibility on Windows/Mac
