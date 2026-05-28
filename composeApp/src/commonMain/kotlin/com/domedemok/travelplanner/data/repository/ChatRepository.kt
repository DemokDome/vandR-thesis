package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Group chat for a trip's members. Backed by `trips/{id}/group_chat` in Firestore;
 * the AI assistant's conversation is in a separate subcollection — see [AiChatRepository].
 */
interface ChatRepository {
    suspend fun sendMessage(tripId: String, message: ChatMessage): Result<Unit>

    /** Real-time stream of the most recent messages, oldest first. */
    fun getMessagesFlow(tripId: String): Flow<List<ChatMessage>>

    /** One-shot fetch of the most recent messages, oldest first. */
    suspend fun getMessages(tripId: String): Result<List<ChatMessage>>
}
