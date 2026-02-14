# Geocoding Implementation Guide

## Overview
The Work Time Tracker app now includes address search functionality in the Map screen, allowing users to search for addresses and automatically navigate to them when configuring geofence zones.

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                         MapScreen                           │
│  - Search TextField                                         │
│  - Search Results Dropdown                                  │
│  - Google Map with Zones                                    │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                      MapViewModel                           │
│  - uiState: MapUiState                                      │
│  - updateSearchQuery()                                      │
│  - performSearch()                                          │
│  - selectSearchResult()                                     │
│  - clearSearch()                                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   GeocodingService                          │
│  Interface: searchAddress(query) -> Result<List<Result>>   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                GeocodingServiceImpl                         │
│  - Uses Android Geocoder                                   │
│  - Handles Android 13+ async API                           │
│  - Formats addresses nicely                                │
│  - Returns max 5 results                                   │
└─────────────────────────────────────────────────────────────┘
```

## Usage

### For End Users

1. **Search for Address:**
   - Type address in search field at top of Map screen
   - Press Enter or tap Search icon
   - Wait for results to appear in dropdown

2. **Select Result:**
   - Tap on desired result
   - Camera animates to location
   - Can now tap map to place zone at/near location

3. **Clear Search:**
   - Tap X button in search field
   - Or select a result (auto-clears)

### For Developers

#### Using the Service

```kotlin
@HiltViewModel
class YourViewModel @Inject constructor(
    private val geocodingService: GeocodingService
) : ViewModel() {

    fun search(query: String) {
        viewModelScope.launch {
            val result = geocodingService.searchAddress(query)

            result.onSuccess { searchResults ->
                // Handle results
                searchResults.forEach { result ->
                    println("${result.name} - ${result.address}")
                    println("LatLng: ${result.latLng}")
                }
            }

            result.onFailure { error ->
                // Handle error
                println("Search failed: ${error.message}")
            }
        }
    }
}
```

#### Testing with Mock

```kotlin
@Test
fun `test search functionality`() = runTest {
    // Given
    val mockService = mockk<GeocodingService>()
    val expectedResults = listOf(
        SearchResult(
            name = "Test Location",
            address = "Test St, Test City",
            latLng = LatLng(48.0, 11.0)
        )
    )
    coEvery { mockService.searchAddress("test") } returns Result.success(expectedResults)

    val viewModel = YourViewModel(mockService)

    // When
    viewModel.search("test")
    advanceUntilIdle()

    // Then
    assertEquals(expectedResults, viewModel.uiState.value.searchResults)
}
```

## Data Models

### SearchResult
```kotlin
data class SearchResult(
    val name: String,         // Feature name (e.g., "München Hauptbahnhof")
    val address: String,      // Full formatted address
    val latLng: LatLng       // Coordinates
)
```

### MapUiState (Search-Related Fields)
```kotlin
data class MapUiState(
    // ... other fields ...
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val cameraTarget: LatLng? = null
)
```

## API Details

### GeocodingService Interface

```kotlin
interface GeocodingService {
    /**
     * Search for addresses matching the query.
     *
     * @param query Search string (e.g., "München Hauptbahnhof")
     * @return Result with list of SearchResult (max 5), or error
     */
    suspend fun searchAddress(query: String): Result<List<SearchResult>>
}
```

### Implementation Details

**Android Version Handling:**
- **Android 13+ (API 33+):** Uses async `getFromLocationName()` with callback
- **Android 12 (API 31-32):** Uses deprecated sync API (suppressed warning)

**Result Filtering:**
- Removes results with lat=0, lng=0 (invalid)
- Returns maximum 5 results
- Empty query returns empty list (no error)

**Error Handling:**
- Checks `Geocoder.isPresent()` before search
- Catches exceptions and returns as `Result.failure()`
- UI displays error message from exception

## Dependency Injection

### Module Setup
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class GeocodingModule {
    @Binds
    @Singleton
    abstract fun bindGeocodingService(
        impl: GeocodingServiceImpl
    ): GeocodingService
}
```

