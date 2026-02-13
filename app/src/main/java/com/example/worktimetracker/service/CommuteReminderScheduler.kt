package com.example.worktimetracker.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the CommuteReminderWorker to run periodically.
 *
 * The worker checks at regular intervals whether reminders should be shown:
 * - "No tracking started" reminder (at ~10:00 on commute days)
 * - "Forgot to stop tracking" reminder (at ~21:00)
 */
@Singleton
class CommuteReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Schedules the commute reminder worker as a periodic task.
     * Uses a 1-hour interval with KEEP policy (does not replace existing).
     */
    fun scheduleReminders() {
        val workRequest = PeriodicWorkRequestBuilder<CommuteReminderWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CommuteReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Cancels the scheduled commute reminder worker.
     */
    fun cancelReminders() {
        WorkManager.getInstance(context).cancelUniqueWork(CommuteReminderWorker.WORK_NAME)
    }
}
