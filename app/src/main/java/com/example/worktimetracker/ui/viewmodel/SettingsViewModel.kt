package com.example.worktimetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worktimetracker.data.repository.GeofenceRepository
import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.model.TimeWindow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsProvider: SettingsProvider,
    private val geofenceRepository: GeofenceRepository
) : ViewModel() {

    private val _dialogState = MutableStateFlow(DialogState())

    /**
     * UI state for the settings screen.
     */
    val uiState = combine(
        combine(
            settingsProvider.commuteDays,
            settingsProvider.outboundWindow,
            settingsProvider.returnWindow,
            settingsProvider.beaconUuid
        ) { commuteDays, outboundWindow, returnWindow, beaconUuid ->
            PartialSettings1(commuteDays, outboundWindow, returnWindow, beaconUuid)
        },
        combine(
            settingsProvider.beaconTimeout,
            settingsProvider.bleScanInterval,
            settingsProvider.workTimeWindow,
            settingsProvider.weeklyTargetHours
        ) { beaconTimeout, bleScanInterval, workTimeWindow, weeklyTargetHours ->
            PartialSettings2(beaconTimeout, bleScanInterval, workTimeWindow, weeklyTargetHours)
        },
        _dialogState,
        geofenceRepository.getAllZones().map { it.size }
    ) { partial1, partial2, dialogState, zoneCount ->
        SettingsUiState(
            commuteDays = partial1.commuteDays,
            outboundWindow = partial1.outboundWindow,
            returnWindow = partial1.returnWindow,
            beaconUuid = partial1.beaconUuid,
            beaconTimeout = partial2.beaconTimeout,
            bleScanInterval = partial2.bleScanInterval,
            workTimeWindow = partial2.workTimeWindow,
            weeklyTargetHours = partial2.weeklyTargetHours,
            zoneCount = zoneCount,
            showResetConfirmation = dialogState.showResetConfirmation,
            showCommuteDaysDialog = dialogState.showCommuteDaysDialog,
            showOutboundWindowDialog = dialogState.showOutboundWindowDialog,
            showReturnWindowDialog = dialogState.showReturnWindowDialog,
            showWorkTimeWindowDialog = dialogState.showWorkTimeWindowDialog,
            showBeaconTimeoutDialog = dialogState.showBeaconTimeoutDialog,
            showBleScanIntervalDialog = dialogState.showBleScanIntervalDialog,
            showWeeklyTargetHoursDialog = dialogState.showWeeklyTargetHoursDialog,
            showBeaconUuidDialog = dialogState.showBeaconUuidDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    /**
     * Updates the commute days.
     */
    fun updateCommuteDays(days: Set<DayOfWeek>) {
        viewModelScope.launch {
            settingsProvider.setCommuteDays(days)
        }
    }

    /**
     * Updates the outbound commute time window.
     */
    fun updateOutboundWindow(window: TimeWindow) {
        viewModelScope.launch {
            settingsProvider.setOutboundWindow(window)
        }
    }

    /**
     * Updates the return commute time window.
     */
    fun updateReturnWindow(window: TimeWindow) {
        viewModelScope.launch {
            settingsProvider.setReturnWindow(window)
        }
    }

    /**
     * Updates the beacon UUID.
     */
    fun updateBeaconUuid(uuid: String?) {
        viewModelScope.launch {
            settingsProvider.setBeaconUuid(uuid)
        }
    }

    /**
     * Updates the beacon timeout in minutes.
     */
    fun updateBeaconTimeout(minutes: Int) {
        viewModelScope.launch {
            try {
                settingsProvider.setBeaconTimeout(minutes)
            } catch (e: IllegalArgumentException) {
                // Handle validation error - could emit to UI state
            }
        }
    }

    /**
     * Updates the BLE scan interval in seconds.
     */
    fun updateBleScanInterval(seconds: Int) {
        viewModelScope.launch {
            try {
                settingsProvider.setBleScanInterval(seconds)
            } catch (e: IllegalArgumentException) {
                // Handle validation error - could emit to UI state
            }
        }
    }

    /**
     * Updates the work time window.
     */
    fun updateWorkTimeWindow(window: TimeWindow) {
        viewModelScope.launch {
            settingsProvider.setWorkTimeWindow(window)
        }
    }

    /**
     * Updates the weekly target hours.
     */
    fun updateWeeklyTargetHours(hours: Float) {
        viewModelScope.launch {
            try {
                settingsProvider.setWeeklyTargetHours(hours)
            } catch (e: IllegalArgumentException) {
                // Handle validation error - could emit to UI state
            }
        }
    }

    /**
     * Shows the reset confirmation dialog.
     */
    fun showResetConfirmation() {
        _dialogState.update { it.copy(showResetConfirmation = true) }
    }

    /**
     * Dismisses the reset confirmation dialog.
     */
    fun dismissResetConfirmation() {
        _dialogState.update { it.copy(showResetConfirmation = false) }
    }

    /**
     * Resets all data.
     */
    fun resetAllData() {
        viewModelScope.launch {
            settingsProvider.clearAllSettings()
            dismissResetConfirmation()
        }
    }

    /**
     * Shows the commute days selection dialog.
     */
    fun showCommuteDaysDialog() {
        _dialogState.update { it.copy(showCommuteDaysDialog = true) }
    }

    /**
     * Dismisses the commute days selection dialog.
     */
    fun dismissCommuteDaysDialog() {
        _dialogState.update { it.copy(showCommuteDaysDialog = false) }
    }

    /**
     * Shows the outbound time window dialog.
     */
    fun showOutboundWindowDialog() {
        _dialogState.update { it.copy(showOutboundWindowDialog = true) }
    }

    /**
     * Dismisses the outbound time window dialog.
     */
    fun dismissOutboundWindowDialog() {
        _dialogState.update { it.copy(showOutboundWindowDialog = false) }
    }

    /**
     * Shows the return time window dialog.
     */
    fun showReturnWindowDialog() {
        _dialogState.update { it.copy(showReturnWindowDialog = true) }
    }

    /**
     * Dismisses the return time window dialog.
     */
    fun dismissReturnWindowDialog() {
        _dialogState.update { it.copy(showReturnWindowDialog = false) }
    }

    /**
     * Shows the work time window dialog.
     */
    fun showWorkTimeWindowDialog() {
        _dialogState.update { it.copy(showWorkTimeWindowDialog = true) }
    }

    /**
     * Dismisses the work time window dialog.
     */
    fun dismissWorkTimeWindowDialog() {
        _dialogState.update { it.copy(showWorkTimeWindowDialog = false) }
    }

    /**
     * Shows the beacon timeout dialog.
     */
    fun showBeaconTimeoutDialog() {
        _dialogState.update { it.copy(showBeaconTimeoutDialog = true) }
    }

    /**
     * Dismisses the beacon timeout dialog.
     */
    fun dismissBeaconTimeoutDialog() {
        _dialogState.update { it.copy(showBeaconTimeoutDialog = false) }
    }

    /**
     * Shows the BLE scan interval dialog.
     */
    fun showBleScanIntervalDialog() {
        _dialogState.update { it.copy(showBleScanIntervalDialog = true) }
    }

    /**
     * Dismisses the BLE scan interval dialog.
     */
    fun dismissBleScanIntervalDialog() {
        _dialogState.update { it.copy(showBleScanIntervalDialog = false) }
    }

    /**
     * Shows the weekly target hours dialog.
     */
    fun showWeeklyTargetHoursDialog() {
        _dialogState.update { it.copy(showWeeklyTargetHoursDialog = true) }
    }

    /**
     * Dismisses the weekly target hours dialog.
     */
    fun dismissWeeklyTargetHoursDialog() {
        _dialogState.update { it.copy(showWeeklyTargetHoursDialog = false) }
    }

    /**
     * Shows the beacon UUID dialog.
     */
    fun showBeaconUuidDialog() {
        _dialogState.update { it.copy(showBeaconUuidDialog = true) }
    }

    /**
     * Dismisses the beacon UUID dialog.
     */
    fun dismissBeaconUuidDialog() {
        _dialogState.update { it.copy(showBeaconUuidDialog = false) }
    }
}

/**
 * UI state for the settings screen.
 */
data class SettingsUiState(
    val commuteDays: Set<DayOfWeek> = emptySet(),
    val outboundWindow: TimeWindow = TimeWindow.DEFAULT_OUTBOUND,
    val returnWindow: TimeWindow = TimeWindow.DEFAULT_RETURN,
    val beaconUuid: String? = null,
    val beaconTimeout: Int = 10,
    val bleScanInterval: Long = 60000L,
    val workTimeWindow: TimeWindow = TimeWindow.DEFAULT_WORK_TIME,
    val weeklyTargetHours: Float = 40f,
    val zoneCount: Int = 0,
    val showResetConfirmation: Boolean = false,
    val showCommuteDaysDialog: Boolean = false,
    val showOutboundWindowDialog: Boolean = false,
    val showReturnWindowDialog: Boolean = false,
    val showWorkTimeWindowDialog: Boolean = false,
    val showBeaconTimeoutDialog: Boolean = false,
    val showBleScanIntervalDialog: Boolean = false,
    val showWeeklyTargetHoursDialog: Boolean = false,
    val showBeaconUuidDialog: Boolean = false
)

/**
 * Internal state for dialogs.
 */
private data class DialogState(
    val showResetConfirmation: Boolean = false,
    val showCommuteDaysDialog: Boolean = false,
    val showOutboundWindowDialog: Boolean = false,
    val showReturnWindowDialog: Boolean = false,
    val showWorkTimeWindowDialog: Boolean = false,
    val showBeaconTimeoutDialog: Boolean = false,
    val showBleScanIntervalDialog: Boolean = false,
    val showWeeklyTargetHoursDialog: Boolean = false,
    val showBeaconUuidDialog: Boolean = false
)

/**
 * Helper class to combine first set of settings.
 */
private data class PartialSettings1(
    val commuteDays: Set<DayOfWeek>,
    val outboundWindow: TimeWindow,
    val returnWindow: TimeWindow,
    val beaconUuid: String?
)

/**
 * Helper class to combine second set of settings.
 */
private data class PartialSettings2(
    val beaconTimeout: Int,
    val bleScanInterval: Long,
    val workTimeWindow: TimeWindow,
    val weeklyTargetHours: Float
)
