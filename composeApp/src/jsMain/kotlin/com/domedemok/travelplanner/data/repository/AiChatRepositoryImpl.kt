package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.ChatMessage
import com.domedemok.travelplanner.data.remote.ChatLimits
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.AiChatBusinessRules
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.js
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * JS implementation of [AiChatRepository].
 *
 * Uses manual JS-dynamic parsing (same approach as JS TripRepositoryImpl) because
 * the previous `data<AiChatMessage>()` auto-deserialiser depended on the now-removed
 * [AiChatMessage] wrapper type. Timestamps are read as Double to handle both epoch-ms
 * Longs written by Android and Firestore server Timestamp objects.
 *
 * Firestore document shape `{ baseMessage: { … } }` is preserved for backward-compat.
 */
class AiChatRepositoryImpl : AiChatRepository {

    private val auth      = Firebase.auth
    private val firestore = Firebase.firestore

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val currentUserName: String?
        get() = auth.currentUser?.displayName

    private fun aiChatRef(tripId: String): CollectionReference =
        firestore.collection(FirestoreCollections.TRIPS).document(tripId).collection(FirestoreCollections.AI_CHAT)

    override suspend fun sendAiMessage(tripId: String, message: ChatMessage): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            // Pure rule: authorize sender + resolve the display name to write.
            val decision = AiChatBusinessRules.validateAndPrepareSend(
                messageSenderId = message.senderId,
                currentUserId   = userId,
                currentUserName = currentUserName,
            )

            when (decision) {
                AiChatBusinessRules.SendDecision.SenderIdMismatch -> {
                    console.error("AiChatRepository: Sender ID mismatch!")
                    return Result.failure(Exception("Sender ID mismatch"))
                }
                is AiChatBusinessRules.SendDecision.Allowed -> {
                    val docRef = aiChatRef(tripId).document
                    val basePayload = AiChatBusinessRules.buildMessagePayload(
                        docId      = docRef.id,
                        tripId     = tripId,
                        message    = message,
                        senderName = decision.resolvedSenderName,
                    )

                    // JS Firestore needs Double for the 64-bit timestamp — patch the
                    // inner map produced by the shared builder.
                    @Suppress("UNCHECKED_CAST")
                    val patched = basePayload.toMutableMap().apply {
                        val inner = (basePayload["baseMessage"] as Map<String, Any>).toMutableMap()
                        inner["timestamp"] = message.timestamp.toDouble()
                        put("baseMessage", inner)
                    }

                    docRef.set(patched)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            console.error("AiChatRepository: sendAiMessage failed:", e)
            Result.failure(e)
        }
    }

    override fun getAiMessagesFlow(tripId: String): Flow<List<ChatMessage>> =
        recentMessagesQuery(tripId).snapshots.map { snapshot ->
            snapshot.documents.mapNotNull { parseAiMessage(it, tripId) }.reversed()
        }

    override suspend fun getAiMessages(tripId: String): Result<List<ChatMessage>> = runCatching {
        recentMessagesQuery(tripId).get()
            .documents
            .mapNotNull { parseAiMessage(it, tripId) }
            .reversed()
    }

    private fun recentMessagesQuery(tripId: String): Query =
        aiChatRef(tripId)
            .orderBy(AiChatBusinessRules.ORDER_FIELD, Direction.DESCENDING)
            .limit(ChatLimits.MESSAGE_LIMIT)

    // ── Parsing ───────────────────────────────────────────────────────────────

    private fun parseAiMessage(doc: DocumentSnapshot, tripId: String): ChatMessage? = try {
        val dyn = doc.dynData() ?: return null
        val base = dyn.baseMessage ?: return null
        val senderId = parseDynString(base.senderId)
        ChatMessage(
            id          = parseDynString(base.id, doc.id),
            tripId      = parseDynString(base.tripId, tripId),
            text        = parseDynString(base.text),
            senderId    = senderId,
            senderName  = parseDynString(base.senderName),
            timestamp   = parseDynTimestamp(base.timestamp),
            messageType = AiChatBusinessRules.messageTypeFor(senderId),
        )
    } catch (_: Exception) { null }

    private fun DocumentSnapshot.dynData(): dynamic {
        val data = js.data() ?: return null
        if (data == js("undefined")) return null
        return data.asDynamic()
    }

    private fun parseDynString(value: dynamic, default: String = ""): String = try {
        if (value == null || value == js("undefined")) default else value.toString()
    } catch (_: Exception) { default }

    private fun parseDynTimestamp(value: dynamic): Long = try {
        when {
            value == null || value == js("undefined") -> 0L
            // Firestore Timestamp object from Android client
            js("typeof value === 'object' && value !== null && 'seconds' in value") as Boolean ->
                ((value.seconds as Double) * 1000).toLong()
            else -> value.toString().toDoubleOrNull()?.toLong() ?: 0L
        }
    } catch (_: Exception) { 0L }
}
