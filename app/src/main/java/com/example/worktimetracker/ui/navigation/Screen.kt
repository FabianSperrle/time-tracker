package com.example.worktimetracker.ui.navigation

/**
 * Sealed class representing all navigation destinations in the app.
 */
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Map : Screen("map")
    data object Entries : Screen("entries")
    data object Settings : Screen("settings")
}
