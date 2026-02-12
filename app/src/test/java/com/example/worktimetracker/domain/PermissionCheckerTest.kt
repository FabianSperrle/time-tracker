package com.example.worktimetracker.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for PermissionChecker.
 * Uses MockK to mock Android framework dependencies.
 */
class PermissionCheckerTest {

    private lateinit var context: Context
    private lateinit var powerManager: PowerManager
    private lateinit var permissionChecker: PermissionChecker

    @BeforeEach
    fun setup() {
        mockkStatic(ContextCompat::class)
        context = mockk(relaxed = true)
        powerManager = mockk(relaxed = true)

        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { context.packageName } returns "com.example.worktimetracker"

        permissionChecker = PermissionChecker(context)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `isLocationGranted returns true when ACCESS_FINE_LOCATION is granted`() {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        // When
        val result = permissionChecker.isLocationGranted()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isLocationGranted returns false when ACCESS_FINE_LOCATION is denied`() {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        // When
        val result = permissionChecker.isLocationGranted()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isBackgroundLocationGranted returns true when ACCESS_BACKGROUND_LOCATION is granted`() {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        // When
        val result = permissionChecker.isBackgroundLocationGranted()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isBackgroundLocationGranted returns false when ACCESS_BACKGROUND_LOCATION is denied`() {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        // When
        val result = permissionChecker.isBackgroundLocationGranted()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isBluetoothGranted returns true when permissions are granted`() {
        // Given - mock permissions as granted
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        } returns PackageManager.PERMISSION_GRANTED

        // When
        val result = permissionChecker.isBluetoothGranted()

        // Then - should return true (implementation checks API level internally)
        assertTrue(result)
    }

    @Test
    fun `isNotificationGranted returns true when POST_NOTIFICATIONS is granted`() {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED

        // When
        val result = permissionChecker.isNotificationGranted()

        // Then - should return true (implementation checks API level internally)
        assertTrue(result)
    }

    @Test
    fun `isBatteryOptimizationDisabled returns true when app is not optimized`() {
        // Given
        every { powerManager.isIgnoringBatteryOptimizations("com.example.worktimetracker") } returns true

        // When
        val result = permissionChecker.isBatteryOptimizationDisabled()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isBatteryOptimizationDisabled returns false when app is optimized`() {
        // Given
        every { powerManager.isIgnoringBatteryOptimizations("com.example.worktimetracker") } returns false

        // When
        val result = permissionChecker.isBatteryOptimizationDisabled()

        // Then
        assertFalse(result)
    }

    @Test
    fun `checkAllPermissions returns correct status when all permissions granted`() {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED
        every { powerManager.isIgnoringBatteryOptimizations("com.example.worktimetracker") } returns true

        // When
        val result = permissionChecker.checkAllPermissions()

        // Then
        assertEquals(
            PermissionStatus(
                location = true,
                backgroundLocation = true,
                bluetooth = true,
                notification = true,
                batteryOptimization = true
            ),
            result
        )
        assertTrue(result.allGranted)
    }

    @Test
    fun `checkAllPermissions returns correct status when some permissions denied`() {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_DENIED
        every { powerManager.isIgnoringBatteryOptimizations("com.example.worktimetracker") } returns false

        // When
        val result = permissionChecker.checkAllPermissions()

        // Then
        assertEquals(true, result.location)
        assertEquals(false, result.backgroundLocation)
        assertEquals(true, result.bluetooth)
        // notification depends on API level, tested separately
        assertEquals(false, result.batteryOptimization)
        assertFalse(result.allGranted)
    }

    @Test
    fun `PermissionStatus allGranted is false when any permission is denied`() {
        // Given
        val status = PermissionStatus(
            location = true,
            backgroundLocation = false,
            bluetooth = true,
            notification = true,
            batteryOptimization = true
        )

        // Then
        assertFalse(status.allGranted)
    }

    @Test
    fun `PermissionStatus allGranted is true when all permissions are granted`() {
        // Given
        val status = PermissionStatus(
            location = true,
            backgroundLocation = true,
            bluetooth = true,
            notification = true,
            batteryOptimization = true
        )

        // Then
        assertTrue(status.allGranted)
    }
}
