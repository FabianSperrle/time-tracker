package com.example.worktimetracker.domain.tracking

import com.example.worktimetracker.data.local.entity.TrackingType
import java.time.LocalDateTime

/**
 * Represents the current state of the tracking system.
 */
sealed class TrackingState {
    /**
     * No active tracking session.
     */
    object Idle : TrackingState()

    /**
     * Currently tracking work time.
     *
     * @property entryId The ID of the active tracking entry
     * @property type The type of tracking
     * @property startTime When tracking started
     */
    data class Tracking(
        val entryId: String,
        val type: TrackingType,
        val startTime: LocalDateTime
    ) : TrackingState()

    /**
     * Tracking is paused.
     *
     * @property entryId The ID of the tracking entry
     * @property type The type of tracking
     * @property pauseId The ID of the active pause
     */
    data class Paused(
        val entryId: String,
        val type: TrackingType,
        val pauseId: String
    ) : TrackingState()
}
