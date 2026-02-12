package com.example.worktimetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worktimetracker.data.OnboardingPreferences
import com.example.worktimetracker.domain.PermissionChecker
import com.example.worktimetracker.domain.PermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the onboarding flow.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    private val _currentStep = MutableStateFlow(OnboardingStep.WELCOME)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    private val _permissionStatus = MutableStateFlow(
        PermissionStatus(
            location = false,
            backgroundLocation = false,
            bluetooth = false,
            notification = false,
            batteryOptimization = false
        )
    )
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    init {
        refreshPermissionStatus()
    }

    /**
     * Advances to the next onboarding step.
     */
    fun nextStep() {
        val current = _currentStep.value
        _currentStep.value = when (current) {
            OnboardingStep.WELCOME -> OnboardingStep.LOCATION
            OnboardingStep.LOCATION -> OnboardingStep.BLUETOOTH
            OnboardingStep.BLUETOOTH -> OnboardingStep.BATTERY
            OnboardingStep.BATTERY -> OnboardingStep.NOTIFICATION
            OnboardingStep.NOTIFICATION -> {
                markOnboardingComplete()
                current
            }
        }
    }

    /**
     * Goes back to the previous onboarding step.
     */
    fun previousStep() {
        val current = _currentStep.value
        _currentStep.value = when (current) {
            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
            OnboardingStep.LOCATION -> OnboardingStep.WELCOME
            OnboardingStep.BLUETOOTH -> OnboardingStep.LOCATION
            OnboardingStep.BATTERY -> OnboardingStep.BLUETOOTH
            OnboardingStep.NOTIFICATION -> OnboardingStep.BATTERY
        }
    }

    /**
     * Refreshes the current permission status.
     */
    fun refreshPermissionStatus() {
        viewModelScope.launch {
            _permissionStatus.value = permissionChecker.checkAllPermissions()
        }
    }

    /**
     * Skips the onboarding flow.
     */
    fun skipOnboarding() {
        _isCompleted.value = true
    }

    /**
     * Marks onboarding as complete.
     */
    fun markOnboardingComplete() {
        onboardingPreferences.markOnboardingCompleted()
        _isCompleted.value = true
    }
}

/**
 * Enum representing the onboarding steps.
 */
enum class OnboardingStep {
    WELCOME,
    LOCATION,
    BLUETOOTH,
    BATTERY,
    NOTIFICATION
}
