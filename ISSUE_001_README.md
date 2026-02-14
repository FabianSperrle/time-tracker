# Issue #1: Google Maps Not Loading - Complete Fix

## Problem Statement

**Issue:** Map does not load in the geofence configuration screen (Feature F06)

**Symptom:** When navigating to the Map screen, instead of seeing Google Maps tiles, the screen shows:
- Blank/grey area
- No map tiles loading
- Cannot configure geofence zones

**Root Cause:** Invalid Google Maps API key (`"YOUR_API_KEY_HERE"` placeholder)

## Solution Overview

This fix implements a flexible, secure configuration system for the Google Maps API key that:
- ✅ Reads from `local.properties` (gitignored, secure)
- ✅ Supports environment variables for CI/CD
- ✅ Falls back gracefully to placeholder
- ✅ Provides comprehensive documentation
- ✅ Includes verification script

## Quick Fix (3 Steps)

```bash
# 1. Copy the template
cp local.properties.template local.properties

# 2. Get your API key from Google Cloud Console
# Visit: https://console.cloud.google.com/
# Enable "Maps SDK for Android" and create an API key

# 3. Edit local.properties and paste your key
echo "MAPS_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" > local.properties

# 4. Verify configuration
./verify_maps_setup.sh

# 5. Rebuild and test
./gradlew clean assembleDebug
./gradlew installDebug
```

## Detailed Documentation

### For End Users (Developers)
👉 **[docs/GOOGLE_MAPS_SETUP.md](docs/GOOGLE_MAPS_SETUP.md)** - Complete setup guide
- Step-by-step Google Cloud Console setup
- How to obtain and configure API key
- Security best practices
- Troubleshooting guide

### For Project Setup
👉 **[SETUP.md](SETUP.md)** - General project setup
- Prerequisites and initial setup
- Build commands
- Common issues and solutions

### For Technical Details
👉 **[ISSUE_001_FIX.md](ISSUE_001_FIX.md)** - Technical documentation
- Code changes made
- Architecture decisions
- Security considerations
- Build verification

### For Quick Reference
👉 **[ISSUE_001_SUMMARY.md](ISSUE_001_SUMMARY.md)** - Executive summary
- What was done
- How to use
- Impact analysis
- Next steps

## Files Changed

### Configuration Files (Modified)
1. **app/build.gradle.kts**
   - Added API key resolution from `local.properties` or environment
   - Uses `manifestPlaceholders` to inject into AndroidManifest

2. **app/src/main/AndroidManifest.xml**
   - Changed from hardcoded value to placeholder: `${MAPS_API_KEY}`

3. **docs/features/F06-geofence-map-config.md**
   - Updated "Known Limitations" section
   - Marked API key issue as FIXED

4. **.gitignore**
   - Added comment clarifying Maps API key handling

### Documentation Files (Created)
1. **docs/GOOGLE_MAPS_SETUP.md** - Comprehensive setup guide (200+ lines)
2. **local.properties.template** - Template for developers
3. **SETUP.md** - General project setup guide
4. **ISSUE_001_FIX.md** - Technical fix documentation
5. **ISSUE_001_SUMMARY.md** - Executive summary
6. **ISSUE_001_README.md** - This file
7. **verify_maps_setup.sh** - Verification script

## How It Works

### Build-Time Configuration Flow

```
Build Process
    ↓
app/build.gradle.kts
    ↓
Check local.properties for MAPS_API_KEY
    ↓ (if not found)
Check environment variable MAPS_API_KEY
    ↓ (if not found)
Use placeholder "YOUR_API_KEY_HERE"
    ↓
Set manifestPlaceholders["MAPS_API_KEY"]
    ↓
AndroidManifest.xml uses ${MAPS_API_KEY}
    ↓
Final APK contains the API key
```

### Priority Order

1. **local.properties** (Highest priority)
   - For local development
   - Gitignored, secure
   - Example: `MAPS_API_KEY=AIzaSy...`

2. **Environment Variable**
   - For CI/CD pipelines
   - Example: `export MAPS_API_KEY=AIzaSy...`

3. **Placeholder** (Fallback)
   - For initial setup
   - App builds but map won't load
   - Value: `"YOUR_API_KEY_HERE"`

## Verification

### Automated Verification

Run the verification script:
```bash
./verify_maps_setup.sh
```

**Expected output (correctly configured):**
```
✓ local.properties exists
✓ MAPS_API_KEY found in local.properties
✓ API key format looks valid (starts with 'AIza')
✓ Configuration appears correct!
```

**Expected output (needs configuration):**
```
✗ API key is still the placeholder 'YOUR_API_KEY_HERE'
  Action required: Replace with your actual API key
```

### Manual Verification

1. **Check local.properties exists:**
   ```bash
   test -f local.properties && echo "OK" || echo "Missing"
   ```

2. **Check API key is set:**
   ```bash
   grep MAPS_API_KEY local.properties
   ```

3. **Build the project:**
   ```bash
   ./gradlew clean assembleDebug
   ```

4. **Check manifest (after build):**
   ```bash
   # Extract and check the merged manifest
   cat app/build/intermediates/merged_manifests/debug/AndroidManifest.xml | grep "com.google.android.geo.API_KEY"
   ```

## Testing the Fix

