package com.domedemok.travelplanner.data.model

import kotlinx.serialization.Serializable

/**
 * A single calendar day in a trip's itinerary.
 *
 * Stored in Firestore under `trips/{tripId}/itinerary/day_{dayNumber}`.
 * [placeIds] is an ordered list — the first element is the first stop of the day.
 */
@Serializable
data class ItineraryDay(
    val id: String = "",                           // Firestore doc ID ("day_1", "day_2", …)
    val tripId: String = "",
    val dayNumber: Int = 0,                        // 1-indexed
    val date: Long = 0L,                           // epoch-ms of midnight for that day
    val placeIds: List<String> = emptyList(),      // ordered place IDs assigned to this day
)
