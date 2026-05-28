package com.domedemok.travelplanner.data.repository.rules

import com.domedemok.travelplanner.data.model.Place

/**
 * Pure decision logic for [com.domedemok.travelplanner.data.repository.PlaceRepository].
 *
 * Concerns: which fields persist on add, which fields are mutable on update,
 * and which fields are intentionally omitted (e.g. `distanceMeters` is
 * ephemeral search-time data and should never be written to Firestore).
 *
 * Parsing back from Firestore stays platform-specific because the JS wrapper
 * needs dynamic-JS access for 64-bit Long fields, while Android can use the
 * typed accessors directly.
 */
object PlaceBusinessRules {

    /**
     * Field-shape for `trips/{tripId}/places/{id}` on insert.
     *
     * `distanceMeters` is intentionally omitted — it is a search-result-only
     * field meaningful for the user's current location, not for the saved
     * place. Persisting it would lock in a stale value.
     *
     * @param place       The user-supplied place data.
     * @param addedByUid  uid of the authenticated user; "" if unauthenticated
     *                    (the caller should normally have rejected this).
     */
    fun buildAddPayload(place: Place, addedByUid: String): Map<String, Any> = mapOf(
        "name"         to place.name,
        "address"      to place.address,
        "latitude"     to place.latitude,
        "longitude"    to place.longitude,
        "notes"        to place.notes,
        "category"     to place.category,
        "foursquareId" to place.foursquareId,
        "addedAt"      to place.addedAt,
        "addedBy"      to addedByUid,
        // Enrichment fields — may be 0 / empty for manually-added places.
        "rating"       to place.rating,
        "popularity"   to place.popularity,
        "photoUrl"     to place.photoUrl,
    )

    /**
     * Field-shape for `trips/{tripId}/places/{id}` on update.
     *
     * Intentionally omits `addedAt`, `addedBy`, `foursquareId`, and the
     * enrichment fields:
     *  - `addedAt` is the sort key and must not change.
     *  - `addedBy` is provenance — only the original creator is recorded.
     *  - Enrichment fields come from Foursquare and are managed separately;
     *    a user edit must not silently overwrite them.
     */
    fun buildUpdatePayload(place: Place): Map<String, Any> = mapOf(
        "name"      to place.name,
        "address"   to place.address,
        "latitude"  to place.latitude,
        "longitude" to place.longitude,
        "notes"     to place.notes,
        "category"  to place.category,
    )
}
