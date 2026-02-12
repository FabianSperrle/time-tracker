package com.example.worktimetracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var stateMachine: TrackingStateMachine

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        // Restore state from storage
        CoroutineScope(Dispatchers.Default).launch {
            stateMachine.restoreState()

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
