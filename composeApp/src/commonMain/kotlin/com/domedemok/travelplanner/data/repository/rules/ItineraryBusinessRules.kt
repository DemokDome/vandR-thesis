package com.domedemok.travelplanner.data.repository.rules

/**
 * Pure decision logic for [com.domedemok.travelplanner.data.repository.ItineraryRepository].
 *
 * The `ensureDaysExist` algorithm (range validation, day-count computation,
 * gap detection against existing doc IDs) is entirely deterministic — no
 * Firestore involvement at all. We extract it here so both platforms
 * iterate over the *same* plan and only differ in how they write each day.
 */
object ItineraryBusinessRules {

    const val MS_PER_DAY:     Long = 86_400_000L

    /**
     * Defends against malformed start/end timestamps producing a runaway
     * batch of writes — one year is well beyond any real trip duration.
     */
    const val MAX_DAY_COUNT: Long = 365L

    /** A single day that should be created in Firestore. */
    data class DayToCreate(
        /** Document id under `trips/{tripId}/itinerary/{dayId}` — `"day_$dayNumber"`. */
        val dayId:     String,
        /** 1-based day index from the start of the trip. */
        val dayNumber: Int,
        /** Epoch-ms timestamp of the day's start. */
        val dateMs:    Long,
    )

    /**
     * Computes the list of itinerary day documents that should be created for
     * the trip's date range, skipping any that already exist.
     *
     * Returns an empty list — never throws — for:
     *  - non-positive start or end (`<= 0`)
     *  - end strictly before start
     *  - day count exceeding [MAX_DAY_COUNT]
     *
     * @param startDateMs       Trip start, epoch ms.
     * @param endDateMs         Trip end, epoch ms (inclusive — same day → 1 doc).
     * @param existingDayIds    Doc ids already present in the itinerary collection.
     */
    fun planDaysToCreate(
        startDateMs:    Long,
        endDateMs:      Long,
        existingDayIds: Set<String>,
    ): List<DayToCreate> {
        if (startDateMs <= 0L || endDateMs <= 0L || endDateMs < startDateMs) return emptyList()

        val dayCount = (((endDateMs - startDateMs) / MS_PER_DAY) + 1L)
            .coerceAtMost(MAX_DAY_COUNT)
            .toInt()

        return (0 until dayCount).mapNotNull { offset ->
            val dayNumber = offset + 1
            val dayId     = "day_$dayNumber"
            if (dayId in existingDayIds) null
            else DayToCreate(
                dayId     = dayId,
                dayNumber = dayNumber,
                dateMs    = startDateMs + offset * MS_PER_DAY,
            )
        }
    }
}
