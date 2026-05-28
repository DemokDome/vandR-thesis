package com.domedemok.travelplanner.data.remote

import com.domedemok.travelplanner.data.model.Place
import com.domedemok.travelplanner.util.Logger
import com.domedemok.travelplanner.util.getCurrentTimestamp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.http.URLBuilder

/**
 * Foursquare Places API client — search and geocoding for the v3 endpoint.
 *
 * Design notes:
 * - We intentionally do NOT send a `fields` parameter for general search.
 *   Restricting fields makes requests fragile: any field unavailable on the
 *   current plan tier returns HTTP 400. Letting the server pick its default
 *   field set is more resilient; every enrichment field on [Place] has a
 *   null-safe fallback.
 * - `sort` is sent for both modes: POPULARITY for browse, RELEVANCE for text.
 * - The API key is sanitised once at construction time and never re-derived.
 */
class FoursquareService(
    private val httpClient: HttpClient,
) {
    // ── Constants ────────────────────────────────────────────────────────────

    companion object {
        /** Foursquare's hard server-side cap on `limit` for the search endpoint. */
        const val MAX_LIMIT = 50

        private const val SEARCH_ENDPOINT       = "search"
        private const val PHOTO_SIZE            = "500x500"  // inserted between Foursquare's photo prefix/suffix
        private const val DEFAULT_CATEGORY_NAME = "Point of Interest"

        // Geocoding only — venue lookups don't need the full enrichment payload.
        private const val GEOCODE_FIELDS = "fsq_place_id,latitude,longitude,location"
    }

    // Strip wrapping quotes / line breaks that may sneak in from `local.properties`.
    private val apiKey: String = FoursquareConfig.API_KEY
        .trim()
        .replace("\"", "")
        .replace("\n", "")
        .replace("\r", "")

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Searches venues near ([latitude], [longitude]).
     *
     * Sort strategy:
     *  - With non-blank [query] → RELEVANCE (best textual match first).
     *  - Without query          → POPULARITY (best-known spots first).
     *
     * [limit] is clamped to `1..MAX_LIMIT` (Foursquare's hard cap).
     * [categoryId] accepts a comma-separated list (see [FoursquareCategories]).
     */
    suspend fun searchPlaces(
        query:      String,
        latitude:   Double,
        longitude:  Double,
        categoryId: String? = null,
        limit:      Int     = 20,
    ): Result<List<Place>> = runCatching {
        val params = buildMap {
            put("ll",    "$latitude,$longitude")
            put("limit", limit.coerceIn(1, MAX_LIMIT).toString())
            put("sort",  if (query.isNotBlank()) "RELEVANCE" else "POPULARITY")
            if (query.isNotBlank())  put("query",            query)
            if (categoryId != null)  put("fsq_category_ids", categoryId)
        }

        getJson<FoursquareSearchResponse>(SEARCH_ENDPOINT, params)
            .results
            .mapNotNull { it.toPlace(latitude, longitude) }

    }.onFailure { logFailure("searchPlaces", it) }

    /**
     * Resolves a free-text [address] to (latitude, longitude).
     * Returns [Result.failure] if the city cannot be resolved — the caller is
     * responsible for showing a user-facing error rather than silently falling
     * back to a hard-coded default.
     */
    suspend fun geocodeAddress(address: String): Result<Pair<Double, Double>> = runCatching {
        val params = mapOf(
            "near"   to address,
            "limit"  to "1",
            "fields" to GEOCODE_FIELDS,
        )

        val response = getJson<FoursquareSearchResponse>(SEARCH_ENDPOINT, params)
        val centre   = response.context?.geoBounds?.circle?.center
        val first    = response.results.firstOrNull()

        when {
            centre != null -> centre.latitude to centre.longitude
            first?.latitude != null && first.longitude != null ->
                first.latitude to first.longitude
            else -> error("Could not resolve location: \"$address\"")
        }

    }.onFailure { logFailure("geocodeAddress(\"$address\")", it) }

    // ── HTTP plumbing ────────────────────────────────────────────────────────

    private fun buildUrl(endpoint: String, params: Map<String, String>): String {
        val direct = URLBuilder("${FoursquareConfig.BASE_URL}$endpoint").apply {
            params.forEach { (k, v) -> parameters.append(k, v) }
        }.buildString()
        // Browser fetch is CORS-restricted on JS — see FoursquareConfig.USE_CORS_PROXY.
        return if (USE_CORS_PROXY) "${FoursquareConfig.CORS_PROXY}$direct" else direct
    }

    private suspend inline fun <reified T> getJson(
        endpoint: String,
        params:   Map<String, String>,
    ): T = httpClient.get(buildUrl(endpoint, params)) {
        headers.append("Authorization",        "Bearer $apiKey")
        headers.append("Accept",               "application/json")
        headers.append("X-Places-Api-Version", FoursquareConfig.API_VERSION)
    }.body()

    private fun logFailure(operation: String, e: Throwable) {
        val detail = when (e) {
            is ClientRequestException  ->
                "HTTP ${e.response.status.value} — check API key / plan limits"
            is ServerResponseException ->
                "HTTP ${e.response.status.value} — Foursquare server error"
            else -> e.message ?: "Unknown error"
        }
        Logger.e("FoursquareService", "$operation failed: $detail")
    }

    // ── Venue → Place mapper ─────────────────────────────────────────────────

    /**
     * Maps a raw API venue onto our [Place] domain model.
     * Returns null only when the venue lacks the minimum required fields
     * (stable ID and display name) — caller filters with `mapNotNull`.
     */
    private fun FoursquareVenue.toPlace(fallbackLat: Double, fallbackLon: Double): Place? {
        val venueId     = fsqPlaceId ?: return null
        val displayName = name       ?: return null

        val resolvedAddress = location?.formattedAddress
            ?: location?.address
            ?: buildString {
                location?.locality?.let { append("$it, ") }
                location?.country?.let  { append(it) }
            }.trim().trimEnd(',')

        // Only build a photo URL when both prefix and suffix are present.
        val photoUrl = photos
            .firstOrNull { p -> p.prefix != null && p.suffix != null }
            ?.let { p -> "${p.prefix}$PHOTO_SIZE${p.suffix}" }
            .orEmpty()

        return Place(
            name           = displayName,
            address        = resolvedAddress,
            latitude       = latitude  ?: fallbackLat,
            longitude      = longitude ?: fallbackLon,
            notes          = description.orEmpty(),
            category       = categories.firstOrNull()?.name ?: DEFAULT_CATEGORY_NAME,
            foursquareId   = venueId,
            addedAt        = getCurrentTimestamp(),
            rating         = rating     ?: 0.0,
            popularity     = popularity ?: 0.0,
            photoUrl       = photoUrl,
            distanceMeters = distance   ?: 0,
        )
    }
}
