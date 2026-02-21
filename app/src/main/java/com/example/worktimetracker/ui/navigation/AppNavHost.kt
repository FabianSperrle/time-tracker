package com.example.worktimetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.worktimetracker.ui.screens.DashboardScreen
import com.example.worktimetracker.ui.screens.EntriesScreen
import com.example.worktimetracker.ui.screens.ExportDialog
import com.example.worktimetracker.ui.screens.MapScreen
import com.example.worktimetracker.ui.screens.OnboardingScreen
import com.example.worktimetracker.ui.screens.SettingsScreen
import com.example.worktimetracker.ui.screens.WeekScreen
import java.time.LocalDate

/**
 * Main navigation host for the app.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Week.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Week.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }
        composable(Screen.Week.route) {
            var showExportDialog by remember { mutableStateOf(false) }

            WeekScreen(
                onDayClick = { date ->
                    navController.navigate(Screen.DayView.createRoute(date.toString()))
                },
                onExportClick = {
                    showExportDialog = true
                }
            )

            if (showExportDialog) {
                ExportDialog(
                    onDismiss = { showExportDialog = false }
                )
            }
        }
        composable(
            route = Screen.DayView.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val dateString = backStackEntry.arguments?.getString("date")
            val date = dateString?.let { LocalDate.parse(it) } ?: LocalDate.now()
            // For now, navigate to entries screen filtered by date
            // TODO: Implement dedicated DayViewScreen when needed
            LaunchedEffect(date) {
                navController.navigate(Screen.Entries.route) {
                    popUpTo(Screen.Week.route)
                }
            }
        }
        composable(Screen.Map.route) {
            MapScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Entries.route) {
            EntriesScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                }
            )
        }
    }
}
