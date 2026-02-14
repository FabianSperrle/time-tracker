# Work Time Tracker - Setup Guide

## Prerequisites

- Android Studio (latest stable version recommended)
- JDK 17 or higher
- Android SDK with API Level 31-35
- Physical Android device or emulator (API 31+)

## Initial Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd work-time-tracker
```

### 2. Configure Google Maps API Key

**IMPORTANT:** The app requires a Google Maps API key to display the geofence configuration map (Feature F06).

#### Quick Setup:

1. Copy the template file:
   ```bash
   cp local.properties.template local.properties
   ```

2. Obtain a Google Maps API key from [Google Cloud Console](https://console.cloud.google.com/)

3. Edit `local.properties` and replace `YOUR_API_KEY_HERE` with your actual API key:
   ```properties
   MAPS_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   ```

#### Detailed Instructions:

See **[docs/GOOGLE_MAPS_SETUP.md](docs/GOOGLE_MAPS_SETUP.md)** for comprehensive setup instructions including:
- Step-by-step API key creation
- Security best practices
- Troubleshooting common issues

### 3. Build the Project

```bash
./gradlew clean assembleDebug
```

### 4. Run Tests

```bash
./gradlew testDebugUnitTest
```

### 5. Install on Device

```bash
./gradlew installDebug
```

Or open the project in Android Studio and click **Run**.

## Common Issues

### Issue #1: Map Doesn't Load

**Symptom:** The geofence configuration screen shows a blank/grey map.

**Solution:** You need to configure a valid Google Maps API key. See [docs/GOOGLE_MAPS_SETUP.md](docs/GOOGLE_MAPS_SETUP.md) for detailed instructions.

### Issue #2: Build Fails with "MAPS_API_KEY not found"

**Solution:**
1. Ensure `local.properties` exists in the project root
2. Verify the file contains: `MAPS_API_KEY=your_actual_key`
3. Rebuild: `./gradlew clean assembleDebug`

### Issue #3: Location Permissions Not Working

**Solution:**
- Grant location permissions when prompted
- For background location (geofencing), go to Settings → Apps → Work Time Tracker → Permissions → Location → "Allow all the time"

## Project Structure

```
work-time-tracker/
├── app/                           # Main application module
│   ├── src/main/
│   │   ├── java/.../              # Kotlin source files
│   │   │   ├── data/              # Data layer (Room, repositories)
│   │   │   ├── domain/            # Domain layer (use cases, if any)
│   │   │   ├── service/           # Background services
│   │   │   ├── ui/                # UI layer (Composables, ViewModels)
│   │   │   └── di/                # Dependency injection (Hilt modules)
│   │   ├── res/                   # Resources (layouts, strings, etc.)
│   │   └── AndroidManifest.xml
│   └── src/test/                  # Unit tests
├── docs/                          # Documentation
│   ├── features/                  # Feature specifications
│   └── GOOGLE_MAPS_SETUP.md       # Google Maps API setup guide
├── gradle/                        # Gradle configuration
├── local.properties.template      # Template for local.properties
├── CLAUDE.md                      # AI agent instructions
└── SETUP.md                       # This file
```

## Development

### Architecture

- **Pattern:** MVVM + Repository
- **DI:** Hilt (Dagger)
- **Database:** Room
- **UI:** Jetpack Compose + Material 3
- **Background Work:** WorkManager + Foreground Service
- **Location:** Google Play Services (Geofencing, Location)
- **BLE:** AltBeacon Library

### Key Technologies

- Kotlin 1.9.25
- Compose BOM 2024.12.01
- Room 2.6.1
- Hilt 2.52
- Maps Compose 4.4.1

### Testing

- **Unit Tests:** JUnit 5 + MockK + Turbine
- **UI Tests:** Compose UI Testing
- Run all tests: `./gradlew test`

### Gradle Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run lint checks
./gradlew lintDebug

# Clean build
./gradlew clean

# Install on connected device
./gradlew installDebug
```

## Features Status

See individual feature specifications in `docs/features/` for detailed status and acceptance criteria.

- [x] F01: Project Setup & Navigation
- [x] F02: Local Database (Room)
- [x] F03: State Machine
- [x] F04: Foreground Service
- [x] F05: Permissions Management
- [x] F06: Geofence Map Configuration
- [x] F07: Geofence Monitoring
- [x] F08: Commute Tracking Logic
- [x] F09: BLE Beacon Scanning
- [x] F10: Home Office Tracking
- [ ] F11: Settings & Configuration
- [ ] F12: Entry Editing
- [ ] ... (see CLAUDE.md for full list)

## License

[Add license information here]

## Support

For issues related to Google Maps setup, see [docs/GOOGLE_MAPS_SETUP.md](docs/GOOGLE_MAPS_SETUP.md).

For other issues, please check the feature documentation in `docs/features/`.
