package com.domedemok.travelplanner.data.model

import kotlinx.serialization.Serializable

/**
 * JS-only DTO for writing trips to Firestore.
 *
 * All numeric timestamps are encoded as [Double] because the GitLive Firebase
 * wrapper on Kotlin/JS routes values through `kotlinx.serialization`, and the
 * JS target cannot reliably round-trip 64-bit integers — any [Long] value
 * silently arrives as `0` on the receiving side.
 *
 * Reads do not need this DTO: the dynamic `doc.js.data()` path (see
 * `TripRepositoryImpl.parseDynamicTimestamp`) reads the raw JS value directly
 * and converts it back to [Long] without going through serialisation.
 */
@Serializable
data class TripDto(
    val name:         String,
    val description:  String,
    val destination:  String              = "",
    val createdBy:     String,
    val tripMembers:   Map<String, String>,
    val formerMembers: Map<String, String> = emptyMap(),
    val startDate:    Double,
    val endDate:      Double,
    val createdAt:    Double,
    val updatedAt:    Double,
    val lastOpenedAt: Double = 0.0,
    val joinCode:     String = "",
)
