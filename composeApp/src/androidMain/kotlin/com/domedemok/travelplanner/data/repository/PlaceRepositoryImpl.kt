package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.local.database.TravelDatabase
import com.domedemok.travelplanner.data.model.Place
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.remote.FoursquareService
import com.domedemok.travelplanner.data.repository.rules.PlaceBusinessRules
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PlaceRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: TravelDatabase,
    private val foursquareService: FoursquareService
) : PlaceRepository {

    override suspend fun addPlaceToTrip(tripId: String, place: Place): Result<Place> {
        return try {
            val placeData = PlaceBusinessRules.buildAddPayload(
                place      = place,
                addedByUid = auth.currentUser?.uid ?: "",
            )

            val docRef = firestore.collection(FirestoreCollections.TRIPS)
                .document(tripId)
                .collection(FirestoreCollections.PLACES)
                .add(placeData)
                .await()

            val createdPlace = place.copy(id = docRef.id, tripId = tripId)

            savePlaceToLocal(createdPlace)

            Result.success(createdPlace)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPlacesForTrip(tripId: String): Result<List<Place>> {
        return try {
            val snapshot = firestore.collection(FirestoreCollections.TRIPS)
                .document(tripId)
                .collection(FirestoreCollections.PLACES)
                .orderBy("addedAt")
                .get()
                .await()

            val places = snapshot.documents.mapNotNull { doc ->
                parsePlaceFromDoc(doc, tripId)
            }

            places.forEach { savePlaceToLocal(it) }

            Result.success(places)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPlacesForTripFlow(tripId: String): Flow<List<Place>> = callbackFlow {
        val listener = firestore.collection(FirestoreCollections.TRIPS)
            .document(tripId)
            .collection(FirestoreCollections.PLACES)
            .orderBy("addedAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val places = snapshot?.documents?.mapNotNull { doc ->
                    parsePlaceFromDoc(doc, tripId)
                } ?: emptyList()

                trySend(places)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun updatePlace(tripId: String, place: Place): Result<Unit> {
        return try {
            firestore.collection(FirestoreCollections.TRIPS)
                .document(tripId)
                .collection(FirestoreCollections.PLACES)
                .document(place.id)
                .update(PlaceBusinessRules.buildUpdatePayload(place))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePlace(tripId: String, placeId: String): Result<Unit> {
        return try {
            firestore.collection(FirestoreCollections.TRIPS)
                .document(tripId)
                .collection(FirestoreCollections.PLACES)
                .document(placeId)
                .delete()
                .await()

            database.placeEntityQueries.deletePlace(placeId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchPlaces(
        query: String,
        latitude: Double,
        longitude: Double,
        categoryId: String?,
        limit: Int,
    ): Result<List<Place>> {
        return foursquareService.searchPlaces(query, latitude, longitude, categoryId, limit)
    }

    override suspend fun geocodeAddress(address: String): Result<Pair<Double, Double>> {
        return foursquareService.geocodeAddress(address)
    }

    override suspend fun getLastSearchLocation(tripId: String): String? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            database.searchStateEntityQueries.getSearchState(userId, tripId)
                .executeAsOneOrNull()
        } catch (e: Exception) {
            android.util.Log.w("PlaceRepository", "Local search-state read failed: ${e.message}")
            null
        }
    }

    override suspend fun saveSearchLocation(tripId: String, location: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            database.searchStateEntityQueries.upsertSearchState(userId, tripId, location)
        } catch (e: Exception) {
            android.util.Log.w("PlaceRepository", "Local search-state write failed: ${e.message}")
        }
    }

    private fun parsePlaceFromDoc(
        doc:    com.google.firebase.firestore.DocumentSnapshot,
        tripId: String,
    ): Place? = try {
        Place(
            id             = doc.id,
            tripId         = tripId,
            name           = doc.getString("name")         ?: "",
            address        = doc.getString("address")      ?: "",
            latitude       = doc.getDouble("latitude")     ?: 0.0,
            longitude      = doc.getDouble("longitude")    ?: 0.0,
            notes          = doc.getString("notes")        ?: "",
            category       = doc.getString("category")     ?: "",
            foursquareId   = doc.getString("foursquareId") ?: "",
            addedAt        = doc.getLong("addedAt")        ?: 0L,
            addedBy        = doc.getString("addedBy")      ?: "",
            // Enrichment fields — default to neutral values when absent (old docs).
            rating         = doc.getDouble("rating")       ?: 0.0,
            popularity     = doc.getDouble("popularity")   ?: 0.0,
            photoUrl       = doc.getString("photoUrl")     ?: "",
            distanceMeters = doc.getLong("distanceMeters")?.toInt() ?: 0,
        )
    } catch (e: Exception) {
        android.util.Log.e("PlaceRepository", "Failed to parse place ${doc.id}", e)
        null
    }

    private fun savePlaceToLocal(place: Place) {
        try {
            database.placeEntityQueries.insertPlace(
                id           = place.id,
                tripId       = place.tripId,
                name         = place.name,
                address      = place.address,
                latitude     = place.latitude,
                longitude    = place.longitude,
                category     = place.category,
                foursquareId = place.foursquareId,
                addedAt      = place.addedAt,
            )
        } catch (e: Exception) {
            android.util.Log.w("PlaceRepository", "Local cache write failed for ${place.id}: ${e.message}")
        }
    }
}