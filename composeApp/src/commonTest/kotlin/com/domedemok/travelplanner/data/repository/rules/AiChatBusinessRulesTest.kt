package com.domedemok.travelplanner.data.repository.rules

import com.domedemok.travelplanner.data.model.ChatMessage
import com.domedemok.travelplanner.data.model.MessageType
import com.domedemok.travelplanner.data.remote.ContextBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AiChatBusinessRulesTest {

    // ── validateAndPrepareSend ──────────────────────────────────────────────

    @Test
    fun `bot sender is always allowed and gets the AI display name`() {
        val decision = AiChatBusinessRules.validateAndPrepareSend(
            messageSenderId = ContextBuilder.AI_BOT_USER_ID,
            currentUserId   = "u-1",
            currentUserName = "Alice",
        )
        val allowed = assertIs<AiChatBusinessRules.SendDecision.Allowed>(decision)
        assertEquals(AiChatBusinessRules.AI_DISPLAY_NAME, allowed.resolvedSenderName)
    }

    @Test
    fun `current user sender is allowed and gets their display name`() {
        val decision = AiChatBusinessRules.validateAndPrepareSend(
            messageSenderId = "u-1",
            currentUserId   = "u-1",
            currentUserName = "Alice",
        )
        val allowed = assertIs<AiChatBusinessRules.SendDecision.Allowed>(decision)
        assertEquals("Alice", allowed.resolvedSenderName)
    }

    @Test
    fun `current user with null display name falls back to Unknown User`() {
        val decision = AiChatBusinessRules.validateAndPrepareSend(
            messageSenderId = "u-1",
            currentUserId   = "u-1",
            currentUserName = null,
        )
        val allowed = assertIs<AiChatBusinessRules.SendDecision.Allowed>(decision)
        assertEquals(AiChatBusinessRules.UNKNOWN_DISPLAY_NAME, allowed.resolvedSenderName)
    }

    @Test
    fun `impersonation attempt is rejected`() {
        val decision = AiChatBusinessRules.validateAndPrepareSend(
            messageSenderId = "other-user-99",
            currentUserId   = "u-1",
            currentUserName = "Alice",
        )
        assertEquals(AiChatBusinessRules.SendDecision.SenderIdMismatch, decision)
    }

    // ── buildMessagePayload ─────────────────────────────────────────────────

    @Test
    fun `payload nests fields under baseMessage for backward compatibility`() {
        val msg = ChatMessage(
            id          = "ignored-existing-id",
            tripId      = "t-1",
            text        = "Hi Gemini",
            senderId    = "u-1",
            senderName  = "should-not-be-used",
            timestamp   = 5_000L,
            messageType = MessageType.USER_CHAT,
        )
        val payload = AiChatBusinessRules.buildMessagePayload(
            docId      = "doc-42",
            tripId     = "t-1",
            message    = msg,
            senderName = "Alice",
        )
        assertEquals(setOf("baseMessage"), payload.keys)
        @Suppress("UNCHECKED_CAST")
        val base = payload["baseMessage"] as Map<String, Any>
        assertEquals("doc-42",   base["id"])
        assertEquals("t-1",      base["tripId"])
        assertEquals("Hi Gemini",base["text"])
        assertEquals("u-1",      base["senderId"])
        assertEquals("Alice",    base["senderName"])
        assertEquals(5_000L,     base["timestamp"])
    }

    // ── messageTypeFor ──────────────────────────────────────────────────────

    @Test
    fun `bot id maps to AI_CHAT`() {
        assertEquals(MessageType.AI_CHAT, AiChatBusinessRules.messageTypeFor(ContextBuilder.AI_BOT_USER_ID))
    }

    @Test
    fun `any other id maps to USER_CHAT`() {
        assertEquals(MessageType.USER_CHAT, AiChatBusinessRules.messageTypeFor("u-1"))
        assertEquals(MessageType.USER_CHAT, AiChatBusinessRules.messageTypeFor(""))
    }
}
