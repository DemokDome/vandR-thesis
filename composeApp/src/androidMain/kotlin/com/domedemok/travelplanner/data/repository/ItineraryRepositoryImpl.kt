package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.local.database.TravelDatabase
import com.domedemok.travelplanner.data.model.ItineraryDay
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.ItineraryBusinessRules
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ItineraryRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val database: TravelDatabase,
) : ItineraryRepository {

    private fun col(tripId: String): CollectionReference =
        firestore.collection(FirestoreCollections.TRIPS).document(tripId).collection(FirestoreCollections.ITINERARY)

    // ── Real-time stream (offline-first) ─────────────────────────────────────
    // The local SQLDelight cache is surfaced immediately so the screen has
    // content before — or entirely without — a network round-trip. Each live
    // Firestore snapshot is then written through to the cache, and a snapshot
    // error (typically: offline) falls back to the cache instead of tearing
    // down the stream. Mirrors the offline-first strategy used for trips.

    override fun getDaysFlow(tripId: String): Flow<List<ItineraryDay>> = callbackFlow {
        loadDaysFromLocal(tripId).takeIf { it.isNotEmpty() }?.let { trySend(it) }

        val listener = col(tripId)
            .orderBy("dayNumber")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(loadDaysFromLocal(tripId))
                    return@addSnapshotListener
                }
                val days = snap?.documents?.mapNotNull { parseDayFromDoc(it, tripId) } ?: emptyList()
                saveDaysToLocal(tripId, days)
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
        // Write the cache first so the reorder/add/remove survives offline and
        // across navigation even while the Firestore write is still queued.
        updateLocalDayPlaces(tripId, dayId, placeIds)
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
        // The newly created days are picked up and cached by the snapshot
        // listener in getDaysFlow, so no explicit local write is needed here.
    }

    // ── Firestore document parsing ───────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun parseDayFromDoc(doc: DocumentSnapshot, tripId: String): ItineraryDay? = try {
        ItineraryDay(
            id        = doc.id,
            tripId    = tripId,
            dayNumber = (doc.getLong("dayNumber") ?: 0L).toInt(),
            date      = doc.getLong("date") ?: 0L,
            placeIds  = (doc.get("placeIds") as? List<*>)
                ?.mapNotNull { it as? String } ?: emptyList(),
        )
    } catch (_: Exception) { null }

    // ── Local cache (SQLDelight) ─────────────────────────────────────────────

    private fun loadDaysFromLocal(tripId: String): List<ItineraryDay> = runCatching {
        database.itineraryDayEntityQueries.getDaysForTrip(tripId).executeAsList().map { entity ->
            ItineraryDay(
                id        = entity.id,
                tripId    = entity.tripId,
                dayNumber = entity.dayNumber.toInt(),
                date      = entity.date,
                placeIds  = runCatching {
                    Json.decodeFromString<List<String>>(entity.placeIds)
                }.getOrDefault(emptyList()),
            )
        }
    }.getOrDefault(emptyList())

    /** Replaces the cached day set for [tripId] so remote deletions are reflected too. */
    private fun saveDaysToLocal(tripId: String, days: List<ItineraryDay>) {
        runCatching {
            database.itineraryDayEntityQueries.transaction {
                database.itineraryDayEntityQueries.deleteDaysForTrip(tripId)
                days.forEach { day ->
                    database.itineraryDayEntityQueries.insertDay(
                        tripId    = tripId,
                        id        = day.id,
                        dayNumber = day.dayNumber.toLong(),
                        date      = day.date,
                        placeIds  = Json.encodeToString(day.placeIds),
                    )
                }
            }
        }
    }

    private fun updateLocalDayPlaces(tripId: String, dayId: String, placeIds: List<String>) {
        runCatching {
            database.itineraryDayEntityQueries.updateDayPlaces(
                placeIds = Json.encodeToString(placeIds),
                tripId   = tripId,
                id       = dayId,
            )
        }
    }
}
