package com.example.worktimetracker.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.worktimetracker.MainActivity
import com.example.worktimetracker.R
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Foreground service that keeps tracking alive in the background.
 * Shows a persistent notification with current tracking status.
 */
@AndroidEntryPoint
class TrackingForegroundService : Service() {

    @Inject
    lateinit var stateMachine: TrackingStateMachine

    @Inject
    lateinit var channelManager: NotificationChannelManager

    @Inject
    lateinit var beaconScanner: BeaconScanner

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var stateJob: Job? = null
    private var updateJob: Job? = null
    private var isBeaconScanningStarted = false

    companion object {
        const val ACTION_START = "com.example.worktimetracker.action.START"
        const val ACTION_STOP = "com.example.worktimetracker.action.STOP"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()

        // Ensure notification channels are created
        channelManager.createChannels()

        // Start BLE beacon scanning
        if (!isBeaconScanningStarted) {
            serviceScope.launch {
                try {
                    beaconScanner.startMonitoring()
                    isBeaconScanningStarted = true
                } catch (e: Exception) {
                    // Log error but don't crash the service
                    // Beacon scanning is optional and may not be configured
                }
            }
        }

        // Start observing state machine (protect against multiple onCreate calls)
        if (stateJob?.isActive == true) return

        stateJob = serviceScope.launch {
            stateMachine.state.collectLatest { state ->
                when (state) {
                    is TrackingState.Idle -> {
                        stopSelf()
                    }
                    is TrackingState.Tracking -> {
                        startForeground(NOTIFICATION_ID, createTrackingNotification(state))
                        startPeriodicUpdates(state)
                    }
                    is TrackingState.Paused -> {
                        startForeground(NOTIFICATION_ID, createPausedNotification(state))
                        cancelPeriodicUpdates()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // Service already started in onCreate
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stateJob?.cancel()
        stateJob = null
        cancelPeriodicUpdates()

        // Stop beacon scanning
        if (isBeaconScanningStarted) {
            beaconScanner.stopMonitoring()
            isBeaconScanningStarted = false
        }

        serviceScope.cancel()
    }

    private fun startPeriodicUpdates(state: TrackingState.Tracking) {
        cancelPeriodicUpdates()
        updateJob = serviceScope.launch {
            while (isActive) {
                delay(60_000) // Update every 60 seconds
                updateNotification(state)
            }
        }
    }

    private fun cancelPeriodicUpdates() {
        updateJob?.cancel()
        updateJob = null
    }

    private fun updateNotification(state: TrackingState.Tracking) {
        val notification = createTrackingNotification(state)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createTrackingNotification(state: TrackingState.Tracking): Notification {
        val elapsed = Duration.between(state.startTime, LocalDateTime.now())
        val hours = elapsed.toHours()
        val minutes = elapsed.toMinutes() % 60
        val elapsedText = "${hours}h ${minutes}min"

        val typeText = when (state.type) {
            com.example.worktimetracker.data.local.entity.TrackingType.HOME_OFFICE -> "Home Office"
            com.example.worktimetracker.data.local.entity.TrackingType.COMMUTE_OFFICE -> "Pendel"
            com.example.worktimetracker.data.local.entity.TrackingType.MANUAL -> "Manuell"
        }

        val startTimeText = state.startTime.toLocalTime().toString()

        // Intent to open app
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Pause
        val pauseIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Stop
        val stopIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_TRACKING_ACTIVE)
            .setContentTitle("🔴 Arbeitszeit läuft — $elapsedText")
            .setContentText("$typeText seit $startTimeText")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "Pause", pausePendingIntent)
            .addAction(0, "Stopp", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createPausedNotification(state: TrackingState.Paused): Notification {
        val typeText = when (state.type) {
            com.example.worktimetracker.data.local.entity.TrackingType.HOME_OFFICE -> "Home Office"
            com.example.worktimetracker.data.local.entity.TrackingType.COMMUTE_OFFICE -> "Pendel"
            com.example.worktimetracker.data.local.entity.TrackingType.MANUAL -> "Manuell"
        }

        // Intent to open app
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Resume
        val resumeIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_RESUME
        }
        val resumePendingIntent = PendingIntent.getBroadcast(
            this,
            3,
            resumeIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Stop
        val stopIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_TRACKING_ACTIVE)
            .setContentTitle("⏸ Pause — $typeText")
            .setContentText("Tracking pausiert")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "Fortsetzen", resumePendingIntent)
            .addAction(0, "Stopp", stopPendingIntent)
            .setOngoing(true)
            .build()
    }
}
