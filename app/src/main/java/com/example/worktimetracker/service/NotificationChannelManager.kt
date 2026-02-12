package com.example.worktimetracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages notification channels for the tracking system.
 */
@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_TRACKING_ACTIVE = "tracking_active"
        const val CHANNEL_TRACKING_EVENTS = "tracking_events"
        const val CHANNEL_TRACKING_REMINDERS = "tracking_reminders"
    }

    /**
     * Creates all required notification channels.
     * Should be called once during app initialization.
     */
    fun createChannels() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Channel 1: Active Tracking (LOW priority - persistent notification)
        val activeChannel = NotificationChannel(
            CHANNEL_TRACKING_ACTIVE,
            "Aktives Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent Notification während Tracking läuft"
        }

        // Channel 2: Tracking Events (HIGH priority - confirmations)
        val eventsChannel = NotificationChannel(
            CHANNEL_TRACKING_EVENTS,
            "Tracking Events",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Bestätigungs-Notifications (Start/Stop)"
        }

        // Channel 3: Reminders (DEFAULT priority)
        val remindersChannel = NotificationChannel(
            CHANNEL_TRACKING_REMINDERS,
            "Erinnerungen",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Pendeltag-Reminder, Export-Erinnerung"
        }

        notificationManager.createNotificationChannel(activeChannel)
        notificationManager.createNotificationChannel(eventsChannel)
        notificationManager.createNotificationChannel(remindersChannel)
    }
}
