package com.example.worktimetracker.service

import android.content.Context
import android.content.Intent
import com.example.worktimetracker.di.ServiceDispatcher
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle of the TrackingForegroundService by observing
 * the TrackingStateMachine and starting the service when needed.
 */
@Singleton
class TrackingServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stateMachine: TrackingStateMachine,
    @ServiceDispatcher private val dispatcher: CoroutineDispatcher
) {
    private var observerScope: CoroutineScope? = null

    /**
     * Starts observing the state machine and launches the foreground service
     * when tracking becomes active (TRACKING or PAUSED states).
     */
    fun startObserving() {
        // Prevent multiple observers
        if (observerScope != null) return

        observerScope = CoroutineScope(dispatcher + Job()).apply {
            launch {
                stateMachine.state
                    .map { state -> state !is TrackingState.Idle }
                    .distinctUntilChanged()
                    .collect { isActive ->
                        if (isActive) {
                            startForegroundService()
                        }
                    }
            }
        }
    }

    /**
     * Stops observing the state machine.
     * Call this when the app process is shutting down (usually not needed).
     */
    fun stopObserving() {
        observerScope?.cancel()
        observerScope = null
    }

    private fun startForegroundService() {
        val intent = Intent(context, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_START
        }
        context.startForegroundService(intent)
    }
}
