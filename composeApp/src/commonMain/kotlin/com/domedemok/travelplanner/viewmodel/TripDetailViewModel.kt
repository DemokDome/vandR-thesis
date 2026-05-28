package com.domedemok.travelplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domedemok.travelplanner.data.model.Trip
import com.domedemok.travelplanner.data.repository.TripRepository
import com.domedemok.travelplanner.util.SecurityValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TripDetailUiState(
    val isLoading: Boolean = true,
    val trip: Trip? = null,
    val error: String? = null,
    val isEditing: Boolean = false,
    val isSharing: Boolean = false,
    val isDeleting: Boolean = false,
    val resolvedJoinCode: String = "",   // populated on demand by ensureJoinCode()
)

class TripDetailViewModel(
    private val tripRepository: TripRepository
) : ViewModel() {

    val currentUserId: String?
        get() = tripRepository.currentUserId

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    fun loadTrip(tripId: String, showLoading: Boolean = true) {
        // Reset synchronously so stale trip data from a previous trip is never
        // visible in the composition between navigation and the coroutine starting.
        if (showLoading) {
            _uiState.value = _uiState.value.copy(isLoading = true, trip = null, error = null)
        }
        viewModelScope.launch {
            tripRepository.getTripById(tripId)
                .onSuccess { trip ->
                    _uiState.value = _uiState.value.copy(isLoading = false, trip = trip)
                    // Stamp the trip with the current time so Discover can show the
                    // most recently viewed trip, even if no edits were made.
                    tripRepository.updateLastOpened(tripId)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load trip"
                    )
                }
        }
    }


    fun updateTrip(name: String, description: String, startDate: Long, endDate: Long) {
        val currentTrip = _uiState.value.trip ?: return

        val cleanName        = SecurityValidator.sanitize(name)
        val cleanDescription = SecurityValidator.sanitize(description)

        SecurityValidator.validateTripName(cleanName)?.let {
            _uiState.value = _uiState.value.copy(error = it)
            return
        }
        SecurityValidator.validateTripDescription(cleanDescription)?.let {
            _uiState.value = _uiState.value.copy(error = it)
            return
        }
        if (startDate > 0L && endDate > 0L && endDate < startDate) {
            _uiState.value = _uiState.value.copy(error = "The trip cannot end before it starts.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEditing = true, error = null)

            val updatedTrip = currentTrip.copy(
                name = cleanName,
                description = cleanDescription,
                startDate = startDate,
                endDate = endDate
            )

            tripRepository.updateTrip(updatedTrip)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isEditing = false,
                        trip = updatedTrip
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isEditing = false,
                        error = exception.message ?: "Failed to update trip"
                    )
                }
        }
    }

    /**
     * Persists a destination change that the user made inside the Places tab.
     * Only updates the `destination` field — all other trip data is left intact.
     */
    fun updateDestination(destination: String) {
        val currentTrip = _uiState.value.trip ?: return
        val cleanDestination = destination.trim()

        viewModelScope.launch {
            val updatedTrip = currentTrip.copy(destination = cleanDestination)
            tripRepository.updateTrip(updatedTrip)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(trip = updatedTrip)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to save destination"
                    )
                }
        }
    }

    fun leaveTrip(onSuccess: () -> Unit) {
        val tripId = _uiState.value.trip?.id ?: return

        viewModelScope.launch {
            // Reuses `isDeleting` because the UI flow ends in the same screen exit as a delete.
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)

            tripRepository.leaveTrip(tripId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isDeleting = false)
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        error = e.message ?: "Failed to leave trip"
                    )
                }
        }
    }

    fun removeMember(userIdToRemove: String) {
        val tripId = _uiState.value.trip?.id ?: return
        val currentTrip = _uiState.value.trip ?: return
        // Optimistic: move the member from active to former so the UI reacts
        // immediately while still preserving their name on historical splits.
        val removedName = currentTrip.tripMembers[userIdToRemove]
        val optimisticTrip = currentTrip.copy(
            tripMembers   = currentTrip.tripMembers - userIdToRemove,
            formerMembers = if (removedName != null)
                currentTrip.formerMembers + (userIdToRemove to removedName)
            else
                currentTrip.formerMembers,
        )
        _uiState.value = _uiState.value.copy(trip = optimisticTrip)
        viewModelScope.launch {
            tripRepository.removeMemberFromTrip(tripId, userIdToRemove)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(trip = currentTrip, error = e.message ?: "Failed to remove member")
                }
        }
    }

    fun renameMember(userId: String, newName: String) {
        val tripId = _uiState.value.trip?.id ?: return
        val clean  = SecurityValidator.sanitize(newName).trim()
        if (clean.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Name cannot be empty.")
            return
        }
        if (clean.length > 50) {
            _uiState.value = _uiState.value.copy(error = "Name is too long (max 50 characters).")
            return
        }
        val currentTrip = _uiState.value.trip ?: return
        // Optimistic: apply the new name locally right away.
        val optimisticTrip = currentTrip.copy(
            tripMembers = currentTrip.tripMembers + (userId to clean),
        )
        _uiState.value = _uiState.value.copy(trip = optimisticTrip)
        viewModelScope.launch {
            tripRepository.renameMemberInTrip(tripId, userId, clean)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(trip = currentTrip, error = e.message ?: "Failed to rename member")
                }
        }
    }

    fun shareTrip(userEmail: String) {
        val currentTrip = _uiState.value.trip ?: return

        val cleanEmail = SecurityValidator.sanitize(userEmail)
        SecurityValidator.validateEmail(cleanEmail)?.let {
            _uiState.value = _uiState.value.copy(error = it)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSharing = true, error = null)
            tripRepository.shareTrip(currentTrip.id, cleanEmail)
                .onSuccess {
                    loadTrip(currentTrip.id)
                    _uiState.value = _uiState.value.copy(isSharing = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSharing = false,
                        error = exception.message ?: "Failed to share trip"
                    )
                }
        }
    }

    fun deleteTrip(onSuccess: () -> Unit) {
        val currentTrip = _uiState.value.trip ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)

            tripRepository.deleteTrip(currentTrip.id)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        error = exception.message ?: "Failed to delete trip"
                    )
                }
        }
    }

    /**
     * Ensures the trip has a join code (generates one if missing) and stores it
     * in [TripDetailUiState.resolvedJoinCode] so the ShareTripDialog can display it immediately.
     */
    fun ensureJoinCode() {
        val tripId = _uiState.value.trip?.id ?: return
        // If the trip already carries a code, use it directly.
        val existing = _uiState.value.trip?.joinCode.orEmpty()
        if (existing.isNotBlank()) {
            _uiState.value = _uiState.value.copy(resolvedJoinCode = existing)
            return
        }
        viewModelScope.launch {
            tripRepository.ensureJoinCode(tripId)
                .onSuccess { code ->
                    _uiState.value = _uiState.value.copy(resolvedJoinCode = code)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to get join code")
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}