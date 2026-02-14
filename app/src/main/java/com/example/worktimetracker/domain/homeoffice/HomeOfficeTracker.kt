package com.example.worktimetracker.domain.homeoffice

import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.commute.CommuteDayChecker
import com.example.worktimetracker.domain.tracking.TrackingEvent
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Business logic for Home Office tracking.
 *
 * Connects BLE beacon events with time window validation and controls
 * automatic start/stop of tracking at home workspace.
 */
@Singleton
class HomeOfficeTracker @Inject constructor(
    private val stateMachine: TrackingStateMachine,
    private val commuteDayChecker: CommuteDayChecker,
    private val settingsProvider: SettingsProvider
) {

    /**
     * Called when a beacon is detected.
     *
     * Performs plausibility checks:
     * - Outside work time window: ignored
     * - Already tracking: ignored
     * - Commute day with active commute tracking: ignored
     * - Otherwise: starts HOME_OFFICE tracking
     */
    suspend fun onBeaconDetected(
        uuid: String,
        timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        val workWindow = settingsProvider.workTimeWindow.first()

        // Check if within work time window
        val time = timestamp.toLocalTime()
        if (time.isBefore(workWindow.start) || time.isAfter(workWindow.end)) {
            return
        }

        // Check if already tracking
        val currentState = stateMachine.state.value
        if (currentState is TrackingState.Tracking) {
            // On commute days: ignore beacon if commute tracking is active
            // (user returned home from office and sits at desk)
            val isCommuteDay = commuteDayChecker.isCommuteDay(timestamp.toLocalDate())
            if (isCommuteDay && currentState.type == TrackingType.COMMUTE_OFFICE) {
                return
            }
            // For any other active tracking, also ignore
            return
        }

        // All checks passed - trigger beacon detected event
        stateMachine.processEvent(TrackingEvent.BeaconDetected(uuid, timestamp))
    }

    /**
     * Called when beacon times out.
     *
     * Only stops tracking if current tracking type is HOME_OFFICE.
     */
    suspend fun onBeaconTimeout(
        timestamp: LocalDateTime = LocalDateTime.now(),
        lastSeenTimestamp: LocalDateTime? = null
    ) {
        val currentState = stateMachine.state.value
        if (currentState is TrackingState.Tracking && currentState.type == TrackingType.HOME_OFFICE) {
            stateMachine.processEvent(TrackingEvent.BeaconLost(timestamp, lastSeenTimestamp))
        }
    }
}
