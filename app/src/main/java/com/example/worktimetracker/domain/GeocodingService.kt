package com.example.worktimetracker.domain

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class SearchResult(
    val name: String,
    val address: String,
    val latLng: LatLng
)

interface GeocodingService {
    suspend fun searchAddress(query: String): Result<List<SearchResult>>
}

@Singleton
class GeocodingServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GeocodingService {

    override suspend fun searchAddress(query: String): Result<List<SearchResult>> {
        if (query.isBlank()) {
            return Result.success(emptyList())
        }

        if (!Geocoder.isPresent()) {
            return Result.failure(Exception("Geocoding service not available"))
        }

        return try {
            val geocoder = Geocoder(context)
            val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ async API
                getAddressesAsync(geocoder, query)
            } else {
                // Older sync API (deprecated but still needed for API < 33)
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 5) ?: emptyList()
            }

            Result.success(results.mapNotNull { address ->
                val lat = address.latitude
                val lon = address.longitude
                if (lat != 0.0 && lon != 0.0) {
                    SearchResult(
                        name = address.featureName ?: address.thoroughfare ?: query,
                        address = formatAddress(address),
                        latLng = LatLng(lat, lon)
                    )
                } else {
                    null
                }
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getAddressesAsync(geocoder: Geocoder, query: String): List<Address> =
        suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(query, 5) { addresses ->
                    continuation.resume(addresses)
                }
            } else {
                @Suppress("DEPRECATION")
                continuation.resume(geocoder.getFromLocationName(query, 5) ?: emptyList())
            }
        }

    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()

        address.thoroughfare?.let { parts.add(it) }
        address.locality?.let { parts.add(it) }
        address.adminArea?.let { parts.add(it) }
        address.countryName?.let { parts.add(it) }

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            address.getAddressLine(0) ?: ""
        }
    }
}
