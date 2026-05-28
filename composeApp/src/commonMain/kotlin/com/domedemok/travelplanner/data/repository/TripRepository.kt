package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.Trip
import kotlinx.coroutines.flow.Flow



/**
 * Trip CRUD + membership + share/join operations.
 *
 * Membership is the single field [Trip.tripMembers] (`userId → displayName`):
 * Firestore security rules check `request.auth.uid in tripMembers.keys()`, and
 * the UI reads display names directly from the map without an extra
 * `users/{uid}` round-trip.
 */
interface TripRepository {
    /** UID of the signed-in Firebase user, or `null` if not signed in. */
    val currentUserId: String?

    suspend fun createTrip(trip: Trip): Result<Trip>

    /** One-shot: trips the user owns + trips shared with them, deduplicated. */
    suspend fun getTrips(): Result<List<Trip>>

    /** Real-time merge of the owned-trips and shared-with-me queries. */
    fun getTripsFlow(): Flow<List<Trip>>

    suspend fun getTripById(tripId: String): Result<Trip>
    suspend fun updateTrip(trip: Trip): Result<Unit>

    /** Deletes the trip document, its chat history, and the local cache row. */
    suspend fun deleteTrip(tripId: String): Result<Unit>

    /** Removes the current user from [Trip.tripMembers]. */
    suspend fun leaveTrip(tripId: String): Result<Unit>

    suspend fun toggleFavorite(tripId: String, isFavorite: Boolean): Result<Unit>

    /** Looks up a user by [userEmail] and adds them to the trip. */
    suspend fun shareTrip(tripId: String, userEmail: String): Result<Unit>

    suspend fun getFavoriteTripIds(): Result<Set<String>>

    /** Owner-only: removes a member from [Trip.tripMembers]. */
    suspend fun removeMemberFromTrip(tripId: String, userIdToRemove: String): Result<Unit>

    suspend fun renameMemberInTrip(tripId: String, userId: String, newName: String): Result<Unit>

    /** Stamps [tripId] with the current time as `lastOpenedAt` — cheap single-field write. */
    suspend fun updateLastOpened(tripId: String)

    suspend fun syncTripsToLocal()
    suspend fun getLocalTrips(): List<Trip>

    /** Looks up a trip by its [code] and adds the current user as a member. */
    suspend fun joinTripByCode(code: String): JoinTripResult

    /**
     * Returns the existing join code for [tripId], generating and persisting one
     * if the document does not have one yet (e.g. trips created before this feature).
     */
    suspend fun ensureJoinCode(tripId: String): Result<String>
}
