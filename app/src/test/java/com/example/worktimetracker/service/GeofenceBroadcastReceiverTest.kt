package com.example.worktimetracker.service

import com.example.worktimetracker.data.local.entity.ZoneType
import com.example.worktimetracker.domain.tracking.TrackingEvent
import com.google.android.gms.location.Geofence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for GeofenceBroadcastReceiver.
 *
 * Note on test coverage: These tests cover the static [GeofenceBroadcastReceiver.mapTransitionToEvent]
 * helper method which contains the core event-mapping logic. Full unit tests for [onReceive] are not
 * feasible because [GeofencingEvent.fromIntent] is a static factory that cannot be mocked in plain
 * unit tests. The onReceive() flow (zone lookup via DAO, goAsync() lifecycle, state machine
 * forwarding, and exception handling) should be validated via instrumented tests on a real device
 * or emulator.
 */
class GeofenceBroadcastReceiverTest {

    @Test
    fun `mapTransitionToEvent maps ENTER with HOME_STATION to GeofenceEntered`() {
        // Act
        val event = GeofenceBroadcastReceiver.mapTransitionToEvent(
            transition = Geofence.GEOFENCE_TRANSITION_ENTER,
            zoneType = ZoneType.HOME_STATION
        )

        // Assert
        assertTrue(event is TrackingEvent.GeofenceEntered)
        assertEquals(ZoneType.HOME_STATION, (event as TrackingEvent.GeofenceEntered).zoneType)
    }

    @Test
    fun `mapTransitionToEvent maps ENTER with OFFICE to GeofenceEntered`() {
        // Act
        val event = GeofenceBroadcastReceiver.mapTransitionToEvent(
            transition = Geofence.GEOFENCE_TRANSITION_ENTER,
            zoneType = ZoneType.OFFICE
        )

        // Assert
        assertTrue(event is TrackingEvent.GeofenceEntered)
        assertEquals(ZoneType.OFFICE, (event as TrackingEvent.GeofenceEntered).zoneType)
    }

    @Test
    fun `mapTransitionToEvent maps EXIT with OFFICE to GeofenceExited`() {
        // Act
        val event = GeofenceBroadcastReceiver.mapTransitionToEvent(
            transition = Geofence.GEOFENCE_TRANSITION_EXIT,
            zoneType = ZoneType.OFFICE
        )

        // Assert
        assertTrue(event is TrackingEvent.GeofenceExited)
        assertEquals(ZoneType.OFFICE, (event as TrackingEvent.GeofenceExited).zoneType)
    }

    @Test
    fun `mapTransitionToEvent maps EXIT with HOME_STATION to GeofenceExited`() {
        // Act
        val event = GeofenceBroadcastReceiver.mapTransitionToEvent(
            transition = Geofence.GEOFENCE_TRANSITION_EXIT,
            zoneType = ZoneType.HOME_STATION
        )

        // Assert
        assertTrue(event is TrackingEvent.GeofenceExited)
        assertEquals(ZoneType.HOME_STATION, (event as TrackingEvent.GeofenceExited).zoneType)
    }

    @Test
    fun `mapTransitionToEvent maps ENTER with OFFICE_STATION to GeofenceEntered`() {
        // Act
        val event = GeofenceBroadcastReceiver.mapTransitionToEvent(
            transition = Geofence.GEOFENCE_TRANSITION_ENTER,
            zoneType = ZoneType.OFFICE_STATION
        )

        // Assert
        assertTrue(event is TrackingEvent.GeofenceEntered)
        assertEquals(ZoneType.OFFICE_STATION, (event as TrackingEvent.GeofenceEntered).zoneType)
    }

    @Test
    fun `mapTransitionToEvent maps EXIT with OFFICE_STATION to GeofenceExited`() {
        // Act
        val event = GeofenceBroadcastReceiver.mapTransitionToEvent(
            transition = Geofence.GEOFENCE_TRANSITION_EXIT,
            zoneType = ZoneType.OFFICE_STATION
        )

        // Assert
        assertTrue(event is TrackingEvent.GeofenceExited)
        assertEquals(ZoneType.OFFICE_STATION, (event as TrackingEvent.GeofenceExited).zoneType)
    }

    @Test
    fun `mapTransitionToEvent returns null for DWELL transition`() {
        // Act
        val event = GeofenceBroadcastReceiver.mapTransitionToEvent(
            transition = Geofence.GEOFENCE_TRANSITION_DWELL,
            zoneType = ZoneType.OFFICE
        )

        // Assert
        assertNull(event)
    }

    @Test
    fun `ACTION_GEOFENCE_EVENT constant is set correctly`() {
        assertEquals(
            "com.example.worktimetracker.action.GEOFENCE_EVENT",
            GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT
        )
    }
}
