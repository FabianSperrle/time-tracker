package com.example.worktimetracker

import android.app.Application
import com.example.worktimetracker.service.NotificationChannelManager
import com.example.worktimetracker.service.TrackingServiceManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WorkTimeTrackerApp : Application() {

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    @Inject
    lateinit var trackingServiceManager: TrackingServiceManager

    override fun onCreate() {
        super.onCreate()

        // Create notification channels on app start
        notificationChannelManager.createChannels()

        // Start observing tracking state to launch foreground service when needed
        trackingServiceManager.startObserving()
    }
}
