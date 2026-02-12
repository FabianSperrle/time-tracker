package com.example.worktimetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.worktimetracker.data.OnboardingPreferences
import com.example.worktimetracker.data.local.AppDatabase
import com.example.worktimetracker.data.repository.TestRepository
import com.example.worktimetracker.ui.navigation.AppNavHost
import com.example.worktimetracker.ui.navigation.BottomNavigationBar
import com.example.worktimetracker.ui.navigation.Screen
import com.example.worktimetracker.ui.theme.WorkTimeTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var testRepository: TestRepository

    @Inject
    lateinit var onboardingPreferences: OnboardingPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WorkTimeTrackerTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Determine start destination based on onboarding status
                val startDestination = remember {
                    if (onboardingPreferences.isOnboardingCompleted()) {
                        Screen.Dashboard.route
                    } else {
                        Screen.Onboarding.route
                    }
                }

                // Hide bottom bar on onboarding screen
                val showBottomBar = currentRoute != Screen.Onboarding.route

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavigationBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
