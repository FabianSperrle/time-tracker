package com.example.worktimetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.worktimetracker.ui.screens.DashboardScreen
import com.example.worktimetracker.ui.screens.EntriesScreen
import com.example.worktimetracker.ui.screens.MapScreen
import com.example.worktimetracker.ui.screens.SettingsScreen

/**
 * Main navigation host for the app.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }
        composable(Screen.Map.route) {
            MapScreen()
        }
        composable(Screen.Entries.route) {
            EntriesScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
