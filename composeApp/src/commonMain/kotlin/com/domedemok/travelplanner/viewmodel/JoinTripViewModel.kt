package com.domedemok.travelplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domedemok.travelplanner.data.repository.JoinTripResult
import com.domedemok.travelplanner.data.repository.TripRepository
import com.domedemok.travelplanner.util.JOIN_CODE_LENGTH
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Distinct failure modes surfaced in the UI layer.
 * The repository returns a typed [com.domedemok.travelplanner.data.repository.JoinTripResult];
 * this enum mirrors those cases for consumption by the Composable screen.
 */
sealed class JoinError {
    /** The code didn't pass the 6-character format check or no trip matched it. */
    object InvalidCode : JoinError()

    /** The current user is already a member (or the owner) of the matched trip. */
    object AlreadyMember : JoinError()

    /** Anything else — network failure, Firestore rule violation, parse error. */
    data class Unknown(val message: String) : JoinError()
}

data class JoinTripUiState(
    val isLoading: Boolean = false,
    val error: JoinError? = null,
    /** Non-null after a successful join; the UI navigates to this trip. */
    val joinedTripId: String? = null,
)

class JoinTripViewModel(
    private val tripRepository: TripRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinTripUiState())
    val uiState: StateFlow<JoinTripUiState> = _uiState.asStateFlow()

    fun joinByCode(code: String) {
        val clean = code.trim().uppercase()
        if (clean.length != JOIN_CODE_LENGTH) {
            _uiState.value = _uiState.value.copy(error = JoinError.InvalidCode)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            _uiState.value = when (val result = tripRepository.joinTripByCode(clean)) {
                is JoinTripResult.Success     -> _uiState.value.copy(isLoading = false, joinedTripId = result.trip.id)
                is JoinTripResult.InvalidCode  -> _uiState.value.copy(isLoading = false, error = JoinError.InvalidCode)
                is JoinTripResult.AlreadyMember -> _uiState.value.copy(isLoading = false, error = JoinError.AlreadyMember)
                is JoinTripResult.Failure      -> _uiState.value.copy(isLoading = false, error = JoinError.Unknown(result.cause.message ?: "unknown_error"))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

}
