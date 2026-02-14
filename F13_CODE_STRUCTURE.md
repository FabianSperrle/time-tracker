# F13 - Code Structure Reference

## File Organization

```
app/src/main/java/com/example/worktimetracker/
├── domain/
│   └── model/
│       └── DayStats.kt                    # NEW - Daily statistics model
├── ui/
│   ├── viewmodel/
│   │   └── DashboardViewModel.kt          # MODIFIED - Added statistics
│   └── screens/
│       └── DashboardScreen.kt             # MODIFIED - Enhanced UI
└── data/
    ├── repository/
    │   └── TrackingRepository.kt          # UNCHANGED - Used by ViewModel
    └── settings/
        └── SettingsProvider.kt            # UNCHANGED - Used by ViewModel

app/src/test/java/com/example/worktimetracker/
├── domain/
│   └── model/
│       └── DayStatsTest.kt                # NEW - 8 unit tests
└── ui/
    └── viewmodel/
        └── DashboardViewModelTest.kt      # MODIFIED - 5 new tests
```

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                       DashboardScreen                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Header: "Dienstag, 14. Februar 2026"                 │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Status Card (IdleCard / TrackingCard / PausedCard)   │   │
│  │  - Live Timer (if tracking)                          │   │
│  │  - Start/Pause/Stop buttons                          │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Daily Stats Card                                     │   │
│  │  - Brutto: 4h 23min                                  │   │
│  │  - Pausen: 0min                                      │   │
│  │  - Netto: 4h 23min (highlighted)                     │   │
│  │  - Soll: 8h 00min                                    │   │
│  │  - Verbleibend: 3h 37min                             │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ collectAsState()
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    DashboardViewModel                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ uiState: StateFlow<DashboardUiState>                 │   │
│  │  ← stateMachine.state                                │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ todayStats: StateFlow<DayStats>                      │   │
│  │  ← combine(                                          │   │
│  │      repository.getTodayEntries(),                   │   │
│  │      settingsProvider.weeklyTargetHours              │   │
│  │    ) { entries, target ->                            │   │
│  │      DayStats.from(entries, target/5f)               │   │
│  │    }                                                  │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Functions:                                           │   │
│  │  - startManualTracking(type)                         │   │
│  │  - stopTracking()                                    │   │
│  │  - pauseTracking()                                   │   │
│  │  - resumeTracking()                                  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                     │              │
                     │              │
        ┌────────────┘              └────────────┐
        │                                        │
        ▼                                        ▼
┌──────────────────┐                  ┌───────────────────┐
│ TrackingRepo     │                  │ SettingsProvider  │
│                  │                  │                   │
│ getTodayEntries()│                  │ weeklyTargetHours │
│   ↓              │                  │   ↓               │
│ TrackingDao      │                  │ DataStore         │
└──────────────────┘                  └───────────────────┘
```

## DayStats Calculation Logic

```kotlin
// Input: List<TrackingEntryWithPauses>, dailyTargetHours: Float

// Step 1: Calculate gross work time
grossMinutes = entries.sumOf { entry ->
    val endTime = entry.endTime ?: LocalDateTime.now()  // Active entry
    Duration.between(entry.startTime, endTime).toMinutes()
}

// Step 2: Calculate pause time (only completed pauses)
pauseMinutes = entries.sumOf { entry ->
    entry.pauses
        .filter { it.endTime != null }  // Only completed
        .sumOf { pause ->
            Duration.between(pause.startTime, pause.endTime!!).toMinutes()
        }
}

// Step 3: Calculate derived values
gross = Duration.ofMinutes(grossMinutes)
pause = Duration.ofMinutes(pauseMinutes)
net = gross - pause
target = Duration.ofMinutes((dailyTargetHours * 60).toLong())
remaining = max(0, target - net)  // Never negative

return DayStats(gross, pause, net, target, remaining)
```

## Example Scenarios

### Scenario 1: Single Active Entry (No Pause)
```
Entry 1: 08:00 - (now: 12:23) [MANUAL]
Pauses: none

Calculation:
- gross = 4h 23min
- pause = 0min
- net = 4h 23min
- target = 8h (40h/week ÷ 5 days)
- remaining = 3h 37min
```

### Scenario 2: Completed Entry with Pause
```
Entry 1: 08:00 - 17:00 [MANUAL]
Pause 1: 12:00 - 13:00

Calculation:
- gross = 9h
- pause = 1h
- net = 8h
- target = 8h
- remaining = 0h
```

### Scenario 3: Multiple Entries
```
Entry 1: 08:00 - 12:00 [MANUAL]
Entry 2: 13:00 - (now: 16:30) [HOME_OFFICE]

Calculation:
- gross = 4h + 3h 30min = 7h 30min
- pause = 0min
- net = 7h 30min
- target = 8h
- remaining = 30min
```

### Scenario 4: Active Entry with Active Pause
```
Entry 1: 08:00 - (now: 12:30) [MANUAL]
Pause 1: 12:00 - 12:15 (completed)
Pause 2: 12:20 - (now: 12:30) (active)

Calculation:
- gross = 4h 30min
- pause = 15min  (only completed pause)
- net = 4h 15min
- target = 8h
- remaining = 3h 45min

Note: Active pause is ignored until completed
```

## UI State Transitions

```
┌──────────────┐
│  Idle State  │ ──startManualTracking()──> TrackingState
└──────────────┘                                  │
       ▲                                          │
       │                                          │
       │ stopTracking()                           │ pauseTracking()
       │                                          ▼
       │                                   ┌──────────────┐
       └───────────────────────────────────│ Paused State │
                                           └──────────────┘
                                                  │
                                                  │ resumeTracking()
                                                  ▼
                                           Back to Tracking
```

## Testing Coverage

### DayStatsTest (8 tests)
1. `from should calculate correct stats for single completed entry`
2. `from should calculate correct stats with pauses`
3. `from should calculate correct stats for multiple entries`
4. `from should handle active tracking entry correctly`
5. `from should return zero stats for empty entries`
6. `from should handle active pause correctly`
7. `from should handle overtime correctly`

### DashboardViewModelTest (5 new tests)
1. `todayStats should calculate correct stats for single entry`
2. `todayStats should calculate correct stats with pauses`
3. `todayStats should handle multiple entries correctly`
4. `todayStats should return EMPTY for no entries`

Plus 6 existing tests from F11 for start/stop/pause/resume.

## Key Design Patterns

1. **MVVM**: Clear separation between UI (Screen), ViewModel, and Domain logic
2. **Repository Pattern**: Abstraction over data access
3. **State Flow**: Reactive state management
4. **Combine**: Merging multiple flows for computed state
5. **TDD**: Tests written before implementation
6. **Immutable Data Classes**: All state is immutable
7. **Dependency Injection**: Hilt provides all dependencies

## Performance Considerations

1. **Flow WhileSubscribed(5000)**: Stats flow stops collecting 5 seconds after UI stops observing
2. **State In**: Converts cold Flow to hot StateFlow, shared among collectors
3. **Live Timer**: Only runs when TrackingCard is visible (LaunchedEffect lifecycle)
4. **Calculation Efficiency**: O(n) for entries and pauses, no nested loops
