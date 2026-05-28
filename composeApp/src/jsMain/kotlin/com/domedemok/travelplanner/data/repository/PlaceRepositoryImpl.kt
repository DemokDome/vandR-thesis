package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.Place
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.remote.FoursquareService
import com.domedemok.travelplanner.data.repository.rules.PlaceBusinessRules
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.js
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * JS/Wasm implementation of [PlaceRepository].
 *
 * Uses the GitLive Firebase Firestore wrapper (suspending API, no callbacks).
 * Enrichment fields (rating, popularity, photoUrl, distanceMeters) are
 * persisted alongside core fields so the saved-places list can display them
 * even without hitting Foursquare again.
 *
 * Note: `addedAt` is stored as a Long (ms since epoch), NOT converted to
 * Double — mixing numeric types across platforms corrupts the Firestore sort
 * order used in `orderBy("addedAt")`.
 */
class PlaceRepositoryImpl(
    private val foursquareService: FoursquareService,
) : PlaceRepository {

    private val firestore = Firebase.firestore

    private fun placesRef(tripId: String) =
        firestore.collection(FirestoreCollections.TRIPS).document(tripId).collection(FirestoreCollections.PLACES)

    // ── Write ────────────────────────────────────────────────────────────────

    override suspend fun addPlaceToTrip(tripId: String, place: Place): Result<Place> =
        runCatching {
            // Kotlin Long is not a JS number — overwrite `addedAt` with Double after
            // the shared payload builder applies the Long.
            val data = PlaceBusinessRules.buildAddPayload(
                place      = place,
                addedByUid = Firebase.auth.currentUser?.uid ?: "",
            ).toMutableMap().apply { put("addedAt", place.addedAt.toDouble()) }

            val ref = placesRef(tripId).add(data)
            place.copy(id = ref.id, tripId = tripId)
        }.onFailure { e -> console.error("[PlaceRepository] addPlaceToTrip failed:", e) }

    // ── Read (one-shot) ──────────────────────────────────────────────────────

    override suspend fun getPlacesForTrip(tripId: String): Result<List<Place>> =
        runCatching {
            placesRef(tripId).orderBy("addedAt").get()
                .documents
                .mapNotNull { parsePlaceFromDoc(it, tripId) }
        }.onFailure { e -> console.error("[PlaceRepository] getPlacesForTrip failed:", e) }

    // ── Read (real-time) ─────────────────────────────────────────────────────

    override fun getPlacesForTripFlow(tripId: String): Flow<List<Place>> =
        placesRef(tripId).snapshots.map { snap ->
            snap.documents.mapNotNull { parsePlaceFromDoc(it, tripId) }
                .sortedBy { it.addedAt }   // orderBy("addedAt") silently fails on JS; sort client-side
        }

    // ── Update ───────────────────────────────────────────────────────────────

    override suspend fun updatePlace(tripId: String, place: Place): Result<Unit> =
        runCatching {
            placesRef(tripId).document(place.id).update(PlaceBusinessRules.buildUpdatePayload(place))
        }.onFailure { e -> console.error("[PlaceRepository] updatePlace failed:", e) }

    // ── Delete ───────────────────────────────────────────────────────────────

    override suspend fun deletePlace(tripId: String, placeId: String): Result<Unit> =
        runCatching {
            placesRef(tripId).document(placeId).delete()
        }.onFailure { e -> console.error("[PlaceRepository] deletePlace failed:", e) }

    // ── API delegation ───────────────────────────────────────────────────────

    override suspend fun searchPlaces(
        query:      String,
        latitude:   Double,
        longitude:  Double,
        categoryId: String?,
        limit:      Int,
    ): Result<List<Place>> = foursquareService.searchPlaces(query, latitude, longitude, categoryId, limit)

    override suspend fun geocodeAddress(address: String): Result<Pair<Double, Double>> =
        foursquareService.geocodeAddress(address)

    override suspend fun getLastSearchLocation(tripId: String): String? = null

    override suspend fun saveSearchLocation(tripId: String, location: String) { }

    // ── Firestore document parser ────────────────────────────────────────────

    /**
     * Converts a Firestore document snapshot into a [Place].
     * Returns null and logs an error on parse failure so one bad document
     * doesn't break the entire list.
     *
     * Uses try-per-field rather than a single catch block so partial data is
     * still usable, and errors are attributed to specific fields.
     */
    private fun parsePlaceFromDoc(doc: DocumentSnapshot, tripId: String): Place? = try {
        Place(
            id             = doc.id,
            tripId         = tripId,
            name           = safeString(doc, "name"),
            address        = safeString(doc, "address"),
            latitude       = safeDouble(doc, "latitude"),
            longitude      = safeDouble(doc, "longitude"),
            notes          = safeString(doc, "notes"),
            category       = safeString(doc, "category"),
            foursquareId   = safeString(doc, "foursquareId"),
            addedAt        = safeLong  (doc, "addedAt"),
            addedBy        = safeString(doc, "addedBy"),
            rating         = safeDouble(doc, "rating"),
            popularity     = safeDouble(doc, "popularity"),
            photoUrl       = safeString(doc, "photoUrl"),
            distanceMeters = safeInt   (doc, "distanceMeters"),
        )
    } catch (e: Exception) {
        console.error("[PlaceRepository] Failed to parse document ${doc.id}:", e)
        null
    }

    // ── Type-safe field helpers ──────────────────────────────────────────────
    // Firestore JS SDK may return numeric fields as Double, Long, or Int
    // depending on the value's magnitude. These helpers try multiple casts and
    // fall back to a safe default, preventing ClassCastException.

    private fun safeString(doc: DocumentSnapshot, field: String): String = try {
        doc.get<String?>(field) ?: (doc.get<Any?>(field) as? String) ?: ""
    } catch (_: Exception) { "" }

    private fun safeDouble(doc: DocumentSnapshot, field: String): Double = try {
        doc.get<Double?>(field)
            ?: (doc.get<Any?>(field) as? Number)?.toDouble()
            ?: 0.0
    } catch (_: Exception) { 0.0 }

    private fun safeLong(doc: DocumentSnapshot, field: String): Long = try {
        // Read straight from the JS object. `doc.get<Long?>` / `doc.get<Any?>` go
        // through kotlinx serialization, which on Kotlin/JS does not reliably
        // round-trip 64-bit integers written from Android (the value silently
        // becomes 0). The dynamic-JS path matches what TripRepositoryImpl uses
        // for trip timestamps and works for both Long and Double encodings.
        val data = doc.js.data() ?: return 0L
        val raw = data.asDynamic()[field]
        when {
            raw == null || raw == js("undefined") -> 0L
            js("typeof raw === 'number'") as Boolean -> (raw as Number).toLong()
            // Firestore server-side Timestamp object: { seconds, nanoseconds }
            raw.seconds != null && raw.seconds != js("undefined") ->
                (raw.seconds as Number).toLong() * 1000L
            else -> 0L
        }
    } catch (_: Exception) { 0L }

    private fun safeInt(doc: DocumentSnapshot, field: String): Int = try {
        (doc.get<Any?>(field) as? Number)?.toInt() ?: 0
    } catch (_: Exception) { 0 }
}
