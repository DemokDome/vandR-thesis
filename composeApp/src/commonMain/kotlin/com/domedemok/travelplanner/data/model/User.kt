package com.domedemok.travelplanner.data.model

import kotlinx.serialization.Serializable

/** A signed-in app user. Stored in Firestore under `users/{uid}`. */
@Serializable
data class User(
    val id:               String       = "",   // Firebase Auth UID
    val email:            String       = "",
    val displayName:      String       = "",
    val createdAt:        Long         = 0L,
    val favoriteTripIds:  List<String> = emptyList(),
)
