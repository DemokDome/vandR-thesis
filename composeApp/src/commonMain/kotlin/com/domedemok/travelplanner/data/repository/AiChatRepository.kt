package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Persists the in-trip AI assistant conversation under `trips/{id}/ai_chat`.
 *
 * Uses [ChatMessage] directly — distinguished from group chat messages via
 * [com.domedemok.travelplanner.data.model.MessageType.AI_CHAT] on the sender side.
 * The Firestore document structure is kept as `{ baseMessage: { … } }` for
 * backward-compatibility with existing documents.
 */
interface AiChatRepository {
    suspend fun sendAiMessage(tripId: String, message: ChatMessage): Result<Unit>

    /** Real-time stream of the most recent messages, oldest first. */
    fun getAiMessagesFlow(tripId: String): Flow<List<ChatMessage>>

    /** One-shot fetch of the most recent messages, oldest first — used to seed Gemini context. */
    suspend fun getAiMessages(tripId: String): Result<List<ChatMessage>>
}
