package com.example.worktimetracker.ui.viewmodel

import com.example.worktimetracker.data.local.entity.GeofenceZone
import com.example.worktimetracker.data.repository.GeofenceRepository
import com.example.worktimetracker.data.settings.SettingsProvider
import com.example.worktimetracker.domain.model.TimeWindow
import com.example.worktimetracker.service.BeaconScanner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Unit tests for SettingsViewModel.
 * Note: Full flow testing with Turbine would require more complex setup.
 * These tests verify the basic behavior and interactions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var settingsProvider: SettingsProvider
    private lateinit var geofenceRepository: GeofenceRepository
    private lateinit var beaconScanner: BeaconScanner
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsProvider = mockk(relaxed = true)
        geofenceRepository = mockk(relaxed = true)
        beaconScanner = mockk(relaxed = true)

        // Setup default flows
        every { settingsProvider.commuteDays } returns flowOf(emptySet())
        every { settingsProvider.outboundWindow } returns flowOf(TimeWindow.DEFAULT_OUTBOUND)
        every { settingsProvider.returnWindow } returns flowOf(TimeWindow.DEFAULT_RETURN)
        every { settingsProvider.beaconUuid } returns flowOf(null)
        every { settingsProvider.beaconTimeout } returns flowOf(10)
        every { settingsProvider.bleScanInterval } returns flowOf(60000L)
        every { settingsProvider.workTimeWindow } returns flowOf(TimeWindow.DEFAULT_WORK_TIME)
        every { settingsProvider.weeklyTargetHours } returns flowOf(40f)
        every { settingsProvider.beaconRssiThreshold } returns flowOf(null)
        every { geofenceRepository.getAllZones() } returns flowOf(emptyList<GeofenceZone>())
        every { beaconScanner.scanResults } returns kotlinx.coroutines.flow.MutableStateFlow(emptyList())

        viewModel = SettingsViewModel(settingsProvider, geofenceRepository, beaconScanner)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateCommuteDays calls settingsProvider`() = runTest {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        coEvery { settingsProvider.setCommuteDays(days) } returns Unit

        viewModel.updateCommuteDays(days)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsProvider.setCommuteDays(days) }
    }

    @Test
    fun `updateOutboundWindow calls settingsProvider`() = runTest {
        val window = TimeWindow(LocalTime.of(7, 0), LocalTime.of(10, 0))
        coEvery { settingsProvider.setOutboundWindow(window) } returns Unit

        viewModel.updateOutboundWindow(window)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsProvider.setOutboundWindow(window) }
    }

    @Test
    fun `updateBeaconUuid calls settingsProvider`() = runTest {
        val uuid = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
        coEvery { settingsProvider.setBeaconUuid(uuid) } returns Unit

        viewModel.updateBeaconUuid(uuid)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsProvider.setBeaconUuid(uuid) }
    }

    @Test
    fun `resetAllData calls settingsProvider`() = runTest {
        coEvery { settingsProvider.clearAllSettings() } returns Unit

        viewModel.resetAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsProvider.clearAllSettings() }
    }

    @Test
    fun `SettingsUiState has correct defaults`() {
        val state = SettingsUiState()

        assertEquals(emptySet<DayOfWeek>(), state.commuteDays)
        assertEquals(TimeWindow.DEFAULT_OUTBOUND, state.outboundWindow)
        assertEquals(TimeWindow.DEFAULT_RETURN, state.returnWindow)
        assertEquals(null, state.beaconUuid)
        assertEquals(10, state.beaconTimeout)
        assertEquals(60000L, state.bleScanInterval)
        assertEquals(TimeWindow.DEFAULT_WORK_TIME, state.workTimeWindow)
        assertEquals(40f, state.weeklyTargetHours)
        assertEquals(false, state.showResetConfirmation)
        assertEquals(false, state.showCommuteDaysDialog)
    }
}
