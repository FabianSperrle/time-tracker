package com.example.worktimetracker.domain.tracking

import android.content.SharedPreferences
import com.example.worktimetracker.data.local.entity.TrackingType
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists and restores tracking state to/from SharedPreferences.
 */
@Singleton
class TrackingStateStorage @Inject constructor(
    private val preferences: SharedPreferences
) {
    companion object {
        private const val KEY_STATE_TYPE = "tracking_state_type"
        private const val KEY_ENTRY_ID = "tracking_entry_id"
        private const val KEY_PAUSE_ID = "tracking_pause_id"
        private const val KEY_TRACKING_TYPE = "tracking_type"
        private const val KEY_START_TIME = "tracking_start_time"

        private const val STATE_IDLE = "IDLE"
        private const val STATE_TRACKING = "TRACKING"
        private const val STATE_PAUSED = "PAUSED"
    }

    /**
     * Saves the current tracking state to SharedPreferences.
     */
    fun saveState(state: TrackingState) {
        preferences.edit().apply {
            when (state) {
                is TrackingState.Idle -> {
                    putString(KEY_STATE_TYPE, STATE_IDLE)
                    remove(KEY_ENTRY_ID)
                    remove(KEY_PAUSE_ID)
                    remove(KEY_TRACKING_TYPE)
                    remove(KEY_START_TIME)
                }
                is TrackingState.Tracking -> {
                    putString(KEY_STATE_TYPE, STATE_TRACKING)
                    putString(KEY_ENTRY_ID, state.entryId)
                    putString(KEY_TRACKING_TYPE, state.type.name)
                    putString(KEY_START_TIME, state.startTime.toString())
                    remove(KEY_PAUSE_ID)
                }
                is TrackingState.Paused -> {
                    putString(KEY_STATE_TYPE, STATE_PAUSED)
                    putString(KEY_ENTRY_ID, state.entryId)
                    putString(KEY_PAUSE_ID, state.pauseId)
                    putString(KEY_TRACKING_TYPE, state.type.name)
                    remove(KEY_START_TIME)
                }
            }
        }.apply()
    }

    /**
     * Loads the tracking state from SharedPreferences.
     * Returns Idle if no state is saved or if state is corrupted.
     */
    fun loadState(): TrackingState {
        val stateType = preferences.getString(KEY_STATE_TYPE, STATE_IDLE)

        return try {
            when (stateType) {
                STATE_TRACKING -> {
                    val entryId = preferences.getString(KEY_ENTRY_ID, null)
                    val trackingType = preferences.getString(KEY_TRACKING_TYPE, null)
                    val startTime = preferences.getString(KEY_START_TIME, null)

                    if (entryId != null && trackingType != null && startTime != null) {
                        TrackingState.Tracking(
                            entryId = entryId,
                            type = TrackingType.valueOf(trackingType),
                            startTime = LocalDateTime.parse(startTime)
                        )
                    } else {
                        TrackingState.Idle
                    }
                }
                STATE_PAUSED -> {
                    val entryId = preferences.getString(KEY_ENTRY_ID, null)
                    val pauseId = preferences.getString(KEY_PAUSE_ID, null)
                    val trackingType = preferences.getString(KEY_TRACKING_TYPE, null)

                    if (entryId != null && pauseId != null && trackingType != null) {
                        TrackingState.Paused(
                            entryId = entryId,
                            type = TrackingType.valueOf(trackingType),
                            pauseId = pauseId
                        )
                    } else {
                        TrackingState.Idle
                    }
                }
                else -> TrackingState.Idle
            }
        } catch (e: Exception) {
            // Handle corrupted state (e.g., invalid enum, invalid datetime)
            TrackingState.Idle
        }
    }

    /**
     * Clears all saved state.
     */
    fun clear() {
        preferences.edit().apply {
            remove(KEY_STATE_TYPE)
            remove(KEY_ENTRY_ID)
            remove(KEY_PAUSE_ID)
            remove(KEY_TRACKING_TYPE)
            remove(KEY_START_TIME)
        }.apply()
    }
}
