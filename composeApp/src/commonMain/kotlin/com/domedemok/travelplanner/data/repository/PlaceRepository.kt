package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.Place
import kotlinx.coroutines.flow.Flow

/**
 * Persisted places for a trip plus the search-side surface that wraps Foursquare.
 *
 * The search/geocode methods are bundled here so view models can depend on a single
 * abstraction (the underlying remote service is implementation detail). Per-user,
 * per-trip "last search location" memory lives in the local SQLDelight DB so the
 * input is restored when the user returns to the search screen.
 */
interface PlaceRepository {
    suspend fun addPlaceToTrip(tripId: String, place: Place): Result<Place>
    suspend fun getPlacesForTrip(tripId: String): Result<List<Place>>
    fun getPlacesForTripFlow(tripId: String): Flow<List<Place>>
    suspend fun updatePlace(tripId: String, place: Place): Result<Unit>
    suspend fun deletePlace(tripId: String, placeId: String): Result<Unit>

    /** Searches venues near (lat, lon) via Foursquare. See [com.domedemok.travelplanner.data.remote.FoursquareService]. */
    suspend fun searchPlaces(
        query: String,
        latitude: Double,
        longitude: Double,
        categoryId: String? = null,
        limit: Int = 20,
    ): Result<List<Place>>

    /** Resolves a free-text [address] to a (lat, lon) pair via Foursquare's geocoder. */
    suspend fun geocodeAddress(address: String): Result<Pair<Double, Double>>

    /** Last "search near…" input for this user + trip; `null` if none has been recorded. */
    suspend fun getLastSearchLocation(tripId: String): String?
    suspend fun saveSearchLocation(tripId: String, location: String)
}
