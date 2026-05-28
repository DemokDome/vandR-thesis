package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.ItineraryDay
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.ItineraryBusinessRules
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.js
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

/**
 * On-the-wire shape of an itinerary day for the JS Firestore client.
 *
 * Written via a typed `@Serializable` DTO rather than a raw `Map<String, Any>`
 * because GitLive's JS wrapper serialises map values polymorphically through
 * kotlinx-serialization, and a `List<String>` inside an `Any`-typed map
 * doesn't resolve to a serializer at runtime — it surfaces as cryptic
 * "Serializer for class 'Array' is not found" or "Invalid array length"
 * errors. A typed DTO sidesteps the issue: the field types are known
 * statically so the compiler generates a proper serializer for the whole
 * document.
 *
 * `dayNumber` and `date` are Doubles for the same reason TripDto uses Doubles:
 * Kotlin Long is a two-Int struct in JS that Firebase JS SDK rejects.
 */
@Serializable
private data class ItineraryDayDto(
    val dayNumber: Double,
    val date:      Double,
    val placeIds:  List<String> = emptyList(),
)

class ItineraryRepositoryImpl : ItineraryRepository {

    private val firestore = Firebase.firestore

    private fun col(tripId: String): CollectionReference =
        firestore.collection(FirestoreCollections.TRIPS).document(tripId).collection(FirestoreCollections.ITINERARY)

    // ── Real-time stream ─────────────────────────────────────────────────────
    // NOTE: orderBy is intentionally omitted here — in the GitLive JS wrapper,
    // attaching orderBy to a subcollection snapshot listener can silently fail
    // to emit on the first load. We sort client-side instead.

    override fun getDaysFlow(tripId: String): Flow<List<ItineraryDay>> =
        col(tripId).snapshots.map { snap ->
            snap.documents
                .mapNotNull { doc -> parseDayFromDoc(doc, tripId) }
                .sortedBy { it.dayNumber }
        }

    // ── Write ────────────────────────────────────────────────────────────────

    override suspend fun updateDayPlaces(
        tripId: String,
        dayId: String,
        placeIds: List<String>,
    ): Result<Unit> = runCatching {
        col(tripId).document(dayId).updateFields { "placeIds" to placeIds }
    }

    override suspend fun ensureDaysExist(
        tripId: String,
        startDate: Long,
        endDate: Long,
    ): Result<Unit> = runCatching {
        val existingDayIds = col(tripId).get().documents.map { it.id }.toSet()

        // Pure rule: validates the range, computes the day plan, skips existing ids.
        val daysToCreate = ItineraryBusinessRules.planDaysToCreate(
            startDateMs    = startDate,
            endDateMs      = endDate,
            existingDayIds = existingDayIds,
        )

        // Write via the typed DTO — see KDoc above on why raw Map<String, Any>
        // with a List value fails on JS.
        daysToCreate.forEach { day ->
            col(tripId).document(day.dayId).set(
                ItineraryDayDto(
                    dayNumber = day.dayNumber.toDouble(),
                    date      = day.dateMs.toDouble(),
                    placeIds  = emptyList(),
                )
            )
        }
    }

    // ── Native JS document parsing ───────────────────────────────────────────
    // GitLive's doc.get<Any?>() returns JS dynamic objects that don't satisfy
    // Kotlin's `is Number` / `is List<*>` checks in the JS runtime.
    // We mirror the pattern used in TripRepositoryImpl: read via doc.js.data()
    // and access fields as dynamic properties.

    private fun parseDayFromDoc(doc: DocumentSnapshot, tripId: String): ItineraryDay? {
        return try {
            val data = doc.js.data()
            if (data == null || data == js("undefined")) return null

            val dyn = data.asDynamic()

            val dayNumber = parseDynamicInt(dyn.dayNumber)
            val date      = parseDynamicLong(dyn.date)
            val placeIds  = parseDynamicStringList(dyn.placeIds)

            ItineraryDay(
                id        = doc.id,
                tripId    = tripId,
                dayNumber = dayNumber,
                date      = date,
                placeIds  = placeIds,
            )
        } catch (e: Exception) {
            console.error("Error parsing itinerary day ${doc.id}:", e)
            null
        }
    }

    private fun parseDynamicInt(value: dynamic): Int {
        return try {
            if (value == null || value == js("undefined")) return 0
            (value as Number).toInt()
        } catch (_: Exception) { 0 }
    }

    private fun parseDynamicLong(value: dynamic): Long {
        return try {
            if (value == null || value == js("undefined")) return 0L
            (value as Number).toLong()
        } catch (_: Exception) { 0L }
    }

    private fun parseDynamicStringList(value: dynamic): List<String> {
        return try {
            if (value == null || value == js("undefined")) return emptyList()
            if (js("Array.isArray(value)") as Boolean) {
                val arr = value as Array<*>
                arr.mapNotNull { it?.toString() }
            } else {
                emptyList()
            }
        } catch (_: Exception) { emptyList() }
    }
}