### Prerequisites
- Valid Google Maps API key (see docs/GOOGLE_MAPS_SETUP.md)
- Android device or emulator (API 31+)
- Location permissions granted

### Test Steps

1. **Configure API key** (see Quick Fix above)

2. **Build and install:**
   ```bash
   ./gradlew clean assembleDebug
   ./gradlew installDebug
   ```

3. **Launch app** on device/emulator

4. **Navigate to Map screen** (bottom navigation bar)

5. **Verify map loads:**
   - ✅ Google Maps tiles appear
   - ✅ "My Location" button visible (if permission granted)
   - ✅ Can tap on map
   - ✅ Can add/edit geofence zones

6. **Check logcat** (optional):
   ```bash
   adb logcat | grep -i "maps\|google"
   ```
   - ✅ No "Authorization failure" errors
   - ✅ No "Invalid API key" errors

### Expected Results

**Before Fix:**
- ❌ Blank/grey map
- ❌ "Authorization failure" in logcat
- ❌ Cannot configure zones

**After Fix (with valid API key):**
- ✅ Map tiles load correctly
- ✅ Interactive map with zoom/pan
- ✅ Can place geofence zones
- ✅ Zones render as circles + markers

## Troubleshooting

### Map still doesn't load

1. **Verify API key is correct:**
   ```bash
   ./verify_maps_setup.sh
   ```

2. **Check Google Cloud Console:**
   - Is "Maps SDK for Android" enabled?
   - Is billing enabled? (required even for free tier)
   - Are there any quota limits exceeded?

3. **Check API key restrictions:**
   - Package name: `com.example.worktimetracker`
   - SHA-1 fingerprint: Add debug keystore fingerprint
   - API restrictions: Only "Maps SDK for Android"

4. **Rebuild completely:**
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```

### Common Errors

**"Authorization failure" in logcat**
- **Cause:** API key restrictions don't match app
- **Solution:** In Google Cloud Console, add correct package name and SHA-1

**"API key not found"**
- **Cause:** local.properties not read by Gradle
- **Solution:** Ensure file is in project root, rebuild

**Map shows "For development purposes only"**
- **Cause:** Billing not enabled in Google Cloud
- **Solution:** Enable billing (free tier available)

**Location button doesn't work**
- **Cause:** Location permissions not granted
- **Solution:** NOT an API key issue - grant permissions in Settings

## Security Best Practices

### ✅ DO
- ✅ Store API key in `local.properties` (gitignored)
- ✅ Use environment variables for CI/CD
- ✅ Restrict API key in Google Cloud Console
- ✅ Limit API key to "Maps SDK for Android" only
- ✅ Add package name and SHA-1 restrictions
- ✅ Monitor API usage and set billing alerts

### ❌ DON'T
- ❌ Commit `local.properties` to git
- ❌ Hardcode API key in source files
- ❌ Share API keys publicly
- ❌ Use unrestricted API keys
- ❌ Skip SHA-1 fingerprint restrictions

## CI/CD Configuration

For continuous integration, set the environment variable:

**GitHub Actions:**
```yaml
env:
  MAPS_API_KEY: ${{ secrets.MAPS_API_KEY }}

- name: Build
  run: ./gradlew assembleDebug
```

**GitLab CI:**
```yaml
variables:
  MAPS_API_KEY: $MAPS_API_KEY  # Set in GitLab CI/CD settings

build:
  script:
    - ./gradlew assembleDebug
```

**Jenkins:**
```groovy
environment {
  MAPS_API_KEY = credentials('maps-api-key')
}
stages {
  stage('Build') {
    steps {
      sh './gradlew assembleDebug'
    }
  }
}
```

## Cost and Billing

- Google Maps SDK requires billing enabled
- **$200 free monthly credit** (sufficient for development)
- Mobile SDKs typically stay within free tier
- Set up billing alerts to monitor usage
- See: https://mapsplatform.google.com/pricing/

## Support and Resources

### Documentation
- 📖 [Complete Setup Guide](docs/GOOGLE_MAPS_SETUP.md)
- 📖 [Project Setup](SETUP.md)
- 📖 [Technical Details](ISSUE_001_FIX.md)

### Google Resources
- [Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk/overview)
- [Get API Key](https://developers.google.com/maps/documentation/android-sdk/get-api-key)
- [Google Cloud Console](https://console.cloud.google.com/)

### Scripts
- `./verify_maps_setup.sh` - Verify configuration
- `./gradlew assembleDebug` - Build APK
- `./gradlew installDebug` - Install on device

## Next Steps

1. ✅ **Fix implemented** - Configuration system in place
2. ✅ **Documentation complete** - Comprehensive guides available
3. ⏳ **Get API key** - Obtain from Google Cloud Console
4. ⏳ **Configure** - Add to local.properties
5. ⏳ **Test** - Build and verify map loads
6. ⏳ **Deploy** - Ready for production use

## Questions?

- **Setup issues:** See [docs/GOOGLE_MAPS_SETUP.md](docs/GOOGLE_MAPS_SETUP.md)
- **Build issues:** See [SETUP.md](SETUP.md)
- **Technical details:** See [ISSUE_001_FIX.md](ISSUE_001_FIX.md)

---

**Issue Status:** ✅ FIXED
**Date:** 2026-02-14
**Fixed By:** Claude Sonnet 4.5 (Developer Agent)
**Ready For:** Testing with valid API key
