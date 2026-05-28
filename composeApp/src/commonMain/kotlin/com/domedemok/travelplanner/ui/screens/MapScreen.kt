package com.domedemok.travelplanner.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.domedemok.travelplanner.data.model.ItineraryDay
import com.domedemok.travelplanner.data.model.Place

/**
 * Platform-specific map screen
 * - Android: Google Maps with day-filter, colored markers, and member location tracking
 * - Web: Leaflet + OpenStreetMap (day-filter UI only, no member locations)
 */
@Composable
expect fun MapScreen(
    places: List<Place>,
    itineraryDays: List<ItineraryDay> = emptyList(),
    tripId: String = "",
    currentUserId: String? = null,
    tripMembers: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier,
)
