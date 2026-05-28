package com.domedemok.travelplanner.data.repository.rules

import com.domedemok.travelplanner.data.model.ChatMessage

/**
 * Pure decision logic for [com.domedemok.travelplanner.data.repository.ChatRepository]
 * (group chat — distinct from the AI chat).
 *
 * Smaller than the other rule files because group chat genuinely has very
 * little logic beyond the SDK call. We extract only the sender-authorization
 * check, the display-name fallback, and the message-document shape.
 */
object ChatBusinessRules {

    /** Fallback when the auth user's displayName is missing. */
    const val UNKNOWN_DISPLAY_NAME: String = "Unknown User"

    /** Firestore field used for chronological ordering. */
    const val ORDER_FIELD:          String = "timestamp"

    /** Decision returned by [validateAndPrepareSend]. */
    sealed class SendDecision {
        /** `message.senderId` doesn't match the current authenticated user — reject. */
        data object SenderIdMismatch : SendDecision()

        /** Caller may write the message with [resolvedSenderName]. */
        data class Allowed(val resolvedSenderName: String) : SendDecision()
    }

    /**
     * Authorizes a group-chat send. Unlike AI chat, there is no "bot" sender —
     * the only allowed sender is the currently authenticated user.
     */
    fun validateAndPrepareSend(
        messageSenderId: String,
        currentUserId:   String,
        currentUserName: String?,
    ): SendDecision =
        if (messageSenderId != currentUserId) SendDecision.SenderIdMismatch
        else SendDecision.Allowed(
            currentUserName?.takeIf { it.isNotBlank() } ?: UNKNOWN_DISPLAY_NAME
        )

    /**
     * Field-shape for `trips/{tripId}/group_chat/{docId}` on insert.
     *
     * The platform impl allocates [docId] beforehand and (on JS) converts
     * `timestamp` to Double after the fact.
     */
    fun buildMessagePayload(
        docId:      String,
        tripId:     String,
        message:    ChatMessage,
        senderName: String,
    ): Map<String, Any> = mapOf(
        "id"         to docId,
        "tripId"     to tripId,
        "text"       to message.text,
        "senderId"   to message.senderId,
        "senderName" to senderName,
        "timestamp"  to message.timestamp,
    )
}
