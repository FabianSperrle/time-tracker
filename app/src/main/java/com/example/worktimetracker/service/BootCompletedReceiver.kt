package com.example.worktimetracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives BOOT_COMPLETED broadcast and restores tracking state.
 * If tracking was active before reboot, restarts the foreground service.
 * Also re-registers geofences since they are lost on reboot.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var stateMachine: TrackingStateMachine

    @Inject
    lateinit var geofenceRegistrar: GeofenceRegistrar

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        // Restore state from storage
        CoroutineScope(Dispatchers.Default).launch {
            stateMachine.restoreState()

            // Re-register geofences (they are lost on reboot)
            try {
                geofenceRegistrar.registerAllZones()
                Log.d(TAG, "Geofences re-registered after boot")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-register geofences after boot", e)
            }

            // If tracking was active, restart the service
            val currentState = stateMachine.state.value
            if (currentState is TrackingState.Tracking || currentState is TrackingState.Paused) {
                val serviceIntent = Intent(context, TrackingForegroundService::class.java).apply {
                    action = TrackingForegroundService.ACTION_START
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
