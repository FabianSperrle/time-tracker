# Google Maps API Setup Guide

## Overview

The Work Time Tracker app uses Google Maps SDK for Android to display geofence zones on an interactive map (Feature F06). To use the map functionality, you need a valid Google Maps API key.

## Issue

If the map doesn't load in the geofence configuration screen, it's likely because the Google Maps API key is not configured or invalid.

## Symptoms

- Map screen shows a blank/grey area instead of the map
- Logcat may show errors like "Authorization failure" or "API key not found"
- Geofence zones cannot be viewed or configured

## Solution

### Step 1: Obtain a Google Maps API Key

1. **Go to Google Cloud Console:**
   - Navigate to [Google Cloud Console](https://console.cloud.google.com/)
   - Sign in with your Google account

2. **Create or Select a Project:**
   - Click on the project dropdown at the top of the page
   - Click "New Project" or select an existing project
   - Give your project a name (e.g., "Work Time Tracker")

3. **Enable Maps SDK for Android:**
   - In the left sidebar, navigate to **APIs & Services** → **Library**
   - Search for "Maps SDK for Android"
   - Click on it and press **Enable**

4. **Create API Credentials:**
   - Go to **APIs & Services** → **Credentials**
   - Click **Create Credentials** → **API Key**
   - Your new API key will be displayed (e.g., `AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX`)
   - **Copy this key immediately**

5. **Restrict the API Key (Recommended for Security):**
   - Click on the newly created API key to edit it
   - Under **Application restrictions**, select "Android apps"
   - Click **Add an item**
   - Enter the package name: `com.example.worktimetracker`
   - Get your SHA-1 fingerprint:
     ```bash
     # Debug keystore (for development)
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
   - Copy the SHA-1 fingerprint and paste it
   - Under **API restrictions**, select "Restrict key"
   - Select only "Maps SDK for Android"
   - Click **Save**

### Step 2: Configure the API Key in Your Project

You have **three options** to configure the API key:

#### Option A: Local Properties File (Recommended for Development)

1. Create or edit the file `local.properties` in the project root directory:
   ```
   /workspace/local.properties
   ```

2. Add the following line with your API key:
   ```properties
   MAPS_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   ```

3. Save the file

**Note:** The `local.properties` file is already gitignored and will NOT be committed to version control, keeping your API key secure.

#### Option B: Environment Variable (Recommended for CI/CD)

Set an environment variable before building:

```bash
export MAPS_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
./gradlew assembleDebug
```

On Windows:
```cmd
set MAPS_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
gradlew.bat assembleDebug
```

#### Option C: Direct Manifest Edit (NOT Recommended - For Testing Only)

Edit `app/src/main/AndroidManifest.xml` and replace the placeholder:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" />
```

⚠️ **WARNING:** Do NOT commit this change to version control, as it exposes your API key!

### Step 3: Build and Run

1. Clean and rebuild the project:
   ```bash
   ./gradlew clean assembleDebug
   ```

2. Install and run the app on your device/emulator:
   ```bash
   ./gradlew installDebug
   ```

3. Navigate to the Map screen (via the bottom navigation bar)

4. The map should now load correctly, showing your current location and any configured geofence zones

## Verification

To verify the API key is correctly configured:

1. **Check Build Logs:**
   The build process should show no errors related to MAPS_API_KEY

2. **Check Logcat:**
   ```bash
   adb logcat | grep -i "maps\|google"
   ```
   You should NOT see errors like "Authorization failure" or "Invalid API key"

3. **Test the Map:**
   - Open the app
   - Navigate to the Map screen
   - The map should display with tiles loaded
   - "My Location" button should appear (if location permission granted)
   - You should be able to tap on the map and add geofence zones

## Troubleshooting

### Map Shows Grey Tiles

**Cause:** Invalid or missing API key

**Solution:**
- Double-check that your API key is correctly copied to `local.properties`
- Verify there are no extra spaces or quotes around the key
- Rebuild the app: `./gradlew clean assembleDebug`

### "Authorization Failure" in Logcat

**Cause:** API key restrictions don't match your app

**Solution:**
- In Google Cloud Console, check your API key restrictions
- Ensure package name is `com.example.worktimetracker`
- Add your debug keystore SHA-1 fingerprint
- For development, you can temporarily remove restrictions

### Map Loads but "My Location" Doesn't Work

**Cause:** Location permissions not granted

**Solution:**
- This is NOT an API key issue
- Grant location permissions in app settings or when prompted
- Ensure GPS is enabled on your device

### API Key Not Found in Build

**Cause:** `local.properties` file is not in the correct location

**Solution:**
- The file must be in the **project root** directory (same level as `build.gradle.kts`)
- NOT in the `app/` subdirectory
- Check the file is named exactly `local.properties` (no `.txt` extension)

## Cost and Billing

- Google Maps SDK for Android requires billing to be enabled on your Google Cloud project
- Google provides $200 of free monthly usage (which is sufficient for development and moderate use)
- Mobile SDKs have unlimited usage within the free tier for most use cases
- Set up billing alerts in Google Cloud Console to monitor usage

## Security Best Practices

1. ✅ **Always use `local.properties`** for local development (already gitignored)
2. ✅ **Use environment variables** for CI/CD pipelines
3. ✅ **Restrict API keys** to your app's package name and SHA-1
4. ✅ **Enable only required APIs** (Maps SDK for Android)
5. ❌ **NEVER commit API keys** to version control
6. ❌ **NEVER hardcode keys** in source files that are committed

## Additional Resources

- [Google Maps Platform Documentation](https://developers.google.com/maps/documentation/android-sdk/overview)
- [Get API Key Guide](https://developers.google.com/maps/documentation/android-sdk/get-api-key)
- [Using API Keys](https://cloud.google.com/docs/authentication/api-keys)
- [Pricing Information](https://mapsplatform.google.com/pricing/)

## Related Files

- `/workspace/app/build.gradle.kts` - Reads API key from local.properties/environment
- `/workspace/app/src/main/AndroidManifest.xml` - Uses manifestPlaceholder `${MAPS_API_KEY}`
- `/workspace/local.properties` - Store your API key here (gitignored)
- `/workspace/app/src/main/java/com/example/worktimetracker/ui/screens/MapScreen.kt` - Map implementation
