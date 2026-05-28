package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.Trip

/**
 * Typed result for [TripRepository.joinTripByCode].
 *
 * Replaces the previous `Result<Trip>` with stringly-typed error messages
 * ("invalid_code", "already_member") that had to be string-matched in the
 * ViewModel layer. Callers now use an exhaustive `when` expression.
 */
sealed class JoinTripResult {
    data class Success(val trip: Trip)       : JoinTripResult()
    object InvalidCode                       : JoinTripResult()
    object AlreadyMember                     : JoinTripResult()
    data class Failure(val cause: Throwable) : JoinTripResult()
}
