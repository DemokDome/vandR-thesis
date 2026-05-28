package com.domedemok.travelplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domedemok.travelplanner.data.model.ChatMessage
import com.domedemok.travelplanner.data.model.MessageType
import com.domedemok.travelplanner.data.remote.AiService
import com.domedemok.travelplanner.data.remote.ContextBuilder
import com.domedemok.travelplanner.data.repository.AiChatRepository
import com.domedemok.travelplanner.data.repository.AuthRepository
import com.domedemok.travelplanner.data.repository.ItineraryRepository
import com.domedemok.travelplanner.data.repository.PlaceRepository
import com.domedemok.travelplanner.data.repository.TripRepository
import com.domedemok.travelplanner.util.generateUniqueId
import com.domedemok.travelplanner.util.getCurrentTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val isLoading: Boolean = false,
    val isAiThinking: Boolean = false,
    val error: String? = null,
)

class AiChatViewModel(
    private val aiChatRepository: AiChatRepository,
    private val authRepository: AuthRepository,
    private val tripRepository: TripRepository,
    private val placeRepository: PlaceRepository,
    private val itineraryRepository: ItineraryRepository,
    private val aiService: AiService,
    private val contextBuilder: ContextBuilder,
) : ViewModel() {

    private companion object {
        const val AI_DISPLAY_NAME = "Gemini AI"
        const val UNKNOWN_NAME    = "Unknown"
        /** Strips Markdown decoration that Gemini occasionally emits despite the system-prompt instruction. */
        val MARKDOWN_RE = Regex("[*#_`]")
    }

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

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
            aiChatRepository.getAiMessagesFlow(tripId)
                .catch { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
                .collect { messages ->
                    _uiState.value = _uiState.value.copy(messages = messages, isLoading = false)
                }
        }
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendAiMessage(tripId: String) {
        val text     = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        val userId   = _uiState.value.currentUserId ?: return
        val userName = _uiState.value.currentUserName ?: UNKNOWN_NAME

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(inputText = "", isAiThinking = true, error = null)

            // Persist the user's turn first so it shows in the conversation immediately.
            aiChatRepository.sendAiMessage(
                tripId,
                buildMessage(tripId, text, userId, userName, MessageType.USER_CHAT),
            )

            val trip = tripRepository.getTripById(tripId).getOrNull()
            if (trip == null) {
                _uiState.value = _uiState.value.copy(isAiThinking = false, error = "Failed to load trip data for context")
                return@launch
            }
            val places        = placeRepository.getPlacesForTrip(tripId).getOrNull().orEmpty()
            val itineraryDays = runCatching { itineraryRepository.getDaysFlow(tripId).first() }.getOrElse { emptyList() }

            // Re-fetch the persisted history so the request includes the turn we just wrote.
            val historyMessages = aiChatRepository.getAiMessages(tripId).getOrNull().orEmpty()

            val travelContext = contextBuilder.buildTravelContext(
                trip          = trip,
                places        = places,
                itineraryDays = itineraryDays,
            )
            val chatHistory = contextBuilder.buildChatHistory(messages = historyMessages)

            aiService.sendMessage(text, travelContext, chatHistory)
                .onSuccess { reply ->
                    aiChatRepository.sendAiMessage(
                        tripId,
                        buildMessage(
                            tripId      = tripId,
                            text        = stripMarkdown(reply),
                            senderId    = ContextBuilder.AI_BOT_USER_ID,
                            name        = AI_DISPLAY_NAME,
                            messageType = MessageType.AI_CHAT,
                        ),
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = "AI failed to respond: ${e.message ?: "unknown error"}")
                }

            _uiState.value = _uiState.value.copy(isAiThinking = false)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun buildMessage(
        tripId:      String,
        text:        String,
        senderId:    String,
        name:        String,
        messageType: MessageType = MessageType.USER_CHAT,
    ): ChatMessage = ChatMessage(
        id          = generateUniqueId(),
        tripId      = tripId,
        text        = text,
        senderId    = senderId,
        senderName  = name,
        timestamp   = getCurrentTimestamp(),
        messageType = messageType,
    )

    /**
     * Defence-in-depth: the system prompt instructs Gemini not to emit Markdown,
     * but the model occasionally slips in `*`, `#`, `_`, or backtick markers.
     * We strip them client-side so the chat bubble always renders as plain text.
     * [MARKDOWN_RE] is compiled once as a companion val to avoid per-call overhead.
     */
    private fun stripMarkdown(text: String): String = MARKDOWN_RE.replace(text, "")
}