Service is automatically available via Hilt injection in:
- ViewModels (via `@HiltViewModel`)
- Other services (via constructor injection)

## UI Components

### SearchBar
Enhanced TopAppBar with:
- TextField for query input
- Search icon (leading)
- Clear button (trailing, when query not empty)
- Keyboard action = Search (triggers search on Enter)

### Search Results Dropdown
Card-based dropdown showing:
- **Loading State:** CircularProgressIndicator
- **Error State:** Red error text
- **Results State:** Scrollable list (max 200dp)
  - Location icon
  - Result name (bold)
  - Full address (small text)
  - Clickable items

### Camera Animation
```kotlin
LaunchedEffect(uiState.cameraTarget) {
    uiState.cameraTarget?.let { target ->
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(target, 15f),
            durationMs = 1000
        )
        viewModel.clearCameraTarget()
    }
}
```

## Testing

### Unit Tests Location
- **MapViewModelTest.kt:** 21 tests (7 for geocoding)
- **GeocodingServiceTest.kt:** Basic data class tests

### Test Coverage
✅ Search with results
✅ Search with loading state
✅ Search with error
✅ Search with blank query
✅ Select result (camera animation)
✅ Clear search
✅ Update search query

### Manual Testing Checklist
- [ ] Search for known address (e.g., "München Hauptbahnhof")
- [ ] Verify results appear
- [ ] Tap result, verify camera animates
- [ ] Search for invalid address, verify error handling
- [ ] Clear search, verify state resets
- [ ] Press Enter on keyboard, verify search triggers
- [ ] Test on Android 12 and Android 13+ devices

## Performance

**Optimization:**
- Search only on explicit action (button/Enter), not on every keystroke
- Async operation (doesn't block UI)
- Results cached by Android Geocoder internally
- Max 5 results limit prevents UI overflow

**Network Usage:**
- Device-dependent (some devices geocode offline, others require network)
- No additional API calls beyond device's geocoding backend

## Troubleshooting

### "Geocoding service not available"
**Cause:** Device doesn't support Geocoder
**Solution:** Error shown in UI, fallback to manual map tap

### No results for valid address
**Cause:** Device geocoding backend doesn't recognize address
**Solution:** Try more specific/less specific query, or use map tap

### Search slow/timing out
**Cause:** Network issues or slow device geocoding backend
**Solution:** Loading indicator shown, results appear when ready

### Results not accurate
**Cause:** Geocoding quality varies by region/device
**Solution:** User can still manually tap correct location on map

## Future Enhancements

### Potential Upgrades
1. **Google Places Autocomplete:** Better UX with suggestions while typing
2. **Reverse Geocoding:** Show address when tapping map location
3. **Recent Searches:** Cache and show recent searches
4. **Favorites:** Save frequently used locations
5. **Offline DB:** Pre-load common addresses for offline use

### Migration Path to Places API
Thanks to interface abstraction, switching is easy:

```kotlin
// 1. Add dependency
implementation("com.google.android.libraries.places:places:3.x.x")

// 2. Create new implementation
class PlacesGeocodingService @Inject constructor(
    @ApplicationContext private val context: Context
) : GeocodingService {
    override suspend fun searchAddress(query: String): Result<List<SearchResult>> {
        // Use Places API
    }
}

// 3. Update module binding
@Binds
@Singleton
abstract fun bindGeocodingService(
    impl: PlacesGeocodingService  // Changed from GeocodingServiceImpl
): GeocodingService
```

No changes needed in MapViewModel or UI!

## Security & Privacy

✅ **No data leaves device** (uses device's local geocoding)
✅ **No API keys required** (Android framework service)
✅ **No tracking** (no analytics on searches)
✅ **Input sanitized** (Geocoder handles special characters)

## References

- Android Geocoder: https://developer.android.com/reference/android/location/Geocoder
- Places API (future): https://developers.google.com/maps/documentation/places/android-sdk
- Issue #2 Fix Summary: `/workspace/ISSUE_2_FIX_SUMMARY.md`
- Feature Spec: `/workspace/docs/features/F06-geofence-map-config.md`
