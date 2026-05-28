package com.domedemok.travelplanner.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TripPhoto(
    val id:           String = "",
    val tripId:       String = "",
    val uploadedBy:   String = "",
    val uploaderName: String = "",
    val storageUrl:   String = "",
    val uploadedAt:   Long   = 0L,
    val caption:      String = "",
)
