package com.example.worktimetracker.service

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.worktimetracker.R
import com.example.worktimetracker.data.repository.TrackingRepository
import com.example.worktimetracker.domain.commute.CommuteDayChecker
import com.example.worktimetracker.domain.commute.CommuteReminderLogic
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalTime

/**
 * WorkManager worker that checks for commute reminders.
 *
 * Two types of reminders:
 * 1. "No tracking started" - if it's a commute day and no tracking is active by 10:00
 * 2. "Forgot to stop" - if tracking is still active after 21:00
 */
@HiltWorker
class CommuteReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val commuteDayChecker: CommuteDayChecker,
    private val trackingRepository: TrackingRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "commute_reminder"
        const val NOTIFICATION_ID_NO_TRACKING = 100
        const val NOTIFICATION_ID_LATE_TRACKING = 101
    }

    override suspend fun doWork(): Result {
        val currentTime = LocalTime.now()
        val isCommuteDay = commuteDayChecker.isCommuteDay()
        val hasActiveTracking = trackingRepository.getActiveEntry() != null

        // Check "no tracking started" reminder
        if (CommuteReminderLogic.shouldShowNoTrackingReminder(
                currentTime = currentTime,
                isCommuteDay = isCommuteDay,
                hasActiveTracking = hasActiveTracking
            )) {
            showNoTrackingNotification()
        }

        // Check "forgot to stop tracking" reminder
        if (CommuteReminderLogic.shouldShowLateTrackingReminder(
                currentTime = currentTime,
                hasActiveTracking = hasActiveTracking
            )) {
            showLateTrackingNotification()
        }

        return Result.success()
    }

    private fun showNoTrackingNotification() {
        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationChannelManager.CHANNEL_TRACKING_REMINDERS
        )
            .setContentTitle("Pendeltag - Tracking nicht gestartet")
            .setContentText("Heute ist Pendeltag, aber es wurde noch kein Tracking gestartet.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_NO_TRACKING, notification)
    }

    private fun showLateTrackingNotification() {
        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationChannelManager.CHANNEL_TRACKING_REMINDERS
        )
            .setContentTitle("Tracking noch aktiv")
            .setContentText("Tracking noch aktiv - vergessen zu stoppen?")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_LATE_TRACKING, notification)
    }
}
