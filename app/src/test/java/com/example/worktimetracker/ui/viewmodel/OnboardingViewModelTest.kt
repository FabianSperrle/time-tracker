package com.example.worktimetracker.ui.viewmodel

import com.example.worktimetracker.data.OnboardingPreferences
import com.example.worktimetracker.domain.PermissionChecker
import com.example.worktimetracker.domain.PermissionStatus
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for OnboardingViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private lateinit var permissionChecker: PermissionChecker
    private lateinit var onboardingPreferences: OnboardingPreferences
    private lateinit var viewModel: OnboardingViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        permissionChecker = mockk(relaxed = true)
        onboardingPreferences = mockk(relaxed = true)
        justRun { onboardingPreferences.markOnboardingCompleted() }
        viewModel = OnboardingViewModel(permissionChecker, onboardingPreferences)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Welcome screen`() {
        // Then
        assertEquals(OnboardingStep.WELCOME, viewModel.currentStep.value)
        assertFalse(viewModel.isCompleted.value)
    }

    @Test
    fun `nextStep advances from Welcome to Location`() {
        // When
        viewModel.nextStep()

        // Then
        assertEquals(OnboardingStep.LOCATION, viewModel.currentStep.value)
    }

    @Test
    fun `nextStep advances through all steps in order`() {
        // When/Then - Welcome -> Location
        assertEquals(OnboardingStep.WELCOME, viewModel.currentStep.value)
        viewModel.nextStep()
        assertEquals(OnboardingStep.LOCATION, viewModel.currentStep.value)

        // Location -> Bluetooth
        viewModel.nextStep()
        assertEquals(OnboardingStep.BLUETOOTH, viewModel.currentStep.value)

        // Bluetooth -> Battery
        viewModel.nextStep()
        assertEquals(OnboardingStep.BATTERY, viewModel.currentStep.value)

        // Battery -> Notification
        viewModel.nextStep()
        assertEquals(OnboardingStep.NOTIFICATION, viewModel.currentStep.value)

        // Notification -> Completed
        viewModel.nextStep()
        assertTrue(viewModel.isCompleted.value)
    }

    @Test
    fun `previousStep navigates back through steps`() {
        // Given - navigate to Battery step
        viewModel.nextStep() // Location
        viewModel.nextStep() // Bluetooth
        viewModel.nextStep() // Battery

        // When
        viewModel.previousStep()

        // Then
        assertEquals(OnboardingStep.BLUETOOTH, viewModel.currentStep.value)
    }

    @Test
    fun `previousStep does nothing on Welcome screen`() {
        // When
        viewModel.previousStep()

        // Then
        assertEquals(OnboardingStep.WELCOME, viewModel.currentStep.value)
    }

    @Test
    fun `refreshPermissionStatus calls permissionChecker`() = runTest {
        // Given
        val permissionStatus = PermissionStatus(
            location = true,
            backgroundLocation = true,
            bluetooth = true,
            notification = true,
            batteryOptimization = true
        )
        every { permissionChecker.checkAllPermissions() } returns permissionStatus

        // When
        viewModel.refreshPermissionStatus()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { permissionChecker.checkAllPermissions() }
        assertEquals(permissionStatus, viewModel.permissionStatus.value)
    }

    @Test
    fun `skipOnboarding sets isCompleted to true`() {
        // When
        viewModel.skipOnboarding()

        // Then
        assertTrue(viewModel.isCompleted.value)
    }

    @Test
    fun `markOnboardingComplete sets isCompleted to true and saves to preferences`() {
        // When
        viewModel.markOnboardingComplete()

        // Then
        assertTrue(viewModel.isCompleted.value)
        verify { onboardingPreferences.markOnboardingCompleted() }
    }
}
