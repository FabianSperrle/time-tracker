package com.example.worktimetracker.service

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.worktimetracker.data.local.entity.TrackingType
import com.example.worktimetracker.domain.tracking.TrackingState
import com.example.worktimetracker.domain.tracking.TrackingStateMachine
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TrackingServiceManagerTest {

    private lateinit var context: Context
    private lateinit var stateMachine: TrackingStateMachine
    private lateinit var stateFlow: MutableStateFlow<TrackingState>
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var serviceManager: TrackingServiceManager

    @Before
    fun setup() {
        context = spyk(ApplicationProvider.getApplicationContext())
        stateFlow = MutableStateFlow<TrackingState>(TrackingState.Idle)
        stateMachine = mockk(relaxed = true)
        every { stateMachine.state } returns stateFlow
        testDispatcher = UnconfinedTestDispatcher()
    }

    @Test
    fun `startObserving starts service when state changes to Tracking`() = runTest(testDispatcher) {
        // Given
        serviceManager = TrackingServiceManager(context, stateMachine, testDispatcher)
        val intentSlot = slot<Intent>()

        every { context.startForegroundService(capture(intentSlot)) } returns null

        // When
        serviceManager.startObserving()
        stateFlow.value = TrackingState.Tracking(
            entryId = "test-entry",
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.now()
        )

        // Then
        verify(exactly = 1) { context.startForegroundService(any()) }
        assert(intentSlot.captured.component?.className == TrackingForegroundService::class.java.name)
    }

    @Test
    fun `startObserving starts service when state changes to Paused`() = runTest(testDispatcher) {
        // Given
        serviceManager = TrackingServiceManager(context, stateMachine, testDispatcher)
        val intentSlot = slot<Intent>()

        every { context.startForegroundService(capture(intentSlot)) } returns null

        // When
        serviceManager.startObserving()
        stateFlow.value = TrackingState.Paused(
            entryId = "test-entry",
            type = TrackingType.HOME_OFFICE,
            pauseId = "pause-1"
        )

        // Then
        verify(exactly = 1) { context.startForegroundService(any()) }
        assert(intentSlot.captured.component?.className == TrackingForegroundService::class.java.name)
    }

    @Test
    fun `startObserving does not start service when state is Idle`() = runTest(testDispatcher) {
        // Given
        serviceManager = TrackingServiceManager(context, stateMachine, testDispatcher)

        every { context.startForegroundService(any()) } returns null

        // When
        serviceManager.startObserving()
        stateFlow.value = TrackingState.Idle

        // Then
        verify(exactly = 0) { context.startForegroundService(any()) }
    }

    @Test
    fun `startObserving does not start service multiple times for same state`() = runTest(testDispatcher) {
        // Given
        serviceManager = TrackingServiceManager(context, stateMachine, testDispatcher)
        val trackingState = TrackingState.Tracking(
            entryId = "test-entry",
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.now()
        )

        every { context.startForegroundService(any()) } returns null

        // When
        serviceManager.startObserving()
        stateFlow.value = trackingState
        stateFlow.value = trackingState // Same state again

        // Then - should only start once
        verify(exactly = 1) { context.startForegroundService(any()) }
    }

    @Test
    fun `stopObserving cancels state observation`() = runTest(testDispatcher) {
        // Given
        serviceManager = TrackingServiceManager(context, stateMachine, testDispatcher)

        every { context.startForegroundService(any()) } returns null

        serviceManager.startObserving()

        // When
        serviceManager.stopObserving()

        // State change after stopping should not start service
        stateFlow.value = TrackingState.Tracking(
            entryId = "test-entry",
            type = TrackingType.HOME_OFFICE,
            startTime = LocalDateTime.now()
        )

        // Then
        verify(exactly = 0) { context.startForegroundService(any()) }
    }
}
