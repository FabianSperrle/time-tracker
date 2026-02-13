package com.example.worktimetracker.service

import android.app.PendingIntent
import android.util.Log
import com.example.worktimetracker.data.local.dao.GeofenceDao
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages registration and removal of geofences with Google Play Services.
 *
 * Loads geofence zones from the database and registers them with the
 * GeofencingClient. Geofences must be re-registered after device reboot
 * and when zone configuration changes.
 */
@Singleton
class GeofenceRegistrar @Inject constructor(
    private val geofencingClient: GeofencingClient,
    private val geofenceDao: GeofenceDao,
    private val geofencePendingIntent: PendingIntent
) {

    companion object {
        private const val TAG = "GeofenceRegistrar"
    }

    /**
     * Registers geofences for all zones stored in the database.
     * Does nothing if no zones are configured.
     */
    @SuppressWarnings("MissingPermission")
    suspend fun registerAllZones() {
        val zones = geofenceDao.getAllZonesOnce()
        if (zones.isEmpty()) {
            Log.d(TAG, "No geofence zones configured, skipping registration")
            return
        }

        val geofences = zones.map { zone ->
            Geofence.Builder()
                .setRequestId(zone.id)
                .setCircularRegion(zone.latitude, zone.longitude, zone.radiusMeters)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully registered ${zones.size} geofence(s)")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to register geofences: ${e.message}", e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing location permission for geofence registration", e)
        }
    }

    /**
     * Removes all registered geofences.
     */
    suspend fun unregisterAll() {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully removed all geofences")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove geofences: ${e.message}", e)
            }
    }

    /**
     * Refreshes geofence registrations by removing all existing ones
     * and re-registering from the database.
     *
     * Should be called when zone configuration changes in the map (F06).
     */
    suspend fun refreshRegistrations() {
        unregisterAll()
        registerAllZones()
    }
}
