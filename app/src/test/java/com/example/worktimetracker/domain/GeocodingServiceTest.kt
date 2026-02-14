package com.example.worktimetracker.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GeocodingServiceTest {

    @Test
    fun `SearchResult holds expected data`() {
        // Given
        val name = "München Hauptbahnhof"
        val address = "Bayerstraße, München, Germany"
        val latLng = com.google.android.gms.maps.model.LatLng(48.1405, 11.5584)

        // When
        val result = SearchResult(
            name = name,
            address = address,
            latLng = latLng
        )

        // Then
        assertEquals(name, result.name)
        assertEquals(address, result.address)
        assertEquals(latLng, result.latLng)
    }

    @Test
    fun `SearchResult with empty name is valid`() {
        // Given/When
        val result = SearchResult(
            name = "",
            address = "Test Address",
            latLng = com.google.android.gms.maps.model.LatLng(0.0, 0.0)
        )

        // Then
        assertNotNull(result)
        assertEquals("", result.name)
    }
}
