package com.domedemok.travelplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domedemok.travelplanner.data.model.ItineraryDay
import com.domedemok.travelplanner.data.model.Place
import com.domedemok.travelplanner.data.repository.ItineraryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItineraryUiState(
    val days: List<ItineraryDay> = emptyList(),
    val savedPlaces: List<Place> = emptyList(), // all places saved to the trip
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ItineraryViewModel(
    private val itineraryRepository: ItineraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItineraryUiState())
    val uiState: StateFlow<ItineraryUiState> = _uiState.asStateFlow()

    private var daysJob: Job? = null

    // ── Load ──────────────────────────────────────────────────────────────────

    /**
     * Starts listening to the day stream for [tripId].
     * If the trip has valid dates, also bootstraps the day documents so the
     * user sees the right number of day slots immediately.
     */
    fun load(
        tripId: String,
        startDate: Long,
        endDate: Long,
        savedPlaces: List<Place>,
    ) {
        _uiState.update { it.copy(savedPlaces = savedPlaces) }

        // Bootstrap day documents if dates are set.
        if (startDate > 0L && endDate > 0L) {
            viewModelScope.launch {
                itineraryRepository.ensureDaysExist(tripId, startDate, endDate)
                    .onFailure { e ->
                        _uiState.update { it.copy(error = "Could not create day slots: ${e.message}") }
                    }
            }
        }

        daysJob?.cancel()
        daysJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            itineraryRepository.getDaysFlow(tripId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load itinerary") }
                }
                .collect { days ->
                    _uiState.update { it.copy(days = days, isLoading = false) }
                }
        }
    }

    /** Called from TripDetailScreen whenever the saved-places list changes. */
    fun updateSavedPlaces(places: List<Place>) {
        _uiState.update { it.copy(savedPlaces = places) }
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Reorders a place within the same day (drag-and-drop result).
     * Applies an optimistic update immediately, then persists to Firestore.
     */
    fun reorderInDay(tripId: String, dayId: String, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val day = _uiState.value.days.find { it.id == dayId } ?: return

        val newIds = day.placeIds.toMutableList().apply {
            add(toIndex.coerceIn(0, size), removeAt(fromIndex.coerceIn(0, lastIndex)))
        }

        applyDayUpdate(dayId, newIds)

        viewModelScope.launch {
            itineraryRepository.updateDayPlaces(tripId, dayId, newIds)
                .onFailure { e ->
                    applyDayUpdate(dayId, day.placeIds) // rollback
                    _uiState.update { it.copy(error = e.message ?: "Failed to reorder") }
                }
        }
    }

    /** Adds a place to a day from the unassigned pool. */
    fun addPlaceToDay(tripId: String, dayId: String, placeId: String) {
        val day = _uiState.value.days.find { it.id == dayId } ?: return
        if (placeId in day.placeIds) return

        val newIds = day.placeIds + placeId
        applyDayUpdate(dayId, newIds)

        viewModelScope.launch {
            itineraryRepository.updateDayPlaces(tripId, dayId, newIds)
                .onFailure { e ->
                    applyDayUpdate(dayId, day.placeIds)
                    _uiState.update { it.copy(error = e.message ?: "Failed to add place") }
                }
        }
    }

    /** Removes a place from a day (returns it to the unassigned pool). */
    fun removePlaceFromDay(tripId: String, dayId: String, placeId: String) {
        val day = _uiState.value.days.find { it.id == dayId } ?: return

        val newIds = day.placeIds.filter { it != placeId }
        applyDayUpdate(dayId, newIds)

        viewModelScope.launch {
            itineraryRepository.updateDayPlaces(tripId, dayId, newIds)
                .onFailure { e ->
                    applyDayUpdate(dayId, day.placeIds)
                    _uiState.update { it.copy(error = e.message ?: "Failed to remove place") }
                }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun applyDayUpdate(dayId: String, newIds: List<String>) {
        _uiState.update { state ->
            state.copy(
                days = state.days.map { day ->
                    if (day.id == dayId) day.copy(placeIds = newIds) else day
                }
            )
        }
    }
}
