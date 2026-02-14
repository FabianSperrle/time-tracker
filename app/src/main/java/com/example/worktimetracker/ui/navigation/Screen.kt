package com.example.worktimetracker.ui.navigation

/**
 * Sealed class representing all navigation destinations in the app.
 */
sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object Week : Screen("week")
    data object Map : Screen("map")
    data object Entries : Screen("entries")
    data object Settings : Screen("settings")
    data object DayView : Screen("day/{date}") {
        fun createRoute(date: String): String = "day/$date"
    }
}
