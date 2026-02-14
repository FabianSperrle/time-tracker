#!/bin/bash
# Verification script for Issue #3 fix

set -e

echo "=================================================="
echo "Verifying Issue #3 Fix: Manual Tracking Entries"
echo "=================================================="
echo ""

echo "Step 1: Running affected tests..."
echo ""

# Test 1: TrackingRepositoryTest
echo "→ Running TrackingRepositoryTest..."
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.data.repository.TrackingRepositoryTest" || {
    echo "❌ TrackingRepositoryTest failed"
    exit 1
}
echo "✓ TrackingRepositoryTest passed"
echo ""

# Test 2: EntriesViewModelTest
echo "→ Running EntriesViewModelTest..."
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.ui.viewmodel.EntriesViewModelTest" || {
    echo "❌ EntriesViewModelTest failed"
    exit 1
}
echo "✓ EntriesViewModelTest passed"
echo ""

# Test 3: ManualTrackingIntegrationTest
echo "→ Running ManualTrackingIntegrationTest..."
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.integration.ManualTrackingIntegrationTest" || {
    echo "❌ ManualTrackingIntegrationTest failed"
    exit 1
}
echo "✓ ManualTrackingIntegrationTest passed"
echo ""

# Test 4: ManualTrackingEntriesListTest
echo "→ Running ManualTrackingEntriesListTest (NEW)..."
./gradlew testDebugUnitTest --tests "com.example.worktimetracker.integration.ManualTrackingEntriesListTest" || {
    echo "❌ ManualTrackingEntriesListTest failed"
    exit 1
}
echo "✓ ManualTrackingEntriesListTest passed"
echo ""

echo "Step 2: Building debug APK..."
./gradlew assembleDebug || {
    echo "❌ Build failed"
    exit 1
}
echo "✓ Build successful"
echo ""

echo "Step 3: Locating APK..."
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "✓ APK found: $APK_PATH ($APK_SIZE)"
else
    echo "⚠ APK not found at expected location"
fi
echo ""

echo "=================================================="
echo "✅ All automated tests passed!"
echo "=================================================="
echo ""
echo "Manual Testing Checklist:"
echo ""
echo "1. Install APK on device/emulator:"
echo "   adb install -r $APK_PATH"
echo ""
echo "2. Test Manual Tracking Flow:"
echo "   a. Open app → Dashboard"
echo "   b. Click 'Start' → Select 'Manuell'"
echo "   c. ✓ Verify: Entry does NOT appear in Entries list"
echo "   d. ✓ Verify: Dashboard shows timer running"
echo "   e. Click 'Stop'"
echo "   f. ✓ Verify: Entry NOW appears in Entries list"
echo "   g. ✓ Verify: Entry shows type 'Manuell' with correct time"
echo ""
echo "3. Test Multiple Sessions:"
echo "   a. Start and stop 3 manual sessions"
echo "   b. ✓ Verify: All 3 entries appear in list"
echo "   c. ✓ Verify: Sorted newest first"
echo ""
echo "4. Test Different Types:"
echo "   a. Start with 'Home Office' → Stop"
echo "   b. Start with 'Büro' → Stop"
echo "   c. ✓ Verify: Both entries appear with correct types"
echo ""
echo "=================================================="
