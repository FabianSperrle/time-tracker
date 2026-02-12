package com.example.worktimetracker.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks the status of all required runtime permissions.
 */
@Singleton
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Check all required permissions and return their status.
     */
    fun checkAllPermissions(): PermissionStatus {
        return PermissionStatus(
            location = isLocationGranted(),
            backgroundLocation = isBackgroundLocationGranted(),
            bluetooth = isBluetoothGranted(),
            notification = isNotificationGranted(),
            batteryOptimization = isBatteryOptimizationDisabled()
        )
    }

    /**
     * Check if fine location permission is granted.
     */
    fun isLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if background location permission is granted.
     */
    fun isBackgroundLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if Bluetooth permissions are granted (API 31+).
     * On Android 12+, both BLUETOOTH_SCAN and BLUETOOTH_CONNECT are required.
     */
    fun isBluetoothGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Below API 31, Bluetooth permissions are not runtime permissions
            true
        }
    }

    /**
     * Check if notification permission is granted (API 33+).
     * Below API 33, this permission is not required.
     */
    fun isNotificationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Below API 33, no notification permission required
            true
        }
    }

    /**
     * Check if battery optimization is disabled for this app.
     */
    fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }
}

/**
 * Data class representing the status of all required permissions.
 */
data class PermissionStatus(
    val location: Boolean,
    val backgroundLocation: Boolean,
    val bluetooth: Boolean,
    val notification: Boolean,
    val batteryOptimization: Boolean
) {
    /**
     * Returns true if all critical permissions are granted.
     */
    val allGranted: Boolean
        get() = location && backgroundLocation && bluetooth && notification && batteryOptimization
}
