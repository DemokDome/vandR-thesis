package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.Trip
import com.domedemok.travelplanner.data.model.TripDto
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.TripBusinessRules
import com.domedemok.travelplanner.util.generateJoinCode
import com.domedemok.travelplanner.util.getCurrentTimestamp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.js
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * JS implementation of [TripRepository].
 *
 * Trip documents are read via raw JS dynamic-property access rather than
 * GitLive's `data<TripDto>()` deserialiser because timestamps written by the
 * Android client may arrive as Firestore server-side `Timestamp` objects
 * (`{ seconds, nanoseconds }`) — kotlinx serialisation does not unwrap those.
 * The hand-rolled `parseDynamic*` helpers handle every encoding we've seen.
 */
class TripRepositoryImpl : TripRepository {

    private companion object {
        const val UNKNOWN_NAME = TripBusinessRules.UNKNOWN_DISPLAY_NAME
    }

    private val auth      = Firebase.auth
    private val firestore = Firebase.firestore

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun trips(): CollectionReference = firestore.collection(FirestoreCollections.TRIPS)
    private fun tripDoc(tripId: String): DocumentReference = trips().document(tripId)
    private fun userDoc(uid: String): DocumentReference = firestore.collection(FirestoreCollections.USERS).document(uid)

    /**
     * Membership query — see Android counterpart KDoc. `orderBy` on a nested
     * map field implicitly filters out documents where the field is missing.
     */
    private fun membershipQuery(userId: String): Query =
        trips().where { "tripMembers.$userId" greaterThanOrEqualTo "" }

    override suspend fun createTrip(trip: Trip): Result<Trip> {
        return try {
            val userId          = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            val currentUserName = auth.currentUser?.displayName ?: UNKNOWN_NAME
            val joinCode        = generateJoinCode()

            val createdAt = if (trip.createdAt == 0L) getCurrentTimestamp() else trip.createdAt
            val updatedAt = getCurrentTimestamp()

            val tripDto = TripDto(
                name          = trip.name,
                description   = trip.description,
                destination   = trip.destination,
                createdBy     = userId,
                tripMembers   = mapOf(userId to currentUserName),
                formerMembers = emptyMap(),
                joinCode      = joinCode,
                startDate     = trip.startDate.toDouble(),
                endDate       = trip.endDate.toDouble(),
                createdAt     = createdAt.toDouble(),
                updatedAt     = updatedAt.toDouble(),
            )

            val docRef = trips().add(tripDto)

            Result.success(
                trip.copy(
                    id          = docRef.id,
                    createdBy   = userId,
                    tripMembers = tripDto.tripMembers,
                    joinCode    = joinCode,
                    createdAt   = createdAt,
                    updatedAt   = updatedAt,
                )
            )
        } catch (e: Exception) {
            console.error("Error creating trip:", e)
            Result.failure(e)
        }
    }

