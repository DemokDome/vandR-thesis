package com.domedemok.travelplanner.data.repository.rules

import com.domedemok.travelplanner.data.model.ChatMessage
import com.domedemok.travelplanner.data.model.MessageType
import com.domedemok.travelplanner.data.remote.ContextBuilder

/**
 * Pure decision logic for [com.domedemok.travelplanner.data.repository.AiChatRepository].
 *
 * Covers: sender-id authorization (only the auth'd user or the AI bot may
 * write), display-name override for AI messages, the nested `baseMessage`
 * Firestore field shape, and the message-type derivation from `senderId`.
 */
object AiChatBusinessRules {

    /** Display name shown for messages originating from the Gemini bot. */
    const val AI_DISPLAY_NAME:      String = "Gemini AI"

    /** Fallback when the auth user's displayName is missing. */
    const val UNKNOWN_DISPLAY_NAME: String = "Unknown User"

    /** Nested field path used for Firestore order-by queries. */
    const val ORDER_FIELD:          String = "baseMessage.timestamp"

    /** Decision returned by [validateAndPrepareSend]. */
    sealed class SendDecision {
        /** `message.senderId` is neither the AI bot id nor the current user — reject. */
        data object SenderIdMismatch : SendDecision()

        /** Caller may write the message with [resolvedSenderName] under the bot/user identity. */
        data class Allowed(val resolvedSenderName: String) : SendDecision()
    }

    /**
     * Authorizes a send attempt and resolves the display name to write.
     *
     *  - AI bot messages are always allowed and always get [AI_DISPLAY_NAME].
     *  - User messages must match the caller's [currentUserId] and are written
     *    with [currentUserName] (falling back to [UNKNOWN_DISPLAY_NAME]).
     *  - Any other senderId is rejected as a mismatch (prevents impersonation).
     */
    fun validateAndPrepareSend(
        messageSenderId: String,
        currentUserId:   String,
        currentUserName: String?,
    ): SendDecision = when (messageSenderId) {
        ContextBuilder.AI_BOT_USER_ID -> SendDecision.Allowed(AI_DISPLAY_NAME)
        currentUserId                 -> SendDecision.Allowed(
            currentUserName?.takeIf { it.isNotBlank() } ?: UNKNOWN_DISPLAY_NAME
        )
        else                          -> SendDecision.SenderIdMismatch
    }

    /**
     * Field-shape for `trips/{tripId}/ai_chat/{docId}` on insert.
     *
     * Wrapped in `{ baseMessage: { … } }` for backward-compat with the
     * previous `AiChatMessage` typed wrapper. The platform impl is responsible
     * for allocating [docId] and (on JS) converting `timestamp` to Double.
     */
    fun buildMessagePayload(
        docId:      String,
        tripId:     String,
        message:    ChatMessage,
        senderName: String,
    ): Map<String, Any> = mapOf(
        "baseMessage" to mapOf(
            "id"         to docId,
            "tripId"     to tripId,
            "text"       to message.text,
            "senderId"   to message.senderId,
            "senderName" to senderName,
            "timestamp"  to message.timestamp,
        )
    )

    /**
     * Derives the rendering type of a chat message from its sender id. Done
     * at parse time so the schema doesn't need a separate type field.
     */
    fun messageTypeFor(senderId: String): MessageType =
        if (senderId == ContextBuilder.AI_BOT_USER_ID) MessageType.AI_CHAT
        else MessageType.USER_CHAT
}
