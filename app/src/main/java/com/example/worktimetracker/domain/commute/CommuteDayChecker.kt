package com.example.worktimetracker.domain.commute

import com.example.worktimetracker.data.settings.SettingsProvider
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks whether the current day is a commute day and whether
 * the current time falls within outbound or return time windows.
 */
@Singleton
class CommuteDayChecker @Inject constructor(
    private val settingsProvider: SettingsProvider
) {

    /**
     * Checks if the given date is a configured commute day.
     */
    suspend fun isCommuteDay(date: LocalDate = LocalDate.now()): Boolean {
        val commuteDays = settingsProvider.commuteDays.first()
        return date.dayOfWeek in commuteDays
    }

    /**
     * Checks if the given time is within the outbound commute window.
     */
    suspend fun isInOutboundWindow(time: LocalTime = LocalTime.now()): Boolean {
        val window = settingsProvider.outboundWindow.first()
        return !time.isBefore(window.start) && !time.isAfter(window.end)
    }

    /**
     * Checks if the given time is within the return commute window.
     */
    suspend fun isInReturnWindow(time: LocalTime = LocalTime.now()): Boolean {
        val window = settingsProvider.returnWindow.first()
        return !time.isBefore(window.start) && !time.isAfter(window.end)
    }
}
