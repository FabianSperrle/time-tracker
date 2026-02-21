package com.example.worktimetracker.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test for Screen navigation routes.
 */
class ScreenTest {

    @Test
    fun `dashboard route is correct`() {
        assertEquals("dashboard", Screen.Dashboard.route)
    }

    @Test
    fun `map route is correct`() {
        assertEquals("map", Screen.Map.route)
    }

    @Test
    fun `entries route is correct`() {
        assertEquals("entries", Screen.Entries.route)
    }

    @Test
    fun `settings route is correct`() {
        assertEquals("settings", Screen.Settings.route)
    }

    @Test
    fun `all navigation items are present`() {
        assertEquals(4, navigationItems.size)
        assertEquals(Screen.Week,      navigationItems[0].screen)
        assertEquals(Screen.Dashboard, navigationItems[1].screen)
        assertEquals(Screen.Entries,   navigationItems[2].screen)
        assertEquals(Screen.Settings,  navigationItems[3].screen)
    }
}
