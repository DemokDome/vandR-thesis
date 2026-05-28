package com.domedemok.travelplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domedemok.travelplanner.data.model.Place
import com.domedemok.travelplanner.data.remote.FoursquareCategories
import com.domedemok.travelplanner.data.remote.FoursquareService
import com.domedemok.travelplanner.data.repository.PlaceRepository
import com.domedemok.travelplanner.util.SecurityValidator
import com.domedemok.travelplanner.util.getCurrentTimestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaceUiState(
    val places:            List<Place> = emptyList(),
    val searchResults:     List<Place> = emptyList(),
    val isLoading:         Boolean     = false,
    val isSearching:       Boolean     = false,
    val isSaving:          Boolean     = false,
    val isDeleting:        Boolean     = false,
    val error:             String?     = null,
    val searchQuery:       String      = "",
    val locationQuery:     String      = "",
    val currentLatitude:   Double      = 0.0,
    val currentLongitude:  Double      = 0.0,
    val selectedCategoryId: String?    = null,
    // Foursquare limit-based pagination. Bumped in 20-step increments by
    // [PlaceViewModel.loadMoreSearchResults]; capped at FOURSQUARE_MAX_LIMIT.
    val searchLimit:       Int         = INITIAL_SEARCH_LIMIT,
    val canLoadMore:       Boolean     = false,
) {
    companion object {
        const val INITIAL_SEARCH_LIMIT = 20
        const val SEARCH_PAGE_STEP     = 20
        /** Mirrors [FoursquareService.MAX_LIMIT] — the hard server-side cap on a single search. */
        const val FOURSQUARE_MAX_LIMIT = FoursquareService.MAX_LIMIT
    }
}

