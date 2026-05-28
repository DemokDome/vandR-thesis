package com.domedemok.travelplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domedemok.travelplanner.data.model.Trip
import com.domedemok.travelplanner.data.repository.AuthRepository
import com.domedemok.travelplanner.data.repository.TripRepository
import com.domedemok.travelplanner.util.SecurityValidator
import com.domedemok.travelplanner.util.getCurrentTimestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TripUiState(
    val isLoading: Boolean = false,
    val trips: List<Trip> = emptyList(),
    val favoriteTripIds: Set<String> = emptySet(),
    val currentUserId: String? = null,
    val error: String? = null,
    val isCreatingTrip: Boolean = false
)

class TripViewModel(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripUiState())
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    /**
     * Outstanding subscription to the trips Firestore listener for the current user.
     * Re-assigned (and the previous one cancelled) whenever the auth uid changes,
     * so a sign-in race that started the listener with a missing token gets a
     * fresh, authenticated listener as soon as the token lands.
     */
    private var tripsJob: Job? = null

    init {
        // Observe auth state instead of reading currentUserId once at init: that
        // racy path produced an empty / failed Firestore listener whenever the
        // ViewModel was instantiated before Firebase Auth finished propagating
        // (typical right after sign-in / sign-up), and the listener never
        // recovered until the user restarted the app.
        viewModelScope.launch {
            authRepository.getAuthState()
                .map { it?.id }
                .distinctUntilChanged()
                .collect { userId ->
                    _uiState.update { it.copy(currentUserId = userId) }
                    tripsJob?.cancel()
                    if (userId == null) {
                        _uiState.update {
                            it.copy(isLoading = false, trips = emptyList(), favoriteTripIds = emptySet())
                        }
                    } else {
                        tripsJob = launchTripsListener(userId)
                    }
                }
        }
    }

    private fun launchTripsListener(userId: String): Job {
        // Clear any stale data from a previous user before the new flow emits.
        _uiState.update { it.copy(isLoading = true, trips = emptyList(), favoriteTripIds = emptySet()) }

        return viewModelScope.launch {
            // Load favorites exactly once before the real-time flow starts.
            val initialFavorites = tripRepository.getFavoriteTripIds().getOrNull() ?: emptySet()
            _uiState.update { it.copy(favoriteTripIds = initialFavorites) }

            tripRepository.getTripsFlow()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { trips ->
                    // If the auth uid changed mid-flight, ignore stale emissions —
                    // the auth-state collector above will start a fresh listener.
                    if (tripRepository.currentUserId != userId) return@collect

                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            trips = sortTrips(trips, currentState.favoriteTripIds),
                        )
                    }
                }
        }
    }

    private fun sortTrips(trips: List<Trip>, favorites: Set<String>): List<Trip> {
        val currentTime = getCurrentTimestamp()
        return trips.sortedWith(
            compareByDescending<Trip> { favorites.contains(it.id) }
                .thenBy { it.endDate < currentTime && it.endDate != 0L }
                .thenBy { if (it.startDate == 0L) Long.MAX_VALUE else it.startDate }
        )
    }

    fun toggleFavorite(tripId: String, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch {
            val newFavoriteState = !isCurrentlyFavorite

            // Optimistic UI update — re-sort right away so the row jumps into the favorites section.
            _uiState.update { currentState ->
                val currentFavorites = currentState.favoriteTripIds
                val updatedFavorites = if (newFavoriteState) currentFavorites + tripId else currentFavorites - tripId

                currentState.copy(
                    favoriteTripIds = updatedFavorites,
                    trips = sortTrips(currentState.trips, updatedFavorites)
                )
            }

            tripRepository.toggleFavorite(tripId, newFavoriteState)
                .onFailure { exception ->
                    // Rollback on failure: revert the favorites set and re-sort.
                    _uiState.update { currentState ->
                        val fallbackFavorites = if (isCurrentlyFavorite) currentState.favoriteTripIds + tripId else currentState.favoriteTripIds - tripId
                        currentState.copy(
                            favoriteTripIds = fallbackFavorites,
                            trips = sortTrips(currentState.trips, fallbackFavorites),
                            error = exception.message ?: "Failed to update favorite"
                        )
                    }
                }
        }
    }

    fun createTrip(name: String, destination: String, description: String, startDate: Long, endDate: Long) {
        val cleanName        = SecurityValidator.sanitize(name)
        val cleanDestination = SecurityValidator.sanitize(destination)
        val cleanDescription = SecurityValidator.sanitize(description)

        SecurityValidator.validateTripName(cleanName)?.let {
            _uiState.update { state -> state.copy(error = it) }
            return
        }
        SecurityValidator.validateTripDescription(cleanDescription)?.let {
            _uiState.update { state -> state.copy(error = it) }
            return
        }
        if (startDate > 0L && endDate > 0L && endDate < startDate) {
            _uiState.update { it.copy(error = "The trip cannot end before it starts.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingTrip = true, error = null) }

            val trip = Trip(
                id = "",
                name = cleanName,
                description = cleanDescription,
                destination = cleanDestination,
                createdBy = "",
                startDate = startDate,
                endDate = endDate,
                createdAt = getCurrentTimestamp(),
                updatedAt = getCurrentTimestamp()
            )

            tripRepository.createTrip(trip)
                .onSuccess {
                    _uiState.update { it.copy(isCreatingTrip = false) }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isCreatingTrip = false,
                            error = exception.message ?: "Failed to create trip"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

}
