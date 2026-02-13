package com.example.worktimetracker

import android.app.Application
import android.util.Log
import com.example.worktimetracker.service.GeofenceRegistrar
import com.example.worktimetracker.service.NotificationChannelManager
import com.example.worktimetracker.service.TrackingServiceManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WorkTimeTrackerApp : Application() {

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    @Inject
    lateinit var trackingServiceManager: TrackingServiceManager

    @Inject
    lateinit var geofenceRegistrar: GeofenceRegistrar

    private val appScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Create notification channels on app start
        notificationChannelManager.createChannels()

        // Start observing tracking state to launch foreground service when needed
        trackingServiceManager.startObserving()

        // Register geofences at app start (they may have been lost due to reboot or system cleanup)
        appScope.launch {
            try {
                geofenceRegistrar.registerAllZones()
            } catch (e: Exception) {
                Log.e("WorkTimeTrackerApp", "Failed to register geofences at app start", e)
            }
        }
    }
}
