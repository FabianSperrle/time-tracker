package com.example.worktimetracker.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.worktimetracker.ui.viewmodel.OnboardingStep
import com.example.worktimetracker.ui.viewmodel.OnboardingViewModel

/**
 * Main onboarding screen that shows the current step.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()
    val permissionStatus by viewModel.permissionStatus.collectAsState()

    LaunchedEffect(isCompleted) {
        if (isCompleted) {
            onComplete()
        }
    }

    when (currentStep) {
        OnboardingStep.WELCOME -> WelcomeStep(
            onNext = { viewModel.nextStep() },
            onSkip = { viewModel.skipOnboarding() }
        )

        OnboardingStep.LOCATION -> LocationPermissionStep(
            permissionStatus = permissionStatus,
            onNext = { viewModel.nextStep() },
            onBack = { viewModel.previousStep() },
            onPermissionResult = { viewModel.refreshPermissionStatus() }
        )

        OnboardingStep.BLUETOOTH -> BluetoothPermissionStep(
            permissionStatus = permissionStatus,
            onNext = { viewModel.nextStep() },
            onBack = { viewModel.previousStep() },
            onPermissionResult = { viewModel.refreshPermissionStatus() }
        )

        OnboardingStep.BATTERY -> BatteryOptimizationStep(
            permissionStatus = permissionStatus,
            onNext = { viewModel.nextStep() },
            onBack = { viewModel.previousStep() },
            onPermissionResult = { viewModel.refreshPermissionStatus() }
        )

        OnboardingStep.NOTIFICATION -> NotificationPermissionStep(
            permissionStatus = permissionStatus,
            onNext = { viewModel.nextStep() },
            onBack = { viewModel.previousStep() },
            onPermissionResult = { viewModel.refreshPermissionStatus() }
        )
    }
}

@Composable
private fun WelcomeStep(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    OnboardingStepLayout(
        title = "Welcome to Work Time Tracker",
        description = "This app automatically tracks your work time using location and Bluetooth beacons.\n\n" +
                "To function properly, the app needs several permissions. Let's set them up together.\n\n" +
                "If you previously used this app on another device, your data will be restored automatically from Google Backup.",
        onNext = onNext,
        onBack = null,
        nextButtonText = "Get Started",
        showSkip = true,
        onSkip = onSkip
    )
}

@Composable
private fun LocationPermissionStep(
    permissionStatus: com.example.worktimetracker.domain.PermissionStatus,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onPermissionResult: () -> Unit
) {
    val context = LocalContext.current

    val fineLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onPermissionResult()
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onPermissionResult()
    }

    OnboardingStepLayout(
        title = "Location Permission",
        description = "Location is used to detect when you arrive at the office or train station.\n\n" +
                "The app needs:\n" +
                "• Fine Location - to detect your position\n" +
                "• Background Location - to track when you're not using the app",
        onNext = onNext,
        onBack = onBack,
        nextButtonText = if (permissionStatus.location && permissionStatus.backgroundLocation) "Continue" else "Next",
        additionalContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!permissionStatus.location) {
                    Button(
                        onClick = {
                            fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Fine Location")
                    }
                }

                if (permissionStatus.location && !permissionStatus.backgroundLocation) {
                    Button(
                        onClick = {
                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Background Location")
                    }
                }

                if (permissionStatus.location && permissionStatus.backgroundLocation) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✓ Location permissions granted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun BluetoothPermissionStep(
    permissionStatus: com.example.worktimetracker.domain.PermissionStatus,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onPermissionResult: () -> Unit
) {
    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onPermissionResult()
    }

    OnboardingStepLayout(
        title = "Bluetooth Permission",
        description = "Bluetooth is used to detect your presence at your desk using a BLE beacon.\n\n" +
                "This allows the app to distinguish between home office and being present in the office.",
        onNext = onNext,
        onBack = onBack,
        nextButtonText = if (permissionStatus.bluetooth) "Continue" else "Next",
        additionalContent = {
            if (!permissionStatus.bluetooth && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Button(
                    onClick = {
                        bluetoothLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Bluetooth Permissions")
                }
            } else if (permissionStatus.bluetooth) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ Bluetooth permissions granted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun BatteryOptimizationStep(
    permissionStatus: com.example.worktimetracker.domain.PermissionStatus,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onPermissionResult: () -> Unit
) {
    val context = LocalContext.current

    OnboardingStepLayout(
        title = "Battery Optimization",
        description = "To ensure reliable background tracking, the app needs to be excluded from battery optimization.\n\n" +
                "Some manufacturers (Samsung, Xiaomi, Huawei) have additional battery saving features that need to be disabled.\n\n" +
                "For more information, visit dontkillmyapp.com",
        onNext = onNext,
        onBack = onBack,
        nextButtonText = if (permissionStatus.batteryOptimization) "Continue" else "Next",
        additionalContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!permissionStatus.batteryOptimization) {
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                            onPermissionResult()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Battery Settings")
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✓ Battery optimization disabled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://dontkillmyapp.com/${Build.MANUFACTURER.lowercase()}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Learn more about ${Build.MANUFACTURER} settings")
                }
            }
        }
    )
}

@Composable
private fun NotificationPermissionStep(
    permissionStatus: com.example.worktimetracker.domain.PermissionStatus,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onPermissionResult: () -> Unit
) {
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onPermissionResult()
    }

    OnboardingStepLayout(
        title = "Notification Permission",
        description = "Notifications allow the app to inform you about tracked time entries and running timers.\n\n" +
                "This is required on Android 13 and above.",
        onNext = onNext,
        onBack = onBack,
        nextButtonText = "Complete Setup",
        additionalContent = {
            if (!permissionStatus.notification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Button(
                    onClick = {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Notification Permission")
                }
            } else if (permissionStatus.notification || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ Notification permissions granted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun OnboardingStepLayout(
    title: String,
    description: String,
    onNext: () -> Unit,
    onBack: (() -> Unit)?,
    nextButtonText: String = "Next",
    showSkip: Boolean = false,
    onSkip: (() -> Unit)? = null,
    additionalContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            if (showSkip && onSkip != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            additionalContent?.invoke()
        }

        Column {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(nextButtonText)
            }

            if (onBack != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }
}
