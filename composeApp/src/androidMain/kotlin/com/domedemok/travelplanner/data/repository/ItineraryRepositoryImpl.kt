package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.ItineraryDay
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.ItineraryBusinessRules
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ItineraryRepositoryImpl(
    private val firestore: FirebaseFirestore,
) : ItineraryRepository {

    private fun col(tripId: String): CollectionReference =
        firestore.collection(FirestoreCollections.TRIPS).document(tripId).collection(FirestoreCollections.ITINERARY)

    // ── Real-time stream ─────────────────────────────────────────────────────

    override fun getDaysFlow(tripId: String): Flow<List<ItineraryDay>> = callbackFlow {
        val listener = col(tripId)
            .orderBy("dayNumber")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val days = snap?.documents?.mapNotNull { doc ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        ItineraryDay(
                            id        = doc.id,
                            tripId    = tripId,
                            dayNumber = (doc.getLong("dayNumber") ?: 0L).toInt(),
                            date      = doc.getLong("date") ?: 0L,
                            placeIds  = (doc.get("placeIds") as? List<*>)
                                ?.mapNotNull { it as? String } ?: emptyList(),
                        )
                    } catch (_: Exception) { null }
                } ?: emptyList()
                trySend(days)
            }
        awaitClose { listener.remove() }
    }

    // ── Write ────────────────────────────────────────────────────────────────

    override suspend fun updateDayPlaces(
        tripId: String,
        dayId: String,
        placeIds: List<String>,
    ): Result<Unit> = runCatching {
        col(tripId).document(dayId)
            .update("placeIds", placeIds)
            .await()
    }

    override suspend fun ensureDaysExist(
        tripId: String,
        startDate: Long,
        endDate: Long,
    ): Result<Unit> = runCatching {
        val existingDayIds = col(tripId).get().await().documents.map { it.id }.toSet()

        // Pure rule: validates the range, computes the day plan, skips existing ids.
        val daysToCreate = ItineraryBusinessRules.planDaysToCreate(
            startDateMs    = startDate,
            endDateMs      = endDate,
            existingDayIds = existingDayIds,
        )

        daysToCreate.forEach { day ->
            col(tripId).document(day.dayId).set(
                mapOf(
                    "dayNumber" to day.dayNumber,
                    "date"      to day.dateMs,
                    "placeIds"  to emptyList<String>(),
                )
            ).await()
        }
    }
}
