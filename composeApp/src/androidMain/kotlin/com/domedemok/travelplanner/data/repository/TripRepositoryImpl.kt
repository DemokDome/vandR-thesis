package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.local.database.TravelDatabase
import com.domedemok.travelplanner.data.model.Trip
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.TripBusinessRules
import com.domedemok.travelplanner.util.generateJoinCode
import com.domedemok.travelplanner.util.getCurrentTimestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TripRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: TravelDatabase,
) : TripRepository {

    private companion object {
        const val UNKNOWN_NAME = TripBusinessRules.UNKNOWN_DISPLAY_NAME
    }

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun trips(): CollectionReference = firestore.collection(FirestoreCollections.TRIPS)
    private fun tripDoc(tripId: String): DocumentReference = trips().document(tripId)
    private fun userDoc(uid: String): DocumentReference = firestore.collection(FirestoreCollections.USERS).document(uid)

    /**
     * Firestore membership query: a range filter on the nested map field
     * `tripMembers.{uid}` implicitly restricts results to documents where
     * that field exists — i.e. trips the user is a member of.
     *
     * Using `whereGreaterThanOrEqualTo` (same as the JS `greaterThanOrEqualTo ""`)
     * avoids the composite index requirement that `orderBy` on a dynamic nested
     * field would impose, which caused silent permission / index errors on Android.
     */
    private fun membershipQuery(userId: String) =
        trips().whereGreaterThanOrEqualTo("tripMembers.$userId", "")

    override suspend fun createTrip(trip: Trip): Result<Trip> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            val currentUserName = auth.currentUser?.displayName ?: UNKNOWN_NAME
            val joinCode = generateJoinCode()

            val tripData = mapOf(
                "name"          to trip.name,
                "description"   to trip.description,
                "destination"   to trip.destination,
                "createdBy"     to userId,
                "tripMembers"   to mapOf(userId to currentUserName),
                "formerMembers" to emptyMap<String, String>(),
                "joinCode"      to joinCode,
                "startDate"     to trip.startDate,
                "endDate"       to trip.endDate,
                "createdAt"     to trip.createdAt,
                "updatedAt"     to trip.updatedAt,
            )

            val docRef = trips().add(tripData).await()
            val createdTrip = trip.copy(
                id          = docRef.id,
                createdBy   = userId,
                tripMembers = mapOf(userId to currentUserName),
                joinCode    = joinCode,
            )

            saveTripToLocal(createdTrip)
            Result.success(createdTrip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrips(): Result<List<Trip>> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            val snapshot = membershipQuery(userId).get().await()
            val allTrips = snapshot.documents.mapNotNull { parseTripFromDoc(it) }

            allTrips.forEach { saveTripToLocal(it) }
            Result.success(allTrips)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getTripsFlow(): Flow<List<Trip>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            close(Exception("User not authenticated"))
            return@callbackFlow
        }

        val listener = membershipQuery(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Propagate so TripViewModel's .catch{} can surface the error in the UI
                // instead of leaving the loading spinner spinning forever.
                android.util.Log.e("TripRepository", "Trips snapshot error: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents?.mapNotNull { parseTripFromDoc(it) } ?: emptyList())
        }

        awaitClose { listener.remove() }
    }

    override suspend fun getTripById(tripId: String): Result<Trip> {
        return try {
            val doc = tripDoc(tripId).get().await()
            if (!doc.exists()) return Result.failure(Exception("Trip not found"))

            val trip = parseTripFromDoc(doc) ?: return Result.failure(Exception("Failed to parse trip"))
            saveTripToLocal(trip)
            Result.success(trip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTrip(trip: Trip): Result<Unit> {
        return try {
            currentUserId ?: return Result.failure(Exception("User not authenticated"))

            val updates = mapOf(
                "name"          to trip.name,
                "description"   to trip.description,
                "destination"   to trip.destination,
                "tripMembers"   to trip.tripMembers,
                "formerMembers" to trip.formerMembers,
                "startDate"     to trip.startDate,
                "endDate"       to trip.endDate,
                "updatedAt"     to getCurrentTimestamp(),
            )

            tripDoc(trip.id).update(updates).await()
            saveTripToLocal(trip.copy(updatedAt = getCurrentTimestamp()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            tripDoc(tripId).delete().await()
            runCatching { database.tripEntityQueries.deleteTrip(tripId) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveTrip(tripId: String): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            val snap          = tripDoc(tripId).get().await()
            val currentMembers = (snap.get("tripMembers") as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.mapValues { it.value.toString() }
                ?: emptyMap()

            // Pure rule: pick the name we want to preserve in formerMembers so
            // the leaver still resolves on historical expense splits.
            val preservedDisplayName = TripBusinessRules.resolveLeaverDisplayName(
                currentMembers   = currentMembers,
                leaverId         = userId,
                authDisplayName  = auth.currentUser?.displayName,
            )

            tripDoc(tripId).update(
                mapOf(
                    "tripMembers.$userId"   to FieldValue.delete(),
                    "formerMembers.$userId" to preservedDisplayName,
                )
            ).await()

            runCatching { database.tripEntityQueries.deleteTrip(tripId) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(tripId: String, isFavorite: Boolean): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            val op = if (isFavorite) FieldValue.arrayUnion(tripId) else FieldValue.arrayRemove(tripId)
            userDoc(userId).update("favoriteTripIds", op).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun shareTrip(tripId: String, userEmail: String): Result<Unit> {
        return try {
            val currentId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            val userQuery = firestore.collection(FirestoreCollections.USERS)
                .whereEqualTo("email", userEmail)
                .limit(1)
                .get()
                .await()

            if (userQuery.isEmpty) return Result.failure(Exception("User not found with email: $userEmail"))

            val targetUserDoc  = userQuery.documents.first()
            val targetUserId   = targetUserDoc.id
            val targetUserName = targetUserDoc.getString("displayName") ?: "Unknown User"

            val tripSnap        = tripDoc(tripId).get().await()
            val existingMembers = (tripSnap.get("tripMembers") as? Map<*, *>)
                ?.keys?.map { it.toString() }?.toSet()
                ?: emptySet()

            // Pure rule: rejects self-share and already-member; otherwise yields
            // the (id, name) pair to persist under `tripMembers`.
            val decision = TripBusinessRules.evaluateShareAttempt(
                sharerUserId      = currentId,
                targetUserId      = targetUserId,
                existingMemberIds = existingMembers,
                targetUserName    = targetUserName,
            )

            when (decision) {
                TripBusinessRules.ShareDecision.SelfShare ->
                    return Result.failure(Exception("You cannot share a trip with yourself."))
                TripBusinessRules.ShareDecision.AlreadyShared ->
                    return Result.failure(Exception("This trip is already shared with this user."))
                is TripBusinessRules.ShareDecision.Allowed -> {
                    tripDoc(tripId).update(
                        mapOf(
                            "tripMembers.${decision.targetUserId}" to decision.targetUserName,
                            "updatedAt"                             to getCurrentTimestamp(),
                        )
                    ).await()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFavoriteTripIds(): Result<Set<String>> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            val userDoc = userDoc(userId).get().await()
            Result.success(parseStringList(userDoc.get("favoriteTripIds")).toSet())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLastOpened(tripId: String) {
        // Non-critical best-effort write — failure here just means the "recent" sort is stale.
        runCatching { tripDoc(tripId).update(mapOf("lastOpenedAt" to getCurrentTimestamp())).await() }
    }

    override suspend fun syncTripsToLocal() {
        runCatching {
            val trips = getTrips().getOrNull() ?: return
            trips.forEach { saveTripToLocal(it) }
        }
    }

    override suspend fun renameMemberInTrip(tripId: String, userId: String, newName: String): Result<Unit> {
        return try {
            currentUserId ?: return Result.failure(Exception("User not authenticated"))
            tripDoc(tripId)
                .update(mapOf("tripMembers.$userId" to newName.trim()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinTripByCode(code: String): JoinTripResult {
        return try {
            val userId   = currentUserId ?: return JoinTripResult.Failure(Exception("User not authenticated"))
            val userName = auth.currentUser?.displayName ?: UNKNOWN_NAME
            val clean    = code.trim().uppercase()

            val query = trips().whereEqualTo("joinCode", clean).limit(1).get().await()
            if (query.isEmpty) return JoinTripResult.InvalidCode

            val doc        = query.documents.first()
            val tripId     = doc.id
            val createdBy  = doc.getString("createdBy") ?: ""
            val memberIds  = (doc.get("tripMembers") as? Map<*, *>)
                ?.keys?.map { it.toString() }?.toSet()
                ?: emptySet()

            // Pure rule: detects "already a member" (owner-or-tripMembers entry).
            val decision = TripBusinessRules.evaluateJoinAttempt(
                tripCreatorId     = createdBy,
                existingMemberIds = memberIds,
                joinerId          = userId,
                joinerName        = userName,
            )

            when (decision) {
                TripBusinessRules.JoinDecision.AlreadyMember -> return JoinTripResult.AlreadyMember
                is TripBusinessRules.JoinDecision.Allowed -> {
                    tripDoc(tripId).update(
                        mapOf(
                            "tripMembers.${decision.joinerId}" to decision.joinerName,
                            "updatedAt"                        to getCurrentTimestamp(),
                        )
                    ).await()
                }
            }

            val trip = parseTripFromDoc(doc, tripId)
                ?.let { it.copy(tripMembers = it.tripMembers + (userId to userName)) }
                ?: return JoinTripResult.Failure(Exception("Failed to parse trip document"))

            JoinTripResult.Success(trip)
        } catch (e: Exception) {
            JoinTripResult.Failure(e)
        }
    }

    override suspend fun ensureJoinCode(tripId: String): Result<String> {
        return try {
            val doc = tripDoc(tripId).get().await()
            val existing = doc.getString("joinCode").orEmpty()
            if (existing.isNotBlank()) return Result.success(existing)

            val newCode = generateJoinCode()
            tripDoc(tripId).update(mapOf("joinCode" to newCode)).await()
            Result.success(newCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMemberFromTrip(tripId: String, userIdToRemove: String): Result<Unit> {
        return try {
            val currentId      = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            val tripSnap       = tripDoc(tripId).get().await()
            val tripCreatorId  = tripSnap.getString("createdBy") ?: ""
            val currentMembers = (tripSnap.get("tripMembers") as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.mapValues { it.value.toString() }
                ?: emptyMap()

            // Pure rule: owner-only check (security rules also enforce server-side)
            // + resolution of the display name to archive into `formerMembers`.
            val decision = TripBusinessRules.evaluateRemoveMember(
                removerId        = currentId,
                tripCreatorId    = tripCreatorId,
                existingMembers  = currentMembers,
                memberIdToRemove = userIdToRemove,
            )

            when (decision) {
                TripBusinessRules.RemoveMemberDecision.NotOwner ->
                    return Result.failure(Exception("Only the trip owner can remove members."))
                is TripBusinessRules.RemoveMemberDecision.Allowed -> {
                    tripDoc(tripId).update(
                        mapOf(
                            "tripMembers.${decision.removedUserId}"   to FieldValue.delete(),
                            "formerMembers.${decision.removedUserId}" to decision.preservedDisplayName,
                        )
                    ).await()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLocalTrips(): List<Trip> {
        return try {
            database.tripEntityQueries.getAllTrips().executeAsList().map { entity ->
                Trip(
                    id            = entity.id,
                    name          = entity.name,
                    description   = entity.description,
                    createdBy     = entity.createdBy,
                    tripMembers   = runCatching {
                        Json.decodeFromString<Map<String, String>>(entity.tripMembers)
                    }.getOrDefault(emptyMap()),
                    formerMembers = runCatching {
                        Json.decodeFromString<Map<String, String>>(entity.formerMembers)
                    }.getOrDefault(emptyMap()),
                    startDate     = entity.startDate ?: 0L,
                    endDate       = entity.endDate   ?: 0L,
                    createdAt     = entity.createdAt,
                    updatedAt     = entity.updatedAt,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Casts a Firestore array-of-string field tolerantly: nulls and non-strings are dropped. */
    private fun parseStringList(value: Any?): List<String> =
        (value as? List<*>).orEmpty().mapNotNull { it as? String }

    private fun parseTripFromDoc(doc: DocumentSnapshot, overrideId: String? = null): Trip? = try {
        val membersRaw = doc.get("tripMembers")   as? Map<*, *> ?: emptyMap<Any, Any>()
        val formerRaw  = doc.get("formerMembers") as? Map<*, *> ?: emptyMap<Any, Any>()
        val membersMap = membersRaw.mapKeys { it.key.toString() }.mapValues { it.value.toString() }
        val formerMap  = formerRaw.mapKeys  { it.key.toString() }.mapValues { it.value.toString() }
        Trip(
            id            = overrideId ?: doc.id,
            name          = doc.getString("name") ?: "",
            description   = doc.getString("description") ?: "",
            destination   = doc.getString("destination") ?: "",
            createdBy     = doc.getString("createdBy") ?: "",
            tripMembers   = membersMap,
            formerMembers = formerMap,
            joinCode      = doc.getString("joinCode") ?: "",
            startDate     = doc.getLong("startDate") ?: 0L,
            endDate       = doc.getLong("endDate") ?: 0L,
            createdAt     = doc.getLong("createdAt") ?: 0L,
            updatedAt     = doc.getLong("updatedAt") ?: 0L,
            lastOpenedAt  = doc.getLong("lastOpenedAt") ?: 0L,
        )
    } catch (_: Exception) { null }

    private fun saveTripToLocal(trip: Trip) {
        runCatching {
            database.tripEntityQueries.insertTrip(
                id            = trip.id,
                name          = trip.name,
                description   = trip.description,
                createdBy     = trip.createdBy,
                tripMembers   = Json.encodeToString(trip.tripMembers),
                formerMembers = Json.encodeToString(trip.formerMembers),
                startDate     = trip.startDate,
                endDate       = trip.endDate,
                createdAt     = trip.createdAt,
                updatedAt     = trip.updatedAt,
                lastSyncedAt  = getCurrentTimestamp(),
            )
        }
    }
}
