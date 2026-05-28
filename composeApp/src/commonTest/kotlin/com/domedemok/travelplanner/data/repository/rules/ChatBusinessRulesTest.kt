package com.domedemok.travelplanner.data.repository.rules

import com.domedemok.travelplanner.data.model.ChatMessage
import com.domedemok.travelplanner.data.model.MessageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ChatBusinessRulesTest {

    // ── validateAndPrepareSend ──────────────────────────────────────────────

    @Test
    fun `current user sender is allowed and gets their display name`() {
        val decision = ChatBusinessRules.validateAndPrepareSend(
            messageSenderId = "u-1",
            currentUserId   = "u-1",
            currentUserName = "Alice",
        )
        val allowed = assertIs<ChatBusinessRules.SendDecision.Allowed>(decision)
        assertEquals("Alice", allowed.resolvedSenderName)
    }

    @Test
    fun `current user with blank display name falls back to Unknown User`() {
        val decision = ChatBusinessRules.validateAndPrepareSend(
            messageSenderId = "u-1",
            currentUserId   = "u-1",
            currentUserName = "",
        )
        val allowed = assertIs<ChatBusinessRules.SendDecision.Allowed>(decision)
        assertEquals(ChatBusinessRules.UNKNOWN_DISPLAY_NAME, allowed.resolvedSenderName)
    }

    @Test
    fun `current user with null display name falls back to Unknown User`() {
        val decision = ChatBusinessRules.validateAndPrepareSend(
            messageSenderId = "u-1",
            currentUserId   = "u-1",
            currentUserName = null,
        )
        val allowed = assertIs<ChatBusinessRules.SendDecision.Allowed>(decision)
        assertEquals(ChatBusinessRules.UNKNOWN_DISPLAY_NAME, allowed.resolvedSenderName)
    }

    @Test
    fun `impersonation attempt is rejected`() {
        val decision = ChatBusinessRules.validateAndPrepareSend(
            messageSenderId = "other-user",
            currentUserId   = "u-1",
            currentUserName = "Alice",
        )
        assertEquals(ChatBusinessRules.SendDecision.SenderIdMismatch, decision)
    }

    // ── buildMessagePayload ─────────────────────────────────────────────────

    @Test
    fun `payload is flat (not nested) and includes the six expected fields`() {
        val msg = ChatMessage(
            id          = "ignored-existing-id",
            tripId      = "t-1",
            text        = "Hello team",
            senderId    = "u-1",
            senderName  = "should-not-be-used",
            timestamp   = 5_000L,
            messageType = MessageType.USER_CHAT,
        )
        val payload = ChatBusinessRules.buildMessagePayload(
            docId      = "doc-42",
            tripId     = "t-1",
            message    = msg,
            senderName = "Alice",
        )
        assertEquals(
            setOf("id", "tripId", "text", "senderId", "senderName", "timestamp"),
            payload.keys,
        )
        assertEquals("doc-42",     payload["id"])
        assertEquals("t-1",        payload["tripId"])
        assertEquals("Hello team", payload["text"])
        assertEquals("u-1",        payload["senderId"])
        assertEquals("Alice",      payload["senderName"])
        assertEquals(5_000L,       payload["timestamp"])
    }
}
