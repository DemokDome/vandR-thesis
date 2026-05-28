package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.ItineraryDay
import kotlinx.coroutines.flow.Flow

interface ItineraryRepository {

    /** Real-time stream of days for a trip, sorted ascending by [ItineraryDay.dayNumber]. */
    fun getDaysFlow(tripId: String): Flow<List<ItineraryDay>>

    /**
     * Replaces the ordered [placeIds] list for a single day document.
     * This is the only mutation needed — reorder, add, and remove all go through here.
     */
    suspend fun updateDayPlaces(
        tripId: String,
        dayId: String,
        placeIds: List<String>,
    ): Result<Unit>

    /**
     * Creates a day document for every calendar day between [startDate] and [endDate]
     * (inclusive, epoch-ms).  Documents that already exist are left untouched —
     * safe to call on every screen open.
     */
    suspend fun ensureDaysExist(
        tripId: String,
        startDate: Long,
        endDate: Long,
    ): Result<Unit>
}
