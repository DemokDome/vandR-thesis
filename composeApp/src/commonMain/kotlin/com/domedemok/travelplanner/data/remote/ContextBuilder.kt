@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.domedemok.travelplanner.data.remote

import com.domedemok.travelplanner.data.model.ChatMessage
import com.domedemok.travelplanner.data.model.GeminiContent
import com.domedemok.travelplanner.data.model.GeminiPart
import com.domedemok.travelplanner.data.model.ItineraryDay
import com.domedemok.travelplanner.data.model.Place
import com.domedemok.travelplanner.data.model.Trip
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Builds the two contextual inputs the AI assistant needs:
 *  1. a one-shot trip summary embedded in the system prompt, and
 *  2. the structured chat history in Gemini's wire format.
 *
 * Pure transformations only — no I/O, safe to share as a singleton.
 */
class ContextBuilder {

    companion object {
        /**
         * Sentinel `senderId` used to distinguish AI replies from user messages
         * when stored in the same Firestore collection. Must match the value
         * AI replies are written with.
         */
        const val AI_BOT_USER_ID = "ai_bot"

        private val MONTH_SHORT = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )
    }

    /** Formats an epoch-ms timestamp as "Apr 27, 2026", or "not set" if missing. */
    private fun fmtDate(epochMs: Long): String {
        if (epochMs <= 0L) return "not set"
        val dt = Instant.fromEpochMilliseconds(epochMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        return "${MONTH_SHORT[dt.month.number - 1]} ${dt.day}, ${dt.year}"
    }

    /**
     * Formats a 0–10 rating as one decimal place using integer arithmetic,
     * which is safe across all KMP targets (JS has no String.format).
     * Returns null if the rating is zero (= missing / unrated).
     */
    private fun fmtRating(rating: Double): String? {
        if (rating <= 0.0) return null
        val tenths = (rating * 10).toInt()
        return "${tenths / 10}.${tenths % 10}/10"
    }

    /**
     * Produces the trip-context blob that gets appended to the system prompt.
     *
     * Includes destination, dates, duration, member names, saved places with
     * address / rating / notes, and the full day-by-day itinerary if the trip
     * has dates set and places assigned to days.
     */
    fun buildTravelContext(
        trip: Trip,
        places: List<Place>,
        itineraryDays: List<ItineraryDay> = emptyList(),
    ): String {
        val sb = StringBuilder()

        // ── Basic info ────────────────────────────────────────────────────────
        sb.appendLine("Trip: ${trip.name}")
        if (trip.destination.isNotBlank()) sb.appendLine("Destination: ${trip.destination}")
        if (trip.description.isNotBlank()) sb.appendLine("Description: ${trip.description}")

        // ── Dates + duration ──────────────────────────────────────────────────
        val hasStart = trip.startDate > 0L
        val hasEnd   = trip.endDate   > 0L
        if (hasStart || hasEnd) {
            val start = if (hasStart) fmtDate(trip.startDate) else "not set"
            val end   = if (hasEnd)   fmtDate(trip.endDate)   else "not set"
            sb.append("Dates: $start – $end")
            if (hasStart && hasEnd) {
                val days = ((trip.endDate - trip.startDate) / 86_400_000L + 1).coerceAtLeast(1)
                sb.append(" ($days ${if (days == 1L) "day" else "days"})")
            }
            sb.appendLine()
        }

        // ── Members ───────────────────────────────────────────────────────────
        val memberNames = trip.tripMembers.values.filter { it.isNotBlank() }
        if (memberNames.isNotEmpty()) {
            sb.appendLine("Group members (${memberNames.size}): ${memberNames.joinToString(", ")}")
        }

        // ── Saved places ──────────────────────────────────────────────────────
        if (places.isEmpty()) {
            sb.appendLine("\nNo places saved yet.")
        } else {
            sb.appendLine("\nSaved places (${places.size}):")
            for (place in places) {
                val details = buildList {
                    if (place.category.isNotBlank()) add(place.category)
                    if (place.address.isNotBlank())  add(place.address)
                    fmtRating(place.rating)?.let    { add("Rating: $it") }
                    if (place.notes.isNotBlank())    add("Notes: ${place.notes}")
                }
                val suffix = if (details.isEmpty()) "" else " (${details.joinToString(", ")})"
                sb.appendLine("  • ${place.name}$suffix")
            }
        }

        // ── Day-by-day itinerary ──────────────────────────────────────────────
        val sortedDays = itineraryDays.sortedBy { it.dayNumber }
        if (sortedDays.isNotEmpty()) {
            val placeById   = places.associateBy { it.id }
            val assignedIds = sortedDays.flatMap { it.placeIds }.toSet()
            val unassigned  = places.filter { it.id !in assignedIds }

            sb.appendLine("\nItinerary:")
            for (day in sortedDays) {
                val dateStr    = if (day.date > 0L) " (${fmtDate(day.date)})" else ""
                val dayPlaces  = day.placeIds.mapNotNull { placeById[it]?.name }
                val placesStr  = if (dayPlaces.isEmpty()) "no places assigned yet" else dayPlaces.joinToString(", ")
                sb.appendLine("  Day ${day.dayNumber}$dateStr: $placesStr")
            }
            if (unassigned.isNotEmpty()) {
                sb.appendLine("  Not yet scheduled: ${unassigned.joinToString(", ") { it.name }}")
            }
        }

        return sb.toString().trimEnd()
    }

    /**
     * Converts the persisted chat history into Gemini's `contents` array.
     * Messages whose senderId equals [aiUserId] are tagged as `"model"`,
     * everything else as `"user"`.
     */
    fun buildChatHistory(
        messages: List<ChatMessage>,
        aiUserId: String = AI_BOT_USER_ID,
    ): List<GeminiContent> = messages.map { message ->
        GeminiContent(
            role  = if (message.senderId == aiUserId) "model" else "user",
            parts = listOf(GeminiPart(message.text)),
        )
    }
}
