package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.TripPhoto
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    /** Real-time stream of photos for [tripId], ordered by upload time. */
    fun getPhotosFlow(tripId: String): Flow<List<TripPhoto>>

    /**
     * Compress, upload to Firebase Storage, save metadata to Firestore.
     * Returns the saved [TripPhoto] on success.
     */
    suspend fun uploadPhoto(
        tripId:     String,
        imageBytes: ByteArray,
        caption:    String = "",
    ): Result<TripPhoto>

    /** Delete the photo from Storage and its Firestore metadata document. */
    suspend fun deletePhoto(
        tripId:     String,
        photoId:    String,
        storageUrl: String,
    ): Result<Unit>
}
