package com.domedemok.travelplanner.data.model

import kotlinx.serialization.Serializable

/**
 * Distinguishes who authored a [ChatMessage].
 * Used to separate user messages from AI responses without a separate wrapper type.
 */
enum class MessageType { USER_CHAT, AI_CHAT }

/** A single message — used for both trip group chat and the per-trip AI assistant chat. */
@Serializable
data class ChatMessage(
    val id:          String      = "",
    val tripId:      String      = "",
    val text:        String      = "",
    val senderId:    String      = "",            // Firebase Auth UID (or AI_BOT_USER_ID)
    val senderName:  String      = "",            // denormalised display name
    val timestamp:   Long        = 0L,
    val messageType: MessageType = MessageType.USER_CHAT,
)
