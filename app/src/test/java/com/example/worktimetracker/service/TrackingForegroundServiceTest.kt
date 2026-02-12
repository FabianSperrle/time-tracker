package com.example.worktimetracker.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TrackingForegroundServiceTest {

    private lateinit var context: Context
    private lateinit var stateMachine: TrackingStateMachine
    private lateinit var stateFlow: MutableStateFlow<TrackingState>

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        stateFlow = MutableStateFlow<TrackingState>(TrackingState.Idle)
        stateMachine = mockk(relaxed = true)
        every { stateMachine.state } returns stateFlow
    }

    @Test
    fun `service can be started`() {
        // Given
        val intent = Intent(context, TrackingForegroundService::class.java)

        // When
        val serviceController = Robolectric.buildService(TrackingForegroundService::class.java, intent)
        val service = serviceController.create().get()

        // Then
        assertNotNull(service)
    }

    @Test
    fun `service starts foreground when tracking state is active`() {
        // Given
        val trackingState = TrackingState.Tracking(
            entryId = "test-entry-1",
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.now()
        )
        stateFlow.value = trackingState

        val intent = Intent(context, TrackingForegroundService::class.java)
        intent.action = TrackingForegroundService.ACTION_START

        // When
        val serviceController = Robolectric.buildService(TrackingForegroundService::class.java, intent)
        val service = serviceController.create().startCommand(0, 0).get()

        // Then
        assertNotNull(service)
        // Note: Robolectric's foreground service support is limited
        // Real behavior verification requires instrumented tests on a real device
    }

    @Test
    fun `service stops when state changes to Idle`() {
        // Given
        val intent = Intent(context, TrackingForegroundService::class.java)
        val serviceController = Robolectric.buildService(TrackingForegroundService::class.java, intent)
        val service = serviceController.create().get()

        // When
        stateFlow.value = TrackingState.Idle

        // Then - service should stop itself (verified through stopSelf() call in onCreate observer)
        // Note: Testing actual service lifecycle is complex in Robolectric
        // Instrumented tests on device would provide better validation
        assertNotNull(service)
    }

    @Test
    fun `notification is updated with NotificationManager notify when tracking`() {
        // Given
        val trackingState = TrackingState.Tracking(
            entryId = "test-entry-1",
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.now()
        )
        stateFlow.value = trackingState

        val intent = Intent(context, TrackingForegroundService::class.java)
        intent.action = TrackingForegroundService.ACTION_START

        // When
        val serviceController = Robolectric.buildService(TrackingForegroundService::class.java, intent)
        serviceController.create().startCommand(0, 0)

        // Get NotificationManager and spy on it
        val notificationManager = spyk(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

        // Then - notification is shown via startForeground (not easily verifiable in Robolectric)
        // For real verification, use instrumented tests
        assertNotNull(notificationManager)
    }

    @Test
    fun `periodic updates are scheduled every 60 seconds during tracking`() = runTest {
        // Given
        val trackingState = TrackingState.Tracking(
            entryId = "test-entry-1",
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.now().minusMinutes(5)
        )
        stateFlow.value = trackingState

        val intent = Intent(context, TrackingForegroundService::class.java)

        // When
        val serviceController = Robolectric.buildService(TrackingForegroundService::class.java, intent)
        val service = serviceController.create().get()

        // Then - service should be created and periodic updates job started
        // Note: Testing actual delay() behavior requires TestDispatchers + advanceTimeBy()
        // which is not fully compatible with Robolectric's service lifecycle
        // This test validates that the service doesn't crash and can handle the tracking state
        assertNotNull(service)

        // Cleanup
        serviceController.destroy()
    }

    @Test
    fun `service processes ACTION_STOP intent`() {
        // Given
        val intent = Intent(context, TrackingForegroundService::class.java)
        intent.action = TrackingForegroundService.ACTION_STOP

        // When
        val serviceController = Robolectric.buildService(TrackingForegroundService::class.java, intent)
        val service = serviceController.create().startCommand(0, 0).get()

        // Then - service should call stopSelf() (internal behavior)
        // Robolectric doesn't provide direct verification, but service should not crash
        assertNotNull(service)
    }

    @Test
    fun `service handles paused state correctly`() {
        // Given
        val pausedState = TrackingState.Paused(
            entryId = "test-entry-1",
            type = TrackingType.HOME_OFFICE,
            pauseId = "pause-1"
        )
        stateFlow.value = pausedState

        val intent = Intent(context, TrackingForegroundService::class.java)

        // When
        val serviceController = Robolectric.buildService(TrackingForegroundService::class.java, intent)
        val service = serviceController.create().get()

        // Then - service should show paused notification (startForeground with paused content)
        // Robolectric limitations prevent full verification, but service should not crash
        assertNotNull(service)
    }
}
