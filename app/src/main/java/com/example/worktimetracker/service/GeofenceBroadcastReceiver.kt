package com.example.worktimetracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.worktimetracker.data.local.dao.GeofenceDao
import com.example.worktimetracker.data.local.entity.ZoneType
import com.example.worktimetracker.domain.tracking.TrackingEvent
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Receives geofence transition events from Google Play Services.
 *
 * Maps geofence transitions (ENTER/EXIT) to TrackingEvents and forwards
 * them to the TrackingStateMachine. Zone type lookup is done via the
 * GeofenceDao using the geofence request ID (which maps to the zone ID).
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var stateMachine: TrackingStateMachine

    @Inject
    lateinit var geofenceDao: GeofenceDao

    companion object {
        private const val TAG = "GeofenceBroadcastRcv"
        const val ACTION_GEOFENCE_EVENT = "com.example.worktimetracker.action.GEOFENCE_EVENT"

        /**
         * Maps a geofence transition and zone type to a TrackingEvent.
         *
         * Returns null for OFFICE_STATION zones (informational only)
         * and for DWELL transitions (not used).
         *
         * @param transition The geofence transition type (ENTER, EXIT, DWELL)
         * @param zoneType The type of zone that triggered the transition
         * @return The corresponding TrackingEvent, or null if the transition should be ignored
         */
        fun mapTransitionToEvent(transition: Int, zoneType: ZoneType): TrackingEvent? {
            // OFFICE_STATION transitions are informational only (logging)
            if (zoneType == ZoneType.OFFICE_STATION) {
                return null
            }

            return when (transition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    TrackingEvent.GeofenceEntered(zoneType)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    TrackingEvent.GeofenceExited(zoneType)
                }
                else -> null
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "Could not parse GeofencingEvent from intent")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofence error: $errorMessage (code: ${geofencingEvent.errorCode})")
            handleGeofenceError(context, geofencingEvent.errorCode)
            return
        }

        val transition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        val timestamp = LocalDateTime.now()
        val transitionName = when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELL"
            else -> "UNKNOWN($transition)"
        }

        Log.d(TAG, "Geofence transition: $transitionName at $timestamp, " +
                "triggering ${triggeringGeofences.size} geofence(s)")

        // Process each triggering geofence asynchronously.
        // goAsync() extends the BroadcastReceiver lifecycle so the coroutine
        // can complete before the system reclaims the receiver.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                for (geofence in triggeringGeofences) {
                    val zoneId = geofence.requestId
                    val zone = try {
                        geofenceDao.getZoneById(zoneId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load zone for geofence ID: $zoneId", e)
                        continue
                    }

                    if (zone == null) {
                        Log.w(TAG, "No zone found for geofence ID: $zoneId")
                        continue
                    }

                    Log.d(TAG, "Geofence event: $transitionName zone=${zone.name} " +
                            "type=${zone.zoneType} at $timestamp")

                    val event = mapTransitionToEvent(transition, zone.zoneType)
                    if (event != null) {
                        Log.i(TAG, "Forwarding event to state machine: $event")
                        try {
                            stateMachine.processEvent(event)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to process event $event in state machine", e)
                        }
                    } else {
                        Log.d(TAG, "Transition $transitionName for ${zone.zoneType} is informational only")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error processing geofence transitions", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Handles geofence error codes.
     *
     * - GEOFENCE_NOT_AVAILABLE: GPS may be disabled
     * - GEOFENCE_TOO_MANY_GEOFENCES: Too many registered (should not occur)
     * - GEOFENCE_TOO_MANY_PENDING_INTENTS: Too many pending intents
     */
    @Suppress("UNUSED_PARAMETER") // context will be used in F09 for user notifications
    private fun handleGeofenceError(context: Context, errorCode: Int) {
        when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> {
                Log.w(TAG, "Geofence not available - GPS may be disabled")
                // TODO(F09): Show notification to user about GPS being disabled
            }
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> {
                Log.e(TAG, "Too many geofences registered")
            }
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> {
                Log.e(TAG, "Too many pending intents for geofencing")
            }
        }
    }
}
