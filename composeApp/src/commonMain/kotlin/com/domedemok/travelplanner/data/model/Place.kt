package com.domedemok.travelplanner.data.model

import kotlinx.serialization.Serializable

/**
 * A point of interest saved to a trip — either browsed from Foursquare or
 * added manually by the user.
 *
 * Foursquare-sourced places carry a non-empty [foursquareId] plus the
 * enrichment fields below. Manually-added places have [foursquareId] empty
 * and 0 / blank enrichment values.
 */
@Serializable
data class Place(
    val id:           String = "",
    val tripId:       String = "",
    val name:         String = "",
    val address:      String = "",
    val latitude:     Double = 0.0,
    val longitude:    Double = 0.0,
    val notes:        String = "",
    val category:     String = "",   // Foursquare category label, e.g. "Coffee Shop"
    val foursquareId: String = "",   // Foursquare venue ID (empty for manual entries)
    val addedAt:      Long   = 0L,
    val addedBy:      String = "",   // Firebase Auth UID

    // ── Enrichment fields (populated from Foursquare; defaults when missing) ──
    val rating:         Double = 0.0,   // 0.0 – 10.0
    val popularity:     Double = 0.0,   // 0.0 – 1.0
    val photoUrl:       String = "",    // pre-assembled URL: `prefix + size + suffix`

    /**
     * Distance from the search origin in metres — **ephemeral, not persisted**.
     * Only meaningful while the place is a live search result; once saved to
     * Firestore it defaults to 0. Do not write this field to the database.
     */
    val distanceMeters: Int    = 0,
)