    override suspend fun getTrips(): Result<List<Trip>> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            val snap = membershipQuery(userId).get()
            Result.success(snap.documents.mapNotNull { parseTripFromDoc(it) })
        } catch (e: Exception) {
            console.error("Error getting trips:", e)
            Result.failure(e)
        }
    }

    override fun getTripsFlow(): Flow<List<Trip>> = channelFlow {
        auth.authStateChanged.collectLatest { firebaseUser ->
            if (firebaseUser == null) {
                send(emptyList())
                return@collectLatest
            }

            try {
                firebaseUser.getIdToken(forceRefresh = true)

                membershipQuery(firebaseUser.uid).snapshots.collect { snap ->
                    send(snap.documents.mapNotNull { parseTripFromDoc(it) })
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Szedjük ki a konkrét hibaüzenetet, mert a Kotlin/JS elrejti!
                val errorMessage = e.message ?: "Nincs üzenet"
                val causeMessage = e.cause?.message ?: "Nincs belső ok"

                console.error("Firestore Hibaüzenet:", errorMessage)
                console.error("Firestore Belső ok:", causeMessage)
                console.error("Teljes Exception:", e)
            }
        }
    }

    override suspend fun getTripById(tripId: String): Result<Trip> {
        return try {
            val doc = tripDoc(tripId).get()
            if (!doc.exists) return Result.failure(Exception("Trip not found"))
            parseTripFromDoc(doc)
                ?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to parse trip"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTrip(trip: Trip): Result<Unit> {
        return try {
            val tripDto = TripDto(
                name          = trip.name,
                description   = trip.description,
                destination   = trip.destination,
                createdBy     = trip.createdBy,
                tripMembers   = trip.tripMembers,
                formerMembers = trip.formerMembers,
                joinCode      = trip.joinCode,   // preserve existing code, never overwrite with ""
                startDate     = trip.startDate.toDouble(),
                endDate       = trip.endDate.toDouble(),
                createdAt     = trip.createdAt.toDouble(),
                updatedAt     = getCurrentTimestamp().toDouble(),
            )
            tripDoc(trip.id).set(tripDto, merge = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            tripDoc(tripId).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveTrip(tripId: String): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            val snap           = tripDoc(tripId).get()
            val currentMembers = parseDynamicStringMap(snap.dynData()?.tripMembers)

            // Pure rule: pick the name to archive into formerMembers so the
            // leaver still resolves on historical expense splits.
            val preservedDisplayName = TripBusinessRules.resolveLeaverDisplayName(
                currentMembers   = currentMembers,
                leaverId         = userId,
                authDisplayName  = auth.currentUser?.displayName,
            )

            tripDoc(tripId).updateFields {
                "tripMembers.$userId"   to FieldValue.delete
                "formerMembers.$userId" to preservedDisplayName
            }
            Result.success(Unit)
        } catch (e: Exception) {
            console.error("Error leaving trip:", e)
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(tripId: String, isFavorite: Boolean): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            val op     = if (isFavorite) FieldValue.arrayUnion(tripId) else FieldValue.arrayRemove(tripId)
            userDoc(userId).updateFields { "favoriteTripIds" to op }
            Result.success(Unit)
        } catch (e: Exception) {
            console.error("Error toggling favorite:", e)
            Result.failure(e)
        }
    }

    override suspend fun shareTrip(tripId: String, userEmail: String): Result<Unit> {
        return try {
            val currentId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            val userQuery = firestore.collection(FirestoreCollections.USERS)
                .where { "email" equalTo userEmail }
                .limit(1)
                .get()

            if (userQuery.documents.isEmpty()) {
                return Result.failure(Exception("User not found with email: $userEmail"))
            }

            val targetUserDoc  = userQuery.documents.first()
            val targetUserId   = targetUserDoc.id
            val targetUserName = parseDynamicString(targetUserDoc.dynData()?.displayName, default = "Unknown User")

            val tripSnap        = tripDoc(tripId).get()
            val existingMembers = parseDynamicStringMap(tripSnap.dynData()?.tripMembers).keys

            // Pure rule: self-share + already-shared rejection.
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
                    tripDoc(tripId).updateFields {
                        "tripMembers.${decision.targetUserId}" to decision.targetUserName
                        "updatedAt"                             to getCurrentTimestamp().toDouble()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            console.error("Error:", e)
            Result.failure(e)
        }
    }

    override suspend fun getFavoriteTripIds(): Result<Set<String>> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            val doc    = userDoc(userId).get()
            if (!doc.exists) return Result.success(emptySet())
            Result.success(parseDynamicStringList(doc.dynData()?.favoriteTripIds).toSet())
        } catch (e: Exception) {
            console.error("Error getting favorites:", e)
            Result.failure(e)
        }
    }

    override suspend fun updateLastOpened(tripId: String) {
        // Non-critical best-effort write — failure here just means the "recent" sort is stale.
        runCatching {
            tripDoc(tripId).updateFields { "lastOpenedAt" to getCurrentTimestamp().toDouble() }
        }
    }

    override suspend fun syncTripsToLocal() {}
    override suspend fun getLocalTrips(): List<Trip> = emptyList()

    override suspend fun renameMemberInTrip(tripId: String, userId: String, newName: String): Result<Unit> {
        return try {
            currentUserId ?: return Result.failure(Exception("User not authenticated"))
            tripDoc(tripId).updateFields { "tripMembers.$userId" to newName.trim() }
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

            val query = trips().where { "joinCode" equalTo clean }.limit(1).get()
            if (query.documents.isEmpty()) return JoinTripResult.InvalidCode

            val doc       = query.documents.first()
            val tripId    = doc.id
            val dyn       = doc.dynData()
            val createdBy = parseDynamicString(dyn?.createdBy)
            val memberIds = parseDynamicStringMap(dyn?.tripMembers).keys

            // Pure rule: detects "already a member" (owner or in tripMembers).
            val decision = TripBusinessRules.evaluateJoinAttempt(
                tripCreatorId     = createdBy,
                existingMemberIds = memberIds,
                joinerId          = userId,
                joinerName        = userName,
            )

            when (decision) {
                TripBusinessRules.JoinDecision.AlreadyMember -> return JoinTripResult.AlreadyMember
                is TripBusinessRules.JoinDecision.Allowed -> {
                    tripDoc(tripId).updateFields {
                        "tripMembers.${decision.joinerId}" to decision.joinerName
                        "updatedAt"                        to getCurrentTimestamp().toDouble()
                    }
                }
            }

            parseTripFromDoc(doc)
                ?.let { JoinTripResult.Success(it.copy(tripMembers = it.tripMembers + (userId to userName))) }
                ?: JoinTripResult.Failure(Exception("Failed to parse trip document"))
        } catch (e: Exception) {
            JoinTripResult.Failure(e)
        }
    }

    override suspend fun ensureJoinCode(tripId: String): Result<String> {
        return try {
            val doc      = tripDoc(tripId).get()
            val existing = parseDynamicString(doc.dynData()?.joinCode)
            if (existing.isNotBlank()) return Result.success(existing)

            val newCode = generateJoinCode()
            tripDoc(tripId).updateFields { "joinCode" to newCode }
            Result.success(newCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMemberFromTrip(tripId: String, userIdToRemove: String): Result<Unit> {
        return try {
            val currentId       = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            val tripSnap        = tripDoc(tripId).get()
            val dyn             = tripSnap.dynData()
            val tripCreatorId   = parseDynamicString(dyn?.createdBy)
            val currentMembers  = parseDynamicStringMap(dyn?.tripMembers)

            // Pure rule: owner-only check (security rules also enforce server-side)
            // + resolution of the display name to archive in `formerMembers`.
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
                    tripDoc(tripId).updateFields {
                        "tripMembers.${decision.removedUserId}"   to FieldValue.delete
                        "formerMembers.${decision.removedUserId}" to decision.preservedDisplayName
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Native JS document parsing ───────────────────────────────────────────

    /** Returns the raw JS data object for [this] document, or null if the document is empty. */
    private fun DocumentSnapshot.dynData(): dynamic {
        val data = js.data() ?: return null
        if (data == js("undefined")) return null
        return data.asDynamic()
    }

    private fun parseTripFromDoc(doc: DocumentSnapshot): Trip? {
        return try {
            val dyn = doc.dynData() ?: return null
            Trip(
                id            = doc.id,
                name          = parseDynamicString(dyn.name),
                description   = parseDynamicString(dyn.description),
                destination   = parseDynamicString(dyn.destination),
                createdBy     = parseDynamicString(dyn.createdBy),
                tripMembers   = parseDynamicStringMap(dyn.tripMembers),
                formerMembers = parseDynamicStringMap(dyn.formerMembers),
                joinCode      = parseDynamicString(dyn.joinCode),
                startDate     = parseDynamicTimestamp(dyn.startDate),
                endDate       = parseDynamicTimestamp(dyn.endDate),
                createdAt     = parseDynamicTimestamp(dyn.createdAt),
                updatedAt     = parseDynamicTimestamp(dyn.updatedAt),
                lastOpenedAt  = parseDynamicTimestamp(dyn.lastOpenedAt),
            )
        } catch (e: Exception) {
            console.error("Error parsing trip ${doc.id}:", e)
            null
        }
    }

    private fun parseDynamicString(value: dynamic, default: String = ""): String = try {
        if (value == null || value == js("undefined")) default else value.toString()
    } catch (_: Exception) { default }

    private fun parseDynamicStringList(value: dynamic): List<String> = try {
        when {
            value == null || value == js("undefined") -> emptyList()
            js("Array.isArray(value)") as Boolean ->
                (value as Array<*>).mapNotNull { it?.toString() }
            else -> emptyList()
        }
    } catch (_: Exception) { emptyList() }

    private fun parseDynamicStringMap(value: dynamic): Map<String, String> = try {
        if (value == null || value == js("undefined")) {
            emptyMap()
        } else {
            val out  = mutableMapOf<String, String>()
            val keys = js("Object.keys(value)") as Array<String>
            for (key in keys) {
                val v = value[key]
                if (v != null && v != js("undefined")) out[key] = v.toString()
            }
            out
        }
    } catch (_: Exception) { emptyMap() }

    /**
     * Reads a timestamp field that may be a number (epoch ms, written by us)
     * or a Firestore server-side `Timestamp` object (`{ seconds, nanoseconds }`
     * or the admin-SDK `_seconds` variant).
     */
    private fun parseDynamicTimestamp(value: dynamic): Long = try {
        when {
            value == null || value == js("undefined") -> 0L
            js("typeof value === 'number'") as Boolean -> (value as Number).toLong()
            value.seconds != null && value.seconds != js("undefined") ->
                (value.seconds as Number).toLong() * 1000L
            value._seconds != null && value._seconds != js("undefined") ->
                (value._seconds as Number).toLong() * 1000L
            else -> 0L
        }
    } catch (_: Exception) { 0L }
}
