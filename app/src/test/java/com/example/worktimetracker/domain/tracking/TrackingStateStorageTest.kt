package com.example.worktimetracker.domain.tracking

import android.content.SharedPreferences
import com.example.worktimetracker.data.local.entity.TrackingType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TrackingStateStorageTest {

    private lateinit var preferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var storage: TrackingStateStorage

    @BeforeEach
    fun setup() {
        preferences = mockk()
        editor = mockk(relaxed = true)

        every { preferences.edit() } returns editor
        every { editor.apply() } just Runs

        storage = TrackingStateStorage(preferences)
    }

    @Test
    fun `saveState saves IDLE state`() {
        // Arrange
        val state = TrackingState.Idle
        val putStringSlots = mutableListOf<Pair<String, String>>()
        val removeSlots = mutableListOf<String>()

        every { editor.putString(capture(slot()), capture(slot())) } answers {
            putStringSlots.add(firstArg<String>() to secondArg<String>())
            editor
        }
        every { editor.remove(capture(slot())) } answers {
            removeSlots.add(firstArg())
            editor
        }

        // Act
        storage.saveState(state)

        // Assert
        assertTrue(putStringSlots.any { it.first == "tracking_state_type" && it.second == "IDLE" })
        assertTrue(removeSlots.contains("tracking_entry_id"))
        assertTrue(removeSlots.contains("tracking_pause_id"))
        assertTrue(removeSlots.contains("tracking_type"))
        assertTrue(removeSlots.contains("tracking_start_time"))
        verify { editor.apply() }
    }

    @Test
    fun `saveState saves TRACKING state`() {
        // Arrange
        val startTime = LocalDateTime.of(2026, 2, 9, 8, 0)
        val state = TrackingState.Tracking(
            entryId = "entry-1",
            type = TrackingType.COMMUTE_OFFICE,
            startTime = startTime
        )

        val savedData = mutableMapOf<String, String>()
        val removeSlots = mutableListOf<String>()

        every { editor.putString(capture(slot()), capture(slot())) } answers {
            savedData[firstArg()] = secondArg()
            editor
        }
        every { editor.remove(capture(slot())) } answers {
            removeSlots.add(firstArg())
            editor
        }

        // Act
        storage.saveState(state)

        // Assert
        assertEquals("TRACKING", savedData["tracking_state_type"])
        assertEquals("entry-1", savedData["tracking_entry_id"])
        assertEquals("COMMUTE_OFFICE", savedData["tracking_type"])
        assertEquals(startTime.toString(), savedData["tracking_start_time"])
        assertTrue(removeSlots.contains("tracking_pause_id"))
        verify { editor.apply() }
    }

    @Test
    fun `saveState saves PAUSED state`() {
        // Arrange
        val state = TrackingState.Paused(
            entryId = "entry-1",
            type = TrackingType.MANUAL,
            pauseId = "pause-1"
        )

        val savedData = mutableMapOf<String, String>()
        val removeSlots = mutableListOf<String>()

        every { editor.putString(capture(slot()), capture(slot())) } answers {
            savedData[firstArg()] = secondArg()
            editor
        }
        every { editor.remove(capture(slot())) } answers {
            removeSlots.add(firstArg())
            editor
        }

        // Act
        storage.saveState(state)

        // Assert
        assertEquals("PAUSED", savedData["tracking_state_type"])
        assertEquals("entry-1", savedData["tracking_entry_id"])
        assertEquals("pause-1", savedData["tracking_pause_id"])
        assertEquals("MANUAL", savedData["tracking_type"])
        assertTrue(removeSlots.contains("tracking_start_time"))
        verify { editor.apply() }
    }

    @Test
    fun `loadState returns IDLE when no state saved`() {
        // Arrange
        every { preferences.getString("tracking_state_type", "IDLE") } returns "IDLE"

        // Act
        val state = storage.loadState()

        // Assert
        assertTrue(state is TrackingState.Idle)
    }

    @Test
    fun `loadState restores TRACKING state`() {
        // Arrange
        val startTime = LocalDateTime.of(2026, 2, 9, 8, 0)

        every { preferences.getString("tracking_state_type", "IDLE") } returns "TRACKING"
        every { preferences.getString("tracking_entry_id", null) } returns "entry-1"
        every { preferences.getString("tracking_type", null) } returns "HOME_OFFICE"
        every { preferences.getString("tracking_start_time", null) } returns startTime.toString()

        // Act
        val state = storage.loadState()

        // Assert
        assertTrue(state is TrackingState.Tracking)
        assertEquals("entry-1", (state as TrackingState.Tracking).entryId)
        assertEquals(TrackingType.HOME_OFFICE, state.type)
        assertEquals(startTime, state.startTime)
    }

    @Test
    fun `loadState restores PAUSED state`() {
        // Arrange
        every { preferences.getString("tracking_state_type", "IDLE") } returns "PAUSED"
        every { preferences.getString("tracking_entry_id", null) } returns "entry-1"
        every { preferences.getString("tracking_pause_id", null) } returns "pause-1"
        every { preferences.getString("tracking_type", null) } returns "MANUAL"

        // Act
        val state = storage.loadState()

        // Assert
        assertTrue(state is TrackingState.Paused)
        assertEquals("entry-1", (state as TrackingState.Paused).entryId)
        assertEquals("pause-1", state.pauseId)
        assertEquals(TrackingType.MANUAL, state.type)
    }

    @Test
    fun `loadState returns IDLE on corrupted TRACKING state`() {
        // Arrange: Missing required field
        every { preferences.getString("tracking_state_type", "IDLE") } returns "TRACKING"
        every { preferences.getString("tracking_entry_id", null) } returns null
        every { preferences.getString("tracking_type", null) } returns null
        every { preferences.getString("tracking_start_time", null) } returns null

        // Act
        val state = storage.loadState()

        // Assert
        assertTrue(state is TrackingState.Idle)
    }

    @Test
    fun `loadState returns IDLE on corrupted PAUSED state`() {
        // Arrange: Missing required field
        every { preferences.getString("tracking_state_type", "IDLE") } returns "PAUSED"
        every { preferences.getString("tracking_entry_id", null) } returns "entry-1"
        every { preferences.getString("tracking_pause_id", null) } returns null
        every { preferences.getString("tracking_type", null) } returns null

        // Act
        val state = storage.loadState()

        // Assert
        assertTrue(state is TrackingState.Idle)
    }

    @Test
    fun `loadState returns IDLE on invalid enum value`() {
        // Arrange
        every { preferences.getString("tracking_state_type", "IDLE") } returns "TRACKING"
        every { preferences.getString("tracking_entry_id", null) } returns "entry-1"
        every { preferences.getString("tracking_type", null) } returns "INVALID_TYPE"
        every { preferences.getString("tracking_start_time", null) } returns "2026-02-09T08:00:00"

        // Act
        val state = storage.loadState()

        // Assert
        assertTrue(state is TrackingState.Idle)
    }

    @Test
    fun `loadState returns IDLE on invalid datetime`() {
        // Arrange
        every { preferences.getString("tracking_state_type", "IDLE") } returns "TRACKING"
        every { preferences.getString("tracking_entry_id", null) } returns "entry-1"
        every { preferences.getString("tracking_type", null) } returns "MANUAL"
        every { preferences.getString("tracking_start_time", null) } returns "invalid-datetime"

        // Act
        val state = storage.loadState()

        // Assert
        assertTrue(state is TrackingState.Idle)
    }

    @Test
    fun `clear removes all state data`() {
        // Arrange
        val removeSlots = mutableListOf<String>()

        every { editor.remove(capture(slot())) } answers {
            removeSlots.add(firstArg())
            editor
        }

        // Act
        storage.clear()

        // Assert
        assertTrue(removeSlots.contains("tracking_state_type"))
        assertTrue(removeSlots.contains("tracking_entry_id"))
        assertTrue(removeSlots.contains("tracking_pause_id"))
        assertTrue(removeSlots.contains("tracking_type"))
        assertTrue(removeSlots.contains("tracking_start_time"))
        verify { editor.apply() }
    }
}
