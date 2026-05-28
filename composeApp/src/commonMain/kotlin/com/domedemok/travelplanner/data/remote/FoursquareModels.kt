package com.domedemok.travelplanner.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FoursquareSearchResponse(
    val results: List<FoursquareVenue> = emptyList(),
    val context: FoursquareContext? = null
)

@Serializable
data class FoursquareVenue(
    @SerialName("fsq_place_id")
    val fsqPlaceId: String? = null,
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location: FoursquareLocation? = null,
    val categories: List<FoursquareCategory> = emptyList(),
    val description: String? = null,
    // ── New Foursquare v2024 fields (opt-in via `fields` query parameter) ──
    val rating: Double? = null,       // 0.0 – 10.0 venue rating
    val popularity: Double? = null,   // 0.0 – 1.0 relative popularity score
    val photos: List<FoursquarePhoto> = emptyList(),
    val distance: Int? = null,        // metres from search origin
)

/**
 * Photo envelope returned by the Places API.
 * A complete image URL is `${prefix}${size}${suffix}` — e.g. `${prefix}original${suffix}`.
 */
@Serializable
data class FoursquarePhoto(
    val id: String? = null,
    val prefix: String? = null,
    val suffix: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class FoursquareLocation(
    val address: String? = null,
    val locality: String? = null,
    val country: String? = null,
    @SerialName("formatted_address")
    val formattedAddress: String? = null
)

@Serializable
data class FoursquareCategory(
    val name: String? = null
)

@Serializable
data class FoursquareContext(
    @SerialName("geo_bounds")
    val geoBounds: FoursquareGeoBounds? = null
)

@Serializable
data class FoursquareGeoBounds(
    val circle: FoursquareCircle? = null
)

@Serializable
data class FoursquareCircle(
    val center: FoursquareCenter? = null
)

@Serializable
data class FoursquareCenter(
    val latitude: Double,
    val longitude: Double
)