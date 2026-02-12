package com.example.worktimetracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.worktimetracker.domain.tracking.TrackingEvent
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives actions from notification buttons and forwards them to the state machine.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var stateMachine: TrackingStateMachine

    companion object {
        const val ACTION_PAUSE = "com.example.worktimetracker.action.PAUSE"
        const val ACTION_RESUME = "com.example.worktimetracker.action.RESUME"
        const val ACTION_STOP = "com.example.worktimetracker.action.STOP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val event = when (intent.action) {
            ACTION_PAUSE -> TrackingEvent.PauseStart
            ACTION_RESUME -> TrackingEvent.PauseEnd
            ACTION_STOP -> TrackingEvent.ManualStop
            else -> return
        }

        // Process event asynchronously using goAsync() to prevent ANR
        // This ensures the broadcast remains active while the coroutine executes
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                stateMachine.processEvent(event)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
