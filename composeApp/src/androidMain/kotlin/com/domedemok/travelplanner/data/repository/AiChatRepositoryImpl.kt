package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.ChatMessage
import com.domedemok.travelplanner.data.remote.ChatLimits
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.AiChatBusinessRules
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Persists the AI assistant conversation. Messages are written under
 * `trips/{id}/ai_chat` with a nested `baseMessage` object — kept this way for
 * backward-compatibility with existing Firestore documents.
 *
 * [MessageType] is derived from [ChatMessage.senderId] at parse time so no
 * schema migration is required.
 */
class AiChatRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AiChatRepository {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val currentUserName: String?
        get() = auth.currentUser?.displayName

    private fun aiChatRef(tripId: String) =
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
                AiChatBusinessRules.SendDecision.SenderIdMismatch ->
                    return Result.failure(Exception("Sender ID mismatch"))
                is AiChatBusinessRules.SendDecision.Allowed -> {
                    val docRef = aiChatRef(tripId).document()
                    docRef.set(
                        AiChatBusinessRules.buildMessagePayload(
                            docId      = docRef.id,
                            tripId     = tripId,
                            message    = message,
                            senderName = decision.resolvedSenderName,
                        )
                    ).await()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAiMessagesFlow(tripId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = recentMessagesQuery(tripId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { parseAiMessage(it, tripId) }.reversed())
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getAiMessages(tripId: String): Result<List<ChatMessage>> = runCatching {
        recentMessagesQuery(tripId)
            .get()
            .await()
            .documents
            .mapNotNull { parseAiMessage(it, tripId) }
            .reversed()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun recentMessagesQuery(tripId: String) =
        aiChatRef(tripId)
            .orderBy(AiChatBusinessRules.ORDER_FIELD, Query.Direction.DESCENDING)
            .limit(ChatLimits.MESSAGE_LIMIT.toLong())

    private fun parseAiMessage(doc: DocumentSnapshot, tripId: String): ChatMessage? = try {
        val baseMap = doc.get("baseMessage") as? Map<*, *> ?: return null
        val senderId = baseMap["senderId"] as? String ?: ""
        ChatMessage(
            id          = baseMap["id"]         as? String ?: doc.id,
            tripId      = baseMap["tripId"]     as? String ?: tripId,
            text        = baseMap["text"]       as? String ?: "",
            senderId    = senderId,
            senderName  = baseMap["senderName"] as? String ?: "",
            timestamp   = (baseMap["timestamp"] as? Number)?.toLong() ?: 0L,
            messageType = AiChatBusinessRules.messageTypeFor(senderId),
        )
    } catch (_: Exception) { null }
}