class PlaceViewModel(
    private val placeRepository: PlaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaceUiState())
    val uiState: StateFlow<PlaceUiState> = _uiState.asStateFlow()

    private var placesJob: Job? = null
    private var currentTripId: String = ""

    // ── Saved-places stream ──────────────────────────────────────────────────

    fun loadPlaces(tripId: String, initialLocation: String = "") {
        placesJob?.cancel()
        val tripChanged = currentTripId != tripId
        currentTripId = tripId

        // On trip switch, wipe transient state so stale emissions from the cancelled
        // previous flow can't flash on screen. Same-trip reloads keep the existing list
        // visible — the Firestore snapshot will replace it in-place without flicker.
        if (tripChanged) {
            _uiState.update {
                it.copy(
                    locationQuery      = "",
                    places             = emptyList(),
                    searchResults      = emptyList(),
                    searchQuery        = "",
                    selectedCategoryId = null,
                    error              = null,
                )
            }
        }

        // Pre-fill from the trip's destination field if available.
        if (initialLocation.isNotBlank() && _uiState.value.locationQuery.isBlank()) {
            _uiState.update { it.copy(locationQuery = initialLocation) }
        }
        // If still blank, try loading the last used location from local DB for this trip.
        if (_uiState.value.locationQuery.isBlank()) {
            viewModelScope.launch {
                val saved = placeRepository.getLastSearchLocation(tripId)
                // Only apply if we haven't switched to a different trip while the DB was loading.
                if (!saved.isNullOrBlank() && currentTripId == tripId) {
                    _uiState.update { it.copy(locationQuery = saved) }
                }
            }
        }
        placesJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            placeRepository.getPlacesForTripFlow(tripId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load places") }
                }
                .collect { places ->
                    // Guard against late emissions from a cancelled previous flow.
                    if (currentTripId == tripId) {
                        _uiState.update { it.copy(places = places, isLoading = false) }
                    }
                }
        }
    }

    // ── Search state mutators ────────────────────────────────────────────────

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = SecurityValidator.sanitize(query)) }
    }

    fun updateLocationQuery(query: String) {
        _uiState.update { it.copy(locationQuery = SecurityValidator.sanitize(query)) }
    }

    fun setCategoryFilter(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    // ── Search ───────────────────────────────────────────────────────────────

    /**
     * Executes a Foursquare search using the current [PlaceUiState]:
     *  1. Geocode [PlaceUiState.locationQuery] to coordinates (required — returns early if blank).
     *  2. Apply implicit category when no query text and no explicit filter:
     *     browse-mode defaults to Sights + Museums (most universally useful).
     *  3. Fetch results and expose them via [PlaceUiState.searchResults].
     *
     * All errors are surfaced through [PlaceUiState.error] so the UI can show
     * them inside the search sheet rather than behind it.
     */
    fun searchPlaces() {
        // A fresh search always resets pagination to the initial page size.
        _uiState.update { it.copy(searchLimit = PlaceUiState.INITIAL_SEARCH_LIMIT) }
        runSearch()
    }

    /** Bumps the limit by SEARCH_PAGE_STEP (capped at FOURSQUARE_MAX_LIMIT) and re-runs the search. */
    fun loadMoreSearchResults() {
        val current = _uiState.value.searchLimit
        if (current >= PlaceUiState.FOURSQUARE_MAX_LIMIT) return
        val next = (current + PlaceUiState.SEARCH_PAGE_STEP)
            .coerceAtMost(PlaceUiState.FOURSQUARE_MAX_LIMIT)
        _uiState.update { it.copy(searchLimit = next) }
        runSearch()
    }

    private fun runSearch() {
        val query      = _uiState.value.searchQuery.trim()
        val location   = _uiState.value.locationQuery.trim()
        val categoryId = _uiState.value.selectedCategoryId
        val limit      = _uiState.value.searchLimit

        if (location.isEmpty()) {
            _uiState.update { it.copy(error = "Set a destination first.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }

            // Step 1 — resolve city name to coordinates.
            val geocodeResult = placeRepository.geocodeAddress(location)

            if (geocodeResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        error = "Could not find location \"$location\". " +
                                "Try a different city name.",
                    )
                }
                return@launch
            }

            val (latitude, longitude) = geocodeResult.getOrThrow()
            _uiState.update { it.copy(currentLatitude = latitude, currentLongitude = longitude) }

            // Persist the location so it is pre-filled next time this trip is opened.
            if (currentTripId.isNotBlank()) {
                placeRepository.saveSearchLocation(currentTripId, location)
            }

            // Step 2 — determine effective category filter.
            val effectiveCategory = when {
                categoryId != null -> categoryId
                query.isEmpty() ->
                    // Browse mode: show popular sights and museums by default.
                    "${FoursquareCategories.SIGHTS},${FoursquareCategories.MUSEUMS}"
                else -> null // Free-text query — let Foursquare decide relevance.
            }

            // Step 3 — search.
            placeRepository.searchPlaces(query, latitude, longitude, effectiveCategory, limit)
                .onSuccess { results ->
                    // Foursquare returns up to `limit` items. We can ask for more iff
                    // we're still under the API cap AND the server actually filled the
                    // current page (a short page means there are no more results to fetch).
                    val canLoadMore = limit < PlaceUiState.FOURSQUARE_MAX_LIMIT &&
                            results.size >= limit
                    _uiState.update {
                        it.copy(
                            searchResults = results,
                            isSearching   = false,
                            canLoadMore   = canLoadMore,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            error = "Search failed: ${e.message ?: "Unknown error"}",
                        )
                    }
                }
        }
    }

    // ── Place mutations ──────────────────────────────────────────────────────

    fun addPlace(tripId: String, place: Place) {
        val safePlace = sanitizePlace(place) ?: return
        // Stamp addedAt here — Foursquare search results arrive with addedAt = 0.
        val now = getCurrentTimestamp()
        val placeWithTime = safePlace.copy(addedAt = now)
        val tempId = "opt_$now"
        val optimistic = placeWithTime.copy(id = tempId, tripId = tripId)
        _uiState.update { it.copy(places = it.places + optimistic, isSaving = true, error = null) }
        viewModelScope.launch {
            placeRepository.addPlaceToTrip(tripId, placeWithTime)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        state.copy(
                            places   = state.places.filter { it.id != tempId },
                            isSaving = false,
                            error    = e.message ?: "Failed to add place",
                        )
                    }
                }
        }
    }

    fun updatePlace(tripId: String, place: Place) {
        val safePlace = sanitizePlace(place) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            placeRepository.updatePlace(tripId, safePlace)
                .onSuccess { _uiState.update { it.copy(isSaving = false) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, error = e.message ?: "Failed to update place")
                    }
                }
        }
    }

    fun deletePlace(tripId: String, placeId: String) {
        // Optimistic: remove from the list immediately; restore on failure.
        var backup: Place? = null
        _uiState.update { state ->
            backup = state.places.find { it.id == placeId }
            state.copy(places = state.places.filter { it.id != placeId })
        }
        viewModelScope.launch {
            placeRepository.deletePlace(tripId, placeId)
                .onFailure { e ->
                    backup?.let { removed ->
                        _uiState.update { state ->
                            state.copy(
                                places = state.places + removed,
                                error  = e.message ?: "Failed to delete place",
                            )
                        }
                    }
                }
        }
    }

    // ── Reset ────────────────────────────────────────────────────────────────

    /** Called when the search sheet is dismissed. Clears all transient search state. */
    fun clearSearchResults() {
        _uiState.update { it.copy(
            searchResults      = emptyList(),
            searchQuery        = "",
            selectedCategoryId = null,   // ← also reset category so next open starts fresh
            searchLimit        = PlaceUiState.INITIAL_SEARCH_LIMIT,
            canLoadMore        = false,
        ) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Sanitises a place's user-editable text fields; returns null and sets an error on failure. */
    private fun sanitizePlace(place: Place): Place? {
        val cleanName = SecurityValidator.sanitize(place.name)
        val cleanNote = SecurityValidator.sanitize(place.notes)

        if (cleanName.isBlank()) {
            _uiState.update { it.copy(error = "Place name cannot be empty.") }
            return null
        }

        SecurityValidator.validateNote(cleanNote)?.let { msg ->
            _uiState.update { it.copy(error = msg) }
            return null
        }

        return place.copy(name = cleanName, notes = cleanNote)
    }
}
