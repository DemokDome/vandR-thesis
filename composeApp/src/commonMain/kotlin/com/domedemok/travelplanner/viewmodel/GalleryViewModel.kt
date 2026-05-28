package com.domedemok.travelplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domedemok.travelplanner.data.model.TripPhoto
import com.domedemok.travelplanner.data.repository.PhotoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

data class GalleryUiState(
    val photos:      List<TripPhoto> = emptyList(),
    val isLoading:   Boolean         = true,
    val isUploading: Boolean         = false,
    val error:       String?         = null,
)

class GalleryViewModel(
    private val photoRepository: PhotoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    // Deduplication: cancel old listener before starting a new one
    private var collectJob: Job? = null
    private var currentTripId: String? = null

    // Atomic upload guard: prevents concurrent uploads from double-tap / rapid calls
    private val uploadMutex = Mutex()

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadPhotos(tripId: String) {
        if (tripId == currentTripId) return   // already streaming this trip — no-op
        currentTripId = tripId
        collectJob?.cancel()                  // cancel previous Firestore listener
        collectJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            photoRepository.getPhotosFlow(tripId)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { photos ->
                    _uiState.update { it.copy(photos = photos, isLoading = false) }
                }
        }
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    fun uploadPhoto(tripId: String, bytes: ByteArray, caption: String = "") {
        viewModelScope.launch {
            // tryLock() is non-blocking: returns false if another upload is already running
            if (!uploadMutex.tryLock()) return@launch
            try {
                _uiState.update { it.copy(isUploading = true) }
                photoRepository.uploadPhoto(tripId, bytes, caption)
                    .onSuccess { _uiState.update { it.copy(isUploading = false) } }
                    .onFailure { e ->
                        _uiState.update { it.copy(isUploading = false, error = e.message) }
                    }
            } finally {
                uploadMutex.unlock()
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deletePhoto(tripId: String, photo: TripPhoto) {
        viewModelScope.launch {
            photoRepository.deletePhoto(tripId, photo.id, photo.storageUrl)
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun setError(message: String) { _uiState.update { it.copy(error = message) } }
}
