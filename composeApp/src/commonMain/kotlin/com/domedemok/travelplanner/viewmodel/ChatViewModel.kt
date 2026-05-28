package com.domedemok.travelplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domedemok.travelplanner.data.model.ChatMessage
import com.domedemok.travelplanner.data.repository.AuthRepository
import com.domedemok.travelplanner.data.repository.ChatRepository
import com.domedemok.travelplanner.util.getCurrentTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private companion object {
        const val UNKNOWN_NAME = "Unknown"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _uiState.value = _uiState.value.copy(
                currentUserId   = user?.id,
                currentUserName = user?.displayName ?: UNKNOWN_NAME,
            )
        }
    }

    fun loadMessages(tripId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            chatRepository.getMessagesFlow(tripId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load messages"
                    )
                }
                .collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false
                    )
                }
        }
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage(tripId: String) {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val userId = _uiState.value.currentUserId
        if (userId == null) {
            _uiState.value = _uiState.value.copy(error = "User not authenticated")
            return
        }
        val userName = _uiState.value.currentUserName ?: UNKNOWN_NAME

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, error = null)

            val message = ChatMessage(
                tripId     = tripId,
                text       = text,
                senderId   = userId,
                senderName = userName,
                timestamp  = getCurrentTimestamp(),
            )

            chatRepository.sendMessage(tripId, message)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(inputText = "", isSending = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error     = e.message ?: "Failed to send message",
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}